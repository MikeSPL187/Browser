package dev.sk2andy.materialbrowser.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.data.AppearanceSettings
import dev.sk2andy.materialbrowser.data.BrowserAppearanceMode
import dev.sk2andy.materialbrowser.shared.ui.icons.VolaIcons
import dev.sk2andy.materialbrowser.ui.theme.MaterialBrowserTheme
import dev.sk2andy.materialbrowser.ui.theme.VolaPreviews
import dev.sk2andy.materialbrowser.ui.theme.VolaStatePageTokens
import dev.sk2andy.materialbrowser.ui.theme.VolaTheme
import dev.sk2andy.materialbrowser.ui.theme.auraBrush

/** The color of a [VolaStatePageIcon]: plain, a warning, a danger, or the page's accent. */
internal enum class VolaStatePageTone { Neutral, Warning, Error, Accent }

/**
 * A whole page in one state (boards W-Offline, W-HttpsOnly, W-Locked), the full-screen sibling
 * of [VolaStateMessage]: an icon, a title and one sentence near the middle, the actions at the
 * bottom where the thumb is. The text scrolls when large fonts need more room; the actions stay.
 *
 * [announce] reads the title out when it changes, for states that change by themselves (the
 * connection coming back) rather than after a tap. [details] go under the sentence, such as a
 * card of facts about the site.
 */
@Composable
internal fun VolaStatePage(
    title: String,
    message: String?,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    announce: Boolean = false,
    details: (@Composable ColumnScope.() -> Unit)? = null,
    actions: @Composable ColumnScope.() -> Unit = {},
) {
    val colors = MaterialTheme.colorScheme
    Column(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = VolaStatePageTokens.maxContentWidth)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(
                        horizontal = VolaStatePageTokens.contentPaddingHorizontal,
                        vertical = VolaStatePageTokens.contentPaddingVertical,
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(VolaStatePageTokens.contentGap),
            ) {
                Box(modifier = Modifier.padding(bottom = VolaStatePageTokens.iconBottomGap)) { icon() }
                Text(
                    text = title,
                    modifier = Modifier.semantics {
                        heading()
                        if (announce) liveRegion = LiveRegionMode.Polite
                    },
                    style = MaterialTheme.typography.headlineMedium,
                    color = colors.onSurface,
                    textAlign = TextAlign.Center,
                )
                if (message != null) {
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyLarge,
                        color = colors.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
                details?.invoke(this)
            }
        }
        Column(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .widthIn(max = VolaStatePageTokens.maxContentWidth)
                .fillMaxWidth()
                .padding(
                    start = VolaStatePageTokens.actionsPaddingHorizontal,
                    end = VolaStatePageTokens.actionsPaddingHorizontal,
                    bottom = VolaStatePageTokens.actionsPaddingBottom,
                ),
            verticalArrangement = Arrangement.spacedBy(VolaStatePageTokens.actionGap),
            content = actions,
        )
    }
}

/** The icon of a [VolaStatePage] in its rounded square. Decorative: the title says it all. */
@Composable
internal fun VolaStatePageIcon(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    tone: VolaStatePageTone = VolaStatePageTone.Neutral,
) {
    VolaStatePageIconContainer(tone = tone, modifier = modifier) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(VolaStatePageTokens.iconSize),
        )
    }
}

/** A [VolaStatePageIcon] that spins while the page tries again. */
@Composable
internal fun VolaStatePageProgress(modifier: Modifier = Modifier) {
    VolaStatePageIconContainer(tone = VolaStatePageTone.Accent, modifier = modifier) {
        CircularProgressIndicator(
            modifier = Modifier.size(VolaStatePageTokens.iconSize),
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun VolaStatePageIconContainer(
    tone: VolaStatePageTone,
    modifier: Modifier,
    content: @Composable () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val extended = VolaTheme.extendedColors
    val (container, contentColor) = when (tone) {
        VolaStatePageTone.Neutral -> colors.surfaceContainerHigh to colors.onSurfaceVariant
        VolaStatePageTone.Warning -> extended.warnContainer to extended.onWarnContainer
        VolaStatePageTone.Error -> colors.errorContainer to colors.onErrorContainer
        VolaStatePageTone.Accent -> colors.primaryContainer to colors.onPrimaryContainer
    }
    Surface(
        modifier = modifier.size(VolaStatePageTokens.iconContainerSize),
        shape = VolaStatePageTokens.iconContainerShape,
        color = container,
        contentColor = contentColor,
    ) {
        Box(contentAlignment = Alignment.Center) { content() }
    }
}

/** The main action of a [VolaStatePage]: a filled pill across the page. */
@Composable
internal fun VolaStatePagePrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(VolaStatePageTokens.buttonHeight),
        enabled = enabled,
        shape = CircleShape,
        contentPadding = if (icon != null) {
            ButtonDefaults.ButtonWithIconContentPadding
        } else {
            ButtonDefaults.ContentPadding
        },
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier
                    .padding(end = ButtonDefaults.IconSpacing)
                    .size(VolaStatePageTokens.buttonIconSize),
            )
        }
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}

/** A second action of a [VolaStatePage]: an outlined pill under the main one. */
@Composable
internal fun VolaStatePageSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(VolaStatePageTokens.buttonHeight),
        shape = CircleShape,
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}

/** A quiet way out of a [VolaStatePage], such as going to another workspace. */
@Composable
internal fun VolaStatePageTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TextButton(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = ButtonDefaults.textButtonColors(
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}

@VolaPreviews
@Composable
private fun VolaStatePagePreview() {
    MaterialBrowserTheme(settings = AppearanceSettings(appearanceMode = BrowserAppearanceMode.System)) {
        Box(modifier = Modifier.background(VolaTheme.auraBrush)) {
            VolaStatePage(
                title = stringResource(R.string.page_error_offline_title),
                message = stringResource(R.string.page_error_offline_body),
                icon = { VolaStatePageIcon(icon = VolaIcons.WifiOff) },
            ) {
                VolaStatePagePrimaryButton(
                    text = stringResource(R.string.action_retry),
                    onClick = {},
                    icon = VolaIcons.Refresh,
                )
                VolaStatePageSecondaryButton(
                    text = stringResource(R.string.page_error_network_settings),
                    onClick = {},
                )
            }
        }
    }
}
