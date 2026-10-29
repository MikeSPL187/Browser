package dev.sk2andy.materialbrowser.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.setValue

internal val HistoryUndoSaver = Saver<HistoryUndo?, Any>(
    save = { undo -> undo?.let(LibraryRules::saveUndo) },
    restore = LibraryRules::restoreUndo,
)
