package dev.sk2andy.materialbrowser.ui

import android.app.Activity
import android.content.res.Resources
import android.os.Build
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.data.AppearanceSettings
import dev.sk2andy.materialbrowser.data.BrowserAppearanceMode
import dev.sk2andy.materialbrowser.data.BrowserSessionStore

/**
 * The system launch screen is the only splash (board W-Splash): the Vola mark on the theme's
 * background (`windowSplashScreen*` in the app theme). With the startup animation on it fades out
 * while the mark grows a little; otherwise it leaves at once and the browser shows on its first
 * frame. Nothing is drawn over the browser afterwards.
 */
internal object LaunchScreen {
    private const val EXIT_MILLIS = 320L
    private const val EXIT_ICON_SCALE = 1.25f

    fun installExit(activity: Activity, store: BrowserSessionStore, appearance: AppearanceSettings) {
        val animated = CandyAnimationRules.startupAnimationEnabled(
            animationsEnabled = appearance.animationsEnabled,
            startupAnimationEnabled = store.loadStartupAnimationEnabled(),
        )
        // The system draws the launch screen before the app runs: on Android 13+ it can remember a
        // light or dark one for a theme fixed in Vola, so a dark Vola never opens on white.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            activity.splashScreen.setSplashScreenTheme(
                when (appearance.appearanceMode) {
                    BrowserAppearanceMode.System -> Resources.ID_NULL
                    BrowserAppearanceMode.Light -> R.style.Theme_Vola_LaunchLight
                    BrowserAppearanceMode.Dark -> R.style.Theme_Vola_LaunchDark
                },
            )
        }
        activity.splashScreen.setOnExitAnimationListener { launchScreen ->
            if (!animated) {
                launchScreen.remove()
                return@setOnExitAnimationListener
            }
            launchScreen.iconView?.animate()
                ?.scaleX(EXIT_ICON_SCALE)
                ?.scaleY(EXIT_ICON_SCALE)
                ?.setDuration(EXIT_MILLIS)
                ?.start()
            launchScreen.animate()
                .alpha(0f)
                .setDuration(EXIT_MILLIS)
                .withEndAction { launchScreen.remove() }
                .start()
        }
    }
}
