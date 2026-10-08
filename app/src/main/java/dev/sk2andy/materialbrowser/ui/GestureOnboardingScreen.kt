package dev.sk2andy.materialbrowser.ui

import dev.sk2andy.materialbrowser.shared.ui.TabDismissPhysics

import android.view.HapticFeedbackConstants
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.data.AppearanceSettings
import dev.sk2andy.materialbrowser.data.BrowserAppearanceMode
import dev.sk2andy.materialbrowser.ui.theme.MaterialBrowserTheme
import dev.sk2andy.materialbrowser.ui.theme.VolaFirstRunTokens
import dev.sk2andy.materialbrowser.ui.theme.VolaGestureLessonTokens
import dev.sk2andy.materialbrowser.ui.theme.VolaPreviews
import kotlin.math.absoluteValue
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal enum class GestureOnboardingStep {
    SwitchTabs,
    OpenTabOverview,
    CloseTab,
}

internal object GestureOnboardingRules {
    /** What the lesson teaches; beside a tab strip tabs are switched there, not on the address bar. */
    fun steps(usesTabStrip: Boolean): List<GestureOnboardingStep> = if (usesTabStrip) {
        GestureOnboardingStep.entries.filterNot { it == GestureOnboardingStep.SwitchTabs }
    } else {
        GestureOnboardingStep.entries
    }

    fun isCompleted(
        step: GestureOnboardingStep,
        dragX: Float,
        dragY: Float,
        threshold: Float,
    ): Boolean {
        if (threshold <= 0f) return false
        return when (step) {
            GestureOnboardingStep.SwitchTabs ->
                dragX.absoluteValue >= threshold && dragX.absoluteValue > dragY.absoluteValue
            GestureOnboardingStep.OpenTabOverview,
            GestureOnboardingStep.CloseTab,
            -> dragY <= -threshold && dragY.absoluteValue > dragX.absoluteValue
        }
    }

    /** The practice card takes the height left over, between its smallest and largest size. */
    fun practiceHeight(available: Int, min: Int, max: Int): Int = available.coerceIn(min, max)

    /** A phone on its side has no room for the copy above the card: they go side by side. */
    fun placesPracticeBeside(width: Float, height: Float, stackedMinHeight: Float): Boolean =
        width > height && height < stackedMinHeight
}

@Composable
internal fun GestureOnboardingScreen(
    onCompleted: () -> Unit,
    modifier: Modifier = Modifier,
    /** «Back» in the lesson; on its own it skips, like the Skip button. */
    onBack: () -> Unit = onCompleted,
    /** The lesson's own welcome page; the first run already said hello and skips it. */
    showWelcome: Boolean = true,
) {
    val wideTabStripEnabled = AddressBarWideLayoutRules.usesTabStrip(
        LocalConfiguration.current.screenWidthDp.toFloat(),
    )
    val steps = GestureOnboardingRules.steps(usesTabStrip = wideTabStripEnabled)
    var welcomeVisible by rememberSaveable { mutableStateOf(showWelcome) }
    var celebrationVisible by rememberSaveable { mutableStateOf(false) }
    var stepIndex by rememberSaveable { mutableIntStateOf(0) }
    var dragX by remember { mutableFloatStateOf(0f) }
    var dragY by remember { mutableFloatStateOf(0f) }
    var practiceWidthPx by remember { mutableFloatStateOf(0f) }
    var rootWidthPx by remember { mutableFloatStateOf(0f) }
    val lessonScrollState = rememberScrollState()
    val currentStepIndex = stepIndex.coerceIn(0, steps.lastIndex)
    val step = steps[currentStepIndex]
    val density = LocalDensity.current
    val threshold = with(density) {
        when (step) {
            GestureOnboardingStep.SwitchTabs ->
                if (rootWidthPx > 0f) {
                    rootWidthPx * AddressBarTabSwitchRules.DISTANCE_FRACTION
                } else {
                    VolaGestureLessonTokens.switchFallbackThreshold.toPx()
                }
            GestureOnboardingStep.OpenTabOverview ->
                AddressBarGestureRules.OPEN_TABS_THRESHOLD_DP.dp.toPx()
            GestureOnboardingStep.CloseTab ->
                if (practiceWidthPx > 0f) {
                    ((practiceWidthPx / 0.53f) * 0.28f) *
                        TabDismissPhysics.DEFAULT_RESISTANCE_FRACTION
                } else {
                    VolaGestureLessonTokens.closeFallbackThreshold.toPx()
                }
        }
    }
    val view = LocalView.current
    val stepAccessibilityDescription = stepDescription(step)
    val completeActionLabel = stringResource(R.string.onboarding_accessibility_complete_action)

    BackHandler(onBack = onBack)
    LaunchedEffect(welcomeVisible, celebrationVisible, step) {
        if (!welcomeVisible) lessonScrollState.scrollTo(0)
    }

    fun completeStep() {
        dragX = 0f
        dragY = 0f
        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        if (currentStepIndex == steps.lastIndex) {
            celebrationVisible = true
        } else {
            stepIndex = currentStepIndex + 1
        }
    }

    fun isCompletionKey(key: Key): Boolean = when (step) {
        GestureOnboardingStep.SwitchTabs ->
            key == Key.DirectionLeft || key == Key.DirectionRight
        GestureOnboardingStep.OpenTabOverview,
        GestureOnboardingStep.CloseTab,
        -> key == Key.DirectionUp
    }

    val currentStep by rememberUpdatedState(step)
    val currentThreshold by rememberUpdatedState(threshold)
    val currentCompleteStep by rememberUpdatedState { completeStep() }

    val gestureModifier = Modifier
        .testTag("gesture_onboarding_${step.name}")
        .onSizeChanged {
            practiceWidthPx = it.width.toFloat()
        }
        .semantics {
            contentDescription = stepAccessibilityDescription
            customActions = listOf(
                CustomAccessibilityAction(completeActionLabel) {
                    completeStep()
                    true
                },
            )
        }
        .onPreviewKeyEvent { event ->
            val handlesEvent = isCompletionKey(event.key)
            if (handlesEvent && event.type == KeyEventType.KeyUp) completeStep()
            handlesEvent
        }
        .focusable()
        .pointerInput(Unit) {
            awaitEachGesture {
                val down = awaitFirstDown(
                    requireUnconsumed = false,
                    pass = PointerEventPass.Initial,
                )
                down.consume()
                dragX = 0f
                dragY = 0f
                var lastPosition = down.position
                var released = false
                while (true) {
                    val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                    val delta = change.position - lastPosition
                    dragX += delta.x
                    dragY += delta.y
                    lastPosition = change.position
                    change.consume()
                    if (!change.pressed) {
                        released = true
                        break
                    }
                }
                if (released) {
                    if (
                        GestureOnboardingRules.isCompleted(
                            currentStep,
                            dragX,
                            dragY,
                            currentThreshold,
                        )
                    ) {
                        currentCompleteStep()
                    } else {
                        dragX = 0f
                        dragY = 0f
                    }
                } else {
                    dragX = 0f
                    dragY = 0f
                }
            }
        }

    Surface(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { rootWidthPx = it.width.toFloat() }
            .testTag("gesture_onboarding"),
        color = MaterialTheme.colorScheme.background,
    ) {
        if (welcomeVisible) {
            GestureOnboardingWelcome(
                steps = steps,
                onStart = { welcomeVisible = false },
                onSkip = onCompleted,
            )
        } else if (celebrationVisible) {
            GestureOnboardingCelebration(onContinue = onCompleted)
        } else {
            FirstRunBackground {
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val copy = @Composable {
                        LessonCopy(step = step, stepNumber = currentStepIndex + 1, stepCount = steps.size, onSkip = onCompleted)
                    }
                    val practice = @Composable {
                        PracticeCard(step = step, dragX = dragX, dragY = dragY, gestureModifier = gestureModifier)
                    }
                    val hints = @Composable { LessonHints(step = step) }
                    val beside = GestureOnboardingRules.placesPracticeBeside(
                        width = maxWidth.value,
                        height = maxHeight.value,
                        stackedMinHeight = VolaGestureLessonTokens.stackedMinHeight.value,
                    )
                    if (beside) {
                        Row(
                            modifier = Modifier.fillMaxSize().padding(VolaGestureLessonTokens.screenPadding),
                            horizontalArrangement = Arrangement.spacedBy(VolaGestureLessonTokens.besideGap),
                        ) {
                            Column(
                                modifier = Modifier.weight(1f).fillMaxHeight().verticalScroll(lessonScrollState),
                                verticalArrangement = Arrangement.Center,
                            ) {
                                copy()
                                hints()
                            }
                            Box(modifier = Modifier.weight(1f).fillMaxHeight()) { practice() }
                        }
                    } else {
                        StackedLesson(
                            copy = copy,
                            practice = practice,
                            hints = hints,
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(lessonScrollState)
                                .padding(VolaGestureLessonTokens.screenPadding),
                        )
                    }
                }
            }
        }
    }
}

/**
 * The lesson in a column: the copy, the practice card, the hints. The card takes the height the
 * screen has left, between its smallest and largest size, so the pretend address bar stays in
 * view; only when even the smallest card does not fit does the column scroll.
 */
@Composable
private fun StackedLesson(
    copy: @Composable () -> Unit,
    practice: @Composable () -> Unit,
    hints: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Layout(contents = listOf(copy, practice, hints), modifier = modifier) { (copyParts, practiceParts, hintParts), constraints ->
        val free = constraints.copy(minWidth = 0, minHeight = 0, maxHeight = Constraints.Infinity)
        val top = copyParts.map { it.measure(free) }
        val bottom = hintParts.map { it.measure(free) }
        val used = top.sumOf { it.height } + bottom.sumOf { it.height }
        val cardHeight = GestureOnboardingRules.practiceHeight(
            available = constraints.minHeight - used,
            min = VolaGestureLessonTokens.practiceMinHeight.roundToPx(),
            max = VolaGestureLessonTokens.practiceMaxHeight.roundToPx(),
        )
        val card = practiceParts.map { it.measure(free.copy(minHeight = cardHeight, maxHeight = cardHeight)) }
        val content = used + cardHeight
        val height = maxOf(constraints.minHeight, content)
        layout(constraints.maxWidth, height) {
            var y = (height - content) / 2
            (top + card + bottom).forEach { placeable ->
                placeable.placeRelative(0, y)
                y += placeable.height
            }
        }
    }
}

/** The title row with the step count and Skip, the progress line, and what to do in this step. */
@Composable
private fun LessonCopy(step: GestureOnboardingStep, stepNumber: Int, stepCount: Int, onSkip: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // The title takes what is left and wraps: with a large font it used to take the whole
            // row and leave Skip no room at all, so the lesson could not be skipped.
            Text(
                text = stringResource(R.string.onboarding_title),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(
                        R.string.onboarding_progress,
                        stepNumber,
                        stepCount,
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(
                    onClick = onSkip,
                    modifier = Modifier.testTag("gesture_onboarding_skip"),
                ) {
                    Text(
                        text = stringResource(R.string.onboarding_skip),
                        color = gestureAccent(step),
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
        Spacer(Modifier.height(VolaGestureLessonTokens.progressGap))
        LinearProgressIndicator(
            progress = { stepNumber.toFloat() / stepCount },
            modifier = Modifier
                .fillMaxWidth()
                .height(VolaGestureLessonTokens.progressHeight)
                .clip(CircleShape),
            color = gestureAccent(step),
            trackColor = gestureAccent(step).copy(alpha = VolaGestureLessonTokens.PROGRESS_TRACK_ALPHA),
        )
        Spacer(Modifier.height(VolaGestureLessonTokens.headerGap))
        AnimatedContent(
            targetState = step,
            transitionSpec = {
                fadeIn(tween(VolaGestureLessonTokens.COPY_FADE_IN_MILLIS)) togetherWith
                    fadeOut(tween(VolaGestureLessonTokens.COPY_FADE_OUT_MILLIS))
            },
            label = "gesture-onboarding-copy",
        ) { currentStep ->
            Column {
                Text(
                    text = stepTitle(currentStep),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(VolaGestureLessonTokens.copyGap))
                Text(
                    text = stepDescription(currentStep),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(VolaGestureLessonTokens.practiceGap))
    }
}

/** The card the gesture is practised on; [gestureModifier] makes its target listen. */
@Composable
private fun PracticeCard(step: GestureOnboardingStep, dragX: Float, dragY: Float, gestureModifier: Modifier) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        shape = VolaGestureLessonTokens.practiceShape,
        color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = VolaGestureLessonTokens.PRACTICE_ALPHA),
        tonalElevation = VolaGestureLessonTokens.practiceElevation,
        shadowElevation = VolaGestureLessonTokens.practiceElevation,
    ) {
        GesturePracticeArea(
            step = step,
            dragX = dragX,
            dragY = dragY,
            modifier = gestureModifier,
        )
    }
}

/** Under the card: which way to swipe, and the way out. */
@Composable
private fun LessonHints(step: GestureOnboardingStep) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Spacer(Modifier.height(VolaGestureLessonTokens.badgeGap))
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            GestureDirectionBadge(step = step)
        }
        Spacer(Modifier.height(VolaGestureLessonTokens.hintGap))
        Text(
            text = stringResource(R.string.onboarding_follow_pointer),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelLarge,
            color = gestureAccent(step),
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(VolaGestureLessonTokens.footerGap))
        Text(
            text = stringResource(R.string.onboarding_required_hint),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun GestureOnboardingWelcome(
    /** The steps this screen will teach; the tab switch is left out beside a tab strip. */
    steps: List<GestureOnboardingStep>,
    onStart: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FirstRunBackground(modifier = modifier.testTag("gesture_onboarding_welcome")) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(VolaGestureLessonTokens.welcomePadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
        LessonWelcomeHero()
        Spacer(Modifier.height(VolaGestureLessonTokens.welcomeHeroGap))
        Text(
            text = stringResource(R.string.onboarding_welcome_title),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(VolaGestureLessonTokens.welcomeTextGap))
        Text(
            text = stringResource(R.string.onboarding_welcome_body),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(VolaGestureLessonTokens.welcomeSectionGap))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = VolaGestureLessonTokens.welcomeCardShape,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(
                    alpha = VolaGestureLessonTokens.WELCOME_CARD_ALPHA,
                ),
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = VolaGestureLessonTokens.welcomeCardElevation),
        ) {
            Column(modifier = Modifier.padding(VolaGestureLessonTokens.welcomeCardPadding)) {
                Text(
                    text = pluralStringResource(R.plurals.onboarding_welcome_card_title, steps.size, steps.size),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(VolaGestureLessonTokens.welcomeCardTitleGap))
                steps.forEach { step -> WelcomeGestureRow(step) }
            }
        }
        Spacer(Modifier.height(VolaGestureLessonTokens.welcomeSectionGap))
        Button(
            onClick = onStart,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = VolaFirstRunTokens.buttonHeight)
                .testTag("gesture_onboarding_start"),
        ) {
            Text(
                text = stringResource(R.string.onboarding_welcome_start),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }
            Spacer(Modifier.height(VolaGestureLessonTokens.welcomeButtonGap))
        }
        TextButton(
            onClick = onSkip,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(VolaGestureLessonTokens.skipPadding)
                .testTag("gesture_onboarding_skip"),
        ) {
            Text(
                text = stringResource(R.string.onboarding_skip),
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

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
    val confettiTransition = rememberInfiniteTransition(label = "onboarding-confetti")
    val streamProgress by confettiTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(VolaGestureLessonTokens.CONFETTI_STREAM_MILLIS, easing = LinearEasing),
        ),
        label = "onboarding-confetti-stream",
    )
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

/** The lesson's mark, floating between two soft shapes in the accent colors. */
@Composable
private fun LessonWelcomeHero(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "lesson-welcome-hero")
    val floatOffset by transition.animateFloat(
        initialValue = -VolaGestureLessonTokens.HERO_FLOAT_PX,
        targetValue = VolaGestureLessonTokens.HERO_FLOAT_PX,
        animationSpec = infiniteRepeatable(
            animation = tween(VolaGestureLessonTokens.HERO_FLOAT_MILLIS, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "lesson-welcome-float",
    )
    val colors = MaterialTheme.colorScheme

    Box(
        modifier = modifier.size(VolaGestureLessonTokens.heroSize),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = VolaGestureLessonTokens.heroTileOffsetX, y = VolaGestureLessonTokens.heroTileOffsetY)
                .size(VolaGestureLessonTokens.heroTileSize)
                .graphicsLayer { rotationZ = VolaGestureLessonTokens.HERO_TILE_ROTATION + floatOffset }
                .clip(VolaGestureLessonTokens.heroTileShape)
                .background(colors.tertiary.copy(alpha = VolaGestureLessonTokens.HERO_SHAPE_ALPHA)),
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = VolaGestureLessonTokens.heroDotOffsetX, y = VolaGestureLessonTokens.heroDotOffsetY)
                .size(VolaGestureLessonTokens.heroDotSize)
                .clip(CircleShape)
                .background(colors.primary.copy(alpha = VolaGestureLessonTokens.HERO_SHAPE_ALPHA)),
        )
        Surface(
            modifier = Modifier
                .size(VolaGestureLessonTokens.heroDiscSize)
                .graphicsLayer {
                    translationY = floatOffset
                    rotationZ = floatOffset * VolaGestureLessonTokens.HERO_FLOAT_TILT
                },
            shape = CircleShape,
            color = colors.surface,
            tonalElevation = VolaGestureLessonTokens.heroDiscElevation,
            shadowElevation = VolaGestureLessonTokens.heroDiscShadow,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(VolaGestureLessonTokens.heroDiscInset)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                colors.tertiary.copy(alpha = VolaGestureLessonTokens.HERO_GLOW_START_ALPHA),
                                colors.primary.copy(alpha = VolaGestureLessonTokens.HERO_GLOW_END_ALPHA),
                            ),
                        ),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_launcher_foreground_art),
                    contentDescription = null,
                    modifier = Modifier.size(VolaGestureLessonTokens.heroMarkSize),
                )
            }
        }
    }
}

@Composable
private fun WelcomeGestureRow(step: GestureOnboardingStep) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = VolaGestureLessonTokens.rowPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier = Modifier.size(VolaGestureLessonTokens.rowIconSize),
            shape = CircleShape,
            color = gestureAccent(step),
            contentColor = gestureOnAccent(step),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = directionSymbol(step),
                    modifier = Modifier.offset(y = -VolaGestureLessonTokens.rowSymbolLift),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        Spacer(Modifier.width(VolaGestureLessonTokens.rowGap))
        Text(
            text = stepTitle(step),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun GesturePracticeArea(
    step: GestureOnboardingStep,
    dragX: Float,
    dragY: Float,
    modifier: Modifier,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(VolaGestureLessonTokens.practiceInset)
            .clip(VolaGestureLessonTokens.practiceInnerShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        contentAlignment = Alignment.Center,
    ) {
        when (step) {
            GestureOnboardingStep.SwitchTabs,
            GestureOnboardingStep.OpenTabOverview,
            -> AddressBarGesturePractice(step, dragX, dragY, modifier)
            GestureOnboardingStep.CloseTab ->
                CloseTabPractice(dragY, modifier)
        }
        GesturePointerGuide(
            step = step,
            userDragging = dragX.absoluteValue > 1f || dragY.absoluteValue > 1f,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun GesturePointerGuide(
    step: GestureOnboardingStep,
    userDragging: Boolean,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "gesture-pointer-${step.name}")
    val loopProgress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(VolaGestureLessonTokens.POINTER_LOOP_MILLIS, easing = LinearEasing),
        ),
        label = "gesture-pointer-progress",
    )
    val primary = gestureAccent(step)
    val surface = MaterialTheme.colorScheme.surface
    Canvas(
        modifier = modifier
            .testTag("gesture_onboarding_pointer_${step.name}"),
    ) {
        val (start, end) = when (step) {
            GestureOnboardingStep.SwitchTabs ->
                Offset(size.width * 0.74f, size.height * 0.84f) to
                    Offset(size.width * 0.28f, size.height * 0.84f)
            GestureOnboardingStep.OpenTabOverview ->
                Offset(size.width * 0.68f, size.height * 0.84f) to
                    Offset(size.width * 0.68f, size.height * 0.55f)
            GestureOnboardingStep.CloseTab ->
                Offset(size.width * 0.58f, size.height * 0.58f) to
                    Offset(size.width * 0.58f, size.height * 0.18f)
        }
        val rawTravel = ((loopProgress - 0.16f) / 0.62f).coerceIn(0f, 1f)
        val travel = FastOutSlowInEasing.transform(rawTravel)
        val loopAlpha = when {
            loopProgress < 0.12f -> loopProgress / 0.12f
            loopProgress > 0.86f -> (1f - loopProgress) / 0.14f
            else -> 1f
        }.coerceIn(0f, 1f)
        val guideAlpha = if (userDragging) 0f else loopAlpha
        val pointer = Offset(
            x = start.x + (end.x - start.x) * travel,
            y = start.y + (end.y - start.y) * travel,
        )
        drawLine(
            color = primary.copy(alpha = VolaGestureLessonTokens.POINTER_TRAIL_ALPHA * guideAlpha),
            start = start,
            end = end,
            strokeWidth = VolaGestureLessonTokens.pointerTrailWidth.toPx(),
            pathEffect = PathEffect.dashPathEffect(
                intervals = floatArrayOf(
                    VolaGestureLessonTokens.pointerDash.toPx(),
                    VolaGestureLessonTokens.pointerDashGap.toPx(),
                ),
                phase = -loopProgress * VolaGestureLessonTokens.pointerDashTravel.toPx(),
            ),
        )
        drawCircle(
            color = primary.copy(alpha = VolaGestureLessonTokens.POINTER_HALO_ALPHA * guideAlpha),
            radius = VolaGestureLessonTokens.pointerHaloRadius.toPx() +
                VolaGestureLessonTokens.pointerHaloGrowth.toPx() * (1f - rawTravel),
            center = pointer,
        )
        drawCircle(
            color = surface.copy(alpha = guideAlpha),
            radius = VolaGestureLessonTokens.pointerRadius.toPx(),
            center = pointer,
        )
        drawCircle(
            color = primary.copy(alpha = guideAlpha),
            radius = VolaGestureLessonTokens.pointerRadius.toPx(),
            center = pointer,
            style = Stroke(width = VolaGestureLessonTokens.pointerRing.toPx()),
        )
        drawCircle(
            color = primary.copy(alpha = guideAlpha),
            radius = VolaGestureLessonTokens.pointerDot.toPx(),
            center = pointer,
        )
    }
}

@Composable
private fun AddressBarGesturePractice(
    step: GestureOnboardingStep,
    dragX: Float,
    dragY: Float,
    modifier: Modifier,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.primaryContainer,
                        MaterialTheme.colorScheme.tertiaryContainer,
                    ),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (step == GestureOnboardingStep.SwitchTabs) {
            FakeBrowserPage(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.tertiaryContainer),
                accent = MaterialTheme.colorScheme.tertiary,
            )
        } else {
            MiniTabCard(modifier = Modifier.width(VolaGestureLessonTokens.tabCardWidth))
        }
        FakeBrowserPage(
            modifier = Modifier
                .fillMaxSize()
                .offset {
                    if (step == GestureOnboardingStep.SwitchTabs) {
                        IntOffset((dragX * 0.55f).roundToInt(), 0)
                    } else {
                        IntOffset(0, (dragY.coerceAtMost(0f) * 0.58f).roundToInt())
                    }
                },
            accent = MaterialTheme.colorScheme.primary,
            addressBarModifier = modifier,
        )
    }
}

@Composable
private fun CloseTabPractice(dragY: Float, modifier: Modifier) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.primaryContainer,
                        MaterialTheme.colorScheme.tertiaryContainer,
                    ),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        MiniTabCard(
            modifier = modifier
                .width(VolaGestureLessonTokens.closeTabCardWidth)
                .offset { IntOffset(0, (dragY.coerceAtMost(0f) * 0.75f).roundToInt()) }
                .graphicsLayer {
                    alpha = (1f - (-dragY.coerceAtMost(0f) / 320f))
                        .coerceIn(VolaGestureLessonTokens.CLOSE_CARD_MIN_ALPHA, 1f)
                },
        )
    }
}

@Composable
private fun FakeBrowserPage(
    modifier: Modifier,
    accent: Color,
    showAddressBar: Boolean = true,
    addressBarModifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = VolaGestureLessonTokens.pageShape,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(modifier = Modifier.padding(VolaGestureLessonTokens.pagePadding)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.58f)
                    .height(VolaGestureLessonTokens.pageTitleHeight)
                    .background(
                        accent.copy(alpha = VolaGestureLessonTokens.PAGE_TITLE_ALPHA),
                        VolaGestureLessonTokens.pageTitleShape,
                    ),
            )
            Spacer(Modifier.height(VolaGestureLessonTokens.pagePadding))
            repeat(VolaGestureLessonTokens.PAGE_LINES) { index ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth(if (index == VolaGestureLessonTokens.PAGE_LINES - 1) 0.64f else 1f)
                        .height(VolaGestureLessonTokens.pageLineHeight)
                        .background(
                            MaterialTheme.colorScheme.onSurface.copy(alpha = VolaGestureLessonTokens.PAGE_LINE_ALPHA),
                            VolaGestureLessonTokens.pageLineShape,
                        ),
                )
                Spacer(Modifier.height(VolaGestureLessonTokens.pageLineHeight))
            }
            Spacer(Modifier.weight(1f))
            if (showAddressBar) FakeAddressBar(addressBarModifier)
        }
    }
}

/** The address on the lesson's pretend page: a plain domain, the same in every language. */
private const val LESSON_PAGE_HOST = "wikipedia.org"

@Composable
private fun FakeAddressBar(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(VolaGestureLessonTokens.addressBarHeight),
        shape = VolaGestureLessonTokens.addressBarShape,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        tonalElevation = VolaGestureLessonTokens.addressBarElevation,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = LESSON_PAGE_HOST,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun MiniTabCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.heightIn(max = VolaGestureLessonTokens.tabCardHeight).fillMaxHeight(),
        shape = VolaGestureLessonTokens.tabCardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = VolaGestureLessonTokens.tabCardElevation),
    ) {
        Column(modifier = Modifier.padding(VolaGestureLessonTokens.tabCardPadding)) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(VolaGestureLessonTokens.tabCardGap))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.tertiaryContainer,
                            ),
                        ),
                        VolaGestureLessonTokens.tabCardImageShape,
                    ),
            )
        }
    }
}

@Composable
private fun GestureDirectionBadge(
    step: GestureOnboardingStep,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = gestureAccent(step),
        contentColor = gestureOnAccent(step),
        shadowElevation = VolaGestureLessonTokens.badgeShadow,
    ) {
        Text(
            text = directionSymbol(step),
            modifier = Modifier.padding(VolaGestureLessonTokens.badgePadding),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
    }
}

private fun directionSymbol(step: GestureOnboardingStep): String = when (step) {
    GestureOnboardingStep.SwitchTabs -> "↔"
    GestureOnboardingStep.OpenTabOverview,
    GestureOnboardingStep.CloseTab,
    -> "↑"
}

/** Swipes along the address bar use the accent, the overview the tertiary color of the theme. */
@Composable
private fun gestureAccent(step: GestureOnboardingStep): Color = when (step) {
    GestureOnboardingStep.OpenTabOverview -> MaterialTheme.colorScheme.tertiary
    GestureOnboardingStep.SwitchTabs,
    GestureOnboardingStep.CloseTab,
    -> MaterialTheme.colorScheme.primary
}

@Composable
private fun gestureOnAccent(step: GestureOnboardingStep): Color = when (step) {
    GestureOnboardingStep.OpenTabOverview -> MaterialTheme.colorScheme.onTertiary
    GestureOnboardingStep.SwitchTabs,
    GestureOnboardingStep.CloseTab,
    -> MaterialTheme.colorScheme.onPrimary
}

@Composable
private fun stepTitle(step: GestureOnboardingStep): String = stringResource(
    when (step) {
        GestureOnboardingStep.SwitchTabs -> R.string.onboarding_switch_tabs_title
        GestureOnboardingStep.OpenTabOverview -> R.string.onboarding_open_tabs_title
        GestureOnboardingStep.CloseTab -> R.string.onboarding_close_tab_title
    },
)

@Composable
private fun stepDescription(step: GestureOnboardingStep): String = stringResource(
    when (step) {
        GestureOnboardingStep.SwitchTabs -> R.string.onboarding_switch_tabs_description
        GestureOnboardingStep.OpenTabOverview -> R.string.onboarding_open_tabs_description
        GestureOnboardingStep.CloseTab -> R.string.onboarding_close_tab_description
    },
)

/** The lesson's welcome, a step and the end, in the light, dark and private themes. */
@VolaPreviews
@Composable
private fun GestureLessonWelcomePreview() {
    MaterialBrowserTheme(settings = AppearanceSettings(appearanceMode = BrowserAppearanceMode.System)) {
        GestureOnboardingScreen(onCompleted = {})
    }
}

@VolaPreviews
@Composable
private fun GestureLessonStepPreview() {
    MaterialBrowserTheme(settings = AppearanceSettings(appearanceMode = BrowserAppearanceMode.System)) {
        GestureOnboardingScreen(onCompleted = {}, showWelcome = false)
    }
}

/** A phone on its side: the copy beside the card, nothing below the fold. */
@Preview(name = "Landscape phone", group = "Vola", widthDp = 780, heightDp = 360, showBackground = true)
@Composable
private fun GestureLessonLandscapePreview() {
    MaterialBrowserTheme(settings = AppearanceSettings(appearanceMode = BrowserAppearanceMode.System)) {
        GestureOnboardingScreen(onCompleted = {}, showWelcome = false)
    }
}

@Preview(name = "Private", group = "Vola", widthDp = 412, showBackground = true)
@Composable
private fun GestureLessonPrivatePreview() {
    MaterialBrowserTheme(privateMode = true) {
        GestureOnboardingScreen(onCompleted = {}, showWelcome = false)
    }
}

@VolaPreviews
@Composable
private fun GestureLessonCelebrationPreview() {
    MaterialBrowserTheme(settings = AppearanceSettings(appearanceMode = BrowserAppearanceMode.System)) {
        GestureOnboardingCelebration(onContinue = {})
    }
}
