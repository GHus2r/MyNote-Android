package com.mynote.android.ui.lock.widget

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

/**
 * 无序漂浮的毛玻璃数字气泡键盘（0-9）
 * 支持加速漂浮 → 聚合成键盘排列 → 散开
 */
class FloatingBubbleKeypad @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {

    enum class State { FLOATING, SPEEDING, ARRANGING, ARRANGED, SCATTERING }

    private val bubbleRadius = 130f
    private val bubblePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 216f
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
        setShadowLayer(3f, 0f, 2f, Color.argb(80, 0, 0, 0))
    }
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(30, 0, 0, 0)
    }

    data class Bubble(
        val digit: Int,
        var x: Float,
        var y: Float,
        var vx: Float,
        var vy: Float,
        var targetX: Float = 0f,
        var targetY: Float = 0f,
        var alpha: Float = 0.85f,
        var scale: Float = 1f,
        var glowPhase: Float = Random.nextFloat()
    )

    private val bubbles = mutableListOf<Bubble>()
    private var animTime = 0f
    private var isActive = false

    private var state = State.FLOATING
    private var stateTimer = 0f
    private var speedBoost = 1f
    private var lerpProgress = 0f

    var onDigitTap: ((Int) -> Unit)? = null
    var onEmptyTap: (() -> Unit)? = null

    private val handler = Handler(Looper.getMainLooper())
    private val runnable = object : Runnable {
        override fun run() {
            if (!isActive) return
            animTime += 0.028f
            updateBubbles()
            invalidate()
            handler.postDelayed(this, 10)
        }
    }

    fun show() {
        if (bubbles.isEmpty()) initBubbles()
        isActive = true
        handler.post(runnable)
    }

    fun hide() {
        isActive = false
        handler.removeCallbacks(runnable)
    }

    /** 点击空白处 — 加速漂浮 */
    fun turboBoost() {
        if (state == State.ARRANGING || state == State.ARRANGED) return
        state = State.SPEEDING
        speedBoost = (speedBoost + 0.3f).coerceAtMost(3.5f)
        stateTimer = 0f
    }

    /** 持续点击后 — 聚合成键盘排列 */
    fun arrangeToKeypad() {
        if (state == State.ARRANGING || state == State.ARRANGED) return

        val w = width.toFloat()
        val h = height.toFloat()
        if (w == 0f || h == 0f) return

        val spacing = 175f
        val startX = w / 2f - spacing
        val startY = h / 2f - spacing * 1.6f

        // 电话拨号盘布局: 1-2-3, 4-5-6, 7-8-9, 0
        val layout = mapOf(
            1 to (0 to 0), 2 to (1 to 0), 3 to (2 to 0),
            4 to (0 to 1), 5 to (1 to 1), 6 to (2 to 1),
            7 to (0 to 2), 8 to (1 to 2), 9 to (2 to 2),
            0 to (1 to 3)
        )

        for (b in bubbles) {
            val (col, row) = layout[b.digit]!!
            b.targetX = startX + col * spacing
            b.targetY = startY + row * spacing
            b.vx = 0f
            b.vy = 0f
        }

        state = State.ARRANGING
        lerpProgress = 0f
        speedBoost = 1f
    }

    /** 停止点击后 — 散开回漂浮 */
    fun scatterAway() {
        if (state == State.SCATTERING || state == State.FLOATING) return

        for (b in bubbles) {
            b.vx = (Random.nextFloat() - 0.5f) * 10f
            b.vy = (Random.nextFloat() - 0.5f) * 10f
        }

        state = State.SCATTERING
        lerpProgress = 1f
        speedBoost = 1f
    }

    /** 当前是否处于聚合键盘状态 */
    val isArranged: Boolean get() = state == State.ARRANGED

    private fun initBubbles() {
        bubbles.clear()
        for (d in 0..9) {
            bubbles.add(Bubble(
                digit = d,
                x = Random.nextFloat() * 800,
                y = Random.nextFloat() * 1200,
                vx = (Random.nextFloat() - 0.5f) * 4f,
                vy = (Random.nextFloat() - 0.5f) * 4f
            ))
        }
    }

    private fun updateBubbles() {
        val w = width.toFloat()
        val h = height.toFloat()
        if (w == 0f || h == 0f) return

        stateTimer += 0.028f

        when (state) {
            State.FLOATING -> {
                // 正常漂浮物理
                applyPhysics(w, h)
            }

            State.SPEEDING -> {
                // 加速漂浮
                applyPhysics(w, h)
                // 速度回退: 3秒内无新点击则回到正常
                if (stateTimer > 3f) {
                    speedBoost = (speedBoost - 0.005f).coerceAtLeast(1f)
                    if (speedBoost <= 1f) {
                        state = State.FLOATING
                        speedBoost = 1f
                    }
                }
            }

            State.ARRANGING -> {
                // 平滑移动到键盘目标位置
                lerpProgress = (lerpProgress + 0.04f).coerceAtMost(1f)
                for (b in bubbles) {
                    b.x += (b.targetX - b.x) * 0.15f
                    b.y += (b.targetY - b.y) * 0.15f
                }
                // 全部接近目标 → 完成
                if (lerpProgress >= 0.98f) {
                    for (b in bubbles) {
                        b.x = b.targetX
                        b.y = b.targetY
                    }
                    state = State.ARRANGED
                    lerpProgress = 1f
                }
            }

            State.ARRANGED -> {
                // 停留在键盘位置，微小的呼吸浮动
                for (b in bubbles) {
                    b.x = b.targetX + sin(animTime * 0.5f + b.digit) * 5f
                    b.y = b.targetY + cos(animTime * 0.4f + b.digit) * 4f
                }
            }

            State.SCATTERING -> {
                // 爆炸散开 + 逐渐恢复物理
                lerpProgress = (lerpProgress - 0.015f).coerceAtLeast(0f)
                applyPhysics(w, h)
                if (lerpProgress <= 0f && stateTimer > 1.5f) {
                    state = State.FLOATING
                    lerpProgress = 0f
                    speedBoost = 1f
                }
            }
        }

        for (b in bubbles) {
            b.glowPhase += 0.02f
        }
    }

    private fun applyPhysics(w: Float, h: Float) {
        val spd = speedBoost
        for (b in bubbles) {
            b.vx += (Random.nextFloat() - 0.5f) * 0.15f * spd
            b.vy += (Random.nextFloat() - 0.5f) * 0.15f * spd
            b.vx = b.vx.coerceIn(-5f * spd, 5f * spd)
            b.vy = b.vy.coerceIn(-5f * spd, 5f * spd)

            b.x += b.vx
            b.y += b.vy

            // 边界弹回
            if (b.x < bubbleRadius) { b.x = bubbleRadius; b.vx = -b.vx }
            if (b.x > w - bubbleRadius) { b.x = w - bubbleRadius; b.vx = -b.vx }
            if (b.y < bubbleRadius) { b.y = bubbleRadius; b.vy = -b.vy }
            if (b.y > h - bubbleRadius) { b.y = h - bubbleRadius; b.vy = -b.vy }

            // 气泡间碰撞
            for (other in bubbles) {
                if (other === b) continue
                val dx = other.x - b.x
                val dy = other.y - b.y
                val dist = hypot(dx, dy)
                if (dist < bubbleRadius * 2f && dist > 0.1f) {
                    val overlap = bubbleRadius * 2f - dist
                    val nx = dx / dist
                    val ny = dy / dist
                    b.x -= nx * overlap * 0.5f
                    b.y -= ny * overlap * 0.5f
                    other.x += nx * overlap * 0.5f
                    other.y += ny * overlap * 0.5f
                    val dvx = b.vx - other.vx
                    val dvy = b.vy - other.vy
                    val dv = dvx * nx + dvy * ny
                    if (dv > 0) {
                        b.vx -= dv * nx * 0.5f
                        b.vy -= dv * ny * 0.5f
                        other.vx += dv * nx * 0.5f
                        other.vy += dv * ny * 0.5f
                    }
                }
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        for (b in bubbles) {
            drawBubble(canvas, b)
        }
    }

    private fun drawBubble(canvas: Canvas, b: Bubble) {
        val r = bubbleRadius * b.scale
        val alpha = (b.alpha * 255).toInt()

        // 投影
        shadowPaint.alpha = (alpha * 0.2f).toInt()
        canvas.drawCircle(b.x + 2f, b.y + 4f, r, shadowPaint)

        // 毛玻璃主体 - 奶白半透明
        val main = RadialGradient(b.x - r * 0.3f, b.y - r * 0.3f, r,
            intArrayOf(
                Color.argb((alpha * 0.55f).toInt(), 255, 255, 255),
                Color.argb((alpha * 0.30f).toInt(), 245, 240, 235),
                Color.argb((alpha * 0.12f).toInt(), 235, 225, 215)
            ),
            floatArrayOf(0f, 0.6f, 1f), Shader.TileMode.CLAMP)
        bubblePaint.shader = main
        canvas.drawCircle(b.x, b.y, r, bubblePaint)
        bubblePaint.shader = null

        // 边框 - 淡淡的暖灰边
        bubblePaint.style = Paint.Style.STROKE
        bubblePaint.strokeWidth = 1.5f
        bubblePaint.color = Color.argb((alpha * 0.35f).toInt(), 200, 190, 180)
        canvas.drawCircle(b.x, b.y, r - 1f, bubblePaint)
        bubblePaint.style = Paint.Style.FILL

        // 数字
        textPaint.alpha = alpha
        val yOffset = textPaint.textSize / 3f
        canvas.drawText(b.digit.toString(), b.x, b.y + yOffset, textPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isActive) return false
        if (state == State.ARRANGING) return true // 动画中拦截触摸

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                for (b in bubbles) {
                    val dx = event.x - b.x
                    val dy = event.y - b.y
                    if (hypot(dx, dy) < bubbleRadius) {
                        val anim = ValueAnimator.ofFloat(1f, 0.8f, 1f)
                        anim.duration = 150
                        anim.interpolator = DecelerateInterpolator()
                        anim.addUpdateListener { b.scale = it.animatedValue as Float; invalidate() }
                        anim.start()
                        onDigitTap?.invoke(b.digit)
                        return true
                    }
                }
                // 点击到空白区域
                onEmptyTap?.invoke()
            }
        }
        return false
    }
}
