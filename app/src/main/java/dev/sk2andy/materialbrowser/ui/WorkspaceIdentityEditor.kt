package dev.sk2andy.materialbrowser.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.browser.BrowserProfile
import dev.sk2andy.materialbrowser.browser.DEFAULT_PROFILE_ID
import dev.sk2andy.materialbrowser.browser.WorkspaceAccent
import dev.sk2andy.materialbrowser.browser.WorkspaceNameRules
import dev.sk2andy.materialbrowser.shared.ui.icons.VolaIcons
import dev.sk2andy.materialbrowser.ui.theme.MaterialBrowserTheme
import dev.sk2andy.materialbrowser.ui.theme.color
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.sp
import dev.sk2andy.materialbrowser.shared.ui.PlatformProfileEmoji
import dev.sk2andy.materialbrowser.shared.ui.WorkspaceIcons
import androidx.compose.foundation.layout.ExperimentalLayoutApi

internal object WorkspaceIdentityTestTags {
    const val Name = "workspace_name"

    fun accent(accent: WorkspaceAccent): String = "workspace_accent:${accent.wireValue}"
}

/** The name shown for a workspace: its own name, or a localized default when it has none. */
@Composable
internal fun BrowserProfile.workspaceDisplayName(): String = name.ifBlank {
    stringResource(
        if (id == DEFAULT_PROFILE_ID) R.string.workspace_default_name else R.string.workspace_untitled,
    )
}

/** Name field and accent row shared by the create and edit workspace sheets. */
@Composable
internal fun WorkspaceIdentityEditor(
    name: String,
    namePlaceholder: String,
    accent: WorkspaceAccent,
    onNameChange: (String) -> Unit,
    onAccentChange: (WorkspaceAccent) -> Unit,
    modifier: Modifier = Modifier,
    onNameDone: () -> Unit = {},
) {
    val focusManager = LocalFocusManager.current
    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = name,
            onValueChange = { value -> onNameChange(value.take(WorkspaceNameRules.MAX_LENGTH)) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag(WorkspaceIdentityTestTags.Name),
            label = { Text(stringResource(R.string.workspace_name_label)) },
            placeholder = { Text(namePlaceholder) },
            singleLine = true,
            shape = MaterialTheme.shapes.large,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                    onNameDone()
                },
            ),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.workspace_accent_label),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(10.dp))
        WorkspaceAccentPicker(selected = accent, onSelect = onAccentChange)
    }
}

@Composable
internal fun WorkspaceAccentPicker(
    selected: WorkspaceAccent,
    onSelect: (WorkspaceAccent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .selectableGroup(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        WorkspaceAccent.entries.forEach { accent ->
            WorkspaceAccentSwatch(
                accent = accent,
                selected = accent == selected,
                onClick = { onSelect(accent) },
            )
        }
    }
}

@Composable
private fun WorkspaceAccentSwatch(
    accent: WorkspaceAccent,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val color = accent.color()
    val label = accent.displayName()
    val ringColor by animateColorAsState(
        targetValue = if (selected) color else Color.Transparent,
        label = "workspace accent ring",
    )
    val dotSize by animateDpAsState(
        targetValue = if (selected) 26.dp else 32.dp,
        label = "workspace accent dot",
    )
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .border(width = 2.dp, color = ringColor, shape = CircleShape)
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .semantics { contentDescription = label }
            .testTag(WorkspaceIdentityTestTags.accent(accent)),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(dotSize)
                .clip(CircleShape)
                .background(color),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Icon(
                    imageVector = VolaIcons.Check,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = if (color.luminance() > 0.4f) Color.Black else Color.White,
                )
            }
        }
    }
}

@Composable
private fun WorkspaceAccent.displayName(): String = stringResource(
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

@Preview(name = "Workspace identity · light", widthDp = 360)
@Composable
private fun WorkspaceIdentityEditorPreview() {
    MaterialBrowserTheme {
        androidx.compose.material3.Surface {
            WorkspaceIdentityEditor(
                name = "Работа",
                namePlaceholder = "Personal",
                accent = WorkspaceAccent.Teal,
                onNameChange = {},
                onAccentChange = {},
                modifier = Modifier.padding(20.dp),
            )
        }
    }
}

@Preview(
    name = "Workspace identity · dark",
    widthDp = 360,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun WorkspaceIdentityEditorDarkPreview() {
    MaterialBrowserTheme {
        androidx.compose.material3.Surface {
            WorkspaceIdentityEditor(
                name = "",
                namePlaceholder = "Personal",
                accent = WorkspaceAccent.Rose,
                onNameChange = {},
                onAccentChange = {},
                modifier = Modifier.padding(20.dp),
            )
        }
    }
}

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
