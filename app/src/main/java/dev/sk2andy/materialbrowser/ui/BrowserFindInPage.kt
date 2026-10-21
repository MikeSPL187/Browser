package dev.sk2andy.materialbrowser.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.zIndex
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.browser.BrowserController
import dev.sk2andy.materialbrowser.browser.FindInPageMatchPosition
import dev.sk2andy.materialbrowser.browser.FindInPageRules
import dev.sk2andy.materialbrowser.browser.FindInPageState
import dev.sk2andy.materialbrowser.ui.theme.VolaSpacing
import dev.sk2andy.materialbrowser.ui.theme.v5.VolaV5Theme
import dev.sk2andy.materialbrowser.ui.v5.V5FindInPage
import dev.sk2andy.materialbrowser.ui.v5.V5FindInPageLabels

/**
 * Find in page in the bar's place, above the keyboard: under v5 on a phone the capsule of canvas
 * screen «с1» (V28, B5), otherwise the current find bar. v5 leaves out the match-case and
 * whole-word options (principle 2): Safari's find has neither.
 */
@Composable
internal fun BrowserFindInPage(
    controller: BrowserController,
    findState: FindInPageState,
    isPrivate: Boolean,
    fullWindowHeightPx: Int,
    rootBottomInWindowPx: Int,
    modifier: Modifier = Modifier,
) {
    val matchPosition = FindInPageRules.displayPosition(findState)
    val isCounting = findState.query.isNotEmpty() && !findState.isDoneCounting
    val canNavigate = FindInPageRules.canNavigate(findState)
    val placement = modifier
        .addressBarWindowInsetsPadding(
            fullWindowHeightPx = fullWindowHeightPx,
            rootBottomInWindowPx = rootBottomInWindowPx,
            imeInsets = WindowInsets.ime,
            navigationBarInsets = WindowInsets.addressBarEdges,
        )
    if (rememberDrawsV5()) {
        val dark = MaterialTheme.colorScheme.background.luminance() < FIND_DARK_LUMINANCE
        VolaV5Theme(dark = dark, accent = controller.v5Accent, privateBrowsing = isPrivate) {
            V5FindInPage(
                query = findState.query,
                onQueryChange = controller::updateFindInPageQuery,
                matchText = v5FindInPageMatchText(findState, matchPosition),
                isCounting = isCounting,
                canNavigate = canNavigate,
                labels = V5FindInPageLabels(
                    placeholder = stringResource(R.string.action_find_in_page),
                    counting = stringResource(R.string.cd_find_in_page_counting),
                    previous = stringResource(R.string.cd_find_in_page_previous),
                    next = stringResource(R.string.cd_find_in_page_next),
                    done = stringResource(R.string.action_done),
                    clear = stringResource(R.string.v5_editor_clear),
                ),
                onPrevious = { controller.findNextInPage(forward = false) },
                onNext = { controller.findNextInPage(forward = true) },
                onDone = controller::closeFindInPage,
                modifier = placement.padding(vertical = VolaSpacing.x2).zIndex(FIND_Z_INDEX),
            )
        }
        return
    }
    FindInPageBar(
        query = findState.query,
        onQueryChange = controller::updateFindInPageQuery,
        matchText = findInPageMatchText(matchPosition),
        isCounting = isCounting,
        canNavigate = canNavigate,
        focusNonce = 0,
        autoFocus = true,
        placeholder = stringResource(R.string.action_find_in_page),
        queryContentDescription = stringResource(R.string.cd_find_in_page_query),
        countingContentDescription = stringResource(R.string.cd_find_in_page_counting),
        previousMatchContentDescription = stringResource(R.string.cd_find_in_page_previous),
        nextMatchContentDescription = stringResource(R.string.cd_find_in_page_next),
        closeContentDescription = stringResource(R.string.cd_find_in_page_close),
        onPreviousMatch = { controller.findNextInPage(forward = false) },
        onNextMatch = { controller.findNextInPage(forward = true) },
        onClose = controller::closeFindInPage,
        options = findState.options,
        optionsAvailable = controller.supportsFindInPageOptions,
        matchCaseLabel = stringResource(R.string.find_in_page_match_case),
        wholeWordLabel = stringResource(R.string.find_in_page_whole_word),
        onOptionsChange = controller::updateFindInPageOptions,
        isPrivate = isPrivate,
        // The find bar takes the island's place in the thumb zone, above the keyboard.
        modifier = placement
            .padding(horizontal = VolaSpacing.x2, vertical = ADDRESS_BAR_VERTICAL_MARGIN)
            .zIndex(FIND_Z_INDEX),
    )
}

/** «2 of 7» in words, as TalkBack reads it, and «Not found» instead of "0/0"; empty without a query. */
@Composable
private fun v5FindInPageMatchText(state: FindInPageState, position: FindInPageMatchPosition): String = when {
    state.query.isBlank() -> ""
    FindInPageRules.isNotFound(state) -> stringResource(R.string.v5_find_in_page_not_found)
    position.isCountKnown -> stringResource(
        R.string.v5_find_in_page_match_count,
        position.activeMatchNumber,
        position.matchCount,
    )
    else -> stringResource(R.string.v5_find_in_page_match_count_unknown, position.activeMatchNumber)
}

private const val FIND_DARK_LUMINANCE = 0.5f
private const val FIND_Z_INDEX = 25f
