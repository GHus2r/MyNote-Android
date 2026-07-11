package com.mynote.android.util

import android.content.Context
import android.util.Base64
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File

/**
 * 天翼AI — 维吾尔语非流式语音识别
 * 通过临时文件上传中转，解决 file_link 需求
 */
object TianyiAsrClient {

    private const val API_URL = "https://api.teleai.com.cn/aipaas/voice/v1/UyghurNostreamingAsr/filetrans"

    /** 临时文件上传地址（备选: https://0x0.st, https://transfer.sh） */
    private val UPLOAD_URLS = listOf(
        "https://0x0.st",
        "https://transfer.sh"
    )

    suspend fun transcribe(context: Context, audioFile: File): Result<String> = withContext(Dispatchers.IO) {
        val p = Prefs(context)
        val appId = p.tianyiAppId
        val apiKey = p.tianyiApiKey
        if (appId.isEmpty() || apiKey.isEmpty()) return@withContext Result.failure(Exception("未配置天翼API"))

        // Step 1: 上传音频文件获得 HTTP URL
        val fileLink = uploadFile(audioFile) ?: return@withContext Result.failure(Exception("文件上传失败，请检查网络"))

        // Step 2: 调天翼识别
        callAsr(appId, apiKey, p.tianyiDeviceUuid, fileLink)
    }

    // ---------- 内部实现 ----------

    private fun uploadFile(file: File): String? {
        val bytes = file.readBytes()
        val mediaType = "application/octet-stream".toMediaType()
        val name = file.name
        val client = OkHttpClient.Builder().connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS).build()

        for (baseUrl in UPLOAD_URLS) {
            try {
                if (baseUrl.contains("0x0.st")) {
                    // 0x0.st: multipart POST, field="file"
                    val body = MultipartBody.Builder().setType(MultipartBody.FORM)
                        .addFormDataPart("file", name, bytes.toRequestBody(mediaType))
                        .build()
                    val resp = client.newCall(Request.Builder().url(baseUrl).post(body).build()).execute()
                    val link = resp.body?.string()?.trim()
                    if (!link.isNullOrEmpty() && link.startsWith("http")) return link
                } else if (baseUrl.contains("transfer.sh")) {
                    // transfer.sh: PUT with filename in path
                    val body = bytes.toRequestBody(mediaType)
                    val resp = client.newCall(Request.Builder().url("$baseUrl/$name").put(body).build()).execute()
                    val link = resp.body?.string()?.trim()
                    if (!link.isNullOrEmpty() && link.startsWith("http")) return link
                }
            } catch (_: Exception) { continue }
        }
        return null
    }

    private fun callAsr(appId: String, apiKey: String, deviceUuid: String, fileLink: String): Result<String> {
        val json = JSONObject().apply {
            put("file_link", fileLink)
            put("lang", "ug")
            put("encoding", "utf8")
        }
        val body = json.toString().toRequestBody("application/json".toMediaType())

        val client = OkHttpClient.Builder().connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(120, java.util.concurrent.TimeUnit.SECONDS).build()

        val resp = client.newCall(
            Request.Builder().url(API_URL)
                .header("X-APP-ID", appId)
                .header("Device-Uuid", deviceUuid)
                .header("Authorization", "Bearer $apiKey")
                .post(body)
                .build()
        ).execute()

        val respBody = resp.body?.string() ?: ""
        if (!resp.isSuccessful) return Result.failure(Exception("天翼错误 ${resp.code}: $respBody"))

        val result = JSONObject(respBody)
        val code = result.optInt("code", -1)
        if (code != 0) return Result.failure(Exception("天翼错误 $code: ${result.optString("message")}"))

        // 结果可能在 text / result / data 字段，不同接口返回不同
        return Result.success(
            result.optString("text", "")
                .ifEmpty { result.optJSONObject("data")?.optString("text") ?: "" }
                .ifEmpty { result.optJSONObject("result")?.optString("text") ?: "" }
        )
    }
}
