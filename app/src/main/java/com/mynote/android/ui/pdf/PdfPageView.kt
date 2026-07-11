package com.mynote.android.ui.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.graphics.pdf.PdfRenderer
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import kotlin.math.abs

/**
 * 单个 PDF 页面的渲染 View
 * - 背景：PdfRenderer 渲染的 Bitmap
 * - 前景：批注层（手写 + 高亮 + 文字）
 * - 支持 pinch zoom + pan
 */
class PdfPageView(context: Context) : View(context) {

    companion object {
        private const val MAX_SCALE = 5f
        private const val MIN_SCALE = 0.5f
        private const val MAX_BITMAP_DIM = 2048 // 限制最大纹理尺寸，避免 OOM
    }

    // ===== 渲染 =====
    private var pageBitmap: Bitmap? = null
    private var pageWidth = 0
    private var pageHeight = 0

    /** 当前显示的 Bitmap 在 View 中的偏移 */
    private var offsetX = 0f
    private var offsetY = 0f
    private var scale = 1f

    // ===== 批注 =====
    var pageAnnotations: PageAnnotations = PageAnnotations()
    var annotationDirty = false

    // ===== 工具 =====
    enum class Tool { NONE, PEN, HIGHLIGHTER, TEXT, ERASER }
    var currentTool: Tool = Tool.NONE
        set(value) {
            field = value
            // 非绘制模式时关闭"禁止拦截触摸"（让 ViewPager 翻页）
            isDrawingMode = value != Tool.NONE
        }
    var currentColor: Int = Color.parseColor("#FF0000")
    var currentWidth: Float = 3f

    /** 调用方监听模式切换 */
    var onDrawingModeChanged: ((Boolean) -> Unit)? = null
    private var isDrawingMode = true
        set(value) {
            if (field != value) {
                field = value
                onDrawingModeChanged?.invoke(value)
            }
        }

    // ===== Paint =====
    private val penPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        xfermode = PorterDuffXfermode(PorterDuff.Mode.MULTIPLY)
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        textSize = 40f
    }
    private val eraseBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#26FFFFFF")
        xfermode = null
    }

    // ===== 绘制状态 =====
    private var currentStroke: StrokeAnnotation? = null
    private var drawingPath = Path()
    private var highlightStartX = 0f
    private var highlightStartY = 0f
    private var highlightRect: RectF? = null

    // ===== 手势 =====
    private val scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            if (currentTool != Tool.NONE) return false
            val newScale = (scale * detector.scaleFactor).coerceIn(MIN_SCALE, MAX_SCALE)
            val focusX = detector.focusX
            val focusY = detector.focusY
            offsetX = focusX - (focusX - offsetX) * (newScale / scale)
            offsetY = focusY - (focusY - offsetY) * (newScale / scale)
            scale = newScale
            clampOffset()
            invalidate()
            return true
        }
    })

    private var lastTouchX = 0f
    private var lastTouchY = 0f

    fun setPage(page: PdfRenderer.Page) {
        pageBitmap?.recycle()
        pageWidth = page.width
        pageHeight = page.height

        // 缩放到位图不超过 MAX_BITMAP_DIM，避免超大 PDF 页面 OOM
        val scale = (MAX_BITMAP_DIM.toFloat() / pageWidth.coerceAtLeast(pageHeight)).coerceAtMost(1f)
        val bw = (pageWidth * scale).toInt().coerceAtLeast(1)
        val bh = (pageHeight * scale).toInt().coerceAtLeast(1)

        try {
            val bitmap = Bitmap.createBitmap(bw, bh, Bitmap.Config.ARGB_8888)
            val matrix = android.graphics.Matrix()
            matrix.postScale(scale, scale)
            page.render(bitmap, null, matrix, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            pageBitmap = bitmap
            // 更新为实际渲染尺寸
            pageWidth = bw
            pageHeight = bh
        } catch (e: OutOfMemoryError) {
            // 内存不足时进一步缩小
            val smallerScale = scale * 0.5f
            val sbw = (page.width * smallerScale).toInt().coerceAtLeast(1)
            val sbh = (page.height * smallerScale).toInt().coerceAtLeast(1)
            try {
                val bitmap = Bitmap.createBitmap(sbw, sbh, Bitmap.Config.RGB_565) // 用 RGB_565 省一半内存
                val matrix = android.graphics.Matrix()
                matrix.postScale(smallerScale, smallerScale)
                page.render(bitmap, null, matrix, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                pageBitmap = bitmap
                pageWidth = sbw
                pageHeight = sbh
            } catch (e2: Exception) {
                // 完全无法渲染，保持空状态
                pageBitmap = null
            }
        } catch (e: Exception) {
            pageBitmap = null
        }

        fitToView()
    }

    fun recycleBitmap() {
        pageBitmap?.recycle()
        pageBitmap = null
    }

    private fun fitToView() {
        if (pageWidth == 0 || pageHeight == 0) return
        val vw = width.toFloat()
        val vh = height.toFloat()
        if (vw == 0f || vh == 0f) return
        val scaleX = vw / pageWidth
        val scaleY = vh / pageHeight
        scale = (scaleX.coerceAtMost(scaleY) * 0.92f).coerceIn(MIN_SCALE, MAX_SCALE)
        offsetX = (vw - pageWidth * scale) / 2f
        offsetY = (vh - pageHeight * scale) / 2f
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (oldw == 0 && oldh == 0) fitToView()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // 背景
        canvas.drawColor(Color.parseColor("#F0F0F0"))

        // PDF 页面
        pageBitmap?.let { bitmap ->
            canvas.save()
            canvas.translate(offsetX, offsetY)
            canvas.scale(scale, scale)
            canvas.drawBitmap(bitmap, 0f, 0f, null)

            // 批注（在 PDF 坐标系中绘制）
            drawAnnotations(canvas)
            canvas.restore()
        }
    }

    private fun drawAnnotations(canvas: Canvas) {
        val pw = pageWidth.toFloat()
        val ph = pageHeight.toFloat()

        // 高亮
        for (h in pageAnnotations.highlights) {
            highlightPaint.color = h.color
            highlightPaint.alpha = 80
            canvas.drawRect(h.x * pw, h.y * ph, (h.x + h.w) * pw, (h.y + h.h) * ph, highlightPaint)
        }

        // 手写笔划
        for (s in pageAnnotations.strokes) {
            if (s.points.size < 2) continue
            penPaint.color = s.color
            penPaint.strokeWidth = s.width / scale
            val path = Path()
            path.moveTo(s.points[0][0], s.points[0][1])
            for (i in 1 until s.points.size) {
                path.lineTo(s.points[i][0], s.points[i][1])
            }
            canvas.drawPath(path, penPaint)
        }

        // 文字批注
        for (t in pageAnnotations.texts) {
            textPaint.color = t.color
            textPaint.textSize = t.fontSize / scale
            canvas.drawText(t.text, t.x * pw, t.y * ph, textPaint)
        }

        // 绘制中的高亮矩形
        highlightRect?.let { rect ->
            highlightPaint.color = currentColor
            highlightPaint.alpha = 80
            canvas.drawRect(rect, highlightPaint)
        }

        // 绘制中的笔划
        if (currentStroke != null && !drawingPath.isEmpty) {
            penPaint.color = currentColor
            penPaint.strokeWidth = currentWidth / scale
            canvas.drawPath(drawingPath, penPaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (currentTool == Tool.NONE) {
            // 缩放模式
            scaleDetector.onTouchEvent(event)
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    lastTouchX = event.x
                    lastTouchY = event.y
                }
                MotionEvent.ACTION_MOVE -> {
                    if (event.pointerCount == 1) {
                        offsetX += event.x - lastTouchX
                        offsetY += event.y - lastTouchY
                        lastTouchX = event.x
                        lastTouchY = event.y
                        clampOffset()
                        invalidate()
                    }
                }
            }
            return true
        }

        // 批注模式
        val px = (event.x - offsetX) / scale
        val py = (event.y - offsetY) / scale

        when (currentTool) {
            Tool.PEN -> handlePen(event, px, py)
            Tool.HIGHLIGHTER -> handleHighlighter(event, px, py)
            Tool.TEXT -> handleText(event, px, py)
            Tool.ERASER -> handleEraser(event, px, py)
            else -> {}
        }
        return true
    }

    private fun handlePen(event: MotionEvent, px: Float, py: Float) {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                currentStroke = StrokeAnnotation(
                    mutableListOf(floatArrayOf(px, py)),
                    currentColor, currentWidth
                )
                drawingPath.reset()
                drawingPath.moveTo(px, py)
                parent.requestDisallowInterceptTouchEvent(true)
            }
            MotionEvent.ACTION_MOVE -> {
                currentStroke?.points?.add(floatArrayOf(px, py))
                drawingPath.lineTo(px, py)
                invalidate()
            }
            MotionEvent.ACTION_UP -> {
                currentStroke?.let { stroke ->
                    if (stroke.points.size > 1) {
                        pageAnnotations.strokes.add(stroke)
                        annotationDirty = true
                    }
                }
                currentStroke = null
                drawingPath.reset()
                parent.requestDisallowInterceptTouchEvent(false)
            }
        }
    }

    private fun handleHighlighter(event: MotionEvent, px: Float, py: Float) {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                highlightStartX = px
                highlightStartY = py
                parent.requestDisallowInterceptTouchEvent(true)
            }
            MotionEvent.ACTION_MOVE -> {
                highlightRect = RectF(
                    highlightStartX.coerceAtMost(px),
                    highlightStartY.coerceAtMost(py),
                    highlightStartX.coerceAtLeast(px),
                    highlightStartY.coerceAtLeast(py)
                )
                invalidate()
            }
            MotionEvent.ACTION_UP -> {
                highlightRect?.let { rect ->
                    if (rect.width() > 10 && rect.height() > 10) {
                        val pw = pageWidth.toFloat()
                        val ph = pageHeight.toFloat()
                        pageAnnotations.highlights.add(HighlightAnnotation(
                            rect.left / pw, rect.top / ph,
                            rect.width() / pw, rect.height() / ph,
                            currentColor
                        ))
                        annotationDirty = true
                    }
                }
                highlightRect = null
                invalidate()
                parent.requestDisallowInterceptTouchEvent(false)
            }
        }
    }

    private fun handleText(event: MotionEvent, px: Float, py: Float) {
        if (event.action == MotionEvent.ACTION_UP) {
            val pw = pageWidth.toFloat()
            val ph = pageHeight.toFloat()
            onTextTap?.invoke(px / pw, py / ph)
        }
    }

    private fun handleEraser(event: MotionEvent, px: Float, py: Float) {
        if (event.action != MotionEvent.ACTION_UP) return
        // 查找点击位置附近的批注并删除
        val threshold = 30f / scale
        val pw = pageWidth.toFloat()
        val ph = pageHeight.toFloat()

        // 检查文字批注
        val textIter = pageAnnotations.texts.iterator()
        while (textIter.hasNext()) {
            val t = textIter.next()
            if (abs(px - t.x * pw) < threshold * 2 && abs(py - t.y * ph) < threshold * 2) {
                textIter.remove()
                annotationDirty = true
                invalidate()
                return
            }
        }

        // 检查高亮
        val hlIter = pageAnnotations.highlights.iterator()
        while (hlIter.hasNext()) {
            val h = hlIter.next()
            if (px >= h.x * pw - threshold && px <= (h.x + h.w) * pw + threshold &&
                py >= h.y * ph - threshold && py <= (h.y + h.h) * ph + threshold) {
                hlIter.remove()
                annotationDirty = true
                invalidate()
                return
            }
        }
    }

    /** 文字批注点击回调 */
    var onTextTap: ((Float, Float) -> Unit)? = null

    /** 添加文字批注 */
    fun addTextAnnotation(x: Float, y: Float, text: String) {
        pageAnnotations.texts.add(TextAnnotation(x, y, 0.3f, 0.1f, text, currentColor, currentWidth * 12f))
        annotationDirty = true
        invalidate()
    }

    /** 撤销上一次批注 */
    fun undo() {
        when {
            pageAnnotations.texts.isNotEmpty() -> pageAnnotations.texts.removeLast()
            pageAnnotations.highlights.isNotEmpty() -> pageAnnotations.highlights.removeLast()
            pageAnnotations.strokes.isNotEmpty() -> pageAnnotations.strokes.removeLast()
            else -> return
        }
        annotationDirty = true
        invalidate()
    }

    private fun clampOffset() {
        val vw = width.toFloat()
        val vh = height.toFloat()
        val pw = pageWidth * scale
        val ph = pageHeight * scale
        if (pw > vw) {
            offsetX = offsetX.coerceIn(vw - pw, 0f)
        } else {
            offsetX = (vw - pw) / 2f
        }
        if (ph > vh) {
            offsetY = offsetY.coerceIn(vh - ph, 0f)
        } else {
            offsetY = (vh - ph) / 2f
        }
    }
}
