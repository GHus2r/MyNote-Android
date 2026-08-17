package com.mynote.android.util

/**
 * 临床常用医学计算器
 * 公式遵循最新国际指南 (KDIGO 2024, ESC 2024, AHA/ACC 等)
 */
object MedicalCalculator {

    // ═══════════ eGFR (CKD-EPI 2021, 无种族系数) ═══════════
    data class EgfrResult(val egfr: Double, val stage: String, val interpretation: String)

    fun egfr(age: Int, creatinine: Double, isFemale: Boolean): EgfrResult {
        val scr = if (creatinine <= 0.2) 0.2 else creatinine
        val k = if (isFemale) 0.7 else 0.9
        val a = if (isFemale) -0.241 else -0.302
        val sexFactor = if (isFemale) 1.012 else 1.0

        val egfr = 142.0 *
                Math.min(scr / k, 1.0).pow(a) *
                Math.max(scr / k, 1.0).pow(-1.200) *
                0.9938.pow(age.toDouble()) *
                sexFactor

        val stage = when {
            egfr >= 90 -> "G1 (正常)"
            egfr >= 60 -> "G2 (轻度下降)"
            egfr >= 45 -> "G3a (轻中度下降)"
            egfr >= 30 -> "G3b (中重度下降)"
            egfr >= 15 -> "G4 (重度下降)"
            else -> "G5 (肾衰竭)"
        }
        val interp = when {
            egfr >= 90 -> "肾功能正常"
            egfr >= 60 -> "轻度下降，密切随访"
            egfr >= 30 -> "中度 CKD，需专科评估"
            egfr >= 15 -> "重度 CKD，准备肾脏替代治疗"
            else -> "终末期肾病，需透析或移植"
        }
        return EgfrResult(egfr.coerceAtLeast(1.0), stage, interp)
    }

    private fun Double.pow(p: Double) = Math.pow(this, p)

    // ═══════════ CHA₂DS₂-VASc (房颤卒中风险) ═══════════
    data class ChadVascResult(val score: Int, val risk: String, val recommendation: String)

    fun chadsVasc(chf: Boolean, htn: Boolean, age75: Boolean, age65: Boolean,
                  dm: Boolean, stroke: Boolean, vascular: Boolean, isFemale: Boolean): ChadVascResult {
        var score = 0
        if (chf) score += 1
        if (htn) score += 1
        if (age75) score += 2
        if (age65 && !age75) score += 1
        if (dm) score += 1
        if (stroke) score += 2
        if (vascular) score += 1
        if (isFemale) score += 1

        return when {
            score >= 2 -> ChadVascResult(score, "高危", "推荐口服抗凝药 (NOAC 首选，如达比加群/利伐沙班)")
            score == 1 -> ChadVascResult(score, "中危", "可考虑口服抗凝药，个体化决策")
            else -> ChadVascResult(score, "低危", "不推荐抗凝治疗")
        }
    }

    // ═══════════ HAS-BLED (抗凝出血风险) ═══════════
    data class HasBledResult(val score: Int, val risk: String)

    fun hasBled(htn: Boolean, renal: Boolean, liver: Boolean, stroke: Boolean,
                bleeding: Boolean, labileInr: Boolean, age65: Boolean,
                drugs: Boolean, alcohol: Boolean): HasBledResult {
        var score = 0
        if (htn) score += 1
        if (renal) score += 1
        if (liver) score += 1
        if (stroke) score += 1
        if (bleeding) score += 1
        if (labileInr) score += 1
        if (age65) score += 1
        if (drugs) score += 1
        if (alcohol) score += 1
        return when {
            score >= 3 -> HasBledResult(score, "高风险 (≥3)，需谨慎抗凝")
            else -> HasBledResult(score, "低风险，可常规抗凝")
        }
    }

    // ═══════════ CURB-65 (社区获得性肺炎严重度) ═══════════
    data class CurbResult(val score: Int, val mortality: String, val site: String)

    fun curb65(confusion: Boolean, bunGt19: Boolean, rrGt30: Boolean,
               sbpLt90: Boolean, ageGt65: Boolean): CurbResult {
        var score = 0
        if (confusion) score += 1
        if (bunGt19) score += 1  // BUN > 7 mmol/L ≈ 19.6 mg/dL
        if (rrGt30) score += 1
        if (sbpLt90) score += 1  // SBP < 90 or DBP ≤ 60
        if (ageGt65) score += 1
        return when (score) {
            0, 1 -> CurbResult(score, "<3%", "可门诊治疗")
            2 -> CurbResult(score, "~9%", "短期住院或密切观察")
            3 -> CurbResult(score, "~17%", "需住院，考虑ICU")
            else -> CurbResult(score, "≥28%", "需入住ICU")
        }
    }

    // ═══════════ Wells DVT 评分 ═══════════
    data class WellsDvtResult(val score: Int, val risk: String, val action: String)

    fun wellsDvt(cancer: Boolean, paralysis: Boolean, bedridden: Boolean,
                 localTenderness: Boolean, swellingEntire: Boolean,
                 calfSwelling: Boolean, pittingEdema: Boolean,
                 collaterals: Boolean, altDxLessLikely: Boolean): WellsDvtResult {
        var score = 0
        if (cancer) score += 1
        if (paralysis) score += 1
        if (bedridden) score += 1
        if (localTenderness) score += 1
        if (swellingEntire) score += 1
        if (calfSwelling) score += 1  // >3cm
        if (pittingEdema) score += 1
        if (collaterals) score += 1
        if (altDxLessLikely) score -= 2
        return when {
            score >= 2 -> WellsDvtResult(score, "DVT 可能性高", "做超声 + D-二聚体")
            score >= 0 -> WellsDvtResult(score, "DVT 可能性中", "查 D-二聚体，阳性→超声")
            else -> WellsDvtResult(score, "DVT 可能性低", "临床观察")
        }
    }

    // ═══════════ HEART 评分 (胸痛危险分层) ═══════════
    data class HeartResult(val score: Int, val risk: String, val action: String)

    fun heart(history: Int, ecg: Int, age: Int, riskFactors: Int, troponin: Int): HeartResult {
        val score = history + ecg + age + riskFactors + troponin
        return when {
            score >= 7 -> HeartResult(score, "高危", "紧急冠脉造影")
            score >= 4 -> HeartResult(score, "中危", "留观+系列肌钙蛋白±负荷试验")
            else -> HeartResult(score, "低危", "可早期出院")
        }
    }

    // ═══════════ 补钠公式 ═══════════
    fun sodiumDeficit(weightKg: Double, targetNa: Int, currentNa: Int): Double {
        if (currentNa >= targetNa) return 0.0
        return 0.6 * weightKg * (targetNa - currentNa)
    }

    // ═══════════ 补液公式 (4-2-1 法则) ═══════════
    fun maintenanceFluid(weightKg: Double): Double {
        return when {
            weightKg <= 10 -> weightKg * 100
            weightKg <= 20 -> 1000 + (weightKg - 10) * 50
            else -> 1500 + (weightKg - 20) * 20
        }
    }

    // ═══════════ BMI ═══════════
    data class BmiResult(val bmi: Double, val category: String)

    fun bmi(weightKg: Double, heightCm: Double): BmiResult {
        val heightM = heightCm / 100.0
        val bmi = weightKg / (heightM * heightM)
        val cat = when {
            bmi < 18.5 -> "偏瘦"
            bmi < 24 -> "正常"
            bmi < 28 -> "超重"
            else -> "肥胖"
        }
        return BmiResult(bmi, cat)
    }

    // ═══════════ GRACE 2.0 (ACS 出院/入院死亡风险) ═══════════
    data class GraceResult(val score: Int, val inhospMortality: String, val risk: String)

    fun grace(age: Int, heartRate: Int, sbp: Int, creatinine: Double,
              killip: Int, cardiacArrest: Boolean, stDeviation: Boolean,
              elevatedTroponin: Boolean): GraceResult {
        var score = 0
        score += when { age < 30 -> 0; age < 40 -> 8; age < 50 -> 25; age < 60 -> 41; age < 70 -> 58; age < 80 -> 75; age < 90 -> 91; else -> 100 }
        score += when { heartRate < 50 -> 0; heartRate < 70 -> 3; heartRate < 90 -> 9; heartRate < 110 -> 15; heartRate < 150 -> 24; else -> 38 }
        score += when { sbp < 80 -> 58; sbp < 100 -> 53; sbp < 120 -> 43; sbp < 140 -> 34; sbp < 160 -> 24; sbp < 200 -> 10; else -> 0 }
        score += when { creatinine < 0.4 -> 1; creatinine < 0.8 -> 4; creatinine < 1.2 -> 7; creatinine < 1.6 -> 10; creatinine < 2.0 -> 13; creatinine < 4.0 -> 21; else -> 28 }
        score += when (killip) { 1 -> 0; 2 -> 20; 3 -> 39; else -> 59 }
        if (cardiacArrest) score += 39
        if (stDeviation) score += 28
        if (elevatedTroponin) score += 14
        val mort = when { score <= 108 -> "<1%"; score <= 140 -> "1-3%"; score <= 165 -> "3-5%"; score <= 195 -> "5-10%"; else -> ">10%" }
        val risk = when { score <= 108 -> "低危"; score <= 140 -> "中危"; else -> "高危" }
        return GraceResult(score, mort, risk)
    }

    // ═══════════ Wells PE (肺栓塞) ═══════════
    data class WellsPeResult(val score: Double, val probability: String, val action: String)

    fun wellsPe(dvtSigns: Boolean, peMostLikely: Boolean, heartRateGt100: Boolean, immobilization: Boolean,
                previousPeDvt: Boolean, hemoptysis: Boolean, cancer: Boolean): WellsPeResult {
        var score = 0.0
        if (dvtSigns) score += 3.0; if (peMostLikely) score += 3.0
        if (heartRateGt100) score += 1.5; if (immobilization) score += 1.5
        if (previousPeDvt) score += 1.5; if (hemoptysis) score += 1.0; if (cancer) score += 1.0
        return when {
            score > 6 -> WellsPeResult(score, "高危", "直接 CTPA")
            score >= 2 -> WellsPeResult(score, "中危", "查 D-二聚体，阳性→CTPA")
            else -> WellsPeResult(score, "低危", "PERC 排除 → 可排除 PE")
        }
    }

    // ═══════════ PERC 规则 (PE 排除标准) ═══════════
    fun perc(ageGt50: Boolean, hrGt100: Boolean, spo2Lt95: Boolean, priorPeDvt: Boolean,
             surgery: Boolean, hemoptysis: Boolean, estrogen: Boolean, unilateralLeg: Boolean): String {
        val allNegative = !ageGt50 && !hrGt100 && !spo2Lt95 && !priorPeDvt && !surgery && !hemoptysis && !estrogen && !unilateralLeg
        return if (allNegative) "PERC 全部阴性 → 可排除 PE，无需进一步检查" else "不能排除 PE，需查 D-二聚体"
    }

    // ═══════════ TIMI (UA/NSTEMI) ═══════════
    data class TimiResult(val score: Int, val risk14d: String)

    fun timi(ageGt65: Boolean, cadRiskGt3: Boolean, cadGt50: Boolean, aspirin7d: Boolean,
             angina2x24h: Boolean, stDay: Boolean, elevatedTroponin: Boolean): TimiResult {
        var score = 0
        if (ageGt65) score++; if (cadRiskGt3) score++; if (cadGt50) score++; if (aspirin7d) score++
        if (angina2x24h) score++; if (stDay) score++; if (elevatedTroponin) score++
        return listOf(TimiResult(0, "4.7%"), TimiResult(1, "4.7%"), TimiResult(2, "8.3%"), TimiResult(3, "13.2%"),
            TimiResult(4, "19.9%"), TimiResult(5, "26.2%"), TimiResult(6, "40.9%"))[score]
    }

    // ═══════════ GCS (格拉斯哥昏迷评分) ═══════════
    data class GcsResult(val total: Int, val level: String)
    fun gcs(eye: Int, verbal: Int, motor: Int): GcsResult {
        val total = eye + verbal + motor
        return GcsResult(total, when { total >= 13 -> "轻度"; total >= 9 -> "中度"; else -> "重度昏迷" })
    }

    // ═══════════ MELD (终末期肝病模型) ═══════════
    fun meld(bilirubin: Double, inr: Double, creatinine: Double, dialysis: Boolean = false): Int {
        val b = if (bilirubin < 1) 1.0 else bilirubin
        val c = if (creatinine < 1) 1.0 else if (creatinine > 4) 4.0 else creatinine
        var score = (0.957 * Math.log(b) + 0.378 * Math.log(if (inr < 1) 1.0 else inr) + 0.643 * Math.log(c) + 0.643) * 3.78
        if (dialysis) score = (0.957 * Math.log(b) + 0.378 * Math.log(if (inr < 1) 1.0 else inr) + 0.643 * 1.08 + 0.643) * 3.78
        return score.toInt().coerceIn(6, 40)
    }

    // ═══════════ Child-Pugh (肝硬化分级) ═══════════
    data class ChildPughResult(val score: Int, val grade: String, val survival: String)

    fun childPugh(bilirubin: Double, albumin: Double, inr: Double, ascites: Int, encephalopathy: Int): ChildPughResult {
        var score = 0
        score += when { bilirubin < 34 -> 1; bilirubin < 50 -> 2; else -> 3 }
        score += when { albumin > 35 -> 1; albumin > 28 -> 2; else -> 3 }
        score += when { inr < 1.7 -> 1; inr < 2.3 -> 2; else -> 3 }
        score += ascites
        score += encephalopathy
        return when {
            score <= 6 -> ChildPughResult(score, "A级", "1年生存率 100%，可耐受手术")
            score <= 9 -> ChildPughResult(score, "B级", "1年生存率 80%，高风险手术")
            else -> ChildPughResult(score, "C级", "1年生存率 45%，肝移植指征")
        }
    }

    // ═══════════ qSOFA (脓毒症筛查) ═══════════
    fun qsofa(rrGt22: Boolean, sbpLt100: Boolean, gcsLt15: Boolean): String =
        if (listOf(rrGt22, sbpLt100, gcsLt15).count { it } >= 2) "qSOFA ≥ 2 → 脓毒症高风险，需进一步评估" else "qSOFA < 2 → 低风险"

    // ═══════════ ABCD² (TIA 后卒中风险) ═══════════
    data class AbcdResult(val score: Int, val risk2d: String, val action: String)
    fun abcd2(ageGt60: Boolean, bpGt14090: Boolean, clinical: Int, duration: Int, dm: Boolean): AbcdResult {
        var s = 0; if (ageGt60) s++; if (bpGt14090) s++; s += clinical; s += duration; if (dm) s++
        return when { s >= 6 -> AbcdResult(s, "8.1%", "紧急住院评估"); s >= 4 -> AbcdResult(s, "4.1%", "24h内专科评估"); else -> AbcdResult(s, "1.0%", "1周内门诊评估") }
    }

    // ═══════════ DAS28 (类风湿关节炎活动度) ═══════════
    fun das28(tender28: Int, swollen28: Int, esr: Double, vaspain: Double): String {
        val score = 0.56 * Math.sqrt(tender28.toDouble()) + 0.28 * Math.sqrt(swollen28.toDouble()) + 0.70 * Math.log(esr + 0.01) + 0.014 * vaspain
        return "DAS28-ESR: %.2f\n%s".format(score, when { score > 5.1 -> "高度活动 → 强化治疗"; score > 3.2 -> "中度活动 → 调整治疗"; score > 2.6 -> "低度活动 → 维持"; else -> "缓解" })
    }

    // ═══════════ Bishop 评分 (宫颈成熟度) ═══════════
    data class BishopResult(val score: Int, val ripening: String, val recommendation: String)
    fun bishop(dilation: Int, effacement: Int, station: Int, consistency: Int, position: Int): BishopResult {
        val s = dilation + effacement + station + consistency + position
        return when { s >= 8 -> BishopResult(s, "宫颈成熟", "可引产"); s >= 6 -> BishopResult(s, "边缘成熟", "可考虑引产或促进成熟"); else -> BishopResult(s, "不成熟", "需促宫颈成熟") }
    }

    // ═══════════ APGAR 评分 ═══════════
    fun apgar(appearance: Int, pulse: Int, grimace: Int, activity: Int, respiration: Int): String {
        val total = appearance + pulse + grimace + activity + respiration
        return "APGAR: $total\n${when { total >= 7 -> "正常"; total >= 4 -> "轻度窒息"; else -> "重度窒息，需复苏" }}"
    }

    // ═══════════ HOMA-IR (胰岛素抵抗) ═══════════
    fun homaIr(fastingInsulin: Double, fastingGlucose: Double): String {
        val i = (fastingInsulin * fastingGlucose) / 22.5
        return "HOMA-IR: %.1f\n%s".format(i, if (i > 2.5) "胰岛素抵抗" else "正常")
    }

    // ═══════════ 矫正血钙 ═══════════
    fun correctedCalcium(ca: Double, albumin: Double): Double = ca + 0.02 * (40 - albumin)

    // ═══════════ 阴离子间隙 ═══════════
    fun anionGap(na: Double, cl: Double, hco3: Double): Double = na - (cl + hco3)

    // ═══════════ 血渗透压 ═══════════
    fun serumOsm(na: Double, glucose: Double, bun: Double): Double = 2 * na + glucose / 18 + bun / 2.8

    // ═══════════ Framingham 10年心血管风险 (简化) ═══════════
    fun framingham(age: Int, totalChol: Double, hdl: Double, sbp: Int, treated: Boolean, smoker: Boolean, dm: Boolean): String {
        var p = 0.0
        p += when { age < 40 -> 0.0; age < 50 -> 2.0; age < 60 -> 4.0; age < 70 -> 6.0; else -> 8.0 }
        p += if (totalChol > 5.2) 2.0 else 1.0
        p += if (hdl < 1.0) 1.0 else 0.0
        p += when { sbp < 120 -> 0.0; sbp < 140 -> 1.0; sbp < 160 -> 2.0; else -> 3.0 }
        if (treated) p += 2.0; if (smoker) p += 3.0; if (dm) p += 2.0
        val risk = (p / 20.0 * 30.0).coerceIn(0.0, 30.0)
        return "10年CVD风险: %.1f%%\n%s".format(risk, when { risk < 5 -> "低危"; risk < 10 -> "中危"; risk < 20 -> "高危"; else -> "极高危" })
    }

    // ═══════════ 补钾公式 ═══════════
    fun potassiumDeficit(weightKg: Double, targetK: Double, currentK: Double): Double =
        if (currentK >= targetK) 0.0 else (targetK - currentK) * weightKg * 0.3

    // ═══════════ 药物剂量转换 ═══════════
    fun prednisoneEquivalent(mg: Double, drug: String): Double = when (drug) {
        "泼尼松/强的松" -> mg * 1.0; "甲泼尼龙" -> mg * 1.25; "地塞米松" -> mg * 6.67; "氢化可的松" -> mg * 0.25; else -> mg }

    // ═══════════ SOFA (序贯器官衰竭评分) ═══════════
    data class SofaResult(val score: Int, val mortality: String)
    fun sofa(resp: Int, coag: Int, liver: Int, cv: Int, cns: Int, renal: Int): SofaResult {
        val score = resp + coag + liver + cv + cns + renal
        val mort = when { score < 2 -> "死亡率<10%"; score in 2..5 -> "死亡率10-20%"; score in 6..9 -> "死亡率20-40%"; score in 10..12 -> "死亡率40-60%"; else -> "死亡率>60%" }
        return SofaResult(score, mort)
    }

    // ═══════════ APACHE II ═══════════
    fun apacheII(temp: Double, map: Int, hr: Int, rr: Int, pao2: Double, fio2: Double, ph: Double,
                 na: Double, k: Double, cr: Double, hct: Double, wbc: Double, gcs: Int,
                 age: Int, chronic: Boolean): String {
        // Temperature
        val tp = when { temp >= 41 -> 4; temp in 39.0..40.9 -> 3; temp in 38.5..38.9 -> 1; temp in 36.0..38.4 -> 0; temp in 34.0..35.9 -> 1; temp in 32.0..33.9 -> 2; temp in 30.0..31.9 -> 3; temp < 30 -> 4; else -> 0 }
        // MAP
        val mp = when { map >= 160 -> 4; map in 130..159 -> 3; map in 110..129 -> 2; map in 70..109 -> 0; map in 50..69 -> 2; map < 50 -> 4; else -> 0 }
        // HR
        val hp = when { hr >= 180 -> 4; hr in 140..179 -> 3; hr in 110..139 -> 2; hr in 70..109 -> 0; hr in 55..69 -> 2; hr in 40..54 -> 3; hr < 40 -> 4; else -> 0 }
        // RR
        val rp = when { rr >= 50 -> 4; rr in 35..49 -> 3; rr in 25..34 -> 1; rr in 12..24 -> 0; rr in 10..11 -> 1; rr in 6..9 -> 2; rr < 6 -> 4; else -> 0 }
        // AaDO2
        val aado2 = if (fio2 >= 0.5) (fio2 * 713 - pao2 - 47).coerceAtLeast(0.0) else pao2
        val oxp = if (fio2 >= 0.5) {
            when { aado2 >= 500 -> 4; aado2 >= 350 -> 3; aado2 >= 200 -> 2; aado2 < 200 -> 0; else -> 0 }
        } else {
            when { pao2.toInt() >= 70 -> 0; pao2.toInt() >= 61 -> 1; pao2.toInt() >= 55 -> 3; pao2.toInt() < 55 -> 4; else -> 0 }
        }
        // pH
        val phs = when { ph >= 7.7 -> 4; ph >= 7.6 -> 3; ph >= 7.5 -> 1; ph >= 7.33 -> 0; ph >= 7.25 -> 2; ph >= 7.15 -> 3; ph < 7.15 -> 4; else -> 0 }
        // Na
        val nap = when { na >= 180 -> 4; na >= 160 -> 3; na >= 155 -> 2; na >= 150 -> 1; na >= 130 -> 0; na >= 120 -> 2; na >= 111 -> 3; na <= 110 -> 4; else -> 0 }
        // K
        val kp = when { k >= 7.0 -> 4; k >= 6.0 -> 3; k >= 5.5 -> 1; k >= 3.5 -> 0; k >= 3.0 -> 1; k >= 2.5 -> 2; k < 2.5 -> 4; else -> 0 }
        // Creatinine
        var crp = when { cr >= 3.5 -> 8; cr >= 2.0 -> 6; cr >= 1.5 -> 4; cr >= 0.6 -> 0; cr < 0.6 -> 2; else -> 0 }
        if (cr >= 1.5 && chronic) crp *= 2 // acute on chronic
        // HCT
        val htp = when { hct >= 60 -> 4; hct in 50.0..59.9 -> 2; hct in 46.0..49.9 -> 1; hct in 30.0..45.9 -> 0; hct in 20.0..29.9 -> 2; hct < 20 -> 4; else -> 0 }
        // WBC
        val wbp = when { wbc >= 40 -> 4; wbc in 20.0..39.9 -> 2; wbc in 15.0..19.9 -> 1; wbc in 3.0..14.9 -> 0; wbc in 1.0..2.9 -> 2; wbc < 1.0 -> 4; else -> 0 }
        // GCS
        val gc = 15 - gcs
        // Age
        val ap = when { age >= 75 -> 6; age in 65..74 -> 5; age in 55..64 -> 3; age in 45..54 -> 2; age < 45 -> 0; else -> 0 }
        // Chronic
        val ch = if (chronic) 5 else 0

        val aps = tp + mp + hp + rp + oxp + phs + nap + kp + crp + htp + wbp + gc
        val total = aps + ap + ch
        val mort = when { total < 10 -> "死亡率~5%"; total in 10..14 -> "死亡率~12%"; total in 15..19 -> "死亡率~22%"; total in 20..24 -> "死亡率~40%"; total in 25..29 -> "死亡率~55%"; total >= 30 -> "死亡率~73%"; else -> "" }
        return "APACHE II=$total\nAPS=$aps  年龄=$ap  慢性=$ch\n$mort"
    }

    // ═══════════ NEWS2 ═══════════
    data class News2Result(val score: Int, val risk: String, val action: String)
    fun news2(rr: Int, spo2: Int, o2Therapy: Boolean, sbp: Int, hr: Int, temp: Double, avpu: Int): News2Result {
        // RR
        val rs = when { rr <= 8 -> 3; rr in 9..11 -> 1; rr in 12..20 -> 0; rr in 21..24 -> 2; rr >= 25 -> 3; else -> 0 }
        // SpO2 scale 1
        val s1 = when { spo2 <= 91 -> 3; spo2 in 92..93 -> 2; spo2 in 94..95 -> 1; spo2 >= 96 -> 0; else -> 0 }
        // SpO2 scale 2 (on O2)
        val s2 = when { spo2 <= 83 -> 3; spo2 in 84..85 -> 2; spo2 in 86..87 -> 1; spo2 in 88..92 -> 0; spo2 in 93..94 -> -1; spo2 in 95..96 -> -2; spo2 >= 97 -> -3; else -> 0 }
        val ss = if (o2Therapy) s2.coerceAtLeast(0) else s1
        // SBP
        val bps = when { sbp <= 90 -> 3; sbp in 91..100 -> 2; sbp in 101..110 -> 1; sbp in 111..219 -> 0; sbp >= 220 -> 3; else -> 0 }
        // HR
        val hrs = when { hr <= 40 -> 3; hr in 41..50 -> 1; hr in 51..90 -> 0; hr in 91..110 -> 1; hr in 111..130 -> 2; hr >= 131 -> 3; else -> 0 }
        // Temp
        val ts = when { temp <= 35.0 -> 3; temp in 35.1..36.0 -> 1; temp in 36.1..38.0 -> 0; temp in 38.1..39.0 -> 1; temp >= 39.1 -> 2; else -> 0 }
        // AVPU
        val as_ = if (avpu == 3) 3 else 0
        // Oxygen
        val os = if (o2Therapy) 2 else 0

        val score = rs + ss + bps + hrs + ts + as_ + os
        val (risk, action) = when {
            score >= 7 -> "高危" to "紧急评估，考虑ICU/HDU"
            score in 5..6 -> "中危" to "紧急响应，1小时内由高年资医生评估"
            score in 1..4 -> "低危" to "病房护士加大监测频率"
            score == 0 -> "低风险" to "继续每12h常规评估"
            else -> "" to ""
        }
        return News2Result(score, risk, action)
    }

    // ═══════════ Ranson 胰腺炎评分 ═══════════
    fun ranson(age: Int, wbc: Double, glucose: Double, ldh: Double, ast: Double,
               hct: Double, bun: Double, calcium: Double, pao2: Double, baseDeficit: Double,
               fluidSeq: Double): String {
        var s = 0
        if (age > 55) s++; if (wbc > 16) s++; if (glucose > 11.1) s++
        if (ldh > 350) s++; if (ast > 250) s++
        if (hct < 30) s++; if (bun > 16) s++; if (calcium < 2.0) s++
        if (pao2 < 60) s++; if (baseDeficit > -4) s++; if (fluidSeq > 6) s++
        val mort = when { s <= 2 -> "<1%"; s in 3..4 -> "15%"; s in 5..6 -> "40%"; else -> ">40%" }
        return "Ranson=${s}分  死亡率${mort}"
    }

    // ═══════════ Centor 咽炎评分 ═══════════
    fun centor(fever: Boolean, exudate: Boolean, adenopathy: Boolean, noCough: Boolean, age315: Boolean): String {
        var s = 0; if (fever) s++; if (exudate) s++; if (adenopathy) s++; if (noCough) s++; if (age315) s++
        val risk = when { s >= 4 -> "GAS概率~50-60%, 建议抗生素"; s in 2..3 -> "GAS概率~20-30%, 建议快速检测"; else -> "GAS概率<10%, 对症治疗" }
        return "Centor=${s}分\n${risk}"
    }

    // ═══════════ Alvarado 阑尾炎评分 ═══════════
    fun alvarado(migra: Int, anorexia: Int, nausea: Int, rlqTender: Int, rebound: Int,
                 temp: Int, wbc: Int, shift: Int): String {
        val s = migra + anorexia + nausea + rlqTender + rebound + temp + wbc + shift
        val risk = when { s >= 7 -> "高度可疑, 建议手术"; s in 5..6 -> "可疑, 影像学检查"; else -> "低概率, 观察" }
        return "Alvarado=${s}分\n${risk}"
    }

    // ═══════════ Geneva PE 评分 ═══════════
    fun genevaPe(age: Int, prevDvt: Boolean, surgery: Boolean, cancer: Boolean,
                 hemoptysis: Boolean, hr: Int, legPain: Boolean): String {
        var s = 0
        if (age > 65) s++; if (prevDvt) s += 3; if (surgery) s += 2
        if (cancer) s += 2; if (hemoptysis) s += 2
        if (hr >= 95) s++; if (hr in 75..94) s += 3
        if (legPain) s++
        val risk = when { s >= 11 -> "高危, PE概率65%"; s in 5..10 -> "中危"; else -> "低危, PE概率<8%" }
        return "Geneva PE=${s}分\n${risk}"
    }

    // ═══════════ Padua 血栓风险 ═══════════
    fun padua(cancer: Boolean, prevVte: Boolean, bedridden: Boolean, thrombophilia: Boolean,
              recentTrauma: Boolean, age70: Boolean, hf: Boolean, ami: Boolean, infection: Boolean,
              obesity: Boolean, hormonal: Boolean): String {
        var s = 0
        if (cancer) s += 3; if (prevVte) s += 3; if (bedridden) s += 3; if (thrombophilia) s += 3
        if (recentTrauma) s += 2; if (age70) s++; if (hf) s++; if (ami) s++
        if (infection) s++; if (obesity) s++; if (hormonal) s++
        if (infection) s++; if (obesity) s++; if (hormonal) s++
        val risk = if (s >= 4) "高危, 需药物预防" else "低危, 物理预防"
        return "Padua=${s}分\n${risk}"
    }

    // ═══════════ 输液速度计算器 ═══════════
    data class IvDripResult(val dropsPerMin: Double, val mlPerHour: Double, val timeMin: Double, val summary: String)
    fun ivDrip(volumeMl: Double, dropFactor: Int = 20, timeMin: Double): IvDripResult {
        val mlH = if (timeMin > 0) volumeMl / timeMin * 60 else 0.0
        val gttMin = if (timeMin > 0) volumeMl * dropFactor / timeMin else 0.0
        return IvDripResult(gttMin, mlH, timeMin, "${volumeMl.toInt()}mL×${dropFactor}滴/mL×${timeMin.toInt()}min → ${gttMin.toInt()}滴/分(${mlH.toInt()}mL/h)")
    }
    fun ivDripFast(volumeMl: Double, gttMin: Double, dropFactor: Int = 20): String {
        val tMin = volumeMl * dropFactor / gttMin
        val mlH = volumeMl / tMin * 60
        return "${gttMin.toInt()}滴/分 → ${tMin.toInt()}min, ${mlH.toInt()}mL/h"
    }

    // ═══════════ BSA 体表面积 ═══════════
    data class BsaResult(val bsa: Double, val formula: String)
    fun bsa(weightKg: Double, heightCm: Double): BsaResult {
        val b = 0.007184 * Math.pow(weightKg, 0.425) * Math.pow(heightCm, 0.725)
        return BsaResult(Math.round(b * 100.0) / 100.0, "DuBois: 0.007184×WT^0.425×HT^0.725")
    }

    // ═══════════ 儿童按体重剂量 ═══════════
    data class PediDoseResult(val doseMg: Double, val volMl: Double, val summary: String)
    fun pediDose(wtKg: Double, mgPerKg: Double, concMgPerMl: Double): PediDoseResult {
        val d = wtKg * mgPerKg; val v = d / concMgPerMl
        return PediDoseResult(d, v, "${wtKg}kg×${mgPerKg}mg/kg=${"%.1f".format(d)}mg → ${"%.1f".format(v)}mL")
    }

    // ═══════════ ISS 创伤严重度 ═══════════
    data class IssResult(val iss: Int, val risk: String, val mortality: String)
    fun iss(aisScores: List<Int>): IssResult {
        val s = aisScores.sortedDescending().take(3).sumOf { it * it }
        return IssResult(s,
            when { s>=25->"重度"; s>=16->"中度"; s>=9->"轻度"; else->"轻伤" },
            when { s>=50->"死亡率~50-75%"; s>=25->"~10-25%"; s>=16->"~5-10%"; s>=9->"<5%"; else->"<1%" })
    }

    // ═══════════ RTS 改良创伤评分 ═══════════
    data class RtsResult(val rts: Double, val coded: String, val surv: String)
    fun rts(gcs: Int, sbp: Int, rr: Int): RtsResult {
        val gc = when { gcs>=13->4.0; gcs>=9->3.0; gcs>=6->2.0; gcs>=4->1.0; else->0.0 }
        val sc = when { sbp>89->4.0; sbp>=76->3.0; sbp>=50->2.0; sbp>=1->1.0; else->0.0 }
        val rc = when { rr in 10..29->4.0; rr>29->3.0; rr in 6..9->2.0; rr in 1..5->1.0; else->0.0 }
        val r = Math.round((gc*0.9368+sc*0.7326+rc*0.2908)*100.0)/100.0
        return RtsResult(r, "GCS=$gcs/SBP=$sbp/RR=$rr",
            when { r>=7.0->"生存率>90%"; r>=5.0->"50-90%"; r>=3.0->"10-50%"; else->"<10%" })
    }

    // ═══════════ 疼痛评估 ═══════════
    data class PainResult(val nrs: Int, val level: String, val mgmt: String)
    fun painAssess(nrsScore: Int): PainResult = PainResult(nrsScore,
        when { nrsScore>=8->"重度"; nrsScore>=4->"中度"; nrsScore>=1->"轻度"; else->"无痛" },
        when { nrsScore>=8->"强阿片(吗啡/芬太尼)±NSAIDs"; nrsScore>=4->"弱阿片(曲马多)+NSAIDs"; nrsScore>=1->"NSAIDs/对乙酰氨基酚"; else->"无需" })

    // ═══════════ 妊娠用药分级(FDA) ═══════════
    data class PregDrug(val drug: String, val cat: String)
    private val pregDrugs = listOf(
        PregDrug("左甲状腺素","A"), PregDrug("叶酸","A"), PregDrug("维生素B6","A"),
        PregDrug("对乙酰氨基酚","B"), PregDrug("阿莫西林","B"), PregDrug("头孢曲松","B"), PregDrug("青霉素","B"),
        PregDrug("红霉素","B"), PregDrug("克林霉素","B"), PregDrug("甲硝唑","B(后3月避)"),
        PregDrug("胰岛素","B"), PregDrug("二甲双胍","B"), PregDrug("氯雷他定","B"), PregDrug("奥美拉唑","B"),
        PregDrug("昂丹司琼","B"), PregDrug("沙丁胺醇","C"), PregDrug("呋塞米","C"), PregDrug("氢氯噻嗪","C"),
        PregDrug("硝苯地平","C"), PregDrug("拉贝洛尔","C"), PregDrug("甲基多巴","C"),
        PregDrug("肝素","C"), PregDrug("吗啡","C"), PregDrug("泼尼松","C"), PregDrug("氟康唑","C"),
        PregDrug("卡托普利","D"), PregDrug("厄贝沙坦","D"), PregDrug("缬沙坦","D"),
        PregDrug("苯妥英","D"), PregDrug("卡马西平","D"), PregDrug("丙戊酸","D"),
        PregDrug("华法林","D"), PregDrug("锂盐","D"), PregDrug("四环素","D"),
        PregDrug("异维A酸","X"), PregDrug("沙利度胺","X"), PregDrug("米索前列醇","X"),
        PregDrug("辛伐他汀","X"), PregDrug("阿托伐他汀","X")
    )
    val pregnancyCategories = listOf(
        "A" to "对照研究无风险 — 安全",
        "B" to "动物研究无风险 — 较安全",
        "C" to "动物研究有风险/无数据 — 权衡利弊",
        "D" to "人体研究有风险 — 危及生命时用",
        "X" to "禁忌 — 致畸风险明确"
    )
    fun pregnancyDrug(name: String): PregDrug? = pregDrugs.firstOrNull { it.drug == name.trim() }
    fun searchPregDrugs(q: String) = pregDrugs.filter { it.drug.lowercase().contains(q.lowercase()) }

    // ═══════════ ABG 血气自动判读 ═══════════
    fun abgInterpret(pH: Double, pCO2: Double, HCO3: Double, Na: Double, Cl: Double, Lac: Double = 0.0, Alb: Double = 40.0): String {
        val sb = StringBuilder()
        // 酸碱判定
        val (primary, compensation) = when {
            pH < 7.35 && pCO2 > 45 -> "呼吸性酸中毒" to (if (HCO3 > 27) "代偿(慢性)" else if (HCO3 > 24) "部分代偿" else "失代偿(急性)")
            pH < 7.35 && HCO3 < 22 -> "代谢性酸中毒" to (if (pCO2 < 35) "代偿" else if (pCO2 < 40) "部分代偿" else "失代偿")
            pH > 7.45 && pCO2 < 35 -> "呼吸性碱中毒" to (if (HCO3 < 22) "代偿(慢性)" else if (HCO3 < 24) "部分代偿" else "失代偿(急性)")
            pH > 7.45 && HCO3 > 27 -> "代谢性碱中毒" to (if (pCO2 > 45) "代偿" else if (pCO2 > 40) "部分代偿" else "失代偿")
            pH in 7.35..7.45 && pCO2 > 45 && HCO3 > 27 -> "代偿性呼吸性酸中毒" to "完全代偿"
            pH in 7.35..7.45 && pCO2 < 35 && HCO3 < 22 -> "代偿性代谢性酸中毒" to "完全代偿"
            pH in 7.35..7.45 && HCO3 > 27 && pCO2 > 45 -> "代偿性代谢性碱中毒" to "完全代偿"
            pH in 7.35..7.45 && pCO2 < 35 && HCO3 < 22 -> "代偿性呼吸性碱中毒" to "完全代偿"
            else -> "正常" to ""
        }
        sb.append("【酸碱状态】$primary ($compensation)\n")
        // AG
        val ag = Na - (Cl + HCO3)
        val agCorr = ag + 0.25 * (40.0 - Alb) // 白蛋白校正
        if (ag > 16) 
            sb.append("【AG】${ag.toInt()} (高AG) → 考虑: 酮症/乳酸/肾衰/甲醇/乙二醇\n")
        else if (ag >= 8.0 && ag <= 16.0) 
            sb.append("【AG】${ag.toInt()} (正常)\n")
        else 
            sb.append("【AG】${ag.toInt()} (低AG) → 考虑: 低白蛋白/锂中毒/副蛋白血症\n")

        if (Alb < 40) sb.append("  (白蛋白校正AG≈${agCorr.toInt()})\n")
        // 乳酸
        if (Lac > 4.0) sb.append("【乳酸】${Lac} mmol/L ⚠️ >4 → 组织低灌注/休克/脓毒症\n")
        else if (Lac > 2.2) sb.append("【乳酸】${Lac} mmol/L ↑ >2.2 → 组织缺氧可能\n")
        else if (Lac > 0) sb.append("【乳酸】${Lac} mmol/L (正常≤2.2)\n")
        // 总结
        sb.append("\n【综合】")
        if (pH < 7.20) sb.append("重度酸血症(pH<7.20) → 考虑碳酸氢钠/CRRT")
        else if (pH < 7.35) sb.append("轻度酸血症")
        else if (pH > 7.55) sb.append("重度碱血症(pH>7.55) → 危及生命")
        else if (pH > 7.45) sb.append("轻度碱血症")
        else sb.append("酸碱平衡正常")
        return sb.toString()
    }
}
