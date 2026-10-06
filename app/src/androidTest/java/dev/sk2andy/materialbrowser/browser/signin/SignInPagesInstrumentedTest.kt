package dev.sk2andy.materialbrowser.browser.signin

import android.content.Context
import android.net.Uri
import android.os.SystemClock
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.sk2andy.materialbrowser.BuildConfig
import dev.sk2andy.materialbrowser.MainActivity
import dev.sk2andy.materialbrowser.browser.StartupAddressFocusMode
import dev.sk2andy.materialbrowser.data.BrowserSessionStore
import dev.sk2andy.materialbrowser.data.GestureOnboardingStore
import dev.sk2andy.materialbrowser.data.ReleaseNotesStore
import dev.sk2andy.materialbrowser.grantNotificationPermissionForTests
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Real sign-in pages load in Vola and the app stays up (#123, H1: the app closed as soon as
 * accounts.google.com had opened). Needs the network; a page that does not load fails the test
 * with its URL rather than passing quietly.
 */
@RunWith(AndroidJUnit4::class)
class SignInPagesInstrumentedTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun setUp() {
        context.getSharedPreferences(BrowserSessionStore.PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        grantNotificationPermissionForTests()
        GestureOnboardingStore(context).markCompleted()
        ReleaseNotesStore(context).markHandled(BuildConfig.VERSION_CODE.toLong())
        BrowserSessionStore(context).apply {
            saveStartupAnimationEnabled(false)
            saveStartupAddressFocusMode(StartupAddressFocusMode.Never)
        }
    }

    @Test
    fun googleSignInPageStaysOpen() = openAndStay("https://accounts.google.com/")

    @Test
    fun googleServiceLoginStaysOpen() = openAndStay(
        "https://accounts.google.com/ServiceLogin?continue=https%3A%2F%2Fmyaccount.google.com%2F",
    )

    @Test
    fun vkIdSignInPageStaysOpen() = openAndStay("https://id.vk.ru/")

    @Test
    fun mailSignInPageStaysOpen() = openAndStay("https://account.mail.ru/login")

    private fun openAndStay(url: String) {
        val host = requireNotNull(Uri.parse(url).host)
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertTrue(activity.browserControllerForTesting().openUrl(url))
            }
            var loadedUrl: String? = null
            val deadline = SystemClock.uptimeMillis() + LOAD_TIMEOUT_MILLIS
            while (SystemClock.uptimeMillis() < deadline) {
                var loading = true
                scenario.onActivity { activity ->
                    val tab = activity.browserControllerForTesting().selectedTab
                    loading = tab.isLoading
                    loadedUrl = tab.url
                }
                if (!loading && loadedUrl?.let { Uri.parse(it).host }?.endsWith(hostRoot(host)) == true) {
                    break
                }
                SystemClock.sleep(250L)
            }
            assertTrue(
                "$url did not finish loading; last address $loadedUrl",
                loadedUrl?.let { Uri.parse(it).host }?.endsWith(hostRoot(host)) == true,
            )
            // Sign-in pages start scripts, credential requests and form detection after load.
            SystemClock.sleep(SETTLE_MILLIS)
            assertEquals(Lifecycle.State.RESUMED, scenario.state)
            scenario.onActivity { activity ->
                assertFalse("Vola closed on $url", activity.isFinishing)
            }
        }
    }

    /** google.com for accounts.google.com: sign-in pages redirect within their own site. */
    private fun hostRoot(host: String): String = host.split('.').takeLast(2).joinToString(".")

    private companion object {
        const val LOAD_TIMEOUT_MILLIS = 45_000L
        const val SETTLE_MILLIS = 15_000L
    }
}
