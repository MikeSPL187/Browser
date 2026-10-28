package dev.sk2andy.materialbrowser.ui

import dev.sk2andy.materialbrowser.browser.BrowserProfile
import dev.sk2andy.materialbrowser.browser.BrowserTab
import dev.sk2andy.materialbrowser.browser.WorkspaceAccent
import dev.sk2andy.materialbrowser.data.EssentialEntry
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

