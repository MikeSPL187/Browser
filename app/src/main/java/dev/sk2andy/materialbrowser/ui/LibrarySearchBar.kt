package dev.sk2andy.materialbrowser.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import dev.sk2andy.materialbrowser.shared.ui.icons.VolaIcons
import dev.sk2andy.materialbrowser.ui.theme.VolaLibrary

/**
 * The library's search pill (boards W-History, W-Favorites): it filters the list under it as you
 * type. A plain text field on [androidx.compose.foundation.text.input.TextFieldState]: Material's
 * SearchBar takes no typing while collapsed, it only expands into a search screen of its own.
 */
@Composable
internal fun LibrarySearchBar(
    query: String,
    placeholder: String,
    clearContentDescription: String,
    testTag: String,
    onQueryChange: (String) -> Unit,
) {
    val state = rememberTextFieldState(query)
    val currentOnQueryChange by rememberUpdatedState(onQueryChange)
    val currentQuery by rememberUpdatedState(query)
    // The screen may reset or trim the query (a folder change, a length limit): follow it.
    LaunchedEffect(query) {
        if (state.text.toString() != query) state.setTextAndPlaceCursorAtEnd(query)
    }
    LaunchedEffect(state) {
        snapshotFlow { state.text.toString() }.collect { text ->
            if (text != currentQuery) currentOnQueryChange(text)
        }
    }
    val colors = MaterialTheme.colorScheme
    TextField(
        state = state,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = VolaLibrary.sidePadding)
            .heightIn(min = VolaLibrary.searchHeight)
            .testTag(testTag),
        placeholder = { Text(placeholder) },
        leadingIcon = { Icon(VolaIcons.Search, contentDescription = null) },
        trailingIcon = if (query.isNotEmpty()) {
            {
                IconButton(onClick = { state.setTextAndPlaceCursorAtEnd("") }) {
                    Icon(VolaIcons.Close, contentDescription = clearContentDescription)
                }
            }
        } else {
            null
        },
        lineLimits = TextFieldLineLimits.SingleLine,
        shape = VolaLibrary.searchShape,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = colors.surfaceContainerHigh,
            unfocusedContainerColor = colors.surfaceContainerHigh,
            focusedIndicatorColor = VolaLibrary.searchIndicator,
            unfocusedIndicatorColor = VolaLibrary.searchIndicator,
        ),
    )
}
