package dev.sk2andy.materialbrowser.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.sk2andy.materialbrowser.data.AppearanceSettings
import dev.sk2andy.materialbrowser.data.BrowserAppearanceMode
import dev.sk2andy.materialbrowser.shared.browser.BrowserFeatureMenuRules
import dev.sk2andy.materialbrowser.shared.browser.BrowserFeatureMenuState
import dev.sk2andy.materialbrowser.shared.ui.BrowserMainMenuHandle
import dev.sk2andy.materialbrowser.shared.ui.BrowserMainMenuTileGrid
import dev.sk2andy.materialbrowser.shared.ui.BrowserMainMenuTileRules
import dev.sk2andy.materialbrowser.ui.theme.MaterialBrowserTheme
import dev.sk2andy.materialbrowser.ui.theme.VolaMenu
import dev.sk2andy.materialbrowser.ui.theme.VolaPreviews

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
    canToggleAlwaysBlockPopups = true,
    canAddSiteCapsule = true,
    canSnooze = true,
)

@VolaPreviews
@Composable
private fun BrowserMainMenuTilesPreview() {
    MaterialBrowserTheme(
        settings = AppearanceSettings(appearanceMode = BrowserAppearanceMode.System),
    ) {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .padding(VolaMenu.contentPadding),
        ) {
            BrowserMainMenuHandle(VolaMenu.tiles)
            BrowserMainMenuTileGrid(
                items = BrowserFeatureMenuRules.items(previewMenuState)
                    .filter(BrowserMainMenuTileRules::isTile),
                tiles = VolaMenu.tiles,
                resources = AndroidBrowserMainMenuResources,
                effects = rememberAndroidBrowserMainMenuEffects(backdropSource = null),
                onCommand = {},
                onToggle = {},
            )
        }
    }
}
