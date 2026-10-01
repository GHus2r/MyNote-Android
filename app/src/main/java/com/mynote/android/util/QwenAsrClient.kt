package com.mynote.android.util

import android.content.Context
import android.util.Base64
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

/**
 * 阿里云 Qwen3-ASR — 长音频切片版（流式落盘防 OOM）
 * qwen3-asr-flash 单次请求限约 3 分钟 / 10MB：
 * PCM 磁盘文件 → 按 90s 随机读取切片封 WAV → 并发 3 路转写 → 按序拼接
 * 支持两种凭证：sk-xxx（Bearer token）和 AK/SK（先换 Token）
 */
object QwenAsrClient {

    private const val ENDPOINT = "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions"
    private const val TOKEN_URL = "https://dashscope.aliyuncs.com/api/v1/tokens"
    private const val MODEL = "qwen3-asr-flash"

    private const val SEGMENT_SECONDS = 90
    private const val SEGMENT_PARALLEL = 3

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(300, java.util.concurrent.TimeUnit.SECONDS)
        .callTimeout(310, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    /**
     * 转写音频文件。自动切片；onProgress 报告 (已完成片数, 总片数)。
     * @param pcmFile 可选共享 PCM 文件（16k mono，调用方解码一次供多路 ASR 共用；不传则内部解码）
     */
    suspend fun transcribe(
        context: Context, audioFile: File,
        onSegmentProgress: ((done: Int, total: Int) -> Unit)? = null,
        pcmFile: File? = null
    ): Result<String> {
        return withContext(Dispatchers.IO) {
            var ownPcm: File? = null
            try {
                val p = Prefs(context)
                val apiKey = p.qwenApiKey
                val apiSecret = p.qwenApiSecret
                if (apiKey.isEmpty()) throw Exception("未配置阿里云凭证")

                val token = getToken(apiKey, apiSecret).getOrElse {
                    throw Exception("获取Token失败: ${it.message}")
                }

                // PCM 流式落盘（磁盘文件），堆内不再持有完整 PCM —— 修复 OOM
                val pcm = pcmFile ?: PcmDecoder.decodeToPcmFile(audioFile).also { ownPcm = it }
                val totalBytes = pcm.length()
                if (totalBytes == 0L) throw Exception("音频解码失败（文件损坏或格式不支持）")

                val segBytes = SEGMENT_SECONDS.toLong() * PcmDecoder.BYTES_PER_SEC
                val sliceList = mutableListOf<Pair<Long, Int>>()
                var off = 0L
                while (off < totalBytes) {
                    sliceList.add(off to minOf(segBytes, totalBytes - off).toInt())
                    off += segBytes
                }

                val total = sliceList.size
                val doneCount = AtomicInteger(0)
                val texts = arrayOfNulls<String>(total)
                val sem = Semaphore(SEGMENT_PARALLEL)
                var firstError: Exception? = null
                val errMutex = Object()

                coroutineScope {
                    sliceList.mapIndexed { idx, slice ->
                        async {
                            sem.withPermit {
                                val r = runCatching {
                                    val seg = readSlice(pcm, slice.first, slice.second)
                                    transcribeSegment(token, seg)
                                }
                                val d = doneCount.incrementAndGet()
                                onSegmentProgress?.invoke(d, total)
                                r.fold(
                                    onSuccess = { text ->
                                        texts[idx] = text
                                    },
                                    onFailure = { e ->
                                        val ex = e as? Exception ?: Exception(e.message ?: "识别失败")
                                        synchronized(errMutex) {
                                            if (firstError == null) firstError = ex
                                        }
                                    }
                                )
                            }
                        }
                    }.awaitAll()
                }

                val anySuccess = texts.any { !it.isNullOrEmpty() }
                if (!anySuccess) throw firstError ?: Exception("识别结果为空")
                val merged = texts.mapIndexed { i, t -> t ?: "[第${i + 1}段识别失败]" }
                    .joinToString("")
                    .trim()
                Result.success(merged)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Result.failure(e)
            } finally {
                ownPcm?.delete()
            }
        }
    }

    /** 从 PCM 磁盘文件随机读取一片（堆内仅此一片，~2.9MB） */
    private fun readSlice(f: File, offset: Long, len: Int): ByteArray {
        val buf = ByteArray(len)
        java.io.RandomAccessFile(f, "r").use { raf ->
            raf.seek(offset)
            raf.readFully(buf)
        }
        return buf
    }

    /** 单片转写：PCM → WAV → base64 dataUri → HTTP */
    private fun transcribeSegment(token: String, pcm: ByteArray): String {
        val wav = PcmDecoder.pcmToWav(pcm)
        val dataUri = "data:audio/wav;base64," + Base64.encodeToString(wav, Base64.NO_WRAP)

        val body = JSONObject().apply {
            put("model", MODEL)
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", JSONArray().apply {
                        put(JSONObject().apply {
                            put("type", "input_audio")
                            put("input_audio", JSONObject().apply {
                                put("data", dataUri)
                            })
                        })
                    })
                })
            })
        }

        val resp = client.newCall(
            Request.Builder().url(ENDPOINT)
                .header("Authorization", "Bearer $token")
                .header("Content-Type", "application/json")
                .post(body.toString().toRequestBody("application/json".toMediaType()))
                .build()
        ).execute()

        val respBody = resp.body?.string() ?: ""
        if (!resp.isSuccessful) throw Exception("阿里云 HTTP ${resp.code}: ${respBody.take(200)}")

        val json = JSONObject(respBody)
        val text = json.getJSONArray("choices")
            .getJSONObject(0)
            .getJSONObject("message")
            .optString("content", "")
        if (text.isEmpty()) throw Exception("识别结果为空")
        return text.trim()
    }

    /** 用 AK/SK 或 sk-xxx 获取临时 Token */
    private fun getToken(apiKey: String, apiSecret: String): Result<String> {
        if (apiKey.startsWith("sk-")) return Result.success(apiKey)
        if (apiSecret.isEmpty()) return Result.failure(Exception("AccessKey Secret 为空"))
        val cred = Base64.encodeToString("$apiKey:$apiSecret".toByteArray(Charsets.UTF_8), Base64.NO_WRAP)

        return try {
            val resp = client.newCall(
                Request.Builder().url(TOKEN_URL)
                    .header("Authorization", "Basic $cred")
                    .post("{}".toRequestBody("application/json".toMediaType()))
                    .build()
            ).execute()
            val body = resp.body?.string() ?: ""
            if (!resp.isSuccessful) return Result.failure(Exception("Token接口 HTTP ${resp.code}: ${body.take(100)}"))
            val token = JSONObject(body).optString("token", "")
            if (token.isEmpty()) Result.failure(Exception("Token为空: ${body.take(100)}"))
            else Result.success(token)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
