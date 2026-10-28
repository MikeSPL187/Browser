package dev.sk2andy.materialbrowser.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.data.BrowserChromeScrollDispatchMode
import dev.sk2andy.materialbrowser.data.DeveloperSettings
import dev.sk2andy.materialbrowser.shared.ui.settings.SettingsCard
import dev.sk2andy.materialbrowser.shared.ui.settings.SettingsCardHeader
import dev.sk2andy.materialbrowser.shared.ui.settings.SettingsCardLinkRow
import dev.sk2andy.materialbrowser.shared.ui.settings.SettingsCardSliderRow
import dev.sk2andy.materialbrowser.shared.ui.settings.SettingsCardSwitchRow
import dev.sk2andy.materialbrowser.shared.ui.theme.SettingsCardTokens
import dev.sk2andy.materialbrowser.ui.theme.browserChromeColor

internal object DeveloperOptionsTestTags {
    const val BrowserChromeScrollDispatchMode = "developer_options_scroll_dispatch_mode"
    const val LayoutQuietPeriod = "developer_options_layout_quiet_period"
    const val RequiredFailures = "developer_options_required_failures"
    const val ForceSafeAreaFallback = "developer_options_force_safe_area_fallback"
    const val VolaGeckoBlocker = "developer_options_vola_gecko_blocker"
    const val UiV5Specimen = "developer_options_ui_v5_specimen"
    const val HttpPasswordAutofill = "developer_options_http_password_autofill"
    const val InputDiagnostics = "developer_options_input_diagnostics"
    const val CopyDiagnostics = "developer_options_copy_diagnostics"
    const val ShowOnboarding = "developer_options_show_onboarding"
    const val ShowReleaseNotes = "developer_options_show_release_notes"
    const val Reset = "developer_options_reset"
}

/** Developer options on cards (board W-Settings), unlocked by a long press on «About». */
@Composable
internal fun DeveloperOptionsSettingsPage(
    settings: DeveloperSettings,
    isHttpPasswordAutofillEnabled: Boolean = false,
    isHttpPasswordAutofillSupported: Boolean = false,
    /** Whether the engine has the page-level safe-area fallback these rows tune (System WebView). */
    isSafeAreaFallbackSupported: Boolean = true,
    isInputDiagnosticsEnabled: Boolean = false,
    onSettingsChanged: (DeveloperSettings) -> Unit,
    onHttpPasswordAutofillEnabledChanged: (Boolean) -> Unit = {},
    onInputDiagnosticsEnabledChanged: (Boolean) -> Unit = {},
    onCopyDiagnostics: () -> Unit = {},
    onShowOnboarding: () -> Unit = {},
    onShowReleaseNotes: () -> Unit = {},
    onBack: () -> Unit,
) {
    var httpAutofillConfirmationVisible by rememberSaveable { mutableStateOf(false) }
    var v5SpecimenVisible by rememberSaveable { mutableStateOf(false) }
    val cardColor = browserChromeColor(MaterialTheme.colorScheme.surfaceContainerHigh)
    val dividerColor = MaterialTheme.colorScheme.surfaceContainerHighest
    SettingsPage(
        title = stringResource(R.string.developer_options_title),
        onBack = onBack,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(SettingsCardTokens.cardGap)) {
            SettingsCardHeader(stringResource(R.string.developer_options_security_section))
            SettingsCard(containerColor = cardColor) {
                SettingsCardSwitchRow(
                    title = stringResource(R.string.settings_http_password_autofill_title),
                    summary = stringResource(
                        if (isHttpPasswordAutofillSupported) {
                            R.string.settings_http_password_autofill_gecko_summary
                        } else {
                            R.string.settings_http_password_autofill_system_webview_summary
                        },
                    ),
                    checked = isHttpPasswordAutofillSupported && isHttpPasswordAutofillEnabled,
                    summaryMaxLines = Int.MAX_VALUE,
                    enabled = isHttpPasswordAutofillSupported,
                    dividerColor = dividerColor,
                    onCheckedChange = { enabled ->
                        if (enabled) httpAutofillConfirmationVisible = true
                        else onHttpPasswordAutofillEnabledChanged(false)
                    },
                    modifier = Modifier.testTag(DeveloperOptionsTestTags.HttpPasswordAutofill),
                )
            }
            CardFootnote(
                stringResource(R.string.settings_http_password_autofill_warning),
                color = MaterialTheme.colorScheme.error,
            )
            SettingsCardHeader(stringResource(R.string.developer_options_diagnostics_section))
            SettingsCard(containerColor = cardColor) {
                SettingsCardSwitchRow(
                    title = stringResource(R.string.developer_options_input_diagnostics),
                    summary = stringResource(R.string.developer_options_input_diagnostics_summary),
                    checked = isInputDiagnosticsEnabled,
                    summaryMaxLines = Int.MAX_VALUE,
                    dividerColor = dividerColor,
                    divider = true,
                    onCheckedChange = onInputDiagnosticsEnabledChanged,
                    modifier = Modifier.testTag(DeveloperOptionsTestTags.InputDiagnostics),
                )
                SettingsCardLinkRow(
                    title = stringResource(R.string.developer_options_copy_diagnostics),
                    summary = stringResource(R.string.developer_options_copy_diagnostics_summary),
                    dividerColor = dividerColor,
                    onClick = onCopyDiagnostics,
                    modifier = Modifier.testTag(DeveloperOptionsTestTags.CopyDiagnostics),
                )
            }
            SettingsCardHeader(stringResource(R.string.developer_options_presentations_section))
            SettingsCard(containerColor = cardColor) {
                SettingsCardLinkRow(
                    title = stringResource(R.string.developer_options_show_onboarding),
                    summary = stringResource(R.string.developer_options_show_onboarding_summary),
                    dividerColor = dividerColor,
                    divider = true,
                    onClick = onShowOnboarding,
                    modifier = Modifier.testTag(DeveloperOptionsTestTags.ShowOnboarding),
                )
                SettingsCardLinkRow(
                    title = stringResource(R.string.developer_options_show_release_notes),
                    summary = stringResource(R.string.developer_options_show_release_notes_summary),
                    dividerColor = dividerColor,
                    onClick = onShowReleaseNotes,
                    modifier = Modifier.testTag(DeveloperOptionsTestTags.ShowReleaseNotes),
                )
            }
            SettingsCardHeader(stringResource(R.string.developer_options_experiments_section))
            SettingsCard(containerColor = cardColor) {
                if (isSafeAreaFallbackSupported) {
                    SettingsCardSwitchRow(
                        title = stringResource(R.string.developer_options_force_safe_area_fallback),
                        summary = stringResource(
                            R.string.developer_options_force_safe_area_fallback_summary,
                        ),
                        checked = settings.forceSafeAreaFallback,
                        summaryMaxLines = Int.MAX_VALUE,
                        dividerColor = dividerColor,
                        divider = true,
                        onCheckedChange = { enabled ->
                            onSettingsChanged(settings.copy(forceSafeAreaFallback = enabled))
                        },
                        modifier = Modifier.testTag(DeveloperOptionsTestTags.ForceSafeAreaFallback),
                    )
                }
                SettingsCardSwitchRow(
                    title = stringResource(R.string.developer_options_vola_gecko_blocker),
                    summary = stringResource(R.string.developer_options_vola_gecko_blocker_summary),
                    checked = settings.volaGeckoBlocker,
                    summaryMaxLines = Int.MAX_VALUE,
                    dividerColor = dividerColor,
                    divider = true,
                    onCheckedChange = { enabled ->
                        onSettingsChanged(settings.copy(volaGeckoBlocker = enabled))
                    },
                    modifier = Modifier.testTag(DeveloperOptionsTestTags.VolaGeckoBlocker),
                )
                SettingsCardLinkRow(
                    title = stringResource(R.string.developer_options_ui_v5_specimen),
                    summary = stringResource(R.string.developer_options_ui_v5_specimen_summary),
                    summaryMaxLines = Int.MAX_VALUE,
                    dividerColor = dividerColor,
                    onClick = { v5SpecimenVisible = true },
                    modifier = Modifier.testTag(DeveloperOptionsTestTags.UiV5Specimen),
                )
            }
            SettingsCardHeader(stringResource(R.string.developer_options_performance_section))
            SettingsCard(containerColor = cardColor) {
                SettingsCardDropdownRow(
                    title = stringResource(R.string.developer_options_scroll_dispatch_mode),
                    selected = settings.browserChromeScrollDispatchMode,
                    options = BrowserChromeScrollDispatchMode.entries,
                    label = { mode -> mode.displayName() },
                    summary = stringResource(R.string.developer_options_scroll_dispatch_mode_summary),
                    dividerColor = dividerColor,
                    onSelected = { mode ->
                        onSettingsChanged(settings.copy(browserChromeScrollDispatchMode = mode))
                    },
                    modifier = Modifier.testTag(
                        DeveloperOptionsTestTags.BrowserChromeScrollDispatchMode,
                    ),
                )
            }
            if (isSafeAreaFallbackSupported) {
                SettingsCardHeader(stringResource(R.string.developer_options_safe_area_section))
                CardFootnote(stringResource(R.string.developer_options_safe_area_summary))
                SettingsCard(containerColor = cardColor) {
                    DeveloperSliderRow(
                        title = stringResource(R.string.developer_options_layout_quiet_period),
                        summary = stringResource(
                            R.string.developer_options_layout_quiet_period_summary,
                        ),
                        valueLabel = { value ->
                            stringResource(R.string.developer_options_milliseconds_value, value)
                        },
                        value = settings.safeAreaLayoutQuietPeriodMillis,
                        range = DeveloperSettings.MIN_SAFE_AREA_LAYOUT_QUIET_PERIOD_MILLIS..
                            DeveloperSettings.MAX_SAFE_AREA_LAYOUT_QUIET_PERIOD_MILLIS,
                        step = DeveloperSettings.SAFE_AREA_LAYOUT_QUIET_PERIOD_STEP_MILLIS,
                        testTag = DeveloperOptionsTestTags.LayoutQuietPeriod,
                        onValueChanged = { value ->
                            onSettingsChanged(
                                settings.copy(safeAreaLayoutQuietPeriodMillis = value),
                            )
                        },
                        dividerColor = dividerColor,
                        divider = true,
                    )
                    DeveloperSliderRow(
                        title = stringResource(R.string.developer_options_required_failures),
                        summary = stringResource(R.string.developer_options_required_failures_summary),
                        valueLabel = { value ->
                            pluralStringResource(R.plurals.developer_options_failed_checks_value, value, value)
                        },
                        value = settings.safeAreaRequiredFailureCount,
                        range = DeveloperSettings.MIN_SAFE_AREA_REQUIRED_FAILURE_COUNT..
                            DeveloperSettings.MAX_SAFE_AREA_REQUIRED_FAILURE_COUNT,
                        step = 1,
                        testTag = DeveloperOptionsTestTags.RequiredFailures,
                        onValueChanged = { value ->
                            onSettingsChanged(
                                settings.copy(safeAreaRequiredFailureCount = value),
                            )
                        },
                        dividerColor = dividerColor,
                    )
                }
                TextButton(
                    onClick = { onSettingsChanged(settings.withDefaultSafeAreaSettings()) },
                    enabled = !settings.hasDefaultSafeAreaSettings,
                    modifier = Modifier
                        .align(Alignment.End)
                        .testTag(DeveloperOptionsTestTags.Reset),
                ) {
                    Text(stringResource(R.string.developer_options_reset_safe_area))
                }
            }
        }
    }
    if (v5SpecimenVisible) {
        V5SpecimenDialog(onDismiss = { v5SpecimenVisible = false })
    }
    if (httpAutofillConfirmationVisible) {
        SettingsPromptV5(
            title = stringResource(R.string.developer_options_http_warning_title),
            message = stringResource(R.string.developer_options_http_warning_message),
            confirm = stringResource(R.string.developer_options_http_warning_confirm),
            onConfirm = {
                httpAutofillConfirmationVisible = false
                onHttpPasswordAutofillEnabledChanged(true)
            },
            onDismiss = { httpAutofillConfirmationVisible = false },
            destructive = true,
        )
    }
}

@Composable
private fun BrowserChromeScrollDispatchMode.displayName(): String = stringResource(
    when (this) {
        BrowserChromeScrollDispatchMode.Optimized ->
            R.string.developer_options_scroll_dispatch_optimized
        BrowserChromeScrollDispatchMode.Fixed120Hz ->
            R.string.developer_options_scroll_dispatch_120_hz
        BrowserChromeScrollDispatchMode.Fixed60Hz ->
            R.string.developer_options_scroll_dispatch_60_hz
        BrowserChromeScrollDispatchMode.Fixed30Hz ->
            R.string.developer_options_scroll_dispatch_30_hz
        BrowserChromeScrollDispatchMode.Fixed15Hz ->
            R.string.developer_options_scroll_dispatch_15_hz
    },
)

private val DeveloperSettings.hasDefaultSafeAreaSettings: Boolean
    get() =
        safeAreaLayoutQuietPeriodMillis ==
        DeveloperSettings.DEFAULT_SAFE_AREA_LAYOUT_QUIET_PERIOD_MILLIS &&
            safeAreaRequiredFailureCount ==
            DeveloperSettings.DEFAULT_SAFE_AREA_REQUIRED_FAILURE_COUNT &&
            !forceSafeAreaFallback

private fun DeveloperSettings.withDefaultSafeAreaSettings(): DeveloperSettings = copy(
    safeAreaLayoutQuietPeriodMillis =
        DeveloperSettings.DEFAULT_SAFE_AREA_LAYOUT_QUIET_PERIOD_MILLIS,
    safeAreaRequiredFailureCount = DeveloperSettings.DEFAULT_SAFE_AREA_REQUIRED_FAILURE_COUNT,
    forceSafeAreaFallback = false,
)

/** A line under a header or a card that explains the card, not one of its rows. */
@Composable
private fun CardFootnote(text: String, color: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Text(
        text = text,
        modifier = Modifier.padding(SettingsCardTokens.headerPadding),
        style = MaterialTheme.typography.bodySmall,
        color = color,
    )
}

/**
 * A whole-number setting on a slider: the value at the end of the row follows the finger, the
 * setting changes once the finger lifts.
 */
@Composable
private fun DeveloperSliderRow(
    title: String,
    summary: String,
    valueLabel: @Composable (Int) -> String,
    value: Int,
    range: IntRange,
    step: Int,
    testTag: String,
    dividerColor: Color,
    onValueChanged: (Int) -> Unit,
    divider: Boolean = false,
    enabled: Boolean = true,
) {
    var live by remember(value) { mutableIntStateOf(value) }
    SettingsCardSliderRow(
        title = title,
        summary = summary,
        value = valueLabel(live),
        dividerColor = dividerColor,
        divider = divider,
    ) {
        SettingsCardSlider(
            value = value,
            range = range,
            label = title,
            step = step,
            enabled = enabled,
            onValueChange = { live = it },
            onValueChangeFinished = onValueChanged,
            modifier = Modifier.testTag(testTag),
        )
    }
}
