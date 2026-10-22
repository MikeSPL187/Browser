package dev.sk2andy.materialbrowser.browser.gecko

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.sk2andy.materialbrowser.browser.engine.GeckoProfileStorageRules
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoSessionSettings

@RunWith(AndroidJUnit4::class)
class GeckoSessionPriorityInstrumentedTest {
    @Test
    fun selectedTabAsksGeckoForHighPriorityAndBackgroundForDefault() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        lateinit var geckoSession: PriorityRecordingSession
        lateinit var session: GeckoBrowserSession

        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            geckoSession = PriorityRecordingSession()
            session = requireNotNull(
                GeckoRuntimeOwner.getOrCreate(context).adoptExtensionSession(
                    session = geckoSession,
                    profileId = PROFILE_ID,
                    isPrivate = false,
                ),
            ) { "Gecko did not adopt the unopened session" }
            session.setActive(true)
            session.setActive(true)
            session.setActive(false)
        }

        try {
            assertEquals(
                listOf(GeckoSession.PRIORITY_HIGH, GeckoSession.PRIORITY_DEFAULT),
                geckoSession.priorityHints,
            )
        } finally {
            InstrumentationRegistry.getInstrumentation().runOnMainSync { session.close() }
        }
    }

    private class PriorityRecordingSession : GeckoSession(
        GeckoSessionSettings.Builder()
            .contextId(GeckoProfileStorageRules.contextId(PROFILE_ID, isolationEnabled = false, isPrivate = false))
            .usePrivateMode(false)
            .build(),
    ) {
        val priorityHints = mutableListOf<Int>()

        override fun setPriorityHint(priorityHint: Int) {
            priorityHints += priorityHint
            super.setPriorityHint(priorityHint)
        }
    }

    private companion object {
        const val PROFILE_ID = "priority-hint"
    }
}
