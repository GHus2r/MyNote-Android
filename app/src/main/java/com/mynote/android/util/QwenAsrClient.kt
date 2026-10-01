package com.mynote.android.util

import android.content.Context
import android.util.Base64
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

/**
 * 阿里云 Qwen3-ASR — 长音频切片版
 * qwen3-asr-flash 单次请求限约 3 分钟 / 10MB：
 * 解码为 16k PCM → 按 90s 切片封 WAV → 并发 3 路转写 → 按序拼接
 * 支持两种凭证：sk-xxx（Bearer token）和 AK/SK（先换 Token）
 */
object QwenAsrClient {

    private const val ENDPOINT = "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions"
    private const val TOKEN_URL = "https://dashscope.aliyuncs.com/api/v1/tokens"
    private const val MODEL = "qwen3-asr-flash"

    private const val SEGMENT_SECONDS = 90
    private const val BYTES_PER_SEC = 16000 * 2
    private const val SEGMENT_PARALLEL = 3

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(300, java.util.concurrent.TimeUnit.SECONDS)
        .callTimeout(310, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    suspend fun transcribe(context: Context, audioFile: File): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val p = Prefs(context)
                val apiKey = p.qwenApiKey
                val apiSecret = p.qwenApiSecret
                if (apiKey.isEmpty()) throw Exception("未配置阿里云凭证")

                val token = getToken(apiKey, apiSecret).getOrElse {
                    throw Exception("获取Token失败: ${it.message}")
                }

                val pcm = PcmDecoder.decodeToPcm16k(audioFile)
                if (pcm.isEmpty()) throw Exception("音频解码失败（文件损坏或格式不支持）")

                val segBytes = SEGMENT_SECONDS * BYTES_PER_SEC
                val segments = if (pcm.size <= segBytes) listOf(pcm) else {
                    (0 until pcm.size step segBytes).map { off ->
                        pcm.copyOfRange(off, minOf(off + segBytes, pcm.size))
                    }
                }

                val total = segments.size
                val doneCount = AtomicInteger(0)
                val texts = arrayOfNulls<String>(total)
                val sem = Semaphore(SEGMENT_PARALLEL)
                var firstError: Exception? = null
                val errMutex = Object()

                coroutineScope {
                    segments.mapIndexed { idx, seg ->
                        async {
                            sem.withPermit {
                                val r = runCatching { transcribeSegment(token, seg) }
                                doneCount.incrementAndGet()
                                r.fold(
                                    onSuccess = { text ->
                                        texts[idx] = text
                                    },
                                    onFailure = { e ->
                                        val ex = e as? Exception ?: Exception(e.message ?: "识别失败")
                                        synchronized(errMutex) {
                                            if (firstError == null) firstError = ex
                                        }
                                    }
                                )
                            }
                        }
                    }.awaitAll()
                }

                val anySuccess = texts.any { !it.isNullOrEmpty() }
                if (!anySuccess) throw firstError ?: Exception("识别结果为空")
                val merged = texts.mapIndexed { i, t -> t ?: "[第${i + 1}段识别失败]" }
                    .joinToString("")
                    .trim()
                Result.success(merged)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /** 单片转写：PCM → WAV → base64 dataUri → HTTP */
    private fun transcribeSegment(token: String, pcm: ByteArray): String {
        val wav = PcmDecoder.pcmToWav(pcm)
        val dataUri = "data:audio/wav;base64," + Base64.encodeToString(wav, Base64.NO_WRAP)

        val body = JSONObject().apply {
            put("model", MODEL)
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", JSONArray().apply {
                        put(JSONObject().apply {
                            put("type", "input_audio")
                            put("input_audio", JSONObject().apply {
                                put("data", dataUri)
                            })
                        })
                    })
                })
            })
        }

        val resp = client.newCall(
            Request.Builder().url(ENDPOINT)
                .header("Authorization", "Bearer $token")
                .header("Content-Type", "application/json")
                .post(body.toString().toRequestBody("application/json".toMediaType()))
                .build()
        ).execute()

        val respBody = resp.body?.string() ?: ""
        if (!resp.isSuccessful) throw Exception("阿里云 HTTP ${resp.code}: ${respBody.take(200)}")

        val json = JSONObject(respBody)
        val text = json.getJSONArray("choices")
            .getJSONObject(0)
            .getJSONObject("message")
            .optString("content", "")
        if (text.isEmpty()) throw Exception("识别结果为空")
        return text.trim()
    }

    /** 用 AK/SK 或 sk-xxx 获取临时 Token */
    private fun getToken(apiKey: String, apiSecret: String): Result<String> {
        if (apiKey.startsWith("sk-")) return Result.success(apiKey)
        if (apiSecret.isEmpty()) return Result.failure(Exception("AccessKey Secret 为空"))
        val cred = Base64.encodeToString("$apiKey:$apiSecret".toByteArray(Charsets.UTF_8), Base64.NO_WRAP)

        return try {
            val resp = client.newCall(
                Request.Builder().url(TOKEN_URL)
                    .header("Authorization", "Basic $cred")
                    .post("{}".toRequestBody("application/json".toMediaType()))
                    .build()
            ).execute()
            val body = resp.body?.string() ?: ""
            if (!resp.isSuccessful) return Result.failure(Exception("Token接口 HTTP ${resp.code}: ${body.take(100)}"))
            val token = JSONObject(body).optString("token", "")
            if (token.isEmpty()) Result.failure(Exception("Token为空: ${body.take(100)}"))
            else Result.success(token)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
