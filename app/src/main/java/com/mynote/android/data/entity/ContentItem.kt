package com.mynote.android.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 笔记内容项
 * 对应 uni-app Note.contentList 数组的每一项
 *
 * type 取值:
 *  - text:  纯文本,content 为文字内容
 *  - html:  富文本,content 为 HTML 字符串
 *  - voice: 语音,content 为文件路径
 *  - image: 图片,content 为文件路径
 *  - video: 视频,content 为文件路径
 *  - shape: 形状,content 为形状类型(circle/square/triangle/star/heart/diamond/pentagon/hexagon)
 *
 * transform 仅对 image/video/shape 有效(拖拽/缩放/旋转)
 */
@Entity(
    tableName = "content_items",
    foreignKeys = [
        ForeignKey(
            entity = Note::class,
            parentColumns = ["id"],
            childColumns = ["noteId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("noteId")]
)
data class ContentItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val noteId: String,
    val type: String,           // text / html / voice / image / video / shape
    val content: String,        // 内容或文件路径或形状类型
    val timestamp: String,      // "yyyy-MM-dd HH:mm"
    // 变换信息(仅 image/video/shape)
    val transformX: Float = 0f,
    val transformY: Float = 0f,
    val transformScale: Float = 1f,
    val transformRotation: Float = 0f,
    // 语音时长(仅 voice)
    val voiceDuration: Long = 0L,
    // 语音时间戳 JSON: [{"t":5000,"txt":"患者述..."}] (仅 voice)
    val voiceTimestamps: String = "",
    // 语音转文字结果 (仅 voice)
    val voiceTranscript: String = ""
)
