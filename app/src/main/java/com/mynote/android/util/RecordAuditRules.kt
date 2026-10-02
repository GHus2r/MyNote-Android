package com.mynote.android.util

/**
 * 病历质控本地规则初筛（离线、零成本、可兜底）
 * 基于 RecordSpec 数据，用正则快检必备要素是否缺失。
 * 输出结构化结果，供 UI 立即反馈 + AI 深度分析补充。
 *
 * 注意：本类为纯新增能力，不修改现有 RecordScorer（两者独立，互不影响）。
 */
object RecordAuditRules {

    data class MissingItem(
        val key: String,
        val label: String,
        val severity: String,   // 必填 / 建议
        val basis: String
    )

    data class NormIssue(
        val point: String,       // 问题点
        val expectation: String, // 规范要求
        val basis: String        // 依据
    )

    data class PreCheck(
        val type: String,
        val missingItems: List<MissingItem>,
        val normIssues: List<NormIssue>,
        val completeness: Int,   // 0-40 完整性初分
        val norms: Int           // 0-30 规范性初分
    )

    fun preCheck(type: String, content: String): PreCheck {
        val spec = RecordSpec.forType(type)
        val missing = mutableListOf<MissingItem>()

        spec.items.forEach { item ->
            val hit = item.hintRegex?.let { Regex(it).containsMatchIn(content) } ?: false
            if (!hit) {
                missing.add(MissingItem(item.key, item.label, item.severity.label, item.basis))
            }
        }

        // 完整性：必填项命中比例 * 40
        val requiredCount = spec.items.count { it.severity == RecordSpec.Severity.REQUIRED }
        val requiredPresent = spec.items.count {
            it.severity == RecordSpec.Severity.REQUIRED &&
                (it.hintRegex?.let { r -> Regex(r).containsMatchIn(content) } ?: false)
        }
        val completeness = if (requiredCount > 0) {
            (requiredPresent * 40 / requiredCount).coerceIn(0, 40)
        } else 40

        // 规范性快检
        val normIssues = mutableListOf<NormIssue>()
        var norms = 30
        if (!Regex("""(一[、.]|二[、.]|三[、.]|1\.|2\.|3\.|①|②|③)""").containsMatchIn(content)) {
            normIssues.add(NormIssue("格式分段", "应使用分段或编号组织内容", "《病历书写基本规范》2022 格式要求"))
            norms -= 8
        }
        if (!content.contains("\n") || content.lines().size < 5) {
            normIssues.add(NormIssue("篇幅/分段", "内容过于简短或缺少换行分段", "《病历书写基本规范》2022 完整性要求"))
            norms -= 7
        }
        if (content.contains("【") || content.contains("...") || content.contains("___") || content.contains("待补充")) {
            normIssues.add(NormIssue("待补充项", "存在未填写的待补充占位", "《病历书写基本规范》2022 完整性要求"))
            norms -= 5
        }
        if (!Regex("""\d{4}[-/.]\d{1,2}[-/.]\d{1,2}""").containsMatchIn(content)) {
            normIssues.add(NormIssue("日期缺失", "缺少规范日期（yyyy-MM-dd）", "《病历书写基本规范》2022 时限要求"))
            norms -= 5
        }
        norms = norms.coerceIn(0, 30)

        return PreCheck(spec.type, missing, normIssues, completeness, norms)
    }
}
