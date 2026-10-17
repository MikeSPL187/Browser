package dev.sk2andy.materialbrowser.ui.v5

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.shared.ui.icons.VolaIcons
import dev.sk2andy.materialbrowser.ui.theme.v5.V5ControlColors
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Elevation
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Motion
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Shapes
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Sizes
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Spacing
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Theme
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.launch

object V5LinkGlanceTestTags {
    const val Root = "v5_glance"
    const val Card = "v5_glance_card"
    const val Address = "v5_glance_address"
    const val Preview = "v5_glance_preview"
    const val Message = "v5_glance_message"
    const val Retry = "v5_glance_retry"
    const val More = "v5_glance_more"
    const val Open = "v5_glance_open"
    const val Background = "v5_glance_background"
    const val Close = "v5_glance_close"
}

/** Shown in place of a page the card can't show: a failed load, a crash or a dangerous site. */
@Immutable
data class V5LinkGlanceMessage(
    val title: String,
    val body: String,
    val danger: Boolean,
    val retryable: Boolean,
)

@Immutable
data class V5LinkGlanceState(
    /** The address the preview is on now (a redirect moves it on from the link). */
    val url: String,
    /** The load from 0 to 1, or null once the page is in. */
    val progress: Float?,
    val privateTab: Boolean,
    val message: V5LinkGlanceMessage?,
    val canOpenInPrivate: Boolean,
    val canDownloadImage: Boolean,
)

/**
 * What the card does. Every action but [onRetry] ends the preview: it runs once the card has gone
 * back into the link, or, for [onOpen], once the card has filled the window.
 */
@Immutable
class V5LinkGlanceActions(
    val onOpen: () -> Unit,
    val onOpenInBackground: () -> Unit,
    val onOpenInNewTab: () -> Unit,
    val onOpenInPrivate: () -> Unit,
    val onCopy: () -> Unit,
    val onShare: () -> Unit,
    val onDownloadLink: () -> Unit,
    val onDownloadImage: () -> Unit,
    val onRetry: () -> Unit,
    val onDismiss: () -> Unit,
)

/**
 * The v5 link preview (V21, canvas screen «н1», system-v5.md section 12). A long press on a link
 * shrinks the page to 0.97 and dims it ([onPageScale] carries the scale to the page, which lives
 * below this layer), and a card with the live page flies out of [origin] (the touch point in window
 * coordinates) along a low arc. «Open» grows the card into the window and opens the link in this
 * tab, as Safari's preview does; «In background» opens it in a new tab behind; «⋯» has the rest.
 * Nothing in the card goes into history until the link is opened.
 *
 * [preview] is drawn in the card's own layer (the engine draws the preview's page as a texture,
 * V21b), so it flies, fades and is clipped to the card's rounded corners with it; the page comes
 * in over the flight ([V5LinkGlanceRules.pageAlpha]).
 */
@Composable
fun V5LinkGlance(
    state: V5LinkGlanceState,
    actions: V5LinkGlanceActions,
    origin: Offset?,
    onPageScale: (Float) -> Unit,
    modifier: Modifier = Modifier,
    // Previews draw the card already in place.
    entered: Boolean = false,
    preview: @Composable (modifier: Modifier) -> Unit,
) {
    val colors = V5Theme.colors
    val density = LocalDensity.current
    val moves = remember { V5Motion.animationsEnabled }
    val spec: AnimationSpec<Float> = if (moves) V5Motion.glance.spec() else V5Motion.reducedFade()
    val glance = remember { Animatable(if (entered) 1f else 0f) }
    val opening = remember { Animatable(0f) }
    var leaving by remember { mutableStateOf(false) }
    var openingStarted by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val currentOnPageScale by rememberUpdatedState(onPageScale)

    // A back gesture may take the card over before it lands.
    LaunchedEffect(Unit) { glance.animateTo(1f, spec) }
    LaunchedEffect(moves) {
        if (!moves) return@LaunchedEffect
        snapshotFlow { if (openingStarted) 1f else glance.value }
            .collect { currentOnPageScale(V5LinkGlanceRules.pageScale(it)) }
    }
    DisposableEffect(Unit) { onDispose { currentOnPageScale(1f) } }

    val leave: (() -> Unit) -> Unit = { then ->
        if (!leaving) {
            leaving = true
            scope.launch {
                glance.animateTo(0f, spec)
                then()
            }
        }
    }
    val openHere: () -> Unit = {
        if (!leaving) {
            leaving = true
            openingStarted = true
            scope.launch {
                if (moves) opening.animateTo(1f, spec) else glance.animateTo(0f, spec)
                actions.onOpen()
            }
        }
    }
    PredictiveBackHandler(enabled = !leaving) { events ->
        try {
            events.collect { event -> glance.snapTo(1f - event.progress * V5LinkGlanceRules.BACK_TRAVEL) }
            leave(actions.onDismiss)
        } catch (cancelled: CancellationException) {
            scope.launch { glance.animateTo(1f, spec) }
            throw cancelled
        }
    }

    var windowOffset by remember { mutableStateOf(Offset.Zero) }
    var rest by remember { mutableStateOf(Rect.Zero) }
    val localOrigin = origin?.minus(windowOffset)
    val address = remember(state.url) { V5LinkGlanceRules.address(state.url) }
    val originPx = with(density) { V5Sizes.glanceOrigin.toPx() }
    val originRadiusPx = with(density) { V5Sizes.glanceOriginRadius.toPx() }
    val cardRadiusPx = with(density) { V5Shapes.radiusL.toPx() }
    val pageRadiusPx = with(density) { V5Sizes.glancePageRadius.toPx() }
    val arcPx = with(density) { V5Motion.GLANCE_ARC_MAX_DP.dp.toPx() }
    val shadow = V5Elevation.e3.forTheme(colors.isDark).shadows.firstOrNull()
    val closeLabel = stringResource(R.string.glance_close)

    Box(
        modifier
            .fillMaxSize()
            .onGloballyPositioned { windowOffset = it.positionInWindow() }
            .semantics { paneTitle = address }
            .testTag(V5LinkGlanceTestTags.Root),
    ) {
        // Above the page's own surface: the frame around the shrunk page, the dimming, and the
        // sheet the card grows into as the link opens here. A tap outside the card closes it.
        Canvas(
            Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    enabled = !leaving,
                    onClick = { leave(actions.onDismiss) },
                )
                .clearAndSetSemantics { },
        ) {
            val depth = if (openingStarted) 1f else glance.value.coerceIn(0f, 1f)
            val window = Rect(Offset.Zero, size)
            if (moves && depth > 0f) {
                val page = V5LinkGlanceRules.scaledAboutCenter(window, V5LinkGlanceRules.pageScale(depth))
                val frame = Path().apply {
                    fillType = PathFillType.EvenOdd
                    addRect(window)
                    addRoundRect(RoundRect(page, CornerRadius(pageRadiusPx * depth)))
                }
                drawPath(frame, V5ControlColors.glanceBackdrop)
            }
            drawRect(colors.scrim.copy(alpha = colors.scrim.alpha * depth))
            if (opening.value > 0f) {
                val sheet = V5LinkGlanceRules.opening(rest, window, opening.value)
                drawRoundRect(
                    color = colors.background,
                    topLeft = sheet.topLeft,
                    size = sheet.size,
                    cornerRadius = CornerRadius(lerp(cardRadiusPx, 0f, opening.value.coerceIn(0f, 1f))),
                )
            }
        }
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = V5Spacing.marginPhone)
                .padding(top = V5Spacing.space10, bottom = V5Spacing.space12),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                Modifier
                    .widthIn(max = V5Sizes.glanceMaxWidth)
                    .fillMaxWidth()
                    .weight(1f)
                    .onGloballyPositioned { rest = it.boundsInWindow().translate(-windowOffset) }
                    .graphicsLayer {
                        val progress = glance.value
                        val fading = (opening.value * V5LinkGlanceRules.OPEN_FADE_SPEED).coerceIn(0f, 1f)
                        if (moves) {
                            val flight = V5LinkGlanceRules.flight(localOrigin, rest, progress, originPx, arcPx)
                            scaleX = flight.scale
                            scaleY = flight.scale
                            translationX = flight.dx
                            translationY = flight.dy
                            val radius = V5LinkGlanceRules.cornerRadius(originRadiusPx, cardRadiusPx, progress)
                            shape = RoundedCornerShape(radius / flight.scale.coerceAtLeast(V5LinkGlanceRules.MIN_SCALE))
                            alpha = 1f - fading
                        } else {
                            shape = RoundedCornerShape(cardRadiusPx)
                            alpha = progress.coerceIn(0f, 1f)
                        }
                        clip = true
                        if (shadow != null) {
                            shadowElevation = shadow.blur.toPx()
                            spotShadowColor = shadow.color
                            ambientShadowColor = shadow.color
                        }
                    }
                    .background(colors.surface)
                    .border(V5Spacing.hairline, colors.edge, RoundedCornerShape(cardRadiusPx))
                    // Like a Surface: a tap on the card stays on it rather than closing it from behind.
                    .pointerInput(Unit) {}
                    .testTag(V5LinkGlanceTestTags.Card),
            ) {
                GlanceHeader(
                    address = address,
                    secure = remember(state.url) { V5LinkGlanceRules.secure(state.url) },
                    progress = state.progress,
                    state = state,
                    onAction = { action -> leave(action) },
                    actions = actions,
                )
                Box(Modifier.fillMaxWidth().weight(1f)) {
                    preview(
                        Modifier
                            .fillMaxSize()
                            // With motion off the whole card fades, the page with it.
                            .graphicsLayer { if (moves) alpha = V5LinkGlanceRules.pageAlpha(glance.value) }
                            .testTag(V5LinkGlanceTestTags.Preview),
                    )
                    state.message?.let { message ->
                        GlanceMessage(message, onRetry = actions.onRetry, onClose = { leave(actions.onDismiss) })
                    }
                }
                GlanceNote(state.privateTab)
            }
            Spacer(Modifier.height(V5Sizes.glanceActionsGap))
            Row(
                Modifier
                    .widthIn(max = V5Sizes.glanceMaxWidth)
                    .fillMaxWidth()
                    .graphicsLayer {
                        val progress = glance.value
                        val fading = (opening.value * V5LinkGlanceRules.OPEN_FADE_SPEED).coerceIn(0f, 1f)
                        alpha = (if (moves) V5LinkGlanceRules.actionsAlpha(progress) else progress.coerceIn(0f, 1f)) *
                            (1f - fading)
                        if (moves) translationY = (1f - progress) * arcPx
                    },
                horizontalArrangement = Arrangement.spacedBy(V5Spacing.space2),
            ) {
                GlanceButton(
                    text = stringResource(R.string.v5_glance_open),
                    icon = VolaIcons.OpenInFull,
                    primary = true,
                    onClick = openHere,
                    modifier = Modifier.weight(1f).testTag(V5LinkGlanceTestTags.Open),
                )
                GlanceButton(
                    text = stringResource(R.string.v5_glance_background),
                    icon = VolaIcons.Tab,
                    primary = false,
                    onClick = { leave(actions.onOpenInBackground) },
                    modifier = Modifier.weight(1f).testTag(V5LinkGlanceTestTags.Background),
                )
                GlanceButton(
                    text = null,
                    icon = VolaIcons.Close,
                    primary = false,
                    onClick = { leave(actions.onDismiss) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag(V5LinkGlanceTestTags.Close)
                        .semantics { contentDescription = closeLabel },
                )
            }
        }
    }
}

/** The address line over the page: lock, host and path, the load's progress and «⋯». */
@Composable
private fun GlanceHeader(
    address: String,
    secure: Boolean,
    progress: Float?,
    state: V5LinkGlanceState,
    actions: V5LinkGlanceActions,
    onAction: (() -> Unit) -> Unit,
) {
    val colors = V5Theme.colors
    var menu by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxWidth().height(V5Sizes.glanceHeader)) {
        Row(
            Modifier.fillMaxSize().padding(start = V5Spacing.space4),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(V5Spacing.space1),
        ) {
            Icon(
                if (secure) VolaIcons.Lock else VolaIcons.LockOpen,
                contentDescription = null,
                tint = if (secure) colors.labelSecondary else colors.warning,
                modifier = Modifier.size(V5Sizes.iconSmall),
            )
            Text(
                address,
                style = V5Theme.type.footnote.copy(fontWeight = FontWeight.SemiBold),
                color = colors.label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).testTag(V5LinkGlanceTestTags.Address),
            )
            Box {
                IconButton(onClick = { menu = true }, modifier = Modifier.testTag(V5LinkGlanceTestTags.More)) {
                    Icon(
                        VolaIcons.MoreHoriz,
                        contentDescription = stringResource(R.string.v5_glance_more),
                        tint = colors.labelSecondary,
                    )
                }
                // «More» as a v5 sheet of the link's actions, each with its glyph, as the menu had.
                val item: @Composable (Int, ImageVector, () -> Unit) -> V5PromptAction = { label, icon, action ->
                    V5PromptAction(
                        label = stringResource(label),
                        icon = icon,
                        onClick = {
                            menu = false
                            onAction(action)
                        },
                    )
                }
                V5PromptSheet(
                    visible = menu,
                    title = address,
                    message = null,
                    actions = listOfNotNull(
                        item(R.string.action_open_in_new_tab, VolaIcons.OpenInNew, actions.onOpenInNewTab),
                        if (state.canOpenInPrivate) item(R.string.v5_glance_private, VolaIcons.VisibilityOff, actions.onOpenInPrivate) else null,
                        item(R.string.external_link_preview_copy_link, VolaIcons.ContentCopy, actions.onCopy),
                        item(R.string.action_share, VolaIcons.Share, actions.onShare),
                        item(R.string.action_download_link, VolaIcons.Download, actions.onDownloadLink),
                        if (state.canDownloadImage) item(R.string.action_download_image, VolaIcons.Image, actions.onDownloadImage) else null,
                    ),
                    onDismiss = { menu = false },
                )
            }
        }
        if (progress != null) {
            Box(
                Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .height(V5Sizes.barProgress)
                    .background(colors.accent),
            )
        }
    }
}

/** In place of a page the card can't show; a dangerous site offers only a way back. */
@Composable
private fun GlanceMessage(message: V5LinkGlanceMessage, onRetry: () -> Unit, onClose: () -> Unit) {
    val colors = V5Theme.colors
    Column(
        Modifier
            .fillMaxSize()
            .background(colors.surface)
            .padding(V5Spacing.space6)
            .semantics { liveRegion = LiveRegionMode.Polite }
            .testTag(V5LinkGlanceTestTags.Message),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(V5Spacing.space3, Alignment.CenterVertically),
    ) {
        Icon(
            if (message.danger) VolaIcons.Dangerous else VolaIcons.PublicOff,
            contentDescription = null,
            tint = if (message.danger) colors.danger else colors.labelSecondary,
            modifier = Modifier.size(V5Sizes.iconMax),
        )
        Text(message.title, style = V5Theme.type.headline, color = colors.label, textAlign = TextAlign.Center)
        Text(message.body, style = V5Theme.type.footnote, color = colors.labelSecondary, textAlign = TextAlign.Center)
        if (message.retryable) {
            GlanceButton(
                text = stringResource(R.string.action_retry),
                icon = VolaIcons.Refresh,
                primary = true,
                onClick = onRetry,
                modifier = Modifier.testTag(V5LinkGlanceTestTags.Retry),
            )
        } else {
            GlanceButton(
                text = stringResource(R.string.glance_close),
                icon = VolaIcons.Close,
                primary = true,
                onClick = onClose,
                modifier = Modifier,
            )
        }
    }
}

/** «Not added to history until you open it» under the page; a private tab saves nothing at all. */
@Composable
private fun GlanceNote(privateTab: Boolean) {
    val colors = V5Theme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = V5Spacing.space4, vertical = V5Spacing.space2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(V5Spacing.space2),
    ) {
        Icon(
            VolaIcons.VisibilityOff,
            contentDescription = null,
            tint = colors.labelSecondary,
            modifier = Modifier.size(V5Sizes.iconSmall),
        )
        Text(
            stringResource(if (privateTab) R.string.v5_glance_private_note else R.string.glance_not_in_history),
            style = V5Theme.type.footnote,
            color = colors.labelSecondary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun GlanceButton(
    text: String?,
    icon: ImageVector,
    primary: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    val colors = V5Theme.colors
    val content: Color = if (primary) colors.onAccentFill else colors.label
    Row(
        modifier
            .heightIn(min = V5Sizes.touchTarget)
            .clip(V5Theme.shapes.full)
            .background(if (primary) colors.accentFill else colors.surface)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = V5Spacing.space4),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(V5Spacing.space2, Alignment.CenterHorizontally),
    ) {
        Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(V5Sizes.iconSmall))
        if (text != null) {
            Text(
                text,
                style = V5Theme.type.callout.copy(fontWeight = FontWeight.SemiBold),
                color = content,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
