package dev.sk2andy.materialbrowser.ui.v5

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Density
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.browser.AddressResolver
import dev.sk2andy.materialbrowser.browser.ClipboardOffer
import dev.sk2andy.materialbrowser.browser.commands.AddressSuggestionItem
import dev.sk2andy.materialbrowser.shared.ui.icons.VolaIcons
import dev.sk2andy.materialbrowser.ui.PrivateKeyboard
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Materials
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Motion
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Sizes
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Spacing
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Theme
import dev.sk2andy.materialbrowser.ui.theme.v5.v5Elevation
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Shapes
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Elevation
import dev.sk2andy.materialbrowser.ui.theme.v5.SmoothCornerShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.drawBehind
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.border
import kotlin.math.roundToInt

/*
 * The v5 address editor (system-v5.md, section 9; canvas screen «c»). The field stays a capsule
 * right above the keyboard with «Cancel» beside it; suggestions grow up from the field, the best
 * match closest to the thumb, and the top of the list fades into the material. Everything under it
 * is the thick material, nearly opaque: the live page is a SurfaceView no blur can see.
 */

object V5EditorTestTags {
    const val Editor = "v5_address_editor"
    const val Field = "v5_address_editor_field"
    const val Cancel = "v5_address_editor_cancel"
    const val Clipboard = "v5_address_editor_clipboard"
    fun row(item: AddressSuggestionItem) = "v5_address_suggestion:${item.stableId}"
}

/** What the editor shows; callbacks go to [V5AddressEditor]. */
@Immutable
data class V5EditorState(
    val value: TextFieldValue,
    val layout: V5SuggestionLayout,
    /** The suggestion a hardware keyboard has highlighted, by [AddressSuggestionItem.stableId]. */
    val highlightedId: String? = null,
    /** A clip the editor may offer while nothing is typed; read only when tapped. */
    val clipboard: ClipboardOffer? = null,
    val isPrivate: Boolean = false,
    /** Changes every time the editor opens, to focus the field and raise the keyboard. */
    val focusNonce: Int = 0,
    /**
     * Over a new tab: while nothing is typed, the tab's pinned sites show through and take taps,
     * as Safari keeps Favorites under an empty field; the editor keeps only its field.
     */
    val overStart: Boolean = false,
)

@Composable
internal fun V5AddressEditor(
    state: V5EditorState,
    onValueChange: (TextFieldValue) -> Unit,
    onSubmit: (String) -> Unit,
    onSelect: (AddressSuggestionItem) -> Unit,
    onFill: (AddressSuggestionItem) -> Unit,
    onClipboard: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    onHighlight: (AddressSuggestionItem?) -> Unit = {},
    /** A command's answer, a line right above the field while the editor stays open. */
    notice: V5CommandNotice? = null,
) {
    val colors = V5Theme.colors
    // The morph (system-v5.md section 7, canvas «MotionAddress»): the editor rises from the bar's
    // height while its field stretches out of the bar's capsule to its full width, on the
    // addressMorph spring; without animations it only fades in.
    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        appear.animateTo(
            targetValue = 1f,
            animationSpec = if (V5Motion.animationsEnabled) V5Motion.addressMorph.spec<Float>() else V5Motion.reducedFade<Float>(),
        )
    }
    val moves = V5Motion.animationsEnabled
    val seeThrough = V5EditorRules.seeThrough(state)
    Box(
        modifier
            .fillMaxSize()
            .graphicsLayer { alpha = appear.value.coerceIn(0f, 1f) }
            .background(
                if (seeThrough) {
                    Color.Transparent
                } else {
                    V5Materials.thick.tint(dark = colors.isDark, blurAvailable = false, reduceTransparency = false)
                },
            )
            .testTag(V5EditorTestTags.Editor),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars))
                .graphicsLayer {
                    if (moves) translationY = (1f - appear.value) * V5Sizes.bar.toPx()
                },
        ) {
            if (seeThrough) {
                // Nothing here takes a touch, so a tap reaches the pinned site under it.
                Spacer(Modifier.weight(1f))
            } else {
                SuggestionList(
                    state = state,
                    onSelect = onSelect,
                    onFill = onFill,
                    onClipboard = onClipboard,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                )
            }
            V5CommandNoticeAccessory(notice)
            EditorField(
                state = state,
                onValueChange = onValueChange,
                onSubmit = onSubmit,
                onCancel = onCancel,
                onArrow = { step ->
                    onHighlight(V5SuggestionLayoutRules.step(state.layout, state.highlightedId, step))
                },
                morph = { if (moves) appear.value else 1f },
            )
        }
    }
}

@Composable
private fun EditorField(
    state: V5EditorState,
    onValueChange: (TextFieldValue) -> Unit,
    onSubmit: (String) -> Unit,
    onCancel: () -> Unit,
    onArrow: (Int) -> Unit,
    /** How far the field has stretched out of the bar's capsule: 0 at the bar, 1 open; read while laid out. */
    morph: () -> Float = { 1f },
) {
    val colors = V5Theme.colors
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(state.focusNonce) {
        focus.requestFocus()
        keyboard?.show()
    }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = V5Spacing.space3, end = V5Spacing.space2, top = V5Spacing.space2, bottom = V5Spacing.space2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            Modifier
                .weight(1f)
                // At the start the capsule ends where the bar's does, before its two round buttons;
                // it grows to the right over them while «Cancel» fades in.
                .layout { measurable, constraints ->
                    val full = constraints.maxWidth
                    val width = (full - V5EditorRules.morphInsetPx(this) * (1f - morph().coerceIn(0f, 1f)))
                        .roundToInt()
                        // The weight hands a fixed width; the capsule draws narrower inside it.
                        .coerceIn(0, full)
                    val placeable = measurable.measure(constraints.copy(minWidth = width, maxWidth = width))
                    layout(full, placeable.height) { placeable.place(0, 0) }
                }
                .heightIn(min = V5Sizes.barCapsule)
                .v5Elevation(V5Elevation.e2, V5Theme.shapes.full)
                .clip(V5Theme.shapes.full)
                .background(colors.surface)
                .padding(start = V5Spacing.space3),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(V5Spacing.space2),
        ) {
            Icon(
                VolaIcons.Search,
                contentDescription = null,
                tint = colors.labelSecondary,
                modifier = Modifier.size(V5Sizes.iconSmall),
            )
            val hint = stringResource(R.string.address_empty_hint)
            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (state.value.text.isEmpty()) {
                    // The field carries the hint for TalkBack; this is only what the eye reads.
                    Text(
                        hint,
                        style = V5Theme.type.body,
                        color = colors.labelTertiary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.clearAndSetSemantics {},
                    )
                }
                PrivateKeyboard(isPrivate = state.isPrivate) {
                    BasicTextField(
                        value = state.value,
                        onValueChange = onValueChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focus)
                            .onPreviewKeyEvent { event ->
                                // A hardware keyboard walks the list: up goes away from the field; Esc
                                // cancels, as «Cancel» does (А-5).
                                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                                when (event.key) {
                                    Key.DirectionUp -> true.also { onArrow(1) }
                                    Key.DirectionDown -> true.also { onArrow(-1) }
                                    Key.Escape -> true.also { onCancel() }
                                    else -> false
                                }
                            }
                            .v5FieldLabel(hint, empty = state.value.text.isEmpty())
                            .testTag(V5EditorTestTags.Field),
                        singleLine = true,
                        textStyle = V5Theme.type.body.copy(color = colors.label),
                        cursorBrush = SolidColor(colors.accent),
                        // Addresses and queries: no autocorrect turning «github» into «gotham».
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Uri,
                            autoCorrectEnabled = false,
                            imeAction = ImeAction.Go,
                        ),
                        keyboardActions = KeyboardActions(onGo = { onSubmit(state.value.text) }),
                    )
                }
            }
            if (state.value.text.isNotEmpty()) {
                IconButton(onClick = { onValueChange(TextFieldValue()) }) {
                    // A small gray circle with the cross cut out of it (canvas «Address»).
                    Box(
                        Modifier
                            .size(V5Sizes.editorClear)
                            .clip(V5Theme.shapes.full)
                            .background(colors.labelTertiary),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            VolaIcons.Close,
                            contentDescription = stringResource(R.string.v5_editor_clear),
                            tint = colors.surface,
                            modifier = Modifier.size(V5Sizes.overviewFavicon),
                        )
                    }
                }
            }
        }
        Box(
            Modifier
                .widthIn(min = V5Sizes.editorCancel)
                .heightIn(min = V5Sizes.touchTarget)
                .graphicsLayer { alpha = morph().coerceIn(0f, 1f) }
                .clickable(role = Role.Button, onClick = onCancel)
                .testTag(V5EditorTestTags.Cancel),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                stringResource(R.string.action_cancel),
                style = V5Theme.type.body.copy(fontWeight = FontWeight.Medium),
                color = colors.accent,
            )
        }
    }
}

@Composable
private fun SuggestionList(
    state: V5EditorState,
    onSelect: (AddressSuggestionItem) -> Unit,
    onFill: (AddressSuggestionItem) -> Unit,
    onClipboard: () -> Unit,
    modifier: Modifier,
) {
    val query = state.value.text
    val layout = state.layout
    val clipboard = state.clipboard?.takeIf { query.isBlank() }
    if (layout.isEmpty && clipboard == null) {
        Box(modifier)
        return
    }
    // reverseLayout: the first item sits at the bottom, by the field, and the list grows upwards.
    LazyColumn(
        modifier = modifier.fadingTop(V5Theme.colors.label.copy(alpha = 1f)),
        reverseLayout = true,
    ) {
        if (clipboard != null) {
            item(key = "clipboard") { ClipboardRow(clipboard, onClipboard) }
        }
        layout.best?.let { best ->
            item(key = "best:${best.stableId}") {
                SuggestionRow(best, query, best = true, state.highlightedId, onSelect, onFill)
            }
            header(key = "best", R.string.v5_suggestion_best)
        }
        layout.sections.forEach { section ->
            // Rows keep their order from the top down, so they go in reversed. Each group is one
            // card (canvas «Address»): the top row rounds the card's top, the last its bottom.
            val count = section.items.size
            itemsIndexed(section.items.asReversed(), key = { _, item -> item.stableId }) { reversedIndex, item ->
                val index = count - 1 - reversedIndex
                SuggestionRow(
                    item,
                    query,
                    best = false,
                    state.highlightedId,
                    onSelect,
                    onFill,
                    first = index == 0,
                    last = index == count - 1,
                )
            }
            header(key = section.group.name, section.group.title())
        }
    }
}

private fun LazyListScope.header(key: String, title: Int) {
    item(key = "header:$key") {
        Text(
            stringResource(title),
            style = V5Theme.type.footnote,
            color = V5Theme.colors.labelSecondary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = V5Spacing.space4, end = V5Spacing.space4, top = V5Spacing.space2, bottom = V5Spacing.space1)
                .semantics { heading() },
        )
    }
}

private fun V5SuggestionGroup.title(): Int = when (this) {
    V5SuggestionGroup.OpenTabs -> R.string.v5_suggestion_open_tabs
    V5SuggestionGroup.Favorites -> R.string.favorites_title
    V5SuggestionGroup.History -> R.string.history_title
    V5SuggestionGroup.PageMemory -> R.string.recall_settings_title
    V5SuggestionGroup.Search -> R.string.v5_suggestion_search
    V5SuggestionGroup.Commands -> R.string.v5_suggestion_commands
}

/** One suggestion as the row shows it. */
private data class RowContent(
    val icon: ImageVector,
    val title: String,
    val subtitle: String?,
    val openTab: Boolean = false,
    val fillable: Boolean = true,
    /** A page's site: its letter tile stands in for the group's symbol (canvas «Address»). */
    val site: String? = null,
)

private fun AddressSuggestionItem.content(): RowContent = when (this) {
    is AddressSuggestionItem.Navigation -> {
        val host = AddressResolver.displayText(suggestion.url)
        val openTab = suggestion.openTabId != null
        RowContent(
            icon = when (V5SuggestionLayoutRules.group(this)) {
                V5SuggestionGroup.OpenTabs -> VolaIcons.Tab
                V5SuggestionGroup.Favorites -> VolaIcons.Star
                else -> VolaIcons.History
            },
            title = suggestion.title.ifBlank { host },
            subtitle = host.takeIf { suggestion.title.isNotBlank() },
            openTab = openTab,
            site = host,
        )
    }
    is AddressSuggestionItem.Recall -> RowContent(
        icon = VolaIcons.Description,
        title = match.title.ifBlank { AddressResolver.displayText(match.url) },
        subtitle = match.excerpt.ifBlank { null },
    )
    is AddressSuggestionItem.Search -> RowContent(VolaIcons.Search, query, subtitle = null)
    is AddressSuggestionItem.Command -> RowContent(
        icon = VolaIcons.Build,
        title = suggestion.name,
        subtitle = suggestion.effect,
        fillable = false,
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SuggestionRow(
    item: AddressSuggestionItem,
    query: String,
    best: Boolean,
    highlightedId: String?,
    onSelect: (AddressSuggestionItem) -> Unit,
    onFill: (AddressSuggestionItem) -> Unit,
    first: Boolean = true,
    last: Boolean = true,
) {
    val colors = V5Theme.colors
    val content = item.content()
    val highlighted = item.stableId == highlightedId
    // A search row fills the field with its own button: a long press saying the same would be a
    // second «Fill in» for TalkBack (А-4).
    val longPressFills = content.fillable && item !is AddressSuggestionItem.Search
    // The best match is its own card (radius m, e2, an accent outline); a group's rows share one card
    // of radius s, its corners on the first and last row, hairlines between them.
    val radius = V5Shapes.radiusS
    val shape = if (best) {
        V5Theme.shapes.m
    } else {
        SmoothCornerShape(
            topStart = if (first) radius else 0.dp,
            topEnd = if (first) radius else 0.dp,
            bottomEnd = if (last) radius else 0.dp,
            bottomStart = if (last) radius else 0.dp,
        )
    }
    val container = if (highlighted) colors.fill else colors.surface
    val separator = colors.separator
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = V5Spacing.space2)
            .heightIn(min = if (best) V5Sizes.suggestionBest else V5Sizes.suggestionRow)
            .then(if (best) Modifier.v5Elevation(V5Elevation.e2, shape) else Modifier)
            .clip(shape)
            .background(container, shape)
            .then(if (best) Modifier.border(V5Sizes.suggestionBestBorder, colors.accent, shape) else Modifier)
            .drawBehind {
                if (best || last) return@drawBehind
                val inset = (V5Spacing.space4 + V5Sizes.suggestionTile + V5Spacing.space3).toPx()
                val y = size.height - V5Spacing.hairline.toPx() / 2
                drawLine(separator, Offset(inset, y), Offset(size.width, y), strokeWidth = V5Spacing.hairline.toPx())
            }
            .semantics { selected = highlighted }
            .combinedClickable(
                role = Role.Button,
                onClick = { onSelect(item) },
                onLongClick = if (longPressFills) ({ onFill(item) }) else null,
                onLongClickLabel = stringResource(R.string.address_suggestion_fill).takeIf { longPressFills },
            )
            .padding(start = V5Spacing.space4, end = V5Spacing.space2)
            .testTag(V5EditorTestTags.row(item)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(V5Spacing.space3),
    ) {
        val tileSize = if (best) V5Sizes.suggestionBestTile else V5Sizes.suggestionTile
        if (content.site != null) {
            V5SiteTile(favicon = null, site = content.site, size = tileSize)
        } else {
            Box(
                Modifier
                    .size(tileSize)
                    .clip(V5Theme.shapes.xs)
                    .background(if (best) colors.accentSoft else colors.fill),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    content.icon,
                    contentDescription = null,
                    tint = if (best) colors.accent else colors.labelSecondary,
                    modifier = Modifier.size(V5Sizes.iconSmall),
                )
            }
        }
        Column(Modifier.weight(1f)) {
            Text(
                highlightedTitle(content.title, query),
                style = V5Theme.type.body,
                color = colors.label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            // The best match that is an open tab says so under its title, in the accent.
            val switchHint = if (best && content.openTab) stringResource(R.string.address_suggestion_switch) else null
            val subtitle = listOfNotNull(switchHint, content.subtitle).joinToString(" · ").ifEmpty { null }
            subtitle?.let {
                Text(
                    it,
                    style = V5Theme.type.footnote,
                    color = if (switchHint != null) colors.accent else colors.labelSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        when {
            content.openTab && !best -> Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(V5Spacing.space1),
            ) {
                Icon(VolaIcons.Tab, contentDescription = null, tint = colors.accent, modifier = Modifier.size(V5Sizes.iconSmall))
                Text(
                    stringResource(R.string.v5_suggestion_tab),
                    style = V5Theme.type.footnote.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.accent,
                )
            }
            item is AddressSuggestionItem.Search -> IconButton(onClick = { onFill(item) }) {
                Icon(
                    VolaIcons.NorthWest,
                    contentDescription = stringResource(R.string.address_suggestion_fill),
                    tint = colors.labelTertiary,
                    modifier = Modifier.size(V5Sizes.iconSmall),
                )
            }
            // A best match other than a search ends in a round accent «go» (canvas «Address»); the row itself is the button.
            best -> Box(
                Modifier
                    .size(V5Sizes.suggestionTile)
                    .clip(V5Theme.shapes.full)
                    .background(colors.accentFill)
                    .clearAndSetSemantics {},
                contentAlignment = Alignment.Center,
            ) {
                Icon(VolaIcons.ArrowForward, contentDescription = null, tint = colors.onAccentFill, modifier = Modifier.size(V5Sizes.iconSmall))
            }
            else -> Unit
        }
    }
}

@Composable
private fun ClipboardRow(offer: ClipboardOffer, onClick: () -> Unit) {
    val colors = V5Theme.colors
    val link = offer == ClipboardOffer.Link
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = V5Sizes.suggestionRow)
            .clickable(
                role = Role.Button,
                onClickLabel = stringResource(if (link) R.string.address_clipboard_open else R.string.address_clipboard_paste),
                onClick = onClick,
            )
            .padding(start = V5Spacing.space4, end = V5Spacing.space4)
            .testTag(V5EditorTestTags.Clipboard),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(V5Spacing.space3),
    ) {
        Box(
            Modifier
                .size(V5Sizes.suggestionTile)
                .clip(V5Theme.shapes.xs)
                .background(colors.accentSoft),
            contentAlignment = Alignment.Center,
        ) {
            Icon(VolaIcons.ContentPaste, contentDescription = null, tint = colors.accent, modifier = Modifier.size(V5Sizes.iconSmall))
        }
        Text(
            stringResource(if (link) R.string.address_clipboard_link else R.string.address_clipboard_text),
            style = V5Theme.type.body,
            color = colors.label,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            stringResource(if (link) R.string.address_clipboard_open else R.string.address_clipboard_paste),
            style = V5Theme.type.footnote.copy(fontWeight = FontWeight.SemiBold),
            color = colors.accent,
        )
    }
}

/** [title] with the typed part in semibold, as Safari marks it. */
private fun highlightedTitle(title: String, query: String): AnnotatedString {
    val range = V5SuggestionLayoutRules.match(title, query) ?: return AnnotatedString(title)
    return buildAnnotatedString {
        append(title.substring(0, range.first))
        withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(title.substring(range)) }
        append(title.substring(range.last + 1))
    }
}

/**
 * The top of the list fades out over [V5Sizes.suggestionFade], so long lists end softly. [mask] is
 * any opaque color: only its alpha counts.
 */
private fun Modifier.fadingTop(mask: Color): Modifier = this
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        val fade = (V5Sizes.suggestionFade.toPx() / size.height).coerceIn(0f, 1f)
        drawRect(
            brush = Brush.verticalGradient(0f to mask.copy(alpha = 0f), fade to mask),
            blendMode = BlendMode.DstIn,
        )
    }

internal object V5EditorRules {
    /**
     * How much narrower the field starts than it ends (canvas «MotionAddress»): in the bar the
     * capsule stops before the tabs and menu buttons, in the editor only before «Cancel». Never
     * below nothing, should the editor's end ever be the wider one.
     */
    fun morphInsetPx(density: Density): Float = with(density) {
        val barEnd = V5Sizes.barCapsule * 2 + V5Spacing.space2 * 2 + V5Spacing.space3
        val editorEnd = V5Sizes.editorCancel + V5Spacing.space2
        (barEnd - editorEnd).toPx().coerceAtLeast(0f)
    }

    /**
     * The new tab shows through while nothing is typed; a clip on offer keeps the list, since the
     * offer lives there.
     */
    fun seeThrough(state: V5EditorState): Boolean =
        state.overStart && state.value.text.isEmpty() && state.clipboard == null
}
