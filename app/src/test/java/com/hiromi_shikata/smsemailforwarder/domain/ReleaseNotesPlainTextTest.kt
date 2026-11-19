package com.hiromi_shikata.smsemailforwarder.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class ReleaseNotesPlainTextTest {
    @Test
    fun `fromHtml converts release note markup to plain text`() {
        val cases = listOf(
            "list items become dash lines" to Pair(
                "<ul><li>Add update check</li><li>Fix inset</li></ul>",
                "- Add update check\n- Fix inset",
            ),
            "paragraphs and line breaks become newlines" to Pair(
                "<p>First</p><p>Second<br>Third</p>",
                "First\nSecond\nThird",
            ),
            "escaped entities are decoded after tags are removed" to Pair(
                "<p>Use &lt;b&gt; &amp; &quot;quotes&quot; &#39;x&#39; &#x41;</p>",
                "Use <b> & \"quotes\" 'x' A",
            ),
            "other tags are removed" to Pair(
                "<p><strong>Bold</strong> <code>code</code></p>",
                "Bold code",
            ),
            "empty input gives empty text" to Pair("", ""),
        )
        cases.forEach { (name, case) ->
            assertEquals(name, case.second, ReleaseNotesPlainText.fromHtml(case.first))
        }
    }
}
