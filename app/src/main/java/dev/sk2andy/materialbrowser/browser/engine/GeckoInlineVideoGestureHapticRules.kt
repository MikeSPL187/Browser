package dev.sk2andy.materialbrowser.browser.engine

import dev.sk2andy.materialbrowser.browser.engine.GeckoInlineVideoGestureHaptic
import dev.sk2andy.materialbrowser.browser.engine.GeckoInlineVideoIdentity

internal object GeckoInlineVideoGestureHapticRules {
    fun shouldForward(
        isActivityResumed: Boolean,
        isPrivate: Boolean,
        isSelectedTab: Boolean,
        isCurrentSession: Boolean,
        currentNavigationGeneration: Int?,
        request: GeckoInlineVideoGestureHaptic,
        isInlineVideoPresented: Boolean,
        currentIdentity: GeckoInlineVideoIdentity?,
    ): Boolean =
        isActivityResumed &&
            !isPrivate &&
            isSelectedTab &&
            isCurrentSession &&
            currentNavigationGeneration == request.navigationGeneration &&
            isInlineVideoPresented &&
            currentIdentity == request.identity

    fun ownerRemainsValid(
        isActivityResumed: Boolean,
        isPrivate: Boolean,
        isSelectedTab: Boolean,
        isCurrentSession: Boolean,
        currentNavigationGeneration: Int?,
        ownerNavigationGeneration: Int,
        isInlineVideoPresented: Boolean,
        currentIdentity: GeckoInlineVideoIdentity?,
        ownerIdentity: GeckoInlineVideoIdentity,
    ): Boolean =
        isActivityResumed &&
            !isPrivate &&
            isSelectedTab &&
            isCurrentSession &&
            currentNavigationGeneration == ownerNavigationGeneration &&
            isInlineVideoPresented &&
            currentIdentity == ownerIdentity
}
