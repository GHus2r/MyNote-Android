package com.mynote.android.util

/**
 * HTML → 纯文本 / Markdown 转换
 */
object HtmlConverter {

    // ── 预编译正则 ──
    private val BR_RE = Regex("<br[^>]*/?>", RegexOption.IGNORE_CASE)
    private val BLOCK_END_RE = Regex("</p>|</div>|</li>|</h[1-6]>|</tr>", RegexOption.IGNORE_CASE)
    private val TAG_RE = Regex("<[^>]+>")
    private val NEWLINE_COMPACT_RE = Regex("\n{3,}")
    private val H1_RE = Regex("<h1[^>]*>(.*?)</h1>", RegexOption.IGNORE_CASE)
    private val H2_RE = Regex("<h2[^>]*>(.*?)</h2>", RegexOption.IGNORE_CASE)
    private val H3_RE = Regex("<h3[^>]*>(.*?)</h3>", RegexOption.IGNORE_CASE)
    private val H4_RE = Regex("<h4[^>]*>(.*?)</h4>", RegexOption.IGNORE_CASE)
    private val H5_RE = Regex("<h5[^>]*>(.*?)</h5>", RegexOption.IGNORE_CASE)
    private val H6_RE = Regex("<h6[^>]*>(.*?)</h6>", RegexOption.IGNORE_CASE)
    private val BOLD_RE = Regex("<b[^>]*>(.*?)</b>|<strong[^>]*>(.*?)</strong>", RegexOption.IGNORE_CASE)
    private val ITALIC_RE = Regex("<i[^>]*>(.*?)</i>|<em[^>]*>(.*?)</em>", RegexOption.IGNORE_CASE)
    private val UNDERLINE_RE = Regex("<u[^>]*>(.*?)</u>", RegexOption.IGNORE_CASE)
    private val LI_RE = Regex("<li[^>]*>(.*?)</li>", RegexOption.IGNORE_CASE)
    private val LINK_RE = Regex("""<a[^>]*href\s*=\s*["'](.*?)["'][^>]*>(.*?)</a>""", RegexOption.IGNORE_CASE)
    private val IMG_ALT_RE = Regex("""<img[^>]*alt\s*=\s*["'](.*?)["'][^>]*/?>""", RegexOption.IGNORE_CASE)
    private val IMG_RE = Regex("<img[^>]*/?>", RegexOption.IGNORE_CASE)
    private val DEL_RE = Regex("<del[^>]*>(.*?)</del>|<s[^>]*>(.*?)</s>", RegexOption.IGNORE_CASE)

    /** HTML → 纯文本（去标签，保留结构换行） */
    fun toText(html: String): String {
        var text = html
            .replace(BR_RE, "\n")
            .replace(BLOCK_END_RE, "\n")
            .replace(TAG_RE, "")
            .replace("&nbsp;", " ")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace(NEWLINE_COMPACT_RE, "\n\n")
            .trim()
        return text
    }

    /** HTML → Markdown */
    fun toMarkdown(html: String): String {
        var md = html
            .replace(H1_RE) { "# ${stripTags(it.groupValues[1])}" }
            .replace(H2_RE) { "## ${stripTags(it.groupValues[1])}" }
            .replace(H3_RE) { "### ${stripTags(it.groupValues[1])}" }
            .replace(H4_RE) { "#### ${stripTags(it.groupValues[1])}" }
            .replace(H5_RE) { "##### ${stripTags(it.groupValues[1])}" }
            .replace(H6_RE) { "###### ${stripTags(it.groupValues[1])}" }
            .replace(BOLD_RE) {
                "**${stripTags(it.groupValues[1].ifEmpty { it.groupValues[2] })}**"
            }
            .replace(ITALIC_RE) {
                "*${stripTags(it.groupValues[1].ifEmpty { it.groupValues[2] })}*"
            }
            .replace(UNDERLINE_RE) { stripTags(it.groupValues[1]) }
            .replace(LI_RE) { "- ${stripTags(it.groupValues[1])}" }
            .replace(BR_RE, "\n")
            .replace(BLOCK_END_RE, "\n")
            .replace(LINK_RE) {
                "[${stripTags(it.groupValues[2])}](${it.groupValues[1]})"
            }
            .replace(IMG_ALT_RE) { "[图片: ${it.groupValues[1]}]" }
            .replace(IMG_RE, "[图片]")
            .replace(DEL_RE) {
                "~~${stripTags(it.groupValues[1].ifEmpty { it.groupValues[2] })}~~"
            }
            .replace(TAG_RE, "")
            .replace("&nbsp;", " ")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace(NEWLINE_COMPACT_RE, "\n\n")
            .trim()
        return md
    }

    private fun stripTags(s: String): String {
        return s.replace(TAG_RE, "")
            .replace("&nbsp;", " ")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
    }
}
