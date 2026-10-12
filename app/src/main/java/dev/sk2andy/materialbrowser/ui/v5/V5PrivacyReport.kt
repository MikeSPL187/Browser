package dev.sk2andy.materialbrowser.ui.v5

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.data.ProtectionSummary
import dev.sk2andy.materialbrowser.shared.ui.icons.VolaIcons
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Sizes
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Spacing
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Theme
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Tile
import java.text.NumberFormat

object V5PrivacyReportTestTags {
    const val Total = "v5_report_total"
    const val Chart = "v5_report_chart"
    const val TopTracker = "v5_report_top_tracker"
    const val BusiestSite = "v5_report_busiest_site"
    const val HideCard = "v5_report_hide_card"
    const val Clear = "v5_report_clear"
}

/**
 * The v5 privacy report (V18, canvas screen «л1», system-v5.md section 12) in a sheet over the
 * whole window: 7 or 30 days, the blocks of the period with a bar per day, the most frequent
 * tracker and the site with the most trackers. Everything stays on the device; «Clear report data»
 * asks first. [onHideCard] (from the new tab's card) also offers to hide that card.
 */
@Composable
fun V5PrivacyReport(
    visible: Boolean,
    summary: ProtectionSummary,
    windowDays: Int,
    onWindowChange: (Int) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit,
    onHideCard: (() -> Unit)? = null,
) {
    val title = stringResource(R.string.v5_report_title)
    var confirmClear by rememberSaveable { mutableStateOf(false) }
    V5WindowSheet(visible = visible, onDismiss = onDismiss, title = title) {
        V5SheetHeader(title = title, onClose = onDismiss)
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(top = V5Spacing.space3, bottom = V5Spacing.space4),
            verticalArrangement = Arrangement.spacedBy(V5Spacing.space4),
        ) {
            V5Segmented(
                options = V5PrivacyReportRules.WINDOWS.map { pluralStringResource(R.plurals.v5_report_days, it, it) },
                selectedIndex = V5PrivacyReportRules.windowIndex(windowDays),
                onSelect = { onWindowChange(V5PrivacyReportRules.WINDOWS[it]) },
                modifier = Modifier.padding(horizontal = V5Spacing.marginPhone),
            )
            TotalCard(summary)
            Findings(summary)
            DeviceNote()
            V5ListGroup {
                if (onHideCard != null) {
                    V5ListRow(
                        title = stringResource(R.string.v5_report_hide_card),
                        onClick = {
                            onDismiss()
                            onHideCard()
                        },
                        modifier = Modifier.testTag(V5PrivacyReportTestTags.HideCard),
                    )
                }
                V5ListRow(
                    title = stringResource(R.string.v5_report_clear),
                    titleColor = V5Theme.colors.danger,
                    enabled = summary.total > 0,
                    onClick = { confirmClear = true },
                    modifier = Modifier.testTag(V5PrivacyReportTestTags.Clear),
                )
            }
        }
        if (confirmClear) {
            AlertDialog(
                onDismissRequest = { confirmClear = false },
                title = { Text(stringResource(R.string.protection_report_clear_title)) },
                text = { Text(stringResource(R.string.v5_report_clear_body)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            confirmClear = false
                            onClear()
                        },
                    ) { Text(stringResource(R.string.v5_report_clear)) }
                },
                dismissButton = {
                    TextButton(onClick = { confirmClear = false }) { Text(stringResource(R.string.action_cancel)) }
                },
            )
        }
    }
}

/** The one big number with its bars by day; today's bar is in the full accent. */
@Composable
private fun TotalCard(summary: ProtectionSummary) {
    val colors = V5Theme.colors
    val format = rememberCountFormat()
    val count = format.format(summary.total)
    val line = pluralStringResource(R.plurals.v5_report_blocked, summary.total, count)
    val countAt = line.indexOf(count)
    Column(
        Modifier
            .padding(horizontal = V5Spacing.marginPhone)
            .fillMaxWidth()
            .clip(V5Theme.shapes.m)
            .background(colors.surface)
            .padding(V5Spacing.space4),
        verticalArrangement = Arrangement.spacedBy(V5Spacing.space3),
    ) {
        Text(
            // The number in the title size, the words around it smaller and quieter.
            buildAnnotatedString {
                val words = V5Theme.type.callout.toSpanStyle().copy(color = colors.labelSecondary)
                if (countAt < 0) {
                    withStyle(words) { append(line) }
                } else {
                    withStyle(words) { append(line.substring(0, countAt)) }
                    withStyle(V5Theme.type.title.toSpanStyle()) { append(count) }
                    withStyle(words) { append(line.substring(countAt + count.length)) }
                }
            },
            style = V5Theme.type.title,
            color = colors.label,
            modifier = Modifier.testTag(V5PrivacyReportTestTags.Total),
        )
        if (summary.total > 0) {
            Chart(summary.daily, format.format(summary.daily.lastOrNull() ?: 0))
        } else {
            Text(stringResource(R.string.v5_report_empty), style = V5Theme.type.footnote, color = colors.labelSecondary)
        }
    }
}

@Composable
private fun Chart(daily: List<Int>, today: String) {
    val colors = V5Theme.colors
    val description = stringResource(R.string.v5_report_chart, today)
    val bar = RoundedCornerShape(V5Sizes.reportBarRadius)
    Row(
        Modifier
            .fillMaxWidth()
            .height(V5Sizes.reportChart)
            .testTag(V5PrivacyReportTestTags.Chart)
            .clearAndSetSemantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(V5Sizes.reportBarGap),
        verticalAlignment = Alignment.Bottom,
    ) {
        val fractions = V5PrivacyReportRules.barFractions(daily)
        fractions.forEachIndexed { index, fraction ->
            val isToday = index == fractions.lastIndex
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight(fraction)
                    .clip(bar)
                    .background(if (isToday) colors.accent else colors.accent.copy(alpha = V5PrivacyReportRules.PAST_BAR_ALPHA)),
            )
        }
    }
}

/** The two findings in the report's own words; a period without them shows neither. */
@Composable
private fun Findings(summary: ProtectionSummary) {
    val tracker = V5PrivacyReportRules.topTracker(summary)
    val site = V5PrivacyReportRules.busiestSite(summary)
    if (tracker == null && site == null) return
    V5ListGroup {
        if (tracker != null) {
            Finding(
                icon = VolaIcons.VisibilityOff,
                tile = V5Tile.Orange,
                caption = stringResource(R.string.v5_report_top_tracker),
                value = tracker.host,
                detail = pluralStringResource(R.plurals.v5_report_tracker_sites, tracker.siteCount, tracker.siteCount),
                modifier = Modifier.testTag(V5PrivacyReportTestTags.TopTracker),
            )
        }
        if (site != null) {
            Finding(
                icon = VolaIcons.Public,
                tile = V5Tile.Red,
                caption = stringResource(R.string.v5_report_busiest_site),
                value = site.host,
                detail = pluralStringResource(R.plurals.v5_report_site_trackers, site.trackerCount, site.trackerCount),
                modifier = Modifier.testTag(V5PrivacyReportTestTags.BusiestSite),
            )
        }
    }
}

@Composable
private fun Finding(
    icon: ImageVector,
    tile: V5Tile,
    caption: String,
    value: String,
    detail: String,
    modifier: Modifier = Modifier,
) {
    val colors = V5Theme.colors
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = V5Sizes.rowThreeLine)
            .semantics(mergeDescendants = true) {}
            .padding(horizontal = V5Spacing.space4, vertical = V5Spacing.space2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(V5Spacing.space3),
    ) {
        Box(
            Modifier
                .size(V5Sizes.rowTile)
                .clip(V5Theme.shapes.xs)
                .background(tile.color),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = tile.glyph, modifier = Modifier.size(V5Sizes.iconSmall))
        }
        Column(Modifier.weight(1f)) {
            Text(caption, style = V5Theme.type.footnote, color = colors.labelSecondary)
            Text(value, style = V5Theme.type.body, color = colors.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(detail, style = V5Theme.type.footnote, color = colors.labelSecondary)
        }
    }
}

/** Where the report lives: on this device only, without private tabs. */
@Composable
private fun DeviceNote() {
    val colors = V5Theme.colors
    Row(
        Modifier.padding(horizontal = V5Spacing.marginPhone + V5Spacing.space1),
        horizontalArrangement = Arrangement.spacedBy(V5Spacing.space2),
    ) {
        Icon(VolaIcons.Lock, contentDescription = null, tint = colors.labelSecondary, modifier = Modifier.size(V5Sizes.iconSmall))
        Text(stringResource(R.string.v5_report_device_only), style = V5Theme.type.footnote, color = colors.labelSecondary)
    }
}

@Composable
private fun rememberCountFormat(): NumberFormat {
    val locale = LocalConfiguration.current.locales[0]
    return remember(locale) { NumberFormat.getIntegerInstance(locale) }
}
