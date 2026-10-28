package dev.sk2andy.materialbrowser.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.browser.BrowserProfile
import dev.sk2andy.materialbrowser.browser.DEFAULT_PROFILE_ID
import dev.sk2andy.materialbrowser.browser.WorkspaceAccent
import dev.sk2andy.materialbrowser.ui.theme.MaterialBrowserTheme
import dev.sk2andy.materialbrowser.ui.theme.color
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.sp
import dev.sk2andy.materialbrowser.shared.ui.PlatformProfileEmoji
import dev.sk2andy.materialbrowser.shared.ui.WorkspaceIcons
import androidx.compose.foundation.layout.ExperimentalLayoutApi

/** The name shown for a workspace: its own name, or a localized default when it has none. */
@Composable
internal fun BrowserProfile.workspaceDisplayName(): String = name.ifBlank {
    stringResource(
        if (id == DEFAULT_PROFILE_ID) R.string.workspace_default_name else R.string.workspace_untitled,
    )
}

@Composable
internal fun WorkspaceAccent.displayName(): String = stringResource(
    when (this) {
        WorkspaceAccent.Violet -> R.string.workspace_accent_violet
        WorkspaceAccent.Blue -> R.string.workspace_accent_blue
        WorkspaceAccent.Teal -> R.string.workspace_accent_teal
        WorkspaceAccent.Green -> R.string.workspace_accent_green
        WorkspaceAccent.Amber -> R.string.workspace_accent_amber
        WorkspaceAccent.Coral -> R.string.workspace_accent_coral
        WorkspaceAccent.Rose -> R.string.workspace_accent_rose
        WorkspaceAccent.Graphite -> R.string.workspace_accent_graphite
    },
)

@OptIn(ExperimentalLayoutApi::class)
@Preview(name = "Workspace icons", widthDp = 360)
@Composable
private fun WorkspaceIconsPreview() {
    MaterialBrowserTheme {
        androidx.compose.material3.Surface {
            FlowRow(
                modifier = Modifier.padding(20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                WorkspaceIcons.ids.forEachIndexed { index, id ->
                    val accent = WorkspaceAccent.entries[index % WorkspaceAccent.entries.size]
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                        contentAlignment = Alignment.Center,
                    ) {
                        CompositionLocalProvider(LocalContentColor provides accent.color()) {
                            PlatformProfileEmoji(
                                emoji = WorkspaceIcons.emojiFor(id).orEmpty(),
                                fontSize = 22.sp,
                            )
                        }
                    }
                }
            }
        }
    }
}
