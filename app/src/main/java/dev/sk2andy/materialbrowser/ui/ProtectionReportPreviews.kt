package dev.sk2andy.materialbrowser.ui

import dev.sk2andy.materialbrowser.data.ProtectionReportRules
import dev.sk2andy.materialbrowser.data.ProtectionDay
import dev.sk2andy.materialbrowser.data.ProtectionWeek
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

