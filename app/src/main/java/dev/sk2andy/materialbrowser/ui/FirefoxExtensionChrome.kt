package dev.sk2andy.materialbrowser.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.browser.BrowserController
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Sizes
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Spacing
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Theme
import dev.sk2andy.materialbrowser.ui.v5.V5PageBar
import dev.sk2andy.materialbrowser.ui.v5.V5SheetHeader
import dev.sk2andy.materialbrowser.ui.v5.V5WindowSheet

internal object FirefoxExtensionChromeTestTags {
    const val Actions = "firefox_extension_actions"
    const val OptionsTopBar = "firefox_extension_options_top_bar"
    const val SectionTitle = "firefox_extension_section_title"
    const val Popup = "firefox_extension_popup"

    fun action(saveableId: String): String = "firefox_extension_action_$saveableId"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FirefoxExtensionOptionsTopBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Under v5 the options page is a page of Vola's own: the v5 bar over it.
    V5PromptTheme {
        V5PageBar(
            title = title,
            onBack = onBack,
            modifier = modifier
                .background(V5Theme.colors.background)
                .statusBarsPadding()
                .testTag(FirefoxExtensionChromeTestTags.OptionsTopBar),
        )
    }
}

/** Firefox popup hosted by Candy chrome; extension content never owns browser controls. */
@Composable
internal fun FirefoxExtensionChrome(controller: BrowserController) {
    FirefoxExtensionPopupV5(controller)
}

/**
 * The popup under v5: a v5 sheet titled with the extension's name (or «Firefox extensions») and
 * «Close», the extension's own page under the header. A swipe down or «Close» dismisses it, as the v4 dialog's back does.
 */
@Composable
private fun FirefoxExtensionPopupV5(controller: BrowserController) {
    val popupView = controller.firefoxExtensionPopupView
    // The extension's own name when its action has one; the identity is set with the view.
    val extensionId = controller.firefoxExtensionPopupIdentity?.extensionId
    val title = controller.firefoxExtensionActions
        .firstOrNull { it.key.extensionId == extensionId }
        ?.title
        ?.takeIf(String::isNotBlank)
        ?: stringResource(R.string.gecko_extensions_title)
    V5PromptTheme {
        V5WindowSheet(
            visible = popupView != null,
            onDismiss = controller::dismissFirefoxExtensionPopup,
            title = title,
        ) {
            V5SheetHeader(title = title, onClose = controller::dismissFirefoxExtensionPopup)
            // A new popup is a new view: the host is made again for it.
            popupView?.let { view ->
                key(view) {
                    AndroidView(
                        factory = { view },
                        // On a short screen the sheet's own limit wins over the popup's maximum.
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .fillMaxWidth()
                            .heightIn(min = V5Sizes.extensionPopupMin, max = V5Sizes.extensionPopupMax)
                            .padding(top = V5Spacing.space2)
                            .testTag(FirefoxExtensionChromeTestTags.Popup),
                    )
                }
            }
        }
    }
}

