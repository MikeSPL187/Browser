package dev.sk2andy.materialbrowser.browser

import java.util.concurrent.Executors
import kotlinx.coroutines.awaitCancellation
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ControllerExecutorsTest {
    @Test
    fun `a task for a shut-down executor is dropped, not thrown`() {
        val executor = Executors.newSingleThreadExecutor()
        executor.shutdown()

        executor.executeIfRunning { error("must not run") }
    }

    @Test
    fun `a push clean-up that hangs counts as failed once its time is up`() {
        val started = System.nanoTime()

        assertFalse(awaitPushCleanup(timeoutMillis = 50) { awaitCancellation() })
        assertTrue(System.nanoTime() - started < 5_000_000_000L)
    }

    @Test
    fun `a push clean-up that answers or throws is taken as it is`() {
        assertTrue(awaitPushCleanup { true })
        assertFalse(awaitPushCleanup { false })
        assertFalse(awaitPushCleanup { error("push server gone") })
    }
}
