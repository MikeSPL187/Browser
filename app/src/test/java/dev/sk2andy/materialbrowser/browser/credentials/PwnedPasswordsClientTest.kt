package dev.sk2andy.materialbrowser.browser.credentials

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PwnedPasswordsClientTest {
    @Test
    fun `only the first five characters of the hash are asked for, once per prefix`() {
        val asked = mutableListOf<String>()
        val client = PwnedPasswordsClient { prefix ->
            asked += prefix
            "1E4C9B93F3F0682250B6CF8331B7EE68FD8:3861493\r\n0018A45C4D1DEF81644B54AB7F969B88D65:0"
        }

        assertEquals("5BAA61E4C9B93F3F0682250B6CF8331B7EE68FD8", PwnedPasswordsClient.sha1Hex("password"))
        assertEquals(3_861_493, client.timesSeen("password"))
        assertEquals(3_861_493, client.timesSeen("password"))
        assertEquals(listOf("5BAA6"), asked)
    }

    @Test
    fun `a failed request is no answer, not a clean password`() {
        assertNull(PwnedPasswordsClient { null }.timesSeen("password"))
    }
}
