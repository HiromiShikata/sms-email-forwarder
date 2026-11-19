package com.hiromi_shikata.smsemailforwarder.domain

object ReleaseNotesPlainText {
    private val lineBreakTag = Regex("(?i)<br\\s*/?>")
    private val blockEndTag = Regex("(?i)</(p|div|h[1-6]|ul|ol)>")
    private val listItemStartTag = Regex("(?i)<li[^>]*>")
    private val listItemEndTag = Regex("(?i)</li>")
    private val anyTag = Regex("<[^>]+>")
    private val numericEntity = Regex("&#(x[0-9a-fA-F]+|[0-9]+);")
    private val blankRuns = Regex("\n{3,}")

    fun fromHtml(html: String): String {
        val withLineBreaks = html
            .replace(lineBreakTag, "\n")
            .replace(blockEndTag, "\n")
            .replace(listItemStartTag, "\n- ")
            .replace(listItemEndTag, "")
        val withoutTags = withLineBreaks.replace(anyTag, "")
        val decoded = decodeEntities(withoutTags)
        return decoded
            .lines()
            .map { it.trimEnd() }
            .joinToString("\n")
            .replace(blankRuns, "\n\n")
            .trim()
    }

    private fun decodeEntities(text: String): String {
        val named = text
            .replace("&nbsp;", " ")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&amp;", "&")
        return numericEntity.replace(named) { match ->
            val value = match.groupValues[1]
            val codePoint = if (value.startsWith("x", ignoreCase = true)) {
                value.substring(1).toInt(16)
            } else {
                value.toInt()
            }
            String(Character.toChars(codePoint))
        }
    }
}
