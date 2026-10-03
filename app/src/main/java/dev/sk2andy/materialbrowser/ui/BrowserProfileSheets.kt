package dev.sk2andy.materialbrowser.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.browser.ProfileLockTrigger
import dev.sk2andy.materialbrowser.browser.ProfileProtection
import dev.sk2andy.materialbrowser.browser.ProfileProtectionRules
import dev.sk2andy.materialbrowser.browser.ProfileWallpaperTarget
import dev.sk2andy.materialbrowser.browser.WorkspaceAccent

internal data class ProfileCreationOptions(
    val protection: ProfileProtection? = null,
    val wallpaperTargets: Set<ProfileWallpaperTarget> = emptySet(),
    val name: String = "",
    val accent: WorkspaceAccent = WorkspaceAccent.Default,
)

internal object ProfileCreationOptionTestTags {
    const val Protection = "profile_creation_protection"
}

internal object ProfileCreationTestTags {
    const val Sheet = "profile_creation_sheet"
    const val Isolation = "profile_creation_isolation"
    const val IconScroll = "profile_creation_icon_scroll"
    const val CreateButton = "profile_creation_create_button"
}

@Composable
internal fun ProfileProtectionDialog(
    current: ProfileProtection?,
    onSave: (ProfileProtection) -> Unit,
    onDismiss: () -> Unit,
) {
    var trigger by remember(current) {
        mutableStateOf(current?.lockTrigger ?: ProfileLockTrigger.AppBackgrounded)
    }
    var cooldownInput by remember(current) {
        mutableStateOf(
            (current?.cooldownMinutes ?: ProfileProtectionRules.DEFAULT_COOLDOWN_MINUTES)
                .toString(),
        )
    }
    val cooldown = cooldownInput.toIntOrNull()
    val cooldownValid = cooldown != null &&
        cooldown in ProfileProtectionRules.MIN_COOLDOWN_MINUTES..
        ProfileProtectionRules.MAX_COOLDOWN_MINUTES
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.profile_protection_dialog_title)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(stringResource(R.string.profile_protection_disclosure))
                ProfileProtectionChoice(
                    title = stringResource(R.string.profile_protection_background),
                    selected = trigger == ProfileLockTrigger.AppBackgrounded,
                    onClick = { trigger = ProfileLockTrigger.AppBackgrounded },
                )
                ProfileProtectionChoice(
                    title = stringResource(R.string.profile_protection_closed),
                    selected = trigger == ProfileLockTrigger.AppClosed,
                    onClick = { trigger = ProfileLockTrigger.AppClosed },
                )
                ProfileProtectionChoice(
                    title = stringResource(R.string.profile_protection_cooldown),
                    selected = trigger == ProfileLockTrigger.Cooldown,
                    onClick = { trigger = ProfileLockTrigger.Cooldown },
                )
                if (trigger == ProfileLockTrigger.Cooldown) {
                    OutlinedTextField(
                        value = cooldownInput,
                        onValueChange = { value ->
                            cooldownInput = value.filter(Char::isDigit).take(4)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.profile_protection_minutes)) },
                        supportingText = {
                            Text(
                                stringResource(
                                    R.string.profile_protection_minutes_range,
                                    ProfileProtectionRules.MIN_COOLDOWN_MINUTES,
                                    ProfileProtectionRules.MAX_COOLDOWN_MINUTES,
                                ),
                            )
                        },
                        isError = !cooldownValid,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        ProfileProtection(
                            lockTrigger = trigger,
                            cooldownMinutes = cooldown
                                ?: ProfileProtectionRules.DEFAULT_COOLDOWN_MINUTES,
                        ),
                    )
                },
                enabled = trigger != ProfileLockTrigger.Cooldown || cooldownValid,
            ) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}

@Composable
private fun ProfileProtectionChoice(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton,
            )
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(title, modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
internal fun profileProtectionSummary(protection: ProfileProtection): String = when (
    protection.lockTrigger
) {
    ProfileLockTrigger.AppBackgrounded ->
        stringResource(R.string.profile_protection_background)
    ProfileLockTrigger.AppClosed -> stringResource(R.string.profile_protection_closed)
    ProfileLockTrigger.Cooldown -> stringResource(
        R.string.profile_protection_cooldown_summary,
        protection.cooldownMinutes,
    )
}
