package dev.sk2andy.materialbrowser.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.ui.theme.v5.VolaV5Theme
import dev.sk2andy.materialbrowser.ui.v5.V5PrivateFact
import dev.sk2andy.materialbrowser.ui.v5.V5PrivateLock
import dev.sk2andy.materialbrowser.ui.v5.V5PrivateNewTab
import dev.sk2andy.materialbrowser.ui.v5.V5PrivateNewTabState

/**
 * The v5 private new tab (V14a) bound to the engine: the same honest facts as the current page
 * (PrivateTabRules), the lock on exit and «Close all private tabs», in the private palette.
 */
@Composable
internal fun BrowserPrivateNewTabV5(privateTab: NewTabPrivate, contentPadding: PaddingValues) {
    val lock = privateTab.lock
    val state = V5PrivateNewTabState(
        // Only GeckoView keeps everything in memory; with WebView the page promises less.
        body = stringResource(
            if (privateTab.storage == PrivateTabStorage.Memory) R.string.v5_private_body else R.string.private_tab_body,
        ),
        facts = PrivateTabRules.facts(privateTab.storage).map { fact ->
            V5PrivateFact(
                icon = fact.icon,
                title = stringResource(fact.title),
                caption = stringResource(fact.caption),
                warning = fact.warning,
            )
        },
        lock = lock?.let {
            V5PrivateLock(
                checked = it.checked,
                available = it.available,
                caption = stringResource(
                    if (it.available) R.string.private_tab_lock_caption else R.string.profile_protection_unavailable,
                ),
            )
        },
        privateTabCount = privateTab.count,
    )
    VolaV5Theme(privateBrowsing = true) {
        V5PrivateNewTab(
            state = state,
            onLockChange = { checked -> lock?.onCheckedChange?.invoke(checked) },
            onCloseAll = privateTab.onCloseAll,
            contentPadding = contentPadding,
        )
    }
}
