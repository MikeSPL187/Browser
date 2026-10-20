package dev.sk2andy.materialbrowser.browser.gecko

import android.os.SystemClock
import android.view.View
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.sk2andy.materialbrowser.browser.EdgeToEdgeSiteFixtureServer
import dev.sk2andy.materialbrowser.browser.credentials.PasskeyAvailability
import dev.sk2andy.materialbrowser.browser.credentials.PasskeySupport
import java.util.UUID
import java.util.concurrent.atomic.AtomicReference
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Passkeys option A on a real Gecko: WebAuthn is exposed exactly where Credential Manager exists,
 * and the platform authenticator question accounts.google.com asks on load (#123, H1) reaches the
 * Google Play services stand-ins and answers `false` instead of crashing Gecko.
 */
@RunWith(AndroidJUnit4::class)
class GeckoWebAuthnInstrumentedTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    @Test
    fun platformAuthenticatorQueryAnswersWithoutCrashingGecko() {
        val expectApi = PasskeySupport.current(instrumentation.targetContext) == PasskeyAvailability.Available
        val title = AtomicReference<String?>(null)
        EdgeToEdgeSiteFixtureServer { _ -> HTML }.use { server ->
            ActivityScenario.launch(GeckoScrollTestActivity::class.java).use { scenario ->
                lateinit var session: GeckoBrowserSession
                lateinit var view: View
                scenario.onActivity { activity ->
                    session = GeckoRuntimeOwner.getOrCreate(activity).createSession(
                        profileId = "webauthn-${UUID.randomUUID()}",
                        isPrivate = false,
                    )
                    session.bindExtensionTab("webauthn-${UUID.randomUUID()}", 1)
                    session.setStateListener { state -> title.set(state.title) }
                    view = session.createView(activity)
                    activity.setContentView(view)
                    session.setActive(true)
                    assertTrue(session.loadUrl(server.fixtureUrl("/webauthn")))
                }
                try {
                    val report = awaitReport(title)
                    assertEquals(expectApi, report.getBoolean("api"))
                    if (expectApi) {
                        assertEquals(false, report.get("uvpaa"))
                    }
                } finally {
                    scenario.onActivity {
                        session.releaseView(view)
                        session.setActive(false)
                        session.close()
                    }
                }
            }
        }
    }

    private fun awaitReport(title: AtomicReference<String?>): JSONObject {
        val deadline = SystemClock.elapsedRealtime() + 30_000
        while (SystemClock.elapsedRealtime() < deadline) {
            instrumentation.waitForIdleSync()
            val value = title.get().orEmpty()
            if (value.startsWith(REPORT_PREFIX)) {
                runCatching { JSONObject(value.removePrefix(REPORT_PREFIX)) }.getOrNull()?.let { return it }
            }
            SystemClock.sleep(50)
        }
        throw AssertionError("WebAuthn report did not arrive: ${title.get()}")
    }

    private companion object {
        const val REPORT_PREFIX = "Vola webauthn: "
        val HTML = """
            <!doctype html><meta name="viewport" content="width=device-width,initial-scale=1">
            <title>loading</title>
            <script>
              (async () => {
                const report = { api: typeof PublicKeyCredential !== "undefined" };
                if (report.api) {
                  try {
                    report.uvpaa = await PublicKeyCredential.isUserVerifyingPlatformAuthenticatorAvailable();
                  } catch (e) {
                    report.uvpaa = "error:" + e.name;
                  }
                }
                document.title = "$REPORT_PREFIX" + JSON.stringify(report);
              })();
            </script>
        """.trimIndent()
    }
}
