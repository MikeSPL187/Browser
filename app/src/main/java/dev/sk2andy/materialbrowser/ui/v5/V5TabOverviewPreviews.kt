package dev.sk2andy.materialbrowser.ui.v5

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import dev.sk2andy.materialbrowser.shared.ui.WorkspaceIcons
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Accent
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Tile
import dev.sk2andy.materialbrowser.ui.theme.v5.VolaV5Theme

/* The v5 tab overview as canvas screen «d»: tabs in light and dark, the private segment, no match. */

private const val PREVIEW_WIDTH_DP = 390
private const val PREVIEW_HEIGHT_DP = 844

private val PreviewCards = listOf(
    "Полярные заметки — сияние в марте",
    "Поезд до Мурманска",
    "Пирог с брусникой",
    "Kotlin coroutines guide",
    "Погода на неделю",
).mapIndexed { index, title ->
    // The second card is a pinned tab: no close button.
    V5TabCard(id = "t$index", title = title, selected = index == 0, preview = null, favicon = null, closable = index != 1)
}

@Composable
private fun OverviewPreview(
    cards: List<V5TabCard> = PreviewCards,
    privateSegment: Boolean = false,
    query: String = "",
    groups: List<V5OverviewGroup> = emptyList(),
) {
    V5TabOverview(
        state = V5OverviewState(
            cards = cards,
            privateSegment = privateSegment,
            counts = V5OverviewCounts(regular = 10, private = 2),
            query = query,
            profileLabel = "Личный",
            profileIcon = WorkspaceIcons.vector("🏔"),
            groups = groups,
        ),
        onQueryChange = {},
        onSegmentChange = {},
        onSelect = {},
        onClose = {},
        onNewTab = {},
        onDone = {},
        onCloseAll = {},
    )
}

@Preview(name = "Overview · light", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun V5OverviewLightPreview() {
    VolaV5Theme(dark = false) { OverviewPreview() }
}

@Preview(name = "Overview · dark", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun V5OverviewDarkPreview() {
    VolaV5Theme(dark = true) { OverviewPreview() }
}

@Preview(name = "Overview · private", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun V5OverviewPrivatePreview() {
    VolaV5Theme(privateBrowsing = true) { OverviewPreview(cards = PreviewCards.take(2), privateSegment = true) }
}

@Preview(name = "Overview · no match", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun V5OverviewEmptyPreview() {
    VolaV5Theme(dark = false) { OverviewPreview(cards = emptyList(), query = "банк") }
}

private val PreviewGroups = listOf(
    V5OverviewGroup("dinner", "Ужин", listOf("t2", "t4"), collapsed = false, tile = V5Tile.Red),
    V5OverviewGroup("trip", "Поездка", listOf("t0", "t1"), collapsed = true, tile = V5Tile.Blue),
)

@Preview(name = "Overview · groups", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun V5OverviewGroupsPreview() {
    VolaV5Theme(dark = false) { OverviewPreview(groups = PreviewGroups) }
}

@Preview(name = "Overview · groups dark", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun V5OverviewGroupsDarkPreview() {
    VolaV5Theme(dark = true) { OverviewPreview(groups = PreviewGroups) }
}

/** The group's frame keeps its own color whatever the profile's accent (canvas «OverviewV2»). */
@Preview(name = "Overview · groups, orange accent, dark", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun V5OverviewGroupsAccentPreview() {
    VolaV5Theme(dark = true, accent = V5Accent.Orange) { OverviewPreview(groups = PreviewGroups) }
}

@Preview(name = "Overview · groups, largest font", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP, fontScale = 2f)
@Composable
private fun V5OverviewGroupsLargeFontPreview() {
    VolaV5Theme(dark = false) { OverviewPreview(groups = PreviewGroups) }
}
