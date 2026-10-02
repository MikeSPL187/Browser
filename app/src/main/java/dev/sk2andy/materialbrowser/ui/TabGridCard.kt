package dev.sk2andy.materialbrowser.ui

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.browser.BLANK_URL
import dev.sk2andy.materialbrowser.browser.BrowserTab
import dev.sk2andy.materialbrowser.shared.ui.CompactTabCardStyle
import dev.sk2andy.materialbrowser.shared.ui.icons.VolaIcons
import dev.sk2andy.materialbrowser.ui.theme.VolaElevation
import dev.sk2andy.materialbrowser.ui.theme.VolaFrame
import dev.sk2andy.materialbrowser.ui.theme.VolaTabOverview
import dev.sk2andy.materialbrowser.ui.theme.VolaTheme

/** The label of a tab card in every overview layout. */
@Composable
internal fun tabCardDescription(tab: BrowserTab, selected: Boolean): String {
    val title = displayTabTitle(tab)
    val current = stringResource(R.string.tab_card_current)
    val pinned = stringResource(R.string.cd_pinned_tab)
    val url = if (tab.url == BLANK_URL) "" else tab.url
    return TabCardDescriptionRules.join(
        TabCardDescriptionRules.parts(title, url) +
            listOfNotNull(current.takeIf { selected }, pinned.takeIf { tab.isPinned }),
    )
}

/** The v4 card of the grid (board W-Tabs): one shape, the current tab ringed in the accent. */
@Composable
internal fun tabOverviewCardStyle(): CompactTabCardStyle {
    val colors = MaterialTheme.colorScheme
    val card = VolaTheme.extendedColors.card
    return remember(colors.primary, card) {
        CompactTabCardStyle(
            shape = VolaTabOverview.cardShape,
            containerColor = card,
            selectedBorder = BorderStroke(VolaTabOverview.selectedRingWidth, colors.primary),
            elevation = VolaElevation.level1,
            selectedElevation = VolaTabOverview.selectedGlowElevation,
            shadowColor = VolaFrame.shadowColor,
            selectedShadowColor = colors.primary,
        )
    }
}

/**
 * The row above the page on a grid card: the site icon, the title and ✕. The card itself carries
 * the label, so the title here is hidden from TalkBack; ✕ keeps its own label.
 */
@Composable
internal fun TabGridCardTitleRow(
    tab: BrowserTab,
    favicon: Bitmap?,
    enabled: Boolean,
    onClose: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val dividerColor = colors.surfaceContainerHigh
    val title = displayTabTitle(tab)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(VolaTabOverview.cardTitleRowHeight)
            .drawBehind {
                val y = size.height - VolaTabOverview.cardDividerWidth.toPx() / 2f
                drawLine(
                    color = dividerColor,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = VolaTabOverview.cardDividerWidth.toPx(),
                )
            }
            .padding(
                start = VolaTabOverview.cardTitleStartPadding,
                end = VolaTabOverview.cardTitleEndPadding,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // The card reads the title; the icon's letter would only repeat it.
        Box(Modifier.clearAndSetSemantics { }) {
            TabFavicon(tab = tab, favicon = favicon, size = VolaTabOverview.cardFaviconSize)
        }
        Text(
            text = title,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = VolaTabOverview.cardTitleGap)
                .clearAndSetSemantics { }
                .testTag(SnoozeTestTags.overviewTitle(tab.id)),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = colors.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (tab.isPinned) {
            Icon(
                painter = painterResource(R.drawable.ic_push_pin),
                contentDescription = null,
                modifier = Modifier
                    .padding(end = VolaTabOverview.cardTitleStartPadding)
                    .size(VolaTabOverview.cardCloseIconSize),
                tint = colors.onSurfaceVariant,
            )
        } else {
            IconButton(
                onClick = onClose ?: {},
                enabled = enabled && onClose != null,
                modifier = Modifier.testTag(SnoozeTestTags.overviewClose(tab.id)),
            ) {
                Icon(
                    VolaIcons.Close,
                    contentDescription = onClose?.let {
                        stringResource(R.string.cd_close_named_tab, title)
                    },
                    modifier = Modifier.size(VolaTabOverview.cardCloseIconSize),
                    tint = colors.onSurfaceVariant,
                )
            }
        }
    }
}
