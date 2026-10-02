package com.mynote.android.util

import android.content.Context
import android.graphics.Bitmap
import android.util.Base64
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream

/**
 * Qwen-VL-OCR — 阿里云通义千问视觉 OCR
 * 复用 QwenAsrClient 的 API key 配置
 */
object QwenOcrClient {

    private const val ENDPOINT = "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions"
    private const val MODEL = "qwen-vl-max"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .callTimeout(65, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    /**
     * 用 Qwen-VL 校准 OCR 结果
     * @param bitmap 原始图片
     * @param localText 本地 ML Kit 识别出的原始文本
     * @return JSON 格式的校准结果字符串，失败返回 null
     */
    suspend fun calibrate(context: Context, bitmap: Bitmap, localText: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val p = Prefs(context)
                val apiKey = p.qwenApiKey
                if (apiKey.isEmpty()) return@withContext null

                // 压缩图片（缩放到最长边 1024px，JPEG 60%）减少传输量
                val scaled = scaleBitmap(bitmap, 1024)
                val bos = ByteArrayOutputStream()
                scaled.compress(Bitmap.CompressFormat.JPEG, 60, bos)
                if (scaled !== bitmap) scaled.recycle()
                val base64 = Base64.encodeToString(bos.toByteArray(), Base64.NO_WRAP)

                val prompt = buildString {
                    append("这是一张化验单图片。请逐行提取所有检验项目，每行格式：项目中文名=英文缩写: 结果值 单位 (参考范围)\n\n")
                    append("本地OCR曾识别出以下文字，其中可能存在错别字或遗漏，请参考并修正：\n")
                    append(localText.take(800))
                    append("\n\n严格要求：\n")
                    append("1. 不要输出JSON，直接输出纯文本行\n")
                    append("2. 每行一项检验指标\n")
                    append("3. 异常项在行尾标注⚠\n")
                    append("4. 不要任何解释，只输出结果\n")
                }

                val body = JSONObject().apply {
                    put("model", MODEL)
                    put("messages", JSONArray().apply {
                        put(JSONObject().apply {
                            put("role", "user")
                            put("content", JSONArray().apply {
                                put(JSONObject().apply {
                                    put("type", "image_url")
                                    put("image_url", JSONObject().apply {
                                        put("url", "data:image/jpeg;base64,$base64")
                                    })
                                })
                                put(JSONObject().apply {
                                    put("type", "text")
                                    put("text", prompt)
                                })
                            })
                        })
                    })
                    put("temperature", 0.1)
                    put("max_tokens", 4096)
                }

                val request = Request.Builder()
                    .url(ENDPOINT)
                    .post(body.toString().toRequestBody("application/json".toMediaType()))
                    .header("Authorization", "Bearer $apiKey")
                    .header("Content-Type", "application/json")
                    .build()

                val response = client.newCall(request).execute()
                val respBody = response.body?.string() ?: return@withContext null
                response.close()

                val json = JSONObject(respBody)
                val choices = json.optJSONArray("choices")
                    ?: return@withContext run { android.util.Log.e("QwenOcr", "No choices: $respBody"); null }
                if (choices.length() == 0) return@withContext null

                val message = choices.getJSONObject(0).optJSONObject("message")
                    ?: return@withContext null
                val content = message.optString("content", "").trim()

                // 去掉可能的 markdown 包裹
                content.removePrefix("```").removePrefix("```").removeSuffix("```").trim()
            } catch (e: Exception) {
                android.util.Log.e("QwenOcr", "校准失败", e)
                null
            }
        }
    }

    /**
     * 病历照片全文 OCR —— 提取病历书写内容（手写/打印）
     * 与 calibrate(化验单) 复用同一 qwen-vl-max 通道与压缩/请求框架，仅 prompt 不同。
     * @param bitmap 病历照片
     * @return 提取出的病历纯文本，失败返回 null
     */
    suspend fun extractMedicalText(context: Context, bitmap: Bitmap): String? {
        return withContext(Dispatchers.IO) {
            try {
                val p = Prefs(context)
                val apiKey = p.qwenApiKey
                if (apiKey.isEmpty()) return@withContext null

                // 病历识别对清晰度要求更高：最长边 1536，JPEG 80%
                val scaled = scaleBitmap(bitmap, 1536)
                val bos = ByteArrayOutputStream()
                scaled.compress(Bitmap.CompressFormat.JPEG, 80, bos)
                if (scaled !== bitmap) scaled.recycle()
                val base64 = Base64.encodeToString(bos.toByteArray(), Base64.NO_WRAP)

                val prompt = buildString {
                    append("这是一张手写或打印的病历照片。请完整、准确地识别并转录病历全文，包括标题、各段落标题（如主诉、现病史、既往史、体格检查、辅助检查、诊断、诊疗计划等）、正文、以及末尾的医师签名与书写日期时间。\n\n")
                    append("严格要求：\n")
                    append("1. 只输出病历原文文字，不要任何解释、评价或补充\n")
                    append("2. 按原文顺序逐行转录，保留段落标题\n")
                    append("3. 手写部分若无法辨认，用「【无法辨认】」占位，不要臆测\n")
                    append("4. 不要输出 JSON 或 markdown 代码块\n")
                }

                val body = JSONObject().apply {
                    put("model", MODEL)
                    put("messages", JSONArray().apply {
                        put(JSONObject().apply {
                            put("role", "user")
                            put("content", JSONArray().apply {
                                put(JSONObject().apply {
                                    put("type", "image_url")
                                    put("image_url", JSONObject().apply {
                                        put("url", "data:image/jpeg;base64,$base64")
                                    })
                                })
                                put(JSONObject().apply {
                                    put("type", "text")
                                    put("text", prompt)
                                })
                            })
                        })
                    })
                    put("temperature", 0.1)
                    put("max_tokens", 4096)
                }

                val request = Request.Builder()
                    .url(ENDPOINT)
                    .post(body.toString().toRequestBody("application/json".toMediaType()))
                    .header("Authorization", "Bearer $apiKey")
                    .header("Content-Type", "application/json")
                    .build()

                val response = client.newCall(request).execute()
                val respBody = response.body?.string() ?: return@withContext null
                response.close()

                val json = JSONObject(respBody)
                val choices = json.optJSONArray("choices") ?: return@withContext null
                if (choices.length() == 0) return@withContext null
                val message = choices.getJSONObject(0).optJSONObject("message") ?: return@withContext null
                val content = message.optString("content", "").trim()
                content.removePrefix("```").removePrefix("```").removeSuffix("```").trim()
            } catch (e: Exception) {
                android.util.Log.e("QwenOcr", "病历识别失败", e)
                null
            }
        }
    }

    private fun scaleBitmap(src: Bitmap, maxSide: Int): Bitmap {
        val w = src.width; val h = src.height
        if (w <= maxSide && h <= maxSide) return src
        val scale = maxSide.toFloat() / maxOf(w, h)
        return Bitmap.createScaledBitmap(src, (w * scale).toInt(), (h * scale).toInt(), true)
    }
}
