package com.mynote.android.data.dao

import androidx.room.*
import com.mynote.android.data.entity.VitalSigns
import kotlinx.coroutines.flow.Flow

@Dao
interface VitalSignsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(v: VitalSigns): Long

    @Query("SELECT * FROM vital_signs WHERE patientId = :patientId ORDER BY recordedAt DESC")
    fun getByPatient(patientId: Long): Flow<List<VitalSigns>>

    @Query("SELECT * FROM vital_signs WHERE recordId = :recordId")
    suspend fun getByRecord(recordId: Long): VitalSigns?

    @Query("DELETE FROM vital_signs WHERE recordId = :recordId")
    suspend fun deleteByRecord(recordId: Long)

    @Query("SELECT * FROM vital_signs WHERE patientId = :patientId AND recordedAt >= :since ORDER BY recordedAt ASC")
    suspend fun getSince(patientId: Long, since: Long): List<VitalSigns>
}
