package dev.sk2andy.materialbrowser.ui

import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.sk2andy.materialbrowser.R
internal data class ToppingCatalogItem(
    val id: String,
    val name: String,
    val description: String,
    val author: String,
    val license: String,
    val version: String,
    val scopes: List<String>,
    val installed: Boolean,
    val enabled: Boolean,
    val updateAvailable: Boolean,
    val busy: Boolean = false,
)

internal sealed interface ToppingCatalogUiState {
    data object Loading : ToppingCatalogUiState

    data class Error(val message: String? = null) : ToppingCatalogUiState

    data class Cached(val items: List<ToppingCatalogItem>) : ToppingCatalogUiState

    data class Content(val items: List<ToppingCatalogItem>) : ToppingCatalogUiState
}

internal object ToppingCatalogTestTags {
    const val Screen = "topping_catalog_screen"
    const val Loading = "topping_catalog_loading"
    const val Error = "topping_catalog_error"
    const val Retry = "topping_catalog_retry"
    const val CachedNotice = "topping_catalog_cached_notice"
    const val List = "topping_catalog_list"
    const val Empty = "topping_catalog_empty"

    fun topping(id: String) = "topping_catalog_item_$id"
    fun toggle(id: String) = "topping_catalog_toggle_$id"
    fun update(id: String) = "topping_catalog_update_$id"
}

@Composable
internal fun ToppingCatalogScreen(
    state: ToppingCatalogUiState,
    onToggle: (id: String, enabled: Boolean) -> Unit,
    onUpdate: (id: String) -> Unit,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ToppingCatalogV5(state, onToggle, onUpdate, onRetry, onDismiss, modifier)
}

@Composable
internal fun toppingStatus(topping: ToppingCatalogItem): String = when {
    topping.busy -> stringResource(R.string.topping_catalog_working)
    !topping.installed -> stringResource(R.string.topping_catalog_not_installed)
    topping.enabled -> stringResource(R.string.topping_catalog_active)
    else -> stringResource(R.string.topping_catalog_inactive)
}

@Composable
internal fun toppingToggleDescription(topping: ToppingCatalogItem): String = when {
    !topping.installed -> stringResource(R.string.topping_catalog_install_description, topping.name)
    topping.enabled -> stringResource(R.string.topping_catalog_disable_description, topping.name)
    else -> stringResource(R.string.topping_catalog_enable_description, topping.name)
}

@Composable
internal fun toppingScopeSummary(scopes: List<String>): String =
    scopes.takeIf { it.isNotEmpty() }
        ?.joinToString(limit = 3, truncated = " …")
        ?: stringResource(R.string.topping_catalog_scope_unknown)
