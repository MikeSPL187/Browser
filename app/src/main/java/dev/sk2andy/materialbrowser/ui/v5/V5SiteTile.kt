package dev.sk2andy.materialbrowser.ui.v5

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import dev.sk2andy.materialbrowser.shared.ui.icons.VolaIcons
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Sizes
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Theme

/**
 * A site as a small tile (canvas «Address», «Overview», «Menu», «Main»): its icon when it has one,
 * otherwise its first letter, white on a color of its own — the same site always gets the same
 * color ([V5NewTabRules.letterTile]). Only a page that is no site at all ([site] null) shows the
 * neutral globe. The tile is decoration: the row around it says the site's name.
 */
@Composable
fun V5SiteTile(
    favicon: ImageBitmap?,
    /** The site's name or host the letter and color come from; null when there is no site. */
    site: String?,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    val shape = V5Theme.shapes.xs
    when {
        favicon != null -> Image(
            favicon,
            contentDescription = null,
            modifier = modifier.size(size).clip(shape),
        )
        site.isNullOrBlank() -> Icon(
            VolaIcons.Public,
            contentDescription = null,
            tint = V5Theme.colors.labelSecondary,
            modifier = modifier.size(size),
        )
        else -> {
            val tile = V5NewTabRules.letterTile(site)
            Box(
                modifier
                    .size(size)
                    .clip(shape)
                    .background(tile.color)
                    .clearAndSetSemantics {},
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    V5NewTabRules.letter(site),
                    style = if (size < V5Sizes.suggestionTile) V5Theme.type.caption else V5Theme.type.headline,
                    color = tile.glyph,
                    maxLines = 1,
                )
            }
        }
    }
}
