package dev.sk2andy.materialbrowser.browser

import dev.sk2andy.materialbrowser.browser.downloads.DownloadSafetyFinding
import dev.sk2andy.materialbrowser.browser.permissions.SitePermission
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BrowserEngineRequestsTest {
    @Test
    fun `download response uses bounded shared request factory`() {
        val request = BrowserEngineDownloadRules.request(
            response = BrowserEngineDownloadResponse(
                url = "https://downloads.example/manual",
                contentDisposition = "attachment; filename=manual.pdf",
                mimeType = "application/pdf; charset=binary",
            ),
            referrer = "https://downloads.example/start",
        )

        assertEquals("manual.pdf", request?.fileName)
        assertEquals("application/pdf", request?.mimeType)
        assertEquals("https://downloads.example/start", request?.referrer)
        assertNull(
            BrowserEngineDownloadRules.request(
                BrowserEngineDownloadResponse(
                    url = "file:///data/user/0/private",
                    contentDisposition = null,
                    mimeType = null,
                ),
                referrer = null,
            ),
        )
    }

    @Test
    fun `a blob program gets the same file check as one from the network`() {
        val disguised = BrowserEngineDownloadRules.localDownload(
            BrowserEngineDownloadResponse(
                url = "blob:https://files.example/0f6c3a0e-2b4c-4b8e-9d55-0d1b9b0a8c11",
                contentDisposition = "attachment; filename=\"invoice.pdf.apk\"",
                mimeType = "application/octet-stream",
            ),
        )

        assertEquals("invoice.pdf.apk", disguised.fileName)
        assertEquals("application/vnd.android.package-archive", disguised.mimeType)
        assertEquals(
            listOf(DownloadSafetyFinding.DisguisedName, DownloadSafetyFinding.AndroidApp),
            disguised.findings,
        )
        val typedOnly = BrowserEngineDownloadRules.localDownload(
            BrowserEngineDownloadResponse(
                url = "blob:https://files.example/1",
                contentDisposition = null,
                mimeType = "application/vnd.android.package-archive",
            ),
        )
        assertEquals("download.apk", typedOnly.fileName)
        assertEquals(listOf(DownloadSafetyFinding.AndroidApp), typedOnly.findings)
    }

    @Test
    fun `an ordinary blob file passes the file check without a question`() {
        val image = BrowserEngineDownloadRules.localDownload(
            BrowserEngineDownloadResponse(
                url = "blob:https://files.example/2",
                contentDisposition = "attachment; filename=photo.png",
                mimeType = "image/png",
            ),
        )

        assertEquals("photo.png", image.fileName)
        assertEquals("image/png", image.mimeType)
        assertTrue(image.findings.isEmpty())
        assertNull(
            BrowserEngineDownloadRules.request(
                BrowserEngineDownloadResponse("blob:https://files.example/2", null, "image/png"),
                referrer = null,
            ),
        )
    }

    @Test
    fun `runtime permissions reject malformed and oversized requests`() {
        assertEquals(
            setOf("android.permission.CAMERA", "android.permission.RECORD_AUDIO"),
            BrowserEnginePermissionRules.runtimePermissions(
                arrayOf(
                    "android.permission.CAMERA",
                    " android.permission.RECORD_AUDIO ",
                    "android.permission.CAMERA",
                ),
            ),
        )
        assertNull(BrowserEnginePermissionRules.runtimePermissions(emptyArray()))
        assertNull(BrowserEnginePermissionRules.runtimePermissions(arrayOf("CAMERA")))
        assertNull(
            BrowserEnginePermissionRules.runtimePermissions(
                arrayOf("android.permission.CAMERA", "android.permission.READ_CONTACTS"),
            ),
        )
        assertNull(
            BrowserEnginePermissionRules.runtimePermissions(
                Array(9) { index -> "android.permission.CANDY_$index" },
            ),
        )
    }

    @Test
    fun `media permissions accept camera and microphone but reject screen capture`() {
        assertEquals(
            setOf(SitePermission.Camera, SitePermission.Microphone),
            BrowserEnginePermissionRules.requestedMediaPermissions(
                hasCamera = true,
                hasMicrophone = true,
                hasUnsupportedVideoSource = false,
                hasUnsupportedAudioSource = false,
            ),
        )
        assertNull(
            BrowserEnginePermissionRules.requestedMediaPermissions(
                hasCamera = false,
                hasMicrophone = false,
                hasUnsupportedVideoSource = true,
                hasUnsupportedAudioSource = false,
            ),
        )
    }

    @Test
    fun `web prompt sanitizes display text and bounds input`() {
        val prompt = BrowserWebPromptRules.sanitized(
            id = 4,
            tabId = "tab-a",
            kind = BrowserWebPromptKind.Text,
            title = " Site title ",
            message = "Enter value",
            defaultValue = "initial",
        )

        assertEquals("Site title", prompt?.title)
        assertEquals("initial", prompt?.defaultValue)
        assertNull(
            BrowserWebPromptRules.sanitized(
                id = 0,
                tabId = "tab-a",
                kind = BrowserWebPromptKind.Alert,
                title = null,
                message = null,
                defaultValue = null,
            ),
        )
        assertNull(
            BrowserWebPromptRules.sanitized(
                id = 5,
                tabId = "tab-a",
                kind = BrowserWebPromptKind.Text,
                title = "trusted\u202Espoofed",
                message = null,
                defaultValue = "x".repeat(BrowserWebPromptRules.MAX_INPUT_LENGTH + 1),
            )?.title,
        )
    }

    @Test
    fun `choice prompt keeps bounded valid choices`() {
        val prompt = BrowserWebPromptRules.sanitized(
            id = 6,
            tabId = "tab-a",
            kind = BrowserWebPromptKind.Choice,
            title = "Pick",
            message = null,
            defaultValue = null,
            choices = listOf(
                BrowserWebPromptChoice("one", "One", selected = true, disabled = false, separator = false),
                BrowserWebPromptChoice("", "Invalid", selected = false, disabled = false, separator = false),
            ),
            allowMultiple = true,
        )

        assertEquals(listOf("one"), prompt?.choices?.map(BrowserWebPromptChoice::id))
        assertTrue(requireNotNull(prompt).allowMultiple)
    }
}
