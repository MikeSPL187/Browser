package dev.sk2andy.materialbrowser.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Records the code a cold start and the first page use, for app/src/main/baseline-prof.txt. The
 * "Baseline profile" workflow runs it on an emulator and publishes the file for review.
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {
    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun startAndOpenAPage() = rule.collect(
        packageName = TARGET_PACKAGE,
        includeInStartupProfile = false,
    ) {
        pressHome()
        startActivityAndWait()
        passFirstRun()
        openPage("https://example.com/")
        scrollPage()
    }
}
