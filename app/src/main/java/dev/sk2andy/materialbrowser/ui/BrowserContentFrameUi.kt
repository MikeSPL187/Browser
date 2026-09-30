package dev.sk2andy.materialbrowser.ui

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
 */
@Composable
internal fun BrowserContentFrameMask(
    frame: BrowserContentFrame,
    modifier: Modifier = Modifier,
) {
    val aura = VolaTheme.auraBrush
    val outlineColor = MaterialTheme.colorScheme.onSurface.copy(alpha = VolaFrame.OUTLINE_ALPHA)
    val shadowColor = VolaFrame.shadowColor.copy(alpha = VolaFrame.SHADOW_ALPHA).toArgb()
    Canvas(modifier = modifier.graphicsLayer()) {
        val card = RoundRect(
            left = frame.leftPx.toFloat(),
            top = frame.topPx.toFloat(),
            right = size.width - frame.rightPx,
            bottom = size.height - frame.bottomPx,
            cornerRadius = CornerRadius(VolaFrame.pageRadius.toPx()),
        )
        if (card.width <= 0f || card.height <= 0f) return@Canvas
        val cardPath = Path().apply { addRoundRect(card) }
        val surround = Path().apply {
            fillType = PathFillType.EvenOdd
            addRect(Rect(Offset.Zero, size))
            addRoundRect(card)
        }
        drawPath(surround, aura)
        clipPath(cardPath, clipOp = ClipOp.Difference) {
            drawIntoCanvas { canvas ->
                val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
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
            val outline = VolaFrame.outlineWidth.toPx()
            drawRoundRect(
                color = outlineColor,
                topLeft = Offset(card.left - outline / 2f, card.top - outline / 2f),
                size = Size(card.width + outline, card.height + outline),
                cornerRadius = CornerRadius(card.topLeftCornerRadius.x + outline / 2f),
                style = Stroke(width = outline),
            )
        }
    }
}
