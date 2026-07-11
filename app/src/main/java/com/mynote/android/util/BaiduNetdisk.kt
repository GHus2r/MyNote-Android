package com.mynote.android.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

/**
 * 百度网盘备份
 *
 * 使用前需要在百度开放平台创建应用：
 * 1. https://developer.baidu.com/ → 创建应用 → 获取 client_id / client_secret
 * 2. 回调地址设为 mynote://callback
 * 3. AndroidManifest.xml 中已注册 mynote://callback scheme
 *
 * 备份文件存储在百度网盘 /apps/MyNote/ 目录下
 */
object BaiduNetdisk {
    private const val TAG = "BaiduNetdisk"

    // ═══ 配置 ═══
    // 回调地址填: mynote://callback
    @Volatile private var clientId: String = "u9nliild4IiF6hTyrOIEWYBZSJxNoIYd"
    @Volatile private var clientSecret: String = "NRKLHDomDmh8QFaOiwQWlYSdvV8JQBHl"

    fun updateConfig(id: String, secret: String) {
        clientId = id; clientSecret = secret
    }
    private const val REDIRECT_URI = "mynote://callback"
    private const val BACKUP_DIR = "/apps/MyNote/"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    // ═══════ OAuth 授权 ═══════

    fun getAuthIntent(force: Boolean = false): Intent? {
        if (clientId.isEmpty()) return null
        val url = "https://openapi.baidu.com/oauth/2.0/authorize" +
                "?response_type=code" +
                "&client_id=$clientId" +
                "&redirect_uri=$REDIRECT_URI" +
                "&scope=basic,netdisk" +
                "&display=mobile" +
                if (force) "&force_login=1" else ""
        return Intent(Intent.ACTION_VIEW, Uri.parse(url))
    }

    suspend fun handleCallback(context: Context, uri: Uri): Boolean {
        val code = uri.getQueryParameter("code") ?: return false
        val error = uri.getQueryParameter("error")
        if (error != null) {
            Log.e(TAG, "授权失败: $error - ${uri.getQueryParameter("error_description")}")
            return false
        }

        // 用 code 换 token
        return exchangeToken(context, code)
    }

    private suspend fun exchangeToken(context: Context, code: String): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val body = FormBody.Builder()
                    .add("grant_type", "authorization_code")
                    .add("code", code)
                    .add("client_id", clientId)
                    .add("client_secret", clientSecret)
                    .add("redirect_uri", REDIRECT_URI)
                    .build()

                val request = Request.Builder()
                    .url("https://openapi.baidu.com/oauth/2.0/token")
                    .post(body)
                    .build()

                val response = client.newCall(request).execute()
                val json = JSONObject(response.body?.string() ?: "{}")
                response.close()

                if (json.has("error")) {
                    Log.e(TAG, "换 token 失败: $json")
                    return@withContext false
                }

                val prefs = Prefs(context)
                prefs.baiduAccessToken = json.getString("access_token")
                prefs.baiduRefreshToken = json.optString("refresh_token", "")
                prefs.baiduTokenExpiresTime = System.currentTimeMillis() + json.getLong("expires_in") * 1000
                Log.i(TAG, "百度网盘授权成功")
                true
            } catch (e: Exception) {
                Log.e(TAG, "换 token 异常", e)
                false
            }
        }

    private suspend fun refreshToken(context: Context): Boolean = withContext(Dispatchers.IO) {
        val prefs = Prefs(context)
        val rt = prefs.baiduRefreshToken ?: return@withContext false
        try {
            val body = FormBody.Builder()
                .add("grant_type", "refresh_token")
                .add("refresh_token", rt)
                .add("client_id", clientId)
                .add("client_secret", clientSecret)
                .build()

            val request = Request.Builder()
                .url("https://openapi.baidu.com/oauth/2.0/token")
                .post(body)
                .build()

            val response = client.newCall(request).execute()
            val json = JSONObject(response.body?.string() ?: "{}")
            response.close()

            if (json.has("error")) { Log.e(TAG, "刷新 token 失败: $json"); return@withContext false }
            prefs.baiduAccessToken = json.getString("access_token")
            prefs.baiduRefreshToken = json.optString("refresh_token", rt)
            prefs.baiduTokenExpiresTime = System.currentTimeMillis() + json.getLong("expires_in") * 1000
            Log.i(TAG, "token 已刷新")
            true
        } catch (e: Exception) { Log.e(TAG, "刷新 token 异常", e); false }
    }

    // ═══════ 备用 ═══════

    suspend fun ensureToken(context: Context): String? {
        val prefs = Prefs(context)
        val token = prefs.baiduAccessToken
        if (token.isNullOrEmpty()) return null

        // 提前刷新（过期前 1 小时）
        if (System.currentTimeMillis() > prefs.baiduTokenExpiresTime - 3600_000) {
            refreshToken(context)
        }
        return prefs.baiduAccessToken
    }

    // ═══════ 上传备份 ═══════

    suspend fun uploadBackup(
        context: Context,
        callback: (Boolean, String) -> Unit = { _, _ -> }
    ) = withContext(Dispatchers.IO) {
        try {
            val token = ensureToken(context)
            if (token.isNullOrEmpty()) {
                callback(false, if (clientId.isEmpty()) "请先配置 AppKey" else "未授权百度网盘")
                return@withContext
            }

            val json = BackupManager.exportJsonString(context)
            if (json.isEmpty()) { callback(false, "备份数据为空"); return@withContext }

            val deviceId = getDeviceId(context)
            val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val filename = "${deviceId}_${ts}_backup.json"
            val path = BACKUP_DIR + filename

            // 单步上传 — PCS file upload（比 precreate→upload→create 更简单可靠）
            val bytes = json.toByteArray(Charsets.UTF_8)
            val uploadUrl = "https://pan.baidu.com/rest/2.0/pcs/file?" +
                    "method=upload&access_token=$token&ondup=overwrite" +
                    "&path=${java.net.URLEncoder.encode(path, "UTF-8")}"

            val multipart = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", filename,
                    bytes.toRequestBody("application/octet-stream".toMediaType()))
                .build()

            val uploadReq = Request.Builder().url(uploadUrl).post(multipart).build()
            val uploadResp = client.newCall(uploadReq).execute()
            val respBody = uploadResp.body?.string() ?: "{}"
            uploadResp.close()

            val respJson = JSONObject(respBody)
            if (respJson.optLong("size", -1L) == bytes.size.toLong() ||
                respJson.optString("md5").isNotEmpty() ||
                respJson.optInt("errno", -1) == 0) {
                Log.i(TAG, "百度网盘上传成功: $path (${bytes.size} bytes)")
                callback(true, "已备份到百度网盘")
            } else {
                Log.e(TAG, "upload failed: $respJson")
                callback(false, "上传失败(${respJson.optInt("errno")}): ${respJson.optString("error_msg", "未知")}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "备份异常", e)
            callback(false, "网络错误: ${e.message}")
        }
    }

    // ═══════ 从百度网盘恢复 ═══════

    data class BackupFile(val path: String, val name: String, val size: Long, val time: String)

    suspend fun listBackupFiles(context: Context): List<BackupFile> = withContext(Dispatchers.IO) {
        val result = mutableListOf<BackupFile>()
        try {
            val token = ensureToken(context) ?: return@withContext result
            val url = "https://pan.baidu.com/rest/2.0/xpan/file?" +
                    "method=list&access_token=$token&dir=${java.net.URLEncoder.encode(BACKUP_DIR, "UTF-8")}" +
                    "&order=time&desc=1&limit=50&web=0"
            val resp = client.newCall(Request.Builder().url(url).get().build()).execute()
            val json = JSONObject(resp.body?.string() ?: "{}")
            resp.close()

            if (json.optInt("errno", -1) != 0) {
                Log.e(TAG, "列文件失败: $json")
                return@withContext result
            }
            val list = json.optJSONArray("list") ?: return@withContext result
            for (i in 0 until list.length()) {
                val f = list.getJSONObject(i)
                result.add(BackupFile(
                    path = f.getString("path"),
                    name = f.optString("server_filename", f.getString("path").split("/").last()),
                    size = f.optLong("size", 0L),
                    time = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                        .format(Date(f.optLong("server_mtime", 0L) * 1000))
                ))
            }
        } catch (e: Exception) { Log.e(TAG, "列文件异常", e) }
        result
    }

    suspend fun downloadBackup(context: Context, remotePath: String): String? = withContext(Dispatchers.IO) {
        try {
            val token = ensureToken(context) ?: return@withContext null
            val url = "https://pan.baidu.com/rest/2.0/xpan/file?" +
                    "method=download&access_token=$token&path=${java.net.URLEncoder.encode(remotePath, "UTF-8")}"
            // 先用 stream 下载到字节数组
            val redirectClient = OkHttpClient.Builder()
                .followRedirects(true)
                .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
                .build()
            val resp = redirectClient.newCall(Request.Builder().url(url).get().build()).execute()
            val content = resp.body?.string()
            resp.close()
            content
        } catch (e: Exception) { Log.e(TAG, "下载异常", e); null }
    }

    // ═══════ 状态检查 ═══════

    fun isAuthorized(context: Context): Boolean {
        val prefs = Prefs(context)
        return !prefs.baiduAccessToken.isNullOrEmpty()
    }

    private fun getDeviceId(context: Context): String {
        val prefs = context.getSharedPreferences("mynote_sync", Context.MODE_PRIVATE)
        var id = prefs.getString("device_id", null)
        if (id == null) {
            id = "android_" + java.util.UUID.randomUUID().toString().take(8)
            prefs.edit().putString("device_id", id).apply()
        }
        return id
    }

    /**
     * 应用启动时从 Prefs 加载已保存的 AppKey/SecretKey
     */
    fun init(context: Context) {
        val p = Prefs(context)
        // Prefs 里有的优先，否则用代码默认值
        if (p.cosSecretId.isNotEmpty()) clientId = p.cosSecretId
        if (p.cosSecretKey.isNotEmpty()) clientSecret = p.cosSecretKey
    }

    fun isConfigured(): Boolean = clientId.isNotEmpty() && clientSecret.isNotEmpty()
}
