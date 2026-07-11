package com.mynote.android.ui.image

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.mynote.android.R
import java.io.File
import java.io.FileOutputStream

/**
 * 图片标注 — 在 CT/B超/胸片等图上画箭头、圈注、写字
 */
class ImageAnnotationActivity : AppCompatActivity() {

    private var imgPath = ""
    private var drawColor = Color.RED
    private var drawMode = 0 // 0=箭头, 1=圆圈, 2=文字
    private var startX = 0f
    private var startY = 0f
    private var editBitmap: Bitmap? = null
    private lateinit var imgCanvas: ImageView
    private lateinit var tvMode: TextView
    private val drawCanvas = android.graphics.Path()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_annotate)

        imgPath = intent.getStringExtra("image_path") ?: run { finish(); return }
        val srcFile = File(imgPath)
        if (!srcFile.exists()) { toast("图片文件不存在"); finish(); return }

        imgCanvas = findViewById(R.id.ia_image)
        try {
            // 限制解码尺寸防止 OOM
            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(imgPath, opts)
            val maxSize = 2048
            var scale = 1
            while (opts.outWidth / scale > maxSize || opts.outHeight / scale > maxSize) scale *= 2
            val decodeOpts = BitmapFactory.Options().apply { inSampleSize = scale }
            val srcBitmap = BitmapFactory.decodeFile(imgPath, decodeOpts)
            editBitmap = srcBitmap?.copy(Bitmap.Config.ARGB_8888, true)
            srcBitmap?.recycle()
            imgCanvas.setImageBitmap(editBitmap)
        } catch (e: Exception) {
            toast("图片加载失败"); finish(); return
        }

        findViewById<TextView>(R.id.ia_back).setOnClickListener { saveAndFinish() }
        tvMode = findViewById(R.id.ia_mode)

        findViewById<TextView>(R.id.ia_arrow).setOnClickListener { drawMode = 0; tvMode.text = "箭头 ↑" }
        findViewById<TextView>(R.id.ia_circle).setOnClickListener { drawMode = 1; tvMode.text = "圈注 ○" }
        findViewById<TextView>(R.id.ia_text).setOnClickListener { drawMode = 2; tvMode.text = "文字 T" }
        findViewById<TextView>(R.id.ia_redo).setOnClickListener set@{
            editBitmap = BitmapFactory.decodeFile(imgPath)?.copy(Bitmap.Config.ARGB_8888, true)
            imgCanvas.setImageBitmap(editBitmap)
        }
        findViewById<TextView>(R.id.ia_color).setOnClickListener {
            drawColor = when (drawColor) {
                Color.RED -> Color.parseColor("#FF9800")
                Color.parseColor("#FF9800") -> Color.parseColor("#4CAF50")
                Color.parseColor("#4CAF50") -> Color.parseColor("#2196F3")
                else -> Color.RED
            }
            findViewById<View>(R.id.ia_color).setBackgroundColor(drawColor)
        }
        findViewById<View>(R.id.ia_color).setBackgroundColor(drawColor)

        imgCanvas.setOnTouchListener { _, event ->
            val img = editBitmap ?: return@setOnTouchListener false
            val scaleX = img.width.toFloat() / imgCanvas.width
            val scaleY = img.height.toFloat() / imgCanvas.height
            val x = event.x * scaleX
            val y = event.y * scaleY

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = x; startY = y
                    when (drawMode) {
                        0, 1 -> { drawCanvas.reset(); drawCanvas.moveTo(x, y) }
                        2 -> drawText(x, y)
                    }
                }
                MotionEvent.ACTION_MOVE -> {
                    when (drawMode) {
                        0, 1 -> drawCanvas.lineTo(x, y)
                    }
                }
                MotionEvent.ACTION_UP -> {
                    when (drawMode) {
                        0 -> drawArrowOnBitmap(img, startX, startY, x, y)
                        1 -> drawCircleOnBitmap(img, startX, startY, x, y)
                    }
                    imgCanvas.setImageBitmap(img)
                }
            }
            imgCanvas.invalidate()
            true
        }
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    private fun drawArrowOnBitmap(bmp: Bitmap, x1: Float, y1: Float, x2: Float, y2: Float) {
        val c = Canvas(bmp)
        val paint = Paint().apply {
            color = drawColor; strokeWidth = 6f; style = Paint.Style.STROKE
            isAntiAlias = true; strokeCap = Paint.Cap.ROUND
        }
        c.drawLine(x1, y1, x2, y2, paint)
        val angle = Math.atan2((y2 - y1).toDouble(), (x2 - x1).toDouble())
        val headLen = 40f
        val a1 = angle + 2.8; val a2 = angle - 2.8
        c.drawLine(x2, y2, x2 - headLen * Math.cos(a1).toFloat(), y2 - headLen * Math.sin(a1).toFloat(), paint)
        c.drawLine(x2, y2, x2 - headLen * Math.cos(a2).toFloat(), y2 - headLen * Math.sin(a2).toFloat(), paint)
    }

    private fun drawCircleOnBitmap(bmp: Bitmap, x1: Float, y1: Float, x2: Float, y2: Float) {
        val c = Canvas(bmp)
        val paint = Paint().apply {
            color = drawColor; strokeWidth = 5f; style = Paint.Style.STROKE; isAntiAlias = true
        }
        val cx = (x1 + x2) / 2; val cy = (y1 + y2) / 2
        val r = Math.sqrt(((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1)).toDouble()).toFloat() / 2
        c.drawCircle(cx, cy, r, paint)
    }

    private fun drawText(x: Float, y: Float) {
        val input = android.widget.EditText(this).apply {
            hint = "输入标注文字"
            setSelectAllOnFocus(true)
        }
        android.app.AlertDialog.Builder(this, R.style.RoundedDialog)
            .setTitle("添加文字标注")
            .setView(input)
            .setPositiveButton("确定") { _, _ ->
                val text = input.text.toString()
                if (text.isNotEmpty()) {
                    val c = Canvas(editBitmap!!)
                    val paint = Paint().apply {
                        color = drawColor; textSize = 60f; isAntiAlias = true
                        setShadowLayer(4f, 0f, 0f, Color.BLACK)
                    }
                    c.drawText(text, x, y, paint)
                    imgCanvas.setImageBitmap(editBitmap)
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun saveAndFinish() {
        val bmp = editBitmap ?: run { finish(); return }
        val dir = File(imgPath).parentFile ?: run { finish(); return }
        val annotatedFile = File(dir, "annotated_${File(imgPath).name}")
        FileOutputStream(annotatedFile).use { bmp.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        Toast.makeText(this, "已保存标注图片", Toast.LENGTH_SHORT).show()
        finish()
    }
}
