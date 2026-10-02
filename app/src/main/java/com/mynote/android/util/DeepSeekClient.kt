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

    // ---------- 病历质控审计 ----------

    /** 审计 prompt 版本号（用于结果可追溯） */
    const val AUDIT_PROMPT_VERSION = "AUDIT_PROMPT_v1"

    /**
     * 构建病历质控审计 prompt
     * @param dept 科室（决定专科判定依据）
     * @param recordType 文书类型（决定必备要素清单）
     * @param ocrText OCR 提取的病历原文
     */
    fun buildAuditPrompt(dept: String, recordType: String, ocrText: String): String {
        val d = dept.ifEmpty { "内科" }
        return buildString {
            append("你是一位三甲医院${d}质控科主任医师，精通《病历书写基本规范》（国家卫健委2022版）与病历质量评审标准。\n")
            append("请对以下已书写完成的病历（由照片 OCR 识别而来，可能含错字）进行严格质控分析。\n\n")

            append("=== 病历原文（OCR 识别结果） ===\n")
            append(ocrText.take(6000))
            append("\n\n")

            append("=== 文书类型 ===\n")
            append(recordType.ifEmpty { "未知，请根据内容判断" })
            append("\n\n")

            // 注入必备要素清单（来自 RecordSpec 单一数据源）
            append("=== 判定依据（必备要素清单） ===\n")
            append(RecordSpec.toPromptBlock(recordType))
            append("\n")

            // 注入科室专科要求（复用 DeptRecordTemplate）
            val spec = DeptRecordTemplate.getSpecialty(d)
            if (spec != null) {
                append("=== ${d}专科书写要求（用于完整性与准确性判定） ===\n")
                append(spec.take(1500))
                append("\n\n")
            }

            append("=== 分析要求 ===\n")
            append("从以下三个维度分析，并严格输出 JSON（不要 markdown 代码块，不要 JSON 之外的任何文字）：\n")
            append("1. 漏写项 missingItems：对照必备要素清单逐项判断是否缺失。\n")
            append("2. 规范性 normIssues：术语规范、格式分段、签名与时限、诊断全称、用药通用名+剂量、禁用「患者」代姓名等。\n")
            append("3. 准确性 accuracyIssues：主诉与现病史一致性、诊断依据逻辑、用药与诊断匹配度、异常值处理、鉴别诊断覆盖。\n\n")

            append("输出 JSON 结构（字段固定，数组可为空，务必只输出 JSON）：\n")
            append("""{"summary":"一句话总评","scoreTotal":0,"scoreCompleteness":0,"scoreNorms":0,"scoreAccuracy":0,"missingItems":[{"item":"","severity":"必填","basis":"","suggestion":""}],"normIssues":[{"point":"","expectation":"","basis":"","fix":""}],"accuracyIssues":[{"aspect":"","issue":"","evidence":"","suggestion":"","refGuideline":""}],"improvementPlan":[{"step":"","action":"","target":"","rationale":""}]}""")
            append("\n\n")

            append("约束：\n")
            append("1. scoreTotal = scoreCompleteness + scoreNorms + scoreAccuracy，各分项取值范围分别为 0-40 / 0-30 / 0-30\n")
            append("2. 每条 issue 的 basis 必须引用具体规范条款或专科要求，禁止泛泛而谈\n")
            append("3. accuracyIssues 的 evidence 必须引用病历原文片段\n")
            append("4. 未发现问题的维度输出空数组 []\n")
            append("5. 基于 OCR 原文判断；若原文疑似识别错误导致歧义，在 suggestion 中提示「建议人工核对原文」\n")
        }
    }

    /** 自定义 prompt 的非流式生成（供病历质控审计等场景复用，低温稳定输出） */
    suspend fun generateWithPrompt(context: Context, prompt: String): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = Prefs(context).qwenApiKey
        if (apiKey.isEmpty()) return@withContext Result.failure(Exception("未配置Qwen API Key"))

        val body = JSONObject().apply {
            put("model", MODEL)
            put("temperature", 0.1)
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
            Exception("HTTP ${resp.code}: ${respBody.take(200)}")
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
}
