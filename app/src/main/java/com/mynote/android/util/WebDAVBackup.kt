package com.mynote.android.util

import android.content.Context
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * WebDAV 云备份（坚果云 / 其他 WebDAV 服务）
 *
 * 配置方式：设置页输入 WebDAV 地址 + 用户名 + 应用密码
 * 坚果云：https://dav.jianguoyun.com/dav/
 * 需要在坚果云安全设置里生成第三方应用密码
 *
 * 备份文件存储在 WebDAV 目录下 /MyNote/backup_xxx.json
 */
object WebDAVBackup {

    private const val TAG = "WebDAVBackup"
    private const val BACKUP_DIR = "MyNote"
    private const val MEDIA_DIR = "media"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    // ═══════ 配置 ═══════

    fun isConfigured(context: Context): Boolean {
        val p = Prefs(context)
        return p.webdavUrl.isNotEmpty() && p.webdavUser.isNotEmpty() && p.webdavPass.isNotEmpty()
    }

    private fun getUrl(context: Context): String {
        val url = Prefs(context).webdavUrl.trimEnd('/')
        return url
    }

    private fun getAuthHeader(context: Context): String {
        val p = Prefs(context)
        val credentials = "${p.webdavUser}:${p.webdavPass}"
        return "Basic " + Base64.encodeToString(credentials.toByteArray(), Base64.NO_WRAP)
    }

    private fun getDeviceId(context: Context): String {
        val prefs = context.getSharedPreferences("mynote_sync", Context.MODE_PRIVATE)
        var id = prefs.getString("device_id", null)
        if (id == null) {
            id = "android_" + UUID.randomUUID().toString().take(8)
            prefs.edit().putString("device_id", id).apply()
        }
        return id
    }

    // ═══════ 上传备份 ═══════

    suspend fun uploadBackup(
        context: Context,
        excludeRestored: Boolean = false,
        callback: (Boolean, String) -> Unit = { _, _ -> }
    ) = withContext(Dispatchers.IO) {
        try {
            if (!isConfigured(context)) {
                callback(false, "请先在设置中配置 WebDAV")
                return@withContext
            }

            // 自动备份（保存时/定时）时跳过「追加恢复」来的笔记，避免把恢复内容重新上传到云端；
            // 手动「立即备份」不排除，备份全部。
            val exclude = if (excludeRestored) Prefs(context).restoredNoteIds else emptySet()
            val json = BackupManager.exportJsonString(context, exclude)
            if (json.isEmpty()) { callback(false, "备份数据为空"); return@withContext }

            val deviceId = getDeviceId(context)
            val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val filename = "backup_${deviceId}_${ts}.json"
            val fullUrl = "${getUrl(context)}/$BACKUP_DIR/$filename"

            // 先创建目录（MKCOL，如果不存在）
            ensureDir(context)

            val request = Request.Builder()
                .url(fullUrl)
                .header("Authorization", getAuthHeader(context))
                .put(json.toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val code = response.code
            response.close()

            if (code in 200..299 || code == 201 || code == 204) {
                Log.i(TAG, "WebDAV 上传成功: $filename")
                // 记录最近一次成功备份时间（立即备份/保存时备份/定时备份统一在此更新）
                Prefs(context).lastBackupTime = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
                // 手动「立即备份」成功后清除恢复标记：用户已接受当前完整状态，恢复来的笔记恢复正常参与备份
                if (!excludeRestored) Prefs(context).clearRestoredNoteIds()
                // 顺带备份媒体文件（图片/视频/附件，跳过 voice_ 录音）
                val media = uploadMediaFiles(context)
                val mediaCount = media.getOrNull() ?: 0
                val mediaMsg = if (mediaCount > 0) "，媒体文件 $mediaCount 个" else ""
                callback(true, "已备份到坚果云$mediaMsg")
            } else {
                Log.e(TAG, "上传失败: HTTP $code")
                callback(false, "上传失败 (HTTP $code)")
            }
        } catch (e: Exception) {
            Log.e(TAG, "备份异常", e)
            callback(false, "网络错误: ${e.message}")
        }
    }

    private suspend fun ensureDir(context: Context) {
        try {
            val dirUrl = "${getUrl(context)}/$BACKUP_DIR"
            // 先 PROPFIND 检查目录是否存在
            val probeReq = Request.Builder()
                .url(dirUrl)
                .header("Authorization", getAuthHeader(context))
                .method("PROPFIND", "".toRequestBody("application/xml".toMediaType()))
                .build()
            val probeResp = client.newCall(probeReq).execute()
            if (probeResp.code in 200..299) { probeResp.close(); return }
            probeResp.close()

            // 不存在则创建
            val mkcolReq = Request.Builder()
                .url(dirUrl)
                .header("Authorization", getAuthHeader(context))
                .method("MKCOL", "".toRequestBody(null))
                .build()
            val mkcolResp = client.newCall(mkcolReq).execute()
            mkcolResp.close()
        } catch (_: Exception) {}
    }

    // ═══════ 列出备份文件 ═══════

    data class BackupFile(val path: String, val name: String, val size: Long, val time: String)

    /** 列出备份文件的结果：files 为空但 error 非空表示出错（而非"没有备份文件"） */
    data class ListResult(val files: List<BackupFile>, val error: String?)

    suspend fun listBackupFiles(context: Context): ListResult = withContext(Dispatchers.IO) {
        val result = mutableListOf<BackupFile>()
        try {
            if (!isConfigured(context)) {
                return@withContext ListResult(result, "未配置坚果云 WebDAV，请先在设置中配置")
            }

            val dirUrl = "${getUrl(context)}/$BACKUP_DIR"
            val body = """
                <?xml version="1.0" encoding="utf-8"?>
                <D:propfind xmlns:D="DAV:">
                  <D:prop>
                    <D:displayname/>
                    <D:getcontentlength/>
                    <D:getlastmodified/>
                  </D:prop>
                </D:propfind>
            """.trimIndent()

            val request = Request.Builder()
                .url(dirUrl)
                .header("Authorization", getAuthHeader(context))
                .header("Depth", "1")
                .method("PROPFIND", body.toRequestBody("application/xml".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            if (response.code !in 200..299) {
                val msg = when (response.code) {
                    401 -> "坚果云登录失败，请检查账号和应用密码"
                    403 -> "坚果云拒绝访问（若开启了微信二次验证请先关闭，或检查密码）"
                    404 -> "坚果云上找不到 /MyNote/ 目录（可先手动备份一次）"
                    503 -> "坚果云请求过于频繁，请稍后再试"
                    else -> "坚果云返回错误 (HTTP ${response.code})"
                }
                Log.e(TAG, "列文件失败: HTTP ${response.code}")
                response.close()
                return@withContext ListResult(result, msg)
            }
            val xml = response.body?.string() ?: ""
            response.close()

            // 简单解析 PROPFIND XML（坚果云/ownCloud 内核命名空间前缀为小写 d:，此处兼容大小写与无前缀）
            val responseRe = Regex("""<(?:\w+:)?response[^>]*>(.*?)</(?:\w+:)?response>""", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
            val hrefRe = Regex("""<(?:\w+:)?href[^>]*>(.*?)</(?:\w+:)?href>""", RegexOption.IGNORE_CASE)
            val nameRe = Regex("""<(?:\w+:)?displayname[^>]*>(.*?)</(?:\w+:)?displayname>""", RegexOption.IGNORE_CASE)
            val sizeRe = Regex("""<(?:\w+:)?getcontentlength[^>]*>(.*?)</(?:\w+:)?getcontentlength>""", RegexOption.IGNORE_CASE)
            val timeRe = Regex("""<(?:\w+:)?getlastmodified[^>]*>(.*?)</(?:\w+:)?getlastmodified>""", RegexOption.IGNORE_CASE)

            for (match in responseRe.findAll(xml)) {
                val seg = match.value
                val hrefRaw = hrefRe.find(seg)?.groupValues?.get(1) ?: continue
                val href = try { java.net.URLDecoder.decode(hrefRaw.trim(), "UTF-8") } catch (_: Exception) { hrefRaw.trim() }
                val name = nameRe.find(seg)?.groupValues?.get(1)?.trim() ?: href.split("/").last()
                if (name.isEmpty() || name == BACKUP_DIR || !name.endsWith(".json")) continue
                val size = sizeRe.find(seg)?.groupValues?.get(1)?.toLongOrNull() ?: 0L
                val timeRaw = timeRe.find(seg)?.groupValues?.get(1)?.trim() ?: ""
                val time = try {
                    val parser = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US)
                    SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(parser.parse(timeRaw)!!)
                } catch (_: Exception) { timeRaw }

                result.add(BackupFile(path = href, name = name, size = size, time = time))
            }
            result.sortByDescending { it.time }
            ListResult(result, null)
        } catch (e: Exception) {
            Log.e(TAG, "列文件异常", e)
            ListResult(result, "网络错误: ${e.message}")
        }
    }

    // ═══════ 下载备份 ═══════

    /**
     * 把 PROPFIND 返回的 href 解析成可下载的完整 URL。
     *
     * 坚果云（ownCloud/Nextcloud 内核）返回的 href 是「绝对路径」
     * /dav/MyNote/backup_xxx.json（不含 host）。若直接用 getUrl（含 /dav）
     * 拼接会得到 .../dav/dav/... 双重 /dav，导致下载 404。
     * 这里提取 /MyNote/ 之后的相对部分，再用 getUrl 拼回完整 URL。
     * （与 listRemoteMedia 里 substringAfter("/MyNote/") 的处理保持一致）
     */
    private fun resolveHref(context: Context, href: String): String {
        if (href.startsWith("http://") || href.startsWith("https://")) return href
        val rel = href.substringAfter("/$BACKUP_DIR/", href.trimStart('/'))
        return "${getUrl(context)}/$BACKUP_DIR/${rel.trimStart('/')}"
    }

    suspend fun downloadBackup(context: Context, remotePath: String): String? = withContext(Dispatchers.IO) {
        try {
            if (!isConfigured(context)) return@withContext null
            val url = resolveHref(context, remotePath)

            val request = Request.Builder()
                .url(url)
                .header("Authorization", getAuthHeader(context))
                .get()
                .build()

            val response = client.newCall(request).execute()
            if (response.code !in 200..299) {
                Log.e(TAG, "下载失败: HTTP ${response.code}")
                response.close(); return@withContext null
            }
            val content = response.body?.string()
            response.close()
            content
        } catch (e: Exception) { Log.e(TAG, "下载异常", e); null }
    }

    // ═══════ 媒体文件备份（图片/视频/PDF/Office 附件；跳过 voice_ 录音） ═══════

    /** 收集本地需备份的媒体文件（跳过 voice_*.m4a 录音） */
    private fun collectLocalMedia(context: Context): List<Pair<String, File>> {
        val notesDir = File(context.filesDir, "notes")
        val result = mutableListOf<Pair<String, File>>()
        if (!notesDir.isDirectory) return result
        for (noteDir in notesDir.listFiles().orEmpty()) {
            if (!noteDir.isDirectory) continue
            for (f in noteDir.listFiles().orEmpty()) {
                if (!f.isFile) continue
                // 录音暂不备份
                if (f.name.startsWith("voice_") && f.name.endsWith(".m4a")) continue
                result.add(noteDir.name to f)
            }
        }
        return result
    }

    /** 列出云端 /MyNote/media/ 下所有文件，返回 remotePath(media/noteId/name) -> size。
     *  坚果云不支持 Depth: infinity（会失败/触发限额），故改为逐层 Depth:1 手动递归。 */
    private suspend fun listRemoteMedia(context: Context): Map<String, Long> {
        val map = mutableMapOf<String, Long>()
        try {
            val mediaDirUrl = "${getUrl(context)}/$BACKUP_DIR/$MEDIA_DIR"
            // 第一层：media/ 下的 noteId 子目录
            val noteDirs = propfindChildren(context, mediaDirUrl).filter { it.size == null }
            for (noteDir in noteDirs) {
                val noteDirUrl = "$mediaDirUrl/${noteDir.name}"
                // 第二层：noteId 目录下的文件
                val files = propfindChildren(context, noteDirUrl).filter { it.size != null }
                for (f in files) {
                    map["$MEDIA_DIR/${noteDir.name}/${f.name}"] = f.size!!
                }
            }
        } catch (e: Exception) { Log.e(TAG, "列媒体异常", e) }
        return map
    }

    /** PROPFIND Depth:1 列出 dirUrl 的下一层子项；size==null 表示目录，非空表示文件 */
    private data class ChildEntry(val name: String, val size: Long?)

    private suspend fun propfindChildren(context: Context, dirUrl: String): List<ChildEntry> {
        val items = mutableListOf<ChildEntry>()
        try {
            val body = """<?xml version="1.0" encoding="utf-8"?>
                <D:propfind xmlns:D="DAV:"><D:prop><D:getcontentlength/></D:prop></D:propfind>""".trimIndent()
            val request = Request.Builder()
                .url(dirUrl)
                .header("Authorization", getAuthHeader(context))
                .header("Depth", "1")
                .method("PROPFIND", body.toRequestBody("application/xml".toMediaType()))
                .build()
            val response = client.newCall(request).execute()
            if (response.code !in 200..299) { response.close(); return items }
            val xml = response.body?.string() ?: ""; response.close()

            val responseRe = Regex("""<(?:\w+:)?response[^>]*>(.*?)</(?:\w+:)?response>""", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
            val hrefRe = Regex("""<(?:\w+:)?href[^>]*>(.*?)</(?:\w+:)?href>""", RegexOption.IGNORE_CASE)
            val sizeRe = Regex("""<(?:\w+:)?getcontentlength[^>]*>(.*?)</(?:\w+:)?getcontentlength>""", RegexOption.IGNORE_CASE)

            // 目录自身的相对路径（/MyNote/ 之后），用于跳过目录自身
            val selfRel = dirUrl.substringAfter("/$BACKUP_DIR/", "").trimEnd('/')

            for (match in responseRe.findAll(xml)) {
                val seg = match.value
                val hrefRaw = hrefRe.find(seg)?.groupValues?.get(1)?.trim() ?: continue
                val href = try { java.net.URLDecoder.decode(hrefRaw, "UTF-8") } catch (_: Exception) { hrefRaw }
                val rel = href.substringAfter("/$BACKUP_DIR/", "").trimEnd('/')
                if (rel == selfRel || rel.isEmpty()) continue  // 跳过目录自身
                val name = rel.substringAfterLast('/')
                if (name.isEmpty()) continue
                val size = sizeRe.find(seg)?.groupValues?.get(1)?.toLongOrNull()
                items.add(ChildEntry(name, size))
            }
        } catch (e: Exception) { Log.e(TAG, "PROPFIND 异常", e) }
        return items
    }

    /** 确保 WebDAV 子目录存在（relPath 相对 /MyNote/，如 "media" 或 "media/<noteId>"） */
    private suspend fun ensureDirPath(context: Context, relPath: String) {
        try {
            val dirUrl = "${getUrl(context)}/$BACKUP_DIR/$relPath"
            val probeReq = Request.Builder()
                .url(dirUrl)
                .header("Authorization", getAuthHeader(context))
                .method("PROPFIND", "".toRequestBody("application/xml".toMediaType()))
                .build()
            val probeResp = client.newCall(probeReq).execute()
            if (probeResp.code in 200..299) { probeResp.close(); return }
            probeResp.close()
            val mkcolReq = Request.Builder()
                .url(dirUrl)
                .header("Authorization", getAuthHeader(context))
                .method("MKCOL", "".toRequestBody(null))
                .build()
            client.newCall(mkcolReq).execute().close()
        } catch (_: Exception) {}
    }

    /**
     * 上传媒体文件（图片/视频/附件，跳过录音）。增量：云端已存在同大小文件则跳过。
     * 返回实际上传的文件数。
     */
    suspend fun uploadMediaFiles(
        context: Context,
        onProgress: (done: Int, total: Int) -> Unit = { _, _ -> }
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            if (!isConfigured(context)) return@withContext Result.failure(Exception("未配置 WebDAV"))
            val files = collectLocalMedia(context)
            if (files.isEmpty()) return@withContext Result.success(0)
            val remote = listRemoteMedia(context)
            ensureDir(context)
            ensureDirPath(context, MEDIA_DIR)
            var uploaded = 0
            var i = 0
            for ((noteId, f) in files) {
                i++
                onProgress(i, files.size)
                val remotePath = "$MEDIA_DIR/$noteId/${f.name}"
                if (remote[remotePath] == f.length()) continue  // 已存在同大小，跳过
                ensureDirPath(context, "$MEDIA_DIR/$noteId")
                val request = Request.Builder()
                    .url("${getUrl(context)}/$BACKUP_DIR/$remotePath")
                    .header("Authorization", getAuthHeader(context))
                    .put(f.asRequestBody("application/octet-stream".toMediaType()))
                    .build()
                val resp = client.newCall(request).execute()
                val code = resp.code; resp.close()
                if (code in 200..299 || code == 201 || code == 204) uploaded++
                else Log.e(TAG, "媒体上传失败 HTTP $code: $remotePath")
            }
            Result.success(uploaded)
        } catch (e: Exception) {
            Log.e(TAG, "媒体上传异常", e)
            Result.failure(e)
        }
    }

    /**
     * 下载云端媒体文件到本地 files/notes/<noteId>/（恢复用）。跳过本地已存在同大小的。
     * 返回实际下载的文件数。
     */
    suspend fun downloadMediaFiles(
        context: Context,
        noteIdMap: Map<String, String> = emptyMap(),
        onProgress: (done: Int, total: Int) -> Unit = { _, _ -> }
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            if (!isConfigured(context)) return@withContext Result.failure(Exception("未配置 WebDAV"))
            val remote = listRemoteMedia(context)
            if (remote.isEmpty()) return@withContext Result.success(0)
            var downloaded = 0
            var i = 0
            for ((remotePath, size) in remote) {
                i++
                onProgress(i, remote.size)
                // remotePath = "media/<noteId>/<fileName>"
                val parts = remotePath.split("/")
                if (parts.size < 3) continue
                val oldNoteId = parts[1]
                // 追加/增量恢复时笔记 id 被重新映射，媒体需落到新 noteId 目录（否则图片对不上新笔记）
                // 增量模式下被跳过的笔记不在 noteIdMap，其媒体也不应下载（否则落到旧 id 目录成孤儿）
                if (noteIdMap.isNotEmpty() && oldNoteId !in noteIdMap) continue
                val noteId = noteIdMap[oldNoteId] ?: oldNoteId
                val fileName = parts.drop(2).joinToString("/")
                val localDir = File(context.filesDir, "notes/$noteId")
                localDir.mkdirs()
                val localFile = File(localDir, fileName)
                if (localFile.exists() && localFile.length() == size) continue
                val request = Request.Builder()
                    .url("${getUrl(context)}/$BACKUP_DIR/$remotePath")
                    .header("Authorization", getAuthHeader(context))
                    .get()
                    .build()
                val resp = client.newCall(request).execute()
                if (resp.code !in 200..299) { resp.close(); continue }
                val body = resp.body
                if (body != null) {
                    val tmp = File(localDir, "$fileName.tmp")
                    tmp.outputStream().use { body.byteStream().copyTo(it) }
                    if (tmp.length() > 0) {
                        if (localFile.exists()) localFile.delete()
                        tmp.renameTo(localFile)
                        downloaded++
                    } else tmp.delete()
                }
                resp.close()
            }
            Result.success(downloaded)
        } catch (e: Exception) {
            Log.e(TAG, "媒体下载异常", e)
            Result.failure(e)
        }
    }
}
