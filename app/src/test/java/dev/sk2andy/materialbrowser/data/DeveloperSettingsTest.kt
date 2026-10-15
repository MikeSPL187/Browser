package dev.sk2andy.materialbrowser.data

import org.junit.Assert.assertEquals
import org.junit.Test

class DeveloperSettingsTest {
    @Test
    fun `defaults preserve the stabilized safe area fallback`() {
        val settings = DeveloperSettings()

        assertEquals(
            BrowserChromeScrollDispatchMode.Optimized,
            settings.browserChromeScrollDispatchMode,
        )
        assertEquals(400, settings.safeAreaLayoutQuietPeriodMillis)
        assertEquals(3, settings.safeAreaRequiredFailureCount)
        assertEquals(false, settings.forceSafeAreaFallback)
    }

    @Test
    fun `vola's own gecko blocker is on by default`() {
        assertEquals(true, DeveloperSettings().volaGeckoBlocker)
        assertEquals(true, DeveloperSettings().normalized().volaGeckoBlocker)
    }

    @Test
    fun `scroll dispatch mode uses stable ids and rejects unknown values`() {
        assertEquals(
            BrowserChromeScrollDispatchMode.Fixed120Hz,
            BrowserChromeScrollDispatchMode.fromStableId("fixed_120_hz"),
        )
        assertEquals(
            BrowserChromeScrollDispatchMode.Optimized,
            BrowserChromeScrollDispatchMode.fromStableId("unknown"),
        )
        assertEquals(
            BrowserChromeScrollDispatchMode.Optimized,
            BrowserChromeScrollDispatchMode.fromStableId(null),
        )
    }

    @Test
    fun `unsafe values are bounded independently`() {
        assertEquals(
            DeveloperSettings(
                safeAreaLayoutQuietPeriodMillis = 100,
                safeAreaRequiredFailureCount = 5,
                forceSafeAreaFallback = true,
            ),
            DeveloperSettings(
                safeAreaLayoutQuietPeriodMillis = 0,
                safeAreaRequiredFailureCount = 99,
                forceSafeAreaFallback = true,
            ).normalized(),
        )
    }

    @Test
    fun `layout quiet period snaps to its documented step`() {
        assertEquals(
            100,
            DeveloperSettings(safeAreaLayoutQuietPeriodMillis = 123)
                .normalized()
                .safeAreaLayoutQuietPeriodMillis,
        )
        assertEquals(
            150,
            DeveloperSettings(safeAreaLayoutQuietPeriodMillis = 126)
                .normalized()
                .safeAreaLayoutQuietPeriodMillis,
        )
    }
}
