package dev.sk2andy.materialbrowser.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.browser.AddressResolver
import dev.sk2andy.materialbrowser.browser.BrowserBackdropBlurRegion
import dev.sk2andy.materialbrowser.browser.BrowserProfile
import dev.sk2andy.materialbrowser.browser.ExternalLinkPreviewState
import dev.sk2andy.materialbrowser.shared.ui.icons.VolaIcons
import dev.sk2andy.materialbrowser.ui.theme.v5.LocalV5ProfileAccent
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Sizes
import dev.sk2andy.materialbrowser.ui.theme.v5.V5SiteTints
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Spacing
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Theme
import dev.sk2andy.materialbrowser.ui.theme.v5.VolaV5Theme
import dev.sk2andy.materialbrowser.ui.v5.V5ListDefaults
import dev.sk2andy.materialbrowser.ui.v5.V5ListGroup
import dev.sk2andy.materialbrowser.ui.v5.V5ListRow
import dev.sk2andy.materialbrowser.ui.v5.V5ProfileSheet
import dev.sk2andy.materialbrowser.ui.v5.V5RowIcon
import dev.sk2andy.materialbrowser.ui.v5.V5RowTrailing
import dev.sk2andy.materialbrowser.ui.v5.V5SafeSides
import dev.sk2andy.materialbrowser.ui.v5.V5SheetHeader
import dev.sk2andy.materialbrowser.ui.v5.V5WindowSheet
import dev.sk2andy.materialbrowser.ui.v5.v5BarDomain

/**
 * A link from another app under v5 (V28, the phone wholly on v5): the v5 bar at the bottom with
 * «Close», the site's domain in the capsule, «Open in Vola» filled in the target profile's accent,
 * the profile (a v5 profiles sheet when there are several) and «More» (share, copy link, find in
 * page, desktop site as a v5 sheet). The bar is opaque, so it asks for no blur behind it. The test
 * tags and callbacks are the current bar's; v4 is unchanged.
 */
@Composable
internal fun ExternalLinkPreviewBarV5(
    state: ExternalLinkPreviewState,
    profiles: List<BrowserProfile>,
    isDesktopView: Boolean,
    onDismissPreview: () -> Unit,
    onOpenInCandy: () -> Unit,
    onSelectProfile: (String) -> Unit,
    onShare: () -> Unit,
    onCopyLink: () -> Unit,
    onFindInPage: () -> Unit,
    onDesktopViewChange: (Boolean) -> Unit,
    onBackdropBlurRegionChanged: (BrowserBackdropBlurRegion?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var profilesOpen by remember(state.sessionId) { mutableStateOf(false) }
    var moreOpen by remember(state.sessionId) { mutableStateOf(false) }
    val clearBlur by rememberUpdatedState(onBackdropBlurRegionChanged)
    LaunchedEffect(Unit) { clearBlur(null) }
    val untitled = stringResource(R.string.v5_profile_untitled)
    val items = V5ProfileRules.items(
        profiles = profiles,
        tabs = emptyList(),
        activeProfileId = state.targetProfileId,
        lockedProfileIds = emptySet(),
        untitled = untitled,
        defaultName = stringResource(R.string.workspace_default_name),
    ).map { item -> item.copy(tabCount = null) }
    val target = items.firstOrNull { it.active } ?: items.firstOrNull()
    val targetName = target?.name.orEmpty()
    val dark = MaterialTheme.colorScheme.background.luminance() < PREVIEW_DARK_SHELL_LUMINANCE
    // The whole bar speaks for the profile the link would open in: its accent fills «Open in Vola».
    VolaV5Theme(dark = dark, accent = target?.accent ?: LocalV5ProfileAccent.current) {
        val colors = V5Theme.colors
        val bar = V5SiteTints.of(null, colors.chrome, colors.label)
        Box(modifier.fillMaxSize().imePadding(), contentAlignment = Alignment.BottomCenter) {
            Column(Modifier.fillMaxWidth().background(bar.container).testTag(ExternalLinkPreviewTestTags.Bar)) {
                Box(Modifier.fillMaxWidth().height(V5Spacing.hairline).background(colors.edge))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(V5SafeSides)
                        .heightIn(min = V5Sizes.bar)
                        .padding(horizontal = V5Spacing.space3, vertical = V5Spacing.space2),
                    horizontalArrangement = Arrangement.spacedBy(V5Spacing.space2),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PreviewRoundButton(bar.field) {
                        IconButton(onClick = onDismissPreview, modifier = Modifier.testTag(ExternalLinkPreviewTestTags.Back)) {
                            Icon(
                                VolaIcons.Close,
                                contentDescription = stringResource(R.string.external_link_preview_close_description),
                                tint = bar.onTint,
                            )
                        }
                    }
                    Box(
                        Modifier
                            .weight(1f)
                            .heightIn(min = V5Sizes.barCapsule)
                            .clip(V5Theme.shapes.full)
                            .background(bar.field)
                            .padding(horizontal = V5Spacing.space4),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = v5BarDomain(state.currentUrl) ?: AddressResolver.displayText(state.currentUrl),
                            style = V5Theme.type.headline,
                            color = bar.onTint,
                            maxLines = 1,
                            overflow = TextOverflow.MiddleEllipsis,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag(ExternalLinkPreviewTestTags.Host)
                                .semantics { contentDescription = state.currentUrl },
                        )
                    }
                    PreviewRoundButton(colors.accentFill) {
                        IconButton(onClick = onOpenInCandy, modifier = Modifier.testTag(ExternalLinkPreviewTestTags.Open)) {
                            Icon(
                                VolaIcons.Add,
                                contentDescription = stringResource(R.string.external_link_preview_open_description, targetName),
                                tint = colors.onAccentFill,
                            )
                        }
                    }
                    if (items.size > 1 && target != null) {
                        val profileDescription = stringResource(R.string.external_link_preview_profile_description, targetName)
                        PreviewRoundButton(bar.field) {
                            IconButton(
                                onClick = { profilesOpen = true },
                                modifier = Modifier
                                    .testTag(ExternalLinkPreviewTestTags.Profile)
                                    .semantics { contentDescription = profileDescription },
                            ) {
                                Icon(target.icon, contentDescription = null, tint = bar.onTint)
                            }
                        }
                    }
                    PreviewRoundButton(bar.field) {
                        IconButton(onClick = { moreOpen = true }, modifier = Modifier.testTag(ExternalLinkPreviewTestTags.Overflow)) {
                            Icon(VolaIcons.MoreHoriz, contentDescription = stringResource(R.string.cd_more_options), tint = bar.onTint)
                        }
                    }
                }
                // The bar runs on under the system navigation (edge to edge), as the v5 browser bar does.
                Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
            }
        }
        if (items.size > 1) {
            V5ProfileSheet(
                visible = profilesOpen,
                profiles = items,
                onSelect = { id ->
                    profilesOpen = false
                    onSelectProfile(id)
                },
                onDismiss = { profilesOpen = false },
            )
        }
        PreviewMoreSheet(
            visible = moreOpen,
            isDesktopView = isDesktopView,
            onDismiss = { moreOpen = false },
            onShare = onShare,
            onCopyLink = onCopyLink,
            onFindInPage = onFindInPage,
            onDesktopViewChange = onDesktopViewChange,
        )
    }
}

/** «More»: what the v4 bar's menu offers, as rows of a v5 sheet; each closes it. */
@Composable
private fun PreviewMoreSheet(
    visible: Boolean,
    isDesktopView: Boolean,
    onDismiss: () -> Unit,
    onShare: () -> Unit,
    onCopyLink: () -> Unit,
    onFindInPage: () -> Unit,
    onDesktopViewChange: (Boolean) -> Unit,
) {
    val title = stringResource(R.string.cd_more_options)
    V5WindowSheet(visible = visible, onDismiss = onDismiss, title = title) {
        V5SheetHeader(title = title, onClose = onDismiss)
        V5ListGroup(
            dividerInset = V5ListDefaults.dividerInsetWithIcon,
            modifier = Modifier.padding(top = V5Spacing.space2, bottom = V5Spacing.space4),
        ) {
            V5ListRow(
                title = stringResource(R.string.action_share),
                icon = V5RowIcon(VolaIcons.Share),
                onClick = {
                    onDismiss()
                    onShare()
                },
                modifier = Modifier.testTag(ExternalLinkPreviewTestTags.Share),
            )
            V5ListRow(
                title = stringResource(R.string.external_link_preview_copy_link),
                icon = V5RowIcon(VolaIcons.ContentCopy),
                onClick = {
                    onDismiss()
                    onCopyLink()
                },
                modifier = Modifier.testTag(ExternalLinkPreviewTestTags.CopyLink),
            )
            V5ListRow(
                title = stringResource(R.string.action_find_in_page),
                icon = V5RowIcon(VolaIcons.Search),
                onClick = {
                    onDismiss()
                    onFindInPage()
                },
                modifier = Modifier.testTag(ExternalLinkPreviewTestTags.Find),
            )
            V5ListRow(
                title = stringResource(R.string.action_desktop_view),
                icon = V5RowIcon(VolaIcons.DesktopWindows),
                trailing = V5RowTrailing.Toggle(isDesktopView, onDesktopViewChange),
                modifier = Modifier.testTag(ExternalLinkPreviewTestTags.Desktop),
            )
        }
    }
}

@Composable
private fun PreviewRoundButton(container: Color, content: @Composable () -> Unit) {
    Box(
        Modifier
            .size(V5Sizes.barCapsule)
            .clip(V5Theme.shapes.full)
            .background(container),
        contentAlignment = Alignment.Center,
    ) { content() }
}

private const val PREVIEW_DARK_SHELL_LUMINANCE = 0.5f
