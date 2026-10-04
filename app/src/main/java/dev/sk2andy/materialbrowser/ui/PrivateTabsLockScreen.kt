package dev.sk2andy.materialbrowser.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.browser.BrowserController
import dev.sk2andy.materialbrowser.browser.BrowserTab
import dev.sk2andy.materialbrowser.browser.PrivateTabsLockRules
import dev.sk2andy.materialbrowser.data.AppearanceSettings
import dev.sk2andy.materialbrowser.data.BrowserAppearanceMode
import dev.sk2andy.materialbrowser.ui.theme.MaterialBrowserTheme
import dev.sk2andy.materialbrowser.ui.theme.VolaEssentials
import dev.sk2andy.materialbrowser.ui.theme.VolaPreviews
import dev.sk2andy.materialbrowser.ui.theme.VolaSpacing
import dev.sk2andy.materialbrowser.ui.theme.VolaTheme
import dev.sk2andy.materialbrowser.ui.theme.VolaTypeScale
import dev.sk2andy.materialbrowser.ui.theme.auraBrush

internal object PrivateTabsLockTestTags {
    const val LockedScreen = "private_tabs_locked_screen"
    const val Unlock = "private_tabs_locked_unlock"
    const val RegularTabs = "private_tabs_locked_regular_tabs"
    const val CloseAll = "private_tabs_locked_close_all"
}

/**
 * Whether private tabs are locked now. Everything that would show a private tab's title, address
 * or icon outside its page (overview cards, the wide tab strip, tab search) asks [hidesPrivateTab].
 */
internal val LocalPrivateTabsLocked = compositionLocalOf { false }

@Composable
@ReadOnlyComposable
internal fun hidesPrivateTab(tab: BrowserTab): Boolean =
    PrivateTabsLockRules.hides(LocalPrivateTabsLocked.current, tab)

/** The «Lock on exit» row, the same on the private page and in Protection and data. */
internal fun BrowserController.privateTabLockRow(): PrivateTabLock = PrivateTabLock(
    checked = privateTabsLock.enabled,
    available = isProfileProtectionSupported,
    onCheckedChange = privateTabsLock::requestEnabled,
)

/** Whatever lock covers the selected page: its workspace's, or the private tabs'. */
@Composable
internal fun BrowserLockScreens(controller: BrowserController) {
    if (controller.isActiveProfileLocked) {
        ProfileLockedOverlay(
            workspace = controller.localBrowserProfiles
                .firstOrNull { profile -> profile.id == controller.activeProfileId },
            unlockAvailable = controller.isProfileProtectionSupported,
            canSwitchProfile = controller.canLeaveLockedProfile,
            onUnlock = controller::retryActiveProfileAuthentication,
            onSwitchProfile = { controller.leaveLockedProfile() },
        )
    } else if (controller.isSelectedContentLocked) {
        val lifecycle = LocalLifecycleOwner.current.lifecycle
        PrivateTabsLockedScreen(
            privateTabCount = controller.tabs.count(BrowserTab::isIncognito),
            unlockAvailable = controller.isProfileProtectionSupported,
            onShown = {
                controller.privateTabsLock.onLockScreenShown(
                    resumed = lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED),
                )
            },
            onUnlock = controller.privateTabsLock::unlock,
            onRegularTabs = {
                PrivateTabsLockRules.regularTabToShow(controller.activeTabs)
                    ?.let { tab -> controller.selectTab(tab.id) }
                    ?: controller.createTab(isIncognito = false)
            },
            onCloseAll = { controller.closeAllPrivateTabs() },
        )
    }
}

/**
 * «Private tabs locked», in place of a locked private page (proposal П9). The page itself is not
 * underneath: its view is detached, so neither the screen nor TalkBack can reach it. The
 * fingerprint is asked for once by itself ([onShown]); then the button asks.
 */
@Composable
internal fun PrivateTabsLockedScreen(
    privateTabCount: Int,
    unlockAvailable: Boolean,
    onShown: () -> Unit,
    onUnlock: () -> Unit,
    onRegularTabs: () -> Unit,
    onCloseAll: () -> Unit,
) {
    LaunchedEffect(Unit) { onShown() }
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag(PrivateTabsLockTestTags.LockedScreen),
        color = colors.surface,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(VolaTheme.auraBrush)
                .safeDrawingPadding(),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = VolaEssentials.maxContentWidth)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = VolaSpacing.x6, vertical = VolaSpacing.x8),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(VolaSpacing.x5),
            ) {
                PrivateTabGem(icon = R.drawable.ic_symbol_fingerprint)
                Text(
                    text = stringResource(R.string.private_tabs_locked_title),
                    modifier = Modifier.semantics { heading() },
                    style = VolaTypeScale.display,
                    color = colors.onSurface,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = if (unlockAvailable) {
                        pluralStringResource(
                            R.plurals.private_tabs_locked_message,
                            privateTabCount,
                            privateTabCount,
                        )
                    } else {
                        stringResource(R.string.profile_protection_unavailable)
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Button(
                    onClick = onUnlock,
                    enabled = unlockAvailable,
                    modifier = Modifier.testTag(PrivateTabsLockTestTags.Unlock),
                    shape = CircleShape,
                    contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_symbol_fingerprint),
                        contentDescription = null,
                        modifier = Modifier
                            .padding(end = ButtonDefaults.IconSpacing)
                            .size(ButtonDefaults.IconSize),
                    )
                    Text(stringResource(R.string.private_tabs_unlock_action))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    TextButton(
                        onClick = onRegularTabs,
                        modifier = Modifier.testTag(PrivateTabsLockTestTags.RegularTabs),
                    ) {
                        Text(stringResource(R.string.private_tabs_locked_regular_tabs))
                    }
                    TextButton(
                        onClick = onCloseAll,
                        modifier = Modifier.testTag(PrivateTabsLockTestTags.CloseAll),
                        colors = ButtonDefaults.textButtonColors(contentColor = colors.error),
                    ) {
                        Text(
                            pluralStringResource(
                                R.plurals.private_tab_close_all,
                                privateTabCount,
                                privateTabCount,
                            ),
                        )
                    }
                }
            }
        }
    }
}

@VolaPreviews
@Composable
private fun PrivateTabsLockedScreenPreview() {
    MaterialBrowserTheme(
        settings = AppearanceSettings(appearanceMode = BrowserAppearanceMode.System),
        privateMode = true,
    ) {
        PrivateTabsLockedScreen(
            privateTabCount = 3,
            unlockAvailable = true,
            onShown = {},
            onUnlock = {},
            onRegularTabs = {},
            onCloseAll = {},
        )
    }
}

@VolaPreviews
@Composable
private fun PrivateTabsLockedScreenUnavailablePreview() {
    MaterialBrowserTheme(
        settings = AppearanceSettings(appearanceMode = BrowserAppearanceMode.System),
        privateMode = true,
    ) {
        PrivateTabsLockedScreen(
            privateTabCount = 1,
            unlockAvailable = false,
            onShown = {},
            onUnlock = {},
            onRegularTabs = {},
            onCloseAll = {},
        )
    }
}
