package dev.sk2andy.materialbrowser.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.data.AppearanceSettings
import dev.sk2andy.materialbrowser.data.BrowserAppearanceMode
import dev.sk2andy.materialbrowser.ui.theme.MaterialBrowserTheme
import dev.sk2andy.materialbrowser.ui.theme.VolaElevation
import dev.sk2andy.materialbrowser.ui.theme.VolaPreviews
import dev.sk2andy.materialbrowser.ui.theme.VolaShapes
import dev.sk2andy.materialbrowser.ui.theme.VolaSpacing
import dev.sk2andy.materialbrowser.ui.theme.VolaStateTokens
import dev.sk2andy.materialbrowser.ui.theme.VolaTheme
import dev.sk2andy.materialbrowser.ui.theme.auraBrush

/** How a [VolaStateMessage] reads: an empty list, or something that went wrong. */
internal enum class VolaStateTone { Empty, Neutral, Error }

/**
 * The one shape every empty, offline or error state takes (board States): an icon in a tinted
 * square, a title, one sentence and at most one action. Lists show it instead of a blank area.
 */
@Composable
internal fun VolaStateMessage(
    icon: Painter,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    tone: VolaStateTone = VolaStateTone.Empty,
    actionLabel: String? = null,
    onAction: () -> Unit = {},
) {
    val colors = MaterialTheme.colorScheme
    val (container, content) = when (tone) {
        VolaStateTone.Empty -> colors.primaryContainer to colors.onPrimaryContainer
        VolaStateTone.Neutral -> colors.surfaceContainerHigh to colors.onSurfaceVariant
        VolaStateTone.Error -> colors.errorContainer to colors.onErrorContainer
    }
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = VolaShapes.material.extraLarge,
        color = VolaTheme.extendedColors.card,
        shadowElevation = VolaElevation.level1,
    ) {
        Column(
            modifier = Modifier.padding(
                start = VolaStateTokens.paddingHorizontal,
                end = VolaStateTokens.paddingHorizontal,
                top = VolaStateTokens.paddingTop,
                bottom = VolaStateTokens.paddingBottom,
            ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(VolaSpacing.x3),
        ) {
            Surface(
                modifier = Modifier.size(VolaStateTokens.iconContainerSize),
                shape = VolaStateTokens.iconContainerShape,
                color = container,
                contentColor = content,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = icon,
                        contentDescription = null,
                        modifier = Modifier.size(VolaStateTokens.iconSize),
                    )
                }
            }
            Text(
                text = title,
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.titleMedium,
                color = colors.onSurface,
                textAlign = TextAlign.Center,
            )
            Text(
                text = message,
                modifier = Modifier.widthIn(max = VolaStateTokens.messageMaxWidth),
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            if (actionLabel != null) {
                FilledTonalButton(
                    onClick = onAction,
                    modifier = Modifier
                        .padding(top = VolaSpacing.x2)
                        .height(VolaStateTokens.buttonHeight),
                    shape = CircleShape,
                ) {
                    Text(actionLabel, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@VolaPreviews
@Composable
private fun VolaStateMessagePreview() {
    MaterialBrowserTheme(settings = AppearanceSettings(appearanceMode = BrowserAppearanceMode.System)) {
        Box(
            modifier = Modifier
                .background(VolaTheme.auraBrush)
                .padding(VolaSpacing.x4),
        ) {
            VolaStateMessage(
                icon = painterResource(R.drawable.ic_push_pin),
                title = stringResource(R.string.essentials_empty_title),
                message = stringResource(R.string.essentials_empty_body),
                actionLabel = stringResource(R.string.essentials_empty_action),
            )
        }
    }
}
