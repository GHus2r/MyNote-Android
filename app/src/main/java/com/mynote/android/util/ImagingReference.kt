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
    )
    val categories = all.map { it.system }.distinct().sorted()
    val modalities = all.map { it.modality }.distinct().sorted()
    fun search(query: String) = all.filter {
        val q = query.lowercase()
        it.title.lowercase().contains(q) || it.description.lowercase().contains(q) || it.system.lowercase().contains(q)
    }
    fun bySystem(sys: String) = all.filter { it.system == sys }
}
