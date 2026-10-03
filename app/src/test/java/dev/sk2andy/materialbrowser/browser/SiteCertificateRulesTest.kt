package dev.sk2andy.materialbrowser.browser

import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SiteCertificateRulesTest {
    private val certificate: X509Certificate = CertificateFactory.getInstance("X.509")
        .generateCertificate(PEM.trimIndent().byteInputStream()) as X509Certificate

    @Test
    fun `a certificate becomes names, dates and its SHA-256 fingerprint`() {
        val site = SiteCertificateRules.fromX509("Example.COM", certificate)!!
        assertEquals("example.com", site.host)
        assertEquals("example.com", site.subjectName)
        assertEquals("Vola Test, Inc.", site.issuerName)
        assertEquals(certificate.notAfter.time, site.validUntilMillis)
        assertEquals(
            "C4:3D:AF:B1:2A:C3:A5:87:B0:FD:81:F3:51:AB:2B:E2:5A:FD:3B:69:39:48:CE:0F:9F:E3:D1:E4:5F:DD:D2:F8",
            site.sha256Fingerprint,
        )
        assertNull(SiteCertificateRules.fromX509(null, certificate))
        assertNull(SiteCertificateRules.fromX509("example.com", null))
    }

    @Test
    fun `RFC 2253 attributes are read with escapes and multi-valued parts`() {
        val name = "CN=example.com+serialNumber=7,O=Example\\, Inc.,C=RU"
        assertEquals("example.com", SiteCertificateRules.x500Attribute(name, "CN"))
        assertEquals("Example, Inc.", SiteCertificateRules.x500Attribute(name, "o"))
        assertEquals("RU", SiteCertificateRules.x500Attribute(name, "C"))
        assertNull(SiteCertificateRules.x500Attribute(name, "OU"))
    }

    @Test
    fun `only the https page of the same host shows the certificate`() {
        val site = SiteCertificate("пример.рф", null, null, null, null, null)
        assertEquals(site, SiteCertificateRules.forPage(site, "https://xn--e1afmkfd.xn--p1ai/path"))
        assertNull(SiteCertificateRules.forPage(site, "http://xn--e1afmkfd.xn--p1ai/"))
        assertNull(SiteCertificateRules.forPage(site, "https://other.org/"))
        assertNull(SiteCertificateRules.forPage(null, "https://xn--e1afmkfd.xn--p1ai/"))
    }

    @Test
    fun `the fingerprint breaks between bytes, eight to a line`() {
        val fingerprint = (1..20).joinToString(":") { "%02X".format(it) }
        assertEquals(
            "01:02:03:04:05:06:07:08\n09:0A:0B:0C:0D:0E:0F:10\n11:12:13:14",
            SiteCertificateRules.fingerprintLines(fingerprint),
        )
    }

    @Test
    fun `a certificate past its end date is expired`() {
        val site = SiteCertificate("example.com", null, null, 0L, 1_000L, null)
        assertFalse(SiteCertificateRules.isExpired(site, 1_000L))
        assertTrue(SiteCertificateRules.isExpired(site, 1_001L))
        assertFalse(SiteCertificateRules.isExpired(site.copy(validUntilMillis = null), 5_000L))
    }

    private companion object {
        const val PEM = """
        -----BEGIN CERTIFICATE-----
        MIIBoTCCAUigAwIBAgIBATAKBggqhkjOPQQDAjAwMRgwFgYDVQQKDA9Wb2xhIFRl
        c3QsIEluYy4xFDASBgNVBAMMC2V4YW1wbGUuY29tMB4XDTI2MTAwMzIxMDAyN1oX
        DTI3MTAwMzIxMDAyN1owMDEYMBYGA1UECgwPVm9sYSBUZXN0LCBJbmMuMRQwEgYD
        VQQDDAtleGFtcGxlLmNvbTBZMBMGByqGSM49AgEGCCqGSM49AwEHA0IABJE9fwwR
        uU5RzriQc9BZetwpVNwdKfonxgX0eCmsMDX8I4HADhA8/25DXRoaCwt8EaKDeajW
        bNj1sDN0WDqTqj6jUzBRMB0GA1UdDgQWBBRwlf+ZIvv+n7a03FDIRrsbiBQfgTAf
        BgNVHSMEGDAWgBRwlf+ZIvv+n7a03FDIRrsbiBQfgTAPBgNVHRMBAf8EBTADAQH/
        MAoGCCqGSM49BAMCA0cAMEQCIHHdwMmIDB42qf8TITYdDJkTPnp/Sh1wfPaYVd/I
        4FglAiAe9p6OjhsSu9Oj7ScCpGLiQ362qJJK1E9CVyFEHcMhdw==
        -----END CERTIFICATE-----
        """
    }
}
