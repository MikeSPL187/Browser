package dev.sk2andy.materialbrowser.ui.v5

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.browser.safety.BlockedSite
import dev.sk2andy.materialbrowser.shared.ui.icons.VolaIcons
import dev.sk2andy.materialbrowser.ui.DangerousSiteTestTags
import dev.sk2andy.materialbrowser.ui.theme.v5.SmoothCornerShape
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Elevation
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Motion
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Sizes
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Spacing
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Theme
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Tile
import dev.sk2andy.materialbrowser.ui.theme.v5.v5Elevation
import dev.sk2andy.materialbrowser.ui.theme.v5.v5Glow

/** Page, «Back to safety» and «Open anyway» keep the tags of [DangerousSiteTestTags]. */
object V5DangerousSiteTestTags {
    const val Why = "v5_danger_why"
    const val WhyText = "v5_danger_why_text"
    const val RealSite = "v5_danger_real_site"
}

/**
 * A site stopped before anything loaded (canvas screen «Danger», audit question 6): on the faint red
 * ground the red tile with its glow, the title on the left, the site's address in a chip with a red
 * dot, what such pages do, and a card saying where the check ran. «Back to safety» is the one filled
 * action; «Why Vola decided so» opens the reason in place (with the real site, for a look-alike);
 * «Open anyway» is small and underlined, and missing where the engine refuses the site.
 */
@Composable
fun V5DangerousSite(
    site: BlockedSite,
    onBackToSafety: () -> Unit,
    onOpenAnyway: () -> Unit,
    onOpenRealSite: () -> Unit,
    modifier: Modifier = Modifier,
    /** Previews show the reason open. */
    whyInitiallyOpen: Boolean = false,
) {
    val colors = V5Theme.colors
    val imitatedHost = site.imitatedHost
    var whyOpen by rememberSaveable(site.url) { mutableStateOf(whyInitiallyOpen) }
    Box(
        modifier
            .fillMaxSize()
            .testTag(DangerousSiteTestTags.Page)
            // The page under the warning must not take touches that miss the controls.
            .pointerInput(Unit) { awaitPointerEventScope { while (true) awaitPointerEvent() } }
            .background(colors.dangerSoft)
            .safeDrawingPadding(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            Modifier
                .widthIn(max = V5Sizes.stateContentMax)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = V5Spacing.space6)
                .padding(top = V5Spacing.space12, bottom = V5Spacing.space6),
        ) {
            DangerTile()
            Text(
                stringResource(if (imitatedHost != null) R.string.dangerous_site_title else R.string.dangerous_site_listed_title),
                style = V5Theme.type.largeTitle,
                color = colors.label,
                modifier = Modifier
                    .padding(top = V5Spacing.space6)
                    .semantics { heading() },
            )
            HostChip(site.host, Modifier.padding(top = V5Spacing.space3))
            Text(
                when {
                    imitatedHost != null -> stringResource(R.string.dangerous_site_message, site.host, imitatedHost)
                    site.reportedBySafeBrowsing -> stringResource(R.string.dangerous_site_safe_browsing_message, site.host)
                    else -> stringResource(R.string.dangerous_site_listed_message, site.host)
                },
                style = V5Theme.type.body,
                color = colors.labelSecondary,
                modifier = Modifier.padding(top = V5Spacing.space4),
            )
            CheckedCard(
                stringResource(
                    when {
                        imitatedHost != null -> R.string.v5_danger_lookalike_source
                        site.reportedBySafeBrowsing -> R.string.dangerous_site_safe_browsing_source
                        else -> R.string.dangerous_site_listed_local
                    },
                ),
                Modifier.padding(top = V5Spacing.space5),
            )
            V5StatePrimaryAction(
                V5StateAction(
                    label = stringResource(R.string.dangerous_site_back),
                    onClick = onBackToSafety,
                    testTag = DangerousSiteTestTags.Back,
                ),
                Modifier
                    .padding(top = V5Spacing.space6)
                    .v5Elevation(V5Elevation.e2, V5Theme.shapes.full),
            )
            V5StateQuietAction(
                V5StateAction(
                    label = stringResource(R.string.v5_danger_why),
                    onClick = { whyOpen = !whyOpen },
                    testTag = V5DangerousSiteTestTags.Why,
                ),
                Modifier.padding(top = V5Spacing.space1),
            )
            AnimatedVisibility(visible = whyOpen, enter = whyEnter(), exit = whyExit()) {
                Why(site, onOpenRealSite)
            }
            // The engine refuses a site Safe Browsing reported, so there is no way through.
            if (!site.reportedBySafeBrowsing) {
                OpenAnyway(onOpenAnyway)
            }
        }
    }
}

/** The warning's red tile and its glyph, glowing in its own color. */
@Composable
private fun DangerTile() {
    val shape = SmoothCornerShape(V5Sizes.dangerTileRadius)
    Box(
        Modifier
            .size(V5Sizes.dangerTile)
            .v5Glow(V5Tile.Red.color, shape)
            .clip(shape)
            .background(V5Tile.Red.color),
        contentAlignment = Alignment.Center,
    ) {
        Icon(VolaIcons.Dangerous, contentDescription = null, tint = V5Tile.Red.glyph, modifier = Modifier.size(V5Sizes.dangerTileIcon))
    }
}

/** The stopped site's address on a white chip, a red dot before it. */
@Composable
private fun HostChip(host: String, modifier: Modifier = Modifier) {
    val colors = V5Theme.colors
    val shape = V5Theme.shapes.full
    Row(
        modifier
            .heightIn(min = V5Sizes.dangerChip)
            .v5Elevation(V5Elevation.e1, shape)
            .clip(shape)
            .background(colors.surface)
            .padding(horizontal = V5Spacing.space3),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(V5Spacing.space2),
    ) {
        Box(
            Modifier
                .size(V5Sizes.dot)
                .clip(V5Theme.shapes.full)
                .background(colors.danger),
        )
        Text(
            host,
            style = V5Theme.type.callout.copy(fontWeight = FontWeight.SemiBold),
            color = colors.label,
            maxLines = 1,
            overflow = TextOverflow.MiddleEllipsis,
        )
    }
}

/** Where the check ran: always on the phone, and what, if anything, left it. */
@Composable
private fun CheckedCard(source: String, modifier: Modifier = Modifier) {
    val colors = V5Theme.colors
    val shape = V5Theme.shapes.m
    Row(
        modifier
            .fillMaxWidth()
            .v5Elevation(V5Elevation.e1, shape)
            .clip(shape)
            .background(colors.surface)
            .semantics(mergeDescendants = true) {}
            .padding(horizontal = V5Spacing.space4, vertical = V5Spacing.space3),
        horizontalArrangement = Arrangement.spacedBy(V5Spacing.space3),
    ) {
        Box(
            Modifier
                .size(V5Sizes.rowTile)
                .clip(V5Theme.shapes.xs)
                .background(V5Tile.Green.color),
            contentAlignment = Alignment.Center,
        ) {
            Icon(VolaIcons.Verified, contentDescription = null, tint = V5Tile.Green.glyph, modifier = Modifier.size(V5Sizes.iconSmall))
        }
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(R.string.v5_danger_checked),
                style = V5Theme.type.callout.copy(fontWeight = FontWeight.SemiBold),
                color = colors.label,
            )
            Text(source, style = V5Theme.type.footnote, color = colors.labelSecondary)
        }
    }
}

/** The reason in Vola's words; a look-alike also offers the site it imitates. */
@Composable
private fun Why(site: BlockedSite, onOpenRealSite: () -> Unit) {
    val colors = V5Theme.colors
    val imitatedHost = site.imitatedHost
    Column(Modifier.fillMaxWidth()) {
        Text(
            when {
                imitatedHost != null -> stringResource(R.string.v5_danger_why_lookalike, site.host, imitatedHost)
                site.reportedBySafeBrowsing -> stringResource(R.string.v5_danger_why_safe_browsing)
                else -> stringResource(R.string.v5_danger_why_listed, site.host)
            },
            style = V5Theme.type.callout,
            color = colors.labelSecondary,
            modifier = Modifier
                .padding(horizontal = V5Spacing.space1, vertical = V5Spacing.space2)
                .testTag(V5DangerousSiteTestTags.WhyText),
        )
        if (imitatedHost != null) {
            V5StateQuietAction(
                V5StateAction(
                    label = stringResource(R.string.dangerous_site_real, imitatedHost),
                    onClick = onOpenRealSite,
                    testTag = V5DangerousSiteTestTags.RealSite,
                ),
            )
        }
    }
}

/** «Open anyway»: small, quiet and underlined, so it is never the easy tap; its touch area stays full. */
@Composable
private fun OpenAnyway(onClick: () -> Unit) {
    val colors = V5Theme.colors
    Box(
        Modifier
            .fillMaxWidth()
            .heightIn(min = V5Sizes.touchTarget)
            .clip(V5Theme.shapes.full)
            .clickable(role = Role.Button, onClick = onClick)
            .testTag(DangerousSiteTestTags.OpenAnyway),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            stringResource(R.string.dangerous_site_open_anyway),
            style = V5Theme.type.footnote.copy(textDecoration = TextDecoration.Underline),
            color = colors.labelSecondary,
        )
    }
}

private fun whyEnter(): EnterTransition = if (V5Motion.animationsEnabled) {
    expandVertically(V5Motion.effects.spec()) + fadeIn(V5Motion.effects.spec())
} else {
    fadeIn(V5Motion.reducedFade())
}

private fun whyExit(): ExitTransition = if (V5Motion.animationsEnabled) {
    shrinkVertically(V5Motion.effects.spec()) + fadeOut(V5Motion.effects.spec())
} else {
    fadeOut(V5Motion.reducedFade())
}
