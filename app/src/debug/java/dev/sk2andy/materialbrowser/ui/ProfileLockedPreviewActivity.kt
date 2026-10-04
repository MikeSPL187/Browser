package dev.sk2andy.materialbrowser.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import dev.sk2andy.materialbrowser.data.AppearanceSettings
import dev.sk2andy.materialbrowser.data.BrowserAppearanceMode
import dev.sk2andy.materialbrowser.ui.theme.MaterialBrowserTheme
import dev.sk2andy.materialbrowser.ui.theme.setCandyContent

/**
 * Debug-only: «Workspace locked» for the emulator tour, which has no biometrics to lock a
 * workspace with. Either button closes it.
 */
internal class ProfileLockedPreviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // The board's «Work» workspace, named in the tour's language.
        val russian = resources.configuration.locales[0].language == "ru"
        val workspace = ProfileLockedPreviewWorkspace.copy(name = if (russian) "Работа" else "Work")
        setCandyContent(animationsEnabled = false) {
            MaterialBrowserTheme(
                settings = AppearanceSettings(appearanceMode = BrowserAppearanceMode.System),
                workspaceAccent = workspace.accent,
            ) {
                ProfileLockedOverlay(
                    workspace = workspace,
                    unlockAvailable = true,
                    canSwitchProfile = true,
                    onUnlock = { finish() },
                    onSwitchProfile = { finish() },
                )
            }
        }
    }
}
