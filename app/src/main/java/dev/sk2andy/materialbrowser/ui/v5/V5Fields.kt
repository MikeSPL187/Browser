package dev.sk2andy.materialbrowser.ui.v5

import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.shared.ui.icons.VolaIcons
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Sizes
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Spacing
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Theme

/*
 * Fields of the v5 sheets that name and color things (tab groups V12b, profiles V11b): one name
 * field and one row of color swatches, so every such sheet looks and behaves the same. The search
 * capsule is the one of the overview and the library.
 */

/**
 * A search capsule on the fill: the glass, the [hint] while [query] is empty (read by TalkBack as
 * the field's name) and a clear button once something is typed. [fieldModifier] reaches the text
 * field itself, for its test tag.
 */
@Composable
fun V5SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    hint: String,
    modifier: Modifier = Modifier,
    fieldModifier: Modifier = Modifier,
) {
    val colors = V5Theme.colors
    Row(
        modifier
            .heightIn(min = V5Sizes.touchTarget)
            .clip(V5Theme.shapes.full)
            .background(colors.fill)
            .padding(horizontal = V5Spacing.space3),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(V5Spacing.space2),
    ) {
        Icon(VolaIcons.Search, contentDescription = null, tint = colors.labelSecondary, modifier = Modifier.size(V5Sizes.iconSmall))
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (query.isEmpty()) {
                // The field carries the hint for TalkBack; this is only what the eye reads.
                Text(
                    hint,
                    style = V5Theme.type.body,
                    color = colors.labelTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.clearAndSetSemantics {},
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = V5Theme.type.body.copy(color = colors.label),
                cursorBrush = SolidColor(colors.accent),
                modifier = fieldModifier
                    .fillMaxWidth()
                    .semantics { contentDescription = hint },
            )
        }
        if (query.isNotEmpty()) {
            IconButton(onClick = { onQueryChange("") }) {
                Icon(
                    VolaIcons.Close,
                    contentDescription = stringResource(R.string.v5_editor_clear),
                    tint = colors.labelSecondary,
                    modifier = Modifier.size(V5Sizes.iconSmall),
                )
            }
        }
    }
}

/** A one-line name on a rounded card, its [placeholder] shown while it is empty and read by TalkBack. */
@Composable
internal fun V5NameField(
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    onDone: () -> Unit,
    maxLength: Int,
    modifier: Modifier = Modifier,
    /** A password: dots on screen and the password keyboard (B9b, signing in to a site). */
    password: Boolean = false,
    /** A name starts with a capital; a site's answer or a user name does not. */
    capitalize: Boolean = true,
    /** An address takes the address keyboard; null picks the text or password one. */
    keyboardType: KeyboardType? = null,
) {
    val colors = V5Theme.colors
    Box(
        Modifier
            .padding(horizontal = V5Spacing.marginPhone)
            .fillMaxWidth()
            .heightIn(min = V5Sizes.rowSingleLine)
            .clip(V5Theme.shapes.s)
            .background(colors.surface)
            .padding(horizontal = V5Spacing.space4),
        contentAlignment = Alignment.CenterStart,
    ) {
        if (value.isEmpty()) {
            Text(placeholder, style = V5Theme.type.body, color = colors.labelTertiary)
        }
        BasicTextField(
            value = value,
            onValueChange = { onValueChange(it.take(maxLength)) },
            singleLine = true,
            textStyle = V5Theme.type.body.copy(color = colors.label),
            cursorBrush = SolidColor(colors.accent),
            keyboardOptions = KeyboardOptions(
                capitalization = if (capitalize && !password) KeyboardCapitalization.Sentences else KeyboardCapitalization.None,
                autoCorrectEnabled = !password && capitalize,
                keyboardType = keyboardType ?: if (password) KeyboardType.Password else KeyboardType.Text,
                imeAction = ImeAction.Done,
            ),
            visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardActions = KeyboardActions(onDone = { onDone() }),
            modifier = modifier
                .fillMaxWidth()
                .semantics { contentDescription = placeholder },
        )
    }
}

/** One swatch: its color and its name for TalkBack. */
@Immutable
internal data class V5Swatch(val color: Color, val name: String)

/** Round color swatches under a [title]; the chosen one is ringed in the label color. */
@Composable
internal fun V5SwatchRow(
    title: String,
    swatches: List<V5Swatch>,
    selected: Int,
    onSelect: (Int) -> Unit,
    testTag: (Int) -> String,
) {
    Column(Modifier.padding(horizontal = V5Spacing.marginPhone)) {
        Text(
            title,
            style = V5Theme.type.footnote,
            color = V5Theme.colors.labelSecondary,
            modifier = Modifier.padding(start = V5Spacing.space4, bottom = V5Spacing.space2),
        )
        Row(
            Modifier.fillMaxWidth().selectableGroup(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            swatches.forEachIndexed { index, swatch ->
                val chosen = index == selected
                Box(
                    Modifier
                        .size(V5Sizes.touchTarget)
                        .clip(V5Theme.shapes.full)
                        .selectable(selected = chosen, role = Role.RadioButton, onClick = { onSelect(index) })
                        .semantics { contentDescription = swatch.name }
                        .testTag(testTag(index)),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        Modifier
                            .size(V5Sizes.suggestionTile)
                            .border(
                                width = V5Sizes.groupBorder,
                                color = if (chosen) V5Theme.colors.label else Color.Transparent,
                                shape = V5Theme.shapes.full,
                            )
                            .padding(V5Sizes.groupBorder * 2)
                            .clip(V5Theme.shapes.full)
                            .background(swatch.color),
                    )
                }
            }
        }
    }
}
