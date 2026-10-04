package dev.sk2andy.materialbrowser.browser.downloads

import dev.sk2andy.materialbrowser.browser.downloads.DownloadSafetyFinding.AndroidApp
import dev.sk2andy.materialbrowser.browser.downloads.DownloadSafetyFinding.DesktopProgram
import dev.sk2andy.materialbrowser.browser.downloads.DownloadSafetyFinding.DisguisedName
import dev.sk2andy.materialbrowser.browser.downloads.DownloadSafetyFinding.InsecureSource
import dev.sk2andy.materialbrowser.browser.downloads.DownloadSafetyFinding.TypeMismatch
import org.junit.Assert.assertEquals
import org.junit.Test

class DownloadSafetyCheckTest {
    private fun check(url: String, name: String, mime: String) =
        DownloadSafetyCheck.findings(url = url, fileName = name, mimeType = mime)

    @Test
    fun `ordinary documents over HTTPS download without a question`() {
        assertEquals(emptyList<DownloadSafetyFinding>(), check(HTTPS, "report.pdf", "application/pdf"))
        assertEquals(emptyList<DownloadSafetyFinding>(), check(HTTPS, "photo.jpeg", "image/jpeg"))
        assertEquals(emptyList<DownloadSafetyFinding>(), check(HTTPS, "archive.tar.gz", "application/gzip"))
        assertEquals(emptyList<DownloadSafetyFinding>(), check(HTTPS, "app.min.js", "text/javascript"))
    }

    @Test
    fun `an Android app is named as one`() {
        assertEquals(listOf(AndroidApp), check(HTTPS, "ice-report.apk", APK))
        assertEquals(listOf(AndroidApp), check(HTTPS, "Game.XAPK", "application/octet-stream"))
    }

    @Test
    fun `a program behind a document extension is disguised`() {
        assertEquals(listOf(DisguisedName, AndroidApp), check(HTTPS, "invoice.pdf.apk", APK))
        assertEquals(
            listOf(DisguisedName, DesktopProgram),
            check(HTTPS, "photo.JPG.exe", "application/octet-stream"),
        )
    }

    @Test
    fun `a program sent under a harmless name does not match its type`() {
        assertEquals(listOf(TypeMismatch, AndroidApp), check(HTTPS, "photo.jpg", APK))
        assertEquals(
            listOf(TypeMismatch),
            check(HTTPS, "notes.txt", "application/x-msdownload"),
        )
    }

    @Test
    fun `a computer program is told apart from an Android app`() {
        assertEquals(listOf(DesktopProgram), check(HTTPS, "setup.exe", "application/x-msdownload"))
        assertEquals(listOf(DesktopProgram), check(HTTPS, "tool.jar", "application/java-archive"))
    }

    @Test
    fun `plain HTTP is a finding of its own and comes last`() {
        assertEquals(listOf(InsecureSource), check("http://example.com/a.pdf", "a.pdf", "application/pdf"))
        assertEquals(
            listOf(AndroidApp, InsecureSource),
            check("HTTP://example.com/a.apk", "a.apk", APK),
        )
    }

    private companion object {
        const val HTTPS = "https://files.example.net/f"
        const val APK = "application/vnd.android.package-archive"
    }
}
