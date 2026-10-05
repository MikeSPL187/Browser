package dev.sk2andy.materialbrowser.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import dev.sk2andy.materialbrowser.browser.BrowserController
import dev.sk2andy.materialbrowser.browser.HttpsOnlyMode
import dev.sk2andy.materialbrowser.browser.integration.PasswordsActivityContract

/** Where the first run is. */
internal enum class FirstRunStage { Welcome, Setup, Gestures }

internal object FirstRunRules {
    /** A new install starts with the welcome; an update with a new lesson starts with the lesson. */
    fun firstStage(showIntro: Boolean): FirstRunStage = if (showIntro) FirstRunStage.Welcome else FirstRunStage.Gestures

    /** After setup: the lesson when it is wanted, otherwise the first run is over (null). */
    fun afterSetup(showGestures: Boolean): FirstRunStage? = FirstRunStage.Gestures.takeIf { showGestures }

    /** «Back» from setup returns to the welcome; elsewhere it stays put. */
    fun back(stage: FirstRunStage): FirstRunStage = if (stage == FirstRunStage.Setup) FirstRunStage.Welcome else stage
}

/**
 * The first run (Q23b): on a new install, welcome (board W-Welcome) and setup (W-Setup), then the
 * gesture lesson when it is wanted; after an update that brings a new lesson, only the lesson.
 * Choices on the setup screen take effect at once through the browser's own settings.
 */
@Composable
internal fun FirstRunOverlay(
    controller: BrowserController,
    showIntro: Boolean,
    onCompleted: () -> Unit,
) {
    val context = LocalContext.current
    var stage by rememberSaveable { mutableStateOf(FirstRunRules.firstStage(showIntro)) }
    var showGestures by rememberSaveable { mutableStateOf(true) }
    // The first run is not left with «Back»; on the setup screen it goes back to the welcome.
    BackHandler(enabled = stage != FirstRunStage.Gestures) {
        stage = FirstRunRules.back(stage)
    }
    AnimatedContent(targetState = stage, label = "first_run") { current ->
        when (current) {
            FirstRunStage.Welcome -> FirstRunWelcomeScreen(
                onStart = { stage = FirstRunStage.Setup },
                // «Move to Vola» opens over the welcome, which waits for the user to come back.
                onImport = { context.startActivity(PasswordsActivityContract.importIntent(context)) },
            )
            FirstRunStage.Setup -> FirstRunSetupScreen(
                setup = FirstRunSetup(
                    appearanceMode = controller.appearanceSettings.appearanceMode,
                    trackerProtection = controller.blockerSettings.blockAdsAndTrackers,
                    httpsOnly = (controller.httpsOnlyMode == HttpsOnlyMode.Always)
                        .takeIf { controller.isHttpsOnlySupported },
                    showGestures = showGestures,
                    isDefaultBrowser = controller.isDefaultBrowser,
                ),
                onAppearanceModeChange = { mode ->
                    controller.updateAppearanceSettings(controller.appearanceSettings.copy(appearanceMode = mode))
                },
                onTrackerProtectionChange = { on ->
                    controller.updateBlockerSettings(controller.blockerSettings.copy(blockAdsAndTrackers = on))
                },
                onHttpsOnlyChange = { on ->
                    controller.updateHttpsOnlyMode(if (on) HttpsOnlyMode.Always else HttpsOnlyMode.Off)
                },
                onShowGesturesChange = { showGestures = it },
                onMakeDefault = controller::openDefaultBrowserSettings,
                onNext = { FirstRunRules.afterSetup(showGestures)?.let { stage = it } ?: onCompleted() },
            )
            FirstRunStage.Gestures -> GestureOnboardingScreen(
                onCompleted = onCompleted,
                showWelcome = !showIntro,
            )
        }
    }
}
