package com.mynote.android.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** 从病历/笔记中自动解析的结构化体征与化验数据 */
@Entity(tableName = "vital_signs")
data class VitalSigns(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val patientId: Long,
    val recordId: Long,          // 关联病历 ID
    val recordedAt: Long = System.currentTimeMillis(),

    // 生命体征
    val temperature: Double? = null,   // ℃
    val pulse: Int? = null,            // 次/分
    val respiration: Int? = null,      // 次/分
    val bpSystolic: Int? = null,       // mmHg
    val bpDiastolic: Int? = null,      // mmHg
    val spo2: Int? = null,             // %
    val weightKg: Double? = null,      // kg

    // 核心化验
    val hba1c: Double? = null,         // %
    val fastingGlu: Double? = null,    // mmol/L
    val creatinine: Double? = null,    // µmol/L 或 mg/dL（标记单位）
    val creatinineUnit: String? = null,// "umol/L" 或 "mg/dL"
    val egfr: Double? = null,          // mL/min/1.73m²
    val potassium: Double? = null,     // mmol/L
    val sodium: Double? = null,        // mmol/L
    val hemoglobin: Double? = null,    // g/L
    val albumin: Double? = null,       // g/L
    val alt: Double? = null,           // U/L
    val ast: Double? = null,           // U/L
    val ldl: Double? = null            // mmol/L
)
