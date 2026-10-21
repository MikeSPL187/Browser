package dev.sk2andy.materialbrowser.ui.v5

/**
 * Which rows of the page menu a tab gets (М-8), kept out of the Composable so each condition has
 * a JVM test. A new tab ([blank]) has no page rows at all; a private tab keeps nothing, so
 * «Favorite» is off there and says why.
 */
internal object V5PageMenuRules {
    fun canFavorite(blank: Boolean, incognito: Boolean): Boolean = !blank && !incognito

    fun favoriteOffInPrivate(blank: Boolean, incognito: Boolean): Boolean = !blank && incognito

    /** Only an engine that can choose page blocks (GeckoView) offers hiding them. */
    fun canHideDistractions(blank: Boolean, engineCanHide: Boolean): Boolean = !blank && engineCanHide

    /** «Show hidden items» needs blocks hidden on this site; [count] is read only when it can apply. */
    fun hiddenCount(canHideDistractions: Boolean, count: () -> Int): Int =
        if (canHideDistractions) count().coerceAtLeast(0) else 0

    /**
     * «Translate»: on the phone (GeckoView) when the offer is on in Settings → Translation and the
     * page's language is not one the person never translates; elsewhere when the web provider can
     * take the address.
     */
    fun canTranslate(
        blank: Boolean,
        onDevice: Boolean,
        offerOn: Boolean,
        offersPageLanguage: Boolean,
        providerCanTranslate: Boolean,
    ): Boolean = when {
        blank -> false
        onDevice -> offerOn && offersPageLanguage
        else -> providerCanTranslate
    }
}
