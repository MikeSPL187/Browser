package dev.sk2andy.materialbrowser.ui

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import dev.sk2andy.materialbrowser.browser.BrowserContentFrame
import dev.sk2andy.materialbrowser.ui.theme.VolaFrame
import dev.sk2andy.materialbrowser.ui.theme.VolaSplit
import dev.sk2andy.materialbrowser.ui.theme.VolaTheme
import dev.sk2andy.materialbrowser.ui.theme.auraBrush

/**
 * A CSS blur radius is two Gaussian sigmas; Android's shadow radius maps to sigma as
 * radius × 0.57735 + 0.5. For the soft shadows used here the offset is negligible.
 */
private const val CSS_BLUR_TO_SHADOW_RADIUS = 0.866f

/**
 * Lays the page host out inside [frame], in exact pixels, so the Compose bounds match the frame
 * the engines were told about.
 */
internal fun Modifier.contentFramePadding(frame: BrowserContentFrame): Modifier =
    if (frame == BrowserContentFrame.None) {
        this
    } else {
        layout { measurable, constraints ->
            val horizontal = frame.leftPx + frame.rightPx
            val vertical = frame.topPx + frame.bottomPx
            val placeable = measurable.measure(
                Constraints(
                    minWidth = (constraints.minWidth - horizontal).coerceAtLeast(0),
                    maxWidth = (constraints.maxWidth - horizontal).coerceAtLeast(0),
                    minHeight = (constraints.minHeight - vertical).coerceAtLeast(0),
                    maxHeight = (constraints.maxHeight - vertical).coerceAtLeast(0),
                ),
            )
            val width = (placeable.width + horizontal)
                .coerceIn(constraints.minWidth, constraints.maxWidth)
            val height = (placeable.height + vertical)
                .coerceIn(constraints.minHeight, constraints.maxHeight)
            layout(width, height) {
                // Physical edges: the frame comes from window insets, not from layout direction.
                placeable.place(frame.leftPx, frame.topPx)
            }
        }
    }

/**
 * The aura around the page card, drawn over the page host. The engines render into a
 * SurfaceView, which ignores rounded clips, so the card corners are cut by painting the aura
 * over them; the same pass draws the card's hairline and soft shadow.
 *
 * [coveredBottomPx] lifts the card's bottom edge above [frame] while the expanded address bar
 * covers the page; it is read while drawing, so a moving bar only redraws the mask.
 */
@Composable
internal fun BrowserContentFrameMask(
    frame: BrowserContentFrame,
    modifier: Modifier = Modifier,
    coveredBottomPx: () -> Float = { 0f },
) {
    BrowserCardsMask(frames = listOf(frame), modifier = modifier, coveredBottomPx = coveredBottomPx)
}

/**
 * The aura around several page cards (Split View's two). The card at [highlighted] is ringed in
 * the accent instead of the hairline; [coveredBottomPx] lifts the last card's bottom edge.
 */
@Composable
internal fun BrowserCardsMask(
    frames: List<BrowserContentFrame>,
    modifier: Modifier = Modifier,
    highlighted: Int? = null,
    coveredBottomPx: () -> Float = { 0f },
) {
    val aura = VolaTheme.auraBrush
    val outlineColor = MaterialTheme.colorScheme.onSurface.copy(alpha = VolaFrame.OUTLINE_ALPHA)
    val highlightColor = MaterialTheme.colorScheme.primary
    val shadowColor = VolaFrame.shadowColor.copy(alpha = VolaFrame.SHADOW_ALPHA).toArgb()
    Canvas(modifier = modifier.graphicsLayer()) {
        val cards = frames.mapIndexedNotNull { index, frame ->
            val covered = if (index == frames.lastIndex) coveredBottomPx().coerceAtLeast(0f) else 0f
            RoundRect(
                left = frame.leftPx.toFloat(),
                top = frame.topPx.toFloat(),
                right = size.width - frame.rightPx,
                bottom = size.height - frame.bottomPx - covered,
                cornerRadius = CornerRadius(VolaFrame.pageRadius.toPx()),
            ).takeIf { card -> card.width > 0f && card.height > 0f }?.let { card -> index to card }
        }
        if (cards.isEmpty()) return@Canvas
        val cardsPath = Path().apply { cards.forEach { (_, card) -> addRoundRect(card) } }
        val surround = Path().apply {
            fillType = PathFillType.EvenOdd
            addRect(Rect(Offset.Zero, size))
            cards.forEach { (_, card) -> addRoundRect(card) }
        }
        drawPath(surround, aura)
        clipPath(cardsPath, clipOp = ClipOp.Difference) {
            cards.forEach { (index, card) ->
                drawIntoCanvas { canvas ->
                    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = shadowColor
                        setShadowLayer(
                            VolaFrame.shadowBlur.toPx() * CSS_BLUR_TO_SHADOW_RADIUS,
                            0f,
                            VolaFrame.shadowOffsetY.toPx(),
                            shadowColor,
                        )
                    }
                    canvas.nativeCanvas.drawRoundRect(
                        card.left,
                        card.top,
                        card.right,
                        card.bottom,
                        card.topLeftCornerRadius.x,
                        card.topLeftCornerRadius.y,
                        paint,
                    )
                }
                val ringed = index == highlighted
                val outline = if (ringed) {
                    VolaSplit.activeOutlineWidth.toPx()
                } else {
                    VolaFrame.outlineWidth.toPx()
                }
                drawRoundRect(
                    color = if (ringed) highlightColor else outlineColor,
                    topLeft = Offset(card.left - outline / 2f, card.top - outline / 2f),
                    size = Size(card.width + outline, card.height + outline),
                    cornerRadius = CornerRadius(card.topLeftCornerRadius.x + outline / 2f),
                    style = Stroke(width = outline),
                )
            }
        }
    }
}
