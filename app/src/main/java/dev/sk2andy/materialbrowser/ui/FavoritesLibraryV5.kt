package dev.sk2andy.materialbrowser.ui

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalAccessibilityManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.data.BrowsingFavoritesRules
import dev.sk2andy.materialbrowser.data.FavoriteEntry
import dev.sk2andy.materialbrowser.data.FavoriteLibrary
import dev.sk2andy.materialbrowser.data.FavoriteMutation
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Spacing
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Theme
import dev.sk2andy.materialbrowser.ui.theme.v5.VolaV5Theme
import dev.sk2andy.materialbrowser.ui.v5.V5Library
import dev.sk2andy.materialbrowser.ui.v5.V5LibraryActions
import dev.sk2andy.materialbrowser.ui.v5.V5LibraryLeading
import dev.sk2andy.materialbrowser.ui.v5.V5LibraryRow
import dev.sk2andy.materialbrowser.ui.v5.V5LibrarySection
import dev.sk2andy.materialbrowser.ui.v5.V5LibrarySegment
import dev.sk2andy.materialbrowser.ui.v5.V5LibraryState
import dev.sk2andy.materialbrowser.ui.v5.V5LibraryTrailing
import dev.sk2andy.materialbrowser.ui.v5.V5ListGroup
import dev.sk2andy.materialbrowser.ui.v5.V5ListRow
import dev.sk2andy.materialbrowser.ui.v5.V5NameField
import dev.sk2andy.materialbrowser.ui.v5.V5SheetHeader
import dev.sk2andy.materialbrowser.ui.v5.V5WindowSheet
import java.net.URI
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

object FavoritesLibraryV5TestTags {
    const val NewFolderName = "v5_favorites_new_folder_name"
    const val NewFolderSave = "v5_favorites_new_folder_save"
}

/**
 * The favorites segment of the v5 library (V28, B2c; canvas screen «р2»): the current profile's
 * folders and sites in their own order, a folder opened in place with a way back up (system back
 * too), a search through every favorite, a site swiped left deleted with «Undo» (БИ-2, as the
 * tablet's favorites), and «New folder».
 * A folder is never deleted by a swipe: it would take its sites with it.
 */
@Composable
internal fun FavoritesLibraryV5(
    library: FavoriteLibrary,
    favicons: Map<String, Bitmap>,
    onOpen: (FavoriteEntry) -> Unit,
    onDelete: (FavoriteEntry, onComplete: (FavoriteMutation?) -> Unit) -> Unit,
    onUndoDelete: (FavoriteMutation) -> Unit,
    onCreateFolder: (parentId: String?, title: String) -> Unit,
    onImport: () -> Unit,
    onSwitch: (V5LibrarySegment) -> Unit,
    onClose: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var openFolderId by rememberSaveable { mutableStateOf<String?>(null) }
    var creatingFolder by rememberSaveable { mutableStateOf(false) }
    // A folder deleted elsewhere (another window, an import) leaves its place: back to the top.
    val folderId = openFolderId?.takeIf { id -> library.folders.any { it.id == id } }
    val view = remember(library, folderId, query) { FavoritesLibraryV5Rules.view(library, folderId, query) }
    val byKey = remember(view) { view.favorites.associateBy(FavoriteEntry::id) }
    val searching = query.isNotBlank()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var undoJob by remember { mutableStateOf<Job?>(null) }
    val removedMessage = stringResource(R.string.favorite_removed_confirmation)
    val undoLabel = stringResource(R.string.action_undo)
    // TalkBack users and longer «Time to take action» settings get the window Android recommends.
    val undoWindowMillis = LocalAccessibilityManager.current?.calculateRecommendedTimeoutMillis(
        LibraryRules.UNDO_WINDOW_MILLIS,
        containsIcons = false,
        containsText = true,
        containsControls = true,
    ) ?: LibraryRules.UNDO_WINDOW_MILLIS
    val delete = { favorite: FavoriteEntry ->
        onDelete(favorite) { mutation ->
            if (mutation != null) {
                undoJob?.cancel()
                snackbarHostState.currentSnackbarData?.dismiss()
                undoJob = scope.launch {
                    val result = withTimeoutOrNull(undoWindowMillis) {
                        snackbarHostState.showSnackbar(removedMessage, undoLabel, duration = SnackbarDuration.Indefinite)
                    }
                    snackbarHostState.currentSnackbarData?.dismiss()
                    if (result == SnackbarResult.ActionPerformed) onUndoDelete(mutation)
                }
            }
        }
    }
    val rows = view.folders.map { (folder, count) ->
        V5LibraryRow(
            key = folder.id,
            title = folder.title,
            subtitle = "",
            leading = V5LibraryLeading.Folder,
            trailing = V5LibraryTrailing.Count(count),
            deletable = false,
        )
    } + view.favorites.map { favorite ->
        val host = hostOf(favorite.url)
        V5LibraryRow(
            key = favorite.id,
            title = favorite.title.ifBlank { host },
            subtitle = host,
            leading = V5LibraryLeading.Site(
                HistoryLibraryV5Rules.siteLetter(host),
                HistoryLibraryV5Rules.siteTile(host),
                favicons[favorite.url]?.asImageBitmap(),
            ),
        )
    }
    val state = V5LibraryState(
        segment = V5LibrarySegment.Favorites,
        query = query,
        sections = listOf(V5LibrarySection(title = null, rows = rows)),
        emptyMessage = stringResource(
            when {
                searching -> R.string.favorites_no_matches
                folderId != null -> R.string.favorites_folder_empty
                else -> R.string.favorites_empty
            },
        ),
        actions = if (searching) {
            V5LibraryActions()
        } else {
            V5LibraryActions(
                start = stringResource(R.string.v5_favorites_new_folder),
                onStart = { creatingFolder = true },
                // Bringing bookmarks over belongs to the whole library, not to one folder.
                end = stringResource(R.string.favorites_empty_action).takeIf { folderId == null },
                onEnd = onImport,
            )
        },
        folder = library.folders.firstOrNull { it.id == folderId }?.title.takeIf { !searching },
    )
    val dark = MaterialTheme.colorScheme.background.luminance() < FAVORITES_DARK_LUMINANCE
    VolaV5Theme(dark = dark) {
        V5Library(
            state = state,
            onSegmentChange = { segment -> if (segment != V5LibrarySegment.Favorites) onSwitch(segment) },
            onQueryChange = { query = it },
            onOpen = { key ->
                val favorite = byKey[key]
                if (favorite != null) onOpen(favorite) else openFolderId = key
            },
            onDelete = { key -> byKey[key]?.let(delete) },
            onRetry = {},
            onClose = onClose,
            onUp = { openFolderId = FavoritesLibraryV5Rules.parentOf(library, folderId) },
            snackbarHostState = snackbarHostState,
        )
        NewFolderSheet(
            visible = creatingFolder,
            onDismiss = { creatingFolder = false },
            onCreate = { title -> onCreateFolder(folderId, title) },
        )
    }
}

/** «New folder»: a name, created in the open folder; an empty name creates nothing. */
@Composable
private fun NewFolderSheet(visible: Boolean, onDismiss: () -> Unit, onCreate: (String) -> Unit) {
    val title = stringResource(R.string.v5_favorites_new_folder)
    var name by rememberSaveable(visible) { mutableStateOf("") }
    val save = {
        onDismiss()
        name.trim().takeIf(String::isNotEmpty)?.let(onCreate)
        Unit
    }
    V5WindowSheet(visible = visible, onDismiss = onDismiss, title = title) {
        V5SheetHeader(title = title, onClose = onDismiss)
        Column(
            Modifier.padding(top = V5Spacing.space3, bottom = V5Spacing.space4),
            verticalArrangement = Arrangement.spacedBy(V5Spacing.space4),
        ) {
            V5NameField(
                value = name,
                placeholder = stringResource(R.string.favorites_name),
                onValueChange = { name = it },
                onDone = save,
                maxLength = BrowsingFavoritesRules.MAX_FOLDER_TITLE_CHARS,
                modifier = Modifier.testTag(FavoritesLibraryV5TestTags.NewFolderName),
            )
            V5ListGroup {
                V5ListRow(
                    title = stringResource(R.string.favorites_create_folder),
                    titleColor = V5Theme.colors.accent,
                    onClick = save,
                    modifier = Modifier.testTag(FavoritesLibraryV5TestTags.NewFolderSave),
                )
            }
        }
    }
}

private fun hostOf(url: String): String =
    runCatching { URI(url).host?.removePrefix("www.") }.getOrNull()?.takeIf(String::isNotBlank) ?: url

private const val FAVORITES_DARK_LUMINANCE = 0.5f
