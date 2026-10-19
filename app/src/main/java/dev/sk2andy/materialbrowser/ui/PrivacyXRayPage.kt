package dev.sk2andy.materialbrowser.ui

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.blocking.CandyRuleAction
import dev.sk2andy.materialbrowser.blocking.PrivacyDomainSummary
import dev.sk2andy.materialbrowser.blocking.PrivacyPartyRelation
import dev.sk2andy.materialbrowser.blocking.PrivacyRequestCategory
import dev.sk2andy.materialbrowser.blocking.PrivacyRuleDecisionAction
import dev.sk2andy.materialbrowser.blocking.PrivacyXRaySnapshot
import dev.sk2andy.materialbrowser.blocking.SiteProtectionState
import dev.sk2andy.materialbrowser.data.AppearanceSettings
import dev.sk2andy.materialbrowser.data.BrowserAppearanceMode
import dev.sk2andy.materialbrowser.data.ProtectionWeek
import dev.sk2andy.materialbrowser.ui.theme.MaterialBrowserTheme
import dev.sk2andy.materialbrowser.ui.theme.VolaMotion
import dev.sk2andy.materialbrowser.ui.theme.VolaPreviews
import dev.sk2andy.materialbrowser.ui.theme.VolaPrivacyXRay
import dev.sk2andy.materialbrowser.ui.theme.VolaSiteInfo
import dev.sk2andy.materialbrowser.ui.theme.VolaSpacing
import dev.sk2andy.materialbrowser.ui.theme.VolaTypeScale
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Motion
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Sizes
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Spacing
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Theme
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Tile
import dev.sk2andy.materialbrowser.ui.theme.v5.VolaV5Theme

/**
 * Privacy X-Ray, the tracker page of Site info (board W-PrivacyXRay): how many requests were
 * blocked on this site, by category, to whom the page tried to send data, and the week.
 * A tracker row opens its rules; a site that signs in through a third party can be blocked again.
 */
@Composable
internal fun PrivacyXRayContent(
    snapshot: PrivacyXRaySnapshot,
    host: String?,
    siteState: SiteProtectionState,
    modifier: Modifier = Modifier,
    week: ProtectionWeek? = null,
    onRevokeThirdPartyCookieCompatibility: () -> Unit = {},
    onRuleAction: (domain: String, action: CandyRuleAction, siteScoped: Boolean) -> Unit =
        { _, _, _ -> },
    onOpenStudio: (ruleId: String?) -> Unit = {},
    onOpenProtectionSettings: (() -> Unit)? = null,
) {
    var showAll by rememberSaveable(host) { mutableStateOf(false) }
    val colors = categoryColors()
    val skin = xRaySkin()
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(VolaPrivacyXRay.sectionGap),
    ) {
        XRayHero(snapshot = snapshot, host = host, paused = siteState.isPaused)
        if (snapshot.totalBlocked > 0) CategoryBreakdown(snapshot, colors)
        if (snapshot.domains.isNotEmpty()) {
            XRaySectionLabel(stringResource(R.string.privacy_xray_destinations))
            val visible = if (showAll) {
                snapshot.domains
            } else {
                snapshot.domains.take(VolaPrivacyXRay.COLLAPSED_DOMAINS)
            }
            SiteInfoCard {
                Column(modifier = Modifier.testTag(PrivacyXRayTestTags.Domains)) {
                    visible.forEachIndexed { index, domain ->
                        if (index > 0) SiteInfoDivider()
                        TrackerRow(
                            domain = domain,
                            color = colors.getValue(domain.category),
                            onRuleAction = onRuleAction,
                            onOpenStudio = onOpenStudio,
                        )
                    }
                    if (snapshot.domains.size > VolaPrivacyXRay.COLLAPSED_DOMAINS) {
                        SiteInfoDivider()
                        XRayTextButton(
                            text = if (showAll) {
                                stringResource(R.string.privacy_xray_hide_details)
                            } else {
                                stringResource(
                                    R.string.privacy_xray_show_all,
                                    snapshot.domains.size,
                                )
                            },
                            onClick = { showAll = !showAll },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag(PrivacyXRayTestTags.ToggleDetails),
                        )
                    }
                }
            }
            if (snapshot.omittedDomainRequests > 0) {
                Text(
                    stringResource(
                        R.string.privacy_xray_omitted_domains,
                        snapshot.omittedDomainRequests,
                    ),
                    modifier = Modifier.padding(horizontal = VolaSiteInfo.labelPadding),
                    style = skin.caption,
                    color = skin.secondary,
                )
            }
        }
        if (siteState.thirdPartyLoginAllowed || siteState.captchaCompatibilityAllowed) {
            SiteInfoCard {
                XRayLinkRow(
                    title = stringResource(R.string.privacy_xray_third_party_cookies_allowed),
                    action = stringResource(R.string.federated_login_revoke),
                    onClick = onRevokeThirdPartyCookieCompatibility,
                )
            }
        }
        SiteInfoCard {
            week?.takeIf { it.total > 0 }?.let { WeekRow(it) }
            XRayLinkRow(
                title = stringResource(R.string.filter_studio_title),
                onClick = { onOpenStudio(null) },
            )
            onOpenProtectionSettings?.let { open ->
                SiteInfoDivider()
                XRayLinkRow(
                    title = stringResource(R.string.settings_protection_data_title),
                    onClick = open,
                )
            }
        }
    }
}

/** «14 requests» with the number in display type, as on the board. */
internal object XRayHeroRules {
    fun countRange(text: String, count: Int): IntRange? {
        val number = count.toString()
        val start = text.indexOf(number)
        return if (start < 0) null else start until start + number.length
    }

    fun emphasizeCount(
        text: String,
        count: Int,
        style: SpanStyle = VolaTypeScale.display.toSpanStyle(),
    ): AnnotatedString = buildAnnotatedString {
        append(text)
        countRange(text, count)?.let { range ->
            addStyle(style, range.first, range.last + 1)
        }
    }
}

@Composable
private fun categoryColors(): Map<PrivacyRequestCategory, Color> {
    if (LocalSiteInfoV5.current) {
        val v5 = V5Theme.colors
        return remember(v5) {
            mapOf(
                PrivacyRequestCategory.Advertising to v5.accent,
                PrivacyRequestCategory.Analytics to V5Tile.Teal.color,
                PrivacyRequestCategory.Social to V5Tile.Purple.color,
                PrivacyRequestCategory.Other to v5.labelTertiary,
            )
        }
    }
    val scheme = MaterialTheme.colorScheme
    return remember(scheme) {
        mapOf(
            PrivacyRequestCategory.Advertising to scheme.primary,
            PrivacyRequestCategory.Analytics to scheme.tertiary,
            PrivacyRequestCategory.Social to scheme.secondary,
            PrivacyRequestCategory.Other to scheme.outline,
        )
    }
}

/**
 * The type and colors of the X-Ray page: Material 3 in v4, the v5 scale and palette under ui.v5
 * (V28, B8b), so each piece below is written once. [primary] Unspecified keeps v4's content color.
 */
@Immutable
private data class XRaySkin(
    val v5: Boolean,
    val heroTitle: TextStyle,
    /** The blocked count inside the hero's first line. */
    val countStyle: TextStyle,
    val heroBody: TextStyle,
    val heroContainer: Color,
    val heroContent: Color,
    val body: TextStyle,
    val caption: TextStyle,
    val value: TextStyle,
    val label: TextStyle,
    val primary: Color,
    val secondary: Color,
    val link: Color,
)

@Composable
private fun xRaySkin(): XRaySkin {
    if (LocalSiteInfoV5.current) {
        val colors = V5Theme.colors
        val type = V5Theme.type
        return XRaySkin(
            v5 = true,
            heroTitle = type.headline,
            countStyle = type.largeTitle,
            heroBody = type.callout,
            heroContainer = colors.accentSoft,
            heroContent = colors.label,
            body = type.body,
            caption = type.footnote,
            value = type.callout,
            label = type.footnote,
            primary = colors.label,
            secondary = colors.labelSecondary,
            link = colors.accent,
        )
    }
    val colors = MaterialTheme.colorScheme
    val type = MaterialTheme.typography
    return XRaySkin(
        v5 = false,
        heroTitle = type.titleMedium,
        countStyle = VolaTypeScale.display,
        heroBody = type.bodyMedium,
        heroContainer = colors.primaryContainer,
        heroContent = colors.onPrimaryContainer,
        body = type.bodyLarge,
        caption = type.bodySmall,
        value = type.labelLarge,
        label = type.titleSmall,
        primary = Color.Unspecified,
        secondary = colors.onSurfaceVariant,
        link = colors.primary,
    )
}

@Composable
private fun XRayHero(snapshot: PrivacyXRaySnapshot, host: String?, paused: Boolean) {
    val skin = xRaySkin()
    val total = snapshot.totalBlocked
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(PrivacyXRayTestTags.Total),
        shape = if (skin.v5) V5Theme.shapes.m else VolaPrivacyXRay.heroShape,
        color = skin.heroContainer,
        contentColor = skin.heroContent,
    ) {
        Column(
            modifier = Modifier
                .semantics(mergeDescendants = true) {}
                .padding(if (skin.v5) V5Spacing.space4 else VolaPrivacyXRay.heroPadding),
            verticalArrangement = Arrangement.spacedBy(VolaSpacing.x1),
        ) {
            if (total > 0) {
                val requests = pluralStringResource(R.plurals.privacy_xray_requests, total, total)
                Text(
                    text = remember(requests, total, skin.v5) {
                        if (skin.v5) {
                            XRayHeroRules.emphasizeCount(requests, total, skin.countStyle.toSpanStyle())
                        } else {
                            XRayHeroRules.emphasizeCount(requests, total)
                        }
                    },
                    style = skin.heroTitle,
                )
                Text(
                    text = if (host.isNullOrEmpty()) {
                        stringResource(R.string.privacy_xray_blocked_here)
                    } else {
                        stringResource(R.string.privacy_xray_blocked_on, host)
                    },
                    modifier = Modifier.semantics { heading() },
                    style = skin.heroTitle,
                )
            } else {
                Text(
                    text = stringResource(R.string.site_info_nothing_blocked),
                    modifier = Modifier.semantics { heading() },
                    style = skin.heroTitle,
                )
            }
            Text(
                text = stringResource(
                    when {
                        paused -> R.string.privacy_xray_paused
                        total > 0 -> R.string.privacy_xray_benefit
                        else -> R.string.privacy_xray_waiting
                    },
                ),
                style = skin.heroBody,
            )
        }
    }
}

@Composable
private fun CategoryBreakdown(
    snapshot: PrivacyXRaySnapshot,
    colors: Map<PrivacyRequestCategory, Color>,
) {
    val categories = PrivacyRequestCategory.entries
        .filter { (snapshot.categoryCounts[it] ?: 0) > 0 }
    if (categories.isEmpty()) return
    val skin = xRaySkin()
    Column(verticalArrangement = Arrangement.spacedBy(VolaPrivacyXRay.legendGap)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(VolaPrivacyXRay.categoryBarHeight)
                .clip(CircleShape),
            horizontalArrangement = Arrangement.spacedBy(VolaPrivacyXRay.categoryGap),
        ) {
            categories.forEach { category ->
                Box(
                    Modifier
                        .weight(snapshot.categoryCounts.getValue(category).toFloat())
                        .height(VolaPrivacyXRay.categoryBarHeight)
                        .background(colors.getValue(category)),
                )
            }
        }
        categories.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(VolaPrivacyXRay.sectionGap)) {
                pair.forEach { category ->
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .semantics(mergeDescendants = true) {},
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(VolaPrivacyXRay.legendGap),
                    ) {
                        Box(
                            Modifier
                                .size(VolaPrivacyXRay.legendDotSize)
                                .background(colors.getValue(category), CircleShape),
                        )
                        Text(
                            category.label(),
                            modifier = Modifier.weight(1f),
                            style = skin.heroBody,
                            color = skin.primary,
                        )
                        Text(
                            snapshot.categoryCounts.getValue(category).toString(),
                            style = skin.value,
                            color = skin.secondary,
                        )
                    }
                }
                if (pair.size == 1) Box(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun TrackerRow(
    domain: PrivacyDomainSummary,
    color: Color,
    onRuleAction: (domain: String, action: CandyRuleAction, siteScoped: Boolean) -> Unit,
    onOpenStudio: (ruleId: String?) -> Unit,
) {
    var actionsVisible by rememberSaveable(domain.host) { mutableStateOf(false) }
    val skin = xRaySkin()
    val total = domain.blockedCount + domain.allowedCount
    val party = domain.partyRelation.label()
    val description = stringResource(
        R.string.privacy_domain_cd,
        domain.host,
        domain.blockedCount,
        domain.allowedCount,
        party,
    )
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = VolaSiteInfo.rowMinHeight)
                .clickable(role = Role.Button) { actionsVisible = !actionsVisible }
                .semantics { contentDescription = description }
                .padding(
                    horizontal = VolaSiteInfo.rowHorizontalPadding,
                    vertical = VolaSiteInfo.rowVerticalPadding,
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(VolaSiteInfo.rowGap),
        ) {
            Icon(
                painter = painterResource(
                    if (domain.blockedCount > 0) {
                        R.drawable.ic_symbol_block
                    } else {
                        R.drawable.ic_symbol_check
                    },
                ),
                contentDescription = null,
                modifier = Modifier.size(VolaSiteInfo.rowIconSize),
                tint = color,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    domain.host,
                    style = skin.body,
                    color = skin.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "${domain.category.label()} · $party",
                    style = skin.caption,
                    color = skin.secondary,
                )
            }
            Text(
                stringResource(R.string.privacy_xray_times, total),
                style = skin.value,
                color = skin.secondary,
            )
        }
        AnimatedVisibility(
            visible = actionsVisible,
            // Without system animations the actions only fade in (plan-v5.md, section 5).
            enter = if (V5Motion.animationsEnabled) {
                expandVertically(VolaMotion.standard()) + fadeIn(VolaMotion.effects())
            } else {
                fadeIn(V5Motion.reducedFade())
            },
            exit = if (V5Motion.animationsEnabled) {
                shrinkVertically(VolaMotion.standard()) + fadeOut(VolaMotion.effects())
            } else {
                fadeOut(V5Motion.reducedFade())
            },
        ) {
            Column(
                modifier = Modifier.padding(
                    start = VolaSiteInfo.rowHorizontalPadding,
                    end = VolaSiteInfo.rowHorizontalPadding,
                    bottom = VolaSiteInfo.rowVerticalPadding,
                ),
                verticalArrangement = Arrangement.spacedBy(VolaSpacing.x1),
            ) {
                domain.ruleDecision?.let { decision ->
                    XRayTextButton(
                        text = stringResource(
                            R.string.filter_deciding_rule_action,
                            if (decision.action == PrivacyRuleDecisionAction.Block) {
                                stringResource(R.string.filter_block)
                            } else {
                                stringResource(R.string.filter_allow)
                            },
                            decision.label,
                        ),
                        onClick = { onOpenStudio(decision.ruleId) },
                    )
                }
                RuleButtons(
                    everywhere = R.string.filter_block_everywhere,
                    onSite = R.string.filter_block_on_site,
                    onClick = { scoped -> onRuleAction(domain.host, CandyRuleAction.Block, scoped) },
                )
                RuleButtons(
                    everywhere = R.string.filter_allow_everywhere,
                    onSite = R.string.filter_allow_on_site,
                    onClick = { scoped -> onRuleAction(domain.host, CandyRuleAction.Allow, scoped) },
                )
            }
        }
    }
}

/** A rule for the tracker everywhere or on this site only. */
@Composable
private fun RuleButtons(
    @StringRes everywhere: Int,
    @StringRes onSite: Int,
    onClick: (siteScoped: Boolean) -> Unit,
) {
    val v5 = LocalSiteInfoV5.current
    Row(horizontalArrangement = Arrangement.spacedBy(VolaSpacing.x2)) {
        listOf(everywhere to false, onSite to true).forEach { (label, siteScoped) ->
            if (v5) {
                XRayCapsule(stringResource(label), { onClick(siteScoped) }, Modifier.weight(1f))
            } else {
                OutlinedButton(onClick = { onClick(siteScoped) }, modifier = Modifier.weight(1f)) {
                    Text(stringResource(label), maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

/** A quiet action in the accent: a text button in v4, accent text on a full-width target in v5. */
@Composable
private fun XRayTextButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    if (!LocalSiteInfoV5.current) {
        TextButton(onClick = onClick, modifier = modifier) {
            Text(text, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        return
    }
    Box(
        modifier = modifier
            .heightIn(min = V5Sizes.touchTarget)
            .clip(V5Theme.shapes.full)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = V5Spacing.space4, vertical = V5Spacing.space2),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = V5Theme.type.body,
            color = V5Theme.colors.accent,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** A v5 rule action: accent text on a filled capsule, as the secondary buttons of the menu. */
@Composable
private fun XRayCapsule(text: String, onClick: () -> Unit, modifier: Modifier) {
    val colors = V5Theme.colors
    Box(
        modifier = modifier
            .heightIn(min = V5Sizes.touchTarget)
            .clip(V5Theme.shapes.full)
            .background(colors.fill)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = V5Spacing.space3, vertical = V5Spacing.space2),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = V5Theme.type.callout,
            color = colors.accent,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun WeekRow(week: ProtectionWeek) {
    val locale = LocalConfiguration.current.locales[0]
    val skin = xRaySkin()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {}
            .padding(
                horizontal = VolaSiteInfo.rowHorizontalPadding,
                vertical = VolaSiteInfo.rowVerticalPadding,
            ),
    ) {
        Text(
            pluralStringResource(
                R.plurals.protection_card_title,
                week.total,
                formatCount(week.total, locale),
            ),
            style = skin.body,
            color = skin.primary,
        )
        Text(
            pluralStringResource(R.plurals.protection_card_sites, week.siteCount, week.siteCount),
            style = skin.caption,
            color = skin.secondary,
        )
    }
    SiteInfoDivider()
}

@Composable
private fun XRayLinkRow(title: String, onClick: () -> Unit, action: String? = null) {
    val skin = xRaySkin()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = VolaSiteInfo.rowMinHeight)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(
                horizontal = VolaSiteInfo.rowHorizontalPadding,
                vertical = VolaSiteInfo.rowVerticalPadding,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = skin.body, color = skin.primary)
            action?.let {
                Text(it, style = skin.caption, color = skin.link)
            }
        }
        if (action == null) {
            Icon(
                painter = painterResource(R.drawable.ic_symbol_chevron_right),
                contentDescription = null,
                tint = if (skin.v5) V5Theme.colors.labelTertiary else skin.secondary,
            )
        }
    }
}

@Composable
private fun XRaySectionLabel(text: String) {
    val skin = xRaySkin()
    Text(
        text = text,
        modifier = Modifier
            .padding(start = VolaSiteInfo.labelPadding)
            .semantics { heading() },
        style = skin.label,
        color = skin.secondary,
    )
}

@Composable
private fun PrivacyRequestCategory.label(): String = stringResource(
    when (this) {
        PrivacyRequestCategory.Advertising -> R.string.privacy_category_advertising
        PrivacyRequestCategory.Analytics -> R.string.privacy_category_analytics
        PrivacyRequestCategory.Social -> R.string.privacy_category_social
        PrivacyRequestCategory.Other -> R.string.privacy_category_other
    },
)

@Composable
private fun PrivacyPartyRelation.label(): String = stringResource(
    when (this) {
        PrivacyPartyRelation.FirstParty -> R.string.privacy_party_first
        PrivacyPartyRelation.ThirdParty -> R.string.privacy_party_third
        PrivacyPartyRelation.Unknown -> R.string.privacy_party_unknown
    },
)

@VolaPreviews
@Composable
private fun PrivacyXRayContentPreview() {
    MaterialBrowserTheme(
        settings = AppearanceSettings(appearanceMode = BrowserAppearanceMode.System),
    ) {
        Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
            PrivacyXRaySample(Modifier.padding(VolaSiteInfo.sidePadding))
        }
    }
}

/** V28 B8b: the same page under v5, in both themes. */
@Preview(name = "X-Ray v5 · light", heightDp = 900)
@Composable
private fun PrivacyXRayContentV5LightPreview() = PrivacyXRayV5Preview(dark = false)

@Preview(name = "X-Ray v5 · dark", heightDp = 900)
@Composable
private fun PrivacyXRayContentV5DarkPreview() = PrivacyXRayV5Preview(dark = true)

@Composable
private fun PrivacyXRayV5Preview(dark: Boolean) {
    VolaV5Theme(dark = dark) {
        CompositionLocalProvider(LocalSiteInfoV5 provides true) {
            Box(Modifier.background(V5Theme.colors.background)) {
                PrivacyXRaySample(Modifier.padding(V5Spacing.marginPhone))
            }
        }
    }
}

@Composable
private fun PrivacyXRaySample(modifier: Modifier) {
    PrivacyXRayContent(
        snapshot = PrivacyXRaySnapshot(
            totalBlocked = 14,
            categoryCounts = mapOf(
                PrivacyRequestCategory.Advertising to 6,
                PrivacyRequestCategory.Analytics to 5,
                PrivacyRequestCategory.Social to 2,
                PrivacyRequestCategory.Other to 1,
            ),
            domains = listOf(
                PrivacyDomainSummary(
                    "ads.example-network.com", 4,
                    category = PrivacyRequestCategory.Advertising,
                    partyRelation = PrivacyPartyRelation.ThirdParty,
                ),
                PrivacyDomainSummary(
                    "metrics.example.net", 3,
                    category = PrivacyRequestCategory.Analytics,
                    partyRelation = PrivacyPartyRelation.ThirdParty,
                ),
            ),
        ),
        host = "north-guide.ru",
        siteState = SiteProtectionState(host = "north-guide.ru"),
        week = ProtectionWeek(
            total = 1284,
            siteCount = 96,
            daily = List(7) { 0 },
            topSites = emptyList(),
            firstEpochDay = 0L,
            lastEpochDay = 6L,
        ),
        onOpenProtectionSettings = {},
        modifier = modifier,
    )
}
