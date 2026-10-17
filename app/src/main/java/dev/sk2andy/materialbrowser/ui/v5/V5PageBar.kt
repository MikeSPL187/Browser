package dev.sk2andy.materialbrowser.ui.v5

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.shared.ui.icons.VolaIcons
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Sizes
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Spacing
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Theme
import dev.sk2andy.materialbrowser.ui.theme.v5.VolaV5Theme

/**
 * The bar on top of a v5 settings page: back in the accent, the title in the middle as a heading,
 * and the page's [actions] at the end. Every v5 page draws this one bar, the settings skin's pages
 * and the managers of extensions, userscripts and filters alike.
 */
@Composable
fun V5PageBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    backContentDescription: String = stringResource(R.string.action_back),
    actions: @Composable RowScope.() -> Unit = {},
) {
    val colors = V5Theme.colors
    Box(modifier.fillMaxWidth().heightIn(min = V5Sizes.touchTarget)) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.align(Alignment.CenterStart).padding(start = V5Spacing.space1),
        ) {
            Icon(VolaIcons.ArrowBack, contentDescription = backContentDescription, tint = colors.accent)
        }
        Text(
            title,
            style = V5Theme.type.headline,
            color = colors.label,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = V5Spacing.space12)
                .semantics { heading() },
        )
        Row(
            Modifier.align(Alignment.CenterEnd).padding(end = V5Spacing.space1),
            verticalAlignment = Alignment.CenterVertically,
            content = actions,
        )
    }
}

@Preview(name = "V5PageBar · light")
@Composable
private fun V5PageBarLightPreview() = V5PageBarPreview(dark = false)

@Preview(name = "V5PageBar · dark")
@Composable
private fun V5PageBarDarkPreview() = V5PageBarPreview(dark = true)

@Composable
private fun V5PageBarPreview(dark: Boolean) {
    VolaV5Theme(dark = dark) {
        V5PageBar(
            title = "Extensions",
            onBack = {},
            modifier = Modifier.background(V5Theme.colors.background).padding(V5Spacing.space2),
            actions = {
                IconButton(onClick = {}) {
                    Icon(VolaIcons.Add, contentDescription = null, tint = V5Theme.colors.accent)
                }
            },
        )
    }
}
