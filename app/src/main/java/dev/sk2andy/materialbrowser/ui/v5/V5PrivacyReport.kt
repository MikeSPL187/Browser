package dev.sk2andy.materialbrowser.ui.v5

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.data.ProtectionSummary
import dev.sk2andy.materialbrowser.shared.ui.icons.VolaIcons
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Elevation
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Shapes
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Sizes
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Spacing
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Theme
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Type.tabular
import dev.sk2andy.materialbrowser.ui.theme.v5.v5Elevation
import java.text.NumberFormat

object V5PrivacyReportTestTags {
    const val Total = "v5_report_total"
    const val Chart = "v5_report_chart"
    const val TopTracker = "v5_report_top_tracker"
    const val BusiestSite = "v5_report_busiest_site"
    const val Clear = "v5_report_clear"
    const val ClearConfirm = "v5_report_clear_confirm"
}

/**
 * The v5 privacy report (V18, canvas screen «Report», system-v5.md section 12) in a sheet over the
 * whole window: the title in the large size, 7 or 30 days, the period's blocks in one big number
 * with a bar per day (a week names its days), the most frequent tracker and the site with the most
 * trackers as cards of their own. Everything stays on the device; «Clear report data» asks first.
 * Hiding the new tab's card stays in that card's «•••».
 */
@Composable
fun V5PrivacyReport(
    visible: Boolean,
    summary: ProtectionSummary,
    windowDays: Int,
    onWindowChange: (Int) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit,
    /** The report keeps anything at all: «Clear» deletes every kept day, not only the period's (PR-01). */
    hasData: Boolean = summary.total > 0,
) {
    val title = stringResource(R.string.v5_report_title)
    val colors = V5Theme.colors
    var confirmClear by rememberSaveable { mutableStateOf(false) }
    V5WindowSheet(visible = visible, onDismiss = onDismiss, title = title) {
        // The sheet's own close stays (section 9); the title below it is the screen's, in the large size.
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
            V5SheetCloseButton(onClose = onDismiss, modifier = Modifier.padding(top = V5Spacing.space2, end = V5Spacing.space1))
        }
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = V5Spacing.marginPhone)
                .padding(bottom = V5Spacing.space4),
        ) {
            Text(
                title,
                style = V5Theme.type.largeTitle,
                color = colors.label,
                modifier = Modifier.semantics { heading() },
            )
            V5Segmented(
                options = V5PrivacyReportRules.WINDOWS.map { pluralStringResource(R.plurals.v5_report_days, it, it) },
                selectedIndex = V5PrivacyReportRules.windowIndex(windowDays),
                onSelect = { onWindowChange(V5PrivacyReportRules.WINDOWS[it]) },
                modifier = Modifier.padding(top = V5Spacing.space4),
            )
            TotalCard(summary, Modifier.padding(top = V5Spacing.space4))
            Findings(summary)
            DeviceNote(Modifier.padding(top = V5Spacing.space5))
            ClearButton(enabled = hasData, onClick = { confirmClear = true })
        }
        V5PromptSheet(
            visible = confirmClear,
            title = stringResource(R.string.protection_report_clear_title),
            message = stringResource(R.string.v5_report_clear_body),
            actions = listOf(
                V5PromptAction(
                    label = stringResource(R.string.v5_report_clear),
                    role = V5PromptRole.Destructive,
                    onClick = {
                        confirmClear = false
                        onClear()
                    },
                    testTag = V5PrivacyReportTestTags.ClearConfirm,
                ),
            ),
            onDismiss = { confirmClear = false },
        )
    }
}

/** The one big number, the words under it, and the bars by day; a week names its days. */
@Composable
private fun TotalCard(summary: ProtectionSummary, modifier: Modifier = Modifier) {
    val colors = V5Theme.colors
    val shape = V5Theme.shapes.l
    val locale = LocalConfiguration.current.locales[0]
    val labels = remember(summary.daily.size, summary.lastEpochDay, locale) {
        V5PrivacyReportRules.dayLabels(summary.daily.size, summary.lastEpochDay, locale)
    }
    Column(
        modifier
            .fillMaxWidth()
            .v5Elevation(V5Elevation.e1, shape)
            .clip(shape)
            .background(colors.surface)
            .padding(start = V5Spacing.space5, end = V5Spacing.space5, top = V5Spacing.space5, bottom = V5Spacing.space4),
    ) {
        ReportHeadline(summary.total)
        if (summary.total > 0) {
            V5ReportChart(summary.daily, Modifier.padding(top = V5Spacing.space4), labels = labels)
        } else {
            Text(
                stringResource(R.string.v5_report_empty),
                style = V5Theme.type.footnote,
                color = colors.labelSecondary,
                modifier = Modifier.padding(top = V5Spacing.space3),
            )
        }
    }
}

/** The report's own total: the number alone in the large size, what it counts under it. */
@Composable
private fun ReportHeadline(total: Int) {
    val colors = V5Theme.colors
    val count = rememberCountFormat().format(total)
    val line = pluralStringResource(R.plurals.v5_report_blocked, total, count)
    Column(
        Modifier
            .testTag(V5PrivacyReportTestTags.Total)
            .clearAndSetSemantics { contentDescription = line },
    ) {
        Text(count, style = V5Theme.type.largeTitle.tabular(), color = colors.label, maxLines = 1)
        Text(
            V5PrivacyReportRules.wordsAround(line, count),
            style = V5Theme.type.callout,
            color = colors.labelSecondary,
        )
    }
}

/** The period's blocks: the number in the title size, the words around it smaller and quieter. */
@Composable
internal fun V5ReportTotal(total: Int, modifier: Modifier = Modifier) {
    val colors = V5Theme.colors
    val count = rememberCountFormat().format(total)
    val line = pluralStringResource(R.plurals.v5_report_blocked, total, count)
    val countAt = line.indexOf(count)
    Text(
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
        modifier = modifier,
    )
}

/**
 * A bar per day, today in the full accent; TalkBack hears today's count, the busiest day's and the
 * average, what the bars show at a glance. Shared with the new tab's card, which has no [labels]:
 * with a label per day the chart is the report's taller one, today's label in the strong ink.
 */
@Composable
internal fun V5ReportChart(daily: List<Int>, modifier: Modifier = Modifier, labels: List<String> = emptyList()) {
    val colors = V5Theme.colors
    val format = rememberCountFormat()
    val facts = V5PrivacyReportRules.chartFacts(daily)
    val description = stringResource(
        R.string.v5_report_chart_days,
        format.format(facts.today),
        format.format(facts.most),
        format.format(facts.average),
    )
    val week = labels.size == daily.size && labels.isNotEmpty()
    val bar = RoundedCornerShape(if (week) V5Shapes.radiusXs else V5Sizes.reportBarRadius)
    val fractions = V5PrivacyReportRules.barFractions(daily)
    Row(
        modifier
            .fillMaxWidth()
            .testTag(V5PrivacyReportTestTags.Chart)
            .clearAndSetSemantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(if (week) V5Sizes.reportWeekBarGap else V5Sizes.reportBarGap),
        verticalAlignment = Alignment.Bottom,
    ) {
        fractions.forEachIndexed { index, fraction ->
            val isToday = index == fractions.lastIndex
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.height(if (week) V5Sizes.reportWeekChart else V5Sizes.reportChart), contentAlignment = Alignment.BottomCenter) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(fraction)
                            .clip(bar)
                            .background(if (isToday) colors.accent else colors.accent.copy(alpha = V5PrivacyReportRules.PAST_BAR_ALPHA)),
                    )
                }
                if (week) {
                    Text(
                        labels[index],
                        style = V5Theme.type.caption.copy(fontWeight = if (isToday) FontWeight.SemiBold else FontWeight.Medium),
                        color = if (isToday) colors.label else colors.labelSecondary,
                        maxLines = 1,
                        modifier = Modifier.padding(top = V5Spacing.space1 + V5Spacing.space1 / 2),
                    )
                }
            }
        }
    }
}

/** The two findings, each a card of its own in the report's words; a period without them shows neither. */
@Composable
private fun Findings(summary: ProtectionSummary) {
    val colors = V5Theme.colors
    val tracker = V5PrivacyReportRules.topTracker(summary)
    val site = V5PrivacyReportRules.busiestSite(summary)
    if (tracker != null) {
        Finding(
            tile = { GlyphTile(VolaIcons.VisibilityOff, colors.surfaceElevated, colors.labelSecondary) },
            caption = stringResource(R.string.v5_report_top_tracker),
            value = tracker.host,
            detail = pluralStringResource(R.plurals.v5_report_tracker_sites, tracker.siteCount, tracker.siteCount),
            number = tracker.siteCount.toString(),
            modifier = Modifier.testTag(V5PrivacyReportTestTags.TopTracker),
        )
    }
    if (site != null) {
        Finding(
            // The site in its own letter tile, as on the new tab and in the address suggestions.
            tile = { V5SiteTile(favicon = null, site = site.host, size = V5Sizes.reportFindingTile) },
            caption = stringResource(R.string.v5_report_busiest_site),
            value = site.host,
            detail = pluralStringResource(R.plurals.v5_report_site_trackers, site.trackerCount, site.trackerCount),
            number = site.trackerCount.toString(),
            modifier = Modifier.testTag(V5PrivacyReportTestTags.BusiestSite),
        )
    }
}

@Composable
private fun Finding(
    tile: @Composable () -> Unit,
    caption: String,
    value: String,
    detail: String,
    number: String,
    modifier: Modifier = Modifier,
) {
    val colors = V5Theme.colors
    val shape = V5Theme.shapes.m
    Column(
        modifier
            .padding(top = V5Spacing.space3)
            .fillMaxWidth()
            .v5Elevation(V5Elevation.e1, shape)
            .clip(shape)
            .background(colors.surface)
            .semantics(mergeDescendants = true) {}
            .padding(horizontal = V5Spacing.space4, vertical = V5Spacing.space3),
        verticalArrangement = Arrangement.spacedBy(V5Spacing.space2),
    ) {
        Text(caption, style = V5Theme.type.footnote.copy(fontWeight = FontWeight.SemiBold), color = colors.labelSecondary)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(V5Spacing.space3)) {
            tile()
            Column(Modifier.weight(1f)) {
                Text(value, style = V5Theme.type.headline, color = colors.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(emphasized(detail, number), style = V5Theme.type.callout, color = colors.labelSecondary)
            }
        }
    }
}

/** A finding's tile that is no site: [icon] on a neutral ground. */
@Composable
private fun GlyphTile(icon: ImageVector, tileColor: Color, glyphColor: Color) {
    Box(
        Modifier
            .size(V5Sizes.reportFindingTile)
            .clip(RoundedCornerShape(V5Sizes.reportFindingTileRadius))
            .background(tileColor),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = glyphColor, modifier = Modifier.size(V5Sizes.iconSmall))
    }
}

/** [line] with its [number] in the strong ink and weight, as the canvas sets it. */
@Composable
private fun emphasized(line: String, number: String) = buildAnnotatedString {
    val at = line.indexOf(number)
    if (at < 0) {
        append(line)
        return@buildAnnotatedString
    }
    append(line.substring(0, at))
    withStyle(SpanStyle(color = V5Theme.colors.label, fontWeight = FontWeight.SemiBold)) { append(number) }
    append(line.substring(at + number.length))
}

/** Where the report lives: on this device only, without private tabs. */
@Composable
private fun DeviceNote(modifier: Modifier = Modifier) {
    val colors = V5Theme.colors
    Row(
        modifier.padding(horizontal = V5Spacing.space1),
        horizontalArrangement = Arrangement.spacedBy(V5Spacing.space2),
    ) {
        Icon(VolaIcons.Lock, contentDescription = null, tint = colors.labelSecondary, modifier = Modifier.size(V5Sizes.iconSmall))
        Text(stringResource(R.string.v5_report_device_only), style = V5Theme.type.footnote, color = colors.labelSecondary)
    }
}

/** «Clear report data» as red text on its own, the way the canvas ends the screen. */
@Composable
private fun ClearButton(enabled: Boolean, onClick: () -> Unit) {
    val colors = V5Theme.colors
    Box(
        Modifier
            .padding(top = V5Spacing.space2)
            .fillMaxWidth()
            .heightIn(min = V5Spacing.space12)
            .clip(V5Theme.shapes.s)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .testTag(V5PrivacyReportTestTags.Clear),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            stringResource(R.string.v5_report_clear),
            style = V5Theme.type.body,
            color = if (enabled) colors.danger else colors.labelTertiary,
        )
    }
}

/** Counts grouped the way the screen's language writes them. */
@Composable
internal fun rememberCountFormat(): NumberFormat {
    val locale = LocalConfiguration.current.locales[0]
    return remember(locale) { NumberFormat.getIntegerInstance(locale) }
}
