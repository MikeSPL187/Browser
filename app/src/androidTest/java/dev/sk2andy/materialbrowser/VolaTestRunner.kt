package dev.sk2andy.materialbrowser

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import dev.sk2andy.materialbrowser.data.GestureOnboardingStore
import dev.sk2andy.materialbrowser.diagnostics.CrashJournal

/**
 * The orchestrator clears app data before every test, so each one would start as a new install and
 * open the first run over the browser, and the crash journal would report the crashes of earlier
 * tests. Tests that cover the first run or the journal reset these stores themselves.
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
        super.callApplicationOnCreate(app)
    }
}
