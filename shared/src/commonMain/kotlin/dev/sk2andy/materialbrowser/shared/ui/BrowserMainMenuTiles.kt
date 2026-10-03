package dev.sk2andy.materialbrowser.shared.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.TextUnit
import dev.sk2andy.materialbrowser.shared.browser.BrowserFeatureMenuItem
import dev.sk2andy.materialbrowser.shared.browser.BrowserFeatureMenuItemKind
import dev.sk2andy.materialbrowser.shared.browser.BrowserFeatureMenuSection

/**
 * The tile grid of the v4 main menu (board W-Menu): page and Vola actions as square tiles, an
 * icon over a short label; toggles keep their state in the tile color. The platform supplies the
 * measures from its theme tokens.
 */
data class BrowserMainMenuTileStyle(
    val columns: Int,
    val tileHeight: Dp,
    val cornerRadius: Dp,
    val spacing: Dp,
    val iconSize: Dp,
    val iconLabelGap: Dp,
    val horizontalPadding: Dp,
    val labelFontSize: TextUnit,
    val labelLineHeight: TextUnit,
    val labelMaxLines: Int,
    val disabledAlpha: Float,
    val handleSize: DpSize,
    val handleTopPadding: Dp,
    val handleBottomPadding: Dp,
)

object BrowserMainMenuTileRules {
    private val TILE_SECTIONS = setOf(
        BrowserFeatureMenuSection.Page,
        BrowserFeatureMenuSection.Candy,
    )

    /** Page and Vola actions become tiles; toolbar, user-script commands and the library stay. */
    fun isTile(item: BrowserFeatureMenuItem): Boolean = item.section in TILE_SECTIONS

    /** Rows of the grid in menu order; the last row may be short. */
    fun <T> rows(items: List<T>, columns: Int): List<List<T>> = items.chunked(columns.coerceAtLeast(1))

    /** A toggle tile switches in place; every other tile runs its action and closes the menu. */
    fun togglesInPlace(item: BrowserFeatureMenuItem): Boolean =
        item.kind == BrowserFeatureMenuItemKind.Toggle && item.checked != null
}

@Composable
fun BrowserMainMenuHandle(tiles: BrowserMainMenuTileStyle) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = tiles.handleTopPadding, bottom = tiles.handleBottomPadding),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(tiles.handleSize)
                .clip(RoundedCornerShape(tiles.handleSize.height))
                .background(MaterialTheme.colorScheme.outlineVariant),
        )
    }
}

@Composable
fun BrowserMainMenuTileGrid(
    items: List<BrowserFeatureMenuItem>,
    tiles: BrowserMainMenuTileStyle,
    resources: BrowserMainMenuResources,
    effects: BrowserMainMenuEffects,
    onCommand: (BrowserFeatureMenuItem) -> Unit,
    onToggle: (BrowserFeatureMenuItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(tiles.spacing),
    ) {
        BrowserMainMenuTileRules.rows(items, tiles.columns).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(tiles.spacing)) {
                row.forEach { item ->
                    BrowserMainMenuTile(
                        item = item,
                        tiles = tiles,
                        resources = resources,
                        effects = effects,
                        onCommand = onCommand,
                        onToggle = onToggle,
                        modifier = Modifier.weight(1f),
                    )
                }
                repeat(tiles.columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun BrowserMainMenuTile(
    item: BrowserFeatureMenuItem,
    tiles: BrowserMainMenuTileStyle,
    resources: BrowserMainMenuResources,
    effects: BrowserMainMenuEffects,
    onCommand: (BrowserFeatureMenuItem) -> Unit,
    onToggle: (BrowserFeatureMenuItem) -> Unit,
    modifier: Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val checked = item.checked == true
    val toggle = BrowserMainMenuTileRules.togglesInPlace(item)
    val containerColor = effects.containerColor(
        color = if (checked) colors.secondaryContainer else colors.surfaceContainerHighest,
        frostedAlpha = 1f,
        role = if (checked) {
            BrowserMainMenuContainerRole.Selected
        } else {
            BrowserMainMenuContainerRole.Regular
        },
    )
    val contentColor = if (checked) colors.onSecondaryContainer else colors.onSurface
    val shape = RoundedCornerShape(tiles.cornerRadius)
    val interaction = if (toggle) {
        Modifier.toggleable(
            value = checked,
            enabled = item.enabled,
            role = Role.Switch,
            onValueChange = { onToggle(item) },
        )
    } else {
        Modifier.clickable(
            enabled = item.enabled,
            role = Role.Button,
            onClick = { onCommand(item) },
        )
    }
    CompositionLocalProvider(LocalContentColor provides contentColor) {
        Column(
            modifier = modifier
                .heightIn(min = tiles.tileHeight)
                .clip(shape)
                .background(containerColor)
                .then(interaction)
                .then(item.testTagModifier())
                .alpha(if (item.enabled) 1f else tiles.disabledAlpha)
                .padding(horizontal = tiles.horizontalPadding, vertical = tiles.iconLabelGap),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(tiles.iconLabelGap, Alignment.CenterVertically),
        ) {
            resources.icon(item, Modifier.size(tiles.iconSize))
            Text(
                text = resources.label(item),
                textAlign = TextAlign.Center,
                maxLines = tiles.labelMaxLines,
                overflow = TextOverflow.Ellipsis,
                fontSize = tiles.labelFontSize,
                lineHeight = tiles.labelLineHeight,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}
