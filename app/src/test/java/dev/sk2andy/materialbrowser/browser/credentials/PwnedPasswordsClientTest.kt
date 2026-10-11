package dev.sk2andy.materialbrowser.browser.credentials

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PwnedPasswordsClientTest {
    @Test
    fun `the hash is upper-case SHA-1 hex, as Pwned Passwords lists it`() {
        assertEquals("5BAA61E4C9B93F3F0682250B6CF8331B7EE68FD8", PwnedPasswordsClient.sha1Hex("password"))
    }

    @Test
    fun `nothing is sent for anything but five upper-case hex characters`() {
        // Each is refused before a connection opens, so the test needs no network.
        assertNull(PwnedPasswordsClient.range("5BAA61"))
        assertNull(PwnedPasswordsClient.range("5baa6"))
        assertNull(PwnedPasswordsClient.range("5BAG6"))
        assertNull(PwnedPasswordsClient.range(""))
    }
}
