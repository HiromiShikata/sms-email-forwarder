package com.hiromi_shikata.smsemailforwarder.data.remote

import com.hiromi_shikata.smsemailforwarder.domain.ReleaseNotesPlainText
import com.hiromi_shikata.smsemailforwarder.domain.entity.AppRelease
import org.w3c.dom.Element
import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory
import org.xml.sax.InputSource

class GithubReleasesAtomParser(
    private val githubRepo: String,
) {
    fun parse(atomXml: String): List<AppRelease> {
        val factory = DocumentBuilderFactory.newInstance()
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
        val document = factory.newDocumentBuilder().parse(InputSource(StringReader(atomXml)))
        val entries = document.getElementsByTagName("entry")
        return (0 until entries.length).map { index ->
            val entry = entries.item(index) as Element
            val title = childText(entry, "title")
                ?: throw IllegalArgumentException("Release feed entry has no title")
            val version = title.removePrefix("v")
            val html = childText(entry, "content") ?: ""
            AppRelease(
                version = version,
                notes = ReleaseNotesPlainText.fromHtml(html),
                apkDownloadUrl = apkDownloadUrl(version),
            )
        }
    }

    fun apkDownloadUrl(version: String): String =
        "https://github.com/$githubRepo/releases/download/v$version/sms-email-forwarder_universal.apk"

    private fun childText(entry: Element, tag: String): String? {
        val nodes = entry.getElementsByTagName(tag)
        if (nodes.length == 0) return null
        return nodes.item(0).textContent.trim()
    }
}
