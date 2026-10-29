package dev.sk2andy.materialbrowser.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.browser.BLANK_URL
import dev.sk2andy.materialbrowser.browser.BrowserTab

@Composable
internal fun displayTabTitle(tab: BrowserTab): String =
    if (hidesPrivateTab(tab)) {
        // A locked private tab is only «Private tab» until it is unlocked.
        stringResource(R.string.private_tab_title)
    } else if (tab.url == BLANK_URL || tab.title.isBlank()) {
        stringResource(R.string.new_tab_title)
    } else {
        tab.title
    }
