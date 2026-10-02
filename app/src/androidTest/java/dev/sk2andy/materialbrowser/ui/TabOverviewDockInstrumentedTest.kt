package dev.sk2andy.materialbrowser.ui

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.sk2andy.materialbrowser.browser.BrowserProfile
import dev.sk2andy.materialbrowser.data.EssentialEntry
import dev.sk2andy.materialbrowser.shared.ui.TabOverviewChromeTestTags
import dev.sk2andy.materialbrowser.ui.theme.MaterialBrowserTheme
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TabOverviewDockInstrumentedTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val personal = BrowserProfile(id = "candy", emoji = "🏠", name = "Personal")
    private val work = BrowserProfile(id = "work", emoji = "💼", name = "Work")
    private val mail = EssentialEntry("https://mail.example.com/", "Mail")

    @Test
    fun workspacesEssentialsAndNewTabAnswerWithFullTouchTargets() {
        val selected = AtomicReference<String>()
        val edited = AtomicReference<String>()
        val opened = AtomicReference<EssentialEntry>()
        val adds = AtomicInteger()
        val newTabs = AtomicInteger()
        setDock(
            enabled = true,
            onSelect = selected::set,
            onLongClick = edited::set,
            onAdd = { adds.incrementAndGet() },
            onOpen = opened::set,
            onNewTab = { newTabs.incrementAndGet() },
        )

        val personalNode = composeRule.onNodeWithTag(ProfileSwitcherTestTags.profile(personal.id))
        val workNode = composeRule.onNodeWithTag(ProfileSwitcherTestTags.profile(work.id))
        val addNode = composeRule.onNodeWithTag(ProfileSwitcherTestTags.Add)
        val essentialNode = composeRule.onNodeWithTag(TabOverviewDockTestTags.essential(mail.id))
        val newTabNode = composeRule.onNodeWithTag(TabOverviewChromeTestTags.NewTab)

        personalNode.assertIsSelected()
        workNode.assertIsNotSelected().performClick()
        personalNode.performTouchInput { longClick() }
        addNode.performClick()
        essentialNode.performClick()
        newTabNode.performClick()

        assertEquals(work.id, selected.get())
        assertEquals(personal.id, edited.get())
        assertEquals(1, adds.get())
        assertEquals(mail, opened.get())
        assertEquals(1, newTabs.get())

        val minimumTouchTargetPx = composeRule.density.density * 48f
        listOf(personalNode, workNode, addNode, essentialNode, newTabNode).forEach { node ->
            val bounds = node.fetchSemanticsNode().touchBoundsInRoot
            assertTrue("width=${bounds.width}", bounds.width >= minimumTouchTargetPx)
            assertTrue("height=${bounds.height}", bounds.height >= minimumTouchTargetPx)
        }
    }

    @Test
    fun aDisabledDockIgnoresEveryAction() {
        val actions = AtomicInteger()
        setDock(
            enabled = false,
            onSelect = { actions.incrementAndGet() },
            onLongClick = { actions.incrementAndGet() },
            onAdd = { actions.incrementAndGet() },
            onOpen = { actions.incrementAndGet() },
            onNewTab = { actions.incrementAndGet() },
        )

        val workNode = composeRule.onNodeWithTag(ProfileSwitcherTestTags.profile(work.id))
        workNode.assertIsNotEnabled().performTouchInput { click() }
        workNode.performTouchInput { longClick() }
        composeRule.onNodeWithTag(ProfileSwitcherTestTags.Add).performTouchInput { click() }
        composeRule.onNodeWithTag(TabOverviewDockTestTags.essential(mail.id))
            .performTouchInput { click() }
        composeRule.onNodeWithTag(TabOverviewChromeTestTags.NewTab).performTouchInput { click() }

        assertEquals(0, actions.get())
    }

    private fun setDock(
        enabled: Boolean,
        onSelect: (String) -> Unit,
        onLongClick: (String) -> Unit,
        onAdd: () -> Unit,
        onOpen: (EssentialEntry) -> Unit,
        onNewTab: () -> Unit,
    ) {
        composeRule.setContent {
            MaterialBrowserTheme {
                TabOverviewDock(
                    essentials = listOf(mail),
                    essentialIcons = emptyMap(),
                    workspaces = listOf(personal, work),
                    activeWorkspaceId = personal.id,
                    showWorkspaces = true,
                    enabled = enabled,
                    onOpenEssential = onOpen,
                    onSelectWorkspace = onSelect,
                    onWorkspaceLongClick = onLongClick,
                    onAddWorkspace = onAdd,
                    onNewTab = onNewTab,
                )
            }
        }
    }
}
