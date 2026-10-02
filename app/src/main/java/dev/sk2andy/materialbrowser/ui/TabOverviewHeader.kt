package dev.sk2andy.materialbrowser.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.shared.ui.TabOverviewChromeTestTags
import dev.sk2andy.materialbrowser.shared.ui.icons.VolaIcons
import dev.sk2andy.materialbrowser.ui.theme.VolaMotion
import dev.sk2andy.materialbrowser.ui.theme.VolaTabOverview

/**
 * The top of the tab overview (board W-Tabs): the workspace name with its tab count, then the
 * actions. The workspaces themselves sit at the bottom, in the dock.
 */
@Composable
internal fun TabOverviewHeader(
    title: String,
    tabCount: Int,
    enabled: Boolean,
    pinnedJumpVisible: Boolean,
    onPinnedJump: () -> Unit,
    onSettings: () -> Unit,
    onMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .fillMaxWidth()
            .testTag(TabOverviewChromeTestTags.Header)
            .heightIn(min = VolaTabOverview.headerHeight)
            .padding(
                start = VolaTabOverview.headerStartPadding,
                end = VolaTabOverview.headerEndPadding,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(VolaTabOverview.headerCountGap),
        ) {
            Text(
                text = title,
                modifier = Modifier
                    .alignByBaseline()
                    .weight(1f, fill = false)
                    .semantics { heading() },
                style = MaterialTheme.typography.headlineMedium,
                color = colors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = pluralStringResource(R.plurals.tab_overview_count, tabCount, tabCount),
                modifier = Modifier.alignByBaseline(),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
                maxLines = 1,
            )
        }
        AnimatedVisibility(
            visible = pinnedJumpVisible,
            enter = fadeIn(VolaMotion.effects()),
            exit = fadeOut(VolaMotion.effects()),
        ) {
            IconButton(
                onClick = onPinnedJump,
                enabled = enabled,
                modifier = Modifier.testTag(TabOverviewChromeTestTags.PinnedTabsJump),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_push_pin),
                    contentDescription = stringResource(R.string.cd_scroll_to_pinned_tabs),
                    tint = colors.onSurface,
                )
            }
        }
        IconButton(
            onClick = onSettings,
            enabled = enabled,
            modifier = Modifier.testTag(TabOverviewChromeTestTags.Settings),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_settings),
                contentDescription = stringResource(R.string.action_settings),
                tint = colors.onSurface,
            )
        }
        IconButton(
            onClick = onMore,
            enabled = enabled,
            modifier = Modifier.testTag(TabOverviewChromeTestTags.More),
        ) {
            Icon(
                VolaIcons.MoreVert,
                contentDescription = stringResource(R.string.cd_tab_actions),
                tint = colors.onSurface,
            )
        }
    }
}
