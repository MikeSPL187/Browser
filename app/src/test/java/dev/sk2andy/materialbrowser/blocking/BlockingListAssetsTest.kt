package dev.sk2andy.materialbrowser.blocking

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BlockingListAssetsTest {
    @Test
    fun webViewRootComesBeforeTheGeckoExtension() {
        assertEquals(
            listOf("easylist_blocked_hosts.txt", "candy_privacy/rules/easylist_blocked_hosts.txt"),
            BlockingListAssets.candidates("easylist_blocked_hosts.txt"),
        )
    }

    @Test
    fun networkListsShipOnceOutsideTheSharedAssets() {
        val lists = listOf(
            "blocked_hosts.txt",
            "uassets_blocked_hosts.txt",
            "easylist_blocked_hosts.txt",
            "hagezi_blocked_hosts.txt",
            "uassets_blocked_host_pairs.txt",
            "easylist_allowed_host_pairs.txt",
            "uassets_allowed_host_pairs.txt",
            "first_party_family_allowed_host_pairs.txt",
            "uassets_advanced_filters.txt",
            "easylist_cosmetic_rules.txt",
            "uassets_cosmetic_rules.txt",
            "uassets_procedural_cosmetic_rules.txt",
        )
        lists.forEach { name ->
            assertTrue(name, File("src/blocking/assets/$name").isFile)
            // In src/main/assets they would ship twice in the Gecko build.
            assertFalse(name, File("src/main/assets/$name").exists())
        }
    }
}
