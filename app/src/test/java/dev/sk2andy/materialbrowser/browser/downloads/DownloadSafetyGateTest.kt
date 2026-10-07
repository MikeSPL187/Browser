package dev.sk2andy.materialbrowser.browser.downloads

import dev.sk2andy.materialbrowser.data.BrowserDownloadRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DownloadSafetyGateTest {
    @Test
    fun `a request with findings waits and saves or cancels once`() {
        val gate = DownloadSafetyGate()
        val events = mutableListOf<String>()
        val request = BrowserDownloadRequest(
            url = "https://www.files.example/get?id=20",
            fileName = "invoice.pdf.apk",
            mimeType = "application/vnd.android.package-archive",
        )

        assertTrue(gate.hold(request, save = { events += "save" }, cancel = { events += "cancel" }))
        assertEquals("files.example", gate.pending?.sourceHost)
        gate.save()
        gate.cancel()

        assertEquals(listOf("save"), events)
        assertNull(gate.pending)
    }

    @Test
    fun `a file checked by its caller is held by its own name and host`() {
        val gate = DownloadSafetyGate()
        val events = mutableListOf<String>()
        val findings = listOf(DownloadSafetyFinding.DisguisedName, DownloadSafetyFinding.AndroidApp)

        assertFalse(gate.hold("photo.png", "files.example", emptyList(), save = {}, cancel = {}))
        assertTrue(gate.hold("invoice.pdf.apk", "files.example", findings, { events += "save" }, { events += "cancel" }))

        val pending = requireNotNull(gate.pending)
        assertEquals("invoice.pdf.apk", pending.fileName)
        assertEquals("files.example", pending.sourceHost)
        assertEquals(findings, pending.findings)
        gate.cancel()
        assertEquals(listOf("cancel"), events)
    }

    @Test
    fun `hosts are named without www and only when the address has one`() {
        assertEquals("files.example", DownloadSafetyGate.hostOf("https://www.files.example/a"))
        assertNull(DownloadSafetyGate.hostOf("blob:https://files.example/1"))
        assertNull(DownloadSafetyGate.hostOf(null))
        assertNull(DownloadSafetyGate.hostOf("not a url"))
    }
}
