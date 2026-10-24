package dev.sk2andy.materialbrowser.ui.v5

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import dev.sk2andy.materialbrowser.browser.SiteConnectionKind
import dev.sk2andy.materialbrowser.shared.ui.icons.VolaIcons
import dev.sk2andy.materialbrowser.ui.theme.v5.V5SiteTint
import dev.sk2andy.materialbrowser.ui.theme.v5.V5SiteTints
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Theme
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Accent
import dev.sk2andy.materialbrowser.ui.theme.v5.VolaV5Theme

/* The v5 bar on a page, a loading page and a new tab, in light, dark and private. */

private const val PREVIEW_WIDTH_DP = 412
private const val PREVIEW_HEIGHT_DP = 200

private val PreviewGestures = V5BarGestures(
    tabSwipeEnabled = true,
    onTabDrag = {},
    onTabDragStopped = {},
    overviewEnabled = true,
    overviewProgress = mutableFloatStateOf(0f),
    onOverviewProgress = {},
    onOverviewStarted = {},
    onOverviewCancelled = {},
)

private fun previewState(domain: String?, loading: Boolean = false, readable: Boolean = false) = V5BarState(
    tabId = "preview",
    domain = domain,
    isLoading = loading,
    progress = if (loading) 0.4f else 0f,
    tabCount = 7,
    siteAvailable = domain != null,
    permissionActivity = false,
    connectionKind = SiteConnectionKind.Https,
    blockedCount = 12,
    readerAvailable = readable,
)

@Composable
private fun BarPreview(
    state: V5BarState,
    collapsed: Boolean = false,
    tint: V5SiteTint? = null,
    listen: V5ListenRowState? = null,
) {
    Box(Modifier.fillMaxSize().background(V5Theme.colors.background)) {
        V5BottomBar(
            state = state,
            gestures = PreviewGestures,
            tint = tint ?: V5SiteTints.of(null, V5Theme.colors.chrome, V5Theme.colors.label),
            collapsed = collapsed,
            onExpand = {},
            onAddress = {},
            onShield = {},
            onReloadOrStop = {},
            onTabs = {},
            onBarPositioned = { _, _ -> },
            menuButton = { IconButton(onClick = {}) { Icon(VolaIcons.MoreVert, contentDescription = null) } },
            accessory = listen?.let { row -> { V5ListenRow(state = row, onPlayPause = {}, onClose = {}) } },
        )
    }
}

@Preview(name = "v5 bar · light", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun V5BarLightPreview() {
    VolaV5Theme(dark = false) { BarPreview(previewState("polar-notes.ru")) }
}

@Preview(name = "v5 bar · dark, loading", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun V5BarDarkPreview() {
    VolaV5Theme(dark = true) { BarPreview(previewState("a-very-long-subdomain.example-news-site.com", loading = true)) }
}

@Preview(name = "v5 bar · article, «Read» (V19)", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun V5BarReadablePreview() {
    VolaV5Theme(dark = false) { BarPreview(previewState("polar-notes.ru", readable = true)) }
}

@Preview(name = "v5 bar · article, «Read», dark", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun V5BarReadableDarkPreview() {
    VolaV5Theme(dark = true) { BarPreview(previewState("a-very-long-subdomain.example-news-site.com", readable = true)) }
}

@Preview(name = "v5 bar · new tab", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun V5BarNewTabPreview() {
    VolaV5Theme(dark = false) { BarPreview(previewState(null)) }
}

@Preview(name = "v5 bar · private", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun V5BarPrivatePreview() {
    VolaV5Theme(privateBrowsing = true) { BarPreview(previewState("polar-notes.ru")) }
}

@Preview(name = "v5 bar · collapsed", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun V5BarCollapsedPreview() {
    VolaV5Theme(dark = true) { BarPreview(previewState("polar-notes.ru"), collapsed = true) }
}

@Preview(name = "v5 bar · site colors", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP * 3)
@Composable
private fun V5BarSiteColorPreview() {
    VolaV5Theme(dark = false) {
        androidx.compose.foundation.layout.Column {
            listOf(0x1DA1F2, 0xFFCC00, 0x1F4E5F).forEach { site ->
                Box(Modifier.weight(1f)) {
                    BarPreview(
                        previewState("example.com"),
                        tint = V5SiteTints.of(site, V5Theme.colors.chrome, V5Theme.colors.label),
                    )
                }
            }
        }
    }
}

@Preview(name = "v5 bar · listening (V19b)", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun V5BarListeningPreview() {
    VolaV5Theme(dark = false) {
        BarPreview(
            previewState("polar-notes.ru"),
            listen = V5ListenRowState(
                title = "Как полярники готовятся к долгой зимовке",
                speaking = true,
            ),
        )
    }
}

@Preview(name = "v5 bar · listening paused, dark", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun V5BarListeningPausedPreview() {
    VolaV5Theme(dark = true) {
        BarPreview(
            previewState("polar-notes.ru"),
            listen = V5ListenRowState(title = null, speaking = false),
        )
    }
}

@Preview(name = "v5 bar · preparing a page to listen (V19c)", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun V5BarListenPreparingPreview() {
    VolaV5Theme(dark = false) {
        BarPreview(
            previewState("polar-notes.ru"),
            listen = V5ListenRowState(
                title = "Как полярники готовятся к долгой зимовке",
                speaking = false,
                canPlay = false,
                message = "Готовлю страницу к чтению вслух…",
            ),
        )
    }
}

/** The shield (canvas «Bar»): a site using the camera gets the accent dot, a large count stays tabular. */
@Preview(name = "v5 bar · shield, camera in use, green", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun V5BarShieldActivityPreview() {
    VolaV5Theme(dark = false, accent = V5Accent.Green) {
        BarPreview(previewState("polar-notes.ru").copy(permissionActivity = true, blockedCount = 1284))
    }
}

/** A plain HTTP site: the shield warns; at the largest font the number leaves it (system-v5.md 8). */
@Preview(name = "v5 bar · shield, HTTP, largest font, dark", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP, fontScale = 2f)
@Composable
private fun V5BarShieldHttpLargeFontPreview() {
    VolaV5Theme(dark = true) {
        BarPreview(previewState("polar-notes.ru").copy(connectionKind = SiteConnectionKind.Http))
    }
}
