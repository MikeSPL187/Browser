@file:OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalLayoutApi::class,
)

package dev.sk2andy.materialbrowser.ui

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.browser.AddressResolver
import dev.sk2andy.materialbrowser.browser.BrowserTab
import dev.sk2andy.materialbrowser.data.EssentialEntry
import dev.sk2andy.materialbrowser.shared.ui.icons.VolaIcons
import dev.sk2andy.materialbrowser.ui.theme.VolaBrand
import dev.sk2andy.materialbrowser.ui.theme.VolaElevation
import dev.sk2andy.materialbrowser.ui.theme.VolaEssentials
import dev.sk2andy.materialbrowser.ui.theme.VolaShapes
import dev.sk2andy.materialbrowser.ui.theme.VolaSpacing
import dev.sk2andy.materialbrowser.ui.theme.VolaTheme
import dev.sk2andy.materialbrowser.ui.theme.auraBrush

/**
 * The new tab as the NewTab board draws it: the workspace name and date, the workspace's
 * Essentials and «Continue» with the tabs used last. A private tab shows only the search hero
 * (its own design is Q6). [interactive] is false for the miniature in the tab overview.
 */
@Composable
internal fun NewTabPage(
    essentials: List<EssentialEntry>,
    essentialIcons: Map<String, Bitmap> = emptyMap(),
    incognito: Boolean,
    modeProgress: Float,
    revealOriginInRoot: Offset,
    onSearch: () -> Unit,
    onOpenEssential: (String) -> Unit,
    editor: NewTabEssentialsEditor? = null,
    interactive: Boolean = true,
    essentialsAlpha: () -> Float = { 1f },
    explicitSafeDrawingPadding: PaddingValues? = null,
    /** The workspace name shown above the page, as on the NewTab board. */
    title: String? = null,
    /** Recently used tabs for the Continue card, newest first. */
    recentTabs: List<BrowserTab> = emptyList(),
    recentTabFavicons: Map<String, Bitmap> = emptyMap(),
    onRecentTab: (String) -> Unit = {},
) {
    val colors = MaterialTheme.colorScheme
    val profileWallpaper = LocalProfileWallpaper.current.takeUnless { incognito }
    val boundedProgress = BlankTabModeMorphRules.bounded(modeProgress)
    val regularIconAlpha = BlankTabModeMorphRules.regularIconAlpha(boundedProgress)
    val incognitoIconAlpha = BlankTabModeMorphRules.incognitoIconAlpha(boundedProgress)
    val openSearchDescription = stringResource(R.string.cd_open_search)
    var editing by rememberSaveable { mutableStateOf(false) }
    val editable = interactive && !incognito && editor != null
    BackHandler(enabled = editing && editable) { editing = false }
    val contentEnabled = interactive
    Box(
        modifier = Modifier
            .fillMaxSize()
            .blankTabModeBackground(
                progress = boundedProgress,
                revealOriginInRoot = revealOriginInRoot,
                regularCenterColor = colors.primaryContainer,
                incognitoCenterColor = colors.inverseSurface,
                edgeColor = colors.surface,
                wallpaper = profileWallpaper,
                regularBackground = VolaTheme.auraBrush,
            ),
    ) {
        if (profileWallpaper != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .windowInsetsTopHeight(WindowInsets.statusBars)
                    .background(colors.surface.copy(alpha = 0.92f)),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .windowInsetsBottomHeight(WindowInsets.navigationBars)
                    .background(colors.surface.copy(alpha = 0.92f)),
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (explicitSafeDrawingPadding != null) {
                        Modifier.padding(explicitSafeDrawingPadding)
                    } else {
                        Modifier.safeDrawingPadding()
                    },
                ),
        ) {
            if (!incognito || boundedProgress < 1f) {
                Column(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .widthIn(max = VolaEssentials.maxContentWidth)
                        .fillMaxSize()
                        .graphicsLayer { alpha = 1f - boundedProgress }
                        .verticalScroll(rememberScrollState(), enabled = interactive)
                        .padding(horizontal = VolaSpacing.x4)
                        .padding(bottom = VolaSpacing.x12),
                    verticalArrangement = Arrangement.spacedBy(VolaSpacing.x5),
                ) {
                    if (interactive && !editing) NewTabHeader(title = title)
                    if (editing) Spacer(Modifier.height(VolaSpacing.x3))
                    NewTabEssentialsSection(
                        entries = if (incognito) emptyList() else essentials,
                        icons = essentialIcons,
                        editing = editing && editable,
                        onEditingChange = { editing = it },
                        enabled = contentEnabled && !incognito,
                        onOpen = { entry -> onOpenEssential(entry.url) },
                        editor = editor.takeIf { editable },
                        modifier = Modifier.graphicsLayer {
                            alpha = essentialsAlpha().coerceIn(0f, 1f)
                        },
                    )
                    if (!incognito && interactive && !editing && recentTabs.isNotEmpty()) {
                        NewTabContinueCard(
                            tabs = recentTabs,
                            favicons = recentTabFavicons,
                            enabled = contentEnabled,
                            onTab = onRecentTab,
                        )
                    }
                }
            }
            if (incognito || boundedProgress > 0f) {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .graphicsLayer { alpha = if (incognito) 1f else boundedProgress }
                        .fillMaxWidth(0.82f)
                        .heightIn(max = 664.dp) // token-exempt: Candy search hero of the private tab, redesigned in Q6
                        .padding(vertical = BlankTabModeMorphRules.HERO_SHADOW_CLEARANCE_DP.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Surface(
                        onClick = onSearch,
                        enabled = contentEnabled,
                        modifier = Modifier
                            .semantics {
                                contentDescription = openSearchDescription
                            },
                        shape = RoundedCornerShape(
                            BlankTabModeMorphRules.heroCornerRadiusDp(boundedProgress).dp,
                        ),
                        color = lerp(VolaBrand.Ink, colors.inverseSurface, boundedProgress),
                        shadowElevation = BlankTabModeMorphRules.HERO_SHADOW_ELEVATION_DP.dp,
                    ) {
                        Box(
                            modifier = Modifier.size(96.dp), // token-exempt: Candy search hero of the private tab, redesigned in Q6
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_launcher_foreground_art),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(68.dp) // token-exempt: Candy search hero of the private tab, redesigned in Q6
                                    .graphicsLayer {
                                        alpha = regularIconAlpha
                                        scaleX = BlankTabModeMorphRules.iconScale(regularIconAlpha)
                                        scaleY = scaleX
                                    },
                                tint = Color.Unspecified,
                            )
                            Icon(
                                painter = painterResource(R.drawable.ic_incognito_filled),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(48.dp) // token-exempt: Candy search hero of the private tab, redesigned in Q6
                                    .graphicsLayer {
                                        alpha = incognitoIconAlpha
                                        scaleX = BlankTabModeMorphRules.iconScale(incognitoIconAlpha)
                                        scaleY = scaleX
                                    },
                                tint = colors.inverseOnSurface,
                            )
                        }
                    }
                }
            }
        }
    }
}

internal const val NEW_TAB_RECENT_TAB_COUNT = 3

/** «Continue»: the tabs used last in this workspace, as on the NewTab board. */
@Composable
private fun NewTabContinueCard(
    tabs: List<BrowserTab>,
    favicons: Map<String, Bitmap>,
    enabled: Boolean,
    onTab: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(VolaSpacing.x2)) {
        Text(
            text = stringResource(R.string.new_tab_continue),
            modifier = Modifier.padding(horizontal = VolaSpacing.x1),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Surface(
            shape = VolaShapes.card,
            color = VolaTheme.extendedColors.card,
            shadowElevation = VolaElevation.level1,
        ) {
            Column {
                tabs.forEach { tab ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = enabled) { onTab(tab.id) }
                            .padding(horizontal = VolaSpacing.x4, vertical = VolaSpacing.x3),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(VolaSpacing.x3),
                    ) {
                        val favicon = favicons[tab.id]
                        if (favicon != null) {
                            Image(
                                bitmap = favicon.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(VolaSpacing.x6)
                                    .clip(VolaShapes.material.extraSmall),
                            )
                        } else {
                            Icon(
                                imageVector = VolaIcons.Tab,
                                contentDescription = null,
                                modifier = Modifier.size(VolaSpacing.x6),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = displayTabTitle(tab),
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = AddressResolver.displayText(tab.url),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Workspace name and today's date, as on the NewTab board. */
@Composable
private fun NewTabHeader(title: String?, modifier: Modifier = Modifier) {
    val locale = LocalConfiguration.current.locales[0]
    val date = remember(locale) {
        val pattern = android.text.format.DateFormat.getBestDateTimePattern(locale, "EEEEdMMMM")
        java.time.LocalDate.now()
            .format(java.time.format.DateTimeFormatter.ofPattern(pattern, locale))
            .replaceFirstChar { it.titlecase(locale) }
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = VolaSpacing.x3),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(VolaSpacing.x3),
    ) {
        Text(
            text = title.orEmpty(),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
        )
        Text(
            text = date,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
