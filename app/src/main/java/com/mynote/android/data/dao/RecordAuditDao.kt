package com.mynote.android.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mynote.android.data.entity.RecordAuditReport
import kotlinx.coroutines.flow.Flow

/**
 * 病历质控报告 DAO（独立文件，不与现有 DAO 混写，避免改动现有文件）
 */
@Dao
interface RecordAuditDao {
    @Query("SELECT * FROM record_audits ORDER BY createdAt DESC")
    fun getAll(): Flow<List<RecordAuditReport>>

    @Query("SELECT * FROM record_audits ORDER BY createdAt DESC")
    suspend fun getAllSync(): List<RecordAuditReport>

    @Query("SELECT * FROM record_audits WHERE patientId = :patientId AND patientId != 0 ORDER BY createdAt DESC")
    fun getByPatient(patientId: Long): Flow<List<RecordAuditReport>>

    @Query("SELECT * FROM record_audits WHERE patientId = :patientId AND patientId != 0 ORDER BY createdAt DESC")
    suspend fun getByPatientSync(patientId: Long): List<RecordAuditReport>

    @Query("SELECT * FROM record_audits WHERE id = :id")
    suspend fun getById(id: Long): RecordAuditReport?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(report: RecordAuditReport): Long

    @Delete
    suspend fun delete(report: RecordAuditReport)

    @Query("DELETE FROM record_audits WHERE id = :id")
    suspend fun deleteById(id: Long)
}
