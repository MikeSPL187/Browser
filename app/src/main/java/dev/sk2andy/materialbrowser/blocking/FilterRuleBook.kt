package dev.sk2andy.materialbrowser.blocking

import androidx.compose.runtime.mutableStateMapOf

/**
 * The owner's filter rules (#500): adding, editing and removing them, imports, subscriptions and
 * export. Rules made in a private tab are temporary: they live in memory only and go with the last
 * private tab, and so do the hits private tabs count. Matching requests stays with the controller,
 * which rebuilds its matcher in [onChanged].
 */
internal class FilterRuleBook(
    /** The rules the filter studio shows; shared with the controller, edited only here. */
    val rules: MutableList<CandyRule>,
    /** The localized group names the batches get. */
    private val importedGroup: () -> String,
    private val subscriptionGroup: () -> String,
    private val newId: () -> String,
    private val now: () -> Long,
    /** The rules changed; [persist] says whether saved rules did. */
    private val onChanged: (persist: Boolean) -> Unit,
) {
    /** Rules made in a private tab. */
    val temporaryIds = mutableSetOf<String>()

    /** Hits counted in private tabs, kept apart from the saved counts. */
    val privateHits = mutableStateMapOf<String, Int>()

    fun find(ruleId: String): CandyRule? = rules.firstOrNull { it.id == ruleId }

    /** The rules a tab's filter studio lists: a private tab sees every rule with its own hits. */
    fun rulesFor(isPrivate: Boolean): List<CandyRule> =
        if (isPrivate) {
            rules.map { rule ->
                rule.copy(
                    hitCount = (rule.hitCount.toLong() + privateHits.getOrDefault(rule.id, 0))
                        .coerceAtMost(Int.MAX_VALUE.toLong())
                        .toInt(),
                )
            }
        } else {
            persistentRules()
        }

    fun subscriptionRulesFor(isPrivate: Boolean): List<CandyRule> =
        if (isPrivate) rules.filter { it.id in temporaryIds } else persistentRules()

    fun persistentRules(): List<CandyRule> = rules.filterNot { it.id in temporaryIds }

    fun add(candidate: CandyRule, temporary: Boolean): CandyRule? {
        val validated = (CandyRuleValidator.validate(candidate) as? CandyRuleValidation.Valid)?.rule
            ?: return null
        val duplicate = rules.firstOrNull { existing ->
            existing.id == validated.id ||
                (existing.action == validated.action &&
                existing.kind == validated.kind &&
                existing.requestHost == validated.requestHost &&
                existing.firstPartyHost == validated.firstPartyHost &&
                existing.cosmeticSelector == validated.cosmeticSelector &&
                existing.profileId == validated.profileId)
        }
        if (duplicate != null) return duplicate.takeIf { CandyRuleSemantics.same(it, validated) }
        if (rules.size >= CandyRuleValidator.MAX_RULES) return null
        if (validated.kind == CandyRuleKind.CosmeticCss &&
            rules.count { it.kind == CandyRuleKind.CosmeticCss } >= CandyRuleValidator.MAX_COSMETIC_RULES
        ) return null
        rules += validated
        if (temporary) temporaryIds += validated.id
        onChanged(!temporary)
        return validated
    }

    fun setActive(ruleId: String, active: Boolean): Boolean {
        val index = rules.indexOfFirst { it.id == ruleId }
        if (index < 0 || rules[index].active == active) return false
        rules[index] = rules[index].copy(active = active)
        onChanged(ruleId !in temporaryIds)
        return true
    }

    fun update(candidate: CandyRule): CandyRule? {
        val index = rules.indexOfFirst { it.id == candidate.id }
        if (index < 0) return null
        val validated = (CandyRuleValidator.validate(candidate) as? CandyRuleValidation.Valid)?.rule
            ?: return null
        if (rules.any { it.id != validated.id && CandyRuleSemantics.same(it, validated) }) return null
        val previous = rules[index]
        if (previous.kind != CandyRuleKind.CosmeticCss &&
            validated.kind == CandyRuleKind.CosmeticCss &&
            rules.count { it.kind == CandyRuleKind.CosmeticCss } >= CandyRuleValidator.MAX_COSMETIC_RULES
        ) return null
        rules[index] = validated.copy(hitCount = previous.hitCount)
        onChanged(validated.id !in temporaryIds)
        return rules[index]
    }

    fun delete(ruleId: String): Boolean {
        val removed = find(ruleId) ?: return false
        rules.remove(removed)
        val temporary = temporaryIds.remove(ruleId)
        onChanged(!temporary)
        return true
    }

    /** Adds an imported file's rules; [profileIds] are the workspaces a rule may name. */
    fun applyImport(preview: CandyRulePreview, profileIds: List<String>, temporary: Boolean): Int {
        if (!preview.isApplicable) return 0
        if (preview.rules.any { !CandyImportScope.isAllowed(it.profileId, profileIds) }) return 0
        val additions = prepareBatch(preview.rules) ?: return 0
        if (additions.isEmpty()) return 0
        rules += additions
        if (temporary) temporaryIds += additions.map(CandyRule::id)
        onChanged(!temporary)
        return additions.size
    }

    /** Replaces [sourceUrl]'s rules for its one workspace with [preview]'s. */
    fun applySubscription(
        sourceUrl: String,
        preview: CandyRulePreview,
        profileIds: List<String>,
        temporary: Boolean,
    ): Int {
        if (!preview.isApplicable || !CandyRuleValidator.isSafeHttpsUrl(sourceUrl)) return 0
        val targetScopes = preview.rules.map(CandyRule::profileId).toSet()
        if (targetScopes.size != 1) return 0
        val targetProfileId = targetScopes.first()
        if (!CandyImportScope.isAllowed(targetProfileId, profileIds)) return 0
        val sourcePrefix = if (temporary) {
            "private-${newId()}"
        } else {
            "subscription-${sourceUrl.hashCode().toUInt().toString(16)}"
        }
        val imported = CandyRuleValidator.normalizeAll(
            preview.rules.mapIndexed { index, rule ->
                rule.copy(
                    id = "$sourcePrefix-$index-${CandyRuleSemantics.key(rule).hashCode().toUInt().toString(16)}",
                    origin = CandyRuleOrigin.Subscription,
                    sourceUrl = sourceUrl,
                    updatedAtMillis = now(),
                    group = CandyFilterPresets.groupFor(sourceUrl)
                        ?: runCatching { java.net.URI(sourceUrl).host }.getOrNull()?.take(48)
                        ?: subscriptionGroup(),
                )
            },
        )
        val isSameSource = { rule: CandyRule ->
            CandySubscriptionRules.isSameSourceScope(rule, sourceUrl, targetProfileId)
        }
        if (temporary) {
            val oldIds = rules.asSequence()
                .filter { isSameSource(it) && it.id in temporaryIds }
                .map(CandyRule::id)
                .toSet()
            val retained = rules.filterNot { it.id in oldIds }
            val persistentSourceIds = retained.asSequence()
                .filter { isSameSource(it) && it.id !in temporaryIds }
                .map(CandyRule::id)
                .toSet()
            val additions = prepareBatch(
                input = imported,
                base = retained,
                ignoreSemanticsForIds = persistentSourceIds,
            ) ?: return 0
            rules.removeAll { it.id in oldIds }
            temporaryIds.removeAll(oldIds)
            privateHits.keys.removeAll(oldIds)
            rules += additions
            temporaryIds += additions.map(CandyRule::id)
            onChanged(false)
            return additions.size
        }
        val oldIds = rules.asSequence()
            .filter { isSameSource(it) && it.id !in temporaryIds }
            .map(CandyRule::id)
            .toSet()
        val retained = rules.filterNot { it.id in oldIds }
        val additions = prepareBatch(imported, retained) ?: return 0
        rules.removeAll { it.id in oldIds }
        rules += additions
        onChanged(true)
        return additions.size
    }

    fun export(): String = CandyRuleFormat.export(persistentRules())

    /** [profileId] was deleted: its rules go too. */
    fun forgetProfile(profileId: String) {
        val profileRuleIds = rules.filter { it.profileId == profileId }.map(CandyRule::id).toSet()
        if (profileRuleIds.isEmpty()) return
        rules.removeAll { it.id in profileRuleIds }
        temporaryIds.removeAll(profileRuleIds)
        onChanged(true)
    }

    /** The last private tab closed: its rules and hits go. */
    fun clearPrivate() {
        privateHits.clear()
        if (temporaryIds.isEmpty()) return
        rules.removeAll { it.id in temporaryIds }
        temporaryIds.clear()
        onChanged(false)
    }

    /**
     * [input] validated and ready to join [base]: duplicates of a rule already there are dropped
     * and a taken id is replaced; null when one rule is invalid or the batch breaks a limit.
     */
    private fun prepareBatch(
        input: List<CandyRule>,
        base: List<CandyRule> = rules,
        ignoreSemanticsForIds: Set<String> = emptySet(),
    ): List<CandyRule>? {
        val existingIds = base.mapTo(mutableSetOf(), CandyRule::id)
        val existingSemantics = base.asSequence()
            .filterNot { it.id in ignoreSemanticsForIds }
            .mapTo(mutableSetOf(), CandySubscriptionRules::storageKey)
        var cosmeticCount = base.count { it.kind == CandyRuleKind.CosmeticCss }
        val additions = ArrayList<CandyRule>(input.size)
        for (inputRule in input) {
            var rule = (CandyRuleValidator.validate(inputRule) as? CandyRuleValidation.Valid)?.rule
                ?: return null
            if (rule.origin == CandyRuleOrigin.Import && rule.group == "Imported") {
                rule = rule.copy(group = importedGroup())
            }
            val semantics = CandySubscriptionRules.storageKey(rule)
            if (!existingSemantics.add(semantics)) continue
            if (rule.id in existingIds) rule = rule.copy(id = newId())
            if (base.size + additions.size >= CandyRuleValidator.MAX_RULES) return null
            if (rule.kind == CandyRuleKind.CosmeticCss &&
                ++cosmeticCount > CandyRuleValidator.MAX_COSMETIC_RULES
            ) return null
            existingIds += rule.id
            additions += rule
        }
        return additions
    }
}
