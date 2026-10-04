@file:OptIn(ExperimentalMaterial3Api::class)

package dev.sk2andy.materialbrowser.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.data.EssentialEntry
import dev.sk2andy.materialbrowser.data.ProtectionWeek
import dev.sk2andy.materialbrowser.ui.theme.VolaElevation
import dev.sk2andy.materialbrowser.ui.theme.VolaEssentials
import dev.sk2andy.materialbrowser.ui.theme.VolaProtection
import dev.sk2andy.materialbrowser.ui.theme.VolaShapes
import dev.sk2andy.materialbrowser.ui.theme.VolaSpacing
import dev.sk2andy.materialbrowser.ui.theme.VolaTheme
import dev.sk2andy.materialbrowser.ui.theme.VolaTypeScale
import java.text.NumberFormat
import java.util.Locale
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle

internal object ProtectionReportTestTags {
    const val Card = "protection_card"
    const val Sheet = "protection_report"
}

/**
 * «N trackers this week» on the new tab (board NewTab): the blocker's work of the last seven days,
 * counted on the device. A tap opens [ProtectionReportSheet].
 */
@Composable
internal fun NewTabProtectionCard(
    week: ProtectionWeek,
    enabled: Boolean,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val locale = LocalConfiguration.current.locales[0]
    val title = if (week.total > 0) {
        pluralStringResource(R.plurals.protection_card_title, week.total, formatCount(week.total, locale))
    } else {
        stringResource(R.string.protection_card_empty_title)
    }
    val subtitle = if (week.total > 0) {
        pluralStringResource(R.plurals.protection_card_sites, week.siteCount, week.siteCount)
    } else {
        stringResource(R.string.protection_card_empty_body)
    }
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag(ProtectionReportTestTags.Card),
        shape = VolaShapes.card,
        color = colors.primaryContainer,
        contentColor = colors.onPrimaryContainer,
    ) {
        Row(
            modifier = Modifier
                .clickable(
                    enabled = enabled,
                    role = Role.Button,
                    onClickLabel = stringResource(R.string.protection_card_open),
                    onClick = onOpen,
                )
                .heightIn(min = VolaProtection.cardMinHeight)
                .padding(horizontal = VolaSpacing.x4, vertical = VolaSpacing.x3),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(VolaSpacing.x3),
        ) {
            Box(
                modifier = Modifier
                    .size(VolaProtection.cardIconSize)
                    .clip(VolaProtection.cardIconShape)
                    .background(colors.primary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_symbol_shield),
                    contentDescription = null,
                    tint = colors.onPrimary,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onPrimaryContainer,
                )
            }
            Icon(
                painter = painterResource(R.drawable.ic_symbol_chevron_right),
                contentDescription = null,
            )
        }
    }
}

/**
 * The weekly report (board ProtectionReport, П7): the week's total, a bar per day and the sites
 * that tried to track the most. Everything comes from this phone; the user can clear the report
 * or hide the card.
 */
@Composable
internal fun ProtectionReportSheet(
    week: ProtectionWeek,
    onClear: () -> Unit,
    onHideCard: () -> Unit,
    onDismiss: () -> Unit,
) {
    var confirmClear by remember { mutableStateOf(false) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = VolaShapes.sheet,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        ProtectionReportContent(
            week = week,
            onClearRequest = { confirmClear = true },
            onHideCard = onHideCard,
        )
    }
    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text(stringResource(R.string.protection_report_clear_title)) },
            text = { Text(stringResource(R.string.protection_report_clear_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmClear = false
                        onClear()
                    },
                ) {
                    Text(stringResource(R.string.protection_report_clear))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

/** The report itself, inside [ProtectionReportSheet]; separate so previews can show it. */
@Composable
internal fun ProtectionReportContent(
    week: ProtectionWeek,
    onClearRequest: () -> Unit,
    onHideCard: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(start = VolaSpacing.x4, end = VolaSpacing.x4, bottom = VolaSpacing.x8),
        verticalArrangement = Arrangement.spacedBy(VolaSpacing.x3),
    ) {
        ProtectionReportHeader(week)
        ProtectionReportTotal(week)
        if (week.total > 0) {
            ProtectionReportChart(week)
            Text(
                text = stringResource(R.string.protection_report_top_sites).uppercase(),
                modifier = Modifier
                    .padding(start = VolaSpacing.x1, top = VolaSpacing.x1)
                    .semantics { heading() },
                style = VolaTypeScale.overline,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            ProtectionReportSites(week)
        }
        Row(
            modifier = Modifier.padding(horizontal = VolaSpacing.x1),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(VolaSpacing.x2),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_symbol_shield_lock),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.protection_report_device_only),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(VolaSpacing.x2)) {
            FilledTonalButton(
                onClick = onClearRequest,
                enabled = week.total > 0,
                modifier = Modifier.weight(1f),
                shape = CircleShape,
            ) {
                Text(stringResource(R.string.protection_report_clear), maxLines = 1)
            }
            FilledTonalButton(
                onClick = onHideCard,
                modifier = Modifier.weight(1f),
                shape = CircleShape,
            ) {
                Text(stringResource(R.string.protection_report_hide), maxLines = 1)
            }
        }
    }
}

@Composable
private fun ProtectionReportHeader(week: ProtectionWeek) {
    val locale = LocalConfiguration.current.locales[0]
    val range = remember(week.firstEpochDay, week.lastEpochDay, locale) {
        val pattern = android.text.format.DateFormat.getBestDateTimePattern(locale, "dMMMM")
        val formatter = DateTimeFormatter.ofPattern(pattern, locale)
        "${LocalDate.ofEpochDay(week.firstEpochDay).format(formatter)} – " +
            LocalDate.ofEpochDay(week.lastEpochDay).format(formatter)
    }
    // The date range sits under the title: next to it, an English title breaks in two.
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = VolaSpacing.x1),
    ) {
        Text(
            text = stringResource(R.string.protection_report_title),
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            text = range,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ProtectionReportTotal(week: ProtectionWeek) {
    val colors = MaterialTheme.colorScheme
    val locale = LocalConfiguration.current.locales[0]
    Surface(
        shape = VolaShapes.material.extraLarge,
        color = VolaTheme.extendedColors.card,
        shadowElevation = VolaElevation.level1,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(VolaSpacing.x4),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(VolaSpacing.x4),
        ) {
            Box(
                modifier = Modifier
                    .size(VolaProtection.heroIconSize)
                    .clip(VolaProtection.heroIconShape)
                    .background(colors.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_symbol_shield),
                    contentDescription = null,
                    modifier = Modifier.size(VolaProtection.heroGlyphSize),
                    tint = colors.onPrimaryContainer,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                if (week.total > 0) {
                    Text(
                        text = pluralStringResource(
                            R.plurals.protection_report_total,
                            week.total,
                            formatCount(week.total, locale),
                        ),
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Text(
                        text = pluralStringResource(R.plurals.protection_card_sites, week.siteCount, week.siteCount),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                    )
                } else {
                    Text(
                        text = stringResource(R.string.protection_report_empty),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun ProtectionReportChart(week: ProtectionWeek) {
    val locale = LocalConfiguration.current.locales[0]
    val colors = MaterialTheme.colorScheme
    val maximum = week.daily.maxOrNull()?.takeIf { it > 0 } ?: 1
    val days = week.daily.mapIndexed { index, count ->
        val date = LocalDate.ofEpochDay(week.firstEpochDay + index)
        date.dayOfWeek.getDisplayName(TextStyle.SHORT_STANDALONE, locale) to count
    }
    val description = stringResource(
        R.string.protection_report_chart,
        days.joinToString { (day, count) -> "$day ${formatCount(count, locale)}" },
    )
    Surface(
        modifier = Modifier.clearAndSetSemantics { contentDescription = description },
        shape = VolaShapes.card,
        color = VolaTheme.extendedColors.card,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(VolaSpacing.x4),
            horizontalArrangement = Arrangement.spacedBy(VolaSpacing.x2),
        ) {
            days.forEachIndexed { index, (day, count) ->
                val today = index == days.lastIndex
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(VolaSpacing.x2),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(VolaProtection.chartHeight),
                        contentAlignment = Alignment.BottomCenter,
                    ) {
                        if (count > 0) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(count.toFloat() / maximum)
                                    .clip(VolaProtection.barShape)
                                    .background(if (today) colors.primary else colors.primaryContainer),
                            )
                        }
                    }
                    Text(
                        text = day,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (today) FontWeight.Bold else FontWeight.Normal,
                        color = if (today) colors.onSurface else colors.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun ProtectionReportSites(week: ProtectionWeek) {
    val colors = MaterialTheme.colorScheme
    val locale = LocalConfiguration.current.locales[0]
    val maximum = week.topSites.maxOfOrNull { it.blocked }?.takeIf { it > 0 } ?: 1
    Surface(shape = VolaShapes.card, color = VolaTheme.extendedColors.card) {
        Column {
            week.topSites.forEachIndexed { index, site ->
                if (index > 0) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = VolaSpacing.x4),
                        color = colors.surfaceContainerHigh,
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = VolaEssentials.sheetRowMinHeight)
                        .padding(horizontal = VolaSpacing.x4, vertical = VolaSpacing.x2)
                        .semantics(mergeDescendants = true) {},
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(VolaSpacing.x3),
                ) {
                    SiteLetter(EssentialEntry(url = "https://${site.host}/", title = site.host), index)
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(VolaSpacing.x2),
                    ) {
                        Text(
                            text = site.host,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(VolaProtection.siteBarHeight)
                                .clip(VolaProtection.siteBarShape)
                                .background(colors.surfaceContainerHigh),
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(site.blocked.toFloat() / maximum)
                                    .clip(VolaProtection.siteBarShape)
                                    .background(colors.primary),
                            )
                        }
                    }
                    Text(
                        text = formatCount(site.blocked, locale),
                        modifier = Modifier.width(VolaProtection.siteCountWidth),
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        textAlign = TextAlign.End,
                    )
                }
            }
        }
    }
}

/** A site's first letter on a container color, like a tile without an icon. */
@Composable
private fun SiteLetter(entry: EssentialEntry, colorIndex: Int) {
    EssentialIcon(entry = entry, icon = null, colorIndex = colorIndex, size = VolaEssentials.sheetIconSize)
}

internal fun formatCount(count: Int, locale: Locale): String =
    NumberFormat.getIntegerInstance(locale).format(count)
