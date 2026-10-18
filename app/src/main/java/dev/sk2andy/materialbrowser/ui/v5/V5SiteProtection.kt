package dev.sk2andy.materialbrowser.ui.v5

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.shared.ui.icons.VolaIcons
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Motion
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Sizes
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Spacing
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Theme
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Tile
import dev.sk2andy.materialbrowser.ui.v5.V5SiteProtectionRules.Page

object V5SiteProtectionTestTags {
    const val Toggle = "v5_site_toggle"
    const val Broken = "v5_site_broken"
    const val Details = "v5_site_details"
    const val SiteInfo = "v5_site_info"
    const val ThirdParty = "v5_site_third_party"
    const val TurnOff = "v5_site_turn_off"
    const val Report = "v5_site_report"
    const val Reset = "v5_site_reset"
    const val Back = "v5_site_back"
}

/** The site's protection as the panel shows it; a null switch is one the engine does not offer here. */
@Immutable
data class V5SiteProtectionState(
    val host: String,
    val protectionOn: Boolean,
    val strict: Boolean,
    /** Everything blocked on the page, counted once. */
    val blocked: Int,
    /** A private tab: changes last until it closes. */
    val privateTab: Boolean,
    val thirdPartyCookiesAllowed: Boolean?,
    val cookieBannersHidden: Boolean?,
    val deAmp: Boolean?,
    val fingerprintingProtection: Boolean?,
    /** The browser-wide HTTPS-only mode as a label, or null where the engine has none. */
    val httpsOnly: String?,
)

class V5SiteProtectionActions(
    val onProtectionChange: (Boolean) -> Unit,
    val onStrictChange: (Boolean) -> Unit,
    val onThirdPartyCookiesChange: (Boolean) -> Unit,
    val onCookieBannersChange: (Boolean) -> Unit,
    val onDeAmpChange: (Boolean) -> Unit,
    val onFingerprintingChange: (Boolean) -> Unit,
    val onHttpsOnly: () -> Unit,
    val onCopyReport: () -> Unit,
    val onReset: () -> Unit,
    /** The full site sheet: permissions, certificate, page fixes. */
    val onSiteInfo: () -> Unit,
)

/**
 * The v5 site protection panel (V17, canvas screens «f1» and «f2», system-v5.md section 12), from
 * the shield in the bar. The simple view is the status, one number and one switch, «Protection for
 * this site»; «Site not working right?» offers the gentle step first (third-party cookies for this
 * site, protection stays) and turning protection off last. «Details» is the second layer: Standard
 * or Strict, cookie banners, the original instead of AMP, fingerprinting, HTTPS-only, the breakage
 * report and reset. Everything else about the site stays in the full site sheet.
 */
@Composable
fun V5SiteProtection(
    visible: Boolean,
    state: V5SiteProtectionState,
    actions: V5SiteProtectionActions,
    onDismiss: () -> Unit,
) {
    var page by rememberSaveable(visible) { mutableStateOf(Page.Main) }
    val title = stringResource(if (state.protectionOn) R.string.v5_site_on else R.string.v5_site_off)
    V5WindowSheet(visible = visible, onDismiss = onDismiss, title = title) {
        // Back steps out of a page before it closes the sheet.
        BackHandler(enabled = page != Page.Main) { page = Page.Main }
        // The pages cross-fade; without system animations the fade is the whole of the motion.
        val animated = V5Motion.animationsEnabled
        AnimatedContent(
            targetState = page,
            transitionSpec = {
                if (animated) {
                    fadeIn(V5Motion.effects.spec()) togetherWith fadeOut(V5Motion.effects.spec())
                } else {
                    fadeIn(V5Motion.reducedFade()) togetherWith fadeOut(V5Motion.reducedFade())
                }
            },
            label = "site protection page",
        ) { shown ->
            Column {
                when (shown) {
                    Page.Main -> V5SheetHeader(
                        title = title,
                        subtitle = state.host,
                        onClose = onDismiss,
                        leading = { ShieldBadge(state.protectionOn) },
                    )
                    Page.Troubleshoot -> V5SheetHeader(
                        title = stringResource(R.string.v5_site_broken),
                        subtitle = state.host,
                        onClose = onDismiss,
                        leading = { BackButton { page = Page.Main } },
                    )
                    Page.Details -> V5SheetHeader(
                        title = stringResource(R.string.v5_site_details),
                        subtitle = pluralStringResource(R.plurals.v5_site_details_subtitle, state.blocked, state.host, state.blocked),
                        onClose = onDismiss,
                        leading = { BackButton { page = Page.Main } },
                    )
                }
                Column(
                    Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(top = V5Spacing.space3, bottom = V5Spacing.space4),
                    verticalArrangement = Arrangement.spacedBy(V5Spacing.space4),
                ) {
                    when (shown) {
                        Page.Main -> MainPage(state, actions, onOpen = { page = it })
                        Page.Troubleshoot -> TroubleshootPage(state, actions, onTurnedOff = { page = Page.Main })
                        Page.Details -> DetailsPage(state, actions)
                    }
                }
            }
        }
    }
}

@Composable
private fun MainPage(state: V5SiteProtectionState, actions: V5SiteProtectionActions, onOpen: (Page) -> Unit) {
    V5ListGroup {
        if (state.protectionOn) {
            Box(Modifier.fillMaxWidth().padding(horizontal = V5Spacing.space4, vertical = V5Spacing.space3)) {
                V5ReportTotal(state.blocked)
            }
        } else {
            Text(
                stringResource(R.string.v5_site_off_note),
                style = V5Theme.type.callout,
                color = V5Theme.colors.labelSecondary,
                modifier = Modifier.fillMaxWidth().padding(horizontal = V5Spacing.space4, vertical = V5Spacing.space3),
            )
        }
        V5ListRow(
            title = stringResource(R.string.v5_site_toggle),
            subtitle = if (state.privateTab) stringResource(R.string.v5_site_toggle_private) else null,
            trailing = V5RowTrailing.Toggle(state.protectionOn, actions.onProtectionChange, V5ToggleTint.Success),
            modifier = Modifier.testTag(V5SiteProtectionTestTags.Toggle),
        )
    }
    V5ListGroup(dividerInset = V5Spacing.space4) {
        if (state.protectionOn) {
            V5ListRow(
                title = stringResource(R.string.v5_site_broken),
                subtitle = stringResource(R.string.v5_site_broken_hint),
                trailing = V5RowTrailing.Chevron,
                onClick = { onOpen(Page.Troubleshoot) },
                modifier = Modifier.testTag(V5SiteProtectionTestTags.Broken),
            )
        }
        V5ListRow(
            title = stringResource(R.string.v5_site_details),
            titleColor = V5Theme.colors.accent,
            trailing = V5RowTrailing.Chevron,
            onClick = { onOpen(Page.Details) },
            modifier = Modifier.testTag(V5SiteProtectionTestTags.Details),
        )
        V5ListRow(
            title = stringResource(R.string.v5_site_info),
            trailing = V5RowTrailing.Chevron,
            onClick = actions.onSiteInfo,
            modifier = Modifier.testTag(V5SiteProtectionTestTags.SiteInfo),
        )
    }
}

/** The gentle step first, the breakage report, and turning protection off last. */
@Composable
private fun TroubleshootPage(state: V5SiteProtectionState, actions: V5SiteProtectionActions, onTurnedOff: () -> Unit) {
    Text(
        stringResource(R.string.v5_site_broken_body),
        style = V5Theme.type.callout,
        color = V5Theme.colors.labelSecondary,
        modifier = Modifier.padding(horizontal = V5Spacing.marginPhone + V5Spacing.space1),
    )
    state.thirdPartyCookiesAllowed?.let { allowed ->
        V5ListGroup {
            V5ListRow(
                title = stringResource(R.string.v5_site_third_party),
                subtitle = stringResource(
                    if (state.strict) R.string.v5_site_third_party_strict else R.string.v5_site_third_party_hint,
                ),
                icon = V5RowIcon(VolaIcons.Key, V5Tile.Orange),
                trailing = V5RowTrailing.Toggle(allowed && !state.strict, actions.onThirdPartyCookiesChange),
                enabled = !state.strict,
                modifier = Modifier.testTag(V5SiteProtectionTestTags.ThirdParty),
            )
        }
    }
    V5ListGroup {
        ReportRow(actions.onCopyReport)
        V5ListRow(
            title = stringResource(R.string.v5_site_turn_off),
            titleColor = V5Theme.colors.danger,
            onClick = {
                actions.onProtectionChange(false)
                onTurnedOff()
            },
            modifier = Modifier.testTag(V5SiteProtectionTestTags.TurnOff),
        )
    }
}

/** The second layer: the level, the site's switches, HTTPS-only, the report and reset. */
@Composable
private fun DetailsPage(state: V5SiteProtectionState, actions: V5SiteProtectionActions) {
    Column(
        Modifier.padding(horizontal = V5Spacing.marginPhone),
        verticalArrangement = Arrangement.spacedBy(V5Spacing.space2),
    ) {
        V5Segmented(
            options = listOf(stringResource(R.string.v5_site_standard), stringResource(R.string.v5_site_strict)),
            selectedIndex = if (state.strict) 1 else 0,
            onSelect = { actions.onStrictChange(it == 1) },
        )
        Text(
            stringResource(if (state.strict) R.string.v5_site_strict_hint else R.string.v5_site_standard_hint),
            style = V5Theme.type.footnote,
            color = V5Theme.colors.labelSecondary,
            modifier = Modifier.padding(horizontal = V5Spacing.space1),
        )
    }
    V5ListGroup {
        state.cookieBannersHidden?.let { hidden ->
            V5ListRow(
                title = stringResource(R.string.v5_site_banners),
                subtitle = stringResource(R.string.v5_site_banners_hint),
                icon = V5RowIcon(VolaIcons.VisibilityOff, V5Tile.Orange),
                trailing = V5RowTrailing.Toggle(hidden, actions.onCookieBannersChange),
            )
        }
        state.deAmp?.let { deAmp ->
            V5ListRow(
                title = stringResource(R.string.v5_site_amp),
                subtitle = stringResource(R.string.v5_site_amp_hint),
                icon = V5RowIcon(VolaIcons.OpenInNew, V5Tile.Blue),
                trailing = V5RowTrailing.Toggle(deAmp, actions.onDeAmpChange),
            )
        }
        state.fingerprintingProtection?.let { protected ->
            V5ListRow(
                title = stringResource(R.string.v5_site_fingerprint),
                subtitle = stringResource(R.string.v5_site_fingerprint_hint),
                icon = V5RowIcon(VolaIcons.Fingerprint, V5Tile.Purple),
                trailing = V5RowTrailing.Toggle(protected, actions.onFingerprintingChange),
            )
        }
        state.httpsOnly?.let { mode ->
            V5ListRow(
                title = stringResource(R.string.v5_site_https),
                icon = V5RowIcon(VolaIcons.Lock, V5Tile.Green),
                value = mode,
                trailing = V5RowTrailing.Chevron,
                onClick = actions.onHttpsOnly,
            )
        }
    }
    V5ListGroup {
        ReportRow(actions.onCopyReport)
        V5ListRow(
            title = stringResource(R.string.v5_site_reset),
            titleColor = V5Theme.colors.danger,
            onClick = actions.onReset,
            modifier = Modifier.testTag(V5SiteProtectionTestTags.Reset),
        )
    }
}

@Composable
private fun ReportRow(onCopy: () -> Unit) {
    V5ListRow(
        title = stringResource(R.string.v5_site_report),
        subtitle = stringResource(R.string.v5_site_report_hint),
        icon = V5RowIcon(VolaIcons.ContentCopy),
        onClick = onCopy,
        modifier = Modifier.testTag(V5SiteProtectionTestTags.Report),
    )
}

/** The big shield: green while the site is protected, gray when it is not. */
@Composable
private fun ShieldBadge(on: Boolean) {
    val colors = V5Theme.colors
    Box(
        Modifier
            .size(V5Sizes.rowTile + V5Spacing.space2)
            .clip(V5Theme.shapes.full)
            .background(if (on) colors.success else colors.fill),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            VolaIcons.ShieldLock,
            contentDescription = null,
            tint = if (on) V5Tile.Green.glyph else colors.labelSecondary,
            modifier = Modifier.size(V5Sizes.iconSmall),
        )
    }
}

@Composable
private fun BackButton(onBack: () -> Unit) {
    IconButton(
        onClick = onBack,
        modifier = Modifier.heightIn(min = V5Sizes.touchTarget).testTag(V5SiteProtectionTestTags.Back),
    ) {
        Icon(VolaIcons.ArrowBack, contentDescription = stringResource(R.string.v5_site_back), tint = V5Theme.colors.label)
    }
}
