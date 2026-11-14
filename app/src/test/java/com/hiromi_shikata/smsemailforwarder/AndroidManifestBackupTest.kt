package com.hiromi_shikata.smsemailforwarder

import org.junit.Assert.assertEquals
import org.junit.Test
import org.w3c.dom.Element
import javax.xml.parsers.DocumentBuilderFactory

class AndroidManifestBackupTest {

    private val manifest by lazy {
        val file = java.io.File("src/main/AndroidManifest.xml")
        DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
    }

    @Test
    fun `application declares allowBackup false`() {
        val applications = manifest.getElementsByTagName("application")
        assertEquals("manifest must declare exactly one application element", 1, applications.length)
        val application = applications.item(0) as Element
        assertEquals("false", application.getAttribute("android:allowBackup"))
    }
}
