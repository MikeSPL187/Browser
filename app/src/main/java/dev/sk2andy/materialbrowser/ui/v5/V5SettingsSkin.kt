package dev.sk2andy.materialbrowser.ui.v5

import android.view.View
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row as LayoutRow
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import dev.sk2andy.materialbrowser.shared.ui.icons.VolaIcons
import dev.sk2andy.materialbrowser.shared.ui.settings.LocalSettingsSkin
import dev.sk2andy.materialbrowser.shared.ui.settings.SettingsRowEnd
import dev.sk2andy.materialbrowser.shared.ui.settings.SettingsSkin
import dev.sk2andy.materialbrowser.shared.ui.settings.SettingsTileColors
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Haptic
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Sizes
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Spacing
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Theme
import dev.sk2andy.materialbrowser.ui.theme.v5.VolaV5Theme
import dev.sk2andy.materialbrowser.ui.theme.v5.performV5Haptic
import kotlinx.coroutines.launch

/**
 * The inner settings pages (V15d) draw their shared components as v5 lists, the way
 * the settings home does (canvas screen «ж», system-v5.md sections 1.5 and 3.4): a bar with the
 * back arrow and the page's name, rounded groups on the grouped background, 17 sp rows of at least
 * 52 dp, dividers from under the text, the v5 switch with its tick, and a grey press highlight
 * instead of the ripple. Every page built from those components changes at once; what each row
 * does and announces stays the components' own.
 */
@Composable
internal fun ProvideV5SettingsSkin(content: @Composable () -> Unit) {
    val dark = MaterialTheme.colorScheme.background.luminance() < DARK_SHELL_LUMINANCE
    VolaV5Theme(dark = dark) {
        val view = LocalView.current
        val skin = remember(view) { V5SettingsSkin(view) }
        CompositionLocalProvider(LocalSettingsSkin provides skin, content = content)
    }
}

internal class V5SettingsSkin(private val view: View) : SettingsSkin {
    @Composable
    override fun Page(
        title: String,
        backContentDescription: String,
        onBack: () -> Unit,
        actions: @Composable RowScope.() -> Unit,
        content: @Composable ColumnScope.() -> Unit,
    ) {
        val colors = V5Theme.colors
        val highlight = remember(colors.fill) { V5RowHighlight(colors.fill) }
        Column(
            Modifier
                .fillMaxSize()
                .background(colors.background)
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding(),
        ) {
            V5PageBar(
                title = title,
                onBack = onBack,
                backContentDescription = backContentDescription,
                actions = actions,
            )
            CompositionLocalProvider(LocalIndication provides highlight) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = V5Spacing.marginPhone)
                        .padding(bottom = V5Spacing.space8),
                    content = content,
                )
            }
        }
    }

    @Composable
    override fun Card(modifier: Modifier, content: @Composable ColumnScope.() -> Unit) {
        Column(
            modifier
                .fillMaxWidth()
                .clip(V5Theme.shapes.s)
                .background(V5Theme.colors.surface),
            content = content,
        )
    }

    @Composable
    override fun Header(text: String, modifier: Modifier) {
        Text(
            text,
            style = V5Theme.type.footnote,
            color = V5Theme.colors.labelSecondary,
            modifier = modifier
                .padding(
                    start = V5Spacing.space4,
                    end = V5Spacing.space4,
                    top = V5Spacing.space5,
                    bottom = V5Spacing.space2,
                )
                .semantics { heading() },
        )
    }

    @Composable
    override fun Tile(colors: SettingsTileColors, content: @Composable (Modifier, Color) -> Unit) {
        Box(
            Modifier
                .size(V5Sizes.rowTile)
                .clip(V5Theme.shapes.xs)
                .background(colors.container),
            contentAlignment = Alignment.Center,
        ) {
            content(Modifier.size(V5Sizes.iconSmall), colors.content)
        }
    }

    @Composable
    override fun Row(
        title: String,
        summary: String?,
        summaryMaxLines: Int,
        enabled: Boolean,
        divider: Boolean,
        interaction: Modifier,
        modifier: Modifier,
        leading: (@Composable () -> Unit)?,
        end: SettingsRowEnd,
    ) {
        val colors = V5Theme.colors
        Column(Modifier.fillMaxWidth()) {
            LayoutRow(
                modifier
                    .fillMaxWidth()
                    .heightIn(min = if (summary.isNullOrEmpty()) V5Sizes.rowSingleLine else V5Sizes.rowTwoLine)
                    .then(interaction)
                    .graphicsLayer { alpha = if (enabled) 1f else V5_DISABLED_ALPHA }
                    .padding(horizontal = V5Spacing.space4, vertical = V5Spacing.space2),
                horizontalArrangement = Arrangement.spacedBy(V5Spacing.space3),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                leading?.invoke()
                val titleAndSummary = @Composable {
                    Column {
                        Text(title, style = V5Theme.type.body, color = colors.label)
                        if (!summary.isNullOrEmpty()) {
                            Text(
                                summary,
                                style = V5Theme.type.footnote,
                                color = colors.labelSecondary,
                                maxLines = summaryMaxLines,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
                if (end is SettingsRowEnd.Value) {
                    // A long value goes under the title instead of squeezing it.
                    V5TitleValueLayout(gap = V5Spacing.space3, modifier = Modifier.weight(1f), title = titleAndSummary) {
                        LayoutRow(verticalAlignment = Alignment.CenterVertically) { RowEnd(end) }
                    }
                } else {
                    Box(Modifier.weight(1f)) { titleAndSummary() }
                    RowEnd(end)
                }
            }
            if (divider) {
                Divider(
                    if (leading != null) V5ListDefaults.dividerInsetWithIcon else V5ListDefaults.dividerInsetText,
                )
            }
        }
    }

    @Composable
    override fun SliderRow(
        title: String,
        summary: String?,
        value: String?,
        divider: Boolean,
        modifier: Modifier,
        slider: @Composable () -> Unit,
    ) {
        val colors = V5Theme.colors
        Column(modifier.fillMaxWidth()) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = V5Sizes.rowTwoLine)
                    .padding(horizontal = V5Spacing.space4, vertical = V5Spacing.space2),
            ) {
                LayoutRow(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, style = V5Theme.type.body, color = colors.label, modifier = Modifier.weight(1f))
                    if (value != null) Text(value, style = V5Theme.type.body, color = colors.labelSecondary)
                }
                if (!summary.isNullOrEmpty()) {
                    Text(summary, style = V5Theme.type.footnote, color = colors.labelSecondary)
                }
                slider()
            }
            if (divider) Divider(V5ListDefaults.dividerInsetText)
        }
    }

    @Composable
    override fun Choices(
        title: String?,
        expanded: Boolean,
        onDismissRequest: () -> Unit,
        content: @Composable ColumnScope.() -> Unit,
    ) {
        // The menu of the current interface becomes v5's choice: the prompt sheet with the values
        // as rows on one card, the chosen one checked (V5ChoiceSheet's look).
        V5PromptSheet(
            visible = expanded,
            title = title.orEmpty(),
            message = null,
            actions = emptyList(),
            onDismiss = onDismissRequest,
        ) {
            Column(
                Modifier
                    .padding(horizontal = V5Spacing.marginPhone)
                    .fillMaxWidth()
                    .clip(V5Theme.shapes.s)
                    .background(V5Theme.colors.surface),
                content = content,
            )
        }
    }

    override fun onToggled(checked: Boolean) {
        view.performV5Haptic(if (checked) V5Haptic.ToggleOn else V5Haptic.ToggleOff)
    }

    @Composable
    private fun RowScope.RowEnd(end: SettingsRowEnd) {
        val colors = V5Theme.colors
        when (end) {
            SettingsRowEnd.Chevron -> Icon(
                VolaIcons.KeyboardArrowRight,
                contentDescription = null,
                tint = colors.labelTertiary,
                modifier = Modifier.size(V5Sizes.iconSmall),
            )
            is SettingsRowEnd.Check -> if (end.selected) {
                Icon(VolaIcons.Check, contentDescription = null, tint = colors.accent, modifier = Modifier.size(V5Sizes.iconSmall))
            }
            is SettingsRowEnd.Switch -> V5Toggle(checked = end.checked, onCheckedChange = null, enabled = end.enabled)
            is SettingsRowEnd.Value -> {
                Text(
                    end.text,
                    style = V5Theme.type.body,
                    color = colors.labelSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Icon(
                    VolaIcons.ArrowDropDown,
                    contentDescription = null,
                    tint = colors.labelTertiary,
                    modifier = Modifier.size(V5Sizes.iconSmall),
                )
            }
        }
    }

    @Composable
    private fun Divider(start: Dp) {
        Box(
            Modifier
                .padding(start = start)
                .fillMaxWidth()
                .height(V5Spacing.hairline)
                .background(V5Theme.colors.separator),
        )
    }
}

/** The grey a pressed row turns, behind its words, as a Safari list row does; no ripple. */
private class V5RowHighlight(private val color: Color) : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode = Node(interactionSource, color)

    override fun equals(other: Any?): Boolean = other is V5RowHighlight && other.color == color

    override fun hashCode(): Int = color.hashCode()

    private class Node(
        private val source: InteractionSource,
        private val color: Color,
    ) : Modifier.Node(), DrawModifierNode {
        private var pressed = false

        override fun onAttach() {
            coroutineScope.launch {
                source.interactions.collect { interaction ->
                    val next = when (interaction) {
                        is PressInteraction.Press -> true
                        is PressInteraction.Release, is PressInteraction.Cancel -> false
                        else -> pressed
                    }
                    if (next != pressed) {
                        pressed = next
                        invalidateDraw()
                    }
                }
            }
        }

        override fun ContentDrawScope.draw() {
            if (pressed) drawRect(color)
            drawContent()
        }
    }
}

/** Below this background luminance the current theme is dark. */
private const val DARK_SHELL_LUMINANCE = 0.5f
