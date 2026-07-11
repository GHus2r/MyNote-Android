package com.mynote.android.ui.pdf

import org.json.JSONArray
import org.json.JSONObject

/**
 * PDF 批注数据模型
 *
 * 存储为 PDF 文件同目录下的 _annotations.json 文件
 */
data class PdfAnnotations(
    val version: Int = 1,
    val pages: MutableMap<Int, PageAnnotations> = mutableMapOf()
) {
    fun toJson(): String {
        val root = JSONObject()
        root.put("version", version)
        val pagesObj = JSONObject()
        for ((page, ann) in pages) {
            pagesObj.put(page.toString(), ann.toJson())
        }
        root.put("pages", pagesObj)
        return root.toString(2)
    }

    companion object {
        fun fromJson(json: String): PdfAnnotations {
            return try {
                val root = JSONObject(json)
                val pages = mutableMapOf<Int, PageAnnotations>()
                val pagesObj = root.optJSONObject("pages") ?: return PdfAnnotations()
                for (key in pagesObj.keys()) {
                    val pageNum = key.toIntOrNull() ?: continue
                    pages[pageNum] = PageAnnotations.fromJson(pagesObj.getJSONObject(key))
                }
                PdfAnnotations(pages = pages)
            } catch (_: Exception) {
                PdfAnnotations()
            }
        }
    }
}

data class PageAnnotations(
    val strokes: MutableList<StrokeAnnotation> = mutableListOf(),
    val highlights: MutableList<HighlightAnnotation> = mutableListOf(),
    val texts: MutableList<TextAnnotation> = mutableListOf(),
    val shapes: MutableList<ShapeAnnotation> = mutableListOf(),
    val lines: MutableList<LineAnnotation> = mutableListOf()
) {
    fun toJson(): JSONObject {
        val obj = JSONObject()
        obj.put("strokes", JSONArray().apply { strokes.forEach { put(it.toJson()) } })
        obj.put("highlights", JSONArray().apply { highlights.forEach { put(it.toJson()) } })
        obj.put("texts", JSONArray().apply { texts.forEach { put(it.toJson()) } })
        obj.put("shapes", JSONArray().apply { shapes.forEach { put(it.toJson()) } })
        obj.put("lines", JSONArray().apply { lines.forEach { put(it.toJson()) } })
        return obj
    }

    companion object {
        fun fromJson(obj: JSONObject): PageAnnotations {
            val strokes = mutableListOf<StrokeAnnotation>()
            obj.optJSONArray("strokes")?.let { arr ->
                for (i in 0 until arr.length()) strokes.add(StrokeAnnotation.fromJson(arr.getJSONObject(i)))
            }
            val highlights = mutableListOf<HighlightAnnotation>()
            obj.optJSONArray("highlights")?.let { arr ->
                for (i in 0 until arr.length()) highlights.add(HighlightAnnotation.fromJson(arr.getJSONObject(i)))
            }
            val texts = mutableListOf<TextAnnotation>()
            obj.optJSONArray("texts")?.let { arr ->
                for (i in 0 until arr.length()) texts.add(TextAnnotation.fromJson(arr.getJSONObject(i)))
            }
            val shapes = mutableListOf<ShapeAnnotation>()
            obj.optJSONArray("shapes")?.let { arr ->
                for (i in 0 until arr.length()) shapes.add(ShapeAnnotation.fromJson(arr.getJSONObject(i)))
            }
            val lines = mutableListOf<LineAnnotation>()
            obj.optJSONArray("lines")?.let { arr ->
                for (i in 0 until arr.length()) lines.add(LineAnnotation.fromJson(arr.getJSONObject(i)))
            }
            return PageAnnotations(strokes, highlights, texts, shapes, lines)
        }
    }
}

/** 手写笔划 */
data class StrokeAnnotation(
    val points: MutableList<FloatArray> = mutableListOf(),
    val color: Int,
    val width: Float
) {
    fun toJson(): JSONObject {
        val pts = JSONArray()
        points.forEach { p -> pts.put(JSONArray().apply { put(p[0].toDouble()); put(p[1].toDouble()) }) }
        return JSONObject().apply {
            put("points", pts)
            put("color", color)
            put("width", width.toDouble())
        }
    }

    companion object {
        fun fromJson(obj: JSONObject): StrokeAnnotation {
            val pts = mutableListOf<FloatArray>()
            obj.optJSONArray("points")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val p = arr.getJSONArray(i)
                    pts.add(floatArrayOf(p.getDouble(0).toFloat(), p.getDouble(1).toFloat()))
                }
            }
            return StrokeAnnotation(pts, obj.optInt("color"), obj.optDouble("width", 3.0).toFloat())
        }
    }
}

/** 高亮标记（归一化坐标 0-1） */
data class HighlightAnnotation(
    val x: Float, val y: Float, val w: Float, val h: Float,
    val color: Int
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("x", x.toDouble()); put("y", y.toDouble())
        put("w", w.toDouble()); put("h", h.toDouble())
        put("color", color)
    }

    companion object {
        fun fromJson(obj: JSONObject): HighlightAnnotation = HighlightAnnotation(
            obj.optDouble("x", 0.0).toFloat(), obj.optDouble("y", 0.0).toFloat(),
            obj.optDouble("w", 0.0).toFloat(), obj.optDouble("h", 0.0).toFloat(),
            obj.optInt("color")
        )
    }
}

/** 文字批注（文本框，归一化坐标 0-1） */
data class TextAnnotation(
    var x: Float, var y: Float,
    var w: Float = 0f, var h: Float = 0f,
    var text: String,
    val color: Int,
    val fontSize: Float
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("x", x.toDouble()); put("y", y.toDouble())
        put("w", w.toDouble()); put("h", h.toDouble())
        put("text", text)
        put("color", color)
        put("fontSize", fontSize.toDouble())
    }

    companion object {
        fun fromJson(obj: JSONObject): TextAnnotation = TextAnnotation(
            obj.optDouble("x", 0.0).toFloat(), obj.optDouble("y", 0.0).toFloat(),
            obj.optDouble("w", 0.0).toFloat(), obj.optDouble("h", 0.0).toFloat(),
            obj.optString("text", ""),
            obj.optInt("color"),
            obj.optDouble("fontSize", 14.0).toFloat()
        )
    }
}

/** 形状框（矩形/圆形，归一化坐标 0-1） */
data class ShapeAnnotation(
    val x: Float, val y: Float, val w: Float, val h: Float,
    val color: Int, val width: Float,
    val type: String // "rect" or "circle"
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("x", x.toDouble()); put("y", y.toDouble())
        put("w", w.toDouble()); put("h", h.toDouble())
        put("color", color)
        put("width", width.toDouble())
        put("type", type)
    }

    companion object {
        fun fromJson(obj: JSONObject): ShapeAnnotation = ShapeAnnotation(
            obj.optDouble("x", 0.0).toFloat(), obj.optDouble("y", 0.0).toFloat(),
            obj.optDouble("w", 0.0).toFloat(), obj.optDouble("h", 0.0).toFloat(),
            obj.optInt("color"),
            obj.optDouble("width", 3.0).toFloat(),
            obj.optString("type", "rect")
        )
    }
}

/** 下划线/删除线（归一化坐标 0-1） */
data class LineAnnotation(
    val x: Float, val y: Float, val w: Float,
    val color: Int, val width: Float,
    val style: String // "underline" or "strikethrough"
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("x", x.toDouble()); put("y", y.toDouble())
        put("w", w.toDouble())
        put("color", color)
        put("width", width.toDouble())
        put("style", style)
    }

    companion object {
        fun fromJson(obj: JSONObject): LineAnnotation = LineAnnotation(
            obj.optDouble("x", 0.0).toFloat(), obj.optDouble("y", 0.0).toFloat(),
            obj.optDouble("w", 0.0).toFloat(),
            obj.optInt("color"),
            obj.optDouble("width", 2.0).toFloat(),
            obj.optString("style", "underline")
        )
    }
}
