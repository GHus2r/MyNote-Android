package com.mynote.android.util

/** 常见中毒与解毒速查 */
object ToxicologyRef {
    data class Tox(val agent: String, val category: String, val toxidrome: String, val antidote: String, val keyPoints: String)

    val all: List<Tox> = listOf(
        Tox("对乙酰氨基酚(扑热息痛)", "药物中毒",
            "0-24h: 无症状/恶心呕吐\n24-72h: RUQ痛/AST/ALT↑(最严重)\n72-96h: 肝衰竭高峰/黄疸/脑病/凝血异常",
            "N-乙酰半胱氨酸(NAC)\n150mg/kg IV(1h)+50mg/kg(4h)+100mg/kg(16h)\nPO:140mg/kg+70mg/kg q4h×17",
            "摄入后4h测血浓度(Rumack-Matthew nomogram判断肝毒性风险)\nNAC在8h内给药最有效;>24h仍有效"),
        Tox("阿司匹林(水杨酸)", "药物中毒",
            "耳鸣/眩晕/过度通气(呼吸性碱中毒)/代谢性酸中毒/高热/意识改变",
            "碱化尿液:NaHCO₃(target尿pH 7.5-8)\n严重:血液透析",
            "Done nomogram不可靠→测系列水杨酸浓度\n血液透析指征:浓度>100mg/dL/严重酸中毒/意识改变/肾衰"),
        Tox("有机磷农药", "农药中毒",
            "胆碱能危象(SLUDGE): 流涎/流泪/排尿/腹泻/胃肠道痉挛/呕吐 + 瞳孔缩小/肌束震颤/心动过缓/呼吸困难",
            "阿托品 2-5mg IV q5-15min至双肺湿啰音消失(阿托品化)\n解磷定(2-PAM)1-2g IV(30min内)",
            "阿托品→控制气道分泌物\n2-PAM→在不可逆老化前<24h给最有效"),
        Tox("百草枯", "农药中毒/高致死",
            "急性:消化道腐蚀/口腔溃疡(假膜)\n数天-数周:肺纤维化(不可逆)→呼吸衰竭",
            "无特效解毒药!\n立即:活性炭+灌洗\n免疫抑制:甲泼尼龙+环磷酰胺(争议)",
            "高致死率(>70%),任何剂量均可能致命\n任何途径(口服/皮肤/吸入)均严重"),
        Tox("氰化物", "快速致死毒物",
            "数秒-数分钟:头痛/意识改变/抽搐→心血管崩溃→死亡\n火焰烟雾吸入/苦杏仁味呼吸",
            "羟钴胺素(Cyanokit)5g IV(一线)\n次选:亚硝酸钠300mg IV+硫代硫酸钠12.5g IV",
            "火焰现场烟雾吸入应测乳酸(>10mmol/L提示氰化物)\n同时处理CO中毒"),
        Tox("一氧化碳(CO)", "气体中毒",
            "流感样:头痛/恶心/眩晕\n严重:意识改变/心肌缺血/乳酸酸中毒\n皮肤樱桃红(罕见)",
            "100% O₂→高压氧(HBO)\nHBO指征:COHb>25%/意识改变/心肌缺血/妊娠(COHb>15%)",
            "脉氧仪不可靠(COHb与O₂Hb不能区分)→必须测血COHb"),
        Tox("地高辛", "药物中毒/心脏",
            "慢性:恶心/视觉异常(黄视)/心律失常(PAT with block/双向VT)\n急性:高钾(>5.5mmol/L=严重中毒)",
            "地高辛免疫Fab(Digibind/DigiFab)\n剂量:vial数≈血浓度(ng/mL)×体重(kg)/100",
            "高钾+地高辛中毒:避免钙剂IV! (may cause 'stone heart')\nMg²⁺+利多卡因治疗室性心律失常"),
        Tox("β阻滞剂/钙通道阻滞剂过量", "药物中毒/心脏",
            "β阻滞剂:心动过缓/低血压/低血糖\nCCB(维拉帕米/地尔硫卓):心动过缓/心脏阻滞/低血压/高血糖",
            "一线:高剂量胰岛素euglycemia(DIK):insulin 1U/kg IV+50%Glucose→泵入\n升压药:去甲/肾上腺素→无效可用亚甲蓝",
            "胰高血糖素:用于β阻滞剂,5-10mg IV\n钙:葡萄糖酸钙3g或氯化钙1g IV(每10-20min)\n静脉脂肪乳(ILE)20%:1.5mL/kg IV→o.25mL/kg/min"),
        Tox("三环类抗抑郁药(TCA)", "药物中毒",
            "抗胆碱能:高热/皮肤干燥/瞳孔散大/谵妄\n心脏:QRS增宽>100ms/QTc延长/室性心律失常\n癫痫/昏迷",
            "NaHCO₃ 1-2mEq/kg IV bolus(target pH 7.5-7.55)→维持泵入",
            "QRS>100ms=严重中毒\n处理代谢性酸中毒→使QRS缩窄; 不预防性给碳酸氢盐"),
        Tox("苯二氮卓类(BZD)", "药物中毒/镇静",
            "嗜睡/言语不清/共济失调/呼吸抑制(严重)\n氟马西尼可诱撤药",
            "支持治疗(气道/呼吸/循环)→一般不推荐氟马西尼",
            "氟马西尼(0.2mg IV):仅用于非依赖者的严重中毒(但可致急性惊厥/戒断危象)\n混合中毒(尤其TCA/BZD):碳氢酸盐优先"),
        Tox("阿片类(海洛因/芬太尼/吗啡)", "药物中毒",
            "瞳孔针尖大+呼吸抑制(<12次/分)+意识水平↓\n针眼(IVDU)",
            "纳洛酮0.4-2mg IV/IM q2-3min→最大10mg",
            "纳洛酮Amp:1 Amp = 0.4mg\n使用前确保气道,防呕吐误吸\n纳洛酮半衰期短→持续泵入"),
        Tox("甲醇/乙二醇(防冻液)", "醇类中毒",
            "早期(0-6h): CNS抑制/醉酒样\n6-24h:高AG代谢性酸中毒+渗透压差↑\n甲醇→视力模糊/失明\n乙二醇→草酸钙尿/肾衰竭",
            "甲吡唑(Fomepizole)15mg/kg IV→10mg/kg q12h×4→15mg/kg q12h(抑制醇脱氢酶)\n乙醇(替代):10%乙醇IV(负荷+维持)\n严重/肾衰竭:血液透析",
            "测血醇浓度+血浆渗透压差(calc Osm-实测Osm>10mosm)\n尿检:针状草酸钙结晶(乙二醇)"),
        Tox("蘑菇中毒-鹅膏毒蕈(死亡帽)", "天然毒物",
            "6-24h潜伏→胃肠炎→24-72h肝损害(急剧ALT↑)→肝衰竭→死亡\n典型:症状→假性恢复→肝衰竭",
            "水飞蓟素(水飞蓟)20-50mg/kg/d IV\nNAC IV(同APAP方案)\n活性炭反复灌洗\n严重:肝移植评估",
            "症状改善后1-2天出现肝酶急剧升高→勿被\"假性恢复\"欺骗"),

        // 重金属
        Tox("铅中毒", "重金属",
            "儿童:厌食/腹痛/便秘/神经行为异常/脑病(昏迷/抽搐)\n成人:腹痛/周围神经病变/牙龈铅线/贫血",
            "EDTA二钠钙或二巯基丁二酸(DMSA)\n严重(铅脑病):二巯基丙醇(BAL/British Anti-Lewisite)",
            "血铅≥5μg/dL→阳性(CDC参考)\n儿童血铅≥45μg/dL→螯合治疗"),
    )

    val categories = all.map { it.category }.distinct().sorted()
    fun search(q: String) = all.filter {
        val lq = q.lowercase()
        it.agent.lowercase().contains(lq) || it.toxidrome.lowercase().contains(lq) || it.antidote.lowercase().contains(lq) || it.category.lowercase().contains(lq)
    }
}
