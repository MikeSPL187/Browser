package dev.sk2andy.materialbrowser.ui.v5

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import dev.sk2andy.materialbrowser.shared.ui.icons.VolaIcons
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Materials
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Spacing
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Theme
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Tile
import dev.sk2andy.materialbrowser.ui.theme.v5.VolaV5Theme

/*
 * V15a building blocks as canvas screens «д» (menu sheet) and «ж» (settings) will use them: groups of
 * rows with tiles, values, chevrons and toggles, and a sheet's top. Preview text is not localized.
 */

private const val PREVIEW_WIDTH_DP = 390
private const val PREVIEW_HEIGHT_DP = 844

@Composable
private fun SettingsSample() {
    Column(
        Modifier
            .fillMaxSize()
            .background(V5Theme.colors.background)
            .padding(vertical = V5Spacing.space4),
        verticalArrangement = Arrangement.spacedBy(V5Spacing.space6),
    ) {
        Text(
            "Настройки",
            style = V5Theme.type.largeTitle,
            color = V5Theme.colors.label,
            modifier = Modifier.padding(horizontal = V5Spacing.marginPhone),
        )
        V5ListGroup {
            V5ListRow("Профили", icon = V5RowIcon(VolaIcons.Workspaces, V5Tile.Indigo), value = "2", trailing = V5RowTrailing.Chevron, onClick = {})
            V5ListRow("Синхронизация", icon = V5RowIcon(VolaIcons.Sync, V5Tile.Sky), value = "Выкл", trailing = V5RowTrailing.Chevron, onClick = {})
        }
        V5ListGroup(header = "Вид") {
            V5ListRow("Поиск", icon = V5RowIcon(VolaIcons.Search, V5Tile.Blue), trailing = V5RowTrailing.Chevron, onClick = {})
            V5ListRow("Внешний вид", icon = V5RowIcon(VolaIcons.Palette, V5Tile.Purple), value = "Как в системе", trailing = V5RowTrailing.Chevron, onClick = {})
            V5ListRow(
                "Окрашивать панель в цвет сайта",
                icon = V5RowIcon(VolaIcons.Contrast, V5Tile.Pink),
                trailing = V5RowTrailing.Toggle(checked = true, onCheckedChange = {}),
            )
        }
        V5ListGroup(footer = "Защита по сайту меняется в меню страницы.") {
            V5ListRow("Защита", icon = V5RowIcon(VolaIcons.ShieldLock, V5Tile.Green), value = "Стандарт", trailing = V5RowTrailing.Chevron, onClick = {})
            V5ListRow(
                "Только HTTPS",
                subtitle = "Небезопасные страницы открываются после вопроса",
                icon = V5RowIcon(VolaIcons.Lock, V5Tile.Green),
                trailing = V5RowTrailing.Toggle(checked = false, onCheckedChange = {}, tint = V5ToggleTint.Success),
            )
            V5ListRow("Синхронизировать пароли", icon = V5RowIcon(VolaIcons.Key, V5Tile.Gray), enabled = false, trailing = V5RowTrailing.Toggle(true, {}))
        }
    }
}

@Composable
private fun MenuSample() {
    Column(
        Modifier
            .fillMaxSize()
            .background(V5Theme.colors.scrim),
        verticalArrangement = Arrangement.Bottom,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(V5Theme.shapes.sheet)
                .background(V5Materials.thick.tint(V5Theme.colors.isDark, blurAvailable = false, reduceTransparency = false))
                .padding(bottom = V5Spacing.space6),
            verticalArrangement = Arrangement.spacedBy(V5Spacing.space4),
        ) {
            V5SheetHeader(title = "Северное сияние в марте", subtitle = "polar-notes.ru", onClose = {})
            V5ListGroup {
                V5ListRow("Перевести страницу", icon = V5RowIcon(VolaIcons.Language), onClick = {})
                V5ListRow("Найти на странице", icon = V5RowIcon(VolaIcons.Search), onClick = {})
                V5ListRow("Скрыть отвлекающее", subtitle = "Коснитесь того, что мешает", icon = V5RowIcon(VolaIcons.VisibilityOff), onClick = {})
            }
            V5ListGroup {
                V5ListRow("Новая частная вкладка", icon = V5RowIcon(VolaIcons.Lock), onClick = {})
                V5ListRow("Настройки", icon = V5RowIcon(VolaIcons.Settings), trailing = V5RowTrailing.Chevron, onClick = {})
            }
        }
    }
}

@Composable
private fun ToggleSample() {
    Row(
        Modifier.background(V5Theme.colors.background).padding(V5Spacing.space4),
        horizontalArrangement = Arrangement.spacedBy(V5Spacing.space2),
    ) {
        V5Toggle(checked = false, onCheckedChange = {})
        V5Toggle(checked = true, onCheckedChange = {})
        V5Toggle(checked = true, onCheckedChange = {}, tint = V5ToggleTint.Success)
        V5Toggle(checked = true, onCheckedChange = {}, enabled = false)
    }
}

@Preview(name = "Settings list · light", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun V5SettingsListLightPreview() {
    VolaV5Theme(dark = false) { SettingsSample() }
}

@Preview(name = "Settings list · dark", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun V5SettingsListDarkPreview() {
    VolaV5Theme(dark = true) { SettingsSample() }
}

@Preview(name = "Menu sheet · light", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun V5MenuSheetLightPreview() {
    VolaV5Theme(dark = false) { MenuSample() }
}

@Preview(name = "Menu sheet · dark", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun V5MenuSheetDarkPreview() {
    VolaV5Theme(dark = true) { MenuSample() }
}

@Preview(name = "Toggles · light")
@Composable
private fun V5TogglesLightPreview() {
    VolaV5Theme(dark = false) { ToggleSample() }
}

@Preview(name = "Toggles · dark")
@Composable
private fun V5TogglesDarkPreview() {
    VolaV5Theme(dark = true) { ToggleSample() }
}

@Preview(name = "Settings list · light, largest font", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP, fontScale = 2f)
@Composable
private fun V5SettingsListLightPreviewLargeFont() {
    VolaV5Theme(dark = false) { SettingsSample() }
}
