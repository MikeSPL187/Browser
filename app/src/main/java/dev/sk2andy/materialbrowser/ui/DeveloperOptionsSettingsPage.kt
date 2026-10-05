package dev.sk2andy.materialbrowser.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import dev.sk2andy.materialbrowser.data.GeckoSafeAreaSettings
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
    const val HttpPasswordAutofill = "developer_options_http_password_autofill"
    const val InputDiagnostics = "developer_options_input_diagnostics"
    const val CopyDiagnostics = "developer_options_copy_diagnostics"
    const val ShowOnboarding = "developer_options_show_onboarding"
    const val ShowReleaseNotes = "developer_options_show_release_notes"
    const val Reset = "developer_options_reset"
    const val GeckoSafeAreaEnabled = "developer_options_gecko_safe_area_enabled"
    const val GeckoRecheckAddedElements = "developer_options_gecko_recheck_added_elements"
    const val GeckoRecheckChangedElements = "developer_options_gecko_recheck_changed_elements"
    const val GeckoRequireInteraction = "developer_options_gecko_require_interaction"
    const val GeckoRecheckOnResize = "developer_options_gecko_recheck_on_resize"
    const val GeckoInteractionWindow = "developer_options_gecko_interaction_window"
    const val GeckoMutationDebounce = "developer_options_gecko_mutation_debounce"
    const val GeckoMaxElementsPerBatch = "developer_options_gecko_max_elements_per_batch"
    const val GeckoMaxBatchDuration = "developer_options_gecko_max_batch_duration"
    const val GeckoMaxInitialElements = "developer_options_gecko_max_initial_elements"
    const val GeckoReset = "developer_options_gecko_reset"
}

/** Developer options on cards (board W-Settings), unlocked by a long press on «About». */
@Composable
internal fun DeveloperOptionsSettingsPage(
    settings: DeveloperSettings,
    isHttpPasswordAutofillEnabled: Boolean = false,
    isHttpPasswordAutofillSupported: Boolean = false,
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
                SettingsCardSwitchRow(
                    title = stringResource(R.string.developer_options_force_safe_area_fallback),
                    summary = stringResource(
                        R.string.developer_options_force_safe_area_fallback_summary,
                    ),
                    checked = settings.forceSafeAreaFallback,
                    summaryMaxLines = Int.MAX_VALUE,
                    dividerColor = dividerColor,
                    onCheckedChange = { enabled ->
                        onSettingsChanged(settings.copy(forceSafeAreaFallback = enabled))
                    },
                    modifier = Modifier.testTag(DeveloperOptionsTestTags.ForceSafeAreaFallback),
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
            GeckoSafeAreaSettingsSection(
                settings = settings.geckoSafeAreaSettings,
                cardColor = cardColor,
                dividerColor = dividerColor,
                onSettingsChanged = { geckoSettings ->
                    onSettingsChanged(
                        settings.copy(geckoSafeAreaSettings = geckoSettings.normalized()),
                    )
                },
            )
        }
    }
    if (httpAutofillConfirmationVisible) {
        AlertDialog(
            onDismissRequest = { httpAutofillConfirmationVisible = false },
            title = { Text(stringResource(R.string.developer_options_http_warning_title)) },
            text = { Text(stringResource(R.string.developer_options_http_warning_message)) },
            confirmButton = {
                Button(
                    onClick = {
                        httpAutofillConfirmationVisible = false
                        onHttpPasswordAutofillEnabledChanged(true)
                    },
                ) {
                    Text(stringResource(R.string.developer_options_http_warning_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { httpAutofillConfirmationVisible = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

@Composable
private fun GeckoSafeAreaSettingsSection(
    settings: GeckoSafeAreaSettings,
    cardColor: Color,
    dividerColor: Color,
    onSettingsChanged: (GeckoSafeAreaSettings) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(SettingsCardTokens.cardGap)) {
        SettingsCardHeader(stringResource(R.string.developer_options_gecko_safe_area_section))
        CardFootnote(stringResource(R.string.developer_options_gecko_safe_area_summary))
        SettingsCard(containerColor = cardColor) {
            SettingsCardSwitchRow(
                title = stringResource(R.string.developer_options_gecko_safe_area_enabled),
                summary = stringResource(R.string.developer_options_gecko_safe_area_enabled_summary),
                checked = settings.enabled,
                summaryMaxLines = Int.MAX_VALUE,
                dividerColor = dividerColor,
                divider = true,
                onCheckedChange = { onSettingsChanged(settings.copy(enabled = it)) },
                modifier = Modifier.testTag(DeveloperOptionsTestTags.GeckoSafeAreaEnabled),
            )
            SettingsCardSwitchRow(
                title = stringResource(R.string.developer_options_gecko_recheck_added_elements),
                summary = stringResource(R.string.developer_options_gecko_recheck_added_elements_summary),
                checked = settings.recheckAddedElements,
                summaryMaxLines = Int.MAX_VALUE,
                enabled = settings.enabled,
                dividerColor = dividerColor,
                divider = true,
                onCheckedChange = { onSettingsChanged(settings.copy(recheckAddedElements = it)) },
                modifier = Modifier.testTag(DeveloperOptionsTestTags.GeckoRecheckAddedElements),
            )
            SettingsCardSwitchRow(
                title = stringResource(R.string.developer_options_gecko_recheck_changed_elements),
                summary = stringResource(R.string.developer_options_gecko_recheck_changed_elements_summary),
                checked = settings.recheckChangedElements,
                summaryMaxLines = Int.MAX_VALUE,
                enabled = settings.enabled,
                dividerColor = dividerColor,
                divider = true,
                onCheckedChange = { onSettingsChanged(settings.copy(recheckChangedElements = it)) },
                modifier = Modifier.testTag(DeveloperOptionsTestTags.GeckoRecheckChangedElements),
            )
            SettingsCardSwitchRow(
                title = stringResource(R.string.developer_options_gecko_require_interaction),
                summary = stringResource(R.string.developer_options_gecko_require_interaction_summary),
                checked = settings.requireInteractionForUpdates,
                summaryMaxLines = Int.MAX_VALUE,
                enabled = settings.enabled,
                dividerColor = dividerColor,
                divider = true,
                onCheckedChange = { onSettingsChanged(settings.copy(requireInteractionForUpdates = it)) },
                modifier = Modifier.testTag(DeveloperOptionsTestTags.GeckoRequireInteraction),
            )
            SettingsCardSwitchRow(
                title = stringResource(R.string.developer_options_gecko_recheck_on_resize),
                summary = stringResource(R.string.developer_options_gecko_recheck_on_resize_summary),
                checked = settings.recheckOnResize,
                summaryMaxLines = Int.MAX_VALUE,
                enabled = settings.enabled,
                dividerColor = dividerColor,
                divider = true,
                onCheckedChange = { onSettingsChanged(settings.copy(recheckOnResize = it)) },
                modifier = Modifier.testTag(DeveloperOptionsTestTags.GeckoRecheckOnResize),
            )
            DeveloperSliderRow(
                title = stringResource(R.string.developer_options_gecko_interaction_window),
                summary = stringResource(R.string.developer_options_gecko_interaction_window_summary),
                valueLabel = { value ->
                    stringResource(R.string.developer_options_milliseconds_value, value)
                },
                value = settings.interactionWindowMillis,
                range = GeckoSafeAreaSettings.MIN_INTERACTION_WINDOW_MILLIS..
                    GeckoSafeAreaSettings.MAX_INTERACTION_WINDOW_MILLIS,
                step = GeckoSafeAreaSettings.INTERACTION_WINDOW_STEP_MILLIS,
                testTag = DeveloperOptionsTestTags.GeckoInteractionWindow,
                enabled = settings.enabled,
                onValueChanged = { onSettingsChanged(settings.copy(interactionWindowMillis = it)) },
                dividerColor = dividerColor,
                divider = true,
            )
            DeveloperSliderRow(
                title = stringResource(R.string.developer_options_gecko_mutation_debounce),
                summary = stringResource(R.string.developer_options_gecko_mutation_debounce_summary),
                valueLabel = { value ->
                    stringResource(R.string.developer_options_milliseconds_value, value)
                },
                value = settings.mutationDebounceMillis,
                range = GeckoSafeAreaSettings.MIN_MUTATION_DEBOUNCE_MILLIS..
                    GeckoSafeAreaSettings.MAX_MUTATION_DEBOUNCE_MILLIS,
                step = GeckoSafeAreaSettings.MUTATION_DEBOUNCE_STEP_MILLIS,
                testTag = DeveloperOptionsTestTags.GeckoMutationDebounce,
                enabled = settings.enabled,
                onValueChanged = { onSettingsChanged(settings.copy(mutationDebounceMillis = it)) },
                dividerColor = dividerColor,
                divider = true,
            )
            DeveloperSliderRow(
                title = stringResource(R.string.developer_options_gecko_max_elements_per_batch),
                summary = stringResource(R.string.developer_options_gecko_max_elements_per_batch_summary),
                valueLabel = { value ->
                    pluralStringResource(R.plurals.developer_options_gecko_elements_value, value, value)
                },
                value = settings.maxElementsPerBatch,
                range = GeckoSafeAreaSettings.MIN_MAX_ELEMENTS_PER_BATCH..
                    GeckoSafeAreaSettings.MAX_MAX_ELEMENTS_PER_BATCH,
                step = GeckoSafeAreaSettings.MAX_ELEMENTS_PER_BATCH_STEP,
                testTag = DeveloperOptionsTestTags.GeckoMaxElementsPerBatch,
                enabled = settings.enabled,
                onValueChanged = { onSettingsChanged(settings.copy(maxElementsPerBatch = it)) },
                dividerColor = dividerColor,
                divider = true,
            )
            DeveloperSliderRow(
                title = stringResource(R.string.developer_options_gecko_max_batch_duration),
                summary = stringResource(R.string.developer_options_gecko_max_batch_duration_summary),
                valueLabel = { value ->
                    stringResource(R.string.developer_options_milliseconds_value, value)
                },
                value = settings.maxBatchDurationMillis,
                range = GeckoSafeAreaSettings.MIN_MAX_BATCH_DURATION_MILLIS..
                    GeckoSafeAreaSettings.MAX_MAX_BATCH_DURATION_MILLIS,
                step = GeckoSafeAreaSettings.MAX_BATCH_DURATION_STEP_MILLIS,
                testTag = DeveloperOptionsTestTags.GeckoMaxBatchDuration,
                enabled = settings.enabled,
                onValueChanged = { onSettingsChanged(settings.copy(maxBatchDurationMillis = it)) },
                dividerColor = dividerColor,
                divider = true,
            )
            DeveloperSliderRow(
                title = stringResource(R.string.developer_options_gecko_max_initial_elements),
                summary = stringResource(R.string.developer_options_gecko_max_initial_elements_summary),
                valueLabel = { value ->
                    pluralStringResource(R.plurals.developer_options_gecko_elements_value, value, value)
                },
                value = settings.maxInitialElements,
                range = GeckoSafeAreaSettings.MIN_MAX_INITIAL_ELEMENTS..
                    GeckoSafeAreaSettings.MAX_MAX_INITIAL_ELEMENTS,
                step = GeckoSafeAreaSettings.MAX_INITIAL_ELEMENTS_STEP,
                testTag = DeveloperOptionsTestTags.GeckoMaxInitialElements,
                enabled = settings.enabled,
                onValueChanged = { onSettingsChanged(settings.copy(maxInitialElements = it)) },
                dividerColor = dividerColor,
            )
        }
        TextButton(
            onClick = { onSettingsChanged(settings.withDefaults()) },
            enabled = !settings.hasDefaultSettings,
            modifier = Modifier.align(Alignment.End).testTag(DeveloperOptionsTestTags.GeckoReset),
        ) {
            Text(stringResource(R.string.developer_options_gecko_reset))
        }
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
