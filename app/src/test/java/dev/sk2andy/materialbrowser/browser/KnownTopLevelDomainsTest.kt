package dev.sk2andy.materialbrowser.browser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KnownTopLevelDomainsTest {
    @Test
    fun `the root zone list is sorted, unique, lower-case Latin`() {
        val list = KnownTopLevelDomains.rootZoneInOrder()
        assertEquals(list.sorted(), list)
        assertEquals(list.size, list.toSet().size)
        assertTrue(list.all { Regex("[a-z]{2,}").matches(it) })
        // IANA delegates more than a thousand Latin ones; a list this short lost lines.
        assertTrue(list.size > 1_200)
    }

    @Test
    fun `knows the common and the country ones, in any case`() {
        listOf("com", "org", "net", "ru", "de", "app", "dev", "io", "ai", "su", "COM", "Ru").forEach { label ->
            assertTrue(label, KnownTopLevelDomains.contains(label))
        }
    }

    @Test
    fun `does not take file extensions and words for domains`() {
        listOf("txt", "js", "exe", "jpg", "pdf", "who", "json", "html", "py3", "").forEach { label ->
            assertFalse(label, KnownTopLevelDomains.contains(label))
        }
    }

    @Test
    fun `knows the names networks use among themselves`() {
        listOf("local", "lan", "home", "internal", "localhost").forEach { label ->
            assertTrue(label, KnownTopLevelDomains.contains(label))
        }
    }
}
