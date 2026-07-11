package com.mynote.android.util

import android.content.Context
import android.content.SharedPreferences
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

object BackgroundHelper {

    private const val PREF_NAME = "mynote_prefs"
    private const val KEY_BG_MODE = "bg_mode"
    private const val KEY_BG_VALUE = "bg_value"
    private const val KEY_BG_IMAGE = "bg_image"

    private const val DEFAULT_BG = "#F8F8F8"

    data class PresetColor(val name: String, val hex: String)
    val presetColors = listOf(
        PresetColor("默认", DEFAULT_BG),
        PresetColor("天空蓝", "#E3F2FD"),
        PresetColor("薄荷绿", "#E8F5E9"),
        PresetColor("暖杏", "#FFF3E0"),
        PresetColor("淡紫", "#F3E5F5"),
        PresetColor("暗黑", "#1A1A2E"),
    )

    data class PresetGradient(val name: String, val startHex: String, val endHex: String)
    val presetGradients = listOf(
        PresetGradient("日落", "#FF7E5F", "#FEB47B"),
        PresetGradient("海洋", "#2193B0", "#6DD5ED"),
        PresetGradient("森林", "#11998E", "#38EF7D"),
        PresetGradient("星空", "#2C3E50", "#3498DB"),
        PresetGradient("樱花", "#FFAFBD", "#FFC3A0"),
        PresetGradient("薰衣草", "#E0C3FC", "#8EC5FC"),
        PresetGradient("薄荷", "#43E97B", "#38F9D7"),
        PresetGradient("芒果", "#F6D365", "#FDA085"),
        PresetGradient("极光", "#00CDAC", "#02AAB0"),
        PresetGradient("暮光", "#667EEA", "#764BA2"),
        PresetGradient("焦糖", "#D4A574", "#F3E7E9"),
        PresetGradient("深海", "#0F2027", "#2C5364"),
        PresetGradient("玫瑰", "#FF6B6B", "#FF8E8E"),
        PresetGradient("天空", "#89F7FE", "#66A6FF"),
        PresetGradient("蜜桃", "#FFECD2", "#FCB69F"),
        PresetGradient("翡翠", "#1D976C", "#93F9B9"),
    )

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun getMode(context: Context): String =
        prefs(context).getString(KEY_BG_MODE, "default") ?: "default"

    fun getValue(context: Context): String? =
        prefs(context).getString(KEY_BG_VALUE, null)

    fun getImagePath(context: Context): String? =
        prefs(context).getString(KEY_BG_IMAGE, null)

    fun setDefault(context: Context) {
        prefs(context).edit()
            .putString(KEY_BG_MODE, "default")
            .remove(KEY_BG_VALUE)
            .remove(KEY_BG_IMAGE)
            .commit()
    }

    fun setColor(context: Context, hex: String) {
        prefs(context).edit()
            .putString(KEY_BG_MODE, "color")
            .putString(KEY_BG_VALUE, hex)
            .remove(KEY_BG_IMAGE)
            .commit()
    }

    fun setGradient(context: Context, startHex: String, endHex: String) {
        prefs(context).edit()
            .putString(KEY_BG_MODE, "gradient")
            .putString(KEY_BG_VALUE, "${startHex}_${endHex}")
            .remove(KEY_BG_IMAGE)
            .commit()
    }

    fun setImage(context: Context, sourceUri: Uri): String? {
        return try {
            val dir = File(context.filesDir, "bg")
            dir.mkdirs()
            val destFile = File(dir, "bg_image_${System.currentTimeMillis()}.jpg")
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(destFile).use { output -> input.copyTo(output) }
            }
            if (destFile.length() > 0) {
                prefs(context).edit()
                    .putString(KEY_BG_MODE, "image")
                    .putString(KEY_BG_IMAGE, destFile.absolutePath)
                    .commit()
                destFile.absolutePath
            } else null
        } catch (e: Exception) { null }
    }

    fun getBackground(context: Context): Drawable? {
        return when (getMode(context)) {
            "color" -> {
                val hex = getValue(context) ?: DEFAULT_BG
                try { ColorDrawable(Color.parseColor(hex)) }
                catch (e: Exception) { ColorDrawable(Color.parseColor(DEFAULT_BG)) }
            }
            "gradient" -> {
                val value = getValue(context) ?: return null
                val parts = value.split("_")
                if (parts.size == 2) {
                    try {
                        GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                            intArrayOf(Color.parseColor(parts[0]), Color.parseColor(parts[1])))
                    } catch (e: Exception) { null }
                } else null
            }
            "image" -> {
                val path = getImagePath(context) ?: return null
                val file = File(path)
                if (file.exists()) BitmapDrawable(context.resources, BitmapFactory.decodeFile(path))
                else null
            }
            else -> null
        }
    }

    fun getCategoryBackground(colorHex: String): Drawable {
        val color = try { Color.parseColor(colorHex) }
                    catch (e: Exception) { Color.parseColor("#4CAF50") }
        val topColor = lighten(color, 0.75f)
        val botColor = lighten(color, 0.92f)
        return GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(topColor, botColor))
    }

    private fun lighten(color: Int, factor: Float): Int {
        val r = (Color.red(color) + (255 - Color.red(color)) * factor).toInt()
        val g = (Color.green(color) + (255 - Color.green(color)) * factor).toInt()
        val b = (Color.blue(color) + (255 - Color.blue(color)) * factor).toInt()
        return Color.rgb(r, g, b)
    }
}
