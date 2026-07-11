package com.mynote.android.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "patients")
data class Patient(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String = "",
    val age: Int = 0,
    val gender: String = "",          // 男/女
    val bedNumber: String = "",       // 床号
    val department: String = "",      // 科室
    val admissionDate: String = "",   // yyyy-MM-dd
    val diagnosis: String = "",       // 入院诊断
    val chiefComplaint: String = "",  // 主诉
    val followupDate: String = "",    // 下次随访日期 yyyy-MM-dd
    val createdAt: Long = System.currentTimeMillis()
)
