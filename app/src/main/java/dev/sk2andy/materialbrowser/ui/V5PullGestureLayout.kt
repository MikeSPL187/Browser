package dev.sk2andy.materialbrowser.ui

import android.annotation.SuppressLint
import android.content.Context
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.widget.FrameLayout
import kotlin.math.roundToInt

/**
 * The v5 pull to refresh gesture over the engine view (V16). It only recognizes the pull, with the
 * same rules as the current interface's [BrowserPullToRefreshLayout] (from the top zone, downward,
 * while the page is at its top), and reports how far the finger went; the page's offset, the
 * indicator, the springs and the haptics are Compose's ([V5PullRefreshState]).
 */
internal class V5PullGestureLayout(
    context: Context,
    internal val contentView: View,
) : FrameLayout(context) {
    interface Listener {
        fun onPull(distancePx: Float)

        fun onRelease()

        fun onCancel()
    }

    var listener: Listener? = null
    var pullEnabled = false
    var canContentScrollUp: () -> Boolean = { true }
    var topInsetPx = 0

    /** How far the page is moved down: the gap above it is where the indicator shows. */
    var pageOffsetPx: Float
        get() = contentView.translationY
        set(value) {
            contentView.translationY = value
        }

    private val zoneHeightPx = (PULL_ZONE_HEIGHT_DP * resources.displayMetrics.density).roundToInt()
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop.toFloat()
    private var downX = 0f
    private var downY = 0f
    private var startRawY = 0f
    private var direction = BrowserPullGestureDirection.Undecided
    private var pulling = false

    init {
        addView(contentView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
    }

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        if (!pullEnabled) {
            direction = BrowserPullGestureDirection.PassThrough
            return false
        }
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x
                downY = event.y
                pulling = false
                direction = if (BrowserPullGestureRules.canStartInTopZone(downY, topInsetPx, zoneHeightPx)) {
                    BrowserPullGestureDirection.Undecided
                } else {
                    BrowserPullGestureDirection.PassThrough
                }
            }
            MotionEvent.ACTION_MOVE -> {
                direction = BrowserPullGestureRules.resolve(
                    current = direction,
                    deltaX = event.x - downX,
                    deltaY = event.y - downY,
                    touchSlop = touchSlop,
                )
                if (direction == BrowserPullGestureDirection.Pull && !pulling) {
                    if (canContentScrollUp()) {
                        direction = BrowserPullGestureDirection.PassThrough
                    } else {
                        pulling = true
                        startRawY = event.rawY
                        parent?.requestDisallowInterceptTouchEvent(true)
                    }
                }
            }
            MotionEvent.ACTION_POINTER_DOWN -> direction = BrowserPullGestureDirection.PassThrough
        }
        return pulling
    }

    @SuppressLint("ClickableViewAccessibility") // The pull has an accessible twin: «Reload» in the menu.
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!pulling) return false
        when (event.actionMasked) {
            MotionEvent.ACTION_MOVE -> listener?.onPull((event.rawY - startRawY).coerceAtLeast(0f))
            MotionEvent.ACTION_UP -> {
                pulling = false
                listener?.onRelease()
            }
            MotionEvent.ACTION_CANCEL -> {
                pulling = false
                listener?.onCancel()
            }
        }
        return true
    }
}
