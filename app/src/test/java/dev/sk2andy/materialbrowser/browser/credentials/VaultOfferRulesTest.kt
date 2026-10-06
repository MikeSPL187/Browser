package dev.sk2andy.materialbrowser.browser.credentials

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VaultOfferRulesTest {
    private fun offer(
        vaultExists: Boolean = false,
        offeredBefore: Int = 0,
        accepted: Boolean = false,
        offeredThisRun: Boolean = false,
        sheetShowing: Boolean = false,
    ) = VaultOfferRules.shouldOffer(vaultExists, offeredBefore, accepted, offeredThisRun, sheetShowing)

    @Test
    fun `the first sign-in field without a vault gets the offer`() {
        assertTrue(offer())
    }

    @Test
    fun `a vault or a taken offer ends it`() {
        assertFalse(offer(vaultExists = true))
        assertFalse(offer(accepted = true))
    }

    @Test
    fun `once a run and at most three times`() {
        assertFalse(offer(offeredThisRun = true))
        assertTrue(offer(offeredBefore = VaultOfferRules.MAX_OFFERS - 1))
        assertFalse(offer(offeredBefore = VaultOfferRules.MAX_OFFERS))
    }

    @Test
    fun `never over another password sheet`() {
        assertFalse(offer(sheetShowing = true))
    }
}
