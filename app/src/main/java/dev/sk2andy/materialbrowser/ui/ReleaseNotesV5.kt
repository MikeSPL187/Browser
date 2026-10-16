package dev.sk2andy.materialbrowser.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.browser.ReleaseNotesBlock
import dev.sk2andy.materialbrowser.browser.ReleaseNotesDocument
import dev.sk2andy.materialbrowser.browser.ReleaseNotesInline
import dev.sk2andy.materialbrowser.shared.ui.icons.VolaIcons
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Sizes
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Spacing
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Theme
import dev.sk2andy.materialbrowser.ui.theme.v5.VolaV5Theme

/**
 * «What's new» under v5 on a phone (V28, B14b): Vola's mark as on the first run, the release's
 * title and version, the notes in v5 type on the grouped background, and «Done» filled in the
 * accent. The notes are parsed and their images loaded as on the current screen.
 */
@Composable
internal fun ReleaseNotesScreenV5(
    versionName: String,
    document: ReleaseNotesDocument,
    onDone: () -> Unit,
    onOpenLink: (String) -> Unit,
) {
    BackHandler(onBack = onDone)
    VolaV5Theme(dark = MaterialTheme.colorScheme.background.luminance() < RELEASE_NOTES_DARK_LUMINANCE) {
        val colors = V5Theme.colors
        Column(
            Modifier
                .fillMaxSize()
                .background(colors.background)
                .safeDrawingPadding()
                .testTag(ReleaseNotesTestTags.Screen),
        ) {
            ReleaseNotesHeaderV5(versionName = versionName, title = document.title.text, onClose = onDone)
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .testTag(ReleaseNotesTestTags.Content),
                contentPadding = PaddingValues(horizontal = V5Spacing.space6, vertical = V5Spacing.space4),
                verticalArrangement = Arrangement.spacedBy(V5Spacing.space4),
            ) {
                items(document.blocks.drop(1)) { block -> ReleaseNotesBlockV5(block, onOpenLink) }
            }
            Box(
                Modifier
                    .padding(horizontal = V5Spacing.space6, vertical = V5Spacing.space4)
                    .fillMaxWidth()
                    .heightIn(min = V5Sizes.touchTarget)
                    .clip(V5Theme.shapes.full)
                    .background(colors.accentFill)
                    .clickable(role = Role.Button, onClick = onDone)
                    .testTag(ReleaseNotesTestTags.Done),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    stringResource(R.string.release_notes_done),
                    style = V5Theme.type.body.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.onAccentFill,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = V5Spacing.space4, vertical = V5Spacing.space3),
                )
            }
        }
    }
}

@Composable
private fun ReleaseNotesHeaderV5(versionName: String, title: String, onClose: () -> Unit) {
    val colors = V5Theme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = V5Spacing.space6, end = V5Spacing.space1, top = V5Spacing.space4),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(V5Spacing.space4),
    ) {
        Box(
            Modifier
                .size(V5Sizes.privateGem)
                .clip(V5Theme.shapes.l)
                .background(colors.accentFill),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                VolaIcons.TravelExplore,
                contentDescription = null,
                tint = colors.onAccentFill,
                modifier = Modifier.size(V5Sizes.privateGemIcon),
            )
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(V5Spacing.space1)) {
            Text(
                stringResource(R.string.release_notes_eyebrow),
                style = V5Theme.type.footnote.copy(fontWeight = FontWeight.SemiBold),
                color = colors.accent,
            )
            Text(
                title,
                style = V5Theme.type.title,
                color = colors.label,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                stringResource(R.string.release_notes_version, versionName),
                style = V5Theme.type.footnote,
                color = colors.labelSecondary,
            )
        }
        IconButton(onClick = onClose, modifier = Modifier.testTag(ReleaseNotesTestTags.Close)) {
            Icon(
                VolaIcons.Close,
                contentDescription = stringResource(R.string.cd_close_release_notes),
                tint = colors.labelSecondary,
            )
        }
    }
}

@Composable
private fun ReleaseNotesBlockV5(block: ReleaseNotesBlock, onOpenLink: (String) -> Unit) {
    val colors = V5Theme.colors
    @Composable
    fun Markdown(content: ReleaseNotesInline, isHeading: Boolean = false, level: Int = 0, modifier: Modifier = Modifier) {
        MarkdownText(
            content = content,
            style = when {
                !isHeading -> V5Theme.type.body
                level <= 1 -> V5Theme.type.title
                else -> V5Theme.type.headline
            },
            onOpenLink = onOpenLink,
            modifier = if (isHeading) modifier.semantics { heading() } else modifier,
            textColor = colors.label,
            linkColor = colors.accent,
            codeBackground = colors.fill,
        )
    }
    when (block) {
        is ReleaseNotesBlock.Heading -> Markdown(block.content, isHeading = true, level = block.level)
        is ReleaseNotesBlock.Paragraph -> Markdown(block.content)
        is ReleaseNotesBlock.ListItems -> Column(verticalArrangement = Arrangement.spacedBy(V5Spacing.space2)) {
            block.items.forEachIndexed { index, item ->
                Row(verticalAlignment = Alignment.Top) {
                    Text(
                        text = if (block.ordered) "${index + 1}." else "•",
                        modifier = Modifier.width(V5Spacing.space6),
                        style = V5Theme.type.body.copy(fontWeight = FontWeight.SemiBold),
                        color = colors.accent,
                    )
                    Markdown(item, modifier = Modifier.weight(1f))
                }
            }
        }
        is ReleaseNotesBlock.Quote -> Row(
            Modifier
                .fillMaxWidth()
                .clip(V5Theme.shapes.s)
                .background(colors.surface)
                .padding(V5Spacing.space4),
        ) {
            Box(
                Modifier
                    .width(V5Spacing.space1)
                    .heightIn(min = V5Spacing.space6)
                    .clip(V5Theme.shapes.full)
                    .background(colors.accent),
            )
            Box(Modifier.width(V5Spacing.space3))
            Markdown(block.content, modifier = Modifier.weight(1f))
        }
        is ReleaseNotesBlock.Code -> Text(
            text = block.content,
            modifier = Modifier
                .fillMaxWidth()
                .clip(V5Theme.shapes.s)
                .background(colors.fill)
                .horizontalScroll(rememberScrollState())
                .padding(V5Spacing.space4),
            style = V5Theme.type.footnote,
            fontFamily = FontFamily.Monospace,
            color = colors.label,
        )
        is ReleaseNotesBlock.Image -> ReleaseNotesImage(
            block = block,
            container = colors.surface,
            altColor = colors.labelSecondary,
            shape = V5Theme.shapes.m,
        )
        ReleaseNotesBlock.Divider -> HorizontalDivider(thickness = V5Spacing.hairline, color = colors.separator)
    }
}

/** Below this background luminance the shell is dark. */
private const val RELEASE_NOTES_DARK_LUMINANCE = 0.5f

private val PreviewNotes = """
# Vola 0.4.0

## Библиотека
Избранное, история и загрузки — **одна** библиотека с поиском.

- Папки в избранном открываются на месте
- Свайп влево удаляет запись истории
- Загрузки показывают прогресс кольцом

> Всё по-прежнему хранится только на устройстве.

---

Подробнее — в [заметках к выпуску](https://github.com/MikeSPL187/Browser/releases).
""".trimIndent()

@androidx.compose.ui.tooling.preview.Preview(name = "What's new v5 · dark", widthDp = 390, heightDp = 844)
@Composable
private fun ReleaseNotesV5DarkPreview() {
    val document = dev.sk2andy.materialbrowser.browser.ReleaseNotesMarkdownRules.parse(PreviewNotes, "0.4.0") ?: return
    VolaV5Theme(dark = true) {
        ReleaseNotesScreenV5(versionName = "0.4.0", document = document, onDone = {}, onOpenLink = {})
    }
}
