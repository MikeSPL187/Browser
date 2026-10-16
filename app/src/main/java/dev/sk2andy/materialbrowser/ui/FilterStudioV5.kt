package dev.sk2andy.materialbrowser.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.blocking.CandyDecisionAction
import dev.sk2andy.materialbrowser.blocking.CandyRule
import dev.sk2andy.materialbrowser.blocking.CandyRuleDecision
import dev.sk2andy.materialbrowser.blocking.CandyRulePreview
import dev.sk2andy.materialbrowser.browser.BrowserProfile
import dev.sk2andy.materialbrowser.shared.ui.icons.VolaIcons
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Sizes
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Spacing
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Theme
import dev.sk2andy.materialbrowser.ui.theme.v5.VolaV5Theme
import dev.sk2andy.materialbrowser.ui.v5.V5ListDefaults
import dev.sk2andy.materialbrowser.ui.v5.V5ListGroup
import dev.sk2andy.materialbrowser.ui.v5.V5ListRow
import dev.sk2andy.materialbrowser.ui.v5.V5RowTrailing
import dev.sk2andy.materialbrowser.ui.v5.V5SearchField
import dev.sk2andy.materialbrowser.ui.v5.V5Segmented
import java.text.DateFormat
import java.util.Date

/**
 * The filter studio under ui.v5 (V28, Б12a), as a v5 settings page: the rules first (type
 * segments, search, only this profile's), each a row that says whether it is on and opens its own
 * page (the switch, what it does, where it came from, edit, delete); then the request test, the
 * tools and the built-in protection note. The rule editor, import, the web list and export are v5
 * sheets. What each action does is the v4 screen's, through the same callbacks; v4 is unchanged.
 */
@Composable
internal fun FilterStudioV5(
    rules: List<CandyRule>,
    subscriptionRules: List<CandyRule>,
    isIncognito: Boolean,
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
    var query by rememberSaveable { mutableStateOf("") }
    var typeFilter by rememberSaveable { mutableStateOf(StudioTypeFilter.All) }
    var profileOnly by rememberSaveable { mutableStateOf(false) }
    var liveInput by rememberSaveable(recentDomain, currentUrl) {
        mutableStateOf(recentDomain.orEmpty().ifBlank { currentUrl })
    }
    var liveDecision by remember { mutableStateOf<FilterStudioV5Test?>(null) }
    // A rule chosen in X-Ray opens on its own page, as v4 scrolls to it.
    var openId by rememberSaveable(selectedRuleId) { mutableStateOf(selectedRuleId) }
    val dialogs = remember { FilterStudioDialogState() }
    val filtered = remember(rules, query, typeFilter, profileOnly, currentProfileId) {
        filterStudioRules(rules, query, typeFilter, profileOnly, currentProfileId)
    }
    // A deleted rule's page has nothing left to show: the list comes back.
    val open = rules.firstOrNull { it.id == openId }
    BackHandler { if (open != null) openId = null else onDismiss() }
    val dark = MaterialTheme.colorScheme.background.luminance() < FILTER_STUDIO_DARK_SHELL_LUMINANCE
    VolaV5Theme(dark = dark) {
        val colors = V5Theme.colors
        Column(
            Modifier
                .fillMaxSize()
                .background(colors.background)
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .testTag(FilterStudioTestTags.Screen),
        ) {
            FilterStudioPageBar(
                title = open?.displayTarget() ?: stringResource(R.string.filter_studio_title),
                onBack = { if (open != null) openId = null else onDismiss() },
                onAdd = { dialogs.addVisible = true }.takeIf { open == null },
            )
            Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(top = V5Spacing.space2, bottom = V5Spacing.space8)
                    .testTag(FilterStudioTestTags.Body),
                verticalArrangement = Arrangement.spacedBy(V5Spacing.space6),
            ) {
                if (open == null) {
                    RuleFinder(
                        query = query,
                        onQueryChange = { query = it },
                        typeFilter = typeFilter,
                        onTypeFilterChange = { typeFilter = it },
                        profileOnly = profileOnly,
                        onProfileOnlyChange = { profileOnly = it },
                    )
                    RuleList(
                        rules = filtered,
                        anyRules = rules.isNotEmpty(),
                        onOpen = { openId = it.id },
                    )
                    RequestTest(
                        input = liveInput,
                        onInputChange = {
                            liveInput = it
                            liveDecision = null
                        },
                        result = liveDecision,
                        onTest = { liveDecision = FilterStudioV5Test(onTest(liveInput)) },
                    )
                    Tools(
                        onWebList = { dialogs.subscriptionVisible = true },
                        onImport = { dialogs.importVisible = true },
                        onExport = { dialogs.exportVisible = true },
                    )
                } else {
                    RulePage(
                        rule = open,
                        profileLabel = open.profileId?.let { id ->
                            FilterStudioV5Rules.profileOf(open, profiles)?.workspaceDisplayName() ?: id
                        } ?: stringResource(R.string.filter_scope_global),
                        onToggle = { onToggle(open.id, it) },
                        onEdit = { dialogs.editingRule = open },
                        onDelete = {
                            onDelete(open.id)
                            openId = null
                        },
                    )
                }
            }
        }
        FilterStudioDialogs(
            dialogs = dialogs,
            subscriptionRules = subscriptionRules,
            isIncognito = isIncognito,
            profiles = profiles,
            currentProfileId = currentProfileId,
            onAdd = onAdd,
            onUpdate = onUpdate,
            onParseImport = onParseImport,
            onApplyImport = onApplyImport,
            onApplySubscription = onApplySubscription,
            onExport = onExport,
            uiV5 = true,
        )
    }
}

/** The last request test; [decision] null means no rule matched. */
internal data class FilterStudioV5Test(val decision: CandyRuleDecision?)

/** The page's bar: back, the title in the middle and, on the list, «Add rule». */
@Composable
private fun FilterStudioPageBar(title: String, onBack: () -> Unit, onAdd: (() -> Unit)?) {
    val colors = V5Theme.colors
    Box(Modifier.fillMaxWidth().heightIn(min = V5Sizes.touchTarget)) {
        IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart).padding(start = V5Spacing.space1)) {
            Icon(VolaIcons.ArrowBack, contentDescription = stringResource(R.string.action_back), tint = colors.accent)
        }
        Text(
            title,
            style = V5Theme.type.headline,
            color = colors.label,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = V5Spacing.space12)
                .semantics { heading() },
        )
        if (onAdd != null) {
            Row(Modifier.align(Alignment.CenterEnd).padding(end = V5Spacing.space1)) {
                IconButton(onClick = onAdd, modifier = Modifier.testTag(FilterStudioTestTags.Add)) {
                    Icon(
                        VolaIcons.Add,
                        contentDescription = stringResource(R.string.filter_add_rule),
                        tint = colors.accent,
                    )
                }
            }
        }
    }
}

@Composable
private fun RuleFinder(
    query: String,
    onQueryChange: (String) -> Unit,
    typeFilter: StudioTypeFilter,
    onTypeFilterChange: (StudioTypeFilter) -> Unit,
    profileOnly: Boolean,
    onProfileOnlyChange: (Boolean) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(V5Spacing.space3)) {
        V5Segmented(
            options = StudioTypeFilter.entries.map { it.label() },
            selectedIndex = typeFilter.ordinal,
            onSelect = { onTypeFilterChange(StudioTypeFilter.entries[it]) },
            modifier = Modifier.padding(horizontal = V5Spacing.marginPhone),
        )
        V5SearchField(
            query = query,
            onQueryChange = onQueryChange,
            hint = stringResource(R.string.filter_search),
            modifier = Modifier.padding(horizontal = V5Spacing.marginPhone),
            fieldModifier = Modifier.testTag(FilterStudioTestTags.Search),
        )
        V5ListGroup(dividerInset = V5ListDefaults.dividerInsetText) {
            V5ListRow(
                title = stringResource(R.string.filter_v5_profile_only),
                trailing = V5RowTrailing.Toggle(profileOnly, onProfileOnlyChange),
            )
        }
    }
}

@Composable
private fun RuleList(rules: List<CandyRule>, anyRules: Boolean, onOpen: (CandyRule) -> Unit) {
    val colors = V5Theme.colors
    if (rules.isEmpty()) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = V5Spacing.marginPhone + V5Spacing.space4, vertical = V5Spacing.space4)
                .testTag(FilterStudioTestTags.EmptyState),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(V5Spacing.space2),
        ) {
            Text(
                stringResource(if (anyRules) R.string.filter_no_results_title else R.string.filter_empty_title),
                style = V5Theme.type.headline,
                color = colors.label,
                textAlign = TextAlign.Center,
            )
            Text(
                stringResource(if (anyRules) R.string.filter_no_results_body else R.string.filter_empty_body),
                style = V5Theme.type.callout,
                color = colors.labelSecondary,
                textAlign = TextAlign.Center,
            )
        }
        return
    }
    V5ListGroup(
        header = stringResource(R.string.filter_rule_results_section),
        footer = stringResource(R.string.filter_rules_count, rules.size),
        dividerInset = V5ListDefaults.dividerInsetText,
    ) {
        rules.forEach { rule ->
            V5ListRow(
                title = rule.displayTarget(),
                subtitle = "${rule.action.label()} · ${rule.scopeLabel()} · ${rule.group}",
                value = stringResource(if (rule.active) R.string.filter_v5_on else R.string.filter_v5_off),
                trailing = V5RowTrailing.Chevron,
                onClick = { onOpen(rule) },
            )
        }
    }
}

/** «Check a request»: a host or address, «Check», and which rule would decide it. */
@Composable
private fun RequestTest(
    input: String,
    onInputChange: (String) -> Unit,
    result: FilterStudioV5Test?,
    onTest: () -> Unit,
) {
    val colors = V5Theme.colors
    val label = stringResource(R.string.filter_test_request)
    Column(
        Modifier.testTag(FilterStudioTestTags.LiveTest),
        verticalArrangement = Arrangement.spacedBy(V5Spacing.space2),
    ) {
        Text(
            stringResource(R.string.filter_test_section),
            style = V5Theme.type.footnote,
            color = colors.labelSecondary,
            modifier = Modifier
                .padding(horizontal = V5Spacing.marginPhone + V5Spacing.space4)
                .semantics { heading() },
        )
        FilterTextField(
            value = input,
            placeholder = label,
            onValueChange = onInputChange,
            onDone = onTest,
            imeAction = ImeAction.Go,
        )
        V5ListGroup(
            footer = result?.let { FilterStudioV5Rules.testResult(it) }?.let { (res, group) ->
                if (group == null) stringResource(res) else stringResource(res, group)
            } ?: stringResource(R.string.filter_test_explanation),
            dividerInset = V5ListDefaults.dividerInsetText,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        ) {
            V5ListRow(
                title = stringResource(R.string.filter_test_action),
                titleColor = colors.accent,
                enabled = input.isNotBlank(),
                onClick = onTest,
            )
        }
    }
}

@Composable
private fun Tools(onWebList: () -> Unit, onImport: () -> Unit, onExport: () -> Unit) {
    val colors = V5Theme.colors
    V5ListGroup(
        header = stringResource(R.string.filter_tools_section),
        footer = stringResource(R.string.filter_transfer_body),
        dividerInset = V5ListDefaults.dividerInsetText,
    ) {
        V5ListRow(
            title = stringResource(R.string.filter_subscription),
            trailing = V5RowTrailing.Chevron,
            onClick = onWebList,
            modifier = Modifier.testTag(FilterStudioTestTags.WebList),
        )
        V5ListRow(
            title = stringResource(R.string.filter_import),
            titleColor = colors.accent,
            onClick = onImport,
            modifier = Modifier.testTag(FilterStudioTestTags.ImportOpen),
        )
        V5ListRow(
            title = stringResource(R.string.filter_export),
            titleColor = colors.accent,
            onClick = onExport,
        )
    }
    Column(
        Modifier
            .padding(horizontal = V5Spacing.marginPhone + V5Spacing.space4)
            .testTag(FilterStudioTestTags.BuiltInProtection),
        verticalArrangement = Arrangement.spacedBy(V5Spacing.space1),
    ) {
        Text(stringResource(R.string.filter_builtin_title), style = V5Theme.type.footnote, color = colors.label)
        Text(stringResource(R.string.filter_builtin_body), style = V5Theme.type.footnote, color = colors.labelSecondary)
    }
}

@Composable
private fun RulePage(
    rule: CandyRule,
    profileLabel: String,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val colors = V5Theme.colors
    val switchDescription = stringResource(
        if (rule.active) R.string.filter_rule_disable else R.string.filter_rule_enable,
        rule.displayTarget(),
    )
    V5ListGroup(
        footer = stringResource(R.string.filter_hits, rule.hitCount),
        dividerInset = V5ListDefaults.dividerInsetText,
    ) {
        V5ListRow(
            title = stringResource(R.string.filter_v5_active),
            trailing = V5RowTrailing.Toggle(rule.active, onToggle),
            modifier = Modifier.semantics { contentDescription = switchDescription },
        )
    }
    V5ListGroup(
        footer = rule.updatedAtMillis.takeIf { rule.sourceUrl != null && it > 0L }?.let { millis ->
            stringResource(
                R.string.filter_updated,
                DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(millis)),
            )
        },
        dividerInset = V5ListDefaults.dividerInsetText,
    ) {
        V5ListRow(title = stringResource(R.string.filter_v5_action), value = rule.action.label())
        V5ListRow(title = stringResource(R.string.filter_scope), value = profileLabel)
        V5ListRow(title = stringResource(R.string.filter_v5_group), value = rule.group)
        V5ListRow(title = stringResource(R.string.filter_v5_origin), value = rule.origin.label())
        rule.sourceUrl?.let { source -> V5ListRow(title = source, titleColor = colors.labelSecondary) }
    }
    V5ListGroup(dividerInset = V5ListDefaults.dividerInsetText) {
        V5ListRow(
            title = stringResource(R.string.filter_edit_rule),
            titleColor = colors.accent,
            onClick = onEdit,
        )
    }
    V5ListGroup(dividerInset = V5ListDefaults.dividerInsetText) {
        V5ListRow(
            title = stringResource(R.string.filter_delete_rule),
            titleColor = colors.danger,
            onClick = onDelete,
        )
    }
}

/** What the v5 page shows, apart from drawing, for unit tests. */
internal object FilterStudioV5Rules {
    /** The profile a rule belongs to; null for a rule of every profile or of a profile that is gone. */
    fun profileOf(rule: CandyRule, profiles: List<BrowserProfile>): BrowserProfile? =
        rule.profileId?.let { id -> profiles.firstOrNull { it.id == id } }

    /** The test's line: which string, and the deciding rule's group when one decided. */
    fun testResult(test: FilterStudioV5Test): Pair<Int, String?> {
        val decision = test.decision ?: return R.string.filter_test_no_match to null
        return when (decision.action) {
            CandyDecisionAction.Block -> R.string.filter_test_blocked_by to decision.rule.group
            CandyDecisionAction.Allow -> R.string.filter_test_allowed_by to decision.rule.group
        }
    }
}

private const val FILTER_STUDIO_DARK_SHELL_LUMINANCE = 0.5f
