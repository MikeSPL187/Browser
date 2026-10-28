package dev.sk2andy.materialbrowser.ui

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import dev.sk2andy.materialbrowser.browser.BLANK_URL
import dev.sk2andy.materialbrowser.browser.BrowserController
import dev.sk2andy.materialbrowser.browser.BrowserTab

/**
 * A snapshot of [tab]'s page for the glass of a v5 sheet opened over it (system-v5.md section 4),
 * taken afresh as the sheet opens. A new tab has no page and a private tab is never captured, so
 * both keep the solid material.
 */
@Composable
internal fun rememberPageBackdrop(controller: BrowserController, tab: BrowserTab, visible: Boolean): Bitmap? {
    val page = !tab.isIncognito && tab.url != BLANK_URL
    LaunchedEffect(visible, page) {
        if (visible && page && tab.id == controller.selectedTabId) controller.refreshSelectedTabPreview()
    }
    return controller.previews[tab.id]?.takeIf { page }
}
