package dev.sk2andy.materialbrowser.ui

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.zIndex
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.browser.AddressResolver
import dev.sk2andy.materialbrowser.data.BrowsingFavoritesRules
import dev.sk2andy.materialbrowser.data.FavoriteEntry
import dev.sk2andy.materialbrowser.data.FavoriteFolder
import dev.sk2andy.materialbrowser.data.FavoriteFolderIcon
import dev.sk2andy.materialbrowser.data.FavoriteLibrary
import dev.sk2andy.materialbrowser.data.FavoriteLibraryEntry
import dev.sk2andy.materialbrowser.data.FavoriteMutation
import dev.sk2andy.materialbrowser.shared.ui.PlatformProfileEmoji
import dev.sk2andy.materialbrowser.shared.ui.icons.VolaIcons
import dev.sk2andy.materialbrowser.ui.theme.VolaLibrary
import dev.sk2andy.materialbrowser.ui.theme.VolaTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FavoritesScreen(
    favorites: List<FavoriteEntry>,
    favicons: Map<String, Bitmap> = emptyMap(),
    onDeleteFavorite: (FavoriteEntry, (FavoriteMutation?) -> Unit) -> Unit,
    onUndoDelete: (FavoriteMutation) -> Unit,
    onOpenFavorite: (FavoriteEntry) -> Unit,
    onBack: () -> Unit,
    library: FavoriteLibrary? = null,
    folderIcons: Map<String, Bitmap> = emptyMap(),
    onRenameEntry: (FavoriteLibraryEntry, String) -> Unit = { _, _ -> },
    onCreateFolder: (String?, String) -> Unit = { _, _ -> },
    onMoveEntry: (FavoriteLibraryEntry, String?) -> Unit = { _, _ -> },
    onReorderEntry: (FavoriteLibraryEntry, Int) -> Unit = { _, _ -> },
    onFolderIconChange: (FavoriteFolder, FavoriteFolderIcon?) -> Unit = { _, _ -> },
    onUploadFolderIcon: (FavoriteFolder) -> Unit = {},
    sort: FavoritesSort = FavoritesSort.Manual,
    onSortChange: (FavoritesSort) -> Unit = {},
    onImportBookmarks: (() -> Unit)? = null,
    /** False once the last deletion can no longer be undone, for instance after a later edit. */
    canUndo: Boolean = true,
) {
    val source = library ?: FavoriteLibrary(favorites)
    val locale = LocalConfiguration.current.locales[0]
    var query by rememberSaveable { mutableStateOf("") }
    var currentFolderId by rememberSaveable { mutableStateOf<String?>(null) }
    val currentFolder = currentFolderId?.let { BrowsingFavoritesRules.folder(source, it) }
    val parentId = currentFolder?.id
    val siblings = BrowsingFavoritesRules.children(source, parentId)
    val visibleEntries = remember(source, parentId, query) {
        if (query.isBlank()) siblings else source.entries.filter { entry ->
            when (entry) {
                is FavoriteEntry -> BrowsingFavoritesRules.visibleEntries(listOf(entry), query).isNotEmpty()
                is FavoriteFolder -> entry.title.contains(query.trim(), ignoreCase = true)
            }
        }
    }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var snackbarJob by remember { mutableStateOf<Job?>(null) }
    var renameTarget by remember { mutableStateOf<FavoriteLibraryEntry?>(null) }
    var creatingFolder by rememberSaveable { mutableStateOf(false) }
    var moveTarget by remember { mutableStateOf<FavoriteLibraryEntry?>(null) }
    var iconTarget by remember { mutableStateOf<FavoriteFolder?>(null) }
    val removedMessage = stringResource(R.string.favorite_removed_confirmation)
    val undoLabel = stringResource(R.string.action_undo)
    // An «Undo» whose snapshot a later change replaced would do nothing; take it off screen.
    LaunchedEffect(canUndo) {
        if (!canUndo) {
            snackbarJob?.cancel()
            snackbarHostState.currentSnackbarData?.dismiss()
        }
    }
    val navigateBack = {
        if (currentFolder != null) {
            currentFolderId = currentFolder.parentFolderId
            query = ""
        } else onBack()
    }
    BackHandler(onBack = navigateBack)

    if (creatingFolder || renameTarget != null) {
        FavoriteNameDialog(
            initialName = renameTarget?.entryTitle().orEmpty(),
            creating = creatingFolder,
            onDismiss = { creatingFolder = false; renameTarget = null },
            onConfirm = { name ->
                renameTarget?.let { onRenameEntry(it, name) } ?: onCreateFolder(parentId, name)
                creatingFolder = false
                renameTarget = null
            },
        )
    }
    moveTarget?.let { target ->
        FavoriteMoveDialog(
            library = source,
            target = target,
            onDismiss = { moveTarget = null },
            onMove = { destination -> onMoveEntry(target, destination); moveTarget = null },
        )
    }
    iconTarget?.let { folder ->
        FavoriteIconDialog(
            folder = folder,
            onDismiss = { iconTarget = null },
            onConfirm = { icon -> onFolderIconChange(folder, icon); iconTarget = null },
            onUpload = { onUploadFolderIcon(folder); iconTarget = null },
        )
    }
    val listState = rememberLazyListState()
    val haptics = LocalHapticFeedback.current
    var draggedId by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    var destinationId by remember { mutableStateOf<String?>(null) }
    val currentReorder by rememberUpdatedState(onReorderEntry)
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(currentFolder?.title ?: stringResource(R.string.favorites_title)) },
                navigationIcon = {
                    IconButton(onClick = navigateBack) {
                        Icon(VolaIcons.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                actions = { FavoritesSortButton(sort = sort, onSortChange = onSortChange) },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            // Board W-Favorites: «Folder» at the bottom, in thumb's reach.
            val createFolderLabel = stringResource(R.string.favorites_create_folder)
            ExtendedFloatingActionButton(
                onClick = { creatingFolder = true },
                icon = { Icon(VolaIcons.Folder, contentDescription = null) },
                text = { Text(stringResource(R.string.favorites_folder_button)) },
                // The button's own label does not reach TalkBack's tree; name the button itself.
                modifier = Modifier
                    .semantics { contentDescription = createFolderLabel }
                    .testTag("favorites_create_folder"),
            )
        },
    ) { contentPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(contentPadding).navigationBarsPadding()
                .testTag(FavoritesScreenTestTags.List),
            contentPadding = PaddingValues(bottom = VolaLibrary.fabClearance),
        ) {
            item(key = "search") {
                LibrarySearchBar(
                    query = query,
                    placeholder = stringResource(R.string.favorites_search),
                    clearContentDescription = stringResource(R.string.favorites_clear_search),
                    testTag = FavoritesScreenTestTags.SearchField,
                    onQueryChange = { query = it.take(BrowsingFavoritesRules.MAX_QUERY_CHARS) },
                )
            }
            if (currentFolder != null && query.isBlank()) {
                item(key = "parent") {
                    LibraryCardSlice(
                        position = LibraryRowPosition.Single,
                        modifier = Modifier.padding(top = VolaLibrary.sectionGap),
                    ) {
                        LibraryRow(
                            title = stringResource(R.string.favorites_parent),
                            detail = BrowsingFavoritesRules.folder(source, currentFolder.parentFolderId.orEmpty())?.title
                                ?: stringResource(R.string.favorites_title),
                            onClick = navigateBack,
                            modifier = Modifier.testTag("favorites_parent"),
                            leading = {
                                FavoriteSymbolTile(colorKey = currentFolder.id) {
                                    Icon(
                                        painterResource(R.drawable.ic_folder_arrow_up),
                                        contentDescription = null,
                                        modifier = Modifier.size(VolaLibrary.tileIconSize),
                                    )
                                }
                            },
                        )
                    }
                }
            }
            if (visibleEntries.isEmpty()) {
                item(key = "empty") {
                    FavoritesEmptyState(
                        searching = query.isNotBlank(),
                        insideFolder = currentFolder != null,
                        onImportBookmarks = onImportBookmarks,
                        modifier = Modifier
                            .padding(horizontal = VolaLibrary.sidePadding)
                            .padding(top = VolaLibrary.sectionGap),
                    )
                }
            }
            val level = LibraryRules.sorted(LibraryRules.level(visibleEntries), sort, locale)
            // Moving by hand only makes sense in the user's own order.
            val canReorder = query.isBlank() && sort == FavoritesSort.Manual
            // Folders first, two cards in a row (board W-Favorites).
            level.folders.chunked(VolaLibrary.FOLDER_COLUMNS).forEachIndexed { rowIndex, rowFolders ->
                item(key = "folders:${rowFolders.first().id}") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = VolaLibrary.sidePadding)
                            .padding(top = if (rowIndex == 0) VolaLibrary.sectionGap else VolaLibrary.folderCardGap),
                        horizontalArrangement = Arrangement.spacedBy(VolaLibrary.folderCardGap),
                    ) {
                        rowFolders.forEach { folder ->
                            val earlier = LibraryRules.reorderTarget(siblings, level.folders, folder.id, -1)
                            val later = LibraryRules.reorderTarget(siblings, level.folders, folder.id, 1)
                            FavoriteFolderCard(
                                folder = folder,
                                siteCount = LibraryRules.siteCount(source, folder.id),
                                customIcon = folderIcons[folder.id],
                                modifier = Modifier.weight(1f),
                                actions = FavoriteActions(
                                    canMoveEarlier = canReorder && earlier != null,
                                    canMoveLater = canReorder && later != null,
                                    onMoveEarlier = { earlier?.let { onReorderEntry(folder, it) } },
                                    onMoveLater = { later?.let { onReorderEntry(folder, it) } },
                                    onRename = { renameTarget = folder },
                                    onMove = { moveTarget = folder },
                                    onIcon = { iconTarget = folder },
                                ),
                                onOpen = { currentFolderId = folder.id; query = "" },
                            )
                        }
                        repeat(VolaLibrary.FOLDER_COLUMNS - rowFolders.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
            if (level.favorites.isNotEmpty()) {
                item(key = "sites") {
                    LibrarySectionLabel(
                        text = stringResource(
                            if (currentFolder == null) R.string.favorites_section_no_folder
                            else R.string.favorites_section_sites,
                        ),
                        modifier = Modifier.padding(top = VolaLibrary.sectionGap),
                    )
                }
            }
            itemsIndexed(level.favorites, key = { _, entry -> entry.id }) { position, entry ->
                val index = siblings.indexOfFirst { it.id == entry.id }
                val earlier = LibraryRules.reorderTarget(siblings, level.favorites, entry.id, -1)
                val later = LibraryRules.reorderTarget(siblings, level.favorites, entry.id, 1)
                val dragging = draggedId == entry.id
                val lift by animateFloatAsState(if (dragging) 1.025f else 1f, spring(), label = "favorite lift")
                val rowModifier = Modifier.animateItem().zIndex(if (dragging) 1f else 0f)
                    .graphicsLayer { translationY = if (dragging) dragOffset else 0f; scaleX = lift; scaleY = lift }
                    .pointerInput(entry.id, canReorder, siblings.map { it.id }) {
                        if (!canReorder) return@pointerInput
                        var startCenter = 0f
                        detectDragGesturesAfterLongPress(
                            onDragStart = {
                                draggedId = entry.id
                                destinationId = entry.id
                                dragOffset = 0f
                                val row = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == entry.id }
                                startCenter = (row?.offset ?: 0) + (row?.size ?: 0) / 2f
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            },
                            onDragCancel = { draggedId = null; dragOffset = 0f },
                            onDragEnd = {
                                val destination = siblings.indexOfFirst { it.id == destinationId }
                                if (destination >= 0 && destination != index) currentReorder(entry, destination)
                                draggedId = null
                                dragOffset = 0f
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            },
                            onDrag = { change, amount ->
                                change.consume()
                                dragOffset += amount.y
                                val center = startCenter + dragOffset
                                val target = listState.layoutInfo.visibleItemsInfo.firstOrNull {
                                    center >= it.offset && center <= it.offset + it.size && siblings.any { entry -> entry.id == it.key }
                                }?.key as? String
                                if (target != null && destinationId != target) {
                                    destinationId = target
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                            },
                        )
                    }
                LibraryCardSlice(
                    position = LibraryRules.position(position, level.favorites.size),
                    modifier = rowModifier,
                ) {
                    FavoriteRow(
                        entry = entry,
                        favicon = favicons[entry.url],
                        actions = FavoriteActions(
                            canMoveEarlier = canReorder && earlier != null,
                            canMoveLater = canReorder && later != null,
                            onMoveEarlier = { earlier?.let { onReorderEntry(entry, it) } },
                            onMoveLater = { later?.let { onReorderEntry(entry, it) } },
                            onRename = { renameTarget = entry },
                            onMove = { moveTarget = entry },
                            onDelete = {
                                onDeleteFavorite(entry) { mutation ->
                                    if (mutation != null) {
                                        snackbarJob?.cancel()
                                        snackbarHostState.currentSnackbarData?.dismiss()
                                        snackbarJob = coroutineScope.launch {
                                            if (snackbarHostState.showSnackbar(removedMessage, undoLabel, duration = SnackbarDuration.Long) == SnackbarResult.ActionPerformed) {
                                                onUndoDelete(mutation)
                                            }
                                        }
                                    }
                                }
                            },
                        ),
                        onOpen = { onOpenFavorite(entry) },
                    )
                }
            }
        }
    }
}

private fun FavoriteLibraryEntry.entryTitle(): String = when (this) {
    is FavoriteEntry -> title
    is FavoriteFolder -> title
}

@Composable
private fun FavoriteNameDialog(initialName: String, creating: Boolean, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var name by rememberSaveable(initialName) { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (creating) R.string.favorites_create_folder else R.string.favorites_rename)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(BrowsingFavoritesRules.MAX_FOLDER_TITLE_CHARS) },
                label = { Text(stringResource(R.string.favorites_name)) },
                singleLine = true,
                modifier = Modifier.testTag("favorites_name"),
            )
        },
        confirmButton = { TextButton(onClick = { onConfirm(name.trim()) }, enabled = name.isNotBlank()) { Text(stringResource(android.R.string.ok)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) } },
    )
}

@Composable
private fun FavoriteMoveDialog(library: FavoriteLibrary, target: FavoriteLibraryEntry, onDismiss: () -> Unit, onMove: (String?) -> Unit) {
    var destination by rememberSaveable(target.id) { mutableStateOf(target.parentFolderId) }
    val folder = destination?.let { BrowsingFavoritesRules.folder(library, it) }
    val eligible = BrowsingFavoritesRules.children(library, destination).filterIsInstance<FavoriteFolder>().filter {
        it.id != target.id && BrowsingFavoritesRules.move(library, target.id, it.id) != library
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.favorites_move)) },
        text = {
            Column {
                Text(folder?.title ?: stringResource(R.string.favorites_title), style = MaterialTheme.typography.titleMedium)
                LazyColumn {
                    if (folder != null) {
                        item {
                            ListItem(
                                onClick = { onMove(folder.parentFolderId) },
                                modifier = Modifier.testTag("favorites_move_parent"),
                                leadingContent = { Icon(painterResource(R.drawable.ic_folder_arrow_up), null) },
                            ) {
                                Text(stringResource(R.string.favorites_parent))
                            }
                        }
                    }
                    itemsIndexed(eligible, key = { _, item -> item.id }) { _, item ->
                        ListItem(
                            onClick = { destination = item.id },
                            modifier = Modifier.testTag("favorites_destination:${item.id}"),
                            leadingContent = { Icon(painterResource(R.drawable.ic_folder), null) },
                        ) {
                            Text(item.title)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onMove(destination) }, enabled = destination != target.parentFolderId) {
                Text(stringResource(R.string.favorites_move_here))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) } },
    )
}

@Composable
private fun FavoriteIconDialog(folder: FavoriteFolder, onDismiss: () -> Unit, onConfirm: (FavoriteFolderIcon?) -> Unit, onUpload: () -> Unit) {
    var emoji by rememberSaveable(folder.id) { mutableStateOf((folder.icon as? FavoriteFolderIcon.Emoji)?.value.orEmpty()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.favorites_folder_icon)) },
        text = {
            Column {
                Text(
                    stringResource(R.string.favorites_folder_symbol),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                IconKeyChoices(
                    selected = emoji,
                    onSelect = { emoji = it },
                    testTagForKey = FavoritesScreenTestTags::folderIcon,
                )
                TextButton(onClick = onUpload) { Text(stringResource(R.string.favorites_upload_icon)) }
                TextButton(onClick = { onConfirm(null) }) { Text(stringResource(R.string.favorites_reset_icon)) }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(FavoriteFolderIcon.Emoji(emoji.trim())) }, enabled = emoji.isNotBlank()) { Text(stringResource(android.R.string.ok)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) } },
    )
}

/** What a favorite's or folder's «⋮» menu offers. */
private class FavoriteActions(
    val canMoveEarlier: Boolean,
    val canMoveLater: Boolean,
    val onMoveEarlier: () -> Unit,
    val onMoveLater: () -> Unit,
    val onRename: () -> Unit,
    val onMove: () -> Unit,
    val onIcon: (() -> Unit)? = null,
    val onDelete: (() -> Unit)? = null,
)

@Composable
private fun FavoriteActionsButton(
    entryId: String,
    label: String,
    actions: FavoriteActions,
    deleteTag: String? = null,
) {
    var menuOpen by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { menuOpen = true }, modifier = Modifier.testTag("favorites_actions:$entryId")) {
            Icon(VolaIcons.MoreVert, stringResource(R.string.favorites_actions, label))
        }
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            DropdownMenuItem(text = { Text(stringResource(R.string.favorites_rename)) }, onClick = { menuOpen = false; actions.onRename() })
            DropdownMenuItem(text = { Text(stringResource(R.string.favorites_move)) }, onClick = { menuOpen = false; actions.onMove() })
            DropdownMenuItem(text = { Text(stringResource(R.string.favorites_move_up)) }, enabled = actions.canMoveEarlier, onClick = { menuOpen = false; actions.onMoveEarlier() })
            DropdownMenuItem(text = { Text(stringResource(R.string.favorites_move_down)) }, enabled = actions.canMoveLater, onClick = { menuOpen = false; actions.onMoveLater() })
            actions.onIcon?.let { onIcon ->
                DropdownMenuItem(text = { Text(stringResource(R.string.favorites_folder_icon)) }, onClick = { menuOpen = false; onIcon() })
            }
            actions.onDelete?.let { onDelete ->
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.favorites_delete_action)) },
                    leadingIcon = { Icon(VolaIcons.Delete, contentDescription = null) },
                    onClick = { menuOpen = false; onDelete() },
                    modifier = deleteTag?.let { Modifier.testTag(it) } ?: Modifier,
                )
            }
        }
    }
}

@Composable
private fun FavoriteRow(
    entry: FavoriteEntry,
    favicon: Bitmap?,
    actions: FavoriteActions,
    onOpen: () -> Unit,
) {
    val title = entry.entryTitle()
    val host = AddressResolver.displayText(entry.url)
    LibraryRow(
        title = title,
        detail = host,
        onClick = onOpen,
        modifier = Modifier.testTag(FavoritesScreenTestTags.favorite(entry.url)),
        leading = { LibrarySiteTile(label = title, colorKey = host, favicon = favicon) },
        trailing = {
            FavoriteActionsButton(
                entryId = entry.id,
                label = title,
                actions = actions,
                deleteTag = FavoritesScreenTestTags.delete(entry.url),
            )
        },
    )
}

/** A folder of board W-Favorites: its symbol on a soft tile, its name and how many sites it holds. */
@Composable
private fun FavoriteFolderCard(
    folder: FavoriteFolder,
    siteCount: Int,
    customIcon: Bitmap?,
    actions: FavoriteActions,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .clip(VolaLibrary.folderCardShape)
            .clickable(role = Role.Button, onClick = onOpen)
            .testTag("favorites_folder:${folder.id}"),
        shape = VolaLibrary.folderCardShape,
        color = VolaTheme.extendedColors.card,
    ) {
        Column(
            modifier = Modifier.padding(VolaLibrary.folderCardPadding),
            verticalArrangement = Arrangement.spacedBy(VolaLibrary.folderCardGap),
        ) {
            Row(verticalAlignment = Alignment.Top) {
                FavoriteSymbolTile(colorKey = folder.id, shape = VolaLibrary.folderTileShape) {
                    when {
                        folder.icon is FavoriteFolderIcon.Emoji ->
                            PlatformProfileEmoji(emoji = folder.icon.value, fontSize = VolaLibrary.folderEmojiSize)
                        folder.icon == FavoriteFolderIcon.Custom && customIcon != null && !customIcon.isRecycled ->
                            Image(customIcon.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        else -> Icon(VolaIcons.Folder, contentDescription = null, modifier = Modifier.size(VolaLibrary.tileIconSize))
                    }
                }
                Spacer(Modifier.weight(1f))
                FavoriteActionsButton(entryId = folder.id, label = folder.title, actions = actions)
            }
            Column(verticalArrangement = Arrangement.spacedBy(VolaLibrary.textGap)) {
                Text(
                    folder.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    pluralStringResource(R.plurals.favorites_folder_sites, siteCount, siteCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun FavoriteSymbolTile(
    colorKey: String,
    shape: Shape = VolaLibrary.tileShape,
    content: @Composable () -> Unit,
) {
    val tile = VolaLibrary.tile(LibraryRules.tileIndex(colorKey, VolaLibrary.tileCount))
    Box(
        modifier = Modifier
            .size(VolaLibrary.tileSize)
            .clip(shape)
            .background(tile.container),
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(LocalContentColor provides tile.content) { content() }
    }
}

internal fun favoriteInitial(favorite: FavoriteEntry): String =
    LibraryRules.initial(favorite.title.ifBlank { AddressResolver.displayText(favorite.url) })

/** Board W-Favorites: the order of sites and folders — the user's own, by name, or newest first. */
@Composable
private fun FavoritesSortButton(sort: FavoritesSort, onSortChange: (FavoritesSort) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }, modifier = Modifier.testTag(FavoritesScreenTestTags.Sort)) {
            Icon(VolaIcons.Sort, contentDescription = stringResource(R.string.favorites_sort))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            FavoritesSort.entries.forEach { option ->
                val chosen = option == sort
                DropdownMenuItem(
                    text = { Text(stringResource(favoritesSortLabel(option))) },
                    trailingIcon = if (chosen) {
                        { Icon(VolaIcons.Check, contentDescription = null) }
                    } else {
                        null
                    },
                    onClick = {
                        open = false
                        onSortChange(option)
                    },
                    modifier = Modifier
                        .semantics { selected = chosen }
                        .testTag(FavoritesScreenTestTags.sort(option)),
                )
            }
        }
    }
}

private fun favoritesSortLabel(sort: FavoritesSort): Int = when (sort) {
    FavoritesSort.Manual -> R.string.favorites_sort_manual
    FavoritesSort.Name -> R.string.favorites_sort_name
    FavoritesSort.Recent -> R.string.favorites_sort_recent
}

/** Board W-States: no favorites yet, an empty folder, or a search that found nothing. */
@Composable
private fun FavoritesEmptyState(
    searching: Boolean,
    insideFolder: Boolean,
    onImportBookmarks: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    // Only an empty library offers bookmarks from elsewhere; a folder or a search has its own way out.
    val action = onImportBookmarks?.takeUnless { searching || insideFolder }
    VolaStateMessage(
        icon = rememberVectorPainter(VolaIcons.Bookmark),
        title = stringResource(
            when {
                searching -> R.string.favorites_no_matches
                insideFolder -> R.string.favorites_folder_empty
                else -> R.string.favorites_empty
            },
        ),
        message = stringResource(
            when {
                searching -> R.string.favorites_no_matches_message
                insideFolder -> R.string.favorites_folder_empty_message
                else -> R.string.favorites_empty_message
            },
        ),
        modifier = modifier,
        tone = if (searching) VolaStateTone.Neutral else VolaStateTone.Empty,
        actionLabel = action?.let { stringResource(R.string.favorites_empty_action) },
        onAction = { action?.invoke() },
    )
}

internal object FavoritesScreenTestTags {
    const val List = "favorites_list"
    const val SearchField = "favorites_search_field"
    const val Sort = "favorites_sort"

    fun sort(option: FavoritesSort): String = "favorites_sort:$option"

    fun favorite(url: String): String = "favorite:$url"

    fun delete(url: String): String = "favorite_delete:$url"

    fun folderIcon(key: String): String = "favorite_folder_icon:$key"
}

/** Up to four favorites of a folder and its subfolders, for the folder's preview tile. */
internal fun favoriteFolderPreviewFavorites(
    library: FavoriteLibrary,
    folderId: String,
): List<FavoriteEntry> {
    if (BrowsingFavoritesRules.folder(library, folderId) == null) return emptyList()
    val visited = hashSetOf<String>()
    fun collect(parentId: String): List<FavoriteEntry> {
        if (!visited.add(parentId)) return emptyList()
        return BrowsingFavoritesRules.children(library, parentId).flatMap { entry ->
            when (entry) {
                is FavoriteEntry -> listOf(entry)
                is FavoriteFolder -> collect(entry.id)
            }
        }
    }
    return collect(folderId).take(FOLDER_PREVIEW_FAVORITE_COUNT)
}

private const val FOLDER_PREVIEW_FAVORITE_COUNT = 4
