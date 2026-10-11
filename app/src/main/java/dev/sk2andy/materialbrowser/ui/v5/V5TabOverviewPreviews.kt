package dev.sk2andy.materialbrowser.ui.v5

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
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
) {
    V5TabOverview(
        state = V5OverviewState(
            cards = cards,
            privateSegment = privateSegment,
            counts = V5OverviewCounts(regular = 10, private = 2),
            query = query,
            profileLabel = "🏔 Личный",
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
