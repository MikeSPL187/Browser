package dev.sk2andy.materialbrowser.ui.v5

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.shared.ui.icons.VolaIcons
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Elevation
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Motion
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Sizes
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Spacing
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Theme
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Tile
import dev.sk2andy.materialbrowser.ui.theme.v5.v5Elevation

object V5TranslateOfferTestTags {
    const val Card = "v5_translate_offer"
    const val Translate = "v5_translate_offer_translate"
    const val NeverOnSite = "v5_translate_offer_never_site"
    const val Close = "v5_translate_offer_close"
}

/** The offer's languages, named as the app language writes them: «русский», «английский». */
@Immutable
data class V5TranslateOfferState(val to: String, val from: String)

/**
 * Vola's own offer to translate the page (canvas «Translate»): a card above the bar, «Translate
 * into Russian?» over the page's language, «Translate» and «Not for this site», and a line that
 * the text stays on the phone. Close puts it away for this page only. It slides up from the bar
 * on the sheet spring; without animations it only fades.
 */
@Composable
fun V5TranslateOffer(
    state: V5TranslateOfferState?,
    onTranslate: () -> Unit,
    onNeverOnSite: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // The last state stays on screen while the card leaves.
    val last = remember { LastOffer() }
    if (state != null) last.state = state
    val animated = V5Motion.animationsEnabled
    AnimatedVisibility(
        visible = state != null,
        modifier = modifier,
        enter = if (animated) {
            slideInVertically(V5Motion.sheet.spec()) { height -> height / 2 } + fadeIn(V5Motion.effects.spec())
        } else {
            fadeIn(V5Motion.reducedFade())
        },
        exit = if (animated) {
            slideOutVertically(V5Motion.sheet.spec()) { height -> height / 2 } + fadeOut(V5Motion.effects.spec())
        } else {
            fadeOut(V5Motion.reducedFade())
        },
    ) {
        last.state?.let { V5TranslateOfferCard(it, onTranslate, onNeverOnSite, onClose) }
    }
}

private class LastOffer {
    var state: V5TranslateOfferState? = null
}

@Composable
internal fun V5TranslateOfferCard(
    state: V5TranslateOfferState,
    onTranslate: () -> Unit,
    onNeverOnSite: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = V5Theme.colors
    val shape = V5Theme.shapes.l
    val paneTitle = stringResource(R.string.v5_translate_offer_region)
    Column(
        modifier
            // Above the bar, as the card that hides a chosen block sits (V5DistractionChoice).
            .navigationBarsPadding()
            .padding(horizontal = V5Spacing.space3)
            .padding(bottom = V5Sizes.bar + V5Spacing.space4)
            .fillMaxWidth()
            .v5Elevation(V5Elevation.e3, shape)
            .clip(shape)
            .background(colors.surface)
            .semantics { this.paneTitle = paneTitle }
            .padding(start = V5Spacing.space4, end = V5Spacing.space2, top = V5Spacing.space2, bottom = V5Spacing.space3)
            .testTag(V5TranslateOfferTestTags.Card),
        verticalArrangement = Arrangement.spacedBy(V5Spacing.space3),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(V5Spacing.space3)) {
            Box(
                Modifier
                    .size(V5Sizes.cardTile)
                    .clip(V5Theme.shapes.s)
                    .background(V5Tile.Teal.color),
                contentAlignment = Alignment.Center,
            ) {
                Icon(VolaIcons.Language, contentDescription = null, tint = V5Tile.Teal.glyph, modifier = Modifier.size(V5Sizes.iconSmall))
            }
            Column(Modifier.weight(1f).semantics(mergeDescendants = true) {}) {
                Text(stringResource(R.string.v5_translate_offer_title, state.to), style = V5Theme.type.headline, color = colors.label)
                Text(
                    stringResource(R.string.v5_translate_offer_page_language, state.from),
                    style = V5Theme.type.footnote,
                    color = colors.labelSecondary,
                )
            }
            IconButton(onClick = onClose, modifier = Modifier.testTag(V5TranslateOfferTestTags.Close)) {
                Icon(
                    VolaIcons.Close,
                    contentDescription = stringResource(R.string.v5_translate_offer_close),
                    tint = colors.labelSecondary,
                    modifier = Modifier.size(V5Sizes.iconSmall),
                )
            }
        }
        Row(
            Modifier.padding(end = V5Spacing.space2),
            horizontalArrangement = Arrangement.spacedBy(V5Spacing.space2),
        ) {
            V5StatePrimaryAction(
                V5StateAction(stringResource(R.string.v5_translate_action), onTranslate, testTag = V5TranslateOfferTestTags.Translate),
                modifier = Modifier.weight(1f),
            )
            Box(
                Modifier
                    .weight(1f)
                    .heightIn(min = V5Sizes.touchTarget)
                    .clip(V5Theme.shapes.full)
                    .background(colors.fill)
                    .clickable(role = Role.Button, onClick = onNeverOnSite)
                    .padding(horizontal = V5Spacing.space3, vertical = V5Spacing.space2)
                    .testTag(V5TranslateOfferTestTags.NeverOnSite),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    stringResource(R.string.v5_translate_offer_never_site),
                    style = V5Theme.type.callout.copy(fontWeight = FontWeight.Medium),
                    color = colors.label,
                    textAlign = TextAlign.Center,
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(V5Spacing.space1)) {
            Icon(VolaIcons.Lock, contentDescription = null, tint = colors.labelSecondary, modifier = Modifier.size(V5Sizes.iconSmall))
            Text(stringResource(R.string.v5_translate_offer_note), style = V5Theme.type.caption, color = colors.labelSecondary)
        }
    }
}
