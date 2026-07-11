package com.mynote.android.util

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
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

    /** 带默认凭证的便捷调用 */
    suspend fun transcribe(audioFile: File, appId: String, apiKey: String, apiSecret: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val url = buildAuthUrl(appId, apiKey, apiSecret)
                val client = buildClient()
                val deferred = CompletableDeferred<String>()

                val ws = client.newWebSocket(
                    Request.Builder().url(url).build(),
                    object : WebSocketListener() {
                        val sb = StringBuilder()

                        override fun onOpen(webSocket: WebSocket, response: Response) {
                            sendAudioFrames(webSocket, audioFile, appId)
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
                                            sb.append(cwArr.getJSONObject(j).optString("w", ""))
                                        }
                                    }
                                }
                                if (data?.optInt("status", 0) == 2) {
                                    deferred.complete(sb.toString().trim())
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
                            if (!deferred.isCompleted) deferred.complete(sb.toString().trim())
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

    private fun sendAudioFrames(webSocket: WebSocket, file: File, appId: String) {
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
                    put("language", "zh_cn")
                    put("domain", "iat")
                    put("accent", "mandarin")
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
        if (trackIndex < 0) return ByteArray(0)
        extractor.selectTrack(trackIndex)

        val mime = extractor.getTrackFormat(trackIndex).getString(MediaFormat.KEY_MIME) ?: "audio/mp4a-latm"
        val codec = MediaCodec.createDecoderByType(mime)
        codec.configure(extractor.getTrackFormat(trackIndex), null, null, 0)
        codec.start()

        val outBuffers = mutableListOf<ShortArray>()
        var totalSamples = 0
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

        codec.stop()
        codec.release()
        extractor.release()

        // 合并并重采样到 16kHz
        val allSamples = ShortArray(totalSamples)
        var pos = 0
        for (arr in outBuffers) {
            System.arraycopy(arr, 0, allSamples, pos, arr.size)
            pos += arr.size
        }

        return if (sampleRate == 16000) {
            shortsToBytes(allSamples)
        } else {
            // 简单线性重采样
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
