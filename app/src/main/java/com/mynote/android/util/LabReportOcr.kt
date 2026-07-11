package com.mynote.android.util

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

/**
 * 化验单 OCR 识别器
 * 拍照识别化验单 → 自动提取常见检验值 → 生成结构化文本
 */
object LabReportOcr {

    // 常见化验项目关键词映射（中文 → 英文缩写）
    private val labPatterns = listOf(
        LabPattern("白细胞|WBC|白细胞计数", "WBC", "×10⁹/L", "3.5-9.5"),
        LabPattern("红细胞|RBC|红细胞计数", "RBC", "×10¹²/L", "4.3-5.8"),
        LabPattern("血红蛋白|Hb|HGB|血红蛋白测定", "Hb", "g/L", "130-175"),
        LabPattern("血小板|PLT|血小板计数", "PLT", "×10⁹/L", "125-350"),
        LabPattern("中性粒细胞|NEUT|中性粒细胞百分比", "NEUT%", "%", "40-75"),
        LabPattern("淋巴细胞|LYMPH|淋巴细胞百分比", "LYMPH%", "%", "20-50"),
        LabPattern("血糖|GLU|葡萄糖|空腹血糖", "GLU", "mmol/L", "3.9-6.1"),
        LabPattern("肌酐|Cr|CREA|血肌酐", "Cr", "μmol/L", "44-133"),
        LabPattern("尿素|BUN|UREA|尿素氮", "BUN", "mmol/L", "2.9-8.2"),
        LabPattern("尿酸|UA|URIC", "UA", "μmol/L", "208-428"),
        LabPattern("ALT|谷丙|丙氨酸", "ALT", "U/L", "9-50"),
        LabPattern("AST|谷草|天冬氨酸", "AST", "U/L", "15-40"),
        LabPattern("总胆红素|TBIL|TBil", "TBIL", "μmol/L", "3.4-20.5"),
        LabPattern("直接胆红素|DBIL|DBil", "DBIL", "μmol/L", "0-6.8"),
        LabPattern("白蛋白|ALB|Alb", "ALB", "g/L", "40-55"),
        LabPattern("总蛋白|TP", "TP", "g/L", "65-85"),
        LabPattern("钾|K\\+|血清钾", "K⁺", "mmol/L", "3.5-5.3"),
        LabPattern("钠|Na\\+|血清钠", "Na⁺", "mmol/L", "137-147"),
        LabPattern("氯|Cl-|血清氯", "Cl⁻", "mmol/L", "99-110"),
        LabPattern("钙|Ca|血清钙", "Ca", "mmol/L", "2.1-2.6"),
        LabPattern("糖化|HbA1c|糖化血红蛋白", "HbA1c", "%", "4.0-6.0"),
        LabPattern("总胆固醇|TC|CHOL", "TC", "mmol/L", "3.1-5.7"),
        LabPattern("甘油三酯|TG|TRIG", "TG", "mmol/L", "0.4-1.8"),
        LabPattern("HDL|高密度|HDL-C", "HDL-C", "mmol/L", "1.0-1.6"),
        LabPattern("LDL|低密度|LDL-C", "LDL-C", "mmol/L", "0-3.4"),
        LabPattern("CRP|C反应蛋白", "CRP", "mg/L", "0-8"),
        LabPattern("降钙素原|PCT|降钙素", "PCT", "ng/mL", "0-0.05"),
        LabPattern("D-二聚体|D-Dimer", "D-Dimer", "mg/L", "0-0.5"),
        LabPattern("BNP|脑钠肽|NT-proBNP", "BNP", "pg/mL", "0-100"),
        LabPattern("肌钙蛋白|cTnI|Troponin|hs-cTn", "cTnI", "ng/mL", "0-0.04"),
        LabPattern("PT|凝血酶原时间", "PT", "s", "11-14.5"),
        LabPattern("APTT|活化部分凝血活酶时间", "APTT", "s", "28-43.5"),
        LabPattern("INR|国际标准化比值", "INR", "", "0.8-1.2"),
        LabPattern("尿蛋白|PRO", "尿PRO", "—", "阴性"),
        LabPattern("尿糖|GLU-U", "尿GLU", "—", "阴性"),
        LabPattern("eGFR|肾小球滤过率|估算肾小球", "eGFR", "mL/min/1.73m²", "≥90"),
        LabPattern("TSH|促甲状腺激素", "TSH", "mIU/L", "0.35-4.94"),
        LabPattern("FT3|游离T3", "FT3", "pmol/L", "3.5-6.5"),
        LabPattern("FT4|游离T4", "FT4", "pmol/L", "11.5-22.7"),
        LabPattern("CK|肌酸激酶|CK-MB", "CK", "U/L", "50-310"),
        LabPattern("淀粉酶|AMY|血淀粉酶", "AMY", "U/L", "35-135"),
        LabPattern("乳酸|Lac|血乳酸", "Lac", "mmol/L", "0.5-1.6"),
    )

    data class LabResult(
        val name: String,
        val abbr: String,
        val value: String,
        val unit: String,
        val reference: String,
        val abnormal: Boolean
    )

    data class LabPattern(
        val regex: String,
        val abbr: String,
        val unit: String,
        val reference: String
    )

    /** 上一次识别的原始文本（供 Qwen 校准用） */
    @Volatile private var lastRawText: String = ""

    fun getLastRawText(): String = lastRawText

    /**
     * 识别化验单图片，返回结构化结果
     */
    suspend fun recognize(bitmap: Bitmap): List<LabResult> = withContext(Dispatchers.IO) {
        val image = InputImage.fromBitmap(bitmap, 0)
        val recognizer = TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())

        val visionText = suspendCancellableCoroutine { cont ->
            recognizer.process(image)
                .addOnSuccessListener { cont.resume(it) }
                .addOnFailureListener { cont.resume(null) }
        }

        if (visionText == null) return@withContext emptyList()

        lastRawText = visionText.text
        val fullText = correctOcrTypos(visionText.text)
        parseResults(fullText)
    }

    /**
     * OCR常见错别字纠正 — 形近字/符号修复
     */
    private val ocrCorrections = mapOf(
        // 血常规
        "白细跑" to "白细胞", "白绌胞" to "白细胞", "白细抱" to "白细胞",
        "红细跑" to "红细胞", "红绌胞" to "红细胞",
        "血小扳" to "血小板", "血小极" to "血小板", "血小枚" to "血小板",
        "血红蛋日" to "血红蛋白", "血红蛋向" to "血红蛋白"
    )

    fun correctOcrTypos(text: String): String {
        var result = text
        for ((wrong, correct) in ocrCorrections) {
            result = result.replace(wrong, correct)
        }
        // 常见符号/单位纠正
        result = result
            .replace("mmoVL", "mmol/L")
            .replace("mmoI/L", "mmol/L")
            .replace("mmoL", "mmol/L")
            .replace("mmo|", "mmol/")
            .replace("umol/L", "μmol/L")
            .replace("μmoVL", "μmol/L")
            .replace("umo/L", "μmol/L")
            .replace("moVL", "mol/L")
            .replace(Regex("(\\d)mgL"), "$1mg/L")
            .replace(Regex("(\\d)gL"), "$1g/L")
            .replace("U/L", "U/L")  // normalize
            .replace("U\\L", "U/L")
            // 字形混淆
            .replace("葡萄塘", "葡萄糖")
            .replace("葡萄椭", "葡萄糖")
            .replace("肌酉干", "肌酐")
            .replace("肌醉", "肌酐")
            .replace("肌酣", "肌酐")
            .replace("胆红索", "胆红素")
            .replace("且红素", "胆红素")
            .replace("白蛋日", "白蛋白")
            .replace("甘油三醋", "甘油三酯")
            .replace("甘汕三酯", "甘油三酯")
            .replace("总胆固酵", "总胆固醇")
            .replace("胆固醉", "胆固醇")
            .replace("脂蛋日", "脂蛋白")
            .replace("谷丙转氪酶", "谷丙转氨酶")
            .replace("谷草转氪酶", "谷草转氨酶")
            .replace("淀粉酷", "淀粉酶")
            .replace("降钙索", "降钙素")
            .replace("降钙索原", "降钙素原")
            .replace("凝血醑", "凝血酶")
            .replace("凝血酶原时问", "凝血酶原时间")
            .replace("甲状腺索", "甲状腺素")
            .replace("促甲状泉", "促甲状腺")
            .replace("肾小球滤过", "肾小球滤过")
            .replace("×10g/L", "×10⁹/L")
            .replace("x109/L", "×10⁹/L")
            .replace("×1012/L", "×10¹²/L")
            .replace("x1012/L", "×10¹²/L")
        return result
    }

    /**
     * 从识别文本中解析化验结果
     */
    fun parseResults(text: String): List<LabResult> {
        val results = mutableListOf<LabResult>()

        for (line in text.lines()) {
            val trimmed = line.trim()
            if (trimmed.isBlank()) continue

            var matched = false
            // 1. 优先匹配预定义的 40 个化验项
            for (pattern in labPatterns) {
                val regex = Regex("(${pattern.regex}).*?([\\d.]+)\\s*(${Regex.escape(pattern.unit)})?", RegexOption.IGNORE_CASE)
                val match = regex.find(trimmed) ?: continue
                val value = match.groupValues[2]
                if (results.any { it.abbr == pattern.abbr }) continue
                results.add(LabResult(
                    name = match.groupValues[1].trim(),
                    abbr = pattern.abbr,
                    value = value,
                    unit = pattern.unit,
                    reference = pattern.reference,
                    abnormal = isValueAbnormal(value, pattern.abbr, pattern.reference)
                ))
                matched = true
                break
            }
            if (matched) continue

            // 2. 通用兜底：匹配 名称=缩写: 数值 单位 (参考范围) 格式
            val generic = Regex("""^(.+?)[=＝:：]\s*([\d.]+)\s*(\S+)\s*[(（]([^)）]+)[)）]?$""").find(trimmed)
            if (generic != null) {
                val name = generic.groupValues[1].trim()
                val value = generic.groupValues[2]
                val unit = generic.groupValues[3].trim()
                val ref = generic.groupValues[4].trim()
                val abbr = name.take(10)  // 用前 10 个字符当缩写
                if (results.any { it.abbr == abbr }) continue
                results.add(LabResult(name = name, abbr = abbr, value = value, unit = unit,
                    reference = ref, abnormal = isValueAbnormal(value, name, ref)))
                continue
            }

            // 3. 更宽泛：匹配任意 数值 + 括号参考 的行
            val loose = Regex("""^(.+?)\s+([\d.]+)\s*[(（]([^)）]+)[)）]""").find(trimmed)
            if (loose != null) {
                val name = loose.groupValues[1].trim()
                val value = loose.groupValues[2]
                val ref = loose.groupValues[3].trim()
                val abbr = name.take(10)
                if (results.any { it.abbr == abbr }) continue
                results.add(LabResult(name = name, abbr = abbr, value = value, unit = "",
                    reference = ref, abnormal = isValueAbnormal(value, name, ref)))
            }
        }
        return results
    }

    private fun isValueAbnormal(value: String, abbr: String, reference: String): Boolean {
        val num = value.toDoubleOrNull() ?: return false
        val ref = reference

        // 解析参考范围，如 "3.5-9.5" 或 "0-0.5" 或 "≤90"
        return when {
            reference == "阴性" -> value != "阴性" && value != "—"
            ref.startsWith("≥") -> {
                val lower = ref.removePrefix("≥").toDoubleOrNull()
                lower != null && num < lower
            }
            ref.startsWith("≤") || ref.startsWith("0-") -> {
                val upper = ref.removePrefix("≤").toDoubleOrNull() ?: ref.split("-").getOrNull(1)?.toDoubleOrNull()
                upper != null && num > upper
            }
            ref.contains("-") -> {
                val parts = ref.split("-")
                val lower = parts[0].toDoubleOrNull()
                val upper = parts[1].toDoubleOrNull()
                lower != null && upper != null && (num < lower || num > upper)
            }
            else -> false
        }
    }

    /**
     * 生成结构化文本，适合插入编辑页
     */
    fun formatResults(results: List<LabResult>): String {
        if (results.isEmpty()) return ""

        val sb = StringBuilder()
        sb.appendLine("<b>【化验结果】</b>")
        sb.appendLine()

        // 按分类分组
        val groups = listOf(
            "血常规" to listOf("WBC", "RBC", "Hb", "PLT", "NEUT%", "LYMPH%"),
            "生化" to listOf("GLU", "Cr", "BUN", "UA", "ALT", "AST", "TBIL", "DBIL", "ALB", "TP", "K⁺", "Na⁺", "Cl⁻", "Ca"),
            "血脂" to listOf("TC", "TG", "HDL-C", "LDL-C"),
            "凝血" to listOf("PT", "APTT", "INR", "D-Dimer"),
            "炎症" to listOf("CRP", "PCT"),
            "心肌" to listOf("cTnI", "BNP", "CK"),
            "内分泌" to listOf("HbA1c", "TSH", "FT3", "FT4"),
            "肾功" to listOf("eGFR"),
            "其他" to listOf("AMY", "Lac", "尿PRO", "尿GLU")
        )

        for ((groupName, abbrs) in groups) {
            val groupResults = results.filter { it.abbr in abbrs }
            if (groupResults.isEmpty()) continue

            sb.appendLine("<b>${groupName}:</b>")
            for (r in groupResults) {
                val marker = if (r.abnormal) "⚠ " else ""
                val arrow = if (r.abnormal) {
                    val num = r.value.toDoubleOrNull()
                    val refParts = r.reference.split("-")
                    val upper = refParts.getOrNull(1)?.toDoubleOrNull()
                    if (num != null && upper != null && num > upper) "↑" else "↓"
                } else ""
                sb.appendLine("${marker}${r.abbr}: ${r.value} ${r.unit} ${arrow} (参考: ${r.reference})")
            }
            sb.appendLine()
        }

        // 未分类的结果
        val categorized = groups.flatMap { it.second }.toSet()
        val others = results.filter { it.abbr !in categorized }
        if (others.isNotEmpty()) {
            sb.appendLine("<b>其他:</b>")
            others.forEach { r ->
                sb.appendLine("${r.abbr}: ${r.value} ${r.unit} (参考: ${r.reference})")
            }
            sb.appendLine()
        }

        return sb.toString().trimEnd()
    }

    /**
     * 解析 Qwen-VL-OCR 返回的 JSON 数组为 LabResult 列表
     */
    fun parseQwenResult(jsonStr: String): List<LabResult> {
        return try {
            val arr = org.json.JSONArray(jsonStr)
            val results = mutableListOf<LabResult>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val value = obj.optString("value", "")
                val ref = obj.optString("reference", "")
                // 判断异常：有箭头标记或数值超出参考范围
                val abnormal = obj.optBoolean("abnormal", false)
                    || value.contains("↑") || value.contains("↓") || value.contains("高") || value.contains("低")
                results.add(LabResult(
                    name = obj.optString("name", ""),
                    abbr = obj.optString("abbr", ""),
                    value = value,
                    unit = obj.optString("unit", ""),
                    reference = ref,
                    abnormal = abnormal
                ))
            }
            results
        } catch (e: Exception) {
            emptyList()
        }
    }
}
