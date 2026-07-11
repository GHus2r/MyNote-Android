package com.mynote.android.ui.lock.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.View
import kotlin.math.sin

/**
 * 红→绿→蓝三色渐变背景 — 持续缓慢流动
 */
class RainbowGradientView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var shift = 0f
    private var animTime = 0f

    private val handler = Handler(Looper.getMainLooper())
    private val runnable = object : Runnable {
        override fun run() {
            animTime += 0.02f
            shift = sin(animTime) * 0.15f
            invalidate()
            handler.postDelayed(this, 40)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        handler.post(runnable)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        handler.removeCallbacks(runnable)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w == 0f || h == 0f) return

        // 红 → 绿 → 蓝 对角线渐变
        val positions = floatArrayOf(
            0f + shift,
            0.5f + shift * 0.5f,
            1f
        )
        val colors = intArrayOf(
            Color.rgb(220, 50, 50),   // 红
            Color.rgb(50, 180, 50),   // 绿
            Color.rgb(50, 80, 220),   // 蓝
        )

        paint.shader = LinearGradient(0f, 0f, w, h, colors, positions, Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, w, h, paint)
    }
}
