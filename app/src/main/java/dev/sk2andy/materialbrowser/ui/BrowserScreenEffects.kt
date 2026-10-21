package dev.sk2andy.materialbrowser.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.getValue
import kotlinx.coroutines.delay

/** A tab changed from a hardware keyboard ([requestId] above 0): the editor and a pending command go. */
@Composable
internal fun HardwareTabChangeEffect(requestId: Int, onChange: () -> Unit) {
    val change by rememberUpdatedState(onChange)
    LaunchedEffect(requestId) { if (requestId > 0) change() }
}

/**
 * The overview opens after the page's snapshot is taken. Should the engine never answer, it opens
 * anyway after [OVERVIEW_OPENING_TIMEOUT_MILLIS] instead of staying shut for good: the opening flag
 * would otherwise refuse every later open.
 */
@Composable
internal fun OverviewOpeningWatchdog(opening: Boolean, onTimeout: () -> Unit) {
    val timeout by rememberUpdatedState(onTimeout)
    LaunchedEffect(opening) {
        if (!opening) return@LaunchedEffect
        delay(OVERVIEW_OPENING_TIMEOUT_MILLIS)
        timeout()
    }
}

/** Far longer than a snapshot takes (tens of milliseconds), short enough not to feel stuck. */
internal const val OVERVIEW_OPENING_TIMEOUT_MILLIS = 2_000L
