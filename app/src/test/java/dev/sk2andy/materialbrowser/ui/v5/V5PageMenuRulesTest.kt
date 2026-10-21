package dev.sk2andy.materialbrowser.ui.v5

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class V5PageMenuRulesTest {
    @Test
    fun `a page in an ordinary tab can be a favorite`() {
        assertTrue(V5PageMenuRules.canFavorite(blank = false, incognito = false))
        assertFalse(V5PageMenuRules.favoriteOffInPrivate(blank = false, incognito = false))
    }

    @Test
    fun `a private tab keeps nothing, and its label says why`() {
        assertFalse(V5PageMenuRules.canFavorite(blank = false, incognito = true))
        assertTrue(V5PageMenuRules.favoriteOffInPrivate(blank = false, incognito = true))
    }

    @Test
    fun `a new tab has nothing to favorite and no private note`() {
        assertFalse(V5PageMenuRules.canFavorite(blank = true, incognito = false))
        assertFalse(V5PageMenuRules.favoriteOffInPrivate(blank = true, incognito = true))
    }

    @Test
    fun `hiding page blocks needs a page and an engine that can`() {
        assertTrue(V5PageMenuRules.canHideDistractions(blank = false, engineCanHide = true))
        assertFalse(V5PageMenuRules.canHideDistractions(blank = false, engineCanHide = false))
        assertFalse(V5PageMenuRules.canHideDistractions(blank = true, engineCanHide = true))
    }

    @Test
    fun `the hidden count is read only where hiding applies`() {
        var reads = 0
        assertEquals(0, V5PageMenuRules.hiddenCount(canHideDistractions = false) { reads++; 3 })
        assertEquals(0, reads)
        assertEquals(3, V5PageMenuRules.hiddenCount(canHideDistractions = true) { 3 })
        assertEquals(0, V5PageMenuRules.hiddenCount(canHideDistractions = true) { -1 })
    }

    @Test
    fun `on the phone, translate follows the setting and the page language`() {
        assertTrue(translate(onDevice = true, offerOn = true, offersPageLanguage = true))
        assertFalse(translate(onDevice = true, offerOn = false, offersPageLanguage = true))
        assertFalse(translate(onDevice = true, offerOn = true, offersPageLanguage = false))
    }

    @Test
    fun `through the web provider, translate follows what the provider takes`() {
        assertTrue(translate(onDevice = false, providerCanTranslate = true))
        assertFalse(translate(onDevice = false, providerCanTranslate = false))
        // The phone's setting is not the provider's.
        assertTrue(translate(onDevice = false, offerOn = false, providerCanTranslate = true))
    }

    @Test
    fun `a new tab never offers translation`() {
        assertFalse(translate(blank = true, onDevice = true, offerOn = true, offersPageLanguage = true))
        assertFalse(translate(blank = true, onDevice = false, providerCanTranslate = true))
    }

    private fun translate(
        blank: Boolean = false,
        onDevice: Boolean,
        offerOn: Boolean = true,
        offersPageLanguage: Boolean = true,
        providerCanTranslate: Boolean = false,
    ) = V5PageMenuRules.canTranslate(blank, onDevice, offerOn, offersPageLanguage, providerCanTranslate)
}
