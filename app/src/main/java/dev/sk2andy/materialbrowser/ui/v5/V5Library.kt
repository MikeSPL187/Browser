package dev.sk2andy.materialbrowser.ui.v5

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.shared.ui.icons.VolaIcons
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Sizes
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Spacing
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Theme
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Tile

/*
 * The v5 library (V28, B2; canvas screens «р1»–«р3»): one sheet with the segment «Favorites ·
 * History · Downloads» instead of three screens, as Safari's bookmarks sheet. Search filters the
 * segment in front, a row swipes left to be deleted, an empty segment is one line and a glyph.
 */

object V5LibraryTestTags {
    const val Page = "v5_library"
    const val Close = "v5_library_close"
    const val Search = "v5_library_search"
    const val Empty = "v5_library_empty"
    const val Up = "v5_library_up"
    fun row(key: String) = "v5_library_row:$key"
    fun retry(key: String) = "v5_library_retry:$key"
    fun pause(key: String) = "v5_library_pause:$key"
}

/** The segments, in the order the control shows them. */
enum class V5LibrarySegment { Favorites, History, Downloads }

/** What a row starts with. */
@Immutable
sealed interface V5LibraryLeading {
    /** A site: its icon when there is one, else its letter on its [tile]. */
    data class Site(val letter: String, val tile: V5Tile, val icon: ImageBitmap? = null) : V5LibraryLeading

    data object Folder : V5LibraryLeading

    /** A downloaded file: the type's glyph on its tile. */
    data class File(val icon: ImageVector, val tile: V5Tile) : V5LibraryLeading
}

/** What a row ends with. */
@Immutable
sealed interface V5LibraryTrailing {
    data object None : V5LibraryTrailing

    /** A time or a size, in the secondary label. */
    data class Value(val text: String) : V5LibraryTrailing

    /** A folder: how many things it holds, and the chevron. */
    data class Count(val count: Int) : V5LibraryTrailing

    /**
     * A download under way; [fraction] is null while its size is unknown. Where it can pause, the
     * ring is a button with the pause or play glyph inside: [paused] is null where it cannot.
     */
    data class Progress(val fraction: Float?, val paused: Boolean? = null) : V5LibraryTrailing

    /** A failed download that can start again. */
    data object Retry : V5LibraryTrailing
}

@Immutable
data class V5LibraryRow(
    val key: String,
    val title: String,
    val subtitle: String,
    val leading: V5LibraryLeading,
    val trailing: V5LibraryTrailing = V5LibraryTrailing.None,
    /** Whether a swipe to the left deletes it (history and favorites; not a download under way). */
    val deletable: Boolean = true,
    /** What a tap does, for TalkBack, where «open» would not say it (pause, resume, retry). */
    val openLabel: String? = null,
    /** The swipe's action for TalkBack where it is not «Delete», such as cancelling a download. */
    val deleteLabel: String? = null,
    /** The swipe asks first (a file, a download under way): the row springs back under the question. */
    val deleteAsks: Boolean = false,
)

/** A group of rows under an optional header, such as a day of history. */
@Immutable
data class V5LibrarySection(val title: String?, val rows: List<V5LibraryRow>)

/** The quiet actions at the bottom of a segment: one at the start, one at the end. */
@Immutable
data class V5LibraryActions(
    val start: String? = null,
    val onStart: () -> Unit = {},
    val end: String? = null,
    val onEnd: () -> Unit = {},
    /** «Clear…» deletes: its label is in the danger color. */
    val endDestructive: Boolean = false,
)

@Immutable
data class V5LibraryState(
    val segment: V5LibrarySegment,
    val query: String,
    val sections: List<V5LibrarySection>,
    /** Shown when [sections] hold no row: nothing yet, or nothing found for [query]. */
    val emptyMessage: String,
    val actions: V5LibraryActions = V5LibraryActions(),
    /** The open folder's name, with a way back up; null at the top. */
    val folder: String? = null,
)

@Composable
fun V5Library(
    state: V5LibraryState,
    onSegmentChange: (V5LibrarySegment) -> Unit,
    onQueryChange: (String) -> Unit,
    onOpen: (String) -> Unit,
    onDelete: (String) -> Unit,
    onRetry: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    /** Back up out of [V5LibraryState.folder]; system back does the same. */
    onUp: () -> Unit = {},
) {
    val colors = V5Theme.colors
    BackHandler(enabled = state.folder != null, onBack = onUp)
    Column(
        modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .windowInsetsPadding(V5SafeSides)
            .testTag(V5LibraryTestTags.Page),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = V5Spacing.marginPhone, end = V5Spacing.space2, top = V5Spacing.space2),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.v5_library_title),
                style = V5Theme.type.title,
                color = colors.label,
                modifier = Modifier.weight(1f).semantics { heading() },
            )
            IconButton(onClick = onClose, modifier = Modifier.testTag(V5LibraryTestTags.Close)) {
                Icon(VolaIcons.Close, contentDescription = stringResource(R.string.v5_library_close), tint = colors.labelSecondary)
            }
        }
        V5Segmented(
            options = listOf(
                stringResource(R.string.v5_library_favorites),
                stringResource(R.string.v5_library_history),
                stringResource(R.string.v5_library_downloads),
            ),
            selectedIndex = state.segment.ordinal,
            onSelect = { index -> onSegmentChange(V5LibrarySegment.entries[index]) },
            modifier = Modifier.padding(horizontal = V5Spacing.marginPhone, vertical = V5Spacing.space2),
        )
        V5SearchField(
            query = state.query,
            onQueryChange = onQueryChange,
            hint = stringResource(
                when (state.segment) {
                    V5LibrarySegment.Favorites -> R.string.v5_library_search_favorites
                    V5LibrarySegment.History -> R.string.v5_library_search_history
                    V5LibrarySegment.Downloads -> R.string.v5_library_search_downloads
                },
            ),
            modifier = Modifier.fillMaxWidth().padding(horizontal = V5Spacing.marginPhone),
            fieldModifier = Modifier.testTag(V5LibraryTestTags.Search),
        )
        if (state.folder != null) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = V5Spacing.space1, end = V5Spacing.marginPhone, top = V5Spacing.space2)
                    .clip(V5Theme.shapes.full)
                    .clickable(role = Role.Button, onClickLabel = stringResource(R.string.v5_library_up), onClick = onUp)
                    .heightIn(min = V5Sizes.touchTarget)
                    .testTag(V5LibraryTestTags.Up),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(V5Spacing.space1),
            ) {
                Icon(VolaIcons.KeyboardArrowLeft, contentDescription = null, tint = colors.accent)
                Text(
                    state.folder,
                    style = V5Theme.type.headline,
                    color = colors.label,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.semantics { heading() },
                )
            }
        }
        val rows = state.sections.sumOf { section -> section.rows.size }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (rows == 0) {
                Empty(state.segment, state.emptyMessage)
            } else {
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = V5Spacing.space3, bottom = V5Spacing.space4),
                ) {
                    state.sections.forEach { section ->
                        if (section.rows.isEmpty()) return@forEach
                        if (section.title != null) {
                            item(key = "header:${section.title}") {
                                Text(
                                    section.title,
                                    style = V5Theme.type.headline,
                                    color = colors.label,
                                    modifier = Modifier
                                        .padding(
                                            start = V5Spacing.marginPhone + V5Spacing.space1,
                                            end = V5Spacing.marginPhone,
                                            top = V5Spacing.space3,
                                            bottom = V5Spacing.space2,
                                        )
                                        .semantics { heading() },
                                )
                            }
                        }
                        items(section.rows, key = { row -> row.key }) { row ->
                            LibraryRow(row, onOpen, onDelete, onRetry)
                        }
                    }
                }
            }
        }
        Actions(state.actions)
    }
}

@Composable
private fun Empty(segment: V5LibrarySegment, message: String) {
    val colors = V5Theme.colors
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = V5Spacing.space8)
            .testTag(V5LibraryTestTags.Empty),
        verticalArrangement = Arrangement.spacedBy(V5Spacing.space3, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            when (segment) {
                V5LibrarySegment.Favorites -> VolaIcons.Star
                V5LibrarySegment.History -> VolaIcons.History
                V5LibrarySegment.Downloads -> VolaIcons.Download
            },
            contentDescription = null,
            tint = colors.labelTertiary,
            modifier = Modifier.size(V5Sizes.iconMax),
        )
        Text(message, style = V5Theme.type.callout, color = colors.labelSecondary, textAlign = TextAlign.Center)
    }
}

@Composable
private fun LibraryRow(
    row: V5LibraryRow,
    onOpen: (String) -> Unit,
    onDelete: (String) -> Unit,
    onRetry: (String) -> Unit,
) {
    val colors = V5Theme.colors
    val deleteLabel = row.deleteLabel ?: stringResource(R.string.v5_library_delete)
    val content: @Composable () -> Unit = {
        Row(
            Modifier
                .fillMaxWidth()
                .background(colors.background)
                .padding(horizontal = V5Spacing.marginPhone)
                .clip(V5Theme.shapes.s)
                .background(colors.surface)
                .heightIn(min = V5Sizes.rowTwoLine)
                .clickable(role = Role.Button, onClickLabel = row.openLabel) { onOpen(row.key) }
                // A swipe is not the only way: TalkBack offers «Delete» as an action of the row.
                .semantics(mergeDescendants = true) {
                    if (row.deletable) {
                        customActions = listOf(CustomAccessibilityAction(deleteLabel) { onDelete(row.key); true })
                    }
                }
                .padding(horizontal = V5Spacing.space4, vertical = V5Spacing.space2)
                .testTag(V5LibraryTestTags.row(row.key)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(V5Spacing.space3),
        ) {
            Leading(row.leading)
            Column(Modifier.weight(1f)) {
                Text(row.title, style = V5Theme.type.body, color = colors.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                // A folder has no second line: its count sits at the end.
                if (row.subtitle.isNotEmpty()) {
                    Text(
                        row.subtitle,
                        style = V5Theme.type.footnote,
                        color = colors.labelSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Trailing(row, onOpen, onRetry)
        }
    }
    if (!row.deletable) {
        Box(Modifier.padding(vertical = V5Spacing.hairline)) { content() }
        return
    }
    Box(Modifier.padding(vertical = V5Spacing.hairline)) {
        V5SwipeToDelete(key = row.key, onDelete = { onDelete(row.key) }, springsBack = row.deleteAsks, content = content)
    }
}

/** Pure rules of the library, for unit tests. */
object V5LibraryRules {
    /** A row swiped to the left past the overview's card-close share of its width is deleted. */
    fun deletes(offsetPx: Float, widthPx: Float): Boolean = V5SwipeToDeleteRules.deletes(offsetPx, widthPx)
}

@Composable
private fun Leading(leading: V5LibraryLeading) {
    val colors = V5Theme.colors
    when (leading) {
        is V5LibraryLeading.Site -> Box(
            Modifier
                .size(V5Sizes.suggestionTile)
                .clip(V5Theme.shapes.xs)
                .background(if (leading.icon != null) colors.fill else leading.tile.color),
            contentAlignment = Alignment.Center,
        ) {
            if (leading.icon != null) {
                Image(leading.icon, contentDescription = null, modifier = Modifier.size(V5Sizes.iconSmall))
            } else {
                Text(
                    leading.letter,
                    style = V5Theme.type.footnote.copy(fontWeight = FontWeight.SemiBold),
                    color = leading.tile.glyph,
                )
            }
        }
        V5LibraryLeading.Folder -> Box(Modifier.size(V5Sizes.suggestionTile), contentAlignment = Alignment.Center) {
            Icon(VolaIcons.Folder, contentDescription = null, tint = colors.accent, modifier = Modifier.size(V5Sizes.icon))
        }
        is V5LibraryLeading.File -> Box(
            Modifier
                .size(V5Sizes.suggestionTile)
                .clip(V5Theme.shapes.xs)
                .background(leading.tile.color),
            contentAlignment = Alignment.Center,
        ) {
            Icon(leading.icon, contentDescription = null, tint = leading.tile.glyph, modifier = Modifier.size(V5Sizes.iconSmall))
        }
    }
}

@Composable
private fun Trailing(row: V5LibraryRow, onOpen: (String) -> Unit, onRetry: (String) -> Unit) {
    val colors = V5Theme.colors
    when (val trailing = row.trailing) {
        V5LibraryTrailing.None -> Unit
        is V5LibraryTrailing.Value -> Text(trailing.text, style = V5Theme.type.footnote, color = colors.labelSecondary)
        is V5LibraryTrailing.Count -> Row(verticalAlignment = Alignment.CenterVertically) {
            Text(trailing.count.toString(), style = V5Theme.type.body, color = colors.labelSecondary)
            Icon(
                VolaIcons.KeyboardArrowRight,
                contentDescription = null,
                tint = colors.labelTertiary,
                modifier = Modifier.size(V5Sizes.iconSmall),
            )
        }
        is V5LibraryTrailing.Progress -> {
            val paused = trailing.paused
            if (paused == null) {
                ProgressRing(trailing.fraction)
            } else {
                // Pausing is not only a tap on the row: the ring says it and is a button of its own.
                Box(
                    Modifier
                        .size(V5Sizes.touchTarget)
                        .clip(V5Theme.shapes.full)
                        .clickable(role = Role.Button) { onOpen(row.key) }
                        .testTag(V5LibraryTestTags.pause(row.key)),
                    contentAlignment = Alignment.Center,
                ) {
                    ProgressRing(trailing.fraction)
                    Icon(
                        if (paused) VolaIcons.PlayArrowFilled else VolaIcons.PauseFilled,
                        contentDescription = stringResource(if (paused) R.string.downloads_resume else R.string.downloads_pause),
                        tint = colors.accent,
                        modifier = Modifier.size(V5Sizes.downloadGlyph),
                    )
                }
            }
        }
        V5LibraryTrailing.Retry -> Box(
            Modifier
                .heightIn(min = V5Sizes.touchTarget)
                .clip(V5Theme.shapes.full)
                .clickable(role = Role.Button) { onRetry(row.key) }
                .padding(horizontal = V5Spacing.space2)
                .testTag(V5LibraryTestTags.retry(row.key)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                stringResource(R.string.v5_library_retry),
                style = V5Theme.type.footnote.copy(fontWeight = FontWeight.SemiBold),
                color = colors.accent,
            )
        }
    }
}

@Composable
private fun ProgressRing(fraction: Float?) {
    val colors = V5Theme.colors
    if (fraction == null) {
        CircularProgressIndicator(
            modifier = Modifier.size(V5Sizes.iconSmall),
            color = colors.accent,
            strokeWidth = V5Sizes.downloadRing,
            trackColor = colors.fill,
        )
    } else {
        CircularProgressIndicator(
            progress = { fraction },
            modifier = Modifier.size(V5Sizes.iconSmall),
            color = colors.accent,
            strokeWidth = V5Sizes.downloadRing,
            trackColor = colors.fill,
        )
    }
}

@Composable
private fun Actions(actions: V5LibraryActions) {
    if (actions.start == null && actions.end == null) return
    val colors = V5Theme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .background(colors.surfaceElevated)
            .navigationBarsPadding()
            .padding(horizontal = V5Spacing.space2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (actions.start != null) {
            ActionText(actions.start, colors.accent, actions.onStart)
        }
        Box(Modifier.weight(1f))
        if (actions.end != null) {
            ActionText(actions.end, if (actions.endDestructive) colors.danger else colors.accent, actions.onEnd)
        }
    }
}

@Composable
private fun ActionText(text: String, color: Color, onClick: () -> Unit) {
    Box(
        Modifier
            .heightIn(min = V5Sizes.touchTarget)
            .clip(V5Theme.shapes.full)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = V5Spacing.space3),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = V5Theme.type.body, color = color)
    }
}
