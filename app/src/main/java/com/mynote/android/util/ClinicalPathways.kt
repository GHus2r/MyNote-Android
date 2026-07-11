package com.mynote.android.util

/**
 * 临床路径 → 树形决策节点
 * 12条常见急诊/专科路径，2024-2026指南
 */
object ClinicalPathways {

    data class Node(val title: String, val content: String = "", val children: List<Node> = emptyList())

    val pathways = listOf(
        Node("一、胸痛（ACS 疑诊）", children = listOf(
            Node("1. 10分钟内完成ECG", content = "ST段抬高 → STEMI路径\n非ST抬高 → NSTE-ACS路径\n正常ECG → 继续观察"),
            Node("2. 心肌标志物", content = "hs-cTnI/hs-cTnT\n0h/1h或0h/2h检测\n升高 >99th URL → 诊断心肌损伤"),
            Node("3. STEMI 再灌注", content = "发病<12h → 急诊PCI (Door-to-Balloon≤90min)\n无PCI条件 → 溶栓 (Door-to-Needle≤30min)\n替奈普酶/阿替普酶"),
            Node("4. 抗栓治疗", content = "阿司匹林 300mg 嚼服 + 替格瑞洛 180mg\n抗凝：肝素/依诺肝素/比伐卢定"),
            Node("5. 长期管理", content = "DAPT 12个月\nβ-blocker + ACEI/ARB + 他汀\n心脏康复")
        )),
        Node("二、急性卒中", children = listOf(
            Node("1. FAST评估", content = "Face(面瘫) Arms(单侧无力) Speech(言语障碍) Time(发病时间)\nNIHSS评分"),
            Node("2. 急诊CT", content = "平扫CT排除出血 → 缺血性卒中\nCTP/CTA评估缺血半暗带"),
            Node("3. 溶栓适应症", content = "发病<4.5h → 阿替普酶/替奈普酶 静脉溶栓\n发病4.5-24h + 半暗带 → 取栓(DEFUSE-3/DAWN标准)"),
            Node("4. 二级预防", content = "抗血小板/抗凝(房颤)\n降压 LDL-C<1.8mmol/L\n康复训练")
        )),
        Node("三、脓毒症/感染性休克", children = listOf(
            Node("1. 1h集束化(SSC 2024)", content = "测乳酸 → 留血培养 → 广谱抗生素 → 晶体液30mL/kg → 升压药(MAP≥65mmHg)"),
            Node("2. 液体复苏", content = "晶体液首选(平衡液>生理盐水)\n动态评估：IVC变异度/被动抬腿/脉压变异"),
            Node("3. 血管活性药", content = "一线：去甲肾上腺素\n二线：血管加压素/肾上腺素\n正性肌力：多巴酚丁胺(心功能低下)"),
            Node("4. 抗生素策略", content = "1h内启用\n降阶梯：48-72h根据培养调整\nPCT指导停药")
        )),
        Node("四、急性心衰", children = listOf(
            Node("1. 分型", content = "暖干型 → 口服利尿剂\n冷湿型 → 正性肌力+升压\n暖湿型(最常见) → 利尿+扩血管"),
            Node("2. 药物治疗", content = "袢利尿剂 iv (呋塞米/托拉塞米)\n硝酸酯类 iv (NTG/硝普钠)\nSGLT2i 早期启用"),
            Node("3. GDMT 四联", content = "ARNI(沙库巴曲缬沙坦) + BB + MRA + SGLT2i\n出院前启动，滴定至目标剂量"),
            Node("4. 出院标准", content = "容量平衡、稳定口服药、无明显静息症状\nBNP下降>30%")
        )),
        Node("五、COPD 急性加重", children = listOf(
            Node("1. 分级", content = "轻度：SABA吸入\n中度：SABA+SAMA + 口服激素\n重度：住院 + 氧疗/无创通气"),
            Node("2. 药物", content = "支气管扩张剂(SABA+SAMA雾化)\n全身激素 泼尼松40mg×5d\n抗生素(脓痰/CRP升高/需通气者)"),
            Node("3. 无创通气指征", content = "pH<7.35, PaCO₂>45mmHg\n呼吸频率>24/min\n意识清楚、能配合"),
            Node("4. GOLD 2025更新", content = "三联吸入(LABA+LAMA+ICS)用于EOS>300\n血EOS指导ICS使用\n生物制剂(度普利尤单抗)")
        )),
        Node("六、急性胰腺炎", children = listOf(
            Node("1. 诊断", content = "腹痛+淀粉酶/脂肪酶>3倍上限+影像\nRanson/APACHE-II 评分"),
            Node("2. 液体复苏", content = "乳酸林格液 5-10mL/kg/h\n目标：BUN下降、尿量>0.5mL/kg/h"),
            Node("3. 营养", content = "轻症：24h内经口进食\n重症：48h内空肠营养(优于肠外)"),
            Node("4. 抗生素", content = "预防性抗生素 不推荐\n感染坏死 → 碳青霉烯类\nCT引导穿刺引流")
        )),
        Node("七、糖尿病管理路径", children = listOf(
            Node("1. 筛查", content = "FPG≥7.0 / OGTT 2h≥11.1 / HbA1c≥6.5%\n每年筛查并发症"),
            Node("2. ADA 2025分层", content = "ASCVD/CKD/HF → SGLT2i/GLP-1RA一线\n无合并症 → 二甲双胍一线\nHbA1c目标：<7%(个体化)"),
            Node("3. 心血管保护", content = "合并ASCVD → GLP-1RA(司美格鲁肽)\n合并HF/CKD → SGLT2i(达格列净/恩格列净)\n非甾体MRA(非奈利酮)用于CKD"),
            Node("4. 新型降糖药", content = "替尔泊肽(GIP/GLP-1双靶) 减重+降糖\nSGLT2i: 达格列净/恩格列净\nGLP-1RA: 司美格鲁肽/利拉鲁肽")
        )),
        Node("八、上消化道出血", children = listOf(
            Node("1. 风险评估", content = "Glasgow-Blatchford评分\n0-1分 → 门诊处理\n≥2分 → 住院/急诊内镜"),
            Node("2. 液体复苏", content = "晶体液 + 悬浮红细胞(Hb<70g/L或活动性出血时<80-90)\n目标SBP≥100mmHg"),
            Node("3. 药物", content = "PPI 大剂量 iv (奥美拉唑80mg+8mg/h)\n抗生素(肝硬化者 头孢曲松)\n特利加压素/奥曲肽(静脉曲张)"),
            Node("4. 内镜时机", content = "高危 → 24h内急诊胃镜\n静脉曲张 → 套扎/组织胶\n非静脉曲张 → 内镜止血+PPI")
        )),
        Node("九、急性肾损伤", children = listOf(
            Node("1. KDIGO分期", content = "1期：Cr↑1.5-1.9x / 尿量<0.5mL/kg/h×6h\n2期：Cr↑2.0-2.9x / 尿量<0.5×12h\n3期：Cr↑≥3x / Cr≥353.6 / 无尿>12h"),
            Node("2. 病因排查", content = "肾前性：纠正低灌注\n肾性：ATN/AIN/肾小球肾炎\n肾后性：解除梗阻"),
            Node("3. 处理", content = "停用肾毒性药物(NSAIDs/ACEi/ARB/氨基糖苷)\n维持MAP>65mmHg\n避免高钾/酸中毒/容量过负荷"),
            Node("4. RRT指征", content = "严重高钾/酸中毒/肺水肿/尿毒症脑病\n优先CRRT(血流动力学不稳定)")
        )),
        Node("十、社区获得性肺炎", children = listOf(
            Node("1. CURB-65评分", content = "C意识 U尿素>7mmol/L R≥30 B<90/60 年龄≥65\n0-1分 → 门诊  2分 → 住院  3-5分 → ICU"),
            Node("2. 抗生素选择", content = "门诊：阿莫西林/多西环素\n住院(非ICU)：β内酰胺+大环内酯\nICU：β内酰胺+大环内酯/氟喹诺酮"),
            Node("3. 疗程", content = "一般5-7天\n临床稳定≥48h + 体温正常 → 可停药"),
            Node("4. 流感相关", content = "流感肺炎 → 奥司他韦+抗生素\n巴洛沙韦(单剂量)\n无需等待病毒检测结果")
        )),
        Node("十一、过敏/过敏性休克", children = listOf(
            Node("1. ABC评估", content = "Airway: 喉头水肿?\nBreathing: 哮鸣/缺氧?\nCirculation: 低血压/休克?"),
            Node("2. 肾上腺素", content = "IM 0.3-0.5mg(大腿中外侧)\n每5-15min可重复\n难治性 → iv 肾上腺素"),
            Node("3. 辅助治疗", content = "液体复苏(晶体液)\nH1拮抗剂(氯雷他定/苯海拉明)\nH2拮抗剂(雷尼替丁)\n糖皮质激素 iv"),
            Node("4. 观察", content = "双相反应风险\n轻症观察4-6h\n重症收住院")
        )),
        Node("十二、心律失常急诊", children = listOf(
            Node("1. 快速评估", content = "稳定 vs 不稳定(低血压/意识改变/胸痛/心衰)\n不稳定 → 同步电复律"),
            Node("2. 房颤/房扑", content = "稳定 → 控制心室率(BB/CCB) ± 抗凝\n新发<48h → 可考虑药物/电复律\n≥48h → TEE排除血栓或抗凝3周"),
            Node("3. 室上速", content = "迷走刺激(瓦氏动作) → 腺苷 6-12-18mg iv\n无效 → CCB(维拉帕米)/电复律"),
            Node("4. 室速/室颤", content = "不稳定 → 立即除颤(非同步)\n稳定室速 → 胺碘酮/利多卡因\n尖端扭转型 → 硫酸镁")
        )),
        Node("十三、糖尿病酮症酸中毒(DKA)", children = listOf(
            Node("1. 诊断标准", content = "血糖>13.9 + 酮体阳性 + pH<7.3 / HCO₃<18\n重度：pH<7.0 / HCO₃<10 / 意识改变"),
            Node("2. 液体复苏 (关键!)", content = "0.9%NS: 第1h 15-20mL/kg (约1-1.5L)\n之后 250-500mL/h (4-14mL/kg/h)\n血糖<13.9时改5%GS+NS"),
            Node("3. 胰岛素", content = "首剂 0.1U/kg iv bolus\n持续输注 0.1U/kg/h\n目标：血糖降 3-4mmol/L/h\npH恢复正常后过渡到皮下"),
            Node("4. 补钾+监测", content = "K<3.3 → 先补钾至>3.3再用胰岛素\nK 3.3-5.5 → 每升液加20-30mmol KCl\nq2h查血糖/K/血气\n警惕脑水肿(儿童)")
        )),
        Node("十四、高渗性高血糖状态(HHS)", children = listOf(
            Node("1. 特点", content = "血糖>33.3 + 有效渗透压>320 + 无/轻度酮症\n多见于老年T2DM、脱水严重"),
            Node("2. 液体", content = "0.9%NS 1-1.5L 第1h → 250-500mL/h\n补液量可达体重10%\n目标：渗透压降3-8mOsm/kg/h"),
            Node("3. 胰岛素", content = "血糖下降速度慢于DKA\n待补液使血糖降至13.9-16.7再开始\n胰岛素剂量0.05-0.1U/kg/h"),
            Node("4. 并发症", content = "死亡率10-20% (远高于DKA)\n关注血栓(常规抗凝)、横纹肌溶解\n搜寻诱因(感染/漏药/心肌梗死)")
        )),
        Node("十五、甲状腺危象", children = listOf(
            Node("1. Burch-Wartofsky评分", content = "≥45分 → 高度提示危象\n25-44分 → 即将危象\n<25分 → 可能性低"),
            Node("2. 抑制合成(BLOCK)", content = "PTU 首剂600mg → 200mg q4h (抑制T4→T3)\n1h后 碘剂(Lugol液5滴q6h / SSKI)\nPTU和碘剂同时：先PTU后1h再用碘"),
            Node("3. 对症+支持", content = "普萘洛尔 60-80mg q4h (控制高代谢)\n氢化可的松 100mg q8h (抑制T4→T3+肾上腺保护)\n降温(对乙酰氨基酚, 禁用阿司匹林)\n补液+纠正电解质"),
            Node("4. 清除(血浆置换)", content = "药物无效 → TPE血浆置换\n考来烯胺 4g q6h 促进T4排泄")
        )),
        Node("十六、高血压急症", children = listOf(
            Node("1. 定义", content = "SBP>180 / DBP>120 + 急性靶器官损害\n(脑/心/肾/视网膜/主动脉)\n无靶器官损害 → 高血压亚急症 → 口服降压"),
            Node("2. 静脉药物", content = "拉贝洛尔 20-80mg iv q10min (心衰/妊娠首选)\n尼卡地平 5mg/h iv 渐增 (脑出血首选)\n硝普钠 0.25-10μg/kg/min (主动脉夹层首选, 避光)\n艾司洛尔 0.5mg/kg bolus → 50-300μg/kg/min"),
            Node("3. 降压目标", content = "第1h降<25%\n2-6h降至160/100-110\n24-48h降至正常\n脑出血：SBP降至140-180"),
            Node("4. 特殊情况", content = "主动脉夹层 → 目标SBP<120/HR<60 (BB+硝普钠)\n子痫前期 → 硫酸镁+拉贝洛尔/硝苯地平\n嗜铬细胞瘤 → α阻滞后BB (酚妥拉明)")
        )),
        Node("十七、癫痫持续状态", children = listOf(
            Node("1. 定义", content = "持续>5min 或 2次发作间意识未恢复\n全面性惊厥性>30min → 不可逆脑损伤风险"),
            Node("2. 第一阶段(0-5min)", content = "维持气道+给氧 → 监测生命体征 → 血糖(低血糖→50mL 50%GS)\n劳拉西泮 4mg iv 或 地西泮 10mg iv"),
            Node("3. 第二阶段(5-30min)", content = "苯妥英钠 20mg/kg iv (≤50mg/min) 或\n丙戊酸钠 30mg/kg iv 或 左乙拉西坦 60mg/kg iv\n或 磷苯妥英 20mg PE/kg"),
            Node("4. 第三阶段(>30min)", content = "难治性 → ICU + 全麻(咪达唑仑/丙泊酚/戊巴比妥)\n脑电监测 → 爆发抑制\n同步查因: CT/MRI/LP/毒筛/抗体")
        )),
        Node("十八、急性心包填塞", children = listOf(
            Node("1. Beck三联征", content = "低血压 + 颈静脉怒张 + 心音遥远\n奇脉 >10mmHg\n超声：心包积液+右室塌陷"),
            Node("2. 紧急处理", content = "补充血容量(晶体液)\n避免正压通气(进一步降低前负荷)\n正性肌力药(多巴酚丁胺)维持血压"),
            Node("3. 心包穿刺", content = "超声引导下心包穿刺引流\n剑突下入路/心尖入路\n留置引流管"),
            Node("4. 病因治疗", content = "肿瘤性 → 硬化/化疗\n结核性 → 抗结核+激素\n尿毒症 → 加强透析")
        ))
    )
}
