package dev.sk2andy.materialbrowser.browser

data class TabStack(
    val id: String,
    val profileId: String,
    val name: String,
    val color: TabStackColor,
    val tabIds: List<String>,
    val previewTabId: String? = null,
    val collapsedAnchorTabId: String? = null,
    val isCollapsed: Boolean = false,
)

/**
 * A group's color as it is stored. The first four are v4's; v5 adds orange, teal and slate for its
 * palette of seven (canvas «GroupEditor»). A value this version does not know reads as [Grape].
 */
enum class TabStackColor(val wireValue: String) {
    Grape("grape"),
    Cherry("cherry"),
    Lime("lime"),
    Blueberry("blueberry"),
    Orange("orange"),
    Teal("teal"),
    Slate("slate"),
    ;

    companion object {
        /** The colors v4's stack editor offers. */
        val classic: List<TabStackColor> = listOf(Grape, Cherry, Lime, Blueberry)

        /** The v5 group palette in the canvas order: orange, teal, purple, blue, pink, green, gray. */
        val palette: List<TabStackColor> = listOf(Orange, Teal, Grape, Blueberry, Cherry, Lime, Slate)

        fun fromWireValue(value: String?): TabStackColor =
            entries.firstOrNull { it.wireValue == value } ?: Grape
    }
}
