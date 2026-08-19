package com.mynote.android.util

import org.json.JSONArray
import org.json.JSONObject

/**
 * AI 临床辅助分析结果（结构化输出）
 * 对应融合推理模型返回的 JSON
 */
data class ClinicalAnalysisResult(
    val diagnoses: List<Diagnosis> = emptyList(),
    val recommendations: List<Recommendation> = emptyList(),
    val criticalAlerts: List<CriticalAlert> = emptyList(),
    val pendingInfo: List<String> = emptyList(),
    val rawText: String = ""
) {
    data class Evidence(
        val type: String = "",      // 化验 / 影像 / 病史
        val item: String = "",      // 项目名
        val value: String = "",     // 值
        val reference: String = "", // 参考范围
        val note: String = ""       // 标注（如 ↑3倍以上）
    )

    data class Diagnosis(
        val name: String = "",
        val likelihood: String = "", // 高 / 中 / 低
        val supporting: List<Evidence> = emptyList(),
        val against: List<Evidence> = emptyList()
    )

    data class Recommendation(
        val type: String = "",      // 检查 / 用药 / 护理 / 其他
        val content: String = "",
        val basis: String = ""
    )

    data class CriticalAlert(
        val item: String = "",
        val value: String = "",
        val hint: String = ""
    )

    companion object {
        /** 解析模型返回文本，容错：剥 markdown、截取 JSON 段、解析失败回退原文 */
        fun parse(raw: String): ClinicalAnalysisResult {
            val cleaned = cleanRef(raw)
            val json = extractJson(cleaned) ?: return ClinicalAnalysisResult(rawText = cleaned)
            return try {
                val obj = JSONObject(json)
                ClinicalAnalysisResult(
                    diagnoses = parseDiagnoses(
                        obj.optJSONArray("鉴别诊断") ?: obj.optJSONArray("differential_diagnoses")
                    ),
                    recommendations = parseRecommendations(
                        obj.optJSONArray("诊疗建议") ?: obj.optJSONArray("recommendations")
                    ),
                    criticalAlerts = parseAlerts(
                        obj.optJSONArray("危急值提醒") ?: obj.optJSONArray("critical_alerts")
                    ),
                    pendingInfo = parseStrings(
                        obj.optJSONArray("待补充") ?: obj.optJSONArray("pending_info")
                    ),
                    rawText = cleaned
                )
            } catch (_: Exception) {
                ClinicalAnalysisResult(rawText = cleaned)
            }
        }

        /** 清理模型循证引用标记（如 ^[2]^、^[1,3]^），避免混入结果展示与保存的病历文本 */
        private fun cleanRef(s: String): String =
            s.replace(Regex("\\^\\[[^\\]]*\\]\\^"), "").trim()

        private fun extractJson(s: String): String? {
            var t = s.trim()
            if (t.startsWith("```")) {
                t = t.removePrefix("```").trimStart()
                if (t.startsWith("json")) t = t.removePrefix("json").trimStart()
                if (t.endsWith("```")) t = t.dropLast(3).trim()
            }
            val start = t.indexOf('{')
            val end = t.lastIndexOf('}')
            if (start < 0 || end <= start) return null
            return t.substring(start, end + 1)
        }

        private fun parseDiagnoses(arr: JSONArray?): List<Diagnosis> {
            if (arr == null) return emptyList()
            val list = mutableListOf<Diagnosis>()
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                list.add(
                    Diagnosis(
                        name = o.optString("诊断", o.optString("name", "")),
                        likelihood = o.optString("可能性", o.optString("likelihood", "")),
                        supporting = parseEvidence(
                            o.optJSONArray("支持证据") ?: o.optJSONArray("supporting")
                        ),
                        against = parseEvidence(
                            o.optJSONArray("不支持证据") ?: o.optJSONArray("against")
                        )
                    )
                )
            }
            return list
        }

        private fun parseEvidence(arr: JSONArray?): List<Evidence> {
            if (arr == null) return emptyList()
            val list = mutableListOf<Evidence>()
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                list.add(
                    Evidence(
                        type = o.optString("类型", o.optString("type", "")),
                        item = o.optString("项目", o.optString("item", "")),
                        value = o.optString("值", o.optString("value", "")),
                        reference = o.optString("参考", o.optString("reference", "")),
                        note = o.optString("标注", o.optString("note", ""))
                    )
                )
            }
            return list
        }

        private fun parseRecommendations(arr: JSONArray?): List<Recommendation> {
            if (arr == null) return emptyList()
            val list = mutableListOf<Recommendation>()
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                list.add(
                    Recommendation(
                        type = o.optString("类型", o.optString("type", "")),
                        content = o.optString("内容", o.optString("content", "")),
                        basis = o.optString("依据", o.optString("basis", ""))
                    )
                )
            }
            return list
        }

        private fun parseAlerts(arr: JSONArray?): List<CriticalAlert> {
            if (arr == null) return emptyList()
            val list = mutableListOf<CriticalAlert>()
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                list.add(
                    CriticalAlert(
                        item = o.optString("项目", o.optString("item", "")),
                        value = o.optString("值", o.optString("value", "")),
                        hint = o.optString("提示", o.optString("hint", ""))
                    )
                )
            }
            return list
        }

        private fun parseStrings(arr: JSONArray?): List<String> {
            if (arr == null) return emptyList()
            val list = mutableListOf<String>()
            for (i in 0 until arr.length()) list.add(arr.optString(i, ""))
            return list.filter { it.isNotBlank() }
        }
    }
}
