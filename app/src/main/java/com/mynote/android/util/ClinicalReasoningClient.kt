package com.mynote.android.util

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

/**
 * AI 临床辅助分析 — 融合推理客户端
 * 模型路由：百川 Baichuan-M3-Plus 优先（证据锚定 95%），未配置或失败则降级 qwen-max
 */
object ClinicalReasoningClient {

    private const val BAICHUAN_ENDPOINT = "https://api.baichuan-ai.com/v1/chat/completions"
    private const val BAICHUAN_MODEL = "Baichuan-M3-Plus"

    private const val QWEN_ENDPOINT = "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions"
    private const val QWEN_MODEL = "qwen-max"

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
        .callTimeout(150, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    private data class Route(val endpoint: String, val model: String, val apiKey: String)

    /** 解析可用路由：百川优先，qwen 兜底 */
    private fun resolveRoute(p: Prefs): Route? {
        val bc = p.baichuanApiKey
        if (bc.isNotEmpty()) return Route(BAICHUAN_ENDPOINT, BAICHUAN_MODEL, bc)
        val qw = p.qwenApiKey
        if (qw.isNotEmpty()) return Route(QWEN_ENDPOINT, QWEN_MODEL, qw)
        return null
    }

    /** 融合推理，返回模型输出的原始文本（JSON） */
    suspend fun analyze(context: Context, prompt: String): Result<String> = withContext(Dispatchers.IO) {
        val p = Prefs(context)
        val primary = resolveRoute(p)
            ?: return@withContext Result.failure(Exception("未配置 AI API Key（请在设置页填入百川或 Qwen Key）"))
        try {
            Result.success(call(primary, prompt))
        } catch (e: Exception) {
            // 主路由是百川且失败时，降级 qwen-max 重试一次
            if (primary.model == BAICHUAN_MODEL && p.qwenApiKey.isNotEmpty()) {
                try {
                    Result.success(call(Route(QWEN_ENDPOINT, QWEN_MODEL, p.qwenApiKey), prompt))
                } catch (e2: Exception) {
                    Result.failure(e2)
                }
            } else {
                Result.failure(e)
            }
        }
    }

    private fun call(route: Route, prompt: String): String {
        val body = JSONObject().apply {
            put("model", route.model)
            put("temperature", 0.2)
            put("max_tokens", 8192)
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", prompt)
                })
            })
        }

        val resp = client.newCall(
            Request.Builder().url(route.endpoint)
                .header("Authorization", "Bearer ${route.apiKey}")
                .header("Content-Type", "application/json")
                .post(body.toString().toRequestBody("application/json".toMediaType()))
                .build()
        ).execute()

        return resp.use { r ->
            val respBody = r.body?.string() ?: ""
            if (!r.isSuccessful) throw Exception("HTTP ${r.code}: ${respBody.take(200)}")
            val json = JSONObject(respBody)
            val text = json.getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .optString("content", "")
            if (text.isBlank()) throw Exception("生成结果为空")
            text.trim()
        }
    }
}
