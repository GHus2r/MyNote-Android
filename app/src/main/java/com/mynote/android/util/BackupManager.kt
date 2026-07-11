package com.mynote.android.util

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.mynote.android.data.AppDatabase
import com.mynote.android.data.entity.ContentItem
import com.mynote.android.data.entity.Note
import com.mynote.android.data.entity.ParentCategory
import com.mynote.android.data.entity.SubCategory
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * 本地备份/恢复管理器
 *
 * 支持两种备份方式：
 * 1. JSON 全量备份 — 备份所有分类、笔记、内容项（可跨版本恢复）
 * 2. 数据库文件备份 — 直接打包 Room .db 文件（速度最快，适合同版本迁移）
 */
object BackupManager {

    private const val BACKUP_DIR = "backups"
    private const val DB_NAME = "mynote.db"
    private val gson = Gson()

    // ===== JSON 全量备份 =====

    /**
     * 创建 JSON 全量备份，通过 SAF 让用户选择保存位置
     */
    suspend fun createJsonBackup(context: Context, targetUri: Uri): BackupResult {
        return try {
            val db = AppDatabase.get(context.applicationContext)
            val parents = db.categoryDao().getParentCategories()
            val subs = mutableListOf<SubCategory>()
            val notes = mutableListOf<Note>()
            val items = mutableListOf<ContentItem>()

            for (p in parents) {
                val subList = db.categoryDao().getSubCategories(p.id)
                subs.addAll(subList)
                for (s in subList) {
                    val noteList = db.noteDao().getNotesBySubCategory(s.id)
                    notes.addAll(noteList)
                    for (n in noteList) {
                        items.addAll(db.noteDao().getContentItems(n.id))
                    }
                }
            }

            val data = mapOf(
                "parentCategories" to parents,
                "subCategories" to subs,
                "notes" to notes,
                "contentItems" to items
            )

            val backup = mapOf(
                "version" to "3.0",
                "backupTime" to SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()),
                "appVersion" to getAppVersion(context),
                "data" to data
            )

            val json = gson.toJson(backup)
            context.contentResolver.openOutputStream(targetUri)?.use { it.write(json.toByteArray(Charsets.UTF_8)) }

            BackupResult.Success(
                noteCount = notes.size,
                itemCount = items.size,
                catCount = parents.size + subs.size
            )
        } catch (e: Exception) {
            BackupResult.Error(e.message ?: "备份失败")
        }
    }

    /**
     * 生成 JSON 备份字符串（用于云上传）
     */
    suspend fun exportJsonString(context: Context): String {
        val db = AppDatabase.get(context.applicationContext)
        val parents = db.categoryDao().getParentCategories()
        val subs = mutableListOf<SubCategory>()
        val notes = mutableListOf<Note>()
        val items = mutableListOf<ContentItem>()
        for (p in parents) {
            val sl = db.categoryDao().getSubCategories(p.id)
            subs.addAll(sl)
            for (s in sl) {
                val nl = db.noteDao().getNotesBySubCategory(s.id)
                notes.addAll(nl)
                for (n in nl) items.addAll(db.noteDao().getContentItems(n.id))
            }
        }
        val backup = mapOf(
            "version" to "3.0",
            "backupTime" to SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()),
            "appVersion" to getAppVersion(context),
            "data" to mapOf("parentCategories" to parents, "subCategories" to subs, "notes" to notes, "contentItems" to items)
        )
        return gson.toJson(backup)
    }

    /**
     * 从 JSON 字符串恢复数据（用于云端下载后恢复）
     */
    suspend fun importJsonString(context: Context, json: String, clearFirst: Boolean = false): Boolean {
        return try {
            val type = object : TypeToken<Map<String, Any>>() {}.type
            val backup: Map<String, Any> = gson.fromJson(json, type)
            @Suppress("UNCHECKED_CAST")
            val data = backup["data"] as? Map<String, Any> ?: (backup as? Map<String, Any>) ?: return false

            val db = AppDatabase.get(context.applicationContext)

            if (clearFirst) {
                val parents = db.categoryDao().getParentCategories()
                for (p in parents) {
                    val subs = db.categoryDao().getSubCategories(p.id)
                    for (s in subs) {
                        val notes = db.noteDao().getNotesBySubCategory(s.id)
                        for (n in notes) db.noteDao().deleteNote(n)
                    }
                }
                for (p in parents) db.categoryDao().deleteParentCategory(p)
            }

            // 父分类
            @Suppress("UNCHECKED_CAST")
            (data["parentCategories"] as? List<Map<String, Any>>)?.forEach { p ->
                db.categoryDao().insertParentCategory(GsonBuilder().create().fromJson(gson.toJson(p), ParentCategory::class.java))
            }
            // 子分类
            @Suppress("UNCHECKED_CAST")
            (data["subCategories"] as? List<Map<String, Any>>)?.forEach { s ->
                db.categoryDao().insertSubCategory(GsonBuilder().create().fromJson(gson.toJson(s), SubCategory::class.java))
            }
            // 笔记
            @Suppress("UNCHECKED_CAST")
            (data["notes"] as? List<Map<String, Any>>)?.forEach { n ->
                db.noteDao().insertNote(GsonBuilder().create().fromJson(gson.toJson(n), Note::class.java))
            }
            // 内容项
            @Suppress("UNCHECKED_CAST")
            (data["contentItems"] as? List<Map<String, Any>>)?.forEach { ci ->
                db.noteDao().insertContentItem(GsonBuilder().create().fromJson(gson.toJson(ci), ContentItem::class.java))
            }
            true
        } catch (e: Exception) {
            Log.e("BackupManager", "恢复失败", e)
            false
        }
    }

    /**
     * 从 JSON 备份文件恢复数据
     */
    suspend fun restoreJsonBackup(context: Context, sourceUri: Uri, clearFirst: Boolean = false): BackupResult {
        return try {
            val json = context.contentResolver.openInputStream(sourceUri)?.bufferedReader()?.readText()
                ?: return BackupResult.Error("无法读取备份文件")

            val type = object : TypeToken<Map<String, Any>>() {}.type
            val backup: Map<String, Any> = gson.fromJson(json, type)
            @Suppress("UNCHECKED_CAST")
            val data = backup["data"] as? Map<String, Any> ?: (backup as? Map<String, Any>)
                ?: return BackupResult.Error("无效的备份文件格式")

            val db = AppDatabase.get(context.applicationContext)

            // 可选：清空后恢复
            if (clearFirst) {
                val allNotes = mutableListOf<Note>()
                val parents = db.categoryDao().getParentCategories()
                for (p in parents) {
                    val subs = db.categoryDao().getSubCategories(p.id)
                    for (s in subs) {
                        allNotes.addAll(db.noteDao().getNotesBySubCategory(s.id))
                    }
                }
                for (n in allNotes) db.noteDao().deleteNote(n)
                for (p in parents) db.categoryDao().deleteParentCategory(p)
            }

            var catCount = 0
            var noteCount = 0
            var itemCount = 0

            // 恢复父分类
            @Suppress("UNCHECKED_CAST")
            val parentsRaw = data["parentCategories"] as? List<Map<String, Any>>
            if (parentsRaw != null) {
                for (p in parentsRaw) {
                    val cat = ParentCategory(
                        id = p["id"]?.toString() ?: continue,
                        name = p["name"]?.toString() ?: "",
                        color = p["color"]?.toString() ?: "",
                        sortOrder = (p["sortOrder"] as? Double)?.toInt() ?: 0
                    )
                    db.categoryDao().insertParentCategory(cat)
                    catCount++
                }
            }

            // 恢复子分类
            @Suppress("UNCHECKED_CAST")
            val subsRaw = data["subCategories"] as? List<Map<String, Any>>
            if (subsRaw != null) {
                for (s in subsRaw) {
                    val sub = SubCategory(
                        id = s["id"]?.toString() ?: continue,
                        parentId = s["parentId"]?.toString() ?: "",
                        name = s["name"]?.toString() ?: "",
                        color = s["color"]?.toString() ?: "",
                        sortOrder = (s["sortOrder"] as? Double)?.toInt() ?: 0
                    )
                    db.categoryDao().insertSubCategory(sub)
                    catCount++
                }
            }

            // 恢复笔记
            @Suppress("UNCHECKED_CAST")
            val notesRaw = data["notes"] as? List<Map<String, Any>>
            if (notesRaw != null) {
                for (n in notesRaw) {
                    val note = Note(
                        id = n["id"]?.toString() ?: UUID.randomUUID().toString(),
                        subCategoryId = n["subCategoryId"]?.toString() ?: "",
                        title = n["title"]?.toString() ?: "",
                        contentText = n["contentText"]?.toString() ?: "",
                        updateTime = n["updateTime"]?.toString() ?: "",
                        createTime = n["createTime"]?.toString() ?: n["updateTime"]?.toString() ?: "",
                        sortOrder = (n["sortOrder"] as? Double)?.toInt() ?: 0
                    )
                    db.noteDao().insertNote(note)
                    noteCount++
                }
            }

            // 恢复内容项
            @Suppress("UNCHECKED_CAST")
            val itemsRaw = data["contentItems"] as? List<Map<String, Any>>
            if (itemsRaw != null) {
                for (i in itemsRaw) {
                    val item = ContentItem(
                        id = 0,
                        noteId = i["noteId"]?.toString() ?: continue,
                        type = i["type"]?.toString() ?: "text",
                        content = i["content"]?.toString() ?: "",
                        timestamp = i["timestamp"]?.toString() ?: "",
                        transformX = (i["transformX"] as? Double)?.toFloat() ?: 0f,
                        transformY = (i["transformY"] as? Double)?.toFloat() ?: 0f,
                        transformScale = (i["transformScale"] as? Double)?.toFloat() ?: 1f,
                        transformRotation = (i["transformRotation"] as? Double)?.toFloat() ?: 0f,
                        voiceDuration = (i["voiceDuration"] as? Double)?.toLong() ?: 0L,
                        voiceTranscript = i["voiceTranscript"]?.toString() ?: ""
                    )
                    db.noteDao().insertContentItem(item)
                    itemCount++
                }
            }

            BackupResult.Success(noteCount, itemCount, catCount)
        } catch (e: Exception) {
            BackupResult.Error(e.message ?: "恢复失败")
        }
    }

    // ===== 数据库文件备份 =====

    /**
     * 创建数据库文件备份（.db + .db-wal + .db-shm 打包为 zip）
     */
    fun createDatabaseBackup(context: Context, targetUri: Uri): BackupResult {
        return try {
            val dbDir = context.getDatabasePath(DB_NAME).parentFile ?: return BackupResult.Error("找不到数据库目录")

            context.contentResolver.openOutputStream(targetUri)?.use { os ->
                ZipOutputStream(os).use { zip ->
                    for (suffix in listOf("", "-wal", "-shm")) {
                        val dbFile = File(dbDir, "$DB_NAME$suffix")
                        if (dbFile.exists()) {
                            zip.putNextEntry(ZipEntry("$DB_NAME$suffix"))
                            FileInputStream(dbFile).use { it.copyTo(zip) }
                            zip.closeEntry()
                        }
                    }
                }
            }
            BackupResult.Success(0, 0, 0)
        } catch (e: Exception) {
            BackupResult.Error(e.message ?: "数据库备份失败")
        }
    }

    /**
     * 从数据库文件备份恢复（替换当前数据库）
     */
    fun restoreDatabaseBackup(context: Context, sourceUri: Uri): BackupResult {
        return try {
            val dbDir = context.getDatabasePath(DB_NAME).parentFile ?: return BackupResult.Error("找不到数据库目录")

            // 解压到临时目录
            val tempDir = File(context.cacheDir, "db_restore_${System.currentTimeMillis()}")
            tempDir.mkdirs()

            context.contentResolver.openInputStream(sourceUri)?.use { is2 ->
                ZipInputStream(is2).use { zip ->
                    var entry = zip.nextEntry
                    while (entry != null) {
                        val outFile = File(tempDir, entry.name)
                        FileOutputStream(outFile).use { zip.copyTo(it) }
                        zip.closeEntry()
                        entry = zip.nextEntry
                    }
                }
            }

            // 关闭数据库连接
            val db = AppDatabase.get(context.applicationContext)
            db.close()

            // 等待数据库关闭
            Thread.sleep(300)

            // 替换数据库文件
            for (suffix in listOf("", "-wal", "-shm")) {
                val srcFile = File(tempDir, "$DB_NAME$suffix")
                if (srcFile.exists()) {
                    val dstFile = File(dbDir, "$DB_NAME$suffix")
                    dstFile.delete()
                    srcFile.copyTo(dstFile, overwrite = true)
                }
            }

            // 清理临时文件
            tempDir.deleteRecursively()

            BackupResult.Success(0, 0, 0)
        } catch (e: Exception) {
            BackupResult.Error(e.message ?: "数据库恢复失败")
        }
    }

    // ===== 备份文件管理 =====

    /**
     * 列出备份目录中的文件
     */
    fun listBackupFiles(context: Context): List<BackupFileInfo> {
        val dir = File(context.filesDir, BACKUP_DIR)
        if (!dir.exists()) return emptyList()

        return dir.listFiles()
            ?.filter { it.isFile && (it.name.endsWith(".json") || it.name.endsWith(".zip")) }
            ?.sortedByDescending { it.lastModified() }
            ?.map { file ->
                BackupFileInfo(
                    name = file.name,
                    path = file.absolutePath,
                    size = file.length(),
                    lastModified = file.lastModified(),
                    isDbBackup = file.name.endsWith(".zip")
                )
            }
            ?: emptyList()
    }

    /**
     * 删除指定备份文件
     */
    fun deleteBackupFile(context: Context, fileName: String): Boolean {
        val file = File(context.filesDir, "$BACKUP_DIR/$fileName")
        return file.exists() && file.delete()
    }

    /**
     * 将备份保存到内部备份目录
     */
    fun saveToInternal(context: Context, sourceUri: Uri, suffix: String): String? {
        return try {
            val dir = File(context.filesDir, BACKUP_DIR)
            if (!dir.exists()) dir.mkdirs()
            val destFile = File(dir, "backup_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.$suffix")
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(destFile).use { input.copyTo(it) }
            }
            destFile.name
        } catch (e: Exception) {
            null
        }
    }

    // ===== 辅助 =====

    private fun getAppVersion(context: Context): String {
        return try {
            val info = context.packageManager.getPackageInfo(context.packageName, 0)
            info.versionName ?: "unknown"
        } catch (e: Exception) { "unknown" }
    }

    // ===== 数据类 =====

    sealed class BackupResult {
        data class Success(val noteCount: Int, val itemCount: Int, val catCount: Int) : BackupResult()
        data class Error(val message: String) : BackupResult()
    }

    data class BackupFileInfo(
        val name: String,
        val path: String,
        val size: Long,
        val lastModified: Long,
        val isDbBackup: Boolean
    )
}
