package dev.sk2andy.materialbrowser.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Spacing
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Theme
import dev.sk2andy.materialbrowser.ui.theme.v5.VolaV5Theme

/* A site capsule's window under v5: the bar loading in Compact, and the page that did not load. */

@Composable
private fun CapsuleBarPreview(dark: Boolean) {
    VolaV5Theme(dark = dark) {
        Column(Modifier.background(V5Theme.colors.background).padding(vertical = V5Spacing.space4)) {
            CapsuleLoadingLineV5(progress = 0.4f)
            CapsuleChromeV5(
                name = "Почта",
                address = "mail.example.com",
                canGoBack = true,
                isLoading = true,
                onBack = {},
                onReloadOrStop = {},
                onOpenFull = {},
            )
        }
    }
}

@Preview(name = "Capsule v5 · bar, light")
@Composable
private fun CapsuleChromeV5LightPreview() = CapsuleBarPreview(dark = false)

@Preview(name = "Capsule v5 · bar, dark")
@Composable
private fun CapsuleChromeV5DarkPreview() = CapsuleBarPreview(dark = true)

@Preview(name = "Capsule v5 · not loaded", heightDp = 560)
@Composable
private fun CapsuleErrorV5Preview() {
    VolaV5Theme(dark = false) { CapsuleErrorV5(message = "net::ERR_NAME_NOT_RESOLVED", onRetry = {}) }
}
