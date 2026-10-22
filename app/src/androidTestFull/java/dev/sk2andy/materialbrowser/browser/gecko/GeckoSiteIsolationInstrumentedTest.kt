package dev.sk2andy.materialbrowser.browser.gecko

import android.content.Context
import androidx.annotation.OptIn
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.geckoview.ExperimentalGeckoViewApi
import org.mozilla.geckoview.GeckoPreferenceController
import org.mozilla.geckoview.GeckoRuntimeSettings

/** Reads the live Gecko preferences, not the settings object, so a Gecko default cannot hide behind it. */
@RunWith(AndroidJUnit4::class)
class GeckoSiteIsolationInstrumentedTest {
    @Test
    fun sitesAreIsolatedByVolasStrategy() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        InstrumentationRegistry.getInstrumentation().runOnMainSync { GeckoRuntimeOwner.getOrCreate(context) }

        val fission = livePref(FISSION_PREF)
        val strategy = livePref(STRATEGY_PREF)

        assertEquals("$FISSION_PREF (strategy $strategy)", true, fission)
        assertEquals(
            "$STRATEGY_PREF (fission $fission)",
            GeckoRuntimeSettings.STRATEGY_ISOLATE_HIGH_VALUE,
            strategy,
        )
    }

    @OptIn(markerClass = [ExperimentalGeckoViewApi::class])
    private fun livePref(name: String): Any? {
        val value = AtomicReference<Any?>()
        val failure = AtomicReference<Throwable?>()
        val done = CountDownLatch(1)
        // GeckoPreferenceController answers on the thread that asks, which needs a Looper.
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            GeckoPreferenceController.getGeckoPref(name).accept(
                { preference ->
                    value.set(preference?.value)
                    done.countDown()
                },
                { error ->
                    failure.set(error)
                    done.countDown()
                },
            )
        }
        assertTrue("$name was not read in time", done.await(20, TimeUnit.SECONDS))
        failure.get()?.let { throw AssertionError("$name could not be read", it) }
        return value.get()
    }

    private companion object {
        const val FISSION_PREF = "fission.autostart"
        const val STRATEGY_PREF = "fission.webContentIsolationStrategy"
    }
}
