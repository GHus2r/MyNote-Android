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
 * 引擎选择：
 *  - ENGINE_M3PLUS（默认）：百川 Baichuan-M3-Plus 医疗循证（证据锚定 95%），未配置/失败降级 qwen-max
 *  - ENGINE_DEEPSEEK：DeepSeek-R1 通用推理，未配置/失败回退 M3-Plus
 */
object ClinicalReasoningClient {

    const val ENGINE_M3PLUS = "m3plus"       // 百川 M3-Plus（医疗循证，推荐）
    const val ENGINE_DEEPSEEK = "deepseek"   // DeepSeek-R1（通用推理）

    private const val BAICHUAN_ENDPOINT = "https://api.baichuan-ai.com/v1/chat/completions"
    private const val BAICHUAN_MODEL = "Baichuan-M3-Plus"

    private const val QWEN_ENDPOINT = "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions"
    private const val QWEN_MODEL = "qwen-max"

    private const val DEEPSEEK_ENDPOINT = "https://api.deepseek.com/v1/chat/completions"
    private const val DEEPSEEK_MODEL = "deepseek-reasoner"

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(180, java.util.concurrent.TimeUnit.SECONDS)
        .callTimeout(200, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    private data class Route(val endpoint: String, val model: String, val apiKey: String)

    /** M3-Plus 优先，qwen 兜底 */
    private fun resolveM3Route(p: Prefs): Route? {
        val bc = p.baichuanApiKey
        if (bc.isNotEmpty()) return Route(BAICHUAN_ENDPOINT, BAICHUAN_MODEL, bc)
        val qw = p.qwenApiKey
        if (qw.isNotEmpty()) return Route(QWEN_ENDPOINT, QWEN_MODEL, qw)
        return null
    }

    /** DeepSeek-R1 */
    private fun resolveDeepRoute(p: Prefs): Route? {
        val ds = p.deepseekApiKey
        if (ds.isNotEmpty()) return Route(DEEPSEEK_ENDPOINT, DEEPSEEK_MODEL, ds)
        return null
    }

    /** 融合推理，返回模型输出的原始文本（JSON）。engine 见 ENGINE_* 常量 */
    suspend fun analyze(context: Context, prompt: String, engine: String = ENGINE_M3PLUS): Result<String> =
        withContext(Dispatchers.IO) {
            val p = Prefs(context)
            if (engine == ENGINE_DEEPSEEK) {
                val ds = resolveDeepRoute(p)
                if (ds != null) {
                    try {
                        return@withContext Result.success(call(ds, prompt))
                    } catch (e: Exception) {
                        // DeepSeek 失败，回退 M3-Plus
                        return@withContext analyzeM3(p, prompt)
                    }
                }
                // 未配置 DeepSeek Key，回退 M3-Plus
                return@withContext analyzeM3(p, prompt)
            }
            return@withContext analyzeM3(p, prompt)
        }

    private fun analyzeM3(p: Prefs, prompt: String): Result<String> {
        val primary = resolveM3Route(p)
            ?: return Result.failure(Exception("未配置 AI API Key（请在设置页填入百川或 Qwen Key）"))
        return try {
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
            // 强制 JSON 输出（deepseek-reasoner 不支持 response_format，其余模型均支持）
            if (route.model != DEEPSEEK_MODEL) {
                put("response_format", JSONObject().apply { put("type", "json_object") })
            }
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
