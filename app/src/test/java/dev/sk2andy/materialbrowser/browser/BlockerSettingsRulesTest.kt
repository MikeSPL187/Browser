package dev.sk2andy.materialbrowser.browser

import dev.sk2andy.materialbrowser.blocking.BlockerSettings
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BlockerSettingsRulesTest {
    private val current = BlockerSettings()

    @Test
    fun `ad switch alone refreshes pages whose session policy filters ads`() {
        val next = current.copy(blockAdsAndTrackers = false)

        assertTrue(BlockerSettingsRules.refreshesSessionPolicy(current, next, sessionPolicyFiltersAds = true))
        assertFalse(BlockerSettingsRules.refreshesSessionPolicy(current, next, sessionPolicyFiltersAds = false))
    }

    @Test
    fun `cookie and consent switches refresh pages in every engine`() {
        for (next in listOf(
            current.copy(blockThirdPartyCookies = false),
            current.copy(hideCookieConsent = false),
        )) {
            assertTrue(BlockerSettingsRules.refreshesSessionPolicy(current, next, sessionPolicyFiltersAds = false))
        }
        assertFalse(BlockerSettingsRules.refreshesSessionPolicy(current, current, sessionPolicyFiltersAds = true))
    }
}
