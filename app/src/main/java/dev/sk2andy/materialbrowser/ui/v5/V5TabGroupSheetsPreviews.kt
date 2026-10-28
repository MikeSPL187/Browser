package dev.sk2andy.materialbrowser.ui.v5

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Accent
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Tile
import dev.sk2andy.materialbrowser.ui.theme.v5.VolaV5Theme

/* The V12b tab group sheets, in light and dark. Preview text is not localized. */

private const val PREVIEW_WIDTH_DP = 390
private const val PREVIEW_HEIGHT_DP = 640

private val PreviewGroups = listOf(
    V5OverviewGroup(id = "trip", name = "Поездка", tabIds = listOf("a", "b"), collapsed = false, tile = V5Tile.Purple),
    V5OverviewGroup(id = "work", name = "Работа", tabIds = listOf("c"), collapsed = true, tile = V5Tile.Blue),
    V5OverviewGroup(id = "recipes", name = "Рецепты", tabIds = listOf("d"), collapsed = false, tile = V5Tile.Green),
)

/** The canvas palette (TabStackColor.palette). */
private val PreviewColors = listOf(
    V5Tile.Orange,
    V5Tile.Teal,
    V5Tile.Purple,
    V5Tile.Blue,
    V5Tile.Pink,
    V5Tile.Green,
    V5Tile.Gray,
)

@Composable
private fun MovePreview(dark: Boolean, groupLimit: Int? = null) {
    VolaV5Theme(dark = dark) {
        V5TabGroupMoveSheet(
            visible = true,
            tabTitle = "Билеты на поезд — РЖД",
            groups = PreviewGroups,
            currentGroupId = "trip",
            onMove = {},
            onNewGroup = {},
            onRemoveFromGroup = {},
            onDismiss = {},
            groupLimit = groupLimit,
        )
    }
}

@Preview(name = "Move to group, twelve groups · light", heightDp = 640)
@Composable
private fun V5TabGroupMoveLimitLightPreview() = MovePreview(dark = false, groupLimit = 12)

@Preview(name = "Move to group, twelve groups · dark", heightDp = 640)
@Composable
private fun V5TabGroupMoveLimitDarkPreview() = MovePreview(dark = true, groupLimit = 12)

private val PreviewTabs = listOf(
    V5GroupEditorTab("a", "Тыквенный суп с имбирём за 30 минут"),
    V5GroupEditorTab("b", "Винная карта: что к супу"),
    V5GroupEditorTab("c", "Список покупок на пятницу"),
)

@Composable
private fun EditPreview(dark: Boolean, accent: V5Accent = V5Accent.Default) {
    VolaV5Theme(dark = dark, accent = accent) {
        V5TabGroupEditSheet(
            visible = true,
            title = "Изменить группу",
            initialName = "Ужин в пятницу",
            initialColorIndex = 0,
            colors = PreviewColors,
            onSave = {},
            onDismiss = {},
            onUngroup = {},
            tabs = PreviewTabs,
            candidates = listOf(V5GroupEditorTab("d", "Рецепты дома")),
            onCloseGroup = {},
        )
    }
}

@Preview(name = "Move to group · light", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun V5TabGroupMoveLightPreview() {
    MovePreview(dark = false)
}

@Preview(name = "Move to group · dark", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun V5TabGroupMoveDarkPreview() {
    MovePreview(dark = true)
}

@Preview(name = "Edit group · light", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun V5TabGroupEditLightPreview() {
    EditPreview(dark = false)
}

@Preview(name = "Edit group · dark", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun V5TabGroupEditDarkPreview() {
    EditPreview(dark = true)
}

@Preview(name = "Edit group · orange accent, light", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun V5TabGroupEditAccentPreview() {
    EditPreview(dark = false, accent = V5Accent.Orange)
}

@Preview(name = "Edit group · largest font", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP, fontScale = 2f)
@Composable
private fun V5TabGroupEditLargeFontPreview() {
    EditPreview(dark = false)
}
