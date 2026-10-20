package dev.sk2andy.materialbrowser.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.sk2andy.materialbrowser.browser.AddressEditorRules
import dev.sk2andy.materialbrowser.browser.AddressEditorState
import dev.sk2andy.materialbrowser.browser.commands.AddressSuggestionItem
import dev.sk2andy.materialbrowser.browser.commands.CommandMatcher

/**
 * Where the arrow keys' highlight stands as the editor's suggestions change. A new query starts it
 * over (on the first command for a command query); new suggestions for the same query, such as
 * search suggestions from the network, keep the chosen row (А-1), so Enter opens what was chosen.
 */
@Composable
internal fun AddressHighlightEffect(query: String, items: List<AddressSuggestionItem>, editor: AddressEditorState) {
    val ids = items.map(AddressSuggestionItem::stableId)
    var placedQuery by remember { mutableStateOf<String?>(null) }
    var placedIds by remember { mutableStateOf(emptyList<String>()) }
    LaunchedEffect(query, ids) {
        val carried = if (query == placedQuery) {
            AddressEditorRules.carriedHighlight(placedIds, editor.highlightedIndex, ids)
        } else {
            null
        }
        editor.highlightedIndex = carried ?: if (CommandMatcher.isExplicitCommandQuery(query) && items.isNotEmpty()) {
            0
        } else {
            AddressEditorState.NO_HIGHLIGHT
        }
        placedQuery = query
        placedIds = ids
    }
}
