package dev.sk2andy.materialbrowser

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import dev.sk2andy.materialbrowser.data.GestureOnboardingStore
import dev.sk2andy.materialbrowser.diagnostics.CrashJournal
import dev.sk2andy.materialbrowser.ui.theme.v5.UiV5Flag

/**
 * The orchestrator clears app data before every test, so each one would start as a new install and
 * open the first run over the browser, and the crash journal would report the crashes of earlier
 * tests. Tests that cover the first run or the journal reset these stores themselves.
 *
 * The tests below were written for the v4 interface, so they pin ui.v5 off: once v5 is the default
 * (V28, step 2c) they would otherwise meet v5 on the phone emulator. The screen walk turns the flag
 * on for its v5 groups itself, and a test of v5 turns it on explicitly; the v4 tests leave with the
 * v4 code (step 4).
 */
class VolaTestRunner : AndroidJUnitRunner() {
    override fun callApplicationOnCreate(app: Application) {
        app.getSharedPreferences(GestureOnboardingStore.PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(GestureOnboardingStore.KEY_COMPLETED_VERSION, GestureOnboardingStore.CURRENT_VERSION)
            .commit()
        app.getSharedPreferences(CrashJournal.PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit()
            .putLong(CrashJournal.KEY_LAST_EXIT_SCAN, System.currentTimeMillis())
            .commit()
        UiV5Flag.setEnabled(app, false)
        super.callApplicationOnCreate(app)
    }
}
