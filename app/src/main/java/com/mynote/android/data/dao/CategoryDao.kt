package com.mynote.android.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.mynote.android.data.entity.ParentCategory
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    // ===== 大主题 =====
    @Query("SELECT * FROM parent_categories ORDER BY sortOrder ASC")
    fun observeParentCategories(): Flow<List<ParentCategory>>

    @Query("SELECT * FROM parent_categories ORDER BY sortOrder ASC")
    suspend fun getParentCategories(): List<ParentCategory>

    @Query("SELECT * FROM parent_categories WHERE id = :id")
    suspend fun getParentCategory(id: String): ParentCategory?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertParentCategory(category: ParentCategory)

    @Update
    suspend fun updateParentCategory(category: ParentCategory)

    @Delete
    suspend fun deleteParentCategory(category: ParentCategory)

    @Query("DELETE FROM parent_categories WHERE id = :id")
    suspend fun deleteParentCategoryById(id: String)

    // ===== 子主题 =====
    @Query("SELECT * FROM sub_categories WHERE parentId = :parentId ORDER BY sortOrder ASC")
    fun observeSubCategories(parentId: String): Flow<List<com.mynote.android.data.entity.SubCategory>>

    @Query("SELECT * FROM sub_categories WHERE parentId = :parentId ORDER BY sortOrder ASC")
    suspend fun getSubCategories(parentId: String): List<com.mynote.android.data.entity.SubCategory>

    @Query("SELECT * FROM sub_categories WHERE id = :id")
    suspend fun getSubCategory(id: String): com.mynote.android.data.entity.SubCategory?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubCategory(subCategory: com.mynote.android.data.entity.SubCategory)

    @Update
    suspend fun updateSubCategory(subCategory: com.mynote.android.data.entity.SubCategory)

    @Delete
    suspend fun deleteSubCategory(subCategory: com.mynote.android.data.entity.SubCategory)

    @Query("DELETE FROM sub_categories WHERE id = :id")
    suspend fun deleteSubCategoryById(id: String)

    @Query("SELECT * FROM sub_categories")
    suspend fun getAllSubCategories(): List<com.mynote.android.data.entity.SubCategory>
}
