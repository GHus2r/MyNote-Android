package com.mynote.android.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 大主题分类
 * 对应 uni-app 的 parentCategories storage key
 */
@Entity(tableName = "parent_categories")
data class ParentCategory(
    @PrimaryKey
    val id: String,
    val name: String,
    val color: String,        // 颜色值如 "#4CAF50"
    val sortOrder: Int = 0
)
