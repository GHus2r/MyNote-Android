package com.mynote.android.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.mynote.android.data.entity.ContentItem
import com.mynote.android.data.entity.Note
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {

    // ===== 笔记 =====
    @Query("SELECT * FROM notes WHERE subCategoryId = :subCategoryId AND isTrashed = 0 ORDER BY isPinned DESC, sortOrder ASC, updateTime DESC")
    fun observeNotesBySubCategory(subCategoryId: String): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE subCategoryId = :subCategoryId AND isTrashed = 0 ORDER BY isPinned DESC, sortOrder ASC, updateTime DESC")
    suspend fun getNotesBySubCategory(subCategoryId: String): List<Note>

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getNote(id: String): Note?

    @Query("""
        SELECT * FROM notes
        WHERE isTrashed = 0 AND (
            title LIKE '%' || :keyword || '%'
           OR contentText LIKE '%' || :keyword || '%'
           OR tags LIKE '%' || :keyword || '%'
           OR id IN (SELECT noteId FROM content_items WHERE content LIKE '%' || :keyword || '%')
        )
        ORDER BY isPinned DESC, updateTime DESC
    """)
    suspend fun searchNotes(keyword: String): List<Note>

    @Query("SELECT * FROM notes WHERE isTrashed = 0 AND tags LIKE '%' || :tag || '%' ORDER BY isPinned DESC, updateTime DESC")
    suspend fun getNotesByTag(tag: String): List<Note>

    @Query("SELECT * FROM notes WHERE isTrashed = 0 AND isTodo = 1 ORDER BY isPinned DESC, sortOrder ASC, updateTime DESC")
    suspend fun getTodoNotes(): List<Note>

    @Query("SELECT DISTINCT tags FROM notes WHERE isTrashed = 0 AND tags != ''")
    suspend fun getAllTags(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: Note)

    @Update
    suspend fun updateNote(note: Note)

    @Delete
    suspend fun deleteNote(note: Note)

    @Query("UPDATE notes SET isPinned = CASE WHEN isPinned = 0 THEN 1 ELSE 0 END WHERE id = :id")
    suspend fun togglePin(id: String)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteNoteById(id: String)

    @Query("SELECT * FROM notes WHERE isTrashed = 1 ORDER BY trashedAt DESC")
    suspend fun getTrashedNotes(): List<Note>

    @Query("UPDATE notes SET isTrashed = 1, trashedAt = :trashedAt, isPinned = 0 WHERE id = :id")
    suspend fun moveToTrash(id: String, trashedAt: Long)

    @Query("UPDATE notes SET isTrashed = 0, trashedAt = 0 WHERE id = :id")
    suspend fun restoreFromTrash(id: String)

    @Query("DELETE FROM notes WHERE isTrashed = 1 AND trashedAt < :beforeTime")
    suspend fun deleteExpiredTrash(beforeTime: Long)

    @Query("DELETE FROM notes WHERE isTrashed = 1")
    suspend fun emptyTrash()

    @Query("SELECT COUNT(*) FROM notes WHERE isTrashed = 1")
    suspend fun getTrashCount(): Int

    @Query("SELECT COUNT(*) FROM notes WHERE isTrashed = 0")
    suspend fun getNoteCount(): Int

    @Query("SELECT COALESCE(SUM(LENGTH(contentText)), 0) FROM notes WHERE isTrashed = 0")
    suspend fun getTotalWordCount(): Long

    @Query("SELECT COUNT(*) FROM notes WHERE isTrashed = 0 AND createTime >= :since")
    suspend fun getNotesSince(since: String): Int

    @Query("SELECT COUNT(DISTINCT substr(createTime, 1, 10)) FROM notes WHERE isTrashed = 0")
    suspend fun getActiveDays(): Int

    // ===== 内容项 =====
    @Query("SELECT * FROM content_items WHERE noteId = :noteId ORDER BY id ASC")
    suspend fun getContentItems(noteId: String): List<ContentItem>

    @Query("SELECT * FROM content_items WHERE noteId = :noteId ORDER BY id ASC")
    fun observeContentItems(noteId: String): Flow<List<ContentItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContentItem(item: ContentItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContentItems(items: List<ContentItem>): List<Long>

    @Update
    suspend fun updateContentItem(item: ContentItem)

    @Delete
    suspend fun deleteContentItem(item: ContentItem)

    @Query("DELETE FROM content_items WHERE id = :id")
    suspend fun deleteContentItemById(id: Long)

    @Query("DELETE FROM content_items WHERE noteId = :noteId")
    suspend fun deleteContentItemsByNote(noteId: String)
}
