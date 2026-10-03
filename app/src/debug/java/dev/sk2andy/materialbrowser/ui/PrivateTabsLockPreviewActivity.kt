package dev.sk2andy.materialbrowser.ui

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import dev.sk2andy.materialbrowser.data.AppearanceSettings
import dev.sk2andy.materialbrowser.data.BrowserAppearanceMode
import dev.sk2andy.materialbrowser.ui.theme.MaterialBrowserTheme
import dev.sk2andy.materialbrowser.ui.theme.setCandyContent

/**
 * Debug-only: the «Private tabs locked» screen for the emulator tour. The emulator has no strong
 * biometric, so real private tabs can never be locked there; this shows the same composable.
 */
internal class PrivateTabsLockPreviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Private tabs are always dark, as MainActivity draws them: light system bar icons.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        val unlockAvailable = !intent.getBooleanExtra(EXTRA_UNAVAILABLE, false)
        setCandyContent(animationsEnabled = false) {
            MaterialBrowserTheme(
                settings = AppearanceSettings(appearanceMode = BrowserAppearanceMode.System),
                privateMode = true,
            ) {
                PrivateTabsLockedScreen(
                    privateTabCount = PREVIEW_PRIVATE_TAB_COUNT,
                    unlockAvailable = unlockAvailable,
                    onShown = {},
                    onUnlock = {},
                    onRegularTabs = ::finish,
                    onCloseAll = ::finish,
                )
            }
        }
    }

    companion object {
        const val EXTRA_UNAVAILABLE = "privateTabsLockUnavailable"
        private const val PREVIEW_PRIVATE_TAB_COUNT = 2
    }
}
