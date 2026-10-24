package dev.sk2andy.materialbrowser.ui.v5

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.shared.ui.icons.VolaIcons
import dev.sk2andy.materialbrowser.ui.theme.v5.SmoothCornerShape
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Sizes
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Spacing
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Theme
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Tile
import dev.sk2andy.materialbrowser.ui.theme.v5.v5Glow

object V5PrivateNewTabTestTags {
    const val Page = "v5_private_new_tab"
    const val Lock = "v5_private_lock"
}

/**
 * One line of what a private tab keeps and does not, on its colored [tile]; [warning] marks a
 * promise the engine cannot keep.
 */
@Immutable
data class V5PrivateFact(
    @param:DrawableRes val icon: Int,
    val title: String,
    val caption: String,
    val tile: V5Tile,
    val warning: Boolean = false,
)

/** «Lock on exit»: [available] is false without a strong biometric, and [caption] says so. */
@Immutable
data class V5PrivateLock(val checked: Boolean, val available: Boolean, val caption: String)

@Immutable
data class V5PrivateNewTabState(
    /** What the engine lets the page promise: everything gone, or only the history. */
    val body: String,
    val facts: List<V5PrivateFact>,
    val lock: V5PrivateLock?,
    /** The quiet line under the card: what stays after the private tabs close. */
    val note: String? = null,
)

/**
 * The v5 private new tab (V14a, canvas screen «Private», system-v5.md section 1.4): always the dark
 * violet-gray private palette with a soft accent glow from the top, the mask on its accent gem,
 * «Private browsing», what is and is not kept for this engine on colored tiles, «Lock on exit» and
 * what stays behind. Closing all private tabs is in the overview and on the lock (audit question 4).
 */
@Composable
fun V5PrivateNewTab(
    state: V5PrivateNewTabState,
    onLockChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    val colors = V5Theme.colors
    val textPadding = Modifier.padding(horizontal = V5Spacing.marginPhone + V5Spacing.space1)
    BoxWithConstraints(
        modifier
            .fillMaxSize()
            .background(colors.background)
            .testTag(V5PrivateNewTabTestTags.Page),
    ) {
        val width = with(LocalDensity.current) { maxWidth.toPx() }
        Box(
            Modifier
                .fillMaxWidth()
                .height(V5Sizes.newTabWash)
                .background(
                    Brush.radialGradient(
                        colors = listOf(colors.accentSoft, Color.Transparent),
                        center = Offset(width / 2f, 0f),
                        radius = width,
                    ),
                ),
        )
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(contentPadding)
                .padding(top = V5Spacing.space16, bottom = V5Spacing.space6),
        ) {
            Gem(textPadding)
            Text(
                stringResource(R.string.v5_private_title),
                style = V5Theme.type.largeTitle,
                color = colors.label,
                modifier = textPadding
                    .padding(top = V5Spacing.space6)
                    .semantics { heading() },
            )
            Text(
                state.body,
                style = V5Theme.type.body,
                color = colors.labelSecondary,
                modifier = textPadding.padding(top = V5Spacing.space2),
            )
            Facts(state.facts, state.lock, onLockChange, Modifier.padding(top = V5Spacing.space6))
            if (state.note != null) {
                Note(state.note, textPadding.padding(top = V5Spacing.space4))
            }
        }
    }
}

/** The mask, white on the accent gem that glows in its own color. */
@Composable
private fun Gem(modifier: Modifier) {
    val colors = V5Theme.colors
    val shape = SmoothCornerShape(V5Sizes.privateGemTileRadius)
    Box(
        modifier
            .size(V5Sizes.privateGemTile)
            .v5Glow(colors.accentFill, shape)
            .clip(shape)
            .background(colors.accentFill),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painterResource(R.drawable.ic_incognito_filled),
            contentDescription = null,
            tint = colors.onAccentFill,
            modifier = Modifier.size(V5Sizes.privateGemTileIcon),
        )
    }
}

@Composable
private fun Facts(
    facts: List<V5PrivateFact>,
    lock: V5PrivateLock?,
    onLockChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    V5ListGroup(modifier.padding(horizontal = V5Spacing.space1)) {
        facts.forEach { fact ->
            V5ListRow(
                title = fact.title,
                subtitle = fact.caption,
                leading = { FactTile(fact) },
                titleColor = if (fact.warning) V5Theme.colors.warning else V5Theme.colors.label,
                modifier = Modifier.semantics(mergeDescendants = true) {},
            )
        }
        if (lock != null) {
            V5ListRow(
                title = stringResource(R.string.private_tab_lock_title),
                subtitle = lock.caption,
                icon = V5RowIcon(VolaIcons.Fingerprint, tile = V5Tile.Indigo),
                trailing = V5RowTrailing.Toggle(
                    checked = lock.checked && lock.available,
                    onCheckedChange = onLockChange,
                ),
                enabled = lock.available,
                modifier = Modifier.testTag(V5PrivateNewTabTestTags.Lock),
            )
        }
    }
}

/** A fact's glyph, white on its tile; a broken promise takes the warning's amber. */
@Composable
private fun FactTile(fact: V5PrivateFact) {
    val tile = if (fact.warning) V5Tile.Amber else fact.tile
    Box(
        Modifier
            .size(V5Sizes.rowTile)
            .clip(V5Theme.shapes.xs)
            .background(tile.color),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painterResource(fact.icon),
            contentDescription = null,
            tint = tile.glyph,
            modifier = Modifier.size(V5Sizes.iconSmall),
        )
    }
}

/** What stays behind, under the card with the download glyph. */
@Composable
private fun Note(text: String, modifier: Modifier) {
    val colors = V5Theme.colors
    Row(
        modifier.padding(horizontal = V5Spacing.space1),
        horizontalArrangement = Arrangement.spacedBy(V5Spacing.space2),
    ) {
        Icon(
            painterResource(R.drawable.ic_reader_download),
            contentDescription = null,
            tint = colors.labelSecondary,
            modifier = Modifier.size(V5Sizes.iconSmall),
        )
        Text(text, style = V5Theme.type.footnote, color = colors.labelSecondary)
    }
}
