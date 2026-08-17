package com.mynote.android.util

import android.content.Context
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
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
                callback(true, "已备份到坚果云")
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
}
