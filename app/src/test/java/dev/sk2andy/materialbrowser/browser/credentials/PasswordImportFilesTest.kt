package dev.sk2andy.materialbrowser.browser.credentials

import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import org.junit.Assert.assertEquals
import org.junit.Test

class PasswordImportFilesTest {
    @Test
    fun `a UTF-8 export is read with its name and size`() {
        val bytes = "url,username,password\nhttps://example.com,anna,пароль\n".toByteArray()

        val file = PasswordImportFiles.read(ByteArrayInputStream(bytes), "export.csv")

        assertEquals(
            PasswordImportFile.Loaded("url,username,password\nhttps://example.com,anna,пароль\n", "export.csv", bytes.size.toLong()),
            file,
        )
    }

    @Test
    fun `other encodings and broken streams are refused`() {
        val latin1 = byteArrayOf(0x75, 0x72, 0x6c, 0xE9.toByte(), 0x0a)
        assertEquals(PasswordImportFile.NotText, PasswordImportFiles.read(ByteArrayInputStream(latin1), null))

        val broken = object : InputStream() {
            override fun read(): Int = throw IOException("gone")
        }
        assertEquals(PasswordImportFile.Unreadable, PasswordImportFiles.read(broken, null))
    }
}
