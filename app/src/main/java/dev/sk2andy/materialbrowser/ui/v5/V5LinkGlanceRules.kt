package dev.sk2andy.materialbrowser.ui.v5

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.util.lerp
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Motion
import java.net.URI
import kotlin.math.PI
import kotlin.math.sin

/*
 * The v5 link preview's geometry and address line (V21, canvas screen «н1», plan-v5.md section 5):
 * the card leaves the touch point, lifts along an arc no higher than 20 dp and settles over the
 * page, which shrinks to 0.97 beneath it. The card keeps its proportions while it flies, so the
 * live page inside it is never stretched.
 */
object V5LinkGlanceRules {
    /** The live page starts to show this far into the flight; before, the card is only its frame. */
    const val PAGE_FADE_FROM = 0.3f

    /** A full back gesture takes the card this far back towards the link before it lets go. */
    const val BACK_TRAVEL = 0.2f

    /** As the link opens here, the card and its buttons are gone by a third of the way. */
    const val OPEN_FADE_SPEED = 3f

    /** The card's corners are drawn before its scale; this keeps them finite at the first frame. */
    const val MIN_SCALE = 0.01f

    /** Where the card is drawn: scaled by [scale] about its center, moved by [dx], [dy] from its rest. */
    data class Flight(val scale: Float, val dx: Float, val dy: Float)

    /**
     * The card at [progress] (0 at the touch point, 1 at [rest]; a spring may overshoot) from
     * [origin], or from the middle of [rest] when the touch point is unknown. It starts
     * [originSizePx] wide and rises by up to [arcPx] on the way.
     */
    fun flight(origin: Offset?, rest: Rect, progress: Float, originSizePx: Float, arcPx: Float): Flight {
        if (rest.width <= 0f) return Flight(1f, 0f, 0f)
        val start = origin ?: rest.center
        val startScale = (originSizePx / rest.width).coerceIn(0f, 1f)
        val lift = arcPx * sin(PI.toFloat() * progress.coerceIn(0f, 1f))
        return Flight(
            scale = lerp(startScale, 1f, progress),
            dx = lerp(start.x, rest.center.x, progress) - rest.center.x,
            dy = lerp(start.y, rest.center.y, progress) - lift - rest.center.y,
        )
    }

    /** The card's corner radius at [progress]: the link's small one at first, the card's own at rest. */
    fun cornerRadius(fromPx: Float, toPx: Float, progress: Float): Float =
        lerp(fromPx, toPx, progress.coerceIn(0f, 1f))

    /** The page's scale beneath the card at [depth] (0 at rest, 1 under the open card). */
    fun pageScale(depth: Float): Float = lerp(1f, V5Motion.GLANCE_PAGE_SCALE, depth.coerceIn(0f, 1f))

    /** [bounds] scaled by [scale] about their center: where the shrunk page sits. */
    fun scaledAboutCenter(bounds: Rect, scale: Float): Rect {
        val halfWidth = bounds.width * scale / 2f
        val halfHeight = bounds.height * scale / 2f
        val center = bounds.center
        return Rect(center.x - halfWidth, center.y - halfHeight, center.x + halfWidth, center.y + halfHeight)
    }

    /** The card growing from [rest] into [window] as the link opens here, at [progress] 0..1. */
    fun opening(rest: Rect, window: Rect, progress: Float): Rect {
        val t = progress.coerceIn(0f, 1f)
        return Rect(
            lerp(rest.left, window.left, t),
            lerp(rest.top, window.top, t),
            lerp(rest.right, window.right, t),
            lerp(rest.bottom, window.bottom, t),
        )
    }

    /**
     * The live page inside the card: it comes in from [PAGE_FADE_FROM] to the end of the flight, so
     * the speck at the touch point is the card's own colour, not a page squeezed into a dot.
     */
    fun pageAlpha(progress: Float): Float =
        ((progress - PAGE_FADE_FROM) / (1f - PAGE_FADE_FROM)).coerceIn(0f, 1f)

    /** The buttons under the card: they come in over the second half of the flight. */
    fun actionsAlpha(progress: Float): Float = ((progress - 0.5f) * 2f).coerceIn(0f, 1f)

    /**
     * The card's address line: host and path, as on the canvas («polar-notes.ru/prognoz»). The query
     * and fragment stay out (they carry tracking more often than meaning); «www.» and a trailing
     * slash go too. A punycode host stays punycode, so a look-alike name can't pass for another.
     */
    fun address(url: String): String {
        val uri = runCatching { URI(url) }.getOrNull() ?: return url
        val host = uri.host?.removePrefix("www.")?.takeIf(String::isNotEmpty) ?: return url
        val path = uri.path.orEmpty().trimEnd('/')
        return host + path
    }

    /** True for an encrypted connection: the address line shows a lock. */
    fun secure(url: String): Boolean =
        runCatching { URI(url).scheme }.getOrNull().equals("https", ignoreCase = true)

    /** What a vertical swipe on the card ends in (canvas «Glance»: up opens, down closes). */
    enum class Swipe { Open, Close, Stay }

    /** Past [commitPx] or faster than [velocityPx] per second: up opens, down closes; else the card settles back. */
    fun swipe(offsetPx: Float, velocityPx: Float, commitPx: Float, minVelocityPx: Float): Swipe = when {
        offsetPx <= -commitPx || (velocityPx <= -minVelocityPx && offsetPx < 0f) -> Swipe.Open
        offsetPx >= commitPx || (velocityPx >= minVelocityPx && offsetPx > 0f) -> Swipe.Close
        else -> Swipe.Stay
    }
}
