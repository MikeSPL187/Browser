package dev.sk2andy.materialbrowser.audit

import android.app.Activity
import android.os.SystemClock
import androidx.compose.ui.semantics.Role
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
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
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
import dev.sk2andy.materialbrowser.settings.SettingsRegistry
import dev.sk2andy.materialbrowser.shared.ui.BrowserMainMenuTestTags
import dev.sk2andy.materialbrowser.shared.ui.TabOverviewChromeTestTags
import dev.sk2andy.materialbrowser.ui.AddressBarTestTags
import dev.sk2andy.materialbrowser.ui.AppearanceMainTestTags
import dev.sk2andy.materialbrowser.ui.FindInPageBarTestTags
import dev.sk2andy.materialbrowser.ui.FirstRunTestTags
import dev.sk2andy.materialbrowser.ui.NewTabPageTestTags
import dev.sk2andy.materialbrowser.ui.PermissionRadarTestTags
import dev.sk2andy.materialbrowser.ui.ReleaseNotesTestTags
import dev.sk2andy.materialbrowser.ui.SettingsDestination
import dev.sk2andy.materialbrowser.ui.SettingsSearchTestTags
import dev.sk2andy.materialbrowser.ui.SiteInfoTestTags
import dev.sk2andy.materialbrowser.ui.TabSettingsTestTags
import dev.sk2andy.materialbrowser.ui.UserscriptManagementTestTags
import java.net.InetAddress
import java.net.ServerSocket
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import org.junit.After
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.Timeout
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * A tester's walk through the real app: launch, then every screen reached the way a user reaches
 * it (taps on the address bar, the menu, the tab counter, the settings rows), each one checked
 * by [ScreenAuditor] from top to bottom. Every group runs in six configurations — English and
 * Russian, light and dark, normal and double font size, the phone upright and on its side, and a
 * tablet — so each screen is gone through six times. A screen the walk cannot reach is a finding too. The test fails with the full list.
 */
@RunWith(Parameterized::class)
class AppScreenWalkInstrumentedTest(
    /** A walk group: [BROWSER_GROUP] and the like, a settings slice, or a subpage slice. */
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
            BROWSER_GROUP -> walk(config, steps = ::walkBrowser)
            LIBRARY_GROUP -> walk(config, steps = ::walkLibrary)
            FIRST_RUN_GROUP -> walk(config, firstRun = true, steps = {})
            PAGE_GROUP -> walk(config, steps = ::walkPage)
            PAGE_STATES_GROUP -> walk(config, steps = ::walkPageStates)
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

    /**
     * The welcome, then every screen after it the way a new user goes on: «Next» through the setup
     * screens, the gesture lessons' intro and the first lesson (then «Skip», as a hurried user
     * does), until the browser shows its new tab.
     */
    private fun walkFirstRun() {
        step("first run: welcome") {
            awaitTag(FirstRunTestTags.Welcome)
            auditScrolling("first run: welcome")
            click(hasTestTag(FirstRunTestTags.Start))
        }
        var reachedBrowser = false
        repeat(MAX_FIRST_RUN_SCREENS) { index ->
            if (reachedBrowser) return
            val screen = "first run: screen ${index + 1}"
            step(screen) {
                composeRule.waitUntil("the next first-run screen", TIMEOUT_MILLIS) {
                    firstRunForward() != null || exists(hasTestTag(NewTabPageTestTags.Header))
                }
                val forward = firstRunForward()
                if (forward == null) {
                    reachedBrowser = true
                } else {
                    auditScrolling(screen)
                    click(hasTestTag(forward))
                    idle()
                }
            }
        }
    }

    private fun firstRunForward(): String? = FIRST_RUN_FORWARD.firstOrNull { tag -> exists(hasTestTag(tag)) }

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
                idle()
                auditScrolling(screen)
                // Some of these are activities of their own: the browser stays composed under them.
                back()
                idle()
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
            openOverview()
            auditScrolling("tab overview")
            backUntilGone(hasTestTag(TabOverviewChromeTestTags.Root))
        }
        step("private tab") {
            scenario.onActivity { activity ->
                activity.browserControllerForTesting().createTab(isIncognito = true)
            }
            idle()
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
                tapUntil(hasText(title) and hasClickAction(), "the $title page to open") { !atSettingsHome() }
                auditScrolling("settings: $title")
                back()
                idle()
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
                idle()
                auditScrolling("settings search, results")
                back()
            }
        }
    }

    /** A real web page typed into the address bar, its site information and find in page. */
    private fun walkPage() {
        EdgeToEdgeSiteFixtureServer(requestHandler = { AUDIT_PAGE_HTML }).use { server ->
            step("web page") {
                openTyped(server.fixtureUrl("/article"))
                auditScrolling("web page")
            }
            step("site information") {
                click(hasTestTag(PermissionRadarTestTags.ActivityBadge))
                awaitTag(SiteInfoTestTags.Overview)
                auditScrolling("site information")
                backUntilGone(hasTestTag(SiteInfoTestTags.Overview))
            }
            step("find in page") {
                openMenuItem(BrowserMainMenuTestTags.FindInPage)
                awaitTag(FindInPageBarTestTags.Bar)
                audit("find in page, empty")
                composeRule.onNodeWithTag(FindInPageBarTestTags.Query).performTextInput("Vola")
                composeRule.waitUntil("matches to be counted", PAGE_TIMEOUT_MILLIS) {
                    exists(hasTestTag(FindInPageBarTestTags.MatchCount))
                }
                audit("find in page, matches")
                click(hasTestTag(FindInPageBarTestTags.Close))
                composeRule.waitUntil("find in page to close", TIMEOUT_MILLIS) {
                    !exists(hasTestTag(FindInPageBarTestTags.Bar))
                }
            }
        }
    }

    /** The menu over a page, a page that fails to load, and the overview with several tabs. */
    private fun walkPageStates() {
        EdgeToEdgeSiteFixtureServer(requestHandler = { AUDIT_PAGE_HTML }).use { server ->
            step("menu on a page") {
                openTyped(server.fixtureUrl("/article"))
                click(hasContentDescription(string(R.string.cd_more_options)))
                awaitTag(BrowserMainMenuTestTags.Menu)
                audit("menu on a page")
                click(hasTestTag(BrowserMainMenuTestTags.More))
                awaitTag(BrowserMainMenuTestTags.MoreGroup)
                audit("menu on a page, more")
                backUntilGone(hasTestTag(BrowserMainMenuTestTags.Menu))
            }
            step("page that fails to load") {
                scenario.onActivity { activity ->
                    activity.browserControllerForTesting().createTab(isIncognito = false)
                }
                idle()
                // A port just freed: nothing listens there and the connection is refused at once.
                // (Port 1 is one Gecko refuses to try at all, which is a different page.)
                val closedPort = ServerSocket(0, 1, InetAddress.getByName("127.0.0.1")).use { it.localPort }
                openTyped("http://127.0.0.1:$closedPort/", expectFailure = true)
                auditScrolling("page that fails to load")
            }
            step("tab overview, two tabs") {
                openOverview()
                auditScrolling("tab overview, two tabs")
                backUntilGone(hasTestTag(TabOverviewChromeTestTags.Root))
            }
        }
    }

    /** Types [url] into the address bar and submits it from the keyboard, as a user does. */
    private fun openTyped(url: String, expectFailure: Boolean = false) {
        click(hasTestTag(AddressBarTestTags.PrimaryField))
        awaitShown(closeAddressInput())
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

    /** Pages one level deeper: their parent page first, then the row that opens them. */
    private fun walkSubpages(slice: Int) {
        step("settings home") { openSettings() }
        for (subpage in subpageSlices().getOrElse(slice) { emptyList() }) {
            val screen = "settings: ${string(subpage.parent)} › ${subpage.name}"
            step(screen) {
                ensureSettingsHome()
                val parent = hasText(string(subpage.parent)) and hasClickAction()
                tapUntil(parent, "${string(subpage.parent)} to open") { !atSettingsHome() }
                val entry = subpage.entry(this)
                tapUntil(entry, "$screen to open") { !exists(entry) }
                auditScrolling(screen)
            }
        }
    }

    /** The menu's Settings, in its short view or under More, the way a user finds it. */
    private fun openSettings() {
        openMenuItem(BrowserMainMenuTestTags.Settings) { atSettingsHome() }
    }

    private fun openOverview() {
        val overview = hasTestTag(TabOverviewChromeTestTags.Root)
        tapUntil(hasTestTag(AddressBarTestTags.TabButton), "the tab overview to open") { exists(overview) }
    }

    private fun atSettingsHome(): Boolean = exists(hasTestTag(SettingsSearchTestTags.Open))

    /** Taps a menu item, in the short menu or under More, until what it opens is there. */
    private fun openMenuItem(
        tag: String,
        opened: () -> Boolean = { !exists(hasTestTag(BrowserMainMenuTestTags.Menu)) },
    ) {
        val menu = hasTestTag(BrowserMainMenuTestTags.Menu)
        tapUntil(hasContentDescription(string(R.string.cd_more_options)), "the menu to open") { exists(menu) }
        if (!exists(hasTestTag(tag))) {
            val more = hasTestTag(BrowserMainMenuTestTags.MoreGroup)
            tapUntil(hasTestTag(BrowserMainMenuTestTags.More), "More to open") { exists(more) }
        }
        tapUntil(hasTestTag(tag), "$tag to open", opened)
    }

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
        if (!exists(hasTestTag(NewTabPageTestTags.Header))) relaunch()
        openSettings()
    }

    /** One step of the walk: a failure to reach the screen is filed, and the walk goes on. */
    private fun step(name: String, body: () -> Unit) {
        stepTimings.clear()
        val started = SystemClock.elapsedRealtime()
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
                    "took ${millis / 1000} s to go through; slowest: ${timingSummary(millis)}",
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
            visited += "$name ${(SystemClock.elapsedRealtime() - started) / 1000}s"
        }
    }

    /**
     * Where the current step spent its time: every timed operation's count and total, for the
     * step's own timing finding. A slow step can be one long wait or hundreds of short ones.
     */
    private val stepTimings = linkedMapOf<String, LongArray>()

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
    }

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
        }
    }

    private fun click(matcher: SemanticsMatcher) {
        // Screens still animating in get a moment first; only then are lists scrolled to look.
        timed("find ${matcher.description}") {
            val shown = runCatching { composeRule.waitUntil(REVEAL_AFTER_MILLIS) { exists(matcher) } }.isSuccess
            if (!shown) revealInLazyList(matcher)
            composeRule.waitUntil("${matcher.description} to tap", TIMEOUT_MILLIS) { exists(matcher) }
        }
        // Scrolled into view first, as a user would: with a large font menu items sit below the fold.
        // Only when it is not in view: a scroll to a row already shown once stalled for minutes.
        val target = composeRule.onAllNodes(matcher).onFirst()
        if (!fullyShown(target.fetchSemanticsNode())) {
            timed("scroll to ${matcher.description}") { runCatching { target.performScrollTo() } }
        }
        timed("tap ${matcher.description}") { target.performClick() }
        idle()
    }

    /**
     * A lazy list composes only the rows near the screen: with a large font a row further down
     * is not in the tree at all until the list is scrolled to it, as a user would scroll.
     */
    private fun revealInLazyList(matcher: SemanticsMatcher): Boolean {
        val lists = composeRule.onAllNodes(hasScrollToNodeAction())
        val count = lists.fetchSemanticsNodes(atLeastOneRootRequired = false).size
        for (index in 0 until count) {
            runCatching { lists[index].performScrollToNode(matcher) }
            if (exists(matcher)) return true
        }
        return false
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

        /** What moves each first-run screen on: «Next», the lessons' «Start», a lesson's «Skip». */
        private val FIRST_RUN_FORWARD = listOf(
            FirstRunTestTags.Next,
            "gesture_onboarding_start",
            "gesture_onboarding_skip",
        )
        private const val PAGES_PER_TEST = 2
        private const val TIMEOUT_MILLIS = 10_000L
        private const val BACK_ATTEMPTS = 3
        private const val SLOW_STEP_MILLIS = 90_000L
        private const val SLOWEST_SHOWN = 4
        private const val WALK_TIMEOUT_MINUTES = 20L
        private const val REVEAL_AFTER_MILLIS = 3_000L
        private const val MAX_SCROLL_PAGES = 8
        private const val SCROLL_PAGE_FRACTION = 0.8f
        private val isVerticallyScrollable = SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange)

        private const val PAGE_GROUP = -4
        private const val PAGE_STATES_GROUP = -5
        private const val SUBPAGE_GROUP_BASE = -10
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


        @JvmStatic
        @Parameterized.Parameters(name = "{index}")
        fun parameters(): List<Array<Any>> {
            val groups = listOf(BROWSER_GROUP, LIBRARY_GROUP, FIRST_RUN_GROUP, PAGE_GROUP, PAGE_STATES_GROUP) +
                settingsSlices().indices + subpageGroups()
            return groups.flatMap { group -> AuditConfig.All.map { config -> arrayOf<Any>(group, config) } }
        }
    }
}
