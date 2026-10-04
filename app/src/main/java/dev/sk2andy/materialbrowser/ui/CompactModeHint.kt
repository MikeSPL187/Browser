package dev.sk2andy.materialbrowser.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.data.AppearanceSettings
import dev.sk2andy.materialbrowser.data.BrowserAppearanceMode
import dev.sk2andy.materialbrowser.shared.ui.icons.VolaIcons
import dev.sk2andy.materialbrowser.ui.theme.MaterialBrowserTheme
import dev.sk2andy.materialbrowser.ui.theme.VolaCompactMode
import dev.sk2andy.materialbrowser.ui.theme.VolaElevation
import dev.sk2andy.materialbrowser.ui.theme.VolaMotion
import dev.sk2andy.materialbrowser.ui.theme.VolaPreviews
import dev.sk2andy.materialbrowser.ui.theme.VolaShapes
import dev.sk2andy.materialbrowser.ui.theme.VolaTheme
import kotlinx.coroutines.delay

internal object CompactModeHintTestTags {
    const val Hint = "compact_mode_hint"
}

/**
 * «Compact mode · Scroll up or tap the handle» (board W-Compact), right after Compact Mode is
 * switched on. It rises from the handle, reads itself out and leaves after a few seconds or a tap.
 */
@Composable
internal fun CompactModeHint(
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(VolaMotion.effects()) + slideInVertically(VolaMotion.standard()) { it / 2 },
        exit = fadeOut(VolaMotion.effects()) + slideOutVertically(VolaMotion.standard()) { it / 2 },
    ) {
        LaunchedEffect(Unit) {
            delay(VolaCompactMode.HINT_MILLIS)
            onDismiss()
        }
        CompactModeHintCard(onClick = onDismiss)
    }
}

@Composable
private fun CompactModeHintCard(onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier
            .testTag(CompactModeHintTestTags.Hint)
            .semantics { liveRegion = LiveRegionMode.Polite }
            .clickable(onClick = onClick),
        shape = VolaShapes.card,
        color = VolaTheme.extendedColors.card,
        shadowElevation = VolaElevation.level2,
    ) {
        Row(
            modifier = Modifier.padding(VolaCompactMode.hintPadding),
            horizontalArrangement = Arrangement.spacedBy(VolaCompactMode.hintGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(VolaCompactMode.hintIconContainerSize)
                    .background(colors.primaryContainer, VolaShapes.material.medium),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = VolaIcons.CloseFullscreen,
                    contentDescription = null,
                    modifier = Modifier.size(VolaCompactMode.hintIconSize),
                    tint = colors.onPrimaryContainer,
                )
            }
            Column {
                Text(
                    text = stringResource(R.string.compact_mode_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.onSurface,
                )
                Text(
                    text = stringResource(R.string.compact_mode_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
            }
        }
    }
}

@VolaPreviews
@Composable
private fun CompactModeHintPreview() {
    MaterialBrowserTheme(settings = AppearanceSettings(appearanceMode = BrowserAppearanceMode.System)) {
        Box(modifier = Modifier.padding(VolaCompactMode.hintPadding)) {
            CompactModeHintCard(onClick = {})
        }
    }
}
