package com.mynote.android.ui.lock.widget

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.random.Random

/**
 * 星空背景 View
 * 随机生成星星，缓慢移动 + 闪烁，营造沉浸式锁屏背景
 */
class StarfieldView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private data class Star(
        var x: Float,
        var y: Float,
        var size: Float,
        var alpha: Int,
        var twinkleSpeed: Float,
        var twinklePhase: Float,
        var driftSpeed: Float
    )

    private val stars = mutableListOf<Star>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private var animator: ValueAnimator? = null
    private val random = Random.Default

    private val bgPaint = Paint().apply {
        style = Paint.Style.FILL
    }

    init {
        setLayerType(LAYER_TYPE_HARDWARE, null)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w > 0 && h > 0) {
            generateStars(w, h)
        }
    }

    private fun generateStars(w: Int, h: Int) {
        stars.clear()
        val count = (w * h / 6000).coerceIn(60, 220)
        repeat(count) {
            stars.add(createStar(w, h))
        }
    }

    private fun createStar(w: Int, h: Int): Star {
        return Star(
            x = random.nextFloat() * w,
            y = random.nextFloat() * h,
            size = random.nextFloat() * 2.5f + 0.5f,
            alpha = random.nextInt(80, 220),
            twinkleSpeed = random.nextFloat() * 0.06f + 0.02f,
            twinklePhase = random.nextFloat() * Math.PI.toFloat() * 2f,
            driftSpeed = random.nextFloat() * 0.15f + 0.03f
        )
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        startAnimation()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopAnimation()
    }

    private fun startAnimation() {
        if (animator != null) return
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 16_000
            repeatMode = ValueAnimator.RESTART
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener { invalidate() }
            start()
        }
    }

    private fun stopAnimation() {
        animator?.cancel()
        animator = null
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // 深蓝到紫黑的渐变背景（用 Paint 的 shader 实现）
        if (bgPaint.shader == null) {
            bgPaint.shader = android.graphics.LinearGradient(
                0f, 0f, 0f, height.toFloat(),
                intArrayOf(
                    0xFF0B1026.toInt(),
                    0xFF1A1F3D.toInt(),
                    0xFF0D0D1A.toInt()
                ),
                floatArrayOf(0f, 0.5f, 1f),
                android.graphics.Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        val time = System.currentTimeMillis() / 1000f
        val width = width.toFloat()
        val height = height.toFloat()

        for (star in stars) {
            val twinkle = (kotlin.math.sin(time * star.twinkleSpeed + star.twinklePhase) + 1f) / 2f
            val currentAlpha = (star.alpha * (0.4f + 0.6f * twinkle)).toInt().coerceIn(20, 255)
            paint.color = 0xFFFFFF or (currentAlpha shl 24)

            // 缓慢向下/右漂移
            star.y += star.driftSpeed
            star.x += star.driftSpeed * 0.3f
            if (star.y > height) {
                star.y = -star.size
                star.x = random.nextFloat() * width
            }
            if (star.x > width) {
                star.x = 0f
            }

            canvas.drawCircle(star.x, star.y, star.size, paint)
        }
    }
}
