package dev.sk2andy.materialbrowser.ui.v5

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import dev.sk2andy.materialbrowser.shared.ui.WorkspaceIcons
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Accent
import dev.sk2andy.materialbrowser.ui.theme.v5.VolaV5Theme

/* Canvas screen «PrivateLock»: the private segment of the overview while its two tabs are locked. */

private const val PREVIEW_WIDTH_DP = 390
private const val PREVIEW_HEIGHT_DP = 844

@Composable
private fun LockedOverviewPreview(unlockAvailable: Boolean = true) {
    V5TabOverview(
        state = V5OverviewState(
            cards = emptyList(),
            privateSegment = true,
            counts = V5OverviewCounts(regular = 7, private = 2),
            query = "",
            profileLabel = "Личный",
            profileIcon = WorkspaceIcons.vector("🏔"),
            privateLock = V5OverviewPrivateLock(count = 2, unlockAvailable = unlockAvailable),
        ),
        onQueryChange = {},
        onSegmentChange = {},
        onSelect = {},
        onClose = {},
        onNewTab = {},
        onDone = {},
        onCloseAll = {},
    )
}

@Preview(name = "Private lock · light", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun V5PrivateLockLightPreview() {
    VolaV5Theme(dark = false, privateBrowsing = true) { LockedOverviewPreview() }
}

@Preview(name = "Private lock · dark", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun V5PrivateLockDarkPreview() {
    VolaV5Theme(dark = true, privateBrowsing = true) { LockedOverviewPreview() }
}

@Preview(name = "Private lock · green, no biometrics", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun V5PrivateLockAccentPreview() {
    VolaV5Theme(dark = false, accent = V5Accent.Green) { LockedOverviewPreview(unlockAvailable = false) }
}

@Preview(name = "Private lock · font 200 %", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP, fontScale = 2f)
@Composable
private fun V5PrivateLockLargeFontPreview() {
    VolaV5Theme(dark = true, privateBrowsing = true) { LockedOverviewPreview() }
}
