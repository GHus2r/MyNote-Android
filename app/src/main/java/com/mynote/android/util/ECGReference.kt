package com.mynote.android.util

/** 心电图速查 — 常见 ECG 表现与判读 */
object ECGReference {
    data class ECG(val title: String, val category: String, val description: String, val url: String = "")

    val all: List<ECG> = listOf(
        // 正常
        ECG("正常窦性心律", "正常", "HR 60-100bpm，P波每QRS前一个、II导联直立、形态一致，PR间期120-200ms，QRS<110ms。", "https://cn.bing.com/images/search?q=%E5%BF%83%E7%94%B5%E5%9B%BE%20%E6%AD%A3%E5%B8%B8%E7%AA%A6%E6%80%A7%E5%BF%83%E5%BE%8B"),
        // 心律失常
        ECG("心房颤动", "心律失常", "P波消失→代之以大小不一、形态不等f波(V1最清楚)，R-R间期绝对不齐。窄QRS(无束支阻滞)。控制心率/律率/抗凝。", "https://cn.bing.com/images/search?q=%E5%BF%83%E7%94%B5%E5%9B%BE%20%E5%BF%83%E6%88%BF%E9%A2%A4%E5%8A%A8"),
        ECG("心房扑动", "心律失常", "P波消失→代之以锯齿状F波(下壁II/III/aVF最清楚)，频率250-350bpm，常见2:1/3:1下传比。无等电位线。", "https://cn.bing.com/images/search?q=%E5%BF%83%E7%94%B5%E5%9B%BE%20%E5%BF%83%E6%88%BF%E6%89%91%E5%8A%A8"),
        ECG("阵发性室上性心动过速(PSVT)", "心律失常", "窄QRS(<120ms)+固定不变R-R间期+150-250bpm，P波多隐藏在T波内不易辨认。", "https://cn.bing.com/images/search?q=%E5%BF%83%E7%94%B5%E5%9B%BE%20%E9%98%B5%E5%8F%91%E6%80%A7%E5%AE%A4%E4%B8%8A%E6%80%A7%E5%BF%83%E5%8A%A8%E8%BF%87%E9%80%9F%28PSVT%29"),
        ECG("室性心动过速(VT)", "心律失常", "宽QRS(>120ms)+RBBB或LBBB型+VVIrregular(房室分离+融合波/夺获。鉴别室速vs室上速+宽QRS：Brugada分步法。", "https://cn.bing.com/images/search?q=%E5%BF%83%E7%94%B5%E5%9B%BE%20%E5%AE%A4%E6%80%A7%E5%BF%83%E5%8A%A8%E8%BF%87%E9%80%9F%28VT%29"),
        ECG("心室颤动(VF)", "心律失常", "完全无辨识QRS-T，呈不规则快速不规则的振幅变化→心脏骤停(无脉)。", "https://cn.bing.com/images/search?q=%E5%BF%83%E7%94%B5%E5%9B%BE%20%E5%BF%83%E5%AE%A4%E9%A2%A4%E5%8A%A8%28VF%29"),
        ECG("窦性心动过缓", "心律失常", "HR<60bpm，P-QRS-T正常顺序。运动员/睡眠/下壁MI/病窦。", "https://cn.bing.com/images/search?q=%E5%BF%83%E7%94%B5%E5%9B%BE%20%E7%AA%A6%E6%80%A7%E5%BF%83%E5%8A%A8%E8%BF%87%E7%BC%93"),
        ECG("窦性心动过速", "心律失常", "HR>100bpm(成人)，P波直立，PR正常。感染/焦虑/低血容量/贫血/PE。", "https://cn.bing.com/images/search?q=%E5%BF%83%E7%94%B5%E5%9B%BE%20%E7%AA%A6%E6%80%A7%E5%BF%83%E5%8A%A8%E8%BF%87%E9%80%9F"),
        ECG("预激综合征(WPW)", "心律失常/预激", "短PR(<120ms)+δ波(起始部有切迹)+宽QRS(>110ms)。VPCS型A(左旁路)/B(右旁路)。", "https://cn.bing.com/images/search?q=%E5%BF%83%E7%94%B5%E5%9B%BE%20%E9%A2%84%E6%BF%80%E7%BB%BC%E5%90%88%E5%BE%81%28WPW%29"),
        ECG("房室传导阻滞I度", "传导阻滞", "PR间期>200ms(恒定)，每个P波下传，1:1。无症状者常见。", "https://cn.bing.com/images/search?q=%E5%BF%83%E7%94%B5%E5%9B%BE%20%E6%88%BF%E5%AE%A4%E4%BC%A0%E5%AF%BC%E9%98%BB%E6%BB%9EI%E5%BA%A6"),
        ECG("房室传导阻滞II度I型(文氏)", "传导阻滞", "PR间期逐渐延长直至QRS脱漏一次，PR从减小后重新开始周期。QRS正常。", "https://cn.bing.com/images/search?q=%E5%BF%83%E7%94%B5%E5%9B%BE%20%E6%88%BF%E5%AE%A4%E4%BC%A0%E5%AF%BC%E9%98%BB%E6%BB%9EII%E5%BA%A6I%E5%9E%8B%28%E6%96%87%E6%B0%8F%29"),
        ECG("房室传导阻滞II度II型(Mobitz II)", "传导阻滞", "PR固定(正常或延长)+间歇P波不下传(无QRS)。QRS通常宽。→起搏器。", "https://cn.bing.com/images/search?q=%E5%BF%83%E7%94%B5%E5%9B%BE%20%E6%88%BF%E5%AE%A4%E4%BC%A0%E5%AF%BC%E9%98%BB%E6%BB%9EII%E5%BA%A6II%E5%9E%8B%28Mobitz%20II%29"),
        ECG("III度房室传导阻滞", "传导阻滞", "P波和QRS完全无关+P-P规则且频率快于R-R，QRS为逸搏(交界/室)。心率<50→起搏器。", "https://cn.bing.com/images/search?q=%E5%BF%83%E7%94%B5%E5%9B%BE%20III%E5%BA%A6%E6%88%BF%E5%AE%A4%E4%BC%A0%E5%AF%BC%E9%98%BB%E6%BB%9E"),
        ECG("右束支传导阻滞(RBBB)", "传导阻滞", "QRS>120ms+V1-V2呈rSR'型(R'约15mm)+I/V6导联宽而模糊S波(>R时间)。继发ST-T改变(前向性)。", "https://cn.bing.com/images/search?q=%E5%BF%83%E7%94%B5%E5%9B%BE%20%E5%8F%B3%E6%9D%9F%E6%94%AF%E4%BC%A0%E5%AF%BC%E9%98%BB%E6%BB%9E%28RBBB%29"),
        ECG("左束支传导阻滞(LBBB)", "传导阻滞", "QRS≥120ms+V5-V6导联R波宽大切迹且无Q波(单向)，V1小r或无r(<5mm)。继发ST-T改变。", "https://cn.bing.com/images/search?q=%E5%BF%83%E7%94%B5%E5%9B%BE%20%E5%B7%A6%E6%9D%9F%E6%94%AF%E4%BC%A0%E5%AF%BC%E9%98%BB%E6%BB%9E%28LBBB%29"),
        // 心肌梗死/缺血
        ECG("前间壁STEMI", "ACS/心肌梗死", "V1-V3 ST段抬高≥2mm(40岁以下男性≥2.5mm)，对应导联无深Q。LAD近端闭塞(近第一间隔支)。", "https://cn.bing.com/images/search?q=%E5%BF%83%E7%94%B5%E5%9B%BE%20%E5%89%8D%E9%97%B4%E5%A3%81STEMI"),
        ECG("前壁STEMI", "ACS/心肌梗死", "V3-V4 ST≥2mm(延至V5可能)。LAD中段远端阻塞→前壁/心尖腹侧。", "https://cn.bing.com/images/search?q=%E5%BF%83%E7%94%B5%E5%9B%BE%20%E5%89%8D%E5%A3%81STEMI"),
        ECG("下壁STEMI", "ACS/心肌梗死", "II/III/aVF ST≥1mm。RCA(80%)或回旋支(20%)。合并V4R ST抬高=右室梗死(RCA近端)。", "https://cn.bing.com/images/search?q=%E5%BF%83%E7%94%B5%E5%9B%BE%20%E4%B8%8B%E5%A3%81STEMI"),
        ECG("侧壁STEMI", "ACS/心肌梗死", "I/aVL/V5-V6 ST≥1mm。回旋支闭塞。", "https://cn.bing.com/images/search?q=%E5%BF%83%E7%94%B5%E5%9B%BE%20%E4%BE%A7%E5%A3%81STEMI"),
        ECG("后壁心肌梗死", "ACS/心肌梗死", "V7-V9 ST≥1mm(常规ECG看不到)。V1-V2呈高大R波(镜像异常)。需加做后壁导联。", "https://cn.bing.com/images/search?q=%E5%BF%83%E7%94%B5%E5%9B%BE%20%E5%90%8E%E5%A3%81%E5%BF%83%E8%82%8C%E6%A2%97%E6%AD%BB"),
        ECG("非ST抬高心肌梗死(NSTEMI)", "ACS/心肌梗死", "ST压低≥0.5mm(至少两相邻导联)+T波倒置+肌钙蛋白升高+无ST抬高。动态演变重要。", "https://cn.bing.com/images/search?q=%E5%BF%83%E7%94%B5%E5%9B%BE%20%E9%9D%9EST%E6%8A%AC%E9%AB%98%E5%BF%83%E8%82%8C%E6%A2%97%E6%AD%BB%28NSTEMI%29"),
        ECG("Wellens综合征", "ACS", "V2-V3 T波双向或深倒T(≥5mm)+无/微ST偏移→LAD近端临界狭窄(CAG紧急)。", "https://cn.bing.com/images/search?q=%E5%BF%83%E7%94%B5%E5%9B%BE%20Wellens%E7%BB%BC%E5%90%88%E5%BE%81"),
        ECG("De Winter征", "ACS", "V1-V6 J点下移+高尖对称T波+胸前导联无ST抬高。提示LAD闭塞。", "https://cn.bing.com/images/search?q=%E5%BF%83%E7%94%B5%E5%9B%BE%20De%20Winter%E5%BE%81"),
        // 电解质异常
        ECG("高钾血症", "电解质", "T波高尖(帐篷状)→P波振幅降低/消失→QRS增宽→正弦波→VF。K+>5.5可见。紧急治疗。", "https://cn.bing.com/images/search?q=%E5%BF%83%E7%94%B5%E5%9B%BE%20%E9%AB%98%E9%92%BE%E8%A1%80%E7%97%87"),
        ECG("低钾血症", "电解质", "ST压低+T波倒置/低平+明显U波(>1mm)+QTU延长。易诱发TdP。", "https://cn.bing.com/images/search?q=%E5%BF%83%E7%94%B5%E5%9B%BE%20%E4%BD%8E%E9%92%BE%E8%A1%80%E7%97%87"),
        ECG("高钙血症", "电解质", "QT间期缩短(ST段消失几乎直接连T波)。Osborn波(低体温也可见)。", "https://cn.bing.com/images/search?q=%E5%BF%83%E7%94%B5%E5%9B%BE%20%E9%AB%98%E9%92%99%E8%A1%80%E7%97%87"),
        ECG("低钙血症", "电解质", "ST段延长+QTU间期延长(主要由于ST>QTc)。可见T波正常大小。", "https://cn.bing.com/images/search?q=%E5%BF%83%E7%94%B5%E5%9B%BE%20%E4%BD%8E%E9%92%99%E8%A1%80%E7%97%87"),
        // 其他常见
        ECG("心脏起搏器(单腔/右室)", "起搏器", "QRS宽+前有起搏钉(spike)+电轴左偏(心尖部起搏)。V1呈LBBB形态(RV起搏)。", "https://cn.bing.com/images/search?q=%E5%BF%83%E7%94%B5%E5%9B%BE%20%E5%BF%83%E8%84%8F%E8%B5%B7%E6%90%8F%E5%99%A8%28%E5%8D%95%E8%85%94/%E5%8F%B3%E5%AE%A4%29"),
        ECG("双腔起搏器", "起搏器", "房室顺序起搏(心房+心室)，P波前有起搏钉及QRS前有起搏钉，顺序各导联一致。", "https://cn.bing.com/images/search?q=%E5%BF%83%E7%94%B5%E5%9B%BE%20%E5%8F%8C%E8%85%94%E8%B5%B7%E6%90%8F%E5%99%A8"),
        ECG("心包炎", "心包", "弥漫性ST弓背向下抬高(I/II/aVF/V3-V6)+PR段压低(2~30%患者)，无T波倒置。", "https://cn.bing.com/images/search?q=%E5%BF%83%E7%94%B5%E5%9B%BE%20%E5%BF%83%E5%8C%85%E7%82%8E"),
        ECG("心包积液/心脏压塞", "心包", "低电压(QRS<5mm肢导联)+电交替(beat-to-beat)，诊断心脏压塞(紧急)。", "https://cn.bing.com/images/search?q=%E5%BF%83%E7%94%B5%E5%9B%BE%20%E5%BF%83%E5%8C%85%E7%A7%AF%E6%B6%B2/%E5%BF%83%E8%84%8F%E5%8E%8B%E5%A1%9E"),
        ECG("左心室肥厚(LVH)", "心肌肥厚", "Sokolow: SV1+RV5>35mm。Cornell: RaVL+SV3>28mm(男)/20mm(女)。伴劳损：ST+T改变(V5-V6)。", "https://cn.bing.com/images/search?q=%E5%BF%83%E7%94%B5%E5%9B%BE%20%E5%B7%A6%E5%BF%83%E5%AE%A4%E8%82%A5%E5%8E%9A%28LVH%29"),
        ECG("肺源性心脏病/肺栓塞", "右心负荷", "S1Q3T3(S深I导+SIII/QIII+TIII倒置)，窦速，不完全/完全RBBB，右轴。", "https://cn.bing.com/images/search?q=%E5%BF%83%E7%94%B5%E5%9B%BE%20%E8%82%BA%E6%BA%90%E6%80%A7%E5%BF%83%E8%84%8F%E7%97%85/%E8%82%BA%E6%A0%93%E5%A1%9E"),
        ECG("早期复极(良性)", "正常变异", "J点抬高+ST段抬高<2mm(V3-V5最多)+ST弓形正常+T:ST>1。年轻男性常见。", "https://cn.bing.com/images/search?q=%E5%BF%83%E7%94%B5%E5%9B%BE%20%E6%97%A9%E6%9C%9F%E5%A4%8D%E6%9E%81%28%E8%89%AF%E6%80%A7%29"),
        ECG("QT间期延长", "药物/遗传", "QTc>440ms(男)>460ms(女)。病因：Ia/Ic/III类抗心律失常药、大环内酯/氟喹诺酮/抗精神病药/抗组胺。", "https://cn.bing.com/images/search?q=%E5%BF%83%E7%94%B5%E5%9B%BE%20QT%E9%97%B4%E6%9C%9F%E5%BB%B6%E9%95%BF"),
        ECG("洋地黄效应", "药物", "ST段下移呈'反向对号'+QT缩短。高浓度：多源室早/室速/心房颤动/房室传导阻滞。", "https://cn.bing.com/images/search?q=%E5%BF%83%E7%94%B5%E5%9B%BE%20%E6%B4%8B%E5%9C%B0%E9%BB%84%E6%95%88%E5%BA%94"),
        // 离子通道病
        ECG("Brugada综合征 1型", "离子通道病", "V1-V2弓背向上ST段抬高>2mm+T波倒置。1型(典型)/2型(马鞍)+多形VT/VF。", "https://cn.bing.com/images/search?q=%E5%BF%83%E7%94%B5%E5%9B%BE%20Brugada%E7%BB%BC%E5%90%88%E5%BE%81%201%E5%9E%8B"),
        ECG("Brugada综合征 2型", "离子通道病", "V1-V2鞍背形ST抬高>2mm，T波正向或双向。3型为表现不全，1型最具诊断价值。", "https://cn.bing.com/images/search?q=%E5%BF%83%E7%94%B5%E5%9B%BE%20Brugada%E7%BB%BC%E5%90%88%E5%BE%81%202%E5%9E%8B"),
        ECG("长QT综合征(LQTS)", "离子通道病", "QTc>500ms或>480ms+不明原因晕厥。T波交替(TWA)=恶性心律失常前兆。LQT1(运动)/2(惊吓)/3(睡眠)。", "https://cn.bing.com/images/search?q=%E5%BF%83%E7%94%B5%E5%9B%BE%20%E9%95%BFQT%E7%BB%BC%E5%90%88%E5%BE%81%28LQTS%29"),
        ECG("短QT综合征(SQTS)", "离子通道病", "QTc<300ms+T波高尖对称。高猝死风险(房颤+室颤，年轻)。ICD一线。", "https://cn.bing.com/images/search?q=%E5%BF%83%E7%94%B5%E5%9B%BE%20%E7%9F%ADQT%E7%BB%BC%E5%90%88%E5%BE%81%28SQTS%29"),
        ECG("儿茶酚胺敏感性VT(CPVT)", "离子通道病", "基础ECG正常→运动/情绪→双向/多形VT。青年猝死原因，治疗:β阻滞剂(纳多洛尔)。", "https://cn.bing.com/images/search?q=%E5%BF%83%E7%94%B5%E5%9B%BE%20%E5%84%BF%E8%8C%B6%E9%85%9A%E8%83%BA%E6%95%8F%E6%84%9F%E6%80%A7VT%28CPVT%29"),
        // 心肌病
        ECG("肥厚型心肌病(HCM)", "心肌病", "LVH电压+Sokolow>35+下侧壁深Q波(假梗死Q)+T波巨大倒置(心尖HCM,TV4-V6>1mV)。", "https://cn.bing.com/images/search?q=%E5%BF%83%E7%94%B5%E5%9B%BE%20%E8%82%A5%E5%8E%9A%E5%9E%8B%E5%BF%83%E8%82%8C%E7%97%85%28HCM%29"),
        ECG("致心律失常性右室心肌病(ARVC)", "心肌病", "V1-V3 T波倒置+Epsilon波(约30%,QRS终末小切迹)+RBBB+晚电位阳性。", "https://cn.bing.com/images/search?q=%E5%BF%83%E7%94%B5%E5%9B%BE%20%E8%87%B4%E5%BF%83%E5%BE%8B%E5%A4%B1%E5%B8%B8%E6%80%A7%E5%8F%B3%E5%AE%A4%E5%BF%83%E8%82%8C%E7%97%85%28ARVC%29"),
        // 更多 ACS
        ECG("后壁STEMI(V7-V9)", "ACS", "V7-V9 ST>=1mm。V1-V2 ST压低+高大R波+直立T波(镜像)。回旋支/右后降支闭塞。", "https://cn.bing.com/images/search?q=%E5%BF%83%E7%94%B5%E5%9B%BE%20%E5%90%8E%E5%A3%81STEMI%28V7-V9%29"),
        ECG("De Winter征", "ACS", "V1-V6 J点压低(上斜)+高尖对称T波。LAD近端闭塞(=STEMI等危)，高致死风险。", "https://cn.bing.com/images/search?q=%E5%BF%83%E7%94%B5%E5%9B%BE%20De%20Winter%E5%BE%81"),
        // 起搏器
        ECG("双腔起搏器 房室顺序", "起搏器", "房室顺序起搏(心房+心室)，P波前及QRS前各有起搏钉，顺序各导联一致。", "https://cn.bing.com/images/search?q=%E5%BF%83%E7%94%B5%E5%9B%BE%20%E5%8F%8C%E8%85%94%E8%B5%B7%E6%90%8F%E5%99%A8%20%E6%88%BF%E5%AE%A4%E9%A1%BA%E5%BA%8F"),
        ECG("起搏器介导性心动过速(PMT)", "起搏器", "起搏器参与折返性心动过速→无限循环快速心室起搏(上限频率)。磁铁终止。", "https://cn.bing.com/images/search?q=%E5%BF%83%E7%94%B5%E5%9B%BE%20%E8%B5%B7%E6%90%8F%E5%99%A8%E4%BB%8B%E5%AF%BC%E6%80%A7%E5%BF%83%E5%8A%A8%E8%BF%87%E9%80%9F%28PMT%29"),
        ECG("房室结折返性心动过速(AVNRT)", "心律失常", "窄QRS+固定R-R+150-250bpm，P波多隐藏在QRS内或假r'波(V1)。迷走刺激可终止。", "https://cn.bing.com/images/search?q=%E5%BF%83%E7%94%B5%E5%9B%BE%20%E6%88%BF%E5%AE%A4%E7%BB%93%E6%8A%98%E8%BF%94%E6%80%A7%E5%BF%83%E5%8A%A8%E8%BF%87%E9%80%9F%28AVNRT%29"),
        // 低温/其他
        ECG("低体温(Osborn波)", "环境/低温", "J波(Osborn波)+QRS延长+QT延长+基线震颤伪差+T<32℃出现。除颤无效至复温。", "https://cn.bing.com/images/search?q=%E5%BF%83%E7%94%B5%E5%9B%BE%20%E4%BD%8E%E4%BD%93%E6%B8%A9%28Osborn%E6%B3%A2%29"),
        ECG("心包填塞(电交替)", "心包", "完全电交替(每跳QRS振幅明显变化)+低电压+窦速。'摇摆心'征，立即心包穿刺。", "https://cn.bing.com/images/search?q=%E5%BF%83%E7%94%B5%E5%9B%BE%20%E5%BF%83%E5%8C%85%E5%A1%AB%E5%A1%9E%28%E7%94%B5%E4%BA%A4%E6%9B%BF%29"),
        ECG("右位心", "正常变异/先心", "I导联P-QRS-T全倒置(I全负)，胸前导联R波递增反向。注意与左右手反接鉴别。", "https://cn.bing.com/images/search?q=%E5%BF%83%E7%94%B5%E5%9B%BE%20%E5%8F%B3%E4%BD%8D%E5%BF%83"),
        ECG("Sgarbossa标准(LBBB+MI)", "ACS/LBBB", "①ST抬高>=1mm与QRS主波同向=5分。②V1-V3 ST压低>=1mm=3分。③ST抬高>=5mm反向=2分。>=3分诊断AMI。", "https://cn.bing.com/images/search?q=%E5%BF%83%E7%94%B5%E5%9B%BE%20Sgarbossa%E6%A0%87%E5%87%86%28LBBB%2BMI%29"),
    )

    val categories = all.map { it.category }.distinct().sorted()
    fun search(q: String) = all.filter {
        val lq = q.lowercase()
        it.title.lowercase().contains(lq) || it.description.lowercase().contains(lq) || it.category.lowercase().contains(lq)
    }
}
