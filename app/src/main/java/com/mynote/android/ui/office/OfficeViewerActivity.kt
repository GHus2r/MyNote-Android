package com.mynote.android.ui.office

import android.content.ClipData
import android.content.ClipboardManager
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.mynote.android.R
import com.mynote.android.data.AppDatabase
import com.mynote.android.data.entity.ContentItem
import com.mynote.android.data.entity.Note
import com.mynote.android.data.entity.ParentCategory
import com.mynote.android.data.entity.SubCategory
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Office 文档查看器
 * 支持 Word (DOCX) / Excel (XLSX) / PPT (PPTX)
 */
class OfficeViewerActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_PATH = "office_path"
        private const val MAX_IMAGE_DIM = 1920
    }

    // ── UI ──
    private lateinit var webView: WebView
    private lateinit var tvTitle: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var pptNavBar: View
    private lateinit var tvSlideCounter: TextView
    private lateinit var btnPrevSlide: TextView
    private lateinit var btnNextSlide: TextView

    // 查看工具栏
    private lateinit var viewButtons: View
    private lateinit var btnSearch: TextView
    private lateinit var btnCopy: TextView
    private lateinit var btnTextColor: TextView
    private lateinit var btnZoomIn: TextView
    private lateinit var btnZoomOut: TextView
    private lateinit var btnFitWidth: TextView
    private lateinit var btnEditMode: TextView

    // 编辑工具栏
    private lateinit var editBar: View
    private lateinit var btnEditDone: TextView
    private lateinit var btnEditBold: TextView
    private lateinit var btnEditItalic: TextView
    private lateinit var btnEditUnderline: TextView
    private lateinit var btnEditFontDown: TextView
    private lateinit var btnEditFontUp: TextView
    private lateinit var btnEditExport: TextView

    // ── 状态 ──
    private var file: File? = null
    private var isPpt = false
    private var isExcel = false
    private var isEditing = false
    private var totalSlides = 0
    private var currentSlide = 1
    private var fitToWidth = true
    private var colorThemeIdx = 0
    private var imageCacheDir: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_office_viewer)

        // ── 绑定 UI ──
        webView = findViewById(R.id.web_office)
        tvTitle = findViewById(R.id.tv_office_title)
        progressBar = findViewById(R.id.progress_office)
        pptNavBar = findViewById(R.id.ppt_nav_bar)
        tvSlideCounter = findViewById(R.id.tv_slide_counter)
        btnPrevSlide = findViewById(R.id.btn_prev_slide)
        btnNextSlide = findViewById(R.id.btn_next_slide)

        viewButtons = findViewById(R.id.view_buttons)
        btnSearch = findViewById(R.id.btn_search)
        btnCopy = findViewById(R.id.btn_copy)
        btnTextColor = findViewById(R.id.btn_text_color)
        btnZoomIn = findViewById(R.id.btn_zoom_in)
        btnZoomOut = findViewById(R.id.btn_zoom_out)
        btnFitWidth = findViewById(R.id.btn_fit_width)
        btnEditMode = findViewById(R.id.btn_edit_mode)

        editBar = findViewById(R.id.edit_bar)
        btnEditDone = findViewById(R.id.btn_edit_done)
        btnEditBold = findViewById(R.id.btn_edit_bold)
        btnEditItalic = findViewById(R.id.btn_edit_italic)
        btnEditUnderline = findViewById(R.id.btn_edit_underline)
        btnEditFontDown = findViewById(R.id.btn_edit_font_down)
        btnEditFontUp = findViewById(R.id.btn_edit_font_up)
        btnEditExport = findViewById(R.id.btn_edit_export)

        // ── WebView 配置 ──
        webView.settings.apply {
            javaScriptEnabled = true
            builtInZoomControls = true
            displayZoomControls = false
            loadWithOverviewMode = true
            useWideViewPort = true
            allowFileAccess = true
            allowContentAccess = true
            setSupportZoom(true)
            defaultFontSize = 16
        }

        // ── 路由 ──
        val path = intent.getStringExtra(EXTRA_PATH)
        if (path == null) { finish(); return }
        file = File(path)
        if (file?.exists() != true) {
            Toast.makeText(this, "文件不存在", Toast.LENGTH_SHORT).show()
            finish(); return
        }

        val ext = file!!.extension.lowercase()
        isPpt = ext == "pptx" || ext == "ppt"
        isExcel = ext == "xlsx" || ext == "xls"
        tvTitle.text = when (ext) {
            "docx", "doc" -> "Word 预览"
            "xlsx", "xls" -> "Excel 预览"
            "pptx", "ppt" -> "PPT 预览"
            else -> ext.uppercase()
        }

        // ── 事件绑定 ──
        findViewById<TextView>(R.id.btn_office_back).setOnClickListener { finish() }

        btnPrevSlide.setOnClickListener { navigateSlide(-1) }
        btnNextSlide.setOnClickListener { navigateSlide(1) }
        btnFitWidth.setOnClickListener { toggleFitWidth() }
        btnZoomIn.setOnClickListener { zoomIn() }
        btnZoomOut.setOnClickListener { zoomOut() }
        btnSearch.setOnClickListener { showSearchDialog() }
        btnCopy.setOnClickListener { copySelectedText() }
        btnTextColor.setOnClickListener { cycleTextColor() }

        btnEditMode.setOnClickListener { toggleEditMode() }
        btnEditDone.setOnClickListener { toggleEditMode() }
        btnEditBold.setOnClickListener { execJs("document.execCommand('bold')") }
        btnEditItalic.setOnClickListener { execJs("document.execCommand('italic')") }
        btnEditUnderline.setOnClickListener { execJs("document.execCommand('underline')") }
        btnEditFontDown.setOnClickListener { execJs("document.execCommand('decreaseFontSize')") }
        btnEditFontUp.setOnClickListener { execJs("document.execCommand('increaseFontSize')") }
        btnEditExport.setOnClickListener { exportEditedText() }

        // ── 初始视觉状态 ──
        btnEditMode.alpha = 0.6f
        btnFitWidth.alpha = 1.0f

        // ── 初始化 ──
        pptNavBar.visibility = if (isPpt) View.VISIBLE else View.GONE
        imageCacheDir = File(cacheDir, "office_images/${file!!.nameWithoutExtension}").also { it.mkdirs() }
        loadPreview()
    }

    // ═══════════════ 工具栏操作 ═══════════════

    private fun toggleFitWidth() {
        fitToWidth = !fitToWidth
        val alpha = if (fitToWidth) 1.0f else 0.4f
        btnFitWidth.alpha = alpha
        webView.evaluateJavascript(
            "document.body.style.zoom='${if (fitToWidth) "1.0" else "1.3"}';" +
            "document.body.style.width='${if (fitToWidth) "100%" else "auto"}';"
        ) {}
    }

    private var zoomLevel = 1.0f

    private fun zoomIn() {
        zoomLevel = (zoomLevel + 0.15f).coerceAtMost(2.5f)
        webView.evaluateJavascript("document.body.style.zoom='$zoomLevel'") {}
    }

    private fun zoomOut() {
        zoomLevel = (zoomLevel - 0.15f).coerceAtLeast(0.5f)
        webView.evaluateJavascript("document.body.style.zoom='$zoomLevel'") {}
    }

    private fun copySelectedText() {
        webView.evaluateJavascript("window.getSelection().toString()") { result ->
            val text = result?.trim('"')?.takeIf { it.isNotEmpty() }
            if (text != null) {
                val clip = ClipData.newPlainText("office", text)
                (getSystemService(ClipboardManager::class.java) as ClipboardManager).setPrimaryClip(clip)
                Toast.makeText(this, "已复制", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "请先长按选中文字", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun cycleTextColor() {
        colorThemeIdx = (colorThemeIdx + 1) % 4
        val (bg, textColor) = when (colorThemeIdx) {
            0 -> "#fff" to "#333"
            1 -> "#f5f0e0" to "#4a3728"
            2 -> "#e3f0e0" to "#2d3a2d"
            3 -> "#1a1a1a" to "#d0d0d0"
            else -> "#fff" to "#333"
        }
        val label = when (colorThemeIdx) { 0 -> "🎨" 1 -> "☕" 2 -> "🌿" 3 -> "🌙" else -> "🎨" }
        webView.evaluateJavascript("""
            document.body.style.background='$bg';document.body.style.color='$textColor';
            var el=document.querySelector('.doc-container,.slide-inner,.table-wrap');
            if(el){el.style.background='$bg';el.style.color='$textColor';}
        """.trimIndent()) {}
        btnTextColor.text = label
        webView.setBackgroundColor(android.graphics.Color.parseColor(bg))
    }

    private fun showSearchDialog() {
        webView.evaluateJavascript("(function(){var s=window.getSelection().toString();return s||'';})()") { result ->
            val selText = result?.trim('"')?.takeIf { it.isNotEmpty() } ?: ""
            val input = android.widget.EditText(this).apply {
                hint = "输入关键词"
                if (selText.isNotEmpty()) setText(selText)
                setSelectAllOnFocus(true)
            }
            android.app.AlertDialog.Builder(this, R.style.GlassDialog)
                .setTitle("查找文本")
                .setView(input)
                .setPositiveButton("查找") { _, _ ->
                    input.text.toString().takeIf { it.isNotEmpty() }?.let { webView.findAllAsync(it) }
                }
                .setNegativeButton("取消", null)
                .show()
        }
    }

    // ═══════════════ 编辑模式 ═══════════════

    private fun toggleEditMode() {
        isEditing = !isEditing
        if (isEditing) {
            viewButtons.visibility = View.GONE
            editBar.visibility = View.VISIBLE
            btnEditMode.text = "🔒"
            btnEditMode.alpha = 1.0f
            if (isExcel) {
                webView.evaluateJavascript(
                    "document.querySelectorAll('td').forEach(function(td){td.contentEditable='true';td.style.outline='1px dashed #ccc'})"
                ) {}
            } else {
                val sel = if (isPpt) ".slide-content" else ".doc-container"
                webView.evaluateJavascript(
                    "var el=document.querySelector('$sel');if(el){el.contentEditable='true';el.focus()}"
                ) {}
            }
        } else {
            editBar.visibility = View.GONE
            viewButtons.visibility = View.VISIBLE
            btnEditMode.text = "✏️"
            btnEditMode.alpha = 0.6f
            if (isExcel) {
                webView.evaluateJavascript(
                    "document.querySelectorAll('td').forEach(function(td){td.contentEditable='false';td.style.outline=''})"
                ) {}
            } else {
                val sel = if (isPpt) ".slide-content" else ".doc-container"
                webView.evaluateJavascript("var el=document.querySelector('$sel');if(el){el.contentEditable='false'}") {}
            }
        }
    }

    private fun execJs(code: String) {
        webView.evaluateJavascript(code) {}
    }

    private fun exportEditedText() {
        val js = if (isExcel) {
            """(function(){var rows=document.querySelectorAll('tr'),out='';rows.forEach(function(r){var cells=r.querySelectorAll('td'),cols=[];cells.forEach(function(c){cols.push(c.innerText.trim())});if(cols.length>0)out+=cols.join('\t')+'\n'});return out.trim();})()"""
        } else {
            val sel = if (isPpt) ".slide-content" else ".doc-container"
            "document.querySelector('$sel').innerText"
        }
        webView.evaluateJavascript(js) { result ->
            val text = result?.trim('"')?.replace("\\n", "\n")?.takeIf { it.isNotEmpty() } ?: ""
            if (text.isBlank()) { Toast.makeText(this, "无内容可导出", Toast.LENGTH_SHORT).show(); return@evaluateJavascript }
            val input = android.widget.EditText(this).apply {
                setText(text.take(200))
                setSelection(0, text.take(200).length)
            }
            android.app.AlertDialog.Builder(this, R.style.GlassDialog)
                .setTitle("导出编辑内容")
                .setMessage("导出为笔记或复制全文？\n\n（不支持写回原始文件）")
                .setPositiveButton("导出为笔记") { _, _ -> exportAsNote(text) }
                .setNeutralButton("复制全文") { _, _ ->
                    val clip = ClipData.newPlainText("edited", text)
                    (getSystemService(ClipboardManager::class.java) as ClipboardManager).setPrimaryClip(clip)
                    Toast.makeText(this, "已复制全文", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("取消", null)
                .show()
        }
    }

    private fun exportAsNote(text: String) {
        lifecycleScope.launch {
            try {
                val db = AppDatabase.get(this@OfficeViewerActivity)
                val dao = db.noteDao()
                val catDao = db.categoryDao()

                val parents = catDao.getParentCategories()
                val parent = if (parents.isNotEmpty()) parents.first() else {
                    val p = ParentCategory(UUID.randomUUID().toString(), "默认分类", "", 0)
                    catDao.insertParentCategory(p); p
                }
                val subs = catDao.getSubCategories(parent.id)
                val sub = if (subs.isNotEmpty()) subs.first() else {
                    val s = SubCategory(UUID.randomUUID().toString(), parent.id, "默认", "", 0)
                    catDao.insertSubCategory(s); s
                }
                val noteId = UUID.randomUUID().toString()
                val now = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
                dao.insertNote(Note(
                    id = noteId, subCategoryId = sub.id,
                    title = file?.nameWithoutExtension ?: "编辑导出",
                    contentText = text.take(500), updateTime = now, createTime = now
                ))
                dao.insertContentItem(ContentItem(noteId = noteId, type = "text", content = text, timestamp = now))
                Toast.makeText(this@OfficeViewerActivity, "已导出为笔记", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@OfficeViewerActivity, "导出失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ═══════════════ PPT 导航 ═══════════════

    private fun navigateSlide(delta: Int) {
        val newSlide = (currentSlide + delta).coerceIn(1, totalSlides)
        if (newSlide != currentSlide) {
            currentSlide = newSlide
            updateSlideCounter()
            webView.evaluateJavascript("showSlide($currentSlide)") {}
        }
    }

    private fun updateSlideCounter() {
        tvSlideCounter.text = "$currentSlide / $totalSlides"
        btnPrevSlide.alpha = if (currentSlide <= 1) 0.3f else 1.0f
        btnNextSlide.alpha = if (currentSlide >= totalSlides) 0.3f else 1.0f
    }

    // ═══════════════ 加载预览 ═══════════════

    private fun loadPreview() {
        progressBar.visibility = View.VISIBLE
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                progressBar.visibility = View.GONE
            }
        }
        Thread {
            try {
                when {
                    isPpt -> loadPptPreview()
                    isExcel -> loadTablePreview()
                    else -> loadDocxPreview()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    progressBar.visibility = View.GONE
                    Toast.makeText(this, "加载失败: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun loadDocxPreview() {
        val text = OfficeTextExtractor.extract(file!!)
        val images = extractImagesToFiles(listOf("word/media/"))
        val title = file!!.nameWithoutExtension
        val imgs = images.entries.joinToString("") { (_, uri) ->
            """<div class="img-wrap"><img src="$uri" /></div>"""
        }
        val body = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
            .split("\n").joinToString("") { p ->
                if (p.isBlank()) "<p>&nbsp;</p>" else "<p>$p</p>"
            }
        val html = buildDocHtml(title, imgs, body)
        runOnUiThread { webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null) }
    }

    private fun loadPptPreview() {
        val slides = OfficeTextExtractor.extractPptxSlides(file!!)
        if (slides.isEmpty()) {
            runOnUiThread {
                progressBar.visibility = View.GONE
                webView.loadDataWithBaseURL(null, EMPTY_HTML, "text/html", "UTF-8", null)
            }
            return
        }
        totalSlides = slides.size
        currentSlide = 1

        val imgCache = mutableMapOf<String, String>()
        for (slide in slides) {
            for (ref in slide.imageRefs) {
                if (!imgCache.containsKey(ref)) {
                    extractImageToFile(ref)?.let { imgCache[ref] = it }
                }
            }
        }

        val slideHtmls = slides.joinToString("") { slide ->
            val imgs = slide.imageRefs.mapNotNull { imgCache[it]?.let { uri -> """<img src="$uri" class="slide-img"/>""" } }.joinToString("")
            val txt = slide.text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .split("\n").filter { it.isNotBlank() }.joinToString("") { "<p>$it</p>" }
            """<div class="slide" id="slide${slide.index}"><div class="slide-inner"><div class="slide-content">$imgs$txt</div><div class="slide-num">${slide.index} / $totalSlides</div></div></div>"""
        }

        val html = buildPptHtml(slideHtmls)
        runOnUiThread { updateSlideCounter(); webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null) }
    }

    private fun loadTablePreview() {
        val text = OfficeTextExtractor.extract(file!!)
        val title = file!!.nameWithoutExtension
        val rows = text.split("\n").filter { it.isNotBlank() }.joinToString("") { row ->
            val cells = row.split("\t").joinToString("") { "<td>${it.replace("&","&amp;").replace("<","&lt;")}</td>" }
            "<tr>$cells</tr>"
        }
        val html = buildTableHtml(title, rows)
        runOnUiThread { webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null) }
    }

    // ═══════════════ 图片处理 ═══════════════

    private fun extractImageToFile(zipPath: String): String? {
        val f = file ?: return null
        val dir = imageCacheDir ?: return null
        val safeName = zipPath.replace("/", "_").replace("\\", "_")
        val cached = File(dir, safeName)
        if (cached.exists() && cached.length() > 0) return "file://${cached.absolutePath}"

        val bytes = OfficeTextExtractor.readZipEntryBytes(f, zipPath) ?: return null
        if (bytes.size > 20_000_000) return null
        return try {
            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
            val sampleSize = maxOf(
                (opts.outWidth + MAX_IMAGE_DIM - 1) / MAX_IMAGE_DIM,
                (opts.outHeight + MAX_IMAGE_DIM - 1) / MAX_IMAGE_DIM
            ).coerceAtLeast(1)
            val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply {
                inSampleSize = sampleSize; inPreferredConfig = android.graphics.Bitmap.Config.RGB_565
            }) ?: return null
            cached.outputStream().use { out ->
                val fmt = if (zipPath.endsWith(".png", true)) android.graphics.Bitmap.CompressFormat.PNG
                          else android.graphics.Bitmap.CompressFormat.JPEG
                bmp.compress(fmt, 85, out)
            }
            bmp.recycle()
            "file://${cached.absolutePath}"
        } catch (_: Exception) { null }
    }

    private fun extractImagesToFiles(dirs: List<String>): Map<String, String> {
        val result = mutableMapOf<String, String>()
        try {
            java.util.zip.ZipFile(file).use { zip ->
                val entries = zip.entries()
                while (entries.hasMoreElements()) {
                    val e = entries.nextElement()
                    val nm = e.name.lowercase()
                    if (dirs.any { nm.startsWith(it) } && nm.matches(Regex(".*\\.(png|jpe?g|gif|webp|bmp)$"))) {
                        extractImageToFile(e.name)?.let { result[e.name] = it }
                    }
                }
            }
        } catch (_: Exception) {}
        return result
    }

    // ═══════════════ HTML 模板 ═══════════════

    private val EMPTY_HTML = "<html><body style='font-family:sans-serif;padding:40px;text-align:center;color:#999'><h3>无法解析</h3><p>文件可能损坏或格式不支持</p></body></html>"

    private val COMMON_CSS = """
        *{margin:0;padding:0;box-sizing:border-box}
        body{font-family:-apple-system,'Noto Sans SC','Segoe UI',sans-serif;font-size:16px;color:#333;line-height:1.8;
             background:#fafafa;padding:8px;-webkit-text-size-adjust:100%;-webkit-tap-highlight-color:transparent}
    """.trimIndent()

    private fun buildDocHtml(title: String, images: String, body: String) = """
<!DOCTYPE html><html><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1.0">
<style>
    $COMMON_CSS
    .doc-container{max-width:800px;margin:0 auto;background:#fff;padding:32px 20px;border-radius:8px;box-shadow:0 1px 4px rgba(0,0,0,.06)}
    .doc-container h1{font-size:22px;color:#1a1a1a;margin-bottom:20px;padding-bottom:12px;border-bottom:2px solid #e8e8e8}
    .doc-container p{margin:8px 0;text-align:justify}
    .img-wrap{margin:12px 0;text-align:center}
    .img-wrap img{max-width:100%;height:auto;border-radius:4px;box-shadow:0 2px 8px rgba(0,0,0,.08)}
    .doc-container:focus{outline:2px solid #90CAF9;outline-offset:4px;border-radius:8px}
</style>
</head><body>
<div class="doc-container"><h1>$title</h1>$images$body</div>
</body></html>
    """.trimIndent()

    private fun buildPptHtml(slides: String) = """
<!DOCTYPE html><html><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1.0">
<style>
    $COMMON_CSS
    .slide-container{max-width:900px;margin:0 auto}
    .slide{display:none;background:#fff;margin:12px 0;border-radius:8px;box-shadow:0 2px 12px rgba(0,0,0,.08);overflow:hidden}
    .slide.active{display:block}
    .slide-inner{padding:24px 20px;min-height:200px}
    .slide-content p{margin:6px 0;line-height:1.8;text-align:justify}
    .slide-content p:first-child{font-size:20px;font-weight:600;color:#1a1a1a;margin-bottom:12px}
    .slide-img{max-width:100%;height:auto;display:block;margin:8px auto;border-radius:4px}
    .slide-num{text-align:center;color:#aaa;font-size:12px;padding:8px;border-top:1px solid #f0f0f0}
    .slide-content:focus{outline:2px solid #90CAF9;outline-offset:-2px}
</style>
<script>
var totalSlides=$totalSlides,currentSlide=$currentSlide;
function showSlide(n){document.querySelectorAll('.slide').forEach(function(s){s.classList.remove('active')});var t=document.getElementById('slide'+n);if(t){t.classList.add('active');currentSlide=n}}
document.addEventListener('keydown',function(e){if(e.key==='ArrowLeft'||e.key==='ArrowUp'||e.key==='PageUp')showSlide(Math.max(1,currentSlide-1)),e.preventDefault();if(e.key==='ArrowRight'||e.key==='ArrowDown'||e.key==='PageDown')showSlide(Math.min(totalSlides,currentSlide+1)),e.preventDefault()});
var tsX=0;document.addEventListener('touchstart',function(e){tsX=e.touches[0].clientX});document.addEventListener('touchend',function(e){var dx=e.changedTouches[0].clientX-tsX;if(Math.abs(dx)>60){dx<0?showSlide(Math.min(totalSlides,currentSlide+1)):showSlide(Math.max(1,currentSlide-1))}});
window.onload=function(){showSlide(1)};
</script>
</head><body><div class="slide-container">$slides</div></body></html>
    """.trimIndent()

    private fun buildTableHtml(title: String, rows: String) = """
<!DOCTYPE html><html><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1.0">
<style>
    $COMMON_CSS
    .table-wrap{max-width:100%;overflow-x:auto;background:#fff;padding:16px 12px;border-radius:8px;box-shadow:0 1px 4px rgba(0,0,0,.06)}
    .table-wrap h1{font-size:18px;color:#1a1a1a;margin-bottom:12px}
    table{border-collapse:collapse;width:100%;font-size:13px}
    td{border:1px solid #e8e8e8;padding:7px 12px;min-width:60px;white-space:nowrap;transition:background .15s}
    td:focus{background:#FFF9C4!important;outline:2px solid #FFC107!important;position:relative;z-index:1}
    tr:nth-child(even){background:#f8f9fa}
    tr:first-child td{background:#e8f0fe;font-weight:600;color:#333}
</style>
<script>document.addEventListener('click',function(e){if(!e.target.closest('td')&&document.activeElement&&document.activeElement.closest('td'))document.activeElement.blur()})</script>
</head><body>
<div class="table-wrap"><h1>$title</h1><table>$rows</table></div>
</body></html>
    """.trimIndent()

    override fun onDestroy() {
        super.onDestroy()
        webView.destroy()
        imageCacheDir?.deleteRecursively()
    }
}
