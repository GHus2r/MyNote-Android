package com.mynote.android.data.dao

import androidx.room.*
import com.mynote.android.data.entity.MedicalRecord
import com.mynote.android.data.entity.Patient
import kotlinx.coroutines.flow.Flow

@Dao
interface PatientDao {
    @Query("SELECT * FROM patients ORDER BY admissionDate DESC, createdAt DESC")
    fun getAll(): Flow<List<Patient>>

    @Query("SELECT * FROM patients ORDER BY admissionDate DESC, createdAt DESC")
    suspend fun getAllSync(): List<Patient>

    @Query("SELECT * FROM patients WHERE id = :id")
    suspend fun getById(id: Long): Patient?

    @Query("SELECT * FROM patients WHERE name LIKE '%' || :query || '%' OR department LIKE '%' || :query || '%'")
    fun search(query: String): Flow<List<Patient>>

    @Query("""
        SELECT * FROM patients WHERE
        (name LIKE '%' || :query || '%' OR department LIKE '%' || :query || '%' OR bedNumber LIKE '%' || :query || '%' OR diagnosis LIKE '%' || :query || '%')
        AND (:dept = '' OR department = :dept)
        ORDER BY admissionDate DESC, createdAt DESC
    """)
    fun searchFiltered(query: String, dept: String): Flow<List<Patient>>

    @Query("SELECT DISTINCT department FROM patients WHERE department != '' ORDER BY department")
    suspend fun getAllDepartments(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(patient: Patient): Long

    @Update
    suspend fun update(patient: Patient)

    @Delete
    suspend fun delete(patient: Patient)

    @Query("DELETE FROM patients WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface MedicalRecordDao {
    @Query("SELECT * FROM medical_records WHERE patientId = :patientId ORDER BY createdAt DESC")
    fun getByPatient(patientId: Long): Flow<List<MedicalRecord>>

    @Query("SELECT * FROM medical_records WHERE patientId = :patientId ORDER BY createdAt DESC")
    suspend fun getByPatientSync(patientId: Long): List<MedicalRecord>

    @Query("SELECT * FROM medical_records WHERE id = :id")
    suspend fun getById(id: Long): MedicalRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: MedicalRecord): Long

    @Update
    suspend fun update(record: MedicalRecord)

    @Delete
    suspend fun delete(record: MedicalRecord)

    @Query("DELETE FROM medical_records WHERE patientId = :patientId")
    suspend fun deleteByPatient(patientId: Long)
}
