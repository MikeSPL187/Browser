package dev.sk2andy.materialbrowser.audit

import android.app.Activity
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.text.TextLayoutResult
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/** One thing a tester would file: where, in which configuration, what is wrong. */
internal data class AuditFinding(
    val screen: String,
    val config: String,
    val kind: AuditKind,
    val detail: String,
) {
    override fun toString(): String = "[$kind] $screen ($config): $detail"
}

internal enum class AuditKind {
    /** The walk could not reach or leave a screen the way a user does. */
    Navigation,

    /** A label or a control under the status bar, the camera cutout or the navigation bar. */
    SystemBars,

    /** Text cut off without an ellipsis: part of the words is simply not drawn. */
    TextClipped,

    /** A control smaller than 48 dp to touch. */
    TouchTarget,

    /** A control TalkBack reads as nothing. */
    NoLabel,

    /** Two controls on top of each other: one of them can't be tapped. */
    Overlap,

    /** A control or a label partly outside the screen with no way to scroll to it. */
    OffScreen,

}

/**
 * Looks at every node on screen the way a careful tester would, after each step of a walk
 * through the real app. Semantics come from every Compose root of the window (dialogs, sheets
 * and the blur target's own ComposeView included); web content is not Compose and is not checked
 * here.
 */
internal class ScreenAuditor(
    private val composeRule: ComposeTestRule,
    private val activity: () -> Activity,
) {
    /** Audits the screen, then scrolls it a page at a time to the end and audits again. */
    fun auditScrolling(screen: String, config: String): List<AuditFinding> {
        val findings = audit(screen, config).toMutableList()
        repeat(MAX_SCROLL_PAGES) { page ->
            val scrollable = composeRule.onAllNodes(hasScrollAction() and isVerticallyScrollable)
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .maxByOrNull { node -> node.boundsInWindow.height * node.boundsInWindow.width }
                ?: return findings
            val matcher = SemanticsMatcher("scrollable ${scrollable.id}") { it.id == scrollable.id }
            val before = scrollable.config.getOrNull(SemanticsProperties.VerticalScrollAxisRange)?.value?.invoke()
            val target = composeRule.onAllNodes(matcher).onFirst()
            // Scrolled by the list's own scroll action, most of a screen at a time. A swipe on a
            // loaded emulator can be slow enough to read as a long press, and on the drag-to-arrange
            // editors that starts a drag: the menu buttons page then took up to 21 minutes.
            val pageHeight = scrollable.boundsInWindow.height * SCROLL_PAGE_FRACTION
            val scrolled = runCatching {
                target.performSemanticsAction(SemanticsActions.ScrollBy) { scrollBy -> scrollBy(0f, pageHeight) }
            }.isSuccess
            if (!scrolled) target.performTouchInput { swipeUp() }
            composeRule.waitForIdle()
            val after = composeRule.onAllNodes(matcher)
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .firstOrNull()
                ?.config
                ?.getOrNull(SemanticsProperties.VerticalScrollAxisRange)
                ?.value
                ?.invoke()
            if (after == null || after == before) return findings
            findings += audit("$screen ↓${page + 1}", config)
        }
        return findings
    }

    fun audit(screen: String, config: String): List<AuditFinding> {
        composeRule.waitForIdle()
        val window = windowGeometry()
        val allNodes = composeRule.onAllNodes(anyNode, useUnmergedTree = false)
            .fetchSemanticsNodes(atLeastOneRootRequired = false)
        val nodes = allNodes.filter { node -> node.isShown(window) }
        // A sheet opened half-way offers to expand: what its lower edge hides is a drag away, as
        // in a list that scrolls on. Its rows were filed only when the content was too short to scroll.
        val expandableRoots = allNodes
            .filter { node -> node.config.contains(SemanticsActions.Expand) }
            .mapTo(HashSet()) { node -> node.rootKey() }
        val findings = mutableListOf<AuditFinding>()
        fun file(kind: AuditKind, detail: String) {
            findings += AuditFinding(screen, config, kind, detail)
        }

        val controls = nodes.filter { it.isControl() }
        val layouts = textLayouts(nodes)
        for (node in nodes) {
            val bounds = node.boundsOnScreen()
            val label = node.label()
            val readable = node.config.contains(SemanticsProperties.Text) || node.isControl()
            val room = node.scrollRoom().let { room ->
                if (node.rootKey() in expandableRoots) room.copy(forward = true) else room
            }
            val underBars = window.underSystemBars(bounds, room)
            if (readable && !node.isFullScreen(window) && underBars) {
                file(
                    AuditKind.SystemBars,
                    "«$label» at ${bounds.short()} — ${window.bars()}${node.scrollContext()}",
                )
            }
            if (readable && !node.isInHorizontalScroll() && window.cutOffSideways(bounds)) {
                file(
                    AuditKind.OffScreen,
                    "«$label» at ${bounds.short()} is outside the ${window.width}px wide window",
                )
            }
            layouts[node.id]?.let { layout ->
                if (layout.isClippedWithoutEllipsis()) {
                    file(AuditKind.TextClipped, "«$label» is cut off: ${layout.clipDetail()}")
                }
            }
        }
        for (node in controls) {
            // A control of no size, or not placed on screen yet (collapsed, or caught
            // mid-animation), is neither seen nor reached by TalkBack: a row that appears in
            // settings search was filed as unlabelled.
            if (node.size.width == 0 || node.size.height == 0) continue
            if (node.boundsInWindow.width <= 0f || node.boundsInWindow.height <= 0f) continue
            val label = node.label()
            // Touch bounds take in the extension Compose gives a small control (a 40 dp icon
            // button answers touches over 48 dp) but are clipped by scrolling; the node's own
            // size is whole. A control is too small only when both are.
            val touch = node.touchBoundsInRoot
            val width = maxOf(touch.width, node.size.width.toFloat())
            val height = maxOf(touch.height, node.size.height.toFloat())
            val minTouchPx = MIN_TOUCH_DP * window.density - 0.5f
            // Half scrolled out of view, a control is judged when it is in view.
            val visible = node.boundsInWindow
            val clipped = visible.width < node.size.width - 1f || visible.height < node.size.height - 1f
            if (!clipped && (width < minTouchPx || height < minTouchPx)) {
                file(
                    AuditKind.TouchTarget,
                    "«$label» is ${(width / window.density).toInt()}×" +
                        "${(height / window.density).toInt()} dp to touch",
                )
            }
            if (label.isBlank()) {
                file(AuditKind.NoLabel, "a control at ${node.boundsOnScreen().short()} has no text or description")
            }
        }
        controls.forEachIndexed { index, first ->
            for (second in controls.drop(index + 1)) {
                if (first.isFullScreen(window) || second.isFullScreen(window)) continue
                if (first.isAncestorOf(second) || second.isAncestorOf(first)) continue
                // A menu or a dialog is a window of its own above the page: that is no conflict.
                if (first.rootKey() != second.rootKey()) continue
                // Content scrolling under a floating bar can still be scrolled clear of it.
                if (first.canScrollFurther() || second.canScrollFurther()) continue
                val a = first.boundsOnScreen()
                val b = second.boundsOnScreen()
                val overlap = a.intersect(b)
                if (overlap.width <= 0f || overlap.height <= 0f) continue
                val smaller = minOf(a.width * a.height, b.width * b.height)
                if (smaller > 0f && overlap.width * overlap.height > smaller * OVERLAP_SHARE) {
                    file(
                        AuditKind.Overlap,
                        "«${first.label()}» ${a.short()} and «${second.label()}» ${b.short()}",
                    )
                }
            }
        }
        return findings.distinctBy { it.kind to it.detail }
    }

    /** What is on screen now, in a line: shown when the walk can't find what it looks for. */
    fun describeScreen(): String {
        val window = windowGeometry()
        return composeRule.onAllNodes(anyNode, useUnmergedTree = false)
            .fetchSemanticsNodes(atLeastOneRootRequired = false)
            .filter { node -> node.isShown(window) && (node.isControl() || node.label().isNotBlank()) }
            .map { node -> node.label() }
            .filter(String::isNotBlank)
            .distinct()
            .take(SCREEN_DESCRIPTION_LIMIT)
            .joinToString(" | ")
    }

    /** The laid-out text of every text node, read in one pass on the main thread. */
    private fun textLayouts(nodes: List<SemanticsNode>): Map<Int, TextLayoutResult> {
        val actions = nodes.mapNotNull { node ->
            node.config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.let { node.id to it }
        }
        val layouts = mutableMapOf<Int, TextLayoutResult>()
        runCatching {
            composeRule.runOnIdle {
                for ((id, action) in actions) {
                    val results = mutableListOf<TextLayoutResult>()
                    if (runCatching { action(results) }.getOrDefault(false)) {
                        results.firstOrNull()?.let { layouts[id] = it }
                    }
                }
            }
        }
        return layouts
    }

    private fun windowGeometry(): WindowGeometry {
        // The activity is looked up off the main thread: ActivityScenario refuses it there.
        val current = activity()
        var geometry: WindowGeometry? = null
        composeRule.runOnIdle {
            val decor = current.window.decorView
            val insets = ViewCompat.getRootWindowInsets(decor)
            val bars = insets?.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout(),
            )
            val statusVisible = insets?.isVisible(WindowInsetsCompat.Type.statusBars()) != false
            val navigationVisible = insets?.isVisible(WindowInsetsCompat.Type.navigationBars()) != false
            geometry = WindowGeometry(
                width = decor.width,
                height = decor.height,
                top = if (statusVisible) bars?.top ?: 0 else 0,
                bottom = if (navigationVisible) bars?.bottom ?: 0 else 0,
                left = bars?.left ?: 0,
                right = bars?.right ?: 0,
                density = current.resources.displayMetrics.density,
            )
        }
        return checkNotNull(geometry)
    }

    private companion object {
        const val MIN_TOUCH_DP = 48f
        const val OVERLAP_SHARE = 0.25f
        const val SCREEN_DESCRIPTION_LIMIT = 40
        val anyNode = SemanticsMatcher("any node") { true }
        val isVerticallyScrollable = SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange)
        const val MAX_SCROLL_PAGES = 8
        const val SCROLL_PAGE_FRACTION = 0.8f
    }
}

internal data class WindowGeometry(
    val width: Int,
    val height: Int,
    val top: Int,
    val bottom: Int,
    val left: Int,
    val right: Int,
    val density: Float,
) {
    /**
     * Touches a system bar band by more than a pixel or two (shadows and ripples aside). Edge to
     * edge, a list scrolls under the bars: content under the status bar is fine while the list
     * can scroll back down, and under the navigation bar while it can scroll on. At its ends the
     * list must keep its first and last rows clear of the bars.
     */
    fun underSystemBars(bounds: Rect, room: ScrollRoom): Boolean {
        val tolerance = SYSTEM_BAR_TOLERANCE_DP * density
        val underTop = top > 0 && !room.back && bounds.top < top - tolerance && bounds.bottom > 0f
        val underBottom = bottom > 0 && !room.forward &&
            bounds.bottom > height - bottom + tolerance && bounds.top < height
        val underLeft = left > 0 && bounds.left < left - tolerance && bounds.right > 0f
        val underRight = right > 0 && bounds.right > width - right + tolerance && bounds.left < width
        return underTop || underBottom || underLeft || underRight
    }

    fun cutOffSideways(bounds: Rect): Boolean {
        val tolerance = SYSTEM_BAR_TOLERANCE_DP * density
        val visible = bounds.right > 0f && bounds.left < width
        return visible && (bounds.left < -tolerance || bounds.right > width + tolerance)
    }

    fun bars(): String = "status bar $top px, navigation bar $bottom px, sides $left/$right px"

    private companion object {
        const val SYSTEM_BAR_TOLERANCE_DP = 2f
    }
}

private fun SemanticsNode.isControl(): Boolean =
    config.contains(SemanticsActions.OnClick) && !config.contains(SemanticsProperties.Disabled)

private fun SemanticsNode.isShown(window: WindowGeometry): Boolean {
    if (config.contains(SemanticsProperties.InvisibleToUser)) return false
    if (!layoutInfo.isAttached || !layoutInfo.isPlaced) return false
    val bounds = boundsOnScreen()
    if (bounds.width <= 0f || bounds.height <= 0f) return false
    return bounds.right > 0f && bounds.bottom > 0f && bounds.left < window.width && bounds.top < window.height
}

private fun SemanticsNode.isFullScreen(window: WindowGeometry): Boolean {
    val bounds = boundsOnScreen()
    return bounds.width >= window.width * FULL_SCREEN_SHARE && bounds.height >= window.height * FULL_SCREEN_SHARE
}

private fun SemanticsNode.isInHorizontalScroll(): Boolean {
    var current: SemanticsNode? = this
    while (current != null) {
        if (current.config.contains(SemanticsProperties.HorizontalScrollAxisRange)) return true
        current = current.parent
    }
    return false
}

/**
 * The node's visible bounds on the screen. Menus, sheets and dialogs are windows of their own,
 * so window coordinates would put a popup's top at 0 whatever its place on the screen.
 */
internal fun SemanticsNode.boundsOnScreen(): Rect =
    boundsInWindow.translate(positionOnScreen - positionInWindow)

/** Where the nearest vertical scroll container is and how far it is scrolled, for diagnosis. */
private fun SemanticsNode.scrollContext(): String {
    var current = parent
    while (current != null) {
        val range = current.config.getOrNull(SemanticsProperties.VerticalScrollAxisRange)
        if (range != null) {
            return "; in a scroll container at ${current.boundsOnScreen().short()} scrolled " +
                "${range.value().toInt()} of ${range.maxValue().toInt()} px"
        }
        current = current.parent
    }
    return "; not in a scroll container"
}

/** How far the nearest vertical scroll container around a node can still move. */
internal data class ScrollRoom(val back: Boolean, val forward: Boolean)

private fun SemanticsNode.scrollRoom(): ScrollRoom {
    var current = parent
    while (current != null) {
        val range = current.config.getOrNull(SemanticsProperties.VerticalScrollAxisRange)
        if (range != null) {
            val position = range.value()
            return ScrollRoom(back = position > 1f, forward = position < range.maxValue() - 1f)
        }
        current = current.parent
    }
    return ScrollRoom(back = false, forward = false)
}

/** Whether a vertical scroll container around the node can still move it. */
private fun SemanticsNode.canScrollFurther(): Boolean {
    var current = parent
    while (current != null) {
        val range = current.config.getOrNull(SemanticsProperties.VerticalScrollAxisRange)
        if (range != null) return range.value() < range.maxValue() - 1f
        current = current.parent
    }
    return false
}

/** Identifies the Compose root (window) the node belongs to. */
private fun SemanticsNode.rootKey(): String {
    var root = this
    while (true) root = root.parent ?: break
    return "${root.id}@${root.boundsOnScreen()}"
}

private fun SemanticsNode.isAncestorOf(other: SemanticsNode): Boolean {
    var current = other.parent
    while (current != null) {
        if (current.id == id) return true
        current = current.parent
    }
    return false
}

internal fun SemanticsNode.label(): String {
    val parts = mutableListOf<String>()
    config.getOrNull(SemanticsProperties.ContentDescription)?.let { parts.addAll(it) }
    config.getOrNull(SemanticsProperties.Text)?.let { texts -> parts.addAll(texts.map { it.text }) }
    config.getOrNull(SemanticsProperties.EditableText)?.let { parts.add(it.text) }
    config.getOrNull(SemanticsProperties.StateDescription)?.let { parts.add(it) }
    return parts.filter(String::isNotBlank).joinToString(" ").take(LABEL_LIMIT)
}

/**
 * Text that lost lines or glyphs with nothing to show it: more lines than allowed, or lines
 * wider or taller than the box the text was given, and no ellipsis anywhere.
 * [TextLayoutResult.hasVisualOverflow] alone is true for ordinary single-line labels too.
 */
private fun TextLayoutResult.isClippedWithoutEllipsis(): Boolean {
    if (lineCount == 0) return false
    if ((0 until lineCount).any { line -> isLineEllipsized(line) }) return false
    val slack = CLIP_SLACK_PX
    val tooTall = multiParagraph.height > size.height + slack
    // Width, not position: centred text is laid out in a wider paragraph than its box.
    val tooWide = (0 until lineCount).any { line ->
        getLineRight(line) - getLineLeft(line) > size.width + slack
    }
    return multiParagraph.didExceedMaxLines || tooTall || tooWide
}

private const val CLIP_SLACK_PX = 2f

/** The numbers that show how text overflows its box: box, widest line, paragraph height. */
private fun TextLayoutResult.clipDetail(): String {
    val widest = (0 until lineCount).maxOfOrNull { line -> getLineRight(line) - getLineLeft(line) } ?: 0f
    return "box ${size.width}×${size.height} px, widest line ${widest.toInt()} px, " +
        "text ${multiParagraph.height.toInt()} px tall, $lineCount lines" +
        if (multiParagraph.didExceedMaxLines) ", more lines than allowed" else ""
}

private fun Rect.short(): String = "[${left.toInt()},${top.toInt()} ${width.toInt()}×${height.toInt()}]"

private const val FULL_SCREEN_SHARE = 0.9f
private const val LABEL_LIMIT = 60
