package com.mynote.android.util

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Base64
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import okhttp3.*
import org.json.JSONObject
import java.io.File
import java.net.URLEncoder
import java.util.concurrent.atomic.AtomicInteger
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import java.security.cert.X509Certificate
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.*

/**
 * 讯飞语音听写 (IAT) WebSocket 封装 —— 长音频版
 * - IAT 单会话限 60s：自动按 55s 分片转写后拼接
 * - 严格按 40ms/1280B 节流发送，business 参数仅首帧携带
 * - 每片 150s 超时 + pingInterval 保活，半开连接不再永久挂起
 * - 片间并发 2 路（讯飞免费版并发上限）
 * 凭证通过 Prefs.iatAppId / iatApiKey / iatApiSecret 配置（设置页→讯飞语音识别）
 */
object IatHelper {

    private const val HOST = "iat-api.xfyun.cn"
    private const val PATH = "/v2/iat"
    private const val URL_BASE = "wss://$HOST$PATH"

    private const val SEGMENT_SECONDS = 55          // 每片时长（< 60s 上限）
    private const val BYTES_PER_SEC = 16000 * 2     // 16kHz 16bit mono
    private const val SEGMENT_PARALLEL = 2          // 并发片数
    private const val SEGMENT_TIMEOUT_MS = 150_000L // 单片超时
    private const val FRAME_INTERVAL_MS = 40L       // 官方要求的 40ms/1280B 节流

    private val dateFormat = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss 'GMT'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("GMT")
    }

    private val mainHandler = Handler(Looper.getMainLooper())

    /**
     * 转写参数
     * @param language zh_cn / en_us
     * @param accent mandarin / cantonese / 空字符串=不限制
     * @param pd 说话人分离: "0"=关闭, "1"=开启（注意：分片模式下说话人编号每片独立，pd 建议仅用于短音频）
     * @param onProgress 流式回调（主线程），传入当前累积文本
     */
    data class TranscribeParams(
        val language: String = "zh_cn",
        val accent: String = "mandarin",
        val pd: String = "0",
        val onProgress: ((accumulatedText: String) -> Unit)? = null
    )

    /**
     * 转写结果
     * @param rawText 纯文本（不含说话人标记）
     * @param annotatedText 带说话人标记的文本（仅 pd=1 时有意义）: "说话人0: xxx\n说话人1: xxx"
     */
    data class TranscribeResult(
        val rawText: String,
        val annotatedText: String
    )

    /**
     * 转写音频文件 → 纯文本。凭证从 Prefs 读取。自动分片，onProgress 报告 (已完成片数, 总片数)。
     * @param pcmFile 可选共享 PCM 文件（16k mono，由调用方解码一次供多路 ASR 共用；不传则内部解码）
     */
    suspend fun transcribe(
        context: Context, audioFile: File,
        onSegmentProgress: ((done: Int, total: Int) -> Unit)? = null,
        pcmFile: File? = null
    ): Result<String> {
        val p = Prefs(context)
        val appId = p.iatAppId
        val apiKey = p.iatApiKey
        val apiSecret = p.iatApiSecret
        if (appId.isEmpty() || apiKey.isEmpty()) {
            return Result.failure(Exception("请先在设置中配置讯飞 API 凭证"))
        }
        return transcribe(audioFile, appId, apiKey, apiSecret, onSegmentProgress, pcmFile)
    }

    /** 带凭证的便捷调用（无流式回调，无 pd） */
    suspend fun transcribe(
        audioFile: File, appId: String, apiKey: String, apiSecret: String,
        onSegmentProgress: ((done: Int, total: Int) -> Unit)? = null,
        pcmFile: File? = null
    ): Result<String> {
        return transcribeStreaming(audioFile, appId, apiKey, apiSecret, TranscribeParams(), onSegmentProgress, pcmFile)
            .map { it.rawText }
    }

    /**
     * 流式转写（长音频自动分片）
     * 每片转写完成后回调 onProgress（主线程，累积文本）
     * 分片进度回调 onSegmentProgress（任意线程，(已完成, 总数)）
     */
    suspend fun transcribeStreaming(
        audioFile: File,
        appId: String,
        apiKey: String,
        apiSecret: String,
        params: TranscribeParams,
        onSegmentProgress: ((done: Int, total: Int) -> Unit)? = null,
        pcmFile: File? = null
    ): Result<TranscribeResult> {
        return withContext(Dispatchers.IO) {
            var ownPcm: File? = null
            try {
                // PCM 流式落盘（磁盘文件），堆内不再持有完整 27MB PCM —— 修复 OOM
                val pcm = pcmFile ?: PcmDecoder.decodeToPcmFile(audioFile).also { ownPcm = it }
                val totalBytes = pcm.length()
                if (totalBytes == 0L) {
                    return@withContext Result.failure(Exception("音频解码失败（文件损坏或格式不支持）"))
                }
                val segBytes = SEGMENT_SECONDS.toLong() * BYTES_PER_SEC
                // 切片 (offset, len)
                val sliceList = mutableListOf<Pair<Long, Int>>()
                var off = 0L
                while (off < totalBytes) {
                    sliceList.add(off to minOf(segBytes, totalBytes - off).toInt())
                    off += segBytes
                }
                // 尾片太短（< 2s）并入前一片（最长 57s < 60s 上限）
                if (sliceList.size > 1 && sliceList.last().second < 2 * BYTES_PER_SEC) {
                    val last = sliceList.removeAt(sliceList.size - 1)
                    val prev = sliceList.removeAt(sliceList.size - 1)
                    sliceList.add(prev.first to prev.second + last.second)
                }

                val total = sliceList.size
                val doneCount = AtomicInteger(0)
                val rawArr = arrayOfNulls<String>(total)
                val annArr = arrayOfNulls<String>(total)
                val sem = Semaphore(SEGMENT_PARALLEL)
                var firstError: Exception? = null
                val errMutex = Object()

                coroutineScope {
                    sliceList.mapIndexed { idx, slice ->
                        async {
                            sem.withPermit {
                                val seg = readSlice(pcm, slice.first, slice.second)
                                val r = transcribeSegment(seg, appId, apiKey, apiSecret, params)
                                val d = doneCount.incrementAndGet()
                                onSegmentProgress?.invoke(d, total)
                                r.fold(
                                    onSuccess = { res ->
                                        rawArr[idx] = res.rawText
                                        annArr[idx] = res.annotatedText
                                        params.onProgress?.let { cb ->
                                            val acc = rawArr.filterNotNull().joinToString("")
                                            mainHandler.post { cb(acc) }
                                        }
                                    },
                                    onFailure = { e ->
                                        synchronized(errMutex) {
                                            if (firstError == null) firstError = e as? Exception ?: Exception(e.message ?: "转写失败")
                                        }
                                    }
                                )
                            }
                        }
                    }.awaitAll()
                }

                val anySuccess = rawArr.any { !it.isNullOrEmpty() }
                if (!anySuccess) {
                    return@withContext Result.failure(firstError ?: Exception("识别结果为空"))
                }
                val raw = rawArr.mapIndexed { i, t ->
                    t ?: "[第${i + 1}段识别失败]"   // 部分失败：拼接占位标记，保留其余内容
                }.joinToString("").trim()
                // 说话人标记：单片段保留讯飞 pd 结果；多片段每片编号独立重置，annotated 降级为 raw
                val annotated = if (total == 1) (annArr[0] ?: raw) else raw
                Result.success(TranscribeResult(raw, annotated))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Result.failure(e)
            } finally {
                ownPcm?.delete()
            }
        }
    }

    /** 从 PCM 磁盘文件随机读取一片（堆内仅此一片，~1.7MB） */
    private fun readSlice(f: File, offset: Long, len: Int): ByteArray {
        val buf = ByteArray(len)
        java.io.RandomAccessFile(f, "r").use { raf ->
            raf.seek(offset)
            raf.readFully(buf)
        }
        return buf
    }

    /** 单片转写（≤55s PCM），内部含超时保护。返回 rawText + 说话人标记 annotatedText（pd=1 时） */
    private suspend fun transcribeSegment(
        pcm: ByteArray, appId: String, apiKey: String, apiSecret: String,
        params: TranscribeParams
    ): Result<TranscribeResult> = withContext(Dispatchers.IO) {
        try {
            val url = buildAuthUrl(appId, apiKey, apiSecret)
            val client = buildClient()
            val deferred = CompletableDeferred<TranscribeResult>()

            val ws = client.newWebSocket(
                Request.Builder().url(url).build(),
                object : WebSocketListener() {
                    val rawSb = StringBuilder()
                    val annotatedSb = StringBuilder()
                    var currentSpeaker = -1

                    override fun onOpen(webSocket: WebSocket, response: Response) {
                        // 独立线程节流发送，避免阻塞 OkHttp 回调线程
                        Thread {
                            try {
                                sendAudioFrames(webSocket, pcm, appId, params.language, params.accent, params.pd)
                            } catch (e: Exception) {
                                if (!deferred.isCompleted) deferred.completeExceptionally(e)
                            }
                        }.start()
                    }

                    override fun onMessage(webSocket: WebSocket, text: String) {
                        try {
                            val json = JSONObject(text)
                            val code = json.optInt("code", 0)
                            if (code != 0) {
                                deferred.completeExceptionally(Exception("讯飞错误 $code: ${json.optString("message")}"))
                                webSocket.close(1000, "")
                                return
                            }
                            val data = json.optJSONObject("data")
                            val result = data?.optJSONObject("result")
                            if (result != null) {
                                val wsArr = result.optJSONArray("ws")
                                for (i in 0 until wsArr.length()) {
                                    val cwArr = wsArr.getJSONObject(i).optJSONArray("cw")
                                    for (j in 0 until cwArr.length()) {
                                        val cwObj = cwArr.getJSONObject(j)
                                        val word = cwObj.optString("w", "")
                                        rawSb.append(word)

                                        // pd=1 时读取说话人标签
                                        if (params.pd == "1") {
                                            val rg = cwObj.optInt("rg", -1)
                                            if (rg >= 0 && rg != currentSpeaker) {
                                                if (annotatedSb.isNotEmpty()) annotatedSb.append("\n")
                                                annotatedSb.append("说话人").append(rg + 1).append(": ")
                                                currentSpeaker = rg
                                            }
                                        }
                                        annotatedSb.append(word)
                                    }
                                }
                            }
                            if (data?.optInt("status", 0) == 2) {
                                val raw = rawSb.toString().trim()
                                val annotated = annotatedSb.toString().trim().ifEmpty { raw }
                                deferred.complete(TranscribeResult(raw, annotated))
                                webSocket.close(1000, "")
                            }
                        } catch (e: Exception) {
                            deferred.completeExceptionally(e)
                            webSocket.close(1000, "")
                        }
                    }

                    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                        deferred.completeExceptionally(t)
                    }

                    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                        if (!deferred.isCompleted) {
                            val raw = rawSb.toString().trim()
                            val annotated = annotatedSb.toString().trim().ifEmpty { raw }
                            deferred.complete(TranscribeResult(raw, annotated))
                        }
                    }

                    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                        if (!deferred.isCompleted) {
                            val raw = rawSb.toString().trim()
                            val annotated = annotatedSb.toString().trim().ifEmpty { raw }
                            deferred.complete(TranscribeResult(raw, annotated))
                        }
                    }
                })

            // 关键：超时保护。此前无超时导致半开连接时永久挂起（"识别中..."卡死）
            val res = withTimeout(SEGMENT_TIMEOUT_MS) { deferred.await() }
            client.dispatcher.executorService.shutdown()
            Result.success(res)
        } catch (e: TimeoutCancellationException) {
            Result.failure(Exception("讯飞识别超时（单片 ${SEGMENT_TIMEOUT_MS / 1000}s）"))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ---------- 内部实现 ----------

    private fun buildAuthUrl(appId: String, apiKey: String, apiSecret: String): String {
        val date = dateFormat.format(Date())
        val signatureOrigin = "host: $HOST\ndate: $date\nGET $PATH HTTP/1.1"
        val signatureSha = hmacSha256(apiSecret, signatureOrigin)
        val signature = Base64.encodeToString(signatureSha, Base64.NO_WRAP)
        val authorizationOrigin = "api_key=\"$apiKey\", algorithm=\"hmac-sha256\", headers=\"host date request-line\", signature=\"$signature\""
        val authorization = Base64.encodeToString(authorizationOrigin.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
        return "$URL_BASE?" +
            "host=${URLEncoder.encode(HOST, "UTF-8")}" +
            "&date=${URLEncoder.encode(date, "UTF-8")}" +
            "&authorization=${URLEncoder.encode(authorization, "UTF-8")}" +
            "&appid=$appId"
    }

    private fun buildClient(): OkHttpClient {
        val trustAll = arrayOf<TrustManager>(object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        })
        val sslContext = SSLContext.getInstance("TLS").apply { init(null, trustAll, SecureRandom()) }
        return OkHttpClient.Builder()
            .sslSocketFactory(sslContext.socketFactory, trustAll[0] as X509TrustManager)
            .hostnameVerifier { _, _ -> true }
            .pingInterval(20, java.util.concurrent.TimeUnit.SECONDS)  // 保活，检测半开连接
            .build()
    }

    /**
     * 节流发送：1280B/帧、每帧间隔 40ms（官方要求）
     * 首帧携带 common + business，后续帧仅 data
     */
    private fun sendAudioFrames(
        webSocket: WebSocket, pcm: ByteArray, appId: String,
        language: String = "zh_cn", accent: String = "mandarin", pd: String = "0"
    ) {
        val frameSize = 1280
        var offset = 0
        var first = true
        var status = 0

        while (offset < pcm.size) {
            val len = minOf(frameSize, pcm.size - offset)
            val chunk = pcm.copyOfRange(offset, offset + len)
            val payload = Base64.encodeToString(chunk, Base64.NO_WRAP)

            val frame = JSONObject()
            if (first) {
                frame.put("common", JSONObject().put("app_id", appId))
                frame.put("business", JSONObject().apply {
                    put("language", language)
                    put("domain", "iat")
                    put("accent", accent)
                    put("pd", pd)
                    put("vad_eos", 10000)
                })
                first = false
            }
            frame.put("data", JSONObject().apply {
                put("status", status)
                put("format", "audio/L16;rate=16000")
                put("encoding", "raw")
                put("audio", payload)
            })
            webSocket.send(frame.toString())
            if (status == 0) status = 1
            offset += len
            Thread.sleep(FRAME_INTERVAL_MS)
        }

        // 结束帧
        val endFrame = JSONObject().apply {
            put("data", JSONObject().apply {
                put("status", 2)
                put("format", "audio/L16;rate=16000")
                put("encoding", "raw")
                put("audio", "")
            })
        }
        webSocket.send(endFrame.toString())
    }

    private fun hmacSha256(key: String, data: String): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key.toByteArray(Charsets.UTF_8), "HmacSHA256"))
        return mac.doFinal(data.toByteArray(Charsets.UTF_8))
    }
}
