package com.mynote.android.ui.viewer

import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.mynote.android.R
import java.io.File

class ImageViewerActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_PATH = "image_path"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_image_viewer)

            val iv = findViewById<ImageView>(R.id.iv_full)
            findViewById<TextView>(R.id.btn_img_back).setOnClickListener { finish() }

            val path = intent.getStringExtra(EXTRA_PATH) ?: run { finish(); return }
            val file = File(path)
            if (!file.exists()) { Toast.makeText(this, "文件不存在", Toast.LENGTH_SHORT).show(); finish(); return }

            val opts = BitmapFactory.Options().apply { inSampleSize = 1 }
            val bitmap = BitmapFactory.decodeFile(path, opts)
            if (bitmap == null) { Toast.makeText(this, "无法加载图片", Toast.LENGTH_SHORT).show(); finish(); return }

            iv.setImageBitmap(bitmap)
        } catch (e: Exception) {
            Toast.makeText(this, "打开失败: ${e.message}", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
