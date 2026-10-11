package dev.sk2andy.materialbrowser.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.shared.ui.icons.VolaIcons
import dev.sk2andy.materialbrowser.shared.ui.settings.SettingsHomeEntry
import dev.sk2andy.materialbrowser.shared.ui.settings.SettingsHomeItem
import dev.sk2andy.materialbrowser.ui.theme.v5.V5SiteTintSetting
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Sizes
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Spacing
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Theme
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Tile
import dev.sk2andy.materialbrowser.ui.theme.v5.VolaV5Theme
import dev.sk2andy.materialbrowser.ui.theme.v5.rememberV5Preference
import dev.sk2andy.materialbrowser.ui.v5.V5ListGroup
import dev.sk2andy.materialbrowser.ui.v5.V5ListRow
import dev.sk2andy.materialbrowser.ui.v5.V5RowIcon
import dev.sk2andy.materialbrowser.ui.v5.V5RowTrailing
import dev.sk2andy.materialbrowser.ui.v5.V5SettingsHomeRules

internal object SettingsHomeV5TestTags {
    const val SiteTint = "settings_home_v5_site_tint"
}

/**
 * The v5 settings home (canvas screen «ж», V15c): «Settings» in the large title, a search field, the
 * same pages as the current home in Safari-like groups with section tiles and their current values,
 * and the «tint the bar» switch next to Appearance. Search, the pages and the default-browser banner
 * are the current ones; a long press on «About» still unlocks the developer options.
 */
@Composable
internal fun SettingsHomeV5(
    entries: List<SettingsHomeEntry>,
    isDefaultBrowser: Boolean,
    onOpenDefaultBrowserSettings: () -> Unit,
    onOpen: (SettingsHomeItem) -> Unit,
    onSearch: () -> Unit,
    onDismiss: () -> Unit,
    developerOptionsUnlocked: Boolean,
    onUnlockDeveloperOptions: (() -> Unit)?,
) {
    val darkShell = MaterialTheme.colorScheme.background.luminance() < DARK_SHELL_LUMINANCE
    val byItem = entries.associateBy { it.item }
    val groups = V5SettingsHomeRules.groups(entries.map { it.item })
    var siteTint by rememberV5Preference(V5SiteTintSetting)
    VolaV5Theme(dark = darkShell) {
        val colors = V5Theme.colors
        Column(
            Modifier
                .fillMaxSize()
                .background(colors.background)
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(bottom = V5Spacing.space8),
            verticalArrangement = Arrangement.spacedBy(V5Spacing.space6),
        ) {
            Column {
                IconButton(onClick = onDismiss, modifier = Modifier.padding(start = V5Spacing.space1)) {
                    Icon(VolaIcons.ArrowBack, contentDescription = stringResource(R.string.action_back), tint = colors.accent)
                }
                Text(
                    stringResource(R.string.settings_title),
                    style = V5Theme.type.largeTitle,
                    color = colors.label,
                    modifier = Modifier
                        .padding(horizontal = V5Spacing.marginPhone)
                        .semantics { heading() },
                )
                SearchField(onSearch)
            }
            if (!isDefaultBrowser) {
                Row(Modifier.padding(horizontal = V5Spacing.marginPhone)) {
                    DefaultBrowserBanner(onMakeDefault = onOpenDefaultBrowserSettings)
                }
            }
            groups.forEachIndexed { index, group ->
                V5ListGroup {
                    group.forEach { item ->
                        val entry = byItem.getValue(item)
                        val aboutUnlocks = item.destination == SettingsDestination.AboutLegal &&
                            !developerOptionsUnlocked && onUnlockDeveloperOptions != null
                        V5ListRow(
                            title = entry.title,
                            subtitle = entry.summary,
                            icon = V5RowIcon(settingsHomeIcon(item.icon), V5SettingsHomeRules.tile(item)),
                            trailing = V5RowTrailing.Chevron,
                            onClick = { onOpen(item) },
                            onLongClick = onUnlockDeveloperOptions.takeIf { aboutUnlocks },
                            onLongClickLabel = stringResource(R.string.developer_options_unlock_action)
                                .takeIf { aboutUnlocks },
                        )
                    }
                    if (index == V5SettingsHomeRules.APPEARANCE_GROUP) {
                        V5ListRow(
                            title = stringResource(R.string.settings_v5_site_tint),
                            icon = V5RowIcon(VolaIcons.Contrast, V5Tile.Pink),
                            trailing = V5RowTrailing.Toggle(siteTint, { siteTint = it }),
                            modifier = Modifier.testTag(SettingsHomeV5TestTags.SiteTint),
                        )
                    }
                }
            }
        }
    }
}

/** «Search settings»: a field-shaped button into the current settings search. */
@Composable
private fun SearchField(onSearch: () -> Unit) {
    val colors = V5Theme.colors
    val label = stringResource(R.string.settings_search_open)
    Row(
        Modifier
            .padding(horizontal = V5Spacing.marginPhone, vertical = V5Spacing.space2)
            .fillMaxWidth()
            .heightIn(min = V5Sizes.touchTarget)
            .clip(V5Theme.shapes.s)
            .background(colors.fill)
            .clickable(role = Role.Button, onClickLabel = label, onClick = onSearch)
            .padding(horizontal = V5Spacing.space3)
            .testTag(SettingsSearchTestTags.Open),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(VolaIcons.Search, contentDescription = null, tint = colors.labelSecondary, modifier = Modifier.size(V5Sizes.iconSmall))
        Text(
            label,
            style = V5Theme.type.body,
            color = colors.labelSecondary,
            modifier = Modifier.padding(start = V5Spacing.space2),
        )
    }
}

/** Below this background luminance the current theme is dark. */
private const val DARK_SHELL_LUMINANCE = 0.5f
