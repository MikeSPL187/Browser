package dev.sk2andy.materialbrowser.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.FloatState
import androidx.compose.runtime.getValue
import androidx.compose.ui.geometry.Rect
import dev.sk2andy.materialbrowser.browser.BrowserController
import dev.sk2andy.materialbrowser.browser.ProfileWallpaperTarget
import dev.sk2andy.materialbrowser.ui.theme.v5.rememberUiV5Flag

/**
 * The tab overview: under ui.v5 on a phone the v5 overview (V10a), otherwise the current one
 * ([TabOverviewV4]). The parameters are the current overview's; the v5 one uses the few it needs.
 * Wide windows keep the current overview until the tablet layout of v5.
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
    val uiV5 by rememberUiV5Flag()
    val wide = rememberWideBrowserWindow()
    if (uiV5 && !wide) {
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
        return
    }
    TabOverviewV4(
        controller = controller,
        backgroundWallpaper = backgroundWallpaper,
        visible = visible,
        bottomBarTopPx = bottomBarTopPx,
        onClose = onClose,
        onSelect = onSelect,
        onSelectDismissAnchor = onSelectDismissAnchor,
        onNewTab = onNewTab,
        onOpenSettings = onOpenSettings,
        onOpenSyncSettings = onOpenSyncSettings,
        onEditProfileWallpaper = onEditProfileWallpaper,
        onConfigureCreatedProfile = onConfigureCreatedProfile,
        destinationChromeVisible = destinationChromeVisible,
        onEntryHeroStarted = onEntryHeroStarted,
        onEntryHeroCompleted = onEntryHeroCompleted,
        onExitHeroVisibilityChanged = onExitHeroVisibilityChanged,
        candyTrailTabId = candyTrailTabId,
        candyTrailSourceBounds = candyTrailSourceBounds,
        candyTrailBackProgress = candyTrailBackProgress,
        candyTrailBackEdgeSign = candyTrailBackEdgeSign,
        candyTrailPredictiveBackCommitted = candyTrailPredictiveBackCommitted,
        onOpenCandyTrail = onOpenCandyTrail,
        onCloseCandyTrail = onCloseCandyTrail,
        onToggleFavoriteTab = onToggleFavoriteTab,
        onAddSiteCapsule = onAddSiteCapsule,
        onSnoozeTab = onSnoozeTab,
        onHeroPagerSnapshotObserved = onHeroPagerSnapshotObserved,
    )
}
