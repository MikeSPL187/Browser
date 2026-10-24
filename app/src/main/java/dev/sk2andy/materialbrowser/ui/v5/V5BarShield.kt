package dev.sk2andy.materialbrowser.ui.v5

import android.os.SystemClock
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.browser.SiteConnectionKind
import dev.sk2andy.materialbrowser.shared.ui.icons.VolaIcons
import dev.sk2andy.materialbrowser.ui.PermissionRadarTestTags
import dev.sk2andy.materialbrowser.ui.PrivacyXRayMotionRules
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Motion
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Sizes
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Spacing
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Theme
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Type.tabular
import kotlinx.coroutines.delay

/**
 * The shield at the start of the address capsule (system-v5.md, section 5.1; canvas «Bar»): the
 * connection's symbol and the number blocked on the page, in the bar's quiet ink, with no plate
 * of its own. A site using the camera, the microphone or the location gets an accent dot. A tap
 * opens the site's protection. At the largest font the number goes, the shield stays (section 8).
 *
 * When the number grows the shield gives a short spring pulse, batched as before
 * ([PrivacyXRayMotionRules]); without system animations it does not move.
 */
@Composable
internal fun V5BarShield(
    tabId: String,
    connectionKind: SiteConnectionKind,
    blockedCount: Int,
    activityVisible: Boolean,
    ink: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pulse = remember(tabId) { Animatable(1f) }
    val currentCount by rememberUpdatedState(blockedCount)
    val initialCount = remember(tabId) { blockedCount }
    val moves = V5Motion.animationsEnabled
    LaunchedEffect(tabId, moves) {
        if (!moves) return@LaunchedEffect
        var previous = initialCount
        var lastPulseAt: Long? = null
        snapshotFlow { currentCount }.collect { count ->
            val before = previous
            previous = count
            val sinceLast = lastPulseAt?.let { SystemClock.uptimeMillis() - it } ?: Long.MAX_VALUE
            val wait = PrivacyXRayMotionRules.badgePulseDelayMillis(before, count, sinceLast) ?: return@collect
            delay(wait)
            val batched = currentCount
            previous = batched
            if (!PrivacyXRayMotionRules.shouldRunBatchedPulse(count, batched)) return@collect
            pulse.animateTo(SHIELD_PULSE_SCALE, V5Motion.glance.spec())
            pulse.animateTo(1f, V5Motion.glance.spec())
            lastPulseAt = SystemClock.uptimeMillis()
        }
    }
    val connection = stringResource(
        when (connectionKind) {
            SiteConnectionKind.Https -> R.string.site_connection_https
            SiteConnectionKind.Http -> R.string.site_connection_http
            SiteConnectionKind.Unavailable -> R.string.site_connection_unavailable
            SiteConnectionKind.Other -> R.string.site_connection_other
        },
    )
    val site = stringResource(R.string.site_info_badge_cd, connection, blockedCount)
    val activity = stringResource(R.string.permission_radar_activity_cd)
    val description = if (activityVisible) "$site. $activity" else site
    val showsCount = blockedCount > 0 && LocalDensity.current.fontScale < COUNT_MAX_FONT_SCALE
    Box(
        modifier
            .heightIn(min = V5Sizes.barCapsule)
            .widthIn(min = V5Sizes.barCapsule)
            .clip(V5Theme.shapes.full)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = V5Spacing.space2)
            .semantics { contentDescription = description }
            .testTag(PermissionRadarTestTags.ActivityBadge),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            Modifier.graphicsLayer {
                scaleX = pulse.value
                scaleY = pulse.value
            },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(V5Spacing.space1),
        ) {
            Box {
                Icon(
                    when (connectionKind) {
                        SiteConnectionKind.Https -> VolaIcons.ShieldLock
                        SiteConnectionKind.Http -> VolaIcons.GppMaybe
                        SiteConnectionKind.Unavailable, SiteConnectionKind.Other -> VolaIcons.Info
                    },
                    contentDescription = null,
                    tint = ink,
                    modifier = Modifier.size(V5Sizes.iconSmall),
                )
                if (activityVisible) {
                    Box(
                        Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = V5Spacing.space1 / 2, y = -V5Spacing.space1 / 2)
                            .size(V5Sizes.dot)
                            .clip(V5Theme.shapes.full)
                            .background(V5Theme.colors.accentFill),
                    )
                }
            }
            if (showsCount) {
                Text(
                    blockedCount.toString(),
                    style = V5Theme.type.caption.tabular(),
                    color = ink,
                    maxLines = 1,
                )
            }
        }
    }
}

/** How much the shield grows in its pulse. */
private const val SHIELD_PULSE_SCALE = 1.1f

/** From this font scale the number leaves the shield (system-v5.md, section 8). */
private const val COUNT_MAX_FONT_SCALE = 2f
