package dev.sk2andy.materialbrowser.diagnostics

import java.io.ByteArrayOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TombstoneSummaryTest {
    @Test
    fun readsSignalAbortMessageAndCrashingThread() {
        val signal = message {
            varint(1, 11)
            string(2, "SIGSEGV")
            string(4, "SEGV_MAPERR")
            varint(8, 1)
            varint(9, 0x10)
        }
        val frame = message {
            varint(1, 0xabcd)
            varint(2, 0x7f00abcd)
            string(4, "mozilla::Crash")
            varint(5, 12)
            string(6, "/data/app/~~random==/io.github.mikespl187.vola/lib/arm64/libxul.so")
            string(8, "deadbeef")
        }
        val crashing = message {
            varint(1, 42)
            string(2, "Gecko")
            bytes(3, message { varint(1, 0) }) // registers: skipped
            bytes(4, frame)
            bytes(4, message { varint(1, 0x10); string(6, "libc.so") })
        }
        val other = message {
            varint(1, 7)
            string(2, "Other")
        }
        val tombstone = message {
            varint(5, 40)
            varint(6, 42)
            bytes(10, signal)
            fixed64(4)
            string(14, "MOZ_CRASH(boom)")
            bytes(16, message { varint(1, 7); bytes(2, other) })
            bytes(16, message { varint(1, 42); bytes(2, crashing) })
            bytes(18, message { string(1, "log lines stay out") })
        }

        val summary = TombstoneSummary.parse(tombstone)

        assertEquals(
            TombstoneSummary(
                signalName = "SIGSEGV",
                signalCode = "SEGV_MAPERR",
                faultAddress = 0x10,
                abortMessage = "MOZ_CRASH(boom)",
                threadName = "Gecko",
                frames = listOf(
                    "#00 pc 0000abcd  libxul.so (mozilla::Crash+12) (BuildId: deadbeef)",
                    "#01 pc 00000010  libc.so",
                ),
            ),
            summary,
        )
    }

    @Test
    fun rejectsBytesThatAreNotATombstone() {
        assertNull(TombstoneSummary.parse("not a protobuf at all".toByteArray()))
        assertNull(TombstoneSummary.parse(byteArrayOf()))
        assertNull(TombstoneSummary.parse(byteArrayOf(0x52, 0x7f, 0x01)))
    }

    private fun message(build: ProtoWriter.() -> Unit): ByteArray = ProtoWriter().apply(build).toByteArray()

    private class ProtoWriter {
        private val output = ByteArrayOutputStream()

        fun varint(field: Int, value: Long) {
            writeVarint((field shl 3).toLong())
            writeVarint(value)
        }

        fun string(field: Int, value: String) = bytes(field, value.toByteArray())

        fun bytes(field: Int, value: ByteArray) {
            writeVarint(((field shl 3) or 2).toLong())
            writeVarint(value.size.toLong())
            output.write(value)
        }

        fun fixed64(field: Int) {
            writeVarint(((field shl 3) or 1).toLong())
            output.write(ByteArray(8))
        }

        fun toByteArray(): ByteArray = output.toByteArray()

        private fun writeVarint(value: Long) {
            var rest = value
            while (rest and 0x7fL.inv() != 0L) {
                output.write(((rest and 0x7f) or 0x80).toInt())
                rest = rest ushr 7
            }
            output.write(rest.toInt())
        }
    }
}
