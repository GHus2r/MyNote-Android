package com.mynote.android.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.mynote.android.data.dao.CategoryDao
import com.mynote.android.data.dao.MeetingDao
import com.mynote.android.data.dao.MedicalRecordDao
import com.mynote.android.data.dao.NoteDao
import com.mynote.android.data.dao.PatientDao
import com.mynote.android.data.dao.VitalSignsDao
import com.mynote.android.data.entity.ContentItem
import com.mynote.android.data.entity.MedicalRecord
import com.mynote.android.data.entity.Meeting
import com.mynote.android.data.entity.MeetingEntry
import com.mynote.android.data.entity.Note
import com.mynote.android.data.entity.ParentCategory
import com.mynote.android.data.entity.Patient
import com.mynote.android.data.entity.SubCategory
import com.mynote.android.data.entity.VitalSigns

@Database(
    entities = [
        ParentCategory::class,
        SubCategory::class,
        Note::class,
        ContentItem::class,
        Patient::class,
        MedicalRecord::class,
        VitalSigns::class,
        Meeting::class,
        MeetingEntry::class
    ],
    version = 12,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun categoryDao(): CategoryDao
    abstract fun noteDao(): NoteDao
    abstract fun patientDao(): PatientDao
    abstract fun medicalRecordDao(): MedicalRecordDao
    abstract fun vitalSignsDao(): VitalSignsDao
    abstract fun meetingDao(): MeetingDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE sub_categories ADD COLUMN color TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notes ADD COLUMN tags TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE notes ADD COLUMN isTodo INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notes ADD COLUMN isPinned INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE content_items ADD COLUMN voiceTimestamps TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE content_items ADD COLUMN voiceTranscript TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) { /* 无 schema 变更 */ }
        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) { /* 无 schema 变更 */ }
        }

        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS vital_signs (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        patientId INTEGER NOT NULL,
                        recordId INTEGER NOT NULL,
                        recordedAt INTEGER NOT NULL,
                        temperature REAL,
                        pulse INTEGER,
                        respiration INTEGER,
                        bpSystolic INTEGER,
                        bpDiastolic INTEGER,
                        spo2 INTEGER,
                        weightKg REAL,
                        hba1c REAL,
                        fastingGlu REAL,
                        creatinine REAL,
                        creatinineUnit TEXT,
                        egfr REAL,
                        potassium REAL,
                        sodium REAL,
                        hemoglobin REAL,
                        albumin REAL,
                        alt REAL,
                        ast REAL,
                        ldl REAL
                    )
                """.trimIndent())
            }
        }

        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) { /* 无 schema 变更 */ }
        }

        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notes ADD COLUMN isTrashed INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE notes ADD COLUMN trashedAt INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `meetings` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, `participants` TEXT NOT NULL, `startedAt` INTEGER NOT NULL, `endedAt` INTEGER NOT NULL DEFAULT 0, `summary` TEXT NOT NULL DEFAULT '', `report` TEXT NOT NULL DEFAULT '', `createdAt` INTEGER NOT NULL DEFAULT 0)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `meeting_entries` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `meetingId` INTEGER NOT NULL, `speaker` TEXT NOT NULL, `content` TEXT NOT NULL, `timestamp` INTEGER NOT NULL DEFAULT 0, FOREIGN KEY(`meetingId`) REFERENCES `meetings`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_meeting_entries_meetingId` ON `meeting_entries` (`meetingId`)")
            }
        }

        fun get(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mynote.db"
                )
                    .addMigrations(
                        MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6,
                        MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11,
                        MIGRATION_11_12
                    )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
