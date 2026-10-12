package dev.sk2andy.materialbrowser.data

import dev.sk2andy.materialbrowser.browser.DEFAULT_PROFILE_ID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FavoriteProfileKeysTest {
    @Test
    fun theListSavedBeforeProfilesStaysWithItsOwner() {
        assertEquals("favorites", FavoriteProfileKeys.key("work", legacyOwner = "work"))
        assertEquals("favorites:home", FavoriteProfileKeys.key("home", legacyOwner = "work"))
        assertNotEquals(
            FavoriteProfileKeys.key("a", legacyOwner = DEFAULT_PROFILE_ID),
            FavoriteProfileKeys.key("b", legacyOwner = DEFAULT_PROFILE_ID),
        )
    }

    @Test
    fun theDefaultProfileOwnsTheOldListOrElseTheFirstOne() {
        assertEquals(DEFAULT_PROFILE_ID, FavoriteProfileKeys.legacyOwner(listOf("work", DEFAULT_PROFILE_ID)))
        assertEquals("work", FavoriteProfileKeys.legacyOwner(listOf("work", "home")))
        assertEquals(DEFAULT_PROFILE_ID, FavoriteProfileKeys.legacyOwner(emptyList()))
    }

    @Test
    fun onlyFavoritesKeysCountAsFavorites() {
        assertTrue(FavoriteProfileKeys.isFavoritesKey("favorites"))
        assertTrue(FavoriteProfileKeys.isFavoritesKey("favorites:home"))
        assertFalse(FavoriteProfileKeys.isFavoritesKey("favorites_legacy_owner"))
        assertFalse(FavoriteProfileKeys.isFavoritesKey("history"))
    }
}
