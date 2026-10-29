package dev.sk2andy.materialbrowser.ui

import dev.sk2andy.materialbrowser.shared.browser.BrowserFeatureMenuState
/** A page with reader, translation and the desktop site on, as on board W-Menu. */
private val previewMenuState = BrowserFeatureMenuState(
    canGoBack = true,
    hasPage = true,
    canToggleFavorite = true,
    canOpenReader = true,
    canTranslatePage = true,
    canUseDocumentActions = true,
    canToggleDesktopView = true,
    isDesktopView = true,
    canAddSiteCapsule = true,
    canSnooze = true,
)

