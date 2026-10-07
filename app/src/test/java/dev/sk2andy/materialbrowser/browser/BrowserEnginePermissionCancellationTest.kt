package dev.sk2andy.materialbrowser.browser

import org.junit.Assert.assertEquals
import org.junit.Test

class BrowserEnginePermissionCancellationTest {
    @Test
    fun cancelReachesTheListenerOnce() {
        val cancellation = BrowserEnginePermissionCancellation()
        var calls = 0
        cancellation.onCanceled { calls++ }

        cancellation.cancel()
        cancellation.cancel()

        assertEquals(1, calls)
    }

    @Test
    fun listenerRegisteredAfterCancelStillHearsIt() {
        val cancellation = BrowserEnginePermissionCancellation()
        var calls = 0

        cancellation.cancel()
        cancellation.onCanceled { calls++ }

        assertEquals(1, calls)
    }

    @Test
    fun requestThatIsNeverCanceledNeverNotifies() {
        val cancellation = BrowserEnginePermissionCancellation()
        var calls = 0

        cancellation.onCanceled { calls++ }

        assertEquals(0, calls)
    }
}
