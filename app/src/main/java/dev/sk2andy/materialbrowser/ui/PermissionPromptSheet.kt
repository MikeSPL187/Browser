package dev.sk2andy.materialbrowser.ui

import androidx.compose.ui.draw.clip
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Sizes
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Theme
import dev.sk2andy.materialbrowser.ui.v5.V5PromptAction
import dev.sk2andy.materialbrowser.ui.v5.V5PromptRole
import dev.sk2andy.materialbrowser.ui.v5.V5PromptSheet
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.browser.SiteConnectionRules
import dev.sk2andy.materialbrowser.browser.permissions.PermissionPrompt
import dev.sk2andy.materialbrowser.browser.permissions.PermissionPromptChoice
import dev.sk2andy.materialbrowser.browser.permissions.PermissionPromptRules
import dev.sk2andy.materialbrowser.browser.permissions.PermissionQuestion
import dev.sk2andy.materialbrowser.browser.permissions.PermissionSiteKey
import dev.sk2andy.materialbrowser.browser.permissions.SitePermission
import dev.sk2andy.materialbrowser.data.AppearanceSettings
import dev.sk2andy.materialbrowser.data.BrowserAppearanceMode
import dev.sk2andy.materialbrowser.ui.theme.MaterialBrowserTheme
import dev.sk2andy.materialbrowser.ui.theme.VolaPermissionPrompt
import dev.sk2andy.materialbrowser.ui.theme.VolaPreviews

/**
 * A site asks for a permission (board W-Permission): the permission's icon, a plain question,
 * the site, and «Allow on this site», «Only this time» or «Don't allow». Closing the sheet without
 * a choice does not allow anything.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PermissionPromptSheet(
    prompt: PermissionPrompt,
    onChoice: (PermissionPromptChoice) -> Unit,
    onShown: () -> Unit = {},
) {
    LaunchedEffect(prompt.id) { onShown() }
    // V28, B9c: under v5 on a phone the one v5 prompt sheet; closing it does not allow.
    if (rememberDrawsV5()) {
        PermissionPromptSheetV5(prompt, onChoice)
        return
    }
    ModalBottomSheet(
        onDismissRequest = { onChoice(PermissionPromptChoice.Block) },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        modifier = Modifier.testTag(PermissionRadarTestTags.Prompt),
    ) {
        PermissionPromptContent(prompt = prompt, onChoice = onChoice)
    }
}

@Composable
private fun PermissionPromptContent(
    prompt: PermissionPrompt,
    onChoice: (PermissionPromptChoice) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val host = SiteConnectionRules.host(prompt.site.origin).ifEmpty { prompt.site.origin }
    val names = prompt.permissions.map { it.displayName() }.joinToString()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(
                start = VolaPermissionPrompt.sidePadding,
                end = VolaPermissionPrompt.sidePadding,
                bottom = VolaPermissionPrompt.bottomPadding,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(VolaPermissionPrompt.contentGap),
    ) {
        Box(
            modifier = Modifier
                .size(VolaPermissionPrompt.iconContainerSize)
                .background(colors.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(PermissionPromptRules.heroPermission(prompt.permissions).icon()),
                contentDescription = null,
                modifier = Modifier.size(VolaPermissionPrompt.iconSize),
                tint = colors.onPrimaryContainer,
            )
        }
        Text(
            text = stringResource(PermissionPromptRules.question(prompt.permissions).title()),
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.permission_radar_request_message, host, names),
            style = MaterialTheme.typography.bodyLarge,
            color = colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(
                if (prompt.isPrivate) {
                    R.string.permission_radar_private_request_note
                } else {
                    R.string.permission_prompt_change_later
                },
            ),
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = VolaPermissionPrompt.contentGap),
            verticalArrangement = Arrangement.spacedBy(VolaPermissionPrompt.buttonGap),
        ) {
            PermissionPromptRules.choices(prompt.permissions).forEach { choice ->
                val label = stringResource(choice.label(prompt.isPrivate))
                val modifier = Modifier.fillMaxWidth()
                when (choice) {
                    PermissionPromptChoice.AllowAlways ->
                        Button(onClick = { onChoice(choice) }, modifier = modifier) { Text(label) }
                    PermissionPromptChoice.AllowOnce ->
                        FilledTonalButton(onClick = { onChoice(choice) }, modifier = modifier) { Text(label) }
                    PermissionPromptChoice.Block ->
                        TextButton(onClick = { onChoice(choice) }, modifier = modifier) { Text(label) }
                }
            }
        }
    }
}

/** The permission question on the v5 prompt sheet: its glyph, the site and the three answers. */
@Composable
private fun PermissionPromptSheetV5(prompt: PermissionPrompt, onChoice: (PermissionPromptChoice) -> Unit) {
    val host = SiteConnectionRules.host(prompt.site.origin).ifEmpty { prompt.site.origin }
    val names = prompt.permissions.map { it.displayName() }.joinToString()
    val message = stringResource(R.string.permission_radar_request_message, host, names)
    val note = stringResource(
        if (prompt.isPrivate) R.string.permission_radar_private_request_note else R.string.permission_prompt_change_later,
    )
    val icon = PermissionPromptRules.heroPermission(prompt.permissions).icon()
    V5PromptTheme {
        val colors = V5Theme.colors
        V5PromptSheet(
            visible = true,
            title = stringResource(PermissionPromptRules.question(prompt.permissions).title()),
            message = "$message\n\n$note",
            actions = PermissionPromptRules.choices(prompt.permissions).map { choice ->
                V5PromptAction(
                    label = stringResource(choice.label(prompt.isPrivate)),
                    onClick = { onChoice(choice) },
                    role = if (choice == PermissionPromptChoice.AllowAlways) V5PromptRole.Primary else V5PromptRole.Default,
                )
            },
            onDismiss = { onChoice(PermissionPromptChoice.Block) },
            testTag = PermissionRadarTestTags.Prompt,
            leading = {
                Box(
                    Modifier
                        .size(V5Sizes.rowTile)
                        .clip(V5Theme.shapes.full)
                        .background(colors.accentFill),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(icon),
                        contentDescription = null,
                        tint = colors.onAccentFill,
                        modifier = Modifier.size(V5Sizes.iconSmall),
                    )
                }
            },
        )
    }
}

@StringRes
private fun PermissionQuestion.title(): Int = when (this) {
    PermissionQuestion.Location -> R.string.permission_prompt_question_location
    PermissionQuestion.Camera -> R.string.permission_prompt_question_camera
    PermissionQuestion.Microphone -> R.string.permission_prompt_question_microphone
    PermissionQuestion.CameraAndMicrophone -> R.string.permission_prompt_question_camera_microphone
    PermissionQuestion.Notifications -> R.string.permission_prompt_question_notifications
    PermissionQuestion.Other -> R.string.permission_radar_request_title
}

@StringRes
private fun PermissionPromptChoice.label(isPrivate: Boolean): Int = when (this) {
    PermissionPromptChoice.AllowAlways ->
        if (isPrivate) R.string.permission_radar_allow_private else R.string.permission_radar_allow_always
    PermissionPromptChoice.AllowOnce -> R.string.permission_radar_allow_once
    PermissionPromptChoice.Block -> R.string.permission_radar_block
}

@VolaPreviews
@Composable
private fun PermissionPromptContentPreview() {
    MaterialBrowserTheme(settings = AppearanceSettings(appearanceMode = BrowserAppearanceMode.System)) {
        Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
            PermissionPromptContent(
                prompt = PermissionPrompt(
                    id = 1L,
                    tabId = "tab",
                    site = PermissionSiteKey(profileId = "candy", origin = "https://maps.example.com"),
                    permissions = setOf(SitePermission.Location),
                    isPrivate = false,
                ),
                onChoice = {},
            )
        }
    }
}
