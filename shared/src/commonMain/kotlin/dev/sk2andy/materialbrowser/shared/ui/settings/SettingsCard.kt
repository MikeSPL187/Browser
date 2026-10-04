package dev.sk2andy.materialbrowser.shared.ui.settings

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
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
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = SettingsCardTokens.rowMinHeight)
                .combinedClickable(
                    enabled = enabled,
                    role = Role.Button,
                    onLongClickLabel = onLongClickLabel,
                    onLongClick = onLongClick,
                    onClick = onClick,
                )
                .graphicsLayer { alpha = if (enabled) 1f else DISABLED_ALPHA }
                .padding(SettingsCardTokens.rowPadding),
            horizontalArrangement = Arrangement.spacedBy(SettingsCardTokens.rowGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(SettingsCardTokens.tileSize)
                    .background(tileColors.container, SettingsCardTokens.tileShape),
                contentAlignment = Alignment.Center,
            ) {
                icon(Modifier.size(SettingsCardTokens.tileIconSize), tileColors.content)
            }
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
                        maxLines = SUMMARY_MAX_LINES,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Icon(
                VolaIcons.KeyboardArrowRight,
                contentDescription = null,
                modifier = Modifier.size(SettingsCardTokens.chevronSize),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (divider) {
            Box(
                modifier = Modifier
                    .padding(
                        start = SettingsCardTokens.dividerStartInset,
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
