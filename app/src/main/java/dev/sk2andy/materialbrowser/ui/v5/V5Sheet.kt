package dev.sk2andy.materialbrowser.ui.v5

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animate
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Velocity
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.shared.ui.icons.VolaIcons
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Materials
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Motion
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Sizes
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Spacing
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Theme
import kotlin.coroutines.cancellation.CancellationException

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
    /** The sheet has left after [visible] turned false (a host window can close then). */
    onHidden: () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = V5Theme.colors
    val density = LocalDensity.current.density
    val dismiss by rememberUpdatedState(onDismiss)
    val hidden by rememberUpdatedState(onHidden)
    val moves = V5Motion.animationsEnabled
    // Previews show the sheet in place; the app always brings it in.
    val inspection = LocalInspectionMode.current
    val presence = remember { Animatable(if (inspection && visible) 1f else 0f) }
    // How far the finger has pulled the sheet below its rest (negative: above it).
    var dragPx by remember { mutableFloatStateOf(0f) }
    var heightPx by remember { mutableFloatStateOf(0f) }
    var backProgress by remember { mutableFloatStateOf(0f) }
    val settle: suspend (Float) -> Unit = { velocity ->
        if (V5SheetRules.dismisses(dragPx, velocity, heightPx, density)) {
            dismiss()
        } else if (moves) {
            animate(dragPx, 0f, velocity, V5Motion.sheet.spec()) { value, _ -> dragPx = value }
        } else {
            dragPx = 0f
        }
    }
    // A list inside the sheet scrolls first; pulled down from its top it moves the sheet, and pushed
    // up while the sheet is lowered it raises the sheet before it scrolls.
    val nestedScroll = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y >= 0f || dragPx <= 0f) return Offset.Zero
                val consumed = maxOf(available.y, -dragPx)
                dragPx += consumed
                return Offset(0f, consumed)
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (source != NestedScrollSource.UserInput || available.y <= 0f) return Offset.Zero
                dragPx = V5SheetRules.drag(dragPx, available.y)
                return Offset(0f, available.y)
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (dragPx == 0f) return Velocity.Zero
                settle(available.y)
                return available
            }
        }
    }
    LaunchedEffect(visible) {
        if (visible) {
            dragPx = 0f
            backProgress = 0f
        }
        presence.animateTo(
            if (visible) 1f else 0f,
            if (moves) V5Motion.sheet.spec() else V5Motion.reducedFade(),
        )
        if (!visible) hidden()
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
    val dragState = rememberDraggableState { delta -> dragPx = V5SheetRules.drag(dragPx, delta) }
    BoxWithConstraints(modifier.fillMaxSize()) {
        val maxSheetHeight = maxHeight * V5Motion.SHEET_MAX_HEIGHT_FRACTION
        // A tap outside closes the sheet; TalkBack closes it with the header's «Close» or back, so
        // the scrim is no second unlabelled control over the whole screen (the v5 walk, 15.10).
        Box(
            Modifier
                .testTag(V5SheetTestTags.Scrim)
                .clearAndSetSemantics {}
                .fillMaxSize()
                .graphicsLayer {
                    alpha = V5SheetRules.scrimAlpha(presence.value, dragPx, heightPx)
                }
                .background(colors.scrim)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    enabled = visible,
                    onClick = { dismiss() },
                ),
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
                    translationY = hidden + dragPx + back
                    // Until its height is known the sheet would show at rest for a frame.
                    alpha = if (heightPx == 0f && !inspection) 0f else if (moves) 1f else presence.value
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
                    onDragStopped = { velocity -> settle(velocity) },
                )
                .nestedScroll(nestedScroll)
                .semantics {
                    paneTitle = title
                    isTraversalGroup = true
                }
                // A name typed in a sheet stays above the keyboard, and clear of the sides sideways.
                .windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars).union(V5SafeSides))
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
 * an optional [subtitle], and «Close» — every sheet has one (section 9). A subtitle that states a
 * status may take its color ([subtitleColor]) and a small symbol before it ([subtitleIcon]).
 */
@Composable
fun V5SheetHeader(
    title: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leading: (@Composable () -> Unit)? = null,
    subtitleColor: Color? = null,
    subtitleIcon: ImageVector? = null,
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
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(V5Spacing.space1)) {
                    if (subtitleIcon != null) {
                        Icon(
                            subtitleIcon,
                            contentDescription = null,
                            tint = subtitleColor ?: colors.labelSecondary,
                            modifier = Modifier.size(V5Sizes.overviewFavicon),
                        )
                    }
                    Text(
                        subtitle,
                        style = V5Theme.type.footnote,
                        color = subtitleColor ?: colors.labelSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
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
