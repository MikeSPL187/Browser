package dev.sk2andy.materialbrowser.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import dev.sk2andy.materialbrowser.browser.AddressResolver
import dev.sk2andy.materialbrowser.data.FavoriteEntry

internal fun favoriteInitial(favorite: FavoriteEntry): String =
    LibraryRules.initial(favorite.title.ifBlank { AddressResolver.displayText(favorite.url) })
