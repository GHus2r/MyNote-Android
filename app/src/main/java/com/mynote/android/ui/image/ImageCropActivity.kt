package com.mynote.android.ui.image

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * 图片裁剪 — 拖动框选「诊断结果」等目标区域，裁剪后返回图片路径
 * 用于影像报告 OCR 前精准框选需要识别的部分
 */
class ImageCropActivity : AppCompatActivity() {

    private lateinit var cropView: CropView
    private lateinit var tvHint: TextView
    private var srcBitmap: Bitmap? = null
    private var loaded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val uriStr = intent.getStringExtra("image_uri")
            ?: run { Toast.makeText(this, "缺少图片", Toast.LENGTH_SHORT).show(); finish(); return }

        setContentView(buildContentView())
        loadBitmap(uriStr)
    }

    private fun buildContentView(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.BLACK)
        }

        // 顶部栏
        val topbar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(12, 16, 12, 12)
            setBackgroundColor(0xFF212121.toInt())
        }
        val btnCancel = Button(this).apply {
            text = "取消"
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.TRANSPARENT)
            setOnClickListener { finish() }
        }
        val tvTitle = TextView(this).apply {
            text = "框选要识别的区域"
            textSize = 16f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }
        val btnOk = Button(this).apply {
            text = "确定"
            setTextColor(0xFF4CAF50.toInt())
            setBackgroundColor(Color.TRANSPARENT)
            setOnClickListener { doCrop() }
        }
        topbar.addView(btnCancel)
        topbar.addView(tvTitle, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        topbar.addView(btnOk)

        cropView = CropView(this)

        tvHint = TextView(this).apply {
            text = "拖动手指框选「诊断结果」区域，松手确认；点「重画」重新框选"
            textSize = 13f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(12, 10, 12, 16)
            setBackgroundColor(0xFF212121.toInt())
        }

        root.addView(topbar)
        root.addView(cropView, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        root.addView(tvHint)

        return root
    }

    private fun loadBitmap(uriStr: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val bmp = try {
                val uri = Uri.parse(uriStr)
                val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
                val maxSize = 2048
                var scale = 1
                while ((opts.outWidth / scale) > maxSize || (opts.outHeight / scale) > maxSize) scale *= 2
                val decodeOpts = BitmapFactory.Options().apply { inSampleSize = scale }
                contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, decodeOpts) }
            } catch (e: Exception) { null }

            withContext(Dispatchers.Main) {
                if (bmp == null) {
                    Toast.makeText(this@ImageCropActivity, "图片加载失败", Toast.LENGTH_SHORT).show()
                    finish()
                    return@withContext
                }
                srcBitmap = bmp
                cropView.setBitmap(bmp)
                loaded = true
                tvHint.text = "拖动手指框选「诊断结果」区域，松手确认；点「确定」裁剪"
            }
        }
    }

    private fun doCrop() {
        val bmp = srcBitmap ?: return
        val rect = cropView.getCropRect() ?: run {
            Toast.makeText(this, "请先框选要识别的区域", Toast.LENGTH_SHORT).show()
            return
        }
        if (rect.width() < 20 || rect.height() < 20) {
            Toast.makeText(this, "框选区域太小，请重新框选", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            val left = rect.left.toInt().coerceIn(0, bmp.width - 1)
            val top = rect.top.toInt().coerceIn(0, bmp.height - 1)
            val right = rect.right.toInt().coerceIn(left + 1, bmp.width)
            val bottom = rect.bottom.toInt().coerceIn(top + 1, bmp.height)

            val cropped = try {
                Bitmap.createBitmap(bmp, left, top, right - left, bottom - top)
            } catch (e: Exception) { null }

            if (cropped == null) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ImageCropActivity, "裁剪失败", Toast.LENGTH_SHORT).show()
                }
                return@launch
            }

            val outFile = File(cacheDir, "crop_${System.currentTimeMillis()}.jpg")
            FileOutputStream(outFile).use { cropped.compress(Bitmap.CompressFormat.JPEG, 92, it) }
            cropped.recycle()

            withContext(Dispatchers.Main) {
                val data = android.content.Intent().putExtra("cropped_path", outFile.absolutePath)
                setResult(RESULT_OK, data)
                finish()
            }
        }
    }

    /** 自定义裁剪视图：fit-center 显示图片，拖动绘制裁剪矩形 + 半透明遮罩 */
    inner class CropView(context: android.content.Context) : View(context) {

        private var bitmap: Bitmap? = null
        private var cropRect: RectF? = null          // bitmap 坐标的裁剪区
        private var dragging = false
        private var downX = 0f
        private var downY = 0f
        private val drawRect = RectF()

        private val shadePaint = Paint().apply {
            color = 0x99000000.toInt()
            style = Paint.Style.FILL
        }
        private val borderPaint = Paint().apply {
            color = 0xFF4CAF50.toInt()
            style = Paint.Style.STROKE
            strokeWidth = 3f
            isAntiAlias = true
        }

        fun setBitmap(bmp: Bitmap) {
            bitmap = bmp
            cropRect = null
            invalidate()
        }

        fun getCropRect(): RectF? = cropRect

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val bmp = bitmap ?: return

            // fit-center 显示区域
            val scale = minOf(width.toFloat() / bmp.width, height.toFloat() / bmp.height)
            val dw = bmp.width * scale
            val dh = bmp.height * scale
            val left = (width - dw) / 2f
            val top = (height - dh) / 2f
            drawRect.set(left, top, left + dw, top + dh)

            canvas.drawBitmap(bmp, null, drawRect, null)

            val rect = cropRect ?: return
            // bitmap 坐标 → 屏幕坐标
            val sLeft = drawRect.left + rect.left / bmp.width * dw
            val sTop = drawRect.top + rect.top / bmp.height * dh
            val sRight = drawRect.left + rect.right / bmp.width * dw
            val sBottom = drawRect.top + rect.bottom / bmp.height * dh

            // 遮罩（裁剪区外的四块）
            canvas.drawRect(0f, 0f, width.toFloat(), sTop, shadePaint)
            canvas.drawRect(0f, sBottom, width.toFloat(), height.toFloat(), shadePaint)
            canvas.drawRect(0f, sTop, sLeft, sBottom, shadePaint)
            canvas.drawRect(sRight, sTop, width.toFloat(), sBottom, shadePaint)

            // 边框
            canvas.drawRect(sLeft, sTop, sRight, sBottom, borderPaint)
        }

        override fun onTouchEvent(event: MotionEvent): Boolean {
            val bmp = bitmap ?: return false
            if (event.action == MotionEvent.ACTION_DOWN && !loaded) return false

            // 屏幕坐标 → bitmap 坐标（clamp 到显示区内）
            fun toBitmap(x: Float, y: Float): Pair<Float, Float> {
                val bx = ((x - drawRect.left) / drawRect.width()).coerceIn(0f, 1f) * bmp.width
                val by = ((y - drawRect.top) / drawRect.height()).coerceIn(0f, 1f) * bmp.height
                return bx to by
            }

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    if (!drawRect.contains(event.x, event.y)) return false
                    dragging = true
                    val (bx, by) = toBitmap(event.x, event.y)
                    downX = bx; downY = by
                    cropRect = RectF(bx, by, bx, by)
                    invalidate()
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    if (!dragging) return false
                    val (bx, by) = toBitmap(event.x, event.y)
                    val r = cropRect ?: return false
                    r.set(
                        minOf(downX, bx), minOf(downY, by),
                        maxOf(downX, bx), maxOf(downY, by)
                    )
                    invalidate()
                    return true
                }
                MotionEvent.ACTION_UP -> {
                    dragging = false
                    return true
                }
            }
            return true
        }
    }
}
