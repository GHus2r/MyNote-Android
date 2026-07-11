package com.mynote.android.ui.pdf

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs

class AnnotationOverlay @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    enum class Tool { NONE, PEN, HIGHLIGHTER, TEXT, ERASER, RECTANGLE, CIRCLE, UNDERLINE, STRIKETHROUGH }

    var currentTool: Tool = Tool.NONE
    var currentColor: Int = Color.parseColor("#FF0000")
    var currentWidth: Float = 3f
    var onDrawingModeChanged: ((Boolean) -> Unit)? = null

    /** 文字批注创建回调：传入归一化坐标的文本框区域 */
    var onTextCreate: ((Float, Float, Float, Float) -> Unit)? = null
    /** 点击已有文本框回调：传入 TextAnnotation 和索引 */
    var onTextEdit: ((TextAnnotation, Int) -> Unit)? = null

    var pageAnnotations: PageAnnotations = PageAnnotations()
        set(value) { field = value; invalidate() }
    var annotationDirty = false

    // ===== Paint =====
    private val penPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND }
    private val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; alpha = 80 }
    private val shapePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
    private val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; textSize = 36f }
    private val textBoxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 2f; color = Color.parseColor("#4FC3F7") }
    private val textBoxFill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; color = Color.parseColor("#10FFFFFF") }
    private val handlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; color = Color.parseColor("#4FC3F7") }

    // ===== 状态 =====
    private var currentStroke: StrokeAnnotation? = null
    private val drawingPath = Path()
    private var dragRect: RectF? = null
    private var dragStartX = 0f
    private var dragStartY = 0f

    // 文字拖动/缩放状态
    private var draggingText: TextAnnotation? = null
    private var draggingTextIdx: Int = -1
    private var dragMode: Int = 0 // 0: 移动, 1: 右下角缩放
    private var textDragStartX = 0f; private var textDragStartY = 0f
    private var textOrigX = 0f; private var textOrigY = 0f
    private var textOrigW = 0f; private var textOrigH = 0f
    private var textHasMoved = false

    init { setWillNotDraw(false) }

    // ===== 绘制 =====
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val vw = width.toFloat().coerceAtLeast(1f)
        val vh = height.toFloat().coerceAtLeast(1f)
        val density = context.resources.displayMetrics.density

        // 高亮
        for (hl in pageAnnotations.highlights) { highlightPaint.color = hl.color; canvas.drawRect(hl.x*vw, hl.y*vh, (hl.x+hl.w)*vw, (hl.y+hl.h)*vh, highlightPaint) }

        // 手写
        for (s in pageAnnotations.strokes) {
            if (s.points.size < 2) continue; penPaint.color = s.color; penPaint.strokeWidth = s.width
            val p = Path().apply { moveTo(s.points[0][0], s.points[0][1]); for (i in 1 until s.points.size) lineTo(s.points[i][0], s.points[i][1]) }
            canvas.drawPath(p, penPaint)
        }

        // 文字文本框
        for (t in pageAnnotations.texts) {
            val l = t.x * vw; val top = t.y * vh; val r = (t.x + t.w) * vw; val b = (t.y + t.h) * vh
            canvas.drawRoundRect(RectF(l, top, r, b), 4f, 4f, textBoxFill)
            canvas.drawRoundRect(RectF(l, top, r, b), 4f, 4f, textBoxPaint)

            // 右下角缩放把手
            val hSz = 16f * density
            canvas.drawCircle(r, b, hSz, handlePaint)

            // 文字
            textPaint.color = t.color; textPaint.textSize = t.fontSize * density
            val txtW = (r - l - 8f * density).coerceAtLeast(1f)
            val layout = StaticLayout.Builder.obtain(t.text, 0, t.text.length, textPaint, txtW.toInt())
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(0f, 1f).setIncludePad(false).build()
            canvas.save()
            canvas.translate(l + 4f * density, top + 4f * density)
            layout.draw(canvas)
            canvas.restore()
        }

        // 形状框
        for (sh in pageAnnotations.shapes) {
            shapePaint.color = sh.color; shapePaint.strokeWidth = sh.width
            val l = sh.x*vw; val t = sh.y*vh; val r = (sh.x+sh.w)*vw; val b = (sh.y+sh.h)*vh
            if (sh.type == "circle") { val cx=(l+r)/2f; val cy=(t+b)/2f; canvas.drawOval(RectF(cx-(r-l)/2f, cy-(b-t)/2f, cx+(r-l)/2f, cy+(b-t)/2f), shapePaint) }
            else canvas.drawRect(l, t, r, b, shapePaint)
        }

        // 下划线/删除线
        for (ln in pageAnnotations.lines) {
            linePaint.color = ln.color; linePaint.strokeWidth = ln.width
            val ly = if (ln.style=="strikethrough") ln.y*vh else ln.y*vh + 4f*ln.width
            canvas.drawLine(ln.x*vw, ly, (ln.x+ln.w)*vw, ly, linePaint)
        }

        // 绘制中的矩形
        dragRect?.let {
            if (currentTool == Tool.HIGHLIGHTER) { highlightPaint.color = currentColor; canvas.drawRect(it, highlightPaint) }
            else if (currentTool == Tool.RECTANGLE || currentTool == Tool.TEXT) { shapePaint.color = currentColor; shapePaint.strokeWidth = currentWidth; canvas.drawRect(it, shapePaint) }
            else if (currentTool == Tool.CIRCLE) { shapePaint.color = currentColor; shapePaint.strokeWidth = currentWidth; canvas.drawOval(it, shapePaint) }
            else if (currentTool == Tool.UNDERLINE || currentTool == Tool.STRIKETHROUGH) {
                linePaint.color = currentColor; linePaint.strokeWidth = currentWidth
                canvas.drawLine(it.left, if (currentTool==Tool.STRIKETHROUGH) (it.top+it.bottom)/2f else it.bottom, it.right, if (currentTool==Tool.STRIKETHROUGH) (it.top+it.bottom)/2f else it.bottom, linePaint)
            }
        }

        if (currentStroke != null && !drawingPath.isEmpty) { penPaint.color = currentColor; penPaint.strokeWidth = currentWidth; canvas.drawPath(drawingPath, penPaint) }
    }

    // ===== 触摸分发 =====
    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (currentTool == Tool.NONE && event.action == MotionEvent.ACTION_DOWN) {
            val hitIdx = findTextAt(event.x, event.y)
            if (hitIdx >= 0) {
                parent.requestDisallowInterceptTouchEvent(true)
            }
        }
        return super.dispatchTouchEvent(event)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x; val y = event.y

        if (currentTool == Tool.NONE) return handleNoneMode(event, x, y)

        when (currentTool) {
            Tool.PEN -> handlePen(event, x, y)
            Tool.HIGHLIGHTER -> handleDrag(event, x, y, Tool.HIGHLIGHTER)
            Tool.TEXT -> handleDrag(event, x, y, Tool.TEXT)
            Tool.ERASER -> handleEraser(event, x, y)
            Tool.RECTANGLE -> handleDrag(event, x, y, Tool.RECTANGLE)
            Tool.CIRCLE -> handleDrag(event, x, y, Tool.CIRCLE)
            Tool.UNDERLINE -> handleDrag(event, x, y, Tool.UNDERLINE)
            Tool.STRIKETHROUGH -> handleDrag(event, x, y, Tool.STRIKETHROUGH)
            else -> {}
        }
        return true
    }

    // ===== NONE 模式：拖动/点击文本框 =====
    private fun handleNoneMode(event: MotionEvent, x: Float, y: Float): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                val idx = findTextAt(x, y)
                if (idx < 0) return false
                val t = pageAnnotations.texts[idx]
                draggingText = t; draggingTextIdx = idx
                textDragStartX = x; textDragStartY = y
                textOrigX = t.x; textOrigY = t.y; textOrigW = t.w; textOrigH = t.h
                textHasMoved = false

                // 检测是否点击了右下角把手
                val vw = width.toFloat().coerceAtLeast(1f); val vh = height.toFloat().coerceAtLeast(1f)
                val hx = (t.x + t.w) * vw; val hy = (t.y + t.h) * vh
                dragMode = if (abs(x - hx) < 40f && abs(y - hy) < 40f) 1 else 0
                onDrawingModeChanged?.invoke(true)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val t = draggingText ?: return true
                val vw = width.toFloat().coerceAtLeast(1f); val vh = height.toFloat().coerceAtLeast(1f)
                val dx = (x - textDragStartX) / vw; val dy = (y - textDragStartY) / vh
                if (dragMode == 1) {
                    t.w = (textOrigW + dx).coerceAtLeast(0.05f)
                    t.h = (textOrigH + dy).coerceAtLeast(0.03f)
                } else {
                    t.x = (textOrigX + dx).coerceIn(0f, 1f - t.w)
                    t.y = (textOrigY + dy).coerceIn(0f, 1f - t.h)
                }
                if (abs(x - textDragStartX) > 15f || abs(y - textDragStartY) > 15f) textHasMoved = true
                annotationDirty = true; invalidate()
                return true
            }
            MotionEvent.ACTION_UP -> {
                if (!textHasMoved && draggingText != null) {
                    onTextEdit?.invoke(draggingText!!, draggingTextIdx)
                }
                draggingText = null; draggingTextIdx = -1
                onDrawingModeChanged?.invoke(false)
                return true
            }
            MotionEvent.ACTION_CANCEL -> { draggingText = null; draggingTextIdx = -1; onDrawingModeChanged?.invoke(false); return false }
        }
        return false
    }

    /** 查找点击位置附近的文本框 */
    private fun findTextAt(x: Float, y: Float): Int {
        val vw = width.toFloat().coerceAtLeast(1f); val vh = height.toFloat().coerceAtLeast(1f)
        val margin = 20f
        for (i in pageAnnotations.texts.indices.reversed()) {
            val t = pageAnnotations.texts[i]
            val l = t.x * vw - margin; val top = t.y * vh - margin
            val r = (t.x + t.w) * vw + margin; val b = (t.y + t.h) * vh + margin
            if (x >= l && x <= r && y >= top && y <= b) return i
        }
        return -1
    }

    /** 更新文本框文字 */
    fun updateTextAnnotation(index: Int, newText: String) {
        if (index in pageAnnotations.texts.indices) {
            pageAnnotations.texts[index].text = newText
            annotationDirty = true; invalidate()
        }
    }

    // ===== 手写 =====
    private fun handlePen(event: MotionEvent, x: Float, y: Float) {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> { currentStroke = StrokeAnnotation(mutableListOf(floatArrayOf(x, y)), currentColor, currentWidth); drawingPath.reset(); drawingPath.moveTo(x, y); parent.requestDisallowInterceptTouchEvent(true) }
            MotionEvent.ACTION_MOVE -> { currentStroke?.points?.add(floatArrayOf(x, y)); drawingPath.lineTo(x, y); invalidate() }
            MotionEvent.ACTION_UP -> { currentStroke?.let { if (it.points.size>1) { pageAnnotations.strokes.add(it); annotationDirty=true } }; currentStroke=null; drawingPath.reset(); parent.requestDisallowInterceptTouchEvent(false) }
        }
    }

    // ===== 拖拽绘制（高亮/形状框/线条/文本框）=====
    private fun handleDrag(event: MotionEvent, x: Float, y: Float, tool: Tool) {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> { dragStartX=x; dragStartY=y; parent.requestDisallowInterceptTouchEvent(true) }
            MotionEvent.ACTION_MOVE -> { dragRect=RectF(dragStartX.coerceAtMost(x), dragStartY.coerceAtMost(y), dragStartX.coerceAtLeast(x), dragStartY.coerceAtLeast(y)); invalidate() }
            MotionEvent.ACTION_UP -> {
                dragRect?.let { r ->
                    val vw=width.toFloat().coerceAtLeast(1f); val vh=height.toFloat().coerceAtLeast(1f)
                    when (tool) {
                        Tool.HIGHLIGHTER -> { if (r.width()>10 && r.height()>10) pageAnnotations.highlights.add(HighlightAnnotation(r.left/vw, r.top/vh, r.width()/vw, r.height()/vh, currentColor)) }
                        Tool.RECTANGLE -> { if (r.width()>10 && r.height()>10) pageAnnotations.shapes.add(ShapeAnnotation(r.left/vw, r.top/vh, r.width()/vw, r.height()/vh, currentColor, currentWidth, "rect")) }
                        Tool.CIRCLE -> { if (r.width()>10 && r.height()>10) pageAnnotations.shapes.add(ShapeAnnotation(r.left/vw, r.top/vh, r.width()/vw, r.height()/vh, currentColor, currentWidth, "circle")) }
                        Tool.UNDERLINE -> { if (r.width()>10) pageAnnotations.lines.add(LineAnnotation(r.left/vw, r.bottom/vh, r.width()/vw, currentColor, currentWidth, "underline")) }
                        Tool.STRIKETHROUGH -> { if (r.width()>10) pageAnnotations.lines.add(LineAnnotation(r.left/vw, (r.top+r.bottom)/(2f*vh), r.width()/vw, currentColor, currentWidth, "strikethrough")) }
                        Tool.TEXT -> {
                            if (r.width()>30 && r.height()>20)
                                onTextCreate?.invoke(r.left/vw, r.top/vh, r.width()/vw, r.height()/vh)
                        }
                        else -> {}
                    }
                    annotationDirty = true
                }
                dragRect=null; invalidate(); parent.requestDisallowInterceptTouchEvent(false)
            }
        }
    }

    // ===== 橡皮擦 =====
    private fun handleEraser(event: MotionEvent, x: Float, y: Float) {
        if (event.action != MotionEvent.ACTION_UP) return
        val t = 40f; val vw=width.toFloat().coerceAtLeast(1f); val vh=height.toFloat().coerceAtLeast(1f)
        fun chk(): Boolean {
            // 文本框
            for (i in pageAnnotations.texts.indices.reversed()) { val tx=pageAnnotations.texts[i]; val l=tx.x*vw-t; val tp=tx.y*vh-t; val r=(tx.x+tx.w)*vw+t; val b=(tx.y+tx.h)*vh+t; if (x>=l && x<=r && y>=tp && y<=b) { pageAnnotations.texts.removeAt(i); return true } }
            for (i in pageAnnotations.highlights.indices.reversed()) { val hl=pageAnnotations.highlights[i]; val l=hl.x*vw-t; val tp=hl.y*vh-t; val r=(hl.x+hl.w)*vw+t; val b=(hl.y+hl.h)*vh+t; if (x>=l && x<=r && y>=tp && y<=b) { pageAnnotations.highlights.removeAt(i); return true } }
            for (i in pageAnnotations.shapes.indices.reversed()) { val sh=pageAnnotations.shapes[i]; val l=sh.x*vw-t; val tp=sh.y*vh-t; val r=(sh.x+sh.w)*vw+t; val b=(sh.y+sh.h)*vh+t; if (x>=l && x<=r && y>=tp && y<=b) { pageAnnotations.shapes.removeAt(i); return true } }
            for (i in pageAnnotations.lines.indices.reversed()) { val ln=pageAnnotations.lines[i]; val ly=if (ln.style=="strikethrough") ln.y*vh else ln.y*vh+4f*ln.width; if (x>=ln.x*vw-t && x<=(ln.x+ln.w)*vw+t && abs(y-ly)<t) { pageAnnotations.lines.removeAt(i); return true } }
            for (i in pageAnnotations.strokes.indices.reversed()) { val s=pageAnnotations.strokes[i]; if (s.points.any { abs(x-it[0])<t && abs(y-it[1])<t }) { pageAnnotations.strokes.removeAt(i); return true } }
            return false
        }
        if (chk()) { annotationDirty=true; invalidate() }
    }

    fun addTextAnnotation(x: Float, y: Float, w: Float, h: Float, text: String) {
        pageAnnotations.texts.add(TextAnnotation(x, y, w, h, text, currentColor, 28f))
        annotationDirty = true; invalidate()
    }

    fun undo() {
        when {
            pageAnnotations.lines.isNotEmpty() -> pageAnnotations.lines.removeLast()
            pageAnnotations.shapes.isNotEmpty() -> pageAnnotations.shapes.removeLast()
            pageAnnotations.texts.isNotEmpty() -> pageAnnotations.texts.removeLast()
            pageAnnotations.highlights.isNotEmpty() -> pageAnnotations.highlights.removeLast()
            pageAnnotations.strokes.isNotEmpty() -> pageAnnotations.strokes.removeLast()
            else -> return
        }
        annotationDirty = true; invalidate()
    }
}
