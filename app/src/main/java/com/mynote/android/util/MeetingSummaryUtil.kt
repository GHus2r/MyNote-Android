package com.mynote.android.util

import android.content.Context
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

/**
 * 会议摘要 — 基于 DeepSeek/Qwen 自动归纳会议要点
 * 支持多种会议类型模板 + 关键词提取 + 结构化待办
 */
object MeetingSummaryUtil {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
        .callTimeout(130, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    /** 待办任务项 */
    data class TodoItem(
        val person: String,     // 负责人
        val task: String,       // 任务描述
        val done: Boolean = false
    )

    data class Summary(
        val title: String,       // 会议标题建议
        val points: String,      // 讨论要点
        val decisions: String,   // 决议事项
        val todos: String,       // 待办任务（Markdown）
        val report: String,      // 完整汇报文档
        val keywords: String,    // 关键词（逗号分隔）
        val todoItems: List<TodoItem> = emptyList()  // 结构化待办
    )

    // ── 会议类型模板（三甲医院）──
    /** 返回 (系统指令, JSON输出格式, 额外要求) */
    private fun templateFor(meetingType: String): Triple<String, String, String> = when (meetingType) {
        // ─ 临床类 ─
        "morning" -> Triple(
            "你是三甲医院晨会交班记录员。请根据对话生成晨会交班记录。",
            """{
  "title": "晨会交班",
  "points": "## 昨日情况\n- 新入院：X人\n- 出院：X人\n- 危重：X人\n\n## 重点交班\n- **患者1**（床号/诊断）：交班内容",
  "decisions": "## 今日安排\n- ✅ 待办1",
  "todos": "## 待办任务\n- [ ] 负责人：任务描述",
  "report": "晨会交班记录",
  "keywords": "诊断关键词, 重点关注"
}""",
            "- 格式：按患者逐一交班，含床号、诊断、目前情况及注意事项\n- 区分「昨日情况」和「重点交班」\n- report 为可直接归档的交班记录"
        )
        "difficult" -> Triple(
            "你是三甲医院疑难病例讨论的专业记录员。请根据讨论内容生成规范的疑难病例讨论记录。",
            """{
  "title": "疑难病例讨论",
  "points": "## 病例摘要\n- 患者基本信息、主诉、现病史、既往史\n\n## 辅助检查\n- 检查结果\n\n## 讨论要点\n- **观点1**（科室/医生）：内容",
  "decisions": "## 诊断意见\n- ✅ 初步诊断\n- ✅ 鉴别诊断",
  "todos": "## 下一步计划\n- [ ] 检查项目 (截止时间)\n- [ ] 治疗方案调整",
  "report": "疑难病例讨论记录",
  "keywords": "诊断关键词, 鉴别诊断关键词"
}""",
            "- 完整记录病例摘要和辅助检查结果\n- 列出各科室/医生的不同观点\n- 明确初步诊断和鉴别诊断\n- report 为符合病历书写规范的讨论记录"
        )
        "mdt" -> Triple(
            "你是三甲医院MDT多学科会诊记录员。请根据多科室讨论生成规范的MDT会诊记录。",
            """{
  "title": "MDT多学科会诊",
  "points": "## 会诊目的\n- 目的说明\n\n## 各科室意见\n- **科室1**：意见\n- **科室2**：意见",
  "decisions": "## 综合诊疗方案\n- ✅ 方案1（科室）\n- ✅ 方案2（科室）",
  "todos": "## 执行计划\n- [ ] 科室/负责人：任务 (时限)",
  "report": "MDT会诊记录",
  "keywords": "疾病关键词, 治疗方案关键词"
}""",
            "- 列出所有参与科室的独立意见\n- 综合各科室意见形成统一诊疗方案\n- 明确每个执行步骤的责任科室和时限\n- report 为可放入病历的正式会诊记录"
        )
        "preop" -> Triple(
            "你是三甲医院术前讨论记录员。请根据讨论生成规范的术前讨论记录。",
            """{
  "title": "术前讨论",
  "points": "## 术前诊断\n- 诊断\n\n## 手术指征\n- 指征说明\n\n## 手术方案\n- 术式、麻醉方式、备选方案",
  "decisions": "## 讨论决议\n- ✅ 手术方案确认\n- ✅ 风险评估等级",
  "todos": "## 术前准备\n- [ ] 检查项目\n- [ ] 备血/备皮\n- [ ] 知情同意",
  "report": "术前讨论记录",
  "keywords": "手术名称, 风险评估关键词"
}""",
            "- 明确手术指征和禁忌症\n- 详细记录手术方案、麻醉方式、备选方案\n- 包含风险评估（ASA分级/NNIS等）\n- report 为符合病历书写规范的术前讨论记录"
        )
        "death" -> Triple(
            "你是三甲医院死亡病例讨论记录员。请根据讨论生成规范的死亡病例讨论记录。",
            """{
  "title": "死亡病例讨论",
  "points": "## 病例摘要\n- 基本信息、住院经过\n\n## 死亡原因分析\n- 直接死因、间接因素\n\n## 讨论要点\n- 诊治过程回顾与反思",
  "decisions": "## 经验教训\n- ✅ 可改进环节1\n- ✅ 流程优化建议",
  "todos": "## 后续事项\n- [ ] 完善病历\n- [ ] 家属沟通",
  "report": "死亡病例讨论记录",
  "keywords": "死因关键词, 经验教训关键词"
}""",
            "- 完整回顾诊治全过程的各环节\n- 深入分析死因和可改进之处\n- 提出具体改进措施避免类似情况\n- report 为可归档的正式讨论记录"
        )
        // ─ 管理类 ─
        "admin" -> Triple(
            "你是三甲医院院周会/行政会议记录员。请根据会议内容生成规范的行政会议纪要。",
            """{
  "title": "院周会纪要",
  "points": "## 工作汇报\n- 部门1：汇报要点\n\n## 议题讨论\n- **议题1**：讨论内容",
  "decisions": "## 院部决议\n- ✅ 决议1（责任部门）\n- ✅ 决议2（责任部门）",
  "todos": "## 任务部署\n- [ ] 科室/负责人：任务描述 (完成时限)",
  "report": "院周会纪要",
  "keywords": "管理关键词, 重点工作"
}""",
            "- 按部门/科室分条记录汇报内容\n- 决议明确标注责任部门和完成时限\n- report 为可正式下发的院周会纪要"
        )
        "dept" -> Triple(
            "你是三甲医院科务会记录员。请根据科室内部会议内容生成科务会纪要。",
            """{
  "title": "科务会纪要",
  "points": "## 医疗质量\n- 质控指标、病历检查\n\n## 学科建设\n- 技术开展、人才培养",
  "decisions": "## 科室决议\n- ✅ 决议1\n- ✅ 决议2",
  "todos": "## 工作安排\n- [ ] 负责人：任务 (截止时间)",
  "report": "科务会纪要",
  "keywords": "科室管理关键词"
}""",
            "- 涵盖医疗质量、学科建设、教学科研等方面\n- 质控数据体现具体指标而非泛泛而谈\n- report 为科室内部正式纪要"
        )
        "quality" -> Triple(
            "你是三甲医院质控与安全会议记录员。请根据会议内容生成质控安全会议纪要。",
            """{
  "title": "质控安全会议",
  "points": "## 质量指标回顾\n- 指标1：数据/趋势\n\n## 不良事件分析\n- **事件1**：描述→根因→改进",
  "decisions": "## 整改措施\n- ✅ 措施1（责任科室/人）\n- ✅ 措施2（责任科室/人）",
  "todos": "## 整改跟踪\n- [ ] 负责人：整改内容 (复查时间)",
  "report": "质控安全会议纪要",
  "keywords": "质控指标, 不良事件关键词"
}""",
            "- 用数据说话，回顾质控指标变化趋势\n- 不良事件用根因分析法（RCA）记录\n- 整改措施明确可考核、可追踪\n- report 为可上报医务科的正式纪要"
        )
        "pharmacy" -> Triple(
            "你是三甲医院药事管理与院感控制会议记录员。请生成规范的药事院感会议纪要。",
            """{
  "title": "药事院感会议",
  "points": "## 药事管理\n- 抗菌药物使用率/强度\n- 处方点评结果\n\n## 院感控制\n- 院感率数据\n- 多重耐药菌监测",
  "decisions": "## 管理决议\n- ✅ 决议1\n- ✅ 决议2",
  "todos": "## 执行计划\n- [ ] 负责人：任务",
  "report": "药事院感会议纪要",
  "keywords": "抗菌药物, 院感关键词"
}""",
            "- 药事部分关注抗菌药物、基药、重点监控药品\n- 院感部分关注感染率、多重耐药菌、手卫生等\n- 数据要有对比（同比/环比/达标情况）\n- report 为可上报的正式纪要"
        )
        // ─ 教学类 ─
        "teaching" -> Triple(
            "你是三甲医院教学查房/规培教学记录员。请根据教学内容生成教学查房记录。",
            """{
  "title": "教学查房",
  "points": "## 病例特点\n- 病史、体征、辅助检查\n\n## 教学要点\n- **知识点1**：讲解内容\n- **知识点2**：讲解内容",
  "decisions": "## 诊疗方案\n- ✅ 诊断依据\n- ✅ 治疗方案",
  "todos": "## 学习任务\n- [ ] 规培生/实习生：任务",
  "report": "教学查房记录",
  "keywords": "教学主题, 疾病关键词"
}""",
            "- 突出教学特色：从病例引出知识点\n- 记录各级医师的提问和讲解要点\n- 列出规培生/实习生需要完成的学习任务\n- report 为可归档的教学查房记录"
        )
        "academic" -> Triple(
            "你是三甲医院学术研讨/课题讨论记录员。请根据学术讨论内容生成学术会议纪要。",
            """{
  "title": "学术研讨会",
  "points": "## 报告内容\n- 报告人：主题\n- 核心观点与数据\n\n## 讨论要点\n- **问题1**：讨论\n- **问题2**：讨论",
  "decisions": "## 共识/结论\n- ✅ 结论1\n- ✅ 结论2",
  "todos": "## 后续工作\n- [ ] 负责人：待办事项",
  "report": "学术研讨会纪要",
  "keywords": "研究关键词, 学术主题"
}""",
            "- 准确记录报告的核心观点和数据\n- 记录有深度的学术讨论和争议点\n- 明确达成的共识和未解决的问题\n- report 为可存档的学术活动记录"
        )
        "journal" -> Triple(
            "你是三甲医院文献汇报会记录员。请根据文献分享内容生成文献汇报纪要。",
            """{
  "title": "文献汇报",
  "points": "## 文献信息\n- 标题、期刊、IF、发表时间\n\n## 核心内容\n- 研究设计、主要结果\n\n## 讨论要点\n- 对临床实践的启示",
  "decisions": "## 临床启示\n- ✅ 可借鉴点1\n- ✅ 可借鉴点2",
  "todos": "## 后续计划\n- [ ] 负责人：任务",
  "report": "文献汇报纪要",
  "keywords": "文献关键词, 研究领域"
}""",
            "- 完整记录文献的来源信息（期刊/IF/年份）\n- 提炼对本科室临床实践的具体启示\n- 讨论该研究的方法学优缺点\n- report 为可分享的文献学习记录"
        )
        // ─ 其他 ─
        "nursing" -> Triple(
            "你是三甲医院护理会议记录员。请根据护理会议内容生成护理工作纪要。",
            """{
  "title": "护理会议",
  "points": "## 护理质量\n- 护理质控指标\n\n## 重点患者护理\n- **患者1**（床号/诊断）：护理要点",
  "decisions": "## 护理措施\n- ✅ 措施1\n- ✅ 措施2",
  "todos": "## 任务分配\n- [ ] 责任护士：任务",
  "report": "护理会议纪要",
  "keywords": "护理关键词, 重点关注患者"
}""",
            "- 重点关注危重患者护理方案\n- 记录护理不良事件和改进措施\n- 明确白班/夜班交接要点\n- report 为护理部正式纪要"
        )
        "patient" -> Triple(
            "你是三甲医院医患沟通记录员。请根据医患沟通内容生成规范的沟通记录。",
            """{
  "title": "医患沟通记录",
  "points": "## 沟通对象\n- 患者/家属姓名、关系\n\n## 沟通内容\n- 病情告知\n- 治疗方案说明\n- 风险告知",
  "decisions": "## 医患共识\n- ✅ 达成共识1\n- ✅ 达成共识2",
  "todos": "## 后续安排\n- [ ] 负责人：待办",
  "report": "医患沟通记录",
  "keywords": "沟通主题, 关键决策"
}""",
            "- 完整记录沟通的时间、地点、参与人\n- 重点记录病情告知、治疗方案、风险说明\n- 明确是否达成共识及后续安排\n- report 为具有法律效力的正式沟通记录"
        )
        // ─ 默认 ─
        else -> Triple(
            "你是三甲医院专业会议记录员。请根据对话内容生成规范的会议纪要。",
            """{
  "title": "会议标题",
  "points": "## 讨论要点\n- **议题1**：内容\n- **议题2**：内容",
  "decisions": "## 决议事项\n- ✅ 决议1\n- ✅ 决议2",
  "todos": "## 待办任务\n- [ ] 负责人：任务描述\n- [ ] 负责人：任务描述",
  "report": "完整汇报文档",
  "keywords": "关键词1, 关键词2, 关键词3"
}""",
            "- points/decisions/todos 使用 Markdown 排版\n- todos 中任务标注具体负责人和时限\n- report 为可直接归档或上报的正式文本\n- keywords 提取 3-8 个核心关键词"
        )
    }

    /**
     * 生成会议摘要
     * @param meetingType 三甲医院会议类型: morning/difficult/mdt/preop/death/admin/dept/quality/pharmacy/teaching/academic/journal/nursing/patient/free
     */
    suspend fun summarize(
        context: Context,
        dialogue: String,
        participants: String,
        title: String,
        meetingType: String = "free"
    ): Summary? {
        val p = Prefs(context)
        val apiKey = p.deepseekApiKey.ifEmpty { p.qwenApiKey }
        if (apiKey.isEmpty()) return null

        val endpoint = if (p.deepseekApiKey.isNotEmpty())
            "https://api.deepseek.com/v1/chat/completions"
        else
            "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions"

        val model = if (p.deepseekApiKey.isNotEmpty()) "deepseek-chat" else "qwen-max"

        val (systemPrompt, jsonFormat, extraReq) = templateFor(meetingType)
        val prompt = buildString {
            append("$systemPrompt\n\n")
            append("会议标题：$title\n")
            append("参会人员：$participants\n\n")
            append("会议对话：\n$dialogue\n\n")
            append("请输出 JSON（只输出 JSON，无其他文字）：\n")
            append(jsonFormat)
            append("\n\n要求：\n")
            append(extraReq)
        }

        return withContext(Dispatchers.IO) {
            try {
                val body = JSONObject().apply {
                    put("model", model)
                    put("messages", JSONArray().apply {
                        put(JSONObject().apply {
                            put("role", "user")
                            put("content", prompt)
                        })
                    })
                    put("temperature", 0.3)
                    put("max_tokens", 4096)
                }

                val request = Request.Builder()
                    .url(endpoint)
                    .post(body.toString().toRequestBody("application/json".toMediaType()))
                    .header("Authorization", "Bearer $apiKey")
                    .header("Content-Type", "application/json")
                    .build()

                val response = client.newCall(request).execute()
                val respBody = response.body?.string() ?: return@withContext null
                response.close()

                val json = JSONObject(respBody)
                val content = json.optJSONArray("choices")
                    ?.optJSONObject(0)
                    ?.optJSONObject("message")
                    ?.optString("content", "") ?: return@withContext null

                // 提取 JSON
                val jsonMatch = Regex("""\{[\s\S]*\}""").find(content)?.value ?: return@withContext null
                val obj = JSONObject(jsonMatch)

                // 解析结构化待办
                val todoItems = parseTodoItems(obj.optString("todos", ""))

                Summary(
                    title = obj.optString("title", title),
                    points = obj.optString("points", ""),
                    decisions = obj.optString("decisions", ""),
                    todos = obj.optString("todos", ""),
                    report = obj.optString("report", ""),
                    keywords = obj.optString("keywords", ""),
                    todoItems = todoItems
                )
            } catch (e: Exception) {
                android.util.Log.e("MeetingSummary", "摘要失败", e)
                null
            }
        }
    }

    // ── 待办解析 ──
    /** 从 Markdown todo 文本中提取结构化 TodoItem */
    fun parseTodoItems(todosText: String): List<TodoItem> {
        val items = mutableListOf<TodoItem>()
        // 匹配: - [ ] 张三：完成XX / - [ ] 张三: 完成XX / - [ ] 张三 完成XX
        val todoRe = Regex("""-\s*\[[ x]\]\s*(.+?)[：:]\s*(.+)""")
        for (match in todoRe.findAll(todosText)) {
            val person = match.groupValues[1].trim()
            val task = match.groupValues[2].trim()
            items.add(TodoItem(person = person, task = task))
        }
        // 兜底：简单格式 "- [ ] 完成XX" 没有负责人
        if (items.isEmpty()) {
            val simpleRe = Regex("""-\s*\[[ x]\]\s*(.+)""")
            for (match in simpleRe.findAll(todosText)) {
                items.add(TodoItem(person = "", task = match.groupValues[1].trim()))
            }
        }
        return items
    }

    /** 将结构化待办保存到 Prefs */
    fun saveTodoItems(context: Context, items: List<TodoItem>, meetingId: String) {
        val p = Prefs(context)
        val existing = p.meetingTodosJson.ifEmpty { "[]" }
        val arr = JSONArray(existing)
        for (item in items) {
            arr.put(JSONObject().apply {
                put("meetingId", meetingId)
                put("person", item.person)
                put("task", item.task)
                put("done", false)
            })
        }
        p.meetingTodosJson = arr.toString()
    }
}
