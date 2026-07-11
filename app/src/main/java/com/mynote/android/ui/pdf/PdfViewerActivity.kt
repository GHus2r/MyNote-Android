package com.mynote.android.ui.pdf

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.text.InputType
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.mynote.android.R
import java.io.File
import java.lang.ref.WeakReference

class PdfViewerActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_PATH = "pdf_path"
        private const val MAX_RENDER_DIM = 1600
    }

    private lateinit var vpPdf: ViewPager2
    private lateinit var annotationOverlay: AnnotationOverlay
    private lateinit var tvPageInfo: TextView

    private var pdfRenderer: PdfRenderer? = null
    private var pageCount = 0
    private var pdfPath = ""
    private var annotationsFile: File? = null
    private var pdfAnnotations = PdfAnnotations()
    private var currentPage = 0

    private var currentColor = Color.parseColor("#FF5252")
    private var currentWidth = 3f
    private var rotationAngle = 0     // 0/90/180/270
    private var scrollMode = false    // false=翻页, true=连续滚动
    private val bookmarks = mutableSetOf<Int>()  // 已收藏页码（0-based）
    private var autoSaveHandler: Handler? = null
    private var autoSaveRunnable: Runnable? = null
    private lateinit var btnBookmark: TextView
    private lateinit var btnSave: TextView
    private lateinit var btnPen: View
    private lateinit var btnHighlighter: View
    private lateinit var btnText: View
    private lateinit var btnEraser: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_pdf_viewer)

            pdfPath = intent.getStringExtra(EXTRA_PATH) ?: run { finish(); return }
            val pdfFile = File(pdfPath)
            if (!pdfFile.exists()) { Toast.makeText(this, "文件不存在", Toast.LENGTH_SHORT).show(); finish(); return }

            annotationsFile = File(pdfFile.parent, "${pdfFile.nameWithoutExtension}_annotations.json")
            if (annotationsFile!!.exists()) pdfAnnotations = PdfAnnotations.fromJson(annotationsFile!!.readText())

            val pfd = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
            pdfRenderer = PdfRenderer(pfd)
            pageCount = pdfRenderer!!.pageCount

            if (pageCount == 0) { Toast.makeText(this, "PDF 无页面", Toast.LENGTH_SHORT).show(); finish(); return }

            loadBookmarks()
            initViews()
        } catch (e: Exception) {
            Toast.makeText(this, "无法打开 PDF: ${e.message}", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun initViews() {
        tvPageInfo = findViewById(R.id.tv_page_info)
        tvPageInfo.setOnClickListener { showPageJumpDialog() }
        val tvPdfTitle = findViewById<TextView>(R.id.tv_pdf_title)
        tvPdfTitle.text = File(pdfPath).name
        annotationOverlay = findViewById(R.id.annotation_overlay)
        annotationOverlay.onTextCreate = { x, y, w, h -> showTextInputForBox(x, y, w, h) }
        annotationOverlay.onTextEdit = { text, index -> showEditTextDialog(text, index) }
        annotationOverlay.onDrawingModeChanged = { mode ->
            vpPdf.isUserInputEnabled = !mode
        }

        vpPdf = findViewById(R.id.vp_pdf)
        vpPdf.offscreenPageLimit = 1
        vpPdf.adapter = PageAdapter()
        vpPdf.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                // 保存旧页面批注
                if (annotationOverlay.annotationDirty) {
                    pdfAnnotations.pages[currentPage] = annotationOverlay.pageAnnotations
                }
                // 加载新页面批注
                currentPage = position
                tvPageInfo.text = "${position + 1} / $pageCount"
                annotationOverlay.pageAnnotations = pdfAnnotations.pages[position] ?: PageAnnotations()
                annotationOverlay.annotationDirty = false
                annotationOverlay.invalidate()
                updatePageInfo()
                updateBookmarkIcon()
            }
        })

        findViewById<TextView>(R.id.btn_back).setOnClickListener { finishWithSave() }
        findViewById<TextView>(R.id.btn_share).setOnClickListener { sharePdf() }
        btnSave = findViewById(R.id.btn_save)
        btnSave.setOnClickListener { saveAnnotations() }

        // 工具按钮
        btnPen = findViewById(R.id.btn_pen)
        btnHighlighter = findViewById(R.id.btn_highlighter)
        btnText = findViewById(R.id.btn_text)
        btnEraser = findViewById(R.id.btn_eraser)
        val btnRect = findViewById<TextView>(R.id.btn_rect)
        val btnCircle = findViewById<TextView>(R.id.btn_circle)
        val btnUnderline = findViewById<TextView>(R.id.btn_underline)
        val btnStrikethrough = findViewById<TextView>(R.id.btn_strikethrough)

        allToolButtons.addAll(listOf(btnPen, btnHighlighter, btnRect, btnCircle, btnUnderline, btnStrikethrough, btnText, btnEraser))

        btnPen.setOnClickListener { setTool(AnnotationOverlay.Tool.PEN, btnPen) }
        btnHighlighter.setOnClickListener { setTool(AnnotationOverlay.Tool.HIGHLIGHTER, btnHighlighter) }
        btnRect.setOnClickListener { setTool(AnnotationOverlay.Tool.RECTANGLE, btnRect) }
        btnCircle.setOnClickListener { setTool(AnnotationOverlay.Tool.CIRCLE, btnCircle) }
        btnUnderline.setOnClickListener { setTool(AnnotationOverlay.Tool.UNDERLINE, btnUnderline) }
        btnStrikethrough.setOnClickListener { setTool(AnnotationOverlay.Tool.STRIKETHROUGH, btnStrikethrough) }
        btnText.setOnClickListener { setTool(AnnotationOverlay.Tool.TEXT, btnText) }
        btnEraser.setOnClickListener { setTool(AnnotationOverlay.Tool.ERASER, btnEraser) }

        // 笔触粗细
        val btnWidthS = findViewById<TextView>(R.id.btn_width_s)
        val btnWidthM = findViewById<TextView>(R.id.btn_width_m)
        val btnWidthL = findViewById<TextView>(R.id.btn_width_l)
        fun selectWidth(btn: TextView, w: Float) {
            currentWidth = w; applyColor()
            btnWidthS.setTextColor(Color.parseColor("#80FFFFFF"))
            btnWidthM.setTextColor(Color.parseColor("#80FFFFFF"))
            btnWidthL.setTextColor(Color.parseColor("#80FFFFFF"))
            btn.setTextColor(Color.WHITE)
        }
        btnWidthS.setOnClickListener { selectWidth(btnWidthS, 2f) }
        btnWidthM.setOnClickListener { selectWidth(btnWidthM, 4f) }
        btnWidthL.setOnClickListener { selectWidth(btnWidthL, 8f) }
        btnWidthM.setTextColor(Color.WHITE) // 默认中粗

        // 颜色盘
        findViewById<TextView>(R.id.btn_color_picker).setOnClickListener { showColorPicker() }

        // 操作
        findViewById<TextView>(R.id.btn_rotate).setOnClickListener { rotatePage() }
        btnBookmark = findViewById(R.id.btn_bookmark)
        btnBookmark.setOnClickListener { toggleBookmark() }
        btnBookmark.setOnLongClickListener { showBookmarkList(); true }
        findViewById<TextView>(R.id.btn_screenshot).setOnClickListener { screenshotPage() }
        val btnScrollMode = findViewById<TextView>(R.id.btn_scroll_mode)
        btnScrollMode.setOnClickListener { toggleScrollMode(btnScrollMode) }
        findViewById<TextView>(R.id.btn_export).setOnClickListener { exportPdfWithAnnotations() }
        findViewById<TextView>(R.id.btn_undo).setOnClickListener { annotationOverlay.undo() }
        findViewById<TextView>(R.id.btn_clear).setOnClickListener { clearCurrentPage() }

        updatePageInfo()
        updateBookmarkIcon()
        startAutoSave()
    }

    private fun setTool(tool: AnnotationOverlay.Tool, btn: View) {
        if (annotationOverlay.currentTool == tool) {
            annotationOverlay.currentTool = AnnotationOverlay.Tool.NONE
            resetToolButtons()
            vpPdf.isUserInputEnabled = true
        } else {
            annotationOverlay.currentTool = tool
            resetToolButtons()
            btn.setBackgroundResource(R.drawable.bg_tool_active)
            vpPdf.isUserInputEnabled = false
        }
        applyColor()
        updatePageInfo() // 工具切换时刷新保存按钮状态
    }

    private val allToolButtons = mutableListOf<View>()

    private fun resetToolButtons() {
        for (b in allToolButtons) b.setBackgroundColor(Color.TRANSPARENT)
    }

    private fun applyColor() {
        annotationOverlay.currentColor = currentColor
        annotationOverlay.currentWidth = currentWidth
    }

    private fun clearCurrentPage() {
        AlertDialog.Builder(this)
            .setTitle("清除批注")
            .setMessage("确定要清除当前页面的所有批注吗？")
            .setPositiveButton("确定") { _, _ ->
                annotationOverlay.pageAnnotations = PageAnnotations()
                annotationOverlay.annotationDirty = true
                annotationOverlay.invalidate()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun updatePageInfo() {
        tvPageInfo.text = "${currentPage + 1} / $pageCount"
        // 未保存指示
        btnSave.setTextColor(if (annotationOverlay.annotationDirty) Color.parseColor("#FF5252") else Color.parseColor("#4CAF50"))
    }

    private fun finishWithSave() {
        if (annotationOverlay.annotationDirty) pdfAnnotations.pages[currentPage] = annotationOverlay.pageAnnotations
        annotationsFile?.let {
            try { it.writeText(pdfAnnotations.toJson()) } catch (_: Exception) {}
        }
        finish()
    }

    /** 保存批注到 JSON 文件 */
    private fun saveAnnotations() {
        if (annotationOverlay.annotationDirty) pdfAnnotations.pages[currentPage] = annotationOverlay.pageAnnotations
        annotationsFile?.let {
            try { it.writeText(pdfAnnotations.toJson()) } catch (_: Exception) {}
        }
        annotationOverlay.annotationDirty = false
        updatePageInfo()
        Toast.makeText(this, "已保存", Toast.LENGTH_SHORT).show()
    }

    /** 分享 PDF 原始文件 */
    private fun sharePdf() {
        try {
            val uri = androidx.core.content.FileProvider.getUriForFile(
                this, "$packageName.fileprovider", File(pdfPath)
            )
            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(android.content.Intent.EXTRA_STREAM, uri)
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(android.content.Intent.createChooser(intent, "分享 PDF"))
        } catch (e: Exception) {
            Toast.makeText(this, "分享失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /** 页面跳转对话框 */
    private fun showPageJumpDialog() {
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            hint = "输入页码 (1-$pageCount)"
            setSelectAllOnFocus(true)
        }
        AlertDialog.Builder(this)
            .setTitle("跳转到页面")
            .setView(input)
            .setPositiveButton("跳转") { _, _ ->
                val page = input.text.toString().toIntOrNull()
                if (page != null && page in 1..pageCount) {
                    vpPdf.setCurrentItem(page - 1, true)
                } else {
                    Toast.makeText(this, "请输入 1-$pageCount 之间的页码", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    /** 旋转页面 90°（0→90→180→270→0） */
    private fun rotatePage() {
        rotationAngle = (rotationAngle + 90) % 360
        vpPdf.adapter?.notifyItemChanged(currentPage)
        annotationOverlay.invalidate()
        Toast.makeText(this, "旋转 ${rotationAngle}°", Toast.LENGTH_SHORT).show()
    }

    /** 切换滚动模式：翻页 ↔ 连续滚动 */
    private fun toggleScrollMode(btn: TextView) {
        scrollMode = !scrollMode
        if (scrollMode) {
            vpPdf.orientation = ViewPager2.ORIENTATION_VERTICAL
            vpPdf.offscreenPageLimit = 2
            btn.text = "📜"
            btn.alpha = 1.0f
            // 退出批注模式，避免手势冲突
            annotationOverlay.currentTool = AnnotationOverlay.Tool.NONE
            resetToolButtons()
            vpPdf.isUserInputEnabled = true
            Toast.makeText(this, "连续滚动 — 上下滑动翻页", Toast.LENGTH_SHORT).show()
        } else {
            vpPdf.orientation = ViewPager2.ORIENTATION_HORIZONTAL
            vpPdf.offscreenPageLimit = 1
            btn.text = "📄"
            btn.alpha = 0.5f
            Toast.makeText(this, "翻页模式", Toast.LENGTH_SHORT).show()
        }
        // 刷新当前页
        vpPdf.adapter?.notifyItemChanged(currentPage)
    }

    /** 书签：切换当前页收藏状态 */
    private fun toggleBookmark() {
        if (bookmarks.contains(currentPage)) {
            bookmarks.remove(currentPage)
        } else {
            bookmarks.add(currentPage)
        }
        updateBookmarkIcon()
        saveBookmarks()
        if (bookmarks.isNotEmpty()) {
            Toast.makeText(this, "书签: ${bookmarks.size} 页", Toast.LENGTH_SHORT).show()
        }
    }

    /** 长按书签按钮显示书签列表 */
    private fun showBookmarkList() {
        if (bookmarks.isEmpty()) {
            Toast.makeText(this, "暂无书签", Toast.LENGTH_SHORT).show()
            return
        }
        val sorted = bookmarks.sorted()
        val items = sorted.map { "第 ${it + 1} 页" }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("书签 (${bookmarks.size})")
            .setItems(items) { _, idx ->
                vpPdf.setCurrentItem(sorted[idx], true)
            }
            .setPositiveButton("清除全部") { _, _ ->
                bookmarks.clear()
                updateBookmarkIcon()
                saveBookmarks()
            }
            .setNegativeButton("关闭", null)
            .show()
    }

    private fun updateBookmarkIcon() {
        if (bookmarks.contains(currentPage)) {
            btnBookmark.text = "⭐"
            btnBookmark.alpha = 1.0f
        } else {
            btnBookmark.text = "🔖"
            btnBookmark.alpha = 0.5f
        }
    }

    private fun saveBookmarks() {
        prefs?.edit()?.putString("pdf_bookmarks_${pdfPath.hashCode()}", bookmarks.joinToString(","))?.apply()
    }

    private fun loadBookmarks() {
        val saved = prefs?.getString("pdf_bookmarks_${pdfPath.hashCode()}", "")?.takeIf { it.isNotEmpty() }
        if (saved != null) {
            bookmarks.addAll(saved.split(",").mapNotNull { it.toIntOrNull() })
        }
    }

    /** 截图当前页保存为 PNG */
    private fun screenshotPage() {
        try {
            val renderer = pdfRenderer ?: return
            val page = renderer.openPage(currentPage) ?: return
            val scale = (1600f / page.width.coerceAtLeast(page.height)).coerceAtMost(1f)
            val bw = (page.width * scale).toInt().coerceAtLeast(1)
            val bh = (page.height * scale).toInt().coerceAtLeast(1)
            val bitmap = Bitmap.createBitmap(bw, bh, Bitmap.Config.ARGB_8888)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()

            val filename = "PDF_${File(pdfPath).nameWithoutExtension}_p${currentPage + 1}.png"
            val dir = File(android.os.Environment.getExternalStoragePublicDirectory(
                android.os.Environment.DIRECTORY_PICTURES
            ), "MyNote")
            if (!dir.exists()) dir.mkdirs()
            val outFile = File(dir, filename)
            outFile.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()

            // 通知相册
            val values = android.content.ContentValues().apply {
                put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, filename)
                put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(android.provider.MediaStore.Images.Media.RELATIVE_PATH, "Pictures/MyNote")
            }
            contentResolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

            Toast.makeText(this, "已保存: $filename", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "截图失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /** 自动保存：每 2 分钟检查并保存 */
    private fun startAutoSave() {
        autoSaveHandler = Handler(Looper.getMainLooper())
        autoSaveRunnable = object : Runnable {
            override fun run() {
                if (annotationOverlay.annotationDirty) {
                    if (annotationOverlay.annotationDirty) pdfAnnotations.pages[currentPage] = annotationOverlay.pageAnnotations
                    annotationsFile?.let {
                        try { it.writeText(pdfAnnotations.toJson()) } catch (_: Exception) {}
                    }
                    annotationOverlay.annotationDirty = false
                    updatePageInfo()
                }
                autoSaveHandler?.postDelayed(this, 120_000) // 2 分钟
            }
        }
        autoSaveHandler?.postDelayed(autoSaveRunnable!!, 120_000)
    }

    private fun stopAutoSave() {
        autoSaveHandler?.removeCallbacks(autoSaveRunnable ?: return)
        autoSaveHandler = null
        autoSaveRunnable = null
    }

    private val prefs by lazy { getSharedPreferences("mynote_pdf_prefs", MODE_PRIVATE) }

    /** 导出所有页面（含批注）到新 PDF */
    private fun exportPdfWithAnnotations() {
        val renderer = pdfRenderer ?: return
        saveAnnotations() // 先确保当前批注已保存
        if (pdfAnnotations.pages.isEmpty() && annotationOverlay.pageAnnotations.let {
            it.strokes.isEmpty() && it.highlights.isEmpty() && it.texts.isEmpty() && it.shapes.isEmpty() && it.lines.isEmpty()
        }) {
            Toast.makeText(this, "无批注可导出", Toast.LENGTH_SHORT).show()
            return
        }

        val dialog = android.app.ProgressDialog(this).apply {
            setMessage("正在导出...")
            setProgressStyle(android.app.ProgressDialog.STYLE_HORIZONTAL)
            max = pageCount
            setCancelable(false)
            show()
        }

        Thread {
            try {
                val pdfDoc = android.graphics.pdf.PdfDocument()
                for (i in 0 until pageCount) {
                    val page = renderer.openPage(i) ?: continue
                    val pw = page.width; val ph = page.height

                    val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(pw, ph, i + 1).create()
                    val pdfPage = pdfDoc.startPage(pageInfo)
                    val canvas = pdfPage.canvas

                    // 1. 渲染 PDF 页面
                    val bitmap = Bitmap.createBitmap(pw, ph, Bitmap.Config.ARGB_8888)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    canvas.drawBitmap(bitmap, 0f, 0f, null)
                    bitmap.recycle()
                    page.close()

                    // 2. 绘制批注
                    val annotations = pdfAnnotations.pages[i] ?: PageAnnotations()
                    drawAnnotations(canvas, annotations, pw.toFloat(), ph.toFloat())

                    pdfDoc.finishPage(pdfPage)

                    runOnUiThread { dialog.progress = i + 1 }
                }

                // 保存文件
                val outFile = File(
                    File(pdfPath).parent,
                    "${File(pdfPath).nameWithoutExtension}_批注.pdf"
                )
                pdfDoc.writeTo(outFile.outputStream())
                pdfDoc.close()

                runOnUiThread {
                    dialog.dismiss()
                    Toast.makeText(this, "已导出: ${outFile.name}", Toast.LENGTH_LONG).show()
                    // 用系统查看器打开导出的 PDF
                    try {
                        val uri = androidx.core.content.FileProvider.getUriForFile(
                            this, "$packageName.fileprovider", outFile
                        )
                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                            setDataAndType(uri, "application/pdf")
                            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        startActivity(intent)
                    } catch (_: Exception) {}
                }
            } catch (e: Exception) {
                runOnUiThread {
                    dialog.dismiss()
                    Toast.makeText(this, "导出失败: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    /** 将批注绘制到 Canvas 上 */
    private fun drawAnnotations(
        canvas: android.graphics.Canvas,
        ann: PageAnnotations,
        pageW: Float, pageH: Float
    ) {
        // 手写笔划
        for (s in ann.strokes) {
            val paint = android.graphics.Paint().apply {
                color = s.color; strokeWidth = s.width; style = android.graphics.Paint.Style.STROKE
                strokeCap = android.graphics.Paint.Cap.ROUND; strokeJoin = android.graphics.Paint.Join.ROUND
                isAntiAlias = true
            }
            val path = android.graphics.Path()
            for (i in s.points.indices) {
                if (i == 0) path.moveTo(s.points[i][0], s.points[i][1])
                else path.lineTo(s.points[i][0], s.points[i][1])
            }
            canvas.drawPath(path, paint)
        }

        // 高亮
        for (h in ann.highlights) {
            val paint = android.graphics.Paint().apply {
                color = h.color; alpha = 80; style = android.graphics.Paint.Style.FILL
                isAntiAlias = true
            }
            canvas.drawRect(h.x * pageW, h.y * pageH, (h.x + h.w) * pageW, (h.y + h.h) * pageH, paint)
        }

        // 形状
        for (s in ann.shapes) {
            val paint = android.graphics.Paint().apply {
                color = s.color; strokeWidth = s.width; style = android.graphics.Paint.Style.STROKE
                isAntiAlias = true
            }
            if (s.type == "circle") {
                canvas.drawOval(s.x * pageW, s.y * pageH, (s.x + s.w) * pageW, (s.y + s.h) * pageH, paint)
            } else {
                canvas.drawRect(s.x * pageW, s.y * pageH, (s.x + s.w) * pageW, (s.y + s.h) * pageH, paint)
            }
        }

        // 下划线/删除线
        for (line in ann.lines) {
            val paint = android.graphics.Paint().apply {
                color = line.color; strokeWidth = line.width; isAntiAlias = true
            }
            val y = if (line.style == "strikethrough") line.y * pageH else (line.y * pageH + line.width * 2)
            canvas.drawLine(line.x * pageW, y, (line.x + line.w) * pageW, y, paint)
        }

        // 文字
        for (t in ann.texts) {
            val paint = android.graphics.Paint().apply {
                color = t.color; textSize = t.fontSize * pageW / 800f; isAntiAlias = true
            }
            val lines = t.text.split("\n")
            val lineH = paint.textSize * 1.3f
            for (li in lines.indices) {
                canvas.drawText(lines[li], t.x * pageW, t.y * pageH + paint.textSize + li * lineH, paint)
            }
        }
    }

    override fun onBackPressed() { finishWithSave() }

    override fun onDestroy() {
        super.onDestroy()
        stopAutoSave()
        pdfRenderer?.close()
    }

    // ===== Adapter =====
    inner class PageAdapter : RecyclerView.Adapter<PageHolder>() {
        override fun getItemCount() = pageCount

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageHolder {
            return PageHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_pdf_page, parent, false))
        }

        override fun onBindViewHolder(holder: PageHolder, position: Int) {
            val iv = holder.itemView.findViewById<ImageView>(R.id.iv_page)
            // 先清空避免显示上一页内容
            iv.setImageBitmap(null)
            renderPage(position, iv)
        }
    }

    inner class PageHolder(view: View) : RecyclerView.ViewHolder(view)

    private fun renderPage(position: Int, iv: ImageView) {
        val renderer = pdfRenderer ?: return
        val page = renderer.openPage(position) ?: return
        val tag = "page_$position"
        iv.tag = tag

        try {
            val pw = page.width
            val ph = page.height
            val rotated = rotationAngle == 90 || rotationAngle == 270
            val rw = if (rotated) ph else pw
            val rh = if (rotated) pw else ph
            val scale = (MAX_RENDER_DIM.toFloat() / rw.coerceAtLeast(rh)).coerceAtMost(1f)
            val bw = (rw * scale).toInt().coerceAtLeast(1)
            val bh = (rh * scale).toInt().coerceAtLeast(1)

            val bitmap = try {
                Bitmap.createBitmap(bw, bh, Bitmap.Config.ARGB_8888)
            } catch (e: OutOfMemoryError) {
                val s2 = scale * 0.7f
                Bitmap.createBitmap(
                    (rw * s2).toInt().coerceAtLeast(1),
                    (rh * s2).toInt().coerceAtLeast(1),
                    Bitmap.Config.RGB_565
                )
            }

            val matrix = android.graphics.Matrix()
            val finalScale = Math.min(bitmap.width.toFloat() / rw, bitmap.height.toFloat() / rh)
            matrix.postScale(finalScale, finalScale)
            if (rotationAngle != 0) {
                matrix.postRotate(rotationAngle.toFloat(), bitmap.width / 2f, bitmap.height / 2f)
            }
            page.render(bitmap, null, matrix, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

            if (iv.tag == tag) {
                iv.setImageBitmap(bitmap)
            } else {
                bitmap.recycle()
            }
        } catch (e: Exception) {
            // 渲染失败，静默
        } finally {
            page.close()
        }
    }

    private fun showTextInputForBox(x: Float, y: Float, w: Float, h: Float) {
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            hint = "输入文字"
            setPadding(16, 16, 16, 16)
            gravity = Gravity.TOP or Gravity.START; minLines = 3; maxLines = 8
        }
        AlertDialog.Builder(this)
            .setTitle("添加文字批注")
            .setView(input)
            .setPositiveButton("确定") { _, _ ->
                val text = input.text.toString().trim()
                if (text.isNotEmpty()) annotationOverlay.addTextAnnotation(x, y, w, h, text)
            }
            .setNegativeButton("取消", null).show()
    }

    private fun showEditTextDialog(text: TextAnnotation, index: Int) {
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            setText(text.text)
            setPadding(16, 16, 16, 16)
            gravity = Gravity.TOP or Gravity.START; minLines = 3; maxLines = 8
        }
        AlertDialog.Builder(this)
            .setTitle("编辑文字批注")
            .setView(input)
            .setPositiveButton("确定") { _, _ ->
                val newText = input.text.toString().trim()
                if (newText.isNotEmpty()) annotationOverlay.updateTextAnnotation(index, newText)
            }
            .setNegativeButton("取消", null).show()
    }

    private fun showColorPicker() {
        val colors = arrayOf("#000000","#FFFFFF","#F44336","#E91E63","#FF5722","#FF9800","#FFC107","#4CAF50","#8BC34A","#009688","#00BCD4","#2196F3","#03A9F4","#3F51B5","#673AB7","#9C27B0","#FF0000","#FF5252","#00E676","#FFD54F")
        val grid = android.widget.GridLayout(this).apply {
            columnCount = 5; rowCount = 4; orientation = android.widget.GridLayout.HORIZONTAL
            alignmentMode = android.widget.GridLayout.ALIGN_MARGINS; useDefaultMargins = true
            setPadding(16, 8, 16, 8)
        }
        for (c in colors) {
            val dot = View(this).apply {
                setBackgroundColor(Color.parseColor(c))
                layoutParams = android.widget.GridLayout.LayoutParams().apply {
                    width = 40 * resources.displayMetrics.density.toInt()
                    height = 40 * resources.displayMetrics.density.toInt()
                    setMargins(4,4,4,4)
                }
                setOnClickListener { currentColor = Color.parseColor(c); applyColor() }
            }
            grid.addView(dot)
        }
        AlertDialog.Builder(this).setTitle("选择颜色").setView(grid).setPositiveButton("关闭", null).show()
    }
}
