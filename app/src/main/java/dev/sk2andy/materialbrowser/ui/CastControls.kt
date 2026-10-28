package dev.sk2andy.materialbrowser.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import dev.sk2andy.materialbrowser.browser.cast.CastUiState
internal object CastControlsTestTags {
    const val RouteButton = "cast_route_button"
    const val MiniController = "cast_mini_controller"
    const val PlayPause = "cast_play_pause"
    const val Disconnect = "cast_disconnect"
    const val ExpandedController = "cast_expanded_controller"
    const val Seek = "cast_seek"
    const val Volume = "cast_volume"
}

@Composable
internal fun CastControls(
    state: CastUiState,
    onTogglePlayback: () -> Unit,
    onSeek: (Long) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onDisconnect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!state.isConnected || !state.hasMedia) return
    var expanded by rememberSaveable { mutableStateOf(false) }
    V5PromptTheme {
        CastMiniControllerV5(state, { expanded = true }, onTogglePlayback, onDisconnect, modifier)
        CastExpandedControllerV5(
            state = state,
            visible = expanded,
            onDismiss = { expanded = false },
            onTogglePlayback = onTogglePlayback,
            onSeek = onSeek,
            onVolumeChange = onVolumeChange,
            onDisconnect = onDisconnect,
        )
    }
}

internal fun formattedMediaTime(millis: Long): String {
    val totalSeconds = millis.coerceAtLeast(0L) / 1_000L
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}
