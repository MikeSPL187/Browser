package dev.sk2andy.materialbrowser.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DownloadIndexRulesTest {
    @Test
    fun `the media index of a download manager file is the same file`() {
        val media = MediaStoreFileLocation(mediaId = 43, relativePath = "Download/", displayName = "report-1.pdf")

        assertTrue(DownloadIndexRules.isSameFile("file:///storage/emulated/0/Download/report-1.pdf", media))
        assertTrue(DownloadIndexRules.isSameFile("content://media/external/downloads/43", media))
    }

    @Test
    fun `a renamed download manager file is matched by the name it was saved under`() {
        val title = MediaStoreFileLocation(mediaId = 43, relativePath = "Download/", displayName = "report.pdf")

        assertFalse(DownloadIndexRules.isSameFile("file:///storage/emulated/0/Download/report-1.pdf", title))
    }

    @Test
    fun `files with the same name in different folders stay apart`() {
        val other = MediaStoreFileLocation(mediaId = 43, relativePath = "Download/B/", displayName = "report.pdf")

        assertFalse(DownloadIndexRules.isSameFile("file:///storage/emulated/0/Download/A/report.pdf", other))
        assertFalse(DownloadIndexRules.isSameFile("file:///storage/emulated/0/Download/report.pdf", other))
        assertFalse(DownloadIndexRules.isSameFile("content://media/external/downloads/44", other))
    }

    @Test
    fun `nested folders and encoded names match on the decoded path`() {
        val media = MediaStoreFileLocation(mediaId = 7, relativePath = "Download/Vola", displayName = "my notes+1.pdf")

        assertTrue(
            DownloadIndexRules.isSameFile("file:///storage/emulated/0/Download/Vola/my%20notes%2B1.pdf", media),
        )
    }

    @Test
    fun `rows whose location cannot be read are kept apart`() {
        val media = MediaStoreFileLocation(mediaId = 7, relativePath = "Download/", displayName = "a.pdf")

        assertFalse(DownloadIndexRules.isSameFile(null, media))
        assertFalse(DownloadIndexRules.isSameFile("", media))
        assertFalse(DownloadIndexRules.isSameFile("not a uri with spaces", media))
        assertFalse(DownloadIndexRules.isSameFile("https://example.com/Download/a.pdf", media))
        assertFalse(DownloadIndexRules.isSameFile("content://downloads/all_downloads/7", media))
        assertFalse(
            DownloadIndexRules.isSameFile("file:///storage/emulated/0/Download/a.pdf", media.copy(relativePath = null)),
        )
        assertFalse(
            DownloadIndexRules.isSameFile("file:///storage/emulated/0/Download/a.pdf", media.copy(displayName = "")),
        )
    }
}
