package dev.sk2andy.materialbrowser.shared.credentials

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class TotpRulesTest {
    @Test
    fun aLinkOrABareKeyBecomesOneCanonicalKey() {
        val link = TotpRules.parse(
            "otpauth://totp/Example:anna%40example.com?secret=JBSWY3DPEHPK3PXPJBSWY3DPEHPK3PXP&issuer=Example&digits=8&period=60&algorithm=sha256",
        )
        assertEquals(TotpConfig("JBSWY3DPEHPK3PXPJBSWY3DPEHPK3PXP", TotpAlgorithm.Sha256, 8, 60), link)
        assertEquals(
            "otpauth://totp/?secret=JBSWY3DPEHPK3PXPJBSWY3DPEHPK3PXP&algorithm=SHA256&digits=8&period=60",
            TotpRules.canonical(link!!),
        )
        assertEquals(link, TotpRules.parse(TotpRules.canonical(link)))
        assertEquals(TotpConfig("JBSWY3DPEHPK3PXP"), TotpRules.parse(" jbsw y3dp-ehpk 3pxp "))
    }

    @Test
    fun anythingElseIsRefused() {
        assertNull(TotpRules.parse(""))
        assertNull(TotpRules.parse("not a key!"))
        assertNull(TotpRules.parse("JBSWY3DP"))
        assertNull(TotpRules.parse("otpauth://hotp/x?secret=JBSWY3DPEHPK3PXP"))
        assertNull(TotpRules.parse("otpauth://totp/x?issuer=Example"))
        assertNull(TotpRules.parse("otpauth://totp/x?secret=JBSWY3DPEHPK3PXP&digits=4"))
        assertNull(TotpRules.parse("otpauth://totp/x?secret=JBSWY3DPEHPK3PXP&algorithm=MD5"))
        assertFalse("JBSW" in TotpConfig("JBSWY3DPEHPK3PXP").toString())
    }

    @Test
    fun base32AndTruncationFollowTheRfcs() {
        assertContentEquals("12345678901234567890".encodeToByteArray(), TotpRules.base32Decode("GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ"))
        // RFC 4226, section 5.4: this HMAC truncates to 872921.
        val digest = "1f8698690e02ca16618550ef7f19da8e945b555a".chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        val code = TotpRules.code(TotpConfig("GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ"), 59_000) { _, _, _ -> digest }
        assertEquals("872921", code)
        assertEquals("872 921", TotpRules.grouped(code))
        assertEquals(1, TotpRules.secondsLeft(59_000, 30))
        assertEquals(30, TotpRules.secondsLeft(60_000, 30))
    }
}
