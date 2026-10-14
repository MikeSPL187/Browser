package dev.sk2andy.materialbrowser.ui.v5

import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Sizes

internal object V5PullRefreshTestTags {
    const val Indicator = "v5_pull_refresh_indicator"
}

/**
 * The pull to refresh indicator (V16, system-v5.md section 7): the M3 Expressive shape that morphs
 * with the pull ([progress] 0..1, read while drawing) and keeps morphing while the page reloads
 * ([progress] null). The experimental API stays in this one component.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun V5PullRefreshIndicator(progress: (() -> Float)?, color: Color, modifier: Modifier = Modifier) {
    val sized = modifier.size(V5Sizes.pullRefreshIndicator).testTag(V5PullRefreshTestTags.Indicator)
    if (progress == null) {
        LoadingIndicator(modifier = sized, color = color)
    } else {
        LoadingIndicator(progress = { progress().coerceIn(0f, 1f) }, modifier = sized, color = color)
    }
}
