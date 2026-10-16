package dev.sk2andy.materialbrowser.data

import dev.sk2andy.materialbrowser.browser.downloads.DownloadSafetyCheck
import dev.sk2andy.materialbrowser.browser.downloads.DownloadSafetyFinding
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BrowserDownloadRequestFactoryTest {
    @Test
    fun `rejects unsafe schemes and embedded credentials`() {
        assertNull(BrowserDownloadRequestFactory.create("javascript:alert(1)"))
        assertNull(BrowserDownloadRequestFactory.create("file:///data/private"))
        assertNull(BrowserDownloadRequestFactory.create("https://user:password@example.com/file"))
    }

    @Test
    fun `sanitizes traversal and control characters from server file name`() {
        val request = BrowserDownloadRequestFactory.create(
            url = "https://example.com/fallback",
            contentDisposition = "attachment; filename=\"../../bad\r\nname\"",
            mimeType = "image/png; charset=utf-8",
        )

        assertEquals("_.._bad__name.png", request?.fileName)
        assertEquals("image/png", request?.mimeType)
    }

    @Test
    fun `uses UTF-8 content disposition and keeps literal plus`() {
        val request = BrowserDownloadRequestFactory.create(
            url = "https://example.com/fallback",
            contentDisposition = "attachment; filename*=UTF-8''Gr%C3%BCn+Logo.png",
            mimeType = "image/png",
        )

        assertEquals("Grün+Logo.png", request?.fileName)
    }

    @Test
    fun `derives decoded file name and extension from URL and MIME`() {
        val request = BrowserDownloadRequestFactory.create(
            url = "https://example.com/My%20Image?size=large",
            mimeType = "image/webp",
        )

        assertEquals("My Image.webp", request?.fileName)
    }

    @Test
    fun `infers text and JSON MIME types from downloadable link paths`() {
        val textRequest = BrowserDownloadRequestFactory.create(
            url = "https://example.com/notes.txt?raw=1",
        )
        val jsonRequest = BrowserDownloadRequestFactory.create(
            url = "https://example.com/export.JSON",
        )

        assertEquals("text/plain", textRequest?.mimeType)
        assertEquals("notes.txt", textRequest?.fileName)
        assertEquals("application/json", jsonRequest?.mimeType)
        assertEquals("export.JSON", jsonRequest?.fileName)
    }

    @Test
    fun `the final type of an APK is an Android package whatever the server sent`() {
        val androidPackage = "application/vnd.android.package-archive"

        assertEquals(androidPackage, SafeDownloadValues.finalMimeType("App.apk", "application/octet-stream"))
        assertEquals(androidPackage, SafeDownloadValues.finalMimeType("invoice.pdf.APK", "application/pdf"))
        assertEquals("application/pdf", SafeDownloadValues.finalMimeType("invoice.pdf", "application/pdf"))
        assertEquals(
            "application/octet-stream",
            SafeDownloadValues.finalMimeType("app.apk.zip", "application/octet-stream"),
        )
    }

    @Test
    fun `infers Android package MIME type from APK link`() {
        val request = BrowserDownloadRequestFactory.create(
            url = "https://example.com/releases/latest/download/App.apk",
        )

        assertEquals("application/vnd.android.package-archive", request?.mimeType)
        assertEquals("App.apk", request?.fileName)
        assertTrue(BrowserDownloadRequestFactory.isAndroidPackage(requireNotNull(request)))
        val genericServerRequest = BrowserDownloadRequestFactory.create(
            url = "https://example.com/download?id=1",
            contentDisposition = "attachment; filename=App.apk",
            mimeType = "application/octet-stream",
        )
        assertEquals("application/vnd.android.package-archive", genericServerRequest?.mimeType)
        assertEquals("App.apk", genericServerRequest?.fileName)
        assertTrue(
            BrowserDownloadRequestFactory.isAndroidPackage(
                BrowserDownloadRequest(
                    url = "https://example.com/download?id=1",
                    fileName = "App.apk",
                    mimeType = "application/octet-stream",
                ),
            ),
        )
        assertFalse(
            BrowserDownloadRequestFactory.isAndroidPackage(
                BrowserDownloadRequest(
                    url = "https://example.com/download?id=1",
                    fileName = "archive.zip",
                    mimeType = "application/zip",
                ),
            ),
        )
    }

    @Test
    fun `drops injected headers and bounds file name`() {
        val request = BrowserDownloadRequestFactory.create(
            url = "https://example.com/${"a".repeat(200)}.png",
            mimeType = "image/png",
            userAgent = "Browser\r\nX-Evil: yes",
            cookies = "session=safe",
        )

        assertNull(request?.userAgent)
        assertEquals("session=safe", request?.cookies)
        assertTrue(requireNotNull(request).fileName.length <= 120)
        assertTrue(request.fileName.endsWith(".png"))
    }

    @Test
    fun `lookalike dots cannot hide an app behind a document name`() {
        val name = SafeDownloadValues.fileName(
            url = "https://example.com/fallback",
            contentDisposition = "attachment; filename*=UTF-8''invoice%E2%80%A4pdf%EF%BC%8Eapk",
            mimeType = "application/octet-stream",
        )

        assertEquals("invoice.pdf.apk", name)
        assertTrue(
            DownloadSafetyFinding.DisguisedName in
                DownloadSafetyCheck.findings("https://example.com/fallback", name, "application/octet-stream"),
        )
    }

    @Test
    fun `C1 controls and line separators are replaced like ASCII ones`() {
        val name = SafeDownloadValues.fileName(
            url = "https://example.com/fallback",
            contentDisposition = "attachment; filename*=UTF-8''a%C2%85b%E2%80%A8c.txt",
            mimeType = "text/plain",
        )

        assertEquals("a_b_c.txt", name)
    }

    @Test
    fun `long names fit the file system in bytes and keep whole characters`() {
        val emoji = SafeDownloadValues.fileName(
            url = "https://example.com/${"%F0%9F%93%84".repeat(100)}.pdf",
            contentDisposition = null,
            mimeType = "application/pdf",
        )
        val cjk = SafeDownloadValues.fileName(
            url = "https://example.com/${"%E6%96%87".repeat(100)}.pdf",
            contentDisposition = null,
            mimeType = "application/pdf",
        )

        assertEquals("\uD83D\uDCC4".repeat(58) + ".pdf", emoji)
        assertEquals("\u6587".repeat(78) + ".pdf", cjk)
        assertTrue(cjk.toByteArray(Charsets.UTF_8).size <= 240)
    }

    @Test
    fun `keeps safe referrer without fragment and rejects unsafe referrer`() {
        val request = BrowserDownloadRequestFactory.create(
            url = "https://example.com/file.pdf",
            mimeType = "application/pdf",
            referrer = "https://example.com/account?tab=files#private-section",
        )

        assertEquals("https://example.com/account?tab=files", request?.referrer)
        assertNull(
            BrowserDownloadRequestFactory.create(
                url = "https://example.com/file.pdf",
                referrer = "javascript:alert(1)",
            )?.referrer,
        )
    }

    @Test
    fun `limits cross-origin referrer and omits downgrade referrer`() {
        val crossOrigin = BrowserDownloadRequestFactory.create(
            url = "https://downloads.example.net/file.pdf",
            referrer = "https://example.com/account?token=secret#section",
        )
        val downgrade = BrowserDownloadRequestFactory.create(
            url = "http://example.com/file.pdf",
            referrer = "https://example.com/account?token=secret",
        )

        assertEquals("https://example.com", crossOrigin?.referrer)
        assertNull(downgrade?.referrer)
    }
}
