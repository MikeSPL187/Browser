package dev.sk2andy.materialbrowser.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import dev.sk2andy.materialbrowser.browser.permissions.PermissionPrompt
import dev.sk2andy.materialbrowser.browser.permissions.PermissionSiteKey
import dev.sk2andy.materialbrowser.browser.permissions.SitePermission
import dev.sk2andy.materialbrowser.data.AppearanceSettings
import dev.sk2andy.materialbrowser.data.BrowserAppearanceMode
import dev.sk2andy.materialbrowser.ui.theme.MaterialBrowserTheme
import dev.sk2andy.materialbrowser.ui.theme.setCandyContent

/**
 * Debug-only: the permission request sheet for the emulator tour, which has no page of its own
 * that asks for a permission. Any choice closes it.
 */
internal class PermissionPromptPreviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setCandyContent(animationsEnabled = false) {
            MaterialBrowserTheme(
                settings = AppearanceSettings(appearanceMode = BrowserAppearanceMode.System),
            ) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
                    PermissionPromptSheet(
                        prompt = PermissionPrompt(
                            id = 1L,
                            tabId = "preview",
                            site = PermissionSiteKey(profileId = "preview", origin = "https://maps.example.com"),
                            permissions = setOf(SitePermission.Location),
                            isPrivate = false,
                        ),
                        onChoice = { finish() },
                    )
                }
            }
        }
    }
}
