package dev.sk2andy.materialbrowser.audit

import android.app.Activity
import android.os.SystemClock
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import dev.sk2andy.materialbrowser.MainActivity
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.browser.BrowserTab
import dev.sk2andy.materialbrowser.browser.EdgeToEdgeSiteFixtureServer
import dev.sk2andy.materialbrowser.dismissSystemNotResponding
import dev.sk2andy.materialbrowser.focusedWindow
import dev.sk2andy.materialbrowser.grantNotificationPermissionForTests
import dev.sk2andy.materialbrowser.ui.AddressBarTestTags
import dev.sk2andy.materialbrowser.ui.PermissionRadarTestTags
import dev.sk2andy.materialbrowser.ui.ReleaseNotesTestTags
import dev.sk2andy.materialbrowser.ui.SettingsSearchTestTags
import dev.sk2andy.materialbrowser.shared.ui.ReaderStudioTestTags
import dev.sk2andy.materialbrowser.ui.v5.V5BarTestTags
import dev.sk2andy.materialbrowser.ui.v5.V5DistractionsPanelTestTags
import dev.sk2andy.materialbrowser.ui.v5.V5EditorTestTags
import dev.sk2andy.materialbrowser.ui.v5.V5FindInPageTestTags
import dev.sk2andy.materialbrowser.ui.v5.V5FirstRunTestTags
import dev.sk2andy.materialbrowser.ui.v5.V5LibraryTestTags
import dev.sk2andy.materialbrowser.ui.v5.V5NewTabTestTags
import dev.sk2andy.materialbrowser.ui.v5.V5OverviewTestTags
import dev.sk2andy.materialbrowser.ui.v5.V5PageMenuTestTags
import dev.sk2andy.materialbrowser.ui.v5.V5PrivateNewTabTestTags
import dev.sk2andy.materialbrowser.ui.v5.V5ProfileEditTestTags
import dev.sk2andy.materialbrowser.ui.v5.V5ProfileSheetTestTags
import dev.sk2andy.materialbrowser.ui.v5.V5SheetTestTags
import dev.sk2andy.materialbrowser.ui.v5.V5SiteProtectionTestTags
import dev.sk2andy.materialbrowser.ui.v5.V5SyncSettingsTestTags
import dev.sk2andy.materialbrowser.ui.v5.V5TranslateTestTags
import dev.sk2andy.materialbrowser.ui.v5.V5WebDavTestTags
import java.net.InetAddress
import java.net.ServerSocket
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import org.junit.After
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.rules.Timeout
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.model.MultipleFailureException
import org.junit.runners.model.Statement
import org.junit.runners.model.TestTimedOutException

/**
 * A tester's walk through the real app: launch, then every screen reached the way a user reaches
 * it (taps on the address bar, the menu, the tab counter, the settings rows), each one checked
 * by [ScreenAuditor] from top to bottom. Every group runs in six configurations — English and
 * Russian, light and dark, normal and double font size, the phone upright and on its side, and a
 * tablet — so each screen is gone through six times. A screen the walk cannot reach is a finding too. The test fails with the full list.
 * The v5 interface is the only one (v4 left on 28.10): its walks run in the four upright phone
 * configurations, the browser and the page also on a phone on its side and on a tablet (the phone's
 * layout in a column). Its settings pages are walked too, a few per test.
 */
@RunWith(Parameterized::class)
class AppScreenWalkInstrumentedTest(
    /** A walk group: [V5_BROWSER_GROUP] and the like, or a settings slice. */
    private val group: Int,
    private val config: AuditConfig,
) {
    /**
     * The orchestrator's own test timeout does not stop a test here; one that hangs held its shard
     * for an hour. This one fails it and names where the test thread was stuck.
     */
    @get:Rule(order = 0)
    val timeout: Timeout = Timeout.builder()
        .withTimeout(WALK_TIMEOUT_MINUTES, TimeUnit.MINUTES)
        .withLookingForStuckThread(true)
        .build()

    /**
     * A walk stopped by [timeout] says which step hung and what the main thread was doing: a hang
     * in the test thread alone is only a wait on a busy app.
     */
    @get:Rule(order = -1)
    val hangReport = TestRule { base, _ ->
        object : Statement() {
            override fun evaluate() {
                try {
                    base.evaluate()
                } catch (failure: Throwable) {
                    val timedOut = (failure as? MultipleFailureException)?.failures
                        ?.firstOrNull { it is TestTimedOutException } ?: failure
                    if (timedOut !is TestTimedOutException) throw failure
                    throw AssertionError(
                        "${timedOut.message} in step «$currentStep» after ${visited.toList()}; " +
                            (stepSampler?.summary() ?: "no step running"),
                        failure,
                    )
                }
            }
        }
    }

    @get:Rule(order = 1)
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
            V5_BROWSER_GROUP -> walk(config, steps = ::walkV5Browser)
            V5_PAGE_GROUP -> walk(config, steps = ::walkV5Page)
            V5_FIRST_RUN_GROUP -> walk(config, firstRun = true, steps = {})
            V5_LIBRARY_GROUP -> walk(config, steps = ::walkV5Library)
            V5_PAGE_STATES_GROUP -> walk(config, steps = ::walkV5PageStates)
            V5_DEEPER_GROUP -> walk(config, steps = ::walkV5Deeper)
            in v5SettingsGroups() -> walk(config) { walkV5Settings(V5_SETTINGS_GROUP_BASE - group) }
            else -> error("No walk for group $group")
        }
    }

    private fun walk(auditConfig: AuditConfig, firstRun: Boolean = false, steps: () -> Unit) {
        // Before the environment: the grant asks UiAutomation with flags of its own, and the
        // reconnect would put the rotation back (AuditEnvironment.turnScreen).
        grantNotificationPermissionForTests()
        AuditEnvironment.enter(context, auditConfig, firstRun)
        scenario = ActivityScenario.launch(MainActivity::class.java)
        awaitOrientation()
        if (firstRun) walkV5FirstRun()
        dismissReleaseNotes()
        step("launch") { awaitTag(newTabTag()) }
        steps()
        if (findings.isNotEmpty()) fail(report())
    }

    /**
     * The v5 first run (V28, B1): the welcome, «Open links in Vola?» (the emulator's default browser
     * is another one) answered «Not now», then the gesture card's «Got it», until the new tab.
     */
    private fun walkV5FirstRun() {
        val welcome = hasTestTag(V5FirstRunTestTags.Welcome)
        val question = hasTestTag(V5FirstRunTestTags.DefaultBrowser)
        val gestures = hasTestTag(V5FirstRunTestTags.Gestures)
        step("v5 first run: welcome") {
            awaitTag(V5FirstRunTestTags.Welcome)
            auditScrolling("v5 first run: welcome")
            tapUntil(hasTestTag(V5FirstRunTestTags.Start), "the next first-run page") { !exists(welcome) }
        }
        step("v5 first run: default browser") {
            composeRule.waitUntil("the default browser question or the gestures", TIMEOUT_MILLIS) {
                exists(question) || exists(gestures)
            }
            if (exists(question)) {
                auditScrolling("v5 first run: default browser")
                tapUntil(hasTestTag(V5FirstRunTestTags.NotNow), "the gesture card") { !exists(question) }
            }
        }
        step("v5 first run: gestures") {
            awaitTag(V5FirstRunTestTags.Gestures)
            auditScrolling("v5 first run: gestures")
            tapUntil(hasTestTag(V5FirstRunTestTags.GesturesDone), "the browser") { !exists(gestures) }
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

    /**
     * The v5 interface on a phone: new tab, address editor, menu, the overview with its private
     * side and the profiles, the privacy report, settings and a private tab.
     */
    private fun walkV5Browser() {
        val sheet = hasTestTag(V5SheetTestTags.Sheet)
        step("v5 new tab") { auditScrolling("v5 new tab") }
        step("v5 address editor") {
            click(hasTestTag(AddressBarTestTags.PrimaryField))
            awaitTag(V5EditorTestTags.Editor)
            audit("v5 address editor, empty")
            composeRule.onAllNodes(hasSetTextAction()).onFirst().performTextInput("vola test")
            audit("v5 address editor, typed")
            backUntilGone(hasTestTag(V5EditorTestTags.Editor))
        }
        step("v5 menu") {
            openV5Menu()
            auditScrolling("v5 menu")
            backUntilGone(sheet)
        }
        step("v5 tab overview") {
            openOverview()
            auditScrolling("v5 tab overview")
            click(hasTestTag(V5OverviewTestTags.SegmentPrivate))
            audit("v5 tab overview, private")
            click(hasTestTag(V5OverviewTestTags.SegmentTabs))
            tapUntil(hasTestTag(V5OverviewTestTags.Profile), "the profiles to open") { exists(sheet) }
            auditScrolling("v5 profiles")
            backUntilGone(sheet)
            backUntilGone(overview())
        }
        step("v5 privacy report") {
            // The new tab shows the report's card until the user hides it.
            if (exists(hasTestTag(V5NewTabTestTags.Report)) || revealInLazyList(hasTestTag(V5NewTabTestTags.Report))) {
                tapUntil(hasTestTag(V5NewTabTestTags.Report), "the privacy report to open") { exists(sheet) }
                auditScrolling("v5 privacy report")
                backUntilGone(sheet)
            } else {
                throw AssertionError("the new tab shows no privacy report card")
            }
        }
        step("v5 settings home") {
            openSettings()
            auditScrolling("v5 settings home")
            backUntilGone(hasTestTag(SettingsSearchTestTags.Open))
        }
        step("v5 private tab") {
            scenario.onActivity { activity ->
                activity.browserControllerForTesting().createTab(isIncognito = true)
            }
            awaitTag(V5PrivateNewTabTestTags.Page)
            auditScrolling("v5 private tab")
        }
    }

    /**
     * Pages of the v5 settings home, as a user opens them: each row, then back. Under «Sync» also
     * «Own server» (V27), which a GeckoView build offers until a server is connected.
     */
    private fun walkV5Settings(slice: Int) {
        val translation = AtomicReference(false)
        scenario.onActivity { activity ->
            translation.set(activity.browserControllerForTesting().browserEngineCapabilities.onDeviceTranslation)
        }
        for (page in v5SettingsSlices().getOrElse(slice) { emptyList() }) {
            // Only GeckoView translates on the device; the System WebView build has no such row.
            if (page == R.string.v5_translation_settings_title && !translation.get()) continue
            val title = string(page)
            step("v5 settings: $title") {
                ensureSettingsHome()
                tapUntil(hasText(title) and hasClickAction(), "the $title page to open") { !atSettingsHome() }
                auditScrolling("v5 settings: $title")
                if (page == R.string.sync_settings_title && exists(hasTestTag(V5SyncSettingsTestTags.Server))) {
                    tapUntil(hasTestTag(V5SyncSettingsTestTags.Server), "the own server to open") {
                        exists(hasTestTag(V5WebDavTestTags.Connect))
                    }
                    auditScrolling("v5 settings: own server")
                    backUntilGone(hasTestTag(V5WebDavTestTags.Connect))
                }
                back()
                idle()
            }
        }
    }

    /** A real web page under v5: the page, its menu, the site's protection, find in page, two tabs. */
    private fun walkV5Page() {
        val sheet = hasTestTag(V5SheetTestTags.Sheet)
        EdgeToEdgeSiteFixtureServer(requestHandler = { AUDIT_PAGE_HTML }).use { server ->
            step("v5 web page") {
                openTyped(server.fixtureUrl("/article"))
                auditScrolling("v5 web page")
            }
            step("v5 menu on a page") {
                openV5Menu()
                auditScrolling("v5 menu on a page")
                backUntilGone(sheet)
            }
            step("v5 site protection") {
                tapUntil(hasTestTag(PermissionRadarTestTags.ActivityBadge), "the site's protection to open") {
                    exists(sheet)
                }
                auditScrolling("v5 site protection")
                backUntilGone(sheet)
            }
            step("v5 find in page") {
                openV5Menu()
                // Under v5 find in page is the capsule above the keyboard (V28, B5), not the v4 bar.
                tapUntil(hasText(string(R.string.action_find_in_page)) and hasClickAction(), "find in page to open") {
                    exists(hasTestTag(V5FindInPageTestTags.Bar))
                }
                audit("v5 find in page")
                tapUntil(hasTestTag(V5FindInPageTestTags.Done), "find in page to close") {
                    !exists(hasTestTag(V5FindInPageTestTags.Bar))
                }
            }
            step("v5 tab overview, two tabs") {
                scenario.onActivity { activity ->
                    activity.browserControllerForTesting().createTab(isIncognito = false)
                }
                idle()
                openOverview()
                auditScrolling("v5 tab overview, two tabs")
                backUntilGone(overview())
            }
        }
    }

    /** Favorites, history and downloads under v5, each opened from the menu as a user does. */
    private fun walkV5Library() {
        val library = hasTestTag(V5LibraryTestTags.Page)
        listOf(
            R.string.v5_menu_favorites to "v5 favorites",
            R.string.action_history to "v5 history",
            R.string.v5_menu_downloads to "v5 downloads",
        ).forEach { (row, screen) ->
            step(screen) {
                openV5Menu()
                tapUntil(hasText(string(row)) and hasClickAction(), "$screen to open") { exists(library) }
                auditScrolling(screen)
                backUntilGone(library)
            }
        }
    }

    /**
     * A page's other states under v5: choosing distracting items, site protection's second pages,
     * find with nothing found, the translate sheet where the engine translates, and a page that
     * fails to load. A tool the engine lacks is not in its menu, and the walk passes it by.
     */
    private fun walkV5PageStates() {
        val sheet = hasTestTag(V5SheetTestTags.Sheet)
        EdgeToEdgeSiteFixtureServer(requestHandler = { AUDIT_PAGE_HTML }).use { server ->
            step("v5 page") { openTyped(server.fixtureUrl("/article")) }
            step("v5 hide distractions") {
                openV5Menu()
                val row = hasTestTag(V5PageMenuTestTags.HideDistractions)
                if (exists(row)) {
                    val panel = hasTestTag(V5DistractionsPanelTestTags.Panel)
                    tapUntil(row, "choosing to start") { exists(panel) }
                    audit("v5 hide distractions")
                    tapUntil(hasTestTag(V5DistractionsPanelTestTags.Done), "choosing to end") { !exists(panel) }
                } else {
                    backUntilGone(sheet)
                }
            }
            step("v5 site protection: details") {
                tapUntil(hasTestTag(PermissionRadarTestTags.ActivityBadge), "the site's protection to open") { exists(sheet) }
                tapUntil(hasTestTag(V5SiteProtectionTestTags.Details), "the details") {
                    exists(hasTestTag(V5SiteProtectionTestTags.Reset))
                }
                auditScrolling("v5 site protection: details")
                tapUntil(hasTestTag(V5SiteProtectionTestTags.Back), "the main page") {
                    exists(hasTestTag(V5SiteProtectionTestTags.Toggle))
                }
                tapUntil(hasTestTag(V5SiteProtectionTestTags.Broken), "«Site not working right?»") {
                    exists(hasTestTag(V5SiteProtectionTestTags.TurnOff))
                }
                auditScrolling("v5 site protection: site not working")
                backUntilGone(sheet)
            }
            step("v5 find in page, not found") {
                openV5Menu()
                tapUntil(hasText(string(R.string.action_find_in_page)) and hasClickAction(), "find in page to open") {
                    exists(hasTestTag(V5FindInPageTestTags.Bar))
                }
                composeRule.onNodeWithTag(V5FindInPageTestTags.Query).performTextInput("qzxwvj")
                idle()
                audit("v5 find in page, not found")
                tapUntil(hasTestTag(V5FindInPageTestTags.Done), "find in page to close") {
                    !exists(hasTestTag(V5FindInPageTestTags.Bar))
                }
            }
            step("v5 translate") {
                val onDevice = AtomicReference(false)
                scenario.onActivity { activity ->
                    onDevice.set(activity.browserControllerForTesting().browserEngineCapabilities.onDeviceTranslation)
                }
                openV5Menu()
                val row = hasText(string(R.string.action_translate_page)) and hasClickAction()
                // System WebView keeps the provider's translation: its row opens a site, not the sheet.
                if (onDevice.get() && exists(row)) {
                    val translate = hasTestTag(V5TranslateTestTags.Sheet)
                    tapUntil(row, "the translate sheet to open") { exists(translate) }
                    auditScrolling("v5 translate")
                    backUntilGone(translate)
                } else {
                    backUntilGone(sheet)
                }
            }
            step("v5 page that fails to load") {
                scenario.onActivity { activity ->
                    activity.browserControllerForTesting().createTab(isIncognito = false)
                }
                idle()
                val closedPort = ServerSocket(0, 1, InetAddress.getByName("127.0.0.1")).use { it.localPort }
                openTyped("http://127.0.0.1:$closedPort/", expectFailure = true)
                auditScrolling("v5 page that fails to load")
            }
        }
    }

    /**
     * v5 screens one step deeper than the others (canvas «Reader» and «NewProfile», GAP-AUDIT
     * «Обход экранов»): a page in reading view, and the sheet that makes a new profile. Reading
     * view needs an engine that finds the article; where «Read» never shows, the walk passes it by.
     */
    private fun walkV5Deeper() {
        val sheet = hasTestTag(V5SheetTestTags.Sheet)
        EdgeToEdgeSiteFixtureServer(requestHandler = { AUDIT_PAGE_HTML }).use { server ->
            step("v5 page") { openTyped(server.fixtureUrl("/article")) }
            step("v5 reader") {
                val read = hasTestTag(V5BarTestTags.Read)
                val offered = runCatching {
                    composeRule.waitUntil("«Read» to show", READ_OFFER_TIMEOUT_MILLIS) { exists(read) }
                }.isSuccess
                if (offered) {
                    val reader = hasTestTag(ReaderStudioTestTags.Screen)
                    tapUntil(read, "reading view to open") { exists(reader) }
                    auditScrolling("v5 reader")
                    backUntilGone(reader)
                }
            }
        }
        step("v5 new profile") {
            openOverview()
            tapUntil(hasTestTag(V5OverviewTestTags.Profile), "the profiles to open") { exists(sheet) }
            val name = hasTestTag(V5ProfileEditTestTags.Name)
            tapUntil(hasTestTag(V5ProfileSheetTestTags.New), "a new profile to open") { exists(name) }
            auditScrolling("v5 new profile")
            backUntilGone(name)
            backUntilGone(sheet)
            backUntilGone(overview())
        }
    }

    private fun openV5Menu() {
        tapUntil(hasContentDescription(string(R.string.cd_more_options)), "the menu to open") {
            exists(hasTestTag(V5SheetTestTags.Sheet))
        }
    }

    /** Types [url] into the address bar and submits it from the keyboard, as a user does. */
    private fun openTyped(url: String, expectFailure: Boolean = false) {
        click(hasTestTag(AddressBarTestTags.PrimaryField))
        awaitShown(addressEditor())
        val field = composeRule.onAllNodes(hasSetTextAction()).onFirst()
        field.performTextInput(url)
        val typed = runCatching {
            field.fetchSemanticsNode().config.getOrNull(SemanticsProperties.EditableText)?.text
        }.getOrNull()
        field.performImeAction()
        val loaded = runCatching {
            composeRule.waitUntil("$url to load", PAGE_TIMEOUT_MILLIS) {
                val current = selectedTab()
                val failed = current.error != null || current.failureKind != null
                !current.isLoading && if (expectFailure) failed else current.url.startsWith(url)
            }
        }
        if (loaded.isFailure) {
            val tab = selectedTab()
            throw AssertionError(
                "$url did not ${if (expectFailure) "fail" else "load"} in ${PAGE_TIMEOUT_MILLIS / 1000} s: " +
                    "typed «$typed», tab at «${tab.url}», loading ${tab.isLoading}, " +
                    "error ${tab.error}, failure ${tab.failureKind}",
            )
        }
        idle()
    }

    private fun selectedTab(): BrowserTab {
        val tab = AtomicReference<BrowserTab>()
        scenario.onActivity { activity -> tab.set(activity.browserControllerForTesting().selectedTab) }
        return tab.get()
    }

    /** The menu's Settings, in its short view or under More, the way a user finds it. */
    private fun openSettings() {
        openV5Menu()
        tapUntil(hasText(string(R.string.action_settings)) and hasClickAction(), "the settings to open") {
            atSettingsHome()
        }
    }

    private fun openOverview() {
        val overview = overview()
        tapUntil(hasTestTag(AddressBarTestTags.TabButton), "the tab overview to open") { exists(overview) }
    }

    private fun overview(): SemanticsMatcher =
        hasTestTag(V5OverviewTestTags.Overview)

    /** What the browser shows on a new tab. */
    private fun newTabTag(): String = V5NewTabTestTags.Page

    private fun addressEditor(): SemanticsMatcher =
        hasTestTag(V5EditorTestTags.Editor)

    private fun atSettingsHome(): Boolean = exists(hasTestTag(SettingsSearchTestTags.Open))

    /**
     * Taps [target] until [done]. On a busy phone a tap that lands while a screen is still
     * animating in can be lost; a user taps once more, and so does the walk — once.
     */
    private fun tapUntil(target: SemanticsMatcher, what: String, done: () -> Boolean) {
        repeat(TAP_ATTEMPTS) { attempt ->
            if (attempt > 0 && !exists(target)) return@repeat
            click(target)
            val opened = timed("wait for $what") {
                runCatching { composeRule.waitUntil(RETAP_AFTER_MILLIS) { done() } }.isSuccess
            }
            if (opened) return
        }
        timed("wait for $what") { composeRule.waitUntil(what, TIMEOUT_MILLIS) { done() } }
    }

    /** The node lies whole inside its window: a tap lands on it without scrolling first. */
    private fun fullyShown(node: SemanticsNode): Boolean {
        val bounds = node.boundsInWindow
        return bounds.width >= node.size.width - 1f && bounds.height >= node.size.height - 1f &&
            bounds.width > 0f && bounds.height > 0f
    }

    private fun ensureSettingsHome() {
        if (exists(hasTestTag(SettingsSearchTestTags.Open))) return
        repeat(BACK_ATTEMPTS) {
            back()
            idle()
            if (exists(hasTestTag(SettingsSearchTestTags.Open))) return
        }
        if (!exists(hasTestTag(newTabTag()))) relaunch()
        openSettings()
    }

    /** One step of the walk: a failure to reach the screen is filed, and the walk goes on. */
    private fun step(name: String, body: () -> Unit) {
        stepTimings.clear()
        val started = SystemClock.elapsedRealtime()
        val sampler = MainThreadSampler()
        stepSampler = sampler
        currentStep = name
        timed("system dialog check") { device.dismissSystemNotResponding() }
        try {
            body()
            val millis = SystemClock.elapsedRealtime() - started
            // A user waits through this too: a screen that takes minutes to go through is a bug,
            // and the timings say where a slow run spent its time.
            if (millis > SLOW_STEP_MILLIS) {
                findings += AuditFinding(
                    name,
                    config.name,
                    AuditKind.Navigation,
                    "took ${millis / 1000} s to go through; slowest: ${timingSummary(millis)}; " +
                        sampler.summary(),
                )
            }
        } catch (error: Throwable) {
            findings += AuditFinding(
                screen = name,
                config = config.name,
                kind = AuditKind.Navigation,
                detail = "${error.javaClass.simpleName}: ${error.message?.lineSequence()?.firstOrNull()}" +
                    " — on screen: ${runCatching { auditor.describeScreen() }.getOrDefault("?")}" +
                    // Back lost to a system window reads like a screen that ignores back.
                    " — focused window: ${device.focusedWindow()}",
            )
            runCatching { recover() }
        } finally {
            sampler.close()
            stepSampler = null
            visited += "$name ${(SystemClock.elapsedRealtime() - started) / 1000}s"
        }
    }

    /**
     * Where the current step spent its time: every timed operation's count and total, for the
     * step's own timing finding. A slow step can be one long wait or hundreds of short ones.
     */
    private val stepTimings = linkedMapOf<String, LongArray>()

    @Volatile private var stepSampler: MainThreadSampler? = null

    @Volatile private var currentStep = ""

    private inline fun <T> timed(operation: String, block: () -> T): T {
        val started = SystemClock.elapsedRealtime()
        try {
            return block()
        } finally {
            val timing = stepTimings.getOrPut(operation) { LongArray(2) }
            timing[0]++
            timing[1] += SystemClock.elapsedRealtime() - started
        }
    }

    private fun timingSummary(stepMillis: Long): String {
        val timedMillis = stepTimings.values.sumOf { it[1] }
        val slowest = stepTimings.entries.sortedByDescending { it.value[1] }.take(SLOWEST_SHOWN)
            .joinToString { (operation, timing) -> "$operation ×${timing[0]} ${timing[1] / 1000} s" }
        return "$slowest; untimed ${(stepMillis - timedMillis).coerceAtLeast(0) / 1000} s"
    }

    private fun back() = timed("back") { device.pressBack() }

    private fun idle() = timed("wait for idle") { composeRule.waitForIdle() }

    private fun recover() {
        device.dismissSystemNotResponding()
        repeat(BACK_ATTEMPTS) {
            if (atBareNewTab()) return
            back()
            idle()
        }
        if (!atBareNewTab()) relaunch()
    }

    /** The new tab with nothing over it: its header stays in the tree under the editor. */
    private fun atBareNewTab(): Boolean =
        exists(hasTestTag(newTabTag())) &&
            !exists(addressEditor()) &&
            !exists(hasTestTag(V5SheetTestTags.Sheet)) &&
            !exists(overview())

    private fun relaunch() {
        runCatching { scenario.close() }
        scenario = ActivityScenario.launch(MainActivity::class.java)
        awaitOrientation()
        dismissReleaseNotes()
        awaitTag(newTabTag())
    }

    /**
     * The rotation setting turns the screen only once the app is in front (the launcher holds it
     * upright before), and late at that: a walk that went on at once walked the shape the previous
     * test left, a portrait walk in a landscape window. The walk waits until the app's window has
     * the configuration's shape and notes how long that took. (Freezing the rotation through
     * UiAutomation or `wm user-rotation lock` did not turn the app at all on the CI emulator.)
     */
    private fun awaitOrientation() {
        val landscape = config.layout == AuditLayout.PhoneLandscape
        AuditEnvironment.turnScreen(config.layout)
        val started = SystemClock.elapsedRealtime()
        val turned = runCatching {
            composeRule.waitUntil(ROTATION_TIMEOUT_MILLIS) {
                // Between the old activity and the turned one there is a moment with none.
                runCatching {
                    val window = currentActivity().window.decorView
                    window.width > 0 && (window.width > window.height) == landscape
                }.getOrDefault(false)
            }
        }.isSuccess
        if (!turned) {
            val shape = runCatching {
                val activity = currentActivity()
                val window = activity.window.decorView
                "window ${window.width}×${window.height}, configuration orientation " +
                    "${activity.resources.configuration.orientation}, requested ${activity.requestedOrientation}"
            }.getOrDefault("no activity")
            val settings = listOf("user_rotation", "accelerometer_rotation")
                .joinToString { key -> "$key ${shellOutput("settings get system $key").trim()}" }
            val windowManager = shellOutput("dumpsys window displays").lineSequence()
                .filter { line -> "otation" in line }
                .map(String::trim)
                .distinct()
                .take(ROTATION_DUMP_LINES)
                .joinToString(" | ")
            throw AssertionError(
                "the app's window did not turn ${if (landscape) "landscape" else "upright"} in " +
                    "${ROTATION_TIMEOUT_MILLIS / 1000} s: $shape, display rotation ${device.displayRotation}, " +
                    "$settings; window manager: $windowManager",
            )
        }
        visited += "orientation ${(SystemClock.elapsedRealtime() - started) / 1000}s"
        idle()
    }

    private fun shellOutput(command: String): String = device.executeShellCommand(command)

    private fun dismissReleaseNotes() {
        idle()
        if (!exists(hasTestTag(ReleaseNotesTestTags.Screen))) return
        step("release notes") {
            auditScrolling("release notes")
            click(hasTestTag(ReleaseNotesTestTags.Done))
            idle()
        }
    }

    private fun audit(screen: String) {
        findings += timed("audit $screen") { auditor.audit(screen, config.name) }
        shoot(screen)
    }

    /** The screen as a picture for the Screenshots workflow, in walk order ([ScreenShots]). */
    private fun shoot(screen: String) {
        if (!ScreenShots.enabled) return
        val theme = if (config.dark) "dark" else "light"
        val prefix = "${config.languageTag}-$theme-%02d".format(shotOrder(group))
        ScreenShots.capture(prefix, "%02d %s".format(shots++, screen))
    }

    private var shots = 0

    /** Audits the screen, then scrolls it a page at a time to the end and audits again. */
    private fun auditScrolling(screen: String) {
        audit(screen)
        repeat(MAX_SCROLL_PAGES) { page ->
            val scrollable = timed("find the list") {
                composeRule.onAllNodes(hasScrollAction() and isVerticallyScrollable)
                    .fetchSemanticsNodes(atLeastOneRootRequired = false)
                    .maxByOrNull { node -> node.boundsInWindow.height * node.boundsInWindow.width }
            } ?: return
            val before = scrollable.config.getOrNull(SemanticsProperties.VerticalScrollAxisRange)?.value?.invoke()
            val target = composeRule.onAllNodes(SemanticsMatcher("scrollable ${scrollable.id}") { it.id == scrollable.id })
                .onFirst()
            // Scrolled by the list's own scroll action, most of a screen at a time. A swipe on a
            // loaded emulator can be slow enough to read as a long press, and on the drag-to-arrange
            // editors that starts a drag: the menu buttons page then took up to 21 minutes.
            val pageHeight = scrollable.boundsInWindow.height * SCROLL_PAGE_FRACTION
            val scrolled = timed("scroll $screen") {
                runCatching {
                    target.performSemanticsAction(SemanticsActions.ScrollBy) { scrollBy -> scrollBy(0f, pageHeight) }
                }.isSuccess
            }
            if (!scrolled) target.performTouchInput { swipeUp() }
            idle()
            val after = composeRule.onAllNodes(SemanticsMatcher("scrollable ${scrollable.id}") { it.id == scrollable.id })
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .firstOrNull()
                ?.config
                ?.getOrNull(SemanticsProperties.VerticalScrollAxisRange)
                ?.value
                ?.invoke()
            if (after == null || after == before) return
            findings += timed("audit $screen ↓${page + 1}") { auditor.audit("$screen ↓${page + 1}", config.name) }
            shoot("$screen ${page + 1}")
        }
    }

    /**
     * [systemTap] taps through the system, as a finger does, instead of Compose's injected touch:
     * on the gesture lesson standing still (animations removed) the injected touch on «Skip» never
     * arrived, while a system tap and the button's own action both worked (runs of 15.10).
     */
    private fun click(matcher: SemanticsMatcher, systemTap: Boolean = false) {
        // Screens still animating in get a moment first; only then are lists scrolled to look.
        timed("find ${matcher.description}") {
            val shown = runCatching { composeRule.waitUntil(REVEAL_AFTER_MILLIS) { exists(matcher) } }.isSuccess
            if (!shown) revealInLazyList(matcher)
            composeRule.waitUntil("${matcher.description} to tap", TIMEOUT_MILLIS) { exists(matcher) }
        }
        // Scrolled into view first, as a user would: with a large font menu items sit below the fold.
        val target = composeRule.onAllNodes(matcher)[liveIndex(matcher) ?: 0]
        if (!fullyShown(target.fetchSemanticsNode())) {
            timed("scroll to ${matcher.description}") { scrollIntoView(matcher) }
        }
        timed("tap ${matcher.description}") {
            if (systemTap) {
                val center = target.fetchSemanticsNode().boundsInWindow.center
                val window = IntArray(2)
                currentActivity().window.decorView.getLocationOnScreen(window)
                device.click(window[0] + center.x.toInt(), window[1] + center.y.toInt())
            } else {
                target.performClick()
            }
        }
        idle()
    }

    /**
     * A lazy list composes only the rows near the screen: with a large font a row further down
     * is not in the tree at all until the list is scrolled to it, as a user would scroll.
     */
    private fun revealInLazyList(matcher: SemanticsMatcher): Boolean {
        val lists = composeRule.onAllNodes(hasScrollToNodeAction())
            .fetchSemanticsNodes(atLeastOneRootRequired = false)
        for (list in lists) {
            for (page in 0 until MAX_SCROLL_PAGES) {
                if (exists(matcher)) return true
                if (!scrollBy(list.id, list.boundsInWindow.height * SCROLL_PAGE_FRACTION)) break
            }
        }
        return exists(matcher)
    }

    /**
     * Scrolls the list around the node until the node lies whole inside it. Compose's own
     * `performScrollTo`/`performScrollToNode` took two to seven minutes here with the main thread
     * mostly idle (sampled on the System WebView build) and hung a walk past its timeout; the
     * list's scroll action takes a moment.
     */
    private fun scrollIntoView(matcher: SemanticsMatcher) {
        repeat(SCROLL_INTO_VIEW_ATTEMPTS) {
            val node = composeRule.onAllNodes(matcher).fetchSemanticsNodes(atLeastOneRootRequired = false)
                .getOrNull(liveIndex(matcher) ?: return) ?: return
            if (fullyShown(node)) return
            var list = node.parent
            while (list != null && !list.config.contains(SemanticsProperties.VerticalScrollAxisRange)) {
                list = list.parent
            }
            if (list == null) return
            val viewport = list.boundsInWindow
            val top = node.positionInWindow.y
            val bottom = top + node.size.height
            val delta = when {
                bottom > viewport.bottom -> bottom - viewport.bottom
                top < viewport.top -> top - viewport.top
                else -> return
            }
            if (!scrollBy(list.id, delta)) return
        }
    }

    /** Scrolls the list with this semantics id by [delta] px with its own scroll action. */
    private fun scrollBy(listId: Int, delta: Float): Boolean {
        val list = composeRule.onAllNodes(SemanticsMatcher("list $listId") { it.id == listId }).onFirst()
        val scrolled = runCatching {
            list.performSemanticsAction(SemanticsActions.ScrollBy) { scrollBy -> scrollBy(0f, delta) }
        }.isSuccess
        idle()
        return scrolled
    }

    private fun awaitTag(tag: String) {
        composeRule.waitUntil("tag $tag to appear", TIMEOUT_MILLIS) { exists(hasTestTag(tag)) }
    }

    /**
     * Presses back the way a user leaves a screen, until the screen is gone. A screen still there
     * after that is filed with what each press found: the keyboard up or down, the screen there.
     */
    private fun backUntilGone(screen: SemanticsMatcher) {
        val presses = mutableListOf<String>()
        repeat(BACK_ATTEMPTS) {
            if (!exists(screen)) return
            device.dismissSystemNotResponding()
            val keyboard = keyboardShown()
            back()
            idle()
            val after = if (exists(screen)) "stayed" else "closed"
            presses += "back with keyboard ${if (keyboard) "up" else "down"} → screen $after"
        }
        val closed = runCatching {
            composeRule.waitUntil(TIMEOUT_MILLIS) { !exists(screen) }
        }.isSuccess
        if (!closed) {
            throw AssertionError("${screen.description} did not close on back: ${presses.joinToString("; ")}")
        }
    }

    private fun keyboardShown(): Boolean {
        val shown = AtomicReference(false)
        scenario.onActivity { activity ->
            shown.set(
                ViewCompat.getRootWindowInsets(activity.window.decorView)
                    ?.isVisible(WindowInsetsCompat.Type.ime()) == true,
            )
        }
        return shown.get()
    }

    private fun awaitShown(matcher: SemanticsMatcher) {
        composeRule.waitUntil("${matcher.description} to appear", TIMEOUT_MILLIS) { exists(matcher) }
    }

    private fun exists(matcher: SemanticsMatcher): Boolean = runCatching { liveIndex(matcher) != null }.getOrDefault(false)

    /**
     * The first node the matcher finds in a Compose root that is on screen. A root of no size can
     * hold a copy of a screen that left: tapping the first-run lesson's Skip there tapped the
     * window's corner, and the lesson stayed.
     */
    private fun liveIndex(matcher: SemanticsMatcher): Int? = composeRule.onAllNodes(matcher)
        .fetchSemanticsNodes(atLeastOneRootRequired = false)
        .indexOfFirst { node -> node.rootBounds().width > 0f && node.rootBounds().height > 0f }
        .takeIf { it >= 0 }

    private fun SemanticsNode.rootBounds(): Rect {
        var root = this
        while (true) root = root.parent ?: break
        return root.boundsInWindow
    }

    private fun string(id: Int): String = currentActivity().getString(id)

    private fun currentActivity(): Activity {
        val activity = AtomicReference<Activity>()
        scenario.onActivity { activity.set(it) }
        return activity.get()
    }

    companion object {

        private const val PAGES_PER_TEST = 2
        private const val TIMEOUT_MILLIS = 10_000L
        private const val ROTATION_TIMEOUT_MILLIS = 60_000L
        private const val ROTATION_DUMP_LINES = 12
        private const val BACK_ATTEMPTS = 3
        private const val SLOW_STEP_MILLIS = 90_000L
        private const val SLOWEST_SHOWN = 4
        private const val WALK_TIMEOUT_MINUTES = 20L
        private const val REVEAL_AFTER_MILLIS = 3_000L
        private const val MAX_SCROLL_PAGES = 8
        private const val SCROLL_INTO_VIEW_ATTEMPTS = 4
        private const val SCROLL_PAGE_FRACTION = 0.8f
        private val isVerticallyScrollable = SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange)

        private const val V5_BROWSER_GROUP = -6
        private const val V5_PAGE_GROUP = -7
        private val V5_GROUPS = setOf(V5_BROWSER_GROUP, V5_PAGE_GROUP)
        private const val V5_FIRST_RUN_GROUP = -8
        private const val V5_LIBRARY_GROUP = -9
        private const val V5_PAGE_STATES_GROUP = -16
        private const val V5_DEEPER_GROUP = -17
        /** How long a loaded article may take before the engine offers reading view. */
        private const val READ_OFFER_TIMEOUT_MILLIS = 10_000L

        /** v5 walks added after the others (N-04): their own list keeps the older walks' indices. */
        private val V5_MORE_GROUPS = listOf(V5_FIRST_RUN_GROUP, V5_LIBRARY_GROUP, V5_PAGE_STATES_GROUP)
        private const val V5_SETTINGS_GROUP_BASE = -20
        private const val PAGE_TIMEOUT_MILLIS = 30_000L
        private const val TAP_ATTEMPTS = 2
        private const val RETAP_AFTER_MILLIS = 4_000L

        /** An ordinary article: headings, paragraphs, a list and links, longer than a screen. */
        private val AUDIT_PAGE_HTML = buildString {
            append("<!doctype html><html lang=en><head><meta charset=utf-8>")
            append("<meta name=viewport content='width=device-width, initial-scale=1'>")
            append("<title>Vola audit article</title></head><body>")
            append("<h1>Vola audit article</h1>")
            repeat(12) { index ->
                append("<h2>Section ${index + 1}</h2>")
                append("<p>Vola keeps the page edge to edge. This paragraph is long enough to wrap ")
                append("over several lines on a phone, with a <a href='#s$index'>link</a> in it.</p>")
                append("<ul><li>First point</li><li>Second point</li></ul>")
            }
            append("</body></html>")
        }

        /**
         * The v5 settings home's pages by their row titles, in its order (V5SettingsHomeRules). The
         * Firefox extensions row opens Gecko's own manager, not a page; developer options show only
         * once unlocked. A row missing from the home fails the walk, so this list cannot quietly
         * drift from the screen.
         */
        private val v5SettingsPages = listOf(
            R.string.sync_settings_title,
            R.string.settings_section_search,
            // Its gestures are the v5 interface's own, so the row is just «Tabs».
            R.string.settings_section_tabs,
            R.string.settings_appearance_title,
            R.string.settings_section_browser,
            R.string.v5_translation_settings_title,
            R.string.settings_protection_data_title,
            R.string.passwords_title,
            R.string.userscript_title,
            R.string.settings_downloads_title,
            R.string.settings_section_about_legal,
        )

        private fun v5SettingsSlices() = v5SettingsPages.chunked(PAGES_PER_TEST)

        private fun v5SettingsGroups() = v5SettingsSlices().indices.map { slice -> V5_SETTINGS_GROUP_BASE - slice }

        /** Where a walk group's pictures go in the gallery: in the order a user meets the screens. */
        private fun shotOrder(group: Int): Int = when (group) {
            V5_FIRST_RUN_GROUP -> 1
            V5_BROWSER_GROUP -> 2
            V5_PAGE_GROUP -> 3
            V5_PAGE_STATES_GROUP -> 4
            V5_LIBRARY_GROUP -> 5
            V5_DEEPER_GROUP -> 6
            else -> SETTINGS_SHOT_ORDER + (V5_SETTINGS_GROUP_BASE - group)
        }

        private const val SETTINGS_SHOT_ORDER = 10

        /**
         * The Screenshots workflow's walk: every group once in each configuration [ScreenShots.configs]
         * names, upright phone only. The screen checks still run and still fail a walk.
         */
        private fun screenshotWalks(): List<Array<Any>> {
            val configs = ScreenShots.configs(
                mapOf(
                    "EnglishLight" to AuditConfig.EnglishLight,
                    "RussianDark" to AuditConfig.RussianDark,
                    "EnglishDarkLargeFont" to AuditConfig.EnglishDarkLargeFont,
                    "RussianLightLargeFont" to AuditConfig.RussianLightLargeFont,
                ),
            )
            val groups = listOf(V5_FIRST_RUN_GROUP) + V5_GROUPS.sortedDescending() +
                listOf(V5_PAGE_STATES_GROUP, V5_LIBRARY_GROUP, V5_DEEPER_GROUP) + v5SettingsGroups()
            return configs.flatMap { config -> groups.map { group -> arrayOf<Any>(group, config) } }
        }

        @JvmStatic
        @Parameterized.Parameters(name = "{index}")
        fun parameters(): List<Array<Any>> {
            if (ScreenShots.enabled) return screenshotWalks()
            val v5Configs = AuditConfig.All.filter { config -> config.layout == AuditLayout.PhonePortrait }
            val v5Walks = V5_GROUPS.sortedDescending().flatMap { group ->
                v5Configs.map { config -> arrayOf<Any>(group, config) }
            }
            // A phone turned sideways keeps v5 (decision 2026-10-15) and a tablet has it too (28.10):
            // each walked once.
            val v5Landscape = V5_GROUPS.sortedDescending().flatMap { group ->
                listOf(AuditConfig.RussianLightLandscape, AuditConfig.EnglishDarkTablet).map { config ->
                    arrayOf<Any>(group, config)
                }
            }
            // Last again, for the same reason: the v5 settings pages in the upright phone configurations.
            val v5SettingsWalks = v5SettingsGroups().flatMap { group ->
                v5Configs.map { config -> arrayOf<Any>(group, config) }
            }
            // Last of all, the v5 walks added later, upright and (but the first run) on its side.
            val v5MoreWalks = V5_MORE_GROUPS.flatMap { group ->
                v5Configs.map { config -> arrayOf<Any>(group, config) }
            } + V5_MORE_GROUPS.filter { it != V5_FIRST_RUN_GROUP }.map { group ->
                arrayOf<Any>(group, AuditConfig.RussianLightLandscape)
            }
            // The newest walk, last so every index before it stays; upright phone configurations only.
            val v5DeeperWalks = v5Configs.map { config -> arrayOf<Any>(V5_DEEPER_GROUP, config) }
            return v5Walks + v5Landscape + v5SettingsWalks + v5MoreWalks + v5DeeperWalks
        }
    }
}
