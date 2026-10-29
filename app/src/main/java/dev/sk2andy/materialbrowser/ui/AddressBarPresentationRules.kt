package dev.sk2andy.materialbrowser.ui

internal enum class AddressBarPresentation {
    Docked,
    Compact,
    Expanded,
    Overview,
    CommandFeedback,
}

internal object AddressBarControlRules {
    fun tabCountLabel(tabCount: Int): String =
        if (tabCount > 99) "∞" else tabCount.coerceAtLeast(0).toString()

    fun editorUsesFullWidth(
        editing: Boolean,
        addressFieldFocused: Boolean,
        imeVisible: Boolean,
    ): Boolean = editing && addressFieldFocused && imeVisible
}
