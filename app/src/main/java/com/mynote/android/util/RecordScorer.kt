package com.mynote.android.util

/**
 * 病历质量评分引擎
 * 规则：完整性 + 规范性 + 字数，总分 100
 */
object RecordScorer {

    data class Score(
        val total: Int,       // 0-100
        val completeness: Int, // 完整性 0-40
        val norms: Int,        // 规范性 0-30
        val richness: Int,     // 内容丰富度 0-20
        val bonus: Int,        // 加分项 0-10
        val details: String    // 扣分/得分说明
    )

    fun score(type: String, content: String): Score {
        var comp = 0
        var norm = 0
        var rich = 0
        var bonus = 0
        val reasons = mutableListOf<String>()

        // === 完整性 (40分) ===
        val hasComplaint = Regex("""主诉[：:]""").containsMatchIn(content)
        val hasExam = Regex("""(查体|体格检查|PE[：:]|T\s*\d|P\s*\d|BP\s*\d)""").containsMatchIn(content)
        val hasAux = Regex("""(辅查|辅助检查|化验|影像|B超|CT[：:]|MRI[：:]|心电图)""").containsMatchIn(content)
        val hasDiag = Regex("""(诊断|考虑|可能为|印象[：:]|结论)""").containsMatchIn(content)
        val hasPlan = Regex("""(治疗|方案|用药|建议|处理|嘱托|随访)""").containsMatchIn(content)
        val hasDate = Regex("""\d{4}[-/.]\d{1,2}[-/.]\d{1,2}""").containsMatchIn(content)
        val hasPending = content.contains("【") || content.contains("...") || content.contains("___")

        if (hasComplaint) comp += 8 else reasons.add("缺主诉(-8)")
        if (hasExam) comp += 10 else reasons.add("缺查体(-10)")
        if (hasAux) comp += 8 else reasons.add("缺辅查(-8)")
        if (hasDiag) comp += 8 else reasons.add("缺诊断(-8)")
        if (hasPlan) comp += 6 else reasons.add("缺治疗计划(-6)")
        if (hasDate) comp += 0 // 不单独计分但提一下
        if (hasPending) { comp -= 5; reasons.add("有待补充项(-5)") }
        if (comp < 0) comp = 0

        // === 规范性 (30分) ===
        val standardTypes = listOf("入院记录", "首次病程", "主治医师查房", "主任查房", "日常病程",
            "术后首次", "术前小结", "术前讨论", "手术记录", "出院小结", "死亡记录", "会诊",
            "交班", "接班", "转出", "转入", "抢救记录", "阶段小结", "病危通知", "操作记录")
        if (standardTypes.any { type.contains(it) }) norm += 10 else if (type.isNotBlank()) norm += 5
        reasons.add("文书类型: $type (+${if (standardTypes.any { type.contains(it) }) 10 else 5})")

        // 格式检查：有分段/编号
        if (Regex("""(一[、.]|二[、.]|三[、.]|1\.|2\.|3\.|①|②|③)""").containsMatchIn(content)) norm += 8
        if (content.contains("\n") && content.lines().size >= 5) norm += 7 else norm += 3
        // 有□勾选或标准查体格式 → 加分
        if (content.contains("□") || content.contains("【 】")) norm += 5
        if (norm > 30) norm = 30

        // === 内容丰富度 (20分) ===
        val len = content.length
        rich = when { len > 2000 -> 20; len > 1000 -> 15; len > 400 -> 10; len > 100 -> 5; else -> 2 }
        reasons.add("字数: ${len}字 (+$rich)")

        // === 加分项 (10分) ===
        if (content.contains("指南") || content.contains("循证")) { bonus += 3; reasons.add("循证加分(+3)") }
        if (content.contains("鉴别") || content.contains("鉴别诊断")) { bonus += 2; reasons.add("鉴别诊断(+2)") }
        if (content.contains("评分") && Regex("""\d+分""").containsMatchIn(content)) { bonus += 2; reasons.add("评分体系(+2)") }
        if (content.contains("告知") || content.contains("知情")) { bonus += 1; reasons.add("知情告知(+1)") }
        if (content.contains("随访") || content.contains("复查")) { bonus += 2; reasons.add("随访计划(+2)") }
        if (bonus > 10) bonus = 10

        val total = (comp + norm + rich + bonus).coerceIn(0, 100)
        return Score(total, comp, norm, rich, bonus, reasons.joinToString("\n"))
    }

    /** 星级显示 */
    fun starLabel(score: Int): String = when {
        score >= 90 -> "★★★★★"
        score >= 75 -> "★★★★☆"
        score >= 60 -> "★★★☆☆"
        score >= 40 -> "★★☆☆☆"
        else -> "★☆☆☆☆"
    }

    /** 颜色 */
    fun colorHex(score: Int): String = when {
        score >= 90 -> "#2E7D32"  // 深绿
        score >= 75 -> "#558B2F"  // 浅绿
        score >= 60 -> "#F57F17"  // 黄
        score >= 40 -> "#E65100"  // 橙
        else -> "#C62828"         // 红
    }
}
