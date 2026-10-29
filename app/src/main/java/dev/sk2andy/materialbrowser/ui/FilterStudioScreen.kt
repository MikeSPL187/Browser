package dev.sk2andy.materialbrowser.ui

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.blocking.CandyImportFormat
import dev.sk2andy.materialbrowser.blocking.CandyRule
import dev.sk2andy.materialbrowser.blocking.CandyRuleAction
import dev.sk2andy.materialbrowser.blocking.CandyRuleDecision
import dev.sk2andy.materialbrowser.blocking.CandyRuleKind
import dev.sk2andy.materialbrowser.blocking.CandyRuleOrigin
import dev.sk2andy.materialbrowser.blocking.CandyRulePreview
import dev.sk2andy.materialbrowser.browser.BrowserProfile
internal object FilterStudioTestTags {
    const val Screen = "filter_studio_screen"
    const val Search = "filter_studio_search"
    const val LiveTest = "filter_studio_live_test"
    const val Add = "filter_studio_add"
    const val DeleteConfirm = "filter_studio_delete_confirm"
    const val Body = "filter_studio_body"
    const val EmptyState = "filter_studio_empty_state"
    const val WebList = "filter_studio_web_list"
    const val ImportOpen = "filter_studio_import_open"
    const val ImportInput = "filter_studio_import_input"
    const val ImportAnalyze = "filter_studio_import_analyze"
    const val ImportScopeGlobal = "filter_studio_import_scope_global"
    const val ImportScopeCurrent = "filter_studio_import_scope_current"
    const val ImportSkippedConfirm = "filter_studio_import_skipped_confirm"
    const val ImportConfirm = "filter_studio_import_confirm"
    const val CssAction = "filter_studio_css_action"
    const val CssExplanation = "filter_studio_css_explanation"
    const val CssSite = "filter_studio_css_site"
    const val CssSelector = "filter_studio_css_selector"
    const val SubscriptionPreset = "filter_studio_subscription_preset"
    const val SubscriptionSource = "filter_studio_subscription_source"
    const val SubscriptionScopeGlobal = "filter_studio_subscription_scope_global"
    const val SubscriptionScopeCurrent = "filter_studio_subscription_scope_current"
    const val SubscriptionSkippedConfirm = "filter_studio_subscription_skipped_confirm"
    const val SubscriptionConfirm = "filter_studio_subscription_confirm"
    const val SubscriptionPrivateNote = "filter_studio_subscription_private_note"
    const val BuiltInProtection = "filter_studio_builtin_protection"
}

internal enum class StudioTypeFilter { All, Block, Allow, Css }

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun FilterStudioScreen(
    rules: List<CandyRule>,
    subscriptionRules: List<CandyRule> = rules,
    isIncognito: Boolean = false,
    profiles: List<BrowserProfile>,
    currentProfileId: String,
    currentUrl: String,
    recentDomain: String?,
    selectedRuleId: String?,
    onTest: (String) -> CandyRuleDecision?,
    onAdd: (CandyRule) -> CandyRule?,
    onUpdate: (CandyRule) -> CandyRule?,
    onToggle: (String, Boolean) -> Unit,
    onDelete: (String) -> Unit,
    onParseImport: (String) -> CandyRulePreview,
    onApplyImport: (CandyRulePreview) -> Int,
    onApplySubscription: (String, CandyRulePreview) -> Int,
    onExport: () -> String,
    onDismiss: () -> Unit,
) {
    FilterStudioV5(
        rules = rules,
        subscriptionRules = subscriptionRules,
        isIncognito = isIncognito,
        profiles = profiles,
        currentProfileId = currentProfileId,
        currentUrl = currentUrl,
        recentDomain = recentDomain,
        selectedRuleId = selectedRuleId,
        onTest = onTest,
        onAdd = onAdd,
        onUpdate = onUpdate,
        onToggle = onToggle,
        onDelete = onDelete,
        onParseImport = onParseImport,
        onApplyImport = onApplyImport,
        onApplySubscription = onApplySubscription,
        onExport = onExport,
        onDismiss = onDismiss,
    )
}

/** Which of the studio's dialogs is open; the v4 screen and the v5 page share them. */
@Stable
internal class FilterStudioDialogState {
    var addVisible by mutableStateOf(false)
    var editingRule by mutableStateOf<CandyRule?>(null)
    var importVisible by mutableStateOf(false)
    var exportVisible by mutableStateOf(false)
    var subscriptionVisible by mutableStateOf(false)
}

/** The rule editor, import, export and web-list dialogs, opened through [dialogs]. */
@Composable
internal fun FilterStudioDialogs(
    dialogs: FilterStudioDialogState,
    subscriptionRules: List<CandyRule>,
    isIncognito: Boolean,
    profiles: List<BrowserProfile>,
    currentProfileId: String,
    onAdd: (CandyRule) -> CandyRule?,
    onUpdate: (CandyRule) -> CandyRule?,
    onParseImport: (String) -> CandyRulePreview,
    onApplyImport: (CandyRulePreview) -> Int,
    onApplySubscription: (String, CandyRulePreview) -> Int,
    onExport: () -> String,
) {
    val view = LocalView.current
    FilterRuleSheetV5(
        initialRule = dialogs.editingRule,
        visible = dialogs.addVisible || dialogs.editingRule != null,
        profiles = profiles,
        currentProfileId = currentProfileId,
        onDismiss = {
            dialogs.addVisible = false
            dialogs.editingRule = null
        },
        onSave = { rule ->
            val saved = if (dialogs.editingRule == null) onAdd(rule) else onUpdate(rule)
            if (saved != null) {
                dialogs.addVisible = false
                dialogs.editingRule = null
                view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            }
            saved != null
        },
    )
    FilterImportSheetV5(
        visible = dialogs.importVisible,
        profiles = profiles,
        currentProfileId = currentProfileId,
        onDismiss = { dialogs.importVisible = false },
        onParse = onParseImport,
        onApply = {
            val applied = onApplyImport(it)
            if (applied > 0) {
                dialogs.importVisible = false
                view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                true
            } else {
                false
            }
        },
    )
    FilterExportSheetV5(
        visible = dialogs.exportVisible,
        text = onExport,
        onDismiss = { dialogs.exportVisible = false },
    )
    FilterSubscriptionSheetV5(
        visible = dialogs.subscriptionVisible,
        existingRules = subscriptionRules,
        profiles = profiles,
        currentProfileId = currentProfileId,
        isIncognito = isIncognito,
        onDismiss = { dialogs.subscriptionVisible = false },
        onApply = { source, preview ->
            val applied = onApplySubscription(source, preview)
            if (applied > 0) {
                dialogs.subscriptionVisible = false
                view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                true
            } else {
                false
            }
        },
    )
}

/** The rules [typeFilter], [profileOnly] and the search [query] leave, in their order. */
internal fun filterStudioRules(
    rules: List<CandyRule>,
    query: String,
    typeFilter: StudioTypeFilter,
    profileOnly: Boolean,
    currentProfileId: String,
): List<CandyRule> {
    val needle = query.trim().lowercase()
    return rules.filter { rule ->
        val matchesType = when (typeFilter) {
            StudioTypeFilter.All -> true
            StudioTypeFilter.Block -> rule.action == CandyRuleAction.Block
            StudioTypeFilter.Allow -> rule.action == CandyRuleAction.Allow
            StudioTypeFilter.Css -> rule.action == CandyRuleAction.Cosmetic
        }
        val matchesProfile = !profileOnly || rule.profileId == currentProfileId
        val matchesQuery = needle.isEmpty() || listOfNotNull(
            rule.requestHost,
            rule.firstPartyHost,
            rule.cosmeticSelector,
            rule.group,
            rule.sourceUrl,
        ).any { needle in it.lowercase() }
        matchesType && matchesProfile && matchesQuery
    }
}

/** The rule's kind after its action changes: hiding is always CSS, the others never are. */
internal fun filterRuleKindFor(action: CandyRuleAction, kind: CandyRuleKind): CandyRuleKind = when {
    action == CandyRuleAction.Cosmetic -> CandyRuleKind.CosmeticCss
    kind == CandyRuleKind.CosmeticCss -> CandyRuleKind.RequestHost
    else -> kind
}

/**
 * The rule the editor saves: [initialRule] (a new personal rule when null) with the fields as
 * edited, blanks as none. A profile that is gone falls back to every profile, unless it is the
 * current one.
 */
internal fun editedFilterRule(
    initialRule: CandyRule?,
    action: CandyRuleAction,
    kind: CandyRuleKind,
    requestHost: String,
    firstPartyHost: String,
    selector: String,
    profileId: String?,
    profiles: List<BrowserProfile>,
    currentProfileId: String,
    personalGroup: String,
    nowMillis: Long,
): CandyRule = (
    initialRule ?: CandyRule.new(
        action = action,
        kind = kind,
        group = personalGroup,
        origin = CandyRuleOrigin.User,
    )
).copy(
    action = action,
    kind = kind,
    requestHost = requestHost.takeIf(String::isNotBlank),
    firstPartyHost = firstPartyHost.takeIf(String::isNotBlank),
    cosmeticSelector = selector.takeIf(String::isNotBlank),
    profileId = profileId?.takeIf { id -> profiles.any { it.id == id } }
        ?: profileId?.takeIf { it == currentProfileId },
    updatedAtMillis = nowMillis,
)

@Composable
internal fun StudioTypeFilter.label(): String = stringResource(
    when (this) {
        StudioTypeFilter.All -> R.string.filter_all
        StudioTypeFilter.Block -> R.string.filter_block
        StudioTypeFilter.Allow -> R.string.filter_allow
        StudioTypeFilter.Css -> R.string.filter_css
    },
)

@Composable
internal fun CandyImportFormat.label(): String = stringResource(
    when (this) {
        CandyImportFormat.CandyRulesV1 -> R.string.filter_import_format_candy
        CandyImportFormat.AdblockCompatible -> R.string.filter_import_format_adblock
    },
)

@Composable
internal fun CandyRuleAction.label(): String = stringResource(
    when (this) {
        CandyRuleAction.Block -> R.string.filter_block
        CandyRuleAction.Allow -> R.string.filter_allow
        CandyRuleAction.Cosmetic -> R.string.filter_css
    },
)

@Composable
internal fun CandyRule.scopeLabel(): String = if (profileId == null) {
    stringResource(R.string.filter_scope_global)
} else {
    stringResource(R.string.filter_scope_profile)
}

@Composable
internal fun CandyRuleOrigin.label(): String = stringResource(
    when (this) {
        CandyRuleOrigin.User -> R.string.filter_origin_user
        CandyRuleOrigin.PrivacyXRay -> R.string.filter_origin_xray
        CandyRuleOrigin.Import -> R.string.filter_origin_import
        CandyRuleOrigin.Subscription -> R.string.filter_origin_subscription
    },
)

@Composable
internal fun filterErrorLabel(code: String): String = stringResource(
    when {
        code == "https-required" || code == "invalid-url" -> R.string.filter_error_https
        code == "network" || code.startsWith("http-") -> R.string.filter_error_network
        code in setOf("size-limit", "line-limit", "rule-limit") -> R.string.filter_error_limit
        code == "subscription-css-forbidden" -> R.string.filter_error_subscription_css
        code == "missing-header" -> R.string.filter_error_header
        code == "no-supported-rules" -> R.string.filter_error_no_supported_rules
        code == "unsupported-adblock" -> R.string.filter_error_adblock_unsupported
        else -> R.string.filter_error_invalid
    },
)

internal fun CandyRule.displayTarget(): String = when (kind) {
    CandyRuleKind.RequestHost -> requestHost.orEmpty()
    CandyRuleKind.HostPair -> "${firstPartyHost.orEmpty()} → ${requestHost.orEmpty()}"
    CandyRuleKind.CosmeticCss -> "${firstPartyHost.orEmpty()} · ${cosmeticSelector.orEmpty()}"
}
