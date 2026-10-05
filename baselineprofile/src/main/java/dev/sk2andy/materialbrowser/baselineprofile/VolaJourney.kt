package dev.sk2andy.materialbrowser.baselineprofile

import android.content.Intent
import android.net.Uri
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until

/** The benchmark build of the GeckoView app (app build type "benchmark", flavor "full"). */
internal const val TARGET_PACKAGE = "io.github.mikespl187.vola.benchmark"

private const val UI_TIMEOUT_MILLIS = 5_000L
private const val PAGE_SETTLE_MILLIS = 6_000L

/** A fresh install first shows the welcome and setup screens; step past them like a person. */
internal fun MacrobenchmarkScope.passFirstRun() {
    repeat(4) {
        val button = listOf("Get started", "Next", "Skip", "Explore Vola", "Начать", "Далее", "Пропустить", "К браузеру")
            .firstNotNullOfOrNull { label -> device.wait(Until.findObject(By.text(label)), UI_TIMEOUT_MILLIS / 4) }
            ?: return
        button.click()
        device.waitForIdle()
    }
}

/** Opens [url] in Vola the way a link from another app does, and waits for the page. */
internal fun MacrobenchmarkScope.openPage(url: String) {
    startActivityAndWait(
        Intent(Intent.ACTION_VIEW, Uri.parse(url)).setPackage(TARGET_PACKAGE),
    )
    Thread.sleep(PAGE_SETTLE_MILLIS)
}

/** Scrolls the page down and back, the first gestures people make. */
internal fun MacrobenchmarkScope.scrollPage() {
    val width = device.displayWidth
    val height = device.displayHeight
    repeat(2) {
        device.swipe(width / 2, height * 3 / 4, width / 2, height / 3, 20)
        device.waitForIdle()
    }
    device.swipe(width / 2, height / 3, width / 2, height * 3 / 4, 20)
    device.waitForIdle()
}
