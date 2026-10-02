package dev.sk2andy.materialbrowser.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.sk2andy.materialbrowser.data.AppearanceSettings
import dev.sk2andy.materialbrowser.data.BrowserAppearanceMode
import dev.sk2andy.materialbrowser.data.ProtectionReportRules
import dev.sk2andy.materialbrowser.data.ProtectionDay
import dev.sk2andy.materialbrowser.data.ProtectionWeek
import dev.sk2andy.materialbrowser.ui.theme.MaterialBrowserTheme
import dev.sk2andy.materialbrowser.ui.theme.VolaPreviews
import dev.sk2andy.materialbrowser.ui.theme.VolaSpacing
import dev.sk2andy.materialbrowser.ui.theme.VolaTheme
import dev.sk2andy.materialbrowser.ui.theme.auraBrush

private const val PREVIEW_TODAY = 20_361L

private val previewWeek: ProtectionWeek = ProtectionReportRules.week(
    days = listOf(38, 62, 30, 22, 80, 55, 70).mapIndexed { index, scale ->
        ProtectionDay(
            epochDay = PREVIEW_TODAY - 6 + index,
            blockedBySite = mapOf(
                "north-guide.ru" to scale * 3,
                "lenta.example.ru" to scale * 2,
                "weather.example.com" to scale,
                "recipes.example.org" to scale / 2,
            ),
        )
    },
    today = PREVIEW_TODAY,
)

@Composable
private fun ProtectionPreviewFrame(content: @Composable () -> Unit) {
    MaterialBrowserTheme(settings = AppearanceSettings(appearanceMode = BrowserAppearanceMode.System)) {
        Column(
            modifier = Modifier
                .background(VolaTheme.auraBrush)
                .padding(VolaSpacing.x4),
            verticalArrangement = Arrangement.spacedBy(VolaSpacing.x3),
        ) {
            content()
        }
    }
}

@VolaPreviews
@Composable
private fun NewTabProtectionCardPreview() {
    ProtectionPreviewFrame {
        NewTabProtectionCard(week = previewWeek, enabled = true, onOpen = {})
        NewTabProtectionCard(week = ProtectionWeek.empty(PREVIEW_TODAY), enabled = true, onOpen = {})
    }
}

@VolaPreviews
@Composable
private fun ProtectionReportContentPreview() {
    ProtectionPreviewFrame {
        ProtectionReportContent(week = previewWeek, onClearRequest = {}, onHideCard = {})
    }
}
