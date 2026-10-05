package dev.sk2andy.materialbrowser.browser.credentials

import dev.sk2andy.materialbrowser.shared.credentials.TotpAlgorithm
import dev.sk2andy.materialbrowser.shared.credentials.TotpConfig
import org.junit.Assert.assertEquals
import org.junit.Test

/** The test vectors of RFC 6238, appendix B. */
class TotpCodesTest {
    private val sha1 = TotpConfig("GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ", TotpAlgorithm.Sha1, digits = 8)
    private val sha256 = TotpConfig(
        "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQGEZA",
        TotpAlgorithm.Sha256,
        digits = 8,
    )

    @Test
    fun `codes match the rfc vectors`() {
        assertEquals("94287082", TotpCodes.code(sha1, 59_000))
        assertEquals("07081804", TotpCodes.code(sha1, 1_111_111_109_000))
        assertEquals("14050471", TotpCodes.code(sha1, 1_111_111_111_000))
        assertEquals("46119246", TotpCodes.code(sha256, 59_000))
        assertEquals("68084774", TotpCodes.code(sha256, 1_111_111_109_000))
    }
}
