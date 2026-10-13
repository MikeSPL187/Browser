package dev.sk2andy.materialbrowser.ui.v5

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import dev.sk2andy.materialbrowser.shared.ui.icons.VolaIcons
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Sizes
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Spacing
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Theme
import dev.sk2andy.materialbrowser.ui.theme.v5.VolaV5Theme

/* V22: «•••» with the downloads ring — a quarter, almost done, and a size not yet known. */

@Composable
private fun RingsPreview(dark: Boolean) {
    VolaV5Theme(dark = dark) {
        Row(
            Modifier.background(V5Theme.colors.chrome).padding(V5Spacing.space4),
            horizontalArrangement = Arrangement.spacedBy(V5Spacing.space4),
        ) {
            listOf(
                V5DownloadRingState.Progress(count = 1, fraction = 0.25f),
                V5DownloadRingState.Progress(count = 2, fraction = 0.9f),
                V5DownloadRingState.Indeterminate(count = 1),
            ).forEach { state ->
                Box(
                    Modifier.size(V5Sizes.barCapsule).clip(CircleShape).background(V5Theme.colors.fill),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(VolaIcons.MoreVert, contentDescription = null, tint = V5Theme.colors.label)
                    V5DownloadRing(state, color = V5Theme.colors.accent, modifier = Modifier.size(V5Sizes.barCapsule))
                }
            }
        }
    }
}

@Preview(name = "Downloads ring · light")
@Composable
private fun V5DownloadRingLightPreview() = RingsPreview(dark = false)

@Preview(name = "Downloads ring · dark")
@Composable
private fun V5DownloadRingDarkPreview() = RingsPreview(dark = true)
