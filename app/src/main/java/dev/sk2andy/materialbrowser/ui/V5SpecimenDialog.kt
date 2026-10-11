package dev.sk2andy.materialbrowser.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Accent
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Colors
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Sizes
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Spacing
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Specimen
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Theme
import dev.sk2andy.materialbrowser.ui.theme.v5.VolaV5Theme

/*
 * The v5 design specimen from developer options (V6b): the owner checks the Inter font, the color
 * roles, the six accents and the smoothed shapes on a real phone before screens are built on them.
 */

internal enum class V5SpecimenMode(@StringRes val label: Int) {
    Light(R.string.v5_specimen_theme_light),
    Dark(R.string.v5_specimen_theme_dark),
    Private(R.string.v5_specimen_theme_private),
}

@StringRes
private fun V5Accent.label(): Int = when (this) {
    V5Accent.Blue -> R.string.v5_accent_blue
    V5Accent.Green -> R.string.v5_accent_green
    V5Accent.Orange -> R.string.v5_accent_orange
    V5Accent.Pink -> R.string.v5_accent_pink
    V5Accent.Purple -> R.string.v5_accent_purple
    V5Accent.Graphite -> R.string.v5_accent_graphite
}

@Composable
internal fun V5SpecimenDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        V5SpecimenScreen(onClose = onDismiss)
    }
}

@Composable
internal fun V5SpecimenScreen(onClose: () -> Unit, modifier: Modifier = Modifier) {
    val systemDark = isSystemInDarkTheme()
    var mode by rememberSaveable { mutableStateOf(if (systemDark) V5SpecimenMode.Dark else V5SpecimenMode.Light) }
    var accent by rememberSaveable { mutableStateOf(V5Accent.Default) }
    VolaV5Theme(
        dark = mode != V5SpecimenMode.Light,
        accent = accent,
        privateBrowsing = mode == V5SpecimenMode.Private,
    ) {
        val colors = V5Theme.colors
        Column(
            modifier
                .fillMaxSize()
                .background(colors.background)
                .systemBarsPadding(),
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = V5Sizes.touchTarget)
                    .padding(horizontal = V5Spacing.marginPhone),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.v5_specimen_title),
                    style = V5Theme.type.headline,
                    color = colors.label,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onClose) {
                    Text(stringResource(R.string.v5_specimen_close), style = V5Theme.type.headline, color = colors.accent)
                }
            }
            Row(
                Modifier.padding(horizontal = V5Spacing.marginPhone),
                horizontalArrangement = Arrangement.spacedBy(V5Spacing.space2),
            ) {
                V5SpecimenMode.entries.forEach { option ->
                    FilterChip(
                        selected = option == mode,
                        onClick = { mode = option },
                        label = { Text(stringResource(option.label), style = V5Theme.type.callout) },
                    )
                }
            }
            if (mode != V5SpecimenMode.Private) {
                AccentPicker(selected = accent, onSelected = { accent = it })
            }
            V5Specimen(Modifier.verticalScroll(rememberScrollState()))
        }
    }
}

@Composable
private fun AccentPicker(selected: V5Accent, onSelected: (V5Accent) -> Unit) {
    val dark = V5Theme.colors.isDark
    Row(
        Modifier.padding(horizontal = V5Spacing.marginPhone, vertical = V5Spacing.space2),
        horizontalArrangement = Arrangement.spacedBy(V5Spacing.space2),
    ) {
        V5Accent.entries.forEach { option ->
            val fill = if (dark) V5Colors.dark(option).accentFill else V5Colors.light(option).accentFill
            val name = stringResource(option.label())
            Box(
                Modifier
                    .size(V5Sizes.touchTarget)
                    .clip(V5Theme.shapes.full)
                    .selectable(selected = option == selected, role = Role.RadioButton, onClick = { onSelected(option) })
                    .semantics { contentDescription = name },
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier
                        .size(V5Sizes.barCollapsed)
                        .clip(V5Theme.shapes.full)
                        .background(fill)
                        .border(
                            width = if (option == selected) V5Sizes.groupBorder else V5Spacing.hairline,
                            color = if (option == selected) V5Theme.colors.label else Color.Transparent,
                            shape = V5Theme.shapes.full,
                        ),
                )
            }
        }
    }
}

@Preview(name = "v5 specimen screen")
@Composable
private fun V5SpecimenScreenPreview() {
    V5SpecimenScreen(onClose = {})
}
