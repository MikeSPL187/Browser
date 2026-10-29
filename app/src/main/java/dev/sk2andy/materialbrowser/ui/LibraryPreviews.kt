package dev.sk2andy.materialbrowser.ui

import dev.sk2andy.materialbrowser.browser.BrowserProfile
import dev.sk2andy.materialbrowser.browser.WorkspaceAccent
import dev.sk2andy.materialbrowser.shared.ui.WorkspaceIcons

private val previewWork = BrowserProfile(
    id = "work",
    emoji = WorkspaceIcons.emojiFor("work").orEmpty(),
    name = "Работа",
    accent = WorkspaceAccent.Teal,
)

private val previewAnime = BrowserProfile(
    id = "anime",
    emoji = WorkspaceIcons.emojiFor("gaming").orEmpty(),
    name = "Аниме",
    accent = WorkspaceAccent.Coral,
)
