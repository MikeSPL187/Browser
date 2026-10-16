package dev.sk2andy.materialbrowser.ui

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.sk2andy.materialbrowser.browser.BrowserController
import dev.sk2andy.materialbrowser.browser.SystemWebViewLegacyTestAccess
import dev.sk2andy.materialbrowser.browser.selectedWebViewForTesting
import dev.sk2andy.materialbrowser.data.BrowserSessionStore
import dev.sk2andy.materialbrowser.ui.theme.MaterialBrowserTheme
import java.util.concurrent.atomic.AtomicInteger
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BrowserFindInPageInstrumentedTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private var controller: BrowserController? = null

    @After
    fun tearDown() {
        composeRule.runOnIdle {
            controller?.destroy()
            controller = null
            clearSession()
        }
    }

    @Test
    fun nextMatchAdvancesThroughBrowserScreen() {
        lateinit var browserController: BrowserController
        composeRule.runOnIdle {
            clearSession()
            SystemWebViewLegacyTestAccess.selectEngine(composeRule.activity)
            browserController = BrowserController(composeRule.activity)
            controller = browserController
        }
        composeRule.setContent {
            MaterialBrowserTheme {
                BrowserScreen(browserController)
            }
        }
        composeRule.runOnIdle {
            browserController.selectedWebViewForTesting().loadDataWithBaseURL(
                "https://find.test/",
                """
                    <html>
                    <body style="margin:0">
                      <p>Candy alpha</p>
                      <div style="height:1800px"></div>
                      <p>Candy beta</p>
                      <div style="height:1800px"></div>
                      <p>Candy gamma</p>
                    </body>
                    </html>
                """.trimIndent(),
                "text/html",
                "utf-8",
                null,
            )
        }
        composeRule.waitUntil(timeoutMillis = 10_000L) {
            browserController.selectedTab.url == "https://find.test/" &&
                browserController.selectedTab.isLoading.not()
        }
        composeRule.waitUntil(timeoutMillis = 10_000L) {
            browserController.selectedWebViewForTesting().isAttachedToWindow
        }

        composeRule.runOnIdle {
            assertTrue(browserController.openFindInPage())
        }
        composeRule.onNodeWithTag(FindInPageBarTestTags.Bar).assertExists()
        composeRule.onNodeWithTag(FindInPageBarTestTags.Query).performTextInput("Candy")
        composeRule.waitUntil(timeoutMillis = 10_000L) {
            browserController.findInPageState?.let { state ->
                state.isDoneCounting && state.matchCount == 3
            } == true
        }
        composeRule.onNodeWithTag(FindInPageBarTestTags.Next).performClick()
        composeRule.waitUntil(timeoutMillis = 10_000L) {
            browserController.findInPageState?.activeMatchOrdinal == 1
        }
        composeRule.runOnIdle {
            assertEquals(1, browserController.findInPageState?.activeMatchOrdinal)
        }
    }

    /**
     * The find bar's query field owns the keyboard (BrowserImeOwnershipEffect): like the address
     * editor, the page keeps its layout under it and never sees the keyboard inset, while other
     * inset changes still reach it. Once the find bar closes, the page owns the keyboard again.
     */
    @Test
    fun findBarOwnsTheKeyboardAndThePageGetsItBackOnClose() {
        lateinit var browserController: BrowserController
        val forwardedImeBottom = AtomicInteger(-1)
        composeRule.runOnIdle {
            clearSession()
            SystemWebViewLegacyTestAccess.selectEngine(composeRule.activity)
            browserController = BrowserController(composeRule.activity)
            controller = browserController
        }
        composeRule.setContent {
            MaterialBrowserTheme {
                BrowserScreen(browserController)
            }
        }
        composeRule.runOnIdle {
            browserController.selectedWebViewForTesting().loadDataWithBaseURL(
                "https://find.test/",
                "<html><body>Candy</body></html>",
                "text/html",
                "utf-8",
                null,
            )
        }
        composeRule.waitUntil(timeoutMillis = 10_000L) {
            browserController.selectedTab.url == "https://find.test/" &&
                browserController.selectedTab.isLoading.not()
        }
        composeRule.waitUntil(timeoutMillis = 10_000L) {
            browserController.selectedWebViewForTesting().isAttachedToWindow
        }
        composeRule.runOnIdle {
            ViewCompat.setOnApplyWindowInsetsListener(
                browserController.selectedWebViewForTesting(),
            ) { _, insets ->
                forwardedImeBottom.set(
                    insets.getInsets(WindowInsetsCompat.Type.ime()).bottom,
                )
                insets
            }
            browserController.onWindowInsetsChanged(windowInsets(imeBottom = 0))
            assertTrue(browserController.openFindInPage())
        }
        composeRule.onNodeWithTag(FindInPageBarTestTags.Bar).assertExists()

        // Each step dispatches and reads in one main-thread block, so the emulator's own inset
        // changes (its keyboard opening for the find field) cannot land in between.
        composeRule.runOnIdle {
            forwardedImeBottom.set(-1)
            browserController.onWindowInsetsChanged(windowInsets(imeBottom = IME_BOTTOM))
            assertNotEquals(IME_BOTTOM, forwardedImeBottom.get())

            browserController.onWindowInsetsChanged(
                windowInsets(imeBottom = IME_BOTTOM, navigationBottom = NAVIGATION_BOTTOM),
            )
            assertEquals(0, forwardedImeBottom.get())
        }

        composeRule.runOnIdle { browserController.closeFindInPage() }
        composeRule.onNodeWithTag(FindInPageBarTestTags.Bar).assertDoesNotExist()
        composeRule.runOnIdle {
            forwardedImeBottom.set(-1)
            browserController.onWindowInsetsChanged(windowInsets(imeBottom = IME_BOTTOM))
            assertEquals(IME_BOTTOM, forwardedImeBottom.get())
        }
    }

    private fun windowInsets(imeBottom: Int, navigationBottom: Int = 0): WindowInsetsCompat =
        WindowInsetsCompat.Builder()
            .setInsets(WindowInsetsCompat.Type.ime(), Insets.of(0, 0, 0, imeBottom))
            .setVisible(WindowInsetsCompat.Type.ime(), imeBottom > 0)
            .setInsets(
                WindowInsetsCompat.Type.navigationBars(),
                Insets.of(0, 0, 0, navigationBottom),
            )
            .build()

    private fun clearSession() {
        InstrumentationRegistry.getInstrumentation().targetContext.getSharedPreferences(
            BrowserSessionStore.PREFERENCES_NAME,
            Context.MODE_PRIVATE,
        ).edit().clear().commit()
    }

    private companion object {
        const val IME_BOTTOM = 731
        const val NAVIGATION_BOTTOM = 63
    }
}
