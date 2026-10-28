package dev.sk2andy.materialbrowser.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
/**
 * Which part of a settings page a screen shows (V15d): under ui.v5 a page keeps one layer of what
 * most people set and moves the rest behind «More», as iOS Settings does (system-v5.md section 10).
 */
internal enum class SettingsLayer {
    /** The current interface: everything on one page. */
    All,

    /** Under ui.v5: the page itself, with a «More» row. */
    Main,

    /** Under ui.v5: the page's «More» screen. */
    More,
    ;

    val main: Boolean get() = this != More
    val more: Boolean get() = this != Main
}

internal object SettingsLayerRules {
    /**
     * Whether settings speak for the v5 interface: whenever the flag is on, since v5 draws the
     * browser on every width.
     */
    @Suppress("UNUSED_PARAMETER")
    fun speaksForV5(flag: Boolean, smallestWidthDp: Float): Boolean = flag

    /** The «More» screen of each page that has one, and the page it belongs to. */
    val moreOf: Map<SettingsDestination, SettingsDestination> = mapOf(
        SettingsDestination.ProtectionMore to SettingsDestination.ProtectionAndData,
        SettingsDestination.BrowserMore to SettingsDestination.Browser,
        SettingsDestination.TabsMore to SettingsDestination.TabsAndGestures,
    )

    fun layer(uiV5: Boolean, destination: SettingsDestination): SettingsLayer = when {
        !uiV5 -> SettingsLayer.All
        destination in moreOf -> SettingsLayer.More
        else -> SettingsLayer.Main
    }
}

/** One row of a settings card that knows whether a line follows it. */
internal typealias CardRow = @Composable (divider: Boolean) -> Unit

/** [rows] one under another, with a line between each two. */
@Composable
internal fun CardRows(rows: List<CardRow>) {
    rows.forEachIndexed { index, row -> row(index < rows.lastIndex) }
}
