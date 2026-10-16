package dev.sk2andy.materialbrowser.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.luminance
import dev.sk2andy.materialbrowser.blocking.CandyRule
import dev.sk2andy.materialbrowser.blocking.CandyRuleAction
import dev.sk2andy.materialbrowser.blocking.CandyRuleKind
import dev.sk2andy.materialbrowser.blocking.CandyRuleOrigin
import dev.sk2andy.materialbrowser.blocking.CandyRulePreview
import dev.sk2andy.materialbrowser.ui.theme.MaterialBrowserTheme
import dev.sk2andy.materialbrowser.ui.theme.VolaPreviews
import dev.sk2andy.materialbrowser.ui.theme.v5.VolaV5Theme

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

@Composable
private fun RuleSheetPreview(rule: CandyRule?) {
    MaterialBrowserTheme {
        VolaV5Theme(dark = MaterialTheme.colorScheme.background.luminance() < 0.5f) {
            FilterRuleSheetV5(
                initialRule = rule,
                visible = true,
                profiles = emptyList(),
                currentProfileId = "default",
                onDismiss = {},
                onSave = { true },
            )
        }
    }
}

/** A new rule: block a request host, for every workspace. */
@VolaPreviews
@Composable
private fun FilterRuleAddSheetV5Preview() {
    RuleSheetPreview(rule = null)
}

/** Editing a hiding rule: the site, the CSS selector and its examples. */
@VolaPreviews
@Composable
private fun FilterRuleCssSheetV5Preview() {
    RuleSheetPreview(rule = previewRules.last())
}

@Composable
private fun SheetPreview(content: @Composable () -> Unit) {
    MaterialBrowserTheme {
        VolaV5Theme(dark = MaterialTheme.colorScheme.background.luminance() < 0.5f) { content() }
    }
}

/** Import: paste rules, then analyze. */
@VolaPreviews
@Composable
private fun FilterImportSheetV5Preview() {
    SheetPreview {
        FilterImportSheetV5(
            visible = true,
            profiles = emptyList(),
            currentProfileId = "default",
            onDismiss = {},
            onParse = { CandyRulePreview(rules = emptyList(), errors = emptyList()) },
            onApply = { true },
        )
    }
}

/** The web list in a private tab: the note, uBlock's source, the address and the scope. */
@VolaPreviews
@Composable
private fun FilterSubscriptionSheetV5Preview() {
    SheetPreview {
        FilterSubscriptionSheetV5(
            visible = true,
            existingRules = previewRules,
            profiles = emptyList(),
            currentProfileId = "default",
            isIncognito = true,
            onDismiss = {},
            onApply = { _, _ -> true },
        )
    }
}

/** Export: the rules as text to select and copy. */
@VolaPreviews
@Composable
private fun FilterExportSheetV5Preview() {
    SheetPreview {
        FilterExportSheetV5(
            visible = true,
            text = { "# Vola rules v1\nblock ads.example\nallow news.example cdn.example" },
            onDismiss = {},
        )
    }
}
