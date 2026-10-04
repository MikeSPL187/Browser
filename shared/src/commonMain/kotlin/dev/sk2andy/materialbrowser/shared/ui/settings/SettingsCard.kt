package dev.sk2andy.materialbrowser.shared.ui.settings

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import dev.sk2andy.materialbrowser.shared.ui.icons.VolaIcons
import dev.sk2andy.materialbrowser.shared.ui.theme.SettingsCardTokens

/** The tile behind a row's icon, in the colors of the row's own accent. */
@Immutable
data class SettingsTileColors(
    val container: Color,
    val content: Color,
)

/** Rows that belong together on one rounded card (board W-Settings). */
@Composable
fun SettingsCard(
    containerColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = SettingsCardTokens.shape,
        color = containerColor,
    ) {
        Column(content = content)
    }
}

/** The small label above a card, such as «Search engine». */
@Composable
fun SettingsCardHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier = modifier
            .padding(SettingsCardTokens.headerPadding)
            .semantics { heading() },
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** A row's leading tile: an icon or a letter on the row's own color. */
@Composable
fun SettingsCardTile(
    colors: SettingsTileColors,
    content: @Composable (Modifier, Color) -> Unit,
) {
    Box(
        modifier = Modifier
            .size(SettingsCardTokens.tileSize)
            .background(colors.container, SettingsCardTokens.tileShape),
        contentAlignment = Alignment.Center,
    ) {
        content(Modifier.size(SettingsCardTokens.tileIconSize), colors.content)
    }
}

/**
 * One row of a [SettingsCard] that opens a page: the icon on its tile, the title, a line of what
 * is set there, and a chevron. [divider] draws the line to the next row in the page's color.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SettingsCardRow(
    title: String,
    summary: String?,
    tileColors: SettingsTileColors,
    icon: @Composable (Modifier, Color) -> Unit,
    dividerColor: Color,
    modifier: Modifier = Modifier,
    divider: Boolean = false,
    enabled: Boolean = true,
    onLongClickLabel: String? = null,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    SettingsCardRowLayout(
        title = title,
        summary = summary,
        dividerColor = dividerColor,
        divider = divider,
        enabled = enabled,
        modifier = modifier,
        interaction = Modifier.combinedClickable(
            enabled = enabled,
            role = Role.Button,
            onLongClickLabel = onLongClickLabel,
            onLongClick = onLongClick,
            onClick = onClick,
        ),
        leading = { SettingsCardTile(colors = tileColors, content = icon) },
        trailing = {
            Icon(
                VolaIcons.KeyboardArrowRight,
                contentDescription = null,
                modifier = Modifier.size(SettingsCardTokens.chevronSize),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
    )
}

/** A row that opens another screen, such as Filter Studio; no tile, a chevron at the end. */
@Composable
fun SettingsCardLinkRow(
    title: String,
    summary: String?,
    dividerColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    divider: Boolean = false,
    summaryMaxLines: Int = SUMMARY_MAX_LINES,
) {
    SettingsCardRowLayout(
        title = title,
        summary = summary,
        dividerColor = dividerColor,
        divider = divider,
        modifier = modifier,
        summaryMaxLines = summaryMaxLines,
        interaction = Modifier.clickable(role = Role.Button, onClick = onClick),
        trailing = {
            Icon(
                VolaIcons.KeyboardArrowRight,
                contentDescription = null,
                modifier = Modifier.size(SettingsCardTokens.chevronSize),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
    )
}

/** One of several options on a card, such as a search engine; the chosen one has a check. */
@Composable
fun SettingsCardChoiceRow(
    title: String,
    summary: String?,
    selected: Boolean,
    dividerColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    divider: Boolean = false,
    leading: (@Composable () -> Unit)? = null,
) {
    SettingsCardRowLayout(
        title = title,
        summary = summary,
        dividerColor = dividerColor,
        divider = divider,
        modifier = modifier,
        interaction = Modifier.selectable(
            selected = selected,
            role = Role.RadioButton,
            onClick = onClick,
        ),
        leading = leading,
        trailing = {
            if (selected) {
                Icon(
                    VolaIcons.Check,
                    contentDescription = null,
                    modifier = Modifier.size(SettingsCardTokens.chevronSize),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        },
    )
}

/** A setting turned on and off on a card; the whole row toggles it. */
@Composable
fun SettingsCardSwitchRow(
    title: String,
    summary: String?,
    checked: Boolean,
    dividerColor: Color,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    divider: Boolean = false,
    enabled: Boolean = true,
    summaryMaxLines: Int = SUMMARY_MAX_LINES,
    leading: (@Composable () -> Unit)? = null,
) {
    SettingsCardRowLayout(
        title = title,
        summary = summary,
        dividerColor = dividerColor,
        divider = divider,
        enabled = enabled,
        modifier = modifier,
        summaryMaxLines = summaryMaxLines,
        interaction = Modifier.toggleable(
            value = checked,
            enabled = enabled,
            role = Role.Switch,
            onValueChange = onCheckedChange,
        ),
        leading = leading,
        trailing = {
            Switch(checked = checked, onCheckedChange = null, enabled = enabled)
        },
    )
}

/** A setting with a few values, the current one at the end; a tap opens the list of values. */
@Composable
fun SettingsCardValueRow(
    title: String,
    value: String,
    summary: String?,
    dividerColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    divider: Boolean = false,
    enabled: Boolean = true,
) {
    SettingsCardRowLayout(
        title = title,
        summary = summary,
        dividerColor = dividerColor,
        divider = divider,
        enabled = enabled,
        modifier = modifier,
        // The summary explains the chosen value, so it is never cut short.
        summaryMaxLines = Int.MAX_VALUE,
        interaction = Modifier.clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        trailing = {
            Text(
                value,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Icon(
                VolaIcons.ArrowDropDown,
                contentDescription = null,
                modifier = Modifier.size(SettingsCardTokens.chevronSize),
                tint = MaterialTheme.colorScheme.primary,
            )
        },
    )
}

/** The shape every card row shares: leading tile, title and summary, trailing control. */
@Composable
private fun SettingsCardRowLayout(
    title: String,
    summary: String?,
    dividerColor: Color,
    divider: Boolean,
    interaction: Modifier,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    summaryMaxLines: Int = SUMMARY_MAX_LINES,
    leading: (@Composable () -> Unit)? = null,
    trailing: @Composable RowScope.() -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // The caller's modifier (a test tag, say) lands on the row that holds the semantics.
        Row(
            modifier = modifier
                .fillMaxWidth()
                .heightIn(min = SettingsCardTokens.rowMinHeight)
                .then(interaction)
                .graphicsLayer { alpha = if (enabled) 1f else DISABLED_ALPHA }
                .padding(SettingsCardTokens.rowPadding),
            horizontalArrangement = Arrangement.spacedBy(SettingsCardTokens.rowGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            leading?.invoke()
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(SettingsCardTokens.textGap),
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (!summary.isNullOrEmpty()) {
                    Text(
                        summary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = summaryMaxLines,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            trailing()
        }
        if (divider) {
            Box(
                modifier = Modifier
                    .padding(
                        start = if (leading != null) {
                            SettingsCardTokens.dividerStartInset
                        } else {
                            SettingsCardTokens.dividerEndInset
                        },
                        end = SettingsCardTokens.dividerEndInset,
                    )
                    .fillMaxWidth()
                    .height(SettingsCardTokens.dividerThickness)
                    .background(dividerColor),
            )
        }
    }
}

private const val DISABLED_ALPHA = 0.38f
private const val SUMMARY_MAX_LINES = 2
