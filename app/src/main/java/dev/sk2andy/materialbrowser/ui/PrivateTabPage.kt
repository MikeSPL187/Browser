package dev.sk2andy.materialbrowser.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.browser.AndroidBrowserEngineKind
import dev.sk2andy.materialbrowser.data.AppearanceSettings
import dev.sk2andy.materialbrowser.data.BrowserAppearanceMode
import dev.sk2andy.materialbrowser.ui.theme.MaterialBrowserTheme
import dev.sk2andy.materialbrowser.ui.theme.VolaPreviews
import dev.sk2andy.materialbrowser.ui.theme.VolaPrivateTab
import dev.sk2andy.materialbrowser.ui.theme.VolaSpacing
import dev.sk2andy.materialbrowser.ui.theme.VolaTheme
import dev.sk2andy.materialbrowser.ui.theme.VolaTypeScale
import dev.sk2andy.materialbrowser.ui.theme.auraBrush

internal object PrivateTabTestTags {
    const val Page = "private_tab_page"
    const val CloseAll = "private_tab_close_all"
}

/** Where a private tab keeps cookies and site data, which decides what the page may promise. */
internal enum class PrivateTabStorage {
    /** GeckoView private browsing: nothing reaches the disk. */
    Memory,

    /** System WebView with profiles: a profile of its own, deleted after the last private tab. */
    SeparateProfile,

    /** System WebView without profiles: cookies are shared with regular tabs. */
    SharedWithRegularTabs,
}

/** One line of the private tab page. */
internal data class PrivateTabFact(
    @param:DrawableRes val icon: Int,
    @param:StringRes val title: Int,
    @param:StringRes val caption: Int,
    val warning: Boolean = false,
)

/** What the private new tab may honestly say for each engine. */
internal object PrivateTabRules {
    fun storage(engine: AndroidBrowserEngineKind, profilesSupported: Boolean): PrivateTabStorage = when {
        engine == AndroidBrowserEngineKind.GeckoView -> PrivateTabStorage.Memory
        profilesSupported -> PrivateTabStorage.SeparateProfile
        else -> PrivateTabStorage.SharedWithRegularTabs
    }

    fun facts(storage: PrivateTabStorage): List<PrivateTabFact> = listOf(
        PrivateTabFact(R.drawable.ic_history, R.string.private_tab_fact_history, R.string.private_tab_fact_history_caption),
        when (storage) {
            PrivateTabStorage.Memory -> PrivateTabFact(
                R.drawable.ic_symbol_cookie,
                R.string.private_tab_fact_memory,
                R.string.private_tab_fact_memory_caption,
            )
            PrivateTabStorage.SeparateProfile -> PrivateTabFact(
                R.drawable.ic_symbol_cookie,
                R.string.private_tab_fact_profile,
                R.string.private_tab_fact_profile_caption,
            )
            PrivateTabStorage.SharedWithRegularTabs -> PrivateTabFact(
                R.drawable.ic_symbol_cookie,
                R.string.private_tab_fact_shared,
                R.string.private_tab_fact_shared_caption,
                warning = true,
            )
        },
        PrivateTabFact(R.drawable.ic_symbol_shield, R.string.private_tab_fact_protection, R.string.private_tab_fact_protection_caption),
        PrivateTabFact(R.drawable.ic_reader_download, R.string.private_tab_fact_downloads, R.string.private_tab_fact_downloads_caption),
    )
}

/**
 * The private new tab as the PrivateTab board draws it: the mask gem, «Private tab», what is
 * and is not kept, and «Close all private tabs». The facts follow the engine, so the page never
 * promises more than the engine does.
 */
@Composable
internal fun PrivateTabPage(
    storage: PrivateTabStorage,
    privateTabCount: Int,
    enabled: Boolean,
    onCloseAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag(PrivateTabTestTags.Page),
        verticalArrangement = Arrangement.spacedBy(VolaSpacing.x5),
    ) {
        Box(
            modifier = Modifier
                .size(VolaPrivateTab.gemSize)
                .shadow(VolaPrivateTab.gemGlow, VolaPrivateTab.gemShape, spotColor = colors.primary)
                .clip(VolaPrivateTab.gemShape)
                // A deep private accent with a light mask, as on the board, in the always-dark scheme.
                .background(colors.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_incognito_filled),
                contentDescription = null,
                modifier = Modifier.size(VolaPrivateTab.gemGlyphSize),
                tint = colors.onPrimaryContainer,
            )
        }
        Text(
            text = stringResource(R.string.private_tab_title),
            modifier = Modifier.semantics { heading() },
            style = VolaTypeScale.display,
            color = colors.onSurface,
        )
        Text(
            text = stringResource(R.string.private_tab_body),
            style = MaterialTheme.typography.bodyLarge,
            color = colors.onSurfaceVariant,
        )
        Column(verticalArrangement = Arrangement.spacedBy(VolaPrivateTab.factGap)) {
            PrivateTabRules.facts(storage).forEach { fact -> PrivateTabFactRow(fact) }
        }
        if (privateTabCount > 0) {
            FilledTonalButton(
                onClick = onCloseAll,
                enabled = enabled,
                modifier = Modifier.testTag(PrivateTabTestTags.CloseAll),
                shape = CircleShape,
            ) {
                Text(
                    text = pluralStringResource(R.plurals.private_tab_close_all, privateTabCount, privateTabCount),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

@Composable
private fun PrivateTabFactRow(fact: PrivateTabFact) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.spacedBy(VolaPrivateTab.factGap),
    ) {
        Box(
            modifier = Modifier
                .size(VolaPrivateTab.factIconSize)
                .clip(VolaPrivateTab.factIconShape)
                .background(if (fact.warning) colors.errorContainer else colors.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(fact.icon),
                contentDescription = null,
                tint = if (fact.warning) colors.onErrorContainer else colors.primary,
            )
        }
        Column(modifier = Modifier.padding(top = VolaSpacing.x1)) {
            Text(
                text = stringResource(fact.title),
                style = MaterialTheme.typography.titleSmall,
                color = colors.onSurface,
            )
            Text(
                text = stringResource(fact.caption),
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
            )
        }
    }
}

@VolaPreviews
@Composable
private fun PrivateTabPagePreview() {
    MaterialBrowserTheme(
        settings = AppearanceSettings(appearanceMode = BrowserAppearanceMode.System),
        privateMode = true,
    ) {
        Box(
            modifier = Modifier
                .background(VolaTheme.auraBrush)
                .padding(VolaSpacing.x6),
        ) {
            PrivateTabPage(
                storage = PrivateTabStorage.Memory,
                privateTabCount = 2,
                enabled = true,
                onCloseAll = {},
            )
        }
    }
}

@VolaPreviews
@Composable
private fun PrivateTabPageSharedCookiesPreview() {
    MaterialBrowserTheme(
        settings = AppearanceSettings(appearanceMode = BrowserAppearanceMode.System),
        privateMode = true,
    ) {
        Box(
            modifier = Modifier
                .background(VolaTheme.auraBrush)
                .padding(VolaSpacing.x6),
        ) {
            PrivateTabPage(
                storage = PrivateTabStorage.SharedWithRegularTabs,
                privateTabCount = 1,
                enabled = true,
                onCloseAll = {},
            )
        }
    }
}
