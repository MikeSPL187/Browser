package dev.sk2andy.materialbrowser.ui.v5

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.shared.ui.icons.VolaIcons
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Sizes
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Spacing
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Theme

object V5PinnedAddSheetTestTags {
    const val Empty = "v5_pin_add_empty"
    fun candidate(id: String) = "v5_pin_add:$id"
}

/**
 * «Pin a site» from the new tab: the profile's open tabs that are not pinned yet, the most recent
 * first, then its favorites and its history (Н-1), so a site unpinned by mistake comes back without
 * opening it first. A tap pins the site and closes the sheet; with nothing to offer it says how to
 * get something.
 */
@Composable
fun V5PinnedAddSheet(
    visible: Boolean,
    candidates: List<V5RecentTab>,
    onAdd: (String) -> Unit,
    onDismiss: () -> Unit,
    favorites: List<V5RecentTab> = emptyList(),
    history: List<V5RecentTab> = emptyList(),
) {
    val colors = V5Theme.colors
    val title = stringResource(R.string.v5_newtab_add_title)
    V5WindowSheet(visible = visible, onDismiss = onDismiss, title = title) {
        V5SheetHeader(title = title, onClose = onDismiss)
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = V5Spacing.marginPhone)
                .padding(top = V5Spacing.space3, bottom = V5Spacing.space4),
        ) {
            if (candidates.isEmpty() && favorites.isEmpty() && history.isEmpty()) {
                Text(
                    stringResource(R.string.v5_newtab_add_empty),
                    style = V5Theme.type.callout,
                    color = colors.labelSecondary,
                    modifier = Modifier
                        .padding(horizontal = V5Spacing.space4, vertical = V5Spacing.space3)
                        .testTag(V5PinnedAddSheetTestTags.Empty),
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(V5Spacing.space4)) {
                    listOf(
                        R.string.v5_newtab_add_tabs to candidates,
                        R.string.v5_newtab_add_favorites to favorites,
                        R.string.v5_newtab_add_history to history,
                    ).filter { (_, rows) -> rows.isNotEmpty() }.forEach { (header, rows) ->
                        Column(verticalArrangement = Arrangement.spacedBy(V5Spacing.space2)) {
                            Text(
                                stringResource(header),
                                style = V5Theme.type.footnote,
                                color = colors.labelSecondary,
                                modifier = Modifier
                                    .padding(horizontal = V5Spacing.space4)
                                    .semantics { heading() },
                            )
                            CandidateList(rows, onAdd, onDismiss)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CandidateList(candidates: List<V5RecentTab>, onAdd: (String) -> Unit, onDismiss: () -> Unit) {
    val colors = V5Theme.colors
    Column(
        Modifier
            .fillMaxWidth()
            .clip(V5Theme.shapes.m)
            .background(colors.surface),
    ) {
        candidates.forEach { tab ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = V5Sizes.rowTwoLine)
                    .clickable(
                        role = Role.Button,
                        onClick = {
                            onDismiss()
                            onAdd(tab.id)
                        },
                    )
                    .padding(horizontal = V5Spacing.space4, vertical = V5Spacing.space2)
                    .testTag(V5PinnedAddSheetTestTags.candidate(tab.id)),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(V5Spacing.space3),
            ) {
                V5SiteTile(tab.favicon, site = tab.host, size = V5Sizes.iconSmall)
                Column(Modifier.weight(1f)) {
                    Text(tab.title, style = V5Theme.type.body, color = colors.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(tab.host, style = V5Theme.type.footnote, color = colors.labelSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Icon(VolaIcons.Add, contentDescription = null, tint = colors.accent, modifier = Modifier.size(V5Sizes.iconSmall))
            }
        }
    }
}
