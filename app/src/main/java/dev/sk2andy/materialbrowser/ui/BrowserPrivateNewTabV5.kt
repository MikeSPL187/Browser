package dev.sk2andy.materialbrowser.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Tile
import dev.sk2andy.materialbrowser.ui.theme.v5.VolaV5Theme
import dev.sk2andy.materialbrowser.ui.v5.V5PrivateFact
import dev.sk2andy.materialbrowser.ui.v5.V5PrivateLock
import dev.sk2andy.materialbrowser.ui.v5.V5PrivateNewTab
import dev.sk2andy.materialbrowser.ui.v5.V5PrivateNewTabState

/**
 * The v5 private new tab (V14a, canvas «Private») bound to the engine: the same honest facts as the
 * current page (PrivateTabRules) on colored tiles, the lock on exit, and the downloads that stay as
 * a note under them, in the private palette. «Close all private tabs» lives in the overview and on
 * the lock screen (audit question 4).
 */
@Composable
internal fun BrowserPrivateNewTabV5(privateTab: NewTabPrivate, contentPadding: PaddingValues) {
    val lock = privateTab.lock
    val facts = PrivateTabRules.facts(privateTab.storage)
    val state = V5PrivateNewTabState(
        // Only GeckoView keeps everything in memory; with WebView the page promises less.
        body = stringResource(
            if (privateTab.storage == PrivateTabStorage.Memory) R.string.v5_private_body else R.string.private_tab_body,
        ),
        facts = facts.filterNot { it.title == R.string.private_tab_fact_downloads }.map { fact ->
            V5PrivateFact(
                icon = fact.icon,
                title = stringResource(fact.title),
                caption = stringResource(fact.caption),
                tile = privateFactTile(fact.icon),
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
        note = stringResource(R.string.v5_private_downloads_note)
            .takeIf { facts.any { it.title == R.string.private_tab_fact_downloads } },
    )
    VolaV5Theme(privateBrowsing = true) {
        V5PrivateNewTab(
            state = state,
            onLockChange = { checked -> lock?.onCheckedChange?.invoke(checked) },
            contentPadding = contentPadding,
        )
    }
}

/** Each fact's tile, by its glyph: one color per kind of promise, as the canvas paints them. */
private fun privateFactTile(@DrawableRes icon: Int): V5Tile = when (icon) {
    R.drawable.ic_history -> V5Tile.Sky
    R.drawable.ic_symbol_cookie -> V5Tile.Purple
    R.drawable.ic_symbol_shield -> V5Tile.Green
    else -> V5Tile.Gray
}
