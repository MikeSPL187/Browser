package dev.sk2andy.materialbrowser.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.browser.WorkspaceAccent
import dev.sk2andy.materialbrowser.data.AppearanceSettings
import dev.sk2andy.materialbrowser.data.BrowserAppearanceMode
import dev.sk2andy.materialbrowser.shared.ui.icons.VolaIcons
import dev.sk2andy.materialbrowser.ui.theme.MaterialBrowserTheme
import dev.sk2andy.materialbrowser.ui.theme.VolaFirstRunTokens
import dev.sk2andy.materialbrowser.ui.theme.VolaPreviews
import dev.sk2andy.materialbrowser.ui.theme.VolaTheme
import dev.sk2andy.materialbrowser.ui.theme.color

internal object FirstRunTestTags {
    const val Welcome = "first_run_welcome"
    const val Start = "first_run_start"
    const val Import = "first_run_import"
    const val Setup = "first_run_setup"
    const val Trackers = "first_run_trackers"
    const val HttpsOnly = "first_run_https_only"
    const val Gestures = "first_run_gestures"
    const val DefaultBrowser = "first_run_default_browser"
    const val Next = "first_run_next"
    fun theme(mode: BrowserAppearanceMode) = "first_run_theme:${mode.name}"
}

/** The choices on «Make it yours»; each one is applied as soon as it is made. */
internal data class FirstRunSetup(
    val appearanceMode: BrowserAppearanceMode,
    val trackerProtection: Boolean,
    /** Null when the engine has no HTTPS-only mode (system WebView): the row is not shown. */
    val httpsOnly: Boolean?,
    val showGestures: Boolean,
    val isDefaultBrowser: Boolean,
)

/** How many steps the first run has: welcome, setup and, when chosen, the gesture lesson. */
private const val FIRST_RUN_STEPS = 3

/**
 * Board W-Welcome: three pages of three spaces, what Vola is in one sentence, «Get started», and a
 * way to bring passwords and bookmarks over first.
 */
@Composable
internal fun FirstRunWelcomeScreen(
    onStart: () -> Unit,
    onImport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FirstRunBackground(modifier.testTag(FirstRunTestTags.Welcome)) {
        Column(
            modifier = Modifier.fillMaxSize().padding(VolaFirstRunTokens.screenPadding),
            verticalArrangement = Arrangement.spacedBy(VolaFirstRunTokens.buttonGap),
        ) {
            // The story scrolls on a small screen; the two buttons stay under the thumb.
            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(VolaFirstRunTokens.textGap),
            ) {
                WelcomeHero(modifier = Modifier.fillMaxWidth())
                Text(
                    text = stringResource(R.string.first_run_welcome_title),
                    modifier = Modifier.semantics { heading() },
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.first_run_welcome_body),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                StepDots(current = 0)
            }
            Button(
                onClick = onStart,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(VolaFirstRunTokens.buttonHeight)
                    .testTag(FirstRunTestTags.Start),
            ) {
                Text(stringResource(R.string.first_run_start))
            }
            OutlinedButton(
                onClick = onImport,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(VolaFirstRunTokens.buttonHeight)
                    .testTag(FirstRunTestTags.Import),
                contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
            ) {
                Icon(VolaIcons.UploadFile, contentDescription = null)
                Text(
                    text = stringResource(R.string.first_run_import),
                    modifier = Modifier.padding(start = VolaFirstRunTokens.buttonIconGap),
                )
            }
        }
    }
}

/**
 * Board W-Setup: the look, the two protections that are on from the start, the gesture lesson,
 * and Vola as the default browser. Nothing here is required; «Next» takes what is set.
 */
@Composable
internal fun FirstRunSetupScreen(
    setup: FirstRunSetup,
    onAppearanceModeChange: (BrowserAppearanceMode) -> Unit,
    onTrackerProtectionChange: (Boolean) -> Unit,
    onHttpsOnlyChange: (Boolean) -> Unit,
    onShowGesturesChange: (Boolean) -> Unit,
    onMakeDefault: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FirstRunBackground(modifier.testTag(FirstRunTestTags.Setup)) {
        Column(
            modifier = Modifier.fillMaxSize().padding(VolaFirstRunTokens.screenPadding),
            verticalArrangement = Arrangement.spacedBy(VolaFirstRunTokens.buttonGap),
        ) {
            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(VolaFirstRunTokens.sectionGap),
            ) {
                StepProgress(current = 1)
                Text(
                    text = stringResource(R.string.first_run_setup_title),
                    modifier = Modifier.semantics { heading() },
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Row(
                    modifier = Modifier.fillMaxWidth().selectableGroup(),
                    horizontalArrangement = Arrangement.spacedBy(VolaFirstRunTokens.themeTileGap),
                ) {
                    listOf(BrowserAppearanceMode.Light, BrowserAppearanceMode.Dark, BrowserAppearanceMode.System)
                        .forEach { mode ->
                            ThemeTile(
                                mode = mode,
                                selected = setup.appearanceMode == mode,
                                onSelect = { onAppearanceModeChange(mode) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                }
                SetupCard {
                    SwitchRow(
                        icon = VolaIcons.VerifiedUser,
                        title = stringResource(R.string.first_run_trackers),
                        summary = stringResource(if (setup.trackerProtection) R.string.first_run_trackers_on else R.string.first_run_trackers_off),
                        checked = setup.trackerProtection,
                        testTag = FirstRunTestTags.Trackers,
                        onCheckedChange = onTrackerProtectionChange,
                    )
                    setup.httpsOnly?.let { httpsOnly ->
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        SwitchRow(
                            icon = VolaIcons.Lock,
                            title = stringResource(R.string.first_run_https),
                            summary = stringResource(if (httpsOnly) R.string.first_run_https_on else R.string.first_run_https_off),
                            checked = httpsOnly,
                            testTag = FirstRunTestTags.HttpsOnly,
                            onCheckedChange = onHttpsOnlyChange,
                        )
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    SwitchRow(
                        icon = VolaIcons.Swipe,
                        title = stringResource(R.string.first_run_gestures),
                        summary = stringResource(R.string.first_run_gestures_summary),
                        checked = setup.showGestures,
                        testTag = FirstRunTestTags.Gestures,
                        onCheckedChange = onShowGesturesChange,
                    )
                }
                SetupCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(VolaFirstRunTokens.rowPadding),
                        horizontalArrangement = Arrangement.spacedBy(VolaFirstRunTokens.rowGap),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(
                                    if (setup.isDefaultBrowser) R.string.first_run_default_done else R.string.first_run_default,
                                ),
                                style = MaterialTheme.typography.titleSmall,
                            )
                            if (!setup.isDefaultBrowser) {
                                Text(
                                    text = stringResource(R.string.first_run_default_summary),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        if (setup.isDefaultBrowser) {
                            Icon(VolaIcons.CheckCircle, contentDescription = null, tint = VolaTheme.extendedColors.ok)
                        } else {
                            FilledTonalButton(
                                onClick = onMakeDefault,
                                modifier = Modifier.testTag(FirstRunTestTags.DefaultBrowser),
                            ) {
                                Text(stringResource(R.string.first_run_default_action))
                            }
                        }
                    }
                }
            }
            Button(
                onClick = onNext,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(VolaFirstRunTokens.buttonHeight)
                    .testTag(FirstRunTestTags.Next),
            ) {
                Text(stringResource(R.string.first_run_next))
            }
        }
    }
}

/**
 * A soft glow of the accent from the top, over the theme's background (pure black in dark); the
 * content sits inside the safe area. Shared with the gesture lesson, so the whole first run looks
 * the same.
 */
@Composable
internal fun FirstRunBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .background(
                Brush.verticalGradient(
                    listOf(colors.primaryContainer.copy(alpha = VolaFirstRunTokens.GLOW_ALPHA), Color.Transparent),
                ),
            )
            .safeDrawingPadding(),
    ) {
        content()
    }
}

/** Three pages leaning on each other: a video space, a reading space, a list. */
@Composable
private fun WelcomeHero(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.height(VolaFirstRunTokens.heroHeight),
        contentAlignment = Alignment.Center,
    ) {
        HeroPage(WorkspaceAccent.Coral, offset = -VolaFirstRunTokens.heroPageOffset, rotation = -VolaFirstRunTokens.HERO_SIDE_ROTATION)
        HeroPage(WorkspaceAccent.Violet, offset = VolaFirstRunTokens.heroPageOffset, rotation = VolaFirstRunTokens.HERO_SIDE_ROTATION)
        HeroPage(WorkspaceAccent.Teal, offset = null, rotation = 0f)
    }
}

@Composable
private fun HeroPage(accent: WorkspaceAccent, offset: Dp?, rotation: Float) {
    val side = offset != null
    val tint = accent.color()
    Surface(
        modifier = Modifier
            .then(if (offset != null) Modifier.offset(x = offset, y = VolaFirstRunTokens.heroPageLift) else Modifier)
            .graphicsLayer {
                rotationZ = rotation
                if (side) {
                    scaleX = VolaFirstRunTokens.HERO_SIDE_SCALE
                    scaleY = VolaFirstRunTokens.HERO_SIDE_SCALE
                }
            }
            .size(VolaFirstRunTokens.heroPageWidth, VolaFirstRunTokens.heroPageHeight),
        shape = VolaFirstRunTokens.heroPageShape,
        color = VolaTheme.extendedColors.card,
        shadowElevation = VolaFirstRunTokens.heroPageLift,
    ) {
        Column(
            modifier = Modifier.padding(VolaFirstRunTokens.heroPagePadding),
            verticalArrangement = Arrangement.spacedBy(VolaFirstRunTokens.heroLineGap),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(VolaFirstRunTokens.heroImageHeight)
                    .clip(VolaFirstRunTokens.themeTileShape)
                    .background(Brush.linearGradient(listOf(tint, tint.copy(alpha = VolaFirstRunTokens.GLOW_ALPHA)))),
            )
            listOf(1f, 0.8f, 0.9f, 0.6f).forEach { fraction ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction)
                        .height(VolaFirstRunTokens.heroLineHeight)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = VolaFirstRunTokens.HERO_LINE_ALPHA)),
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Row(
                horizontalArrangement = Arrangement.spacedBy(VolaFirstRunTokens.heroLineGap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(modifier = Modifier.size(VolaFirstRunTokens.heroImageHeight / 3).clip(CircleShape).background(tint))
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(VolaFirstRunTokens.heroImageHeight / 3)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = VolaFirstRunTokens.HERO_LINE_ALPHA)),
                )
            }
        }
    }
}

@Composable
private fun StepDots(current: Int) {
    val description = stringResource(R.string.first_run_step, current + 1, FIRST_RUN_STEPS)
    Row(
        modifier = Modifier.semantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(VolaFirstRunTokens.dotGap),
    ) {
        repeat(FIRST_RUN_STEPS) { index ->
            Box(
                modifier = Modifier
                    .size(
                        width = if (index == current) VolaFirstRunTokens.dotActiveWidth else VolaFirstRunTokens.dotSize,
                        height = VolaFirstRunTokens.dotSize,
                    )
                    .clip(CircleShape)
                    .background(
                        if (index == current) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                    ),
            )
        }
    }
}

@Composable
private fun StepProgress(current: Int) {
    val description = stringResource(R.string.first_run_step, current + 1, FIRST_RUN_STEPS)
    Row(
        modifier = Modifier.fillMaxWidth().semantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(VolaFirstRunTokens.progressGap),
    ) {
        repeat(FIRST_RUN_STEPS) { index ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(VolaFirstRunTokens.progressHeight)
                    .clip(CircleShape)
                    .background(
                        if (index <= current) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                    ),
            )
        }
    }
}

@Composable
private fun ThemeTile(
    mode: BrowserAppearanceMode,
    selected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = stringResource(
        when (mode) {
            BrowserAppearanceMode.Light -> R.string.first_run_theme_light
            BrowserAppearanceMode.Dark -> R.string.first_run_theme_dark
            BrowserAppearanceMode.System -> R.string.first_run_theme_auto
        },
    )
    Column(
        modifier = modifier
            .selectable(selected = selected, role = Role.RadioButton, onClick = onSelect)
            .testTag(FirstRunTestTags.theme(mode)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(VolaFirstRunTokens.themeLabelGap),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(VolaFirstRunTokens.themeTileHeight)
                .clip(VolaFirstRunTokens.themeTileShape)
                .border(
                    width = VolaFirstRunTokens.themeTileRing,
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                    shape = VolaFirstRunTokens.themeTileShape,
                ),
        ) {
            when (mode) {
                BrowserAppearanceMode.Light -> MiniPhone(light = true, modifier = Modifier.fillMaxSize())
                BrowserAppearanceMode.Dark -> MiniPhone(light = false, modifier = Modifier.fillMaxSize())
                BrowserAppearanceMode.System -> Row(modifier = Modifier.fillMaxSize()) {
                    MiniPhone(light = true, modifier = Modifier.weight(1f).fillMaxSize())
                    MiniPhone(light = false, modifier = Modifier.weight(1f).fillMaxSize())
                }
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        )
    }
}

/** A tiny page in the light or the dark look, the same in either app theme. */
@Composable
private fun MiniPhone(light: Boolean, modifier: Modifier = Modifier) {
    val background = if (light) VolaFirstRunTokens.themeLight else VolaFirstRunTokens.themeDark
    val line = if (light) VolaFirstRunTokens.themeLightLine else VolaFirstRunTokens.themeDarkLine
    Column(
        modifier = modifier
            .background(background)
            .padding(VolaFirstRunTokens.themeTilePadding),
        verticalArrangement = Arrangement.spacedBy(VolaFirstRunTokens.heroLineGap),
    ) {
        Box(modifier = Modifier.fillMaxWidth(0.7f).height(VolaFirstRunTokens.themeTileLineHeight).clip(CircleShape).background(line))
        Box(modifier = Modifier.fillMaxWidth().height(VolaFirstRunTokens.themeTileBlockHeight).clip(VolaFirstRunTokens.heroPageShape).background(line))
        Box(modifier = Modifier.fillMaxWidth(0.8f).height(VolaFirstRunTokens.themeTileLineHeight).clip(CircleShape).background(line))
    }
}

@Composable
private fun SetupCard(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = VolaFirstRunTokens.cardShape,
        color = VolaTheme.extendedColors.card,
    ) {
        Column(modifier = Modifier.padding(VolaFirstRunTokens.cardPadding)) { content() }
    }
}

@Composable
private fun SwitchRow(
    icon: ImageVector,
    title: String,
    summary: String,
    checked: Boolean,
    testTag: String,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = VolaFirstRunTokens.buttonHeight)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(VolaFirstRunTokens.rowPadding)
            .testTag(testTag),
        horizontalArrangement = Arrangement.spacedBy(VolaFirstRunTokens.rowGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = null)
    }
}

/** Board W-Welcome. */
@VolaPreviews
@Composable
private fun FirstRunWelcomePreview() {
    MaterialBrowserTheme(settings = AppearanceSettings(appearanceMode = BrowserAppearanceMode.System)) {
        FirstRunWelcomeScreen(onStart = {}, onImport = {})
    }
}

/** Board W-Setup. */
@VolaPreviews
@Composable
private fun FirstRunSetupPreview() {
    MaterialBrowserTheme(settings = AppearanceSettings(appearanceMode = BrowserAppearanceMode.System)) {
        FirstRunSetupScreen(
            setup = FirstRunSetup(
                appearanceMode = BrowserAppearanceMode.System,
                trackerProtection = true,
                httpsOnly = true,
                showGestures = true,
                isDefaultBrowser = false,
            ),
            onAppearanceModeChange = {},
            onTrackerProtectionChange = {},
            onHttpsOnlyChange = {},
            onShowGesturesChange = {},
            onMakeDefault = {},
            onNext = {},
        )
    }
}
