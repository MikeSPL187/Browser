package dev.sk2andy.materialbrowser.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import dev.sk2andy.materialbrowser.shared.ui.settings.SettingsCardValueRow

/**
 * A setting with a few values on a settings card: the current one at the end of the row, the
 * list under it. Picking the current value again changes nothing.
 */
@Composable
internal fun <T> SettingsCardDropdownRow(
    title: String,
    selected: T,
    options: List<T>,
    label: @Composable (T) -> String,
    dividerColor: Color,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
    divider: Boolean = false,
    enabled: Boolean = true,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        SettingsCardValueRow(
            title = title,
            value = label(selected),
            summary = summary,
            dividerColor = dividerColor,
            divider = divider,
            enabled = enabled,
            onClick = { expanded = true },
        )
        SettingsDropdown(
            expanded = enabled && expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { option ->
                SettingsDropdownItem(
                    label = label(option),
                    selected = option == selected,
                    onClick = {
                        expanded = false
                        if (option != selected) onSelected(option)
                    },
                )
            }
        }
    }
}
