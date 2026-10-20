package dev.sk2andy.materialbrowser.ui.v5

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import dev.sk2andy.materialbrowser.shared.ui.icons.VolaIcons
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Sizes
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Spacing
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Theme

object V5FindInPageTestTags {
    const val Bar = "v5_find_in_page"
    const val Query = "v5_find_in_page_query"
    const val Previous = "v5_find_in_page_previous"
    const val Next = "v5_find_in_page_next"
    const val Done = "v5_find_in_page_done"
}

/** What the find capsule says and may do; the labels come localized from the binding. */
data class V5FindInPageLabels(
    val placeholder: String,
    val counting: String,
    val previous: String,
    val next: String,
    val done: String,
)

/**
 * Find in page (V28, B5; canvas screen «с1»): a capsule in the bar's place, above the keyboard
 * while it is open, as the address editor (screen «в»): the same height and «Done» on the right,
 * as in Safari. The count and the two arrows are inside the capsule; the field takes the focus at
 * once. «Search» on the keyboard goes to the next match, or searches again when there is none.
 */
@Composable
fun V5FindInPage(
    query: String,
    onQueryChange: (String) -> Unit,
    /** «2 of 7» or «Not found», or empty while there is no query. */
    matchText: String,
    isCounting: Boolean,
    canNavigate: Boolean,
    labels: V5FindInPageLabels,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = V5Theme.colors
    val focus = remember { FocusRequester() }
    // A query remembered from the last time comes in selected, so typing replaces it (FP-04).
    var field by remember { mutableStateOf(TextFieldValue(query, TextRange(0, query.length))) }
    if (field.text != query) field = TextFieldValue(query, TextRange(query.length))
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) {
        withFrameNanos { }
        focus.requestFocus()
        keyboard?.show()
    }
    Row(
        modifier
            .fillMaxWidth()
            .padding(horizontal = V5Spacing.space3)
            .testTag(V5FindInPageTestTags.Bar),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(V5Spacing.space1),
    ) {
        Row(
            Modifier
                .weight(1f)
                .heightIn(min = V5Sizes.barCapsule)
                .clip(V5Theme.shapes.full)
                .background(colors.surface)
                .padding(start = V5Spacing.space3),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(V5Spacing.space2),
        ) {
            Icon(VolaIcons.Search, contentDescription = null, tint = colors.labelSecondary, modifier = Modifier.size(V5Sizes.iconSmall))
            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (query.isEmpty()) {
                    // The field carries the placeholder for TalkBack; this is what the eye reads.
                    Text(
                        labels.placeholder,
                        style = V5Theme.type.body,
                        color = colors.labelTertiary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.clearAndSetSemantics {},
                    )
                }
                BasicTextField(
                    value = field,
                    onValueChange = { value ->
                        field = value
                        if (value.text != query) onQueryChange(value.text)
                    },
                    singleLine = true,
                    textStyle = V5Theme.type.body.copy(color = colors.label),
                    cursorBrush = SolidColor(colors.accent),
                    // «Search» even with nothing to step to: the controller then searches again,
                    // the way to retry when the page gave no answer.
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onNext() }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focus)
                        .semantics { contentDescription = labels.placeholder }
                        .testTag(V5FindInPageTestTags.Query),
                )
            }
            when {
                isCounting -> CircularProgressIndicator(
                    modifier = Modifier
                        .size(V5Sizes.iconSmall)
                        .semantics { contentDescription = labels.counting },
                    color = colors.labelSecondary,
                    strokeWidth = V5Sizes.downloadRing,
                )
                matchText.isNotEmpty() -> Text(
                    matchText,
                    style = V5Theme.type.footnote,
                    color = colors.labelSecondary,
                    maxLines = 1,
                    // TalkBack hears the new count as the matches are counted and stepped through.
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )
            }
            IconButton(onClick = onPrevious, enabled = canNavigate, modifier = Modifier.testTag(V5FindInPageTestTags.Previous)) {
                Icon(VolaIcons.KeyboardArrowUp, contentDescription = labels.previous, tint = colors.label)
            }
            IconButton(onClick = onNext, enabled = canNavigate, modifier = Modifier.testTag(V5FindInPageTestTags.Next)) {
                Icon(VolaIcons.KeyboardArrowDown, contentDescription = labels.next, tint = colors.label)
            }
        }
        Box(
            Modifier
                .widthIn(min = V5Sizes.editorCancel)
                .heightIn(min = V5Sizes.touchTarget)
                .clip(V5Theme.shapes.full)
                .clickable(role = Role.Button, onClick = onDone)
                .testTag(V5FindInPageTestTags.Done),
            contentAlignment = Alignment.Center,
        ) {
            Text(labels.done, style = V5Theme.type.body, color = colors.accent, maxLines = 1)
        }
    }
}
