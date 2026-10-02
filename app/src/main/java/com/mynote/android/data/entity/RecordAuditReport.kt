package com.mynote.android.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 病历照片质控分析报告 — 落库实体
 * 对应「病历照片上传与分析」功能：OCR 识图 → 规范性/准确性/漏写项分析 → 改进方案
 *
 * 可追溯设计：
 * - sourceImageHash：原图哈希，溯源到具体照片
 * - ocrRawText：完整保留 OCR 原文（含可能的错字），识别→判定链路可见
 * - ocrEngine / model / promptVersion：固化识别与分析所用的模型与 prompt 版本
 * - reportJson：完整结构化报告（漏写项/规范问题/准确性问题/改进方案），供复审
 *
 * 非破坏式约定：patientId=0 表示未关联患者；不设外键，避免影响现有 patients 表。
 */
@Entity(
    tableName = "record_audits",
    indices = [Index("patientId"), Index("createdAt")]
)
data class RecordAuditReport(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val patientId: Long = 0,          // 0 = 未关联患者

    val dept: String = "",            // 科室（决定判定依据）
    val recordType: String = "",      // 文书类型：入院记录/首次病程/日常病程/出院小结/上级查房...

    val sourceImageHash: String = "", // 原图哈希，溯源
    val ocrRawText: String = "",      // OCR 原始全文（可追溯）
    val ocrEngine: String = "",       // OCR 引擎/模型
    val model: String = "",           // AI 分析模型
    val promptVersion: String = "",   // 审计 prompt 版本号

    val scoreTotal: Int = 0,          // 总分 0-100
    val scoreCompleteness: Int = 0,   // 完整性（漏写项）得分
    val scoreNorms: Int = 0,          // 规范性得分
    val scoreAccuracy: Int = 0,       // 准确性得分

    val summary: String = "",         // 一句话结论
    val reportJson: String = "",      // 完整结构化报告 JSON

    val createdAt: Long = 0           // 字面量默认，插入时显式传时间
)
