package com.mynote.android.ui.lock.widget

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import kotlin.math.max
import kotlin.math.sin

/**
 * 超声波指纹样式动画
 * - 中心指纹图标 + 同心波纹扩散
 */
class FingerprintRingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {

    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val ringCount = 3
    private var time = 0f
    private val handler = Handler(Looper.getMainLooper())

    // 脉冲动画 0→1→0
    private val pulseAnimator = ValueAnimator.ofFloat(0f, 1f, 0f).apply {
        duration = 2000
        repeatCount = ValueAnimator.INFINITE
        interpolator = DecelerateInterpolator()
        start()
    }

    private val runnable = object : Runnable {
        override fun run() {
            time += 0.03f
            invalidate()
            handler.postDelayed(this, 16)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        handler.post(runnable)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        handler.removeCallbacks(runnable)
        pulseAnimator.cancel()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        val maxRadius = minOf(cx, cy) * 0.9f

        val pulse = pulseAnimator.animatedValue as Float

        // 背景光晕
        glowPaint.shader = RadialGradient(cx, cy, maxRadius,
            intArrayOf(Color.argb(60, 100, 200, 255), Color.argb(20, 50, 120, 200), Color.TRANSPARENT),
            floatArrayOf(0.2f, 0.6f, 1f), Shader.TileMode.CLAMP)
        canvas.drawCircle(cx, cy, maxRadius, glowPaint)

        // 同心波纹
        for (i in 0 until ringCount) {
            val phase = (time * 1.5f + i.toFloat() / ringCount) % 1f
            val radius = maxRadius * 0.3f + phase * maxRadius * 0.5f
            val alpha = ((1f - phase) * 150 * (0.5f + 0.5f * pulse)).toInt()

            ringPaint.color = Color.argb(alpha, 100, 200, 255)
            ringPaint.strokeWidth = 3f + (1f - phase) * 3f
            canvas.drawCircle(cx, cy, radius, ringPaint)
        }

        // 中心圆
        ringPaint.color = Color.argb((80 + 40 * pulse).toInt(), 100, 200, 255)
        ringPaint.strokeWidth = 4f
        canvas.drawCircle(cx, cy, maxRadius * 0.18f, ringPaint)

        // 指纹纹理模拟 - 几道弧线
        ringPaint.color = Color.argb((100 + 50 * pulse).toInt(), 150, 220, 255)
        ringPaint.strokeWidth = 2f
        for (j in 0 until 4) {
            val r = maxRadius * (0.22f + j * 0.06f)
            val startAngle = -40f + j * 15f + sin(time + j) * 5f
            val sweepAngle = 260f - j * 20f
            canvas.drawArc(cx - r, cy - r, cx + r, cy + r, startAngle, sweepAngle, false, ringPaint)
        }
    }
}
