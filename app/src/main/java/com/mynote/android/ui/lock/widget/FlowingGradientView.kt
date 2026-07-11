package com.mynote.android.ui.lock.widget

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import kotlin.random.Random

/**
 * 医疗元素漂浮背景 — 使用真实系统 Emoji 渲染
 */
class FlowingGradientView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    class F(
        var x: Float, var y: Float, var dx: Float, var dy: Float,
        var sz: Float, var rot: Float, var rs: Float,
        var alpha: Int, val emoji: String,
        var ph: Float = 0f, var wobbleA: Float = 0f, var wobbleS: Float = 0f
    ) { val trail = mutableListOf<FloatArray>() }

    private val emojis = listOf(
        "✚","💊","💉","🩺","🦴","🏥","🧬","🧪","🔬",
        "🦷","🩸","🩹","👁","🧫","🦻","🚑","🦾","🦿",
        "💀","👂","👃","🦼","🧠","🦽"
    )
    private val fs = mutableListOf<F>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var w = 0f; private var h = 0f
    private lateinit var an: ValueAnimator
    private var g = false
    private val tx = mutableListOf<Float>(); private val ty = mutableListOf<Float>()
    private var bgPhase = 0f

    init {
        an = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 50; repeatCount = ValueAnimator.INFINITE
            addUpdateListener { up() }; start()
        }
    }

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        super.onSizeChanged(w, h, ow, oh)
        if (fs.isEmpty() && w > 0 && h > 0) { this.w = w.toFloat(); this.h = h.toFloat(); initF() }
    }

    private fun initF() {
        val r = Random(123)
        val n = emojis.size
        val cx = w/2f; val cy = h/2f
        // 生成聚合目标：3行网格
        val cols = (n + 2) / 3
        val cellW = w * 0.22f; val cellH = h * 0.12f
        val gridX = cx - cellW * (cols - 1) / 2f
        val gridY = cy - cellH
        for (i in 0 until n) {
            val row = i / cols; val col = i % cols
            val tgX = gridX + col * cellW
            val tgY = gridY + row * cellH
            val sz = (0.04f + r.nextFloat() * 0.12f) * w  // 随机大小
            tx.add(tgX); ty.add(tgY)
            val speed = 0.3f + r.nextFloat() * 1.9f
            val angle = r.nextFloat() * 2f * Math.PI.toFloat()
            val f = F(r.nextFloat()*(w-sz), r.nextFloat()*(h-sz),
                Math.cos(angle.toDouble()).toFloat() * speed,
                Math.sin(angle.toDouble()).toFloat() * speed,
                sz, r.nextFloat()*360f, (r.nextFloat()-0.5f)*0.8f,
                80+r.nextInt(90), emojis[i])
            f.ph = r.nextFloat() * 6.28f
            f.wobbleA = 0.3f + r.nextFloat() * 1.5f
            f.wobbleS = 0.02f + r.nextFloat() * 0.04f
            fs.add(f)
        }
    }

    private fun up() {
        bgPhase += 0.003f
        if(g){ for(i in fs.indices){ val f=fs[i]
            f.x+=(tx[i]-f.x)*0.07f; f.y+=(ty[i]-f.y)*0.07f; f.rot+=(-f.rot)*0.07f; f.dx=0f; f.dy=0f } }
        else{
            for(f in fs){
                // 记录尾迹
                f.trail.add(floatArrayOf(f.x, f.y, f.rot))
                if (f.trail.size > 8) f.trail.removeAt(0)
                f.ph += f.wobbleS
                // 基础位移 + 正弦摆动（模拟漂浮感）
                val wobX = Math.sin(f.ph.toDouble()).toFloat() * f.wobbleA
                val wobY = Math.cos(f.ph * 1.3f.toDouble()).toFloat() * f.wobbleA * 0.7f
                f.x += f.dx + wobX
                f.y += f.dy + wobY
                f.rot += f.rs
                // 软边界：越靠近边缘推力越大，避免生硬反弹
                val margin = f.sz * 0.5f
                if (f.x < margin) f.dx += (margin - f.x) * 0.02f
                if (f.x > w - f.sz - margin) f.dx -= (f.x - (w - f.sz - margin)) * 0.02f
                if (f.y < margin) f.dy += (margin - f.y) * 0.02f
                if (f.y > h - f.sz - margin) f.dy -= (f.y - (h - f.sz - margin)) * 0.02f
                // 限速，防止加速度累积
                val maxSpd = 3f
                f.dx = f.dx.coerceIn(-maxSpd, maxSpd)
                f.dy = f.dy.coerceIn(-maxSpd, maxSpd)
                // 最终钳位，防止飞出屏幕
                f.x = f.x.coerceIn(-f.sz * 0.3f, w - f.sz * 0.7f)
                f.y = f.y.coerceIn(-f.sz * 0.3f, h - f.sz * 0.7f)
            }
        }
        invalidate()
    }

    fun gatherToN() { g = true; fs.forEach { it.trail.clear() } }
    fun scatterOut() { g = false; fs.forEach { it.trail.clear() } }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // 动态流光渐变背景
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        val ms = Math.min(w, h)
        // 三个光斑位置随 bgPhase 漂移
        val c1x = w * (0.3f + 0.15f * Math.sin(bgPhase.toDouble()).toFloat())
        val c1y = h * (0.35f + 0.10f * Math.cos(bgPhase * 0.7f.toDouble()).toFloat())
        val c2x = w * (0.7f + 0.12f * Math.cos(bgPhase * 0.8f.toDouble()).toFloat())
        val c2y = h * (0.6f + 0.12f * Math.sin(bgPhase * 0.6f.toDouble()).toFloat())
        val c3x = w * (0.5f + 0.20f * Math.sin(bgPhase * 0.5f.toDouble()).toFloat())
        val c3y = h * (0.2f + 0.08f * Math.cos(bgPhase * 0.9f.toDouble()).toFloat())

        bgPaint.shader = RadialGradient(c1x, c1y, ms * 0.8f,
            intArrayOf(Color.parseColor("#E0F4FF"), Color.parseColor("#C8F0E0"), Color.parseColor("#A8E4D0")),
            floatArrayOf(0f, 0.4f, 1f), Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, w, h, bgPaint)

        bgPaint.shader = RadialGradient(c2x, c2y, ms * 0.65f,
            intArrayOf(Color.parseColor("#D0ECF8"), Color.parseColor("#B8E8F0"), Color.parseColor("#90D4E0")),
            floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP)
        bgPaint.alpha = 150
        canvas.drawRect(0f, 0f, w, h, bgPaint)

        bgPaint.shader = RadialGradient(c3x, c3y, ms * 0.5f,
            intArrayOf(Color.parseColor("#F0FAFF"), Color.parseColor("#D8F4E8"), Color.parseColor("#B8E8D8")),
            floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP)
        bgPaint.alpha = 160
        canvas.drawRect(0f, 0f, w, h, bgPaint)

        for (f in fs) {
            // 绘制白色尾气拖影
            val tp = Paint(Paint.ANTI_ALIAS_FLAG)
            for (j in f.trail.indices) {
                val t = f.trail[j]
                val fade = (j + 1).toFloat() / f.trail.size  // 0→1，越旧越淡
                val r = f.sz * 0.15f * fade  // 光点从小到大
                val cx = t[0] + f.sz / 2f
                val cy = t[1] + f.sz / 2f
                tp.shader = RadialGradient(cx, cy, r,
                    intArrayOf(Color.argb((80 * fade).toInt(), 255, 255, 255),
                               Color.argb(0, 255, 255, 255)),
                    floatArrayOf(0f, 1f), Shader.TileMode.CLAMP)
                tp.alpha = (150 * fade).toInt()
                canvas.drawCircle(cx, cy, r, tp)
            }
            paint.alpha = 255
            canvas.save()
            canvas.translate(f.x+f.sz/2, f.y+f.sz/2)
            canvas.rotate(f.rot)
            paint.textSize = f.sz
            paint.textAlign = Paint.Align.CENTER
            paint.style = Paint.Style.FILL
            paint.shader = null
            canvas.drawText(f.emoji, 0f, f.sz*0.32f, paint)
            canvas.restore()
        }
    }

    override fun onDetachedFromWindow() { super.onDetachedFromWindow(); an.cancel() }
}
