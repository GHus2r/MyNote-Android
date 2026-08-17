package com.mynote.android.util

/** 急救流程速查 — ACLS/过敏性休克/气管插管/癫痫持续状态 */
object EmergencyProcedures {
    data class Procedure(val title: String, val category: String, val indication: String, val steps: String)

    val all: List<Procedure> = listOf(
        // 心跳骤停
        Procedure("成人心脏骤停 ACLS 流程", "心跳骤停",
            "无反应+无呼吸或濒死喘息+无脉搏",
            "①启动急救(呼叫)→②CPR(100-120次/分,5-6cm深,胸廓完全回弹)交替30:2→③除颤仪到达:rhythm check→④VF/VT:立即除颤(双相200J)→⑤CPR 2min→⑥rhythm check→⑦肾上腺素1mg IV/IO q3-5min→⑧CAPNOGRAPHY(ETCO2>10确认CPR质量)"),
        Procedure("无脉电活动(PEA)处理", "心跳骤停",
            "心电有组织活动但无脉搏",
            "①CPR+肾上腺素1mg q3-5min→②查找可逆病因(H/T)：低血容量/缺氧/酸中毒/高/低钾/低温/张力性气胸/心包填塞/毒素/血栓(肺/冠脉)→③纠正病因→④不除颤"),
        Procedure("可除颤心律(VF/无脉VT)", "心跳骤停",
            "除颤器显示VF或VT无脉",
            "①立即除颤(200J)→②CPR 2min→③rhythm check→④仍VF/VT:再除颤(300J)→⑤CPR+肾上腺素→⑥胺碘酮300mg IV/IO(首剂)，150mg(次剂)"),

        // 过敏性休克
        Procedure("过敏性休克紧急处理", "过敏/休克",
            "暴露于过敏原后数分钟-数小时，出现皮疹/血管性水肿+呼吸困难/喘息+低血压",
            "①立即停过敏原/平卧抬腿→②肾上腺素0.3-0.5mg IM(大腿中外侧)，q5-15min可重复→③大通道IV快速输液(NS 1-2L)→④O2吸入→⑤辅助:苯海拉明50mg IV+甲泼尼龙125mg IV→⑥必要时肾上腺素IV(0.1mg缓慢推)"),
        Procedure("气道急症: 困难气道处理", "气道管理",
            "缺氧+通气/插管失败",
            "①叫帮助→②面罩100%O2→③口咽/鼻咽通气道→④LMA(喉罩)或I-gel→⑤环甲膜切开术(最后手段)→⑥每一步都评估氧合"),

        // 气管插管
        Procedure("快速序贯诱导(RSI)插管流程", "气管插管",
            "呼吸衰竭/气道保护/休克/心跳骤停",
            "准备(SOAPME): Suction/Oxygen/Airway/Positioning/Monitor/Equipment→预氧合(100%O2×3-5min)→诱导:依托咪酯0.3mg/kg或丙泊酚1-2mg/kg或氯胺酮1-2mg/kg→肌松:琥珀胆碱1-1.5mg/kg或罗库溴铵1.2mg/kg→45s后喉镜插管→确认ETT位置(ETCO2+双肺听诊+CXR)"),

        // 癫痫持续状态
        Procedure("癫痫持续状态处理(5min+)", "神经急症",
            "癫痫发作持续>5min或两次发作间意识未恢复",
            "0-5min: 保持气道/给O2/心电监护/IV通路→5-20min: 劳拉西泮4mg IV或地西泮10mg IV或咪达唑仑10mg IM/口腔→20-40min: 左乙拉西坦60mg/kg IV或苯妥英20mg/kg IV或丙戊酸40mg/kg IV→>40min: 咪达唑仑/丙泊酚/戊巴比妥持续泵入→气管插管+ICU"),
        Procedure("急性缺血性卒中溶栓流程", "神经急症/卒中",
            "发病<4.5h, NIHSS>4, 无禁忌症",
            "①非增强CT排除出血→②查凝血/血小板/血糖(>50)→③确认最后正常时间→④rt-PA 0.9mg/kg(最大90mg),10%推注,余量60min泵入→⑤避免抗血小板24h→⑥控制BP<185/110"),

        // 创伤
        Procedure("创伤初级评估(ATLS ABCDE)", "创伤",
            "严重创伤/多发伤",
            "A(气道+颈保护):打开气道/吸引/口咽通气道/颈托→B(呼吸):视触叩听/SPO2/必要时气管插管→C(循环):止血/IV大通道/FAST/交叉配血/启动MTP→D(神经):GCS/瞳孔/血糖→E(暴露):全身检查/保温"),

        // 心脏急症
        Procedure("STEMI 再灌注流程", "ACS",
            "胸痛+ECG ST抬高>1mm(2连续导联)",
            "①ASP 300mg咀嚼+替格瑞洛180mg→②镇痛:吗啡3-5mg IV→③β阻滞(无禁忌):美托洛尔5mg IV q5min×3→④PCI capable hospital:直接PCI(D2B<90min)→⑤非PCI医院:溶栓(阿替普酶/替奈普酶)→⑥维持肝素抗凝"),

        // 脓毒症
        Procedure("脓毒症1小时集束化治疗", "脓毒症/SSC",
            "感染+SOFA≥2(qSOFA≥2,乳酸>2)",
            "①血培养×2(/尿/痰/伤口)→②测乳酸→③广谱抗生素(1h内)→④晶体液30mL/kg(低血压或乳酸≥4)→⑤升压药(液体无效):去甲肾上腺素→⑥每6h查乳酸直至正常"),

        // 肺栓塞
        Procedure("高危肺栓塞(大面积)", "PE/循环",
            "确诊PE+持续性低血压(SBP<90>40min)或心跳骤停",
            "①肝素80U/kg IV推注+18U/kg/h泵入→②溶栓:阿替普酶100mg IV/2h→③血流动力学不稳定+溶栓禁忌/无效:导管介入或外科取栓→④ECMO备选"),
    )

    val categories = all.map { it.category }.distinct().sorted()
    fun search(q: String) = all.filter {
        val lq = q.lowercase()
        it.title.lowercase().contains(lq) || it.steps.lowercase().contains(lq) || it.category.lowercase().contains(lq)
    }
}
