package com.mynote.android.util

object ImagingReference {
    data class Imaging(
        val title: String, val modality: String, val system: String,
        val description: String, val imageUrl: String = ""
    )
    val all: List<Imaging> = listOf(
        Imaging("正常胸部X线", "X线", "呼吸", "肺野透亮度正常，肺纹理走行自然，肺门影不大，心影大小形态正常，膈面光滑，肋膈角锐利。", "https://radiopaedia.org/search?q=normal+chest+radiograph&scope=all"),
        Imaging("大叶性肺炎", "X线/CT", "呼吸", "大片状均匀致密影，空气支气管征(+)。CT：含气实变。", "https://radiopaedia.org/search?q=lobar+pneumonia&scope=all"),
        Imaging("支气管肺炎", "X线/CT", "呼吸", "双肺中下野沿支气管分布的小片状模糊影，小叶中心结节，树芽征。", "https://radiopaedia.org/search?q=bronchopneumonia&scope=all"),
        Imaging("间质性肺炎", "CT", "呼吸", "弥漫磨玻璃影(GGO)，小叶间隔增厚，蜂窝征(晚期)。", "https://radiopaedia.org/search?q=usual+interstitial+pneumonia&scope=all"),
        Imaging("粟粒性肺结核", "X线/CT", "呼吸", "双肺弥漫均匀1-3mm粟粒样结节，'三均匀'，纵隔淋巴结肿大。", "https://radiopaedia.org/search?q=miliary+tuberculosis&scope=all"),
        Imaging("肺脓肿", "CT", "呼吸", "厚壁空洞(壁>5mm)，内壁光滑，气液平面(+)。", "https://radiopaedia.org/search?q=lung+abscess&scope=all"),
        Imaging("中央型肺癌", "CT", "呼吸", "肺门肿块+支气管狭窄/截断；'三阻征'。", "https://radiopaedia.org/search?q=central+lung+cancer&scope=all"),
        Imaging("周围型肺癌", "CT", "呼吸", "分叶状结节/肿块，毛刺征/胸膜凹陷征，空泡征。", "https://radiopaedia.org/search?q=lung+cancer+3&scope=all"),
        Imaging("肺栓塞", "CTPA", "呼吸", "肺动脉内充盈缺损(中心性或贴壁)；马赛克灌注。", "https://radiopaedia.org/search?q=pulmonary+embolism&scope=all"),
        Imaging("气胸", "X线/CT", "呼吸", "脏层胸膜线+无肺纹理透亮区。张力性→纵隔移位。", "https://radiopaedia.org/search?q=pneumothorax&scope=all"),
        Imaging("胸腔积液", "X线/CT", "呼吸", "肋膈角变钝/消失，外高内低弧形致密影。CT：新月形水样密度。", "https://radiopaedia.org/search?q=pleural+effusion&scope=all"),
        Imaging("肺间质纤维化(UIP)", "CT", "呼吸", "蜂窝征+牵拉性支气管扩张，双下肺胸膜下分布。", "https://radiopaedia.org/search?q=usual+interstitial+pneumonia&scope=all"),
        Imaging("COPD", "CT", "呼吸", "肺气肿(小叶中心型/全小叶型)，肺大疱，桶状胸。", "https://radiopaedia.org/search?q=chronic+obstructive+pulmonary+disease+1&scope=all"),
        Imaging("急性主动脉夹层", "CTA", "心血管", "内膜片将主动脉腔分为真假腔。Stanford A/B。", "https://radiopaedia.org/search?q=aortic+dissection&scope=all"),
        Imaging("主动脉瘤", "CTA", "心血管", "主动脉局限扩张>正常1.5倍，附壁血栓(+)。", "https://radiopaedia.org/search?q=aortic+aneurysm&scope=all"),
        Imaging("心包积液", "超声/CT", "心血管", "右室前壁/左室后壁后方无回声区，>2cm为大积液。", "https://radiopaedia.org/search?q=pericardial+effusion&scope=all"),
        Imaging("肺水肿", "X线/CT", "心血管", "心影增大+肺淤血，Kerley B线、蝶翼状分布。", "https://radiopaedia.org/search?q=pulmonary+oedema&scope=all"),
        Imaging("正常腹部立位X线", "X线", "消化", "胃泡、结肠气体正常分布，无气液平面，无游离气体。", ""),
        Imaging("胃肠道穿孔", "X线/CT", "消化", "膈下游离气体(新月形透亮影)。CT：腹腔游离气体+积液。", "https://radiopaedia.org/search?q=pneumoperitoneum&scope=all"),
        Imaging("肠梗阻", "X线/CT", "消化", "阶梯状气液平面，CT移行带(梗阻点)，远端肠管塌陷。", "https://radiopaedia.org/search?q=small+bowel+obstruction&scope=all"),
        Imaging("急性胰腺炎", "CT(增强)", "消化", "间质水肿型/坏死型(无强化区+胰周坏死物)。", "https://radiopaedia.org/search?q=acute+pancreatitis&scope=all"),
        Imaging("急性阑尾炎", "CT", "消化", "阑尾增粗>6mm+壁增厚>2mm+周围脂肪条索影。", "https://radiopaedia.org/search?q=acute+appendicitis+2&scope=all"),
        Imaging("肝细胞癌(HCC)", "CT/MRI", "消化", "CT动脉期强化→门脉期廓清。MRI：DWI高+肝胆期低信号。", "https://radiopaedia.org/search?q=hepatocellular+carcinoma&scope=all"),
        Imaging("肝硬化", "CT/超声", "消化", "肝表面结节状+肝叶比例失调+门脉增宽>13mm+腹水/脾大。", "https://radiopaedia.org/search?q=cirrhosis&scope=all"),
        Imaging("胆囊结石", "超声/CT", "消化", "胆囊内强回声光团+后方声影+随体位移动。", "https://radiopaedia.org/search?q=gallstones+1&scope=all"),
        Imaging("胆总管结石", "MRCP/CT", "消化", "胆总管内充盈缺损+上方胆管扩张。", "https://radiopaedia.org/search?q=choledocholithiasis&scope=all"),
        Imaging("肾结石", "CT平扫", "泌尿", "输尿管行程区小点状致密影+近端积水扩张。", "https://radiopaedia.org/search?q=urolithiasis&scope=all"),
        Imaging("肾细胞癌(RCC)", "CT/MRI", "泌尿", "肾实质肿块+动脉期不均匀强化，排泄期廓清。", "https://radiopaedia.org/search?q=renal+cell+carcinoma+1&scope=all"),
        Imaging("膀胱癌", "CT/超声", "泌尿", "乳头状或扁平状肿块突入膀胱腔内，增强强化。", "https://radiopaedia.org/search?q=bladder+cancer&scope=all"),
        Imaging("肾上腺腺瘤", "CT", "泌尿", "单侧肾上腺<3cm，密度均匀<10HU(富脂型腺瘤)。", "https://radiopaedia.org/search?q=adrenal+adenoma&scope=all"),
        Imaging("急性脑梗死", "CT/MRI", "神经", "DWI超急性期高信号+ADC低信号。", "https://radiopaedia.org/search?q=ischaemic+stroke&scope=all"),
        Imaging("脑出血", "CT(首选)", "神经", "急性期CT高密度60-80HU+周围水肿带。", "https://radiopaedia.org/search?q=intracerebral+haemorrhage&scope=all"),
        Imaging("蛛网膜下腔出血", "CT", "神经", "基底池/侧裂内线状高密度影，CTA找动脉瘤。", "https://radiopaedia.org/search?q=subarachnoid+haemorrhage&scope=all"),
        Imaging("硬膜外血肿", "CT", "神经", "梭形高密度(双凸透镜)，不跨越颅缝。", "https://radiopaedia.org/search?q=extradural+haematoma&scope=all"),
        Imaging("硬膜下血肿", "CT", "神经", "新月形高/等/低密度，跨越颅缝。", "https://radiopaedia.org/search?q=subdural+haemorrhage&scope=all"),
        Imaging("胶质母细胞瘤", "MRI(增强)", "神经", "花环状不规则厚壁强化，中央坏死无强化。", "https://radiopaedia.org/search?q=glioblastoma+idh+wildtype&scope=all"),
        Imaging("脑膜瘤", "CT/MRI", "神经", "均匀强化肿块+宽基底贴附硬膜+硬膜尾征(+)。", "https://radiopaedia.org/search?q=meningioma&scope=all"),
        Imaging("垂体微腺瘤", "MRI(对比)", "神经", "动态增强：正常垂体先强化，腺瘤延迟呈相对低信号。", "https://radiopaedia.org/search?q=pituitary+microadenoma&scope=all"),
        Imaging("多发性硬化", "MRI", "神经", "脑室周围Dawson手指征。", "https://radiopaedia.org/search?q=multiple+sclerosis&scope=all"),
        Imaging("脑积水", "CT/MRI", "神经", "交通性/梗阻性，Evan's index>0.3。", "https://radiopaedia.org/search?q=hydrocephalus&scope=all"),
        Imaging("颈椎间盘突出", "MRI", "骨科/神经", "髓核后突压迫硬膜囊/脊髓。C5/6或C6/7最常见。", "https://radiopaedia.org/search?q=cervical+disc+protrusion&scope=all"),
        Imaging("Colles骨折", "X线", "骨科", "桡骨远端骨折，远折端向背侧移位，'餐叉样'畸形。", "https://radiopaedia.org/search?q=colles+fracture&scope=all"),
        Imaging("股骨颈骨折", "X线/CT", "骨科", "关节囊内骨折，Garden分型I-IV。", "https://radiopaedia.org/search?q=femoral+neck+fracture&scope=all"),
        Imaging("椎体压缩骨折", "X线/MRI", "骨科", "椎体楔形变(前柱压缩)，上终板塌陷。MRI区分新旧。", "https://radiopaedia.org/search?q=vertebral+compression+fracture&scope=all"),
        Imaging("退行性骨关节病", "X线", "骨科", "关节间隙变窄+软骨下骨硬化囊变+骨赘。", "https://radiopaedia.org/search?q=osteoarthritis&scope=all"),
        Imaging("肩关节脱位", "X线", "骨科", "前脱位(>95%)+Hill-Sachs凹陷+Bankart骨折。", "https://radiopaedia.org/search?q=anterior+shoulder+dislocation&scope=all"),
        Imaging("骨髓炎", "MRI", "骨科", "T1低/T2高骨髓信号+骨膜反应。", "https://radiopaedia.org/search?q=osteomyelitis&scope=all"),
        Imaging("软组织肉瘤", "MRI增强", "骨科", "T1等/T2高不均匀显著强化，中央坏死不强化。", "https://radiopaedia.org/search?q=soft+tissue+sarcoma&scope=all"),
        Imaging("半月板撕裂", "MRI", "骨科", "T2/PD高信号线延伸至关节面=III级信号。", "https://radiopaedia.org/search?q=meniscal+tear&scope=all"),
        Imaging("乳腺BI-RADS", "钼靶/超声/MRI", "妇产/乳腺", "0-6级；钼靶看钙化、腺体密度。", "https://radiopaedia.org/search?q=bi+rads&scope=all"),
        Imaging("乳腺癌", "钼靶+超声", "妇产/乳腺", "不规则肿块+微小钙化(簇状)+结构扭曲。", "https://radiopaedia.org/search?q=breast+cancer&scope=all"),
        Imaging("子宫肌瘤", "超声/MRI", "妇产", "子宫肌层内低回声结节，边缘清，可多发。", "https://radiopaedia.org/search?q=uterine+leiomyoma&scope=all"),
        Imaging("子宫内膜癌", "MRI(增强)", "妇产", "侵犯子宫内膜→浸润肌层(交界区中断=浸润>50%)。", "https://radiopaedia.org/search?q=endometrial+carcinoma&scope=all"),
        Imaging("卵巢囊肿良恶性", "超声/MRI", "妇产", "良性：单纯囊肿。恶性：厚壁囊实性+腹水/腹膜种植。", "https://radiopaedia.org/search?q=ovarian+tumours&scope=all"),
        Imaging("新生儿HIE", "MRI+DTI", "儿科", "DWI高信号+内囊后肢/丘脑/基底节累及。", "https://radiopaedia.org/search?q=hypoxic+ischaemic+encephalopathy&scope=all"),
        Imaging("肠套叠", "超声/气体灌肠", "儿科", "横切面'同心圆征'/纵切面'假肾征'。", "https://radiopaedia.org/search?q=intussusception&scope=all"),
        Imaging("发育性髋关节发育不良(DDH)", "超声(Graf)", "儿科", "Graf法α角>60°正常，α<43°脱位。", "https://radiopaedia.org/search?q=developmental+dysplasia+of+the+hip&scope=all"),
        // === 耳鼻喉科 ===
        Imaging("急性鼻窦炎", "CT", "耳鼻喉", "窦腔黏膜增厚>4mm+气液平面+窦口鼻道复合体阻塞。", "https://radiopaedia.org/search?q=acute+sinusitis&scope=all"),
        Imaging("慢性鼻窦炎", "CT", "耳鼻喉", "窦壁骨质增厚硬化+黏膜持续增厚+息肉样变。", "https://radiopaedia.org/search?q=chronic+sinusitis&scope=all"),
        Imaging("鼻咽癌", "MRI(增强)", "耳鼻喉", "鼻咽侧壁(咽隐窝)不对称软组织肿块，不均匀强化，咽后淋巴结转移常见(最早)。", "https://radiopaedia.org/search?q=nasopharyngeal+carcinoma&scope=all"),
        Imaging("声带息肉", "喉镜/CT", "耳鼻喉", "声带游离缘前中1/3交界处息肉样突起，单侧多见，增强均匀强化。", "https://radiopaedia.org/search?q=vocal+cord+polyp&scope=all"),
        Imaging("中耳炎(胆脂瘤型)", "CT(HRCT颞骨)", "耳鼻喉", "上鼓室/鼓窦软组织影+听小骨侵蚀+盾板破坏(克氏征)+迷路瘘管。", "https://radiopaedia.org/search?q=cholesteatoma&scope=all"),
        Imaging("听神经瘤(前庭神经鞘瘤)", "MRI(增强)", "耳鼻喉", "CPA区肿块，内听道扩大，T1等/T2不均匀高+强化明显，'冰淇淋+蛋筒'征。", "https://radiopaedia.org/search?q=vestibular+schwannoma&scope=all"),
        Imaging("过敏性鼻炎", "CT", "耳鼻喉", "下鼻甲肥大，黏膜水肿，窦腔无破坏。非必需检查，仅排除并发症时做。", "https://radiopaedia.org/search?q=allergic+rhinitis&scope=all"),
        // === 眼科 ===
        Imaging("眼眶爆裂骨折", "CT", "眼科", "眶下壁/内侧壁骨折片向窦腔凹陷，眶内脂肪疝出，'泪滴征'。", "https://radiopaedia.org/search?q=orbital+blowout+fracture&scope=all"),
        Imaging("视网膜母细胞瘤", "CT/MRI", "眼科", "儿童(<3岁)眼内钙化性肿块(CT白亮)，T1高+T2低。", "https://radiopaedia.org/search?q=retinoblastoma&scope=all"),
        Imaging("视神经炎", "MRI(增强)", "眼科", "视神经T2高信号增粗+明显强化。多发性硬化相关：脑室周围Dawson手指征。", "https://radiopaedia.org/search?q=optic+neuritis&scope=all"),
        Imaging("青光眼", "OCT/超声", "眼科", "前房角狭窄/关闭(房角镜)；杯盘比>0.6；OCT示RNFL变薄。", "https://radiopaedia.org/search?q=glaucoma+imaging&scope=all"),
        Imaging("白内障", "超声/裂隙灯", "眼科", "晶状体混浊(超声高回声)。术前A超测量眼轴以计算IOL度数。", "https://radiopaedia.org/search?q=cataract+imaging&scope=all"),
        // === 口腔科 ===
        Imaging("牙源性囊肿(根尖/含牙)", "CT(曲面断层)", "口腔", "根尖囊肿：根尖周围透亮区+骨皮质连续。含牙囊肿：牙冠周围透亮囊腔。", "https://radiopaedia.org/search?q=odontogenic+cyst&scope=all"),
        Imaging("颌骨骨髓炎", "CT", "口腔", "骨破坏+死骨片+骨膜反应+软组织蜂窝织炎。", "https://radiopaedia.org/search?q=osteomyelitis+mandible&scope=all"),
        Imaging("颞下颌关节紊乱(TMD)", "MRI", "口腔", "关节盘前移位(闭口位盘在髁状突前方)，张口位/闭口位对照。", "https://radiopaedia.org/search?q=temporomandibular+joint+dysfunction&scope=all"),
        Imaging("腮腺肿瘤", "MRI(增强)", "口腔", "多形性腺瘤：T2明显高+不均匀强化。Warthin瘤：T2均匀高+明显强化。", "https://radiopaedia.org/search?q=parotid+tumour&scope=all"),
        // === 皮肤科 ===
        Imaging("皮肤黑色素瘤", "超声(高频探头)/MRI", "皮肤", "超声：低回声结节>1mm厚度+Breslow分级对比。MRI：T1高(黑色素顺磁性)+T2低信号。", "https://radiopaedia.org/search?q=melanoma+imaging&scope=all"),
        Imaging("皮肤鳞状细胞癌", "超声(高频探头)", "皮肤", "表皮不规则增厚、角化过度+深部侵犯低回声区+丰富血流信号。", "https://radiopaedia.org/search?q=squamous+cell+carcinoma+skin+imaging&scope=all"),
        Imaging("皮脂腺囊肿", "超声(高频探头)", "皮肤", "皮下低/无回声囊性结节，后壁增强，无血流信号，可伴钙化。", "https://radiopaedia.org/search?q=epidermal+inclusion+cyst&scope=all"),
        // === 血管外科 ===
        Imaging("下肢深静脉血栓(DVT)", "超声(加压)", "血管外科", "管腔不可压缩(加压超声)，彩色多普勒无血流信号，血栓呈低/等回声。", "https://radiopaedia.org/search?q=deep+vein+thrombosis+ultrasound&scope=all"),
        Imaging("颈动脉狭窄", "超声/CTA", "血管外科", "超声：PSV>125cm/s(狭窄>50%)。CTA：钙化斑块+狭窄程度测量(NASCET法)。", "https://radiopaedia.org/search?q=carotid+artery+stenosis&scope=all"),
        Imaging("腹主动脉瘤(AAA)", "超声/CTA", "血管外科", "主动脉直径>3cm为动脉瘤。随访：3-4cm/年复查，>5.5cm/生长>1cm/年→考虑修复。", "https://radiopaedia.org/search?q=abdominal+aortic+aneurysm&scope=all"),
        Imaging("外周动脉闭塞性疾病(PAOD)", "超声/CTA", "血管外科", "踝肱指数ABI<0.9。CTA：狭窄/闭塞段+侧支循环。超声：三相波形变钝。", "https://radiopaedia.org/search?q=peripheral+arterial+disease+imaging&scope=all"),
        Imaging("肾动脉狭窄", "超声/CTA", "血管外科", "超声：PSV>200cm/s+肾动脉/主动脉PSV比值>3.5(提示狭窄>60%)。CTA/MRA亦可。", "https://radiopaedia.org/search?q=renal+artery+stenosis&scope=all"),
        // === 胸外科 ===
        Imaging("食管癌", "上消化道造影/CT/MRI", "胸外科", "钡餐：充盈缺损/不规则龛影/苹果核征。CT：食管壁增厚+周围侵犯+TNM分期。", "https://radiopaedia.org/search?q=oesophageal+carcinoma&scope=all"),
        Imaging("食管静脉曲张", "上消化道造影/CT(增强)", "胸外科", "钡餐：串珠状充盈缺损。CT：食管壁静脉扩张强化，可伴门脉高压征象。", "https://radiopaedia.org/search?q=oesophageal+varices&scope=all"),
        Imaging("胸腺瘤", "CT/MRI", "胸外科", "前纵隔边界清晰肿块，均匀强化，可伴重症肌无力(约30-50%)。", "https://radiopaedia.org/search?q=thymoma&scope=all"),
        Imaging("纵隔淋巴瘤", "CT/MRI", "胸外科", "前纵隔巨大软组织肿块融合，均匀低强化+无坏死，年轻成人多见。", "https://radiopaedia.org/search?q=mediastinal+lymphoma&scope=all"),
        Imaging("自发性纵隔气肿", "X线/CT", "胸外科", "皮下气肿+纵隔内气体(空气带勾出纵隔结构轮廓)+气胸(约20%)。", "https://radiopaedia.org/search?q=pneumomediastinum&scope=all"),
        // === 烧伤/整形 ===
        Imaging("深二度烧伤", "超声(LDPI)/激光多普勒", "烧伤科", "LDPI评估灌注深度指导是否需要植皮。超声：真皮深部肿胀+水肿带。", "https://radiopaedia.org/search?q=deep+burn+imaging&scope=all"),
        Imaging("瘢痕疙瘩", "超声(高频探头)", "整形", "真皮层不规则低回声，无包膜，边界不清，丰富血流信号。", "https://radiopaedia.org/search?q=keloid+imaging+ultrasound&scope=all"),
        Imaging("软组织异物", "超声/X线", "整形/急诊", "X线检出高密度异物(金属/玻璃)。超声：低密度异物+周围炎性回声影。", "https://radiopaedia.org/search?q=soft+tissue+foreign+body&scope=all"),
        // === 疼痛/介入 ===
        Imaging("腰椎间盘突出", "MRI", "疼痛科/骨科", "L4/5或L5/S1常见。髓核突出/脱出压迫神经根。", "https://radiopaedia.org/search?q=lumbar+disc+prolapse&scope=all"),
        Imaging("髋关节骨坏死", "MRI(增强)", "骨科/疼痛", "股骨头软骨下T2高信号带±低信号带(双线征)，软骨下塌陷(Ficat III-IV)。", "https://radiopaedia.org/search?q=avascular+necrosis+femoral+head&scope=all"),
        Imaging("髋关节置换术后松动", "X线", "骨科", "假体周围透亮线>2mm或进行性进展，假体移位/骨折。", "https://radiopaedia.org/search?q=hip+prosthesis+loosening&scope=all"),
        Imaging("尺神经卡压(肘管)", "MRI/超声", "疼痛科/骨科", "肘管内尺神经肿胀(横截面积增大)+T2高信号。", "https://radiopaedia.org/search?q=cubital+tunnel+syndrome&scope=all"),
        // === 核医学/PET ===
        Imaging("PET-CT在肿瘤分期中的应用", "PET-CT", "核医学", "FDG高摄取(SUVmax)反映糖代谢活跃、转移灶检测。假阳性：感染/炎症。假阴性：低代谢肿瘤(类癌/GIST部分)。", "https://radiopaedia.org/search?q=FDG+PET+CT+oncology&scope=all"),
        Imaging("骨扫描(全身)", "核素骨显像", "核医学", "99mTc-MDP静脉注射→观察转移瘤(多发非对称高浓聚)，创伤/感染/退行变。", "https://radiopaedia.org/search?q=bone+scan+nuclear+medicine&scope=all"),
        Imaging("甲状腺核素显像", "核医学", "核医学", "99mTc/123I：冷结节(无摄取-可能恶性)，热结节(高摄取-几乎总是良性)。", "https://radiopaedia.org/search?q=thyroid+scintigraphy&scope=all"),
        Imaging("心肌核素显像", "SPECT/PET", "核医学", "铊201/Tc99m心肌灌注成像：可逆性缺损=缺血。固定缺损=梗死/瘢痕。", "https://radiopaedia.org/search?q=myocardial+perfusion+imaging&scope=all"),
        // === 重症/急诊 ===
        Imaging("创伤FAST超声", "超声(床边)", "急诊/ICU", "Morrison肝肾隐窝/脾周/盆腔/心包四个窗快速评估有无游离液体(出血)。", "https://radiopaedia.org/search?q=FAST+ultrasound+trauma&scope=all"),
        Imaging("导管相关血栓", "超声", "ICU/血管", "锁骨下/颈内静脉置管周围管腔不可压缩+血栓回声+无血流。", "https://radiopaedia.org/search?q=catheter+related+thrombosis&scope=all"),
        Imaging("急性呼吸窘迫综合征(ARDS)", "X线/CT", "ICU", "X线：双肺弥漫肺泡浸润(不能完全用心衰/积液解释)。CT：重力依赖分布GGO/实变。", "https://radiopaedia.org/search?q=ARDS+imaging&scope=all"),
        // === 更多儿科 ===
        Imaging("韦格伦脑膜炎(坏死性小肠结肠炎NEC)", "X线(腹部)", "儿科", "肠壁积气(线状/囊状气体)+门静脉气体，气体固定分布+肠袢分离+腹水(穿孔)。", "https://radiopaedia.org/search?q=necrotising+enterocolitis&scope=all"),
        Imaging("肥厚性幽门狭窄", "超声", "儿科", "幽门肌层厚度>3mm(单层)+幽门管长度>16mm+幽门直径>11mm。'靶征'/'肩征'。", "https://radiopaedia.org/search?q=hypertrophic+pyloric+stenosis&scope=all"),
        Imaging("肾母细胞瘤(Wilms瘤)", "CT/MRI", "儿科", "腹部巨大肿块→肾脏起源，不均匀强化+局部坏死/出血，下腔静脉瘤栓(5-10%)。", "https://radiopaedia.org/search?q=Wilms+tumour&scope=all"),
        // === 更多消化 ===
        Imaging("直肠癌", "MRI(高分辨率)", "消化", "T2低信号肿瘤侵犯直肠系膜筋膜(环周切缘CRM)<1mm阳性，淋巴结转移评估。", "https://radiopaedia.org/search?q=rectal+cancer+MRI&scope=all"),
        Imaging("克隆氏病(CD)", "CTE/MRE", "消化", "肠壁增厚>3mm+分层强化(活动性)+纤维脂肪增生+肠系膜淋巴结++，梳齿征。", "https://radiopaedia.org/search?q=Crohn+disease+imaging&scope=all"),
        Imaging("溃疡性结肠炎(UC)", "CT/MRE", "消化", "连续结肠壁增厚+黏膜强化+弥漫性病变(无跳跃)，肠壁变短+结肠袋消失。", "https://radiopaedia.org/search?q=ulcerative+colitis+imaging&scope=all"),
        // === 更多内分泌 ===
        Imaging("甲状腺结节(TI-RADS)", "超声", "内分泌", "TI-RADS分级：1-5级。超声：低回声+微钙化+纵横比>1+不规则边缘=高度可疑。", "https://radiopaedia.org/search?q=thyroid+nodule+TI-RADS&scope=all"),
        Imaging("甲状旁腺腺瘤", "超声/Tc99m sestamibi", "内分泌", "超声：甲状腺后方椭圆形低回声结节(>1cm)。MIBI：延迟期持续示踪剂潴留(甲状旁腺)。", "https://radiopaedia.org/search?q=parathyroid+adenoma&scope=all"),
        Imaging("肾上腺嗜铬细胞瘤", "CT/MRI", "内分泌", "肾上腺>3cm，T2明显高信号(灯泡征)，显著强化，不应作活检风险(高血压危象)。", "https://radiopaedia.org/search?q=phaeochromocytoma&scope=all"),
        // === 更多妇产 ===
        Imaging("宫外孕(异位妊娠)", "超声(阴道)", "妇产", "附件包块(环征)+宫内无囊孕囊+β-hCG>1500。破裂：腹腔积血(FAST)。", "https://radiopaedia.org/search?q=ectopic+pregnancy+ultrasound&scope=all"),
        Imaging("前置胎盘", "超声", "妇产", "胎盘完全/部分覆盖宫颈内口。<20周低位胎盘可随子宫生长上移。", "https://radiopaedia.org/search?q=placenta+praevia&scope=all"),
        Imaging("胎儿超声(中孕筛查)", "超声(中孕)", "妇产", "18-22周系统筛查：双顶径/头围/腹围/股骨长+颅脑/心脏/脊柱/腹部/四肢结构评估。", "https://radiopaedia.org/search?q=mid-trimester+fetal+ultrasound&scope=all"),
        // === 更多呼吸 ===
        Imaging("肺结节(Lung-RADS)", "CT(低剂量)", "呼吸", "Lung-RADS 1-4X分级。实性/部分实性/纯GGO结节。年度筛查>50岁/30包年吸烟者。", "https://radiopaedia.org/search?q=lung+nodule+Lung-RADS&scope=all"),
        Imaging("支气管扩张", "CT(HRCT)", "呼吸", "支气管内径>邻近肺动脉(印戒征)+支气管壁增厚+黏液栓(树芽征)。", "https://radiopaedia.org/search?q=bronchiectasis&scope=all"),
        // === 骨科/运动医学 ===
        Imaging("踝关节骨折(Weber分型)", "X线", "骨科", "Weber A/B/C+胫腓联合评估。A=腓骨骨折线在联合以下；B=联合平面；C=联合上方伴联合分离。", "https://radiopaedia.org/search?q=ankle+fracture+Weber&scope=all"),
        Imaging("跟骨骨折", "CT", "骨科", "关节面塌陷(Bohler角<20°)，CT三维重建指导Sanders分型(决定是否手术)。", "https://radiopaedia.org/search?q=calcaneal+fracture&scope=all"),
        Imaging("舟状骨骨折(腕)", "X线(舟骨位)", "骨科", "X线阴性不能排除→MRI(金标准)或10-14天复查X线。近端坏死风险最高(逆行血供)。", "https://radiopaedia.org/search?q=scaphoid+fracture&scope=all"),
        Imaging("肩袖撕裂", "MRI(斜冠状位)", "骨科", "全层撕裂：肌腱连续性中断+T2高信号液体。部分撕裂：滑囊侧/关节侧/肌腱内。", "https://radiopaedia.org/search?q=rotator+cuff+tear&scope=all"),
        Imaging("前交叉韧带(ACL)撕裂", "MRI", "骨科", "ACL纤维不连续+T2高信号+韧带波浪状/截断。继发征象：胫骨前移(>7mm)+骨挫伤。", "https://radiopaedia.org/search?q=ACL+tear+MRI&scope=all"),
        Imaging("半月板囊肿", "MRI", "骨科", "半月板撕裂→囊肿(关节囊外液体信号)，外侧半月板多于内侧。", "https://radiopaedia.org/search?q=meniscal+cyst&scope=all"),
        Imaging("疲劳性骨折(应力骨折)", "X线/MRI", "骨科", "早期X线阴性→MRI：T2高信号骨膜水肿+T1低信号骨折线。好发：胫骨/跖骨/股骨颈。", "https://radiopaedia.org/search?q=stress+fracture&scope=all"),
        Imaging("痛风性关节炎", "X线(晚期)/超声/CT(双能量)", "骨科/风湿", "X线：凿孔状骨质缺损+软组织肿块+关节间隙正常。超声：双轨征(尿酸盐结晶)。CT双能量：绿色尿酸盐沉积。", "https://radiopaedia.org/search?q=gout+imaging&scope=all"),
        // === 心脏超声 ===
        Imaging("主动脉瓣狭窄", "超声心动图", "心内科", "AVA<1cm²(重度<0.8)+跨瓣峰值流速>4m/s或平均压差>40mmHg。继发左室肥厚。", "https://radiopaedia.org/search?q=aortic+stenosis+echocardiography&scope=all"),
        Imaging("二尖瓣反流", "超声心动图", "心内科", "PISA法(近端等速表面积)测EROA：轻度<0.2cm²；重度>0.4cm²/返流容积>60mL。", "https://radiopaedia.org/search?q=mitral+regurgitation+echocardiography&scope=all"),
        Imaging("房间隔缺损(ASD)", "超声心动图(TTE+TEE)", "心内科", "房间隔回声中断+左→右分流(彩色多普勒)。TEE精确测量缺损大小决定封堵 vs 手术。", "https://radiopaedia.org/search?q=atrial+septal+defect+echocardiography&scope=all"),
        Imaging("室间隔缺损(VSD)", "超声心动图", "心内科", "室间隔回声中断+左→右分流+左室容量负荷过重(左室扩大)。成人残余/新发。", "https://radiopaedia.org/search?q=ventricular+septal+defect&scope=all"),
        Imaging("左室射血分数(EF)评估", "超声心动图", "心内科", "Simpson双平面法(最准确)+M模式。正常>55%，HFrEF<40%，HFmrEF 40-49%。", "https://radiopaedia.org/search?q=ejection+fraction+echocardiography&scope=all"),
        // === 肝胆胰 ===
        Imaging("急性胆囊炎", "超声/CT", "消化", "胆囊壁增厚>3mm+胆囊扩张+胆囊周围积液+超声Murphy征(+)", "https://radiopaedia.org/search?q=acute+cholecystitis&scope=all"),
        Imaging("胰腺假性囊肿", "CT/MRI", "消化", "胰周/胰腺内包裹性液体积聚(>4周)，无强化囊壁。感染性占10-20%。", "https://radiopaedia.org/search?q=pancreatic+pseudocyst&scope=all"),
        Imaging("门静脉血栓", "超声(多普勒)/CT", "消化", "门静脉腔内有回声团块+彩色多普勒无血流。急性/慢性(海绵样变：侧支形成)。", "https://radiopaedia.org/search?q=portal+vein+thrombosis&scope=all"),
        Imaging("布加综合征", "超声(多普勒)/CT", "消化", "肝静脉流出道阻塞→肝大、腹水。超声：肝静脉无血流/反向/狭窄。", "https://radiopaedia.org/search?q=Budd+Chiari+syndrome&scope=all"),
        Imaging("脂肪肝", "超声/CT/MRI(脂肪定量)", "消化", "超声：肝回声增强(亮肝)伴回声衰减。MRI Dixon法(PDFF)：含脂量>5%诊断脂肪肝。", "https://radiopaedia.org/search?q=hepatic+steatosis&scope=all"),
        // === 介入放射 ===
        Imaging("肝癌TACE/DEB-TACE", "DSA/CT", "介入放射", "选择性肝动脉造影→碘油/载药微球栓塞肿瘤供血动脉。CT评估Lipiodol沉积+残留/复发。", "https://radiopaedia.org/search?q=TACE+liver+cancer&scope=all"),
        Imaging("CT引导下穿刺活检", "CT", "介入放射", "实时CT扫描→同轴针穿刺目标→减少调整次数。并发症：出血、气胸、胰腺炎/肠穿孔(罕见)。", "https://radiopaedia.org/search?q=CT+guided+biopsy&scope=all"),
        Imaging("下腔静脉滤器(IVCF)", "DSA/X线", "介入放射", "肾下段IVC定位→释放。位置不正/倾斜>15°/移位/穿透/断裂需要取出。", "https://radiopaedia.org/search?q=IVC+filter&scope=all"),
        Imaging("经皮胆道引流(PTBD)", "超声+CT联合", "介入放射", "胆道扩张+穿刺目标胆管→经皮置管引流→胆道造影确认位置。", "https://radiopaedia.org/search?q=percutaneous+transhepatic+biliary+drainage&scope=all"),
        Imaging("射频消融(RFA)/微波消融(MWA)治疗肝癌", "CT/超声引导", "介入放射", "术前评估：病灶≤3-5cm、少临近大血管/胆囊。术中：电极针准确定位+消融带覆盖肿瘤+0.5cm安全边。", "https://radiopaedia.org/search?q=radiofrequency+ablation+liver&scope=all"),
        // === 儿科 ===
        Imaging("毛细支气管炎(RSV相关)", "X线", "儿科", "过度充气+肺纹理增粗+灶性不张。约20%合并肺炎。", "https://radiopaedia.org/search?q=bronchiolitis+chest+radiograph&scope=all"),
        Imaging("急性喉炎(Croup)", "X线(颈部正位)", "儿科", "前后位颈部X线→声门下狭窄('尖塔征'/steeple sign)。", "https://radiopaedia.org/search?q=croup+steeple+sign&scope=all"),
        Imaging("骶尾部畸胎瘤", "超声(产前)/MRI", "儿科", "超声(≥20周胎儿)：骶尾部不均匀囊实性肿块+血流丰富。出生后MRI评估盆腔侵犯。", "https://radiopaedia.org/search?q=sacrococcygeal+teratoma&scope=all"),
        Imaging("先天性膈疝", "超声(产前)/X线(出生后)", "儿科", "产前超声：腹腔内脏疝入胸腔(胃泡)+纵隔移位。出生后X线：胸腔肠管影+纵隔偏位。", "https://radiopaedia.org/search?q=congenital+diaphragmatic+hernia&scope=all"),
        // === 妇产 ===
        Imaging("胎盘植入(PAS)", "超声/MRI", "妇产", "胎盘不规则血管间隙+胎盘后透明区消失+子宫肌层变薄(<1mm)。MRI：胎盘不均匀/凸出。", "https://radiopaedia.org/search?q=placenta+accreta+spectrum&scope=all"),
        Imaging("双胎输血综合征(TTTS)", "超声", "妇产", "单绒毛膜双胎→受血儿羊水过多(垂直池>8cm)→供血儿羊水过少(<2cm/无)。Quintero分期。", "https://radiopaedia.org/search?q=twin+twin+transfusion+syndrome&scope=all"),
        Imaging("卵巢扭转", "超声(阴道)", "妇产", "卵巢增大+水肿+周边卵泡('珍珠串征')+多普勒无血流(但20-25%可有血流动脉)。", "https://radiopaedia.org/search?q=ovarian+torsion&scope=all"),
        // === 泌尿 ===
        Imaging("前列腺癌(多参数MRI)", "MRI(多参数)", "泌尿", "PI-RADS v2.1评分+DWI低ADC值+早明显强化(动脉期)/早廓清(廓清期)。", "https://radiopaedia.org/search?q=prostate+cancer+MRI+PI-RADS&scope=all"),
        Imaging("良性前列腺增生(BPH)", "超声/CT", "泌尿", "前列腺体积增大>30mL，中叶突入膀胱。尿流率+残余尿量评估梗阻。", "https://radiopaedia.org/search?q=benign+prostatic+hyperplasia+imaging&scope=all"),
        Imaging("肾积水", "超声/CT(平扫)", "泌尿", "肾盂肾盏扩张，肾实质变薄(重度)。急性梗阻：无实质萎缩。慢性：实质萎缩+薄壁。", "https://radiopaedia.org/search?q=hydronephrosis&scope=all"),
        // === 麻醉/手术相关 ===
        Imaging("困难气道评估(影像)", "CT(颈部/上纵隔)", "麻醉科", "上纵隔占位/颈椎骨质增生/舌根肥大/声门上狭窄等导致气管插管困难。术前CT评估气道。", "https://radiopaedia.org/search?q=difficult+airway+imaging&scope=all"),
        Imaging("中心静脉穿刺定位", "超声(床边)", "麻醉/ICU", "颈内/锁骨下/股静脉→超声引导下穿刺→确认导丝走向→X线确认导管位置(右房-上腔静脉连接部)。", "https://radiopaedia.org/search?q=central+venous+catheter+ultrasound&scope=all"),
        // === 脊椎 ===
        Imaging("脊柱侧弯", "X线(全长正侧位)", "骨科/康复", "测量Cobb角(>10°诊断)。Lenke分型指导手术计划。", "https://radiopaedia.org/search?q=scoliosis+imaging&scope=all"),
        Imaging("强直性脊柱炎", "X线/MRI(STIR)", "风湿/骨科", "骶髂关节炎(II级以上)：侵蚀+硬化+融合('竹节脊柱')。MRI STIR：活动性骨髓水肿。", "https://radiopaedia.org/search?q=ankylosing+spondylitis+imaging&scope=all"),
        Imaging("脊髓压迫(转移)", "MRI(增强)", "神经/骨科", "硬膜外肿块(转移最常见)+脊髓/马尾神经受压+脊柱不稳(T2高信号水肿)。急诊→手术/放疗。", "https://radiopaedia.org/search?q=spinal+cord+compression&scope=all"),
        // === 胸外科补充 ===
        Imaging("肺隔离症", "CT(增强)", "胸外科", "体循环供血(来自胸主动脉分支)+正常支气管连接缺失。叶内型(静脉引流肺静脉)/叶外型。", "https://radiopaedia.org/search?q=pulmonary+sequestration&scope=all"),
        Imaging("膈疝", "X线/CT", "胸外科", "膈面破裂→腹腔内脏疝入胸腔(胃/肠管)+纵隔移位。外伤性最常见。", "https://radiopaedia.org/search?q=diaphragmatic+hernia+traumatic&scope=all"),
        // === 急诊创伤分级 ===
        Imaging("脾损伤 AAST 分级", "CT(增强)", "急诊/创伤", "I-II级(血肿/浅表裂伤<3cm)→保守; III级(裂伤>3cm/活动性出血)→介入/手术; IV-V(粉碎/血管撕脱)→手术。", "https://radiopaedia.org/search?q=splenic+trauma+AAST&scope=all"),
        Imaging("肝损伤 AAST 分级", "CT(增强)", "急诊/创伤", "I-II级(包膜下血肿<50%/浅表裂伤<3cm)→保守; III-V(深度裂伤/实质破坏)→介入/手术。注意肝周造影剂外溢=活动性出血。", "https://radiopaedia.org/search?q=liver+trauma+AAST&scope=all"),
        Imaging("肾损伤 AAST 分级", "CT(增强)", "急诊/创伤", "I-II(挫伤/小血肿)→保守; III(裂伤>1cm)→观察; IV(肾盂裂伤/节段性梗死)→手术; V(肾粉碎/肾蒂撕脱)→肾切除。", "https://radiopaedia.org/search?q=renal+trauma+AAST&scope=all"),
        Imaging("骨盆骨折 Tile 分型", "CT(三维重建)", "急诊/骨科", "Tile A(稳定)→保守; B(旋转不稳/垂直稳); C(旋转+垂直都不稳)=最严重→外固定/内固定。盆腔出血主要死亡原因。", "https://radiopaedia.org/search?q=pelvic+fracture+Tile&scope=all"),
        Imaging("胸主动脉损伤(BTAI)", "CTA(胸腹)", "急诊/血管", "主动脉峡部(动脉韧带处)最常见。I级:内膜撕裂; II级:壁内血肿; III级:假性动脉瘤; IV级:破裂→急诊手术/TEVAR。", "https://radiopaedia.org/search?q=blunt+thoracic+aortic+injury&scope=all"),
        // === 神经系统补充 ===
        Imaging("脑脓肿", "MRI(增强+DWI)", "神经/感染", "环形强化+中央DWI高信号(脓液限制扩散→与肿瘤坏死囊变鉴别重要)+周围明显血管源性水肿。", "https://radiopaedia.org/search?q=brain+abscess&scope=all"),
        Imaging("硬膜下脓胸", "MRI(增强+DWI)", "神经/感染", "新月形硬膜下积脓+DWI高信号(脓液)+边缘强化+占位效应。鼻窦炎/中耳炎最常见来源。", "https://radiopaedia.org/search?q=subdural+empyema&scope=all"),
        Imaging("脊髓硬膜外脓肿", "MRI(增强)", "神经/感染", "硬膜外T2高信号脓液积聚+边缘强化+脊髓受压。MRI急诊! 脊柱手术/菌血症/IVDU常见。", "https://radiopaedia.org/search?q=spinal+epidural+abscess&scope=all"),
        Imaging("颈动脉夹层", "CTA/MRI(T1脂肪抑制)", "神经/血管", "颈内动脉颅外段火焰状/串珠状狭窄→动脉腔内新月形T1高信号(高铁血红蛋白壁内血肿)。年轻卒中首要考虑。", "https://radiopaedia.org/search?q=carotid+artery+dissection&scope=all"),
        Imaging("颅内动脉瘤(未破裂)", "CTA/MRA(TOF)", "神经/血管", "前交通/后交通/大脑中分叉部最常见。形态：囊状/梭形/水泡型。>7mm+形态不规则/增长时考虑治疗。", "https://radiopaedia.org/search?q=cerebral+aneurysm&scope=all"),
        Imaging("脑动静脉畸形(AVM)", "MRI/CTA/DSA", "神经/血管", "供血动脉→巢(nidus)→引流静脉。MRI：蜂窝状流空信号。DSA金标准。Spetzler-Martin分级。", "https://radiopaedia.org/search?q=brain+arteriovenous+malformation&scope=all"),
        Imaging("脑静脉窦血栓形成(CVST)", "CTV/MRI(增强)", "神经/血管", "CT：上矢状窦高密度(索状征)+空三角征(增强)。MRI+MRA：窦腔内血栓(各期信号不同)。头痛+癫痫/局灶体征。", "https://radiopaedia.org/search?q=cerebral+venous+sinus+thrombosis&scope=all"),
        // === 脊柱感染/代谢 ===
        Imaging("脊柱化脓性感染(椎间盘炎/骨髓炎)", "MRI(增强)", "骨科/感染", "T1低(终板)+T2高(椎间盘/终板水肿)+明显强化。终板侵蚀+椎间盘变窄。金黄色葡萄球菌最常见。", "https://radiopaedia.org/search?q=discitis+osteomyelitis&scope=all"),
        Imaging("脊柱结核(Pott病)", "MRI(增强)", "骨科/感染", "椎体前部T1低/T2高+寒性脓肿(椎旁/腰大肌流注)。相对椎间盘保留(与化脓性鉴别)。驼背畸形。", "https://radiopaedia.org/search?q=Pott+disease+spine&scope=all"),
        Imaging("骨质疏松(DXA/骨密度)", "DXA/QCT", "内分泌/骨科", "T-score ≥ -1正常; -1 ~ -2.5骨量减少; ≤ -2.5骨质疏松; ≤ -2.5+骨折=严重骨质疏松。腰椎+髋部测量。", "https://radiopaedia.org/search?q=osteoporosis+DXA&scope=all"),
        Imaging("Paget骨病", "X线/全身骨扫描", "骨科/代谢", "X线:局限性骨骼膨大+骨纹粗糙紊乱+骨皮质增厚。颅骨'棉花团状'改变。骨盆增厚+髋臼内陷。", "https://radiopaedia.org/search?q=Paget+disease+bone&scope=all"),
        Imaging("肾性骨营养不良", "X线(手/骨骼)", "肾内科/内分泌", "骨质疏松+骨软化/Looser带(假性骨折)+继发性甲旁亢(棕色瘤/骨膜下吸收)。脊柱'橄榄球衣征'(终板硬化)。", "https://radiopaedia.org/search?q=renal+osteodystrophy&scope=all"),
        // === 风湿免疫 ===
        Imaging("类风湿关节炎(RA)", "X线(双手)/MRI", "风湿/骨科", "对称性关节间隙均匀变窄+边缘性骨质侵蚀+软组织肿胀。晚期:尺侧偏斜+天鹅颈/纽扣畸形+关节融合。", "https://radiopaedia.org/search?q=rheumatoid+arthritis+hand+imaging&scope=all"),
        Imaging("系统性硬化症(硬皮病)", "X线/CT(HRCT)", "风湿/呼吸", "手部:肢端骨质溶解+钙质沉着(指腹钙化)。胃:食管扩张+蠕动消失。肺:NSIP为主(HRCT)。", "https://radiopaedia.org/search?q=systemic+sclerosis+imaging&scope=all"),
        Imaging("银屑病关节炎(PsA)", "X线/MRI", "风湿/骨科", "不对称性关节破坏+DIP受累+'笔帽征'(铅笔在杯中)。骶髂关节炎(常不对称)+肌腱端炎。", "https://radiopaedia.org/search?q=psoriatic+arthritis+imaging&scope=all"),
        Imaging("干燥综合征(肺部)", "CT(HRCT)", "风湿/呼吸", "LIP(淋巴细胞间质性肺炎):随机分布磨玻璃影+囊性变。NSIP或滤泡性细支气管炎也可见。", "https://radiopaedia.org/search?q=sjogren+syndrome+lung+imaging&scope=all"),
        // === 眼科补充 ===
        Imaging("视网膜脱离", "超声(B超)", "眼科", "视网膜脱离：玻璃体内膜状高回声+与视盘连接。孔源性(年龄/近视)vs牵拉性(糖尿病)。紧急修复!", "https://radiopaedia.org/search?q=retinal+detachment+ultrasound&scope=all"),
        Imaging("年龄相关性黄斑变性(AMD)", "OCT", "眼科", "干性:玻璃膜疣+地图状萎缩RPE。湿性:CNVM(脉络膜新生血管膜)→视网膜下积液/出血。", "https://radiopaedia.org/search?q=age+related+macular+degeneration+OCT&scope=all"),
        Imaging("糖尿病视网膜病变", "眼底照相+FFA/OCTA", "眼科/内分泌", "微动脉瘤→出血+渗出→棉绒斑→新生血管(增殖性)+玻璃体积血→视网膜前纤维增殖→牵拉脱离。", "https://radiopaedia.org/search?q=diabetic+retinopathy+imaging&scope=all"),
        // === 生殖泌尿补充 ===
        Imaging("睾丸扭转", "超声(彩色多普勒)", "泌尿/急诊", "睾丸肿大+回声不均+多普勒无血流(或明显减少)。与附睾炎(高血流)鉴别关键。<6h复位率>90%，>24h几乎0%。", "https://radiopaedia.org/search?q=testicular+torsion+ultrasound&scope=all"),
        Imaging("精索静脉曲张", "超声(彩色多普勒)", "泌尿", "立位+瓦氏动作：精索静脉内径>2mm+持续逆行血流>2s。左侧更常见。", "https://radiopaedia.org/search?q=varicocele+ultrasound&scope=all"),
        // === 消化补充 ===
        Imaging("结直肠癌", "CT(增强)+MRI(直肠)", "消化", "结肠壁不规则增厚+管腔狭窄(苹果核征)+周围淋巴结转移。肝转移(门脉期最明显)。", "https://radiopaedia.org/search?q=colorectal+cancer+CT&scope=all"),
        Imaging("胃间质瘤(GIST)", "CT(增强)", "消化", "胃壁外生性肿块+均匀或不均匀强化+中央坏死/出血可呈液液平面。肝转移常见。", "https://radiopaedia.org/search?q=gastric+GIST&scope=all"),
        Imaging("腹膜假性黏液瘤(PMP)", "CT/MRI", "消化", "腹膜广泛包裹+肝/脾表面'扇贝征'+大量黏液性腹水。阑尾黏液性肿瘤破裂来源。", "https://radiopaedia.org/search?q=pseudomyxoma+peritonei&scope=all"),
        // === 儿科补充 ===
        Imaging("先天性心脏病-超声心动图", "超声心动图(TTE)", "儿科/心内", "VSD/ASD/PDA/TOF法洛四联症/TGA大动脉转位。评估分流方向/压力/右室压。", "https://radiopaedia.org/search?q=congenital+heart+disease+echocardiography&scope=all"),
        Imaging("胆道闭锁", "超声/磁共振胰胆管(MRCP)", "儿科", "超声：三角形索状征(肝门纤维斑块)+小胆囊或无胆囊。新生儿直接高胆红素血症最重要鉴别诊断。", "https://radiopaedia.org/search?q=biliary+atresia+imaging&scope=all"),
        // === 更多血管 ===
        Imaging("肠系膜缺血", "CTA(腹部)", "血管/急诊", "急性:动脉栓塞(SMA)+静脉血栓(MVT)。CTA：肠壁无强化+肠壁积气+腹腔积液。死亡率60-80%。", "https://radiopaedia.org/search?q=mesenteric+ischaemia+CT&scope=all"),
        Imaging("上腔静脉综合征(SVCS)", "CTV/MRI", "血管/胸外", "上腔静脉阻塞→侧支静脉扩张。恶性最常见(肺癌/淋巴瘤)。", "https://radiopaedia.org/search?q=superior+vena+cava+obstruction&scope=all"),
        // === 麻醉/呼吸补充 ===
        Imaging("阻塞性睡眠呼吸暂停(OSA)影像学", "CT(气道三维)", "ENT/麻醉", "上气道最小横截面积(MCA)减少+腭后/舌后区域显著。", "https://radiopaedia.org/search?q=obstructive+sleep+apnoea+imaging&scope=all"),
        // === 更多胸部 ===
        Imaging("胸膜间皮瘤", "CT(增强)/PET-CT", "胸外科", "弥漫胸膜结节状增厚(>1cm)+胸腔积液+纵隔胸膜受侵。石棉接触史。VATS胸膜活检确诊。", "https://radiopaedia.org/search?q=pleural+mesothelioma&scope=all"),
        // === 更多肌肉骨骼 ===
        Imaging("外伤性肌肉损伤(分级)", "MRI/超声", "骨科/运动医学", "I级(轻度):肌腱/肌肉轻度挫伤。II级(中度):部分肌纤维断裂+血肿。III级(重度):肌纤维完全断裂+回缩。", "https://radiopaedia.org/search?q=muscle+strain+grading+MRI&scope=all"),
        Imaging("胫骨平台骨折(Schatzker)", "CT(三维重建)", "骨科", "Schatzker I-VI型。V/VI型累及干骺端分离→双重钢板。", "https://radiopaedia.org/search?q=tibial+plateau+fracture+Schatzker&scope=all"),
        // === 脊柱补充 ===
        Imaging("马尾综合征(Cauda Equina)", "MRI(增强/急诊)", "神经/骨科", "马尾神经受压(巨大椎间盘突出/肿瘤/血肿)→鞍区感觉消失+尿潴留+下肢无力。MRI急诊<24h手术。", "https://radiopaedia.org/search?q=cauda+equina+syndrome&scope=all"),
        Imaging("脊髓空洞症(Syringomyelia)", "MRI", "神经/骨科", "脊髓中央管T2高信号囊腔(纵行)，与小脑扁桃体下疝(Chiari I型)密切关联。", "https://radiopaedia.org/search?q=syringomyelia&scope=all"),
        // === 骨与关节补充 ===
        Imaging("SLAP撕裂(上盂唇前后撕裂)", "MRI(关节造影)", "骨科/运动医学", "SLAP I-IV型→上盂唇剥离或桶柄状撕裂。MRI关节造影比常规MRI更准确。投掷运动员常见。", "https://radiopaedia.org/search?q=SLAP+tear&scope=all"),
        Imaging("Bankart损伤", "MRI(关节造影)/CT(MRA)", "骨科/运动医学", "肩关节前下盂唇撕裂+前下盂唇骨性缺损(Bony Bankart)-骨缺损>20-25%=需要Latarjet手术。", "https://radiopaedia.org/search?q=Bankart+lesion&scope=all"),
        Imaging("分离性骨软骨炎(OCD)", "MRI/X线", "骨科/运动医学", "关节软骨+软骨下骨分离(>通常膝关节内侧股骨髁)，骨软骨块脱落→游离体。青少年11-21岁。", "https://radiopaedia.org/search?q=osteochondritis+dissecans&scope=all"),
        Imaging("月骨坏死(Kienbock病)", "X线/MRI", "骨科/手腕", "月骨T1低信号+塌陷(Lichtman分期I-IV)。I-II期可保守/血运重建，III-IV需手术。", "https://radiopaedia.org/search?q=Kienbock+disease&scope=all"),
        // === 血管补充 ===
        Imaging("大动脉炎(Takayasu arteritis)", "CTA/MRA", "血管/风湿", "年轻女性主动脉弓+分支长段管壁环形增厚+管腔狭窄/闭塞。双期(动脉期/延迟期)观察壁强化(活动性)。", "https://radiopaedia.org/search?q=Takayasu+arteritis&scope=all"),
        Imaging("冠状动脉钙化积分(CAC)", "CT(平扫低剂量)", "心内科/筛查", "Agatston评分:CAC 0(极低危)→1-100→100-400→>400(高危)。0分=10年ASCVD<5%。", "https://radiopaedia.org/search?q=coronary+artery+calcium+score&scope=all"),
        // === 腹部补充 ===
        Imaging("正常腹部CT解剖", "CT(增强)", "消化/教学", "门脉期：肝+脾+胰+双肾+肾上腺+胆囊+胆总管<7mm+小肠直径<3cm+结肠直径<6cm。", ""),
        Imaging("肠脂垂炎(Epiploic Appendagitis)", "CT(增强)", "消化/急诊", "结肠旁脂肪密度增高+环征(高密度薄边缘)+中心点状高密度(中央静脉)，无周围肠壁增厚。自限性保守治疗。", "https://radiopaedia.org/search?q=epiploic+appendagitis&scope=all"),
        Imaging("网膜梗死后/大网膜扭转", "CT(增强)", "消化/急诊", "大网膜区域脂肪密度增高(比肠脂垂炎更大更模糊)，无环征。自限性。", "https://radiopaedia.org/search?q=omental+infarction&scope=all"),
        Imaging("肠系膜淋巴结炎", "超声/CT", "消化/儿科", "右下腹肿大淋巴结>3个+回肠末端正常(与阑尾炎鉴别关键!)。", "https://radiopaedia.org/search?q=mesenteric+adenitis&scope=all"),
        // === 胸科补充 ===
        Imaging("正常胸部CT解剖(纵隔窗+肺窗)", "CT", "呼吸/教学", "肺窗:两侧肺野透亮度均匀+叶间裂无偏移。纵隔窗:心/大血管/气管/主支气管/食管/淋巴结(<1cm)。", ""),
        Imaging("后纵隔肿物(神经源性)", "CT/MRI", "胸外科", "后纵隔+椎旁沟内靠近交感链/神经根的软组织肿块。成人多为神经鞘瘤。儿童为神经母细胞瘤/神经节瘤。", "https://radiopaedia.org/search?q=posterior+mediastinal+mass&scope=all"),
        Imaging("前纵隔畸胎瘤", "CT/MRI", "胸外科", "前纵隔囊肿性肿块+脂肪/钙化/液体/毛发等不均质成分。良性占多数。", "https://radiopaedia.org/search?q=mediastinal+teratoma&scope=all"),
        // === 神经补充 ===
        Imaging("正常头颅CT解剖", "CT(平扫)", "神经/教学", "灰白质界面清晰，脑沟+外侧裂+四叠体池+鞍上池可见，中线居中，颅骨对称无异常密度。", ""),
        Imaging("垂体卒中(Pituitary Apoplexy)", "MRI(增强)", "神经/内分泌", "垂体腺瘤内急性出血(磁敏感序列信号异常)+蝶鞍扩张+视交叉受压。急症!急性重度头痛+视交叉综合征+垂体功能减低→激素+急诊手术。", "https://radiopaedia.org/search?q=pituitary+apoplexy&scope=all"),
        Imaging("小脑梗死", "MRI(DWI)+CT(排除出血/脑积水)", "神经", "小脑后下/前下/上动脉供血区DWI高信号+ADC低，水肿可压迫第四脑室/脑干→急诊脑室外引流可能。", "https://radiopaedia.org/search?q=cerebellar+infarction&scope=all"),
        // === 儿科补充 ===
        Imaging("正常新生儿头部超声(颅脑)", "超声(经前囟)", "儿科/神经", "正常脑室前角+颞角无扩张(无脑室出血/脑室增宽)。生发基质+脉络膜回声正常。", "https://radiopaedia.org/search?q=normal+neonatal+cranial+ultrasound&scope=all"),
        Imaging("正常小儿髋关节超声(Graf)", "超声", "儿科/骨科", "α角>60°(骨性臼顶，α<43°脱位)，β角<55°正常，股骨头正常位于髋臼内。", "https://radiopaedia.org/search?q=Graf+ultrasound+hip&scope=all"),
        // === 皮肤科补充 ===
        Imaging("正常皮肤+皮下组织超声", "超声(高频>15MHz)", "皮肤科", "分层解剖：表皮+真皮+皮下脂肪层。各层厚度均匀对称+筋膜层连续清晰+无异常血流。", ""),
        // === 骨折/骨科补充 ===
        Imaging("桡骨头骨折(Mason分型)", "X线/CT", "骨科", "Mason I(无移位)→保守; II(移位>2mm)→ORIF; III(粉碎)→切除/置换; IV(合并脱位)→重建。", "https://radiopaedia.org/search?q=radial+head+fracture&scope=all"),
        Imaging("桡骨远端骨折(Frykman分型)", "X线/CT", "骨科", "Frykman I-VIII(关节内/外+尺骨茎突)。Colles(背侧); Smith(掌侧反Colles); Barton(关节内骨折脱位)。", "https://radiopaedia.org/search?q=distal+radius+fracture&scope=all"),
        Imaging("椎弓崩裂/脊柱滑脱(Spondylolysis)", "CT(矢状+轴位)/MRI", "骨科", "椎弓峡部缺损(Scottie狗颈圈断裂)。Meyerding分级:I(<25%)II(25-50)III(50-75)IV(>75)V(完全脱位=ptosis)。", "https://radiopaedia.org/search?q=spondylolisthesis&scope=all"),
        Imaging("骶骨不全骨折", "MRI/CT", "骨科/老年", "骶骨翼T1低/T2高信号(骨折线)+垂直方向。骨质疏松/放疗后。H型骨折=双侧+中轴=最严重。", "https://radiopaedia.org/search?q=sacral+insufficiency+fracture&scope=all"),
        Imaging("足踝-副舟骨/疼痛性副舟骨", "X线/MRI", "骨科/足踝", "2型副舟骨伴疼痛(内踝前方)+胫后肌腱附着异常/变性+骨髓水肿(疼痛性)。", "https://radiopaedia.org/search?q=accessory+navicular+syndrome&scope=all"),
        Imaging("髌骨骨折", "X线(正侧/轴位)/CT", "骨科/膝关节", "横断/粉碎/下极撕脱。>2mm间隙=移位需ORIF。MRI评估伸膝装置完整性+骨软骨骨折。", "https://radiopaedia.org/search?q=patella+fracture&scope=all"),
        Imaging("DISH(弥漫性特发性骨肥厚)", "X线/CT", "骨科/风湿", "前纵韧带骨化连接>=4个连续椎体(Sinding-Larsen)，右侧流注状。'蜡烛火焰'外观。与AS鉴别：骶髂关节正常+无椎体侵蚀。", "https://radiopaedia.org/search?q=diffuse+idiopathic+skeletal+hyperostosis&scope=all"),
        // === 消化补充 ===
        Imaging("乙状结肠扭转", "X线(腹部)/CT", "消化/急诊", "X线:巨大倒U形扩张结肠('咖啡豆征'), 乙状结肠梗阻点。CT:肠系膜漩涡征(whirl sign)。内镜复位(>80%成功)。", "https://radiopaedia.org/search?q=sigmoid+volvulus&scope=all"),
        Imaging("急性结肠憩室炎", "CT(增强)", "消化", "结肠憩室+壁增厚>5mm+周围脂肪条索+脓肿/微小穿孔。Hinchey分级Ia-IV。", "https://radiopaedia.org/search?q=acute+diverticulitis+CT&scope=all"),
        Imaging("Ogilvie综合征(急性结肠假性梗阻)", "CT/X线", "消化/急诊", "盲肠+升结肠扩张(>9cm为穿孔风险>12cm危险!)，远端结肠口径正常(无机梗阻)。新斯的明(neostigmine)治疗。", "https://radiopaedia.org/search?q=acute+colonic+pseudo-obstruction&scope=all"),
        Imaging("盲肠扭转", "CT", "消化/急诊", "盲肠扭转→中上腹部扩张，远端结肠塌陷。'肾形'扩张盲肠。鸟嘴征。需急诊手术(缺血坏死率高)。", "https://radiopaedia.org/search?q=caecal+volvulus&scope=all"),
        // === 血管补充 ===
        Imaging("May-Thurner综合征(髂静脉受压)", "CTV/MR+超声", "血管", "右髂总动脉压迫左髂总静脉→左下肢DVT+慢性静脉功能不全。年轻女性多见。血管内支架治疗。", "https://radiopaedia.org/search?q=May+Thurner+syndrome&scope=all"),
        Imaging("胡桃夹综合征(Nutcracker)", "CTA+超声", "血管", "左肾静脉被SMA与主动脉之间压迫→血尿/左侧腹部痛。超声：LRV直径比>5:1。", "https://radiopaedia.org/search?q=Nutcracker+syndrome&scope=all"),
        Imaging("正中弓状韧带综合征(MALS)", "CTA(呼气+吸气相)/超声", "血管", "腹腔动脉被弓状韧带压迫(呼气相加重)，呼气相狭窄>吸气相>50%。餐后上腹痛+体重下降。", "https://radiopaedia.org/search?q=median+arcuate+ligament+syndrome&scope=all"),
        // === 神经补充 ===
        Imaging("Moyamoya病", "MRI+MRA/CTA/DSA", "神经/血管", "双侧颈内动脉末段渐进性狭窄/闭塞+烟雾样侧支血管('烟雾吸入'弥漫+模糊)。铃木分期I-VI。缺血/出血均可。", "https://radiopaedia.org/search?q=Moyamoya+disease&scope=all"),
        Imaging("Chiari I型畸形", "MRI(矢状位)", "神经/先天", "小脑扁桃体下疝>5mm通过枕骨大孔(小脑扁桃体下疝)+脊髓空洞症(>50%伴有)。慢性Valsalva性头痛。", "https://radiopaedia.org/search?q=Chiari+I+malformation&scope=all"),
        Imaging("正常压力脑积水(NPH)", "MRI(矢状+冠状)", "神经", "DESH(不成比例扩大的蛛网膜下腔脑积水)。Evan's index>0.3+胼胝体角<90°+高脑凸面蛛网膜下腔狭窄。脑室引流前腰穿放液可评估获益。", "https://radiopaedia.org/search?q=normal+pressure+hydrocephalus+DESH&scope=all"),
        Imaging("Rathke裂囊肿", "MRI(增强)", "神经/内分泌", "鞍内囊性占位(通常<1cm)，T1信号根据蛋白浓度变化+T2高/低信号+囊壁无强化(少数轻度强化)。", "https://radiopaedia.org/search?q=Rathke+cleft+cyst&scope=all"),
        Imaging("硬膜动静脉瘘(dAVF)", "DSA(金标准)/MRI", "神经/血管", "增强血管(静脉早期充盈)→异常的皮层静脉引流(=高危 → 应紧急治疗)。搏动性耳鸣+颅内出血。", "https://radiopaedia.org/search?q=dural+arteriovenous+fistula&scope=all"),
        // === 急危重症补充 ===
        Imaging("Fournier坏疽(会阴部坏死性筋膜炎)", "CT(增强)", "急诊/泌尿", "会阴部软组织积气+筋膜增厚+无强化坏死区域。男性>女性。糖尿病+免疫低下者。急诊手术清创(多科联合)。", "https://radiopaedia.org/search?q=Fournier+gangrene&scope=all"),
        Imaging("气肿性肾盂肾炎", "CT(平扫)", "急诊/泌尿", "肾实质+肾周气囊征(气体)，CT可区分I型(局部)/II型(弥漫→死亡率更高)。糖尿病+妇女常见。急诊肾切除可能。", "https://radiopaedia.org/search?q=emphysematous+pyelonephritis&scope=all"),
        Imaging("气肿性胆囊炎", "CT/超声", "急诊/消化", "胆囊壁/腔内气体，超声:环状气体高回声+后方混响伪差('脏的声影')。急诊手术。", "https://radiopaedia.org/search?q=emphysematous+cholecystitis&scope=all"),
        Imaging("Boerhaave综合征(自发性食管破裂)", "CT(增强)+食管造影(水溶性)", "急诊/胸外", "食管下段透壁不完全破裂+纵隔积气+左侧胸腔积液(胸膜破)+气肿。呕吐后急性剧烈胸痛。急诊手术+引流。", "https://radiopaedia.org/search?q=Boerhaave+syndrome&scope=all"),
        // === 介入放射补充 ===
        Imaging("TIPS(经颈静脉肝内门体分流术)", "超声(多普勒)+CT", "介入放射", "肝-门静脉分流(肝静脉→右门静脉分支)。超声:分流道通畅血流速度50-200cm/s+流出静脉流速。狭窄/阻塞=再次干预。", "https://radiopaedia.org/search?q=TIPS+transjugular+intrahepatic+portosystemic+shunt&scope=all"),
        Imaging("支气管动脉栓塞(BAE-咯血)", "CT血管成像+支气管动脉DSA", "介入放射", "咯血最常见来源:支气管动脉(90%)。血管栓塞:聚乙烯醇颗粒+微弹簧圈。注意勿栓塞脊髓动脉(前脊动脉源于肋间/支气管动脉)。", "https://radiopaedia.org/search?q=bronchial+artery+embolization&scope=all"),
        Imaging("子宫肌瘤栓塞(UFE)", "DSA", "介入放射", "双侧子宫动脉超选择插管→栓塞肌瘤微球(血流丰富肌瘤优先吸收)。MRI术后评估肌瘤坏死(无强化)。", "https://radiopaedia.org/search?q=uterine+fibroid+embolization&scope=all"),
        // === 肿瘤补充 ===
        Imaging("腹膜癌病(Peritoneal Carcinomatosis)", "CT(增强)+MRI", "肿瘤/消化", "腹膜结节状增厚+大网膜饼征+腹水。来源：卵巢/结直肠/胃/胰腺。PCI评分评估范围。", "https://radiopaedia.org/search?q=peritoneal+carcinomatosis&scope=all"),
        Imaging("淋巴瘤(结内+结外)", "CT/PET-CT", "肿瘤/血液", "多区域淋巴肿大(有多个界限清晰的球形团块)。结外：脾/肝/肾/肾上腺/骨骼。PET-CT用于分期+疗效评估。", "https://radiopaedia.org/search?q=lymphoma+imaging&scope=all"),
        // === 血液科补充 ===
        Imaging("多发性骨髓瘤(骨破坏)", "X线(全身骨显像)/CT", "血液/肿瘤", "颅骨'雨滴状'溶解、椎体压缩骨折、肋骨/锁骨/肱骨多发穿凿状溶骨。X线阴性者做全身MRI或PET-CT。", "https://radiopaedia.org/search?q=multiple+myeloma+bone&scope=all"),
        Imaging("血友病性关节病", "X线/MRI", "血液/骨科", "反复关节积血→骨骺过度生长(膝/肘/踝)。关节间隙变窄+软骨下囊变(Arnold分期)。MRI含铁血黄素沉积(T2低信号,梯度回波开花征)。", "https://radiopaedia.org/search?q=haemophilic+arthropathy&scope=all"),
        Imaging("骨髓纤维化", "X线/MRI", "血液/肿瘤", "骨质密度弥漫增高(弥漫性骨硬化)+骨髓腔闭塞。脾高度肿大(髓外造血)。MR:T1+T2弥漫低信号(造血骨髓)。", "https://radiopaedia.org/search?q=myelofibrosis+imaging&scope=all"),
        // === 康复科补充 ===
        Imaging("异位骨化", "X线/CT(三维)/三相骨扫描", "骨科/康复", "关节周围/肌肉内逐渐成熟骨质形成(>2-4周出现)。关节活动受限+压迫神经血管。好发:髋臼骨折/截瘫/烧伤。", "https://radiopaedia.org/search?q=heterotopic+ossification&scope=all"),
        Imaging("肩峰下撞击综合征", "MRI/超声(动态)", "骨科/康复", "肩峰下间隙狭窄(肩峰形态Bigliani分型: I型扁平/II型弧形/III型钩状)。肩袖出口狭窄→肱骨头上移+肩峰下滑囊积液。", "https://radiopaedia.org/search?q=subacromial+impingement+MRI&scope=all"),
        Imaging("肩袖钙化性肌腱炎", "X线/超声", "骨科/康复", "冈上肌腱内钙化沉积(形成/静止/吸收三阶段)。超声:高回声钙化灶+后方声影/混响伪差。急性吸收期剧痛(钙化性滑囊炎)。", "https://radiopaedia.org/search?q=calcific+tendinitis+shoulder&scope=all"),
    )
    val categories = all.map { it.system }.distinct().sorted()
    val modalities = all.map { it.modality }.distinct().sorted()
    fun search(query: String) = all.filter {
        val q = query.lowercase()
        it.title.lowercase().contains(q) || it.description.lowercase().contains(q) || it.system.lowercase().contains(q)
    }
    fun bySystem(sys: String) = all.filter { it.system == sys }
}
