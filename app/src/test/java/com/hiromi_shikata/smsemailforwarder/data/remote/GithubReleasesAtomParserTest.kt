package com.hiromi_shikata.smsemailforwarder.data.remote

import com.hiromi_shikata.smsemailforwarder.domain.entity.AppRelease
import org.junit.Assert.assertEquals
import org.junit.Test

class GithubReleasesAtomParserTest {
    private val parser = GithubReleasesAtomParser("owner/app")

    private val feed = """
        <?xml version="1.0" encoding="UTF-8"?>
        <feed xmlns="http://www.w3.org/2005/Atom">
          <entry>
            <id>tag:github.com,2008:Repository/1/v0.1.346</id>
            <title>0.1.346</title>
            <link rel="alternate" type="text/html" href="https://github.com/owner/app/releases/tag/v0.1.346"/>
            <content type="html">&lt;ul&gt;&lt;li&gt;Add &amp;quot;check&amp;quot;&lt;/li&gt;&lt;li&gt;Fix&lt;/li&gt;&lt;/ul&gt;</content>
          </entry>
          <entry>
            <title>v0.1.345</title>
            <content type="html">&lt;p&gt;Initial&lt;/p&gt;</content>
          </entry>
        </feed>
    """.trimIndent()

    @Test
    fun `parse returns each entry with plain notes and the release apk url`() {
        val expected = listOf(
            AppRelease(
                version = "0.1.346",
                notes = "- Add \"check\"\n- Fix",
                apkDownloadUrl = "https://github.com/owner/app/releases/download/v0.1.346/sms-email-forwarder_universal.apk",
            ),
            AppRelease(
                version = "0.1.345",
                notes = "Initial",
                apkDownloadUrl = "https://github.com/owner/app/releases/download/v0.1.345/sms-email-forwarder_universal.apk",
            ),
        )
        assertEquals(expected, parser.parse(feed))
    }

    @Test
    fun `parse returns no releases for a feed without entries`() {
        val empty = """<feed xmlns="http://www.w3.org/2005/Atom"><title>x</title></feed>"""
        assertEquals(emptyList<AppRelease>(), parser.parse(empty))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `parse fails when an entry has no title`() {
        parser.parse("""<feed xmlns="http://www.w3.org/2005/Atom"><entry><content>x</content></entry></feed>""")
    }
}
