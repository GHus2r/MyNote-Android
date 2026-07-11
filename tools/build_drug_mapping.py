"""Build Chinese→English drug name mapping for DDInter autocomplete."""
import json

# Load DDInter index
json_path = 'F:/NOTE/mynoteANDROID/MyNote-Android/app/src/main/assets/ddinter_interactions.json'
with open(json_path, 'r', encoding='utf-8') as f:
    data = json.load(f)
idx = data.get('ix', {})

# Comprehensive Chinese → English drug name mapping
mapping = {
    # === 抗生素 ===
    '头孢曲松': 'ceftriaxone', '头孢呋辛': 'cefuroxime', '头孢他啶': 'ceftazidime',
    '左氧氟沙星': 'levofloxacin', '莫西沙星': 'moxifloxacin', '环丙沙星': 'ciprofloxacin',
    '阿奇霉素': 'azithromycin', '克拉霉素': 'clarithromycin', '甲硝唑': 'metronidazole',
    '氟康唑': 'fluconazole', '伏立康唑': 'voriconazole', '万古霉素': 'vancomycin',
    '利奈唑胺': 'linezolid', '美罗培南': 'meropenem', '阿莫西林': 'amoxicillin',
    '阿昔洛韦': 'acyclovir', '恩替卡韦': 'entecavir', '替诺福韦': 'tenofovir',
    '利福平': 'rifampin', '异烟肼': 'isoniazid', '乙胺丁醇': 'ethambutol',
    '吡嗪酰胺': 'pyrazinamide', '头孢克洛': 'cefaclor', '多西环素': 'doxycycline',
    '米诺环素': 'minocycline', '庆大霉素': 'gentamicin', '阿米卡星': 'amikacin',
    '克林霉素': 'clindamycin', '替加环素': 'tigecycline', '达托霉素': 'daptomycin',
    '厄他培南': 'ertapenem', '头孢吡肟': 'cefepime', '头孢西丁': 'cefoxitin',
    '头孢地尼': 'cefdinir', '两性霉素B': 'amphotericin b', '伊曲康唑': 'itraconazole',
    '特比萘芬': 'terbinafine', '奥司他韦': 'oseltamivir', '更昔洛韦': 'ganciclovir',
    '利巴韦林': 'ribavirin', '卡泊芬净': 'caspofungin', '磺胺甲恶唑': 'sulfamethoxazole',
    '甲氧苄啶': 'trimethoprim',

    # === NSAIDs / 镇痛 ===
    '布洛芬': 'ibuprofen', '双氯芬酸': 'diclofenac', '双氯芬酸钠': 'diclofenac',
    '阿司匹林': 'aspirin', '对乙酰氨基酚': 'acetaminophen', '塞来昔布': 'celecoxib',
    '依托考昔': 'etoricoxib', '萘普生': 'naproxen', '吲哚美辛': 'indomethacin',
    '酮咯酸': 'ketorolac', '美洛昔康': 'meloxicam', '帕瑞昔布': 'parecoxib',
    '曲马多': 'tramadol', '吗啡': 'morphine', '芬太尼': 'fentanyl',
    '羟考酮': 'oxycodone', '可待因': 'codeine', '哌替啶': 'meperidine',
    '纳洛酮': 'naloxone', '利多卡因': 'lidocaine', '罗哌卡因': 'ropivacaine',
    '布比卡因': 'bupivacaine',

    # === 心血管 ===
    '硝苯地平': 'nifedipine', '氨氯地平': 'amlodipine', '非洛地平': 'felodipine',
    '地尔硫卓': 'diltiazem', '维拉帕米': 'verapamil', '美托洛尔': 'metoprolol',
    '比索洛尔': 'bisoprolol', '阿替洛尔': 'atenolol', '卡维地洛': 'carvedilol',
    '普萘洛尔': 'propranolol', '拉贝洛尔': 'labetalol', '索他洛尔': 'sotalol',
    '卡托普利': 'captopril', '贝那普利': 'benazepril', '依那普利': 'enalapril',
    '赖诺普利': 'lisinopril', '雷米普利': 'ramipril', '培哚普利': 'perindopril',
    '缬沙坦': 'valsartan', '厄贝沙坦': 'irbesartan', '氯沙坦': 'losartan',
    '替米沙坦': 'telmisartan', '坎地沙坦': 'candesartan', '奥美沙坦': 'olmesartan',
    '呋塞米': 'furosemide', '螺内酯': 'spironolactone', '氢氯噻嗪': 'hydrochlorothiazide',
    '托拉塞米': 'torsemide', '吲达帕胺': 'indapamide', '氨苯蝶啶': 'triamterene',
    '硝酸甘油': 'nitroglycerin', '单硝酸异山梨酯': 'isosorbide mononitrate',
    '硝酸异山梨酯': 'isosorbide dinitrate',
    '胺碘酮': 'amiodarone', '地高辛': 'digoxin', '普罗帕酮': 'propafenone',
    '腺苷': 'adenosine', '决奈达隆': 'dronedarone',
    '多巴胺': 'dopamine', '多巴酚丁胺': 'dobutamine', '去甲肾上腺素': 'norepinephrine',
    '肾上腺素': 'epinephrine', '去氧肾上腺素': 'phenylephrine', '米力农': 'milrinone',
    '阿托伐他汀': 'atorvastatin', '瑞舒伐他汀': 'rosuvastatin', '辛伐他汀': 'simvastatin',
    '普伐他汀': 'pravastatin', '氟伐他汀': 'fluvastatin', '匹伐他汀': 'pitavastatin',
    '依折麦布': 'ezetimibe', '非诺贝特': 'fenofibrate', '吉非罗齐': 'gemfibrozil',

    # === 抗凝/抗血小板 ===
    '华法林': 'warfarin', '肝素': 'heparin', '低分子肝素': 'enoxaparin',
    '依诺肝素': 'enoxaparin', '那屈肝素': 'nadroparin', '利伐沙班': 'rivaroxaban',
    '阿哌沙班': 'apixaban', '达比加群': 'dabigatran', '依度沙班': 'edoxaban',
    '磺达肝癸钠': 'fondaparinux', '氯吡格雷': 'clopidogrel', '替格瑞洛': 'ticagrelor',
    '双嘧达莫': 'dipyridamole', '替罗非班': 'tirofiban', '西洛他唑': 'cilostazol',

    # === 消化系统 ===
    '奥美拉唑': 'omeprazole', '泮托拉唑': 'pantoprazole', '兰索拉唑': 'lansoprazole',
    '雷贝拉唑': 'rabeprazole', '埃索美拉唑': 'esomeprazole',
    '法莫替丁': 'famotidine', '雷尼替丁': 'ranitidine', '西咪替丁': 'cimetidine',
    '硫糖铝': 'sucralfate', '甲氧氯普胺': 'metoclopramide', '多潘立酮': 'domperidone',
    '昂丹司琼': 'ondansetron', '格拉司琼': 'granisetron', '洛哌丁胺': 'loperamide',
    '乳果糖': 'lactulose', '柳氮磺吡啶': 'sulfasalazine', '美沙拉嗪': 'mesalamine',
    '熊去氧胆酸': 'ursodiol', '米索前列醇': 'misoprostol',

    # === 呼吸系统 ===
    '氨溴索': 'ambroxol', '乙酰半胱氨酸': 'acetylcysteine',
    '沙丁胺醇': 'albuterol', '特布他林': 'terbutaline', '福莫特罗': 'formoterol',
    '沙美特罗': 'salmeterol', '异丙托溴铵': 'ipratropium', '噻托溴铵': 'tiotropium',
    '布地奈德': 'budesonide', '氟替卡松': 'fluticasone', '孟鲁司特': 'montelukast',
    '茶碱': 'theophylline', '氨茶碱': 'aminophylline',

    # === 内分泌/糖尿病 ===
    '二甲双胍': 'metformin', '格列美脲': 'glimepiride', '格列本脲': 'glyburide',
    '格列齐特': 'gliclazide', '格列吡嗪': 'glipizide',
    '西格列汀': 'sitagliptin', '沙格列汀': 'saxagliptin', '维格列汀': 'vildagliptin',
    '利格列汀': 'linagliptin', '达格列净': 'dapagliflozin', '恩格列净': 'empagliflozin',
    '卡格列净': 'canagliflozin', '利拉鲁肽': 'liraglutide', '司美格鲁肽': 'semaglutide',
    '度拉糖肽': 'dulaglutide', '艾塞那肽': 'exenatide', '替尔泊肽': 'tirzepatide',
    '吡格列酮': 'pioglitazone', '罗格列酮': 'rosiglitazone',
    '瑞格列奈': 'repaglinide', '那格列奈': 'nateglinide', '阿卡波糖': 'acarbose',
    '胰岛素': 'insulin', '甘精胰岛素': 'insulin glargine', '门冬胰岛素': 'insulin aspart',

    # === 激素/代谢 ===
    '地塞米松': 'dexamethasone', '甲泼尼龙': 'methylprednisolone',
    '泼尼松': 'prednisone', '泼尼松龙': 'prednisolone', '氢化可的松': 'hydrocortisone',
    '左甲状腺素钠': 'levothyroxine', '甲巯咪唑': 'methimazole', '丙硫氧嘧啶': 'propylthiouracil',
    '阿仑膦酸': 'alendronate', '唑来膦酸': 'zoledronic acid',

    # === 神经/精神 ===
    '地西泮': 'diazepam', '咪达唑仑': 'midazolam', '劳拉西泮': 'lorazepam',
    '阿普唑仑': 'alprazolam', '氯硝西泮': 'clonazepam',
    '苯巴比妥': 'phenobarbital', '丙戊酸钠': 'valproic acid', '丙戊酸': 'valproic acid',
    '左乙拉西坦': 'levetiracetam', '卡马西平': 'carbamazepine', '奥卡西平': 'oxcarbazepine',
    '加巴喷丁': 'gabapentin', '普瑞巴林': 'pregabalin', '拉莫三嗪': 'lamotrigine',
    '托吡酯': 'topiramate', '苯妥英钠': 'phenytoin', '苯妥英': 'phenytoin',
    '拉考沙胺': 'lacosamide', '舍曲林': 'sertraline', '艾司西酞普兰': 'escitalopram',
    '氟西汀': 'fluoxetine', '帕罗西汀': 'paroxetine', '西酞普兰': 'citalopram',
    '文拉法辛': 'venlafaxine', '度洛西汀': 'duloxetine', '米氮平': 'mirtazapine',
    '曲唑酮': 'trazodone', '安非他酮': 'bupropion',
    '奥氮平': 'olanzapine', '利培酮': 'risperidone', '喹硫平': 'quetiapine',
    '阿立哌唑': 'aripiprazole', '氯氮平': 'clozapine', '氟哌啶醇': 'haloperidol',
    '氯丙嗪': 'chlorpromazine', '碳酸锂': 'lithium', '锂盐': 'lithium',
    '多奈哌齐': 'donepezil', '美金刚': 'memantine', '左旋多巴': 'levodopa',

    # === 抗肿瘤/免疫 ===
    '甲氨蝶呤': 'methotrexate', '环磷酰胺': 'cyclophosphamide', '氟尿嘧啶': 'fluorouracil',
    '顺铂': 'cisplatin', '卡铂': 'carboplatin', '奥沙利铂': 'oxaliplatin',
    '多柔比星': 'doxorubicin', '表柔比星': 'epirubicin', '博来霉素': 'bleomycin',
    '紫杉醇': 'paclitaxel', '多西他赛': 'docetaxel', '长春新碱': 'vincristine',
    '吉西他滨': 'gemcitabine', '卡培他滨': 'capecitabine', '伊立替康': 'irinotecan',
    '依托泊苷': 'etoposide', '替莫唑胺': 'temozolomide', '培美曲塞': 'pemetrexed',
    '环孢素': 'cyclosporine', '他克莫司': 'tacrolimus', '吗替麦考酚酯': 'mycophenolate',
    '伊马替尼': 'imatinib', '厄洛替尼': 'erlotinib', '吉非替尼': 'gefitinib',
    '索拉非尼': 'sorafenib', '舒尼替尼': 'sunitinib', '奥希替尼': 'osimertinib',
    '克唑替尼': 'crizotinib', '贝伐珠单抗': 'bevacizumab', '曲妥珠单抗': 'trastuzumab',
    '利妥昔单抗': 'rituximab', '他莫昔芬': 'tamoxifen', '来曲唑': 'letrozole',
    '阿那曲唑': 'anastrozole', '比卡鲁胺': 'bicalutamide', '恩杂鲁胺': 'enzalutamide',
    '阿比特龙': 'abiraterone', '帕博利珠单抗': 'pembrolizumab', '纳武利尤单抗': 'nivolumab',
    '阿替利珠单抗': 'atezolizumab', '阿达木单抗': 'adalimumab',

    # === 风湿免疫 ===
    '羟氯喹': 'hydroxychloroquine', '来氟米特': 'leflunomide',
    '托法替布': 'tofacitinib', '秋水仙碱': 'colchicine', '非布司他': 'febuxostat',
    '别嘌醇': 'allopurinol', '苯溴马隆': 'benzbromarone',

    # === 泌尿/肾脏 ===
    '坦索罗辛': 'tamsulosin', '非那雄胺': 'finasteride', '度他雄胺': 'dutasteride',
    '索利那新': 'solifenacin', '骨化三醇': 'calcitriol', '司维拉姆': 'sevelamer',
    '西那卡塞': 'cinacalcet', '非奈利酮': 'finerenone',

    # === 过敏/皮肤 ===
    '氯雷他定': 'loratadine', '西替利嗪': 'cetirizine', '非索非那定': 'fexofenadine',
    '酮替芬': 'ketotifen', '异维A酸': 'isotretinoin', '夫西地酸': 'fusidic acid',
    '酮康唑': 'ketoconazole', '糠酸莫米松': 'mometasone',

    # === 妇产科 ===
    '缩宫素': 'oxytocin', '米非司酮': 'mifepristone', '地屈孕酮': 'dydrogesterone',
    '黄体酮': 'progesterone', '雌二醇': 'estradiol', '左炔诺孕酮': 'levonorgestrel',
    '硫酸镁': 'magnesium sulfate',

    # === 眼科 ===
    '噻吗洛尔': 'timolol', '溴莫尼定': 'brimonidine', '拉坦前列素': 'latanoprost',
    '毛果芸香碱': 'pilocarpine',

    # === 麻醉 ===
    '丙泊酚': 'propofol', '依托咪酯': 'etomidate', '氯胺酮': 'ketamine',
    '七氟烷': 'sevoflurane', '罗库溴铵': 'rocuronium', '琥珀胆碱': 'succinylcholine',
    '新斯的明': 'neostigmine', '舒芬太尼': 'sufentanil', '瑞芬太尼': 'remifentanil',

    # === 其他 ===
    '甘露醇': 'mannitol', '西地那非': 'sildenafil', '他达拉非': 'tadalafil',
    '波生坦': 'bosentan', '安立生坦': 'ambrisentan', '氨甲环酸': 'tranexamic acid',
    '维生素K1': 'phytonadione', '叶酸': 'folic acid', '维生素B12': 'cyanocobalamin',
    '奥曲肽': 'octreotide', '生长抑素': 'somatostatin',
}

# Validate against DDInter
found = 0
not_found = []
for cn, en in sorted(mapping.items()):
    if en in idx:
        found += 1
    else:
        not_found.append((cn, en))

print(f'Total mapping: {len(mapping)}')
print(f'Found in DDInter: {found}')
print(f'Not found in DDInter: {len(not_found)}')
if not_found:
    print('\nNot found:')
    for cn, en in not_found:
        # Try to find close matches
        words = en.split()
        close = [k for k in idx if any(w in k.lower() for w in words)]
        if close:
            print(f'  {cn} -> {en}  (suggest: {close[:4]})')
        else:
            print(f'  {cn} -> {en}  (no close match)')

# Output the mapping as Kotlin mapOf
print('\n\n// === Kotlin code ===')
print('private val zhToEn: Map<String, String> = mapOf(')
for cn, en in sorted(mapping.items()):
    print(f'    "{cn}" to "{en}",')
print(')')
