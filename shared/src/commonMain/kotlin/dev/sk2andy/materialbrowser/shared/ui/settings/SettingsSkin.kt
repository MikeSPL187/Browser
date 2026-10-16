package dev.sk2andy.materialbrowser.shared.ui.settings

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/** What a settings row ends with; a [SettingsSkin] draws it its own way. */
@Immutable
sealed interface SettingsRowEnd {
    /** Opens a page or another screen. */
    data object Chevron : SettingsRowEnd

    /** One of several options; the chosen one is checked. */
    data class Check(val selected: Boolean) : SettingsRowEnd

    /** The whole row is the switch. */
    data class Switch(val checked: Boolean, val enabled: Boolean) : SettingsRowEnd

    /** The current value of a setting with a few values; a tap opens the list. */
    data class Value(val text: String) : SettingsRowEnd
}

/**
 * Another look for the settings components, such as the app's v5 interface (V15d), so every page
 * built from them changes at once. The components keep what they mean (roles, toggling, the
 * caller's test tags in [Row]'s `modifier`, the action in `interaction`) and leave the skin only
 * the drawing. Without a skin ([LocalSettingsSkin] is null) they draw the current cards.
 */
@Stable
interface SettingsSkin {
    @Composable
    fun Page(
        title: String,
        backContentDescription: String,
        onBack: () -> Unit,
        actions: @Composable RowScope.() -> Unit,
        content: @Composable ColumnScope.() -> Unit,
    )

    @Composable
    fun Card(modifier: Modifier, content: @Composable ColumnScope.() -> Unit)

    @Composable
    fun Header(text: String, modifier: Modifier)

    @Composable
    fun Tile(colors: SettingsTileColors, content: @Composable (Modifier, Color) -> Unit)

    /**
     * One row: [leading], [title] over [summary], and [end]. [interaction] carries the row's action
     * and semantics and goes on the row with [modifier]; [divider] draws the line to the next row.
     */
    @Composable
    fun Row(
        title: String,
        summary: String?,
        summaryMaxLines: Int,
        enabled: Boolean,
        divider: Boolean,
        interaction: Modifier,
        modifier: Modifier,
        leading: (@Composable () -> Unit)?,
        end: SettingsRowEnd,
    )

    @Composable
    fun SliderRow(
        title: String,
        summary: String?,
        value: String?,
        divider: Boolean,
        modifier: Modifier,
        slider: @Composable () -> Unit,
    )

    /** Feedback as a switch row flips, such as a haptic tick; called before the change. */
    fun onToggled(checked: Boolean) = Unit
}

/** The skin settings components draw with; null keeps the current cards. */
val LocalSettingsSkin = staticCompositionLocalOf<SettingsSkin?> { null }
