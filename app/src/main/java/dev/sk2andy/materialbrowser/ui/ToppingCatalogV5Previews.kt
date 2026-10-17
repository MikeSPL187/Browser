package dev.sk2andy.materialbrowser.ui

import androidx.compose.runtime.Composable
import dev.sk2andy.materialbrowser.ui.theme.MaterialBrowserTheme
import dev.sk2andy.materialbrowser.ui.theme.VolaPreviews

/* The Toppings catalog under ui.v5. Preview-only sample names and texts are not localized. */

private val previewToppings = listOf(
    ToppingCatalogItem(
        id = "wide-reader",
        name = "Wide reader",
        description = "Lets long articles use the whole width of the screen.",
        author = "Vola community",
        license = "MPL-2.0",
        version = "1.2.0",
        scopes = listOf("news.example", "*.wiki.example"),
        installed = true,
        enabled = true,
        updateAvailable = true,
    ),
    ToppingCatalogItem(
        id = "quiet-comments",
        name = "Quiet comments",
        description = "Folds comment threads until you open them.",
        author = "Anna",
        license = "MIT",
        version = "0.4.1",
        scopes = emptyList(),
        installed = false,
        enabled = false,
        updateAvailable = false,
    ),
)

@Composable
private fun CatalogPreview(state: ToppingCatalogUiState) {
    MaterialBrowserTheme {
        ToppingCatalogV5(state = state, onToggle = { _, _ -> }, onUpdate = {}, onRetry = {}, onDismiss = {})
    }
}

/** Each Topping a group: its switch, «Update» when one is out, and what it is in the footer. */
@VolaPreviews
@Composable
private fun ToppingCatalogV5Preview() {
    CatalogPreview(ToppingCatalogUiState.Content(previewToppings))
}

/** A saved catalog, said above the list. */
@VolaPreviews
@Composable
private fun ToppingCatalogCachedV5Preview() {
    CatalogPreview(ToppingCatalogUiState.Cached(previewToppings.take(1)))
}

/** Nothing published yet. */
@VolaPreviews
@Composable
private fun ToppingCatalogEmptyV5Preview() {
    CatalogPreview(ToppingCatalogUiState.Content(emptyList()))
}

/** Loading. */
@VolaPreviews
@Composable
private fun ToppingCatalogLoadingV5Preview() {
    CatalogPreview(ToppingCatalogUiState.Loading)
}

/** The catalog could not be loaded: «Try again». */
@VolaPreviews
@Composable
private fun ToppingCatalogErrorV5Preview() {
    CatalogPreview(ToppingCatalogUiState.Error())
}
