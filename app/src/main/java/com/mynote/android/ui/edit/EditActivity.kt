package com.mynote.android.ui.edit

import android.Manifest
import android.animation.ObjectAnimator
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.Color
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.util.Base64
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.mynote.android.R
import com.mynote.android.data.AppDatabase
import com.mynote.android.data.entity.ContentItem
import com.mynote.android.data.entity.Note
import com.mynote.android.ui.office.OfficeViewerActivity
import com.mynote.android.ui.pdf.PdfViewerActivity
import com.mynote.android.ui.viewer.ImageViewerActivity
import com.mynote.android.ui.viewer.VideoViewerActivity
import com.mynote.android.util.IatHelper
import com.mynote.android.util.QwenAsrClient
import com.mynote.android.util.TianyiAsrClient
import kotlinx.coroutines.CoroutineScope
import okhttp3.MediaType.Companion.toMediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EditActivity : AppCompatActivity() {

    private var noteId: String? = null
    private var htmlContent = ""
    private var isDirty = false
    private val ioScope = CoroutineScope(Dispatchers.IO + Job())
    private val handler = Handler(Looper.getMainLooper())
    private var autoSaveJob: Job? = null

    private lateinit var webView: WebView
    private lateinit var etTitle: EditText
    private lateinit var tvSaveStatus: TextView
    private lateinit var tvWordCount: TextView
    private lateinit var morePanel: View
    private lateinit var btnVoice: android.widget.ImageView

    private val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    private val baseDir get() = File(filesDir, "notes/$noteId")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit)
        noteId = intent.getStringExtra("noteId") ?: run { finish(); return }
        initViews()
        loadContent()
        setupAutoSave()
    }

    private fun initViews() {
        webView = findViewById(R.id.web_editor)
        etTitle = findViewById(R.id.et_title)
        tvSaveStatus = findViewById(R.id.tv_save_status)
        tvWordCount = findViewById(R.id.tv_word_count)
        morePanel = findViewById(R.id.more_panel)

        // 顶栏真玻璃背景（backdrop）
        findViewById<androidx.compose.ui.platform.ComposeView>(R.id.topbar_glass)?.setContent {
            com.mynote.android.ui.glass.GlassBarBackground()
        }

        // 更多面板 / 底部栏真玻璃背景（backdrop）
        findViewById<androidx.compose.ui.platform.ComposeView>(R.id.more_panel_glass)?.setContent {
            com.mynote.android.ui.glass.EditGlassPanel()
        }
        findViewById<androidx.compose.ui.platform.ComposeView>(R.id.bottom_bar_glass)?.setContent {
            com.mynote.android.ui.glass.EditGlassPanel(roundedTop = true)
        }

        webView.settings.apply {
            javaScriptEnabled = true; domStorageEnabled = true
            allowFileAccess = true; allowContentAccess = true
            builtInZoomControls = true; displayZoomControls = false
            loadWithOverviewMode = true; useWideViewPort = true
            // 性能优化
            setRenderPriority(android.webkit.WebSettings.RenderPriority.HIGH)
        }
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        webView.webChromeClient = WebChromeClient()
        webView.webViewClient = WebViewClient()
        webView.addJavascriptInterface(Bridge(), "Android")

        // 顶栏
        findViewById<View>(R.id.btn_back).setOnClickListener { finishWithSave() }
        findViewById<View>(R.id.btn_save).setOnClickListener { saveNote(true) }
        findViewById<View>(R.id.btn_export_pdf).setOnClickListener { showExportDialog() }
        findViewById<View>(R.id.btn_share).setOnClickListener { shareNote() }

        // 底部栏 - 4个图标
        btnVoice = findViewById(R.id.btn_voice2)
        btnVoice.setOnClickListener { toggleVoice() }
        findViewById<android.widget.ImageView>(R.id.btn_gallery).setOnClickListener { pickImage.launch("image/*") }
        findViewById<android.widget.ImageView>(R.id.btn_camera).setOnClickListener { takePhoto() }
        findViewById<android.widget.ImageView>(R.id.btn_video2).setOnClickListener { showVideoPicker() }
        findViewById<View>(R.id.btn_color).setOnClickListener { showColorPicker() }
        findViewById<View>(R.id.btn_more).setOnClickListener {
            morePanel.visibility = if (morePanel.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }

        // 格式按钮
        findViewById<TextView>(R.id.btn_bold).setOnClickListener { js("document.execCommand('bold')") }
        findViewById<TextView>(R.id.btn_italic).setOnClickListener { js("document.execCommand('italic')") }
        findViewById<TextView>(R.id.btn_underline).setOnClickListener { js("document.execCommand('underline')") }
        findViewById<TextView>(R.id.btn_undo).setOnClickListener { js("document.execCommand('undo')") }
        findViewById<TextView>(R.id.btn_redo).setOnClickListener { js("document.execCommand('redo')") }
        findViewById<TextView>(R.id.btn_font_up).setOnClickListener { adjustSize(2) }
        findViewById<TextView>(R.id.btn_font_down).setOnClickListener { adjustSize(-2) }

        // PDF / Office
        findViewById<View>(R.id.btn_pdf2).setOnClickListener { pickPdf.launch("application/pdf") }
        findViewById<View>(R.id.btn_docx2).setOnClickListener { pickDocx.launch("application/vnd.openxmlformats-officedocument.wordprocessingml.document") }

        // 排版辅助
        findViewById<View>(R.id.btn_separator).setOnClickListener { js("insertSeparator()"); markDirty() }
        findViewById<View>(R.id.btn_quote).setOnClickListener { js("insertQuote()"); markDirty() }
        findViewById<View>(R.id.btn_highlight).setOnClickListener { js("insertHighlight()"); markDirty() }
        findViewById<View>(R.id.btn_code).setOnClickListener { js("insertCodeBlock()"); markDirty() }
        findViewById<View>(R.id.btn_checklist).setOnClickListener { js("insertChecklist()"); markDirty() }
        findViewById<View>(R.id.btn_info_card).setOnClickListener { js("insertInfoCard()"); markDirty() }
        findViewById<View>(R.id.btn_warning).setOnClickListener { js("insertWarning()"); markDirty() }
        findViewById<View>(R.id.btn_numbered).setOnClickListener { showNumberedListDialog() }
        findViewById<View>(R.id.btn_page_template).setOnClickListener { showPageTemplateDialog() }
        findViewById<View>(R.id.btn_ocr).setOnClickListener { startOcrLabReport() }
        findViewById<View>(R.id.btn_immersive).setOnClickListener { toggleImmersiveMode() }
        btnImmersiveExit = findViewById(R.id.btn_exit_immersive)
        btnImmersiveExit.setOnClickListener { toggleImmersiveMode() }
        topBar = findViewById(R.id.top_bar)
        bottomBar = findViewById(R.id.bottom_bar)
        findViewById<View>(R.id.btn_xlsx2).setOnClickListener { pickXlsx.launch("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet") }
        findViewById<View>(R.id.btn_pptx2).setOnClickListener { pickPptx.launch("application/vnd.openxmlformats-officedocument.presentationml.presentation") }
        findViewById<View>(R.id.btn_template).setOnClickListener { showTemplatePicker() }
        findViewById<View>(R.id.btn_drug).setOnClickListener { showDrugPicker() }
        findViewById<View>(R.id.btn_disease).setOnClickListener { showDiseasePicker() }
        findViewById<View>(R.id.btn_ai)?.setOnClickListener { showAiAssist() }
        findViewById<View>(R.id.btn_rx)?.setOnClickListener { showRxTemplates() }
    }

    // ===== 沉浸模式 =====
    private var isImmersive = false
    private lateinit var topBar: View
    private lateinit var bottomBar: View
    private lateinit var btnImmersiveExit: View

    private fun toggleImmersiveMode() {
        isImmersive = !isImmersive
        val v = if (isImmersive) View.GONE else View.VISIBLE
        topBar.visibility = v
        etTitle.visibility = v
        bottomBar.visibility = v
        morePanel.visibility = View.GONE
        btnImmersiveExit.visibility = if (isImmersive) View.VISIBLE else View.GONE
        if (isImmersive) {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        } else {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
        }
    }

    // ===== JS 桥接 =====
    inner class Bridge {
        @JavascriptInterface
        fun click(type: String, path: String) {
            runOnUiThread { showMediaActionDialog(type, path) }
        }

        @JavascriptInterface
        fun voiceLongPress(path: String) {
            runOnUiThread { showDeleteMediaDialog("voice", path) }
        }

        @JavascriptInterface
        fun saveContent(html: String) {
            runOnUiThread { saveNoteWithHtml(html) }
        }

        @JavascriptInterface
        fun saveOnHide() {
            runOnUiThread { saveNote(true) }
        }

        @JavascriptInterface
        fun seekAudio(path: String, seekSec: Int) {
            runOnUiThread { seekVoice(path, seekSec) }
        }

        @JavascriptInterface
        fun transcribeVoice(path: String) {
            ioScope.launch {
                val file = File(path)
                if (!file.exists()) {
                    runOnUiThread { toast("录音文件不存在") }
                    return@launch
                }
                runOnUiThread {
                    js("""var el=document.querySelector('.voice-msg[data-path="${esc(path)}"]');if(el){var b=el.querySelector('.voice-transcribe-btn');if(b)b.textContent='识别中...';var c=el.querySelector('.voice-copy-btn');if(c)c.style.display='none'}""")
                }
                // 分片进度：长音频时按钮显示 "识别中(n/m)..."
                val onSeg: (Int, Int) -> Unit = { d, t ->
                    runOnUiThread {
                        js("""var el=document.querySelector('.voice-msg[data-path="${esc(path)}"]');if(el){var b=el.querySelector('.voice-transcribe-btn');if(b)b.textContent='识别中($d/$t)...'}""")
                    }
                }

                // 解码一次 → PCM 磁盘临时文件，讯飞/阿里两路共享（流式落盘防止双路解码 OOM）
                val pcmFile = try {
                    com.mynote.android.util.PcmDecoder.decodeToPcmFile(file)
                } catch (e: Exception) {
                    runOnUiThread {
                        js("""
                            (function(){
                                var el=document.querySelector('.voice-msg[data-path="${esc(path)}"]');
                                if(!el)return;
                                var b=el.querySelector('.voice-transcribe-btn');if(b)b.textContent='重试';
                                var t=el.querySelector('.voice-transcript');
                                if(t){t.textContent="解码失败: ${esc(e.message ?: "未知错误")}";t.style.display='';t.style.maxHeight=(t.scrollHeight+16)+'px';t.style.padding='8px 10px 12px 10px';t.style.color='#E53E3E'}
                            })()
                        """.trimIndent())
                    }
                    return@launch
                }

                try {
                    // 讯飞（中英）始终调用（长音频自动分片）
                    val iatJob = ioScope.async { IatHelper.transcribe(this@EditActivity, file, onSeg, pcmFile) }
                    // 天翼（方言）仅在凭证已配置时调用
                    val p = com.mynote.android.util.Prefs(this@EditActivity)
                    val tianyiOk = p.tianyiAppId.isNotEmpty() && p.tianyiApiKey.isNotEmpty()
                    val tianyiJob = if (tianyiOk) ioScope.async { TianyiAsrClient.transcribe(this@EditActivity, file) } else null
                    // 阿里云 Qwen（维语）仅在凭证已配置时调用
                    val qwenOk = p.qwenApiKey.isNotEmpty()
                    val qwenJob = if (qwenOk) ioScope.async { QwenAsrClient.transcribe(this@EditActivity, file, onSeg, pcmFile) } else null

                    // 收集每个 API 的结果和错误
                    val iatRaw = runCatching { iatJob.await() }
                    val tianyiRaw = if (tianyiJob != null) runCatching { tianyiJob.await() } else null
                    val qwenRaw = if (qwenJob != null) runCatching { qwenJob.await() } else null

                    val iatText = iatRaw.getOrNull()?.getOrNull() ?: ""
                    val tianyiText = tianyiRaw?.getOrNull()?.getOrNull() ?: ""
                    val qwenText = qwenRaw?.getOrNull()?.getOrNull() ?: ""

                    if (iatText.isEmpty() && tianyiText.isEmpty() && qwenText.isEmpty()) {
                        val errors = mutableListOf<String>()
                        val iatErr = iatRaw.getOrNull()?.exceptionOrNull() ?: iatRaw.exceptionOrNull()
                        iatErr?.let { errors.add("讯飞: ${it.message}") } ?: errors.add("讯飞: 返回空")
                        val tianyiErr = tianyiRaw?.getOrNull()?.exceptionOrNull() ?: tianyiRaw?.exceptionOrNull()
                        tianyiErr?.let { errors.add("天翼: ${it.message}") }
                        val qwenErr = qwenRaw?.getOrNull()?.exceptionOrNull() ?: qwenRaw?.exceptionOrNull()
                        qwenErr?.let { errors.add("阿里: ${it.message}") } ?: errors.add("阿里: 返回空")
                        val msg = errors.joinToString("\n")
                        runOnUiThread {
                            js("""
                                (function(){
                                    var el=document.querySelector('.voice-msg[data-path="${esc(path)}"]');
                                    if(!el)return;
                                    var b=el.querySelector('.voice-transcribe-btn');if(b)b.textContent='重试';
                                    var c=el.querySelector('.voice-copy-btn');if(c)c.style.display='none';
                                    var t=el.querySelector('.voice-transcript');
                                    if(t){t.textContent="${esc(msg)}";t.style.display='';t.style.maxHeight=(t.scrollHeight+16)+'px';t.style.padding='8px 10px 12px 10px';t.style.color='#E53E3E'}
                                })()
                            """.trimIndent())
                        }
                        return@launch
                    }

                    val merged = buildString {
                        if (iatText.isNotEmpty()) append(iatText)
                        if (tianyiText.isNotEmpty()) {
                            if (isNotEmpty()) append("\n")
                            append(tianyiText)
                        }
                        if (qwenText.isNotEmpty()) {
                            if (isNotEmpty()) append("\n")
                            append(qwenText)
                        }
                    }
                    val trimmed = merged.trim()
                    val db = AppDatabase.get(this@EditActivity)
                    noteId?.let { nid ->
                        val items = db.noteDao().getContentItems(nid)
                        val item = items.find { it.content == path }
                        if (item != null) {
                            db.noteDao().updateContentItem(item.copy(voiceTranscript = trimmed))
                        }
                    }
                    runOnUiThread {
                        js("""
                            (function(){
                                var el=document.querySelector('.voice-msg[data-path="${esc(path)}"]');
                                if(!el)return;
                                el.dataset.transcript="${esc(trimmed)}";
                                var t=el.querySelector('.voice-transcript');
                                var b=el.querySelector('.voice-transcribe-btn');
                                var c=el.querySelector('.voice-copy-btn');
                                if(t){t.textContent="${esc(trimmed)}";t.style.display='';t.style.maxHeight=(t.scrollHeight+16)+'px';t.style.padding='8px 10px 12px 10px'}
                                if(b)b.textContent='收起'
                                if(c)c.style.display='inline-block'
                            })()
                        """.trimIndent())
                    }
                } finally {
                    // 共享 PCM 临时文件用完即删
                    pcmFile.delete()
                }
            }
        }
    }

    private fun showMediaActionDialog(type: String, path: String) {
        val label = when (type) {
            "image" -> "图片"; "video" -> "视频"; "voice" -> "录音"
            "pdf" -> "PDF"
            "docx" -> "Word"; "xlsx" -> "Excel"; "pptx" -> "PPT"
            else -> "文件"
        }
        if (type == "voice") {
            showVoiceActionDialog(path)
            return
        }
        val items = if (type == "image") {
            arrayOf("查看", "删除")
        } else {
            arrayOf("查看", "删除")
        }
        AlertDialog.Builder(this, R.style.GlassDialog)
            .setTitle(label)
            .setItems(items) { _, which ->
                when (which) {
                    0 -> openMedia(type, path)
                    1 -> showDeleteMediaDialog(type, path)
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showVoiceActionDialog(path: String) {
        val isPlaying = path == playingPath && player?.isPlaying == true
        val playLabel = if (isPlaying) "暂停" else "播放"

        val container = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(0, 8.dp, 0, 8.dp)
        }

        fun addButton(label: String, color: Int, onClick: () -> Unit) {
            val btn = TextView(this).apply {
                text = label
                textSize = 16f
                gravity = android.view.Gravity.CENTER
                setPadding(0, 14.dp, 0, 14.dp)
                setTextColor(color)
                setBackgroundResource(android.R.drawable.list_selector_background)
                setOnClickListener { onClick() }
            }
            container.addView(btn)
        }

        val dialog = AlertDialog.Builder(this, R.style.GlassDialog)
            .setTitle("录音")
            .setView(container)
            .setNegativeButton("取消", null)
            .create()

        addButton(playLabel, Color.parseColor("#333333")) {
            openMedia("voice", path)
            dialog.dismiss()
        }
        addButton("分享", Color.parseColor("#1976D2")) {
            dialog.dismiss()
            shareVoice(path)
        }
        addButton("删除", Color.parseColor("#E53935")) {
            dialog.dismiss()
            showDeleteMediaDialog("voice", path)
        }

        dialog.show()
    }

    private fun shareVoice(path: String) {
        val file = File(path)
        if (!file.exists()) { toast("文件不存在"); return }
        val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "audio/mp4"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, "分享录音"))
    }

    private val Int.dp: Int get() = (this * resources.displayMetrics.density).toInt()

    private fun openMedia(type: String, path: String) {
        val file = File(path)
        if (!file.exists()) { toast("文件不存在"); return }
        when (type) {
            "image" -> start(ImageViewerActivity::class.java, "image_path" to path)
            "video" -> start(VideoViewerActivity::class.java, "video_path" to path)
            "voice" -> {
                playVoice(path)
            }
            "pdf", "docx", "xlsx", "pptx" -> {
                AlertDialog.Builder(this, R.style.GlassDialog)
                    .setTitle("选择查看器")
                    .setItems(arrayOf("内置查看器", "系统查看器")) { _, w ->
                        when (w) {
                            0 -> when (type) {
                                "pdf" -> start(PdfViewerActivity::class.java, "pdf_path" to path)
                                else -> start(OfficeViewerActivity::class.java, "office_path" to path)
                            }
                            1 -> {
                                try {
                                    val uri = FileProvider.getUriForFile(this@EditActivity, "$packageName.fileprovider", File(path))
                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                        setDataAndType(uri, when (type) {
                                            "pdf" -> "application/pdf"
                                            "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                                            "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                                            "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
                                            else -> "*/*"
                                        })
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    startActivity(intent)
                                } catch (_: Exception) { toast("没有可用的系统查看器") }
                            }
                        }
                    }
                    .setNegativeButton("取消", null)
                    .show()
            }
        }
    }

    private fun showDeleteMediaDialog(type: String, path: String) {
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
            hint = "安全码"
            setPadding(32, 16, 32, 16)
            textSize = 18f
            gravity = Gravity.CENTER
            setSingleLine()
            maxWidth = 200.dp
            background = null
            transformationMethod = android.text.method.PasswordTransformationMethod.getInstance()
        }
        val wrap = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(16.dp, 16.dp, 16.dp, 4.dp)
            addView(input)
        }
        val dialog = AlertDialog.Builder(this, R.style.GlassDialog)
            .setView(wrap)
            .create()
        input.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                if (s.toString() == getSecurityCode()) {
                    dialog.dismiss()
                    deleteMediaFromEditor(type, path)
                }
            }
        })
        dialog.show()
        dialog.window?.setLayout(200.dp, android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    private fun deleteMediaFromEditor(type: String, path: String) {
        val selector = "[data-type=\"${esc(type)}\"][data-path=\"${esc(path)}\"]"
        js("""skipRestore=true;var el=document.querySelector('$selector');if(el){el.remove()}setTimeout(function(){skipRestore=false;saveProtected()},200);""")
        markDirty()
    }

    private fun getSecurityCode(): String {
        return getSharedPreferences("mynote_prefs", MODE_PRIVATE)
            .getString("delete_security_code", "737") ?: "737"
    }

    private fun <T : AppCompatActivity> start(cls: Class<T>, vararg extras: Pair<String, String>) {
        startActivity(Intent(this, cls).apply { extras.forEach { putExtra(it.first, it.second) } })
    }
    private fun js(code: String) = webView.evaluateJavascript(code, null)
    private fun jsResult(code: String, cb: (String) -> Unit) {
        webView.evaluateJavascript(code) { s ->
            val raw = s?.trim('"') ?: ""
            val decoded = raw
                .replace("\\\\", "\\")
                .replace("\\\"", "\"")
                .replace("\\n", "\n")
                .replace("\\t", "\t")
                .replace("\\r", "\r")
                .replace("\\/", "/")
            cb(decoded)
        }
    }
    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    // ===== 颜色选择器（小圆球均匀分布） =====
    private fun showColorPicker() {
        val colors = listOf(
            "#000000","#333333","#D32F2F","#E65100","#F9A825",
            "#388E3C","#1565C0","#6A1B9A","#00838F","#E91E63",
            "#FFFFFF","#F44336","#FF9800","#FFEB3B","#4CAF50",
            "#2196F3","#9C27B0","#00BCD4","#795548","#607D8B"
        )
        val root = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(8, 12, 8, 8)
        }
        // 模式切换
        var isForeground = true
        val modeRow = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL; gravity = android.view.Gravity.CENTER
        }
        val tvFore = android.widget.TextView(this).apply {
            text = "✏️文字"; textSize = 14f; setTextColor(Color.parseColor("#1565C0"))
            setPadding(24, 8, 24, 8)
            setBackgroundColor(Color.parseColor("#E3F2FD"))
        }
        val tvBack = android.widget.TextView(this).apply {
            text = "🖍背景"; textSize = 14f; setTextColor(Color.parseColor("#757575"))
            setPadding(24, 8, 24, 8)
        }
        tvFore.setOnClickListener {
            isForeground = true
            tvFore.setBackgroundColor(Color.parseColor("#E3F2FD")); tvFore.setTextColor(Color.parseColor("#1565C0"))
            tvBack.setBackgroundColor(0); tvBack.setTextColor(Color.parseColor("#757575"))
        }
        tvBack.setOnClickListener {
            isForeground = false
            tvBack.setBackgroundColor(Color.parseColor("#E3F2FD")); tvBack.setTextColor(Color.parseColor("#1565C0"))
            tvFore.setBackgroundColor(0); tvFore.setTextColor(Color.parseColor("#757575"))
        }
        modeRow.addView(tvFore); modeRow.addView(tvBack)
        root.addView(modeRow)
        // 颜色网格 4x5
        for (row in 0 until 5) {
            val rowLayout = android.widget.LinearLayout(this).apply {
                orientation = android.widget.LinearLayout.HORIZONTAL; gravity = android.view.Gravity.CENTER
            }
            for (col in 0 until 4) {
                val idx = row * 4 + col
                val size = 36.dp
                val v = android.view.View(this).apply {
                    layoutParams = android.widget.LinearLayout.LayoutParams(size, size).apply { setMargins(7, 7, 7, 7) }
                    val drawable = android.graphics.drawable.GradientDrawable().apply {
                        shape = android.graphics.drawable.GradientDrawable.OVAL
                        setColor(Color.parseColor(colors[idx]))
                        setStroke(1, Color.parseColor("#E0E0E0"))
                    }
                    background = drawable
                    setOnClickListener {
                        val cmd = if (isForeground) "foreColor" else "hiliteColor"
                        js("document.execCommand('$cmd',false,'${colors[idx]}')")
                    }
                }
                rowLayout.addView(v)
            }
            root.addView(rowLayout)
        }
        // 清除颜色按钮
        val clearBtn = android.widget.TextView(this).apply {
            text = "清除颜色"; textSize = 13f; setTextColor(Color.parseColor("#D32F2F"))
            gravity = android.view.Gravity.CENTER; setPadding(0, 10, 0, 4)
            setOnClickListener { js("document.execCommand('removeFormat')") }
        }
        root.addView(clearBtn)
        AlertDialog.Builder(this, R.style.GlassDialog).setTitle("颜色").setView(root)
            .setPositiveButton("关闭", null).show()
    }

    // ===== 字号调整（使用 execCommand fontSize 1-7） =====
    private var currentFontSize = 3 // 1-7, 3是默认
    private fun adjustSize(delta: Int) {
        currentFontSize = (currentFontSize + delta).coerceIn(1, 7)
        js("document.execCommand('fontSize',false,'$currentFontSize')")
    }

    private fun showTemplatePicker() {
        val deptTemps = com.mynote.android.util.DeptTemplates.getAll()
        val deptGroups = deptTemps.groupBy { it.title.substringBefore("-") }
        val cats = mutableListOf("通用模板（80+模板）")
        cats.addAll(deptGroups.keys.map { "${it}·专科" })

        AlertDialog.Builder(this, R.style.GlassDialog).setTitle("选择病历模板").setItems(cats.toTypedArray()) { _, ci ->
            if (ci == 0) {
                showGenericTemplates()
            } else {
                val dept = cats[ci].removeSuffix("·专科")
                val group = deptGroups[dept] ?: return@setItems
                val items = group.map { it.title }.toTypedArray()
                AlertDialog.Builder(this, R.style.GlassDialog).setTitle("${dept}·专科")
                    .setItems(items) { _, i -> insertTemplate(group[i].title, group[i].content) }
                    .setNegativeButton("返回") { _, _ -> showTemplatePicker() }
                    .show()
            }
        }.setNegativeButton("返回", null).show()
    }

    private fun showGenericTemplates() {
        val all = com.mynote.android.util.TemplateManager.getAll()
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(20, 12, 20, 0) }
        val et = EditText(this).apply { hint = "搜索模板 (如: 入院、手术、病程...)"; setSingleLine(true); textSize = 14f }
        root.addView(et)
        val tv = TextView(this).apply { text = "共${all.size}个模板"; textSize = 11f; setTextColor(Color.GRAY); setPadding(0, 6, 0, 0) }
        root.addView(tv)

        fun showFiltered(filtered: List<com.mynote.android.util.TemplateManager.Template>) {
            AlertDialog.Builder(this, R.style.GlassDialog).setTitle("通用模板").setView(root as android.view.View)
                .setItems(filtered.map { t: com.mynote.android.util.TemplateManager.Template -> t.title }.toTypedArray()) { _, i -> insertTemplate(filtered[i].title, filtered[i].content) }
                .setNegativeButton("返回") { _, _ -> showTemplatePicker() }
                .show()
        }
        showFiltered(all)

        et.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) {
                val q = s.toString().trim().lowercase()
                val filtered = if (q.isEmpty()) all else all.filter { it.title.lowercase().contains(q) || it.content.lowercase().contains(q) }
                tv.text = "找到${filtered.size}个 (共${all.size})"
                showFiltered(filtered.ifEmpty { all })
            }
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
        })
    }

    private fun insertTemplate(title: String, content: String) {
        val lines = content.split("\n").joinToString("") { line ->
            val c = if (line.isBlank()) "<br>" else esc(line)
            "<div>$c</div>"
        }
        val html = "<div style='font-family:monospace;font-size:14px;line-height:1.6;background:#FFF3E0;padding:12px 14px;border-radius:12px;margin:8px 0;border-left:4px solid #E65100;word-wrap:break-word;overflow-wrap:break-word;max-width:100%'>$lines</div><div><br></div>"
        js("document.execCommand('insertHTML',false,'${esc(html)}');")
        markDirty()
        Toast.makeText(this, "已插入：$title", Toast.LENGTH_SHORT).show()
    }

    private fun showPageTemplateDialog() {
        val items = arrayOf("空白", "横线本", "点阵本", "暗色底", "护眼绿")
        AlertDialog.Builder(this, R.style.GlassDialog)
            .setTitle("选择页面模板")
            .setItems(items) { _, which ->
                val style = arrayOf("blank", "lined", "dot", "dark", "green")[which]
                js("setPageTemplate('$style')")
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showNumberedListDialog() {
        val items = arrayOf("1-3 项", "1-5 项", "1-7 项", "1-10 项")
        val counts = intArrayOf(3, 5, 7, 10)
        AlertDialog.Builder(this, R.style.GlassDialog)
            .setTitle("插入编号")
            .setItems(items) { _, which ->
                js("insertNumberedList(${counts[which]})")
                markDirty()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showDrugPicker() {
        val drugs = com.mynote.android.util.DrugReference.getAll()
        val groups = com.mynote.android.util.DrugReference.groups

        fun showCategoryDialog() {
            AlertDialog.Builder(this, R.style.GlassDialog).setTitle("常用药速查 — 按分类")
                .setItems(groups.mapIndexed { i, g -> "${i + 1}. $g" }.toTypedArray()) { _, gi ->
                    val group = groups[gi]
                    val groupDrugs = drugs.filter { it.group == group }
                    val items = groupDrugs.mapIndexed { i, d ->
                        "${i + 1}. ${d.name}     ${d.dosage}"
                    }.toTypedArray()
                    AlertDialog.Builder(this, R.style.GlassDialog).setTitle("$group (${groupDrugs.size}种)")
                        .setItems(items) { _, i -> showDrugDetail(groupDrugs[i]) }
                        .setNegativeButton("返回") { _, _ -> showCategoryDialog() }
                        .show()
                }
                .setNeutralButton("🔍 搜索") { _, _ -> showDrugSearch() }
                .setNegativeButton("返回", null)
                .show()
        }

        showCategoryDialog()
    }

    private fun showDrugSearch() {
        val input = android.widget.EditText(this).apply {
            hint = "输入药品名称关键词"
            setSelectAllOnFocus(true)
        }
        AlertDialog.Builder(this, R.style.GlassDialog).setTitle("搜索药品")
            .setView(input)
            .setPositiveButton("搜索") { _, _ ->
                val kw = input.text.toString().trim().takeIf { it.isNotEmpty() } ?: return@setPositiveButton
                val all = com.mynote.android.util.DrugReference.getAll()
                val results = all.filter { it.name.contains(kw, ignoreCase = true) }
                if (results.isEmpty()) {
                    Toast.makeText(this, "未找到匹配药品", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val items = results.mapIndexed { i, d ->
                    "${i + 1}. ${d.name}  |  ${d.dosage}"
                }.toTypedArray()
                AlertDialog.Builder(this, R.style.GlassDialog)
                    .setTitle("搜索结果: ${results.size} 种")
                    .setItems(items) { _, i -> showDrugDetail(results[i]) }
                    .setNegativeButton("关闭", null).show()
            }
            .setNegativeButton("取消", null).show()
    }

    private fun showDrugDetail(d: com.mynote.android.util.DrugReference.Drug) {
        AlertDialog.Builder(this, R.style.GlassDialog)
            .setTitle(d.name)
            .setMessage("【用法用量】${d.dosage}\n【适应症】${d.indications}\n【禁忌】${d.contraindications}\n【注意】${d.caution}")
            .setPositiveButton("插入笔记") { _, _ ->
                val text = "【${d.name}】\n用法用量：${d.dosage}\n适应症：${d.indications}\n禁忌：${d.contraindications}\n注意：${d.caution}"
                val html = "<div style='white-space:pre-wrap;font-family:sans-serif;font-size:13px;line-height:1.6;background:#E3F2FD;padding:10px 12px;border-radius:12px;margin:6px 0;border-left:4px solid #1565C0'>${text.replace("\n","<br>")}</div><br>"
                js("document.execCommand('insertHTML',false,'${esc(html)}');")
                markDirty()
                Toast.makeText(this@EditActivity, "已插入: ${d.name}", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消", null).show()
    }

    // ===== 导出 ====
    private fun safeFileName(name: String): String {
        return name.replace(Regex("[/\\\\:*?\"<>|]"), "_").trim()
    }

    private fun getCurrentHtml(): String {
        return htmlContent
    }

    private fun shareNote() {
        val title = safeFileName(etTitle.text.toString().trim().ifEmpty { "笔记" })
        val items = arrayOf("TXT (纯文本)", "Markdown (.md)")
        AlertDialog.Builder(this, R.style.GlassDialog)
            .setTitle("分享 $title")
            .setItems(items) { _, which ->
                val ext = if (which == 0) "txt" else "md"
                val converter = if (which == 0)
                    { h: String -> com.mynote.android.util.HtmlConverter.toText(h) }
                else
                    { h: String -> com.mynote.android.util.HtmlConverter.toMarkdown(h) }
                doShare(title, ext, converter)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun doShare(title: String, ext: String, converter: (String) -> String) {
        try {
            val html = getCurrentHtml()
            val text = converter(html)
            val file = File(cacheDir, "share_${title}.${ext}")
            // 写 BOM 头，防止微信/QQ 等 App 按 GBK 解码导致乱码
            file.outputStream().use { out ->
                out.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
                out.write(text.toByteArray(Charsets.UTF_8))
            }

            val uri = androidx.core.content.FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, title)
                putExtra(Intent.EXTRA_TEXT, text)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, "分享 $title"))
        } catch (e: Exception) {
            Toast.makeText(this, "分享失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showExportDialog() {
        val title = safeFileName(etTitle.text.toString().trim().ifEmpty { "笔记" })
        val items = arrayOf("PDF (打印/另存)", "Word (.docx)", "TXT (纯文本)", "Markdown (.md)")
        AlertDialog.Builder(this, R.style.GlassDialog)
            .setTitle("导出 $title")
            .setItems(items) { _, which ->
                when (which) {
                    0 -> { exportPdfLauncher.launch("${title}.pdf") }
                    1 -> { showExportPreview("docx", title) { exportDocxLauncher.launch("${title}.docx") } }
                    2 -> { showExportPreview("txt", title) { exportTxtLauncher.launch("${title}.txt") } }
                    3 -> { showExportPreview("md", title) { exportMdLauncher.launch("${title}.md") } }
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showExportPreview(format: String, title: String, onExport: () -> Unit) {
        val html = getCurrentHtml()
        val preview = when (format) {
            "txt" -> com.mynote.android.util.HtmlConverter.toText(html)
            "md" -> com.mynote.android.util.HtmlConverter.toMarkdown(html)
            else -> "[${format.uppercase()}格式] 将以 $title.$format 导出"
        }
        val maxPreview = if (preview.length > 1500) preview.take(1500) + "\n\n... (共 ${preview.length} 字符)" else preview
        val scrollView = android.widget.ScrollView(this).apply {
            setPadding(32, 16, 32, 16)
        }
        val tv = TextView(this).apply {
            text = maxPreview; textSize = 13f
            setTextColor(resources.getColor(com.mynote.android.R.color.text_primary, null))
            setLineSpacing(4f, 1f)
        }
        scrollView.addView(tv)
        AlertDialog.Builder(this, R.style.GlassDialog)
            .setTitle("预览: $title.$format")
            .setView(scrollView, 32, 16, 32, 0)
            .setPositiveButton("导出") { _, _ -> onExport() }
            .setNegativeButton("关闭", null)
            .show()
    }

    private fun writeExportFile(uri: android.net.Uri, converter: () -> String) {
        try {
            val text = converter()
            contentResolver.openOutputStream(uri)?.use { it.write(text.toByteArray(Charsets.UTF_8)) }
            Toast.makeText(this, "导出完成", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "导出失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun writeDocxExport(uri: android.net.Uri) {
        try {
            val tmp = File(cacheDir, "export_${System.currentTimeMillis()}.docx")
            com.mynote.android.util.DocxWriter.write(getCurrentHtml(), tmp)
            val out = contentResolver.openOutputStream(uri)
            if (out == null) {
                tmp.delete()
                Toast.makeText(this, "导出失败: 无法写入文件", Toast.LENGTH_SHORT).show()
                return
            }
            out.use { tmp.inputStream().use { it.copyTo(out) } }
            tmp.delete()
            Toast.makeText(this, "导出完成", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "导出失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun exportToPdf(uri: android.net.Uri) {
        try {
            val picture = webView.capturePicture()
            if (picture == null) {
                Toast.makeText(this, "PDF导出失败: 无法捕获页面内容", Toast.LENGTH_SHORT).show()
                return
            }
            val pageWidth = 595   // A4 72dpi
            val pageHeight = 842
            val scale = pageWidth.toFloat() / picture.width
            val scaledHeight = picture.height * scale
            var yOffset = 0f
            var pageNumber = 1
            val pdf = android.graphics.pdf.PdfDocument()
            while (yOffset < scaledHeight) {
                val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                val page = pdf.startPage(pageInfo)
                val canvas = page.canvas
                canvas.save()
                canvas.scale(scale, scale)
                canvas.translate(0f, -yOffset / scale)
                picture.draw(canvas)
                canvas.restore()
                pdf.finishPage(page)
                yOffset += pageHeight
                pageNumber++
            }
            contentResolver.openOutputStream(uri)?.use { pdf.writeTo(it) }
            pdf.close()
            Toast.makeText(this, "PDF导出完成", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "PDF导出失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // ===== 疾病速查 =====
    private fun showDiseasePicker() {
        val diseases = com.mynote.android.util.DiseaseReference.getAll()
        val depts = diseases.map { it.department }.distinct()

        fun showDeptDialog() {
            AlertDialog.Builder(this, R.style.GlassDialog).setTitle("疾病速查 — 按科室")
                .setItems(depts.mapIndexed { i, d -> "${i + 1}. $d" }.toTypedArray()) { _, di ->
                    val dept = depts[di]
                    val deptDiseases = diseases.filter { it.department == dept }
                    val items = deptDiseases.mapIndexed { i, d -> "${i + 1}. ${d.name}" }.toTypedArray()
                    AlertDialog.Builder(this, R.style.GlassDialog).setTitle("$dept (${deptDiseases.size}种)")
                        .setItems(items) { _, i ->
                            val d = deptDiseases[i]
                            AlertDialog.Builder(this, R.style.GlassDialog)
                                .setTitle(d.name)
                                .setMessage("【核心症状】${d.symptoms}\n【鉴别诊断】${d.differential}\n【常用药物】${d.drugs}\n【治疗手段】${d.treatment}")
                                .setPositiveButton("插入笔记") { _, _ ->
                                    val text = "【${d.name}】\n症状：${d.symptoms}\n鉴别：${d.differential}\n用药：${d.drugs}\n治疗：${d.treatment}"
                                    val html = "<div style='white-space:pre-wrap;font-family:sans-serif;font-size:13px;line-height:1.6;background:#E8F5E9;padding:10px 12px;border-radius:12px;margin:6px 0;border-left:4px solid #2E7D32'>${text.replace("\n","<br>")}</div><br>"
                                    js("document.execCommand('insertHTML',false,'${esc(html)}');")
                                    markDirty()
                                    Toast.makeText(this@EditActivity, "已插入: ${d.name}", Toast.LENGTH_SHORT).show()
                                }
                                .setNeutralButton("插入首次病程") { _, _ ->
                                    insertDiseaseFCR(d)
                                }
                                .setNegativeButton("取消", null).show()
                        }
                        .setNegativeButton("返回") { _, _ -> showDeptDialog() }
                        .show()
                }
                .setNegativeButton("返回", null)
                .show()
        }

        showDeptDialog()
    }

    private fun insertDiseaseFCR(d: com.mynote.android.util.DiseaseReference.Disease) {
        val fcr = """
========== 首次病程记录 ==========
记录时间：20____年____月____日____时____分

【病例特点】
诊断：${d.name}
核心症状：${d.symptoms}
相关科室：${d.department}

主诉（请填写）：因"_________________________________"入院。
起病情况（诱因/时间/急缓）：___________________________________________

重要既往史：
□高血压 □糖尿病 □冠心病 □脑卒中 □手术史________　□过敏史________

查体要点：
生命体征：T____℃　P____次/分　BP____/____mmHg　SpO₂____%
专科查体重点：_________________________________________________________
关键阴性体征（排除鉴别诊断）：_______________________________________

【拟诊讨论】
初步诊断：${d.name}
诊断依据：(1) 临床症状：${d.symptoms}
　　　　　(2) 查体发现：______________________________________________
　　　　　(3) 辅助检查：______________________________________________

鉴别诊断及排除依据：
${d.differential.split("、").joinToString("\n") { "　□ ${it.trim()} — 排除/证实依据：________________" }}

【诊疗计划】
（一）检查计划：
□血常规 □肝肾功能+电解质 □凝血功能 □心电图 □胸片
针对本病需重点检查：__________________________________________________

（二）药物治疗方案：
${d.drugs.split("；").joinToString("\n") { "　○ ${it.trim()}" }}
需补充/个体化调整：___________________________________________________

（三）其他治疗措施：
${d.treatment.split("、").joinToString("\n") { "　○ ${it.trim()}" }}

（四）护理级别：□一级 □二级　饮食：__________　监护：□无 □心电+SpO₂
（五）已向患方告知病情及诊疗计划　□是　□待沟通

住院医师：_______________
""".trimIndent()

        val html = "<div style='white-space:pre-wrap;font-family:monospace;font-size:13px;line-height:1.7;background:#FFF8E1;padding:14px 16px;border-radius:12px;margin:8px 0;border-left:5px solid #F57C00'>${fcr.replace("\n","<br>")}</div><br>"
        js("document.execCommand('insertHTML',false,'${esc(html)}');")
        markDirty()
        Toast.makeText(this, "已插入「${d.name}」首次病程", Toast.LENGTH_SHORT).show()
    }

    // ===== 媒体插入 =====
    private fun insertImage(path: String) {
        val bmp = BitmapFactory.decodeFile(path)
        if (bmp == null) { toast("加载图片失败"); return }
        val bos = ByteArrayOutputStream()
        bmp.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, bos); bmp.recycle()
        val b64 = Base64.encodeToString(bos.toByteArray(), Base64.NO_WRAP)
        val html = withBr("<div contenteditable=\"false\" class=\"media\" data-type=\"image\" data-path=\"" + esc(path) + "\"><img src=\"data:image/jpeg;base64," + b64 + "\"/>" + timeLabel() + "</div>")
        js("document.execCommand('insertHTML',false,'" + esc(html) + "');bindMedia()")
        markDirty()
    }

    private fun insertVideo(path: String) {
        // 提取视频缩略图
        val thumb = try {
            val retriever = android.media.MediaMetadataRetriever()
            retriever.setDataSource(path)
            val frame = retriever.frameAtTime
            retriever.release()
            if (frame != null) {
                val bos = ByteArrayOutputStream()
                frame.compress(android.graphics.Bitmap.CompressFormat.JPEG, 70, bos); frame.recycle()
                "data:image/jpeg;base64," + Base64.encodeToString(bos.toByteArray(), Base64.NO_WRAP)
            } else null
        } catch (_: Exception) { null }

        // 读取时长
        val durMs = try {
            val retriever = android.media.MediaMetadataRetriever()
            retriever.setDataSource(path)
            val d = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0
            retriever.release(); d
        } catch (_: Exception) { 0L }
        val durMin = durMs / 1000 / 60
        val durSec = durMs / 1000 % 60
        val durStr = durMin.toString() + ":" + if (durSec < 10) "0$durSec" else durSec.toString()

        val imgHtml = if (thumb != null) {
            """<img src="$thumb" style="width:100%;height:auto;border-radius:6px"/>"""
        } else """<div style="width:100%;height:120px;background:#333;border-radius:6px;display:flex;align-items:center;justify-content:center;color:#fff;font-size:32px">🎬</div>"""

        val html = withBr("<div contenteditable=\"false\" class=\"media\" data-type=\"video\" data-path=\"" + esc(path) + "\" style=\"position:relative;display:inline-block;max-width:100%;cursor:pointer\">" +
            imgHtml +
            "<div style=\"position:absolute;top:50%;left:50%;transform:translate(-50%,-50%);width:48px;height:48px;background:rgba(244,67,54,0.85);border-radius:50%;display:flex;align-items:center;justify-content:center\">" +
            "<div style=\"width:0;height:0;border-left:16px solid #fff;border-top:10px solid transparent;border-bottom:10px solid transparent;margin-left:4px\"></div>" +
            "</div>" +
            "<div style=\"position:absolute;bottom:6px;right:6px;background:rgba(0,0,0,0.6);color:#fff;font-size:10px;padding:2px 6px;border-radius:3px\">" + durStr + "</div>" +
            timeLabel() + "</div>")
        js("document.execCommand('insertHTML',false,'" + esc(html) + "');bindMedia()")
        markDirty()
    }
    private fun insertAudio(path: String, tsJson: String = "") {
        // 语音条长度随秒数变化，短录音细长
        val dur = getAudioDuration(path)
        val sec = dur.toIntOrNull() ?: 1
        val px = (50 + sec * 8).coerceIn(50, 350)
        val timeFont = if (sec <= 10) "5px" else "8px"
        val tsAttr = if (tsJson.isNotEmpty()) " data-ts='${esc(tsJson)}'" else ""
        val html = withBr("<div contenteditable=\"false\" class=\"voice-msg\" data-type=\"voice\" data-path=\"" + esc(path) + "\"$tsAttr data-transcript=\"\" style=\"display:block;margin:6px 0\">" +
            "<span class=\"voice-playing-tip\" style=\"display:none;font-size:11px;color:#07C160;white-space:nowrap\">正在播放</span>" +
            "<span class=\"voice-body\" style=\"display:flex;align-items:center;gap:6px;flex-wrap:nowrap;background:linear-gradient(135deg,rgba(220,238,255,0.85),rgba(140,190,240,0.45));border-radius:8px;border:1px solid rgba(180,210,240,0.5);box-shadow:inset 0 1px 0 rgba(255,255,255,0.7),0 1px 4px rgba(30,80,160,0.08);padding:5px 10px;cursor:pointer;width:${px}px;max-width:49vw;height:30px;box-sizing:border-box;overflow:hidden\">" +
            "<span class=\"vdur\" style=\"font-size:12px;color:#333;font-weight:500;line-height:1;flex-shrink:0\">" + dur + "\u2033</span>" +
            "<span style=\"font-size:${timeFont};color:#aaa;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;flex-shrink:1;min-width:0;text-align:right\">" + (if (sec <= 10) nowDateShort() else nowTime()) + "</span>" +
            "</span>" +
            "<span class=\"voice-transcribe-btn\" style=\"display:inline-block;font-size:10px;color:#576B95;cursor:pointer;margin-top:2px\">转文字</span>" +
            "<span class=\"voice-copy-btn\" style=\"display:none;font-size:10px;color:#576B95;cursor:pointer;margin-left:8px;margin-top:2px\">复制</span>" +
            "<div class=\"voice-transcript\" style=\"display:none;max-height:0;overflow:hidden;transition:max-height 0.3s ease;font-size:13px;color:#333;background:#F5F5F5;border-radius:6px;padding:0 10px;margin-top:4px;line-height:1.6\"></div>" +
            // 时间戳标记
            tsMarkers(tsJson) +
            "</div>")
        js("document.execCommand('insertHTML',false,'" + esc(html) + "');bindMedia()")
        markDirty()
    }

    /** 生成时间戳标记 HTML：可点击的蓝色小块 */
    private fun tsMarkers(tsJson: String): String {
        if (tsJson.isEmpty()) return ""
        val sb = StringBuilder()
        sb.append("<div style='margin-top:6px;display:flex;flex-wrap:wrap;gap:4px'>")
        try {
            val arr = org.json.JSONArray(tsJson)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val t = obj.getLong("t") / 1000
                val txt = obj.optString("txt", "").take(20)
                val ts = "${t}s"
                sb.append("<span class='ts-marker' data-seek='$t' style='font-size:10px;background:#E8F5E9;color:#2E7D32;padding:2px 6px;border-radius:4px;cursor:pointer'>$ts</span>")
            }
        } catch (_: Exception) {}
        sb.append("</div>")
        return sb.toString()
    }

    private fun getAudioDuration(path: String): String {
        return try {
            val mp = MediaPlayer()
            mp.setDataSource(path); mp.prepare()
            val sec = mp.duration / 1000; mp.release()
            sec.toString()
        } catch (_: Exception) { "0" }
    }
    private fun insertPdf(path: String) = insertCard("pdf", path, "📄", "PDF")

    private fun insertCard(type: String, path: String, icon: String, label: String) {
        val name = path.substringAfterLast("/")
        val html = withBr("<div contenteditable=\"false\" class=\"media-card\" data-type=\"" + type + "\" data-path=\"" + esc(path) + "\"><span class=\"icon\">" + icon + "</span><div><div class=\"label\">" + label + "</div><div class=\"name\">" + esc(name) + "</div></div>" + timeLabel() + "</div>")
        js("document.execCommand('insertHTML',false,'" + esc(html) + "');bindMedia()")
        markDirty()
    }

    private fun insertDoc(path: String, type: String) {
        val icon = when (type) { "docx" -> "📝"; "xlsx" -> "📊"; "pptx" -> "📈"; else -> "📁" }
        val label = when (type) { "docx" -> "Word"; "xlsx" -> "Excel"; "pptx" -> "PPT"; else -> "文档" }
        insertCard(type, path, icon, label)
    }

    // ===== 保存（从 JS Bridge 调用，避免 JSON 转义） =====
    private fun saveNoteWithHtml(html: String) {
        htmlContent = html
        val title = etTitle.text.toString().trim().ifEmpty { "未命名笔记" }
        ioScope.launch {
            val db = AppDatabase.get(this@EditActivity)
            val dao = db.noteDao()
            val note = dao.getNote(noteId!!)
            if (note != null) {
                dao.updateNote(note.copy(title = title))
                dao.deleteContentItemsByNote(noteId!!)
                dao.insertContentItem(ContentItem(
                    noteId = noteId!!, type = "html", content = html,
                    timestamp = timeFormat.format(Date())
                ))
            } else {
                // 从小组件新建时，note 尚不存在 → 自动创建
                val firstParent = db.categoryDao().getParentCategories().firstOrNull()
                val firstSub = if (firstParent != null) {
                    db.categoryDao().getSubCategories(firstParent.id).firstOrNull()
                } else null
                val subId = firstSub?.id ?: "default"
                val now = timeFormat.format(Date())
                dao.insertNote(Note(
                    id = noteId!!, subCategoryId = subId, title = title,
                    contentText = "", updateTime = now, createTime = now
                ))
                dao.insertContentItem(ContentItem(
                    noteId = noteId!!, type = "html", content = html,
                    timestamp = now
                ))
            }
            isDirty = false
            runOnUiThread {
                tvSaveStatus.text = "● 已保存"
                updateWordCount()
                try { com.mynote.android.ui.widget.MyNoteWidget.refreshAll(this@EditActivity) } catch (_: Exception) {}
                // 自动备份到云
                val sp = com.mynote.android.util.Prefs(this@EditActivity)
                if (sp.backupAutoBackup && com.mynote.android.util.WebDAVBackup.isConfigured(this@EditActivity)) {
                    ioScope.launch { com.mynote.android.util.WebDAVBackup.uploadBackup(this@EditActivity) }
                }
            }
        }
    }

    // ===== 加载 / 保存（通过 Bridge 避免 JSON 转义） =====
    private fun loadContent() {
        ioScope.launch {
            val dao = AppDatabase.get(this@EditActivity).noteDao()
            val note = dao.getNote(noteId!!)
            val items = dao.getContentItems(noteId!!)
            val html = items.firstOrNull { it.type == "html" }?.content
                ?: buildHtmlFromItems(items)
            // 修复残留：转写中途退出会把 "识别中..." 按钮文本存进 HTML，载入时复位为可点击的 "转文字"
            val fixedHtml = html.replace(">识别中...</span>", ">转文字</span>")
            htmlContent = fixedHtml
            runOnUiThread {
                note?.let { etTitle.setText(it.title) }
                webView.loadDataWithBaseURL("file://${baseDir.absolutePath}/", editorHtml(fixedHtml), "text/html; charset=UTF-8", "UTF-8", null)
                updateWordCount()
            }
        }
    }

    /** 当没有 HTML ContentItem 时，从其他类型 Item 拼装 HTML */
    private fun buildHtmlFromItems(items: List<ContentItem>): String {
        val sb = StringBuilder()
        for (item in items) {
            when (item.type) {
                "voice" -> {
                    val durSec = if (item.voiceDuration > 0) item.voiceDuration / 1000 else 1
                    val px = (50 + durSec * 8).coerceIn(50, 350)
                    val timeFont = if (durSec <= 10) "5px" else "8px"
                    val durStr = "${durSec}\u2033"
                    val pathEsc = esc(item.content)
                    val hasTranscript = item.voiceTranscript.isNotEmpty()
                    val transcriptEsc = esc(item.voiceTranscript)
                    val btnLabel = if (hasTranscript) "收起" else "转文字"
                    val transcriptStyle = if (hasTranscript) "" else "display:none;max-height:0"
                    val transcriptPad = if (hasTranscript) "8px 10px" else "0 10px"
                    sb.append("<div contenteditable=\"false\" class=\"voice-msg\" data-type=\"voice\" data-path=\"")
                        .append(pathEsc).append("\" data-transcript=\"").append(transcriptEsc)
                        .append("\" style=\"display:block;margin:6px 0\">")
                        .append("<span class=\"voice-playing-tip\" style=\"display:none;font-size:11px;color:#07C160;white-space:nowrap\">正在播放</span>")
                        .append("<span class=\"voice-body\" style=\"display:flex;align-items:center;gap:6px;flex-wrap:nowrap;background:linear-gradient(135deg,rgba(220,238,255,0.85),rgba(140,190,240,0.45));border-radius:8px;border:1px solid rgba(180,210,240,0.5);box-shadow:inset 0 1px 0 rgba(255,255,255,0.7),0 1px 4px rgba(30,80,160,0.08);padding:5px 10px;cursor:pointer;width:${px}px;max-width:49vw;height:30px;box-sizing:border-box;overflow:hidden\">")
                        .append("<span class=\"vdur\" style=\"font-size:12px;color:#333;font-weight:500;line-height:1;flex-shrink:0\">").append(durStr).append("</span>")
                        .append("<span style=\"font-size:").append(timeFont).append(";color:#aaa;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;flex-shrink:1;min-width:0;text-align:right\">").append(if (durSec <= 10) nowDateShort() else nowTime()).append("</span>")
                        .append("</span>")
                        .append("<span class=\"voice-transcribe-btn\" style=\"display:inline-block;font-size:10px;color:#576B95;cursor:pointer;margin-top:2px\">").append(btnLabel).append("</span>")
                        .append("<span class=\"voice-copy-btn\" style=\"display:").append(if(hasTranscript) "inline-block" else "none")
                        .append(";font-size:10px;color:#576B95;cursor:pointer;margin-left:8px;margin-top:2px\">复制</span>")
                        .append("<div class=\"voice-transcript\" style=\"").append(transcriptStyle)
                        .append(";overflow:hidden;transition:max-height 0.3s ease;font-size:13px;color:#333;background:#F5F5F5;border-radius:6px;padding:")
                        .append(transcriptPad).append(";margin-top:4px;line-height:1.6\">")
                        .append(transcriptEsc).append("</div>")
                        .append(tsMarkers(item.voiceTimestamps))
                        .append("</div><br>")
                }
                "image" -> {
                    val pathEsc = esc(item.content)
                    val f = File(item.content)
                    val name = f.name
                    sb.append("<div contenteditable=\"false\" class=\"media-card\" data-type=\"image\" data-path=\"")
                        .append(pathEsc).append("\"><span class=\"icon\">🖼️</span><div><div class=\"label\">图片</div><div class=\"name\">")
                        .append(esc(name)).append("</div></div>").append(timeLabel()).append("</div><br>")
                }
                "video" -> {
                    val pathEsc = esc(item.content)
                    val f = File(item.content)
                    val name = f.name
                    sb.append("<div contenteditable=\"false\" class=\"media-card\" data-type=\"video\" data-path=\"")
                        .append(pathEsc).append("\"><span class=\"icon\">🎬</span><div><div class=\"label\">视频</div><div class=\"name\">")
                        .append(esc(name)).append("</div></div>").append(timeLabel()).append("</div><br>")
                }
                "pdf" -> {
                    val pathEsc = esc(item.content)
                    val f = File(item.content)
                    val name = f.name
                    sb.append("<div contenteditable=\"false\" class=\"media-card\" data-type=\"pdf\" data-path=\"")
                        .append(pathEsc).append("\"><span class=\"icon\">📄</span><div><div class=\"label\">PDF</div><div class=\"name\">")
                        .append(esc(name)).append("</div></div>").append(timeLabel()).append("</div><br>")
                }
                "text" -> {
                    // text 类型：如果内容以 <div/ <p/ <h 开头，视为 HTML 直接插入；否则转义后显示
                    val content = item.content.trim()
                    if (content.startsWith("<div") || content.startsWith("<p") || content.startsWith("<h") ||
                        content.startsWith("<span") || content.startsWith("<ul") || content.startsWith("<ol")) {
                        sb.append(content).append("<br>")
                    } else {
                        val escaped = content.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\n","<br>")
                        if (escaped.isNotBlank()) sb.append(escaped).append("<br>")
                    }
                }
                else -> {
                    val escaped = item.content.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\n","<br>")
                    if (escaped.isNotBlank()) sb.append(escaped).append("<br>")
                }
            }
        }
        return sb.toString().ifEmpty { "" }
    }

    private fun saveNote(showToast: Boolean) {
        // 通过 Bridge 获取 HTML，避免 evaluateJavascript JSON 转义问题
        js("Android.saveContent(document.getElementById('editor').innerHTML)")
        if (showToast) toast("已保存")
    }

    private fun finishWithSave() { if (isDirty) saveNote(false); finish() }
    private fun setupAutoSave() = etTitle.setOnFocusChangeListener { _, _ -> markDirty() }
    private fun markDirty() { isDirty = true; tvSaveStatus.text = "● 未保存"; scheduleAutoSave() }

    private fun updateWordCount() {
        val text = com.mynote.android.util.HtmlConverter.toText(htmlContent)
            .replace(Regex("[^\\u4e00-\\u9fa5\\u3400-\\u4dbfa-zA-Z0-9]"), "") // 只保留汉字+英文+数字
        tvWordCount.text = "${text.length}字"
    }
    private fun scheduleAutoSave() { autoSaveJob?.cancel(); autoSaveJob = ioScope.launch { delay(3000); runOnUiThread { saveNote(false) } } }

    // ===== 编辑器 HTML =====
    private fun editorHtml(body: String): String {
        val template = """<!DOCTYPE html><html><head>
<meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1.0,maximum-scale=1.0,user-scalable=no">
<style>
*{box-sizing:border-box;margin:0;padding:0}
body{font-family:sans-serif;font-size:17px;color:#333;padding:16px;line-height:1.7;min-height:100vh;word-wrap:break-word;overflow-wrap:break-word;word-break:break-word;max-width:100%}
#editor{outline:none;min-height:100vh;word-wrap:break-word;overflow-wrap:break-word;word-break:break-word;max-width:100%}
#editor:empty:before{content:attr(placeholder);color:#aaa}
.media{display:block;margin:12px auto;max-width:100%;cursor:pointer;user-select:none}
.media img{max-width:100%;height:auto;border-radius:8px;display:block}
.media-card{display:flex;align-items:center;padding:12px 16px;background:#f5f5f5;border-radius:8px;border:1px solid #e0e0e0;margin:8px 0;cursor:pointer;user-select:none}
.media-card .icon{font-size:28px;margin-right:12px}
.media-card .label{font-size:14px;color:#666}
.media-card .name{font-size:13px;color:#333;font-weight:500;margin-top:2px}

.voice-msg{user-select:none}
.voice-playing-tip{font-family:sans-serif}
@keyframes wave{from{transform:scaleY(0.4)}to{transform:scaleY(1)}}
.voice-msg.playing .voice-wave span{animation-play-state:running}
.voice-transcript.expanded{max-height:500px!important;padding:8px 10px!important}
.voice-transcribe-btn:hover{text-decoration:underline}
.voice-copy-btn:hover{text-decoration:underline}
</style></head><body>
<div id="editor" contenteditable="true" placeholder="开始写笔记...">__BODY__</div>
<script>
var ed=document.getElementById('editor');
// Enter → <p> 标签，比 <br> 更稳定，光标不会乱跑
ed.addEventListener('keydown',function(e){
    if(e.key==='Enter'){e.preventDefault();document.execCommand('insertHTML',false,'<p><br></p>')}
    // Tab → 缩进
    if(e.key==='Tab'){e.preventDefault();document.execCommand('insertHTML',false,'&emsp;')}
});
// 粘贴清洗：只保留纯文本，去掉来源格式
ed.addEventListener('paste',function(e){
    e.preventDefault();
    var text=(e.clipboardData||window.clipboardData).getData('text/plain');
    if(text){document.execCommand('insertText',false,text)}
});
// 页面隐藏前触发保存
document.addEventListener('visibilitychange',function(){if(document.hidden)Android.saveOnHide()});
// 拦截删除操作：如果选区包含受保护元素则阻止
ed.addEventListener('beforeinput',function(e){
    if(!e.inputType||!e.inputType.startsWith('delete'))return;
    var sel=window.getSelection();
    if(!sel||sel.rangeCount===0)return;
    var r=sel.getRangeAt(0);
    // 检查选区或光标所在位置是否涉及受保护元素
    var c=r.commonAncestorContainer;
    if(c.nodeType===3)c=c.parentNode;
    if(c.closest&&c.closest('.media,.media-card,.voice-msg')){e.preventDefault();return}
    // 检查选区内容是否包含受保护元素
    try{var frag=r.cloneContents();var tmp=document.createElement('div');tmp.appendChild(frag);if(tmp.querySelector('.media,.media-card,.voice-msg')){e.preventDefault()}}catch(ex){}
});

// ── 优化：事件委托 + 防抖，不再每300ms轮询 ──
var protectedSnap=null;
var skipRestore=false;
var restoreTimer=null;
function saveProtected(){
    if(skipRestore)return;
    var items=ed.querySelectorAll('[contenteditable="false"]:not(#editor),.media,.media-card,.voice-msg');
    protectedSnap=Array.from(items).map(function(el){return el.cloneNode(true)});
}
function restoreProtected(){
    if(!protectedSnap||skipRestore)return;
    var current=ed.querySelectorAll('[contenteditable="false"]:not(#editor),.media,.media-card,.voice-msg');
    if(current.length < protectedSnap.length){
        var missing=protectedSnap.length - current.length;
        for(var i=0;i<missing;i++){
            ed.appendChild(protectedSnap[protectedSnap.length-1-i].cloneNode(true));
        }
    }
}
// 防抖恢复：DOM变化后500ms内只执行一次
function scheduleRestore(){
    if(restoreTimer)clearTimeout(restoreTimer);
    restoreTimer=setTimeout(function(){
        restoreProtected();
        saveProtected();
    },500);
}
saveProtected();

// 事件委托：只需绑定一次在#editor上
ed.addEventListener('click',function(e){
    var el=e.target;
    // 转文字按钮（必须在 general media click 之前检查）
    if(el.classList.contains('voice-transcribe-btn')){
        e.preventDefault();e.stopPropagation();
        if(el.textContent.indexOf('识别中')===0)return;  // 防重入：转写进行中忽略点击
        var msg=el.closest('.voice-msg');
        if(!msg)return;
        var t=msg.querySelector('.voice-transcript');
        var path=msg.dataset.path||'';
        var cp=msg.querySelector('.voice-copy-btn');
        if(el.textContent==='重试'){
            t.textContent='';t.style.maxHeight='0px';t.style.padding='0 10px';t.style.color='#333';
            el.textContent='识别中...';if(cp)cp.style.display='none';
            Android.transcribeVoice(path);
            return;
        }
        if(t&&t.textContent.trim()){
            var expanded=t.style.maxHeight!=='0px';
            if(expanded){
                t.style.maxHeight='0px';t.style.padding='0 10px';el.textContent='转文字';if(cp)cp.style.display='none';
            }else{
                t.style.display='';t.style.maxHeight=(t.scrollHeight+16)+'px';t.style.padding='8px 10px 12px 10px';el.textContent='收起';if(cp)cp.style.display='inline-block';
            }
            return;
        }
        el.textContent='识别中...';if(cp)cp.style.display='none';
        Android.transcribeVoice(path);
        return;
    }
    // 媒体卡片点击
    var card=el.closest('.media-card')||el.closest('.media')||el.closest('.voice-msg');
    if(card&&card!==ed){
        e.preventDefault();e.stopPropagation();
        Android.click(card.dataset.type,card.dataset.path||'');
        return;
    }
    // 时间戳跳转
    if(el.classList.contains('ts-marker')){
        e.preventDefault();e.stopPropagation();
        var seekSec=parseInt(el.dataset.seek)||0;
        var voiceEl=el.closest('.voice-msg');
        Android.seekAudio(voiceEl?voiceEl.dataset.path:'',seekSec);
        return;
    }
    // 复制按钮
    if(el.classList.contains('voice-copy-btn')){
        e.preventDefault();e.stopPropagation();
        var tr=el.closest('.voice-msg').querySelector('.voice-transcript');
        var text=tr?tr.textContent.trim():'';
        if(text){navigator.clipboard.writeText(text);el.textContent='已复制';setTimeout(function(){el.textContent='复制'},1500)}
        return;
    }
    // 转文字区域不冒泡
    if(el.classList.contains('voice-transcript')){e.stopPropagation();return}
});
// 语音条长按删除 - 事件委托
var longPressTimer;
ed.addEventListener('touchstart',function(e){
    var voice=el=e.target.closest('.voice-msg');
    if(voice){longPressTimer=setTimeout(function(){Android.voiceLongPress(voice.dataset.path||'')},600)}
},{passive:false});
ed.addEventListener('touchend',function(e){clearTimeout(longPressTimer)});
ed.addEventListener('touchmove',function(e){clearTimeout(longPressTimer)});

// 监听DOM变化：防抖恢复+保存
var obs=new MutationObserver(function(mutations){
    // 检查是否有实质性变化（忽略属性变化）
    var hasChange=mutations.some(function(m){return m.type==='childList'});
    if(hasChange)scheduleRestore();
});
obs.observe(ed,{childList:true,subtree:true,characterData:true,characterDataOldValue:false});
// ── 格式化快捷插入 ──
function insertSeparator(){
    document.execCommand('insertHTML',false,'<div style="text-align:center;margin:10px 0;color:#BDBDBD;font-size:12px;letter-spacing:6px">—— ◆ ——</div><br>');
    saveProtected();
}
function insertQuote(){
    document.execCommand('insertHTML',false,'<div style="border-left:4px solid #1565C0;padding:10px 14px;margin:8px 0;background:#E3F2FD;border-radius:0 6px 6px 0;color:#37474F;font-size:15px;font-style:italic"><br></div><br>');
    saveProtected();
}
function insertHighlight(){
    document.execCommand('insertHTML',false,'<div style="background:linear-gradient(90deg,#FFF9C4,#FFF176);padding:10px 14px;margin:8px 0;border-radius:8px;border:1px solid #FFD54F;font-size:15px;color:#5D4037"><br></div><br>');
    saveProtected();
}
function insertCodeBlock(){
    document.execCommand('insertHTML',false,'<div style="background:#263238;color:#A5D6A7;font-family:monospace;font-size:13px;padding:10px 14px;margin:8px 0;border-radius:8px;border-left:3px solid #FFB300;white-space:pre-wrap;word-break:break-word"><br></div><br>');
    saveProtected();
}
function insertChecklist(){
    document.execCommand('insertHTML',false,'<div style="margin:6px 0"><div>☐ </div><div>☐ </div><div>☐ </div></div><br>');
    saveProtected();
}
function insertInfoCard(){
    document.execCommand('insertHTML',false,'<div style="background:#E3F2FD;padding:10px 14px;margin:8px 0;border-radius:8px;font-size:14px"><b>检查：</b><br><b>结果：</b><br><b>诊断：</b></div><br>');
    saveProtected();
}
function insertWarning(){
    document.execCommand('insertHTML',false,'<div style="background:#FFEBEE;padding:10px 14px;margin:8px 0;border-radius:6px;border-left:4px solid #F44336;font-size:15px;color:#C62828;font-weight:bold">⚠ </div><br>');
    saveProtected();
}
function insertNumberedList(n){
    n=n||3;
    var html='<div style="margin:6px 0">';
    for(var i=1;i<=n;i++) html+='<div>'+i+'. </div>';
    html+='</div><br>';
    document.execCommand('insertHTML',false,html);
    saveProtected();
}
function setPageTemplate(style){
    var ed=document.getElementById('editor');
    // 重置所有样式
    ed.style.background='';ed.style.backgroundSize='';ed.style.lineHeight='';ed.style.color='';
    var styles={
        'blank':'none',
        'lined':'linear-gradient(transparent calc(1.45em - 1px), #B0BEC5 calc(1.45em - 1px), #B0BEC5 1.45em, transparent 1.45em)',
        'dot':'radial-gradient(circle, #CCC 1px, transparent 0)',
        'dark':'none',
        'green':'none'
    };
    var bg=styles[style];
    var size=(style==='lined')?'2em 2em':(style==='dot')?'16px 16px':'';
    if(bg==='none'){
        if(style==='dark'){ed.style.background='#2D2D2D';ed.style.color='#E0E0E0'}
        else if(style==='green'){ed.style.background='#F0F4E8'}
    }else{
        ed.style.background=bg;ed.style.backgroundSize=size;
        if(style==='lined') ed.style.lineHeight='2em'
    }
}
</script></body></html>"""
        return template.replace("__BODY__", body)
    }
    // ===== 视频入口（录制 / 相册） =====
    private fun showVideoPicker() {
        AlertDialog.Builder(this, R.style.GlassDialog)
            .setTitle("添加视频")
            .setItems(arrayOf("拍摄视频", "从相册选择")) { _, which ->
                when (which) {
                    0 -> recordVideo()
                    1 -> pickVideo.launch("video/*")
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private var videoFile: File? = null
    private val recordVideoLauncher = registerForActivityResult(ActivityResultContracts.CaptureVideo()) { ok ->
        if (ok) videoFile?.let { insertVideo(it.absolutePath); markDirty() }
    }

    private fun recordVideo() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), 300); return
        }
        baseDir.mkdirs(); videoFile = File(baseDir, "vid_${System.currentTimeMillis()}.mp4")
        recordVideoLauncher.launch(FileProvider.getUriForFile(this, "$packageName.fileprovider", videoFile!!))
    }

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { it?.let { copy(it, "image") } }
    private val pickVideo = registerForActivityResult(ActivityResultContracts.GetContent()) { it?.let { copy(it, "video") } }
    private val pickPdf = registerForActivityResult(ActivityResultContracts.GetContent()) { it?.let { copy(it, "pdf") } }
    private val pickDocx = registerForActivityResult(ActivityResultContracts.GetContent()) { it?.let { copy(it, "docx") } }
    private val pickXlsx = registerForActivityResult(ActivityResultContracts.GetContent()) { it?.let { copy(it, "xlsx") } }
    private val pickPptx = registerForActivityResult(ActivityResultContracts.GetContent()) { it?.let { copy(it, "pptx") } }
    private var cameraFile: File? = null
    private val takePic = registerForActivityResult(ActivityResultContracts.TakePicture()) { ok -> if (ok) cameraFile?.let { insertImage(it.absolutePath); markDirty() } }

    // 导出 TXT / Markdown 文件选择器
    private val exportTxtLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        uri?.let { writeExportFile(it) { com.mynote.android.util.HtmlConverter.toText(getCurrentHtml()) } }
    }
    private val exportMdLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("text/markdown")) { uri ->
        uri?.let { writeExportFile(it) { com.mynote.android.util.HtmlConverter.toMarkdown(getCurrentHtml()) } }
    }
    private val exportDocxLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.wordprocessingml.document")) { uri ->
        uri?.let { writeDocxExport(it) }
    }
    private val exportPdfLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        uri?.let { exportToPdf(it) }
    }

    private fun takePhoto() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), 100); return
        }
        baseDir.mkdirs(); cameraFile = File(baseDir, "img_${System.currentTimeMillis()}.jpg")
        takePic.launch(FileProvider.getUriForFile(this, "$packageName.fileprovider", cameraFile!!))
    }

    private fun copy(uri: android.net.Uri, type: String) {
        baseDir.mkdirs()
        val ext = when (type) { "image" -> "jpg"; "video" -> "mp4"; else -> type }
        val file = File(baseDir, "${type}_${System.currentTimeMillis()}.$ext")
        contentResolver.openInputStream(uri)?.use { input -> FileOutputStream(file).use { input.copyTo(it) } }
        if (file.length() == 0L) { file.delete(); return }
        when (type) {
            "image" -> insertImage(file.absolutePath)
            "video" -> insertVideo(file.absolutePath)
            "pdf" -> insertPdf(file.absolutePath)
            "docx", "xlsx", "pptx" -> insertDoc(file.absolutePath, type)
        }
    }

    // ===== 录音 =====
    private var recorder: MediaRecorder? = null
    private var audioFile: File? = null
    private var recording = false
    private var recordStartTime = 0L
    private var durationRunnable: Runnable? = null
    private var pulseAnimator: android.animation.ObjectAnimator? = null

    private fun toggleVoice() {
        if (recording) { stopRecord(); return }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 200); return
        }
        startRecord()
    }

    private fun startRecord() {
        baseDir.mkdirs(); audioFile = File(baseDir, "voice_${System.currentTimeMillis()}.m4a")
        recorder = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) MediaRecorder(this) else @Suppress("DEPRECATION") MediaRecorder()
        try {
            recorder?.setAudioSource(MediaRecorder.AudioSource.VOICE_RECOGNITION)
            recorder?.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            recorder?.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            recorder?.setAudioSamplingRate(48000)
            recorder?.setAudioEncodingBitRate(192000)
            recorder?.setOutputFile(audioFile?.absolutePath)
            recorder?.prepare()
            recorder?.start()
            recording = true; recordStartTime = System.currentTimeMillis()
            btnVoice.setColorFilter(Color.parseColor("#F44336"))

            // 时间戳采集：每3秒记录编辑器摘要
            timestampList.clear()
            timestampJob = ioScope.launch {
                while (coroutineContext.isActive) {
                    delay(3000)
                    val elapsed = System.currentTimeMillis() - recordStartTime
                    // 从 WebView 获取纯文本摘要（前80字）
                    val jsCode = """
                        (function(){
                            var t=document.getElementById('editor').innerText||'';
                            return t.substring(0,80);
                        })()
                    """.trimIndent()
                    var summary = ""
                    try {
                        // evaluateJavascript 必须主线程，否则抛异常被吞、时间戳永远采不到
                        summary = withContext(Dispatchers.Main) {
                            var s = ""
                            webView.evaluateJavascript(jsCode) { s = it?.trim('"')?.take(80) ?: "" }
                            delay(100) // 等待回调
                            s
                        }
                    } catch (_: Exception) {}
                    if (summary.isNotBlank()) {
                        timestampList.add(elapsed to summary.replace("\\n", " "))
                    }
                }
            }

            // 波浪动画
            pulseAnimator = android.animation.ObjectAnimator.ofFloat(btnVoice, "scaleY", 1f, 0.6f, 1f).apply {
                duration = 600; repeatCount = android.animation.ObjectAnimator.INFINITE
                repeatMode = android.animation.ObjectAnimator.RESTART; start()
            }

            // 时长更新
            updateDuration()
        } catch (e: Exception) {
            toast("录音失败: ${e.message}")
            recorder?.release(); recorder = null
        }
    }

    private fun updateDuration() {
        durationRunnable = object : Runnable {
            override fun run() {
                if (!recording) return
                val secs = (System.currentTimeMillis() - recordStartTime) / 1000
                val min = secs / 60; val sec = secs % 60
                val secStr = if (sec < 10) "0$sec" else sec.toString()
                tvSaveStatus.text = "● 录音 $min:$secStr"
                handler.postDelayed(this, 500)
            }
        }
        handler.post(durationRunnable!!)
    }

    private fun stopRecord() {
        try { recorder?.stop() } catch (_: Exception) {}
        try { recorder?.release() } catch (_: Exception) {}
        recorder = null; recording = false

        // 停止时间戳采集
        timestampJob?.cancel()
        timestampJob = null

        // 停止动画和时长
        pulseAnimator?.cancel(); pulseAnimator = null
        btnVoice.animate().scaleY(1f).setDuration(100).start()
        btnVoice.clearColorFilter()
        durationRunnable?.let { handler.removeCallbacks(it) }
        tvSaveStatus.text = if (isDirty) "● 未保存" else "● 已保存"

        audioFile?.let { f ->
            if (f.exists() && f.length() > 0) {
                // 构建时间戳 JSON
                val tsJson = if (timestampList.isNotEmpty()) {
                    val arr = org.json.JSONArray()
                    for ((t, txt) in timestampList) {
                        val obj = org.json.JSONObject()
                        obj.put("t", t - recordStartTime) // 相对时间
                        obj.put("txt", txt)
                        arr.put(obj)
                    }
                    arr.toString()
                } else ""
                insertAudio(f.absolutePath, tsJson)
                markDirty()
            }
            else { toast("录音文件为空") }
        }
    }

    // ===== 音频播放 =====
    private var player: MediaPlayer? = null
    private var playingPath: String? = null
    // 时间戳捕获
    private val timestampList = mutableListOf<Pair<Long, String>>()
    private var timestampJob: kotlinx.coroutines.Job? = null
    private fun playVoice(path: String) {
        if (player?.isPlaying == true) {
            player?.stop(); player?.release(); player = null
            playingPath = null
            js("document.querySelectorAll('.voice-msg.playing').forEach(function(el){el.classList.remove('playing');var b=el.querySelector('.voice-body')||el;b.style.background='#FFFFFF';var t=el.querySelector('.voice-playing-tip');if(t)t.style.display='none'})")
            return
        }
        try {
            playingPath = path
            player = MediaPlayer().apply {
                setDataSource(path); prepare(); start()
                setOnCompletionListener {
                    release(); player = null; playingPath = null
                    js("document.querySelectorAll('.voice-msg.playing').forEach(function(el){el.classList.remove('playing');var b=el.querySelector('.voice-body')||el;b.style.background='#FFFFFF';var t=el.querySelector('.voice-playing-tip');if(t)t.style.display='none'})")
                }
            }
            js("""document.querySelectorAll('.voice-msg[data-path="${esc(path)}"]').forEach(function(el){el.classList.add('playing');var b=el.querySelector('.voice-body')||el;b.style.background='#F5F5F5';var t=el.querySelector('.voice-playing-tip');if(t)t.style.display='block'})""")
        } catch (_: Exception) { toast("播放失败") }
    }

    private fun seekVoice(path: String, seekSec: Int) {
        // 如果没有在播放，先开始播放
        if (playingPath != path || player == null) {
            playingPath?.let {
                player?.stop(); player?.release(); player = null
                js("document.querySelectorAll('.voice-msg.playing').forEach(function(el){el.classList.remove('playing');var b=el.querySelector('.voice-body')||el;b.style.background='#FFFFFF';var t=el.querySelector('.voice-playing-tip');if(t)t.style.display='none'})")
            }
            player = MediaPlayer().apply {
                setDataSource(path); prepare()
                seekTo(seekSec * 1000); start()
                setOnCompletionListener { release(); player = null; playingPath = null }
            }
            playingPath = path
        } else {
            player?.seekTo(seekSec * 1000)
        }
        // 高亮播放中的语音条
        js("document.querySelectorAll('.voice-msg.playing').forEach(function(el){el.classList.remove('playing')})")
        js("document.querySelectorAll('.voice-msg[data-path=\"" + esc(path) + "\"]').forEach(function(el){el.classList.add('playing')})")
    }

    private fun esc(s: String) = s.replace("\\","\\\\").replace("'","\\'").replace("\n","\\n").replace("\r","")
    private fun nowTime(): String = timeFormat.format(Date())
    private val dateShortFormat = SimpleDateFormat("yy/M/d HH:mm", Locale.getDefault())
    private fun nowDateShort(): String = dateShortFormat.format(Date())
    private fun timeLabel(): String = "<span style=\"font-size:9px;color:#aaa;margin-left:4px\">" + nowTime() + "</span>"
    // 在媒体块后追加换行，确保下方可编辑
    private fun withBr(html: String): String = html + "<br>"

    override fun onDestroy() {
        super.onDestroy()
        ioScope.cancel()
        handler.removeCallbacksAndMessages(null)
        player?.release(); player = null
        // 正在录音时需先 stop 再 release，否则 MediaRecorder 抛 IllegalStateException
        try { recorder?.stop() } catch (_: Exception) {}
        try { recorder?.release() } catch (_: Exception) {}
        recorder = null
        playingPath = null
        webView.destroy()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        val granted = grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            Toast.makeText(this, "需要授权才能使用此功能", Toast.LENGTH_SHORT).show()
        }
    }

    // ===== AI 辅助写作 =====
    private fun showAiAssist() {
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(32, 16, 32, 0) }
        // 模型选择器
        val modelRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = android.view.Gravity.CENTER }
        val models = listOf("DeepSeek-R1" to "deepseek-reasoner", "Qwen3.7-Max" to "qwen-max")
        var selectedModel = models[0].second
        val modelBtns = models.map { (label, _) ->
            TextView(this).apply { text = label; textSize = 13f; setPadding(16, 8, 16, 8) }
        }
        modelBtns[0].apply { setBackgroundColor(Color.parseColor("#7B1FA2")); setTextColor(Color.WHITE); background = android.graphics.drawable.GradientDrawable().apply { setColor(Color.parseColor("#7B1FA2")); cornerRadius = 16f } }
        modelBtns[1].apply { setTextColor(Color.DKGRAY); background = android.graphics.drawable.GradientDrawable().apply { setColor(Color.parseColor("#E8E8E8")); cornerRadius = 16f } }
        modelBtns.forEachIndexed { i, btn ->
            btn.setOnClickListener {
                selectedModel = models[i].second
                modelBtns.forEachIndexed { j, b ->
                    b.background = android.graphics.drawable.GradientDrawable().apply { setColor(Color.parseColor(if (j == i) "#7B1FA2" else "#E8E8E8")); cornerRadius = 16f }
                    b.setTextColor(if (j == i) Color.WHITE else Color.DKGRAY)
                }
            }
            modelRow.addView(btn)
        }
        root.addView(modelRow)

        val etPrompt = EditText(this).apply { hint = "描述你需要AI帮你写什么 (如: 生成一份肺炎首次病程)"; setLines(3); textSize = 14f; setPadding(0, 12, 0, 0) }
        root.addView(etPrompt)
        val tvStatus = TextView(this).apply { text = ""; textSize = 14f; setTextColor(Color.parseColor("#7B1FA2")); setPadding(0, 12, 0, 0); gravity = android.view.Gravity.CENTER }
        root.addView(tvStatus)
        val handler = android.os.Handler(android.os.Looper.getMainLooper())

        val dialog = AlertDialog.Builder(this, R.style.GlassDialog).setTitle("AI 辅助写作").setView(root)
            .setPositiveButton("✍️ 发送", null)
            .setNegativeButton("取消", null).create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val prompt = etPrompt.text.toString().trim()
                if (prompt.isBlank()) { Toast.makeText(this, "请输入内容", Toast.LENGTH_SHORT).show(); return@setOnClickListener }
                val btn = dialog.getButton(AlertDialog.BUTTON_POSITIVE); btn.isEnabled = false
                tvStatus.text = "⏳ 正在连接 AI..."

                Thread {
                    try {
                        val prefs = com.mynote.android.util.Prefs(this@EditActivity)
                        val model = selectedModel
                        data class Api(val key: String, val url: String)
                        val api = if (model.startsWith("deepseek"))
                            Api(prefs.deepseekApiKey, "https://api.deepseek.com/v1/chat/completions")
                        else
                            Api(prefs.qwenApiKey, "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions")
                        if (api.key.isBlank()) throw Exception("请先在设置中配置 ${if (model.startsWith("deepseek")) "DeepSeek" else "Qwen"} API Key")
                        val client = okhttp3.OkHttpClient.Builder()
                            .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                            .readTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
                            .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                            .build()
                        val json = com.google.gson.Gson().toJson(mapOf(
                            "model" to model,
                            "stream" to true,  // SSE 流式输出
                            "messages" to listOf(
                                mapOf("role" to "system", "content" to "你是一位经验丰富的临床医生，帮助撰写病历、诊疗方案和鉴别诊断。回复专业、简洁、实用。"),
                                mapOf("role" to "user", "content" to prompt)
                            )
                        ))
                        val mt = "application/json".toMediaType()
                        val req = okhttp3.Request.Builder().url(api.url)
                            .header("Authorization", "Bearer ${api.key}").post(okhttp3.RequestBody.create(mt, json)).build()
                        val resp = client.newCall(req).execute()
                        if (!resp.isSuccessful) {
                            val errBody = resp.body?.string() ?: ""
                            throw Exception("HTTP ${resp.code}: ${errBody.take(200)}")
                        }
                        // SSE 流式读取（use 确保 reader 和 Response 关闭，异常也不泄漏）
                        val buffer = StringBuilder()
                        handler.post { tvStatus.text = "🧠 正在生成..." }
                        resp.use {
                            java.io.BufferedReader(java.io.InputStreamReader(it.body?.byteStream(), "UTF-8")).use { reader ->
                                var line: String?
                                while (reader.readLine().also { line = it } != null) {
                                    val l = line ?: continue
                                    if (!l.startsWith("data: ")) continue
                                    val data = l.removePrefix("data: ")
                                    if (data == "[DONE]") break
                                    try {
                                        val chunk = com.google.gson.JsonParser.parseString(data).asJsonObject
                                            ?.getAsJsonArray("choices")?.get(0)?.asJsonObject
                                            ?.getAsJsonObject("delta")?.get("content")?.asString ?: continue
                                        buffer.append(chunk)
                                        handler.post { tvStatus.text = "📝 ${buffer.toString().takeLast(60).replace("\n", " ")}" }
                                    } catch(_: Exception) { continue }
                                }
                            }
                        }
                        val reply = buffer.toString()
                        handler.post {
                            tvStatus.text = "✅ 生成完成！"
                            tvStatus.setTextColor(Color.parseColor("#2E7D32"))
                            val html = "<div style='background:#F3E5F5;padding:12px 14px;border-radius:12px;margin:8px 0;border-left:4px solid #7B1FA2;word-wrap:break-word'>${reply.replace("\n", "<br>")}</div><div><br></div>"
                            js("document.execCommand('insertHTML',false,'${esc(html)}');")
                            markDirty()
                            btn.text = "✅ 已插入"; btn.isEnabled = true
                            Toast.makeText(this@EditActivity, "AI 内容已插入编辑器", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        // streaming - no need to cancel animation
                        handler.post {
                            tvStatus.text = "❌ 失败: ${e.message}"
                            tvStatus.setTextColor(Color.RED); tvStatus.gravity = android.view.Gravity.START
                            btn.apply { text = "✍️ 重试"; isEnabled = true }
                        }
                    }
                }.start()
            }
        }
        dialog.show()
        dialog.window?.setLayout((resources.displayMetrics.widthPixels * 0.92).toInt(), android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    // ===== 处方模板 =====
    private fun showRxTemplates() {
        val rxs = com.mynote.android.util.PrescriptionTemplate.all
        AlertDialog.Builder(this, R.style.GlassDialog).setTitle("处方模板 (${rxs.size})")
            .setItems(rxs.map { "[${it.dept}] ${it.title}" }.toTypedArray()) { _, i ->
                val rx = rxs[i]
                AlertDialog.Builder(this, R.style.GlassDialog).setTitle(rx.title)
                    .setMessage("【${rx.dept}】\n\n${rx.content}")
                    .setPositiveButton("📋 插入处方") { _, _ ->
                        val html = "<div style='background:#FFF8E1;padding:12px 14px;border-radius:12px;margin:8px 0;border-left:4px solid #BF360C;font-family:monospace;font-size:14px;line-height:1.6;word-wrap:break-word'>${rx.content.replace("\n", "<br>")}</div><div><br></div>"
                        js("document.execCommand('insertHTML',false,'${esc(html)}');")
                        markDirty()
                        Toast.makeText(this, "已插入：${rx.title}", Toast.LENGTH_SHORT).show()
                    }.setNegativeButton("关闭", null).show()
            }.setNegativeButton("关闭", null).show()
    }

    // ===== OCR 化验单识别 =====
    private var ocrBitmap: android.graphics.Bitmap? = null
    private var ocrResultDialog: android.app.Dialog? = null

    private fun findOcrDialog(): android.app.Dialog? = ocrResultDialog

    private fun startOcrLabReport() {
        AlertDialog.Builder(this, R.style.GlassDialog)
            .setTitle("化验单 OCR")
            .setMessage("拍照识别化验单，自动提取检验值并结构化插入")
            .setPositiveButton("拍照") { _, _ ->
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(arrayOf(Manifest.permission.CAMERA), 200); return@setPositiveButton
                }
                ocrFile = File(cacheDir, "ocr_${System.currentTimeMillis()}.jpg")
                ocrPicLauncher.launch(FileProvider.getUriForFile(this, "$packageName.fileprovider", ocrFile!!))
            }
            .setNegativeButton("相册") { _, _ ->
                ocrGalleryLauncher.launch("image/*")
            }
            .setNeutralButton("取消", null)
            .show()
    }

    private var ocrFile: File? = null

    private val ocrPicLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && ocrFile != null) {
            processOcrImage(ocrFile!!)
        }
    }

    private val ocrGalleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            val input = contentResolver.openInputStream(it)
            ocrFile = File(cacheDir, "ocr_gallery_${System.currentTimeMillis()}.jpg")
            input?.use { inp -> java.io.FileOutputStream(ocrFile).use { out -> inp.copyTo(out) } }
            input?.close()
            ocrFile?.let { f -> if (f.length() > 0) processOcrImage(f) }
        }
    }

    private fun doQwenCalibrate(bmp: android.graphics.Bitmap, rawText: String) {
        Toast.makeText(this, "正在调用 Qwen-VL 校准...", Toast.LENGTH_LONG).show()
        ioScope.launch {
            try {
                val correctedText = com.mynote.android.util.QwenOcrClient.calibrate(this@EditActivity, bmp, rawText)
                withContext(Dispatchers.Main) {
                    if (correctedText != null && correctedText.isNotBlank()) {
                        val cleaned = com.mynote.android.util.LabReportOcr.correctOcrTypos(correctedText)
                        // 尝试结构化解析，匹配到的用格式化显示，其余保留原文
                        val parsed = com.mynote.android.util.LabReportOcr.parseResults(cleaned)
                        val displayText = if (parsed.isNotEmpty()) {
                            com.mynote.android.util.LabReportOcr.formatResults(parsed)
                        } else {
                            cleaned
                        }
                        AlertDialog.Builder(this@EditActivity, R.style.GlassDialog)
                            .setTitle("AI 校准完成")
                            .setMessage(displayText)
                            .setPositiveButton("插入校准结果") { _, _ ->
                                val html = displayText.replace("<b>", "<b>").replace("</b>", "</b>")
                                    .replace("\n", "<br>").replace("⚠", "⚠").replace("↑", "↑").replace("↓", "↓")
                                js("(function(){var d=document.createElement('div');d.style.cssText='background:#E8F5FE;padding:12px 14px;border-radius:12px;margin:8px 0;border:1px solid #90CAF9;font-size:14px;line-height:1.7;word-wrap:break-word';d.innerHTML='${esc(html)}';var ed=document.getElementById('editor')||document.body;ed.appendChild(d);ed.appendChild(document.createElement('br'));saveProtected();})()")
                                markDirty()
                                Toast.makeText(this@EditActivity, "已插入 AI 校准结果", Toast.LENGTH_SHORT).show()
                            }
                            .setNegativeButton("关闭", null).show()
                        val p = com.mynote.android.util.Prefs(this@EditActivity)
                        if (p.qwenApiKey.isEmpty()) {
                            AlertDialog.Builder(this@EditActivity, R.style.GlassDialog)
                                .setTitle("未配置 Qwen API")
                                .setMessage("请先在设置→AI配置中填写阿里云(Qwen) API Key")
                                .setPositiveButton("确定", null).show()
                        } else {
                            Toast.makeText(this@EditActivity, "校准失败，请检查网络或 API 配置", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@EditActivity, "校准异常: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun processOcrImage(file: File) {
        val bmp = android.graphics.BitmapFactory.decodeFile(file.absolutePath)
        if (bmp == null) {
            Toast.makeText(this, "图片加载失败", Toast.LENGTH_SHORT).show()
            return
        }
        Toast.makeText(this, "正在识别...", Toast.LENGTH_SHORT).show()
        ioScope.launch {
            try {
                val results = com.mynote.android.util.LabReportOcr.recognize(bmp)
                val rawText = com.mynote.android.util.LabReportOcr.getLastRawText()
                withContext(Dispatchers.Main) {
                    if (results.isEmpty()) {
                        if (rawText.isBlank()) {
                            AlertDialog.Builder(this@EditActivity, R.style.GlassDialog)
                                .setTitle("识别结果")
                                .setMessage("未能识别出化验数据\n请确保图片清晰且包含中文化验单")
                                .setPositiveButton("好的", null)
                                .show()
                        } else {
                            // 有文字但没匹配到化验项目 → 显示原始文字并允许 AI 校准
                            val preview = rawText.take(600) + if (rawText.length > 600) "\n…" else ""
                            val root = LinearLayout(this@EditActivity).apply {
                                orientation = LinearLayout.VERTICAL; setPadding(40, 16, 40, 8)
                            }
                            root.addView(TextView(this@EditActivity).apply {
                                text = preview; textSize = 12f; setTextIsSelectable(true)
                                maxLines = 15; ellipsize = android.text.TextUtils.TruncateAt.END
                            })
                            val btnRow = LinearLayout(this@EditActivity).apply {
                                orientation = LinearLayout.HORIZONTAL; gravity = android.view.Gravity.END
                                setPadding(0, 12, 0, 0)
                            }
                            val insertBtn = TextView(this@EditActivity).apply {
                                text = "插入原文"; setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                                setTextColor(0xFF1565C0.toInt()); setPadding(24, 8, 24, 8)
                                setOnClickListener {
                                    val html = preview.replace("\n", "<br>")
                                    js("(function(){var d=document.createElement('div');d.style.cssText='background:#FFF3E0;padding:12px 14px;border-radius:12px;margin:8px 0;border:1px solid #FFCC80;font-size:14px;line-height:1.7';d.innerHTML='${esc(html)}';var ed=document.getElementById('editor')||document.body;ed.appendChild(d);ed.appendChild(document.createElement('br'));saveProtected();})()")
                                    markDirty()
                                    ocrResultDialog?.dismiss()
                                }
                            }
                            val aiBtn = TextView(this@EditActivity).apply {
                                text = "AI 识别校准"; setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                                setTextColor(0xFF1565C0.toInt()); setPadding(24, 8, 24, 8)
                                setOnClickListener {
                                    ocrResultDialog?.dismiss()
                                    doQwenCalibrate(bmp, rawText)
                                }
                            }
                            val closeBtn = TextView(this@EditActivity).apply {
                                text = "关闭"; setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                                setTextColor(0xFF999999.toInt()); setPadding(24, 8, 24, 8)
                                setOnClickListener { ocrResultDialog?.dismiss() }
                            }
                            btnRow.addView(aiBtn)
                            btnRow.addView(insertBtn)
                            btnRow.addView(closeBtn)
                            root.addView(btnRow)
                            ocrResultDialog = AlertDialog.Builder(this@EditActivity, R.style.GlassDialog)
                                .setTitle("本地识别到文字（未匹配化验项）")
                                .setView(root)
                                .create().also { it.show() }
                        }
                        return@withContext
                    }
                    val formatted = com.mynote.android.util.LabReportOcr.formatResults(results)
                    val dialogBmp = bmp // 持有引用供校准使用
                    AlertDialog.Builder(this@EditActivity, R.style.GlassDialog)
                        .setTitle("识别成功 (${results.size}项)")
                        .setMessage(formatted)
                        .setPositiveButton("插入") { _, _ ->
                            val html = formatted
                                .replace("<b>", "<b>")
                                .replace("</b>", "</b>")
                                .replace("\n", "<br>")
                                .replace("⚠", "⚠")
                                .replace("↑", "↑")
                                .replace("↓", "↓")
                            val jsCode = html.replace("\\", "\\\\").replace("'", "\\'")
                            js("(function(){var d=document.createElement('div');d.style.cssText='background:#F1F8E9;padding:12px 14px;border-radius:12px;margin:8px 0;border:1px solid #A5D6A7;font-size:14px;line-height:1.7;word-wrap:break-word';d.innerHTML='${esc(html)}';var ed=document.getElementById('editor')||document.body;ed.appendChild(d);ed.appendChild(document.createElement('br'));saveProtected();})()")
                            markDirty()
                            Toast.makeText(this@EditActivity, "已插入化验结果", Toast.LENGTH_SHORT).show()
                        }
                        .setNeutralButton("AI校准") { _, _ -> doQwenCalibrate(dialogBmp, rawText) }
                        .setNegativeButton("关闭", null)
                        .show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@EditActivity, "识别失败: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
