package com.mynote.android.util

import com.mynote.android.data.entity.MedicalRecord
import com.mynote.android.data.entity.Patient
import com.mynote.android.data.entity.VitalSigns

/**
 * AI 临床辅助分析 — 病例聚合与 Prompt 构建
 * 聚合病历 / 体征 / 影像报告文本，去标识化后组装「证据锚定」prompt
 */
object ClinicalCaseBuilder {

    data class CaseData(
        val patient: Patient,
        val records: List<MedicalRecord>,
        val vital: VitalSigns?,
        val imagingReport: String
    )

    /** 组装融合推理 prompt。不含姓名/床号等可识别信息，已去标识化 */
    fun buildPrompt(data: CaseData): String = buildString {
        val p = data.patient
        append("你是一位三甲医院内科主任医师，具有三十年临床经验。请基于以下患者信息进行临床决策分析，输出鉴别诊断与诊疗建议。\n\n")

        append("=== 患者基本情况 ===\n")
        append("年龄：${p.age}岁\n")
        append("性别：${p.gender}\n")
        if (p.department.isNotEmpty()) append("科室：${p.department}\n")
        append("\n")
        append("=== 初步线索（仅供参考，非确诊） ===\n")
        if (p.diagnosis.isNotEmpty()) append("入院诊断（初诊印象，需结合检查验证）：${p.diagnosis}\n")
        if (p.chiefComplaint.isNotEmpty()) append("主诉（患者自述，未经查体证实）：${p.chiefComplaint}\n")
        append("\n")

        append("=== 病史信息 ===\n")
        val recordText = buildRecordText(data.records)
        if (recordText.isBlank()) append("（暂无）\n") else append(recordText)
        append("\n")

        append("=== 化验结果 ===\n")
        val vitalText = buildVitalText(data.vital)
        if (vitalText.isBlank()) append("（暂无）\n") else append(vitalText)
        append("\n")

        append("=== 影像报告 ===\n")
        if (data.imagingReport.isBlank()) append("（暂无）\n") else append(data.imagingReport.trim())
        append("\n\n")

        append("=== 分析要求 ===\n")
        append("1. 输出鉴别诊断（按可能性从高到低），每个诊断必须附「支持证据」数组，证据必须逐字引用上面【化验结果】【影像报告】【病史信息】中实际存在的内容，严禁编造证据；标注为「（暂无）」的部分没有内容，不得从中引用任何证据\n")
        append("2. 每个诊断可附「不支持证据」\n")
        append("3. 给出诊疗建议（检查/用药），每条附依据\n")
        append("4. 单独列出危急值提醒\n")
        append("5. 列出待补充信息\n")
        append("6. 若信息不足无法判断，明确说明，不得强行下结论\n")
        append("7. 严禁用医学知识库、循证检索或医学常识内容冒充输入数据的证据；主诉与入院诊断仅作为线索，不属于病史证据。若【病史信息】【化验结果】【影像报告】均为「（暂无）」或无法从输入中找到证据，则诊断的「支持证据」留空，并在「待补充」中说明证据不足\n")
        append("8. 证据的「类型」必须标注真实来源：现病史/查体/检验/医嘱/影像（不要统一写成「病史」）；主诉、入院诊断与病历正文矛盾时，一律以病历正文（现病史/查体/检验/医嘱）为准，并在「待补充」中说明该矛盾\n\n")

        append("=== 输出格式 ===\n")
        append("严格输出 JSON，不要任何解释，不要 markdown 代码块：\n")
        append("{\n")
        append("  \"鉴别诊断\": [{\"诊断\":\"\", \"可能性\":\"高|中|低\", \"支持证据\":[{\"类型\":\"现病史|查体|检验|医嘱|影像\",\"项目\":\"\",\"值\":\"\",\"参考\":\"\",\"标注\":\"\"}], \"不支持证据\":[{\"类型\":\"\",\"项目\":\"\",\"值\":\"\",\"参考\":\"\",\"标注\":\"\"}]}],\n")
        append("  \"诊疗建议\": [{\"类型\":\"检查|用药|护理|其他\",\"内容\":\"\",\"依据\":\"\"}],\n")
        append("  \"危急值提醒\": [{\"项目\":\"\",\"值\":\"\",\"提示\":\"\"}],\n")
        append("  \"待补充\": [\"\"]\n")
        append("}\n")
    }

    /** 聚合病历正文，取最近最多 3 条完整文书 */
    private fun buildRecordText(records: List<MedicalRecord>): String {
        if (records.isEmpty()) return ""
        val sb = StringBuilder()
        val sorted = records.sortedByDescending { it.createdAt }
        var count = 0
        for (r in sorted) {
            if (count >= 3) break
            val content = r.content.trim()
            if (content.isBlank()) continue
            val label = when {
                r.type.contains("入院") -> "入院记录"
                r.type.contains("首次") -> "首次病程"
                r.type.contains("出院") -> "出院小结"
                else -> r.type
            }
            sb.append("【").append(label).append("】\n")
            sb.append(truncate(content, 800))
            sb.append("\n\n")
            count++
        }
        return sb.toString()
    }

    /** 将结构化体征转为带参考范围的文本 */
    private fun buildVitalText(v: VitalSigns?): String {
        if (v == null) return ""
        val sb = StringBuilder()
        fun line(name: String, value: String?, ref: String) {
            if (!value.isNullOrBlank()) sb.append("$name: $value (参考 $ref)\n")
        }
        line("体温", v.temperature?.toString(), "36.0-37.3℃")
        line("脉搏", v.pulse?.toString(), "60-100次/分")
        line("呼吸", v.respiration?.toString(), "12-20次/分")
        if (v.bpSystolic != null && v.bpDiastolic != null) {
            line("血压", "${v.bpSystolic}/${v.bpDiastolic}", "90-140/60-90mmHg")
        }
        line("血氧饱和度", v.spo2?.toString(), "95-100%")
        line("糖化血红蛋白", v.hba1c?.toString(), "4.0-6.0%")
        line("空腹血糖", v.fastingGlu?.toString(), "3.9-6.1mmol/L")
        line("肌酐", v.creatinine?.toString(), "44-133μmol/L")
        line("eGFR", v.egfr?.toString(), "≥90mL/min/1.73m²")
        line("血钾", v.potassium?.toString(), "3.5-5.3mmol/L")
        line("血钠", v.sodium?.toString(), "137-147mmol/L")
        line("血红蛋白", v.hemoglobin?.toString(), "130-175g/L")
        line("白蛋白", v.albumin?.toString(), "40-55g/L")
        line("ALT", v.alt?.toString(), "9-50U/L")
        line("AST", v.ast?.toString(), "15-40U/L")
        line("LDL", v.ldl?.toString(), "0-3.4mmol/L")
        return sb.toString()
    }

    private fun truncate(s: String, max: Int): String =
        if (s.length <= max) s else s.substring(0, max) + "…"

    /** 本地危急值规则兜底（不依赖模型，防止漏报；成人参考阈值） */
    fun detectCriticalValues(v: VitalSigns?): List<ClinicalAnalysisResult.CriticalAlert> {
        if (v == null) return emptyList()
        val list = mutableListOf<ClinicalAnalysisResult.CriticalAlert>()
        fun add(item: String, value: String, hint: String) {
            list.add(ClinicalAnalysisResult.CriticalAlert(item, value, hint))
        }
        v.potassium?.let {
            if (it < 2.5) add("血钾 K⁺", "$it mmol/L", "低钾危急值，有心律失常风险")
            else if (it > 6.0) add("血钾 K⁺", "$it mmol/L", "高钾危急值，有心脏骤停风险")
        }
        v.sodium?.let {
            if (it < 120) add("血钠 Na⁺", "$it mmol/L", "重度低钠血症")
            else if (it > 160) add("血钠 Na⁺", "$it mmol/L", "重度高钠血症")
        }
        v.fastingGlu?.let {
            if (it < 2.8) add("空腹血糖", "$it mmol/L", "低血糖危急值")
            else if (it > 33.3) add("空腹血糖", "$it mmol/L", "严重高血糖")
        }
        v.hemoglobin?.let {
            if (it < 60) add("血红蛋白 Hb", "$it g/L", "重度贫血，需紧急评估")
        }
        v.spo2?.let {
            if (it < 90) add("血氧饱和度", "$it%", "低氧血症")
        }
        v.temperature?.let {
            if (it > 41) add("体温", "$it℃", "超高热")
            else if (it < 32) add("体温", "$it℃", "低体温危急")
        }
        v.egfr?.let {
            if (it < 15) add("eGFR", "$it mL/min/1.73m²", "终末期肾病/肾衰竭")
        }
        return list
    }
}
