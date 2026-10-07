package dev.sk2andy.materialbrowser.browser.gecko

import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class GeckoDownloadStreamRulesTest {
    @Test
    fun `an opened output keeps its row`() {
        var rollbacks = 0
        val output = Any()

        val opened = GeckoDownloadStreamRules.openOrRollback(open = { output }, rollback = { rollbacks++ })

        assertSame(output, opened)
        assertEquals(0, rollbacks)
    }

    @Test
    fun `a provider that throws while opening leaves no pending row and the error goes on`() {
        var rollbacks = 0
        val failure = IOException("storage gone")

        val thrown = runCatching {
            GeckoDownloadStreamRules.openOrRollback<Any>(open = { throw failure }, rollback = { rollbacks++ })
        }.exceptionOrNull()

        assertSame(failure, thrown)
        assertEquals(1, rollbacks)
    }

    @Test
    fun `a provider that cannot open the row rolls it back`() {
        var rollbacks = 0

        val thrown = runCatching {
            GeckoDownloadStreamRules.openOrRollback<Any>(open = { null }, rollback = { rollbacks++ })
        }.exceptionOrNull()

        assertTrue(thrown is IllegalStateException)
        assertEquals(1, rollbacks)
    }

    @Test
    fun `a rollback that fails does not hide the open failure`() {
        val failure = SecurityException("denied")

        val thrown = runCatching {
            GeckoDownloadStreamRules.openOrRollback<Any>(
                open = { throw failure },
                rollback = { throw IllegalStateException("delete failed") },
            )
        }.exceptionOrNull()

        assertSame(failure, thrown)
    }
}
