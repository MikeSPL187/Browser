package dev.sk2andy.materialbrowser.audit

import android.app.Activity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import dev.sk2andy.materialbrowser.MainActivity
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.grantNotificationPermissionForTests
import dev.sk2andy.materialbrowser.settings.SettingsRegistry
import dev.sk2andy.materialbrowser.shared.ui.BrowserMainMenuTestTags
import dev.sk2andy.materialbrowser.shared.ui.TabOverviewChromeTestTags
import dev.sk2andy.materialbrowser.ui.AddressBarTestTags
import dev.sk2andy.materialbrowser.ui.NewTabPageTestTags
import dev.sk2andy.materialbrowser.ui.ReleaseNotesTestTags
import dev.sk2andy.materialbrowser.ui.SettingsDestination
import dev.sk2andy.materialbrowser.ui.SettingsSearchTestTags
import java.util.concurrent.atomic.AtomicReference
import org.junit.After
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * A tester's walk through the real app: launch, then every screen reached the way a user reaches
 * it (taps on the address bar, the menu, the tab counter, the settings rows), each one checked
 * by [ScreenAuditor] from top to bottom. Every group runs in four configurations — English and
 * Russian, light and dark, normal and double font size — so each screen is gone through four
 * times. A screen the walk cannot reach is a finding too. The test fails with the full list.
 */
@RunWith(AndroidJUnit4::class)
class AppScreenWalkInstrumentedTest {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    private val findings = mutableListOf<AuditFinding>()
    private val visited = mutableListOf<String>()
    private lateinit var config: AuditConfig
    private lateinit var scenario: ActivityScenario<MainActivity>
    private val auditor = ScreenAuditor(composeRule) { currentActivity() }

    @After
    fun tearDown() {
        if (::scenario.isInitialized) runCatching { scenario.close() }
        AuditEnvironment.reset(context)
    }

    @Test fun browserScreensEnglishLight() = walk(AuditConfig.EnglishLight, ::walkBrowser)

    @Test fun browserScreensRussianDark() = walk(AuditConfig.RussianDark, ::walkBrowser)

    @Test fun browserScreensEnglishDarkLargeFont() = walk(AuditConfig.EnglishDarkLargeFont, ::walkBrowser)

    @Test fun browserScreensRussianLightLargeFont() = walk(AuditConfig.RussianLightLargeFont, ::walkBrowser)

    @Test fun settingsFirstHalfEnglishLight() = walk(AuditConfig.EnglishLight) { walkSettings(FIRST_HALF) }

    @Test fun settingsFirstHalfRussianDark() = walk(AuditConfig.RussianDark) { walkSettings(FIRST_HALF) }

    @Test fun settingsFirstHalfEnglishDarkLargeFont() =
        walk(AuditConfig.EnglishDarkLargeFont) { walkSettings(FIRST_HALF) }

    @Test fun settingsFirstHalfRussianLightLargeFont() =
        walk(AuditConfig.RussianLightLargeFont) { walkSettings(FIRST_HALF) }

    @Test fun settingsSecondHalfEnglishLight() = walk(AuditConfig.EnglishLight) { walkSettings(SECOND_HALF) }

    @Test fun settingsSecondHalfRussianDark() = walk(AuditConfig.RussianDark) { walkSettings(SECOND_HALF) }

    @Test fun settingsSecondHalfEnglishDarkLargeFont() =
        walk(AuditConfig.EnglishDarkLargeFont) { walkSettings(SECOND_HALF) }

    @Test fun settingsSecondHalfRussianLightLargeFont() =
        walk(AuditConfig.RussianLightLargeFont) { walkSettings(SECOND_HALF) }

    private fun walk(auditConfig: AuditConfig, steps: () -> Unit) {
        config = auditConfig
        AuditEnvironment.enter(context, config)
        grantNotificationPermissionForTests()
        scenario = ActivityScenario.launch(MainActivity::class.java)
        dismissReleaseNotes()
        step("new tab") {
            awaitTag(NewTabPageTestTags.Header)
            auditScrolling("new tab")
        }
        steps()
        if (findings.isNotEmpty()) {
            fail(
                "${findings.size} findings in ${config.name}; screens: $visited\n" +
                    findings.joinToString("\n"),
            )
        }
    }

    /** New tab, address editor, menus, tab overview and a private tab. */
    private fun walkBrowser() {
        step("address editor") {
            click(hasTestTag(AddressBarTestTags.PrimaryField))
            awaitTag(AddressBarTestTags.Editor)
            audit("address editor, empty")
            composeRule.onAllNodes(hasSetTextAction()).onFirst().performTextInput("vola test")
            audit("address editor, typed")
            backUntilGone(AddressBarTestTags.Editor)
        }
        step("main menu") {
            click(hasContentDescription(string(R.string.cd_more_options)))
            awaitTag(BrowserMainMenuTestTags.Menu)
            audit("main menu")
            click(hasTestTag(BrowserMainMenuTestTags.More))
            awaitTag(BrowserMainMenuTestTags.MoreGroup)
            audit("main menu, more")
            backUntilGone(BrowserMainMenuTestTags.Menu)
        }
        step("tab overview") {
            click(hasTestTag(AddressBarTestTags.TabButton))
            awaitTag(TabOverviewChromeTestTags.Root)
            auditScrolling("tab overview")
            backUntilGone(TabOverviewChromeTestTags.Root)
        }
        step("private tab") {
            scenario.onActivity { activity ->
                activity.browserControllerForTesting().createTab(isIncognito = true)
            }
            composeRule.waitForIdle()
            auditScrolling("private tab")
        }
    }

    /** The settings home, then every page on it, top to bottom, and back. */
    private fun walkSettings(half: Int) {
        step("settings home") {
            openSettings()
            auditScrolling("settings home")
        }
        val pages = SettingsRegistry.pages
            .filter { page -> page.destination.parent == SettingsDestination.Home }
        val chosen = pages.chunked((pages.size + 1) / 2).getOrElse(half) { emptyList() }
        for (page in chosen) {
            val title = string(page.title)
            step("settings: $title") {
                ensureSettingsHome()
                composeRule.onAllNodes(hasText(title) and hasClickAction()).onFirst()
                    .performScrollTo()
                    .performClick()
                composeRule.waitForIdle()
                auditScrolling("settings: $title")
                device.pressBack()
                composeRule.waitForIdle()
            }
        }
        if (half == SECOND_HALF) {
            step("settings search") {
                ensureSettingsHome()
                click(hasTestTag(SettingsSearchTestTags.Open))
                awaitTag(SettingsSearchTestTags.Field)
                audit("settings search, empty")
                composeRule.onAllNodes(hasSetTextAction()).onFirst()
                    .performTextInput(if (config.languageTag.startsWith("ru")) "вкладки" else "tabs")
                composeRule.waitForIdle()
                auditScrolling("settings search, results")
                device.pressBack()
            }
        }
    }

    private fun openSettings() {
        click(hasContentDescription(string(R.string.cd_more_options)))
        awaitTag(BrowserMainMenuTestTags.Menu)
        click(hasText(string(R.string.action_settings)) and hasClickAction())
        awaitTag(SettingsSearchTestTags.Open)
    }

    private fun ensureSettingsHome() {
        if (exists(hasTestTag(SettingsSearchTestTags.Open))) return
        repeat(BACK_ATTEMPTS) {
            device.pressBack()
            composeRule.waitForIdle()
            if (exists(hasTestTag(SettingsSearchTestTags.Open))) return
        }
        if (!exists(hasTestTag(NewTabPageTestTags.Header))) relaunch()
        openSettings()
    }

    /** One step of the walk: a failure to reach the screen is filed, and the walk goes on. */
    private fun step(name: String, body: () -> Unit) {
        visited += name
        try {
            body()
        } catch (error: Throwable) {
            findings += AuditFinding(
                screen = name,
                config = config.name,
                kind = AuditKind.Navigation,
                detail = "${error.javaClass.simpleName}: ${error.message?.lineSequence()?.firstOrNull()}" +
                    " — on screen: ${runCatching { auditor.describeScreen() }.getOrDefault("?")}",
            )
            runCatching { recover() }
        }
    }

    private fun recover() {
        repeat(BACK_ATTEMPTS) {
            if (exists(hasTestTag(NewTabPageTestTags.Header))) return
            device.pressBack()
            composeRule.waitForIdle()
        }
        if (!exists(hasTestTag(NewTabPageTestTags.Header))) relaunch()
    }

    private fun relaunch() {
        runCatching { scenario.close() }
        scenario = ActivityScenario.launch(MainActivity::class.java)
        dismissReleaseNotes()
        awaitTag(NewTabPageTestTags.Header)
    }

    private fun dismissReleaseNotes() {
        composeRule.waitForIdle()
        if (!exists(hasTestTag(ReleaseNotesTestTags.Screen))) return
        step("release notes") {
            auditScrolling("release notes")
            click(hasTestTag(ReleaseNotesTestTags.Done))
            composeRule.waitForIdle()
        }
    }

    private fun audit(screen: String) {
        findings += auditor.audit(screen, config.name)
    }

    /** Audits the screen, then scrolls it a page at a time to the end and audits again. */
    private fun auditScrolling(screen: String) {
        audit(screen)
        repeat(MAX_SCROLL_PAGES) { page ->
            val scrollable = composeRule.onAllNodes(hasScrollAction() and isVerticallyScrollable)
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .maxByOrNull { node -> node.boundsInWindow.height * node.boundsInWindow.width }
                ?: return
            val before = scrollable.config.getOrNull(SemanticsProperties.VerticalScrollAxisRange)?.value?.invoke()
            composeRule.onAllNodes(SemanticsMatcher("scrollable ${scrollable.id}") { it.id == scrollable.id })
                .onFirst()
                .performTouchInput { swipeUp() }
            composeRule.waitForIdle()
            val after = composeRule.onAllNodes(SemanticsMatcher("scrollable ${scrollable.id}") { it.id == scrollable.id })
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .firstOrNull()
                ?.config
                ?.getOrNull(SemanticsProperties.VerticalScrollAxisRange)
                ?.value
                ?.invoke()
            if (after == null || after == before) return
            // Content under the status bar after scrolling is how edge-to-edge lists look.
            findings += auditor.audit("$screen ↓${page + 1}", config.name)
                .filterNot { it.kind == AuditKind.SystemBars }
        }
    }

    private fun click(matcher: SemanticsMatcher) {
        composeRule.waitUntil(TIMEOUT_MILLIS) { exists(matcher) }
        composeRule.onAllNodes(matcher).onFirst().performClick()
        composeRule.waitForIdle()
    }

    private fun awaitTag(tag: String) {
        composeRule.waitUntil(TIMEOUT_MILLIS) { exists(hasTestTag(tag)) }
    }

    /** Presses back the way a user leaves a screen, until the screen is gone. */
    private fun backUntilGone(tag: String) {
        repeat(BACK_ATTEMPTS) {
            if (!exists(hasTestTag(tag))) return
            device.pressBack()
            composeRule.waitForIdle()
        }
        composeRule.waitUntil(TIMEOUT_MILLIS) { !exists(hasTestTag(tag)) }
    }

    private fun exists(matcher: SemanticsMatcher): Boolean = runCatching {
        composeRule.onAllNodes(matcher).fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty()
    }.getOrDefault(false)

    private fun string(id: Int): String = currentActivity().getString(id)

    private fun currentActivity(): Activity {
        val activity = AtomicReference<Activity>()
        scenario.onActivity { activity.set(it) }
        return activity.get()
    }

    private companion object {
        const val FIRST_HALF = 0
        const val SECOND_HALF = 1
        const val TIMEOUT_MILLIS = 10_000L
        const val BACK_ATTEMPTS = 3
        const val MAX_SCROLL_PAGES = 8
        val isVerticallyScrollable = SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange)
    }
}
