package dev.sk2andy.materialbrowser.ui

import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.blocking.CandyDecisionAction
import dev.sk2andy.materialbrowser.blocking.CandyRule
import dev.sk2andy.materialbrowser.blocking.CandyRuleAction
import dev.sk2andy.materialbrowser.blocking.CandyRuleDecision
import dev.sk2andy.materialbrowser.blocking.CandyRuleKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FilterStudioV5RulesTest {
    private val block = rule("ads", CandyRuleAction.Block, requestHost = "ads.example")
    private val allow = rule("cdn", CandyRuleAction.Allow, requestHost = "cdn.example", profileId = "work")
    private val css = rule("css", CandyRuleAction.Cosmetic, selector = ".sponsored")

    @Test
    fun `type segments keep only rules of that action`() {
        val rules = listOf(block, allow, css)

        assertEquals(rules, filterStudioRules(rules, "", StudioTypeFilter.All, false, "default"))
        assertEquals(listOf(block), filterStudioRules(rules, "", StudioTypeFilter.Block, false, "default"))
        assertEquals(listOf(allow), filterStudioRules(rules, "", StudioTypeFilter.Allow, false, "default"))
        assertEquals(listOf(css), filterStudioRules(rules, "", StudioTypeFilter.Css, false, "default"))
    }

    @Test
    fun `search ignores case and spaces and only this profile keeps its own rules`() {
        val rules = listOf(block, allow, css)

        assertEquals(listOf(block), filterStudioRules(rules, "  ADS.", StudioTypeFilter.All, false, "default"))
        assertEquals(listOf(css), filterStudioRules(rules, "sponsored", StudioTypeFilter.All, false, "default"))
        assertEquals(listOf(allow), filterStudioRules(rules, "", StudioTypeFilter.All, true, "work"))
    }

    @Test
    fun `a rule of every profile has no profile`() {
        assertNull(FilterStudioV5Rules.profileOf(block, emptyList()))
        assertNull(FilterStudioV5Rules.profileOf(allow, emptyList()))
    }

    @Test
    fun `the test line names the deciding rule's group`() {
        assertEquals(
            R.string.filter_test_no_match to null,
            FilterStudioV5Rules.testResult(FilterStudioV5Test(null)),
        )
        assertEquals(
            R.string.filter_test_blocked_by to "Personal",
            FilterStudioV5Rules.testResult(FilterStudioV5Test(CandyRuleDecision(CandyDecisionAction.Block, "ads", block))),
        )
        assertEquals(
            R.string.filter_test_allowed_by to "Personal",
            FilterStudioV5Rules.testResult(FilterStudioV5Test(CandyRuleDecision(CandyDecisionAction.Allow, "cdn", allow))),
        )
    }

    private fun rule(
        id: String,
        action: CandyRuleAction,
        requestHost: String? = null,
        selector: String? = null,
        profileId: String? = null,
    ) = CandyRule(
        id = id,
        action = action,
        kind = if (selector != null) CandyRuleKind.CosmeticCss else CandyRuleKind.RequestHost,
        requestHost = requestHost,
        firstPartyHost = "site.example".takeIf { selector != null },
        cosmeticSelector = selector,
        profileId = profileId,
    )
}
