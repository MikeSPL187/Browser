package dev.sk2andy.materialbrowser.ui.v5

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Sizes
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Spacing
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Theme

/**
 * An action of a [V5StatePage]: the main one is filled in the accent with an optional glyph; a
 * quiet one is accent text, or red when [destructive].
 */
@Immutable
data class V5StateAction(
    val label: String,
    val onClick: () -> Unit,
    val icon: ImageVector? = null,
    val enabled: Boolean = true,
    val testTag: String? = null,
    val destructive: Boolean = false,
)

/**
 * A whole screen that stands in for a page (V28, B13): a glyph on a disc, the title, one message,
 * the filled main action and quiet actions under it (red for one that removes). Locked private tabs
 * and a locked profile use it; a touch that misses the controls stops here.
 */
@Composable
fun V5StatePage(
    icon: ImageVector,
    title: String,
    message: String,
    primary: V5StateAction,
    modifier: Modifier = Modifier,
    secondary: List<V5StateAction> = emptyList(),
) {
    val colors = V5Theme.colors
    Box(
        modifier
            .fillMaxSize()
            .pointerInput(Unit) { awaitPointerEventScope { while (true) awaitPointerEvent() } }
            .background(colors.background)
            .safeDrawingPadding(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .widthIn(max = V5Sizes.stateContentMax)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = V5Spacing.space6, vertical = V5Spacing.space4),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(V5Spacing.space3),
        ) {
            Box(
                Modifier
                    .padding(bottom = V5Spacing.space3)
                    .size(V5Sizes.privateGem)
                    .clip(V5Theme.shapes.full)
                    .background(colors.surfaceElevated),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = colors.accent, modifier = Modifier.size(V5Sizes.privateGemIcon))
            }
            Text(
                title,
                style = V5Theme.type.title,
                color = colors.label,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() },
            )
            Text(message, style = V5Theme.type.callout, color = colors.labelSecondary, textAlign = TextAlign.Center)
            Column(
                Modifier.fillMaxWidth().padding(top = V5Spacing.space4),
                verticalArrangement = Arrangement.spacedBy(V5Spacing.space2),
            ) {
                PrimaryAction(primary)
                secondary.forEach { action -> QuietAction(action) }
            }
        }
    }
}

@Composable
private fun PrimaryAction(action: V5StateAction) {
    val colors = V5Theme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = V5Sizes.touchTarget)
            .clip(V5Theme.shapes.full)
            .background(colors.accentFill)
            .alpha(if (action.enabled) 1f else DISABLED_ALPHA)
            .clickable(enabled = action.enabled, role = Role.Button, onClick = action.onClick)
            .then(if (action.testTag != null) Modifier.testTag(action.testTag) else Modifier)
            .padding(horizontal = V5Spacing.space4, vertical = V5Spacing.space3),
        horizontalArrangement = Arrangement.spacedBy(V5Spacing.space2, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        action.icon?.let { icon ->
            Icon(icon, contentDescription = null, tint = colors.onAccentFill, modifier = Modifier.size(V5Sizes.iconSmall))
        }
        Text(
            action.label,
            style = V5Theme.type.body.copy(fontWeight = FontWeight.SemiBold),
            color = colors.onAccentFill,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun QuietAction(action: V5StateAction) {
    val colors = V5Theme.colors
    Box(
        Modifier
            .fillMaxWidth()
            .heightIn(min = V5Sizes.touchTarget)
            .clip(V5Theme.shapes.full)
            .clickable(role = Role.Button, onClick = action.onClick)
            .then(if (action.testTag != null) Modifier.testTag(action.testTag) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            action.label,
            style = V5Theme.type.body,
            color = if (action.destructive) colors.danger else colors.accent,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = V5Spacing.space4, vertical = V5Spacing.space3),
        )
    }
}

/** A main action that cannot run now, as on Android: faded, still read by TalkBack. */
private const val DISABLED_ALPHA = 0.38f
