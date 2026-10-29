package dev.sk2andy.materialbrowser.ui

import androidx.compose.ui.graphics.luminance
import dev.sk2andy.materialbrowser.shared.ui.icons.VolaIcons
import dev.sk2andy.materialbrowser.ui.theme.v5.VolaV5Theme
import dev.sk2andy.materialbrowser.ui.v5.V5StateAction
import dev.sk2andy.materialbrowser.ui.v5.V5StatePage
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.browser.BrowserController
import dev.sk2andy.materialbrowser.browser.BrowserTab
import dev.sk2andy.materialbrowser.browser.DEFAULT_PROFILE_EMOJI
import dev.sk2andy.materialbrowser.browser.PrivateTabsLockRules
import dev.sk2andy.materialbrowser.data.AppearanceSettings
import dev.sk2andy.materialbrowser.data.BrowserAppearanceMode
import dev.sk2andy.materialbrowser.ui.theme.MaterialBrowserTheme
import dev.sk2andy.materialbrowser.ui.theme.VolaPreviews
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
    available = isProfileProtectionSupported || privateTabsLock.canTurnOffWithoutBiometric,
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
            onNewProfile = {
                controller.createProfile(emoji = DEFAULT_PROFILE_EMOJI)
                Unit
            }.takeIf { !controller.isProfileProtectionSupported && !controller.canLeaveLockedProfile },
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
    // V28, B13: under v5 on a phone the one v5 lock screen, in the private palette.
    VolaV5Theme(dark = MaterialTheme.colorScheme.background.luminance() < LOCK_DARK_LUMINANCE, privateBrowsing = true) {
        V5StatePage(
            icon = VolaIcons.Fingerprint,
            title = stringResource(R.string.private_tabs_locked_title),
            message = if (unlockAvailable) {
                pluralStringResource(R.plurals.private_tabs_locked_message, privateTabCount, privateTabCount)
            } else {
                stringResource(R.string.profile_protection_unavailable)
            },
            primary = V5StateAction(
                label = stringResource(R.string.private_tabs_unlock_action),
                onClick = onUnlock,
                icon = VolaIcons.Fingerprint,
                enabled = unlockAvailable,
                testTag = PrivateTabsLockTestTags.Unlock,
            ),
            secondary = listOf(
                V5StateAction(
                    stringResource(R.string.private_tabs_locked_regular_tabs),
                    onRegularTabs,
                    testTag = PrivateTabsLockTestTags.RegularTabs,
                ),
                V5StateAction(
                    pluralStringResource(R.plurals.private_tab_close_all, privateTabCount, privateTabCount),
                    onCloseAll,
                    testTag = PrivateTabsLockTestTags.CloseAll,
                    destructive = true,
                ),
            ),
            modifier = Modifier.testTag(PrivateTabsLockTestTags.LockedScreen),
        )
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

/** Below this background luminance the shell is dark. */
private const val LOCK_DARK_LUMINANCE = 0.5f
