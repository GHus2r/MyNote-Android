package com.mynote.android.util

/** 儿童生长发育参考值 + 用药剂量速算 */
object PediatricGrowth {
    data class Growth(val title: String, val category: String, val ageRange: String, val normalData: String)

    val all: List<Growth> = listOf(
        // 生长参考
        Growth("体重-出生", "体重", "出生(足月)",
            "男:3.3kg(2.5-4.0) 女:3.2kg(2.4-3.9)\n生理性体重下降:生后3-4天降5-10%,7-10天恢复"),
        Growth("体重-婴儿期增长", "体重", "0-12月",
            "0-3月:每日+25-30g(约800g/月)\n3-6月:每日+20g(约600g/月)\n6-12月:每日+15g(约500g/月)\n公式:1-6月=出生体重+月龄×0.7\n7-12月=6+月龄×0.25"),
        Growth("体重-1-12岁公式", "体重", "1-12岁",
            "公式:体重(kg)=年龄×2+8\n3月≈6kg, 12月≈10kg, 2岁≈12kg, 5岁≈18kg"),
        Growth("身高/身长-出生", "身高/身长", "出生(足月)",
            "平均50cm(48-53cm)\n0-3月:每月+3.5cm\n3-6月:每月+2cm\n6-12月:每月+1.2cm"),
        Growth("身高/身长-1-12岁公式", "身高/身长", "1-12岁",
            "公式:身高(cm)=年龄×7+75(HK公式)\n1岁≈75cm, 2岁≈87cm, 5岁≈110cm, 10岁≈140cm"),
        Growth("头围参考", "头围", "0-2岁",
            "出生:34cm(平均)\n0-3月:每月+2cm(1岁前)\n3-6月:每月+1cm\n6-12月:每月+0.5cm\n1岁:46cm; 2岁:48cm; 5岁:50cm"),
        Growth("前囟闭合时间", "骨骼发育", "关键里程碑",
            "正常闭合:12-18月(最迟≤24月)\n早闭(<6月):颅缝早闭可能\n晚闭(>24月):佝偻病/甲减/颅内压增高"),
        Growth("牙齿萌出", "骨骼发育", "乳牙列",
            "中切牙:6-8月\n侧切牙:8-10月\n第一乳磨牙:12-16月\n尖牙:16-20月\n第二乳磨牙:20-30月\n乳牙数=月龄-4(≤24月)\n恒牙:6岁第一磨牙(六龄齿)"),

        // BMI/营养
        Growth("儿童超重/肥胖标准", "营养评估", "2-18岁",
            "≥85百分位BMI(超重)\n≥95百分位BMI(肥胖)\n严重肥胖≥99百分位或BMI≥35\n临床;需考虑发育曲线,不只看单一数字"),

        // 生命体征
        Growth("生命体征-新生儿(0-28天)", "生命体征", "新生儿期",
            "HR:120-160(睡眠可120,哭闹达180)\nRR:30-60\nSBP:60-90(平均70)\n体温:36.5-37.5℃"),
        Growth("生命体征-婴儿(1-12月)", "生命体征", "婴儿期",
            "HR:100-150(睡眠80-140)\nRR:25-40\nSBP:80-100\nSpO2:>95%"),
        Growth("生命体征-幼儿/学龄前(1-5岁)", "生命体征", "幼儿期",
            "HR:80-140\nRR:20-30\nSBP:90-110\n8-12h睡眠+1-2h午睡"),
        Growth("生命体征-学龄儿童(6-12岁)", "生命体征", "学龄期",
            "HR:70-120\nRR:18-25\nSBP:100-120\n9-11h睡眠"),
        Growth("低血压识别(SBP <5th%)", "生命体征/急救", "1-10岁",
            "SBP下限≈70+(年龄×2)\n例:5岁下限=80mmHg; 10岁下限=90mmHg"),

        // 用药剂量
        Growth("儿童常用药-退热/镇痛", "用药剂量", "按体重",
            "对乙酰氨基酚:10-15mg/kg q4-6h(最大75mg/kg/d)\n布洛芬:5-10mg/kg q6-8h(最大40mg/kg/d)\n>3月"),
        Growth("儿童常用药-抗生素", "用药剂量", "按体重",
            "阿莫西林:40-90mg/kg/d tid(肺炎用高量)\n头孢曲松:50-100mg/kg/d q12-24h IV\n阿奇霉素:10mg/kg/d qd×3d"),
        Growth("儿童常用药-抗组胺/过敏", "用药剂量", "按体重",
            "氯雷他定:>2岁:5mg qd(<30kg); 10mg qd(>30kg)\n西替利嗪:6-12月:2.5mg qd; 1-2岁:2.5mg bid; >2岁:5-10mg qd"),
        Growth("肾上腺素-过敏性休克(儿童)", "用药剂量/急救", "按体重",
            "IM:0.01mg/kg(1:1000肾上腺素)\n最大0.3mg(儿童)/0.5mg(青少年)\n每5-15min可重复"),
        Growth("儿童补液-维持量(4-2-1)", "补液", "任何年龄",
            "4mL/kg/h ×前10kg\n+2mL/kg/h ×第二个10kg\n+1mL/kg/h ×其余kg\n例:25kg=4×10+2×10+1×5=65mL/h"),
    )

    val categories = all.map { it.category }.distinct().sorted()
    fun search(q: String) = all.filter {
        val lq = q.lowercase()
        it.title.lowercase().contains(lq) || it.normalData.lowercase().contains(lq) || it.category.lowercase().contains(lq) || it.ageRange.lowercase().contains(lq)
    }
}
