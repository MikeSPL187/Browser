package dev.sk2andy.materialbrowser.ui.v5

import dev.sk2andy.materialbrowser.browser.commands.AddressSuggestionItem
import dev.sk2andy.materialbrowser.data.AddressSuggestionSource

/*
 * How the v5 address editor lays out suggestions (system-v5.md, section 9): the list grows up from
 * the field, the best match sits right above it, then open tabs, favorites, history, page memory,
 * searches and, farthest away, commands. The suggestions themselves are the current ones; only
 * their order and grouping are new.
 */

/** Groups of the v5 list, from the field upwards. */
enum class V5SuggestionGroup {
    OpenTabs,
    Favorites,
    History,
    PageMemory,
    Search,
    Commands,
}

data class V5SuggestionSection(
    val group: V5SuggestionGroup,
    val items: List<AddressSuggestionItem>,
)

/** [best] right above the field, then [sections] from the field upwards. */
data class V5SuggestionLayout(
    val best: AddressSuggestionItem?,
    val sections: List<V5SuggestionSection>,
) {
    val isEmpty: Boolean get() = best == null && sections.isEmpty()

    /** Every suggestion from the field upwards: the order arrow keys walk. */
    val fromField: List<AddressSuggestionItem>
        get() = listOfNotNull(best) + sections.flatMap { it.items }

    companion object {
        val Empty = V5SuggestionLayout(best = null, sections = emptyList())
    }
}

object V5SuggestionLayoutRules {
    fun group(item: AddressSuggestionItem): V5SuggestionGroup = when (item) {
        is AddressSuggestionItem.Navigation -> when {
            item.suggestion.openTabId != null -> V5SuggestionGroup.OpenTabs
            item.suggestion.source == AddressSuggestionSource.Favorite -> V5SuggestionGroup.Favorites
            else -> V5SuggestionGroup.History
        }
        is AddressSuggestionItem.Recall -> V5SuggestionGroup.PageMemory
        is AddressSuggestionItem.Search -> V5SuggestionGroup.Search
        is AddressSuggestionItem.Command -> V5SuggestionGroup.Commands
    }

    /**
     * The best match is a page, never a search or a command: an open tab first (Safari's «Switch
     * to tab»), else the first page from the library, else page memory. Nothing is typed, nothing
     * is best: the list then only shows what is there.
     */
    fun best(items: List<AddressSuggestionItem>, query: String): AddressSuggestionItem? {
        if (query.isBlank()) return null
        return items.firstOrNull { group(it) == V5SuggestionGroup.OpenTabs }
            ?: items.firstOrNull { it is AddressSuggestionItem.Navigation }
            ?: items.firstOrNull { it is AddressSuggestionItem.Recall }
    }

    fun layout(items: List<AddressSuggestionItem>, query: String): V5SuggestionLayout {
        // The list keys rows by stableId and fails on a repeat: the first of each stays (А-6).
        val unique = items.distinctBy { it.stableId }
        val best = best(unique, query)
        val rest = unique.filterNot { it.stableId == best?.stableId }
        val sections = V5SuggestionGroup.entries.mapNotNull { group ->
            rest.filter { group(it) == group }
                .takeIf { it.isNotEmpty() }
                ?.let { V5SuggestionSection(group, it) }
        }
        return V5SuggestionLayout(best, sections)
    }

    /**
     * The suggestion an arrow key moves to from [currentId]: [step] 1 goes up, away from the field,
     * -1 comes back down; from nothing, up is the closest one. Past either end there is none.
     */
    fun step(layout: V5SuggestionLayout, currentId: String?, step: Int): AddressSuggestionItem? {
        val order = layout.fromField
        val index = order.indexOfFirst { it.stableId == currentId }
        val next = if (index < 0) (if (step > 0) 0 else -1) else index + step
        return order.getOrNull(next)
    }

    /**
     * Where [query] sits in [text], ignoring case, for the bold part of a row; null when it is not
     * there or nothing is typed.
     */
    fun match(text: String, query: String): IntRange? {
        val needle = query.trim()
        if (needle.isEmpty()) return null
        val start = text.indexOf(needle, ignoreCase = true)
        return if (start < 0) null else start until start + needle.length
    }
}
