package com.mynote.android.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 笔记
 * 对应 uni-app 的 notes storage key
 * 关联到 SubCategory
 */
@Entity(
    tableName = "notes",
    foreignKeys = [
        ForeignKey(
            entity = SubCategory::class,
            parentColumns = ["id"],
            childColumns = ["subCategoryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("subCategoryId")]
)
data class Note(
    @PrimaryKey
    val id: String,
    val subCategoryId: String,
    val title: String,
    val contentText: String = "",    // 纯文本副本(搜索用)
    val updateTime: String,          // "yyyy-MM-dd HH:mm" 格式,保持与原项目一致
    val createTime: String,
    val sortOrder: Int = 0,
    val tags: String = "",           // 逗号分隔的标签,如 "待办,重要,病例"
    val isTodo: Boolean = false,     // 是否为待办清单笔记
    val isPinned: Boolean = false,   // 是否置顶
    val isTrashed: Boolean = false,  // 是否在回收站
    val trashedAt: Long = 0L         // 移入回收站时间戳
)
