package dev.sk2andy.materialbrowser.data

enum class TabOverviewMode(val wireValue: String) {
    Hero("hero"),
    Grid("grid"),
    List("list"),
    ;

    companion object {
        /** The grid of the W-Tabs board; a layout the user picked in settings is kept. */
        fun fromWireValue(
            value: String?,
            fallback: TabOverviewMode = Grid,
        ): TabOverviewMode = entries.firstOrNull { it.wireValue == value } ?: fallback
    }
}
