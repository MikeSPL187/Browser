package dev.sk2andy.materialbrowser.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.browser.AddressResolver
import dev.sk2andy.materialbrowser.shared.ui.icons.VolaIcons
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Sizes
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Spacing
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Theme
import dev.sk2andy.materialbrowser.ui.v5.V5StateAction
import dev.sk2andy.materialbrowser.ui.v5.V5StatePage

/*
 * A site capsule's window under v5 on the phone. v5 makes no new capsules, but the ones made
 * before still open from their shortcuts: their bar, loading line and error read as v5's.
 */

/** The capsule's bar: back, reload or stop, its name (and address in Compact), «Open in Vola». */
@Composable
internal fun CapsuleChromeV5(
    name: String,
    address: String?,
    canGoBack: Boolean,
    isLoading: Boolean,
    onBack: () -> Unit,
    onReloadOrStop: () -> Unit,
    onOpenFull: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = V5Theme.colors
    Row(
        modifier
            .navigationBarsPadding()
            .padding(horizontal = V5Spacing.marginPhone, vertical = V5Spacing.space2)
            .fillMaxWidth()
            .heightIn(min = V5Sizes.barCapsule)
            .clip(V5Theme.shapes.full)
            .background(colors.chrome)
            .padding(horizontal = V5Spacing.space1)
            .testTag(SiteCapsuleTestTags.Chrome),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack, enabled = canGoBack) {
            Icon(
                VolaIcons.ArrowBack,
                contentDescription = stringResource(R.string.action_back),
                tint = if (canGoBack) colors.accent else colors.labelTertiary,
            )
        }
        IconButton(onClick = onReloadOrStop) {
            Icon(
                if (isLoading) VolaIcons.Close else VolaIcons.Refresh,
                contentDescription = stringResource(if (isLoading) R.string.action_stop_loading else R.string.action_reload),
                tint = colors.label,
            )
        }
        Column(Modifier.weight(1f).padding(horizontal = V5Spacing.space2)) {
            Text(
                name,
                style = V5Theme.type.footnote.copy(fontWeight = FontWeight.SemiBold),
                color = colors.label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (address != null) {
                Text(address, style = V5Theme.type.caption, color = colors.labelSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        TextButton(onClick = onOpenFull) {
            Text(
                stringResource(R.string.capsule_open_full_candy),
                style = V5Theme.type.footnote.copy(fontWeight = FontWeight.SemiBold),
                color = colors.accent,
                maxLines = 1,
            )
        }
    }
}

/** How far the page has loaded: the bar's thin accent line, at the top edge of the capsule. */
@Composable
internal fun CapsuleLoadingLineV5(progress: Float, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(V5Sizes.barProgress)) {
        Box(Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).fillMaxHeight().background(V5Theme.colors.accentFill))
    }
}

/** The page did not load: the v5 state page with what went wrong and «Try again». */
@Composable
internal fun CapsuleErrorV5(message: String, onRetry: () -> Unit) {
    V5StatePage(
        icon = VolaIcons.Error,
        title = stringResource(R.string.error_page_unreachable),
        message = message,
        primary = V5StateAction(stringResource(R.string.action_retry), onRetry, icon = VolaIcons.Refresh),
    )
}
