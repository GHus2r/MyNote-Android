package com.mynote.android.util

import android.graphics.*
import android.graphics.pdf.PdfDocument
import com.mynote.android.data.entity.MedicalRecord
import com.mynote.android.data.entity.Patient
import java.io.File
import java.io.FileOutputStream

/**
 * Android PdfDocument 正式病历排版输出
 */
object PdfExporter {

    private val pageW = 595  // A4 width in points
    private val pageH = 842  // A4 height
    private val margin = 50f

    fun export(patient: Patient, records: List<MedicalRecord>, outFile: File) {
        val doc = PdfDocument()
        var pageNum = 1

        // Header
        val headerY = margin + 12
        fun drawHeader(page: PdfDocument.Page, y: Float) {
            val c = page.canvas
            val p = Paint(Paint.ANTI_ALIAS_FLAG)
            p.textSize = 16f; p.typeface = Typeface.DEFAULT_BOLD; p.color = Color.BLACK
            c.drawText(patient.name, margin, y, p)
            p.textSize = 10f; p.typeface = Typeface.DEFAULT; p.color = Color.GRAY
            c.drawText("${patient.department}科 · 床号${patient.bedNumber} · ${patient.admissionDate}", margin, y + 14, p)
            c.drawText("诊断：${patient.diagnosis}", margin, y + 28, p)
            // line
            p.color = Color.parseColor("#CCCCCC"); p.strokeWidth = 1f
            c.drawLine(margin, y + 36, pageW - margin, y + 36, p)
        }

        fun drawFooter(page: PdfDocument.Page) {
            val c = page.canvas
            val p = Paint(Paint.ANTI_ALIAS_FLAG)
            p.textSize = 8f; p.color = Color.GRAY; p.typeface = Typeface.DEFAULT
            c.drawText("- $pageNum -", pageW / 2f - 15f, pageH - 20f, p)
            pageNum++
        }

        fun drawText(page: PdfDocument.Page, text: String, yStart: Float, paint: Paint): Float {
            val c = page.canvas
            var y = yStart
            val lines = text.split("\n")
            for (line in lines) {
                if (y > pageH - margin - 20) return -1f // page overflow
                c.drawText(line, margin, y, paint)
                y += paint.textSize + 4
            }
            return y
        }

        for (record in records) {
            var page = doc.startPage(PdfDocument.PageInfo.Builder(pageW, pageH, pageNum).create())
            drawHeader(page, headerY)

            val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = 13f; typeface = Typeface.DEFAULT_BOLD; color = Color.BLACK
            }
            val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = 10f; typeface = Typeface.MONOSPACE; color = Color.DKGRAY
            }

            var y = headerY + 48f
            y = drawText(page, "【${record.type}】", y, titlePaint)
            y = drawText(page, "", y + 6, bodyPaint)

            val paragraphs = record.content.split("\n")
            for (para in paragraphs) {
                if (y < 0 || y > pageH - margin - 30) {
                    drawFooter(page)
                    doc.finishPage(page)
                    page = doc.startPage(PdfDocument.PageInfo.Builder(pageW, pageH, pageNum).create())
                    drawHeader(page, headerY)
                    y = headerY + 48f
                }
                y = drawText(page, para, y, bodyPaint)
                if (y < 0) y = pageH - margin - 30 // force new page
            }
            y += 20f
            if (record != records.last()) {
                val sep = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.LTGRAY; strokeWidth = 1f }
                page.canvas.drawLine(margin, y, pageW - margin, y, sep)
                y += 20f
            }
            drawFooter(page)
            doc.finishPage(page)
        }

        FileOutputStream(outFile).use { doc.writeTo(it) }
        doc.close()
    }
}
