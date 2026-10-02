package dev.sk2andy.materialbrowser.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import dev.sk2andy.materialbrowser.browser.BrowserProfile
import dev.sk2andy.materialbrowser.browser.BrowserTab
import dev.sk2andy.materialbrowser.browser.WorkspaceAccent
import dev.sk2andy.materialbrowser.data.AppearanceSettings
import dev.sk2andy.materialbrowser.data.BrowserAppearanceMode
import dev.sk2andy.materialbrowser.data.EssentialEntry
import dev.sk2andy.materialbrowser.ui.theme.MaterialBrowserTheme
import dev.sk2andy.materialbrowser.ui.theme.VolaPreviews
import dev.sk2andy.materialbrowser.ui.theme.VolaSpacing
import dev.sk2andy.materialbrowser.ui.theme.VolaTabOverview
import dev.sk2andy.materialbrowser.ui.theme.VolaTheme
import dev.sk2andy.materialbrowser.ui.theme.auraBrush

private val previewWorkspaces = listOf(
    BrowserProfile(id = "work", emoji = "💼", name = "Work", accent = WorkspaceAccent.Teal),
    BrowserProfile(id = "anime", emoji = "🎬", name = "Anime", accent = WorkspaceAccent.Coral),
    BrowserProfile(id = "personal", emoji = "🏠", name = "Personal", accent = WorkspaceAccent.Violet),
)

private val previewEssentials = listOf(
    EssentialEntry("https://mail.example.com/", "Mail"),
    EssentialEntry("https://calendar.example.com/", "Calendar"),
    EssentialEntry("https://tasks.example.com/", "Tasks"),
    EssentialEntry("https://docs.example.com/", "Docs"),
    EssentialEntry("https://chat.example.com/", "Chat"),
)

private fun previewTab(id: String, title: String, url: String, pinned: Boolean = false) =
    BrowserTab(id = id, lastAccessedAt = 0L, title = title, url = url, isPinned = pinned)

@Composable
private fun TabOverviewPreviewFrame(content: @Composable () -> Unit) {
    MaterialBrowserTheme(settings = AppearanceSettings(appearanceMode = BrowserAppearanceMode.System)) {
        Column(
            modifier = Modifier
                .background(VolaTheme.auraBrush)
                .padding(vertical = VolaSpacing.x4),
            verticalArrangement = Arrangement.spacedBy(VolaSpacing.x4),
        ) {
            content()
        }
    }
}

/** A grid card as the grid draws it: the title row over a page placeholder. */
@Composable
private fun PreviewGridCard(tab: BrowserTab, selected: Boolean) {
    val style = tabOverviewCardStyle()
    Surface(
        modifier = Modifier.width(PREVIEW_CARD_WIDTH),
        shape = style.shape,
        color = style.containerColor,
        border = style.selectedBorder.takeIf { selected },
        shadowElevation = if (selected) style.selectedElevation else style.elevation,
    ) {
        Column {
            TabGridCardTitleRow(tab = tab, favicon = null, enabled = true, onClose = {})
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(PREVIEW_PAGE_ASPECT_RATIO),
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                shape = RectangleShape,
            ) {}
        }
    }
}

@VolaPreviews
@Composable
private fun TabOverviewHeaderAndCardsPreview() {
    TabOverviewPreviewFrame {
        TabOverviewHeader(
            title = "Work",
            tabCount = 6,
            enabled = true,
            pinnedJumpVisible = true,
            onPinnedJump = {},
            onSettings = {},
            onMore = {},
        )
        Row(
            modifier = Modifier.padding(horizontal = VolaSpacing.x4),
            horizontalArrangement = Arrangement.spacedBy(VolaTabOverview.essentialGap),
        ) {
            PreviewGridCard(
                tab = previewTab("a", "Launch plan for October", "https://docs.example.com/plan"),
                selected = false,
            )
            PreviewGridCard(
                tab = previewTab("b", "Lake Baikal in winter", "https://travel.example.org/"),
                selected = true,
            )
        }
    }
}

@VolaPreviews
@Composable
private fun TabOverviewDockPreview() {
    TabOverviewPreviewFrame {
        TabOverviewDock(
            essentials = previewEssentials,
            essentialIcons = emptyMap(),
            workspaces = previewWorkspaces,
            activeWorkspaceId = "work",
            showWorkspaces = true,
            enabled = true,
            onOpenEssential = {},
            onSelectWorkspace = {},
            onWorkspaceLongClick = {},
            onAddWorkspace = {},
            onNewTab = {},
        )
        TabOverviewDock(
            essentials = emptyList(),
            essentialIcons = emptyMap(),
            workspaces = emptyList(),
            activeWorkspaceId = "",
            showWorkspaces = false,
            enabled = true,
            onOpenEssential = {},
            onSelectWorkspace = {},
            onWorkspaceLongClick = {},
            onAddWorkspace = {},
            onNewTab = {},
        )
    }
}

private val PREVIEW_CARD_WIDTH = 170.dp // token-exempt: preview frame only
private const val PREVIEW_PAGE_ASPECT_RATIO = 0.72f
