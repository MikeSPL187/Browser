package dev.sk2andy.materialbrowser.browser

import org.junit.Assert.assertEquals
import org.junit.Test

class HttpsOnlyModeTest {
    @Test
    fun `HTTPS-only is on in every tab by default`() {
        assertEquals(HttpsOnlyMode.Always, HttpsOnlyMode.Default)
        assertEquals(HttpsOnlyMode.Always, HttpsOnlyMode.fromStableId(null))
    }

    @Test
    fun `stable ids round trip and unknown values keep the safe default`() {
        HttpsOnlyMode.entries.forEach { mode ->
            assertEquals(mode, HttpsOnlyMode.fromStableId(mode.stableId))
        }
        assertEquals(HttpsOnlyMode.Always, HttpsOnlyMode.fromStableId("unknown"))
    }
}
