package dev.sk2andy.materialbrowser.ui

import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import dev.sk2andy.materialbrowser.ui.theme.VolaTabOverview
import dev.sk2andy.materialbrowser.ui.theme.CandyMotionScheme
import dev.sk2andy.materialbrowser.ui.theme.VolaCompactMode
import dev.sk2andy.materialbrowser.ui.theme.VolaIsland
import kotlin.math.PI
import kotlin.math.sin

internal object AddressBarMotion {
    /** The island becomes the tab overview's new-tab button: same size, same place. */
    val OVERVIEW_WIDTH = VolaTabOverview.newTabButtonSize
    val EXPANDED_HEIGHT = 56.dp
    const val DOCK_REPOSITION_FEEDBACK_MILLIS = 260

    fun containerAnimationSpec(motionScheme: CandyMotionScheme): SpringSpec<Dp> =
        spring(
            dampingRatio = motionScheme.addressBarContainerDampingRatio,
            stiffness = motionScheme.addressBarContainerStiffness,
        )

    fun dockProgressAnimationSpec(motionScheme: CandyMotionScheme): SpringSpec<Float> =
        spring(
            dampingRatio = motionScheme.addressBarDockDampingRatio,
            stiffness = motionScheme.addressBarDockStiffness,
        )

    fun dockBreakawayAnimationSpec(motionScheme: CandyMotionScheme): SpringSpec<Float> =
        spring(
            dampingRatio = motionScheme.addressBarBreakawayDampingRatio,
            stiffness = motionScheme.addressBarBreakawayStiffness,
        )

    fun dockRepositionFeedbackScale(progress: Float): Offset {
        val boundedProgress = progress
            .takeIf(Float::isFinite)
            ?.coerceIn(0f, 1f)
            ?: 1f
        val wobble = sin(boundedProgress * PI.toFloat() * 3f) * (1f - boundedProgress)
        return Offset(
            x = 1f + wobble * 0.06f,
            y = 1f - wobble * 0.09f,
        )
    }

    fun widthTarget(
        presentation: AddressBarPresentation,
        compactWidth: Dp,
        maxWidth: Dp,
        feedbackWidth: Dp,
        edgeTabWidth: Dp,
        handle: Boolean = false,
    ): Dp = if (handle && presentation == AddressBarPresentation.Compact) {
        VolaCompactMode.handleWidth
    } else when (presentation) {
        AddressBarPresentation.Docked -> edgeTabWidth.coerceAtMost(maxWidth)
        AddressBarPresentation.Compact -> compactWidth
            .coerceAtLeast(96.dp.coerceAtMost(maxWidth))
            .coerceAtMost(maxWidth)
        AddressBarPresentation.Expanded -> maxWidth
        AddressBarPresentation.Overview -> OVERVIEW_WIDTH.coerceAtMost(maxWidth)
        AddressBarPresentation.CommandFeedback -> feedbackWidth
            .coerceAtLeast(160.dp.coerceAtMost(maxWidth))
            .coerceAtMost(maxWidth)
    }

    fun heightTarget(
        presentation: AddressBarPresentation,
        expandedHeight: Dp = EXPANDED_HEIGHT,
        handle: Boolean = false,
    ): Dp = if (handle && presentation == AddressBarPresentation.Compact) {
        VolaCompactMode.handleHeight
    } else when (presentation) {
        AddressBarPresentation.Docked -> 48.dp
        AddressBarPresentation.Compact -> VolaIsland.compactHeight
        AddressBarPresentation.Expanded -> expandedHeight
        AddressBarPresentation.Overview -> VolaTabOverview.newTabButtonSize
        AddressBarPresentation.CommandFeedback -> 46.dp
    }

    /**
     * How far right of center the island sits as the tab overview's new-tab button: that button
     * ends [VolaTabOverview.dockSideMargin] from the screen edge, the island's row ends
     * [ADDRESS_BAR_HORIZONTAL_MARGIN] from it.
     */
    fun overviewOffsetX(maxWidth: Dp): Dp =
        ((maxWidth - OVERVIEW_WIDTH) / 2f + ADDRESS_BAR_HORIZONTAL_MARGIN -
            VolaTabOverview.dockSideMargin).coerceAtLeast(0.dp)

    fun dockOffsetForPosition(
        position: Offset,
        maxWidth: Dp,
        barWidth: Dp,
        verticalTravel: Dp,
    ): DpOffset {
        val horizontalPosition = position.x
            .takeIf(Float::isFinite)
            ?.coerceIn(-1f, 1f)
            ?: 0f
        val verticalPosition = position.y
            .takeIf(Float::isFinite)
            ?.coerceIn(0f, 1f)
            ?: 0f
        val horizontalTravel = ((maxWidth - barWidth) / 2f + 12.dp).coerceAtLeast(0.dp)
        return DpOffset(
            x = horizontalTravel * horizontalPosition,
            y = if (verticalPosition == 0f) {
                0.dp
            } else {
                -verticalTravel.coerceAtLeast(0.dp) * verticalPosition
            },
        )
    }

    fun usesFadeThrough(
        initial: AddressBarPresentation,
        target: AddressBarPresentation,
    ): Boolean =
        initial == AddressBarPresentation.Compact && target == AddressBarPresentation.Expanded ||
            initial == AddressBarPresentation.Expanded && target == AddressBarPresentation.Compact ||
            initial == AddressBarPresentation.Overview ||
            target == AddressBarPresentation.Overview

    fun exitDurationMillis(
        initial: AddressBarPresentation,
        target: AddressBarPresentation,
        motionScheme: CandyMotionScheme,
    ): Int = if (usesFadeThrough(initial, target)) {
        motionScheme.addressBarFadeThroughExitMillis
    } else {
        motionScheme.addressBarQuickFadeOutMillis
    }

    fun enterDurationMillis(
        initial: AddressBarPresentation,
        target: AddressBarPresentation,
        motionScheme: CandyMotionScheme,
    ): Int = if (usesFadeThrough(initial, target)) {
        motionScheme.addressBarFadeThroughEnterMillis
    } else {
        motionScheme.addressBarQuickFadeInMillis
    }
}

