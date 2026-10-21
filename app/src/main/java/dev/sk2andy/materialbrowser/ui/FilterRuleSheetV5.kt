package dev.sk2andy.materialbrowser.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.blocking.CandyRule
import dev.sk2andy.materialbrowser.blocking.CandyRuleAction
import dev.sk2andy.materialbrowser.blocking.CandyRuleKind
import dev.sk2andy.materialbrowser.browser.BrowserProfile
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Sizes
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Spacing
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Theme
import dev.sk2andy.materialbrowser.ui.v5.V5ListDefaults
import dev.sk2andy.materialbrowser.ui.v5.V5ListGroup
import dev.sk2andy.materialbrowser.ui.v5.V5ListRow
import dev.sk2andy.materialbrowser.ui.v5.V5RowTrailing
import dev.sk2andy.materialbrowser.ui.v5.V5Segmented
import dev.sk2andy.materialbrowser.ui.v5.V5SheetHeader
import dev.sk2andy.materialbrowser.ui.v5.V5WindowSheet
import dev.sk2andy.materialbrowser.ui.v5.v5FieldLabel

/**
 * The filter rule editor under ui.v5 (V28, Б12a-4): a v5 sheet with what the rule does (block,
 * allow, hide) and, for requests, a host or a site → host pair; the hosts or the site and CSS
 * selector; where it applies; then «Add» or «Done». What it saves is the v4 editor's rule
 * ([editedFilterRule]); a rule the store refuses keeps the sheet open with the reason.
 */
@Composable
internal fun FilterRuleSheetV5(
    initialRule: CandyRule?,
    visible: Boolean,
    profiles: List<BrowserProfile>,
    currentProfileId: String,
    onDismiss: () -> Unit,
    onSave: (CandyRule) -> Boolean,
) {
    // Every opening starts from the rule (or blank); the sheet leaves on its spring after saving,
    // still showing what was saved.
    val edited = rememberFilterSheetOpening(visible, initialRule?.id)
    if (edited == 0) return
    var shownRule by remember { mutableStateOf(initialRule) }
    LaunchedEffect(visible, initialRule) { if (visible) shownRule = initialRule }
    val rule = if (visible) initialRule else shownRule
    var action by remember(edited) { mutableStateOf(rule?.action ?: CandyRuleAction.Block) }
    var kind by remember(edited) { mutableStateOf(rule?.kind ?: CandyRuleKind.RequestHost) }
    var requestHost by remember(edited) { mutableStateOf(rule?.requestHost.orEmpty()) }
    var firstPartyHost by remember(edited) { mutableStateOf(rule?.firstPartyHost.orEmpty()) }
    var selector by remember(edited) { mutableStateOf(rule?.cosmeticSelector.orEmpty()) }
    var profileId by remember(edited) { mutableStateOf(rule?.profileId) }
    var invalid by remember(edited) { mutableStateOf(false) }
    val personalGroup = stringResource(R.string.filter_group_personal)
    val colors = V5Theme.colors
    val title = stringResource(if (rule == null) R.string.filter_add_rule else R.string.filter_edit_rule)
    val save = {
        invalid = !onSave(
            editedFilterRule(
                initialRule = rule,
                action = action,
                kind = kind,
                requestHost = requestHost,
                firstPartyHost = firstPartyHost,
                selector = selector,
                profileId = profileId,
                profiles = profiles,
                currentProfileId = currentProfileId,
                personalGroup = personalGroup,
                nowMillis = System.currentTimeMillis(),
            ),
        )
    }
    V5WindowSheet(visible = visible, onDismiss = onDismiss, title = title) {
        V5SheetHeader(title = title, onClose = onDismiss)
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(top = V5Spacing.space3, bottom = V5Spacing.space4),
            verticalArrangement = Arrangement.spacedBy(V5Spacing.space4),
        ) {
            val actions = FilterRuleSheetV5Rules.actions
            V5Segmented(
                options = actions.map { it.label() },
                selectedIndex = actions.indexOf(action),
                onSelect = { index ->
                    action = actions[index]
                    kind = filterRuleKindFor(action, kind)
                    invalid = false
                },
                modifier = Modifier
                    .padding(horizontal = V5Spacing.marginPhone)
                    .testTag(FilterStudioTestTags.CssAction),
            )
            if (action != CandyRuleAction.Cosmetic) {
                val kinds = FilterRuleSheetV5Rules.requestKinds
                V5Segmented(
                    options = listOf(
                        stringResource(R.string.filter_type_host),
                        stringResource(R.string.filter_type_pair),
                    ),
                    selectedIndex = kinds.indexOf(kind).coerceAtLeast(0),
                    onSelect = { index ->
                        kind = kinds[index]
                        invalid = false
                    },
                    modifier = Modifier.padding(horizontal = V5Spacing.marginPhone),
                )
            } else {
                Text(
                    stringResource(R.string.filter_css_explanation_body),
                    style = V5Theme.type.footnote,
                    color = colors.labelSecondary,
                    modifier = Modifier
                        .padding(horizontal = V5Spacing.marginPhone + V5Spacing.space4)
                        .testTag(FilterStudioTestTags.CssExplanation),
                )
            }
            if (kind != CandyRuleKind.RequestHost) {
                FilterTextField(
                    value = firstPartyHost,
                    placeholder = stringResource(
                        if (kind == CandyRuleKind.CosmeticCss) R.string.filter_css_site else R.string.filter_first_party_host,
                    ),
                    onValueChange = {
                        firstPartyHost = it
                        invalid = false
                    },
                    modifier = if (kind == CandyRuleKind.CosmeticCss) {
                        Modifier.testTag(FilterStudioTestTags.CssSite)
                    } else {
                        Modifier
                    },
                )
            }
            if (kind != CandyRuleKind.CosmeticCss) {
                FilterTextField(
                    value = requestHost,
                    placeholder = stringResource(R.string.filter_request_host),
                    onValueChange = {
                        requestHost = it
                        invalid = false
                    },
                    onDone = save,
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(V5Spacing.space2)) {
                    FilterTextField(
                        value = selector,
                        placeholder = stringResource(R.string.filter_css_selector),
                        onValueChange = {
                            selector = it
                            invalid = false
                        },
                        onDone = save,
                        modifier = Modifier.testTag(FilterStudioTestTags.CssSelector),
                    )
                    Text(
                        stringResource(R.string.filter_css_selector_examples),
                        style = V5Theme.type.footnote,
                        color = colors.labelSecondary,
                        modifier = Modifier.padding(horizontal = V5Spacing.marginPhone + V5Spacing.space4),
                    )
                }
            }
            V5ListGroup(header = stringResource(R.string.filter_scope), dividerInset = V5ListDefaults.dividerInsetText) {
                V5ListRow(
                    title = stringResource(R.string.filter_scope_global),
                    trailing = if (profileId == null) V5RowTrailing.Check else V5RowTrailing.None,
                    onClick = { profileId = null },
                )
                profiles.forEach { profile ->
                    V5ListRow(
                        title = profile.workspaceDisplayName(),
                        trailing = if (profileId == profile.id) V5RowTrailing.Check else V5RowTrailing.None,
                        onClick = { profileId = profile.id },
                    )
                }
            }
            if (invalid) {
                Text(
                    stringResource(R.string.filter_invalid_rule),
                    style = V5Theme.type.footnote,
                    color = colors.danger,
                    modifier = Modifier.padding(horizontal = V5Spacing.marginPhone + V5Spacing.space4),
                )
            }
            V5ListGroup {
                V5ListRow(
                    title = stringResource(if (rule == null) R.string.action_add else R.string.action_done),
                    titleColor = colors.accent,
                    enabled = FilterRuleSheetV5Rules.canSave(kind, requestHost, firstPartyHost, selector),
                    onClick = save,
                )
            }
        }
    }
}

/**
 * Which opening of a sheet this is: 0 until it first shows, then a new number each time it shows
 * (or shows for another [subject]). Keyed on it, a sheet's fields start fresh every time while it
 * keeps what it showed on its way out.
 */
@Composable
internal fun rememberFilterSheetOpening(visible: Boolean, subject: Any? = null): Int {
    val openings = remember { intArrayOf(0) }
    return remember(visible, subject) { if (visible) ++openings[0] else openings[0] }
}

/**
 * A one-line field for hosts, sites and selectors on a rounded card: no capital, no autocorrect,
 * the address keyboard, its [placeholder] shown while it is empty and read by TalkBack.
 */
@Composable
internal fun FilterTextField(
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    onDone: (() -> Unit)? = null,
    imeAction: ImeAction = if (onDone != null) ImeAction.Done else ImeAction.Next,
) {
    val colors = V5Theme.colors
    Box(
        Modifier
            .padding(horizontal = V5Spacing.marginPhone)
            .fillMaxWidth()
            .heightIn(min = V5Sizes.rowSingleLine)
            .clip(V5Theme.shapes.s)
            .background(colors.surface)
            .padding(horizontal = V5Spacing.space4),
        contentAlignment = Alignment.CenterStart,
    ) {
        // The field carries the name for TalkBack; this is only what the eye reads (СС-1).
        if (value.isEmpty()) Text(placeholder, style = V5Theme.type.body, color = colors.labelTertiary, modifier = Modifier.clearAndSetSemantics {})
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = V5Theme.type.body.copy(color = colors.label),
            cursorBrush = SolidColor(colors.accent),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                autoCorrectEnabled = false,
                keyboardType = KeyboardType.Uri,
                imeAction = imeAction,
            ),
            keyboardActions = KeyboardActions(
                onDone = { onDone?.invoke() },
                onGo = { onDone?.invoke() },
            ),
            modifier = modifier
                .fillMaxWidth()
                .v5FieldLabel(placeholder, empty = value.isEmpty()),
        )
    }
}

/** What the rule sheet offers, apart from drawing, for unit tests. */
internal object FilterRuleSheetV5Rules {
    val actions = listOf(CandyRuleAction.Block, CandyRuleAction.Allow, CandyRuleAction.Cosmetic)

    val requestKinds = listOf(CandyRuleKind.RequestHost, CandyRuleKind.HostPair)

    /** «Add» waits for every field the kind needs; the store still checks what is in them. */
    fun canSave(kind: CandyRuleKind, requestHost: String, firstPartyHost: String, selector: String): Boolean =
        when (kind) {
            CandyRuleKind.RequestHost -> requestHost.isNotBlank()
            CandyRuleKind.HostPair -> requestHost.isNotBlank() && firstPartyHost.isNotBlank()
            CandyRuleKind.CosmeticCss -> firstPartyHost.isNotBlank() && selector.isNotBlank()
        }
}
