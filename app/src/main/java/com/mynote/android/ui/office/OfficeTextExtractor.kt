package com.mynote.android.ui.office

import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.File
import java.io.StringReader
import java.util.zip.ZipFile

/**
 * 零依赖 Office 文档文本提取器
 * 直接从 docx/xlsx/pptx（ZIP 格式）内部 XML 提取文本
 */
object OfficeTextExtractor {

    /**
     * PPT 单张幻灯片数据
     */
    data class SlideData(
        val index: Int,                     // 1-based slide number
        val text: String,                   // 提取的纯文本
        val imageRefs: List<String> = emptyList()  // ZIP 内图片路径 (如 ppt/media/image1.png)
    )

    fun extract(file: File): String {
        val ext = file.extension.lowercase()
        return when (ext) {
            "docx" -> extractDocx(file)
            "xlsx" -> extractXlsx(file)
            "pptx" -> extractPptx(file)
            else -> "不支持的文件格式"
        }
    }

    /** 提取所有 PPT 幻灯片（含图片引用列表） */
    fun extractPptxSlides(file: File): List<SlideData> {
        val slides = mutableListOf<SlideData>()
        try {
            ZipFile(file).use { zip ->
                // 枚举所有幻灯片
                val slideEntries = zip.entries().asSequence()
                    .filter { it.name.matches(Regex("ppt/slides/slide\\d+\\.xml", RegexOption.IGNORE_CASE)) }
                    .sortedBy {
                        Regex("slide(\\d+)").find(it.name)?.groupValues?.get(1)?.toIntOrNull() ?: 0
                    }
                    .toList()

                for (entry in slideEntries) {
                    val slideNum = Regex("slide(\\d+)").find(entry.name)?.groupValues?.get(1)?.toIntOrNull() ?: continue
                    val xml = zip.getInputStream(entry).bufferedReader().readText()
                    val text = extractSlideText(xml)
                    val imageRefs = extractSlideImageRefs(zip, slideNum)
                    slides.add(SlideData(slideNum, text, imageRefs))
                }
            }
        } catch (_: Exception) {}
        return slides
    }

    /** 从幻灯片 XML 提取文本 */
    private fun extractSlideText(xml: String): String {
        return try {
            val sb = StringBuilder()
            val parser = createParser(xml)
            var inAt = false
            var event = parser.eventType
            while (event != XmlPullParser.END_DOCUMENT) {
                when (event) {
                    XmlPullParser.START_TAG -> {
                        if (parser.name == "a:t") inAt = true
                    }
                    XmlPullParser.TEXT -> {
                        if (inAt) sb.append(parser.text)
                    }
                    XmlPullParser.END_TAG -> {
                        when (parser.name) {
                            "a:t" -> inAt = false
                            "a:p" -> sb.append("\n")
                        }
                    }
                }
                event = parser.next()
            }
            sb.toString().trim()
        } catch (_: Exception) { "" }
    }

    /** 提取幻灯片中引用的图片（通过关系文件找到 rId → 图片路径） */
    private fun extractSlideImageRefs(zip: ZipFile, slideNum: Int): List<String> {
        val refs = mutableListOf<String>()
        try {
            val relsPath = "ppt/slides/_rels/slide${slideNum}.xml.rels"
            val relsEntry = zip.getEntry(relsPath) ?: return refs
            val relsXml = zip.getInputStream(relsEntry).bufferedReader().readText()
            // 解析 Relationship 标签，提取 Target 属性
            val relRegex = Regex("<Relationship[^>]*Target=\"([^\"]+)\"[^>]*/>|<Relationship[^>]*Target='([^']+)'[^>]*/>")
            for (match in relRegex.findAll(relsXml)) {
                val target = match.groupValues[1].ifEmpty { match.groupValues[2] }
                if (target.contains("image") || target.endsWith(".png") || target.endsWith(".jpg")
                    || target.endsWith(".jpeg") || target.endsWith(".gif")) {
                    // 路径可能是 "media/image1.png"，拼接为 "ppt/media/image1.png"
                    val fullPath = if (target.startsWith("../")) {
                        "ppt/" + target.removePrefix("../")
                    } else if (target.startsWith("media/")) {
                        "ppt/$target"
                    } else {
                        "ppt/media/$target"
                    }
                    refs.add(fullPath)
                }
            }
        } catch (_: Exception) {}
        return refs
    }

    // ===== DOCX =====
    private fun extractDocx(file: File): String {
        return try {
            val xml = readZipEntry(file, "word/document.xml") ?: return "无法读取文档"
            val sb = StringBuilder()
            val parser = createParser(xml)
            var inT = false
            var currentPara = StringBuilder()
            var event = parser.eventType
            while (event != XmlPullParser.END_DOCUMENT) {
                when (event) {
                    XmlPullParser.START_TAG -> {
                        when (parser.name) {
                            "t", "w:t" -> inT = true
                            "br", "w:br" -> currentPara.append("\n")
                        }
                    }
                    XmlPullParser.TEXT -> {
                        if (inT) currentPara.append(parser.text)
                    }
                    XmlPullParser.END_TAG -> {
                        when (parser.name) {
                            "t", "w:t" -> inT = false
                            "p", "w:p" -> {
                                val paraText = currentPara.toString().trim()
                                if (paraText.isNotEmpty()) {
                                    sb.append(paraText).append("\n")
                                }
                                currentPara = StringBuilder()
                            }
                        }
                    }
                }
                event = parser.next()
            }
            val result = sb.toString().trim()
            result.ifEmpty { "(空文档)" }
        } catch (e: Exception) { "解析失败: ${e.message}" }
    }

    // ===== XLSX =====
    private fun extractXlsx(file: File): String {
        try {
            val sharedXml = readZipEntry(file, "xl/sharedStrings.xml")
            val sharedMap = mutableMapOf<Int, String>()
            if (sharedXml != null) {
                try {
                    val siRegex = Regex("<si>(.*?)</si>|<si[^>]*>(.*?)</si>", RegexOption.DOT_MATCHES_ALL)
                    var idx = 0
                    for (match in siRegex.findAll(sharedXml)) {
                        val content = (match.groupValues[1] + match.groupValues[2])
                            .replace(Regex("<[^>]+>"), "")
                            .replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">")
                            .replace("&quot;", "\"").replace("&apos;", "'")
                            .trim()
                        if (content.isNotEmpty()) sharedMap[idx] = content
                        idx++
                    }
                } catch (_: Exception) {}
            }

            val sheetXml = readZipEntry(file, "xl/worksheets/sheet1.xml")
            if (sheetXml == null) return "(空表格)"

            val sdStart = sheetXml.indexOf("<sheetData")
            val sdEnd = sheetXml.lastIndexOf("</sheetData>")
            val sheetData = if (sdStart >= 0 && sdEnd > sdStart) {
                sheetXml.substring(sdStart, sdEnd + 12)
            } else sheetXml

            val rowRegex = Regex("<row[^>]*>(.*?)</row>", RegexOption.DOT_MATCHES_ALL)
            val rows = rowRegex.findAll(sheetData)
            if (rows.count() == 0) return "(空表格)"

            val sb = StringBuilder()
            for (rowMatch in rows) {
                val rowContent = rowMatch.groupValues[1]
                val cellRegex = Regex("<c[^>]*t\\s*=\\s*\"s\"[^>]*>(.*?)</c>|<c[^>]*>(.*?)</c>", RegexOption.DOT_MATCHES_ALL)
                val cells = cellRegex.findAll(rowContent)
                for (cellMatch in cells) {
                    val sType = cellMatch.groupValues[1].isNotEmpty()
                    val body = if (sType) cellMatch.groupValues[1] else cellMatch.groupValues[2]
                    val vMatch = Regex("<v>(.*?)</v>", RegexOption.DOT_MATCHES_ALL).find(body)
                    val v = vMatch?.groupValues?.get(1)?.trim() ?: ""
                    if (v.isNotEmpty()) {
                        val num = v.toIntOrNull()
                        if (sType && num != null && sharedMap.containsKey(num)) {
                            sb.append(sharedMap[num])
                        } else {
                            sb.append(v)
                        }
                        sb.append("\t")
                    }
                }
                sb.append("\n")
            }

            val result = sb.toString().trim()
            return result.ifEmpty { "(空表格)" }
        } catch (e: Exception) {
            return "解析失败: ${e.message}"
        }
    }

    // ===== PPTX (单页兼容旧调用) =====
    private fun extractPptx(file: File): String {
        try {
            val slideXml = readZipEntry(file, "ppt/slides/slide1.xml")
            if (slideXml == null) return "(空演示文稿)"
            return extractSlideText(slideXml).ifEmpty { "(空演示文稿)" }
        } catch (e: Exception) {
            return "解析失败: ${e.message}"
        }
    }

    // ===== 工具 =====
    fun readZipEntry(file: File, entryName: String): String? {
        return try {
            ZipFile(file).use { zip ->
                val entry = zip.getEntry(entryName) ?: return null
                zip.getInputStream(entry).bufferedReader().readText()
            }
        } catch (e: Exception) { null }
    }

    fun readZipEntryBytes(file: File, entryName: String): ByteArray? {
        return try {
            ZipFile(file).use { zip ->
                val entry = zip.getEntry(entryName) ?: return null
                zip.getInputStream(entry).readBytes()
            }
        } catch (_: Exception) { null }
    }

    private fun createParser(xml: String): XmlPullParser {
        val cleaned = xml.trimStart('\uFEFF', '\u200B', '\u00A0', ' ', '\t', '\n', '\r')
        val factory = XmlPullParserFactory.newInstance()
        factory.isNamespaceAware = false
        val parser = factory.newPullParser()
        parser.setInput(StringReader(cleaned))
        return parser
    }
}
