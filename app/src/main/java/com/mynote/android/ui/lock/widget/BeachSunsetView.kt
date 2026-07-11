package com.mynote.android.ui.lock.widget

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.cos
import kotlin.math.sin

/**
 * 海滩晚霞动画背景
 * - 天空从上到下：深蓝→紫→橙→金色晚霞渐变
 * - 太阳缓慢下沉
 * - 海面波浪
 * - 底部沙滩
 */
class BeachSunsetView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {

    private val skyPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val sunPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val sunGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val waterPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val waterShinePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val sandPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val cloudPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val shimmerPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var sunY = 0f
    private var waveOffset = 0f
    private var time = 0f
    private var shimmerAlpha = 0

    private val handler = Handler(Looper.getMainLooper())
    private val runnable = object : Runnable {
        override fun run() {
            time += 0.016f
            waveOffset += 2f
            sunY = height * 0.15f + sin(time * 0.3f) * height * 0.05f
            shimmerAlpha = ((sin(time * 2f) + 1f) * 60).toInt()
            invalidate()
            handler.postDelayed(this, 16)
        }
    }

    private val sunAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 30000
        repeatCount = ValueAnimator.INFINITE
        repeatMode = ValueAnimator.REVERSE
        interpolator = LinearInterpolator()
        addUpdateListener {
            sunY = height * 0.12f + it.animatedFraction * height * 0.15f
        }
        start()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        handler.post(runnable)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        handler.removeCallbacks(runnable)
        sunAnimator.cancel()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w == 0f || h == 0f) return

        drawSky(canvas, w, h)
        drawClouds(canvas, w, h)
        drawSun(canvas, w, h)
        drawSunReflection(canvas, w, h)
        drawOcean(canvas, w, h)
        drawSand(canvas, w, h)
    }

    private fun drawSky(canvas: Canvas, w: Float, h: Float) {
        val skyTop = h * 0.5f
        skyPaint.shader = LinearGradient(
            0f, 0f, 0f, skyTop,
            intArrayOf(
                Color.rgb(15, 20, 60),       // 顶部深蓝
                Color.rgb(60, 25, 80),       // 紫色
                Color.rgb(180, 50, 50),      // 红
                Color.rgb(255, 120, 40),     // 橙
                Color.rgb(255, 180, 60),     // 金色
            ),
            floatArrayOf(0f, 0.25f, 0.55f, 0.8f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, w, skyTop, skyPaint)
    }

    private fun drawClouds(canvas: Canvas, w: Float, h: Float) {
        cloudPaint.color = Color.argb(40, 255, 200, 150)
        cloudPaint.style = Paint.Style.FILL

        // 几朵飘动的云
        val cloudPositions = listOf(
            Triple(w * 0.2f, h * 0.12f, 1f),
            Triple(w * 0.55f, h * 0.08f, 0.7f),
            Triple(w * 0.75f, h * 0.15f, 1.3f),
            Triple(w * 0.4f, h * 0.18f, 0.5f),
        )
        for ((cx, cy, scale) in cloudPositions) {
            val x = cx + sin(time * 0.5f + cx * 0.01f) * 20f
            drawCloud(canvas, x, cy, scale)
        }
    }

    private fun drawCloud(canvas: Canvas, cx: Float, cy: Float, scale: Float) {
        val path = Path()
        val s = 40f * scale
        path.addCircle(cx, cy, s * 0.8f, Path.Direction.CW)
        path.addCircle(cx + s * 0.7f, cy - s * 0.2f, s * 0.6f, Path.Direction.CW)
        path.addCircle(cx + s * 1.2f, cy + s * 0.1f, s * 0.7f, Path.Direction.CW)
        path.addCircle(cx - s * 0.5f, cy + s * 0.15f, s * 0.5f, Path.Direction.CW)
        canvas.drawPath(path, cloudPaint)
    }

    private fun drawSun(canvas: Canvas, w: Float, h: Float) {
        val cx = w * 0.5f
        val cy = sunY
        val radius = w * 0.12f

        // 光晕
        sunGlowPaint.shader = RadialGradient(
            cx, cy, radius * 3f,
            intArrayOf(Color.argb(80, 255, 200, 80), Color.argb(40, 255, 150, 30), Color.TRANSPARENT),
            floatArrayOf(0.3f, 0.6f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(cx, cy, radius * 3f, sunGlowPaint)

        // 太阳本体
        sunPaint.shader = RadialGradient(
            cx, cy, radius,
            intArrayOf(Color.rgb(255, 255, 200), Color.rgb(255, 200, 60), Color.rgb(255, 140, 20)),
            floatArrayOf(0f, 0.6f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(cx, cy, radius, sunPaint)
    }

    private fun drawSunReflection(canvas: Canvas, w: Float, h: Float) {
        val waterLine = h * 0.5f
        val sx = w * 0.5f
        val sy = waterLine + 10f
        shimmerPaint.color = Color.argb(shimmerAlpha, 255, 200, 80)
        shimmerPaint.strokeWidth = 2f

        val refWidth = w * 0.25f
        val path = Path()
        path.moveTo(sx - refWidth, sy)
        for (i in 0..20) {
            val t = i / 20f
            val x = sx - refWidth + t * refWidth * 2f
            val y = sy + sin(t * 6f + time * 3f) * 6f + t * t * 40f
            path.lineTo(x, y)
        }
        path.lineTo(sx + refWidth, sy + 40f)
        path.lineTo(sx - refWidth, sy + 40f)
        path.close()
        canvas.drawPath(path, shimmerPaint)
    }

    private fun drawOcean(canvas: Canvas, w: Float, h: Float) {
        val waterLine = h * 0.5f

        waterPaint.shader = LinearGradient(
            0f, waterLine, 0f, h * 0.78f,
            intArrayOf(
                Color.rgb(20, 60, 120),
                Color.rgb(15, 40, 80),
                Color.rgb(10, 25, 50)
            ),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )

        // 大波浪
        val wavePath = Path()
        wavePath.moveTo(0f, waterLine)
        for (x in 0..w.toInt() step 4) {
            val xf = x.toFloat()
            val y = waterLine + sin(xf * 0.008f + waveOffset * 0.03f) * 12f +
                    sin(xf * 0.02f + time * 0.7f) * 8f +
                    cos(xf * 0.005f + time * 0.5f) * 6f
            wavePath.lineTo(xf, y)
        }
        wavePath.lineTo(w, h * 0.78f)
        wavePath.lineTo(0f, h * 0.78f)
        wavePath.close()
        canvas.drawPath(wavePath, waterPaint)

        // 水面的光泽线条
        waterShinePaint.color = Color.argb(30, 255, 200, 100)
        waterShinePaint.strokeWidth = 1f
        waterShinePaint.style = Paint.Style.STROKE
        val shinePath = Path()
        for (i in 0..8) {
            val sx = w * (0.1f + i * 0.1f)
            val sy = waterLine + sin(time * 2f + i) * 5f + 15f
            shinePath.moveTo(sx, sy)
            shinePath.lineTo(sx + 30f, sy + 2f)
        }
        canvas.drawPath(shinePath, waterShinePaint)
    }

    private fun drawSand(canvas: Canvas, w: Float, h: Float) {
        val sandLine = h * 0.78f
        sandPaint.shader = LinearGradient(
            0f, sandLine, 0f, h,
            intArrayOf(Color.rgb(180, 140, 100), Color.rgb(140, 100, 60), Color.rgb(100, 70, 40)),
            floatArrayOf(0f, 0.4f, 1f),
            Shader.TileMode.CLAMP
        )
        val path = Path()
        path.moveTo(0f, sandLine)
        for (x in 0..w.toInt() step 4) {
            val y = sandLine + sin(x.toFloat() * 0.01f) * 4f + cos(x.toFloat() * 0.03f) * 3f
            path.lineTo(x.toFloat(), y)
        }
        path.lineTo(w, h)
        path.lineTo(0f, h)
        path.close()
        canvas.drawPath(path, sandPaint)

        // 沙滩上的光点
        sandPaint.color = Color.argb(20, 255, 220, 150)
        sandPaint.style = Paint.Style.FILL
        for (i in 0..15) {
            val sx = w * (0.05f + i * 0.06f)
            val sy = sandLine + 30f + sin(i * 3f + time) * 15f
            canvas.drawCircle(sx, sy, 2f, sandPaint)
        }
    }
}
