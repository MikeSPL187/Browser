package dev.sk2andy.materialbrowser.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.FloatState
import androidx.compose.ui.geometry.Rect
import dev.sk2andy.materialbrowser.browser.BrowserController
import dev.sk2andy.materialbrowser.browser.ProfileWallpaperTarget

/**
 * The tab overview: the v5 overview (V10a) on every width. The parameters are still the v4
 * overview's; the v5 one uses the few it needs, and the rest go with the v4 code.
 */
@Composable
internal fun TabOverview(
    controller: BrowserController,
    backgroundWallpaper: ProfileWallpaperRuntime? = null,
    visible: Boolean,
    bottomBarTopPx: FloatState,
    onClose: () -> Unit,
    onSelect: (String) -> Unit,
    onSelectDismissAnchor: (String) -> Unit = onSelect,
    onNewTab: () -> Unit,
    /** v5 only: a new tab straight in private (О-2, М-6). */
    onNewPrivateTab: () -> Unit = onNewTab,
    onOpenSettings: () -> Unit,
    onOpenSyncSettings: () -> Unit = onOpenSettings,
    onEditProfileWallpaper: (String, ProfileWallpaperTarget) -> Unit = { _, _ -> },
    onConfigureCreatedProfile: (String, ProfileCreationOptions) -> Unit = { _, _ -> },
    destinationChromeVisible: Boolean,
    onEntryHeroStarted: (Boolean) -> Unit,
    onEntryHeroCompleted: () -> Unit,
    onExitHeroVisibilityChanged: (Boolean) -> Unit,
    candyTrailTabId: String?,
    candyTrailSourceBounds: Rect?,
    candyTrailBackProgress: Float,
    candyTrailBackEdgeSign: Int,
    candyTrailPredictiveBackCommitted: Boolean,
    onOpenCandyTrail: (String, Rect?) -> Unit,
    onCloseCandyTrail: () -> Unit,
    onToggleFavoriteTab: (String) -> Unit,
    onAddSiteCapsule: (String) -> Unit,
    onSnoozeTab: (String) -> Unit,
    onHeroPagerSnapshotObserved: ((HeroPagerSnapshotObservation) -> Unit)? = null,
) {
    BrowserTabOverviewV5(
        controller = controller,
        visible = visible,
        onClose = onClose,
        onSelect = onSelect,
        onNewTab = onNewTab,
        onEntryHeroStarted = onEntryHeroStarted,
        onEntryHeroCompleted = onEntryHeroCompleted,
        onNewPrivateTab = onNewPrivateTab,
    )
}
