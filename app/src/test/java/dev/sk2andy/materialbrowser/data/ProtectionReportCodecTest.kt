package dev.sk2andy.materialbrowser.data

import org.junit.Assert.assertEquals
import org.junit.Test

class ProtectionReportCodecTest {
    @Test
    fun `round trip keeps days and counts`() {
        val days = listOf(
            ProtectionDay(19_999, mapOf("a.com" to 3)),
            ProtectionDay(20_000, mapOf("a.com" to 1, "b.com" to 7)),
        )

        assertEquals(days, ProtectionReportCodec.decode(ProtectionReportCodec.encode(days)))
    }

    @Test
    fun `malformed input is skipped`() {
        assertEquals(emptyList<ProtectionDay>(), ProtectionReportCodec.decode(""))
        assertEquals(
            listOf(ProtectionDay(20_000, mapOf("a.com" to 2))),
            ProtectionReportCodec.decode("""{"x": {"a.com": 1}, "20000": {"a.com": 2, "b.com": -1}, "20001": 5}"""),
        )
    }
}
