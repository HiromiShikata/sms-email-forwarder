package com.hiromi_shikata.smsemailforwarder

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.w3c.dom.Element
import javax.xml.parsers.DocumentBuilderFactory

class AndroidManifestSystemForegroundServiceTypeTest {

    private val manifest by lazy {
        val file = java.io.File("src/main/AndroidManifest.xml")
        DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
    }

    @Test
    fun `SystemForegroundService is declared with foregroundServiceType dataSync`() {
        val services = manifest.getElementsByTagName("service")
        var systemForegroundService: Element? = null
        for (i in 0 until services.length) {
            val service = services.item(i) as Element
            if (service.getAttribute("android:name") == "androidx.work.impl.foreground.SystemForegroundService") {
                systemForegroundService = service
                break
            }
        }
        assertNotNull(
            "SystemForegroundService must be declared in AndroidManifest.xml",
            systemForegroundService,
        )
        assertEquals(
            "dataSync",
            systemForegroundService!!.getAttribute("android:foregroundServiceType"),
        )
    }
}
