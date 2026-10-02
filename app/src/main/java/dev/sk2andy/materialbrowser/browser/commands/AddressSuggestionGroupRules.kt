package dev.sk2andy.materialbrowser.browser.commands

/**
 * Groups of the address suggestions on the Editing board, in screen order from the top down.
 * The list sits above the address field at the bottom, so the last group is the closest to the
 * thumb: open tabs first, then searches, then pages from the library.
 */
enum class AddressSuggestionGroup {
    Commands,
    Recall,
    Library,
    Search,
    OpenTabs,
}

object AddressSuggestionGroupRules {
    fun group(item: AddressSuggestionItem): AddressSuggestionGroup = when (item) {
        is AddressSuggestionItem.Command -> AddressSuggestionGroup.Commands
        is AddressSuggestionItem.Recall -> AddressSuggestionGroup.Recall
        is AddressSuggestionItem.Search -> AddressSuggestionGroup.Search
        is AddressSuggestionItem.Navigation -> if (item.suggestion.openTabId == null) {
            AddressSuggestionGroup.Library
        } else {
            AddressSuggestionGroup.OpenTabs
        }
    }

    /**
     * [items] in screen order: grouped as [AddressSuggestionGroup] lists them, each group keeping
     * the composer's order. Keyboard highlight and submission work on this list.
     */
    fun displayOrder(items: List<AddressSuggestionItem>): List<AddressSuggestionItem> =
        items.sortedBy { item -> group(item).ordinal }

    /** Whether a divider goes above [index]: the first item of every group after the first. */
    fun startsGroup(items: List<AddressSuggestionItem>, index: Int): Boolean {
        if (index <= 0 || index > items.lastIndex) return false
        return group(items[index]) != group(items[index - 1])
    }
}
