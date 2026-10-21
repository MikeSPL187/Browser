package dev.sk2andy.materialbrowser.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.browser.BrowserController
import dev.sk2andy.materialbrowser.browser.HttpsOnlyMode
import dev.sk2andy.materialbrowser.browser.integration.PasswordsActivityContract
import dev.sk2andy.materialbrowser.shared.ui.icons.VolaIcons
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Motion
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Tile
import dev.sk2andy.materialbrowser.ui.theme.v5.VolaV5Theme
import dev.sk2andy.materialbrowser.ui.v5.V5FirstRunDefaultBrowser
import dev.sk2andy.materialbrowser.ui.v5.V5FirstRunGestures
import dev.sk2andy.materialbrowser.ui.v5.V5FirstRunPromise
import dev.sk2andy.materialbrowser.ui.v5.V5FirstRunWelcome

/** Whether the first run covers the browser, which then leaves the accessibility tree as under a dialog. */
internal val LocalBrowserUnderFirstRun = compositionLocalOf { false }

/** Where the first run is. */
internal enum class FirstRunStage {
    Welcome,
    Setup,
    Gestures,

    /** v5 only (canvas screen «п2»): «Open links in Vola?», in place of setup. */
    DefaultBrowser,

    /** v5 only (canvas screen «п3»): the two gestures of the bar, in place of the v4 lesson. */
    GestureSheet,
}

internal object FirstRunRules {
    /** A new install starts with the welcome; an update with a new lesson starts with the lesson. */
    fun firstStage(showIntro: Boolean): FirstRunStage = if (showIntro) FirstRunStage.Welcome else FirstRunStage.Gestures

    /** After setup: the lesson when it is wanted, otherwise the first run is over (null). */
    fun afterSetup(showGestures: Boolean): FirstRunStage? = FirstRunStage.Gestures.takeIf { showGestures }

    /**
     * Where «Back» goes: from setup to the welcome, from the lesson to setup when the first run
     * showed it. Null leaves: on the welcome the system closes Vola (the first run comes back on
     * the next launch), and the lesson on its own (after an update or from settings) is skipped.
     */
    fun back(stage: FirstRunStage, showIntro: Boolean): FirstRunStage? = when (stage) {
        FirstRunStage.Welcome -> null
        FirstRunStage.Setup, FirstRunStage.DefaultBrowser -> FirstRunStage.Welcome
        // The sheet is the last word: «Back» closes it as «Got it» does.
        FirstRunStage.GestureSheet -> null
        FirstRunStage.Gestures -> FirstRunStage.Setup.takeIf { showIntro }
    }

    /**
     * How many steps the first run counts: the welcome, setup and, while it is switched on, the
     * gesture lesson. The welcome counts with the switch as it stands, on at first, so the
     * welcome and setup always agree.
     */
    fun stepCount(showGestures: Boolean): Int = if (showGestures) 3 else 2

    /**
     * The overlay catches «Back» on setup, «п2» and «п3»: the welcome lets it through, the lesson
     * has its own.
     */
    fun handlesBack(stage: FirstRunStage): Boolean =
        stage == FirstRunStage.Setup || stage == FirstRunStage.DefaultBrowser || stage == FirstRunStage.GestureSheet

    /**
     * v5 (V28, B1): after the welcome «Open links in Vola?», skipped when Vola already is the
     * default browser, then the two-gesture sheet. Nothing else is asked: protection is on from
     * the start and the theme follows the system.
     */
    fun v5AfterWelcome(isDefaultBrowser: Boolean): FirstRunStage =
        if (isDefaultBrowser) FirstRunStage.GestureSheet else FirstRunStage.DefaultBrowser

    /**
     * Where v5 starts: the welcome on a new install. After an update that brings a new lesson, or
     * «Show gesture lesson» in developer options, only v5's own two gestures: someone who uses Vola
     * already needs that one step, not the welcome and «Move from another browser» again (FR-04).
     */
    fun v5FirstStage(showIntro: Boolean): FirstRunStage =
        if (showIntro) FirstRunStage.Welcome else FirstRunStage.GestureSheet
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
    if (rememberDrawsV5()) {
        V5FirstRunOverlay(controller, showIntro, onCompleted)
        return
    }
    val context = LocalContext.current
    var stage by rememberSaveable { mutableStateOf(FirstRunRules.firstStage(showIntro)) }
    var showGestures by rememberSaveable { mutableStateOf(true) }
    BackHandler(enabled = FirstRunRules.handlesBack(stage)) {
        FirstRunRules.back(stage, showIntro)?.let { stage = it }
    }
    AnimatedContent(targetState = stage, label = "first_run") { current ->
        when (current) {
            FirstRunStage.Welcome -> FirstRunWelcomeScreen(
                stepCount = FirstRunRules.stepCount(showGestures),
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
                onBack = { FirstRunRules.back(FirstRunStage.Gestures, showIntro)?.let { stage = it } ?: onCompleted() },
                showWelcome = !showIntro,
            )
            // v5 only; the v4 first run never reaches them.
            FirstRunStage.DefaultBrowser, FirstRunStage.GestureSheet -> Unit
        }
    }
}

/**
 * The v5 first run on a phone (V28, B1; canvas screens «п1»–«п3»): the welcome with three promises,
 * «Open links in Vola?» unless Vola already is the default browser, then the two gestures of the
 * bar over the browser.
 */
@Composable
private fun V5FirstRunOverlay(controller: BrowserController, showIntro: Boolean, onCompleted: () -> Unit) {
    val context = LocalContext.current
    var stage by rememberSaveable { mutableStateOf(FirstRunRules.v5FirstStage(showIntro)) }
    // «Make default» waits for the answer: the page moves on once Vola is the default browser, and
    // a refusal leaves it in place, where «Not now» still goes on.
    var askedForDefault by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(askedForDefault, controller.isDefaultBrowser) {
        if (askedForDefault && controller.isDefaultBrowser && stage == FirstRunStage.DefaultBrowser) {
            stage = FirstRunStage.GestureSheet
        }
    }
    BackHandler(enabled = FirstRunRules.handlesBack(stage)) {
        FirstRunRules.back(stage, showIntro)?.let { stage = it } ?: onCompleted()
    }
    val promises = listOf(
        V5FirstRunPromise(
            icon = VolaIcons.VerifiedUser,
            tile = V5Tile.Green,
            title = stringResource(R.string.v5_first_run_protection_title),
            // The system WebView has no HTTPS-only mode: the page promises only what the engine does.
            caption = stringResource(
                if (controller.isHttpsOnlySupported) {
                    R.string.v5_first_run_protection_caption
                } else {
                    R.string.v5_first_run_protection_caption_no_https
                },
            ),
        ),
        V5FirstRunPromise(
            icon = VolaIcons.Workspaces,
            tile = V5Tile.Indigo,
            title = stringResource(R.string.v5_first_run_profiles_title),
            caption = stringResource(R.string.v5_first_run_profiles_caption),
        ),
        V5FirstRunPromise(
            icon = VolaIcons.Lock,
            tile = V5Tile.Gray,
            title = stringResource(R.string.v5_first_run_device_title),
            caption = stringResource(R.string.v5_first_run_device_caption),
        ),
    )
    val dark = MaterialTheme.colorScheme.background.luminance() < V5_FIRST_RUN_DARK_LUMINANCE
    VolaV5Theme(dark = dark, accent = controller.v5Accent) {
        // Without system animations the pages only cross-fade (plan-v5.md, section 5).
        val animated = V5Motion.animationsEnabled
        AnimatedContent(
            targetState = stage,
            transitionSpec = {
                if (animated) {
                    fadeIn(V5Motion.effects.spec()) togetherWith fadeOut(V5Motion.effects.spec())
                } else {
                    fadeIn(V5Motion.reducedFade()) togetherWith fadeOut(V5Motion.reducedFade())
                }
            },
            label = "v5_first_run",
        ) { current ->
            when (current) {
                FirstRunStage.DefaultBrowser -> V5FirstRunDefaultBrowser(
                    onMakeDefault = {
                        askedForDefault = true
                        controller.openDefaultBrowserSettings()
                    },
                    onNotNow = { stage = FirstRunStage.GestureSheet },
                    refused = askedForDefault && !controller.isDefaultBrowser,
                )
                FirstRunStage.GestureSheet -> V5FirstRunGestures(onDone = onCompleted)
                else -> V5FirstRunWelcome(
                    promises = promises,
                    onStart = { stage = FirstRunRules.v5AfterWelcome(controller.isDefaultBrowser) },
                    // «Move from another browser» opens over the welcome, which waits for the user to come back.
                    onImport = { context.startActivity(PasswordsActivityContract.importIntent(context)) },
                )
            }
        }
    }
}

private const val V5_FIRST_RUN_DARK_LUMINANCE = 0.5f
