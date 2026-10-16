package dev.sk2andy.materialbrowser.ui.v5

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Spacing
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Theme

object V5PromptSheetTestTags {
    const val Message = "v5_prompt_message"
    fun action(index: Int) = "v5_prompt_action:$index"
}

/** How an action of a prompt reads: the one it suggests, an ordinary one, or one that removes. */
enum class V5PromptRole { Primary, Default, Destructive }

@Immutable
data class V5PromptAction(
    val label: String,
    val onClick: () -> Unit,
    val role: V5PromptRole = V5PromptRole.Default,
    /** The tag the current dialog's button had, so tests find the action under v5 too. */
    val testTag: String? = null,
)

/**
 * One form for every question the browser asks (V28, B14 and B9; system-v5.md section 9): a sheet
 * with the title, the message in full, and its actions as rows, the suggested one in the accent
 * and a removing one in red. «Close» in the header and a swipe down dismiss it, so no «Cancel» row
 * is needed.
 */
@Composable
fun V5PromptSheet(
    visible: Boolean,
    title: String,
    /** Null when the actions say it all, as a choice between apps does. */
    message: String?,
    actions: List<V5PromptAction>,
    onDismiss: () -> Unit,
    /** The tag the current dialog had. */
    testTag: String? = null,
    /** What the question asks to fill in or pick, between the message and the actions. */
    content: @Composable ColumnScope.() -> Unit = {},
) {
    val colors = V5Theme.colors
    V5WindowSheet(visible = visible, onDismiss = onDismiss, title = title) {
        V5SheetHeader(
            title = title,
            onClose = onDismiss,
            modifier = if (testTag != null) Modifier.testTag(testTag) else Modifier,
        )
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(top = V5Spacing.space2, bottom = V5Spacing.space4),
            verticalArrangement = Arrangement.spacedBy(V5Spacing.space4),
        ) {
            if (message != null) {
                Text(
                    message,
                    style = V5Theme.type.body,
                    color = colors.labelSecondary,
                    modifier = Modifier
                        .padding(horizontal = V5Spacing.space4)
                        .testTag(V5PromptSheetTestTags.Message),
                )
            }
            content()
            V5ListGroup {
                actions.forEachIndexed { index, action ->
                    V5ListRow(
                        title = action.label,
                        titleColor = when (action.role) {
                            V5PromptRole.Primary -> colors.accent
                            V5PromptRole.Default -> colors.label
                            V5PromptRole.Destructive -> colors.danger
                        },
                        onClick = action.onClick,
                        modifier = Modifier.testTag(action.testTag ?: V5PromptSheetTestTags.action(index)),
                    )
                }
            }
        }
    }
}
