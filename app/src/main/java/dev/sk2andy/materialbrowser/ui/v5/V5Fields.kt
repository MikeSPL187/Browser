package dev.sk2andy.materialbrowser.ui.v5

import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
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
                    .v5FieldLabel(hint, empty = query.isEmpty()),
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
    /** The margin to the screen's sides; none on a page that already keeps it, as settings do. */
    sideMargin: Dp = V5Spacing.marginPhone,
    /** [ImeAction.Next] on a form's field that has another after it: the keyboard moves on to it. */
    imeAction: ImeAction = ImeAction.Done,
    enabled: Boolean = true,
    /** A note: several lines, Enter starts a new one, the text starts at the top. */
    multiline: Boolean = false,
) {
    val colors = V5Theme.colors
    val focusManager = LocalFocusManager.current
    // A long app password or phrase is easy to mistype unseen: the eye shows it (ЯС-6).
    var revealed by rememberSaveable { mutableStateOf(false) }
    // Pasted text over the limit loses its end: the field says so instead of failing later (ЯС-4).
    var truncated by remember { mutableStateOf(false) }
    Column(Modifier.padding(horizontal = sideMargin).fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = V5Sizes.rowSingleLine)
                .clip(V5Theme.shapes.s)
                .background(colors.surface)
                .padding(start = V5Spacing.space4, end = if (password) V5Spacing.space1 else V5Spacing.space4)
                .then(if (multiline) Modifier.padding(vertical = V5Spacing.space3) else Modifier),
            verticalAlignment = if (multiline) Alignment.Top else Alignment.CenterVertically,
        ) {
            Box(Modifier.weight(1f), contentAlignment = if (multiline) Alignment.TopStart else Alignment.CenterStart) {
                if (value.isEmpty()) {
                    // The field carries the name for TalkBack; this is only what the eye reads (СС-1).
                    Text(placeholder, style = V5Theme.type.body, color = colors.labelTertiary, modifier = Modifier.clearAndSetSemantics {})
                }
                BasicTextField(
                    value = value,
                    onValueChange = { typed ->
                        truncated = typed.length > maxLength
                        onValueChange(typed.take(maxLength))
                    },
                    singleLine = !multiline,
                    minLines = if (multiline) V5Sizes.NOTE_FIELD_MIN_LINES else 1,
                    enabled = enabled,
                    textStyle = V5Theme.type.body.copy(color = if (enabled) colors.label else colors.labelSecondary),
                    cursorBrush = SolidColor(colors.accent),
                    keyboardOptions = KeyboardOptions(
                        capitalization = if (capitalize && !password) KeyboardCapitalization.Sentences else KeyboardCapitalization.None,
                        autoCorrectEnabled = !password && capitalize,
                        keyboardType = keyboardType ?: if (password) KeyboardType.Password else KeyboardType.Text,
                        imeAction = if (multiline) ImeAction.Default else imeAction,
                    ),
                    visualTransformation = if (password && !revealed) PasswordVisualTransformation() else VisualTransformation.None,
                    // «Next» moves on to the form's next field, as the key promises (M-01).
                    keyboardActions = KeyboardActions(
                        onDone = { onDone() },
                        onNext = { focusManager.moveFocus(FocusDirection.Next) },
                    ),
                    modifier = modifier
                        .fillMaxWidth()
                        .v5FieldLabel(placeholder, empty = value.isEmpty()),
                )
            }
            if (password) {
                IconButton(onClick = { revealed = !revealed }, enabled = enabled) {
                    Icon(
                        if (revealed) VolaIcons.VisibilityOff else VolaIcons.Visibility,
                        contentDescription = stringResource(if (revealed) R.string.passwords_hide else R.string.passwords_show),
                        tint = colors.labelSecondary,
                        modifier = Modifier.size(V5Sizes.iconSmall),
                    )
                }
            }
        }
        if (truncated) {
            Text(
                stringResource(R.string.v5_field_truncated, maxLength),
                style = V5Theme.type.footnote,
                color = colors.warning,
                modifier = Modifier
                    .padding(start = V5Spacing.space4, top = V5Spacing.space1)
                    .semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
    }
}

/** One swatch: its color and its name for TalkBack. */
@Immutable
internal data class V5Swatch(val color: Color, val name: String)

/**
 * Round color swatches under a [title]; the chosen one is ringed in the label color. They wrap to a
 * second line where a narrow phone has no room for all of them at the full touch target.
 */
@OptIn(ExperimentalLayoutApi::class)
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
        FlowRow(
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

/**
 * A text field's name for TalkBack: said while the field is empty. Once something is typed TalkBack
 * reads what was typed, which a name in contentDescription would replace (СС-1, FP-06); the place
 * of the field (its sheet, its heading) still says what it is for.
 */
internal fun Modifier.v5FieldLabel(label: String, empty: Boolean): Modifier =
    semantics { if (empty) contentDescription = label }
