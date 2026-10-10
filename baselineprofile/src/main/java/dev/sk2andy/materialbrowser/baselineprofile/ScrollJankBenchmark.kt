package dev.sk2andy.materialbrowser.baselineprofile

import androidx.benchmark.macro.BaselineProfileMode
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Frames of the browser's own window while a long page scrolls under the finger: the panels that
 * hide and return with the scroll. The "Performance budget" workflow serves the page on the host
 * (scripts/ci/pages/scroll.html, forwarded to the emulator) and compares a pull request with its
 * base (docs/vola/plan-v5.md, section 7). On an emulator without a GPU every frame misses its
 * deadline, so the budget compares the CPU time each frame takes, not the share of late frames.
 */
@RunWith(AndroidJUnit4::class)
class ScrollJankBenchmark {
    @get:Rule
    val rule = MacrobenchmarkRule()

    @Test
    fun scrollLongPage() = rule.measureRepeated(
        packageName = TARGET_PACKAGE,
        metrics = listOf(FrameTimingMetric()),
        compilationMode = CompilationMode.Partial(BaselineProfileMode.UseIfAvailable),
        iterations = 5,
        setupBlock = {
            pressHome()
            startActivityAndWait()
            passFirstRun()
            openPage(SCROLL_PAGE_URL)
        },
    ) {
        repeat(3) { scrollPage() }
    }

    private companion object {
        const val SCROLL_PAGE_URL = "http://localhost:8765/scroll.html"
    }
}
