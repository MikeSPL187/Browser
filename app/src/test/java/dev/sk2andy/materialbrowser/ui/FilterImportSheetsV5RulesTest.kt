package dev.sk2andy.materialbrowser.ui

import dev.sk2andy.materialbrowser.blocking.CandyRule
import dev.sk2andy.materialbrowser.blocking.CandyRuleAction
import dev.sk2andy.materialbrowser.blocking.CandyRuleKind
import dev.sk2andy.materialbrowser.blocking.CandyRuleLineError
import dev.sk2andy.materialbrowser.blocking.CandyRulePreview
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FilterImportSheetsV5RulesTest {
    private val rule = CandyRule(id = "r", action = CandyRuleAction.Block, kind = CandyRuleKind.RequestHost, requestHost = "a.example")
    private val line = CandyRuleLineError(line = 2, message = "unsupported-adblock")

    @Test
    fun `apply needs a clean preview`() {
        assertFalse(FilterImportSheetsV5Rules.canApply(null, skippedConfirmed = true))
        assertTrue(FilterImportSheetsV5Rules.canApply(CandyRulePreview(listOf(rule), emptyList()), skippedConfirmed = false))
        assertFalse(FilterImportSheetsV5Rules.canApply(CandyRulePreview(listOf(rule), listOf(line)), skippedConfirmed = true))
        assertFalse(
            FilterImportSheetsV5Rules.canApply(CandyRulePreview(listOf(rule), emptyList(), truncated = true), skippedConfirmed = true),
        )
    }

    @Test
    fun `skipped lines must be confirmed`() {
        val preview = CandyRulePreview(listOf(rule), emptyList(), skipped = listOf(line))

        assertFalse(FilterImportSheetsV5Rules.canApply(preview, skippedConfirmed = false))
        assertTrue(FilterImportSheetsV5Rules.canApply(preview, skippedConfirmed = true))
    }

    @Test
    fun `hidden changes count what is past the listed ones on each side`() {
        assertEquals(0, FilterImportSheetsV5Rules.hiddenChanges(added = 4, removed = 1))
        assertEquals(3, FilterImportSheetsV5Rules.hiddenChanges(added = 6, removed = 5))
    }
}
