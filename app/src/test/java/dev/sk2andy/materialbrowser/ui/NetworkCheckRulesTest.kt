package dev.sk2andy.materialbrowser.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class NetworkCheckRulesTest {
    private val wifi = NetworkCheckRules.Seen(wifi = true, cellular = false, validated = true)
    private val cellularNoInternet = NetworkCheckRules.Seen(wifi = false, cellular = true, validated = false)

    @Test
    fun `each kind stands on its own networks`() {
        assertEquals(
            NetworkChecks(wifi = NetworkLink.Connected, cellular = NetworkLink.NoInternet),
            NetworkCheckRules.checks(listOf(wifi, cellularNoInternet)),
        )
        assertEquals(
            NetworkChecks(wifi = NetworkLink.NotConnected, cellular = NetworkLink.NotConnected),
            NetworkCheckRules.checks(emptyList()),
        )
    }

    @Test
    fun `one network with the internet is enough for its kind`() {
        val captive = NetworkCheckRules.Seen(wifi = true, cellular = false, validated = false)
        assertEquals(NetworkLink.NoInternet, NetworkCheckRules.link(listOf(captive), wifi = true))
        assertEquals(NetworkLink.Connected, NetworkCheckRules.link(listOf(captive, wifi), wifi = true))
    }
}
