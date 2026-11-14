package com.mynote.android.util

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.gson.Gson
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

    // ===== 云端恢复模式 =====
    const val MODE_APPEND = 0      // 追加：生成「（备）」子主题卡片，不覆盖现有内容
    const val MODE_OVERWRITE = 1   // 覆盖：清空后按原 id 精确还原
    const val MODE_MERGE = 2       // 增量：只补本地没有的分类/笔记，已有的跳过

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
    suspend fun exportJsonString(context: Context, excludeNoteIds: Set<String> = emptySet()): String {
        val db = AppDatabase.get(context.applicationContext)
        val parents = db.categoryDao().getParentCategories()
        val subs = mutableListOf<SubCategory>()
        val notes = mutableListOf<Note>()
        val items = mutableListOf<ContentItem>()
        for (p in parents) {
            val sl = db.categoryDao().getSubCategories(p.id)
            subs.addAll(sl)
            for (s in sl) {
                val nl = db.noteDao().getNotesBySubCategory(s.id).filter { it.id !in excludeNoteIds }
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

    /** 恢复结果：success 是否成功；noteIdMap 为旧 noteId → 新 noteId 的映射（供媒体文件恢复跟随重映射） */
    data class ImportResult(val success: Boolean, val noteIdMap: Map<String, String> = emptyMap())

    /**
     * 从 JSON 字符串恢复数据（用于云端下载后恢复）。
     *
     * 手动逐字段构造实体，不用 Gson 反序列化 Kotlin data class——
     * 因为 JSON 经 Map<String,Any> 中间态后整数会被解析成 Double，
     * 直接反序列化回 Int/Long/Float 字段存在类型不匹配风险（与本地恢复
     * restoreJsonBackup 保持一致的做法）。
     *
     * @param mode 恢复模式：
     *             [MODE_APPEND] 追加：大主题按名称复用，子主题「总是新建」并加「（备）」后缀，
     *             笔记重新生成 id 落到「（备）」子主题下，绝不覆盖本地已有内容；
     *             [MODE_OVERWRITE] 覆盖：清空后按原 id 精确还原；
     *             [MODE_MERGE] 增量：只补本地没有的——大主题/子主题按名称复用（无则新建，
     *             不加「（备）」），笔记按「同子主题下同标题」判断，已有的跳过、缺失的才恢复。
     */
    suspend fun importJsonString(context: Context, json: String, mode: Int = MODE_APPEND): ImportResult {
        return try {
            val type = object : TypeToken<Map<String, Any>>() {}.type
            val backup: Map<String, Any> = gson.fromJson(json, type)
            @Suppress("UNCHECKED_CAST")
            val data = backup["data"] as? Map<String, Any> ?: (backup as? Map<String, Any>) ?: return ImportResult(false)

            val db = AppDatabase.get(context.applicationContext)

            val overwrite = mode == MODE_OVERWRITE
            val merge = mode == MODE_MERGE

            if (overwrite) {
                val parents = db.categoryDao().getParentCategories()
                for (p in parents) {
                    val subs = db.categoryDao().getSubCategories(p.id)
                    for (s in subs) {
                        val notes = db.noteDao().getNotesBySubCategory(s.id)
                        for (n in notes) db.noteDao().deleteNote(n)
                    }
                }
                for (p in parents) db.categoryDao().deleteParentCategory(p)
                // 清空本地媒体目录：覆盖恢复后旧笔记的图片/视频/录音不应残留，
                // 否则下次备份时 collectLocalMedia 会把孤儿文件误当成当前数据重新上传。
                val notesDir = File(context.filesDir, "notes")
                if (notesDir.isDirectory) notesDir.listFiles()?.forEach { it.deleteRecursively() }
            }

            // 追加/增量：不清空，id 重新映射；覆盖：本地已清空，保持原 id 精确还原。
            val remap = !overwrite
            val parentIdMap = mutableMapOf<String, String>()
            val subIdMap = mutableMapOf<String, String>()
            val noteIdMap = mutableMapOf<String, String>()

            // 追加/增量模式下预取现有大主题，供按名称复用
            val existingParents: List<ParentCategory> =
                if (remap) db.categoryDao().getParentCategories() else emptyList()
            val existingSubs: List<SubCategory> =
                if (merge) db.categoryDao().getAllSubCategories() else emptyList()

            // 父分类
            @Suppress("UNCHECKED_CAST")
            (data["parentCategories"] as? List<Map<String, Any>>)?.forEach { p ->
                val oldId = p["id"]?.toString() ?: return@forEach
                val name = p["name"]?.toString() ?: ""
                val newId: String
                if (remap) {
                    val exist = existingParents.firstOrNull { name.isNotEmpty() && it.name == name }
                    newId = if (exist != null) {
                        exist.id // 复用现有大主题，不重复建卡片
                    } else {
                        val id = UUID.randomUUID().toString()
                        db.categoryDao().insertParentCategory(ParentCategory(
                            id = id,
                            name = name,
                            color = p["color"]?.toString() ?: "",
                            sortOrder = (p["sortOrder"] as? Number)?.toInt() ?: 0
                        ))
                        id
                    }
                } else {
                    newId = oldId
                    db.categoryDao().insertParentCategory(ParentCategory(
                        id = oldId,
                        name = name,
                        color = p["color"]?.toString() ?: "",
                        sortOrder = (p["sortOrder"] as? Number)?.toInt() ?: 0
                    ))
                }
                parentIdMap[oldId] = newId
            }
            // 子分类
            @Suppress("UNCHECKED_CAST")
            (data["subCategories"] as? List<Map<String, Any>>)?.forEach { s ->
                val oldId = s["id"]?.toString() ?: return@forEach
                val name = s["name"]?.toString() ?: ""
                val rawParentId = s["parentId"]?.toString() ?: ""
                val newParentId = if (remap) (parentIdMap[rawParentId] ?: rawParentId) else rawParentId
                val newId: String
                if (overwrite) {
                    newId = oldId
                    db.categoryDao().insertSubCategory(SubCategory(
                        id = oldId,
                        parentId = newParentId,
                        name = name,
                        color = s["color"]?.toString() ?: "",
                        sortOrder = (s["sortOrder"] as? Number)?.toInt() ?: 0
                    ))
                } else if (merge) {
                    // 增量：按（parentId + 名称）复用，无同名才新建（不加「（备）」）
                    val exist = existingSubs.firstOrNull { it.parentId == newParentId && name.isNotEmpty() && it.name == name }
                    newId = if (exist != null) {
                        exist.id
                    } else {
                        val id = UUID.randomUUID().toString()
                        db.categoryDao().insertSubCategory(SubCategory(
                            id = id,
                            parentId = newParentId,
                            name = name,
                            color = s["color"]?.toString() ?: "",
                            sortOrder = (s["sortOrder"] as? Number)?.toInt() ?: 0
                        ))
                        id
                    }
                } else {
                    // 追加：总是新建一份「（备）」子主题，不并入现有子主题
                    val id = UUID.randomUUID().toString()
                    db.categoryDao().insertSubCategory(SubCategory(
                        id = id,
                        parentId = newParentId,
                        name = "$name（备）",
                        color = s["color"]?.toString() ?: "",
                        sortOrder = (s["sortOrder"] as? Number)?.toInt() ?: 0
                    ))
                    newId = id
                }
                subIdMap[oldId] = newId
            }
            // 笔记（补全 tags/isTodo/isPinned/isTrashed/trashedAt）
            @Suppress("UNCHECKED_CAST")
            (data["notes"] as? List<Map<String, Any>>)?.forEach { n ->
                val oldId = n["id"]?.toString() ?: UUID.randomUUID().toString()
                val rawSubId = n["subCategoryId"]?.toString() ?: ""
                val newSubId = if (remap) (subIdMap[rawSubId] ?: rawSubId) else rawSubId
                val bkTitle = n["title"]?.toString() ?: ""
                val newId: String
                if (merge) {
                    // 增量：同子主题下已有同标题笔记则跳过，缺失的才恢复
                    val exist = db.noteDao().getNotesBySubCategory(newSubId)
                        .firstOrNull { bkTitle.isNotEmpty() && it.title == bkTitle }
                    if (exist != null) return@forEach
                    newId = UUID.randomUUID().toString()
                } else {
                    newId = if (remap) UUID.randomUUID().toString() else oldId
                }
                noteIdMap[oldId] = newId
                db.noteDao().insertNote(Note(
                    id = newId,
                    subCategoryId = newSubId,
                    title = bkTitle,
                    contentText = n["contentText"]?.toString() ?: "",
                    updateTime = n["updateTime"]?.toString() ?: "",
                    createTime = n["createTime"]?.toString() ?: n["updateTime"]?.toString() ?: "",
                    sortOrder = (n["sortOrder"] as? Number)?.toInt() ?: 0,
                    tags = n["tags"]?.toString() ?: "",
                    isTodo = n["isTodo"] as? Boolean ?: false,
                    isPinned = n["isPinned"] as? Boolean ?: false,
                    isTrashed = n["isTrashed"] as? Boolean ?: false,
                    trashedAt = (n["trashedAt"] as? Number)?.toLong() ?: 0L
                ))
            }
            // 内容项（补全 voiceTimestamps）
            @Suppress("UNCHECKED_CAST")
            (data["contentItems"] as? List<Map<String, Any>>)?.forEach { ci ->
                val rawNoteId = ci["noteId"]?.toString() ?: return@forEach
                // 增量模式下被跳过的笔记不在 noteIdMap，其内容项也一并跳过（否则落到旧 id 成孤儿）
                if (remap && rawNoteId !in noteIdMap) return@forEach
                val newNoteId = if (remap) (noteIdMap[rawNoteId] ?: rawNoteId) else rawNoteId
                db.noteDao().insertContentItem(ContentItem(
                    id = 0,
                    noteId = newNoteId,
                    type = ci["type"]?.toString() ?: "text",
                    content = ci["content"]?.toString() ?: "",
                    timestamp = ci["timestamp"]?.toString() ?: "",
                    transformX = (ci["transformX"] as? Number)?.toFloat() ?: 0f,
                    transformY = (ci["transformY"] as? Number)?.toFloat() ?: 0f,
                    transformScale = (ci["transformScale"] as? Number)?.toFloat() ?: 1f,
                    transformRotation = (ci["transformRotation"] as? Number)?.toFloat() ?: 0f,
                    voiceDuration = (ci["voiceDuration"] as? Number)?.toLong() ?: 0L,
                    voiceTimestamps = ci["voiceTimestamps"]?.toString() ?: "",
                    voiceTranscript = ci["voiceTranscript"]?.toString() ?: ""
                ))
            }
            ImportResult(true, noteIdMap)
        } catch (e: Exception) {
            Log.e("BackupManager", "恢复失败", e)
            ImportResult(false)
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
                        sortOrder = (n["sortOrder"] as? Number)?.toInt() ?: 0,
                        tags = n["tags"]?.toString() ?: "",
                        isTodo = n["isTodo"] as? Boolean ?: false,
                        isPinned = n["isPinned"] as? Boolean ?: false,
                        isTrashed = n["isTrashed"] as? Boolean ?: false,
                        trashedAt = (n["trashedAt"] as? Number)?.toLong() ?: 0L
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
                        transformX = (i["transformX"] as? Number)?.toFloat() ?: 0f,
                        transformY = (i["transformY"] as? Number)?.toFloat() ?: 0f,
                        transformScale = (i["transformScale"] as? Number)?.toFloat() ?: 1f,
                        transformRotation = (i["transformRotation"] as? Number)?.toFloat() ?: 0f,
                        voiceDuration = (i["voiceDuration"] as? Number)?.toLong() ?: 0L,
                        voiceTimestamps = i["voiceTimestamps"]?.toString() ?: "",
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

            // 备份前 checkpoint，把 WAL 增量刷回主库，避免直接复制活库得到不一致的 .db
            try {
                AppDatabase.get(context.applicationContext)
                    .openHelper.writableDatabase
                    .query("PRAGMA wal_checkpoint(TRUNCATE)").use { it.moveToFirst() }
            } catch (_: Exception) {}

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

            // 替换数据库文件：先删除现有 .db/-wal/-shm（避免 zip 里没有 -wal/-shm 时残留旧 WAL 导致数据不一致），
            // 再复制 zip 里存在的文件。
            for (suffix in listOf("", "-wal", "-shm")) {
                val dstFile = File(dbDir, "$DB_NAME$suffix")
                if (dstFile.exists()) dstFile.delete()
            }
            for (suffix in listOf("", "-wal", "-shm")) {
                val srcFile = File(tempDir, "$DB_NAME$suffix")
                if (srcFile.exists()) {
                    srcFile.copyTo(File(dbDir, "$DB_NAME$suffix"), overwrite = true)
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
