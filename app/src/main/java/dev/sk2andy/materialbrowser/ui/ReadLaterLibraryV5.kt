package dev.sk2andy.materialbrowser.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalAccessibilityManager
import androidx.compose.ui.res.stringResource
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.reader.ReaderSnapshot
import dev.sk2andy.materialbrowser.ui.theme.v5.VolaV5Theme
import dev.sk2andy.materialbrowser.ui.v5.V5Library
import dev.sk2andy.materialbrowser.ui.v5.V5LibraryLeading
import dev.sk2andy.materialbrowser.ui.v5.V5LibraryRow
import dev.sk2andy.materialbrowser.ui.v5.V5LibrarySection
import dev.sk2andy.materialbrowser.ui.v5.V5LibrarySegment
import dev.sk2andy.materialbrowser.ui.v5.V5LibraryState
import dev.sk2andy.materialbrowser.ui.v5.V5LibraryTrailing
import kotlinx.coroutines.withTimeoutOrNull

/**
 * The «Read later» segment of the v5 library (canvas «History»): the articles saved offline in
 * Reader view, newest first. A tap opens the article's page; a row swiped left is hidden at once
 * behind «Undo» and deleted when the window closes or the screen goes, as history's rows are.
 */
@Composable
internal fun ReadLaterLibraryV5(
    /** Null while the saved articles are read. */
    snapshots: List<ReaderSnapshot>?,
    onOpen: (ReaderSnapshot) -> Unit,
    onDelete: (ReaderSnapshot) -> Unit,
    onSwitch: (V5LibrarySegment) -> Unit,
    onClose: () -> Unit,
    /** True while the host is being recreated: a deletion behind «Undo» then waits in saved state. */
    isChangingConfigurations: () -> Boolean = { false },
) {
    var query by rememberSaveable { mutableStateOf("") }
    var pendingId by rememberSaveable { mutableStateOf<String?>(null) }
    // TalkBack users and longer «Time to take action» settings get the window Android recommends.
    val undoWindowMillis = LocalAccessibilityManager.current?.calculateRecommendedTimeoutMillis(
        LibraryRules.UNDO_WINDOW_MILLIS,
        containsIcons = false,
        containsText = true,
        containsControls = true,
    ) ?: LibraryRules.UNDO_WINDOW_MILLIS
    val all = snapshots.orEmpty()
    val currentAll by rememberUpdatedState(all)
    val currentDelete by rememberUpdatedState(onDelete)
    val currentChangingConfigurations by rememberUpdatedState(isChangingConfigurations)
    fun commit(id: String) {
        currentAll.firstOrNull { it.id == id }?.let(currentDelete)
    }
    val snackbarHostState = remember { SnackbarHostState() }
    fun commitPending() {
        val id = pendingId ?: return
        pendingId = null
        snackbarHostState.currentSnackbarData?.dismiss()
        commit(id)
    }
    DisposableEffect(Unit) {
        onDispose {
            val id = pendingId
            if (id != null && !currentChangingConfigurations()) commit(id)
        }
    }
    val shown = remember(all, query, pendingId) {
        ReadLaterLibraryV5Rules.shown(all, query).filterNot { it.id == pendingId }
    }
    val byId = remember(shown) { shown.associateBy(ReaderSnapshot::id) }
    val progressFormat = stringResource(R.string.v5_read_later_progress)
    val rows = remember(shown, progressFormat) {
        shown.map { snapshot ->
            val host = ReadLaterLibraryV5Rules.host(snapshot.document.sourceUrl)
            V5LibraryRow(
                key = snapshot.id,
                title = snapshot.document.title.ifBlank { host },
                subtitle = snapshot.document.siteName.ifBlank { host },
                leading = V5LibraryLeading.Site(
                    HistoryLibraryV5Rules.siteLetter(host),
                    HistoryLibraryV5Rules.siteTile(host),
                ),
                trailing = ReadLaterLibraryV5Rules.readPercent(snapshot.progress)
                    ?.let { percent -> V5LibraryTrailing.Value(progressFormat.format(percent)) }
                    ?: V5LibraryTrailing.None,
            )
        }
    }
    val state = V5LibraryState(
        segment = V5LibrarySegment.ReadLater,
        query = query,
        sections = listOf(V5LibrarySection(null, rows)),
        emptyMessage = stringResource(
            if (query.isBlank()) R.string.v5_read_later_empty_message else R.string.v5_read_later_no_results,
        ),
        emptyTitle = stringResource(R.string.v5_read_later_empty_title).takeIf { query.isBlank() },
        loading = snapshots == null,
    )
    val deletedMessage = stringResource(R.string.v5_read_later_deleted)
    val undoLabel = stringResource(R.string.action_undo)
    LaunchedEffect(pendingId) {
        val id = pendingId ?: return@LaunchedEffect
        val result = withTimeoutOrNull(undoWindowMillis) {
            snackbarHostState.showSnackbar(deletedMessage, undoLabel, duration = SnackbarDuration.Indefinite)
        }
        snackbarHostState.currentSnackbarData?.dismiss()
        if (result != SnackbarResult.ActionPerformed) commit(id)
        pendingId = null
    }
    val dark = MaterialTheme.colorScheme.background.luminance() < READ_LATER_DARK_LUMINANCE
    VolaV5Theme(dark = dark) {
        V5Library(
            state = state,
            onSegmentChange = { segment ->
                if (segment != V5LibrarySegment.ReadLater) {
                    commitPending()
                    onSwitch(segment)
                }
            },
            onQueryChange = { query = it },
            onOpen = { key ->
                byId[key]?.let { snapshot ->
                    commitPending()
                    onOpen(snapshot)
                }
            },
            onDelete = { key ->
                if (key in byId) {
                    // A second deletion confirms the first: one «Undo» at a time, as in history.
                    pendingId?.let(::commit)
                    pendingId = key
                }
            },
            onRetry = {},
            onClose = {
                commitPending()
                onClose()
            },
            snackbarHostState = snackbarHostState,
        )
    }
}

private const val READ_LATER_DARK_LUMINANCE = 0.5f
