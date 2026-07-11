package com.mynote.android.util

import com.mynote.android.data.entity.VitalSigns

/** 从病历文本中提取结构化体征数据 */
object VitalSignsParser {

    fun parse(patientId: Long, recordId: Long, text: String): VitalSigns? {
        var hasAny = false
        val t = parseTemp(text); if (t != null) hasAny = true
        val p = parsePulse(text); if (p != null) hasAny = true
        val r = parseResp(text); if (r != null) hasAny = true
        val bp = parseBP(text); if (bp != null) hasAny = true
        val s = parseSpO2(text); if (s != null) hasAny = true
        val w = parseWeight(text); if (w != null) hasAny = true
        val hba1c = parseHbA1c(text); if (hba1c != null) hasAny = true
        val glu = parseFastingGlu(text); if (glu != null) hasAny = true
        val cr = parseCreatinine(text); val crUnit = parseCrUnit(text); if (cr != null) hasAny = true
        val egfr = parseEgfr(text); if (egfr != null) hasAny = true
        val k = parseK(text); if (k != null) hasAny = true
        val na = parseNa(text); if (na != null) hasAny = true
        val hb = parseHb(text); if (hb != null) hasAny = true
        val alb = parseAlbumin(text); if (alb != null) hasAny = true
        val alt = parseAlt(text); if (alt != null) hasAny = true
        val ast = parseAst(text); if (ast != null) hasAny = true
        val ldl = parseLDL(text); if (ldl != null) hasAny = true

        if (!hasAny) return null
        return VitalSigns(
            patientId = patientId, recordId = recordId,
            temperature = t, pulse = p, respiration = r,
            bpSystolic = bp?.first, bpDiastolic = bp?.second,
            spo2 = s, weightKg = w,
            hba1c = hba1c, fastingGlu = glu,
            creatinine = cr, creatinineUnit = crUnit, egfr = egfr,
            potassium = k, sodium = na, hemoglobin = hb, albumin = alb,
            alt = alt, ast = ast, ldl = ldl
        )
    }

    private fun parseTemp(text: String) = Regex("T\\s*([\\d.]+)\\s*℃").find(text)?.groupValues?.get(1)?.toDoubleOrNull()
    private fun parsePulse(text: String) = Regex("P\\s*(\\d+)\\s*次/分").find(text)?.groupValues?.get(1)?.toIntOrNull()
    private fun parseResp(text: String) = Regex("R\\s*(\\d+)\\s*次/分").find(text)?.groupValues?.get(1)?.toIntOrNull()
    private fun parseBP(text: String): Pair<Int, Int>? {
        val m = Regex("BP\\s*(\\d+)\\s*/\\s*(\\d+)\\s*mmHg").find(text) ?: return null
        return m.groupValues[1].toIntOrNull()?.let { sys ->
            m.groupValues[2].toIntOrNull()?.let { dia -> sys to dia }
        }
    }
    private fun parseSpO2(text: String) = Regex("SpO[₂2]\\s*(\\d+)\\s*%").find(text)?.groupValues?.get(1)?.toIntOrNull()
    private fun parseWeight(text: String) = Regex("体重\\s*([\\d.]+)\\s*kg").find(text)?.groupValues?.get(1)?.toDoubleOrNull()
    private fun parseHbA1c(text: String) = Regex("HbA1c\\s*([\\d.]+)\\s*%").find(text)?.groupValues?.get(1)?.toDoubleOrNull()
    private fun parseFastingGlu(text: String) = Regex("(?:空腹)?血糖\\s*([\\d.]+)\\s*mmol/L").find(text)?.groupValues?.get(1)?.toDoubleOrNull()
    private fun parseCreatinine(text: String) = Regex("(?:Scr|肌酐)\\s*([\\d.]+)").find(text)?.groupValues?.get(1)?.toDoubleOrNull()
    private fun parseCrUnit(text: String) = if (Regex("µ?umol/L|μmol/L").containsMatchIn(text)) "umol/L" else if (Regex("mg/dL").containsMatchIn(text)) "mg/dL" else null
    private fun parseEgfr(text: String) = Regex("eGFR\\s*([\\d.]+)").find(text)?.groupValues?.get(1)?.toDoubleOrNull()
    private fun parseK(text: String) = Regex("K[⁺+]\\s*([\\d.]+)\\s*mmol/L").find(text)?.groupValues?.get(1)?.toDoubleOrNull()
    private fun parseNa(text: String) = Regex("Na[⁺+]\\s*([\\d.]+)\\s*mmol/L").find(text)?.groupValues?.get(1)?.toDoubleOrNull()
    private fun parseHb(text: String) = Regex("Hb\\s*([\\d.]+)\\s*g/L").find(text)?.groupValues?.get(1)?.toDoubleOrNull()
    private fun parseAlbumin(text: String) = Regex("ALB\\s*([\\d.]+)\\s*g/L").find(text)?.groupValues?.get(1)?.toDoubleOrNull()
    private fun parseAlt(text: String) = Regex("ALT\\s*([\\d.]+)\\s*U/L").find(text)?.groupValues?.get(1)?.toDoubleOrNull()
    private fun parseAst(text: String) = Regex("AST\\s*([\\d.]+)\\s*U/L").find(text)?.groupValues?.get(1)?.toDoubleOrNull()
    private fun parseLDL(text: String) = Regex("LDL[−\\-C]?\\s*([\\d.]+)\\s*mmol/L").find(text)?.groupValues?.get(1)?.toDoubleOrNull()
}
