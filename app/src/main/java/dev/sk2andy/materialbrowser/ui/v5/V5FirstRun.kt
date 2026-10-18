package dev.sk2andy.materialbrowser.ui.v5

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.shared.ui.icons.VolaIcons
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Sizes
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Spacing
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Theme
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Tile

object V5FirstRunTestTags {
    const val Welcome = "v5_first_run_welcome"
    const val Start = "v5_first_run_start"
    const val Import = "v5_first_run_import"
    const val DefaultBrowser = "v5_first_run_default_browser"
    const val MakeDefault = "v5_first_run_make_default"
    const val NotNow = "v5_first_run_not_now"
    const val Gestures = "v5_first_run_gestures"
    const val GesturesDone = "v5_first_run_gestures_done"
}

/** One promise on the welcome: what Vola does without a single setting. */
@Immutable
data class V5FirstRunPromise(val icon: ImageVector, val tile: V5Tile, val title: String, val caption: String)

/**
 * Canvas screen «п1» (V28, B1): Vola's mark, its name, one line, three promises that hold without
 * settings, «Get started» and a quiet «Move from another browser». Nothing is asked here: protection
 * is on from the start and the theme follows the system.
 */
@Composable
fun V5FirstRunWelcome(
    promises: List<V5FirstRunPromise>,
    onStart: () -> Unit,
    onImport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = V5Theme.colors
    FirstRunPage(modifier.testTag(V5FirstRunTestTags.Welcome), glow = true) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top = V5Spacing.space16),
            verticalArrangement = Arrangement.spacedBy(V5Spacing.space2),
        ) {
            Box(
                Modifier
                    .padding(bottom = V5Spacing.space4)
                    .size(V5Sizes.firstRunMark)
                    .clip(V5Theme.shapes.l)
                    .background(colors.accentFill),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    VolaIcons.TravelExplore,
                    contentDescription = null,
                    tint = colors.onAccentFill,
                    modifier = Modifier.size(V5Sizes.firstRunMarkIcon),
                )
            }
            Text(
                stringResource(R.string.app_name),
                style = V5Theme.type.largeTitle,
                color = colors.label,
                modifier = Modifier.semantics { heading() },
            )
            Text(stringResource(R.string.v5_first_run_tagline), style = V5Theme.type.callout, color = colors.labelSecondary)
            Spacer(Modifier.height(V5Spacing.space4))
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(V5Theme.shapes.s)
                    .background(colors.surface),
            ) {
                promises.forEach { promise ->
                    V5ListRow(
                        title = promise.title,
                        subtitle = promise.caption,
                        icon = V5RowIcon(promise.icon, promise.tile),
                        modifier = Modifier.semantics(mergeDescendants = true) {},
                    )
                }
            }
        }
        FirstRunActions(
            primary = stringResource(R.string.first_run_start),
            primaryTag = V5FirstRunTestTags.Start,
            onPrimary = onStart,
            secondary = stringResource(R.string.first_run_import),
            secondaryTag = V5FirstRunTestTags.Import,
            onSecondary = onImport,
        )
    }
}

/**
 * Canvas screen «п2»: one question, «Open links in Vola?», answered by «Make default» or «Not now».
 * Not now loses nothing: the same switch is in settings.
 */
@Composable
fun V5FirstRunDefaultBrowser(
    onMakeDefault: () -> Unit,
    onNotNow: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = V5Theme.colors
    FirstRunPage(modifier.testTag(V5FirstRunTestTags.DefaultBrowser), glow = false) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(V5Spacing.space3, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            V5StateGlyph(VolaIcons.OpenInNew, Modifier.padding(bottom = V5Spacing.space3))
            Text(
                stringResource(R.string.v5_first_run_default_title),
                style = V5Theme.type.title,
                color = colors.label,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                stringResource(R.string.v5_first_run_default_body),
                style = V5Theme.type.callout,
                color = colors.labelSecondary,
                textAlign = TextAlign.Center,
            )
        }
        FirstRunActions(
            primary = stringResource(R.string.v5_first_run_default_action),
            primaryTag = V5FirstRunTestTags.MakeDefault,
            onPrimary = onMakeDefault,
            secondary = stringResource(R.string.v5_first_run_not_now),
            secondaryTag = V5FirstRunTestTags.NotNow,
            onSecondary = onNotNow,
        )
    }
}

/**
 * The page under both screens: the screen background with, on the welcome, the accent glow from the
 * top (as the new tab), clear of the system bars. A touch that misses the controls stops here and
 * never reaches the browser underneath.
 */
@Composable
private fun FirstRunPage(
    modifier: Modifier,
    glow: Boolean,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = V5Theme.colors
    BoxWithConstraints(
        modifier
            .fillMaxSize()
            .pointerInput(Unit) { awaitPointerEventScope { while (true) awaitPointerEvent() } }
            .background(colors.background),
    ) {
        if (glow) {
            val width = with(LocalDensity.current) { maxWidth.toPx() }
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(V5Sizes.newTabWash)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(colors.accentSoft, Color.Transparent),
                            center = Offset(width / 2f, 0f),
                            radius = width,
                        ),
                    ),
            )
        }
        Column(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = V5Spacing.space6, vertical = V5Spacing.space4),
            content = content,
        )
    }
}

/** The filled main action and the quiet text action under it: the state screens' own buttons. */
@Composable
private fun FirstRunActions(
    primary: String,
    primaryTag: String,
    onPrimary: () -> Unit,
    secondary: String,
    secondaryTag: String,
    onSecondary: () -> Unit,
) {
    V5StateActions(
        primary = V5StateAction(primary, onPrimary, testTag = primaryTag),
        secondary = listOf(V5StateAction(secondary, onSecondary, testTag = secondaryTag)),
        modifier = Modifier.padding(top = V5Spacing.space4),
    )
}

/**
 * Canvas screen «п3»: the two gestures of the v5 bar on a card over the browser, so the bar it
 * points at stays in sight under the scrim: a swipe along the bar switches tabs, a swipe up opens
 * the overview. One «Got it» closes it; there is nothing to practise.
 */
@Composable
fun V5FirstRunGestures(onDone: () -> Unit, modifier: Modifier = Modifier) {
    val colors = V5Theme.colors
    Box(
        modifier
            .fillMaxSize()
            .pointerInput(Unit) { awaitPointerEventScope { while (true) awaitPointerEvent() } }
            .background(colors.scrim)
            .testTag(V5FirstRunTestTags.Gestures),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            Modifier
                .statusBarsPadding()
                .navigationBarsPadding()
                // The card ends above the bar, which the two gestures are about.
                .padding(start = V5Spacing.space3, end = V5Spacing.space3, bottom = V5Sizes.bar + V5Spacing.space2)
                .fillMaxWidth()
                .clip(V5Theme.shapes.xl)
                .background(colors.surfaceElevated)
                // Landscape or a large font leaves less height than the card: it scrolls, so «Got
                // it» never gets squeezed out.
                .verticalScroll(rememberScrollState())
                .padding(V5Spacing.space4),
            verticalArrangement = Arrangement.spacedBy(V5Spacing.space3),
        ) {
            Text(
                stringResource(R.string.v5_first_run_gestures_title),
                style = V5Theme.type.title,
                color = colors.label,
                modifier = Modifier.padding(horizontal = V5Spacing.space1).semantics { heading() },
            )
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(V5Theme.shapes.s)
                    .background(colors.surface),
            ) {
                GestureRow(
                    rotation = 0f,
                    title = stringResource(R.string.v5_first_run_gesture_tabs_title),
                    caption = stringResource(R.string.v5_first_run_gesture_tabs_caption),
                )
                GestureRow(
                    // The same hand, turned to point up.
                    rotation = SWIPE_UP_ROTATION,
                    title = stringResource(R.string.v5_first_run_gesture_overview_title),
                    caption = stringResource(R.string.v5_first_run_gesture_overview_caption),
                )
            }
            V5StatePrimaryAction(
                V5StateAction(
                    label = stringResource(R.string.v5_first_run_gestures_done),
                    onClick = onDone,
                    testTag = V5FirstRunTestTags.GesturesDone,
                ),
            )
        }
    }
}

@Composable
private fun GestureRow(rotation: Float, title: String, caption: String) {
    val colors = V5Theme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = V5Sizes.rowThreeLine)
            .semantics(mergeDescendants = true) {}
            .padding(horizontal = V5Spacing.space4, vertical = V5Spacing.space2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(V5Spacing.space3),
    ) {
        Box(
            Modifier
                .size(V5Sizes.firstRunGesture)
                .clip(V5Theme.shapes.m)
                .background(colors.fill),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                VolaIcons.Swipe,
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier.size(V5Sizes.iconMax).rotate(rotation),
            )
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = V5Theme.type.body, color = colors.label)
            Text(caption, style = V5Theme.type.footnote, color = colors.labelSecondary)
        }
    }
}

private const val SWIPE_UP_ROTATION = -90f
