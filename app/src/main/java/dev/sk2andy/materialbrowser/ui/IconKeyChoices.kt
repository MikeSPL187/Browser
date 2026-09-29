package dev.sk2andy.materialbrowser.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.sk2andy.materialbrowser.WorkspaceIconResources
import dev.sk2andy.materialbrowser.shared.ui.PlatformProfileEmoji
import dev.sk2andy.materialbrowser.shared.ui.WorkspaceIcons
import dev.sk2andy.materialbrowser.ui.theme.MaterialBrowserTheme

/**
 * A two-row, horizontally scrolling set of Material Symbols. Values are the stored emoji keys of
 * [WorkspaceIcons], so callers keep their existing storage format.
 */
@Composable
internal fun IconKeyChoices(
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    keys: List<String> = WorkspaceIcons.keys,
    testTagForKey: ((String) -> String)? = null,
) {
    val selectedId = WorkspaceIcons.idFor(selected)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 8.dp)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        keys.chunked(ROWS).forEach { column ->
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                column.forEach { key ->
                    IconKeyChoice(
                        key = key,
                        selected = WorkspaceIcons.idFor(key) == selectedId,
                        onClick = { onSelect(key) },
                        modifier = testTagForKey?.let { tagFor -> Modifier.testTag(tagFor(key)) }
                            ?: Modifier,
                    )
                }
            }
        }
    }
}

@Composable
private fun IconKeyChoice(
    key: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val name = stringResource(WorkspaceIconResources.nameFor(key))
    val container by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        label = "icon choice container",
    )
    val content by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        label = "icon choice content",
    )
    Surface(
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .semantics { contentDescription = name },
        shape = CircleShape,
        color = container,
    ) {
        Box(contentAlignment = Alignment.Center) {
            CompositionLocalProvider(LocalContentColor provides content) {
                PlatformProfileEmoji(emoji = key, fontSize = 22.sp)
            }
        }
    }
}

private const val ROWS = 2

@Preview(name = "Icon choices", widthDp = 360)
@Composable
private fun IconKeyChoicesPreview() {
    MaterialBrowserTheme {
        Surface {
            IconKeyChoices(
                selected = "💼",
                onSelect = {},
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
    }
}
