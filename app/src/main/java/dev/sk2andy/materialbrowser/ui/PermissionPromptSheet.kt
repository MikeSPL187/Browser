package dev.sk2andy.materialbrowser.ui

import android.graphics.Bitmap
import androidx.compose.ui.draw.clip
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Sizes
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Theme
import dev.sk2andy.materialbrowser.ui.theme.v5.LocalV5ProfileAccent
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Accent
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Spacing
import dev.sk2andy.materialbrowser.ui.theme.v5.VolaV5Theme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.tooling.preview.Preview
import dev.sk2andy.materialbrowser.ui.v5.V5PromptSheet
import dev.sk2andy.materialbrowser.ui.v5.V5StateAction
import dev.sk2andy.materialbrowser.ui.v5.V5StatePrimaryAction
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
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
    /** A snapshot of the asking page, for the v5 sheet's glass. */
    backdrop: Bitmap? = null,
) {
    LaunchedEffect(prompt.id) { onShown() }
    // V28, B9c: under v5 on a phone the one v5 prompt sheet; closing it does not allow.
    if (rememberDrawsV5()) {
        PermissionPromptSheetV5(prompt, onChoice, backdrop)
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

/**
 * The permission question on the v5 prompt sheet (canvas «Permission»): its glyph, the site, the main
 * answer filled under the thumb («Only this time» where offered), the others side by side under it,
 * and where to change it later in small print.
 */
@Composable
private fun PermissionPromptSheetV5(
    prompt: PermissionPrompt,
    onChoice: (PermissionPromptChoice) -> Unit,
    backdrop: Bitmap? = null,
) {
    val host = SiteConnectionRules.host(prompt.site.origin).ifEmpty { prompt.site.origin }
    val names = prompt.permissions.map { it.displayName() }.joinToString()
    val message = stringResource(R.string.permission_radar_request_message, host, names)
    val note = stringResource(
        if (prompt.isPrivate) R.string.permission_radar_private_request_note else R.string.permission_prompt_change_later,
    )
    val icon = PermissionPromptRules.heroPermission(prompt.permissions).icon()
    val choices = PermissionPromptRules.choices(prompt.permissions)
    val main = PermissionPromptRules.mainChoice(choices)
    V5PromptTheme {
        val colors = V5Theme.colors
        V5PromptSheet(
            visible = true,
            title = stringResource(PermissionPromptRules.question(prompt.permissions).title()),
            message = message,
            actions = emptyList(),
            content = {
                Column(
                    Modifier.padding(horizontal = V5Spacing.marginPhone),
                    verticalArrangement = Arrangement.spacedBy(V5Spacing.space2),
                ) {
                    V5StatePrimaryAction(V5StateAction(stringResource(main.label(prompt.isPrivate)), onClick = { onChoice(main) }))
                    Row(horizontalArrangement = Arrangement.spacedBy(V5Spacing.space2)) {
                        choices.filter { it != main }.forEach { choice ->
                            Box(
                                Modifier
                                    .weight(1f)
                                    .heightIn(min = V5Sizes.touchTarget)
                                    .clip(V5Theme.shapes.full)
                                    .background(colors.fill)
                                    .clickable(role = Role.Button, onClick = { onChoice(choice) })
                                    .padding(horizontal = V5Spacing.space3, vertical = V5Spacing.space2),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    stringResource(choice.label(prompt.isPrivate)),
                                    style = V5Theme.type.callout.copy(fontWeight = FontWeight.Medium),
                                    color = colors.label,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }
                    Text(
                        note,
                        style = V5Theme.type.caption,
                        color = colors.labelSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(top = V5Spacing.space1),
                    )
                }
            },
            onDismiss = { onChoice(PermissionPromptChoice.Block) },
            testTag = PermissionRadarTestTags.Prompt,
            backdrop = backdrop,
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

/** The v5 sheet (canvas «Permission») in the theme and accent of the page under it. */
@Composable
private fun PermissionPromptV5Preview(dark: Boolean, accent: V5Accent, permission: SitePermission) {
    CompositionLocalProvider(LocalV5ProfileAccent provides accent) {
        VolaV5Theme(dark = dark) {
            PermissionPromptSheetV5(
                prompt = PermissionPrompt(
                    id = 1L,
                    tabId = "tab",
                    site = PermissionSiteKey(profileId = "personal", origin = "https://maps.example.com"),
                    permissions = setOf(permission),
                    isPrivate = false,
                ),
                onChoice = {},
            )
        }
    }
}

@Preview(name = "Permission v5 · location, light", heightDp = 640)
@Composable
private fun PermissionPromptV5LightPreview() = PermissionPromptV5Preview(false, V5Accent.Blue, SitePermission.Location)

@Preview(name = "Permission v5 · camera, dark, green", heightDp = 640)
@Composable
private fun PermissionPromptV5DarkPreview() = PermissionPromptV5Preview(true, V5Accent.Green, SitePermission.Camera)

@Preview(name = "Permission v5 · notifications, largest font", heightDp = 900, fontScale = 2f)
@Composable
private fun PermissionPromptV5LargeFontPreview() =
    PermissionPromptV5Preview(false, V5Accent.Blue, SitePermission.Notifications)
