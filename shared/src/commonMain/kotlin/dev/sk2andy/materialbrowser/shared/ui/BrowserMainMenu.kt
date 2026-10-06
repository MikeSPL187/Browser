package dev.sk2andy.materialbrowser.shared.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import dev.sk2andy.materialbrowser.shared.browser.BrowserFeatureMenuAction
import dev.sk2andy.materialbrowser.shared.browser.BrowserFeatureMenuItem
import dev.sk2andy.materialbrowser.shared.browser.BrowserFeatureMenuItemKind
import dev.sk2andy.materialbrowser.shared.browser.BrowserFeatureMenuSection
import dev.sk2andy.materialbrowser.shared.ui.theme.densityRowMinHeight
import dev.sk2andy.materialbrowser.shared.ui.theme.densityVerticalPadding
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

object BrowserMainMenuMotion {
    const val EXIT_DURATION_MILLIS = 160
    const val COLLAPSED_SCALE_X = 0.94f
    const val COLLAPSED_SCALE_Y = 0.84f
    const val CONTENT_OFFSET_Y_DP = 14f
    private const val POPUP_OFFSET_Y_DP = -10f
    private const val CONTENT_REVEAL_START = 0.18f

    fun surfaceScale(
        expansionProgress: Float,
        surfaceSize: Float,
        anchorSize: Float,
    ): Float = AddressBarMorphRules.containerScale(
        progress = 1f - expansionProgress.coerceIn(0f, 1f),
        sourceSize = surfaceSize,
        targetSize = anchorSize,
    )

    fun surfaceCornerRadii(
        expansionProgress: Float,
        surfaceWidth: Float,
        surfaceHeight: Float,
        anchorSize: Float,
        surfaceCornerRadius: Float,
    ): AddressBarMorphCornerRadii = AddressBarMorphRules.cornerRadii(
        progress = 1f - expansionProgress.coerceIn(0f, 1f),
        sourceWidth = surfaceWidth,
        sourceHeight = surfaceHeight,
        targetSize = anchorSize,
        sourceCornerRadius = surfaceCornerRadius,
    )

    fun surfaceAlpha(
        expansionProgress: Float,
        preservesVisualEffect: Boolean,
    ): Float = if (preservesVisualEffect) {
        1f
    } else {
        expansionProgress.coerceIn(0f, 1f)
    }

    fun contentProgress(expansionProgress: Float): Float =
        ((expansionProgress.coerceIn(0f, 1f) - CONTENT_REVEAL_START) /
            (1f - CONTENT_REVEAL_START)).coerceIn(0f, 1f)

    fun popupOffsetYDp(
        expansionProgress: Float,
        hasMorphAnchor: Boolean,
    ): Float = if (hasMorphAnchor) {
        POPUP_OFFSET_Y_DP * expansionProgress.coerceIn(0f, 1f)
    } else {
        POPUP_OFFSET_Y_DP
    }
}

object BrowserMenuIconRules {
    fun useFilledVariant(
        action: BrowserFeatureMenuAction,
        selected: Boolean,
    ): Boolean = when (action) {
        BrowserFeatureMenuAction.ToggleFavorite,
        BrowserFeatureMenuAction.TogglePinned,
        -> selected
        else -> true
    }
}

enum class BrowserMainMenuContainerRole {
    Regular,
    Selected,
    Branded,
}

data class BrowserMainMenuStyle(
    val showHeader: Boolean = true,
    val menuMaxWidth: Dp = 400.dp,
    val menuCornerRadius: Dp = 16.dp,
    val groupCornerRadius: Dp = 12.dp,
    val groupInnerCornerRadius: Dp = 4.dp,
    val contentHorizontalPadding: Dp = 16.dp,
    val contentVerticalPadding: Dp = 8.dp,
    val toolbarSpacing: Dp = 4.dp,
    val toolbarMinHeight: Dp = 64.dp,
    val toolbarIconSize: Dp = 22.dp,
    val toolbarButtonSize: Dp = 48.dp,
    val showToolbarLabels: Boolean = true,
    val groupItemSpacing: Dp = 2.dp,
    val rowMinHeight: Dp = 44.dp,
    val rowHorizontalPadding: Dp = 16.dp,
    val rowVerticalPadding: Dp = 6.dp,
    val toolbarLabelFontSize: TextUnit = TextUnit.Unspecified,
    val rowLabelFontSize: TextUnit = TextUnit.Unspecified,
    val rowSupportingTextFontSize: TextUnit = TextUnit.Unspecified,
    val toggleTrackColor: Color? = null,
    val useExpressiveToggleButtons: Boolean = false,
    /** Space left on each side of the menu; null keeps the narrow popup width. */
    val screenMargin: Dp? = null,
    /** Page and Vola actions as a tile grid under a sheet handle (v4); null keeps the list. */
    val tiles: BrowserMainMenuTileStyle? = null,
)

interface BrowserMainMenuResources {
    @Composable
    fun title(): String

    @Composable
    fun sectionTitle(section: BrowserFeatureMenuSection): String

    @Composable
    fun label(item: BrowserFeatureMenuItem, toolbar: Boolean = false): String

    @Composable
    fun accessibilityLabel(item: BrowserFeatureMenuItem): String? = null

    @Composable
    fun supportingText(item: BrowserFeatureMenuItem, snoozedTabCount: Int): String?

    @Composable
    fun icon(item: BrowserFeatureMenuItem, modifier: Modifier)

    @Composable
    fun trailingIcon(modifier: Modifier)

    /** The way back from «More» to the first view. */
    @Composable
    fun backLabel(): String

    @Composable
    fun backIcon(modifier: Modifier)
}

interface BrowserMainMenuEffects {
    val style: BrowserMainMenuStyle
        get() = BrowserMainMenuStyle()

    @Composable
    fun menuSurface(
        modifier: Modifier,
        shape: Shape,
        content: @Composable () -> Unit,
    )

    @Composable
    fun containerColor(
        color: Color,
        frostedAlpha: Float = 0.82f,
        role: BrowserMainMenuContainerRole = BrowserMainMenuContainerRole.Regular,
    ): Color

    @Composable
    fun sectionTitleColor(color: Color): Color = color

    @Composable
    fun spatialAnimationSpec(expanding: Boolean): FiniteAnimationSpec<Float> = tween(
        durationMillis = BrowserMainMenuMotion.EXIT_DURATION_MILLIS,
        easing = if (expanding) LinearOutSlowInEasing else FastOutLinearInEasing,
    )

    @Composable
    fun effectsAnimationSpec(expanding: Boolean): FiniteAnimationSpec<Float> = tween(
        durationMillis = BrowserMainMenuMotion.EXIT_DURATION_MILLIS,
        easing = if (expanding) LinearOutSlowInEasing else FastOutLinearInEasing,
    )

    /**
     * UIKit visual effects must stay fully opaque while their host view scales.
     * Applying alpha to the host forces offscreen composition and breaks glass.
     */
    fun preservesVisualEffectDuringMorph(): Boolean = false

    fun maxHeightFraction(): Float = BROWSER_MAIN_MENU_MAX_HEIGHT_FRACTION

    fun popupState(expanded: Boolean, visible: Boolean) = Unit
}

object DefaultBrowserMainMenuEffects : BrowserMainMenuEffects {
    @Composable
    override fun menuSurface(
        modifier: Modifier,
        shape: Shape,
        content: @Composable () -> Unit,
    ) {
        Surface(modifier = modifier, shape = shape, content = content)
    }

    @Composable
    override fun containerColor(
        color: Color,
        frostedAlpha: Float,
        role: BrowserMainMenuContainerRole,
    ): Color = color
}

private data class BrowserMainMenuSnapshot(
    val pageSubtitle: String,
    val items: List<BrowserFeatureMenuItem>,
    val snoozedTabCount: Int,
)

@Composable
fun BrowserMainMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    pageSubtitle: String,
    items: List<BrowserFeatureMenuItem>,
    snoozedTabCount: Int,
    screenSize: DpSize,
    resources: BrowserMainMenuResources,
    onAction: (BrowserFeatureMenuItem) -> Unit,
    effects: BrowserMainMenuEffects = DefaultBrowserMainMenuEffects,
    morphAnchorSize: DpSize? = null,
    morphProgress: Float? = null,
    extensionContent: @Composable ColumnScope.((() -> Unit) -> Unit) -> Unit = {},
) {
    val colors = MaterialTheme.colorScheme
    val style = effects.style
    val menuShape = RoundedCornerShape(style.menuCornerRadius)
    val outerCorners = RoundedCornerShape(style.groupCornerRadius)
    val innerCorners = RoundedCornerShape(style.groupInnerCornerRadius)
    val firstItemShape = RoundedCornerShape(
        topStart = outerCorners.topStart,
        topEnd = outerCorners.topEnd,
        bottomEnd = innerCorners.bottomEnd,
        bottomStart = innerCorners.bottomStart,
    )
    val lastItemShape = RoundedCornerShape(
        topStart = innerCorners.topStart,
        topEnd = innerCorners.topEnd,
        bottomEnd = outerCorners.bottomEnd,
        bottomStart = outerCorners.bottomStart,
    )
    val menuMargin = style.screenMargin ?: 12.dp // token-exempt: Candy's popup margin
    val menuWidth = minOf(style.menuMaxWidth, screenSize.width - menuMargin * 2)
    val toolbarSingleRowMinWidth = style.contentHorizontalPadding * 2 +
        style.toolbarButtonSize * 5 + style.toolbarSpacing * 4
    val compactToolbar = if (style.showToolbarLabels) {
        menuWidth < 340.dp
    } else {
        menuWidth < toolbarSingleRowMinWidth
    }
    val menuMaxHeight = screenSize.height * effects.maxHeightFraction()
    val menuScrollState = rememberScrollState()
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val menuWidthPx = with(density) { menuWidth.toPx() }
    val menuHeightPx = with(density) { menuMaxHeight.toPx() }
    val anchorWidthPx = morphAnchorSize?.let { with(density) { it.width.toPx() } }
    val anchorHeightPx = morphAnchorSize?.let { with(density) { it.height.toPx() } }
    val anchorSizePx = minOf(anchorWidthPx ?: 0f, anchorHeightPx ?: 0f)
    val menuCornerRadiusPx = when (
        val outline = menuShape.createOutline(
            size = Size(menuWidthPx, menuHeightPx),
            layoutDirection = layoutDirection,
            density = density,
        )
    ) {
        is Outline.Rounded -> outline.roundRect.topRightCornerRadius.x
        else -> 0f
    }
    val requestedSnapshot = BrowserMainMenuSnapshot(
        pageSubtitle = pageSubtitle,
        items = items,
        snoozedTabCount = snoozedTabCount,
    )
    var snapshot by remember { mutableStateOf(requestedSnapshot) }
    if (expanded && requestedSnapshot != snapshot) {
        snapshot = requestedSnapshot
    }
    var popupVisible by remember { mutableStateOf(expanded) }
    var actionCommitted by remember { mutableStateOf(false) }
    var moreOpen by remember { mutableStateOf(false) }
    val spatialProgress = remember { Animatable(if (expanded) 1f else 0f) }
    val effectsProgress = remember { Animatable(if (expanded) 1f else 0f) }
    val openingSpatialAnimationSpec = effects.spatialAnimationSpec(expanding = true)
    val closingSpatialAnimationSpec = effects.spatialAnimationSpec(expanding = false)
    val openingEffectsAnimationSpec = effects.effectsAnimationSpec(expanding = true)
    val closingEffectsAnimationSpec = effects.effectsAnimationSpec(expanding = false)
    val menuTransformOrigin = if (layoutDirection == LayoutDirection.Ltr) {
        TransformOrigin(1f, 1f)
    } else {
        TransformOrigin(0f, 1f)
    }
    LaunchedEffect(expanded) {
        if (expanded) {
            actionCommitted = false
            val reversingExit = popupVisible
            popupVisible = true
            if (!reversingExit) {
                moreOpen = false
                menuScrollState.scrollTo(0)
                spatialProgress.snapTo(0f)
                effectsProgress.snapTo(0f)
            }
            coroutineScope {
                launch {
                    spatialProgress.animateTo(
                        targetValue = 1f,
                        animationSpec = openingSpatialAnimationSpec,
                    )
                }
                launch {
                    effectsProgress.animateTo(
                        targetValue = 1f,
                        animationSpec = openingEffectsAnimationSpec,
                    )
                }
            }
        } else if (popupVisible) {
            coroutineScope {
                launch {
                    spatialProgress.animateTo(
                        targetValue = 0f,
                        animationSpec = closingSpatialAnimationSpec,
                    )
                }
                launch {
                    effectsProgress.animateTo(
                        targetValue = 0f,
                        animationSpec = closingEffectsAnimationSpec,
                    )
                }
            }
            popupVisible = false
        }
    }
    LaunchedEffect(expanded, popupVisible) {
        effects.popupState(expanded, popupVisible)
    }
    fun commit(action: () -> Unit) {
        if (!expanded || actionCommitted) return
        actionCommitted = true
        onDismissRequest()
        action()
    }

    fun dismissThen(item: BrowserFeatureMenuItem) = commit { onAction(item) }

    // «More» turns the sheet's page in place; every other command runs and closes the menu.
    fun onCommand(item: BrowserFeatureMenuItem) {
        if (item.action == BrowserFeatureMenuAction.OpenMore) moreOpen = true else dismissThen(item)
    }
    LaunchedEffect(moreOpen) { menuScrollState.scrollTo(0) }

    if (popupVisible) {
        val currentSpatialProgress = morphProgress ?: spatialProgress.value
        val currentEffectsProgress = morphProgress ?: effectsProgress.value
        val contentProgress = BrowserMainMenuMotion.contentProgress(
            if (expanded) currentSpatialProgress else currentEffectsProgress,
        )
        val popupOffsetY = BrowserMainMenuMotion.popupOffsetYDp(
            expansionProgress = currentSpatialProgress,
            hasMorphAnchor = morphAnchorSize != null,
        ).dp
        val popupOffset = with(density) { IntOffset(0, popupOffsetY.roundToPx()) }
        val morphRadii = if (anchorSizePx > 0f) {
            BrowserMainMenuMotion.surfaceCornerRadii(
                expansionProgress = currentSpatialProgress,
                surfaceWidth = menuWidthPx,
                surfaceHeight = menuHeightPx,
                anchorSize = anchorSizePx,
                surfaceCornerRadius = menuCornerRadiusPx,
            )
        } else {
            null
        }
        val animatedMenuShape = morphRadii?.let { radii ->
            GenericShape { size, _ ->
                addRoundRect(
                    RoundRect(
                        left = 0f,
                        top = 0f,
                        right = size.width,
                        bottom = size.height,
                        topLeftCornerRadius = CornerRadius(radii.horizontal, radii.vertical),
                        topRightCornerRadius = CornerRadius(radii.horizontal, radii.vertical),
                        bottomRightCornerRadius = CornerRadius(radii.horizontal, radii.vertical),
                        bottomLeftCornerRadius = CornerRadius(radii.horizontal, radii.vertical),
                    ),
                )
            }
        } ?: menuShape
        val sheetPosition = remember(popupOffset.y) {
            BrowserMainMenuSheetPositionProvider(offsetY = popupOffset.y)
        }
        BrowserMainMenuPopup(
            centered = style.screenMargin != null,
            sheetPosition = sheetPosition,
            offset = popupOffset,
            onDismissRequest = onDismissRequest,
            properties = PopupProperties(focusable = expanded),
        ) {
            effects.menuSurface(
                modifier = Modifier
                    .width(menuWidth)
                    .height(menuMaxHeight)
                    .graphicsLayer {
                        alpha = BrowserMainMenuMotion.surfaceAlpha(
                            expansionProgress = currentEffectsProgress,
                            preservesVisualEffect = effects.preservesVisualEffectDuringMorph(),
                        )
                        if (anchorWidthPx != null && anchorHeightPx != null) {
                            scaleX = BrowserMainMenuMotion.surfaceScale(
                                expansionProgress = currentSpatialProgress,
                                surfaceSize = menuWidthPx,
                                anchorSize = anchorWidthPx,
                            )
                            scaleY = BrowserMainMenuMotion.surfaceScale(
                                expansionProgress = currentSpatialProgress,
                                surfaceSize = menuHeightPx,
                                anchorSize = anchorHeightPx,
                            )
                        } else {
                            scaleX = BrowserMainMenuMotion.COLLAPSED_SCALE_X +
                                (1f - BrowserMainMenuMotion.COLLAPSED_SCALE_X) *
                                currentSpatialProgress
                            scaleY = BrowserMainMenuMotion.COLLAPSED_SCALE_Y +
                                (1f - BrowserMainMenuMotion.COLLAPSED_SCALE_Y) *
                                currentSpatialProgress
                        }
                        transformOrigin = menuTransformOrigin
                    }
                    .clip(animatedMenuShape)
                    .testTag(BrowserMainMenuTestTags.Menu),
                shape = animatedMenuShape,
            ) {
                BrowserMainMenuContent(
                    snapshot = snapshot,
                    moreOpen = moreOpen,
                    compactToolbar = compactToolbar,
                    resources = resources,
                    effects = effects,
                    firstItemShape = firstItemShape,
                    innerCorners = innerCorners,
                    lastItemShape = lastItemShape,
                    onCommand = ::onCommand,
                    onToggle = onAction,
                    onCloseMore = { moreOpen = false },
                    extensionContent = extensionContent,
                    onExtensionCommit = ::commit,
                    modifier = Modifier
                        .graphicsLayer {
                            alpha = contentProgress
                            translationY = with(density) {
                                (BrowserMainMenuMotion.CONTENT_OFFSET_Y_DP *
                                    (1f - contentProgress)).dp.toPx()
                            }
                        }
                        .verticalScroll(menuScrollState)
                        .padding(
                            horizontal = style.contentHorizontalPadding,
                            vertical = style.contentVerticalPadding,
                        ),
                )
            }
        }
    }
}

/**
 * A screen-wide sheet sits centered in the window; the narrow popup keeps its bottom-end anchor.
 * Either way the bottom edge follows the anchor's, as before.
 */
private class BrowserMainMenuSheetPositionProvider(
    private val offsetY: Int,
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset = IntOffset(
        x = (windowSize.width - popupContentSize.width) / 2,
        y = anchorBounds.bottom - popupContentSize.height + offsetY,
    )
}

@Composable
private fun BrowserMainMenuPopup(
    centered: Boolean,
    sheetPosition: PopupPositionProvider,
    offset: IntOffset,
    onDismissRequest: () -> Unit,
    properties: PopupProperties,
    content: @Composable () -> Unit,
) {
    if (centered) {
        Popup(
            popupPositionProvider = sheetPosition,
            onDismissRequest = onDismissRequest,
            properties = properties,
            content = content,
        )
    } else {
        Popup(
            alignment = Alignment.BottomEnd,
            offset = offset,
            onDismissRequest = onDismissRequest,
            properties = properties,
            content = content,
        )
    }
}

@Composable
private fun BrowserMainMenuContent(
    snapshot: BrowserMainMenuSnapshot,
    moreOpen: Boolean,
    compactToolbar: Boolean,
    resources: BrowserMainMenuResources,
    effects: BrowserMainMenuEffects,
    firstItemShape: Shape,
    innerCorners: Shape,
    lastItemShape: Shape,
    onCommand: (BrowserFeatureMenuItem) -> Unit,
    onToggle: (BrowserFeatureMenuItem) -> Unit,
    onCloseMore: () -> Unit,
    extensionContent: @Composable ColumnScope.((() -> Unit) -> Unit) -> Unit,
    onExtensionCommit: (() -> Unit) -> Unit,
    modifier: Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val groupedItems = snapshot.items.groupBy(BrowserFeatureMenuItem::section)
    val tiles = effects.style.tiles
    @Composable
    fun group(
        section: BrowserFeatureMenuSection,
        tag: String,
        titled: Boolean = true,
    ) {
        val items = groupedItems[section].orEmpty()
        if (items.isEmpty()) return
        if (titled) {
            BrowserMainMenuSectionTitle(
                title = resources.sectionTitle(section),
                topPadding = 8,
                effects = effects,
            )
        } else {
            Spacer(Modifier.height(effects.style.contentVerticalPadding))
        }
        BrowserMainMenuItemGroup(
            items = items,
            resources = resources,
            effects = effects,
            snoozedTabCount = snapshot.snoozedTabCount,
            firstItemShape = firstItemShape,
            innerCorners = innerCorners,
            lastItemShape = lastItemShape,
            onCommand = onCommand,
            onToggle = onToggle,
            modifier = Modifier.testTag(tag),
        )
    }
    Column(modifier = modifier) {
        tiles?.let { BrowserMainMenuHandle(it) }
        if (effects.style.showHeader) {
            Text(
                text = resources.title(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = snapshot.pageSubtitle,
                modifier = Modifier.padding(top = 2.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelMedium,
                color = colors.onSurfaceVariant,
            )
            Spacer(Modifier.height(10.dp))
        }
        // «More» slides in over the short first view and back out; both keep the sheet's size.
        AnimatedContent(
            targetState = moreOpen,
            transitionSpec = {
                val direction = if (targetState) 1 else -1
                (slideInHorizontally { width -> direction * width / 4 } + fadeIn())
                    .togetherWith(slideOutHorizontally { width -> -direction * width / 4 } + fadeOut())
            },
            label = "browserMainMenuMore",
        ) { more ->
            Column {
                if (more) {
                    BrowserMainMenuMoreHeader(resources = resources, effects = effects, onBack = onCloseMore)
                    group(BrowserFeatureMenuSection.More, BrowserMainMenuTestTags.MoreGroup, titled = false)
                    group(BrowserFeatureMenuSection.Toppings, BrowserMainMenuTestTags.ToppingsGroup)
                    extensionContent(onExtensionCommit)
                } else {
                    val toolbarItems = groupedItems[BrowserFeatureMenuSection.Toolbar].orEmpty()
                    if (toolbarItems.isNotEmpty()) {
                        BrowserMainMenuToolbar(
                            items = toolbarItems,
                            compact = compactToolbar,
                            resources = resources,
                            effects = effects,
                            onClick = onCommand,
                        )
                    }
                    val pageItems = groupedItems[BrowserFeatureMenuSection.Page].orEmpty()
                    if (tiles != null && pageItems.isNotEmpty()) {
                        Spacer(Modifier.height(tiles.spacing))
                        BrowserMainMenuTileGrid(
                            items = pageItems,
                            tiles = tiles,
                            resources = resources,
                            effects = effects,
                            onCommand = onCommand,
                            onToggle = onToggle,
                            modifier = Modifier.testTag(BrowserMainMenuTestTags.PageGroup),
                        )
                    } else {
                        group(BrowserFeatureMenuSection.Page, BrowserMainMenuTestTags.PageGroup)
                    }
                    group(BrowserFeatureMenuSection.Browser, BrowserMainMenuTestTags.BrowserGroup)
                }
            }
        }
    }
}

@Composable
private fun BrowserMainMenuMoreHeader(
    resources: BrowserMainMenuResources,
    effects: BrowserMainMenuEffects,
    onBack: () -> Unit,
) {
    val style = effects.style
    val backLabel = resources.backLabel()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = style.toolbarMinHeight)
            .clip(RoundedCornerShape(style.groupCornerRadius))
            .clickable(onClickLabel = backLabel, role = Role.Button, onClick = onBack)
            .testTag(BrowserMainMenuTestTags.MoreBack)
            .padding(horizontal = style.rowHorizontalPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(style.rowHorizontalPadding),
    ) {
        resources.backIcon(Modifier.size(style.toolbarIconSize))
        Text(
            text = resources.sectionTitle(BrowserFeatureMenuSection.More),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun BrowserMainMenuToolbar(
    items: List<BrowserFeatureMenuItem>,
    compact: Boolean,
    resources: BrowserMainMenuResources,
    effects: BrowserMainMenuEffects,
    onClick: (BrowserFeatureMenuItem) -> Unit,
) {
    val primaryItems = if (compact) items.take(3) else items
    val secondaryItems = if (compact) items.drop(3) else emptyList()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(BrowserMainMenuTestTags.Toolbar),
        verticalArrangement = Arrangement.spacedBy(effects.style.toolbarSpacing),
    ) {
        BrowserMainMenuToolbarRow(primaryItems, resources, effects, onClick)
        if (secondaryItems.isNotEmpty()) {
            BrowserMainMenuToolbarRow(secondaryItems, resources, effects, onClick)
        }
    }
}

@Composable
private fun BrowserMainMenuToolbarRow(
    items: List<BrowserFeatureMenuItem>,
    resources: BrowserMainMenuResources,
    effects: BrowserMainMenuEffects,
    onClick: (BrowserFeatureMenuItem) -> Unit,
) {
    val style = effects.style
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (style.showToolbarLabels) {
            Arrangement.spacedBy(style.toolbarSpacing)
        } else {
            Arrangement.SpaceBetween
        },
    ) {
        items.forEach { item ->
            val label = resources.label(item, toolbar = true)
            BrowserMenuToolbarAction(
                label = label,
                icon = { resources.icon(item, Modifier.size(style.toolbarIconSize)) },
                enabled = item.enabled || item.action in RELOAD_ACTIONS,
                selected = item.checked == true,
                accessibilityLabel = resources.accessibilityLabel(item) ?: label.takeUnless {
                    style.showToolbarLabels
                },
                minHeight = style.toolbarMinHeight,
                verticalLabelFontSize = style.toolbarLabelFontSize,
                showLabel = style.showToolbarLabels,
                shape = if (style.showToolbarLabels) {
                    MaterialTheme.shapes.large
                } else {
                    CircleShape
                },
                containerColor = effects.containerColor(
                    if (item.checked == true) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHighest
                    },
                    role = if (item.checked == true) {
                        BrowserMainMenuContainerRole.Selected
                    } else {
                        BrowserMainMenuContainerRole.Regular
                    },
                ),
                onClick = { onClick(item) },
                modifier = if (style.showToolbarLabels) {
                    Modifier
                        .weight(1f)
                        .then(item.testTagModifier())
                } else {
                    Modifier
                        .size(style.toolbarButtonSize)
                        .then(item.testTagModifier())
                },
            )
        }
    }
}

private val RELOAD_ACTIONS = setOf(
    BrowserFeatureMenuAction.Reload,
    BrowserFeatureMenuAction.Stop,
)

@Composable
private fun BrowserMainMenuSectionTitle(
    title: String,
    topPadding: Int,
    effects: BrowserMainMenuEffects,
) {
    Spacer(Modifier.height(topPadding.dp))
    Text(
        text = title,
        modifier = Modifier.padding(start = 8.dp, bottom = 6.dp),
        style = MaterialTheme.typography.labelLarge,
        color = effects.sectionTitleColor(MaterialTheme.colorScheme.primary),
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun BrowserMainMenuItemGroup(
    items: List<BrowserFeatureMenuItem>,
    resources: BrowserMainMenuResources,
    effects: BrowserMainMenuEffects,
    snoozedTabCount: Int,
    firstItemShape: Shape,
    innerCorners: Shape,
    lastItemShape: Shape,
    onCommand: (BrowserFeatureMenuItem) -> Unit,
    onToggle: (BrowserFeatureMenuItem) -> Unit,
    modifier: Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(effects.style.groupItemSpacing),
    ) {
        items.forEachIndexed { index, item ->
            val shape = when {
                items.size == 1 -> RoundedCornerShape(effects.style.groupCornerRadius)
                index == 0 -> firstItemShape
                index == items.lastIndex -> lastItemShape
                else -> innerCorners
            }
            val itemModifier = item.testTagModifier()
            when {
                effects.style.useExpressiveToggleButtons &&
                    item.kind == BrowserFeatureMenuItemKind.Toggle &&
                    item.checked != null -> {
                    BrowserMenuExpressiveToggleButton(
                        label = resources.label(item),
                        icon = { resources.icon(item, Modifier.size(20.dp)) },
                        checked = item.checked,
                        enabled = item.enabled,
                        onCheckedChange = { onToggle(item) },
                        modifier = itemModifier,
                        minHeight = densityRowMinHeight(effects.style.rowMinHeight),
                        horizontalPadding = effects.style.rowHorizontalPadding,
                        verticalPadding = densityVerticalPadding(effects.style.rowVerticalPadding),
                        labelFontSize = effects.style.rowLabelFontSize,
                        uncheckedContainerColor = effects.containerColor(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            frostedAlpha = 1f,
                        ),
                        uncheckedContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        checkedContainerColor = effects.containerColor(
                            color = MaterialTheme.colorScheme.secondary,
                            frostedAlpha = 1f,
                            role = BrowserMainMenuContainerRole.Selected,
                        ),
                        checkedContentColor = MaterialTheme.colorScheme.onSecondary,
                    )
                }
                item.action == BrowserFeatureMenuAction.ToggleDomainMute -> {
                    BrowserMenuIconToggleItem(
                        label = resources.label(item),
                        icon = { resources.icon(item, Modifier.size(20.dp)) },
                        checked = item.checked == true,
                        enabled = item.enabled,
                        onCheckedChange = { onToggle(item) },
                        modifier = itemModifier,
                        shape = shape,
                        containerColor = effects.containerColor(containerColor),
                        minHeight = densityRowMinHeight(effects.style.rowMinHeight),
                        horizontalPadding = effects.style.rowHorizontalPadding,
                        verticalPadding = densityVerticalPadding(effects.style.rowVerticalPadding),
                        labelFontSize = effects.style.rowLabelFontSize,
                        checkedTrackColor = effects.style.toggleTrackColor,
                    )
                }
                item.kind == BrowserFeatureMenuItemKind.Toggle && item.checked != null -> {
                    BrowserMenuToggleItem(
                        label = resources.label(item),
                        supportingText = resources.supportingText(item, snoozedTabCount).orEmpty(),
                        checked = item.checked,
                        enabled = item.enabled,
                        onCheckedChange = { onToggle(item) },
                        modifier = itemModifier,
                        shape = shape,
                        containerColor = effects.containerColor(containerColor),
                        minHeight = densityRowMinHeight(effects.style.rowMinHeight),
                        horizontalPadding = effects.style.rowHorizontalPadding,
                        verticalPadding = densityVerticalPadding(effects.style.rowVerticalPadding),
                        labelFontSize = effects.style.rowLabelFontSize,
                        supportingTextFontSize = effects.style.rowSupportingTextFontSize,
                        checkedTrackColor = effects.style.toggleTrackColor,
                    )
                }
                else -> {
                    BrowserMenuRow(
                        label = resources.label(item),
                        icon = { resources.icon(item, Modifier.size(20.dp)) },
                        shape = shape,
                        onClick = { onCommand(item) },
                        modifier = itemModifier,
                        enabled = item.enabled,
                        containerColor = effects.containerColor(
                            color = containerColor,
                            frostedAlpha = 0.68f,
                            role = if (containerColor == MaterialTheme.colorScheme.tertiaryContainer) {
                                BrowserMainMenuContainerRole.Branded
                            } else {
                                BrowserMainMenuContainerRole.Regular
                            },
                        ),
                        contentColor = contentColor,
                        supportingText = resources.supportingText(item, snoozedTabCount),
                        trailingContent = if (item.hasTrailingIcon()) {
                            { resources.trailingIcon(Modifier.size(20.dp)) }
                        } else {
                            null
                        },
                        minHeight = densityRowMinHeight(effects.style.rowMinHeight),
                        horizontalPadding = effects.style.rowHorizontalPadding,
                        verticalPadding = densityVerticalPadding(effects.style.rowVerticalPadding),
                        labelFontSize = effects.style.rowLabelFontSize,
                        supportingTextFontSize = effects.style.rowSupportingTextFontSize,
                    )
                }
            }
        }
    }
}

private fun BrowserFeatureMenuItem.hasTrailingIcon(): Boolean = action in setOf(
    BrowserFeatureMenuAction.OpenSnoozedTabs,
    BrowserFeatureMenuAction.OpenFavorites,
    BrowserFeatureMenuAction.OpenDownloads,
    BrowserFeatureMenuAction.OpenHistory,
    BrowserFeatureMenuAction.OpenFirefoxExtensions,
    BrowserFeatureMenuAction.OpenPasswords,
    BrowserFeatureMenuAction.OpenSettings,
)

internal fun BrowserFeatureMenuItem.testTagModifier(): Modifier = when (action) {
    BrowserFeatureMenuAction.ToggleFavorite -> Modifier.testTag(BrowserMainMenuTestTags.Favorite)
    BrowserFeatureMenuAction.TogglePinned -> Modifier.testTag(BrowserMainMenuTestTags.Pin)
    BrowserFeatureMenuAction.TranslatePage -> Modifier.testTag(BrowserMainMenuTestTags.Translate)
    BrowserFeatureMenuAction.ToggleSplitView -> Modifier.testTag(BrowserMainMenuTestTags.SplitView)
    BrowserFeatureMenuAction.FindInPage -> Modifier.testTag(BrowserMainMenuTestTags.FindInPage)
    BrowserFeatureMenuAction.DuplicateTab -> Modifier.testTag(BrowserMainMenuTestTags.DuplicateTab)
    BrowserFeatureMenuAction.ToggleDesktopView -> Modifier.testTag(BrowserMainMenuTestTags.DesktopView)
    BrowserFeatureMenuAction.ToggleDomainMute -> Modifier.testTag(DomainMuteMenuTestTags.Item)
    BrowserFeatureMenuAction.SnoozeTab -> Modifier.testTag(BrowserMainMenuTestTags.Snooze)
    BrowserFeatureMenuAction.DockAddressBar -> Modifier.testTag(BrowserMainMenuTestTags.DockAddressBar)
    BrowserFeatureMenuAction.OpenSnoozedTabs -> Modifier.testTag(BrowserMainMenuTestTags.SnoozedTabs)
    BrowserFeatureMenuAction.OpenFavorites -> Modifier.testTag(BrowserMainMenuTestTags.Favorites)
    BrowserFeatureMenuAction.OpenDownloads -> Modifier.testTag(BrowserMainMenuTestTags.Downloads)
    BrowserFeatureMenuAction.OpenHistory -> Modifier.testTag(BrowserMainMenuTestTags.History)
    BrowserFeatureMenuAction.OpenFirefoxExtensions ->
        Modifier.testTag(BrowserMainMenuTestTags.FirefoxExtensions)
    BrowserFeatureMenuAction.OpenSettings -> Modifier.testTag(BrowserMainMenuTestTags.Settings)
    BrowserFeatureMenuAction.OpenPasswords -> Modifier.testTag(BrowserMainMenuTestTags.Passwords)
    BrowserFeatureMenuAction.OpenMore -> Modifier.testTag(BrowserMainMenuTestTags.More)
    BrowserFeatureMenuAction.CloseTab -> Modifier.testTag(BrowserMainMenuTestTags.CloseTab)
    BrowserFeatureMenuAction.InvokeToppingCommand ->
        Modifier.testTag(BrowserMainMenuTestTags.userScriptCommand(toppingCommandId.orEmpty()))
    else -> Modifier
}

object BrowserMainMenuTestTags {
    const val Menu = "browser_main_menu"
    const val Toolbar = "browser_main_menu_toolbar"
    const val Favorite = "browser_main_menu_favorite"
    const val Pin = "browser_main_menu_pin"
    const val PageGroup = "browser_main_menu_page_group"
    const val Translate = "browser_main_menu_translate"
    const val SplitView = "browser_main_menu_split_view"
    const val More = "browser_main_menu_more"
    const val MoreBack = "browser_main_menu_more_back"
    const val MoreGroup = "browser_main_menu_more_group"
    const val CloseTab = "browser_main_menu_close_tab"
    const val Passwords = "browser_main_menu_passwords"
    const val ToppingsGroup = "browser_main_menu_toppings_group"
    const val BrowserGroup = "browser_main_menu_browser_group"
    const val Favorites = "browser_main_menu_favorites"
    const val Downloads = "browser_main_menu_downloads"
    const val History = "browser_main_menu_history"
    const val FirefoxExtensions = "browser_main_menu_firefox_extensions"
    const val Settings = "browser_main_menu_settings"
    const val Snooze = "browser_main_menu_snooze"
    const val SnoozedTabs = "browser_main_menu_snoozed_tabs"
    const val DockAddressBar = "browser_main_menu_dock_address_bar"
    const val DesktopView = "browser_main_menu_desktop_view"
    const val FindInPage = "browser_main_menu_find_in_page"
    const val DuplicateTab = "browser_main_menu_duplicate_tab"

    fun userScriptCommand(commandId: String): String =
        "browser_main_menu_topping_command_$commandId"
}

object DomainMuteMenuTestTags {
    const val Item = "domain_mute_menu_item"
}

const val BROWSER_MAIN_MENU_MAX_HEIGHT_FRACTION = 0.8f
