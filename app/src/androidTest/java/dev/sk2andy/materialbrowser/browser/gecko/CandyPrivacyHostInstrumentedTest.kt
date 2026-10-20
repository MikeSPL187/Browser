package dev.sk2andy.materialbrowser.browser.gecko

import android.content.Context
import android.os.SystemClock
import android.view.View
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.net.InetAddress
import java.net.ServerSocket
import java.net.SocketException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.geckoview.ContentBlocking
import dev.sk2andy.materialbrowser.browser.engine.GeckoBrowserSessionState
import dev.sk2andy.materialbrowser.browser.engine.GeckoPrivacyEvent
import dev.sk2andy.materialbrowser.browser.engine.GeckoPrivacyEventSink
import dev.sk2andy.materialbrowser.browser.engine.GeckoPrivacyPolicy

@RunWith(AndroidJUnit4::class)
class CandyPrivacyHostInstrumentedTest {
    @Test
    fun rapidPolicyRevisionsKeepOneBootstrapAndGateOnTheCurrentRevision() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val server = FixtureServer()
        val policiesReady = CountDownLatch(3)
        val readyCount = AtomicInteger()
        val loadedPage = CountDownLatch(1)
        lateinit var runtime: GeckoRuntimeHandle
        lateinit var session: GeckoBrowserSession
        lateinit var view: View

        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            runtime = GeckoRuntimeOwner.getOrCreate(context)
            session = runtime.createSession(
                profileId = "privacy-rapid-policy",
                isPrivate = false,
                privacyPolicy = GeckoPrivacyPolicy.Disabled.copy(pageHost = PAGE_HOST),
            )
            view = session.createView(context)
            session.setStateListener { state ->
                if (state.title == ALLOWED_TITLE) loadedPage.countDown()
            }
            listOf(
                GeckoPrivacyPolicy.Disabled.copy(pageHost = PAGE_HOST),
                GeckoPrivacyPolicy.Disabled.copy(
                    pageHost = PAGE_HOST,
                    hideCookieConsent = true,
                    cookieBannerRemovalDisabled = false,
                ),
                GeckoPrivacyPolicy.Disabled.copy(
                    pageHost = PAGE_HOST,
                    hideCookieConsent = false,
                    cookieBannerRemovalDisabled = true,
                ),
            ).forEach { policy ->
                session.updatePrivacyPolicy(policy) {
                    readyCount.incrementAndGet()
                    policiesReady.countDown()
                }
            }
            assertTrue(session.loadUrl(server.pageUrl(PAGE_HOST)))
        }

        try {
            assertTrue("Current Privacy policy was not acknowledged", policiesReady.await(20, TimeUnit.SECONDS))
            assertEquals(3, readyCount.get())
            assertTrue("Final policy was not applied", loadedPage.await(20, TimeUnit.SECONDS))
        } finally {
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                session.releaseView(view)
                session.close()
            }
            server.close()
        }
    }

    @Test
    fun thirdPartyCookieSettingBlocksGloballyAndAllowsConfirmedSiteException() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val server = FixtureServer()
        lateinit var runtime: GeckoRuntimeHandle
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            runtime = GeckoRuntimeOwner.getOrCreate(context)
        }

        try {
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                runtime.setBlockThirdPartyCookies(true)
            }
            assertCookieScenario(
                context = context,
                runtime = runtime,
                server = server,
                profileId = "privacy-cookie-blocked",
                policy = GeckoPrivacyPolicy.Disabled.copy(
                    pageHost = COOKIE_PAGE_HOST,
                    blockThirdPartyCookies = true,
                ),
                expectedTitle = COOKIE_BLOCKED_TITLE,
            )
            assertCookieScenario(
                context = context,
                runtime = runtime,
                server = server,
                profileId = "privacy-cookie-site-allowed",
                policy = GeckoPrivacyPolicy.Disabled.copy(
                    pageHost = COOKIE_PAGE_HOST,
                    blockThirdPartyCookies = true,
                    allowThirdPartyCookiesForSite = true,
                ),
                expectedTitle = COOKIE_ALLOWED_TITLE,
            )
            assertCookieScenario(
                context = context,
                runtime = runtime,
                server = server,
                profileId = "privacy-cookie-site-allowed",
                policy = GeckoPrivacyPolicy.Disabled.copy(
                    pageHost = COOKIE_PAGE_HOST,
                    blockThirdPartyCookies = true,
                ),
                expectedTitle = COOKIE_BLOCKED_TITLE,
            )
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                runtime.setBlockThirdPartyCookies(false)
            }
            assertCookieScenario(
                context = context,
                runtime = runtime,
                server = server,
                profileId = "privacy-cookie-global-allowed",
                policy = GeckoPrivacyPolicy.Disabled.copy(
                    pageHost = COOKIE_PAGE_HOST,
                    blockThirdPartyCookies = false,
                ),
                expectedTitle = COOKIE_ALLOWED_TITLE,
            )
        } finally {
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                runtime.setBlockThirdPartyCookies(true)
            }
            server.close()
        }
    }

    @Test
    fun allowedSiteCookieBehaviorIsAppliedBeforeInitialNavigation() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val server = FixtureServer()
        val loadingStarted = CountDownLatch(1)
        val navigationFinished = CountDownLatch(1)
        val loadedPage = CountDownLatch(1)
        lateinit var runtime: GeckoViewRuntimeHandle
        lateinit var session: GeckoBrowserSession
        lateinit var view: View

        try {
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                runtime = GeckoRuntimeOwner.getOrCreate(context) as GeckoViewRuntimeHandle
                runtime.setBlockThirdPartyCookies(true)
                session = runtime.createSession(
                    profileId = "privacy-cookie-initial-navigation",
                    isPrivate = false,
                    privacyPolicy = GeckoPrivacyPolicy.Disabled.copy(
                        pageHost = COOKIE_PAGE_HOST,
                        blockThirdPartyCookies = true,
                        allowThirdPartyCookiesForSite = true,
                    ),
                )
                view = session.createView(context)
                session.setStateListener { state ->
                    if (state.url == server.slowCookiePageUrl() && state.isLoading) {
                        loadingStarted.countDown()
                    }
                    if (
                        state.url == server.slowCookiePageUrl() &&
                        state.title == COOKIE_ALLOWED_TITLE &&
                        !state.isLoading &&
                        state.lastNavigationSucceeded != null
                    ) {
                        navigationFinished.countDown()
                    }
                    if (state.title == COOKIE_ALLOWED_TITLE) loadedPage.countDown()
                }

                assertEquals(
                    ContentBlocking.CookieBehavior.ACCEPT_FIRST_PARTY,
                    runtime.normalCookieBehaviorForTesting(),
                )
                assertTrue(session.loadUrl(server.cookiePageUrl()))
                assertEquals(
                    ContentBlocking.CookieBehavior.ACCEPT_FIRST_PARTY_AND_ISOLATE_OTHERS,
                    runtime.normalCookieBehaviorForTesting(),
                )
                session.stop()
                assertEquals(
                    ContentBlocking.CookieBehavior.ACCEPT_FIRST_PARTY,
                    runtime.normalCookieBehaviorForTesting(),
                )
                session.releaseView(view)
                session.close()

                session = runtime.createSession(
                    profileId = "privacy-cookie-inactive-navigation",
                    isPrivate = false,
                    privacyPolicy = GeckoPrivacyPolicy.Disabled.copy(
                        pageHost = COOKIE_PAGE_HOST,
                        blockThirdPartyCookies = true,
                        allowThirdPartyCookiesForSite = true,
                    ),
                )
                view = session.createView(context)
                session.setStateListener { state ->
                    if (state.url == server.slowCookiePageUrl() && state.isLoading) {
                        loadingStarted.countDown()
                    }
                    if (
                        state.url == server.slowCookiePageUrl() &&
                        state.title == COOKIE_ALLOWED_TITLE &&
                        !state.isLoading &&
                        state.lastNavigationSucceeded != null
                    ) {
                        navigationFinished.countDown()
                    }
                    if (state.title == COOKIE_ALLOWED_TITLE) loadedPage.countDown()
                }
                assertTrue(session.loadUrl(server.slowCookiePageUrl()))
                assertEquals(
                    ContentBlocking.CookieBehavior.ACCEPT_FIRST_PARTY_AND_ISOLATE_OTHERS,
                    runtime.normalCookieBehaviorForTesting(),
                )
                session.setActive(true)
            }
            assertTrue(
                "Inactive cookie navigation did not start",
                loadingStarted.await(20, TimeUnit.SECONDS),
            )
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                session.setActive(false)
                assertEquals(
                    ContentBlocking.CookieBehavior.ACCEPT_FIRST_PARTY_AND_ISOLATE_OTHERS,
                    runtime.normalCookieBehaviorForTesting(),
                )
            }
            server.releaseSlowCookiePage()

            assertTrue(
                "Allowed cookie page did not load after inactive navigation",
                loadedPage.await(20, TimeUnit.SECONDS),
            )
            assertTrue(
                "Inactive cookie navigation did not finish",
                navigationFinished.await(20, TimeUnit.SECONDS),
            )
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                assertEquals(
                    ContentBlocking.CookieBehavior.ACCEPT_FIRST_PARTY,
                    runtime.normalCookieBehaviorForTesting(),
                )
            }
        } finally {
            server.releaseSlowCookiePage()
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                session.releaseView(view)
                session.setActive(false)
                session.close()
            }
            server.close()
        }
    }

    @Test
    fun allowedSiteCookieBehaviorIsAppliedBeforeSessionRestore() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val server = FixtureServer()
        val initialPageLoaded = CountDownLatch(1)
        val restoreLoadingStarted = CountDownLatch(1)
        val restoreNavigationFinished = CountDownLatch(1)
        val restoredPageLoaded = CountDownLatch(1)
        val policy = GeckoPrivacyPolicy.Disabled.copy(
            pageHost = COOKIE_PAGE_HOST,
            blockThirdPartyCookies = true,
            allowThirdPartyCookiesForSite = true,
        )
        lateinit var runtime: GeckoViewRuntimeHandle
        lateinit var session: GeckoBrowserSession
        lateinit var view: View
        var restoreGate: FixtureServer.ResponseGate? = null

        try {
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                runtime = GeckoRuntimeOwner.getOrCreate(context) as GeckoViewRuntimeHandle
                runtime.setBlockThirdPartyCookies(true)
                session = runtime.createSession(
                    profileId = "privacy-cookie-session-restore",
                    isPrivate = false,
                    privacyPolicy = policy,
                )
                view = session.createView(context)
                session.setStateListener { state ->
                    if (state.title == COOKIE_ALLOWED_TITLE) initialPageLoaded.countDown()
                }
                session.setActive(true)
                assertTrue(session.loadUrl(server.cookiePageUrl()))
            }
            assertTrue(
                "Initial allowed cookie page did not load",
                initialPageLoaded.await(20, TimeUnit.SECONDS),
            )
            val historyDeadline = SystemClock.elapsedRealtime() + 20_000L
            while (
                session.historyUrlAtOffset(0) != server.cookiePageUrl() &&
                SystemClock.elapsedRealtime() < historyDeadline
            ) {
                SystemClock.sleep(25L)
            }
            assertEquals(server.cookiePageUrl(), session.historyUrlAtOffset(0))

            lateinit var snapshot: String
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                snapshot = requireNotNull(session.sessionStateSnapshot())
                session.releaseView(view)
                session.setActive(false)
                session.close()
                assertEquals(
                    ContentBlocking.CookieBehavior.ACCEPT_FIRST_PARTY,
                    runtime.normalCookieBehaviorForTesting(),
                )

                session = runtime.createSession(
                    profileId = "privacy-cookie-session-restore",
                    isPrivate = false,
                    privacyPolicy = policy,
                )
                view = session.createView(context)
                session.setStateListener { state ->
                    if (state.url == server.cookiePageUrl() && state.isLoading) {
                        restoreLoadingStarted.countDown()
                    }
                    if (
                        state.url == server.cookiePageUrl() &&
                        state.title == COOKIE_ALLOWED_TITLE &&
                        !state.isLoading &&
                        state.lastNavigationSucceeded != null
                    ) {
                        restoreNavigationFinished.countDown()
                    }
                    if (state.title == COOKIE_ALLOWED_TITLE) restoredPageLoaded.countDown()
                }
                restoreGate = server.blockNextCookiePage()
                assertTrue(session.restoreSessionState(snapshot))
                assertEquals(
                    ContentBlocking.CookieBehavior.ACCEPT_FIRST_PARTY_AND_ISOLATE_OTHERS,
                    runtime.normalCookieBehaviorForTesting(),
                )
            }
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                assertEquals(
                    ContentBlocking.CookieBehavior.ACCEPT_FIRST_PARTY_AND_ISOLATE_OTHERS,
                    runtime.normalCookieBehaviorForTesting(),
                )
                session.setActive(true)
            }
            assertTrue(
                "Restored cookie navigation did not start",
                restoreLoadingStarted.await(20, TimeUnit.SECONDS),
            )
            assertTrue(
                "Restored cookie fixture was not requested",
                requireNotNull(restoreGate).started.await(20, TimeUnit.SECONDS),
            )
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                session.setActive(false)
                assertEquals(
                    ContentBlocking.CookieBehavior.ACCEPT_FIRST_PARTY_AND_ISOLATE_OTHERS,
                    runtime.normalCookieBehaviorForTesting(),
                )
            }
            requireNotNull(restoreGate).release.countDown()

            assertTrue(
                "Allowed cookie page did not load after session restore",
                restoredPageLoaded.await(20, TimeUnit.SECONDS),
            )
            assertTrue(
                "Restored cookie navigation did not finish",
                restoreNavigationFinished.await(20, TimeUnit.SECONDS),
            )
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                assertEquals(
                    ContentBlocking.CookieBehavior.ACCEPT_FIRST_PARTY,
                    runtime.normalCookieBehaviorForTesting(),
                )
            }
        } finally {
            restoreGate?.release?.countDown()
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                session.releaseView(view)
                session.setActive(false)
                session.close()
            }
            server.close()
        }
    }

    @Test
    fun recognizedCompatibilityHostIsObservedWithoutBlockingTheRequest() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val server = FixtureServer()
        val observed = CountDownLatch(1)
        val compatibilityEvent = AtomicReference<GeckoPrivacyEvent>()
        lateinit var runtime: GeckoRuntimeHandle
        lateinit var session: GeckoBrowserSession
        lateinit var view: View
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            runtime = GeckoRuntimeOwner.getOrCreate(context)
            session = runtime.createSession(
                profileId = "privacy-compatibility-observation",
                isPrivate = false,
                privacyPolicy = GeckoPrivacyPolicy(
                    pageHost = PAGE_HOST,
                    blockAdsAndTrackers = false,
                    hideCookieConsent = false,
                    cookieBannerRemovalDisabled = true,
                    pausedHosts = emptySet(),
                    candyRules = emptyList(),
                    compatibilityRequestHosts = setOf(TRACKER_HOST),
                ),
                privacyEventSink = GeckoPrivacyEventSink { event ->
                    if (event.isCompatibilityObservation) {
                        compatibilityEvent.set(event)
                        observed.countDown()
                    }
                },
            )
            view = session.createView(context)
            assertTrue(session.loadUrl(server.pageUrl(PAGE_HOST)))
        }

        try {
            assertTrue(
                "Compatibility request was not observed",
                observed.await(20, TimeUnit.SECONDS),
            )
            assertEquals(server.scriptUrl, compatibilityEvent.get().requestUrl)
            assertFalse(compatibilityEvent.get().wasBlocked)
        } finally {
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                session.releaseView(view)
                session.close()
            }
            server.close()
        }
    }

    // Since 13.09 (cf17419d) and H4 (#123) a Gecko page sits below the status bar on the host's
    // native margin and Vola writes nothing into it: the old page-level spacer, header shift and
    // native fallback are System WebView only. A document inset in the policy must not move the page.
    @Test
    fun documentInsetLeavesGeckoPageAtItsOwnTop() {
        assertPageAtItsOwnTop(isPrivate = false)
    }

    @Test
    fun privateDocumentInsetLeavesGeckoPageAtItsOwnTop() {
        assertPageAtItsOwnTop(isPrivate = true)
    }

    private fun assertPageAtItsOwnTop(isPrivate: Boolean) {
        val server = FixtureServer()
        val finalState = AtomicReference<GeckoBrowserSessionState>()
        lateinit var session: GeckoBrowserSession
        lateinit var view: View
        ActivityScenario.launch(GeckoScrollTestActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val policy = GeckoPrivacyPolicy.Disabled.copy(
                    pageHost = COOKIE_PAGE_HOST,
                    topInsetPx = SAFE_AREA_INSET_PX,
                    navigationGeneration = 1,
                )
                session = GeckoRuntimeOwner.getOrCreate(activity).createSession(
                    profileId = "privacy-safe-area-$isPrivate",
                    isPrivate = isPrivate,
                    privacyPolicy = policy,
                )
                view = session.createView(activity)
                activity.setContentView(view)
                session.setStateListener { state -> finalState.set(state) }
                session.setActive(true)
                assertTrue(session.loadUrl(server.safeAreaPageUrl()))
            }

            try {
                val loaded = awaitTitle(finalState, SAFE_AREA_REPORT_PREFIX)
                val (headerTop, spacerHeight) = loaded.removePrefix(SAFE_AREA_REPORT_PREFIX)
                    .split(':')
                    .map { value -> value.toDoubleOrNull() ?: 0.0 }
                assertEquals("Fixed header moved: $loaded", 0.0, headerTop, 0.5)
                assertEquals("Vola drew a spacer: $loaded", 0.0, spacerHeight, 0.5)
                scenario.onActivity { session.scrollToVerticalOffset(SCROLL_OFFSET_PX) }
                val scrolled = awaitTitle(finalState, SCROLLED_REPORT_PREFIX)
                val stickyTop = scrolled.removePrefix(SCROLLED_REPORT_PREFIX).toDoubleOrNull()
                assertEquals("Sticky header moved: $scrolled", 0.0, stickyTop ?: -1.0, 0.5)
            } finally {
                scenario.onActivity {
                    session.releaseView(view)
                    session.setActive(false)
                    session.close()
                }
                server.close()
            }
        }
    }

    private fun awaitTitle(state: AtomicReference<GeckoBrowserSessionState>, prefix: String): String {
        val deadline = SystemClock.elapsedRealtime() + 45_000
        while (SystemClock.elapsedRealtime() < deadline) {
            state.get()?.title?.takeIf { title -> title.startsWith(prefix) }?.let { return it }
            SystemClock.sleep(50)
        }
        throw AssertionError("Page did not report $prefix; state=${state.get()}")
    }

    private fun assertCookieScenario(
        context: Context,
        runtime: GeckoRuntimeHandle,
        server: FixtureServer,
        profileId: String,
        policy: GeckoPrivacyPolicy,
        expectedTitle: String,
    ) {
        val titleReached = CountDownLatch(1)
        val finalState = AtomicReference<GeckoBrowserSessionState>()
        lateinit var session: GeckoBrowserSession
        lateinit var view: View
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            session = runtime.createSession(
                profileId = profileId,
                isPrivate = false,
                privacyPolicy = policy,
            )
            view = session.createView(context)
            session.setActive(true)
            session.setStateListener { state ->
                finalState.set(state)
                if (state.title == expectedTitle) titleReached.countDown()
            }
            assertTrue(session.loadUrl(server.cookiePageUrl()))
        }

        try {
            assertTrue(
                "Third-party cookie fixture did not reach $expectedTitle; state=${finalState.get()}",
                titleReached.await(20, TimeUnit.SECONDS),
            )
        } finally {
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                session.releaseView(view)
                session.setActive(false)
                session.close()
            }
        }
    }

    private class FixtureServer : AutoCloseable {
        data class ResponseGate(
            val started: CountDownLatch = CountDownLatch(1),
            val release: CountDownLatch = CountDownLatch(1),
        )

        private val socket = ServerSocket(0, 8, InetAddress.getByName("127.0.0.1"))
        private val slowCookiePageRelease = CountDownLatch(1)
        private val nextCookiePageGate = AtomicReference<ResponseGate?>()
        val scriptUrl = "http://$TRACKER_HOST:${socket.localPort}/probe.js"
        private val thread = Thread({ serve() }, "gecko-privacy-fixture").apply {
            isDaemon = true
            start()
        }

        fun pageUrl(host: String) = "http://$host:${socket.localPort}/"

        fun cookiePageUrl() = "http://$COOKIE_PAGE_HOST:${socket.localPort}/cookie-page"

        fun slowCookiePageUrl() = "http://$COOKIE_PAGE_HOST:${socket.localPort}/slow-cookie-page"

        fun releaseSlowCookiePage() = slowCookiePageRelease.countDown()

        fun blockNextCookiePage(): ResponseGate = ResponseGate().also { gate ->
            check(nextCookiePageGate.compareAndSet(null, gate))
        }

        fun safeAreaPageUrl() = "http://$COOKIE_PAGE_HOST:${socket.localPort}/safe-area"



        private fun serve() {
            while (!socket.isClosed) {
                try {
                    socket.accept().use { connection ->
                        val reader = connection.getInputStream().bufferedReader()
                        val requestLine = reader.readLine().orEmpty()
                        var host = ""
                        while (true) {
                            val header = reader.readLine() ?: break
                            if (header.isEmpty()) break
                            if (header.startsWith("Host:", ignoreCase = true)) {
                                host = header.substringAfter(':').substringBefore(':').trim()
                            }
                        }
                        val isScript = requestLine.contains(" /probe.js ")
                        val isCookieFrame = requestLine.contains(" /cookie-frame ")
                        val isSlowCookiePage = requestLine.contains(" /slow-cookie-page ")
                        val isCookiePage = requestLine.contains(" /cookie-page ")
                        val responseGate = if (isCookiePage) nextCookiePageGate.getAndSet(null) else null
                        val body = when {
                            isScript -> "document.title='$ALLOWED_TITLE';"
                            isCookieFrame -> cookieFrame()
                            isSlowCookiePage -> cookiePage()
                            isCookiePage -> cookiePage()
                            requestLine.contains(" /safe-area ") -> safeAreaPage()
                            else -> page(host)
                        }.toByteArray()
                        connection.getOutputStream().apply {
                            write("HTTP/1.1 200 OK\r\n".toByteArray())
                            write(
                                "Content-Type: ${if (isScript) "text/javascript" else "text/html"}; charset=utf-8\r\n"
                                    .toByteArray(),
                            )
                            write("Content-Length: ${body.size}\r\n".toByteArray())
                            write("Connection: close\r\n\r\n".toByteArray())
                            flush()
                            responseGate?.started?.countDown()
                            responseGate?.release?.await(20, TimeUnit.SECONDS)
                            if (isSlowCookiePage) {
                                slowCookiePageRelease.await(20, TimeUnit.SECONDS)
                            }
                            write(body)
                            flush()
                        }
                    }
                } catch (error: SocketException) {
                    if (socket.isClosed) return
                    // Gecko may cancel an in-flight fixture response while applying a policy
                    // reload. A closed peer is expected and must not crash the instrumentation
                    // process that is validating the next session binding.
                }
            }
        }

        private fun page(host: String): String = """
            <!doctype html>
            <html><head><title>$BLOCKED_TITLE</title></head>
            <body><script src="$scriptUrl"></script></body></html>
        """.trimIndent()

        private fun cookiePage(): String = """
            <!doctype html>
            <html><head><title>Checking third-party cookie</title></head><body>
            <script>
            addEventListener('message', function(event) {
              if (event.origin !== 'http://$COOKIE_FRAME_HOST:${socket.localPort}') return;
              document.title = event.data === 'cookie-present' ?
                '$COOKIE_ALLOWED_TITLE' : '$COOKIE_BLOCKED_TITLE';
            });
            </script>
            <iframe src="http://$COOKIE_FRAME_HOST:${socket.localPort}/cookie-frame"></iframe>
            </body></html>
        """.trimIndent()

        private fun cookieFrame(): String = """
            <!doctype html>
            <html><body><script>
            var cookieVisible = false;
            try {
              document.cookie = 'candyThirdPartyCookie=present; path=/';
              cookieVisible = document.cookie.indexOf('candyThirdPartyCookie=present') >= 0;
            } catch (error) {}
            parent.postMessage(
              cookieVisible ? 'cookie-present' : 'cookie-blocked',
              'http://$COOKIE_PAGE_HOST:${socket.localPort}'
            );
            </script></body></html>
        """.trimIndent()

        // Reports where the fixed header sits and whether anything was drawn above the document.
        private fun safeAreaPage(): String = """
            <!doctype html>
            <html><head><title>Checking safe area</title>
            <meta name="viewport" content="width=device-width,initial-scale=1,viewport-fit=cover">
            <style>
              html, body { margin: 0; min-height: 200vh; }
              #header { position: fixed; inset: 0 0 auto; height: 24px; background: red; }
              #lead { height: 200px; }
              #sticky { position: sticky; top: 0; height: 24px; background: blue; }
              #content { height: 4000px; }
            </style></head><body><header id="header">Header</header>
            <div id="lead"></div><nav id="sticky">Sticky</nav><main id="content"></main><script>
              addEventListener('load', () => setTimeout(() => {
                const top = document.querySelector('#header').getBoundingClientRect().top;
                const spacer = Number.parseFloat(
                  getComputedStyle(document.documentElement, '::before').height
                ) || 0;
                document.title = '$SAFE_AREA_REPORT_PREFIX' + top + ':' + spacer;
              }, 500));
              addEventListener('scroll', () => {
                if (scrollY > 100) {
                  document.title = '$SCROLLED_REPORT_PREFIX' +
                    document.querySelector('#sticky').getBoundingClientRect().top;
                }
              }, { passive: true });
            </script></body></html>
        """.trimIndent()

        override fun close() {
            socket.close()
            thread.join(2_000)
        }
    }

    private companion object {
        const val PAGE_HOST = "page.candy.localhost"
        const val TRACKER_HOST = "tracker.ads.localhost"
        const val COOKIE_PAGE_HOST = "localhost"
        const val COOKIE_FRAME_HOST = "127.0.0.1"
        const val BLOCKED_TITLE = "Candy Privacy Blocked"
        const val ALLOWED_TITLE = "Candy Privacy Allowed"
        const val COOKIE_BLOCKED_TITLE = "Third-party cookie blocked"
        const val COOKIE_ALLOWED_TITLE = "Third-party cookie allowed"
        const val SAFE_AREA_INSET_PX = 96
        const val SCROLL_OFFSET_PX = 600
        const val SAFE_AREA_REPORT_PREFIX = "Candy safe area: "
        const val SCROLLED_REPORT_PREFIX = "Candy sticky top: "
    }
}
