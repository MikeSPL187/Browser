package dev.sk2andy.materialbrowser

import android.content.Context
import androidx.compose.runtime.Composable
import dev.sk2andy.materialbrowser.diagnostics.CrashReportPrompt

/** The prompts a launch may bring up once onboarding and release notes are out of the way. */
@Composable
internal fun LaunchPrompts(
    context: Context,
    visible: Boolean,
    onOpenReleaseNotes: (String) -> Boolean,
) {
    CrashReportPrompt(visible = visible)
    AppUpdatePrompt(
        context = context,
        visible = visible,
        onOpenReleaseNotes = onOpenReleaseNotes,
    )
}
