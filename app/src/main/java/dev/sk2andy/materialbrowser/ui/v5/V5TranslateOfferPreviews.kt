package dev.sk2andy.materialbrowser.ui.v5

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Accent
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Theme
import dev.sk2andy.materialbrowser.ui.theme.v5.VolaV5Theme

/* Vola's offer to translate the page as canvas screen «Translate»: light, dark, an accent, the largest font. */

private const val PREVIEW_WIDTH_DP = 390
private const val PREVIEW_HEIGHT_DP = 844

@Composable
private fun OfferPreview(dark: Boolean, accent: V5Accent = V5Accent.Blue) {
    VolaV5Theme(dark = dark, accent = accent) {
        Box(Modifier.fillMaxSize().background(V5Theme.colors.background)) {
            V5TranslateOfferCard(
                state = V5TranslateOfferState(to = "русский", from = "английский"),
                onTranslate = {},
                onNeverOnSite = {},
                onClose = {},
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

@Preview(name = "Translate offer · light", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun V5TranslateOfferLightPreview() = OfferPreview(dark = false)

@Preview(name = "Translate offer · dark", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun V5TranslateOfferDarkPreview() = OfferPreview(dark = true)

@Preview(name = "Translate offer · orange accent", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun V5TranslateOfferAccentPreview() = OfferPreview(dark = false, accent = V5Accent.Orange)

@Preview(name = "Translate offer · largest font", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP, fontScale = 2f)
@Composable
private fun V5TranslateOfferLargeFontPreview() = OfferPreview(dark = false)
