package dev.sk2andy.materialbrowser.ui.v5

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import dev.sk2andy.materialbrowser.shared.ui.WorkspaceIcons
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Accent
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

/** V28 B6a: the field open over the new tab; with nothing typed the pinned sites show through. */
@Preview(name = "New tab v5 · empty editor, dark", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun V5NewTabEditorDarkPreview() {
    VolaV5Theme(dark = true) {
        Box {
            NewTabPreview(dark = true)
            V5AddressEditor(
                state = V5EditorState(
                    value = TextFieldValue(""),
                    layout = V5SuggestionLayout.Empty,
                    overStart = true,
                ),
                onValueChange = {},
                onSubmit = {},
                onSelect = {},
                onFill = {},
                onClipboard = {},
                onCancel = {},
            )
        }
    }
}

/** Canvas screen «EmptyStart»: the new tab of a profile with nothing of its own yet. */
private val FreshState = V5NewTabState(
    profileLabel = "Учёба",
    profileIcon = WorkspaceIcons.vector("📚"),
    pinned = emptyList(),
    canAdd = true,
    report = null,
    recent = emptyList(),
    fresh = V5NewTabFresh(profileName = "Учёба", copyFrom = "Личный", canMoveTabs = true),
)

@Composable
private fun FreshPreview(dark: Boolean, accent: V5Accent = V5Accent.Orange) {
    VolaV5Theme(dark = dark, accent = accent) {
        V5NewTab(
            state = FreshState,
            onOpenPinned = {},
            onRemovePinned = {},
            onAddPinned = {},
            onOpenReport = {},
            onHideReport = {},
            onOpenRecent = {},
        )
    }
}

@Preview(name = "Fresh profile · light", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun V5NewTabFreshLightPreview() {
    FreshPreview(dark = false)
}

@Preview(name = "Fresh profile · dark", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun V5NewTabFreshDarkPreview() {
    FreshPreview(dark = true)
}

@Preview(name = "Fresh profile · green, dark", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun V5NewTabFreshGreenPreview() {
    FreshPreview(dark = true, accent = V5Accent.Green)
}

@Preview(name = "Fresh profile · largest font", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP, fontScale = 2f)
@Composable
private fun V5NewTabFreshLargeFontPreview() {
    FreshPreview(dark = false)
}

@Preview(name = "Move tabs here · dark", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun V5MoveTabsSheetPreview() {
    VolaV5Theme(dark = true, accent = V5Accent.Orange) {
        V5MoveTabsSheet(
            visible = true,
            groups = listOf(V5MovableTabs("Личный", PreviewState.recent)),
            onMove = {},
            onDismiss = {},
        )
    }
}
