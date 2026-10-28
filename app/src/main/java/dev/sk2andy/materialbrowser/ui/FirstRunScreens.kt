package dev.sk2andy.materialbrowser.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import dev.sk2andy.materialbrowser.ui.theme.VolaFirstRunTokens
/**
 * A soft glow of the accent from the top, over the theme's background (pure black in dark); the
 * content sits inside the safe area. Shared with the gesture lesson, so the whole first run looks
 * the same.
 */
@Composable
internal fun FirstRunBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .fillMaxSize()
            // The first run covers the browser: a touch that misses its controls stops here
            // instead of reaching the page or the address bar underneath.
            .pointerInput(Unit) { awaitPointerEventScope { while (true) awaitPointerEvent() } }
            .background(colors.background)
            .background(
                Brush.verticalGradient(
                    listOf(colors.primaryContainer.copy(alpha = VolaFirstRunTokens.GLOW_ALPHA), Color.Transparent),
                ),
            )
            .safeDrawingPadding(),
    ) {
        content()
    }
}

