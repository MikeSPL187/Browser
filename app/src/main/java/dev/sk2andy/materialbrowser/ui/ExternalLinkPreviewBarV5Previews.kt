package dev.sk2andy.materialbrowser.ui

import androidx.compose.runtime.Composable
import dev.sk2andy.materialbrowser.browser.BrowserProfile
import dev.sk2andy.materialbrowser.browser.ExternalLinkPreviewState
import dev.sk2andy.materialbrowser.browser.WorkspaceAccent
import dev.sk2andy.materialbrowser.shared.ui.WorkspaceIcons
import dev.sk2andy.materialbrowser.ui.theme.MaterialBrowserTheme
import dev.sk2andy.materialbrowser.ui.theme.VolaPreviews

/* A link from another app under ui.v5. Preview-only names and addresses are not localized. */

private val previewProfiles = listOf(
    BrowserProfile(id = "home", emoji = WorkspaceIcons.emojiFor("home").orEmpty(), name = "Personal"),
    BrowserProfile(
        id = "work",
        emoji = WorkspaceIcons.emojiFor("work").orEmpty(),
        name = "Work",
        accent = WorkspaceAccent.Teal,
    ),
)

@Composable
private fun LinkPreviewBarPreview(profiles: List<BrowserProfile>, targetProfileId: String) {
    MaterialBrowserTheme {
        ExternalLinkPreviewBarV5(
            state = ExternalLinkPreviewState(
                sessionId = 1L,
                generation = 1,
                currentUrl = "https://www.north-guide.example/baikal/winter",
                targetProfileId = targetProfileId,
            ),
            profiles = profiles,
            isDesktopView = false,
            onDismissPreview = {},
            onOpenInCandy = {},
            onSelectProfile = {},
            onShare = {},
            onCopyLink = {},
            onFindInPage = {},
            onDesktopViewChange = {},
            onBackdropBlurRegionChanged = {},
        )
    }
}

/** Two profiles: «Open in Vola» in the target profile's accent, and the profile's symbol beside it. */
@VolaPreviews
@Composable
private fun ExternalLinkPreviewBarV5Preview() {
    LinkPreviewBarPreview(previewProfiles, targetProfileId = "work")
}

/** One profile: no profile button. */
@VolaPreviews
@Composable
private fun ExternalLinkPreviewBarV5SingleProfilePreview() {
    LinkPreviewBarPreview(previewProfiles.take(1), targetProfileId = "home")
}
