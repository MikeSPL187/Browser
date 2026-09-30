package dev.sk2andy.materialbrowser.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.sk2andy.materialbrowser.data.AppearanceSettings
import dev.sk2andy.materialbrowser.data.BrowserAddressBarStyle
import dev.sk2andy.materialbrowser.data.BrowserAppearanceMode
import dev.sk2andy.materialbrowser.data.BrowserChromeStyle
import dev.sk2andy.materialbrowser.shared.ui.icons.VolaIcons
import dev.sk2andy.materialbrowser.ui.theme.MaterialBrowserTheme
import dev.sk2andy.materialbrowser.ui.theme.VolaFrame
import dev.sk2andy.materialbrowser.ui.theme.VolaSpacing
import dev.sk2andy.materialbrowser.ui.theme.VolaTheme
import dev.sk2andy.materialbrowser.ui.theme.auraBrush

private const val PREVIEW_WIDTH_DP = 360
private const val PREVIEW_HEIGHT_DP = 780
private val PreviewStatusBarHeight = 40.dp
private val PreviewNavigationBarHeight = 24.dp

/**
 * The browser shell in both layouts, without an engine: the page is a stand-in article. The frame
 * geometry, aura and corner mask are the ones the browser screen uses.
 */
@Composable
private fun BrowserShellPreviewContent(
    mode: BrowserAppearanceMode,
    chromeStyle: BrowserChromeStyle,
) {
    val settings = AppearanceSettings(appearanceMode = mode, chromeStyle = chromeStyle)
    MaterialBrowserTheme(settings = settings) {
        val density = LocalDensity.current
        val framed = BrowserContentFrameRules.isFramed(
            chromeStyle = chromeStyle,
            browserChromeVisible = true,
            isBlankPage = false,
        )
        val frame = with(density) {
            BrowserContentFrameRules.resolve(
                framed = framed,
                safeLeftPx = 0,
                safeTopPx = PreviewStatusBarHeight.roundToPx(),
                safeRightPx = 0,
                safeBottomPx = PreviewNavigationBarHeight.roundToPx(),
                sideGutterPx = VolaFrame.sideGutter.toPx(),
                barGapPx = VolaFrame.barGap.toPx(),
                addressBarReservePx = (
                    addressBarExpandedHeight(BrowserAddressBarStyle.Classic) +
                        ADDRESS_BAR_VERTICAL_MARGIN
                    ).toPx(),
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(VolaTheme.auraBrush),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .contentFramePadding(frame)
                    .background(
                        if (framed) {
                            VolaTheme.extendedColors.card
                        } else {
                            MaterialTheme.colorScheme.surface
                        },
                    ),
            ) {
                PreviewArticle(topInset = if (framed) 0.dp else PreviewStatusBarHeight)
            }
            if (framed) {
                BrowserContentFrameMask(frame = frame, modifier = Modifier.fillMaxSize())
            }
            PreviewAddressBar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = PreviewNavigationBarHeight)
                    .padding(horizontal = VolaSpacing.x4, vertical = ADDRESS_BAR_VERTICAL_MARGIN),
            )
        }
    }
}

@Composable
private fun PreviewArticle(topInset: Dp) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = topInset)
            .padding(VolaSpacing.x5),
        verticalArrangement = Arrangement.spacedBy(VolaSpacing.x3),
    ) {
        Text(
            text = "Winter · 12 min read",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = "Lake Baikal in winter: choosing a route across the ice",
            style = MaterialTheme.typography.headlineMedium,
        )
        BoxWithConstraints {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(maxWidth * 0.55f)
                    .clip(VolaFrame.pageShape)
                    .background(MaterialTheme.colorScheme.tertiaryContainer),
            )
        }
        Text(
            text = "The ice becomes strong by mid-February. Until then, prefer walks along the " +
                "shore and routes with a guide.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PreviewAddressBar(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(addressBarExpandedHeight(BrowserAddressBarStyle.Classic))
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .padding(horizontal = VolaSpacing.x4),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(VolaSpacing.x2),
    ) {
        Icon(
            imageVector = VolaIcons.Lock,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "north-guide.ru",
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.size(VolaSpacing.x1))
        Icon(imageVector = VolaIcons.MoreVert, contentDescription = null)
    }
}

@Preview(name = "Shell · Frame · light", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun FrameLightPreview() {
    BrowserShellPreviewContent(BrowserAppearanceMode.Light, BrowserChromeStyle.Frame)
}

@Preview(
    name = "Shell · Frame · dark",
    widthDp = PREVIEW_WIDTH_DP,
    heightDp = PREVIEW_HEIGHT_DP,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun FrameDarkPreview() {
    BrowserShellPreviewContent(BrowserAppearanceMode.Dark, BrowserChromeStyle.Frame)
}

@Preview(name = "Shell · Air · light", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun AirLightPreview() {
    BrowserShellPreviewContent(BrowserAppearanceMode.Light, BrowserChromeStyle.Air)
}

@Preview(
    name = "Shell · Air · dark",
    widthDp = PREVIEW_WIDTH_DP,
    heightDp = PREVIEW_HEIGHT_DP,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun AirDarkPreview() {
    BrowserShellPreviewContent(BrowserAppearanceMode.Dark, BrowserChromeStyle.Air)
}

@Preview(
    name = "Shell · Frame · light · 200 %",
    widthDp = PREVIEW_WIDTH_DP,
    heightDp = PREVIEW_HEIGHT_DP,
    fontScale = 2f,
)
@Composable
private fun FrameLargeFontPreview() {
    BrowserShellPreviewContent(BrowserAppearanceMode.Light, BrowserChromeStyle.Frame)
}
