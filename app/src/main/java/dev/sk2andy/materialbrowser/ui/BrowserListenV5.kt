package dev.sk2andy.materialbrowser.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.browser.BrowserController
import dev.sk2andy.materialbrowser.reader.ListenPreparation
import dev.sk2andy.materialbrowser.reader.ListenRules
import dev.sk2andy.materialbrowser.reader.ListenSession
import dev.sk2andy.materialbrowser.reader.ReaderExtractionResult
import dev.sk2andy.materialbrowser.reader.ReaderSpeechStatus
import dev.sk2andy.materialbrowser.ui.v5.V5ListenAccessory
import dev.sk2andy.materialbrowser.ui.v5.V5ListenRowState
import dev.sk2andy.materialbrowser.ui.v5.V5ListenTiming
import java.text.NumberFormat
import kotlin.math.roundToInt

/** V19b: the browser-wide [ListenSession] as the mini player above the v5 bar. */
@Composable
internal fun BrowserListenAccessory(selectedTabPrivate: Boolean) {
    val context = LocalContext.current
    val session = remember(context) { ListenSession.get(context) }
    val item = session.item
    val status = session.state.status
    val preparation = session.preparation
    LaunchedEffect(item, status, preparation) {
        // A finished article lets the voice go.
        if (item != null && preparation == null && status == ReaderSpeechStatus.Ready) session.close()
    }
    val message = when (preparation) {
        ListenPreparation.Preparing -> stringResource(R.string.listen_preparing)
        is ListenPreparation.Failed ->
            stringResource(ReaderExtractionResult.Failure(preparation.reason).readerActionMessageRes())
        null -> if (status == ReaderSpeechStatus.Unavailable) stringResource(R.string.listen_unavailable) else null
    }
    val locale = LocalConfiguration.current.locales[0]
    val speed = remember(session.speed, locale) {
        NumberFormat.getNumberInstance(locale).apply { maximumFractionDigits = 1 }.format(session.speed.toDouble())
    }
    val text = item?.text.orEmpty()
    val duration = ListenRules.durationSeconds(text, session.speed)
    val elapsed = (duration * ListenRules.progress(text, session.state.characterOffset)).roundToInt()
    val timing = if (message == null && duration > 0) {
        V5ListenTiming(
            progress = ListenRules.progress(text, session.state.characterOffset),
            text = stringResource(R.string.v5_listen_time, ListenRules.clock(elapsed), ListenRules.clock(duration)),
            description = stringResource(
                R.string.v5_listen_time_description,
                ListenRules.clock(elapsed),
                ListenRules.clock(duration),
            ),
        )
    } else {
        null
    }
    val state = item?.takeIf { ListenRules.showsPlayer(it, status, preparation) }?.let { shown ->
        V5ListenRowState(
            title = shown.title.ifBlank { shown.sourceUrl }
                .takeIf { ListenRules.showsTitle(shown, selectedTabPrivate) },
            speaking = status == ReaderSpeechStatus.Speaking || status == ReaderSpeechStatus.Initializing,
            canPlay = message == null,
            message = message,
            timing = timing,
            speed = speed.takeIf { message == null },
        )
    }
    V5ListenAccessory(
        state = state,
        onPlayPause = { if (state?.speaking == true) session.pause() else session.resume() },
        onClose = session::close,
        onSpeed = session::nextSpeed,
    )
}

/** V19c: «Listen» in the v5 menu: the page's article read aloud without opening the reader. */
internal fun listenToSelectedPage(controller: BrowserController, session: ListenSession, title: String) {
    val tab = controller.selectedTab
    val id = session.prepare(title, tab.url, tab.isIncognito)
    controller.extractSelectedPageForReader { result ->
        when (result) {
            is ReaderExtractionResult.Success -> session.prepared(id, result.document)
            is ReaderExtractionResult.Failure -> session.failed(id, result.reason)
        }
    }
}
