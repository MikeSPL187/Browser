package dev.sk2andy.materialbrowser.ui.passwords

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import dev.sk2andy.materialbrowser.ui.theme.v5.VolaV5Theme

/* The v5 generator sheet over a sign-up page (canvas «ц4»). Preview text is not localized. */

@Composable
private fun GeneratorPreview(dark: Boolean) {
    VolaV5Theme(dark = dark) {
        PasswordGeneratorSheetV5(
            visible = true,
            subtitle = "cloud.example.com",
            useLabel = "Использовать и сохранить",
            useNeedsFingerprint = true,
            onUse = {},
            onDismiss = {},
            state = remember { PasswordGeneratorState(wordlist = listOf("acid", "acorn", "acre", "acts")) },
        )
    }
}

@Preview(name = "Generator v5 · light", heightDp = 720)
@Composable
private fun PasswordGeneratorV5LightPreview() = GeneratorPreview(dark = false)

@Preview(name = "Generator v5 · dark", heightDp = 720)
@Composable
private fun PasswordGeneratorV5DarkPreview() = GeneratorPreview(dark = true)
