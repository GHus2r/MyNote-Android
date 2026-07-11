package com.mynote.android.ui.edit.widget

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.animation.OvershootInterpolator
import androidx.core.content.ContextCompat
import com.mynote.android.R

/**
 * 可拖拽、缩放、切换类型的形状 View
 *
 * 内容格式: "shape_type|size_dp|color_hex"  如 "circle|100|#2196F3"
 */
class ShapeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    companion object {
        private const val MIN_SIZE_DP = 30f
        private const val MAX_SIZE_DP = 320f
        private const val HANDLE_SIZE_DP = 32f
        private const val LONG_PRESS_MS = 500L
    }

    // ===== 属性 =====
    var shapeType: String = "circle"
        set(value) { field = value; invalidate() }

    var shapeColor: Int = ContextCompat.getColor(context, R.color.primary)
        set(value) { field = value; paint.color = value; invalidate() }

    var shapeSizeDp: Float = 100f
        private set

    /** 形状在视图内的偏移（dp），用于自由拖动 */
    var shapeOffsetX: Float = 0f
        private set
    var shapeOffsetY: Float = 0f
        private set

    /** 内容序列化 / 反序列化 */
    fun fromContent(content: String) {
        val parts = content.split("|")
        if (parts.size >= 2) {
            shapeType = parts[0]
            shapeSizeDp = parts.getOrNull(1)?.toFloatOrNull() ?: 100f
            shapeColor = try {
                Color.parseColor(parts.getOrNull(2) ?: "#2196F3")
            } catch (_: Exception) {
                ContextCompat.getColor(context, R.color.primary)
            }
            shapeOffsetX = parts.getOrNull(3)?.toFloatOrNull() ?: 0f
            shapeOffsetY = parts.getOrNull(4)?.toFloatOrNull() ?: 0f
        } else {
            shapeType = content
            shapeSizeDp = 100f
        }
    }

    fun toContent(): String = "$shapeType|${shapeSizeDp.toInt()}|#${Integer.toHexString(shapeColor).uppercase().takeLast(6)}|${shapeOffsetX.toInt()}|${shapeOffsetY.toInt()}"

    /** 外部回调：形状属性变更时触发 */
    var onShapeChanged: (() -> Unit)? = null

    /** 外部回调：双击时触发 */
    var onDoubleTap: (() -> Unit)? = null

    // ===== 手势检测 =====
    private val gestureListener = object : GestureDetector.SimpleOnGestureListener() {
        override fun onDoubleTap(e: MotionEvent): Boolean {
            onDoubleTap?.invoke()
            return true
        }
        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            isSelected = true
            return true
        }
    }
    private val gestureDetector = GestureDetector(context, gestureListener)
    private var scaleBeforeGesture = 100f
    private val scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
            scaleBeforeGesture = shapeSizeDp; return true
        }
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            shapeSizeDp = (scaleBeforeGesture * detector.scaleFactor).coerceIn(MIN_SIZE_DP, MAX_SIZE_DP)
            requestLayout(); onShapeChanged?.invoke(); return true
        }
    })

    // ===== 绘制 =====
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = ContextCompat.getColor(context, R.color.primary)
    }
    private val handlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        color = Color.parseColor("#80FFFFFF")
    }
    private val handleFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#26FFFFFF")
    }
    private val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.5f
        pathEffect = android.graphics.DashPathEffect(floatArrayOf(6f, 4f), 0f)
        color = Color.parseColor("#40FFFFFF")
    }
    private val path = Path()

    private val density = context.resources.displayMetrics.density
    private val handleSize = HANDLE_SIZE_DP * density

    // ===== 拖拽 & 缩放状态 =====
    private var isDragging = false
    private var isResizing = false
    private var touchStartX = 0f
    private var touchStartY = 0f
    private var offsetStartX = 0f
    private var offsetStartY = 0f
    private var initialSizeDp = 0f

    // ===== 长按 =====
    private val longPressHandler = Handler(Looper.getMainLooper())
    private var pendingLongPress: Runnable? = null
    private var longPressTriggered = false
    var onLongPress: (() -> Unit)? = null

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        val cx = w / 2f + shapeOffsetX * density
        val cy = h / 2f + shapeOffsetY * density
        val radius = (shapeSizeDp * density) / 2f

        paint.color = shapeColor

        // 绘制形状
        when (shapeType) {
            "circle" -> canvas.drawCircle(cx, cy, radius, paint)
            "square" -> canvas.drawRect(cx - radius, cy - radius, cx + radius, cy + radius, paint)
            "triangle" -> {
                path.reset()
                path.moveTo(cx, cy - radius)
                path.lineTo(cx + radius * 0.866f, cy + radius * 0.5f)
                path.lineTo(cx - radius * 0.866f, cy + radius * 0.5f)
                path.close()
                canvas.drawPath(path, paint)
            }
            "diamond" -> {
                path.reset()
                path.moveTo(cx, cy - radius)
                path.lineTo(cx + radius, cy)
                path.lineTo(cx, cy + radius)
                path.lineTo(cx - radius, cy)
                path.close()
                canvas.drawPath(path, paint)
            }
            "pentagon" -> drawPolygon(canvas, cx, cy, radius, 5)
            "hexagon" -> drawPolygon(canvas, cx, cy, radius, 6)
            "star" -> drawStar(canvas, cx, cy, radius, 5)
            "heart" -> drawHeart(canvas, cx, cy, radius)
            else -> canvas.drawCircle(cx, cy, radius, paint)
        }

        // 选中指示：虚线边框
        if (isSelected) {
            val outlineRadius = radius + 8f * density
            canvas.drawRect(
                cx - outlineRadius, cy - outlineRadius,
                cx + outlineRadius, cy + outlineRadius,
                outlinePaint
            )
        }

        // 右下角缩放把手
        val hx = cx + radius - handleSize / 2f
        val hy = cy + radius - handleSize / 2f
        canvas.drawRoundRect(hx, hy, hx + handleSize, hy + handleSize, 6f, 6f, handleFillPaint)
        canvas.drawRoundRect(hx, hy, hx + handleSize, hy + handleSize, 6f, 6f, handlePaint)

        // 把手内部图标（小三角）
        val iconX = hx + handleSize * 0.6f
        val iconY = hy + handleSize * 0.7f
        val iconR = handleSize * 0.22f
        path.reset()
        path.moveTo(iconX, iconY - iconR)
        path.lineTo(iconX + iconR * 0.866f, iconY + iconR * 0.5f)
        path.lineTo(iconX - iconR * 0.866f, iconY + iconR * 0.5f)
        path.close()
        canvas.drawPath(path, handlePaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(event)
        scaleDetector.onTouchEvent(event)

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                touchStartX = event.rawX
                touchStartY = event.rawY
                offsetStartX = shapeOffsetX
                offsetStartY = shapeOffsetY
                initialSizeDp = shapeSizeDp
                longPressTriggered = false

                // 检测是否点击了右下角把手区域（基于实际形状位置）
                val radius = (shapeSizeDp * density) / 2f
                val actualCx = width / 2f + shapeOffsetX * density
                val actualCy = height / 2f + shapeOffsetY * density
                val handleCenterX = actualCx + radius - handleSize / 2f
                val handleCenterY = actualCy + radius - handleSize / 2f
                val distX = event.x - handleCenterX
                val distY = event.y - handleCenterY
                isResizing = abs(distX) < handleSize * 1.2f && abs(distY) < handleSize * 1.2f
                isDragging = !isResizing

                // 启动长按计时
                if (isDragging) {
                    pendingLongPress = Runnable {
                        longPressTriggered = true
                        isDragging = false
                        isResizing = false
                        onLongPress?.invoke()
                    }
                    longPressHandler.postDelayed(pendingLongPress!!, LONG_PRESS_MS)
                }

                return true
            }

            MotionEvent.ACTION_MOVE -> {
                if (longPressTriggered) return true

                val dx = event.rawX - touchStartX
                val dy = event.rawY - touchStartY

                if (abs(dx) > 10f || abs(dy) > 10f) {
                    pendingLongPress?.let { longPressHandler.removeCallbacks(it) }
                    pendingLongPress = null
                }

                if (isResizing) {
                    // 右上方向放大，左下方向缩小
                    val delta = -(dx + dy) / density
                    shapeSizeDp = (initialSizeDp + delta * 0.6f)
                        .coerceIn(MIN_SIZE_DP, MAX_SIZE_DP)
                    requestLayout()
                    onShapeChanged?.invoke()
                } else if (isDragging && pendingLongPress == null) {
                    // 自由拖动形状位置
                    shapeOffsetX = offsetStartX + dx / density
                    shapeOffsetY = offsetStartY + dy / density
                    invalidate()
                    onShapeChanged?.invoke()
                }
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                pendingLongPress?.let { longPressHandler.removeCallbacks(it) }
                pendingLongPress = null
                if (!longPressTriggered) {
                    isDragging = false
                    isResizing = false
                }
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    /** 更新尺寸并触发 layout */
    fun updateSize(dp: Float) {
        shapeSizeDp = dp.coerceIn(MIN_SIZE_DP, MAX_SIZE_DP)
        requestLayout()
        onShapeChanged?.invoke()
    }

    // ===== 形状绘制方法 =====
    private fun drawPolygon(canvas: Canvas, cx: Float, cy: Float, r: Float, sides: Int) {
        path.reset()
        for (i in 0 until sides) {
            val angle = Math.PI * 2 * i / sides - Math.PI / 2
            val x = cx + (r * Math.cos(angle)).toFloat()
            val y = cy + (r * Math.sin(angle)).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        canvas.drawPath(path, paint)
    }

    private fun drawStar(canvas: Canvas, cx: Float, cy: Float, r: Float, points: Int) {
        path.reset()
        val outerRadius = r
        val innerRadius = r * 0.4f
        val count = points * 2
        for (i in 0 until count) {
            val angle = Math.PI * i / points - Math.PI / 2
            val radius = if (i % 2 == 0) outerRadius else innerRadius
            val x = cx + (radius * Math.cos(angle)).toFloat()
            val y = cy + (radius * Math.sin(angle)).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        canvas.drawPath(path, paint)
    }

    private fun drawHeart(canvas: Canvas, cx: Float, cy: Float, r: Float) {
        path.reset()
        val scale = r / 25f
        path.moveTo(cx, cy + 10 * scale)
        path.cubicTo(cx - 25 * scale, cy - 5 * scale, cx - 12 * scale, cy - 22 * scale, cx, cy - 12 * scale)
        path.cubicTo(cx + 12 * scale, cy - 22 * scale, cx + 25 * scale, cy - 5 * scale, cx, cy + 10 * scale)
        path.close()
        canvas.drawPath(path, paint)
    }

    private fun abs(value: Float): Float = if (value < 0) -value else value
}
