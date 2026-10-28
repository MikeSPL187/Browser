package dev.sk2andy.materialbrowser.browser.gecko

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ActivityScenario
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.browser.actions.WebContentTarget
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.sk2andy.materialbrowser.MainActivity
import dev.sk2andy.materialbrowser.browser.BrowserTab
import dev.sk2andy.materialbrowser.data.BrowserSessionStore
import dev.sk2andy.materialbrowser.grantNotificationPermissionForTests
import dev.sk2andy.materialbrowser.data.GestureOnboardingStore
import dev.sk2andy.materialbrowser.ui.v5.V5LinkGlanceTestTags
import dev.sk2andy.materialbrowser.ui.v5.V5OverviewTestTags
import java.io.Closeable
import java.net.InetAddress
import java.net.ServerSocket
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import dev.sk2andy.materialbrowser.browser.engine.GeckoToppingHostState

@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class GeckoLinkPeekInstrumentedTest {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val preferences by lazy {
        context.getSharedPreferences(BrowserSessionStore.PREFERENCES_NAME, Context.MODE_PRIVATE)
    }

    @Before
    fun setUp() {
        // The private tab the test opens asks for notifications; the system dialog would take
        // the taps on the preview ("Failed to inject touch input").
        grantNotificationPermissionForTests()
        preferences.edit().clear().commit()
        GestureOnboardingStore(context).markCompleted()
    }

    @After
    fun tearDown() {
        preferences.edit().clear().commit()
    }

    @Test
    fun longPressedGeckoLinkShowsPreviewOpensPrivateBackgroundTabAndDismisses() {
        FixtureServer().use { server ->
            val toppingHostReady = CountDownLatch(1)
            instrumentation.runOnMainSync {
                GeckoRuntimeOwner.getOrCreate(context)
                    .toppings
                    .setStateListener { state ->
                        if (state == GeckoToppingHostState.Ready) toppingHostReady.countDown()
                    }
            }
            assertTrue(
                "Gecko Topping host did not initialize",
                toppingHostReady.await(20, TimeUnit.SECONDS),
            )
            instrumentation.waitForIdleSync()
            Thread.sleep(PRIVACY_HOST_SETTLE_MILLIS)

            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                lateinit var sourceTab: BrowserTab
                scenario.onActivity { activity ->
                    val controller = activity.browserControllerForTesting()
                    assertTrue(controller.usesGeckoEngine)
                    assertTrue(controller.openUrl(server.url(SOURCE_PATH)))
                }
                awaitCondition {
                    var loaded = false
                    scenario.onActivity { activity ->
                        val controller = activity.browserControllerForTesting()
                        loaded = server.requested(SOURCE_PATH)
                    }
                    loaded
                }
                lateinit var initialTabId: String
                scenario.onActivity { activity ->
                    initialTabId = activity.browserControllerForTesting().selectedTabId
                }
                // If the overview opened over the new page, its card brings the page back.
                val card = composeRule.onAllNodesWithTag(V5OverviewTestTags.card(initialTabId), useUnmergedTree = true)
                if (card.fetchSemanticsNodes().isNotEmpty()) card.onFirst().performClick()
                awaitCondition {
                    var attached = false
                    scenario.onActivity { activity ->
                        attached = activity.browserControllerForTesting()
                            .selectedGeckoViewForTesting()
                            ?.isAttachedToWindow == true
                    }
                    attached
                }
                scenario.onActivity { activity ->
                    val controller = activity.browserControllerForTesting()
                    assertTrue(controller.openLinkInPrivate(server.url(SOURCE_PATH)))
                    sourceTab = controller.selectedTab
                }
                var selectedState = "unavailable"
                awaitCondition(description = { selectedState }) {
                    var loaded = false
                    scenario.onActivity { activity ->
                        val controller = activity.browserControllerForTesting()
                        selectedState = "id=${controller.selectedTab.id}, expected=${sourceTab.id}, " +
                            "private=${controller.selectedTab.isIncognito}, " +
                            "url=${controller.selectedTab.url}, expectedUrl=${server.url(SOURCE_PATH)}, " +
                            "loading=${controller.selectedTab.isLoading}"
                        loaded = server.requested(SOURCE_PATH) &&
                            controller.selectedTab.id == sourceTab.id &&
                            controller.selectedTab.isIncognito &&
                            controller.selectedTab.url == server.url(SOURCE_PATH) &&
                            !controller.selectedTab.isLoading
                    }
                    loaded
                }

                scenario.onActivity { activity ->
                    activity.browserControllerForTesting()
                        .dispatchSelectedGeckoContentTargetForTesting(
                            WebContentTarget(linkUrl = server.url(TARGET_PATH)),
                        )
                }

                awaitCondition {
                    var visible = false
                    scenario.onActivity { activity ->
                        val controller = activity.browserControllerForTesting()
                        visible = controller.contentActions.isLinkPeekVisible &&
                            controller.contentActions.sourceTabId == sourceTab.id &&
                            controller.contentActions.target?.linkUrl == server.url(TARGET_PATH)
                    }
                    visible
                }
                composeRule.onNodeWithTag(V5LinkGlanceTestTags.Root).assertIsDisplayed()
                awaitCondition {
                    var previewCreated = false
                    scenario.onActivity { activity ->
                        previewCreated = activity.browserControllerForTesting()
                            .activeLinkPeekPreviewCountForTesting == 1
                    }
                    previewCreated && server.requested(TARGET_PATH)
                }

                val originalTabIds = mutableSetOf<String>()
                scenario.onActivity { activity ->
                    originalTabIds += activity.browserControllerForTesting().tabs.map(BrowserTab::id)
                }
                // «In background» lives in the card's «More».
                composeRule.onNodeWithTag(V5LinkGlanceTestTags.More).performClick()
                composeRule.onNodeWithText(context.getString(R.string.v5_glance_open_background)).performClick()
                composeRule.waitForIdle()
                var openState = "unavailable"
                awaitCondition(description = { openState }) {
                    var opened = false
                    scenario.onActivity { activity ->
                        val controller = activity.browserControllerForTesting()
                        val createdTabs = controller.tabs.filter { tab -> tab.id !in originalTabIds }
                        openState = "created=${createdTabs.map { it.url to it.isIncognito }}, " +
                            "selected=${controller.selectedTabId}, source=${sourceTab.id}, " +
                            "visible=${controller.contentActions.isVisible}, " +
                            "committing=${controller.contentActions.isLinkPeekCommitting}"
                        opened = createdTabs.size == 1 &&
                            createdTabs.single().url == server.url(TARGET_PATH) &&
                            createdTabs.single().isIncognito &&
                            controller.selectedTabId == sourceTab.id &&
                            !controller.contentActions.isVisible
                    }
                    opened
                }

                awaitCondition {
                    var released = false
                    scenario.onActivity { activity ->
                        released = activity.browserControllerForTesting()
                            .activeLinkPeekPreviewCountForTesting == 0
                    }
                    released
                }
                scenario.onActivity { activity ->
                    activity.browserControllerForTesting()
                        .dispatchSelectedGeckoContentTargetForTesting(
                            WebContentTarget(linkUrl = server.url(TARGET_PATH)),
                        )
                }
                awaitCondition {
                    var visible = false
                    scenario.onActivity { activity ->
                        visible = activity.browserControllerForTesting()
                            .contentActions.isLinkPeekVisible
                    }
                    visible
                }
                Espresso.pressBack()
                awaitCondition {
                    var dismissed = false
                    scenario.onActivity { activity ->
                        dismissed = !activity.browserControllerForTesting().contentActions.isVisible
                    }
                    dismissed
                }
                composeRule.onNodeWithTag(V5LinkGlanceTestTags.Root).assertDoesNotExist()

                scenario.onActivity { activity ->
                    val controller = activity.browserControllerForTesting()
                    assertEquals(sourceTab.id, controller.selectedTabId)
                    assertTrue(controller.selectedTab.isIncognito)
                    assertTrue(controller.supportsPageContentActions)
                    controller.tabs
                        .filter { tab -> tab.id != sourceTab.id }
                        .map(BrowserTab::id)
                        .forEach(controller::closeTab)
                }
            }
        }
    }

    private fun awaitCondition(
        timeoutMillis: Long = 30_000,
        description: () -> String = { "" },
        condition: () -> Boolean,
    ) {
        val deadline = System.currentTimeMillis() + timeoutMillis
        while (System.currentTimeMillis() < deadline) {
            instrumentation.waitForIdleSync()
            if (condition()) return
            Thread.sleep(50)
        }
        assertTrue(
            "Condition not met within ${timeoutMillis}ms: ${description()}",
            condition(),
        )
    }

    private class FixtureServer : Closeable {
        private val socket = ServerSocket(0, REQUEST_BACKLOG, InetAddress.getByName("127.0.0.1"))
        private val baseUrl = "http://127.0.0.1:${socket.localPort}/"
        private val paths = CopyOnWriteArrayList<String>()
        private val thread = Thread(::serve, "gecko-link-peek-fixture").apply {
            isDaemon = true
            start()
        }

        fun url(path: String): String = "$baseUrl$path"

        fun requested(path: String): Boolean = "/$path" in paths

        private fun serve() {
            while (!socket.isClosed) {
                val client = runCatching { socket.accept() }.getOrNull() ?: return
                client.use { connection ->
                    runCatching {
                        val reader = connection.getInputStream().bufferedReader()
                        val path = reader.readLine()?.split(' ')?.getOrNull(1).orEmpty()
                        paths += path
                        while (!reader.readLine().isNullOrEmpty()) {
                            // Drain request headers before writing the local fixture.
                        }
                        val body = if (path == "/$SOURCE_PATH") {
                            """
                            <!doctype html>
                            <html><head><meta name="viewport" content="width=device-width"></head>
                            <body style="margin:0">
                              <a href="${url(TARGET_PATH)}" style="position:fixed;inset:0;display:flex;align-items:center;justify-content:center;font-size:32px">
                                Open Link Peek
                              </a>
                            </body></html>
                            """.trimIndent()
                        } else {
                            "<html><head><title>Peek target</title></head><body>Preview loaded</body></html>"
                        }.toByteArray()
                        connection.getOutputStream().buffered().use { output ->
                            output.write(
                                "HTTP/1.1 200 OK\r\nContent-Type: text/html; charset=utf-8\r\n".toByteArray(),
                            )
                            output.write(
                                "Content-Length: ${body.size}\r\nConnection: close\r\n\r\n".toByteArray(),
                            )
                            output.write(body)
                        }
                    }
                }
            }
        }

        override fun close() {
            socket.close()
            thread.join(1_000)
        }
    }

    private companion object {
        const val REQUEST_BACKLOG = 4
        const val PRIVACY_HOST_SETTLE_MILLIS = 2_000L
        const val SOURCE_PATH = "source"
        const val TARGET_PATH = "target"
    }
}
