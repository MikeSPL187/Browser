package dev.sk2andy.materialbrowser.ui

import dev.sk2andy.materialbrowser.blocking.CandyRule
import dev.sk2andy.materialbrowser.blocking.CandyRuleAction
import dev.sk2andy.materialbrowser.blocking.CandyRuleKind
import dev.sk2andy.materialbrowser.blocking.CandyRuleOrigin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FilterRuleSheetV5RulesTest {
    @Test
    fun `hiding is always css and the request actions never are`() {
        assertEquals(CandyRuleKind.CosmeticCss, filterRuleKindFor(CandyRuleAction.Cosmetic, CandyRuleKind.HostPair))
        assertEquals(CandyRuleKind.RequestHost, filterRuleKindFor(CandyRuleAction.Block, CandyRuleKind.CosmeticCss))
        assertEquals(CandyRuleKind.HostPair, filterRuleKindFor(CandyRuleAction.Allow, CandyRuleKind.HostPair))
    }

    @Test
    fun `add waits for every field the kind needs`() {
        assertTrue(FilterRuleSheetV5Rules.canSave(CandyRuleKind.RequestHost, "ads.example", "", ""))
        assertFalse(FilterRuleSheetV5Rules.canSave(CandyRuleKind.RequestHost, " ", "", ""))
        assertFalse(FilterRuleSheetV5Rules.canSave(CandyRuleKind.HostPair, "cdn.example", "", ""))
        assertTrue(FilterRuleSheetV5Rules.canSave(CandyRuleKind.HostPair, "cdn.example", "news.example", ""))
        assertFalse(FilterRuleSheetV5Rules.canSave(CandyRuleKind.CosmeticCss, "", "shop.example", ""))
        assertTrue(FilterRuleSheetV5Rules.canSave(CandyRuleKind.CosmeticCss, "", "shop.example", ".ad"))
    }

    @Test
    fun `a new rule is personal and blanks become none`() {
        val rule = edited(initial = null, kind = CandyRuleKind.RequestHost, requestHost = "ads.example", firstParty = " ")

        assertEquals("Personal", rule.group)
        assertEquals(CandyRuleOrigin.User, rule.origin)
        assertEquals("ads.example", rule.requestHost)
        assertNull(rule.firstPartyHost)
        assertNull(rule.cosmeticSelector)
        assertEquals(7L, rule.updatedAtMillis)
    }

    @Test
    fun `an edited rule keeps its identity and a gone profile falls back unless current`() {
        val initial = CandyRule(
            id = "r1",
            action = CandyRuleAction.Block,
            kind = CandyRuleKind.RequestHost,
            requestHost = "old.example",
            group = "Imported",
            hitCount = 3,
        )

        val kept = edited(initial, CandyRuleKind.RequestHost, "new.example", profileId = "current")
        assertEquals("r1", kept.id)
        assertEquals("Imported", kept.group)
        assertEquals(3, kept.hitCount)
        assertEquals("new.example", kept.requestHost)
        assertEquals("current", kept.profileId)

        assertNull(edited(initial, CandyRuleKind.RequestHost, "new.example", profileId = "gone").profileId)
    }

    private fun edited(
        initial: CandyRule?,
        kind: CandyRuleKind,
        requestHost: String,
        firstParty: String = "",
        profileId: String? = null,
    ) = editedFilterRule(
        initialRule = initial,
        action = CandyRuleAction.Block,
        kind = kind,
        requestHost = requestHost,
        firstPartyHost = firstParty,
        selector = "",
        profileId = profileId,
        profiles = emptyList(),
        currentProfileId = "current",
        personalGroup = "Personal",
        nowMillis = 7L,
    )
}
