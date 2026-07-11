package com.mynote.android.util

/** 心电图速查 — 常见 ECG 表现与判读 */
object ECGReference {
    data class ECG(val title: String, val category: String, val description: String)

    val all: List<ECG> = listOf(
        // 正常
        ECG("正常窦性心律", "正常", "HR 60-100bpm，P波每QRS前一个、II导联直立、形态一致，PR间期120-200ms，QRS<110ms。"),
        // 心律失常
        ECG("心房颤动", "心律失常", "P波消失→代之以大小不一、形态不等f波(V1最清楚)，R-R间期绝对不齐。窄QRS(无束支阻滞)。控制心率/律率/抗凝。"),
        ECG("心房扑动", "心律失常", "P波消失→代之以锯齿状F波(下壁II/III/aVF最清楚)，频率250-350bpm，常见2:1/3:1下传比。无等电位线。"),
        ECG("阵发性室上性心动过速(PSVT)", "心律失常", "窄QRS(<120ms)+固定不变R-R间期+150-250bpm，P波多隐藏在T波内不易辨认。"),
        ECG("室性心动过速(VT)", "心律失常", "宽QRS(>120ms)+RBBB或LBBB型+VVIrregular(房室分离+融合波/夺获。鉴别室速vs室上速+宽QRS：Brugada分步法。"),
        ECG("心室颤动(VF)", "心律失常", "完全无辨识QRS-T，呈不规则快速不规则的振幅变化→心脏骤停(无脉)。"),
        ECG("窦性心动过缓", "心律失常", "HR<60bpm，P-QRS-T正常顺序。运动员/睡眠/下壁MI/病窦。"),
        ECG("窦性心动过速", "心律失常", "HR>100bpm(成人)，P波直立，PR正常。感染/焦虑/低血容量/贫血/PE。"),
        ECG("预激综合征(WPW)", "心律失常/预激", "短PR(<120ms)+δ波(起始部有切迹)+宽QRS(>110ms)。VPCS型A(左旁路)/B(右旁路)。"),
        ECG("房室传导阻滞I度", "传导阻滞", "PR间期>200ms(恒定)，每个P波下传，1:1。无症状者常见。"),
        ECG("房室传导阻滞II度I型(文氏)", "传导阻滞", "PR间期逐渐延长直至QRS脱漏一次，PR从减小后重新开始周期。QRS正常。"),
        ECG("房室传导阻滞II度II型(Mobitz II)", "传导阻滞", "PR固定(正常或延长)+间歇P波不下传(无QRS)。QRS通常宽。→起搏器。"),
        ECG("III度房室传导阻滞", "传导阻滞", "P波和QRS完全无关+P-P规则且频率快于R-R，QRS为逸搏(交界/室)。心率<50→起搏器。"),
        ECG("右束支传导阻滞(RBBB)", "传导阻滞", "QRS>120ms+V1-V2呈rSR'型(R'约15mm)+I/V6导联宽而模糊S波(>R时间)。继发ST-T改变(前向性)。"),
        ECG("左束支传导阻滞(LBBB)", "传导阻滞", "QRS≥120ms+V5-V6导联R波宽大切迹且无Q波(单向)，V1小r或无r(<5mm)。继发ST-T改变。"),
        // 心肌梗死/缺血
        ECG("前间壁STEMI", "ACS/心肌梗死", "V1-V3 ST段抬高≥2mm(40岁以下男性≥2.5mm)，对应导联无深Q。LAD近端闭塞(近第一间隔支)。"),
        ECG("前壁STEMI", "ACS/心肌梗死", "V3-V4 ST≥2mm(延至V5可能)。LAD中段远端阻塞→前壁/心尖腹侧。"),
        ECG("下壁STEMI", "ACS/心肌梗死", "II/III/aVF ST≥1mm。RCA(80%)或回旋支(20%)。合并V4R ST抬高=右室梗死(RCA近端)。"),
        ECG("侧壁STEMI", "ACS/心肌梗死", "I/aVL/V5-V6 ST≥1mm。回旋支闭塞。"),
        ECG("后壁心肌梗死", "ACS/心肌梗死", "V7-V9 ST≥1mm(常规ECG看不到)。V1-V2呈高大R波(镜像异常)。需加做后壁导联。"),
        ECG("非ST抬高心肌梗死(NSTEMI)", "ACS/心肌梗死", "ST压低≥0.5mm(至少两相邻导联)+T波倒置+肌钙蛋白升高+无ST抬高。动态演变重要。"),
        ECG("Wellens综合征", "ACS", "V2-V3 T波双向或深倒T(≥5mm)+无/微ST偏移→LAD近端临界狭窄(CAG紧急)。"),
        ECG("De Winter征", "ACS", "V1-V6 J点下移+高尖对称T波+胸前导联无ST抬高。提示LAD闭塞。"),
        // 电解质异常
        ECG("高钾血症", "电解质", "T波高尖(帐篷状)→P波振幅降低/消失→QRS增宽→正弦波→VF。K+>5.5可见。紧急治疗。"),
        ECG("低钾血症", "电解质", "ST压低+T波倒置/低平+明显U波(>1mm)+QTU延长。易诱发TdP。"),
        ECG("高钙血症", "电解质", "QT间期缩短(ST段消失几乎直接连T波)。Osborn波(低体温也可见)。"),
        ECG("低钙血症", "电解质", "ST段延长+QTU间期延长(主要由于ST>QTc)。可见T波正常大小。"),
        // 其他常见
        ECG("心脏起搏器(单腔/右室)", "起搏器", "QRS宽+前有起搏钉(spike)+电轴左偏(心尖部起搏)。V1呈LBBB形态(RV起搏)。"),
        ECG("双腔起搏器", "起搏器", "房室顺序起搏(心房+心室)，P波前有起搏钉及QRS前有起搏钉，顺序各导联一致。"),
        ECG("心包炎", "心包", "弥漫性ST弓背向下抬高(I/II/aVF/V3-V6)+PR段压低(2~30%患者)，无T波倒置。"),
        ECG("心包积液/心脏压塞", "心包", "低电压(QRS<5mm肢导联)+电交替(beat-to-beat)，诊断心脏压塞(紧急)。"),
        ECG("左心室肥厚(LVH)", "心肌肥厚", "Sokolow: SV1+RV5>35mm。Cornell: RaVL+SV3>28mm(男)/20mm(女)。伴劳损：ST+T改变(V5-V6)。"),
        ECG("肺源性心脏病/肺栓塞", "右心负荷", "S1Q3T3(S深I导+SIII/QIII+TIII倒置)，窦速，不完全/完全RBBB，右轴。"),
        ECG("早期复极(良性)", "正常变异", "J点抬高+ST段抬高<2mm(V3-V5最多)+ST弓形正常+T:ST>1。年轻男性常见。"),
        ECG("QT间期延长", "药物/遗传", "QTc>440ms(男)>460ms(女)。病因：Ia/Ic/III类抗心律失常药、大环内酯/氟喹诺酮/抗精神病药/抗组胺。"),
        ECG("洋地黄效应", "药物", "ST段下移呈'反向对号'+QT缩短。高浓度：多源室早/室速/心房颤动/房室传导阻滞。"),
    )

    val categories = all.map { it.category }.distinct().sorted()
    fun search(q: String) = all.filter {
        val lq = q.lowercase()
        it.title.lowercase().contains(lq) || it.description.lowercase().contains(lq) || it.category.lowercase().contains(lq)
    }
}
