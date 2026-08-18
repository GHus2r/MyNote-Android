package com.mynote.android.util

import android.content.Context
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * DeepSeek API — AI 病历生成
 * OpenAI 兼容接口
 */
object DeepSeekClient {

    private const val ENDPOINT = "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions"
    private const val MODEL = "qwen-max"

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
        .callTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    /** 病历生成参数 */
    data class GenParams(
        val patientName: String,
        val age: Int,
        val gender: String,
        val bedNumber: String,
        val department: String,
        val admissionDate: String,
        val diagnosis: String,
        val chiefComplaint: String,
        val presentIllness: String = "",
        val physicalExam: String = "",
        val labResults: String = "",
        val orders: String = "",
        val recordTypes: List<String>  // 需要生成的文书类型列表
    )

    /** 流式回调 */
    interface StreamCallback {
        fun onToken(token: String)          // 逐字/逐块输出
        fun onDone(fullText: String)        // 完成
        fun onError(e: Exception)           // 出错
    }

    /** 流式生成（SSE） */
    fun generateStream(context: Context, params: GenParams, callback: StreamCallback) {
        generateStreamWithPrompt(context, buildPrompt(params), callback)
    }

    /** 流式生成 — 使用自定义 prompt（润色/改写等） */
    fun generateStreamWithPrompt(context: Context, prompt: String, callback: StreamCallback) {
        val apiKey = Prefs(context).qwenApiKey
        if (apiKey.isEmpty()) { callback.onError(Exception("未配置Qwen API Key")); return }
        val body = JSONObject().apply {
            put("model", MODEL)
            put("temperature", 0.3)
            put("max_tokens", 8192)
            put("stream", true)
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", prompt)
                })
            })
        }

        Thread {
            try {
                val resp = client.newCall(
                    Request.Builder().url(ENDPOINT)
                        .header("Authorization", "Bearer $apiKey")
                        .header("Content-Type", "application/json")
                        .post(body.toString().toRequestBody("application/json".toMediaType()))
                        .build()
                ).execute()

                // 用 use 确保 Response 及底层连接被释放
                resp.use { r ->
                    if (!r.isSuccessful) {
                        callback.onError(Exception("HTTP ${r.code}: ${r.body?.string()?.take(200)}"))
                        return@use
                    }

                    val sb = StringBuilder()
                    var done = false
                    r.body?.charStream()?.buffered()?.forEachLine { line ->
                        if (line.startsWith("data: ")) {
                            val data = line.removePrefix("data: ")
                            if (data == "[DONE]") {
                                callback.onDone(sb.toString().trim())
                                done = true
                                return@forEachLine
                            }
                            try {
                                val content = JSONObject(data)
                                    .getJSONArray("choices")
                                    .getJSONObject(0)
                                    .getJSONObject("delta")
                                    .optString("content", "")
                                if (content.isNotEmpty()) {
                                    sb.append(content)
                                    callback.onToken(content)
                                }
                            } catch (_: Exception) { /* 跳过非 content 的 delta 行 */ }
                        }
                    }
                    if (!done) callback.onDone(sb.toString().trim())
                }
            } catch (e: Exception) {
                callback.onError(e)
            }
        }.start()
    }

    suspend fun generate(context: Context, params: GenParams): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = Prefs(context).qwenApiKey
        if (apiKey.isEmpty()) return@withContext Result.failure(Exception("未配置Qwen API Key"))

        val prompt = buildPrompt(params)

        val body = JSONObject().apply {
            put("model", MODEL)
            put("temperature", 0.3)
            put("max_tokens", 8192)
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", prompt)
                })
            })
        }

        val resp = client.newCall(
            Request.Builder().url(ENDPOINT)
                .header("Authorization", "Bearer $apiKey")
                .header("Content-Type", "application/json")
                .post(body.toString().toRequestBody("application/json".toMediaType()))
                .build()
        ).execute()

        val respBody = resp.body?.string() ?: ""
        if (!resp.isSuccessful) return@withContext Result.failure(
            Exception("DeepSeek HTTP ${resp.code}: ${respBody.take(200)}")
        )

        try {
            val json = JSONObject(respBody)
            val text = json.getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .optString("content", "")
            if (text.isEmpty()) return@withContext Result.failure(Exception("生成结果为空"))
            Result.success(text.trim())
        } catch (e: Exception) {
            Result.failure(Exception("解析失败: ${e.message}"))
        }
    }

    // ---------- Prompt 构建 ----------

    private fun buildPrompt(p: GenParams): String {
        val typeNames = p.recordTypes.joinToString("、")
        val dept = p.department.ifEmpty { "内科" }
        return buildString {
            append("你是一位三甲医院${dept}主任医师，具有三十年临床经验。请根据以下患者信息，以专业格式撰写病历文书。\n\n")

            append("=== 患者基本信息 ===\n")
            append("姓名：${p.patientName}\n")
            append("年龄：${p.age}岁\n")
            append("性别：${p.gender}\n")
            if (p.bedNumber.isNotEmpty()) append("床号：${p.bedNumber}\n")
            append("科室：${dept}\n")
            if (p.admissionDate.isNotEmpty()) append("入院日期：${p.admissionDate}\n")
            append("入院诊断：${p.diagnosis}\n\n")

            append("=== 临床信息 ===\n")
            append("主诉：${p.chiefComplaint}\n")
            if (p.presentIllness.isNotEmpty()) append("现病史：${p.presentIllness}\n")
            if (p.physicalExam.isNotEmpty()) append("体格检查：${p.physicalExam}\n")
            if (p.labResults.isNotEmpty()) append("辅助检查：${p.labResults}\n")
            if (p.orders.isNotEmpty()) append("当前医嘱：${p.orders}\n")
            append("\n")

            append("=== 生成要求 ===\n")
            append("需要生成的文书类型：${typeNames}\n")
            append("严格遵守《病历书写基本规范》（国家卫健委2022版）三甲医院标准：\n\n")

            append("【入院记录】必须包含以下全部段落，缺一不可：\n")
            append("  一般项目（姓名、性别、年龄、民族、婚姻、出生地、职业、入院时间、记录时间、病史陈述者）\n")
            append("  主诉（症状+部位+持续时间，≤20字，不用诊断名词）\n")
            append("  现病史（围绕主诉详述：起病诱因/缓急、主要症状特点、伴随症状、诊疗经过、发病以来一般情况如精神/饮食/睡眠/二便/体重变化）\n")
            append("  既往史（平素健康状况、传染病史、预防接种史、手术外伤史、输血史、过敏史、慢性病史）\n")
            append("  个人史（出生地/居住地、烟酒嗜好、冶游史、职业暴露）\n")
            append("  婚育史（婚姻、生育）\n")
            append("  家族史（父母/兄弟姐妹/子女健康状况，遗传病史）\n")
            append("  体格检查（T℃/P次/分/R次/分/BPmmHg，按系统：一般情况→皮肤黏膜→浅表淋巴结→头颈部→胸部→腹部→脊柱四肢→神经系统，逐项□勾选，每系统至少列出3个检查项目）\n")
            append("  辅助检查（院前+入院后，注明检查日期和机构）\n")
            append("  初步诊断（主次分明，编号排列，诊断全名不可缩写，修正诊断后续补充）\n")
            append("  诊疗计划（逐条列出检查项目、治疗用药、护理级别）\n\n")

            append("【首次病程记录】严格包含三部分：\n")
            append("  病例特点（提炼主诉现病史关键点、阳性体征、异常检查结果的归纳，200-300字）\n")
            append("  拟诊讨论（诊断依据逐条列出+鉴别诊断逐一分析排除，必须有逻辑推理过程）\n")
            append("  诊疗计划（具体检查项目名称、治疗用药方案、进一步检查安排）\n\n")

            append("【日常病程记录】按SOAP格式：\n")
            append("  主观（患者自述症状变化）\n")
            append("  客观（今日查体要点、新检查结果）\n")
            append("  评估（病情分析、诊断调整）\n")
            append("  计划（下一步诊疗措施、医嘱调整）\n\n")

            append("通用要求：\n")
            append("1. 已有信息按实际内容撰写，保持专业术语规范\n")
            append("2. 缺失的段落必须保留标题，内容写「【待补充】」，严禁编造\n")
            append("3. 用药需注明通用名+剂量+用法+频次\n")
            append("4. 检查结果需注明数值+单位+参考范围\n")
            append("5. 每份文书前用「【文书类型名称】」标识，各文书间用「---」分隔\n")
            append("6. 日期时间格式：yyyy-MM-dd HH:mm\n")
            append("7. 严禁使用\"患者\"\"病人\"替代姓名，已有姓名的地方用实际姓名\n\n")

            // ── 科室专属模板注入 ──
            val spec = DeptRecordTemplate.getSpecialty(dept)
            if (spec != null) {
                append("=== ${dept}专科专属要求（严格遵照执行） ===\n")
                append(spec)
            }
        }
    }
}
