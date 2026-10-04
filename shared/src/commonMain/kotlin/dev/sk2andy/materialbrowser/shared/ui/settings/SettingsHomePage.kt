package dev.sk2andy.materialbrowser.shared.ui.settings

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import dev.sk2andy.materialbrowser.shared.ui.icons.VolaIcons
import dev.sk2andy.materialbrowser.shared.ui.theme.SettingsHomeTokens
import dev.sk2andy.materialbrowser.ui.SettingsDestination

enum class SettingsHomeIcon {
    Search,
    Sync,
    TabsAndGestures,
    Appearance,
    Browser,
    Downloads,
    Userscripts,
    FirefoxExtensions,
    SiteCapsules,
    ProtectionAndData,
    DeveloperOptions,
    AboutLegal,
}

enum class SettingsHomeLabel {
    Title,
    Back,
    SearchTitle,
    SearchSummary,
    SyncTitle,
    SyncSummary,
    TabsAndGesturesTitle,
    TabsAndGesturesSummary,
    AppearanceTitle,
    AppearanceSummary,
    BrowserTitle,
    BrowserSummary,
    DownloadsTitle,
    UserscriptsTitle,
    UserscriptsSummary,
    FirefoxExtensionsTitle,
    FirefoxExtensionsSummary,
    SiteCapsulesTitle,
    SiteCapsulesSummary,
    ProtectionAndDataTitle,
    ProtectionAndDataSummary,
    DeveloperOptionsTitle,
    DeveloperOptionsSummary,
    UnlockDeveloperOptions,
    AboutLegalTitle,
    AboutLegalSummary,
    MakeDefaultTitle,
    MakeDefaultSummary,
    MakeDefaultAction,
}

/** The cards of the settings home, top to bottom (board W-Settings). */
enum class SettingsHomeGroup {
    Protection,
    Personal,
    Tools,
    About,
}

interface SettingsHomeResources {
    @Composable
    fun text(label: SettingsHomeLabel): String
}

data class SettingsHomeItem(
    val group: SettingsHomeGroup,
    val destination: SettingsDestination?,
    val icon: SettingsHomeIcon,
    val title: SettingsHomeLabel,
    val summary: SettingsHomeLabel?,
    val isFirefoxExtensionsAction: Boolean = false,
)

object SettingsHomeRules {
    fun items(
        hasFirefoxExtensions: Boolean,
        hasDeveloperOptions: Boolean = false,
    ): List<SettingsHomeItem> = buildList {
        add(item(SettingsHomeGroup.Protection, SettingsDestination.ProtectionAndData, SettingsHomeIcon.ProtectionAndData, SettingsHomeLabel.ProtectionAndDataTitle, SettingsHomeLabel.ProtectionAndDataSummary))
        add(item(SettingsHomeGroup.Protection, SettingsDestination.Sync, SettingsHomeIcon.Sync, SettingsHomeLabel.SyncTitle, SettingsHomeLabel.SyncSummary))
        add(item(SettingsHomeGroup.Personal, SettingsDestination.Appearance, SettingsHomeIcon.Appearance, SettingsHomeLabel.AppearanceTitle, SettingsHomeLabel.AppearanceSummary))
        add(item(SettingsHomeGroup.Personal, SettingsDestination.TabsAndGestures, SettingsHomeIcon.TabsAndGestures, SettingsHomeLabel.TabsAndGesturesTitle, SettingsHomeLabel.TabsAndGesturesSummary))
        add(item(SettingsHomeGroup.Personal, SettingsDestination.Search, SettingsHomeIcon.Search, SettingsHomeLabel.SearchTitle, SettingsHomeLabel.SearchSummary))
        if (hasFirefoxExtensions) {
            add(
                SettingsHomeItem(
                    group = SettingsHomeGroup.Tools,
                    destination = null,
                    icon = SettingsHomeIcon.FirefoxExtensions,
                    title = SettingsHomeLabel.FirefoxExtensionsTitle,
                    summary = SettingsHomeLabel.FirefoxExtensionsSummary,
                    isFirefoxExtensionsAction = true,
                ),
            )
        }
        add(item(SettingsHomeGroup.Tools, SettingsDestination.Userscripts, SettingsHomeIcon.Userscripts, SettingsHomeLabel.UserscriptsTitle, SettingsHomeLabel.UserscriptsSummary))
        add(item(SettingsHomeGroup.Tools, SettingsDestination.SiteCapsules, SettingsHomeIcon.SiteCapsules, SettingsHomeLabel.SiteCapsulesTitle, SettingsHomeLabel.SiteCapsulesSummary))
        add(item(SettingsHomeGroup.Tools, SettingsDestination.Downloads, SettingsHomeIcon.Downloads, SettingsHomeLabel.DownloadsTitle, null))
        add(item(SettingsHomeGroup.Tools, SettingsDestination.Browser, SettingsHomeIcon.Browser, SettingsHomeLabel.BrowserTitle, SettingsHomeLabel.BrowserSummary))
        if (hasDeveloperOptions) {
            add(item(SettingsHomeGroup.About, SettingsDestination.DeveloperOptions, SettingsHomeIcon.DeveloperOptions, SettingsHomeLabel.DeveloperOptionsTitle, SettingsHomeLabel.DeveloperOptionsSummary))
        }
        add(item(SettingsHomeGroup.About, SettingsDestination.AboutLegal, SettingsHomeIcon.AboutLegal, SettingsHomeLabel.AboutLegalTitle, SettingsHomeLabel.AboutLegalSummary))
    }

    /** One card per group, in the order of [items]. */
    fun cards(items: List<SettingsHomeItem>): List<List<SettingsHomeItem>> =
        items.groupBy(SettingsHomeItem::group).values.toList()

    /** A live summary joins what is known, «Frame · theme auto»; nothing known keeps the static one. */
    fun joinSummary(parts: List<String?>): String? =
        parts.mapNotNull { part -> part?.trim()?.takeIf(String::isNotEmpty) }
            .takeIf(List<String>::isNotEmpty)
            ?.joinToString(SUMMARY_SEPARATOR)

    private const val SUMMARY_SEPARATOR = " · "

    /** With large text the «Make default» button moves under the text instead of squeezing it. */
    fun stacksMakeDefault(fontScale: Float): Boolean = fontScale >= LARGE_FONT_SCALE

    private const val LARGE_FONT_SCALE = 1.3f

    private fun item(
        group: SettingsHomeGroup,
        destination: SettingsDestination,
        icon: SettingsHomeIcon,
        title: SettingsHomeLabel,
        summary: SettingsHomeLabel?,
    ): SettingsHomeItem = SettingsHomeItem(
        group = group,
        destination = destination,
        icon = icon,
        title = title,
        summary = summary,
    )
}

/** A tile's colors: each category has its own tint (board W-Settings). */
data class SettingsHomeTileColors(
    val container: Color,
    val content: Color,
)

/** The app's colors for the home: shared code has no tokens of its own. */
data class SettingsHomeStyle(
    val cardColor: Color,
    val dividerColor: Color,
    val defaultCardColor: Color,
    val defaultCardContentColor: Color,
    val defaultMarkColor: Color,
    val tileColors: (SettingsHomeIcon) -> SettingsHomeTileColors,
)

@Composable
fun SettingsHomePage(
    downloadSummary: String,
    resources: SettingsHomeResources,
    style: SettingsHomeStyle,
    icon: @Composable (SettingsHomeIcon, Modifier, Color) -> Unit,
    onDestinationChanged: (SettingsDestination) -> Unit,
    onDismiss: () -> Unit,
    onOpenFirefoxExtensions: (() -> Unit)? = null,
    developerOptionsUnlocked: Boolean = false,
    onUnlockDeveloperOptions: (() -> Unit)? = null,
    isDestinationEnabled: (SettingsDestination) -> Boolean = { true },
    /** A live summary for a row, such as «Frame · theme auto»; null keeps the static one. */
    liveSummary: @Composable (SettingsHomeItem) -> String? = { null },
    /** Shown while Vola is not the default browser; null hides the card. */
    onMakeDefault: (() -> Unit)? = null,
    brandMark: @Composable (Modifier) -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
) {
    SettingsPage(
        title = resources.text(SettingsHomeLabel.Title),
        backContentDescription = resources.text(SettingsHomeLabel.Back),
        onBack = onDismiss,
        actions = actions,
    ) {
        Column(
            modifier = Modifier.padding(top = SettingsHomeTokens.topGap),
            verticalArrangement = Arrangement.spacedBy(SettingsHomeTokens.cardGap),
        ) {
            if (onMakeDefault != null) {
                SettingsHomeDefaultCard(
                    resources = resources,
                    style = style,
                    brandMark = brandMark,
                    onMakeDefault = onMakeDefault,
                )
            }
            val items = SettingsHomeRules.items(
                hasFirefoxExtensions = onOpenFirefoxExtensions != null,
                hasDeveloperOptions = developerOptionsUnlocked,
            )
            SettingsHomeRules.cards(items).forEach { card ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = SettingsHomeTokens.cardShape,
                    color = style.cardColor,
                ) {
                    Column {
                        card.forEachIndexed { index, item ->
                            if (index != 0) {
                                Box(
                                    Modifier
                                        .padding(
                                            start = SettingsHomeTokens.dividerStartInset,
                                            end = SettingsHomeTokens.dividerEndInset,
                                        )
                                        .fillMaxWidth()
                                        .height(SettingsHomeTokens.dividerThickness)
                                        .background(style.dividerColor),
                                )
                            }
                            val destination = item.destination
                            val aboutUnlock = destination == SettingsDestination.AboutLegal &&
                                !developerOptionsUnlocked &&
                                onUnlockDeveloperOptions != null
                            SettingsHomeRow(
                                title = resources.text(item.title),
                                summary = liveSummary(item) ?: item.summary
                                    ?.let { resources.text(it) } ?: downloadSummary,
                                tile = style.tileColors(item.icon),
                                icon = { modifier, tint -> icon(item.icon, modifier, tint) },
                                enabled = item.isFirefoxExtensionsAction ||
                                    destination?.let(isDestinationEnabled) == true,
                                onLongClickLabel = resources.text(SettingsHomeLabel.UnlockDeveloperOptions)
                                    .takeIf { aboutUnlock },
                                onLongClick = onUnlockDeveloperOptions.takeIf { aboutUnlock },
                                onClick = {
                                    if (item.isFirefoxExtensionsAction) {
                                        onOpenFirefoxExtensions?.invoke()
                                    } else {
                                        destination?.let(onDestinationChanged)
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SettingsHomeRow(
    title: String,
    summary: String,
    tile: SettingsHomeTileColors,
    icon: @Composable (Modifier, Color) -> Unit,
    enabled: Boolean,
    onLongClickLabel: String?,
    onLongClick: (() -> Unit)?,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = SettingsHomeTokens.rowMinHeight)
            .combinedClickable(
                enabled = enabled,
                role = Role.Button,
                onLongClickLabel = onLongClickLabel,
                onLongClick = onLongClick,
                onClick = onClick,
            )
            .graphicsLayer { alpha = if (enabled) 1f else DISABLED_ALPHA }
            .padding(SettingsHomeTokens.rowPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SettingsHomeTokens.rowGap),
    ) {
        Box(
            modifier = Modifier
                .size(SettingsHomeTokens.tileSize)
                .background(tile.container, SettingsHomeTokens.tileShape),
            contentAlignment = Alignment.Center,
        ) {
            icon(Modifier.size(SettingsHomeTokens.tileIconSize), tile.content)
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(SettingsHomeTokens.textGap),
        ) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(
                summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(
            VolaIcons.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SettingsHomeDefaultCard(
    resources: SettingsHomeResources,
    style: SettingsHomeStyle,
    brandMark: @Composable (Modifier) -> Unit,
    onMakeDefault: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = SettingsHomeTokens.cardShape,
        color = style.defaultCardColor,
        contentColor = style.defaultCardContentColor,
    ) {
        val stacked = SettingsHomeRules.stacksMakeDefault(LocalDensity.current.fontScale)
        val button: @Composable () -> Unit = {
            Button(
                onClick = onMakeDefault,
                modifier = Modifier
                    .heightIn(min = SettingsHomeTokens.defaultButtonHeight)
                    .testTag(SettingsHomeTestTags.MakeDefault),
            ) {
                Text(resources.text(SettingsHomeLabel.MakeDefaultAction))
            }
        }
        Column(
            modifier = Modifier.padding(SettingsHomeTokens.defaultCardPadding),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(SettingsHomeTokens.defaultCardGap),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(SettingsHomeTokens.defaultCardGap),
            ) {
                Box(
                    modifier = Modifier
                        .size(SettingsHomeTokens.defaultMarkSize)
                        .background(style.defaultMarkColor, SettingsHomeTokens.defaultMarkShape),
                    contentAlignment = Alignment.Center,
                ) {
                    brandMark(Modifier.size(SettingsHomeTokens.defaultMarkIconSize))
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(SettingsHomeTokens.textGap),
                ) {
                    Text(
                        resources.text(SettingsHomeLabel.MakeDefaultTitle),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        resources.text(SettingsHomeLabel.MakeDefaultSummary),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                if (!stacked) button()
            }
            if (stacked) button()
        }
    }
}

object SettingsHomeTestTags {
    const val MakeDefault = "settings_home_make_default"
}

private const val DISABLED_ALPHA = 0.38f
