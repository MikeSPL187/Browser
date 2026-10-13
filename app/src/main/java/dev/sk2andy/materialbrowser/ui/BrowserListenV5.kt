package dev.sk2andy.materialbrowser.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import dev.sk2andy.materialbrowser.reader.ListenRules
import dev.sk2andy.materialbrowser.reader.ListenSession
import dev.sk2andy.materialbrowser.reader.ReaderSpeechStatus
import dev.sk2andy.materialbrowser.ui.v5.V5ListenAccessory
import dev.sk2andy.materialbrowser.ui.v5.V5ListenRowState

/** V19b: the browser-wide [ListenSession] as the mini player above the v5 bar. */
@Composable
internal fun BrowserListenAccessory(selectedTabPrivate: Boolean) {
    val context = LocalContext.current
    val session = remember(context) { ListenSession.get(context) }
    val item = session.item
    val status = session.state.status
    LaunchedEffect(item, status) {
        // A finished article lets the voice go.
        if (item != null && status == ReaderSpeechStatus.Ready) session.close()
    }
    val state = item?.takeIf { ListenRules.showsPlayer(it, status) }?.let { shown ->
        V5ListenRowState(
            title = shown.title.ifBlank { shown.sourceUrl }
                .takeIf { ListenRules.showsTitle(shown, selectedTabPrivate) },
            speaking = status == ReaderSpeechStatus.Speaking || status == ReaderSpeechStatus.Initializing,
            unavailable = status == ReaderSpeechStatus.Unavailable,
        )
    }
    V5ListenAccessory(
        state = state,
        onPlayPause = { if (state?.speaking == true) session.pause() else session.resume() },
        onClose = session::close,
    )
}
