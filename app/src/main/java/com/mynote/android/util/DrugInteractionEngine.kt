package com.mynote.android.util

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import java.io.InputStreamReader

/**
 * DDInter 2.0 药物相互作用引擎
 * 来源：中南大学 DDInter 2.0 (CC BY-NC-SA 4.0)
 * 约 30 万对相互作用，离线本地查询
 */
object DrugInteractionEngine {

    private var loaded = false
    private var interactions: List<DdiItem> = emptyList()
    private var index: Map<String, List<Int>> = emptyMap()

    data class DdiItem(
        val a: String,    // 药物A
        val b: String,    // 药物B
        val lvl: Int,     // 1=禁忌 2=谨慎 3=关注
        val desc: String, // 作用描述
        val mng: String   // 处理建议
    )

    fun init(context: Context) {
        if (loaded) return
        try {
            val input = context.assets.open("ddinter_interactions.json")
            val reader = JsonReader(InputStreamReader(input, "UTF-8"))
            reader.beginObject()
            var ix: Map<String, List<Int>>? = null
            var ddi: List<DdiItem>? = null
            while (reader.hasNext()) {
                when (reader.nextName()) {
                    "ix" -> ix = Gson().fromJson(reader, object : TypeToken<Map<String, List<Int>>>() {}.type)
                    "ddi" -> ddi = Gson().fromJson(reader, object : TypeToken<List<DdiItem>>() {}.type)
                    else -> reader.skipValue()
                }
            }
            reader.endObject()
            reader.close()
            interactions = ddi ?: emptyList()
            index = ix ?: emptyMap()
            loaded = true
        } catch (e: Exception) {
            // 无 DDInter 数据时退回到硬编码规则
            interactions = emptyList()
            index = emptyMap()
            loaded = true
        }
    }

    /** 查询两个药物的相互作用（含剂型变体） */
    fun check(drugA: String, drugB: String): List<DdiItem> {
        val keyA = drugA.lowercase()
        val keyB = drugB.lowercase()

        // 收集所有匹配的索引（含 topical/ophthalmic 等后缀变体）
        val idsA = mutableSetOf<Int>()
        for ((k, v) in index) {
            if (k == keyA || k.startsWith("$keyA ")) idsA.addAll(v)
        }
        val idsB = mutableSetOf<Int>()
        for ((k, v) in index) {
            if (k == keyB || k.startsWith("$keyB ")) idsB.addAll(v)
        }

        val common = idsA.intersect(idsB)
        return common.mapNotNull { interactions.getOrNull(it) }
    }


    /** 模糊查询：用药名包含关键词的所有相互作用 */
    fun search(query: String): List<DdiItem> {
        val q = query.lowercase()
        val matched = index.filter { it.key.contains(q) }
        val ids = matched.flatMap { it.value }.toSet()
        return ids.mapNotNull { interactions.getOrNull(it) }
    }

    fun isLoaded() = loaded && interactions.isNotEmpty()

    /** 返回 DDInter 索引中所有药名（用于自动补全） */
    fun allDrugNames(): List<String> = index.keys.sorted()

    /**
     * 药品名搜索 — 支持中文和英文模糊匹配
     * 返回 DDInter 中匹配的药名列表（英文通用名）
     */
    fun searchDrugNames(query: String, limit: Int = 30): List<DrugMatch> {
        val q = query.trim().lowercase()

        // 空查询 → 返回有中文映射的药品
        if (q.isEmpty()) {
            val reverse = ZH_TO_EN.entries.groupBy({ it.value }, { it.key })
            return ZH_TO_EN.values.distinct().take(limit).map { en ->
                DrugMatch(en, reverse[en]?.firstOrNull())
            }
        }

        // 1. 先查中文映射
        val enCandidates = mutableSetOf<String>()
        for ((zh, en) in ZH_TO_EN) {
            if (zh.contains(q) || q.contains(zh)) {
                enCandidates.add(en)
            }
        }

        // 2. 直接匹配英文索引（前缀 + 包含）
        for (name in index.keys) {
            val nl = name.lowercase()
            if (nl.startsWith(q) || nl.contains(q)) {
                enCandidates.add(name)
            }
        }

        // 3. 构建带中文标签的结果
        val reverse = ZH_TO_EN.entries.groupBy({ it.value }, { it.key })
        return enCandidates.take(limit).map { en ->
            DrugMatch(en, reverse[en]?.firstOrNull())
        }
    }

    data class DrugMatch(
        val enName: String,      // DDInter 英文药名
        val zhLabel: String?     // 中文名（如有映射）
    )

    fun levelLabel(lvl: Int): String = when (lvl) {
        1 -> "❌ 禁忌/不推荐"
        2 -> "⚠️ 谨慎合用"
        3 -> "⚡ 关注"
        else -> "⚠️"
    }

    // ===== 中→英药名映射 (335条, 305条命中DDInter) =====
    private val ZH_TO_EN: Map<String, String> = mapOf(
        "七氟烷" to "sevoflurane",
        "万古霉素" to "vancomycin",
        "丙戊酸" to "valproic acid",
        "丙戊酸钠" to "valproic acid",
        "丙泊酚" to "propofol",
        "丙硫氧嘧啶" to "propylthiouracil",
        "两性霉素B" to "amphotericin b",
        "乙胺丁醇" to "ethambutol",
        "乙酰半胱氨酸" to "acetylcysteine",
        "乳果糖" to "lactulose",
        "二甲双胍" to "metformin",
        "他克莫司" to "tacrolimus",
        "他莫昔芬" to "tamoxifen",
        "他达拉非" to "tadalafil",
        "伊曲康唑" to "itraconazole",
        "伊立替康" to "irinotecan",
        "伊马替尼" to "imatinib",
        "伏立康唑" to "voriconazole",
        "低分子肝素" to "enoxaparin",
        "依度沙班" to "edoxaban",
        "依托咪酯" to "etomidate",
        "依托泊苷" to "etoposide",
        "依托考昔" to "etoricoxib",
        "依折麦布" to "ezetimibe",
        "依诺肝素" to "enoxaparin",
        "依那普利" to "enalapril",
        "克唑替尼" to "crizotinib",
        "克拉霉素" to "clarithromycin",
        "克林霉素" to "clindamycin",
        "兰索拉唑" to "lansoprazole",
        "决奈达隆" to "dronedarone",
        "利伐沙班" to "rivaroxaban",
        "利培酮" to "risperidone",
        "利多卡因" to "lidocaine",
        "利奈唑胺" to "linezolid",
        "利妥昔单抗" to "rituximab",
        "利巴韦林" to "ribavirin",
        "利拉鲁肽" to "liraglutide",
        "利格列汀" to "linagliptin",
        "利福平" to "rifampicin",
        "别嘌醇" to "allopurinol",
        "加巴喷丁" to "gabapentin",
        "劳拉西泮" to "lorazepam",
        "匹伐他汀" to "pitavastatin",
        "华法林" to "warfarin",
        "单硝酸异山梨酯" to "isosorbide mononitrate",
        "博来霉素" to "bleomycin",
        "卡培他滨" to "capecitabine",
        "卡托普利" to "captopril",
        "卡格列净" to "canagliflozin",
        "卡泊芬净" to "caspofungin",
        "卡维地洛" to "carvedilol",
        "卡铂" to "carboplatin",
        "卡马西平" to "carbamazepine",
        "厄他培南" to "ertapenem",
        "厄洛替尼" to "erlotinib",
        "厄贝沙坦" to "irbesartan",
        "去氧肾上腺素" to "phenylephrine",
        "去甲肾上腺素" to "norepinephrine",
        "双嘧达莫" to "dipyridamole",
        "双氯芬酸" to "diclofenac",
        "双氯芬酸钠" to "diclofenac",
        "可待因" to "codeine",
        "叶酸" to "folic acid",
        "司维拉姆" to "sevelamer",
        "司美格鲁肽" to "semaglutide",
        "吉西他滨" to "gemcitabine",
        "吉非替尼" to "gefitinib",
        "吉非罗齐" to "gemfibrozil",
        "吗啡" to "morphine",
        "吗替麦考酚酯" to "mycophenolate mofetil",
        "吡嗪酰胺" to "pyrazinamide",
        "吡格列酮" to "pioglitazone",
        "吲哚美辛" to "indomethacin",
        "吲达帕胺" to "indapamide",
        "呋塞米" to "furosemide",
        "咪达唑仑" to "midazolam",
        "哌替啶" to "meperidine",
        "唑来膦酸" to "zoledronic acid",
        "喹硫平" to "quetiapine",
        "噻吗洛尔" to "timolol",
        "噻托溴铵" to "tiotropium",
        "地塞米松" to "dexamethasone",
        "地尔硫卓" to "diltiazem",
        "地西泮" to "diazepam",
        "地高辛" to "digoxin",
        "坦索罗辛" to "tamsulosin",
        "埃索美拉唑" to "esomeprazole",
        "培哚普利" to "perindopril",
        "培美曲塞" to "pemetrexed",
        "塞来昔布" to "celecoxib",
        "多奈哌齐" to "donepezil",
        "多巴胺" to "dopamine",
        "多巴酚丁胺" to "dobutamine",
        "多柔比星" to "doxorubicin",
        "多西他赛" to "docetaxel",
        "多西环素" to "doxycycline",
        "头孢他啶" to "ceftazidime",
        "头孢克洛" to "cefaclor",
        "头孢吡肟" to "cefepime",
        "头孢呋辛" to "cefuroxime",
        "头孢地尼" to "cefdinir",
        "头孢曲松" to "ceftriaxone",
        "头孢西丁" to "cefoxitin",
        "奥卡西平" to "oxcarbazepine",
        "奥司他韦" to "oseltamivir",
        "奥希替尼" to "osimertinib",
        "奥曲肽" to "octreotide",
        "奥氮平" to "olanzapine",
        "奥沙利铂" to "oxaliplatin",
        "奥美拉唑" to "omeprazole",
        "奥美沙坦" to "olmesartan",
        "孟鲁司特" to "montelukast",
        "安立生坦" to "ambrisentan",
        "安非他酮" to "bupropion",
        "对乙酰氨基酚" to "acetaminophen",
        "左乙拉西坦" to "levetiracetam",
        "左旋多巴" to "levodopa",
        "左氧氟沙星" to "levofloxacin",
        "左炔诺孕酮" to "levonorgestrel",
        "左甲状腺素钠" to "levothyroxine",
        "布地奈德" to "budesonide",
        "布比卡因" to "bupivacaine",
        "布洛芬" to "ibuprofen",
        "帕博利珠单抗" to "pembrolizumab",
        "帕罗西汀" to "paroxetine",
        "庆大霉素" to "gentamicin",
        "度他雄胺" to "dutasteride",
        "度拉糖肽" to "dulaglutide",
        "度洛西汀" to "duloxetine",
        "异丙托溴铵" to "ipratropium",
        "异烟肼" to "isoniazid",
        "异维A酸" to "isotretinoin",
        "恩替卡韦" to "entecavir",
        "恩杂鲁胺" to "enzalutamide",
        "恩格列净" to "empagliflozin",
        "托吡酯" to "topiramate",
        "托法替布" to "tofacitinib",
        "拉坦前列素" to "latanoprost",
        "拉考沙胺" to "lacosamide",
        "拉莫三嗪" to "lamotrigine",
        "拉贝洛尔" to "labetalol",
        "文拉法辛" to "venlafaxine",
        "新斯的明" to "neostigmine",
        "昂丹司琼" to "ondansetron",
        "普伐他汀" to "pravastatin",
        "普瑞巴林" to "pregabalin",
        "普罗帕酮" to "propafenone",
        "普萘洛尔" to "propranolol",
        "曲唑酮" to "trazodone",
        "曲妥珠单抗" to "trastuzumab",
        "曲马多" to "tramadol",
        "更昔洛韦" to "ganciclovir",
        "替加环素" to "tigecycline",
        "替格瑞洛" to "ticagrelor",
        "替米沙坦" to "telmisartan",
        "替罗非班" to "tirofiban",
        "替莫唑胺" to "temozolomide",
        "来曲唑" to "letrozole",
        "来氟米特" to "leflunomide",
        "柳氮磺吡啶" to "sulfasalazine",
        "格列吡嗪" to "glipizide",
        "格列本脲" to "glyburide",
        "格列美脲" to "glimepiride",
        "格拉司琼" to "granisetron",
        "比卡鲁胺" to "bicalutamide",
        "比索洛尔" to "bisoprolol",
        "毛果芸香碱" to "pilocarpine",
        "氟伐他汀" to "fluvastatin",
        "氟哌啶醇" to "haloperidol",
        "氟尿嘧啶" to "fluorouracil",
        "氟康唑" to "fluconazole",
        "氟替卡松" to "fluticasone",
        "氟西汀" to "fluoxetine",
        "氢化可的松" to "hydrocortisone",
        "氢氯噻嗪" to "hydrochlorothiazide",
        "氨氯地平" to "amlodipine",
        "氨甲环酸" to "tranexamic acid",
        "氨苯蝶啶" to "triamterene",
        "氨茶碱" to "aminophylline",
        "氯丙嗪" to "chlorpromazine",
        "氯吡格雷" to "clopidogrel",
        "氯氮平" to "clozapine",
        "氯沙坦" to "losartan",
        "氯硝西泮" to "clonazepam",
        "氯胺酮" to "ketamine",
        "氯雷他定" to "loratadine",
        "沙格列汀" to "saxagliptin",
        "沙美特罗" to "salmeterol",
        "法莫替丁" to "famotidine",
        "波生坦" to "bosentan",
        "泮托拉唑" to "pantoprazole",
        "泼尼松" to "prednisone",
        "泼尼松龙" to "prednisolone",
        "洛哌丁胺" to "loperamide",
        "溴莫尼定" to "brimonidine",
        "特布他林" to "terbutaline",
        "特比萘芬" to "terbinafine",
        "环丙沙星" to "ciprofloxacin",
        "环孢素" to "cyclosporine",
        "环磷酰胺" to "cyclophosphamide",
        "琥珀胆碱" to "succinylcholine",
        "瑞格列奈" to "repaglinide",
        "瑞舒伐他汀" to "rosuvastatin",
        "瑞芬太尼" to "remifentanil",
        "甘精胰岛素" to "insulin glargine",
        "甘露醇" to "mannitol",
        "甲巯咪唑" to "methimazole",
        "甲氧氯普胺" to "metoclopramide",
        "甲氧苄啶" to "trimethoprim",
        "甲氨蝶呤" to "methotrexate",
        "甲泼尼龙" to "methylprednisolone",
        "甲硝唑" to "metronidazole",
        "硝苯地平" to "nifedipine",
        "硝酸异山梨酯" to "isosorbide dinitrate",
        "硝酸甘油" to "nitroglycerin",
        "硫糖铝" to "sucralfate",
        "硫酸镁" to "magnesium sulfate",
        "碳酸锂" to "lithium carbonate",
        "锂盐" to "lithium carbonate",
        "磺胺甲恶唑" to "sulfamethoxazole",
        "磺达肝癸钠" to "fondaparinux",
        "福莫特罗" to "formoterol",
        "秋水仙碱" to "colchicine",
        "米力农" to "milrinone",
        "米氮平" to "mirtazapine",
        "米索前列醇" to "misoprostol",
        "米诺环素" to "minocycline",
        "米非司酮" to "mifepristone",
        "索他洛尔" to "sotalol",
        "索利那新" to "solifenacin",
        "索拉非尼" to "sorafenib",
        "紫杉醇" to "paclitaxel",
        "纳武利尤单抗" to "nivolumab",
        "纳洛酮" to "naloxone",
        "维拉帕米" to "verapamil",
        "维生素B12" to "cyanocobalamin",
        "缩宫素" to "oxytocin",
        "缬沙坦" to "valsartan",
        "罗哌卡因" to "ropivacaine",
        "罗库溴铵" to "rocuronium",
        "罗格列酮" to "rosiglitazone",
        "美托洛尔" to "metoprolol",
        "美沙拉嗪" to "mesalazine",
        "美洛昔康" to "meloxicam",
        "美罗培南" to "meropenem",
        "美金刚" to "memantine",
        "羟氯喹" to "hydroxychloroquine",
        "羟考酮" to "oxycodone",
        "肝素" to "heparin",
        "肾上腺素" to "epinephrine",
        "胰岛素" to "insulin human",
        "门冬胰岛素" to "insulin aspart (aspart)",
        "胺碘酮" to "amiodarone",
        "腺苷" to "adenosine",
        "舍曲林" to "sertraline",
        "舒尼替尼" to "sunitinib",
        "舒芬太尼" to "sufentanil",
        "艾司西酞普兰" to "escitalopram",
        "艾塞那肽" to "exenatide",
        "芬太尼" to "fentanyl",
        "苯妥英" to "phenytoin",
        "苯妥英钠" to "phenytoin",
        "苯巴比妥" to "phenobarbital",
        "茶碱" to "theophylline",
        "莫西沙星" to "moxifloxacin",
        "萘普生" to "naproxen",
        "螺内酯" to "spironolactone",
        "表柔比星" to "epirubicin",
        "西咪替丁" to "cimetidine",
        "西地那非" to "sildenafil",
        "西替利嗪" to "cetirizine",
        "西格列汀" to "sitagliptin",
        "西洛他唑" to "cilostazol",
        "西那卡塞" to "cinacalcet",
        "西酞普兰" to "citalopram",
        "贝伐珠单抗" to "bevacizumab",
        "贝那普利" to "benazepril",
        "赖诺普利" to "lisinopril",
        "辛伐他汀" to "simvastatin",
        "达托霉素" to "daptomycin",
        "达格列净" to "dapagliflozin",
        "那格列奈" to "nateglinide",
        "酮咯酸" to "ketorolac",
        "酮康唑" to "ketoconazole",
        "酮替芬" to "ketotifen",
        "长春新碱" to "vincristine",
        "阿卡波糖" to "acarbose",
        "阿司匹林" to "acetylsalicylic acid",
        "阿哌沙班" to "apixaban",
        "阿奇霉素" to "azithromycin",
        "阿托伐他汀" to "atorvastatin",
        "阿昔洛韦" to "acyclovir",
        "阿普唑仑" to "alprazolam",
        "阿替利珠单抗" to "atezolizumab",
        "阿替洛尔" to "atenolol",
        "阿比特龙" to "abiraterone",
        "阿立哌唑" to "aripiprazole",
        "阿米卡星" to "amikacin",
        "阿莫西林" to "amoxicillin",
        "阿达木单抗" to "adalimumab",
        "阿那曲唑" to "anastrozole",
        "雌二醇" to "estradiol",
        "雷尼替丁" to "ranitidine",
        "雷米普利" to "ramipril",
        "雷贝拉唑" to "rabeprazole",
        "非布司他" to "febuxostat",
        "非洛地平" to "felodipine",
        "非索非那定" to "fexofenadine",
        "非诺贝特" to "fenofibrate",
        "非那雄胺" to "finasteride",
        "顺铂" to "cisplatin",
        "骨化三醇" to "calcitriol",
        "黄体酮" to "progesterone",
    )
}
