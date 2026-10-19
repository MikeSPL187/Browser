package dev.sk2andy.materialbrowser.browser

import kotlin.test.Test
import kotlin.test.assertEquals

class DistinctNamesTest {
    @Test
    fun aFreeNameStaysAsItIs() {
        assertEquals("Work", DistinctNames.distinct("Work", listOf("Home"), maxLength = 32))
    }

    @Test
    fun aTakenNameGetsTheLowestFreeNumber() {
        assertEquals("Work 2", DistinctNames.distinct("Work", listOf("work "), maxLength = 32))
        assertEquals("Work 3", DistinctNames.distinct("Work", listOf("Work", "Work 2"), maxLength = 32))
        assertEquals("Group 2", DistinctNames.distinct("Group", listOf("Group", "Group 3"), maxLength = 32))
    }

    @Test
    fun theNumberStillFitsTheLength() {
        val long = "a".repeat(10)
        assertEquals("aaaaaaaa 2", DistinctNames.distinct(long, listOf(long), maxLength = 10))
        val emoji = "aaaaaaa😀"
        assertEquals("aaaaaaa 2", DistinctNames.distinct(emoji, listOf(emoji), maxLength = 10))
    }
}
