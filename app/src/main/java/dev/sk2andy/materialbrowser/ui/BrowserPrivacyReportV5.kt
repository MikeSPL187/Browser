package dev.sk2andy.materialbrowser.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.luminance
import dev.sk2andy.materialbrowser.data.ProtectionReportRules
import dev.sk2andy.materialbrowser.ui.theme.v5.VolaV5Theme
import dev.sk2andy.materialbrowser.ui.v5.V5PrivacyReport

/**
 * The v5 privacy report (V18) opened from the new tab's card: the week first, the summary of the
 * chosen period from the report's data, and the card's own «Clear» and «Hide».
 */
@Composable
internal fun BrowserPrivacyReportV5(
    protection: NewTabProtection,
    visible: Boolean,
    onDismiss: () -> Unit,
) {
    var windowDays by rememberSaveable { mutableIntStateOf(ProtectionReportRules.DAYS) }
    // The summary reads the report's state, so it follows new blocks while the sheet is open.
    val summarize by rememberUpdatedState(protection.summary)
    val summary by remember { derivedStateOf { summarize(windowDays) } }
    val hasData by remember { derivedStateOf { summarize(ProtectionReportRules.KEPT_DAYS).total > 0 } }
    val dark = MaterialTheme.colorScheme.background.luminance() < DARK_BACKGROUND_LUMINANCE
    VolaV5Theme(dark = dark) {
        V5PrivacyReport(
            visible = visible,
            summary = summary,
            windowDays = windowDays,
            onWindowChange = { windowDays = it },
            onClear = protection.onClear,
            onDismiss = onDismiss,
            hasData = hasData,
        )
    }
}

/** Below this background luminance the current theme is dark. */
private const val DARK_BACKGROUND_LUMINANCE = 0.5f
