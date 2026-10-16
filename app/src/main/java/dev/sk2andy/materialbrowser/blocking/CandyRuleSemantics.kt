package dev.sk2andy.materialbrowser.blocking

/** What a filter rule does, apart from its id and source: two rules with the same key are one rule. */
internal object CandyRuleSemantics {
    fun key(rule: CandyRule): String = listOf(
        rule.action.name,
        rule.kind.name,
        rule.requestHost.orEmpty(),
        rule.firstPartyHost.orEmpty(),
        rule.cosmeticSelector.orEmpty(),
        rule.profileId.orEmpty(),
    ).joinToString("\u0000")

    fun same(left: CandyRule, right: CandyRule): Boolean = key(left) == key(right)
}
