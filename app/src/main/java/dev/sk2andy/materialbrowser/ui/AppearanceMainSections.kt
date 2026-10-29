package dev.sk2andy.materialbrowser.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import dev.sk2andy.materialbrowser.ui.theme.VolaAppearance
@Composable
internal fun AppearanceGroupTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier = modifier,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
    )
}

/** Choices in a row; the chosen one is a filled pill (board: a connected button group). */
@Composable
internal fun <T> AppearanceChoiceRow(
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    icon: ((T) -> ImageVector)?,
    testTag: (T) -> String,
    onSelect: (T) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(VolaAppearance.choiceGap),
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            Surface(
                selected = isSelected,
                onClick = { onSelect(option) },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = VolaAppearance.choiceHeight)
                    .semantics { role = Role.RadioButton }
                    .testTag(testTag(option)),
                shape = if (isSelected) {
                    VolaAppearance.choiceSelectedShape
                } else {
                    VolaAppearance.choiceShape
                },
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHigh
                },
                contentColor = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = VolaAppearance.choiceIconGap),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    icon?.let { iconOf ->
                        Icon(
                            iconOf(option),
                            contentDescription = null,
                            modifier = Modifier.size(VolaAppearance.choiceIconSize),
                        )
                        Spacer(Modifier.width(VolaAppearance.choiceIconGap))
                    }
                    Text(
                        label(option),
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
