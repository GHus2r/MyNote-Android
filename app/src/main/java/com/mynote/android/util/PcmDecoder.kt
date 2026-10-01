package com.mynote.android.util

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * 音频解码工具：M4A/AAC/AMR 等 → 16kHz 16bit 单声道 PCM
 *
 * 关键设计——流式落盘：
 * 长音频（如 14 分钟）完整 PCM 约 27MB，若在堆内多份复制会 OOM（256MB 堆上限）。
 * 本类把解码结果直接写入磁盘临时文件，内存峰值仅单个 codec 缓冲区（几十 KB），
 * 消费方（讯飞/阿里 ASR）按片从磁盘随机读取。
 */
object PcmDecoder {

    const val SAMPLE_RATE = 16000
    const val BYTES_PER_SEC = SAMPLE_RATE * 2   // 16bit mono

    /**
     * 流式解码音频 → 16k mono PCM 磁盘文件（<原文件名>.pcm，同目录）
     * 失败抛异常。调用方用完后应 delete()。
     */
    fun decodeToPcmFile(audio: File): File {
        val outFile = File(audio.parentFile, audio.name + ".pcm")
        if (outFile.exists()) outFile.delete()

        val extractor = MediaExtractor()
        extractor.setDataSource(audio.absolutePath)
        var trackIndex = -1
        var sampleRate = 0
        for (i in 0 until extractor.trackCount) {
            val fmt = extractor.getTrackFormat(i)
            if (fmt.getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true) {
                trackIndex = i
                sampleRate = fmt.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                break
            }
        }
        if (trackIndex < 0) { extractor.release(); throw Exception("音频文件无音轨") }
        extractor.selectTrack(trackIndex)

        val mime = extractor.getTrackFormat(trackIndex).getString(MediaFormat.KEY_MIME) ?: "audio/mp4a-latm"
        val codec = MediaCodec.createDecoderByType(mime)
        val fos = FileOutputStream(outFile)
        try {
            codec.configure(extractor.getTrackFormat(trackIndex), null, null, 0)
            codec.start()

            val bufInfo = MediaCodec.BufferInfo()
            var eosQueued = false
            var idleCount = 0

            // 流式线性插值重采样状态（跨 chunk 连续，44.1k/48k → 16k）
            val ratio = sampleRate.toDouble() / SAMPLE_RATE
            var fracPos = 0.0     // 下一个输出样本在输入流中的位置
            var prev: Short = 0   // 上一个输入样本（样本 n-1）
            var consumed = 0L     // 已消费输入样本数

            while (true) {
                if (!eosQueued) {
                    val inIndex = codec.dequeueInputBuffer(10_000)
                    if (inIndex >= 0) {
                        val inputBuf = codec.getInputBuffer(inIndex)!!
                        val sampleSize = extractor.readSampleData(inputBuf, 0)
                        if (sampleSize < 0) {
                            codec.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            eosQueued = true
                        } else {
                            codec.queueInputBuffer(inIndex, 0, sampleSize, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }

                val outIndex = codec.dequeueOutputBuffer(bufInfo, 10_000)
                when {
                    outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> continue
                    outIndex == MediaCodec.INFO_TRY_AGAIN_LATER -> {
                        // EOS 已送入但长时间无输出（连续 ~15s）→ 放弃，保留已解出的数据
                        if (eosQueued) { idleCount++; if (idleCount > 1500) break }
                        continue
                    }
                    outIndex >= 0 -> {
                        idleCount = 0
                        val outBuf = codec.getOutputBuffer(outIndex)!!
                        val shortArr = ShortArray(bufInfo.size / 2)
                        outBuf.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shortArr)
                        outBuf.clear()
                        codec.releaseOutputBuffer(outIndex, false)

                        if (sampleRate == SAMPLE_RATE) {
                            fos.write(shortsToBytes(shortArr))
                        } else {
                            // 流式线性插值重采样 → 16k：输出位置 fracPos < n 时插值 [样本n-1(prev), 样本n(x)]
                            val out = ByteArrayOutputStream(shortArr.size * 2)
                            for (x in shortArr) {
                                while (fracPos < consumed) {
                                    val f = fracPos - (consumed - 1)
                                    val v = (prev * (1 - f) + x * f).toInt().coerceIn(-32768, 32767)
                                    out.write(v and 0xFF)
                                    out.write((v shr 8) and 0xFF)
                                    fracPos += ratio
                                }
                                prev = x
                                consumed++
                            }
                            fos.write(out.toByteArray())
                        }

                        if (bufInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) break
                    }
                }
            }
        } finally {
            try { fos.close() } catch (_: Exception) {}
            try { codec.stop() } catch (_: Exception) {}
            try { codec.release() } catch (_: Exception) {}
            try { extractor.release() } catch (_: Exception) {}
        }

        if (outFile.length() == 0L) {
            outFile.delete()
            throw Exception("音频解码失败（文件损坏或格式不支持）")
        }
        return outFile
    }

    fun shortsToBytes(shorts: ShortArray): ByteArray {
        val bytes = ByteArray(shorts.size * 2)
        val buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        buf.asShortBuffer().put(shorts)
        return bytes
    }

    /** 给 16kHz 16bit 单声道 PCM 包一层 44 字节 WAV 头 */
    fun pcmToWav(pcm: ByteArray, sampleRate: Int = SAMPLE_RATE, channels: Int = 1, bitsPerSample: Int = 16): ByteArray {
        val totalLen = 44 + pcm.size
        val out = ByteArrayOutputStream(totalLen)
        val le = { v: Int, n: Int -> ByteArray(n) { i -> ((v shr (8 * i)) and 0xFF).toByte() } }
        out.write("RIFF".toByteArray())
        out.write(le(totalLen - 8, 4))
        out.write("WAVE".toByteArray())
        out.write("fmt ".toByteArray())
        out.write(le(16, 4))
        out.write(le(1, 2))                       // PCM
        out.write(le(channels, 2))
        out.write(le(sampleRate, 4))
        out.write(le(sampleRate * channels * bitsPerSample / 8, 4)) // byte rate
        out.write(le(channels * bitsPerSample / 8, 2))              // block align
        out.write(le(bitsPerSample, 2))
        out.write("data".toByteArray())
        out.write(le(pcm.size, 4))
        out.write(pcm)
        return out.toByteArray()
    }
}
