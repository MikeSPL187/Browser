package dev.sk2andy.materialbrowser.browser.transfer

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.sk2andy.materialbrowser.shared.transfer.LocalTransferBookmark
import dev.sk2andy.materialbrowser.shared.transfer.LocalTransferFailure
import dev.sk2andy.materialbrowser.shared.transfer.LocalTransferHandle
import dev.sk2andy.materialbrowser.shared.transfer.LocalTransferOfferState
import dev.sk2andy.materialbrowser.shared.transfer.LocalTransferPairingRules
import dev.sk2andy.materialbrowser.shared.transfer.LocalTransferPayload
import dev.sk2andy.materialbrowser.shared.transfer.LocalTransferPort
import dev.sk2andy.materialbrowser.shared.transfer.LocalTransferReceiveResult
import dev.sk2andy.materialbrowser.shared.transfer.LocalTransferTab
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The transfer on a real Android network stack (V26): the port finds the Wi-Fi address,
 * binds the one-shot server there and fetches through a socket bound to the Wi-Fi network,
 * both ends in this process. Skipped when the device has no Wi-Fi (the port then reports
 * Unavailable, which the JVM tests cover).
 */
@RunWith(AndroidJUnit4::class)
class AndroidLocalTransferPortInstrumentedTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val payload = LocalTransferPayload(
        deviceName = "Emulator",
        tabs = listOf(LocalTransferTab("https://example.com/a", "Page A")),
        bookmarks = listOf(LocalTransferBookmark("https://example.org/", "Example", "Work")),
    )
    private val handles = mutableListOf<LocalTransferHandle>()

    @After
    fun tearDown() {
        handles.forEach(LocalTransferHandle::cancel)
    }

    @Test
    fun aPayloadMovesOverTheDevicesWifi() {
        val port = AndroidLocalTransferPort.create(context)
        val code = offerCode(port)

        assertTrue(code, code.startsWith(LocalTransferPairingRules.SCHEME))
        assertEquals(LocalTransferReceiveResult.Received(payload), receive(port, code))
    }

    @Test
    fun aCancelledOfferCannotBeFetched() {
        val port = AndroidLocalTransferPort.create(context)
        val code = offerCode(port)
        handles.removeAt(handles.lastIndex).cancel()

        assertEquals(LocalTransferReceiveResult.Failed(LocalTransferFailure.Unreachable), receive(port, code))
    }

    private fun offerCode(port: LocalTransferPort): String {
        val first = CountDownLatch(1)
        val state = AtomicReference<LocalTransferOfferState>()
        handles += port.offer(payload) { offered ->
            if (state.compareAndSet(null, offered)) first.countDown()
        }
        assertTrue("the offer did not answer", first.await(TIMEOUT_SECONDS, TimeUnit.SECONDS))
        val offered = state.get()
        assumeTrue(
            "no Wi-Fi on this device: $offered",
            offered != LocalTransferOfferState.Failed(LocalTransferFailure.Unavailable),
        )
        return (offered as LocalTransferOfferState.Waiting).code
    }

    private fun receive(port: LocalTransferPort, code: String): LocalTransferReceiveResult {
        val done = CountDownLatch(1)
        val result = AtomicReference<LocalTransferReceiveResult>()
        handles += port.receive(code) { received ->
            result.set(received)
            done.countDown()
        }
        assertTrue("the receive did not finish", done.await(TIMEOUT_SECONDS, TimeUnit.SECONDS))
        return result.get()
    }

    private companion object {
        const val TIMEOUT_SECONDS = 20L
    }
}
