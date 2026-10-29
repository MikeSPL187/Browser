package dev.sk2andy.materialbrowser.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.data.AppearanceSettings
import dev.sk2andy.materialbrowser.data.BrowserAppearanceMode
import dev.sk2andy.materialbrowser.ui.theme.MaterialBrowserTheme
import dev.sk2andy.materialbrowser.ui.theme.VolaFirstRunTokens
import dev.sk2andy.materialbrowser.ui.theme.VolaGestureLessonTokens
import dev.sk2andy.materialbrowser.ui.theme.VolaPreviews
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Motion
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
private fun GestureOnboardingCelebration(
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val completionTitle = stringResource(R.string.onboarding_completion_title)
    val burstProgress = remember { Animatable(0f) }
    val exitProgress = remember { Animatable(0f) }
    var contentVisible by remember { mutableStateOf(false) }
    var finishing by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val streamProgress by if (remember { V5Motion.animationsEnabled }) {
        rememberInfiniteTransition(label = "onboarding-confetti").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(VolaGestureLessonTokens.CONFETTI_STREAM_MILLIS, easing = LinearEasing),
            ),
            label = "onboarding-confetti-stream",
        )
    } else {
        remember { mutableFloatStateOf(0f) }
    }
    LaunchedEffect(Unit) {
        burstProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(VolaGestureLessonTokens.CONFETTI_BURST_MILLIS, easing = FastOutSlowInEasing),
        )
    }
    LaunchedEffect(Unit) {
        delay(VolaGestureLessonTokens.CELEBRATION_CONTENT_DELAY_MILLIS)
        contentVisible = true
    }
    val colors = MaterialTheme.colorScheme
    val confettiColors = listOf(colors.primary, colors.tertiary, colors.secondary, colors.tertiaryContainer)

    FirstRunBackground(
        modifier = modifier
            .testTag("gesture_onboarding_celebration")
            .graphicsLayer {
                val exit = exitProgress.value
                alpha = 1f - ((exit - 0.52f) / 0.48f).coerceIn(0f, 1f)
            }
            .semantics {
                paneTitle = completionTitle
                liveRegion = LiveRegionMode.Polite
            },
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val origin = Offset(size.width / 2f, size.height / 2f)
            val maxDistance = size.maxDimension * 0.72f
            val burst = burstProgress.value
            val burstAlpha = when {
                burst < 0.72f -> 1f
                else -> ((1f - burst) / 0.28f).coerceIn(0f, 1f)
            }
            val streamAlpha = ((burst - 0.32f) / 0.38f).coerceIn(0f, 1f)

            if (burst < 1f) {
                repeat(34) { index ->
                    val angle = Math.toRadians(((index * 137.5f) % 360f).toDouble())
                    val distance = burst * maxDistance * (0.55f + (index % 7) * 0.055f)
                    val center = Offset(
                        x = origin.x + cos(angle).toFloat() * distance,
                        y = origin.y + sin(angle).toFloat() * distance +
                            burst * burst * size.height * 0.12f,
                    )
                    drawConfettiPiece(
                        index = index,
                        center = center,
                        rotation = burst * 620f + index * 29f,
                        color = confettiColors[index % confettiColors.size].copy(
                            alpha = burstAlpha,
                        ),
                    )
                }
            }

            repeat(48) { index ->
                val emissionDelay = ((index * 23) % 100) / 100f
                val age = (streamProgress - emissionDelay + 1f) % 1f
                val angle = Math.toRadians(((index * 131f + 12f) % 360f).toDouble())
                val distance = age * maxDistance * (0.48f + (index % 9) * 0.045f)
                val center = Offset(
                    x = origin.x + cos(angle).toFloat() * distance,
                    y = origin.y + sin(angle).toFloat() * distance +
                        age * age * size.height * 0.16f,
                )
                val alpha = streamAlpha * when {
                    age < 0.08f -> age / 0.08f
                    age > 0.82f -> (1f - age) / 0.18f
                    else -> 1f
                }.coerceIn(0f, 1f)
                drawConfettiPiece(
                    index = index,
                    center = center,
                    rotation = streamProgress * 720f + index * 31f,
                    color = confettiColors[index % confettiColors.size].copy(alpha = alpha),
                )
            }
        }
        AnimatedVisibility(
            visible = contentVisible,
            modifier = Modifier.fillMaxSize(),
            enter = fadeIn(tween(VolaGestureLessonTokens.CELEBRATION_ENTER_MILLIS)) +
                scaleIn(
                    initialScale = VolaGestureLessonTokens.CELEBRATION_ENTER_SCALE,
                    animationSpec = tween(
                        VolaGestureLessonTokens.CELEBRATION_ENTER_MILLIS,
                        easing = FastOutSlowInEasing,
                    ),
                ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(VolaGestureLessonTokens.celebrationPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
            Surface(
                modifier = Modifier.size(VolaGestureLessonTokens.celebrationBadgeSize),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                shadowElevation = VolaGestureLessonTokens.celebrationBadgeShadow,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(R.drawable.ic_symbol_check),
                        contentDescription = null,
                        modifier = Modifier.size(VolaGestureLessonTokens.celebrationIconSize),
                        tint = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }
            Spacer(Modifier.height(VolaGestureLessonTokens.celebrationIconGap))
            Text(
                text = completionTitle,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { heading() },
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(VolaGestureLessonTokens.celebrationTextGap))
            Text(
                text = stringResource(R.string.onboarding_completion_body),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(VolaGestureLessonTokens.celebrationButtonGap))
            Button(
                enabled = !finishing,
                onClick = {
                    if (!finishing) {
                        finishing = true
                        coroutineScope.launch {
                            exitProgress.animateTo(
                                targetValue = 1f,
                                animationSpec = tween(
                                    VolaGestureLessonTokens.CELEBRATION_EXIT_MILLIS,
                                    easing = FastOutSlowInEasing,
                                ),
                            )
                            onContinue()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = VolaFirstRunTokens.buttonHeight)
                    .testTag("gesture_onboarding_finish")
                    .graphicsLayer {
                        val exit = exitProgress.value
                        val buttonScale = when {
                            exit < 0.28f -> 1f - 0.18f * (exit / 0.28f)
                            exit < 0.58f ->
                                0.82f + 0.28f * ((exit - 0.28f) / 0.30f)
                            else -> 1.10f - 0.10f * ((exit - 0.58f) / 0.42f)
                        }
                        scaleX = buttonScale
                        scaleY = buttonScale
                    },
                // The button stays in its colors while it bounces away.
                colors = ButtonDefaults.buttonColors(
                    disabledContainerColor = MaterialTheme.colorScheme.primary,
                    disabledContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Text(
                    text = stringResource(R.string.onboarding_completion_cta),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}
}

private fun DrawScope.drawConfettiPiece(
    index: Int,
    center: Offset,
    rotation: Float,
    color: Color,
) {
    if (index % 3 == 0) {
        drawCircle(
            color = color,
            radius = VolaGestureLessonTokens.confettiDotRadius.toPx(),
            center = center,
        )
    } else {
        val width = VolaGestureLessonTokens.confettiStripWidth.toPx()
        val length = VolaGestureLessonTokens.confettiStripLength.toPx()
        rotate(degrees = rotation, pivot = center) {
            drawRect(
                color = color,
                topLeft = Offset(x = center.x - width / 2f, y = center.y - length / 2f),
                size = Size(width, length),
            )
        }
    }
}

@VolaPreviews
@Composable
private fun GestureLessonCelebrationPreview() {
    MaterialBrowserTheme(settings = AppearanceSettings(appearanceMode = BrowserAppearanceMode.System)) {
        GestureOnboardingCelebration(onContinue = {})
    }
}
