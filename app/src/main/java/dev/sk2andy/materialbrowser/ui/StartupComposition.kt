package dev.sk2andy.materialbrowser.ui

import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
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
