package dev.sk2andy.materialbrowser.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.platform.testTag
import dev.sk2andy.materialbrowser.shared.ui.TabOverviewChromeTestTags
import dev.sk2andy.materialbrowser.ui.theme.VolaTheme
import dev.sk2andy.materialbrowser.ui.theme.auraBrush

/**
 * Behind the tab overview: the workspace aura, as around the framed page, or the workspace's tab
 * switcher wallpaper with the system bars kept readable.
 */
@Composable
internal fun TabOverviewBackground(
    wallpaper: ProfileWallpaperRuntime?,
    statusBarInsets: WindowInsets = WindowInsets.statusBars,
    navigationBarInsets: WindowInsets = WindowInsets.navigationBars,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val backgroundModifier = if (wallpaper == null) {
        Modifier.background(VolaTheme.auraBrush)
    } else {
        Modifier.drawBehind {
            drawProfileWallpaper(
                bitmap = wallpaper.bitmap,
                wallpaper = wallpaper.wallpaper,
                scrimAlpha = WALLPAPER_SCRIM_ALPHA,
            )
        }
    }
    Box(
        modifier = modifier
            .testTag(TabOverviewChromeTestTags.Background)
            .then(backgroundModifier),
    ) {
        if (wallpaper != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .windowInsetsTopHeight(statusBarInsets)
                    .background(colors.surface.copy(alpha = SYSTEM_BAR_SCRIM_ALPHA)),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .windowInsetsBottomHeight(navigationBarInsets)
                    .background(colors.surface.copy(alpha = SYSTEM_BAR_SCRIM_ALPHA)),
            )
        }
    }
}

private const val WALLPAPER_SCRIM_ALPHA = 0.54f
private const val SYSTEM_BAR_SCRIM_ALPHA = 0.92f
