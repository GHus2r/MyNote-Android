package com.mynote.android.util

/**
 * 病历书写必备要素规则表（数据驱动，单一数据源）
 * 依据《病历书写基本规范》（国家卫健委 2022 版）
 *
 * 设计要点：
 * - 本地规则初筛（RecordAuditRules）与 AI 审计 prompt（DeepSeekClient.buildAuditPrompt）均引用此表，
 *   避免两套规则漂移。
 * - 每条必备项都带 basis（判定依据/规范条款），保证结果可追溯。
 */
object RecordSpec {

    enum class Severity(val label: String) {
        REQUIRED("必填"),      // 缺失即红标、扣分
        RECOMMENDED("建议")    // 建议补充、黄标
    }

    data class RequiredItem(
        val key: String,            // 稳定标识，如 chief_complaint
        val label: String,          // 中文名，如 主诉
        val severity: Severity,     // 必填/建议
        val basis: String,          // 判定依据（规范条款）
        val hintRegex: String? = null // 本地初筛正则；null 表示仅靠 AI 判断
    )

    data class RecordTypeSpec(
        val type: String,           // 标准名
        val aliases: List<String>,  // 别名，用于匹配
        val items: List<RequiredItem>
    )

    val all: List<RecordTypeSpec> = listOf(
        // —— 入院记录：11 段必备 ——
        RecordTypeSpec("入院记录", listOf("入院记录", "住院志", "入院病历"), listOf(
            RequiredItem("general_info", "一般项目（姓名/性别/年龄/民族/婚姻/职业/入院时间/病史陈述者）", Severity.REQUIRED, "《病历书写基本规范》2022 入院记录-一般项目"),
            RequiredItem("chief_complaint", "主诉（症状+部位+持续时间，≤20字，不用诊断名词）", Severity.REQUIRED, "《病历书写基本规范》2022 入院记录-主诉", """主诉[：:]"""),
            RequiredItem("present_illness", "现病史（起病诱因/缓急、主要症状特点、伴随症状、诊疗经过、一般情况）", Severity.REQUIRED, "《病历书写基本规范》2022 入院记录-现病史", """现病史|现病[：:]"""),
            RequiredItem("past_history", "既往史（健康状况/传染病/手术外伤/输血/过敏/慢性病）", Severity.REQUIRED, "《病历书写基本规范》2022 入院记录-既往史", """既往史|既往[：:]"""),
            RequiredItem("personal_history", "个人史（出生地/居住地/烟酒嗜好/职业暴露）", Severity.REQUIRED, "《病历书写基本规范》2022 入院记录-个人史", """个人史"""),
            RequiredItem("marital_history", "婚育史（婚姻/生育）", Severity.REQUIRED, "《病历书写基本规范》2022 入院记录-婚育史", """婚育史|婚姻|生育"""),
            RequiredItem("family_history", "家族史（父母/兄弟姐妹/子女健康、遗传病）", Severity.REQUIRED, "《病历书写基本规范》2022 入院记录-家族史", """家族史|家族"""),
            RequiredItem("physical_exam", "体格检查（T/P/R/BP 及各系统查体，逐项□勾选）", Severity.REQUIRED, "《病历书写基本规范》2022 入院记录-体格检查", """查体|体格检查|PE[：:]|T\s*\d|P\s*\d|BP\s*\d"""),
            RequiredItem("aux_exam", "辅助检查（院前+入院后，注明日期与机构）", Severity.REQUIRED, "《病历书写基本规范》2022 入院记录-辅助检查", """辅查|辅助检查|化验|影像|B超|CT[：:]|MRI[：:]|心电图"""),
            RequiredItem("initial_diagnosis", "初步诊断（主次分明、编号排列、诊断全称）", Severity.REQUIRED, "《病历书写基本规范》2022 入院记录-初步诊断", """诊断|考虑|可能为|印象[：:]|结论"""),
            RequiredItem("treatment_plan", "诊疗计划（检查项目/治疗用药/护理级别，逐条）", Severity.REQUIRED, "《病历书写基本规范》2022 入院记录-诊疗计划", """治疗|方案|用药|建议|处理|嘱托|随访"""),
            RequiredItem("physician_sign", "医师签名", Severity.REQUIRED, "《病历书写基本规范》2022 基本要求-签名", """签名|医师|经治医师|主治医师"""),
            RequiredItem("record_time", "书写/记录时间（精确到分钟）", Severity.REQUIRED, "《病历书写基本规范》2022 基本要求-时限", """\d{4}[-/.]\d{1,2}[-/.]\d{1,2}""")
        )),

        // —— 首次病程记录：三部分 ——
        RecordTypeSpec("首次病程记录", listOf("首次病程", "首次病程记录", "首程"), listOf(
            RequiredItem("case_features", "病例特点（主诉现病史关键点/阳性体征/异常结果归纳）", Severity.REQUIRED, "《病历书写基本规范》2022 首次病程-病例特点", """病例特点|病例特征"""),
            RequiredItem("diagnosis_discussion", "拟诊讨论（诊断依据逐条+鉴别诊断分析）", Severity.REQUIRED, "《病历书写基本规范》2022 首次病程-拟诊讨论", """拟诊|鉴别|诊断依据|考虑"""),
            RequiredItem("treatment_plan", "诊疗计划（检查/用药/进一步安排）", Severity.REQUIRED, "《病历书写基本规范》2022 首次病程-诊疗计划", """诊疗计划|治疗|用药|检查"""),
            RequiredItem("physician_sign", "医师签名", Severity.REQUIRED, "《病历书写基本规范》2022 基本要求-签名", """签名|医师"""),
            RequiredItem("record_time", "记录时间（精确到分钟）", Severity.REQUIRED, "《病历书写基本规范》2022 基本要求-时限", """\d{4}[-/.]\d{1,2}[-/.]\d{1,2}""")
        )),

        // —— 日常病程记录：SOAP ——
        RecordTypeSpec("日常病程记录", listOf("日常病程", "病程记录", "日常病程记录"), listOf(
            RequiredItem("subjective", "主观（患者自述症状变化）", Severity.REQUIRED, "《病历书写基本规范》2022 日常病程-SOAP-S", """主观|自述|诉"""),
            RequiredItem("objective", "客观（今日查体要点/新检查结果）", Severity.REQUIRED, "《病历书写基本规范》2022 日常病程-SOAP-O", """客观|查体|检查"""),
            RequiredItem("assessment", "评估（病情分析/诊断调整）", Severity.REQUIRED, "《病历书写基本规范》2022 日常病程-SOAP-A", """评估|分析|考虑"""),
            RequiredItem("plan", "计划（下一步诊疗/医嘱调整）", Severity.REQUIRED, "《病历书写基本规范》2022 日常病程-SOAP-P", """计划|诊疗|医嘱|下一步"""),
            RequiredItem("physician_sign", "医师签名", Severity.REQUIRED, "《病历书写基本规范》2022 基本要求-签名", """签名|医师"""),
            RequiredItem("record_time", "记录时间（精确到分钟）", Severity.REQUIRED, "《病历书写基本规范》2022 基本要求-时限", """\d{4}[-/.]\d{1,2}[-/.]\d{1,2}""")
        )),

        // —— 出院小结 ——
        RecordTypeSpec("出院小结", listOf("出院小结", "出院记录"), listOf(
            RequiredItem("admit_discharge_date", "入院日期与出院日期", Severity.REQUIRED, "《病历书写基本规范》2022 出院小结-入院/出院日期", """入院日期|出院日期|\d{4}[-/.]\d{1,2}[-/.]\d{1,2}"""),
            RequiredItem("admit_diagnosis", "入院诊断", Severity.REQUIRED, "《病历书写基本规范》2022 出院小结-入院诊断", """入院诊断"""),
            RequiredItem("discharge_diagnosis", "出院诊断", Severity.REQUIRED, "《病历书写基本规范》2022 出院小结-出院诊断", """出院诊断"""),
            RequiredItem("admit_situation", "入院情况（主要症状/体征/辅助检查）", Severity.REQUIRED, "《病历书写基本规范》2022 出院小结-入院情况", """入院情况"""),
            RequiredItem("treatment_course", "诊疗经过", Severity.REQUIRED, "《病历书写基本规范》2022 出院小结-诊疗经过", """诊疗经过|诊治经过"""),
            RequiredItem("discharge_situation", "出院情况", Severity.REQUIRED, "《病历书写基本规范》2022 出院小结-出院情况", """出院情况"""),
            RequiredItem("discharge_advice", "出院医嘱（用药/复查/随访）", Severity.REQUIRED, "《病历书写基本规范》2022 出院小结-出院医嘱", """出院医嘱|嘱|随访|复查"""),
            RequiredItem("physician_sign", "医师签名", Severity.REQUIRED, "《病历书写基本规范》2022 基本要求-签名", """签名|医师""")
        )),

        // —— 上级医师查房记录 ——
        RecordTypeSpec("上级医师查房记录", listOf("上级查房", "主治医师查房", "主任查房", "查房记录", "教学查房"), listOf(
            RequiredItem("round_time", "查房时间（精确到分钟）", Severity.REQUIRED, "《病历书写基本规范》2022 查房记录-时间", """\d{4}[-/.]\d{1,2}[-/.]\d{1,2}|\d{1,2}[:：]\d{2}"""),
            RequiredItem("round_opinion", "查房意见（病情分析+诊疗指示）", Severity.REQUIRED, "《病历书写基本规范》2022 查房记录-查房意见", """查房|意见|指示|分析"""),
            RequiredItem("physician_sign", "医师签名", Severity.REQUIRED, "《病历书写基本规范》2022 基本要求-签名", """签名|医师""")
        ))
    )

    /** 兜底：无法匹配具体文书类型时，通用必备要素 */
    val fallback: RecordTypeSpec = RecordTypeSpec("通用病历", listOf("病历"), listOf(
        RequiredItem("chief_complaint", "主诉", Severity.REQUIRED, "《病历书写基本规范》2022 基本要求", """主诉[：:]"""),
        RequiredItem("present_illness", "现病史", Severity.REQUIRED, "《病历书写基本规范》2022 基本要求", """现病史|现病[：:]"""),
        RequiredItem("diagnosis", "诊断", Severity.REQUIRED, "《病历书写基本规范》2022 基本要求", """诊断|考虑|印象|结论"""),
        RequiredItem("treatment_plan", "诊疗计划", Severity.REQUIRED, "《病历书写基本规范》2022 基本要求", """治疗|方案|用药|建议|处理"""),
        RequiredItem("physician_sign", "医师签名", Severity.REQUIRED, "《病历书写基本规范》2022 基本要求-签名", """签名|医师"""),
        RequiredItem("record_time", "记录时间", Severity.REQUIRED, "《病历书写基本规范》2022 基本要求-时限", """\d{4}[-/.]\d{1,2}[-/.]\d{1,2}""")
    ))

    /** 根据文书类型名匹配规则，找不到返回兜底 */
    fun forType(type: String): RecordTypeSpec {
        val t = type.trim()
        for (spec in all) {
            if (spec.type == t || spec.aliases.any { it == t || t.contains(it) }) return spec
        }
        return fallback
    }

    /** 生成供 AI 审计 prompt 引用的必备要素清单文本 */
    fun toPromptBlock(type: String): String {
        val spec = forType(type)
        val sb = StringBuilder()
        sb.append("【${spec.type}】必备要素清单（缺失即判为漏写项）：\n")
        spec.items.forEachIndexed { i, item ->
            val sev = if (item.severity == Severity.REQUIRED) "必填" else "建议"
            sb.append("  ${i + 1}. ${item.label}（$sev；依据：${item.basis}）\n")
        }
        return sb.toString()
    }
}
