package com.mynote.android.util

/**
 * 临床指南 PDF 库 — 元数据索引
 * 来源：中华医学会/ESC/AHA/WHO/NICE 等公开指南
 * 点击打开浏览器查看原文 PDF
 */
object GuidelineLibrary {
    data class Guideline(
        val title: String, val source: String, val year: String,
        val dept: String, val url: String, val keywords: String = ""
    )

    val all: List<Guideline> = listOf(
        Guideline("中国高血压防治指南2024", "中华医学会", "2024", "心内科", "https://guide.medlive.cn/search?q=%E4%B8%AD%E5%9B%BD%E9%AB%98%E8%A1%80%E5%8E%8B%E9%98%B2%E6%B2%BB%E6%8C%87%E5%8D%972024%20PDF"),
        Guideline("ESC Guidelines for ACS 2023", "ESC", "2023", "心内科", "https://guide.medlive.cn/search?q=ESC%20Guidelines%20for%20ACS%202023%20PDF"),
        Guideline("AHA/ACC Heart Failure Guideline 2022", "AHA/ACC", "2022", "心内科", "https://guide.medlive.cn/search?q=AHA/ACC%20Heart%20Failure%20Guideline%202022%20PDF"),
        Guideline("中国2型糖尿病防治指南2024", "中华医学会糖尿病分会", "2024", "内分泌科", "https://guide.medlive.cn/search?q=%E4%B8%AD%E5%9B%BD2%E5%9E%8B%E7%B3%96%E5%B0%BF%E7%97%85%E9%98%B2%E6%B2%BB%E6%8C%87%E5%8D%972024%20PDF"),
        Guideline("ADA Standards of Care 2025", "ADA", "2025", "内分泌科", "https://guide.medlive.cn/search?q=ADA%20Standards%20of%20Care%202025%20PDF"),
        Guideline("GOLD 2025 COPD 全球策略", "GOLD", "2025", "呼吸科", "https://guide.medlive.cn/search?q=GOLD%202025%20COPD%20%E5%85%A8%E7%90%83%E7%AD%96%E7%95%A5%20PDF"),
        Guideline("GINA 2024 哮喘全球策略", "GINA", "2024", "呼吸科", "https://guide.medlive.cn/search?q=GINA%202024%20%E5%93%AE%E5%96%98%E5%85%A8%E7%90%83%E7%AD%96%E7%95%A5%20PDF"),
        Guideline("SSC 脓毒症与感染性休克指南2024", "SCCM/ESICM", "2024", "ICU", "https://guide.medlive.cn/search?q=SSC%20%E8%84%93%E6%AF%92%E7%97%87%E4%B8%8E%E6%84%9F%E6%9F%93%E6%80%A7%E4%BC%91%E5%85%8B%E6%8C%87%E5%8D%972024%20PDF"),
        Guideline("中国社区获得性肺炎指南2023", "中华医学会呼吸分会", "2023", "呼吸科", "https://guide.medlive.cn/search?q=%E4%B8%AD%E5%9B%BD%E7%A4%BE%E5%8C%BA%E8%8E%B7%E5%BE%97%E6%80%A7%E8%82%BA%E7%82%8E%E6%8C%87%E5%8D%972023%20PDF"),
        Guideline("KDIGO 2024 CKD 评估与管理", "KDIGO", "2024", "肾内科", "https://guide.medlive.cn/search?q=KDIGO%202024%20CKD%20%E8%AF%84%E4%BC%B0%E4%B8%8E%E7%AE%A1%E7%90%86%20PDF"),
        Guideline("中国心房颤动指南2023", "中华医学会心电生理分会", "2023", "心内科", "https://guide.medlive.cn/search?q=%E4%B8%AD%E5%9B%BD%E5%BF%83%E6%88%BF%E9%A2%A4%E5%8A%A8%E6%8C%87%E5%8D%972023%20PDF"),
        Guideline("NCCN Guidelines 2025 (各癌种)", "NCCN", "2025", "肿瘤科", "https://www.nccn.org/guidelines/category_1"),
        Guideline("中国急性胰腺炎诊治指南2022", "中华医学会消化分会", "2022", "消化科", "https://guide.medlive.cn/search?q=%E4%B8%AD%E5%9B%BD%E6%80%A5%E6%80%A7%E8%83%B0%E8%85%BA%E7%82%8E%E8%AF%8A%E6%B2%BB%E6%8C%87%E5%8D%972022%20PDF"),
        Guideline("AASLD 肝硬化腹水指南2021", "AASLD", "2021", "消化科", "https://guide.medlive.cn/search?q=AASLD%20%E8%82%9D%E7%A1%AC%E5%8C%96%E8%85%B9%E6%B0%B4%E6%8C%87%E5%8D%972021%20PDF"),
        Guideline("中国急性缺血性脑卒中指南2023", "中华医学会神经分会", "2023", "神经内科", "https://guide.medlive.cn/search?q=%E4%B8%AD%E5%9B%BD%E6%80%A5%E6%80%A7%E7%BC%BA%E8%A1%80%E6%80%A7%E8%84%91%E5%8D%92%E4%B8%AD%E6%8C%87%E5%8D%972023%20PDF"),
        Guideline("中国癫痫诊疗指南2023", "中华医学会神经分会", "2023", "神经内科", "https://guide.medlive.cn/search?q=%E4%B8%AD%E5%9B%BD%E7%99%AB%E7%97%AB%E8%AF%8A%E7%96%97%E6%8C%87%E5%8D%972023%20PDF"),
        Guideline("AHA/ACC Chest Pain Guideline 2021", "AHA/ACC", "2021", "心内科", "https://guide.medlive.cn/search?q=AHA/ACC%20Chest%20Pain%20Guideline%202021%20PDF"),
        Guideline("IDSA 抗菌药物管理指南2024", "IDSA", "2024", "感染科", "https://guide.medlive.cn/search?q=IDSA%20%E6%8A%97%E8%8F%8C%E8%8D%AF%E7%89%A9%E7%AE%A1%E7%90%86%E6%8C%87%E5%8D%972024%20PDF"),
        Guideline("ACR 类风湿关节炎指南2021", "ACR", "2021", "风湿科", "https://guide.medlive.cn/search?q=ACR%20%E7%B1%BB%E9%A3%8E%E6%B9%BF%E5%85%B3%E8%8A%82%E7%82%8E%E6%8C%87%E5%8D%972021%20PDF"),
        Guideline("中国骨质疏松症指南2022", "中华医学会骨质疏松分会", "2022", "骨科", "https://guide.medlive.cn/search?q=%E4%B8%AD%E5%9B%BD%E9%AA%A8%E8%B4%A8%E7%96%8F%E6%9D%BE%E7%97%87%E6%8C%87%E5%8D%972022%20PDF"),
        Guideline("ACOG 产前保健指南2024", "ACOG", "2024", "妇产科", "https://guide.medlive.cn/search?q=ACOG%20%E4%BA%A7%E5%89%8D%E4%BF%9D%E5%81%A5%E6%8C%87%E5%8D%972024%20PDF"),
        Guideline("ESMO 肿瘤诊疗指南2025", "ESMO", "2025", "肿瘤科", "https://guide.medlive.cn/search?q=ESMO%20%E8%82%BF%E7%98%A4%E8%AF%8A%E7%96%97%E6%8C%87%E5%8D%972025%20PDF"),
        Guideline("中国儿童肺炎指南2024", "中华医学会儿科分会", "2024", "儿科", "https://guide.medlive.cn/search?q=%E4%B8%AD%E5%9B%BD%E5%84%BF%E7%AB%A5%E8%82%BA%E7%82%8E%E6%8C%87%E5%8D%972024%20PDF"),
        Guideline("WHO 结核病治疗指南2024", "WHO", "2024", "感染科", "https://guide.medlive.cn/search?q=WHO%20%E7%BB%93%E6%A0%B8%E7%97%85%E6%B2%BB%E7%96%97%E6%8C%87%E5%8D%972024%20PDF"),
        Guideline("AAOS 骨科临床指南2024", "AAOS", "2024", "骨科", "https://guide.medlive.cn/search?q=AAOS%20%E9%AA%A8%E7%A7%91%E4%B8%B4%E5%BA%8A%E6%8C%87%E5%8D%972024%20PDF"),
        Guideline("EAU 泌尿外科指南2025", "EAU", "2025", "泌尿外科", "https://guide.medlive.cn/search?q=EAU%20%E6%B3%8C%E5%B0%BF%E5%A4%96%E7%A7%91%E6%8C%87%E5%8D%972025%20PDF"),
        Guideline("中国麻醉指南2024", "中华医学会麻醉分会", "2024", "麻醉科", "https://guide.medlive.cn/search?q=%E4%B8%AD%E5%9B%BD%E9%BA%BB%E9%86%89%E6%8C%87%E5%8D%972024%20PDF"),
        Guideline("中国精神分裂症指南2023", "中华医学会精神分会", "2023", "精神科", "https://guide.medlive.cn/search?q=%E4%B8%AD%E5%9B%BD%E7%B2%BE%E7%A5%9E%E5%88%86%E8%A3%82%E7%97%87%E6%8C%87%E5%8D%972023%20PDF"),
        Guideline("BSG 上消化道出血指南2023", "BSG", "2023", "消化科", "https://guide.medlive.cn/search?q=BSG%20%E4%B8%8A%E6%B6%88%E5%8C%96%E9%81%93%E5%87%BA%E8%A1%80%E6%8C%87%E5%8D%972023%20PDF"),
        Guideline("中国糖尿病酮症酸中毒指南2023", "中华医学会糖尿病分会", "2023", "内分泌科", "https://guide.medlive.cn/search?q=%E4%B8%AD%E5%9B%BD%E7%B3%96%E5%B0%BF%E7%97%85%E9%85%AE%E7%97%87%E9%85%B8%E4%B8%AD%E6%AF%92%E6%8C%87%E5%8D%972023%20PDF"),
        Guideline("ACC/AHA 血脂管理指南2023", "ACC/AHA", "2023", "心内科", "https://guide.medlive.cn/search?q=ACC/AHA%20%E8%A1%80%E8%84%82%E7%AE%A1%E7%90%86%E6%8C%87%E5%8D%972023%20PDF"),
        Guideline("NICE 慢性疼痛指南2021", "NICE", "2021", "疼痛科", "https://guide.medlive.cn/search?q=NICE%20%E6%85%A2%E6%80%A7%E7%96%BC%E7%97%9B%E6%8C%87%E5%8D%972021%20PDF"),
        // === 扩展 70+ ===
        Guideline("WHO 高血压药物治疗指南2024", "WHO", "2024", "心内科", "https://guide.medlive.cn/search?q=WHO%20%E9%AB%98%E8%A1%80%E5%8E%8B%E8%8D%AF%E7%89%A9%E6%B2%BB%E7%96%97%E6%8C%87%E5%8D%972024%20PDF"),
        Guideline("ESC 心衰指南2024", "ESC", "2024", "心内科", "https://guide.medlive.cn/search?q=ESC%20%E5%BF%83%E8%A1%B0%E6%8C%87%E5%8D%972024%20PDF"),
        Guideline("中国冠心病血运重建指南2023", "中华医学会心血管分会", "2023", "心内科", "https://guide.medlive.cn/search?q=%E4%B8%AD%E5%9B%BD%E5%86%A0%E5%BF%83%E7%97%85%E8%A1%80%E8%BF%90%E9%87%8D%E5%BB%BA%E6%8C%87%E5%8D%972023%20PDF"),
        Guideline("AHA 心肺复苏指南2023", "AHA", "2023", "急诊科", "https://guide.medlive.cn/search?q=AHA%20%E5%BF%83%E8%82%BA%E5%A4%8D%E8%8B%8F%E6%8C%87%E5%8D%972023%20PDF"),
        Guideline("中国肺栓塞诊治指南2023", "中华医学会呼吸分会", "2023", "呼吸科", "https://guide.medlive.cn/search?q=%E4%B8%AD%E5%9B%BD%E8%82%BA%E6%A0%93%E5%A1%9E%E8%AF%8A%E6%B2%BB%E6%8C%87%E5%8D%972023%20PDF"),
        Guideline("中国支气管哮喘指南2024", "中华医学会呼吸分会", "2024", "呼吸科", "https://guide.medlive.cn/search?q=%E4%B8%AD%E5%9B%BD%E6%94%AF%E6%B0%94%E7%AE%A1%E5%93%AE%E5%96%98%E6%8C%87%E5%8D%972024%20PDF"),
        Guideline("ATS/ERS 肺功能指南2022", "ATS/ERS", "2022", "呼吸科", "https://guide.medlive.cn/search?q=ATS/ERS%20%E8%82%BA%E5%8A%9F%E8%83%BD%E6%8C%87%E5%8D%972022%20PDF"),
        Guideline("中国幽门螺杆菌诊治指南2023", "中华医学会消化分会", "2023", "消化科", "https://guide.medlive.cn/search?q=%E4%B8%AD%E5%9B%BD%E5%B9%BD%E9%97%A8%E8%9E%BA%E6%9D%86%E8%8F%8C%E8%AF%8A%E6%B2%BB%E6%8C%87%E5%8D%972023%20PDF"),
        Guideline("ACG 溃疡性结肠炎指南2024", "ACG", "2024", "消化科", "https://guide.medlive.cn/search?q=ACG%20%E6%BA%83%E7%96%A1%E6%80%A7%E7%BB%93%E8%82%A0%E7%82%8E%E6%8C%87%E5%8D%972024%20PDF"),
        Guideline("EASL 肝硬化管理指南2023", "EASL", "2023", "消化科", "https://guide.medlive.cn/search?q=EASL%20%E8%82%9D%E7%A1%AC%E5%8C%96%E7%AE%A1%E7%90%86%E6%8C%87%E5%8D%972023%20PDF"),
        Guideline("中国炎症性肠病指南2023", "中华医学会消化分会", "2023", "消化科", "https://guide.medlive.cn/search?q=%E4%B8%AD%E5%9B%BD%E7%82%8E%E7%97%87%E6%80%A7%E8%82%A0%E7%97%85%E6%8C%87%E5%8D%972023%20PDF"),
        Guideline("KDIGO 肾小球肾炎指南2023", "KDIGO", "2023", "肾内科", "https://guide.medlive.cn/search?q=KDIGO%20%E8%82%BE%E5%B0%8F%E7%90%83%E8%82%BE%E7%82%8E%E6%8C%87%E5%8D%972023%20PDF"),
        Guideline("中国终末期肾病指南2023", "中华医学会肾内分会", "2023", "肾内科", "https://guide.medlive.cn/search?q=%E4%B8%AD%E5%9B%BD%E7%BB%88%E6%9C%AB%E6%9C%9F%E8%82%BE%E7%97%85%E6%8C%87%E5%8D%972023%20PDF"),
        Guideline("ERA-EDTA 肾性贫血指南2023", "ERA-EDTA", "2023", "肾内科", "https://guide.medlive.cn/search?q=ERA-EDTA%20%E8%82%BE%E6%80%A7%E8%B4%AB%E8%A1%80%E6%8C%87%E5%8D%972023%20PDF"),
        Guideline("中国帕金森病指南2023", "中华医学会神经分会", "2023", "神经内科", "https://guide.medlive.cn/search?q=%E4%B8%AD%E5%9B%BD%E5%B8%95%E9%87%91%E6%A3%AE%E7%97%85%E6%8C%87%E5%8D%972023%20PDF"),
        Guideline("中国阿尔茨海默病指南2024", "中华医学会神经分会", "2024", "神经内科", "https://guide.medlive.cn/search?q=%E4%B8%AD%E5%9B%BD%E9%98%BF%E5%B0%94%E8%8C%A8%E6%B5%B7%E9%BB%98%E7%97%85%E6%8C%87%E5%8D%972024%20PDF"),
        Guideline("中国多发性硬化指南2024", "中华医学会神经分会", "2024", "神经内科", "https://guide.medlive.cn/search?q=%E4%B8%AD%E5%9B%BD%E5%A4%9A%E5%8F%91%E6%80%A7%E7%A1%AC%E5%8C%96%E6%8C%87%E5%8D%972024%20PDF"),
        Guideline("中国重症肌无力指南2023", "中华医学会神经分会", "2023", "神经内科", "https://guide.medlive.cn/search?q=%E4%B8%AD%E5%9B%BD%E9%87%8D%E7%97%87%E8%82%8C%E6%97%A0%E5%8A%9B%E6%8C%87%E5%8D%972023%20PDF"),
        Guideline("ADA 糖尿病肾病指南2024", "ADA", "2024", "内分泌科", "https://guide.medlive.cn/search?q=ADA%20%E7%B3%96%E5%B0%BF%E7%97%85%E8%82%BE%E7%97%85%E6%8C%87%E5%8D%972024%20PDF"),
        Guideline("中国甲状腺结节指南2023", "中华医学会内分泌分会", "2023", "内分泌科", "https://guide.medlive.cn/search?q=%E4%B8%AD%E5%9B%BD%E7%94%B2%E7%8A%B6%E8%85%BA%E7%BB%93%E8%8A%82%E6%8C%87%E5%8D%972023%20PDF"),
        Guideline("ATA 甲亢指南2024", "ATA", "2024", "内分泌科", "https://guide.medlive.cn/search?q=ATA%20%E7%94%B2%E4%BA%A2%E6%8C%87%E5%8D%972024%20PDF"),
        Guideline("中国痛风指南2024", "中华医学会风湿分会", "2024", "风湿科", "https://guide.medlive.cn/search?q=%E4%B8%AD%E5%9B%BD%E7%97%9B%E9%A3%8E%E6%8C%87%E5%8D%972024%20PDF"),
        Guideline("EULAR 系统性红斑狼疮指南2024", "EULAR", "2024", "风湿科", "https://guide.medlive.cn/search?q=EULAR%20%E7%B3%BB%E7%BB%9F%E6%80%A7%E7%BA%A2%E6%96%91%E7%8B%BC%E7%96%AE%E6%8C%87%E5%8D%972024%20PDF"),
        Guideline("ACR 银屑病关节炎指南2023", "ACR", "2023", "风湿科", "https://guide.medlive.cn/search?q=ACR%20%E9%93%B6%E5%B1%91%E7%97%85%E5%85%B3%E8%8A%82%E7%82%8E%E6%8C%87%E5%8D%972023%20PDF"),
        Guideline("中国强直性脊柱炎指南2023", "中华医学会风湿分会", "2023", "风湿科", "https://guide.medlive.cn/search?q=%E4%B8%AD%E5%9B%BD%E5%BC%BA%E7%9B%B4%E6%80%A7%E8%84%8A%E6%9F%B1%E7%82%8E%E6%8C%87%E5%8D%972023%20PDF"),
        Guideline("NCCN 非小细胞肺癌指南2025", "NCCN", "2025", "肿瘤科", "https://guide.medlive.cn/search?q=NCCN%20%E9%9D%9E%E5%B0%8F%E7%BB%86%E8%83%9E%E8%82%BA%E7%99%8C%E6%8C%87%E5%8D%972025%20PDF"),
        Guideline("NCCN 乳腺癌指南2025", "NCCN", "2025", "肿瘤科", "https://guide.medlive.cn/search?q=NCCN%20%E4%B9%B3%E8%85%BA%E7%99%8C%E6%8C%87%E5%8D%972025%20PDF"),
        Guideline("NCCN 结直肠癌指南2025", "NCCN", "2025", "肿瘤科", "https://guide.medlive.cn/search?q=NCCN%20%E7%BB%93%E7%9B%B4%E8%82%A0%E7%99%8C%E6%8C%87%E5%8D%972025%20PDF"),
        Guideline("CSCO 肝癌指南2024", "CSCO", "2024", "肿瘤科", "https://guide.medlive.cn/search?q=CSCO%20%E8%82%9D%E7%99%8C%E6%8C%87%E5%8D%972024%20PDF"),
        Guideline("中国肺癌筛查指南2024", "中华医学会肿瘤分会", "2024", "肿瘤科", "https://guide.medlive.cn/search?q=%E4%B8%AD%E5%9B%BD%E8%82%BA%E7%99%8C%E7%AD%9B%E6%9F%A5%E6%8C%87%E5%8D%972024%20PDF"),
        Guideline("WHO 结核感染筛查指南2024", "WHO", "2024", "感染科", "https://guide.medlive.cn/search?q=WHO%20%E7%BB%93%E6%A0%B8%E6%84%9F%E6%9F%93%E7%AD%9B%E6%9F%A5%E6%8C%87%E5%8D%972024%20PDF"),
        Guideline("IDSA 艰难梭菌指南2023", "IDSA", "2023", "感染科", "https://guide.medlive.cn/search?q=IDSA%20%E8%89%B0%E9%9A%BE%E6%A2%AD%E8%8F%8C%E6%8C%87%E5%8D%972023%20PDF"),
        Guideline("中国流感诊治指南2024", "中华医学会感染分会", "2024", "感染科", "https://guide.medlive.cn/search?q=%E4%B8%AD%E5%9B%BD%E6%B5%81%E6%84%9F%E8%AF%8A%E6%B2%BB%E6%8C%87%E5%8D%972024%20PDF"),
        Guideline("中国新型冠状病毒感染指南2024", "中华医学会", "2024", "感染科", "https://guide.medlive.cn/search?q=%E4%B8%AD%E5%9B%BD%E6%96%B0%E5%9E%8B%E5%86%A0%E7%8A%B6%E7%97%85%E6%AF%92%E6%84%9F%E6%9F%93%E6%8C%87%E5%8D%972024%20PDF"),
        Guideline("IDSA MRSA感染指南2023", "IDSA", "2023", "感染科", "https://guide.medlive.cn/search?q=IDSA%20MRSA%E6%84%9F%E6%9F%93%E6%8C%87%E5%8D%972023%20PDF"),
        Guideline("ASH 弥漫大B细胞淋巴瘤指南2024", "ASH", "2024", "血液科", "https://guide.medlive.cn/search?q=ASH%20%E5%BC%A5%E6%BC%AB%E5%A4%A7B%E7%BB%86%E8%83%9E%E6%B7%8B%E5%B7%B4%E7%98%A4%E6%8C%87%E5%8D%972024%20PDF"),
        Guideline("NCCN 急性髓系白血病指南2025", "NCCN", "2025", "血液科", "https://guide.medlive.cn/search?q=NCCN%20%E6%80%A5%E6%80%A7%E9%AB%93%E7%B3%BB%E7%99%BD%E8%A1%80%E7%97%85%E6%8C%87%E5%8D%972025%20PDF"),
        Guideline("中国免疫性血小板减少症指南2023", "中华医学会血液分会", "2023", "血液科", "https://guide.medlive.cn/search?q=%E4%B8%AD%E5%9B%BD%E5%85%8D%E7%96%AB%E6%80%A7%E8%A1%80%E5%B0%8F%E6%9D%BF%E5%87%8F%E5%B0%91%E7%97%87%E6%8C%87%E5%8D%972023%20PDF"),
        Guideline("AAOS 膝关节置换指南2023", "AAOS", "2023", "骨科", "https://guide.medlive.cn/search?q=AAOS%20%E8%86%9D%E5%85%B3%E8%8A%82%E7%BD%AE%E6%8D%A2%E6%8C%87%E5%8D%972023%20PDF"),
        Guideline("中国腰椎间盘突出症指南2024", "中华医学会骨科分会", "2024", "骨科", "https://guide.medlive.cn/search?q=%E4%B8%AD%E5%9B%BD%E8%85%B0%E6%A4%8E%E9%97%B4%E7%9B%98%E7%AA%81%E5%87%BA%E7%97%87%E6%8C%87%E5%8D%972024%20PDF"),
        Guideline("NICE 髋部骨折指南2023", "NICE", "2023", "骨科", "https://guide.medlive.cn/search?q=NICE%20%E9%AB%8B%E9%83%A8%E9%AA%A8%E6%8A%98%E6%8C%87%E5%8D%972023%20PDF"),
        Guideline("中国脊柱侧弯指南2023", "中华医学会骨科分会", "2023", "骨科", "https://guide.medlive.cn/search?q=%E4%B8%AD%E5%9B%BD%E8%84%8A%E6%9F%B1%E4%BE%A7%E5%BC%AF%E6%8C%87%E5%8D%972023%20PDF"),
        Guideline("ACOG 妊娠期高血压指南2024", "ACOG", "2024", "妇产科", "https://guide.medlive.cn/search?q=ACOG%20%E5%A6%8A%E5%A8%A0%E6%9C%9F%E9%AB%98%E8%A1%80%E5%8E%8B%E6%8C%87%E5%8D%972024%20PDF"),
        Guideline("中国产后出血指南2023", "中华医学会妇产分会", "2023", "妇产科", "https://guide.medlive.cn/search?q=%E4%B8%AD%E5%9B%BD%E4%BA%A7%E5%90%8E%E5%87%BA%E8%A1%80%E6%8C%87%E5%8D%972023%20PDF"),
        Guideline("ACOG 月经异常指南2023", "ACOG", "2023", "妇产科", "https://guide.medlive.cn/search?q=ACOG%20%E6%9C%88%E7%BB%8F%E5%BC%82%E5%B8%B8%E6%8C%87%E5%8D%972023%20PDF"),
        Guideline("中国新生儿复苏指南2024", "中华医学会儿科分会", "2024", "儿科", "https://guide.medlive.cn/search?q=%E4%B8%AD%E5%9B%BD%E6%96%B0%E7%94%9F%E5%84%BF%E5%A4%8D%E8%8B%8F%E6%8C%87%E5%8D%972024%20PDF"),
        Guideline("AAP 儿童哮喘指南2023", "AAP", "2023", "儿科", "https://guide.medlive.cn/search?q=AAP%20%E5%84%BF%E7%AB%A5%E5%93%AE%E5%96%98%E6%8C%87%E5%8D%972023%20PDF"),
        Guideline("中国儿童腹泻病指南2023", "中华医学会儿科分会", "2023", "儿科", "https://guide.medlive.cn/search?q=%E4%B8%AD%E5%9B%BD%E5%84%BF%E7%AB%A5%E8%85%B9%E6%B3%BB%E7%97%85%E6%8C%87%E5%8D%972023%20PDF"),
        Guideline("AAO 白内障指南2024", "AAO", "2024", "眼科", "https://guide.medlive.cn/search?q=AAO%20%E7%99%BD%E5%86%85%E9%9A%9C%E6%8C%87%E5%8D%972024%20PDF"),
        Guideline("中国眼底病指南2023", "中华医学会眼科分会", "2023", "眼科", "https://guide.medlive.cn/search?q=%E4%B8%AD%E5%9B%BD%E7%9C%BC%E5%BA%95%E7%97%85%E6%8C%87%E5%8D%972023%20PDF"),
        Guideline("AAO-HNS 中耳炎指南2023", "AAO-HNS", "2023", "耳鼻喉科", "https://guide.medlive.cn/search?q=AAO-HNS%20%E4%B8%AD%E8%80%B3%E7%82%8E%E6%8C%87%E5%8D%972023%20PDF"),
        Guideline("中国耳聋指南2023", "中华医学会ENT分会", "2023", "耳鼻喉科", "https://guide.medlive.cn/search?q=%E4%B8%AD%E5%9B%BD%E8%80%B3%E8%81%8B%E6%8C%87%E5%8D%972023%20PDF"),
        Guideline("BAP 双相障碍指南2024", "BAP", "2024", "精神科", "https://guide.medlive.cn/search?q=BAP%20%E5%8F%8C%E7%9B%B8%E9%9A%9C%E7%A2%8D%E6%8C%87%E5%8D%972024%20PDF"),
        Guideline("中国抑郁障碍指南2023", "中华医学会精神分会", "2023", "精神科", "https://guide.medlive.cn/search?q=%E4%B8%AD%E5%9B%BD%E6%8A%91%E9%83%81%E9%9A%9C%E7%A2%8D%E6%8C%87%E5%8D%972023%20PDF"),
        Guideline("APA 精神分裂症指南2023", "APA", "2023", "精神科", "https://guide.medlive.cn/search?q=APA%20%E7%B2%BE%E7%A5%9E%E5%88%86%E8%A3%82%E7%97%87%E6%8C%87%E5%8D%972023%20PDF"),
        Guideline("ISBI 烧伤指南2023", "ISBI", "2023", "烧伤科", "https://guide.medlive.cn/search?q=ISBI%20%E7%83%A7%E4%BC%A4%E6%8C%87%E5%8D%972023%20PDF"),
        Guideline("ASA 困难气道指南2024", "ASA", "2024", "麻醉科", "https://guide.medlive.cn/search?q=ASA%20%E5%9B%B0%E9%9A%BE%E6%B0%94%E9%81%93%E6%8C%87%E5%8D%972024%20PDF"),
        Guideline("中国麻醉质控指南2023", "中华医学会麻醉分会", "2023", "麻醉科", "https://guide.medlive.cn/search?q=%E4%B8%AD%E5%9B%BD%E9%BA%BB%E9%86%89%E8%B4%A8%E6%8E%A7%E6%8C%87%E5%8D%972023%20PDF"),
        Guideline("SFAR 围手术期管理指南2024", "SFAR", "2024", "麻醉科", "https://guide.medlive.cn/search?q=SFAR%20%E5%9B%B4%E6%89%8B%E6%9C%AF%E6%9C%9F%E7%AE%A1%E7%90%86%E6%8C%87%E5%8D%972024%20PDF"),
        Guideline("中国口腔颌面外科指南2023", "中华医学会口腔分会", "2023", "口腔科", "https://guide.medlive.cn/search?q=%E4%B8%AD%E5%9B%BD%E5%8F%A3%E8%85%94%E9%A2%8C%E9%9D%A2%E5%A4%96%E7%A7%91%E6%8C%87%E5%8D%972023%20PDF"),
        Guideline("中国康复评定指南2024", "中华医学会康复分会", "2024", "康复科", "https://guide.medlive.cn/search?q=%E4%B8%AD%E5%9B%BD%E5%BA%B7%E5%A4%8D%E8%AF%84%E5%AE%9A%E6%8C%87%E5%8D%972024%20PDF"),
        Guideline("中国老年医学科指南2024", "中华医学会老年分会", "2024", "老年医学科", "https://guide.medlive.cn/search?q=%E4%B8%AD%E5%9B%BD%E8%80%81%E5%B9%B4%E5%8C%BB%E5%AD%A6%E7%A7%91%E6%8C%87%E5%8D%972024%20PDF"),
        Guideline("中国全科诊疗指南2023", "中华医学会全科分会", "2023", "全科", "https://guide.medlive.cn/search?q=%E4%B8%AD%E5%9B%BD%E5%85%A8%E7%A7%91%E8%AF%8A%E7%96%97%E6%8C%87%E5%8D%972023%20PDF"),
        Guideline("中国中医针灸指南2024", "中华中医药学会", "2024", "中医科", "https://guide.medlive.cn/search?q=%E4%B8%AD%E5%9B%BD%E4%B8%AD%E5%8C%BB%E9%92%88%E7%81%B8%E6%8C%87%E5%8D%972024%20PDF"),
        Guideline("中国中药饮片指南2023", "中华中医药学会", "2023", "中医科", "https://guide.medlive.cn/search?q=%E4%B8%AD%E5%9B%BD%E4%B8%AD%E8%8D%AF%E9%A5%AE%E7%89%87%E6%8C%87%E5%8D%972023%20PDF"),
        Guideline("SCCM 重症营养指南2023", "SCCM", "2023", "ICU", "https://guide.medlive.cn/search?q=SCCM%20%E9%87%8D%E7%97%87%E8%90%A5%E5%85%BB%E6%8C%87%E5%8D%972023%20PDF"),
        Guideline("中国VTE防治指南2024", "中华医学会血管分会", "2024", "血管外科", "https://guide.medlive.cn/search?q=%E4%B8%AD%E5%9B%BDVTE%E9%98%B2%E6%B2%BB%E6%8C%87%E5%8D%972024%20PDF"),
        Guideline("EAU 前列腺癌指南2025", "EAU", "2025", "泌尿外科", "https://guide.medlive.cn/search?q=EAU%20%E5%89%8D%E5%88%97%E8%85%BA%E7%99%8C%E6%8C%87%E5%8D%972025%20PDF"),
        Guideline("中国皮肤科指南汇编2024", "中华医学会皮肤分会", "2024", "皮肤科", "https://guide.medlive.cn/search?q=%E4%B8%AD%E5%9B%BD%E7%9A%AE%E8%82%A4%E7%A7%91%E6%8C%87%E5%8D%97%E6%B1%87%E7%BC%962024%20PDF"),
        Guideline("ATS 成人肺炎指南2023", "ATS/IDSA", "2023", "呼吸科", "https://guide.medlive.cn/search?q=ATS%20%E6%88%90%E4%BA%BA%E8%82%BA%E7%82%8E%E6%8C%87%E5%8D%972023%20PDF"),
        Guideline("ESMO 胃癌指南2025", "ESMO", "2025", "肿瘤科", "https://guide.medlive.cn/search?q=ESMO%20%E8%83%83%E7%99%8C%E6%8C%87%E5%8D%972025%20PDF"),
    )

    val categories = all.map { it.dept }.distinct().sorted()

    fun search(query: String) = all.filter {
        val q = query.lowercase()
        it.title.lowercase().contains(q) || it.keywords.lowercase().contains(q) || it.source.lowercase().contains(q) || it.dept.lowercase().contains(q)
    }

    fun byDept(dept: String) = all.filter { it.dept == dept }
}
