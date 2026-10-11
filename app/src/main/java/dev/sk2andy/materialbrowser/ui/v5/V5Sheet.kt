package dev.sk2andy.materialbrowser.ui.v5

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.shared.ui.icons.VolaIcons
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Materials
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Motion
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Sizes
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Spacing
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Theme
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.launch

object V5SheetTestTags {
    const val Scrim = "v5_sheet_scrim"
    const val Sheet = "v5_sheet"
    const val Close = "v5_sheet_close"
}

/**
 * A v5 sheet (system-v5.md, sections 2.4 and 3.3): it rises from the bottom over a scrim on the
 * sheet spring, in the thick material with `xl` top corners and a handle. Dragged down past a
 * quarter of its height, flung down, tapped outside or sent back (predictive back lowers it first),
 * it asks [onDismiss]; the caller then sets [visible] to false and the sheet leaves from where it
 * is. Without system animations it only fades. TalkBack announces [title] as the pane.
 *
 * It sits over the live page, so the material takes its opaque fallback: blur only ever covers
 * Vola's own pixels (section 2.3).
 */
@Composable
fun V5Sheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = V5Theme.colors
    val density = LocalDensity.current.density
    val scope = rememberCoroutineScope()
    val dismiss by rememberUpdatedState(onDismiss)
    val moves = V5Motion.animationsEnabled
    val presence = remember { Animatable(0f) }
    val offset = remember { Animatable(0f) }
    var heightPx by remember { mutableFloatStateOf(0f) }
    var backProgress by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(visible) {
        if (visible) {
            offset.snapTo(0f)
            backProgress = 0f
        }
        presence.animateTo(
            if (visible) 1f else 0f,
            if (moves) V5Motion.sheet.spec() else V5Motion.reducedFade(),
        )
    }
    PredictiveBackHandler(enabled = visible) { events ->
        try {
            events.collect { backProgress = it.progress }
            dismiss()
        } catch (cancelled: CancellationException) {
            backProgress = 0f
            throw cancelled
        }
    }
    if (!visible && presence.value == 0f) return
    val dragState = rememberDraggableState { delta ->
        scope.launch { offset.snapTo(V5SheetRules.drag(offset.value, delta)) }
    }
    BoxWithConstraints(modifier.fillMaxSize()) {
        val maxSheetHeight = maxHeight * V5Motion.SHEET_MAX_HEIGHT_FRACTION
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = V5SheetRules.scrimAlpha(presence.value, offset.value, heightPx)
                }
                .background(colors.scrim)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    enabled = visible,
                    onClick = { dismiss() },
                )
                .testTag(V5SheetTestTags.Scrim),
        )
        val shape = V5Theme.shapes.sheet
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .heightIn(max = maxSheetHeight)
                .onSizeChanged { heightPx = it.height.toFloat() }
                .graphicsLayer {
                    val hidden = if (moves) (1f - presence.value) * heightPx else 0f
                    val back = backProgress * V5Motion.SHEET_BACK_TRAVEL_DP * density
                    translationY = hidden + offset.value + back
                    // Until its height is known the sheet would show at rest for a frame.
                    alpha = if (heightPx == 0f) 0f else if (moves) 1f else presence.value
                }
                .clip(shape)
                .background(
                    V5Materials.thick.tint(dark = colors.isDark, blurAvailable = false, reduceTransparency = false),
                    shape,
                )
                .border(V5Spacing.hairline, colors.edge, shape)
                .draggable(
                    state = dragState,
                    orientation = Orientation.Vertical,
                    enabled = visible,
                    onDragStopped = { velocity ->
                        if (V5SheetRules.dismisses(offset.value, velocity, heightPx, density)) {
                            dismiss()
                        } else if (moves) {
                            offset.animateTo(0f, V5Motion.sheet.spec(), initialVelocity = velocity)
                        } else {
                            offset.snapTo(0f)
                        }
                    },
                )
                .semantics {
                    paneTitle = title
                    isTraversalGroup = true
                }
                .navigationBarsPadding()
                .testTag(V5SheetTestTags.Sheet),
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(top = V5Sizes.sheetHandleTop),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier
                        .size(V5Sizes.sheetHandleWidth, V5Sizes.sheetHandleHeight)
                        .clip(V5Theme.shapes.full)
                        .background(colors.labelTertiary),
                )
            }
            content()
        }
    }
}

/**
 * The top of a sheet: an optional [leading] glyph, the title (a heading, where TalkBack starts) with
 * an optional [subtitle], and «Close» — every sheet has one (section 9).
 */
@Composable
fun V5SheetHeader(
    title: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leading: (@Composable () -> Unit)? = null,
) {
    val colors = V5Theme.colors
    Row(
        modifier
            .fillMaxWidth()
            .padding(start = V5Spacing.space4, end = V5Spacing.space1, top = V5Spacing.space2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) {
            leading()
            Spacer(Modifier.size(V5Spacing.space3))
        }
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = V5Theme.type.headline,
                color = colors.label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.semantics { heading() },
            )
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = V5Theme.type.footnote,
                    color = colors.labelSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        IconButton(onClick = onClose, modifier = Modifier.testTag(V5SheetTestTags.Close)) {
            Box(
                Modifier
                    .size(V5Sizes.overviewClosePlate)
                    .clip(V5Theme.shapes.full)
                    .background(colors.fill),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    VolaIcons.Close,
                    contentDescription = stringResource(R.string.v5_sheet_close),
                    tint = colors.labelSecondary,
                    modifier = Modifier.size(V5Sizes.overviewFavicon),
                )
            }
        }
    }
}
