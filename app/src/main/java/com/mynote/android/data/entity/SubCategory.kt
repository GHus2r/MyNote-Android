package com.mynote.android.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 子主题分类
 * 对应 uni-app parentCategories 里的 subCategories 数组
 * 关联到 ParentCategory
 */
@Entity(
    tableName = "sub_categories",
    foreignKeys = [
        ForeignKey(
            entity = ParentCategory::class,
            parentColumns = ["id"],
            childColumns = ["parentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("parentId")]
)
data class SubCategory(
    @PrimaryKey
    val id: String,
    val parentId: String,
    val name: String,
    val color: String = "",     // 子主题独立颜色；空字符串表示继承父分类颜色
    val sortOrder: Int = 0
)
