package com.mynote.android.util

import android.content.Context
import android.util.Base64
import com.mynote.android.data.AppDatabase
import com.mynote.android.data.dao.NoteDao
import java.io.File

/**
 * 修复「给笔记添加照片后，重新打开该笔记闪退」的问题。
 *
 * 根因：旧版 insertImage 把照片 base64 内嵌进 HTML，保存时整段 innerHTML 存成
 * content_items 的一行（type=html）。手机照片 base64 后单行轻松超过 SQLite
 * CursorWindow 的 2MB 上限，于是 getContentItems 查询时抛
 * SQLiteBlobTooBigException。
 *
 * 本迁移：定位超限行 → 分段读出 content（绕开 CursorWindow）→ 把内嵌 base64
 * 图片替换为本地文件名引用（图片文件本就落在 filesDir/notes/<noteId>/ 下，
 * loadDataWithBaseURL 以该目录为 base，可直接加载）→ 写回瘦身后的 content。
 *
 * 注意：不能直接用 getContentItems 读超限行（SELECT * 在查询阶段就会因
 * CursorWindow 装不下而抛异常），所以这里用字节长度投影 + substr 分段读取。
 */
object ContentSizeMigrator {

    /** 干预阈值：1MB 字节，提前于 CursorWindow 2MB 上限触发 */
    private const val THRESHOLD_BYTES = 1024 * 1024

    /** 分段读取的单段字符数（远小于 2MB） */
    private const val CHUNK = 256 * 1024

    /**
     * 匹配一段「内嵌 base64 图片」的媒体块：
     *   <div ... data-type="image" data-path="X" ...><img ... src="data:image/...;base64,YYY".../>
     * 分组：1=前缀 2=data-path 值 3=base64 内容 4=尾巴
     */
    private val BASE64_IMG = Regex(
        """(<div[^>]*data-type=["']image["'][^>]*data-path=["']([^"']+)["'][^>]*>\s*<img[^>]*src=["'])data:image/[A-Za-z0-9.+-]+;base64,([A-Za-z0-9+/=\s]+)("["']?[^>]*>)""",
        RegexOption.IGNORE_CASE
    )

    suspend fun migrateIfNeeded(context: Context, db: AppDatabase) {
        val dao = db.noteDao()
        val oversized = try {
            dao.findOversizedHtmlItems(THRESHOLD_BYTES)
        } catch (_: Exception) { return }
        if (oversized.isEmpty()) return

        for (item in oversized) {
            try {
                val content = readFullContent(dao, item.id) ?: continue
                val cleaned = stripBase64Images(context, item.noteId, content)
                if (cleaned != content) {
                    dao.updateContentItemContent(item.id, cleaned)
                }
            } catch (_: Exception) {}
        }
    }

    /** 分段读出完整 content，绕开 CursorWindow（单行 > 2MB 时 SELECT * 会崩） */
    private suspend fun readFullContent(dao: NoteDao, id: Long): String? {
        val sb = StringBuilder()
        var start = 1
        while (true) {
            val part = dao.getContentChunk(id, start, CHUNK) ?: break
            if (part.isEmpty()) break
            sb.append(part)
            if (part.length < CHUNK) break
            start += CHUNK
        }
        return if (sb.isEmpty()) null else sb.toString()
    }

    /** 把内嵌 base64 图片替换为本地文件名引用 */
    private fun stripBase64Images(context: Context, noteId: String, html: String): String {
        val noteDir = File(context.filesDir, "notes/$noteId")
        var counter = 0
        return BASE64_IMG.replace(html) { m ->
            val prefix = m.groupValues[1]
            val dataPath = m.groupValues[2]
            val b64 = m.groupValues[3]
            val tail = m.groupValues[4]
            val fileName = resolveFileName(noteDir, dataPath, b64, counter++)
            if (fileName == null) m.value else prefix + fileName + tail
        }
    }

    /**
     * 优先复用 data-path 指向的原图文件（图片本就复制在 notes/<noteId>/ 下）；
     * 若文件已丢失，则把 base64 解码落盘恢复。
     */
    private fun resolveFileName(noteDir: File, dataPath: String, b64: String, idx: Int): String? {
        val existing = File(dataPath)
        if (existing.exists() && existing.length() > 0) return existing.name

        val decoded = try {
            Base64.decode(b64.filterNot { it.isWhitespace() }, Base64.DEFAULT)
        } catch (_: Exception) { null }
        if (decoded == null || decoded.isEmpty()) return null

        return try {
            noteDir.mkdirs()
            val f = File(noteDir, "img_restore_${System.currentTimeMillis()}_$idx.jpg")
            f.writeBytes(decoded)
            f.name
        } catch (_: Exception) { null }
    }
}
