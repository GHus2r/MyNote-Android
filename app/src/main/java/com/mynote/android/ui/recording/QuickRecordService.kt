package com.mynote.android.ui.recording

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.mynote.android.R
import com.mynote.android.data.AppDatabase
import com.mynote.android.data.entity.ContentItem
import com.mynote.android.data.entity.Note
import com.mynote.android.ui.settings.SettingsActivity
import kotlinx.coroutines.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * 摇一摇后台录音服务
 * 前台服务 + 加速度传感器监听 + 锁屏录音
 */
class QuickRecordService : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var recorder: MediaRecorder? = null
    private var audioFile: File? = null
    private var recording = false
    private var recordStartTime = 0L
    private var currentNoteId: String? = null
    private var wakeLock: PowerManager.WakeLock? = null

    // 摇一摇检测
    private var lastShakeTime = 0L
    private var shakeCount = 0
    private val shakeThreshold = 20f
    private val shakeCooldown = 5000L
    private val shakeWindow = 600L
    private fun requiredShakeCount(): Int = if (com.mynote.android.util.Prefs(this).shakeSensitive) 3 else 4

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    companion object {
        const val CHANNEL_ID = "qr_v2"
        const val NOTIFY_ID = 4001
        const val ACTION_START = "com.mynote.quickrecord.START"
        const val ACTION_STOP = "com.mynote.quickrecord.STOP"
        const val ACTION_TOGGLE = "com.mynote.quickrecord.TOGGLE"

        fun start(context: Context) {
            val intent = Intent(context, QuickRecordService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                context.startForegroundService(intent)
            else context.startService(intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, QuickRecordService::class.java))
        }
    }

    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_TOGGLE -> toggleRecording()
            ACTION_START -> if (!recording) toggleRecording()
            ACTION_STOP -> if (recording) toggleRecording()
        }
        startShakeDetection()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                startForeground(NOTIFY_ID, buildNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE or ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            } catch (e: SecurityException) {
                // Android 15+ 要求运行时持有 RECORD_AUDIO 权限
                startForeground(NOTIFY_ID, buildNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            }
        } else {
            startForeground(NOTIFY_ID, buildNotification())
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ── 摇一摇检测 ──
    private fun startShakeDetection() {
        val accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        sensorManager.registerListener(this, accel, SensorManager.SENSOR_DELAY_UI)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type != Sensor.TYPE_ACCELEROMETER) return
        val x = event.values[0]; val y = event.values[1]; val z = event.values[2]
        val g = kotlin.math.sqrt(x * x + y * y + z * z.toDouble()) - SensorManager.GRAVITY_EARTH
        if (g > shakeThreshold) {
            val now = System.currentTimeMillis()
            if (now - lastShakeTime > shakeWindow) shakeCount = 0
            lastShakeTime = now
            shakeCount++
            if (shakeCount >= requiredShakeCount()) {
                // 两次触发间冷却
                if (System.currentTimeMillis() - (lastToggleTime) > shakeCooldown) {
                    lastToggleTime = System.currentTimeMillis()
                    toggleRecording()
                }
                shakeCount = 0
            }
        }
    }

    private var lastToggleTime = 0L

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    // ── 录音控制 ──
    private fun toggleRecording() {
        if (recording) stopRecording() else startRecording()
    }

    private fun startRecording() {
        scope.launch {
            try {
                // 获取或创建"快速录音"子分类
                val db = AppDatabase.get(this@QuickRecordService)
                val dao = db.noteDao()
                val catDao = db.categoryDao()

                // 确保有 "Wuw" 父分类
                val parents = catDao.getParentCategories()
                var parent = parents.firstOrNull { it.name == "Wuw" }
                if (parent == null) {
                    parent = com.mynote.android.data.entity.ParentCategory(
                        id = UUID.randomUUID().toString(),
                        name = "Wuw",
                        color = "#FF6B6B",
                        sortOrder = parents.size
                    )
                    catDao.insertParentCategory(parent)
                }

                // 找到或创建"快速录音"子分类（在 Wuw 下）
                var sub = catDao.getSubCategories(parent.id).firstOrNull { it.name == "快速录音" }
                if (sub == null) {
                    val newSub = com.mynote.android.data.entity.SubCategory(
                        id = UUID.randomUUID().toString(),
                        parentId = parent.id,
                        name = "快速录音",
                        sortOrder = 0,
                        color = "#FF6B6B"
                    )
                    catDao.insertSubCategory(newSub)
                    sub = newSub
                }

                // 创建笔记
                val nowStr = timeFormat.format(Date())
                val noteId = UUID.randomUUID().toString()
                val note = Note(
                    id = noteId,
                    subCategoryId = sub.id,
                    title = "快速录音 ${timeFormat.format(Date()).substringAfter(" ")}",
                    contentText = "",
                    updateTime = nowStr,
                    createTime = nowStr,
                    tags = "快速录音"
                )
                dao.insertNote(note)
                currentNoteId = noteId

                // 开始录音
                val dir = File(filesDir, "notes/$noteId")
                dir.mkdirs()
                audioFile = File(dir, "voice_${System.currentTimeMillis()}.m4a")

                // 先获取唤醒锁（锁屏录音必须，保持音频 DSP 活跃）
                val pm = getSystemService(POWER_SERVICE) as PowerManager
                wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "mynote:quickrecord")
                wakeLock?.acquire(10 * 60 * 1000L) // 最长10分钟

                recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                    MediaRecorder(this@QuickRecordService)
                else @Suppress("DEPRECATION") MediaRecorder()

                // VOICE_RECOGNITION 提供 AGC 自动增益 + 降噪，锁屏录音远距离也能拾音
                recorder?.setAudioSource(MediaRecorder.AudioSource.VOICE_RECOGNITION)
                recorder?.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                recorder?.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                recorder?.setAudioChannels(1)
                recorder?.setAudioSamplingRate(48000)
                recorder?.setAudioEncodingBitRate(192000)
                recorder?.setOutputFile(audioFile?.absolutePath)
                recorder?.prepare()
                recorder?.start()
                recording = true
                recordStartTime = System.currentTimeMillis()

                // 震动反馈
                val vibrator = getSystemService(VIBRATOR_SERVICE) as android.os.Vibrator
                vibrator.vibrate(android.os.VibrationEffect.createOneShot(80, android.os.VibrationEffect.DEFAULT_AMPLITUDE))

            } catch (e: Exception) {
                // prepare/start 失败时释放已获取的唤醒锁和录音器，避免电池耗尽/资源泄漏
                try { recorder?.release() } catch (_: Exception) {}
                recorder = null
                try { wakeLock?.release() } catch (_: Exception) {}
                wakeLock = null
            }
        }
    }

    private fun stopRecording() {
        scope.launch {
            try {
                recorder?.stop()
                recorder?.release()
                recorder = null
                recording = false
                wakeLock?.release()
                wakeLock = null

                // 保存语音内容到笔记
                val db = AppDatabase.get(this@QuickRecordService)
                val noteId = currentNoteId ?: return@launch
                val f = audioFile
                var voiceDur = 0L
                if (f != null && f.exists() && f.length() > 0) {
                    val dur = try {
                        val mp = android.media.MediaPlayer()
                        mp.setDataSource(f.absolutePath); mp.prepare()
                        val ms = mp.duration.toLong(); mp.release()
                        ms
                    } catch (_: Exception) { 0L }
                    voiceDur = dur
                    val item = ContentItem(
                        noteId = noteId,
                        type = "voice",
                        content = f.absolutePath,
                        timestamp = timeFormat.format(Date()),
                        voiceDuration = dur
                    )
                    db.noteDao().insertContentItem(item)
                }

                // 更新笔记时间和语音预览
                db.noteDao().getNote(noteId)?.let {
                    val durSec = voiceDur / 1000
                    val voicePreview = "语音 (${String.format("%02d:%02d", durSec / 60, durSec % 60)})"
                    db.noteDao().updateNote(it.copy(
                        updateTime = timeFormat.format(Date()),
                        contentText = voicePreview
                    ))
                }

                currentNoteId = null
                audioFile = null

                // 震动反馈
                val vibrator = getSystemService(VIBRATOR_SERVICE) as android.os.Vibrator
                vibrator.vibrate(android.os.VibrationEffect.createOneShot(50, android.os.VibrationEffect.DEFAULT_AMPLITUDE))

            } catch (e: Exception) {
            }
        }
    }

    // ── 通知 ──
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // 删旧频道
            getSystemService(NotificationManager::class.java).deleteNotificationChannel("quick_record")
            val importance = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) NotificationManager.IMPORTANCE_NONE
            else NotificationManager.IMPORTANCE_MIN
            val chan = NotificationChannel(CHANNEL_ID, "qr", importance).apply {
                setShowBadge(false)
                setSound(null, null)
                enableVibration(false)
                lockscreenVisibility = Notification.VISIBILITY_SECRET
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(chan)
        }
    }

    private fun buildNotification(isRecording: Boolean = false, text: String = ""): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("")
            .setContentText("")
            .setSmallIcon(R.drawable.ic_transparent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
            .setShowWhen(false)
            .build()
    }

    private fun updateNotification(isRecording: Boolean, text: String) {
        // 通知已通过 stopForeground(true) 隐藏，不再更新
    }

    override fun onDestroy() {
        sensorManager.unregisterListener(this)
        recorder?.let { try { it.stop() } catch (_: Exception) {}; try { it.release() } catch (_: Exception) {} }
        wakeLock?.let { if (it.isHeld) it.release() }
        scope.cancel()
        super.onDestroy()
    }
}
