package dev.sk2andy.materialbrowser.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.browser.gecko.GeckoExtension
import dev.sk2andy.materialbrowser.browser.engine.GeckoExtensionChromeRules
import dev.sk2andy.materialbrowser.browser.engine.GeckoExtensionManagerState
import dev.sk2andy.materialbrowser.browser.gecko.GeckoExtensionPermissionRequest
import dev.sk2andy.materialbrowser.shared.ui.icons.VolaIcons
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Spacing
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Theme
import dev.sk2andy.materialbrowser.ui.theme.v5.VolaV5Theme
import dev.sk2andy.materialbrowser.ui.v5.V5PageBar
import dev.sk2andy.materialbrowser.ui.v5.V5ListDefaults
import dev.sk2andy.materialbrowser.ui.v5.V5ListGroup
import dev.sk2andy.materialbrowser.ui.v5.V5ListRow
import dev.sk2andy.materialbrowser.ui.v5.V5NameField
import dev.sk2andy.materialbrowser.ui.v5.V5PromptAction
import dev.sk2andy.materialbrowser.ui.v5.V5PromptRole
import dev.sk2andy.materialbrowser.ui.v5.V5PromptSheet
import dev.sk2andy.materialbrowser.ui.v5.V5RowTrailing
import dev.sk2andy.materialbrowser.ui.v5.V5SheetHeader
import dev.sk2andy.materialbrowser.ui.v5.V5WindowSheet

/**
 * The Firefox extension manager under ui.v5 (V28, Б12a), laid out as Safari's Extensions settings:
 * every installed extension is a row with its on/off switch, and a tap opens its own page with
 * private tabs, its options page, update and remove. Installing by address and the permission
 * question are v5 sheets. What each action does is the current manager's; v4 is unchanged.
 */
@Composable
internal fun FirefoxExtensionManagerV5(
    state: GeckoExtensionManagerState,
    onInstall: (String) -> Unit,
    onSetEnabled: (GeckoExtension, Boolean) -> Unit,
    onSetPrivate: (GeckoExtension, Boolean) -> Unit,
    onUpdate: (GeckoExtension) -> Unit,
    onUninstall: (GeckoExtension) -> Unit,
    onOpenOptionsPage: (GeckoExtension) -> Unit,
    onPermissionDecision: (Boolean, Boolean) -> Unit,
    onDismiss: () -> Unit,
    /** An extension whose page is open at first, for previews. */
    initialOpenId: String? = null,
) {
    var openId by remember { mutableStateOf(initialOpenId) }
    var installing by remember { mutableStateOf(false) }
    // A removed extension's page has nothing left to show: the list comes back.
    val open = state.snapshot.extensions.firstOrNull { it.id == openId }
    BackHandler { if (open != null) openId = null else onDismiss() }
    val dark = MaterialTheme.colorScheme.background.luminance() < DARK_SHELL_LUMINANCE
    VolaV5Theme(dark = dark) {
        val colors = V5Theme.colors
        Column(
            Modifier
                .fillMaxSize()
                .background(colors.background)
                .statusBarsPadding()
                .navigationBarsPadding()
                .testTag(FirefoxExtensionManagerTestTags.Overlay),
        ) {
            if (open == null) {
                V5PageBar(
                    title = stringResource(R.string.gecko_extensions_title),
                    onBack = onDismiss,
                    actions = {
                        IconButton(onClick = { installing = true }, enabled = state.canManage) {
                            Icon(
                                VolaIcons.Add,
                                contentDescription = stringResource(R.string.gecko_extension_install),
                                tint = colors.accent,
                            )
                        }
                    },
                )
            } else {
                V5PageBar(title = FirefoxExtensionManagerV5Rules.name(open), onBack = { openId = null })
            }
            if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth(), color = colors.accent)
            Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(top = V5Spacing.space4, bottom = V5Spacing.space8),
                verticalArrangement = Arrangement.spacedBy(V5Spacing.space6),
            ) {
                FirefoxExtensionManagerV5Rules.notices(state).forEach { notice ->
                    Text(
                        stringResource(notice),
                        style = V5Theme.type.footnote,
                        color = colors.danger,
                        modifier = Modifier.padding(horizontal = V5Spacing.marginPhone + V5Spacing.space4),
                    )
                }
                if (open == null) {
                    ExtensionList(state, onOpen = { openId = it.id })
                } else {
                    ExtensionPage(
                        extension = open,
                        canManage = state.canManage,
                        onSetEnabled = { onSetEnabled(open, it) },
                        onSetPrivate = { onSetPrivate(open, it) },
                        onOpenOptionsPage = { onOpenOptionsPage(open) },
                        onUpdate = { onUpdate(open) },
                        onUninstall = {
                            onUninstall(open)
                            openId = null
                        },
                    )
                }
            }
        }
        InstallSheet(
            visible = installing,
            onInstall = { url ->
                installing = false
                onInstall(url)
            },
            onDismiss = { installing = false },
        )
        PermissionSheet(state.permissionRequest, onPermissionDecision)
    }
}

@Composable
private fun ExtensionList(
    state: GeckoExtensionManagerState,
    onOpen: (GeckoExtension) -> Unit,
) {
    val extensions = state.snapshot.extensions
    if (extensions.isEmpty()) {
        Text(
            stringResource(R.string.gecko_extensions_empty),
            style = V5Theme.type.body,
            color = V5Theme.colors.labelSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(horizontal = V5Spacing.marginPhone, vertical = V5Spacing.space8),
        )
        return
    }
    V5ListGroup(
        footer = stringResource(R.string.gecko_extensions_manager_summary),
        dividerInset = V5ListDefaults.dividerInsetText,
    ) {
        extensions.forEach { extension ->
            // As in Safari, the switch is on the extension's page; the row says whether it is on.
            V5ListRow(
                title = FirefoxExtensionManagerV5Rules.name(extension),
                subtitle = FirefoxExtensionManagerV5Rules.details(extension),
                value = stringResource(
                    if (extension.enabled) R.string.gecko_extension_v5_on else R.string.gecko_extension_v5_off,
                ),
                trailing = V5RowTrailing.Chevron,
                onClick = { onOpen(extension) },
            )
        }
    }
}

@Composable
private fun ExtensionPage(
    extension: GeckoExtension,
    canManage: Boolean,
    onSetEnabled: (Boolean) -> Unit,
    onSetPrivate: (Boolean) -> Unit,
    onOpenOptionsPage: () -> Unit,
    onUpdate: () -> Unit,
    onUninstall: () -> Unit,
) {
    val colors = V5Theme.colors
    val controls = FirefoxExtensionManagerV5Rules.controlsEnabled(extension, canManage)
    V5ListGroup(
        footer = if (extension.isBuiltIn) {
            stringResource(R.string.gecko_extension_v5_built_in)
        } else {
            FirefoxExtensionManagerV5Rules.details(extension)
        },
        dividerInset = V5ListDefaults.dividerInsetText,
    ) {
        V5ListRow(
            title = stringResource(R.string.gecko_extension_v5_enabled),
            trailing = V5RowTrailing.Toggle(extension.enabled, onSetEnabled),
            enabled = controls,
        )
        V5ListRow(
            title = stringResource(R.string.gecko_extension_allow_private),
            trailing = V5RowTrailing.Toggle(extension.allowedInPrivateBrowsing, onSetPrivate),
            enabled = controls,
        )
    }
    V5ListGroup(dividerInset = V5ListDefaults.dividerInsetText) {
        val options = FirefoxExtensionManagerV5Rules.optionsAvailable(extension, canManage)
        V5ListRow(
            title = stringResource(R.string.gecko_extension_v5_options),
            subtitle = stringResource(R.string.gecko_extension_no_options).takeIf { !options },
            trailing = V5RowTrailing.Chevron,
            enabled = options,
            onClick = onOpenOptionsPage,
            modifier = Modifier.testTag(FirefoxExtensionManagerTestTags.optionsPage(extension.id)),
        )
        V5ListRow(
            title = stringResource(R.string.gecko_extension_update),
            titleColor = colors.accent,
            enabled = controls,
            onClick = onUpdate,
        )
    }
    V5ListGroup(dividerInset = V5ListDefaults.dividerInsetText) {
        V5ListRow(
            title = stringResource(R.string.gecko_extension_uninstall),
            titleColor = colors.danger,
            enabled = controls,
            onClick = onUninstall,
        )
    }
}

/** «Install extension»: the XPI address, then «Verify and install». */
@Composable
private fun InstallSheet(visible: Boolean, onInstall: (String) -> Unit, onDismiss: () -> Unit) {
    var url by remember(visible) { mutableStateOf("") }
    val title = stringResource(R.string.gecko_extension_install)
    val install = { if (url.isNotBlank()) onInstall(url) }
    V5WindowSheet(visible = visible, onDismiss = onDismiss, title = title) {
        V5SheetHeader(title = title, onClose = onDismiss, subtitle = stringResource(R.string.gecko_extension_install_hint))
        Column(
            Modifier.padding(top = V5Spacing.space3, bottom = V5Spacing.space4),
            verticalArrangement = Arrangement.spacedBy(V5Spacing.space4),
        ) {
            V5NameField(
                value = url,
                placeholder = stringResource(R.string.gecko_extension_v5_address),
                onValueChange = { url = it },
                onDone = install,
                maxLength = FirefoxExtensionManagerV5Rules.MAX_ADDRESS_LENGTH,
                modifier = Modifier.testTag(FirefoxExtensionManagerTestTags.InstallUrl),
                capitalize = false,
            )
            V5ListGroup {
                V5ListRow(
                    title = stringResource(R.string.gecko_extension_install_confirm),
                    titleColor = V5Theme.colors.accent,
                    enabled = url.isNotBlank(),
                    onClick = install,
                )
            }
        }
    }
}

/** The extension's permission question: what it asks for, private tabs too, allow or deny. */
@Composable
private fun PermissionSheet(request: GeckoExtensionPermissionRequest?, onDecision: (Boolean, Boolean) -> Unit) {
    // The sheet leaves on its spring after the answer, still showing what it asked.
    var shown by remember { mutableStateOf(request) }
    LaunchedEffect(request) { if (request != null) shown = request }
    val asked = request ?: shown ?: return
    var allowPrivate by remember(asked.extensionId) { mutableStateOf(false) }
    val title = asked.extensionName ?: asked.extensionId
    val access = FirefoxExtensionManagerV5Rules.requestedAccess(asked)
    // The extension's question on the browser's one prompt form: what it asks for, private tabs
    // too, then «Allow» in the accent; «Close» and a swipe down deny, as «Deny» does.
    V5PromptSheet(
        visible = request != null,
        title = title,
        message = stringResource(R.string.gecko_extension_permission_intro),
        actions = listOf(
            V5PromptAction(
                label = stringResource(R.string.action_allow),
                onClick = { onDecision(true, allowPrivate) },
                role = V5PromptRole.Primary,
            ),
            V5PromptAction(
                label = stringResource(R.string.action_deny),
                onClick = { onDecision(false, false) },
            ),
        ),
        onDismiss = { onDecision(false, false) },
        testTag = FirefoxExtensionManagerTestTags.PermissionPrompt,
    ) {
        V5ListGroup(dividerInset = V5ListDefaults.dividerInsetText) {
            if (access.isEmpty()) {
                V5ListRow(title = stringResource(R.string.gecko_extension_no_permissions))
            } else {
                access.forEach { value -> V5ListRow(title = value) }
            }
        }
        V5ListGroup(dividerInset = V5ListDefaults.dividerInsetText) {
            V5ListRow(
                title = stringResource(R.string.gecko_extension_allow_private_too),
                trailing = V5RowTrailing.Toggle(allowPrivate, { allowPrivate = it }),
            )
        }
    }
}

/** What the v5 manager shows, apart from drawing, for unit tests. */
internal object FirefoxExtensionManagerV5Rules {
    /** Longer than any real XPI address; the field stops there. */
    const val MAX_ADDRESS_LENGTH = 2048

    fun name(extension: GeckoExtension): String = extension.name ?: extension.id

    /** Version and ID under the name, as the current manager shows them. */
    fun details(extension: GeckoExtension): String = listOfNotNull(extension.version, extension.id).joinToString(" · ")

    /** Built-in extensions and a private selected tab leave the switches and actions read-only. */
    fun controlsEnabled(extension: GeckoExtension, canManage: Boolean): Boolean = canManage && !extension.isBuiltIn

    fun optionsAvailable(extension: GeckoExtension, canManage: Boolean): Boolean =
        canManage && GeckoExtensionChromeRules.optionsPageTarget(extension) != null

    /** Everything the extension asks for, each once, in the order Gecko reports it. */
    fun requestedAccess(request: GeckoExtensionPermissionRequest): List<String> =
        (request.permissions + request.origins + request.dataCollectionPermissions).distinct()

    /** The lines above the list: why changes are off, then what went wrong last. */
    fun notices(state: GeckoExtensionManagerState): List<Int> = listOfNotNull(
        R.string.gecko_extension_private_management_disabled.takeIf { state.managementContext.isPrivate },
        state.message?.let(::extensionManagerMessageRes),
    )
}

private const val DARK_SHELL_LUMINANCE = 0.5f
