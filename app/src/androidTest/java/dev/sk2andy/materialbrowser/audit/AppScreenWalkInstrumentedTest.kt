package dev.sk2andy.materialbrowser.audit

import android.app.Activity
import androidx.compose.ui.semantics.Role
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
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import dev.sk2andy.materialbrowser.MainActivity
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.grantNotificationPermissionForTests
import dev.sk2andy.materialbrowser.settings.SettingsRegistry
import dev.sk2andy.materialbrowser.shared.ui.BrowserMainMenuTestTags
import dev.sk2andy.materialbrowser.shared.ui.TabOverviewChromeTestTags
import dev.sk2andy.materialbrowser.ui.AddressBarTestTags
import dev.sk2andy.materialbrowser.ui.AppearanceMainTestTags
import dev.sk2andy.materialbrowser.ui.FirstRunTestTags
import dev.sk2andy.materialbrowser.ui.NewTabPageTestTags
import dev.sk2andy.materialbrowser.ui.ReleaseNotesTestTags
import dev.sk2andy.materialbrowser.ui.SettingsDestination
import dev.sk2andy.materialbrowser.ui.SettingsSearchTestTags
import dev.sk2andy.materialbrowser.ui.TabSettingsTestTags
import dev.sk2andy.materialbrowser.ui.UserscriptManagementTestTags
import java.util.concurrent.atomic.AtomicReference
import org.junit.After
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * A tester's walk through the real app: launch, then every screen reached the way a user reaches
 * it (taps on the address bar, the menu, the tab counter, the settings rows), each one checked
 * by [ScreenAuditor] from top to bottom. Every group runs in four configurations — English and
 * Russian, light and dark, normal and double font size — so each screen is gone through four
 * times. A screen the walk cannot reach is a finding too. The test fails with the full list.
 */
@RunWith(Parameterized::class)
class AppScreenWalkInstrumentedTest(
    /** A walk group: [BROWSER_GROUP] and the like, a settings slice, or a subpage slice. */
    private val group: Int,
    private val config: AuditConfig,
) {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    private val findings = mutableListOf<AuditFinding>()
    private val visited = mutableListOf<String>()
    private lateinit var scenario: ActivityScenario<MainActivity>
    private val auditor = ScreenAuditor(composeRule) { currentActivity() }

    @After
    fun tearDown() {
        if (::scenario.isInitialized) runCatching { scenario.close() }
        AuditEnvironment.reset(context)
    }

    @Test
    fun walk() {
        when (group) {
            BROWSER_GROUP -> walk(config, steps = ::walkBrowser)
            LIBRARY_GROUP -> walk(config, steps = ::walkLibrary)
            FIRST_RUN_GROUP -> walk(config, firstRun = true, steps = {})
            in subpageGroups() -> walk(config) { walkSubpages(SUBPAGE_GROUP_BASE - group) }
            else -> walk(config) { walkSettings(group) }
        }
    }

    private fun walk(auditConfig: AuditConfig, firstRun: Boolean = false, steps: () -> Unit) {
        AuditEnvironment.enter(context, auditConfig, firstRun)
        grantNotificationPermissionForTests()
        scenario = ActivityScenario.launch(MainActivity::class.java)
        if (firstRun) walkFirstRun()
        dismissReleaseNotes()
        step("launch") { awaitTag(NewTabPageTestTags.Header) }
        steps()
        if (findings.isNotEmpty()) fail(report())
    }

    /** The welcome, then every setup screen through «Next», as a new user goes through them. */
    private fun walkFirstRun() {
        step("first run: welcome") {
            awaitTag(FirstRunTestTags.Welcome)
            auditScrolling("first run: welcome")
            click(hasTestTag(FirstRunTestTags.Start))
        }
        repeat(MAX_FIRST_RUN_SCREENS) { index ->
            if (!exists(hasTestTag(FirstRunTestTags.Next))) return
            step("first run: screen ${index + 1}") {
                auditScrolling("first run: screen ${index + 1}")
                click(hasTestTag(FirstRunTestTags.Next))
            }
        }
    }

    /** Favorites, downloads, history and snoozed tabs, opened from the menu. */
    private fun walkLibrary() {
        listOf(
            BrowserMainMenuTestTags.Favorites to "favorites",
            BrowserMainMenuTestTags.Downloads to "downloads",
            BrowserMainMenuTestTags.History to "history",
            BrowserMainMenuTestTags.SnoozedTabs to "snoozed tabs",
        ).forEach { (tag, screen) ->
            step(screen) {
                openMenuItem(tag)
                composeRule.waitForIdle()
                auditScrolling(screen)
                // Some of these are activities of their own: the browser stays composed under them.
                device.pressBack()
                composeRule.waitForIdle()
                recover()
            }
        }
    }

    /**
     * One line per distinct finding, most serious kinds first, with the screens it was seen on:
     * the same label under the status bar on five screens is one problem, not five.
     */
    private fun report(): String {
        val lines = findings
            .groupBy { finding -> finding.kind to finding.detail }
            .entries
            .sortedWith(compareBy({ it.key.first.ordinal }, { it.key.second }))
            .map { (key, seen) ->
                val screens = seen.map(AuditFinding::screen).distinct()
                "[${key.first}] ${key.second} — on ${screens.size}: ${screens.joinToString()}"
            }
        return "${lines.size} distinct findings in ${config.name}; screens walked: $visited\n" +
            lines.joinToString("\n")
    }

    /** New tab, address editor, menus, tab overview and a private tab. */
    private fun walkBrowser() {
        step("new tab") { auditScrolling("new tab") }
        step("address editor") {
            click(hasTestTag(AddressBarTestTags.PrimaryField))
            awaitShown(closeAddressInput())
            audit("address editor, empty")
            composeRule.onAllNodes(hasSetTextAction()).onFirst().performTextInput("vola test")
            audit("address editor, typed")
            backUntilGone(closeAddressInput())
        }
        step("main menu") {
            click(hasContentDescription(string(R.string.cd_more_options)))
            awaitTag(BrowserMainMenuTestTags.Menu)
            audit("main menu")
            click(hasTestTag(BrowserMainMenuTestTags.More))
            awaitTag(BrowserMainMenuTestTags.MoreGroup)
            audit("main menu, more")
            backUntilGone(hasTestTag(BrowserMainMenuTestTags.Menu))
        }
        step("tab overview") {
            click(hasTestTag(AddressBarTestTags.TabButton))
            awaitTag(TabOverviewChromeTestTags.Root)
            auditScrolling("tab overview")
            backUntilGone(hasTestTag(TabOverviewChromeTestTags.Root))
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
    private fun walkSettings(slice: Int) {
        step("settings home") {
            openSettings()
            if (slice == 0) auditScrolling("settings home")
        }
        val chosen = settingsSlices().getOrElse(slice) { emptyList() }
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
        if (slice == settingsSlices().lastIndex) {
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

    /** Pages one level deeper: their parent page first, then the row that opens them. */
    private fun walkSubpages(slice: Int) {
        step("settings home") { openSettings() }
        for (subpage in subpageSlices().getOrElse(slice) { emptyList() }) {
            val screen = "settings: ${string(subpage.parent)} › ${subpage.name}"
            step(screen) {
                ensureSettingsHome()
                composeRule.onAllNodes(hasText(string(subpage.parent)) and hasClickAction()).onFirst()
                    .performScrollTo()
                    .performClick()
                composeRule.waitForIdle()
                click(subpage.entry(this))
                composeRule.waitForIdle()
                auditScrolling(screen)
            }
        }
    }

    /** The menu's Settings, in its short view or under More, the way a user finds it. */
    private fun openSettings() {
        openMenuItem(BrowserMainMenuTestTags.Settings)
        awaitTag(SettingsSearchTestTags.Open)
    }

    /** Taps a menu item, in the short menu or under More. */
    private fun openMenuItem(tag: String) {
        click(hasContentDescription(string(R.string.cd_more_options)))
        awaitTag(BrowserMainMenuTestTags.Menu)
        if (!exists(hasTestTag(tag))) {
            click(hasTestTag(BrowserMainMenuTestTags.More))
            awaitTag(BrowserMainMenuTestTags.MoreGroup)
        }
        click(hasTestTag(tag))
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
                    " — on screen: ${runCatching { auditor.describeScreen() }.getOrDefault("?")}" +
                    // Back lost to a system window reads like a screen that ignores back.
                    " — focused package: ${runCatching { device.currentPackageName }.getOrDefault("?")}",
            )
            runCatching { recover() }
        }
    }

    private fun recover() {
        repeat(BACK_ATTEMPTS) {
            if (atBareNewTab()) return
            device.pressBack()
            composeRule.waitForIdle()
        }
        if (!atBareNewTab()) relaunch()
    }

    /** The new tab with nothing over it: its header stays in the tree under the editor. */
    private fun atBareNewTab(): Boolean =
        exists(hasTestTag(NewTabPageTestTags.Header)) &&
            !exists(closeAddressInput()) &&
            !exists(hasTestTag(BrowserMainMenuTestTags.Menu)) &&
            !exists(hasTestTag(TabOverviewChromeTestTags.Root))

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
        composeRule.waitUntil("${matcher.description} to tap", TIMEOUT_MILLIS) { exists(matcher) }
        // Scrolled into view first, as a user would: with a large font menu items sit below the fold.
        val target = composeRule.onAllNodes(matcher).onFirst()
        runCatching { target.performScrollTo() }
        target.performClick()
        composeRule.waitForIdle()
    }

    private fun awaitTag(tag: String) {
        composeRule.waitUntil("tag $tag to appear", TIMEOUT_MILLIS) { exists(hasTestTag(tag)) }
    }

    /** Presses back the way a user leaves a screen, until the screen is gone. */
    private fun backUntilGone(screen: SemanticsMatcher) {
        repeat(BACK_ATTEMPTS) {
            if (!exists(screen)) return
            device.pressBack()
            composeRule.waitForIdle()
        }
        composeRule.waitUntil("${screen.description} to close on back", TIMEOUT_MILLIS) { !exists(screen) }
    }

    private fun awaitShown(matcher: SemanticsMatcher) {
        composeRule.waitUntil("${matcher.description} to appear", TIMEOUT_MILLIS) { exists(matcher) }
    }

    /** The open address editor's close button (the scrim behind it has the same label). */
    private fun closeAddressInput(): SemanticsMatcher =
        hasContentDescription(string(R.string.cd_close_address_input)) and
            SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button)

    private fun exists(matcher: SemanticsMatcher): Boolean = runCatching {
        composeRule.onAllNodes(matcher).fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty()
    }.getOrDefault(false)

    private fun string(id: Int): String = currentActivity().getString(id)

    private fun currentActivity(): Activity {
        val activity = AtomicReference<Activity>()
        scenario.onActivity { activity.set(it) }
        return activity.get()
    }

    companion object {
        private const val BROWSER_GROUP = -1
        private const val LIBRARY_GROUP = -2
        private const val FIRST_RUN_GROUP = -3
        private const val MAX_FIRST_RUN_SCREENS = 10
        private const val PAGES_PER_TEST = 2
        private const val TIMEOUT_MILLIS = 10_000L
        private const val BACK_ATTEMPTS = 3
        private const val MAX_SCROLL_PAGES = 8
        private val isVerticallyScrollable = SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange)

        private const val SUBPAGE_GROUP_BASE = -10

        /** A settings page opened from a row on another settings page. */
        private class Subpage(
            val parent: Int,
            val name: String,
            val entry: AppScreenWalkInstrumentedTest.() -> SemanticsMatcher,
        )

        private val subpages = listOf(
            Subpage(R.string.settings_tabs_gestures_title, "address bar long press") {
                hasTestTag(TabSettingsTestTags.AddressBarLongPressAction)
            },
            Subpage(R.string.settings_tabs_gestures_title, "link peek buttons") {
                hasText(string(R.string.settings_link_peek_actions_title)) and hasClickAction()
            },
            Subpage(R.string.settings_tabs_gestures_title, "address bar buttons") {
                hasText(string(R.string.settings_address_bar_actions_title)) and hasClickAction()
            },
            Subpage(R.string.settings_tabs_gestures_title, "menu buttons") {
                hasText(string(R.string.settings_menu_actions_title)) and hasClickAction()
            },
            Subpage(R.string.settings_appearance_title, "themes") {
                hasTestTag(AppearanceMainTestTags.Themes)
            },
            Subpage(R.string.userscript_title, "script catalog") {
                hasTestTag(UserscriptManagementTestTags.Discover)
            },
        )

        private fun subpageSlices() = subpages.chunked(PAGES_PER_TEST)

        private fun subpageGroups() = subpageSlices().indices.map { slice -> SUBPAGE_GROUP_BASE - slice }

        /** The settings pages opened from the settings home, a few per test to stay in time. */
        private fun settingsSlices() = SettingsRegistry.pages
            .filter { page -> page.destination.parent == SettingsDestination.Home }
            .chunked(PAGES_PER_TEST)

        private val configs = listOf(
            AuditConfig.EnglishLight,
            AuditConfig.RussianDark,
            AuditConfig.EnglishDarkLargeFont,
            AuditConfig.RussianLightLargeFont,
        )

        @JvmStatic
        @Parameterized.Parameters(name = "{index}")
        fun parameters(): List<Array<Any>> {
            val groups = listOf(BROWSER_GROUP, LIBRARY_GROUP, FIRST_RUN_GROUP) +
                settingsSlices().indices + subpageGroups()
            return groups.flatMap { group -> configs.map { config -> arrayOf<Any>(group, config) } }
        }
    }
}
