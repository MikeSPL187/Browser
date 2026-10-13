package dev.sk2andy.materialbrowser.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.data.AppearanceSettings
import dev.sk2andy.materialbrowser.data.BrowserAppearanceMode
import dev.sk2andy.materialbrowser.shared.ui.settings.SettingsCard
import dev.sk2andy.materialbrowser.shared.ui.settings.SettingsCardChoiceRow
import dev.sk2andy.materialbrowser.shared.ui.settings.SettingsCardHeader
import dev.sk2andy.materialbrowser.shared.ui.settings.SettingsCardSliderRow
import dev.sk2andy.materialbrowser.shared.ui.settings.SettingsCardSwitchRow
import dev.sk2andy.materialbrowser.ui.theme.browserChromeColor
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Spacing

object AppearanceSettingsV5TestTags {
    const val FontSize = "appearance_v5_font_size"
    const val DarkSites = "appearance_v5_dark_sites"
    const val Animations = "appearance_v5_animations"

    fun mode(mode: BrowserAppearanceMode) = "appearance_v5_mode:${mode.stableId}"
}

/**
 * «Appearance» under ui.v5 (V15d-2): only what the v5 interface uses, one layer. The theme, the
 * size of text on sites, dark sites and motion. The current interface's frame («Рама»/«Воздух»),
 * themes and palettes, address bar colours and glass change nothing in v5, so they stay on its own
 * page. The bar's site colour is on the settings home, as on the canvas. Dark sites are only
 * offered where the engine can darken them (System WebView).
 */
@Composable
internal fun AppearanceSettingsPageV5(
    settings: AppearanceSettings,
    onSettingsChanged: (AppearanceSettings) -> Unit,
    onBack: () -> Unit,
    forceDarkWebsitesAvailable: Boolean,
) {
    val container = browserChromeColor(MaterialTheme.colorScheme.surfaceContainerHigh)
    val separator = MaterialTheme.colorScheme.outlineVariant
    SettingsPage(title = stringResource(R.string.settings_appearance_title), onBack = onBack) {
        SettingsCardHeader(stringResource(R.string.settings_appearance_mode))
        SettingsCard(containerColor = container) {
            AppearanceV5Rules.MODES.forEachIndexed { index, mode ->
                SettingsCardChoiceRow(
                    title = if (mode == BrowserAppearanceMode.System) {
                        stringResource(R.string.appearance_mode_auto)
                    } else {
                        mode.displayName()
                    },
                    summary = null,
                    selected = settings.appearanceMode == mode,
                    dividerColor = separator,
                    onClick = { onSettingsChanged(settings.copy(appearanceMode = mode)) },
                    modifier = Modifier.testTag(AppearanceSettingsV5TestTags.mode(mode)),
                    divider = index < AppearanceV5Rules.MODES.lastIndex,
                )
            }
        }
        SettingsCardHeader(stringResource(R.string.settings_browser_group_websites))
        SettingsCard(containerColor = container) {
            var pending by remember(settings.webContentFontSizePercent) {
                mutableIntStateOf(settings.webContentFontSizePercent)
            }
            val fontSizeTitle = stringResource(R.string.settings_web_content_font_size)
            SettingsCardSliderRow(
                title = fontSizeTitle,
                summary = null,
                value = stringResource(R.string.settings_value_percent, pending),
                dividerColor = separator,
                divider = forceDarkWebsitesAvailable,
                modifier = Modifier.testTag(AppearanceSettingsV5TestTags.FontSize),
            ) {
                SettingsCardSlider(
                    value = pending,
                    range = AppearanceSettings.MIN_WEB_CONTENT_FONT_SIZE_PERCENT..
                        AppearanceSettings.MAX_WEB_CONTENT_FONT_SIZE_PERCENT,
                    step = AppearanceSettings.WEB_CONTENT_FONT_SIZE_STEP_PERCENT,
                    label = fontSizeTitle,
                    onValueChange = { pending = it },
                    onValueChangeFinished = { percent ->
                        val updated = settings.copy(webContentFontSizePercent = percent).normalized()
                        pending = updated.webContentFontSizePercent
                        if (updated != settings) onSettingsChanged(updated)
                    },
                )
            }
            if (forceDarkWebsitesAvailable) {
                SettingsCardSwitchRow(
                    title = stringResource(R.string.settings_force_dark_websites),
                    summary = stringResource(R.string.settings_force_dark_websites_summary),
                    checked = settings.forceDarkWebsites,
                    dividerColor = separator,
                    onCheckedChange = { onSettingsChanged(settings.copy(forceDarkWebsites = it)) },
                    modifier = Modifier.testTag(AppearanceSettingsV5TestTags.DarkSites),
                )
            }
        }
        Spacer(Modifier.height(V5Spacing.space6))
        SettingsCard(containerColor = container) {
            SettingsCardSwitchRow(
                title = stringResource(R.string.settings_animations),
                summary = stringResource(R.string.settings_animations_summary),
                checked = settings.animationsEnabled,
                dividerColor = separator,
                onCheckedChange = { onSettingsChanged(settings.copy(animationsEnabled = it)) },
                modifier = Modifier.testTag(AppearanceSettingsV5TestTags.Animations),
            )
        }
    }
}

/** The pure part of the v5 Appearance page. */
internal object AppearanceV5Rules {
    /** Auto first, as Safari and the system settings list it. */
    val MODES = listOf(BrowserAppearanceMode.System, BrowserAppearanceMode.Light, BrowserAppearanceMode.Dark)
}
