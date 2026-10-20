package dev.sk2andy.materialbrowser.blocking

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class FilterRuleBookTest {
    private val changes = mutableListOf<Boolean>()
    private var nextId = 0
    private val book = FilterRuleBook(
        rules = mutableListOf(),
        importedGroup = { "Imported (ru)" },
        subscriptionGroup = { "Subscription" },
        newId = { "id-${++nextId}" },
        now = { 1_000L },
        onChanged = { changes += it },
    )

    private fun block(host: String) = CandyRule.new(
        action = CandyRuleAction.Block,
        kind = CandyRuleKind.RequestHost,
        requestHost = host,
    )

    private fun preview(vararg rules: CandyRule) = CandyRulePreview(rules = rules.toList(), errors = emptyList())

    @Test
    fun aRuleFromARegularTabIsSavedAndADuplicateReturnsTheFirst() {
        val added = book.add(block("ads.example"), temporary = false)!!

        assertEquals(listOf(true), changes)
        assertSame(added, book.add(block("ads.example"), temporary = false))
        assertEquals(1, book.rules.size)
    }

    @Test
    fun aPrivateTabsRuleIsTemporaryAndGoesWithTheLastPrivateTab() {
        book.add(block("ads.example"), temporary = false)
        val temporary = book.add(block("tracker.example"), temporary = true)!!
        book.privateHits[temporary.id] = 3

        assertEquals(listOf(true, false), changes)
        assertEquals(listOf("ads.example"), book.persistentRules().map(CandyRule::requestHost))
        assertEquals(3, book.rulesFor(isPrivate = true).first { it.id == temporary.id }.hitCount)
        assertEquals(1, book.rulesFor(isPrivate = false).size)

        book.clearPrivate()
        assertEquals(listOf("ads.example"), book.rules.map(CandyRule::requestHost))
        assertTrue(book.privateHits.isEmpty())
    }

    @Test
    fun editingKeepsTheHitCountAndDeletingReportsWhatChanged() {
        val rule = book.add(block("ads.example"), temporary = false)!!
        book.rules[0] = book.rules[0].copy(hitCount = 5)

        val edited = book.update(rule.copy(requestHost = "ads2.example"))!!
        assertEquals(5, edited.hitCount)
        assertTrue(book.setActive(rule.id, active = false))
        assertFalse(book.setActive(rule.id, active = false))

        assertTrue(book.delete(rule.id))
        assertFalse(book.delete(rule.id))
        assertTrue(book.rules.isEmpty())
    }

    @Test
    fun anImportSkipsDuplicatesAndNamesTheImportedGroup() {
        book.add(block("ads.example"), temporary = false)
        val imported = block("tracker.example").copy(origin = CandyRuleOrigin.Import, group = "Imported")

        val count = book.applyImport(preview(block("ads.example"), imported), emptyList(), temporary = false)

        assertEquals(1, count)
        assertEquals("Imported (ru)", book.rules.last().group)
    }

    @Test
    fun anImportForAnUnknownWorkspaceIsRefused() {
        val foreign = block("ads.example").copy(profileId = "gone")

        assertEquals(0, book.applyImport(preview(foreign), listOf("home"), temporary = false))
        assertTrue(book.rules.isEmpty())
    }

    @Test
    fun aSubscriptionReplacesItsOwnRulesOnly() {
        val source = "https://lists.example/filters.txt"
        book.add(block("mine.example"), temporary = false)
        assertEquals(2, book.applySubscription(source, preview(block("a.example"), block("b.example")), emptyList(), false))

        assertEquals(1, book.applySubscription(source, preview(block("c.example")), emptyList(), false))

        assertEquals(
            listOf("mine.example", "c.example"),
            book.rules.map(CandyRule::requestHost),
        )
        assertEquals(CandyRuleOrigin.Subscription, book.rules.last().origin)
        assertEquals("lists.example", book.rules.last().group)
    }

    @Test
    fun aSubscriptionFromAnUnsafeAddressIsRefused() {
        assertEquals(0, book.applySubscription("http://lists.example/f.txt", preview(block("a.example")), emptyList(), false))
    }

    @Test
    fun aDeletedWorkspacesRulesGo() {
        book.add(block("ads.example").copy(profileId = "work"), temporary = false)
        book.add(block("ads.example"), temporary = false)

        book.forgetProfile("work")

        assertNull(book.rules.firstOrNull { it.profileId == "work" })
        assertEquals(1, book.rules.size)
    }

    @Test
    fun theExportLeavesTemporaryRulesOut() {
        book.add(block("ads.example"), temporary = false)
        book.add(block("secret.example"), temporary = true)

        val exported = book.export()

        assertTrue("ads.example" in exported)
        assertFalse("secret.example" in exported)
    }
}
