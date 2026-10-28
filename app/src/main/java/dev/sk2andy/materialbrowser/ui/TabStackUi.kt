package dev.sk2andy.materialbrowser.ui

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed as lazyItemsIndexed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed as gridItemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.sk2andy.materialbrowser.browser.EssentialsController
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.browser.AddressResolver
import dev.sk2andy.materialbrowser.browser.BLANK_URL
import dev.sk2andy.materialbrowser.browser.BrowserTab
import dev.sk2andy.materialbrowser.browser.TabStack
import dev.sk2andy.materialbrowser.browser.TabStackColor
import dev.sk2andy.materialbrowser.data.TabOverviewMode
import dev.sk2andy.materialbrowser.shared.ui.icons.VolaIcons
import kotlinx.coroutines.delay

internal object TabStackTestTags {
    const val Dialog = "tab-stack-dialog"
    const val Create = "tab-stack-create"
    const val Folder = "tab-stack-folder"
    const val FolderHero = "tab-stack-folder-hero"
    const val FolderGrid = "tab-stack-folder-grid"
    const val FolderList = "tab-stack-folder-list"
    const val Name = "tab-stack-name"
    const val DialogConfirm = "tab-stack-dialog-confirm"
    const val ColorChoices = "tab-stack-color-choices"

    fun marker(stackId: String, tabId: String) = "tab-stack-marker-$stackId-$tabId"
    fun collapsedCard(stackId: String) = "tab-stack-collapsed-$stackId"
    fun candidate(tabId: String) = "tab-stack-candidate-$tabId"
    fun previewChoice(tabId: String) = "tab-stack-preview-$tabId"
    fun color(color: TabStackColor) = "tab-stack-color-${color.wireValue}"
    fun folderTab(tabId: String) = "tab-stack-folder-tab-$tabId"
    fun motionCard(tabId: String) = "tab-stack-motion-card-$tabId"
}

@Composable
internal fun TabStackMarker(
    stack: TabStack,
    tabId: String,
    onToggleCollapsed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = stackColors(stack.color)
    val arrowRotation by animateFloatAsState(
        targetValue = if (stack.isCollapsed) 0f else 180f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 560f),
        label = "stackMarkerArrow",
    )
    val markerScale by animateFloatAsState(
        targetValue = if (stack.isCollapsed) 1f else 0.96f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 620f),
        label = "stackMarkerScale",
    )
    Surface(
        onClick = onToggleCollapsed,
        modifier = modifier
            .minimumInteractiveComponentSize()
            .graphicsLayer {
                scaleX = markerScale
                scaleY = markerScale
            }
            .testTag(TabStackTestTags.marker(stack.id, tabId)),
        shape = RoundedCornerShape(12.dp),
        color = colors.container.copy(alpha = 0.94f),
        contentColor = colors.content,
        shadowElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier.padding(start = 8.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.tab_stack_badge, stack.name, stack.tabIds.size),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
            )
            Icon(
                imageVector = VolaIcons.KeyboardArrowDown,
                contentDescription = stringResource(
                    if (stack.isCollapsed) R.string.action_expand_tab_stack
                    else R.string.action_collapse_tab_stack,
                ),
                modifier = Modifier
                    .size(18.dp)
                    .graphicsLayer { rotationZ = arrowRotation },
            )
        }
    }
}

@Composable
internal fun TabStackCardFrame(
    stack: TabStack?,
    previewAspectRatio: Float,
    cornerRadius: Dp = 22.dp,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val collapsed = stack?.isCollapsed == true
    val colors = stack?.let { stackColors(it.color) }
    val depthOffset by animateDpAsState(
        targetValue = if (collapsed) 8.dp else 0.dp,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 520f),
        label = "stackDepthOffset",
    )
    val depthProgress by animateFloatAsState(
        targetValue = if (collapsed) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 500f),
        label = "stackDepthProgress",
    )
    val safeDepthOffset = depthOffset.coerceAtLeast(0.dp)
    val safeDepthProgress = depthProgress.coerceIn(0f, 1f)
    Box(
        modifier = modifier
            .padding(end = safeDepthOffset, bottom = safeDepthOffset),
    ) {
        if (safeDepthProgress > 0.001f && colors != null) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(previewAspectRatio)
                    .offset(x = safeDepthOffset, y = safeDepthOffset)
                    .graphicsLayer {
                        alpha = safeDepthProgress * 0.74f
                        rotationZ = safeDepthProgress * 1.2f
                        val scale = 0.965f + safeDepthProgress * 0.035f
                        scaleX = scale
                        scaleY = scale
                    },
                shape = RoundedCornerShape(cornerRadius),
                color = colors.accent.copy(alpha = 0.56f),
            ) {}
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(previewAspectRatio)
                    .offset(x = safeDepthOffset / 2, y = safeDepthOffset / 2)
                    .graphicsLayer {
                        alpha = safeDepthProgress
                        rotationZ = safeDepthProgress * 0.55f
                    }
                    .then(
                        if (collapsed) {
                            Modifier.testTag(
                                TabStackTestTags.collapsedCard(requireNotNull(stack).id),
                            )
                        } else {
                            Modifier
                        },
                    ),
                shape = RoundedCornerShape(cornerRadius),
                color = colors.accent.copy(alpha = 0.72f),
            ) {}
        }
        Box(
            modifier = Modifier.graphicsLayer {
                val scale = 1f - safeDepthProgress * 0.012f
                scaleX = scale
                scaleY = scale
            },
            content = content,
        )
    }
}

@Composable
internal fun TabStackFolderDialog(
    stack: TabStack,
    tabs: List<BrowserTab>,
    mode: TabOverviewMode,
    previews: Map<String, Bitmap>,
    favicons: Map<String, Bitmap>,
    essentials: EssentialsController? = null,
    onSelectTab: (String) -> Unit,
    onPreviewTabChanged: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = stackColors(stack.color)
    val folderVisibility = remember(stack.id) {
        MutableTransitionState(false).apply { targetState = true }
    }
    var pendingDismissAction by remember(stack.id) {
        mutableStateOf<(() -> Unit)?>(null)
    }
    val dismissPending = pendingDismissAction != null
    fun dismissAfter(action: () -> Unit) {
        if (dismissPending) return
        pendingDismissAction = action
        folderVisibility.targetState = false
    }
    LaunchedEffect(
        dismissPending,
        folderVisibility.currentState,
        folderVisibility.isIdle,
    ) {
        if (dismissPending && folderVisibility.isIdle && !folderVisibility.currentState) {
            val action = pendingDismissAction
            pendingDismissAction = null
            action?.invoke()
        }
    }
    Dialog(
        onDismissRequest = { dismissAfter(onDismiss) },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            contentAlignment = Alignment.Center,
        ) {
            val layout = TabStackFolderLayoutRules.layout(maxWidth.value)
            AnimatedVisibility(
                visibleState = folderVisibility,
                enter = fadeIn(
                    tween(TabStackMotionRules.FOLDER_ENTER_DURATION_MILLIS),
                ) + scaleIn(
                    initialScale = 0.9f,
                    animationSpec = spring(dampingRatio = 0.82f, stiffness = 520f),
                ) + slideInVertically(
                    animationSpec = spring(dampingRatio = 0.82f, stiffness = 520f),
                    initialOffsetY = { height -> height / 12 },
                ),
                exit = fadeOut(
                    tween(
                        TabStackMotionRules.FOLDER_EXIT_DURATION_MILLIS,
                        easing = FastOutSlowInEasing,
                    ),
                ) + scaleOut(
                    targetScale = 0.94f,
                    animationSpec = tween(
                        TabStackMotionRules.FOLDER_EXIT_DURATION_MILLIS,
                        easing = FastOutSlowInEasing,
                    ),
                ) + slideOutVertically(
                    animationSpec = tween(
                        TabStackMotionRules.FOLDER_EXIT_DURATION_MILLIS,
                        easing = FastOutSlowInEasing,
                    ),
                    targetOffsetY = { height -> height / 16 },
                ),
            ) {
                Surface(
                    modifier = Modifier
                        .widthIn(max = layout.dialogMaxWidth.dp)
                        .fillMaxWidth()
                        .heightIn(max = 640.dp)
                        .testTag(TabStackTestTags.Folder),
                    shape = RoundedCornerShape(32.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    tonalElevation = 8.dp,
                    shadowElevation = 16.dp,
                ) {
                    Column(
                        modifier = Modifier.padding(TabStackFolderLayoutRules.CONTENT_PADDING.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Surface(
                                modifier = Modifier.size(12.dp),
                                shape = CircleShape,
                                color = colors.accent,
                            ) {}
                            Text(
                                text = stack.name,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 10.dp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                            )
                            IconButton(
                                onClick = { dismissAfter(onDismiss) },
                                enabled = !dismissPending,
                            ) {
                                Icon(
                                    imageVector = VolaIcons.Close,
                                    contentDescription = stringResource(R.string.action_cancel),
                                )
                            }
                        }
                        val animatedSelectTab: (String) -> Unit = { tabId ->
                            dismissAfter { onSelectTab(tabId) }
                        }
                        when (mode) {
                            TabOverviewMode.Hero -> TabStackFolderCoverflow(
                                stack = stack,
                                tabs = tabs,
                                previews = previews,
                                favicons = favicons,
                                essentials = essentials,
                                interactionsEnabled = !dismissPending,
                                pageWidth = layout.coverflowPageWidth.dp,
                                contentPadding = layout.coverflowContentPadding.dp,
                                onSelectTab = animatedSelectTab,
                                onPreviewTabChanged = onPreviewTabChanged,
                            )
                            TabOverviewMode.Grid -> TabStackFolderGrid(
                                stack = stack,
                                tabs = tabs,
                                previews = previews,
                                favicons = favicons,
                                essentials = essentials,
                                interactionsEnabled = !dismissPending,
                                columnCount = layout.gridColumnCount,
                                onSelectTab = animatedSelectTab,
                                onPreviewTabChanged = onPreviewTabChanged,
                            )
                            TabOverviewMode.List -> TabStackFolderList(
                                stack = stack,
                                tabs = tabs,
                                previews = previews,
                                favicons = favicons,
                                essentials = essentials,
                                interactionsEnabled = !dismissPending,
                                onSelectTab = animatedSelectTab,
                                onPreviewTabChanged = onPreviewTabChanged,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TabStackFolderCoverflow(
    stack: TabStack,
    tabs: List<BrowserTab>,
    previews: Map<String, Bitmap>,
    favicons: Map<String, Bitmap>,
    essentials: EssentialsController? = null,
    interactionsEnabled: Boolean,
    pageWidth: Dp,
    contentPadding: Dp,
    onSelectTab: (String) -> Unit,
    onPreviewTabChanged: (String) -> Unit,
) {
    val tabIds = tabs.map(BrowserTab::id)
    val initialPage = remember(stack.id, tabIds) {
        tabs.indexOfFirst { it.id == stack.previewTabId }.coerceAtLeast(0)
    }
    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = tabs::size,
    )
    HorizontalPager(
        state = pagerState,
        modifier = Modifier
            .fillMaxWidth()
            .height(390.dp)
            .testTag(TabStackTestTags.FolderHero),
        contentPadding = PaddingValues(horizontal = contentPadding, vertical = 12.dp),
        pageSpacing = 12.dp,
        pageSize = PageSize.Fixed(pageWidth),
        verticalAlignment = Alignment.CenterVertically,
        userScrollEnabled = interactionsEnabled,
        key = { page -> tabs[page].id },
    ) { page ->
        val pageOffset = (pagerState.currentPage - page) +
            pagerState.currentPageOffsetFraction
        val transform = TabStackMotionRules.folderCoverflowTransform(pageOffset)
        TabStackFolderPreviewCard(
            stack = stack,
            tab = tabs[page],
            preview = previews[tabs[page].id],
            favicon = favicons[tabs[page].id],
            essentials = essentials,
            interactionsEnabled = interactionsEnabled,
            onSelectTab = onSelectTab,
            onPreviewTabChanged = onPreviewTabChanged,
            modifier = Modifier
                .stackFolderItemEntrance(tabs[page].id, page)
                .graphicsLayer {
                    translationY = transform.translationY
                    scaleX = transform.scale
                    scaleY = transform.scale
                    rotationZ = transform.rotationZ
                    alpha = transform.alpha
                    cameraDistance = 18f * density
                    rotationY = pageOffset.coerceIn(-1f, 1f) * -7f
                },
        )
    }
}

@Composable
private fun TabStackFolderGrid(
    stack: TabStack,
    tabs: List<BrowserTab>,
    previews: Map<String, Bitmap>,
    favicons: Map<String, Bitmap>,
    essentials: EssentialsController? = null,
    interactionsEnabled: Boolean,
    columnCount: Int,
    onSelectTab: (String) -> Unit,
    onPreviewTabChanged: (String) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(columnCount),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 540.dp)
            .testTag(TabStackTestTags.FolderGrid),
        contentPadding = PaddingValues(top = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        userScrollEnabled = interactionsEnabled,
    ) {
        gridItemsIndexed(tabs, key = { _, tab -> tab.id }) { index, tab ->
            TabStackFolderPreviewCard(
                stack = stack,
                tab = tab,
                preview = previews[tab.id],
                favicon = favicons[tab.id],
                essentials = essentials,
                interactionsEnabled = interactionsEnabled,
                onSelectTab = onSelectTab,
                onPreviewTabChanged = onPreviewTabChanged,
                modifier = Modifier.stackFolderItemEntrance(tab.id, index),
            )
        }
    }
}

@Composable
private fun TabStackFolderList(
    stack: TabStack,
    tabs: List<BrowserTab>,
    previews: Map<String, Bitmap>,
    favicons: Map<String, Bitmap>,
    essentials: EssentialsController? = null,
    interactionsEnabled: Boolean,
    onSelectTab: (String) -> Unit,
    onPreviewTabChanged: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 540.dp)
            .testTag(TabStackTestTags.FolderList),
        contentPadding = PaddingValues(top = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        userScrollEnabled = interactionsEnabled,
    ) {
        lazyItemsIndexed(tabs, key = { _, tab -> tab.id }) { index, tab ->
            val tabTitle = tabStackTabTitle(tab)
            val previewDescription = stringResource(
                R.string.tab_stack_preview_choice,
                tabTitle,
            )
            Card(
                modifier = Modifier.stackFolderItemEntrance(
                    key = tab.id,
                    index = index,
                    horizontal = true,
                ),
                shape = RoundedCornerShape(18.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .width(104.dp)
                            .height(78.dp)
                            .testTag(TabStackTestTags.folderTab(tab.id))
                            .clickable(
                                enabled = interactionsEnabled,
                                role = Role.Button,
                                onClick = { onSelectTab(tab.id) },
                            )
                            .semantics { contentDescription = tabTitle },
                    ) {
                        TabPreviewContent(
                            tab = tab,
                            preview = previews[tab.id],
                            favicon = favicons[tab.id],
                            essentials = essentials,
                        )
                    }
                    Text(
                        text = tabTitle,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 12.dp),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    RadioButton(
                        selected = stack.previewTabId == tab.id,
                        onClick = { onPreviewTabChanged(tab.id) },
                        enabled = interactionsEnabled,
                        modifier = Modifier
                            .testTag(TabStackTestTags.previewChoice(tab.id))
                            .semantics { contentDescription = previewDescription },
                    )
                }
            }
        }
    }
}

@Composable
private fun Modifier.stackFolderItemEntrance(
    key: String,
    index: Int,
    horizontal: Boolean = false,
): Modifier {
    val progress = remember(key) { Animatable(0f) }
    LaunchedEffect(key) {
        delay(
            index.coerceAtMost(6) * TabStackMotionRules.ITEM_STAGGER_MILLIS.toLong(),
        )
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
        )
    }
    return graphicsLayer {
        val hiddenProgress = 1f - progress.value
        alpha = progress.value
        scaleX = 0.94f + progress.value * 0.06f
        scaleY = 0.94f + progress.value * 0.06f
        translationX = if (horizontal) hiddenProgress * 28.dp.toPx() else 0f
        translationY = if (horizontal) 0f else hiddenProgress * 22.dp.toPx()
    }
}

@Composable
private fun TabStackFolderPreviewCard(
    stack: TabStack,
    tab: BrowserTab,
    preview: Bitmap?,
    favicon: Bitmap?,
    essentials: EssentialsController? = null,
    interactionsEnabled: Boolean,
    onSelectTab: (String) -> Unit,
    onPreviewTabChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tabTitle = tabStackTabTitle(tab)
    val previewDescription = stringResource(R.string.tab_stack_preview_choice, tabTitle)
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.72f)
                    .testTag(TabStackTestTags.folderTab(tab.id))
                    .clickable(
                        enabled = interactionsEnabled,
                        role = Role.Button,
                        onClick = { onSelectTab(tab.id) },
                    )
                    .semantics { contentDescription = tabTitle },
            ) {
                TabPreviewContent(
                    tab = tab,
                    preview = preview,
                    favicon = favicon,
                    essentials = essentials,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = tabTitle,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 10.dp, top = 10.dp, bottom = 10.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelLarge,
                )
                RadioButton(
                    selected = stack.previewTabId == tab.id,
                    onClick = { onPreviewTabChanged(tab.id) },
                    enabled = interactionsEnabled,
                    modifier = Modifier
                        .testTag(TabStackTestTags.previewChoice(tab.id))
                        .semantics { contentDescription = previewDescription },
                )
            }
        }
    }
}

@Composable
private fun tabStackTabTitle(tab: BrowserTab): String {
    val title = tab.title.trim()
    return when {
        hidesPrivateTab(tab) -> stringResource(R.string.private_tab_title)
        title.isNotEmpty() -> title
        tab.url == BLANK_URL -> stringResource(R.string.new_tab_title)
        else -> AddressResolver.displayText(tab.url)
    }
}

private data class TabStackColors(
    val accent: Color,
    val container: Color,
    val content: Color,
)

@Composable
private fun stackColors(color: TabStackColor): TabStackColors {
    val accent = when (color) {
        TabStackColor.Grape -> Color(0xFF7457D7)
        TabStackColor.Cherry -> Color(0xFFFF2F78)
        TabStackColor.Lime -> Color(0xFF7CB342)
        TabStackColor.Blueberry -> Color(0xFF3F6EDB)
        // v5's colors: a group colored in v5 keeps its color here.
        TabStackColor.Orange,
        TabStackColor.Teal,
        TabStackColor.Slate,
        -> color.v5Tile().color
    }
    val content = when (color) {
        TabStackColor.Lime -> Color(0xFF182800)
        else -> Color.White
    }
    return TabStackColors(
        accent = accent,
        container = accent,
        content = content,
    )
}

