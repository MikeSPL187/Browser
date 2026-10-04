package dev.sk2andy.materialbrowser.ui

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.blocking.BlockerSettings
import dev.sk2andy.materialbrowser.browser.HttpsOnlyMode
import dev.sk2andy.materialbrowser.browser.SearchEngine
import dev.sk2andy.materialbrowser.data.AppearanceSettings
import dev.sk2andy.materialbrowser.data.BrowserAppearanceMode
import dev.sk2andy.materialbrowser.data.BrowserChromeStyle
import dev.sk2andy.materialbrowser.settings.SettingsRegistry

/** What the settings home shows under a page: what is set there now (board W-Settings). */
internal data class SettingsHomeState(
    val appearance: AppearanceSettings,
    val searchEngine: SearchEngine,
    val blocker: BlockerSettings,
    val httpsOnlyMode: HttpsOnlyMode,
    val isHttpsOnlySupported: Boolean,
    val downloadSummary: String,
    val searchSuggestionsOn: Boolean = false,
    val tabCount: Int = 0,
    val userscriptCount: Int = 0,
    val capsuleCount: Int = 0,
    val isDefaultBrowser: Boolean = false,
)

/** The pure part of the home's summaries, string resources picked by the settings. */
internal object SettingsHomeSummaryRules {
    @StringRes
    fun chromeStyle(style: BrowserChromeStyle): Int = when (style) {
        BrowserChromeStyle.Frame -> R.string.settings_chrome_style_frame
        BrowserChromeStyle.Air -> R.string.settings_chrome_style_air
    }

    @StringRes
    fun theme(mode: BrowserAppearanceMode): Int = when (mode) {
        BrowserAppearanceMode.System -> R.string.settings_home_theme_auto
        BrowserAppearanceMode.Light -> R.string.settings_home_theme_light
        BrowserAppearanceMode.Dark -> R.string.settings_home_theme_dark
    }

    /** Tracker blocking and HTTPS in every tab are what protection means at a glance. */
    @StringRes
    fun protection(state: SettingsHomeState): Int? {
        val trackers = state.blocker.blockAdsAndTrackers
        val httpsEverywhere = state.isHttpsOnlySupported &&
            state.httpsOnlyMode == HttpsOnlyMode.Always
        return when {
            trackers && httpsEverywhere -> R.string.settings_home_protection_trackers_https
            trackers -> R.string.settings_home_protection_trackers
            httpsEverywhere -> R.string.settings_home_protection_https
            else -> null
        }
    }

    /** Parts of a summary, the missing ones left out, joined the way the board joins them. */
    fun join(parts: List<String?>): String? =
        parts.filterNot { it.isNullOrBlank() }.joinToString(SEPARATOR).ifEmpty { null }

    /** From a large font on, the «Make default» button goes under the text instead of beside it. */
    fun stacksMakeDefault(fontScale: Float): Boolean = fontScale >= LARGE_FONT_SCALE

    private const val SEPARATOR = " · "
    private const val LARGE_FONT_SCALE = 1.3f
}

/** The line under [destination] on the settings home; the registry's description otherwise. */
@Composable
internal fun settingsHomeSummary(
    destination: SettingsDestination,
    state: SettingsHomeState,
): String? = when (destination) {
    SettingsDestination.Appearance -> stringResource(
        R.string.settings_home_appearance_live,
        stringResource(SettingsHomeSummaryRules.chromeStyle(state.appearance.chromeStyle)),
        stringResource(SettingsHomeSummaryRules.theme(state.appearance.appearanceMode)),
    )
    SettingsDestination.Search -> SettingsHomeSummaryRules.join(
        listOf(
            state.searchEngine.displayName,
            stringResource(R.string.settings_home_search_suggestions)
                .takeIf { state.searchSuggestionsOn },
        ),
    )
    SettingsDestination.Downloads -> state.downloadSummary
    SettingsDestination.TabsAndGestures -> state.tabCount.takeIf { it > 0 }
        ?.let { count -> pluralStringResource(R.plurals.settings_home_open_tabs, count, count) }
        ?: registrySummary(destination)
    SettingsDestination.Userscripts -> state.userscriptCount.takeIf { it > 0 }
        ?.let { count ->
            pluralStringResource(R.plurals.settings_home_userscripts_count, count, count)
        }
        ?: registrySummary(destination)
    SettingsDestination.SiteCapsules -> state.capsuleCount.takeIf { it > 0 }
        ?.let { count -> pluralStringResource(R.plurals.settings_home_capsules_count, count, count) }
        ?: registrySummary(destination)
    SettingsDestination.Browser -> if (state.isDefaultBrowser) {
        stringResource(R.string.settings_default_browser_active)
    } else {
        registrySummary(destination)
    }
    SettingsDestination.ProtectionAndData ->
        SettingsHomeSummaryRules.protection(state)?.let { stringResource(it) }
            ?: registrySummary(destination)
    else -> registrySummary(destination)
}

@Composable
private fun registrySummary(destination: SettingsDestination): String? =
    SettingsRegistry.page(destination)?.summary?.let { stringResource(it) }
