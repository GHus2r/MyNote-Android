package com.mynote.android.util

import android.content.Context
import android.util.Base64
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File

/**
 * 阿里云 Qwen3-ASR — 维吾尔语语音识别
 * 支持两种凭证：sk-xxx（Bearer token）和 AK/SK（先换 Token）
 */
object QwenAsrClient {

    private const val ENDPOINT = "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions"
    private const val TOKEN_URL = "https://dashscope.aliyuncs.com/api/v1/tokens"
    private const val MODEL = "qwen3-asr-flash"

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .callTimeout(65, java.util.concurrent.TimeUnit.SECONDS)
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

                val bytes = audioFile.readBytes()
                if (bytes.size > 10 * 1024 * 1024) throw Exception("音频超过10MB限制")
                val dataUri = "data:audio/mp4;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)

                val body = JSONObject().apply {
                    put("model", MODEL)
                    put("messages", org.json.JSONArray().apply {
                        put(JSONObject().apply {
                            put("role", "user")
                            put("content", org.json.JSONArray().apply {
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
                Result.success(text.trim())
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
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
