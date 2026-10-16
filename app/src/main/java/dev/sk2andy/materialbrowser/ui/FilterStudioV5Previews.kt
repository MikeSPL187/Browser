package dev.sk2andy.materialbrowser.ui

import androidx.compose.runtime.Composable
import dev.sk2andy.materialbrowser.blocking.CandyRule
import dev.sk2andy.materialbrowser.blocking.CandyRuleAction
import dev.sk2andy.materialbrowser.blocking.CandyRuleKind
import dev.sk2andy.materialbrowser.blocking.CandyRuleOrigin
import dev.sk2andy.materialbrowser.blocking.CandyRulePreview
import dev.sk2andy.materialbrowser.ui.theme.MaterialBrowserTheme
import dev.sk2andy.materialbrowser.ui.theme.VolaPreviews

/* Б12a: the filter studio under ui.v5. Preview-only sample hosts are not localized. */

private val previewRules = listOf(
    CandyRule(
        id = "ads",
        action = CandyRuleAction.Block,
        kind = CandyRuleKind.RequestHost,
        requestHost = "ads.example",
        hitCount = 42,
    ),
    CandyRule(
        id = "cdn",
        action = CandyRuleAction.Allow,
        kind = CandyRuleKind.HostPair,
        firstPartyHost = "news.example",
        requestHost = "cdn.example",
        origin = CandyRuleOrigin.PrivacyXRay,
        active = false,
    ),
    CandyRule(
        id = "banner",
        action = CandyRuleAction.Cosmetic,
        kind = CandyRuleKind.CosmeticCss,
        firstPartyHost = "shop.example",
        cosmeticSelector = ".sponsored",
        profileId = "work",
    ),
)

@Composable
private fun StudioPreview(rules: List<CandyRule> = previewRules, selectedRuleId: String? = null) {
    MaterialBrowserTheme {
        FilterStudioV5(
            rules = rules,
            subscriptionRules = rules,
            isIncognito = false,
            profiles = emptyList(),
            currentProfileId = "default",
            currentUrl = "https://news.example/",
            recentDomain = "ads.example",
            selectedRuleId = selectedRuleId,
            onTest = { null },
            onAdd = { it },
            onUpdate = { it },
            onToggle = { _, _ -> },
            onDelete = {},
            onParseImport = { CandyRulePreview(rules = emptyList(), errors = emptyList()) },
            onApplyImport = { 0 },
            onApplySubscription = { _, _ -> 0 },
            onExport = { "" },
            onDismiss = {},
        )
    }
}

/** The rules with type segments and search, then the request test and the tools. */
@VolaPreviews
@Composable
private fun FilterStudioListV5Preview() {
    StudioPreview()
}

/** No rules of the user's own yet. */
@VolaPreviews
@Composable
private fun FilterStudioEmptyV5Preview() {
    StudioPreview(rules = emptyList())
}

/** One rule's page: the switch, what it does, where it came from, edit and delete. */
@VolaPreviews
@Composable
private fun FilterRulePageV5Preview() {
    StudioPreview(selectedRuleId = "cdn")
}
