package dev.sk2andy.materialbrowser.ui

import android.content.res.Resources
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext

/**
 * Resources for strings formatted outside composition, such as lambdas that take runtime
 * arguments. Reading LocalConfiguration makes the caller recompose when the locale or another
 * configuration value changes, the way LocalResources does in newer Compose releases. Prefer
 * stringResource() for strings known during composition, and move callers to
 * LocalResources.current once the Compose BOM provides it.
 */
@Composable
@ReadOnlyComposable
internal fun currentResources(): Resources {
    LocalConfiguration.current
    return LocalContext.current.resources
}
