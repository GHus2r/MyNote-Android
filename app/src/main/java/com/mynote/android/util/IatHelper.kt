package com.mynote.android.util

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.os.Handler
import android.os.Looper
import android.util.Base64
import kotlinx.coroutines.*
import okhttp3.*
import org.json.JSONObject
import java.io.File
import java.net.URLEncoder
import java.nio.ByteBuffer
import java.nio.ByteOrder
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
 * 讯飞语音听写 (IAT) WebSocket 封装
 * 凭证通过 Prefs.iatAppId / iatApiKey / iatApiSecret 配置（设置页→讯飞语音识别）
 */
object IatHelper {

    private const val HOST = "iat-api.xfyun.cn"
    private const val PATH = "/v2/iat"
    private const val URL_BASE = "wss://$HOST$PATH"

    private val dateFormat = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss 'GMT'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("GMT")
    }

    private val mainHandler = Handler(Looper.getMainLooper())

    /**
     * 转写参数
     * @param language zh_cn / en_us
     * @param accent mandarin / cantonese / 空字符串=不限制
     * @param pd 说话人分离: "0"=关闭, "1"=开启（最多区分10人）
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
     * 转写音频文件 → 纯文本。凭证从 Prefs 读取。
     */
    suspend fun transcribe(context: Context, audioFile: File): Result<String> {
        val p = Prefs(context)
        val appId = p.iatAppId
        val apiKey = p.iatApiKey
        val apiSecret = p.iatApiSecret
        if (appId.isEmpty() || apiKey.isEmpty()) {
            return Result.failure(Exception("请先在设置中配置讯飞 API 凭证"))
        }
        return transcribe(audioFile, appId, apiKey, apiSecret)
    }

    /** 带凭证的便捷调用（无流式回调，无 pd） */
    suspend fun transcribe(audioFile: File, appId: String, apiKey: String, apiSecret: String): Result<String> {
        return transcribeStreaming(audioFile, appId, apiKey, apiSecret, TranscribeParams())
            .map { it.rawText }
    }

    /**
     * 流式转写——每收到一帧结果就回调 onProgress（主线程）
     * 返回 TranscribeResult（rawText + 带说话人标记的 annotatedText）
     */
    suspend fun transcribeStreaming(
        audioFile: File,
        appId: String,
        apiKey: String,
        apiSecret: String,
        params: TranscribeParams
    ): Result<TranscribeResult> {
        return withContext(Dispatchers.IO) {
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
                            sendAudioFrames(webSocket, audioFile, appId, params.language, params.accent, params.pd)
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
                                // 流式回调
                                val displayText = if (params.pd == "1") annotatedSb.toString() else rawSb.toString()
                                if (data?.optInt("status", 0) == 1 && displayText.isNotEmpty()) {
                                    params.onProgress?.let { cb ->
                                        mainHandler.post { cb(displayText) }
                                    }
                                }
                                if (data?.optInt("status", 0) == 2) {
                                    val trimmedRaw = rawSb.toString().trim()
                                    val trimmedAnnotated = annotatedSb.toString().trim()
                                    params.onProgress?.let { cb ->
                                        mainHandler.post { cb(if (params.pd == "1") trimmedAnnotated else trimmedRaw) }
                                    }
                                    deferred.complete(TranscribeResult(
                                        rawText = trimmedRaw,
                                        annotatedText = if (params.pd == "1") trimmedAnnotated else trimmedRaw
                                    ))
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
                                val trimmedRaw = rawSb.toString().trim()
                                val trimmedAnnotated = annotatedSb.toString().trim()
                                deferred.complete(TranscribeResult(
                                    rawText = trimmedRaw,
                                    annotatedText = if (params.pd == "1") trimmedAnnotated else trimmedRaw
                                ))
                            }
                        }
                    })

                Result.success(deferred.await())
            } catch (e: Exception) {
                Result.failure(e)
            }
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
            .build()
    }

    private fun sendAudioFrames(
        webSocket: WebSocket, file: File, appId: String,
        language: String = "zh_cn", accent: String = "mandarin", pd: String = "0"
    ) {
        val pcm = decodeToPcm16k(file)
        val frameSize = 1280
        var offset = 0
        var first = true
        var status = 0

        while (offset < pcm.size) {
            val len = minOf(frameSize, pcm.size - offset)
            val chunk = pcm.copyOfRange(offset, offset + len)
            val payload = Base64.encodeToString(chunk, Base64.NO_WRAP)

            val frame = JSONObject().apply {
                put("common", JSONObject().put("app_id", appId))
                put("business", JSONObject().apply {
                    put("language", language)
                    put("domain", "iat")
                    put("accent", accent)
                    put("pd", pd)
                    put("vad_eos", 10000)
                })
                put("data", JSONObject().apply {
                    put("status", status)
                    put("format", "audio/L16;rate=16000")
                    put("encoding", "raw")
                    put("audio", payload)
                })
            }
            webSocket.send(frame.toString())
            if (first) { status = 1; first = false }
            offset += len
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

    /** 将 M4A/AAC 音频解码为 16kHz 16bit 单声道 PCM */
    private fun decodeToPcm16k(file: File): ByteArray {
        val extractor = MediaExtractor()
        extractor.setDataSource(file.absolutePath)
        var trackIndex = -1
        var sampleRate = 0
        for (i in 0 until extractor.trackCount) {
            val fmt = extractor.getTrackFormat(i)
            if (fmt.getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true) {
                trackIndex = i
                sampleRate = fmt.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                break
            }
        }
        if (trackIndex < 0) { extractor.release(); return ByteArray(0) }
        extractor.selectTrack(trackIndex)

        val mime = extractor.getTrackFormat(trackIndex).getString(MediaFormat.KEY_MIME) ?: "audio/mp4a-latm"
        val codec = MediaCodec.createDecoderByType(mime)
        val outBuffers = mutableListOf<ShortArray>()
        var totalSamples = 0
        try {
            codec.configure(extractor.getTrackFormat(trackIndex), null, null, 0)
            codec.start()

            var done = false
            val bufInfo = MediaCodec.BufferInfo()

            while (!done) {
                val inIndex = codec.dequeueInputBuffer(10_000)
                if (inIndex >= 0) {
                    val inputBuf = codec.getInputBuffer(inIndex)!!
                    val sampleSize = extractor.readSampleData(inputBuf, 0)
                    if (sampleSize < 0) {
                        codec.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                    } else {
                        codec.queueInputBuffer(inIndex, 0, sampleSize, extractor.sampleTime, 0)
                        extractor.advance()
                    }
                }

                val outIndex = codec.dequeueOutputBuffer(bufInfo, 10_000)
                when {
                    outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> continue
                    outIndex == MediaCodec.INFO_TRY_AGAIN_LATER -> continue
                    outIndex >= 0 -> {
                        val outBuf = codec.getOutputBuffer(outIndex)!!
                        val shortArr = ShortArray(bufInfo.size / 2)
                        outBuf.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shortArr)
                        outBuf.clear()
                        outBuffers.add(shortArr)
                        totalSamples += shortArr.size
                        codec.releaseOutputBuffer(outIndex, false)
                    }
                }
                if (bufInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) done = true
            }
        } finally {
            try { codec.stop() } catch (_: Exception) {}
            try { codec.release() } catch (_: Exception) {}
            try { extractor.release() } catch (_: Exception) {}
        }

        val allSamples = ShortArray(totalSamples)
        var pos = 0
        for (arr in outBuffers) {
            System.arraycopy(arr, 0, allSamples, pos, arr.size)
            pos += arr.size
        }

        return if (sampleRate == 16000) {
            shortsToBytes(allSamples)
        } else {
            val ratio = sampleRate.toDouble() / 16000.0
            val resampled = ShortArray((allSamples.size / ratio).toInt())
            for (i in resampled.indices) {
                val srcIdx = (i * ratio).toInt().coerceIn(0, allSamples.size - 1)
                resampled[i] = allSamples[srcIdx]
            }
            shortsToBytes(resampled)
        }
    }

    private fun shortsToBytes(shorts: ShortArray): ByteArray {
        val bytes = ByteArray(shorts.size * 2)
        val buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        buf.asShortBuffer().put(shorts)
        return bytes
    }

    private fun hmacSha256(key: String, data: String): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key.toByteArray(Charsets.UTF_8), "HmacSHA256"))
        return mac.doFinal(data.toByteArray(Charsets.UTF_8))
    }
}
