package dev.sk2andy.materialbrowser.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.shared.ui.icons.VolaIcons
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Sizes
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Spacing
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Theme
import dev.sk2andy.materialbrowser.ui.theme.v5.VolaV5Theme
import dev.sk2andy.materialbrowser.ui.v5.V5ListDefaults
import dev.sk2andy.materialbrowser.ui.v5.V5ListGroup
import dev.sk2andy.materialbrowser.ui.v5.V5ListRow
import dev.sk2andy.materialbrowser.ui.v5.V5PageBar
import dev.sk2andy.materialbrowser.ui.v5.V5RowTrailing
import dev.sk2andy.materialbrowser.ui.v5.V5StateAction
import dev.sk2andy.materialbrowser.ui.v5.V5StateGlyph
import dev.sk2andy.materialbrowser.ui.v5.V5StatePrimaryAction

/**
 * The Toppings catalog under ui.v5 (V28, the phone wholly on v5), laid out as the v5 Toppings page
 * it opens from: each Topping is a group, its switch row on top (installs and enables it, or turns
 * it off), «Update» under it when one is out, and what it is and where it runs in the footer.
 * Loading, an error with «Try again», a saved catalog and an empty one are states of the same page.
 * The test tags and callbacks are the current screen's; v4 is unchanged.
 */
@Composable
internal fun ToppingCatalogV5(
    state: ToppingCatalogUiState,
    onToggle: (id: String, enabled: Boolean) -> Unit,
    onUpdate: (id: String) -> Unit,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dark = MaterialTheme.colorScheme.background.luminance() < CATALOG_DARK_SHELL_LUMINANCE
    VolaV5Theme(dark = dark) {
        val colors = V5Theme.colors
        Column(
            modifier
                .fillMaxSize()
                .background(colors.background)
                .statusBarsPadding()
                .navigationBarsPadding()
                .testTag(ToppingCatalogTestTags.Screen),
        ) {
            V5PageBar(title = stringResource(R.string.topping_catalog_title), onBack = onDismiss)
            when (state) {
                ToppingCatalogUiState.Loading -> CatalogLoading()
                is ToppingCatalogUiState.Error -> CatalogError(state.message, onRetry)
                is ToppingCatalogUiState.Cached -> CatalogList(state.items, cached = true, onToggle, onUpdate)
                is ToppingCatalogUiState.Content -> CatalogList(state.items, cached = false, onToggle, onUpdate)
            }
        }
    }
}

@Composable
private fun CatalogLoading() {
    val colors = V5Theme.colors
    Box(Modifier.fillMaxSize().testTag(ToppingCatalogTestTags.Loading), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(V5Spacing.space3),
        ) {
            CircularProgressIndicator(
                color = colors.accent,
                strokeWidth = V5Sizes.barProgress,
                trackColor = colors.fill,
                modifier = Modifier.size(V5Sizes.iconSmall),
            )
            Text(stringResource(R.string.topping_catalog_loading), style = V5Theme.type.callout, color = colors.labelSecondary)
        }
    }
}

@Composable
private fun CatalogError(message: String?, onRetry: () -> Unit) {
    val colors = V5Theme.colors
    Box(Modifier.fillMaxSize().testTag(ToppingCatalogTestTags.Error), contentAlignment = Alignment.Center) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = V5Spacing.space6, vertical = V5Spacing.space4),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(V5Spacing.space3),
        ) {
            V5StateGlyph(VolaIcons.WifiOff, Modifier.padding(bottom = V5Spacing.space3))
            Text(
                stringResource(R.string.topping_catalog_error_title),
                style = V5Theme.type.title,
                color = colors.label,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                message ?: stringResource(R.string.topping_catalog_error_body),
                style = V5Theme.type.callout,
                color = colors.labelSecondary,
                textAlign = TextAlign.Center,
            )
            V5StatePrimaryAction(
                V5StateAction(
                    label = stringResource(R.string.action_retry),
                    onClick = onRetry,
                    icon = VolaIcons.Refresh,
                    testTag = ToppingCatalogTestTags.Retry,
                ),
                Modifier.padding(top = V5Spacing.space4),
            )
        }
    }
}

@Composable
private fun CatalogList(
    items: List<ToppingCatalogItem>,
    cached: Boolean,
    onToggle: (id: String, enabled: Boolean) -> Unit,
    onUpdate: (id: String) -> Unit,
) {
    val colors = V5Theme.colors
    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(top = V5Spacing.space4, bottom = V5Spacing.space8)
            .testTag(ToppingCatalogTestTags.List),
        verticalArrangement = Arrangement.spacedBy(V5Spacing.space6),
    ) {
        Text(
            stringResource(if (cached) R.string.topping_catalog_cached_notice else R.string.topping_catalog_subtitle),
            style = V5Theme.type.footnote,
            color = colors.labelSecondary,
            modifier = Modifier
                .padding(horizontal = V5Spacing.marginPhone + V5Spacing.space4)
                .then(if (cached) Modifier.testTag(ToppingCatalogTestTags.CachedNotice) else Modifier),
        )
        if (items.isEmpty()) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = V5Spacing.marginPhone + V5Spacing.space4, vertical = V5Spacing.space4)
                    .testTag(ToppingCatalogTestTags.Empty),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(V5Spacing.space2),
            ) {
                Text(
                    stringResource(R.string.topping_catalog_empty_title),
                    style = V5Theme.type.headline,
                    color = colors.label,
                    textAlign = TextAlign.Center,
                )
                Text(
                    stringResource(R.string.topping_catalog_empty_body),
                    style = V5Theme.type.callout,
                    color = colors.labelSecondary,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            items.forEach { topping ->
                CatalogTopping(topping, onToggle = { onToggle(topping.id, it) }, onUpdate = { onUpdate(topping.id) })
            }
        }
    }
}

@Composable
private fun CatalogTopping(topping: ToppingCatalogItem, onToggle: (Boolean) -> Unit, onUpdate: () -> Unit) {
    val colors = V5Theme.colors
    val toggleDescription = toppingToggleDescription(topping)
    val details = listOf(
        topping.description,
        stringResource(R.string.topping_catalog_scope, toppingScopeSummary(topping.scopes)),
        stringResource(R.string.topping_catalog_author, topping.author),
        stringResource(R.string.topping_catalog_version_license, topping.version, topping.license),
    ).filter(String::isNotBlank).joinToString("\n")
    V5ListGroup(
        footer = details,
        dividerInset = V5ListDefaults.dividerInsetText,
        modifier = Modifier.testTag(ToppingCatalogTestTags.topping(topping.id)),
    ) {
        V5ListRow(
            title = topping.name,
            subtitle = toppingStatus(topping),
            trailing = V5RowTrailing.Toggle(topping.enabled, onToggle),
            enabled = !topping.busy,
            modifier = Modifier
                .testTag(ToppingCatalogTestTags.toggle(topping.id))
                .semantics { contentDescription = toggleDescription },
        )
        if (topping.installed && topping.updateAvailable) {
            V5ListRow(
                title = stringResource(R.string.topping_catalog_update),
                titleColor = colors.accent,
                enabled = !topping.busy,
                onClick = onUpdate,
                modifier = Modifier.testTag(ToppingCatalogTestTags.update(topping.id)),
            )
        }
    }
}

/** Below it the shell is dark: the settings' Material theme says which v5 theme to draw in. */
private const val CATALOG_DARK_SHELL_LUMINANCE = 0.5f
