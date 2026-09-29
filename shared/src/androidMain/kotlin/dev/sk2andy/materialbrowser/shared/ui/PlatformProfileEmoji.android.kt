package dev.sk2andy.materialbrowser.shared.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.TextUnit

/**
 * Draws a workspace icon. The emoji is only the stored key: Vola shows the matching Material
 * Symbol, tinted like the surrounding content, at the size the emoji glyph used to take.
 */
@Composable
actual fun PlatformProfileEmoji(
    emoji: String,
    fontSize: TextUnit,
    modifier: Modifier,
) {
    val iconSize = with(LocalDensity.current) { fontSize.toDp() }
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = WorkspaceIcons.vector(emoji),
            contentDescription = null,
            modifier = Modifier.size(iconSize),
            tint = LocalContentColor.current,
        )
    }
}
