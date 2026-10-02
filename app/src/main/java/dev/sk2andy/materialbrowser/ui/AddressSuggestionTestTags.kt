package dev.sk2andy.materialbrowser.ui

internal object AddressSuggestionTestTags {
    const val Card = "address_suggestions"
    const val ClipboardChip = "address_clipboard_chip"

    fun searchRow(query: String): String = "address_search_suggestion:$query"
    fun fillSearch(query: String): String = "address_search_suggestion_fill:$query"
    fun recallRow(url: String): String = "address_recall_suggestion:$url"
    fun navigationRow(url: String): String = "address_navigation_suggestion:$url"
}
