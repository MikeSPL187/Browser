package dev.sk2andy.materialbrowser.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import dev.sk2andy.materialbrowser.shared.ui.settings.SettingsHomeEntry
import dev.sk2andy.materialbrowser.shared.ui.settings.SettingsHomeRules
import dev.sk2andy.materialbrowser.ui.theme.v5.VolaV5Theme

/* The v5 settings home as canvas screens «ж» and «Settings», light, dark and the largest font. Preview text is not localized. */

private const val PREVIEW_WIDTH_DP = 390
private const val PREVIEW_HEIGHT_DP = 1100

private val PreviewTitles = mapOf(
    SettingsDestination.Sync to ("Синхронизация" to "Выкл"),
    SettingsDestination.Search to ("Поиск" to "DuckDuckGo · подсказки"),
    SettingsDestination.TabsAndGestures to ("Вкладки и жесты" to "12 вкладок"),
    SettingsDestination.Appearance to ("Внешний вид" to "Как в системе"),
    SettingsDestination.Browser to ("Браузер" to "По умолчанию"),
    SettingsDestination.ProtectionAndData to ("Защита и данные" to "Трекеры блокируются · только HTTPS"),
    SettingsDestination.SiteCapsules to ("Капсулы сайтов" to "4 капсулы"),
    SettingsDestination.Passwords to ("Пароли" to null),
    SettingsDestination.Userscripts to ("Пользовательские скрипты" to "2 скрипта"),
    SettingsDestination.Downloads to ("Загрузки" to "Vola"),
    SettingsDestination.AboutLegal to ("О приложении и правовая информация" to null),
)

private fun previewEntries(): List<SettingsHomeEntry> =
    SettingsHomeRules.items(hasFirefoxExtensions = false).map { item ->
        val (title, summary) = PreviewTitles[item.destination] ?: ("" to null)
        SettingsHomeEntry(item = item, title = title, summary = summary)
    }

@Composable
private fun HomePreview(dark: Boolean) {
    VolaV5Theme(dark = dark) {
        SettingsHomeV5(
            entries = previewEntries(),
            isDefaultBrowser = true,
            onOpenDefaultBrowserSettings = {},
            onOpen = {},
            onSearch = {},
            onDismiss = {},
            developerOptionsUnlocked = false,
            onUnlockDeveloperOptions = {},
        )
    }
}

@Preview(name = "Settings home v5 · light", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun SettingsHomeV5LightPreview() {
    HomePreview(dark = false)
}

@Preview(name = "Settings home v5 · dark", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun SettingsHomeV5DarkPreview() {
    HomePreview(dark = true)
}

@Preview(name = "Settings home v5 · largest font", widthDp = PREVIEW_WIDTH_DP, heightDp = 2200, fontScale = 2f)
@Composable
private fun SettingsHomeV5LargeFontPreview() {
    HomePreview(dark = false)
}
