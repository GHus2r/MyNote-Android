package com.mynote.android.util

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * 音频解码工具：M4A/AAC → 16kHz 16bit 单声道 PCM
 * 供讯飞 IAT / 阿里 Qwen-ASR 长音频切片共用
 */
object PcmDecoder {

    /** 将音频文件解码为 16kHz 16bit 单声道 PCM 字节数组 */
    fun decodeToPcm16k(file: java.io.File): ByteArray {
        val extractor = MediaExtractor()
        extractor.setDataSource(file.absolutePath)
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
        if (trackIndex < 0) { extractor.release(); return ByteArray(0) }
        extractor.selectTrack(trackIndex)

        val mime = extractor.getTrackFormat(trackIndex).getString(MediaFormat.KEY_MIME) ?: "audio/mp4a-latm"
        val codec = MediaCodec.createDecoderByType(mime)
        val outBuffers = mutableListOf<ShortArray>()
        var totalSamples = 0
        try {
            codec.configure(extractor.getTrackFormat(trackIndex), null, null, 0)
            codec.start()

            var done = false
            var eosQueued = false
            var tryAgainCount = 0
            val bufInfo = MediaCodec.BufferInfo()

            while (!done) {
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
                    outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> { tryAgainCount = 0; continue }
                    outIndex == MediaCodec.INFO_TRY_AGAIN_LATER -> {
                        // EOS 已送入但长时间无输出（连续 ~15s）→ 放弃，保留已解出的数据
                        if (eosQueued) {
                            tryAgainCount++
                            if (tryAgainCount > 1500) done = true
                        }
                        continue
                    }
                    outIndex >= 0 -> {
                        tryAgainCount = 0
                        val outBuf = codec.getOutputBuffer(outIndex)!!
                        val shortArr = ShortArray(bufInfo.size / 2)
                        outBuf.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shortArr)
                        outBuf.clear()
                        outBuffers.add(shortArr)
                        totalSamples += shortArr.size
                        codec.releaseOutputBuffer(outIndex, false)
                        if (bufInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) done = true
                    }
                }
            }
        } finally {
            try { codec.stop() } catch (_: Exception) {}
            try { codec.release() } catch (_: Exception) {}
            try { extractor.release() } catch (_: Exception) {}
        }

        val allSamples = ShortArray(totalSamples)
        var pos = 0
        for (arr in outBuffers) {
            System.arraycopy(arr, 0, allSamples, pos, arr.size)
            pos += arr.size
        }

        return if (sampleRate == 16000) {
            shortsToBytes(allSamples)
        } else {
            val ratio = sampleRate.toDouble() / 16000.0
            val resampled = ShortArray((allSamples.size / ratio).toInt())
            for (i in resampled.indices) {
                val srcIdx = (i * ratio).toInt().coerceIn(0, allSamples.size - 1)
                resampled[i] = allSamples[srcIdx]
            }
            shortsToBytes(resampled)
        }
    }

    fun shortsToBytes(shorts: ShortArray): ByteArray {
        val bytes = ByteArray(shorts.size * 2)
        val buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        buf.asShortBuffer().put(shorts)
        return bytes
    }

    /** 给 16kHz 16bit 单声道 PCM 包一层 44 字节 WAV 头 */
    fun pcmToWav(pcm: ByteArray, sampleRate: Int = 16000, channels: Int = 1, bitsPerSample: Int = 16): ByteArray {
        val totalLen = 44 + pcm.size
        val out = java.io.ByteArrayOutputStream(totalLen)
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
