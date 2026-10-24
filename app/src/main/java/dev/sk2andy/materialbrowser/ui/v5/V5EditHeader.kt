package dev.sk2andy.materialbrowser.ui.v5

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Sizes
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Spacing
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Theme

object V5EditHeaderTestTags {
    const val Cancel = "v5_edit_header_cancel"
}

/**
 * The header of a sheet that edits something (canvas «GroupEditor», «NewProfile»): «Cancel» at the
 * start, the title in the middle, the saving action ([done], semibold) at the end, as iOS edits. Both
 * actions are accent text with a 48 dp target; [doneEnabled] false fades the action and stops it.
 */
@Composable
fun V5EditHeader(
    title: String,
    cancel: String,
    onCancel: () -> Unit,
    done: String,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    doneEnabled: Boolean = true,
    doneTestTag: String? = null,
) {
    val colors = V5Theme.colors
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = V5Sizes.rowTwoLine)
            .padding(horizontal = V5Spacing.space2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HeaderAction(
            label = cancel,
            bold = false,
            enabled = true,
            onClick = onCancel,
            modifier = Modifier.weight(1f),
            testTag = V5EditHeaderTestTags.Cancel,
            alignment = Alignment.CenterStart,
        )
        Text(
            title,
            style = V5Theme.type.headline,
            color = colors.label,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(2f).semantics { heading() },
        )
        HeaderAction(
            label = done,
            bold = true,
            enabled = doneEnabled,
            onClick = onDone,
            modifier = Modifier.weight(1f),
            testTag = doneTestTag,
            alignment = Alignment.CenterEnd,
        )
    }
}

@Composable
private fun HeaderAction(
    label: String,
    bold: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
    alignment: Alignment,
    testTag: String?,
) {
    Box(modifier, contentAlignment = alignment) {
        Box(
            Modifier
                .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
                .widthIn(min = V5Sizes.touchTarget)
                .heightIn(min = V5Sizes.touchTarget)
                .clip(V5Theme.shapes.full)
                .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
                .alpha(if (enabled) 1f else DISABLED_ALPHA)
                .padding(horizontal = V5Spacing.space2),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                label,
                style = if (bold) V5Theme.type.body.copy(fontWeight = FontWeight.SemiBold) else V5Theme.type.body,
                color = V5Theme.colors.accent,
                maxLines = 2,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** A header action that cannot run now, as on Android: faded, still read by TalkBack. */
private const val DISABLED_ALPHA = 0.38f
