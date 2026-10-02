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
        callback: (Boolean, String) -> Unit = { _, _ -> }
    ) = withContext(Dispatchers.IO) {
        try {
            if (!isConfigured(context)) {
                callback(false, "请先在设置中配置 WebDAV")
                return@withContext
            }

            val json = BackupManager.exportJsonString(context)
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

    suspend fun listBackupFiles(context: Context): List<BackupFile> = withContext(Dispatchers.IO) {
        val result = mutableListOf<BackupFile>()
        try {
            if (!isConfigured(context)) return@withContext result

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
                Log.e(TAG, "列文件失败: HTTP ${response.code}")
                response.close(); return@withContext result
            }
            val xml = response.body?.string() ?: ""
            response.close()

            // 简单解析 PROPFIND XML
            val responseRe = Regex("""<D:response>(.*?)</D:response>""", RegexOption.DOT_MATCHES_ALL)
            val hrefRe = Regex("""<D:href>(.*?)</D:href>""")
            val nameRe = Regex("""<D:displayname>(.*?)</D:displayname>""")
            val sizeRe = Regex("""<D:getcontentlength>(.*?)</D:getcontentlength>""")
            val timeRe = Regex("""<D:getlastmodified>(.*?)</D:getlastmodified>""")

            for (match in responseRe.findAll(xml)) {
                val seg = match.value
                val href = hrefRe.find(seg)?.groupValues?.get(1) ?: continue
                val name = nameRe.find(seg)?.groupValues?.get(1) ?: href.split("/").last()
                if (name.isEmpty() || name == BACKUP_DIR || !name.endsWith(".json")) continue
                val size = sizeRe.find(seg)?.groupValues?.get(1)?.toLongOrNull() ?: 0L
                val timeRaw = timeRe.find(seg)?.groupValues?.get(1) ?: ""
                val time = try {
                    val parser = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US)
                    SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(parser.parse(timeRaw)!!)
                } catch (_: Exception) { timeRaw }

                result.add(BackupFile(path = href, name = name, size = size, time = time))
            }
            result.sortByDescending { it.time }
        } catch (e: Exception) { Log.e(TAG, "列文件异常", e) }
        result
    }

    // ═══════ 下载备份 ═══════

    suspend fun downloadBackup(context: Context, remotePath: String): String? = withContext(Dispatchers.IO) {
        try {
            if (!isConfigured(context)) return@withContext null
            val url = if (remotePath.startsWith("http")) remotePath
            else "${getUrl(context)}/${remotePath.trimStart('/')}"

            val request = Request.Builder()
                .url(url)
                .header("Authorization", getAuthHeader(context))
                .get()
                .build()

            val response = client.newCall(request).execute()
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

    /** 递归列出云端 /MyNote/media/ 下所有文件，返回 remotePath(media/noteId/name) -> size */
    private suspend fun listRemoteMedia(context: Context): Map<String, Long> {
        val map = mutableMapOf<String, Long>()
        try {
            val dirUrl = "${getUrl(context)}/$BACKUP_DIR/$MEDIA_DIR"
            val body = """<?xml version="1.0" encoding="utf-8"?>
                <D:propfind xmlns:D="DAV:"><D:prop><D:getcontentlength/></D:prop></D:propfind>""".trimIndent()
            val request = Request.Builder()
                .url(dirUrl)
                .header("Authorization", getAuthHeader(context))
                .header("Depth", "infinity")
                .method("PROPFIND", body.toRequestBody("application/xml".toMediaType()))
                .build()
            val response = client.newCall(request).execute()
            if (response.code !in 200..299) { response.close(); return map }
            val xml = response.body?.string() ?: ""; response.close()

            val responseRe = Regex("""<D:response>(.*?)</D:response>""", RegexOption.DOT_MATCHES_ALL)
            val hrefRe = Regex("""<D:href>(.*?)</D:href>""")
            val sizeRe = Regex("""<D:getcontentlength>(.*?)</D:getcontentlength>""")
            for (match in responseRe.findAll(xml)) {
                val seg = match.value
                val href = hrefRe.find(seg)?.groupValues?.get(1) ?: continue
                val size = sizeRe.find(seg)?.groupValues?.get(1)?.toLongOrNull() ?: continue  // 目录项无长度，跳过
                val decoded = try { java.net.URLDecoder.decode(href, "UTF-8") } catch (_: Exception) { href }
                val rel = decoded.substringAfter("/$BACKUP_DIR/", "")
                if (rel.isEmpty() || !rel.startsWith("$MEDIA_DIR/")) continue
                map[rel] = size
            }
        } catch (e: Exception) { Log.e(TAG, "列媒体异常", e) }
        return map
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
                val noteId = parts[1]
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
