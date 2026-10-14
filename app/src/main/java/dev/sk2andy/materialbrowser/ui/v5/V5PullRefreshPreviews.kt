package dev.sk2andy.materialbrowser.ui.v5

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Spacing
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Theme
import dev.sk2andy.materialbrowser.ui.theme.v5.VolaV5Theme

/* V16: the pull to refresh indicator a third pulled, at the threshold, and while the page reloads. */

@Composable
private fun IndicatorsPreview(dark: Boolean) {
    VolaV5Theme(dark = dark) {
        Row(
            Modifier.background(V5Theme.colors.background).padding(V5Spacing.space4),
            horizontalArrangement = Arrangement.spacedBy(V5Spacing.space6),
        ) {
            V5PullRefreshIndicator(progress = { PULLED_A_THIRD }, color = V5Theme.colors.accent)
            V5PullRefreshIndicator(progress = { 1f }, color = V5Theme.colors.accent)
            V5PullRefreshIndicator(progress = null, color = V5Theme.colors.accent)
        }
    }
}

@Preview(name = "Pull to refresh · light")
@Composable
private fun V5PullRefreshLightPreview() = IndicatorsPreview(dark = false)

@Preview(name = "Pull to refresh · dark")
@Composable
private fun V5PullRefreshDarkPreview() = IndicatorsPreview(dark = true)

private const val PULLED_A_THIRD = 0.33f
