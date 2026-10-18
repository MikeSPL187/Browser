package dev.sk2andy.materialbrowser.baselineprofile

import androidx.benchmark.macro.BaselineProfileMode
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.ExperimentalMetricApi
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.TraceSectionMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Cold start with and without the baseline profile, so the workflow shows what the profile buys.
 * On an emulator the numbers are a trend, not a budget. Next to the start itself it measures the
 * app's own startup steps (StartupTimeline's «Vola.Startup.*» trace sections), so the performance
 * job shows where the time goes before the first frame; perf_gate.py reports them, never gates them.
 */
@RunWith(AndroidJUnit4::class)
class StartupBenchmark {
    @get:Rule
    val rule = MacrobenchmarkRule()

    @Test
    fun coldStartWithoutProfile() = coldStart(CompilationMode.None())

    @Test
    fun coldStartWithProfileIfBundled() = coldStart(CompilationMode.Partial(BaselineProfileMode.UseIfAvailable))

    private fun coldStart(compilation: CompilationMode) = rule.measureRepeated(
        packageName = TARGET_PACKAGE,
        metrics = listOf(StartupTimingMetric()) + startupSections(),
        compilationMode = compilation,
        startupMode = StartupMode.COLD,
        iterations = 5,
        setupBlock = {
            pressHome()
            startActivityAndWait()
            passFirstRun()
            pressHome()
        },
    ) {
        startActivityAndWait()
    }
}

/**
 * The startup steps StartupTimeline wraps in trace sections. Sum, not First: a step that did not run
 * in an iteration reads as 0 instead of failing the benchmark.
 */
@OptIn(ExperimentalMetricApi::class)
private fun startupSections() = listOf(
    "ControllerCreate",
    "GeckoRuntimeCreate",
    "LoadTabs",
    "LoadHistory",
    "LoadProfiles",
    "LoadFavorites",
    "LoadFilterRules",
    "LoadUserScripts",
    "BlockingListsWait",
).map { step -> TraceSectionMetric("Vola.Startup.$step", TraceSectionMetric.Mode.Sum) }
