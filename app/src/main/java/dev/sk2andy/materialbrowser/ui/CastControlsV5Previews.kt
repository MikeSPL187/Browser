package dev.sk2andy.materialbrowser.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import dev.sk2andy.materialbrowser.browser.cast.CastUiState
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Spacing
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Theme
import dev.sk2andy.materialbrowser.ui.theme.v5.VolaV5Theme

/* Casting under v5: the line above the bar and the sheet it opens. Preview text is not localized. */

private val previewCast = CastUiState(
    isConnected = true,
    hasMedia = true,
    isPlaying = true,
    title = "Байкал зимой — 4K",
    deviceName = "Телевизор в гостиной",
    positionMillis = 754_000L,
    durationMillis = 1_820_000L,
    deviceVolume = 0.4f,
)

@Composable
private fun MiniPreview(dark: Boolean) {
    VolaV5Theme(dark = dark) {
        Box(Modifier.background(V5Theme.colors.background).padding(vertical = V5Spacing.space4)) {
            CastMiniControllerV5(previewCast, onExpand = {}, onTogglePlayback = {}, onDisconnect = {})
        }
    }
}

@Preview(name = "Cast v5 · line, light")
@Composable
private fun CastMiniV5LightPreview() = MiniPreview(dark = false)

@Preview(name = "Cast v5 · line, dark")
@Composable
private fun CastMiniV5DarkPreview() = MiniPreview(dark = true)

@Preview(name = "Cast v5 · sheet, dark", heightDp = 560)
@Composable
private fun CastExpandedV5DarkPreview() {
    VolaV5Theme(dark = true) {
        CastExpandedControllerV5(
            state = previewCast,
            visible = true,
            onDismiss = {},
            onTogglePlayback = {},
            onSeek = {},
            onVolumeChange = {},
            onDisconnect = {},
        )
    }
}

@Preview(name = "Cast v5 · line, light, largest font", fontScale = 2f)
@Composable
private fun CastMiniV5LightPreviewLargeFont() = MiniPreview(dark = false)
