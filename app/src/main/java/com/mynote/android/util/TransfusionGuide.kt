package com.mynote.android.util

/** 输血指征速查 — 各成分血输注阈值与规范 */
object TransfusionGuide {
    data class TxIndication(val title: String, val component: String, val threshold: String, val notes: String)

    val all: List<TxIndication> = listOf(
        // 红细胞
        TxIndication("红细胞-普通住院患者(血流动力学稳定)", "悬浮红细胞(RBC)",
            "Hb<70g/L → 推荐输注\nHb 70-100g/L → 个体化(出血/缺血症状)\nHb>100g/L → 不推荐",
            "限制性输血策略(Hb<70)与自由策略(90-100)死亡率无差异"),
        TxIndication("红细胞-ACS/急性心肌梗死", "悬浮红细胞(RBC)",
            "Hb<80g/L → 推荐输注\nHb 80-100g/L → 权衡(心肌缺血持续存在时)",
            "目标Hb 80-100g/L"),
        TxIndication("红细胞-创伤/活动性出血", "悬浮红细胞(RBC)",
            "以血流动力学和持续出血为指征\n不以单一Hb阈值触发",
            "启动MTP(大量输血方案): RBC:FFP:Plt=1:1:1(或6:4:1)"),
        TxIndication("红细胞-ICU/脓毒症", "悬浮红细胞(RBC)",
            "Hb<70g/L → 推荐(无ACS)\nHb 70-90g/L → 个体化",
            "TRISS试验:限制性输血 vs 自由输血 90天死亡率无差异"),

        // 血小板
        TxIndication("血小板-预防性(无出血/血液科)", "血小板(PLT)",
            "Plt<10×10⁹/L → 推荐(无风险因素)\nPlt<20×10⁹/L → 发热/感染\nPlt<50×10⁹/L → 有创操作/侵入性手术",
            "造血干细胞抑制/化疗患者"),
        TxIndication("血小板-治疗性(活动性出血)", "血小板(PLT)",
            "Plt<50×10⁹/L + 显著出血\nPlt<100×10⁹/L + CNS出血(ICH/TBI)\nPlt<100×10⁹/L + 多发出血/TIC/大量输血",
            "血小板功能不良(抗血小板+脑出血):尽管Plt正常也输注"),
        TxIndication("血小板-术前预防", "血小板(PLT)",
            "Plt<20×10⁹/L → 神经外科/眼科手术\nPlt<50×10⁹/L → 大手术/脊髓麻醉/硬膜外\nPlt<80-100×10⁹/L → 心脏手术CPB",
            "药物: DDAVP (尿毒症)/氨甲环酸"),

        // FFP
        TxIndication("新鲜冰冻血浆-活动性出血/MTP", "新鲜冰冻血浆(FFP)",
            "PT/APTT>1.5×正常值+活动性出血\n大量输血(MTP,>10U RBC)",
            "大出血:以凝血功能为导向\n目标PT<1.5×正常值"),
        TxIndication("新鲜冰冻血浆-无出血/仅INR延长", "新鲜冰冻血浆(FFP)",
            "INR延长(无出血)→不推荐常规输注",
            "无证据支持INR轻度升高预防性FFP有效。口服维生素K更合理。"),
        TxIndication("新鲜冰冻血浆-华法林紧急逆转(颅内出血)", "新鲜冰冻血浆(FFP)/PCC",
            "4因子PCC (优先) 25-50 IU/kg IV (基于INR)\n无PCC时:FFP 15-20 mL/kg IV",
            "PCC(凝血酶原复合物):起效快/容量小/逆转完全 → 优于FFP\n同时IV维生素K 10mg"),

        // 冷沉淀
        TxIndication("冷沉淀-低纤维蛋白原血症", "冷沉淀",
            "Fg<1.0g/L + 大出血/创伤/MTP\nFg<1.5g/L + 产后出血",
            "剂量:1U/5-10kg(通常成人10U)\n预期升高Fg 0.5-1.0g/L"),
        TxIndication("冷沉淀-无出血/仅低纤维蛋白原", "冷沉淀",
            "Fg<1.0g/L(无出血)—可考虑\nFg>1.0g/L(无出血)—不推荐",
            "注意:Fg是急性期蛋白,感染时升高。低Fg需排除DIC/肝病。"),

        // 大量输血方案 MTP
        TxIndication("大量输血方案(MTP)", "全血成分",
            "成人:24h输RBC≥10U 或 1h输≥4U\n预测需要: ABC评分≥2分",
            "方案: RBC:FFP:Plt = 1:1:1(各6U→6U→1人份)\n补充钙(每4U RBC补充1g葡萄糖酸钙) + 氨甲环酸1g IV(创伤3h内)"),

        // 特殊
        TxIndication("围产期产科输血", "产科/RBC",
            "产后出血Hb<70(年轻健康女性)\nPPH(产后大出血):按MTP方案启动",
            "产科MTP:RBC:FFP:冷沉淀:Cryo=6:4:10:1\n注意:妊娠期间生理性稀释,输血阈值需综合评估"),
        TxIndication("DIC(弥散性血管内凝血)", "DIC",
            "以血栓/出血为基础评估\n出血型DIC: PLT<50 + Fg<1.0 → Plt+冷沉淀+FFP\n非出血型DIC:仅肝素/抗凝",
            "DIC最常见的病因:脓毒症/恶性肿瘤/创伤/产科急症"),
        TxIndication("TEG/ROTEM 导向输血(黏弹性试验)", "TEG/ROTEM",
            "R/CT延长→FFP\nK/CFT延长+α角↓→FFP+冷沉淀\nMA/MCF↓→血小板\nLY30/CL30↑→抗纤溶(氨甲环酸)",
            "创伤/肝移植/心脏手术等大手术中实用"),
    )

    val categories = all.map { it.component }.distinct().sorted()
    fun search(q: String) = all.filter {
        val lq = q.lowercase()
        it.title.lowercase().contains(lq) || it.threshold.lowercase().contains(lq) || it.notes.lowercase().contains(lq) || it.component.lowercase().contains(lq)
    }
}
