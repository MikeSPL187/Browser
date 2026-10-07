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

    @Test
    fun `the exact day total survives a round trip`() {
        val days = listOf(ProtectionDay(20_000, mapOf("a.com" to 3), total = 9))

        assertEquals(days, ProtectionReportCodec.decode(ProtectionReportCodec.encode(days)))
    }

    @Test
    fun `a day saved before totals existed counts the sum of its sites`() {
        assertEquals(
            listOf(ProtectionDay(20_000, mapOf("a.com" to 2, "b.com" to 3), total = 5)),
            ProtectionReportCodec.decode("""{"20000": {"a.com": 2, "b.com": 3}}"""),
        )
        assertEquals(
            listOf(ProtectionDay(20_000, mapOf("a.com" to 2), total = 2)),
            ProtectionReportCodec.decode("""{"20000": {"a.com": 2}, "totals": {"20000": 1, "19999": 4}}"""),
        )
    }
}
