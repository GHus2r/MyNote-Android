package com.mynote.android.util

import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * 零三方库 DOCX 生成器 — 完整 OOXML 规范（Word 2010+ 兼容）
 */
object DocxWriter {

    // ── 预编译正则 ──
    private val STYLE_SCRIPT_RE = Regex("""<(style|script)[^>]*>.*?</\1>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
    private val BLOCK_SPLIT_RE = Regex("""<br\s*/?>|</p>|</div>|</h[1-6]>|</li>|<p[^>]*>|<div[^>]*>|<h[1-6][^>]*>""", RegexOption.IGNORE_CASE)
    private val HEADING_RE = Regex("""<h([1-6])[^>]*>(.*?)</h\1>""", RegexOption.IGNORE_CASE)
    private val B_TAG_RE = Regex("""<b[^>]*>(.*?)</b>""", RegexOption.IGNORE_CASE)
    private val I_TAG_RE = Regex("""<i[^>]*>(.*?)</i>""", RegexOption.IGNORE_CASE)

    fun write(html: String, outFile: File) {
        val body = buildBodyXml(html)
        ZipOutputStream(FileOutputStream(outFile)).use { zip ->
            putEntry(zip, "[Content_Types].xml", CONTENT_TYPES)
            putEntry(zip, "_rels/.rels", ROOT_RELS)
            putEntry(zip, "word/_rels/document.xml.rels", WORD_RELS)
            putEntry(zip, "word/document.xml", documentXml(body))
            putEntry(zip, "word/styles.xml", STYLES)
            putEntry(zip, "word/settings.xml", SETTINGS)
            putEntry(zip, "word/fontTable.xml", FONT_TABLE)
        }
    }

    fun writeMedicalRecords(file: File, patient: com.mynote.android.data.entity.Patient, records: List<com.mynote.android.data.entity.MedicalRecord>) {
        val sb = StringBuilder()
        sb.append("<h1>${esc(patient.name)} 病历</h1>")
        sb.append("<p><b>科室：</b>${esc(patient.department)}　<b>年龄：</b>${patient.age}　<b>性别：</b>${esc(patient.gender)}</p>")
        sb.append("<p><b>入院：</b>${esc(patient.admissionDate)}　<b>诊断：</b>${esc(patient.diagnosis)}</p>")
        sb.append("<hr>")
        for (r in records) {
            sb.append("<h2>${esc(r.type)}</h2>")
            // 简单段落处理：空行 → <br>，其余每行一个 <p>
            val paragraphs = r.content.split("\n")
            for (p in paragraphs) {
                val t = p.trim()
                if (t.isEmpty()) sb.append("<br>")
                else sb.append("<p>${esc(t)}</p>")
            }
            sb.append("<p>　</p>")
        }
        write(sb.toString(), file)
    }

    private fun esc(s: String) = s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

    private fun putEntry(zip: ZipOutputStream, name: String, content: String) {
        zip.putNextEntry(ZipEntry(name))
        zip.write(content.toByteArray(Charsets.UTF_8))
        zip.closeEntry()
    }

    // ── 内容生成 ──

    private fun buildBodyXml(html: String): String {
        val sb = StringBuilder()

        // 移除 <style>...</style> 和 <script>...</script> 完整块
        var h = STYLE_SCRIPT_RE.replace(html, "")
        h = h.replace(Regex("</?(head|body|meta|font)[^>]*>", RegexOption.IGNORE_CASE), "")
        h = h.replace(Regex("<ul[^>]*>|</ul>|<ol[^>]*>|</ol>", RegexOption.IGNORE_CASE), "")

        // 按块级元素分割（不含捕获组，分隔符被丢弃）
        val tokens = h.split(BLOCK_SPLIT_RE)
        for (token in tokens) {
            val t = token.trim()
            if (t.isEmpty()) continue
            sb.append(tokenToParagraph(t))
        }

        if (sb.isEmpty()) {
            sb.append(emptyParagraph())
        }

        return sb.toString()
    }

    private fun tokenToParagraph(token: String): String {
        val trimmed = token.trim()

        // 检测 h1-h6 标题（惰性匹配）
        val hMatch = HEADING_RE.find(trimmed)
        if (hMatch != null) {
            val level = hMatch.groupValues[1].toInt()
            val inner = hMatch.groupValues[2]
            val text = stripTags(inner)
            if (text.isBlank()) return ""
            val sz = (40 - level * 4).toString()
            val runs = runXml(text, bold = true, fontSize = sz, font = "Microsoft YaHei")
            val align = if (level <= 1) "center" else "left"
            return paragraphXml(runs, align = align, spacingAfter = "120")
        }

        // 检测 li
        if (trimmed.startsWith("<li", ignoreCase = true)) {
            val text = stripTags(trimmed.replace(Regex("</?li[^>]*>", RegexOption.IGNORE_CASE), ""))
            if (text.isBlank()) return ""
            return paragraphXml(runXml("• $text", fontSize = "22"), spacingAfter = "60", indent = "360")
        }

        // 普通段落 — 解析内联标签
        val runs = mutableListOf<String>()
        var remaining = trimmed
        while (remaining.isNotEmpty()) {
            val bMatch = B_TAG_RE.find(remaining)
            val iMatch = I_TAG_RE.find(remaining)
            val first = listOfNotNull(
                bMatch?.let { it to "b" },
                iMatch?.let { it to "i" }
            ).minByOrNull { it.first.range.first }

            if (first == null) {
                val text = stripTags(remaining)
                if (text.isNotBlank()) {
                    runs.add(runXml(text, fontSize = "22"))
                }
                break
            }

            val (match, type) = first
            val before = stripTags(remaining.substring(0, match.range.first))
            if (before.isNotBlank()) {
                runs.add(runXml(before, fontSize = "22"))
            }
            val inner = stripTags(match.groupValues[1])
            if (inner.isNotBlank()) {
                runs.add(runXml(inner, bold = type == "b", italic = type == "i", fontSize = "22"))
            }
            remaining = remaining.substring(match.range.last + 1)
        }

        if (runs.isEmpty()) return ""
        return paragraphXml(runs.joinToString(""), spacingAfter = "80")
    }

    private fun stripTags(s: String): String {
        return s.replace(Regex("<[^>]+>"), "")
            .replace("&nbsp;", " ")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .trim()
    }

    private fun escapeXml(s: String): String = s
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")

    // ── OOXML 片段 ──

    private fun runXml(
        text: String,
        bold: Boolean = false,
        italic: Boolean = false,
        fontSize: String = "22",
        font: String = "Microsoft YaHei"
    ): String {
        val safe = escapeXml(text)
        return buildString {
            append("<w:r>")
            append("<w:rPr>")
            if (bold) append("<w:b/><w:bCs/>")
            if (italic) append("<w:i/><w:iCs/>")
            append("<w:sz w:val=\"$fontSize\"/>")
            append("<w:szCs w:val=\"$fontSize\"/>")
            append("<w:rFonts w:eastAsia=\"$font\" w:ascii=\"$font\" w:hAnsi=\"$font\"/>")
            append("<w:lang w:val=\"zh-CN\" w:eastAsia=\"zh-CN\"/>")
            append("</w:rPr>")
            append("<w:t xml:space=\"preserve\">$safe</w:t>")
            append("</w:r>")
        }
    }

    private fun paragraphXml(
        runs: String,
        align: String = "left",
        spacingAfter: String = "80",
        indent: String = "0"
    ): String {
        return buildString {
            append("<w:p>")
            append("<w:pPr>")
            if (align != "left") append("<w:jc w:val=\"$align\"/>")
            append("<w:spacing w:after=\"$spacingAfter\" w:line=\"360\" w:lineRule=\"auto\"/>")
            if (indent != "0") append("<w:ind w:left=\"$indent\" w:hanging=\"360\"/>")
            append("</w:pPr>")
            append(runs)
            append("</w:p>")
        }
    }

    private fun emptyParagraph() = paragraphXml(runXml(" ", fontSize = "22"))

    // ── OOXML 固定模板 ──

    private fun documentXml(body: String) = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:document xmlns:wpc="http://schemas.microsoft.com/office/word/2010/wordprocessingCanvas"
            xmlns:mc="http://schemas.openxmlformats.org/markup-compatibility/2006"
            xmlns:o="urn:schemas-microsoft-com:office:office"
            xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"
            xmlns:m="http://schemas.openxmlformats.org/officeDocument/2006/math"
            xmlns:v="urn:schemas-microsoft-com:vml"
            xmlns:wp="http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing"
            xmlns:w10="urn:schemas-microsoft-com:office:word"
            xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"
            xmlns:wne="http://schemas.microsoft.com/office/word/2006/wordml">
<w:body>
$body
<w:sectPr>
<w:pgSz w:w="11906" w:h="16838"/>
<w:pgMar w:top="1440" w:right="1440" w:bottom="1440" w:left="1440" w:header="851" w:footer="851" w:gutter="0"/>
<w:cols w:space="425"/>
<w:docGrid w:type="lines" w:linePitch="312"/>
</w:sectPr>
</w:body>
</w:document>""".trimIndent()

    private val CONTENT_TYPES = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
<Default Extension="xml" ContentType="application/xml"/>
<Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
<Override PartName="/word/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.styles+xml"/>
<Override PartName="/word/settings.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.settings+xml"/>
<Override PartName="/word/fontTable.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.fontTable+xml"/>
</Types>""".trimIndent()

    private val ROOT_RELS = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
</Relationships>""".trimIndent()

    private val WORD_RELS = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
<Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/settings" Target="settings.xml"/>
<Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/fontTable" Target="fontTable.xml"/>
</Relationships>""".trimIndent()

    private val STYLES = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:styles xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
<w:docDefaults>
<w:rPrDefault><w:rPr>
<w:rFonts w:ascii="Microsoft YaHei" w:hAnsi="Microsoft YaHei" w:eastAsia="Microsoft YaHei"/>
<w:sz w:val="22"/><w:szCs w:val="22"/>
<w:lang w:val="zh-CN" w:eastAsia="zh-CN"/>
</w:rPr></w:rPrDefault>
<w:pPrDefault><w:pPr><w:spacing w:line="360" w:lineRule="auto"/></w:pPr></w:pPrDefault>
</w:docDefaults>
</w:styles>""".trimIndent()

    private val SETTINGS = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:settings xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
<w:defaultTabStop w:val="420"/>
<w:characterSpacingControl w:val="compressPunctuation"/>
</w:settings>""".trimIndent()

    private val FONT_TABLE = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:fonts xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
<w:font w:name="Microsoft YaHei"><w:family w:val="swiss"/><w:charset w:val="86"/></w:font>
</w:fonts>""".trimIndent()
}
