package dev.sk2andy.materialbrowser.ui.v5

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Theme
import dev.sk2andy.materialbrowser.ui.theme.v5.VolaV5Theme

/* The v5 page menu as canvas screen «д»: a page in light and dark, and a new tab. */

private const val PREVIEW_WIDTH_DP = 390
private const val PREVIEW_HEIGHT_DP = 844

private val PageState = V5PageMenuState(
    title = "Северное сияние в марте",
    subtitle = "polar-notes.ru",
    favicon = null,
    hasPage = true,
    canGoForward = true,
    canFavorite = true,
    isFavorite = true,
    canRead = true,
    canTranslate = true,
    isPinned = false,
    desktopView = false,
    siteMuted = null,
    canCloseTab = true,
)

private val NoActions = V5PageMenuActions(
    onForward = {}, onToggleFavorite = {}, onShare = {}, onPrint = {}, onRead = {}, onTranslate = {},
    onFindInPage = {}, onTogglePinned = {}, onDesktopViewChange = {}, onSiteMutedChange = {},
    onNewTab = {}, onNewPrivateTab = {}, onCloseTab = {}, onFavorites = {}, onHistory = {},
    onDownloads = {}, onSettings = {}, onDuplicateTab = {}, onOpenInApp = {},
)

@Composable
private fun MenuPreview(state: V5PageMenuState, page: V5PageMenuPage = V5PageMenuPage.Main) {
    Box(Modifier.fillMaxSize().background(V5Theme.colors.background)) {
        V5PageMenu(visible = true, state = state, actions = NoActions, onDismiss = {}, initialPage = page)
    }
}

/** PM-05: the rarer actions on «More». */
@Preview(name = "Page menu · more, light", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun V5PageMenuMoreLightPreview() {
    VolaV5Theme(dark = false) { MenuPreview(PageState, V5PageMenuPage.More) }
}

@Preview(name = "Page menu · more, dark", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun V5PageMenuMoreDarkPreview() {
    VolaV5Theme(dark = true) { MenuPreview(PageState, V5PageMenuPage.More) }
}

@Preview(name = "Page menu · light", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun V5PageMenuLightPreview() {
    VolaV5Theme(dark = false) { MenuPreview(PageState) }
}

@Preview(name = "Page menu · dark", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun V5PageMenuDarkPreview() {
    VolaV5Theme(dark = true) { MenuPreview(PageState) }
}

/** V28 B4: a page with an extension's action and a user script's command. */
@Preview(name = "Page menu · extensions", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun V5PageMenuExtrasPreview() {
    VolaV5Theme(dark = true) {
        MenuPreview(
            PageState.copy(
                extras = listOf(
                    V5PageMenuExtra("ext", "Dark Reader", subtitle = null, enabled = true, onClick = {}),
                    V5PageMenuExtra("script", "Show comments", subtitle = "Forum tweaks", enabled = true, onClick = {}),
                ),
            ),
        )
    }
}

@Preview(name = "Page menu · new tab", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun V5PageMenuNewTabPreview() {
    VolaV5Theme(dark = false) {
        MenuPreview(
            PageState.copy(
                title = "Новая вкладка",
                subtitle = null,
                hasPage = false,
                canGoForward = false,
                canFavorite = false,
                isFavorite = false,
                canRead = false,
                canTranslate = false,
                desktopView = null,
            ),
        )
    }
}

/** A profile of another device: no private tab there, so no row for it. */
@Preview(name = "Page menu · no private tab · light", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun V5PageMenuNoPrivateLightPreview() {
    VolaV5Theme(dark = false) { MenuPreview(PageState.copy(canNewPrivateTab = false)) }
}

@Preview(name = "Page menu · no private tab · dark", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun V5PageMenuNoPrivateDarkPreview() {
    VolaV5Theme(dark = true) { MenuPreview(PageState.copy(canNewPrivateTab = false)) }
}

/** A private tab: «Favorite» is off and says why (PM-04). */
@Preview(name = "Page menu · private tab · dark", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun V5PageMenuPrivateDarkPreview() {
    VolaV5Theme(dark = true) {
        MenuPreview(PageState.copy(canFavorite = false, favoriteOffInPrivate = true, isFavorite = false))
    }
}

/** While a file downloads: the row on top, and no second «Downloads» below (PM-02). */
@Preview(name = "Page menu · downloading · light", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun V5PageMenuDownloadingLightPreview() {
    VolaV5Theme(dark = false) {
        MenuPreview(PageState.copy(downloads = V5DownloadRingState.Progress(count = 1, fraction = 0.4f)))
    }
}
