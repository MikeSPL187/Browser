package dev.sk2andy.materialbrowser.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import dev.sk2andy.materialbrowser.browser.BrowserProfile
import dev.sk2andy.materialbrowser.shared.ui.PlatformProfileEmoji
import dev.sk2andy.materialbrowser.ui.theme.VolaGem
import dev.sk2andy.materialbrowser.ui.theme.volaGemColors

/**
 * The workspace gem from the Components board: the workspace icon on a rounded square in the
 * workspace's own primary color. Decorative; the caller labels the workspace in text.
 */
@Composable
internal fun WorkspaceGem(
    workspace: BrowserProfile,
    size: Dp,
    modifier: Modifier = Modifier,
    privateMode: Boolean = false,
) {
    val colors = volaGemColors(workspace.accent, privateMode)
    val iconSize = with(LocalDensity.current) { (size * VolaGem.ICON_FRACTION).toSp() }
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * VolaGem.CORNER_FRACTION))
            .background(colors.fill),
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(LocalContentColor provides colors.content) {
            PlatformProfileEmoji(emoji = workspace.emoji, fontSize = iconSize)
        }
    }
}
