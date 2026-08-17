package com.mynote.android.ui.meeting

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.mynote.android.data.AppDatabase
import com.mynote.android.data.entity.ContentItem
import com.mynote.android.data.entity.Meeting
import com.mynote.android.data.entity.MeetingEntry
import com.mynote.android.data.entity.Note
import com.mynote.android.data.entity.ParentCategory
import com.mynote.android.data.entity.SubCategory
import com.mynote.android.util.DocxWriter
import com.mynote.android.util.IatHelper
import com.mynote.android.util.MeetingSummaryUtil
import com.mynote.android.util.Prefs
import kotlinx.coroutines.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class MeetingActivity : AppCompatActivity() {

    private val C_BLUE = Color.parseColor("#1565C0")
    private val C_GREEN = Color.parseColor("#2E7D32")
    private val C_RED = Color.parseColor("#C62828")
    private val C_ORANGE = Color.parseColor("#EF6C00")
    private val C_PURPLE = Color.parseColor("#6A1B9A")
    private val C_GRAY = Color.parseColor("#757575")
    private val C_WHITE = Color.WHITE
    private val C_DARK = Color.parseColor("#212121")
    private val C_LIGHT_BG = Color.parseColor("#F5F5F5")

    // ── 页面 ──
    private var setupPage: LinearLayout? = null
    private var recordPage: LinearLayout? = null

    // ── 设置页 ──
    private lateinit var etTitle: EditText
    private lateinit var etParticipants: EditText
    private var meetingType = "free"
    private var iatLanguage = "zh_cn"
    private var iatAccent = "mandarin"
    private var multiSpeaker = false  // 多人模式 → 讯飞 pd=1 说话人分离

    // ── 录制页 ──
    private var tvTopTitle: TextView? = null
    private var tvPartLabel: TextView? = null
    private var dotIndicator: View? = null
    private lateinit var tvTimer: TextView
    private lateinit var tvStatus: TextView
    private lateinit var tvTranscription: TextView
    private lateinit var speakerScroll: HorizontalScrollView
    private lateinit var speakerContainer: LinearLayout
    private lateinit var waveformContainer: LinearLayout
    private var waveformBars = mutableListOf<View>()
    private lateinit var btnRecord: Button
    private lateinit var btnEnd: Button
    private lateinit var btnSave: Button
    private lateinit var btnExport: Button

    // ── 语音条 ──
    private var voiceBar: LinearLayout? = null
    private var btnPlay: Button? = null
    private var seekBar: SeekBar? = null
    private var tvVoiceTime: TextView? = null

    // ── 结果区 ──
    private var resultsCard: LinearLayout? = null
    private var actionRow: LinearLayout? = null

    // ── 录音 ──
    private var mediaRecorder: MediaRecorder? = null
    private var audioFile: File? = null
    private var isRecording = false
    private var currentSpeaker = ""
    private var participants = mutableListOf<String>()
    private var meetingTitle = ""
    private var recordingStartTime = 0L
    private var totalMs = 0L
    private val segments = mutableListOf<SpeakerSegment>()

    // ── WakeLock / AudioFocus ──
    private var wakeLock: PowerManager.WakeLock? = null
    private var audioManager: AudioManager? = null
    private var audioFocusLocked = false

    // ── 波形 ──
    private var waveformRunnable: Runnable? = null

    // ── 协程 ──
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var dotPulse: Job? = null
    private var mediaPlayer: MediaPlayer? = null
    private val handler = Handler(Looper.getMainLooper())

    // ── 结果 ──
    private var finalDialogue = ""
    private var finalRawText = ""
    private var finalSummary: MeetingSummaryUtil.Summary? = null
    private var transcriptionProgress = ""

    private data class SpeakerSegment(val speaker: String, val startMs: Long, val endMs: Long)

    // ══════════════════════════════════════════════
    //  onCreate
    // ══════════════════════════════════════════════
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        val container = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(C_WHITE) }

        buildSetupPage(container)
        buildRecordPage(container)

        setContentView(container)
    }

    // ══════════════════════════════════════════════
    //  设置页
    // ══════════════════════════════════════════════
    private fun buildSetupPage(container: LinearLayout) {
        setupPage = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(28, 56, 28, 28) }
        setupPage!!.addView(makeTitle("新建会议"))
        setupPage!!.addView(spacer(16))
        setupPage!!.addView(makeLabel("会议标题"))
        etTitle = makeInput("例：科室例会"); setupPage!!.addView(etTitle)
        setupPage!!.addView(spacer(16))
        setupPage!!.addView(makeLabel("参会人员"))
        setupPage!!.addView(makeHint("姓名之间用逗号分隔"))
        etParticipants = makeInput("例：张三, 李四, 王五"); setupPage!!.addView(etParticipants)

        // 会议类型
        setupPage!!.addView(spacer(16))
        setupPage!!.addView(makeLabel("会议类型"))
        setupPage!!.addView(buildTypeSelector())

        // 识别语言
        setupPage!!.addView(spacer(16))
        setupPage!!.addView(makeLabel("语音识别"))
        setupPage!!.addView(buildLangSelector())

        // 多人/单人模式
        setupPage!!.addView(spacer(16))
        setupPage!!.addView(makeLabel("说话人识别"))
        setupPage!!.addView(buildSpeakerModeToggle())

        setupPage!!.addView(spacer(36))
        setupPage!!.addView(makeFullWidthBtn("开始会议", C_BLUE) { startMeeting() })

        val setupScroll = ScrollView(this).apply { addView(setupPage!!) }
        container.addView(setupScroll)
    }

    private fun buildTypeSelector(): LinearLayout {
        // 三甲医院会议类型 —— 按类别分组
        val typeGroups = listOf(
            "临床" to listOf(
                "morning" to "晨会交班",
                "difficult" to "疑难病例讨论",
                "mdt" to "MDT会诊",
                "preop" to "术前讨论",
                "death" to "死亡病例讨论"
            ),
            "管理" to listOf(
                "admin" to "院周会",
                "dept" to "科务会",
                "quality" to "质控安全",
                "pharmacy" to "药事院感"
            ),
            "教学" to listOf(
                "teaching" to "教学查房",
                "academic" to "学术研讨",
                "journal" to "文献汇报"
            ),
            "其他" to listOf(
                "nursing" to "护理会议",
                "patient" to "医患沟通",
                "free" to "自由格式"
            )
        )

        val container = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        for ((catLabel, types) in typeGroups) {
            container.addView(TextView(this).apply {
                text = catLabel; textSize = 11f; setTextColor(Color.parseColor("#9E9E9E"))
                setTypeface(null, Typeface.BOLD); setPadding(0, 6, 0, 4)
            })
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            for ((key, label) in types) {
                val selected = meetingType == key
                val chip = TextView(this).apply {
                    text = label; textSize = 11f; setPadding(10, 6, 10, 6); gravity = Gravity.CENTER
                    setTextColor(if (selected) C_WHITE else Color.parseColor("#455A64"))
                    setTypeface(null, if (selected) Typeface.BOLD else Typeface.NORMAL)
                    background = roundRect(if (selected) Color.parseColor("#1976D2") else Color.parseColor("#ECEFF1"), 16f)
                    setOnClickListener {
                        meetingType = key
                        // 刷新所有 chip：遍历容器内每行 LinearLayout
                        for (gi in 0 until container.childCount) {
                            val child = container.getChildAt(gi)
                            if (child is LinearLayout && child.orientation == LinearLayout.HORIZONTAL) {
                                for (ti in 0 until child.childCount) {
                                    val c = child.getChildAt(ti) as? TextView ?: continue
                                    val isSel = c.text == label
                                    c.setTextColor(if (isSel) C_WHITE else Color.parseColor("#455A64"))
                                    c.setTypeface(null, if (isSel) Typeface.BOLD else Typeface.NORMAL)
                                    c.background = roundRect(if (isSel) Color.parseColor("#1976D2") else Color.parseColor("#ECEFF1"), 16f)
                                }
                            }
                        }
                    }
                }
                row.addView(chip, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 0, 6, 0) })
            }
            container.addView(row)
        }
        return container
    }

    private fun buildLangSelector(): LinearLayout {
        val langs = listOf(
            Triple("zh_cn", "mandarin", "普通话"),
            Triple("zh_cn", "cantonese", "粤语"),
            Triple("zh_cn", "", "中文混合"),
            Triple("en_us", "", "英语")
        )
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        var first = true
        for ((lang, accent, label) in langs) {
            val chip = TextView(this).apply {
                text = label; textSize = 12f; setPadding(14, 8, 14, 8); gravity = Gravity.CENTER
                setTextColor(if (first) C_WHITE else Color.parseColor("#6A1B9A"))
                setTypeface(null, if (first) Typeface.BOLD else Typeface.NORMAL)
                background = roundRect(if (first) C_PURPLE else Color.parseColor("#F3E5F5"), 20f)
                setOnClickListener {
                    iatLanguage = lang; iatAccent = accent
                    for (i in 0 until row.childCount) {
                        val c = row.getChildAt(i) as TextView
                        val isSel = this == c
                        c.setTextColor(if (isSel) C_WHITE else Color.parseColor("#6A1B9A"))
                        c.setTypeface(null, if (isSel) Typeface.BOLD else Typeface.NORMAL)
                        c.background = roundRect(if (isSel) C_PURPLE else Color.parseColor("#F3E5F5"), 20f)
                    }
                }
            }
            row.addView(chip, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 0, 8, 0) })
            first = false
        }
        return row
    }

    private fun buildSpeakerModeToggle(): LinearLayout {
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val single = TextView(this).apply {
            text = "单人录音"; textSize = 12f; setPadding(14, 8, 14, 8); gravity = Gravity.CENTER
            setTextColor(C_WHITE); setTypeface(null, Typeface.BOLD)
            background = roundRect(C_BLUE, 20f)
        }
        val multi = TextView(this).apply {
            text = "多人会议（AI 区分说话人）"; textSize = 12f; setPadding(14, 8, 14, 8); gravity = Gravity.CENTER
            setTextColor(Color.parseColor("#78909C"))
            background = roundRect(Color.parseColor("#ECEFF1"), 20f)
        }
        single.setOnClickListener {
            multiSpeaker = false
            single.setTextColor(C_WHITE); single.setTypeface(null, Typeface.BOLD); single.background = roundRect(C_BLUE, 20f)
            multi.setTextColor(Color.parseColor("#78909C")); multi.setTypeface(null, Typeface.NORMAL); multi.background = roundRect(Color.parseColor("#ECEFF1"), 20f)
        }
        multi.setOnClickListener {
            multiSpeaker = true
            multi.setTextColor(C_WHITE); multi.setTypeface(null, Typeface.BOLD); multi.background = roundRect(C_BLUE, 20f)
            single.setTextColor(Color.parseColor("#78909C")); single.setTypeface(null, Typeface.NORMAL); single.background = roundRect(Color.parseColor("#ECEFF1"), 20f)
        }
        row.addView(single, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 0, 8, 0) })
        row.addView(multi)
        return row
    }

    // ══════════════════════════════════════════════
    //  录制页
    // ══════════════════════════════════════════════
    private fun buildRecordPage(container: LinearLayout) {
        recordPage = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; visibility = View.GONE }

        // 顶栏
        val topBar = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(28, 20, 28, 4); gravity = Gravity.CENTER_VERTICAL }
        tvTopTitle = TextView(this).apply { textSize = 16f; setTypeface(null, Typeface.BOLD); setTextColor(C_DARK); layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f) }
        topBar.addView(tvTopTitle)
        val timeBlock = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        dotIndicator = View(this).apply { layoutParams = LinearLayout.LayoutParams(10, 10).apply { setMargins(0, 0, 8, 0) }; background = circle(C_GRAY) }
        timeBlock.addView(dotIndicator)
        tvTimer = TextView(this).apply { text = "00:00"; textSize = 18f; setTypeface(null, Typeface.BOLD); setTextColor(C_DARK) }
        timeBlock.addView(tvTimer); topBar.addView(timeBlock)
        recordPage!!.addView(topBar)

        tvPartLabel = TextView(this).apply { textSize = 11f; setTextColor(C_GRAY); setPadding(28, 0, 28, 0) }
        recordPage!!.addView(tvPartLabel)

        // 波形条
        recordPage!!.addView(spacer(6))
        waveformContainer = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(28, 0, 28, 0); gravity = Gravity.CENTER_VERTICAL; layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 40) }
        recordPage!!.addView(waveformContainer)

        // 转录卡片
        recordPage!!.addView(spacer(8))
        val card = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(20, 12, 20, 12); setBackgroundColor(Color.parseColor("#FAFAFA")); background = roundRect(C_LIGHT_BG, 16f); layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f).apply { setMargins(16, 0, 16, 0) } }
        card.addView(TextView(this).apply { text = "实时转录"; textSize = 11f; setTextColor(C_GRAY); setPadding(0, 0, 0, 6) })
        tvTranscription = TextView(this).apply { text = "点击「开始录音」..."; textSize = 13f; setTextIsSelectable(true); setTextColor(Color.parseColor("#424242")) }
        card.addView(ScrollView(this).apply { layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f); addView(tvTranscription) } as View)
        recordPage!!.addView(card)

        // 发言人
        recordPage!!.addView(makeLabel("发言人").apply { setPadding(28, 10, 28, 6) })
        speakerScroll = HorizontalScrollView(this).apply { setPadding(28, 0, 28, 0); isHorizontalScrollBarEnabled = false }
        speakerContainer = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        speakerScroll.addView(speakerContainer)
        recordPage!!.addView(speakerScroll)

        // 状态
        tvStatus = TextView(this).apply { text = "就绪"; textSize = 12f; setTextColor(C_GRAY); setPadding(28, 8, 28, 0); gravity = Gravity.CENTER }
        recordPage!!.addView(tvStatus)

        // 按钮行
        val btnRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(16, 12, 16, 4) }
        btnRecord = makeHButton("开始录音") { toggleRecording() }; (btnRecord.layoutParams as LinearLayout.LayoutParams).apply { weight = 1f; setMargins(0, 0, 8, 0) }
        btnEnd = makeOutlineBtn("结束") { endMeeting() }; btnEnd.isEnabled = false; (btnEnd.layoutParams as LinearLayout.LayoutParams).apply { weight = 1f; setMargins(8, 0, 0, 0) }
        btnRow.addView(btnRecord); btnRow.addView(btnEnd)
        recordPage!!.addView(btnRow)

        // 语音条
        voiceBar = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(20, 8, 20, 8); visibility = View.GONE; background = roundRect(Color.parseColor("#E8F5E9"), 12f); layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { setMargins(16, 6, 16, 0) } }
        btnPlay = Button(this).apply { text = "▶"; textSize = 16f; setTextColor(C_WHITE); setPadding(14, 8, 14, 8); background = roundRect(C_GREEN, 20f) }
        voiceBar!!.addView(btnPlay)
        seekBar = SeekBar(this).apply { layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { setMargins(12, 0, 12, 0) }; isEnabled = false }
        voiceBar!!.addView(seekBar)
        tvVoiceTime = TextView(this).apply { text = "00:00"; textSize = 13f; setTextColor(C_DARK); setTypeface(null, Typeface.BOLD) }
        voiceBar!!.addView(tvVoiceTime); recordPage!!.addView(voiceBar!!)

        // 结果卡片
        resultsCard = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(16, 0, 16, 0); visibility = View.GONE }
        recordPage!!.addView(resultsCard!!)

        // 保存 & 导出
        val ar = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(16, 10, 16, 20); visibility = View.GONE }
        actionRow = ar
        btnExport = makeOutlineBtn("导出 Word") { exportMinutes() }
        btnExport.isEnabled = false; (btnExport.layoutParams as LinearLayout.LayoutParams).apply { weight = 1f; setMargins(0, 0, 8, 0) }
        btnSave = makeFullWidthBtn("保存到笔记", C_PURPLE) { saveToNote() }
        btnSave.isEnabled = false; (btnSave.layoutParams as LinearLayout.LayoutParams).apply { weight = 1f; setMargins(8, 0, 0, 0) }
        ar.addView(btnExport); ar.addView(btnSave)
        recordPage!!.addView(ar)

        val recordScroll = ScrollView(this).apply { addView(recordPage!!) }
        container.addView(recordScroll)
    }

    // ══════════════════════════════════════════════
    //  流程
    // ══════════════════════════════════════════════
    private fun startMeeting() {
        meetingTitle = etTitle.text.toString().trim().ifEmpty { "会议 ${now("MM/dd HH:mm")}" }
        val parts = etParticipants.text.toString().split(",", "，").map { it.trim() }.filter { it.isNotEmpty() }
        if (parts.isEmpty()) { toast("请至少输入一位参会人员"); return }
        participants = parts.toMutableList(); currentSpeaker = parts.first()
        setupPage?.visibility = View.GONE; recordPage?.visibility = View.VISIBLE
        tvTopTitle?.text = meetingTitle; tvPartLabel?.text = "参会: " + participants.joinToString(" · ")
        refreshSpeakers()
        buildWaveformBars()
    }

    // ── 发言人 ──
    private fun refreshSpeakers() {
        speakerContainer.removeAllViews()
        for (p in participants) {
            val active = p == currentSpeaker
            val chip = TextView(this).apply {
                text = p; textSize = 13f; gravity = Gravity.CENTER; setPadding(20, 10, 20, 10)
                setTextColor(if (active) C_WHITE else C_BLUE)
                setTypeface(null, if (active) Typeface.BOLD else Typeface.NORMAL)
                background = roundRect(if (active) Color.parseColor("#1976D2") else Color.parseColor("#E3F2FD"), 24f)
                setOnClickListener { if (isRecording) switchSpeaker(p) else toast("请先开始录音") }
            }
            speakerContainer.addView(chip, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 0, 10, 0) })
        }
        // 加人按钮
        val addBtn = TextView(this).apply {
            text = "+"; textSize = 16f; gravity = Gravity.CENTER; setPadding(14, 8, 14, 8)
            setTextColor(C_GRAY); setTypeface(null, Typeface.BOLD)
            background = roundRect(Color.parseColor("#EEEEEE"), 24f)
            setOnClickListener { showAddParticipant() }
        }
        speakerContainer.addView(addBtn, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
    }

    private fun showAddParticipant() {
        val input = EditText(this).apply { hint = "姓名"; setSingleLine(); setPadding(18, 14, 18, 14); textSize = 14f }
        val dlg = android.app.AlertDialog.Builder(this)
            .setTitle("添加参会人")
            .setView(input)
            .setPositiveButton("添加") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty() && name !in participants) {
                    participants = (participants + name).toMutableList()
                    tvPartLabel?.text = "参会: " + participants.joinToString(" · ")
                    refreshSpeakers()
                    toast("已添加 $name")
                }
            }
            .setNegativeButton("取消", null)
            .create()
        dlg.show()
        // 弹窗圆角
        dlg.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }

    private fun switchSpeaker(newSpeaker: String) {
        if (newSpeaker == currentSpeaker) return
        val now = System.currentTimeMillis()
        val startMs = totalMs + (now - recordingStartTime)
        segments.add(SpeakerSegment(currentSpeaker, totalMs, startMs))
        currentSpeaker = newSpeaker; recordingStartTime = now
        refreshSpeakers()
        tvTranscription.append("\n\n" + newSpeaker + "  " + timeStr() + "\n")
    }

    // ══════════════════════════════════════════════
    //  录音 + WakeLock + AudioFocus + 波形
    // ══════════════════════════════════════════════
    private fun toggleRecording() = if (isRecording) pauseRecording() else startRecording()

    private fun startRecording() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 100); return
        }

        // 获取音频焦点
        val result = audioManager?.requestAudioFocus(
            { focusChange ->
                when (focusChange) {
                    AudioManager.AUDIOFOCUS_LOSS, AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                        if (isRecording) pauseRecording()
                    }
                }
            },
            AudioManager.STREAM_MUSIC,
            AudioManager.AUDIOFOCUS_GAIN
        ) ?: AudioManager.AUDIOFOCUS_REQUEST_FAILED
        audioFocusLocked = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED

        // WakeLock 防止锁屏休眠
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyNote:MeetingRecord")
        wakeLock?.acquire(2 * 60 * 60 * 1000L) // 最长 2 小时

        try {
            audioFile = File(cacheDir, "meeting_" + System.currentTimeMillis() + ".m4a")
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(this)
            } else {
                MediaRecorder()
            }
            mediaRecorder!!.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(16000)
                setAudioEncodingBitRate(64000)
                setOutputFile(audioFile!!.absolutePath)
                prepare(); start()
            }
            isRecording = true; recordingStartTime = System.currentTimeMillis()
            dotIndicator?.background = circle(C_RED)
            btnRecord.text = "暂停"; btnRecord.background = roundRect(C_ORANGE, 12f); btnEnd.isEnabled = true
            tvStatus.text = "正在录制..."; tvStatus.setTextColor(C_RED); tvStatus.setTypeface(null, Typeface.BOLD)
            tvTranscription.text = "=== " + now("HH:mm:ss") + " 开始 ===\n\n" + currentSpeaker + "："

            // 脉冲 + 波形
            dotPulse = scope.launch {
                var show = false
                while (isActive) {
                    dotIndicator?.background = circle(if (show) C_RED else Color.TRANSPARENT)
                    show = !show
                    val elapsed = totalMs + (if (isRecording) System.currentTimeMillis() - recordingStartTime else 0)
                    tvTimer.text = String.format("%02d:%02d", elapsed / 60000, (elapsed % 60000) / 1000)
                    tvTimer.setTextColor(if (isRecording) C_RED else C_DARK)
                    delay(500)
                }
            }
            startWaveformPolling()
        } catch (e: Exception) { toast("录音失败: " + e.message) }
    }

    private fun pauseRecording() {
        try {
            val now = System.currentTimeMillis()
            segments.add(SpeakerSegment(currentSpeaker, totalMs, totalMs + (now - recordingStartTime)))
            totalMs += (now - recordingStartTime)
            mediaRecorder?.apply { stop(); release() }; mediaRecorder = null; isRecording = false
            dotPulse?.cancel()
            dotIndicator?.background = circle(C_GRAY)
            btnRecord.text = "继续"; btnRecord.background = roundRect(C_GREEN, 12f)
            tvStatus.text = "已暂停"; tvStatus.setTextColor(C_GRAY); tvStatus.setTypeface(null, Typeface.NORMAL)
            stopWaveformPolling()
            releaseAudioResources()
        } catch (e: Exception) { toast("暂停失败: " + e.message) }
    }

    private fun releaseAudioResources() {
        wakeLock?.let { if (it.isHeld) it.release() }; wakeLock = null
        if (audioFocusLocked) { audioManager?.abandonAudioFocus { } }; audioFocusLocked = false
    }

    // ── 波形 ──
    private fun buildWaveformBars() {
        waveformContainer.removeAllViews(); waveformBars.clear()
        for (i in 0 until 40) {
            val bar = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(3, 6).apply { setMargins(1, 0, 1, 0) }
                background = roundRect(Color.parseColor("#E0E0E0"), 1.5f)
            }
            waveformContainer.addView(bar); waveformBars.add(bar)
        }
    }

    private fun startWaveformPolling() {
        stopWaveformPolling()
        waveformRunnable = object : Runnable {
            override fun run() {
                if (!isRecording) return
                val amp = mediaRecorder?.maxAmplitude ?: 0
                val normalized = (amp / 32767.0).coerceIn(0.0, 1.0) * 3.0 // 放大
                val activeBars = (normalized * waveformBars.size).toInt().coerceAtMost(waveformBars.size)
                for (i in waveformBars.indices) {
                    val h = if (i < activeBars) {
                        (6 + (i.toDouble() / waveformBars.size * 28).toInt()).coerceAtMost(34)
                    } else 4
                    waveformBars[i].layoutParams = LinearLayout.LayoutParams(3, h).apply { setMargins(1, 0, 1, 0) }
                    waveformBars[i].background = roundRect(
                        if (i < activeBars) Color.parseColor("#42A5F5") else Color.parseColor("#E0E0E0"), 1.5f
                    )
                }
                handler.postDelayed(this, 80)
            }
        }
        handler.post(waveformRunnable!!)
    }

    private fun stopWaveformPolling() {
        waveformRunnable?.let { handler.removeCallbacks(it) }; waveformRunnable = null
    }

    // ══════════════════════════════════════════════
    //  结束 & 处理
    // ══════════════════════════════════════════════
    private fun endMeeting() {
        if (isRecording) pauseRecording()
        if (segments.isEmpty() && audioFile?.exists() != true) { toast("没有录制内容"); return }
        btnEnd.isEnabled = false; btnRecord.isEnabled = false
        stopWaveformPolling()
        setStatus("转写中...", C_BLUE)

        // 动态超时：音频时长 + 60s 缓冲，最大 10 分钟
        val timeoutMs = ((totalMs / 1000) + 60_000).coerceIn(60_000, 600_000)

        scope.launch {
            try {
                val pdParam = if (multiSpeaker) "1" else "0"
                val result = if (audioFile?.exists() == true) {
                    val p = Prefs(this@MeetingActivity)
                    val appId = p.iatAppId; val apiKey = p.iatApiKey; val apiSecret = p.iatApiSecret
                    if (appId.isNotEmpty() && apiKey.isNotEmpty()) {
                        withTimeout(timeoutMs) {
                            IatHelper.transcribeStreaming(
                                audioFile!!, appId, apiKey, apiSecret,
                                IatHelper.TranscribeParams(
                                    language = iatLanguage,
                                    accent = iatAccent,
                                    pd = pdParam,
                                    onProgress = { text ->
                                        transcriptionProgress = text
                                        tvTranscription.text = "=== 转写中... ===\n\n$text"
                                    }
                                )
                            ).getOrElse { IatHelper.TranscribeResult("", "") }
                        }
                    } else IatHelper.TranscribeResult("", "")
                } else IatHelper.TranscribeResult("", "")
                finalRawText = result.rawText

                // 多人模式：直接用讯飞 pd=1 的说话人标记文本
                if (multiSpeaker && result.annotatedText != result.rawText && result.annotatedText.isNotBlank()) {
                    finalDialogue = result.annotatedText
                    // 从 annotatedText 解析说话人分段用于统计
                    parseIatSegments(result.annotatedText)
                } else {
                    // 单人模式：按手动标记的时间段切分
                    finalDialogue = if (result.rawText.isNotBlank() && segments.isNotEmpty()) {
                        splitByTimeRatio(result.rawText)
                    } else if (result.rawText.isNotBlank()) result.rawText
                    else buildDialogueRaw()
                }

                tvTranscription.text = finalDialogue

                toast("AI 摘要..."); setStatus("AI 摘要...", C_PURPLE)
                finalSummary = MeetingSummaryUtil.summarize(
                    this@MeetingActivity, finalDialogue,
                    participants.joinToString("、"), meetingTitle, meetingType
                )
                setStatus("完成", C_GREEN, true)
                showResults()
            } catch (e: Exception) {
                setStatus("错误: " + (e.message?.take(30) ?: ""), C_RED, true)
                toast("处理失败: " + e.message)
            }
        }
    }

    private fun showResults() {
        btnSave.isEnabled = true; btnExport.isEnabled = true
        actionRow?.visibility = View.VISIBLE
        showVoiceBar()

        resultsCard?.removeAllViews()
        resultsCard?.visibility = View.VISIBLE

        // 标题
        resultsCard!!.addView(buildSectionHeader("会议结果"))
        resultsCard!!.addView(spacer(10))

        // 发言时长统计
        if (segments.isNotEmpty()) {
            resultsCard!!.addView(buildTimeStats())
            resultsCard!!.addView(spacer(12))
        }

        // AI 摘要
        if (finalSummary != null) {
            resultsCard!!.addView(buildSummaryView(finalSummary!!))
            resultsCard!!.addView(spacer(10))
        }

        // 关键词
        val kw = finalSummary?.keywords?.trim()
        if (!kw.isNullOrEmpty()) {
            resultsCard!!.addView(buildKeywordView(kw))
        }
    }

    private fun buildSectionHeader(title: String): TextView = TextView(this).apply {
        text = title; textSize = 17f; setTypeface(null, Typeface.BOLD)
        setTextColor(Color.parseColor("#37474F")); setPadding(4, 0, 0, 4)
    }

    private fun buildTimeStats(): LinearLayout {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(18, 14, 18, 14)
            background = roundRect(Color.parseColor("#F5F7FA"), 14f)
        }
        card.addView(TextView(this).apply {
            text = "⏱ 发言时长统计"; textSize = 13f; setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#455A64")); setPadding(0, 0, 0, 10)
        })
        val speakerTotal = mutableMapOf<String, Long>()
        for (s in segments) {
            speakerTotal[s.speaker] = (speakerTotal[s.speaker] ?: 0L) + (s.endMs - s.startMs)
        }
        val totalSpoken = speakerTotal.values.sum()
        val colors = listOf(
            Color.parseColor("#1565C0"), Color.parseColor("#2E7D32"), Color.parseColor("#EF6C00"),
            Color.parseColor("#6A1B9A"), Color.parseColor("#C62828"), Color.parseColor("#00838F"),
            Color.parseColor("#4E342E"), Color.parseColor("#37474F")
        )
        var ci = 0
        for ((name, ms) in speakerTotal.entries.sortedByDescending { it.value }) {
            val pct = if (totalSpoken > 0) (ms.toDouble() / totalSpoken * 100).toInt() else 0
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(0, 5, 0, 5) }
            row.addView(TextView(this).apply {
                text = name; textSize = 12f; setTextColor(Color.parseColor("#37474F"))
                setTypeface(null, Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 0.22f)
            })
            val barWrap = LinearLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 0.53f)
                orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_VERTICAL
            }
            val bar = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 16
                ).apply { setMargins(0, 0, 8, 0); weight = (pct / 100f).coerceAtLeast(0.02f) }
                background = roundRect(colors[ci % colors.size], 8f)
            }
            barWrap.addView(bar); row.addView(barWrap)
            row.addView(TextView(this).apply {
                text = "$pct%  ${formatMs(ms)}"; textSize = 11f
                setTextColor(Color.parseColor("#78909C"))
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            })
            card.addView(row); ci++
        }
        return card
    }

    private fun buildSummaryView(s: MeetingSummaryUtil.Summary): LinearLayout {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(18, 14, 18, 14)
            // 双层背景模拟阴影 + 渐变
            background = roundRect(Color.parseColor("#F3F0FF"), 14f)
        }
        // 标题栏
        card.addView(TextView(this).apply {
            text = "🤖 AI 摘要"; textSize = 13f; setTypeface(null, Typeface.BOLD)
            setTextColor(C_PURPLE); setPadding(0, 0, 0, 10)
        })
        if (s.title.isNotBlank()) {
            card.addView(TextView(this).apply {
                text = s.title; textSize = 16f; setTypeface(null, Typeface.BOLD)
                setTextColor(Color.parseColor("#37474F")); setPadding(0, 0, 0, 12)
            })
        }

        // 分区渲染 Markdown
        if (s.points.isNotBlank()) {
            card.addView(buildMdSection("讨论要点", "💬", Color.parseColor("#EDE7F6"), s.points))
            card.addView(spacer(8))
        }
        if (s.decisions.isNotBlank()) {
            card.addView(buildMdSection("决议事项", "✅", Color.parseColor("#E8F5E9"), s.decisions))
            card.addView(spacer(8))
        }
        if (s.todos.isNotBlank()) {
            card.addView(buildMdSection("待办任务", "📋", Color.parseColor("#FFF3E0"), s.todos))
            card.addView(spacer(8))
        }
        return card
    }

    /** 渲染一个 Markdown 分区（标题 + 内容，支持 **bold** / - list / ✅ / [ ]） */
    private fun buildMdSection(label: String, icon: String, bgColor: Int, md: String): LinearLayout {
        val section = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(14, 10, 14, 10)
            background = roundRect(bgColor, 10f)
        }
        section.addView(TextView(this).apply {
            text = "$icon $label"; textSize = 12f; setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#455A64")); setPadding(0, 0, 0, 6)
        })

        val contentContainer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        // 按行解析
        val lines = md.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
        for (line in lines) {
            contentContainer.addView(renderMdLine(line))
        }
        section.addView(contentContainer)
        return section
    }

    /** 单行 Markdown 渲染为 TextView（支持 ## 标题 / **加粗** / - 列表 / ✅ / [ ]） */
    private fun renderMdLine(line: String): TextView {
        val tv = TextView(this).apply {
            textSize = 12f; setTextColor(Color.parseColor("#424242"))
            setPadding(0, 3, 0, 3)
        }

        // ## 二级标题
        if (line.startsWith("## ")) {
            tv.text = line.removePrefix("## ")
            tv.textSize = 13f; tv.setTypeface(null, Typeface.BOLD)
            tv.setTextColor(Color.parseColor("#37474F"))
            tv.setPadding(0, 6, 0, 4)
            return tv
        }

        // 处理行内 **bold**
        val processed = line
            .replace(Regex("""\*\*(.+?)\*\*""")) { it.groupValues[1] }
            .replace(Regex("""\*(.+?)\*""")) { it.groupValues[1] }

        // 去掉开头的 - 或 ✅ 或 [ ]
        var display = processed
        if (display.startsWith("✅ ")) {
            display = display.removePrefix("✅ ")
            tv.text = "✓  $display"
            tv.setTextColor(Color.parseColor("#2E7D32"))
        } else if (display.startsWith("- [ ] ")) {
            display = display.removePrefix("- [ ] ")
            tv.text = "☐  $display"
            tv.setTextColor(Color.parseColor("#EF6C00"))
        } else if (display.startsWith("- [x] ") || display.startsWith("- [X] ")) {
            display = display.removePrefix("- [x] ").removePrefix("- [X] ")
            tv.text = "☑  $display"
            tv.setTextColor(Color.parseColor("#2E7D32"))
        } else if (display.startsWith("- ")) {
            display = display.removePrefix("- ")
            tv.text = "•  $display"
            tv.setPadding(12, 3, 0, 3)
        } else {
            tv.text = display
        }

        // 对 bold 部分应用加粗（用 Spannable 处理 **text**）
        val spannable = SpannableString(tv.text)
        val boldRe = Regex("""\*\*(.+?)\*\*""")
        // 原 line 中找 bold 位置在 processed 中的对应
        for (match in boldRe.findAll(line)) {
            val boldText = match.groupValues[1]
            var idx = tv.text.indexOf(boldText)
            if (idx >= 0) {
                spannable.setSpan(android.text.style.StyleSpan(Typeface.BOLD), idx, idx + boldText.length, 0)
            }
        }
        tv.text = spannable
        return tv
    }

    private fun buildKeywordView(keywords: String): LinearLayout {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(18, 14, 18, 14)
            background = roundRect(Color.parseColor("#F1F8E9"), 14f)
        }
        card.addView(TextView(this).apply {
            text = "🔑 关键词"; textSize = 12f; setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#558B2F")); setPadding(0, 0, 0, 8)
        })
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        for (kw in keywords.split(",", "，").map { it.trim() }.filter { it.isNotEmpty() }) {
            val chip = TextView(this).apply {
                text = kw; textSize = 11f; setPadding(12, 5, 12, 5)
                setTextColor(Color.parseColor("#33691E"))
                setTypeface(null, Typeface.BOLD)
                background = roundRect(Color.parseColor("#DCEDC8"), 16f)
            }
            row.addView(chip, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 0, 8, 0) })
        }
        card.addView(row)
        highlightKeywords(keywords)
        return card
    }

    private fun highlightKeywords(keywords: String) {
        val kws = keywords.split(",", "，").map { it.trim() }.filter { it.length >= 2 }
        if (kws.isEmpty()) return
        val spannable = SpannableString(tvTranscription.text)
        for (kw in kws) {
            val text = tvTranscription.text.toString()
            var idx = text.indexOf(kw)
            while (idx >= 0) {
                spannable.setSpan(ForegroundColorSpan(C_GREEN), idx, idx + kw.length, 0)
                idx = text.indexOf(kw, idx + 1)
            }
        }
        tvTranscription.text = spannable
    }

    // ── 文本切分 ──
    private fun splitByTimeRatio(rawText: String): String {
        if (segments.isEmpty()) return rawText
        val spokenMs = segments.sumOf { it.endMs - it.startMs }
        if (spokenMs <= 0) return rawText
        val textLen = rawText.length
        val sb = StringBuilder()
        var charOffset = 0
        var lastSpeaker = ""
        for (seg in segments) {
            val segMs = seg.endMs - seg.startMs
            val ratio = segMs.toDouble() / spokenMs
            val chunkLen = (textLen * ratio).toInt().coerceAtLeast(1)
            val endPos = (charOffset + chunkLen).coerceAtMost(textLen)
            val chunk = rawText.substring(charOffset, endPos).trim()
            charOffset = endPos
            if (chunk.isEmpty()) continue
            if (seg.speaker == lastSpeaker && sb.isNotEmpty()) {
                val lines = sb.lines().toMutableList()
                val last = lines.removeLastOrNull()?.trimStart() ?: ""
                val prefix = seg.speaker + ": "
                lines.add(prefix + last.removePrefix(prefix) + " " + chunk)
                sb.clear(); sb.append(lines.joinToString("\n"))
            } else {
                if (sb.isNotEmpty()) sb.append("\n\n")
                sb.append(seg.speaker + ": " + chunk)
            }
            lastSpeaker = seg.speaker
        }
        if (charOffset < textLen) sb.append(rawText.substring(charOffset))
        return sb.toString()
    }

    /** 从讯飞 pd=1 的说话人标记文本中解析分段，用于发言时长统计 */
    private fun parseIatSegments(annotatedText: String) {
        segments.clear()
        val re = Regex("""说话人(\d+):\s*""")
        var lastSpeaker = ""
        var lastPos = 0
        var totalChars = 0
        for (match in re.findAll(annotatedText)) {
            val speaker = "说话人" + match.groupValues[1]
            if (lastSpeaker.isNotEmpty()) {
                val segLen = match.range.first - lastPos
                if (segLen > 0) {
                    segments.add(SpeakerSegment(lastSpeaker,
                        totalChars.toLong(),
                        (totalChars + segLen).toLong()))
                    totalChars += segLen
                }
            }
            lastSpeaker = speaker
            lastPos = match.range.last + 1
        }
        // 最后一段
        if (lastSpeaker.isNotEmpty() && lastPos < annotatedText.length) {
            val segLen = annotatedText.length - lastPos
            if (segLen > 0) {
                segments.add(SpeakerSegment(lastSpeaker,
                    totalChars.toLong(),
                    (totalChars + segLen).toLong()))
            }
        }
    }

    private fun buildDialogueRaw(): String {
        val sb = StringBuilder()
        sb.appendLine("会议：" + meetingTitle); sb.appendLine("参会：" + participants.joinToString("、"))
        for (s in segments) sb.appendLine("  " + s.speaker + ": " + formatMs(s.startMs) + "-" + formatMs(s.endMs))
        return sb.toString()
    }

    // ══════════════════════════════════════════════
    //  保存到笔记
    // ══════════════════════════════════════════════
    private fun saveToNote() {
        scope.launch {
            try {
                val db = AppDatabase.get(this@MeetingActivity)
                val dao = db.categoryDao(); val noteDao = db.noteDao()
                val meetingDao = db.meetingDao()

                // 分类
                var parent = dao.getParentCategories().firstOrNull { it.name == "会议" }
                if (parent == null) {
                    val pId = UUID.randomUUID().toString()
                    dao.insertParentCategory(ParentCategory(pId, "会议", "#1565C0"))
                    parent = ParentCategory(pId, "会议", "#1565C0")
                }
                var sub = dao.getSubCategories(parent.id).firstOrNull { it.name == "会议记录" }
                if (sub == null) {
                    val sId = UUID.randomUUID().toString()
                    dao.insertSubCategory(SubCategory(sId, parent.id, "会议记录", "", 0))
                    sub = SubCategory(sId, parent.id, "会议记录", "", 0)
                }

                // 组装笔记文本（用于列表预览，纯文本去格式）
                var meetingText = finalDialogue
                if (finalSummary != null) {
                    meetingText = finalSummary!!.title + " | " +
                        finalSummary!!.points.replace(Regex("[#*\\-\\[\\]]"), "").take(300)
                }

                // 写 Note
                val noteId = UUID.randomUUID().toString(); val nowStr = nowFull(); val nowMs = System.currentTimeMillis()
                noteDao.insertNote(Note(noteId, sub.id, meetingTitle, meetingText.take(500), nowStr, nowStr))

                // 写 Meeting 表（结构化留存）
                try {
                    val mId = meetingDao.insertMeeting(Meeting(
                        title = meetingTitle, participants = participants.joinToString("、"),
                        startedAt = nowMs - totalMs, endedAt = nowMs,
                        summary = finalSummary?.report ?: "", report = finalSummary?.report ?: ""))
                    var entryTs = nowMs - totalMs
                    for (seg in segments) {
                        meetingDao.insertEntry(MeetingEntry(
                            meetingId = mId, speaker = seg.speaker,
                            content = formatMs(seg.startMs) + "-" + formatMs(seg.endMs),
                            timestamp = entryTs + seg.startMs))
                    }
                } catch (_: Exception) {}

                // 语音文件
                var voiceFilePath: String? = null
                if (audioFile?.exists() == true) {
                    val d = File(filesDir, "notes/" + noteId); d.mkdirs()
                    val vf = File(d, "meeting.m4a")
                    audioFile!!.copyTo(vf, overwrite = true)
                    voiceFilePath = vf.absolutePath
                }
                if (voiceFilePath != null) {
                    noteDao.insertContentItem(ContentItem(
                        noteId = noteId, type = "voice", content = voiceFilePath,
                        timestamp = nowStr, voiceDuration = totalMs,
                        voiceTranscript = finalRawText.ifEmpty { buildDialogueRaw() }.take(5000)
                    ))
                }
                // AI 摘要 → HTML
                if (finalSummary != null) {
                    noteDao.insertContentItem(ContentItem(
                        noteId = noteId, type = "text",
                        content = buildSummaryHtml(finalSummary!!),
                        timestamp = nowStr
                    ))
                }
                // 原始转录
                if (finalRawText.isNotBlank()) {
                    noteDao.insertContentItem(ContentItem(
                        noteId = noteId, type = "text",
                        content = "原始转录\n\n" + finalRawText, timestamp = nowStr
                    ))
                }
                if (finalDialogue.isNotBlank() && finalDialogue != finalRawText) {
                    noteDao.insertContentItem(ContentItem(
                        noteId = noteId, type = "text",
                        content = "发言人标记\n\n" + finalDialogue, timestamp = nowStr
                    ))
                }

                // 保存结构化待办
                if (finalSummary != null && finalSummary!!.todoItems.isNotEmpty()) {
                    MeetingSummaryUtil.saveTodoItems(this@MeetingActivity, finalSummary!!.todoItems, noteId)
                }

                toast("已保存到「会议 → 会议记录」")
                Prefs(this@MeetingActivity).needsRefresh = true
                finish()
            } catch (e: Exception) { toast("保存失败: " + e.message) }
        }
    }

    // ══════════════════════════════════════════════
    //  导出 Word 纪要
    // ══════════════════════════════════════════════
    private fun exportMinutes() {
        scope.launch {
            try {
                val html = buildExportHtml()
                val exportFile = File(cacheDir, "会议纪要_${meetingTitle}_${now("yyyyMMdd_HHmm")}.docx")
                DocxWriter.write(html, exportFile)

                val uri = FileProvider.getUriForFile(this@MeetingActivity, "${packageName}.fileprovider", exportFile)
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(Intent.createChooser(shareIntent, "分享会议纪要"))
            } catch (e: Exception) { toast("导出失败: " + e.message) }
        }
    }

    private fun buildExportHtml(): String {
        val sb = StringBuilder()
        sb.append("<h1>$meetingTitle</h1>")
        sb.append("<p><b>时间：</b>${nowFull()}　<b>参会：</b>${participants.joinToString("、")}</p>")
        sb.append("<hr>")
        val s = finalSummary
        if (s != null) {
            if (s.points.isNotBlank()) { sb.append("<h2>讨论要点</h2>"); sb.append(mdToHtml(s.points)) }
            if (s.decisions.isNotBlank()) { sb.append("<h2>决议事项</h2>"); sb.append(mdToHtml(s.decisions)) }
            if (s.todos.isNotBlank()) { sb.append("<h2>待办任务</h2>"); sb.append(mdToHtml(s.todos)) }
        }
        sb.append("<h2>会议记录</h2>")
        sb.append("<p>${finalDialogue.replace("\n", "<br>")}</p>")
        return sb.toString()
    }

    /**
     * 生成 AI 摘要的完整 HTML（用于保存到笔记 WebView 渲染）
     */
    private fun buildSummaryHtml(s: MeetingSummaryUtil.Summary): String {
        val sb = StringBuilder()
        // 外层容器
        sb.append("<div style='font-family:sans-serif;line-height:1.7;color:#333;padding:8px 0;'>")

        // 标题
        sb.append("<h3 style='color:#6A1B9A;margin:0 0 12px 0;font-size:17px;'>🤖 AI 摘要</h3>")
        if (s.title.isNotBlank()) {
            sb.append("<p style='font-size:16px;font-weight:bold;color:#212121;margin:0 0 14px 0;'>${escHtml(s.title)}</p>")
        }

        // 三个分区
        if (s.points.isNotBlank()) {
            sb.append("<div style='background:#EDE7F6;border-radius:10px;padding:12px 14px;margin-bottom:10px;'>")
            sb.append("<p style='font-weight:bold;color:#455A64;margin:0 0 6px 0;font-size:13px;'>💬 讨论要点</p>")
            sb.append(mdSectionToHtml(s.points))
            sb.append("</div>")
        }
        if (s.decisions.isNotBlank()) {
            sb.append("<div style='background:#E8F5E9;border-radius:10px;padding:12px 14px;margin-bottom:10px;'>")
            sb.append("<p style='font-weight:bold;color:#455A64;margin:0 0 6px 0;font-size:13px;'>✅ 决议事项</p>")
            sb.append(mdSectionToHtml(s.decisions))
            sb.append("</div>")
        }
        if (s.todos.isNotBlank()) {
            sb.append("<div style='background:#FFF3E0;border-radius:10px;padding:12px 14px;margin-bottom:10px;'>")
            sb.append("<p style='font-weight:bold;color:#455A64;margin:0 0 6px 0;font-size:13px;'>📋 待办任务</p>")
            sb.append(mdSectionToHtml(s.todos))
            sb.append("</div>")
        }

        // 关键词
        val kw = s.keywords.trim()
        if (kw.isNotEmpty()) {
            sb.append("<div style='background:#F1F8E9;border-radius:10px;padding:12px 14px;'>")
            sb.append("<p style='font-weight:bold;color:#558B2F;margin:0 0 6px 0;font-size:13px;'>🔑 关键词</p>")
            sb.append("<p style='margin:0;'>")
            for (k in kw.split(",", "，").map { it.trim() }.filter { it.isNotEmpty() }) {
                sb.append("<span style='display:inline-block;background:#DCEDC8;color:#33691E;font-weight:bold;padding:3px 10px;border-radius:14px;margin:2px 4px;font-size:11px;'>${escHtml(k)}</span>")
            }
            sb.append("</p></div>")
        }

        sb.append("</div>")
        return sb.toString()
    }

    /** 单段 Markdown → HTML（列表 + 加粗） */
    private fun mdSectionToHtml(md: String): String {
        val sb = StringBuilder()
        val lines = md.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
        var inList = false
        for (line in lines) {
            var processed = line

            // ## 标题
            if (processed.startsWith("## ")) {
                if (inList) { sb.append("</ul>"); inList = false }
                sb.append("<p style='font-weight:bold;color:#37474F;margin:8px 0 4px 0;font-size:14px;'>${escHtml(processed.removePrefix("## "))}</p>")
                continue
            }

            // ✅ 行
            if (processed.startsWith("✅ ")) {
                if (inList) { sb.append("</ul>"); inList = false }
                sb.append("<p style='color:#2E7D32;margin:3px 0;padding-left:4px;'>✓ ${escHtml(processed.removePrefix("✅ "))}</p>")
                continue
            }

            // - [ ] 和 - [x]
            if (processed.startsWith("- [ ] ")) {
                if (inList) { sb.append("</ul>"); inList = false }
                sb.append("<p style='color:#EF6C00;margin:3px 0;padding-left:4px;'>☐ ${escHtml(processed.removePrefix("- [ ] "))}</p>")
                continue
            }
            if (processed.startsWith("- [x] ") || processed.startsWith("- [X] ")) {
                if (inList) { sb.append("</ul>"); inList = false }
                val t = processed.removePrefix("- [x] ").removePrefix("- [X] ")
                sb.append("<p style='color:#2E7D32;margin:3px 0;padding-left:4px;'>☑ ${escHtml(t)}</p>")
                continue
            }

            // - 列表
            if (processed.startsWith("- ")) {
                if (!inList) { sb.append("<ul style='margin:2px 0;padding-left:18px;'>"); inList = true }
                val item = boldToHtml(escHtml(processed.removePrefix("- ")))
                sb.append("<li style='margin:2px 0;'>$item</li>")
                continue
            }

            // 普通行
            if (inList) { sb.append("</ul>"); inList = false }
            sb.append("<p style='margin:3px 0;'>${boldToHtml(escHtml(processed))}</p>")
        }
        if (inList) sb.append("</ul>")
        return sb.toString()
    }

    /** **text** → <b>text</b> */
    private fun boldToHtml(text: String): String {
        return text.replace(Regex("""\*\*(.+?)\*\*""")) { "<b>${it.groupValues[1]}</b>" }
    }

    private fun escHtml(s: String): String {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
    }

    private fun mdToHtml(md: String): String {
        return md
            .replace(Regex("""^## (.+)$""", RegexOption.MULTILINE)) { "<h3>${it.groupValues[1]}</h3>" }
            .replace(Regex("""^- (.+)$""", RegexOption.MULTILINE)) { "<li>${it.groupValues[1]}</li>" }
            .replace(Regex("""\*\*(.+?)\*\*""")) { "<b>${it.groupValues[1]}</b>" }
    }

    // ══════════════════════════════════════════════
    //  语音条播放
    // ══════════════════════════════════════════════
    private fun showVoiceBar() {
        if (audioFile?.exists() != true) return
        voiceBar?.visibility = View.VISIBLE
        tvVoiceTime?.text = formatMs(totalMs); seekBar?.max = (totalMs / 1000).toInt()
        btnPlay?.setOnClickListener {
            if (mediaPlayer?.isPlaying == true) pausePlayback() else startPlayback()
        }
    }

    private fun startPlayback() {
        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(audioFile!!.absolutePath); prepare(); start()
                btnPlay?.text = "||"; btnPlay?.background = roundRect(C_ORANGE, 20f)
                setOnCompletionListener {
                    btnPlay?.text = "▶"; btnPlay?.background = roundRect(C_GREEN, 20f)
                    seekBar?.progress = seekBar?.max ?: 0
                }
            }
            handler.post(object : Runnable {
                override fun run() {
                    mediaPlayer?.let {
                        if (it.isPlaying) {
                            seekBar?.progress = it.currentPosition / 1000
                            tvVoiceTime?.text = formatMs(it.currentPosition * 1000L)
                        }
                    }
                    handler.postDelayed(this, 250)
                }
            })
        } catch (e: Exception) { toast("播放失败: " + e.message) }
    }

    private fun pausePlayback() {
        mediaPlayer?.pause()
        btnPlay?.text = "▶"; btnPlay?.background = roundRect(C_GREEN, 20f)
    }

    // ══════════════════════════════════════════════
    //  工具方法
    // ══════════════════════════════════════════════
    private fun makeTitle(t: String) = TextView(this).apply { text = t; textSize = 24f; setTypeface(null, Typeface.BOLD); setTextColor(C_DARK) }
    private fun makeLabel(t: String) = TextView(this).apply { text = t; textSize = 13f; setTextColor(C_DARK); setPadding(0, 0, 0, 6); setTypeface(null, Typeface.BOLD) }
    private fun makeHint(t: String) = TextView(this).apply { text = t; textSize = 11f; setTextColor(C_GRAY); setPadding(0, 0, 0, 8) }
    private fun makeInput(h: String) = EditText(this).apply { hint = h; setSingleLine(); setPadding(18, 14, 18, 14); setTextColor(C_DARK); setHintTextColor(Color.parseColor("#BDBDBD")); textSize = 14f; background = roundRect(Color.parseColor("#F0F0F0"), 12f); layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT) }
    private fun spacer(h: Int) = View(this).apply { layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, h) }
    private fun circle(c: Int) = GradientDrawable().apply { setColor(c); shape = GradientDrawable.OVAL }
    private fun roundRect(c: Int, r: Float) = GradientDrawable().apply { setColor(c); cornerRadius = r * resources.displayMetrics.density }
    private fun makeFullWidthBtn(t: String, bg: Int, click: () -> Unit) = Button(this).apply { text = t; textSize = 15f; setTextColor(C_WHITE); setAllCaps(false); setPadding(16, 14, 16, 14); setOnClickListener { click() }; typeface = Typeface.DEFAULT_BOLD; layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT); background = roundRect(bg, 12f) }
    private fun makeHButton(t: String, click: () -> Unit) = Button(this).apply { text = t; textSize = 14f; setTextColor(C_WHITE); setAllCaps(false); setPadding(16, 12, 16, 12); setOnClickListener { click() }; typeface = Typeface.DEFAULT_BOLD; layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT); background = roundRect(C_GREEN, 12f) }
    private fun makeOutlineBtn(t: String, click: () -> Unit) = Button(this).apply { text = t; textSize = 14f; setTextColor(C_BLUE); setAllCaps(false); setPadding(16, 12, 16, 12); setOnClickListener { click() }; layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT); background = GradientDrawable().apply { setStroke(2, C_BLUE); cornerRadius = 12f * resources.displayMetrics.density } }
    private fun setStatus(s: String, c: Int, done: Boolean = false) { tvStatus.text = s; tvStatus.setTextColor(c); if (done) tvStatus.setTypeface(null, Typeface.NORMAL) }
    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_SHORT).show()
    private fun timeStr() = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
    private fun now(p: String) = SimpleDateFormat(p, Locale.getDefault()).format(Date())
    private fun nowFull() = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
    private fun formatMs(ms: Long) = (ms / 60000).toString() + ":" + String.format("%02d", (ms % 60000) / 1000)

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        handler.removeCallbacksAndMessages(null)
        stopWaveformPolling()
        releaseAudioResources()
        mediaPlayer?.release()
        mediaRecorder?.apply { try { stop(); release() } catch (_: Exception) {} }
    }
}
