package dev.sk2andy.materialbrowser.ui

import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import dev.sk2andy.materialbrowser.browser.StartupTimeline

/**
 * Times the first composition of [content] for the cold-start timeline (stage S3c): how long
 * composing it took and when that composition was applied. Logged once per process, and only in
 * debug builds; a release build pays two clock reads.
 */
@Composable
internal inline fun StartupComposition(name: String, content: @Composable () -> Unit) {
    val start = SystemClock.uptimeMillis()
    content()
    StartupTimeline.duration("Compose.$name", SystemClock.uptimeMillis() - start)
    SideEffect { StartupTimeline.mark("Composed.$name") }
}

/**
 * False while the first frame is drawn, true from the second (stage S3c): what the first screen
 * does not show, such as the hidden tab overview or settings, is composed a frame later instead of
 * delaying the first frame. The first frame's own callback still runs before its draw, so the
 * switch waits for the frame after it.
 */
@Composable
internal fun rememberAfterFirstFrame(): Boolean {
    StartupTimeline.mark("BrowserScreenComposeStart")
    var after by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        withFrameNanos { }
        withFrameNanos { }
        after = true
    }
    return after
}
