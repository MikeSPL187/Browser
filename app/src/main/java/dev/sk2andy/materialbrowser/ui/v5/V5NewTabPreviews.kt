package dev.sk2andy.materialbrowser.ui.v5

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import dev.sk2andy.materialbrowser.shared.ui.WorkspaceIcons
import dev.sk2andy.materialbrowser.ui.theme.v5.VolaV5Theme

/* The v5 new tab as canvas screen «а», in light and dark, and the add sheet. Preview text is not localized. */

private const val PREVIEW_WIDTH_DP = 390
private const val PREVIEW_HEIGHT_DP = 844

private val PreviewState = V5NewTabState(
    profileLabel = "Личный",
    profileIcon = WorkspaceIcons.vector("🏔"),
    pinned = listOf(
        V5PinnedSite("wiki", "Википедия", null),
        V5PinnedSite("mail", "Почта", null),
        V5PinnedSite("maps", "Карты", null),
        V5PinnedSite("news", "Новости", null),
        V5PinnedSite("music", "Музыка", null),
        V5PinnedSite("docs", "Документы", null),
        V5PinnedSite("weather", "Погода", null),
    ),
    canAdd = true,
    report = V5NewTabReport(
        total = 1248,
        daily = listOf(46, 62, 38, 80, 100, 55, 70),
        weekdays = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс"),
    ),
    recent = listOf(
        V5RecentTab("t1", "Пирог с брусникой", "recipes.example", null),
        V5RecentTab("t2", "Домик у моря", "travel.example", null),
    ),
)

@Composable
private fun NewTabPreview(dark: Boolean) {
    VolaV5Theme(dark = dark) {
        V5NewTab(
            state = PreviewState,
            onOpenPinned = {},
            onRemovePinned = {},
            onAddPinned = {},
            onOpenReport = {},
            onHideReport = {},
            onOpenRecent = {},
        )
    }
}

@Preview(name = "New tab v5 · light", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun V5NewTabLightPreview() {
    NewTabPreview(dark = false)
}

@Preview(name = "New tab v5 · dark", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun V5NewTabDarkPreview() {
    NewTabPreview(dark = true)
}

@Preview(name = "Pin a site · dark", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun V5PinnedAddSheetPreview() {
    VolaV5Theme(dark = true) {
        V5PinnedAddSheet(
            visible = true,
            candidates = PreviewState.recent,
            onAdd = {},
            onDismiss = {},
        )
    }
}
