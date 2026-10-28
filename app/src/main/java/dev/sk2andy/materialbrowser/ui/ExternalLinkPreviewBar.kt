package dev.sk2andy.materialbrowser.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import dev.sk2andy.materialbrowser.browser.BrowserBackdropBlurRegion
import dev.sk2andy.materialbrowser.browser.BrowserProfile
import dev.sk2andy.materialbrowser.browser.ExternalLinkPreviewState
internal object ExternalLinkPreviewTestTags {
    const val Bar = "external_link_preview_bar"
    const val Back = "external_link_preview_back"
    const val Host = "external_link_preview_host"
    const val Open = "external_link_preview_open"
    const val Profile = "external_link_preview_profile"
    const val Overflow = "external_link_preview_overflow"
    const val Share = "external_link_preview_share"
    const val CopyLink = "external_link_preview_copy_link"
    const val Find = "external_link_preview_find"
    const val Desktop = "external_link_preview_desktop"

    fun profile(profileId: String) = "external_link_preview_profile:$profileId"
}

@Composable
internal fun ExternalLinkPreviewBar(
    state: ExternalLinkPreviewState,
    profiles: List<BrowserProfile>,
    isDesktopView: Boolean,
    backdropSource: CandyChromeBackdropSource?,
    rootBottomInWindowPx: Int,
    onDismissPreview: () -> Unit,
    onOpenInCandy: () -> Unit,
    onSelectProfile: (String) -> Unit,
    onShare: () -> Unit,
    onCopyLink: () -> Unit,
    onFindInPage: () -> Unit,
    onDesktopViewChange: (Boolean) -> Unit,
    onBackdropBlurRegionChanged: (BrowserBackdropBlurRegion?) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    ExternalLinkPreviewBarV5(
        state = state,
        profiles = profiles,
        isDesktopView = isDesktopView,
        onDismissPreview = onDismissPreview,
        onOpenInCandy = onOpenInCandy,
        onSelectProfile = onSelectProfile,
        onShare = onShare,
        onCopyLink = onCopyLink,
        onFindInPage = onFindInPage,
        onDesktopViewChange = onDesktopViewChange,
        onBackdropBlurRegionChanged = onBackdropBlurRegionChanged,
        modifier = modifier,
    )
}

