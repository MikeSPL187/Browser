package dev.sk2andy.materialbrowser.blocking

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExemptSitesTest {
    private val sites = ExemptSites.parse(File("src/blocking/gecko/${PopupListFilter.EXEMPT_SITES_ASSET}").readText())

    @Test
    fun aListedDomainCoversItsSubdomains() {
        assertTrue(sites.containsHost("sberbank.ru"))
        assertTrue(sites.containsHost("online.sberbank.ru"))
        assertTrue(sites.containsHost("Online.SberBank.RU."))
        assertTrue(sites.containsUrl("https://3ds.cardinalcommerce.com/acs?x=1"))
    }

    @Test
    fun lookalikesAndOtherSitesAreNotListed() {
        assertFalse(sites.containsHost("sberbank.ru.example"))
        assertFalse(sites.containsHost("notpaypal.com"))
        assertFalse(sites.containsHost("ru"))
        assertFalse(sites.containsHost(null))
        assertFalse(sites.containsUrl("file:///sdcard/paypal.com"))
        assertFalse(sites.containsUrl("not a url"))
        assertFalse(ExemptSites.None.containsHost("paypal.com"))
    }
}
