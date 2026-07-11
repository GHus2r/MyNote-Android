package com.mynote.android.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "medical_records",
    foreignKeys = [
        ForeignKey(
            entity = Patient::class,
            parentColumns = ["id"],
            childColumns = ["patientId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("patientId")]
)
data class MedicalRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val patientId: Long,
    val type: String,               // 入院记录/首次病程/日常病程/出院小结/上级查房/会诊记录/抢救记录
    val subType: String = "",       // 具体细分: SOAP/精简版/完整版 等
    val content: String,            // 病历正文
    val createdAt: Long = System.currentTimeMillis(),
    val generatedBy: String = ""    // AI / 手动
)
