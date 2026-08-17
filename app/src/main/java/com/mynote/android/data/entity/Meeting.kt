package com.mynote.android.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 会议记录
 */
@Entity(tableName = "meetings")
data class Meeting(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,                    // 会议标题
    val participants: String,             // 参会人，逗号分隔
    val startedAt: Long,                  // 开始时间戳
    val endedAt: Long = 0,                // 结束时间戳
    val summary: String = "",             // AI 摘要
    val report: String = "",              // 汇报文档
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * 会议发言条目
 */
@Entity(
    tableName = "meeting_entries",
    foreignKeys = [ForeignKey(
        entity = Meeting::class,
        parentColumns = ["id"],
        childColumns = ["meetingId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("meetingId")]
)
data class MeetingEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val meetingId: Long,
    val speaker: String,                  // 发言人
    val content: String,                  // 发言内容
    val timestamp: Long = System.currentTimeMillis()
)
