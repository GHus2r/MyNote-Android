package com.mynote.android.util

import android.content.Context
import android.graphics.Bitmap
import com.mynote.android.data.AppDatabase
import com.mynote.android.data.entity.RecordAuditReport
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.security.MessageDigest

/**
 * 病历照片质控分析编排器
 * 流程：OCR 识图 → 本地规则初筛 → AI 深度审计 → 合并结果 → 落库（可追溯）
 *
 * 可追溯：sourceImageHash / ocrRawText / ocrEngine / model / promptVersion 均写入报告。
 * 非破坏式：纯新增能力，不修改现有 OCR/AI/评分模块。
 */
object MedicalRecordAuditor {

    /** 供 UI 展示的结构化结果 */
    data class AuditResult(
        val ocrRawText: String,          // OCR 原始全文（可追溯）
        val summary: String,
        val scoreTotal: Int,
        val scoreCompleteness: Int,
        val scoreNorms: Int,
        val scoreAccuracy: Int,
        val missingItems: List<IssueItem>,    // 漏写项
        val normIssues: List<IssueItem>,      // 规范问题
        val accuracyIssues: List<IssueItem>,  // 准确性问题
        val improvementPlan: List<PlanItem>   // 改进方案
    )

    data class IssueItem(
        val title: String,       // 条目名（item/point/aspect）
        val detail: String,      // 描述（expectation/issue）
        val severity: String,    // 必填/建议（仅漏写项用）
        val basis: String,       // 判定依据
        val evidence: String,    // 原文片段（仅准确性问题用）
        val suggestion: String,  // 改进建议（fix/suggestion）
        val refGuideline: String // 指南出处（仅准确性问题用）
    )

    data class PlanItem(
        val step: String,
        val action: String,
        val target: String,
        val rationale: String
    )

    /**
     * 执行病历质控分析
     * @param bitmaps 病历照片（支持多张，多页病历逐页 OCR 后按页序拼接）
     * @param dept 科室
     * @param recordType 文书类型
     * @param patientId 可选关联患者（0=不关联）
     */
    suspend fun audit(
        context: Context,
        bitmaps: List<Bitmap>,
        dept: String,
        recordType: String,
        patientId: Long = 0
    ): Result<AuditResult> {
        if (bitmaps.isEmpty()) return Result.failure(Exception("未提供病历照片"))

        // 逐页 OCR 识图并拼接（多页病历：每页标注页码，便于追溯）
        val pageTexts = ArrayList<String>()
        bitmaps.forEachIndexed { i, bmp ->
            val t = QwenOcrClient.extractMedicalText(context, bmp)
            if (!t.isNullOrBlank()) {
                pageTexts.add(if (bitmaps.size > 1) "【第 ${i + 1} 页】\n$t" else t)
            }
        }
        if (pageTexts.isEmpty()) {
            return Result.failure(Exception("OCR 识别失败，请确认已配置 API Key 且网络正常"))
        }
        val ocrText = pageTexts.joinToString("\n\n")
        return analyzeText(context, ocrText, dept, recordType, patientId,
            imageHash = bitmaps.joinToString("-") { sha256(it) }, ocrEngine = "qwen-vl-max")
    }

    /**
     * 直接粘贴/输入病历文字分析（跳过 OCR，供「粘贴文字」入口复用）
     */
    suspend fun auditText(
        context: Context,
        text: String,
        dept: String,
        recordType: String,
        patientId: Long = 0
    ): Result<AuditResult> {
        val t = text.trim()
        if (t.isEmpty()) return Result.failure(Exception("请粘贴病历文字"))
        return analyzeText(context, t, dept, recordType, patientId,
            imageHash = "", ocrEngine = "手动粘贴")
    }

    /** 公共：规则初筛 + AI 审计 + 落库（照片/文字两条入口共用） */
    private suspend fun analyzeText(
        context: Context,
        ocrText: String,
        dept: String,
        recordType: String,
        patientId: Long,
        imageHash: String,
        ocrEngine: String
    ): Result<AuditResult> {
        // 1. 本地规则初筛（离线、立即、兜底）
        val pre = RecordAuditRules.preCheck(recordType, ocrText)

        // 2. AI 深度审计
        val prompt = DeepSeekClient.buildAuditPrompt(dept, recordType, ocrText)
        val ai = DeepSeekClient.generateWithPrompt(context, prompt)

        // 3. 合并结果（AI 失败则回退本地初筛）
        val result = ai.fold(
            onSuccess = { json -> parseAiResult(json, pre, ocrText) },
            onFailure = { e -> fallbackResult(pre, ocrText, e) }
        )

        // 4. 落库（可追溯）
        val report = RecordAuditReport(
            patientId = patientId,
            dept = dept,
            recordType = recordType,
            sourceImageHash = imageHash,
            ocrRawText = ocrText,
            ocrEngine = ocrEngine,
            model = "qwen-max",
            promptVersion = DeepSeekClient.AUDIT_PROMPT_VERSION,
            scoreTotal = result.scoreTotal,
            scoreCompleteness = result.scoreCompleteness,
            scoreNorms = result.scoreNorms,
            scoreAccuracy = result.scoreAccuracy,
            summary = result.summary,
            reportJson = toReportJson(result),
            createdAt = System.currentTimeMillis()
        )
        AppDatabase.get(context).recordAuditDao().insert(report)

        return Result.success(result)
    }

    // ---------- 解析与兜底 ----------

    private fun parseAiResult(jsonText: String, pre: RecordAuditRules.PreCheck, ocrText: String): AuditResult {
        val clean = jsonText
            .replace("```json", "")
            .replace("```", "")
            .trim()
        return try {
            val o = JSONObject(clean)
            val summary = o.optString("summary", "")
            val st = o.optInt("scoreTotal", -1)
            val sc = o.optInt("scoreCompleteness", -1)
            val sn = o.optInt("scoreNorms", -1)
            val sa = o.optInt("scoreAccuracy", -1)

            val missing = parseIssues(o.optJSONArray("missingItems")) { it ->
                IssueItem(
                    title = it.optString("item"),
                    detail = "",
                    severity = it.optString("severity", "必填"),
                    basis = it.optString("basis"),
                    evidence = "",
                    suggestion = it.optString("suggestion"),
                    refGuideline = ""
                )
            }
            val norms = parseIssues(o.optJSONArray("normIssues")) { it ->
                IssueItem(it.optString("point"), it.optString("expectation"), "", it.optString("basis"), "", it.optString("fix"), "")
            }
            val accuracy = parseIssues(o.optJSONArray("accuracyIssues")) { it ->
                IssueItem(it.optString("aspect"), it.optString("issue"), "", it.optString("basis"), it.optString("evidence"), it.optString("suggestion"), it.optString("refGuideline"))
            }
            val plan = parsePlan(o.optJSONArray("improvementPlan"))

            AuditResult(
                ocrRawText = ocrText,
                summary = summary,
                scoreTotal = if (st >= 0) st else (pre.completeness + pre.norms).coerceIn(0, 100),
                scoreCompleteness = if (sc >= 0) sc else pre.completeness,
                scoreNorms = if (sn >= 0) sn else pre.norms,
                scoreAccuracy = if (sa >= 0) sa else 0,
                missingItems = missing.ifEmpty { pre.missingItems.map { IssueItem(it.label, "", it.severity, it.basis, "", "", "") } },
                normIssues = norms.ifEmpty { pre.normIssues.map { IssueItem(it.point, it.expectation, "", it.basis, "", "", "") } },
                accuracyIssues = accuracy,
                improvementPlan = plan
            )
        } catch (e: Exception) {
            fallbackResult(pre, ocrText, e)
        }
    }

    private fun fallbackResult(pre: RecordAuditRules.PreCheck, ocrText: String, e: Throwable?): AuditResult {
        val total = (pre.completeness + pre.norms).coerceIn(0, 100)
        return AuditResult(
            ocrRawText = ocrText,
            summary = "AI 深度分析不可用（${e?.message ?: "未知错误"}），以下为本地规则初筛结果，仅供参考。",
            scoreTotal = total,
            scoreCompleteness = pre.completeness,
            scoreNorms = pre.norms,
            scoreAccuracy = 0,
            missingItems = pre.missingItems.map { IssueItem(it.label, "", it.severity, it.basis, "", "", "") },
            normIssues = pre.normIssues.map { IssueItem(it.point, it.expectation, "", it.basis, "", "", "") },
            accuracyIssues = emptyList(),
            improvementPlan = emptyList()
        )
    }

    private fun parseIssues(arr: JSONArray?, mapper: (JSONObject) -> IssueItem): List<IssueItem> {
        if (arr == null) return emptyList()
        val out = ArrayList<IssueItem>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            out.add(mapper(o))
        }
        return out
    }

    private fun parsePlan(arr: JSONArray?): List<PlanItem> {
        if (arr == null) return emptyList()
        val out = ArrayList<PlanItem>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            out.add(
                PlanItem(
                    step = o.optString("step"),
                    action = o.optString("action"),
                    target = o.optString("target"),
                    rationale = o.optString("rationale")
                )
            )
        }
        return out
    }

    private fun toReportJson(r: AuditResult): String {
        return try {
            val o = JSONObject()
            o.put("summary", r.summary)
            o.put("scoreTotal", r.scoreTotal)
            o.put("scoreCompleteness", r.scoreCompleteness)
            o.put("scoreNorms", r.scoreNorms)
            o.put("scoreAccuracy", r.scoreAccuracy)
            o.put("missingItems", JSONArray(r.missingItems.map { JSONObject().put("item", it.title).put("severity", it.severity).put("basis", it.basis).put("suggestion", it.suggestion) }))
            o.put("normIssues", JSONArray(r.normIssues.map { JSONObject().put("point", it.title).put("expectation", it.detail).put("basis", it.basis).put("fix", it.suggestion) }))
            o.put("accuracyIssues", JSONArray(r.accuracyIssues.map { JSONObject().put("aspect", it.title).put("issue", it.detail).put("evidence", it.evidence).put("suggestion", it.suggestion).put("refGuideline", it.refGuideline) }))
            o.put("improvementPlan", JSONArray(r.improvementPlan.map { JSONObject().put("step", it.step).put("action", it.action).put("target", it.target).put("rationale", it.rationale) }))
            o.toString()
        } catch (e: Exception) {
            "{}"
        }
    }

    private fun sha256(bitmap: Bitmap): String {
        return try {
            val bos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, bos)
            val md = MessageDigest.getInstance("SHA-256")
            md.digest(bos.toByteArray()).joinToString("") { "%02x".format(it.toInt() and 0xff) }
        } catch (e: Exception) {
            ""
        }
    }
}
