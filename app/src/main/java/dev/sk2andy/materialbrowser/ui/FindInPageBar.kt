package dev.sk2andy.materialbrowser.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.progressSemantics
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.sk2andy.materialbrowser.browser.FindInPageOptions
import dev.sk2andy.materialbrowser.data.AppearanceSettings
import dev.sk2andy.materialbrowser.data.BrowserAddressBarStyle
import dev.sk2andy.materialbrowser.data.BrowserAppearanceMode
import dev.sk2andy.materialbrowser.shared.ui.icons.VolaIcons
import dev.sk2andy.materialbrowser.ui.theme.MaterialBrowserTheme
import dev.sk2andy.materialbrowser.ui.theme.VolaElevation
import dev.sk2andy.materialbrowser.ui.theme.VolaPreviews
import dev.sk2andy.materialbrowser.ui.theme.VolaSpacing
import dev.sk2andy.materialbrowser.ui.theme.VolaTheme
import dev.sk2andy.materialbrowser.ui.theme.auraBrush

/** Accent ring of the find field while it is open (`box-shadow: 0 0 0 2px var(--pri)`). */
private val FIND_BAR_RING_WIDTH = 2.dp
private val FIND_BAR_COUNT_MIN_WIDTH = 52.dp
private val FIND_BAR_PROGRESS_SIZE = 18.dp

/**
 * Find in page as the v4 Find board draws it: a card capsule in the thumb zone with the query,
 * the match count and the previous, next and close actions. Match case and whole word sit above
 * it when the engine supports them.
 */
@Composable
internal fun FindInPageBar(
    query: String,
    onQueryChange: (String) -> Unit,
    matchText: String,
    isCounting: Boolean,
    canNavigate: Boolean,
    focusNonce: Int,
    autoFocus: Boolean,
    placeholder: String,
    queryContentDescription: String,
    countingContentDescription: String,
    previousMatchContentDescription: String,
    nextMatchContentDescription: String,
    closeContentDescription: String,
    onPreviousMatch: () -> Unit,
    onNextMatch: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    options: FindInPageOptions = FindInPageOptions(),
    optionsAvailable: Boolean = false,
    matchCaseLabel: String = "",
    wholeWordLabel: String = "",
    onOptionsChange: (FindInPageOptions) -> Unit = {},
) {
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val colors = MaterialTheme.colorScheme
    LaunchedEffect(autoFocus, focusNonce) {
        if (autoFocus) {
            withFrameNanos { }
            focusRequester.requestFocus()
            keyboard?.show()
        }
    }

    Column(
        modifier = modifier
            .widthIn(max = 600.dp)
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(VolaSpacing.x2),
    ) {
        if (optionsAvailable) {
            Row(horizontalArrangement = Arrangement.spacedBy(VolaSpacing.x2)) {
                FindOptionChip(
                    label = matchCaseLabel,
                    selected = options.matchCase,
                    onClick = { onOptionsChange(options.copy(matchCase = !options.matchCase)) },
                    modifier = Modifier.testTag(FindInPageBarTestTags.MatchCase),
                )
                FindOptionChip(
                    label = wholeWordLabel,
                    selected = options.wholeWord,
                    onClick = { onOptionsChange(options.copy(wholeWord = !options.wholeWord)) },
                    modifier = Modifier.testTag(FindInPageBarTestTags.WholeWord),
                )
            }
        }
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(addressBarExpandedHeight(BrowserAddressBarStyle.Classic))
                .border(FIND_BAR_RING_WIDTH, colors.primary, CircleShape)
                .testTag(FindInPageBarTestTags.Bar),
            shape = CircleShape,
            color = VolaTheme.extendedColors.card,
            contentColor = colors.onSurface,
            shadowElevation = VolaElevation.level2,
        ) {
            Row(
                modifier = Modifier.padding(start = VolaSpacing.x4, end = VolaSpacing.x1),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = VolaIcons.Search,
                    contentDescription = null,
                    tint = colors.onSurfaceVariant,
                )
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = VolaSpacing.x3)
                        .focusRequester(focusRequester)
                        .semantics { contentDescription = queryContentDescription }
                        .testTag(FindInPageBarTestTags.Query),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.titleMedium.copy(
                        color = colors.onSurface,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    cursorBrush = SolidColor(colors.primary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = { if (canNavigate) onNextMatch() },
                    ),
                    decorationBox = { innerTextField ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (query.isEmpty()) {
                                Text(
                                    text = placeholder,
                                    color = colors.onSurfaceVariant,
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            innerTextField()
                        }
                    },
                )
                Box(
                    modifier = Modifier.widthIn(min = FIND_BAR_COUNT_MIN_WIDTH),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isCounting) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(FIND_BAR_PROGRESS_SIZE)
                                .progressSemantics()
                                .semantics {
                                    contentDescription = countingContentDescription
                                    liveRegion = LiveRegionMode.Polite
                                }
                                .testTag(FindInPageBarTestTags.Progress),
                            strokeWidth = 2.dp,
                            color = colors.primary,
                        )
                    } else {
                        Text(
                            text = matchText,
                            modifier = Modifier
                                .semantics { liveRegion = LiveRegionMode.Polite }
                                .testTag(FindInPageBarTestTags.MatchCount),
                            color = colors.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
                IconButton(
                    onClick = onPreviousMatch,
                    enabled = canNavigate,
                    modifier = Modifier.testTag(FindInPageBarTestTags.Previous),
                ) {
                    Icon(
                        imageVector = VolaIcons.KeyboardArrowUp,
                        contentDescription = previousMatchContentDescription,
                    )
                }
                IconButton(
                    onClick = onNextMatch,
                    enabled = canNavigate,
                    modifier = Modifier.testTag(FindInPageBarTestTags.Next),
                ) {
                    Icon(
                        imageVector = VolaIcons.KeyboardArrowDown,
                        contentDescription = nextMatchContentDescription,
                    )
                }
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.testTag(FindInPageBarTestTags.Close),
                ) {
                    Icon(
                        imageVector = VolaIcons.Close,
                        contentDescription = closeContentDescription,
                    )
                }
            }
        }
    }
}

@Composable
private fun FindOptionChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        modifier = modifier,
        leadingIcon = if (selected) {
            {
                Icon(
                    imageVector = VolaIcons.Check,
                    contentDescription = null,
                    modifier = Modifier.size(FilterChipDefaults.IconSize),
                )
            }
        } else {
            null
        },
        colors = FilterChipDefaults.filterChipColors(
            containerColor = VolaTheme.extendedColors.card,
            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
            selectedLeadingIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
        elevation = FilterChipDefaults.filterChipElevation(elevation = VolaElevation.level1),
    )
}

internal object FindInPageBarTestTags {
    const val Bar = "find_in_page_bar"
    const val Query = "find_in_page_query"
    const val Progress = "find_in_page_progress"
    const val MatchCount = "find_in_page_match_count"
    const val Previous = "find_in_page_previous"
    const val Next = "find_in_page_next"
    const val Close = "find_in_page_close"
    const val MatchCase = "find_in_page_match_case"
    const val WholeWord = "find_in_page_whole_word"
}

@Composable
private fun FindInPageBarPreviewContent(mode: BrowserAppearanceMode) {
    MaterialBrowserTheme(settings = AppearanceSettings(appearanceMode = mode)) {
        Box(
            modifier = Modifier
                .background(VolaTheme.auraBrush)
                .padding(VolaSpacing.x2),
        ) {
            FindInPageBar(
                query = "лёд",
                onQueryChange = {},
                matchText = "2 / 7",
                isCounting = false,
                canNavigate = true,
                focusNonce = 0,
                autoFocus = false,
                placeholder = "Find on page",
                queryContentDescription = "",
                countingContentDescription = "",
                previousMatchContentDescription = "",
                nextMatchContentDescription = "",
                closeContentDescription = "",
                onPreviousMatch = {},
                onNextMatch = {},
                onClose = {},
                options = FindInPageOptions(wholeWord = true),
                optionsAvailable = true,
                matchCaseLabel = "Match case",
                wholeWordLabel = "Whole word",
            )
        }
    }
}

@VolaPreviews
@Composable
private fun FindInPageBarPreview() {
    FindInPageBarPreviewContent(BrowserAppearanceMode.System)
}
