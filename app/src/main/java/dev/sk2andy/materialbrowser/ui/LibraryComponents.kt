package dev.sk2andy.materialbrowser.ui

import android.graphics.Bitmap
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.sk2andy.materialbrowser.shared.ui.icons.VolaIcons
import dev.sk2andy.materialbrowser.ui.theme.VolaLibrary
import dev.sk2andy.materialbrowser.ui.theme.VolaTheme
import dev.sk2andy.materialbrowser.shared.ui.theme.densityRowMinHeight
import dev.sk2andy.materialbrowser.shared.ui.theme.densityRowPadding

/** «Today», «Yesterday», «No folder»: the small title over a card of rows. */
@Composable
internal fun LibrarySectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier
            .padding(horizontal = VolaLibrary.sidePadding)
            .padding(VolaLibrary.sectionLabelPadding),
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/**
 * One row of a card: rows of a day or of «No folder» share one rounded card, split by hairlines,
 * so each lazy item draws only its own slice of it.
 */
@Composable
internal fun LibraryCardSlice(
    position: LibraryRowPosition,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val top = if (position.roundsTop) VolaLibrary.groupRadius else 0.dp
    val bottom = if (position.roundsBottom) VolaLibrary.groupRadius else 0.dp
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = VolaLibrary.sidePadding),
        shape = RoundedCornerShape(
            topStart = top,
            topEnd = top,
            bottomStart = bottom,
            bottomEnd = bottom,
        ),
        color = VolaTheme.extendedColors.card,
    ) {
        Column {
            if (!position.roundsTop) {
                Box(
                    Modifier
                        .padding(
                            start = VolaLibrary.dividerStartInset,
                            end = VolaLibrary.dividerEndInset,
                        )
                        .fillMaxWidth()
                        .height(VolaLibrary.dividerThickness)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                )
            }
            content()
        }
    }
}

/** The row of board W-History and W-Favorites: tile, title over a line of detail, trailing. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun LibraryRow(
    title: String,
    detail: String,
    leading: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    extra: String? = null,
    detailColor: Color? = null,
    enabled: Boolean = true,
    onLongClick: (() -> Unit)? = null,
    onLongClickLabel: String? = null,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = densityRowMinHeight(VolaLibrary.rowMinHeight))
            .combinedClickable(
                enabled = enabled,
                role = Role.Button,
                onLongClickLabel = onLongClickLabel,
                onLongClick = onLongClick,
                onClick = onClick,
            )
            .padding(densityRowPadding(VolaLibrary.rowPadding)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(VolaLibrary.rowGap),
    ) {
        leading()
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(VolaLibrary.textGap),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall,
                color = detailColor ?: MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            extra?.takeIf(String::isNotBlank)?.let { value ->
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(VolaLibrary.trailingGap),
            content = trailing,
        )
    }
}

/**
 * A site's tile: its favicon when there is one, else its first letter on a soft color that is
 * the same for the same site every time. Selected, it turns into a check.
 */
@Composable
internal fun LibrarySiteTile(
    label: String,
    colorKey: String,
    modifier: Modifier = Modifier,
    favicon: Bitmap? = null,
    selected: Boolean = false,
) {
    val tile = VolaLibrary.tile(LibraryRules.tileIndex(colorKey, VolaLibrary.tileCount))
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .size(VolaLibrary.tileSize)
            .clip(VolaLibrary.tileShape)
            .background(if (selected) colors.primary else tile.container),
        contentAlignment = Alignment.Center,
    ) {
        when {
            selected -> Icon(
                VolaIcons.Check,
                contentDescription = null,
                modifier = Modifier.size(VolaLibrary.tileIconSize),
                tint = colors.onPrimary,
            )
            favicon != null && !favicon.isRecycled -> Image(
                bitmap = favicon.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
            else -> Text(
                text = LibraryRules.initial(label),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = tile.content,
            )
        }
    }
}
