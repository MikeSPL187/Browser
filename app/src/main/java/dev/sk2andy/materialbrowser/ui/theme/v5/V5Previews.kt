package dev.sk2andy.materialbrowser.ui.theme.v5

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Type.tabular

/*
 * The v5 specimen: color roles, the six accents, the type scale, the smoothed shapes, settings
 * tiles and a bar mock-up, in light, dark and private. Preview-only sample text is not localized.
 */

private const val SPECIMEN_WIDTH_DP = 412
private const val SPECIMEN_HEIGHT_DP = 1500

@Composable
internal fun V5Specimen(modifier: Modifier = Modifier) {
    val colors = V5Theme.colors
    val type = V5Theme.type
    Column(
        modifier
            .background(colors.background)
            .padding(V5Spacing.marginPhone),
        verticalArrangement = Arrangement.spacedBy(V5Spacing.space4),
    ) {
        Text("Vola v5", style = type.largeTitle, color = colors.label)
        BarMock()
        Group("Type") {
            Text("Large title 34", style = type.largeTitle, color = colors.label)
            Text("Title 22 · Отчёт о приватности", style = type.title, color = colors.label)
            Text("Headline 17 · polar-notes.ru", style = type.headline, color = colors.label)
            Text("Body 17 — съешь же ещё этих мягких французских булок", style = type.body, color = colors.label)
            Text("Callout 15 · подсказка под строкой", style = type.callout, color = colors.labelSecondary)
            Text("Footnote 13 · ЗАГОЛОВОК ГРУППЫ → Заголовок группы", style = type.footnote,
                color = colors.labelSecondary)
            Text("Caption 12 · заблокировано 1 284", style = type.caption.tabular(), color = colors.labelSecondary)
        }
        Group("Roles") {
            Row(horizontalArrangement = Arrangement.spacedBy(V5Spacing.space2)) {
                listOf(
                    colors.background, colors.surface, colors.surfaceElevated, colors.surfaceHighest,
                    colors.chrome, colors.success, colors.warning, colors.danger,
                ).forEach { Swatch(it, V5Theme.shapes.xs) }
            }
        }
        Group("Accents") {
            Row(horizontalArrangement = Arrangement.spacedBy(V5Spacing.space2)) {
                V5Accent.entries.forEach { accent ->
                    val scheme = if (colors.isDark) V5Colors.dark(accent) else V5Colors.light(accent)
                    Column(verticalArrangement = Arrangement.spacedBy(V5Spacing.space1)) {
                        Box(
                            Modifier.size(V5Sizes.touchTarget).clip(V5Theme.shapes.full).background(scheme.accentFill),
                            contentAlignment = Alignment.Center,
                        ) { Text("Go", style = type.footnote, color = scheme.onAccentFill) }
                        Text("Aa · ${accent.ordinal + 1}", style = type.caption, color = scheme.accent)
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Switch", style = type.body, color = colors.label, modifier = Modifier.weight(1f))
                Switch(checked = true, onCheckedChange = null)
            }
        }
        Group("Shapes xs · s · m · l · xl") {
            Row(horizontalArrangement = Arrangement.spacedBy(V5Spacing.space3)) {
                with(V5Theme.shapes) { listOf(xs, s, m, l, xl) }.forEach { Swatch(colors.accentSoft, it, 60) }
            }
        }
        Group("Settings tiles") {
            Row(horizontalArrangement = Arrangement.spacedBy(V5Spacing.space2)) {
                V5Tile.entries.forEach { Swatch(it.color, V5Theme.shapes.xs, 22) }
            }
        }
    }
}

@Composable
private fun BarMock() {
    val colors = V5Theme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .height(V5Sizes.bar)
            .background(colors.chrome)
            .padding(horizontal = V5Spacing.space3, vertical = V5Spacing.space2),
        horizontalArrangement = Arrangement.spacedBy(V5Spacing.space2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .weight(1f)
                .height(V5Sizes.barCapsule)
                .clip(V5Theme.shapes.full)
                .background(colors.fill),
            contentAlignment = Alignment.Center,
        ) {
            Text("polar-notes.ru", style = V5Theme.type.headline, color = colors.label, maxLines = 1,
                overflow = TextOverflow.Ellipsis)
        }
        repeat(2) {
            Box(Modifier.size(V5Sizes.barCapsule).clip(V5Theme.shapes.full).background(colors.fill))
        }
    }
}

@Composable
private fun Group(title: String, content: @Composable () -> Unit) {
    val colors = V5Theme.colors
    Column(verticalArrangement = Arrangement.spacedBy(V5Spacing.space2)) {
        Text(title, style = V5Theme.type.footnote, color = colors.labelSecondary)
        Column(
            Modifier
                .fillMaxWidth()
                .clip(V5Theme.shapes.s)
                .background(colors.surface)
                .padding(V5Spacing.space4)
                .heightIn(min = V5Sizes.rowSingleLine),
            verticalArrangement = Arrangement.spacedBy(V5Spacing.space2),
        ) { content() }
    }
}

@Composable
private fun Swatch(color: Color, shape: Shape, sizeDp: Int = 32) {
    Box(
        Modifier
            .size(sizeDp.dp)
            .clip(shape)
            .background(color)
            .border(V5Spacing.hairline, V5Theme.colors.separator, shape),
    )
}

@Preview(name = "v5 · light", widthDp = SPECIMEN_WIDTH_DP, heightDp = SPECIMEN_HEIGHT_DP)
@Composable
private fun V5SpecimenLightPreview() {
    VolaV5Theme(dark = false) { V5Specimen() }
}

@Preview(name = "v5 · dark", widthDp = SPECIMEN_WIDTH_DP, heightDp = SPECIMEN_HEIGHT_DP)
@Composable
private fun V5SpecimenDarkPreview() {
    VolaV5Theme(dark = true) { V5Specimen() }
}

@Preview(name = "v5 · private", widthDp = SPECIMEN_WIDTH_DP, heightDp = SPECIMEN_HEIGHT_DP)
@Composable
private fun V5SpecimenPrivatePreview() {
    VolaV5Theme(privateBrowsing = true) { V5Specimen() }
}

@Preview(name = "v5 · dark · graphite", widthDp = SPECIMEN_WIDTH_DP, heightDp = SPECIMEN_HEIGHT_DP)
@Composable
private fun V5SpecimenGraphitePreview() {
    VolaV5Theme(dark = true, accent = V5Accent.Graphite) { V5Specimen(Modifier.width(SPECIMEN_WIDTH_DP.dp)) }
}
