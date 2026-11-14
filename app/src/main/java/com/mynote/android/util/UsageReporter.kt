package com.mynote.android.util

import android.content.Context
import android.os.Build
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * 匿名使用统计 + 用户反馈上报。
 *
 * 上报内容全部匿名：随机 UUID（设备标识，与任何账号/IMEI 无关）+ App 版本号 + 系统版本 + 机型。
 * 不采集任何个人敏感信息（位置、通讯录、笔记内容、账号等）。
 *
 * 后端见仓库 `server/` 目录（Flask + SQLite）。
 */
object UsageReporter {

    /**
     * 统计后端地址（部署后填写，末尾不要带 `/`）。
     * 例如：`https://mynote-stats.onrender.com`
     *
     * 留空则上报与反馈功能自动禁用（静默跳过，不影响 App 运行）。
     */
    private const val SERVER_URL = ""

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS)
        .writeTimeout(6, TimeUnit.SECONDS)
        .build()

    private fun baseUrl(context: Context): String {
        val override = Prefs(context).feedbackServerUrl
        if (override.isNotBlank()) return override.trimEnd('/')
        return SERVER_URL.trimEnd('/')
    }

    private fun enabled(context: Context): Boolean =
        baseUrl(context).isNotBlank() && Prefs(context).usageStatEnabled

    private fun appVersion(context: Context): String = try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "?"
    } catch (e: Exception) { "?" }

    /**
     * App 启动时调用：匿名上报一次使用。
     * 失败静默、异步、不阻塞启动。
     */
    fun reportLaunch(context: Context) {
        if (!enabled(context)) return
        val body = JSONObject().apply {
            put("device_id", Prefs(context).usageStatDeviceId)
            put("app_version", appVersion(context))
            put("android_version", Build.VERSION.RELEASE)
            put("device_model", Build.MODEL)
        }
        post("${baseUrl(context)}/api/ping", body, null)
    }

    /**
     * 提交用户反馈。
     * @param onResult 在后台线程回调 (是否成功, 失败时的错误信息)
     */
    fun submitFeedback(
        context: Context,
        title: String,
        content: String,
        contact: String,
        onResult: (Boolean, String) -> Unit
    ) {
        val url = baseUrl(context)
        if (url.isBlank()) {
            onResult(false, "统计服务器未配置")
            return
        }
        val body = JSONObject().apply {
            put("device_id", Prefs(context).usageStatDeviceId)
            put("title", title)
            put("content", content)
            put("contact", contact)
            put("app_version", appVersion(context))
        }
        post("$url/api/feedback", body, onResult)
    }

    private fun post(url: String, body: JSONObject, onResult: ((Boolean, String) -> Unit)?) {
        val request = try {
            Request.Builder()
                .url(url)
                .post(body.toString().toRequestBody("application/json".toMediaType()))
                .build()
        } catch (e: Exception) {
            onResult?.invoke(false, e.message ?: "请求构造失败")
            return
        }
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                onResult?.invoke(false, e.message ?: "网络错误")
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val ok = it.isSuccessful
                    val msg = if (ok) "" else "HTTP ${it.code}"
                    onResult?.invoke(ok, msg)
                }
            }
        })
    }
}
