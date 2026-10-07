package dev.sk2andy.materialbrowser.ui

/**
 * Tab stacks are hidden until the overview draws them (#123, screen 8, the owner's call): the
 * actions sheet could create a stack that the overview never showed. Stored stacks stay as they
 * are; turning this on brings back the sheet section, the folder setting and the menu entry.
 */
internal object TabStacksFeature {
    const val ENABLED = false
}
