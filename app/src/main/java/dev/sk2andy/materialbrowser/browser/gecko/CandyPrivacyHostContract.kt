package dev.sk2andy.materialbrowser.browser.gecko

import dev.sk2andy.materialbrowser.browser.InlineMediaPlayerMode
import dev.sk2andy.materialbrowser.blocking.CandyRule
import dev.sk2andy.materialbrowser.blocking.CandyRuleAction
import dev.sk2andy.materialbrowser.blocking.CandyRuleKind
import dev.sk2andy.materialbrowser.browser.BrowserEngineScrollMetrics
import dev.sk2andy.materialbrowser.browser.BrowserViewportRect
import dev.sk2andy.materialbrowser.browser.CaptchaCompatibilityRules
import dev.sk2andy.materialbrowser.browser.FederatedLoginRules
import dev.sk2andy.materialbrowser.data.DeveloperSettings
import dev.sk2andy.materialbrowser.data.HiddenDistractionsRules
import org.json.JSONArray
import org.json.JSONObject
import dev.sk2andy.materialbrowser.browser.engine.GeckoInlineVideoGestureHaptic
import dev.sk2andy.materialbrowser.browser.engine.GeckoInlineVideoGestureHapticPhase
import dev.sk2andy.materialbrowser.browser.engine.GeckoInlineVideoIdentity
import dev.sk2andy.materialbrowser.browser.engine.GeckoInlineVideoOpenRequest

internal object CandyPrivacyHostContract {
    const val EXTENSION_ID = "candy-privacy-host@sk2andy.dev"
    const val EXTENSION_LOCATION = "resource://android/assets/candy_privacy/"
    const val NATIVE_APP = "dev.sk2andy.materialbrowser.privacy"
    const val PROTOCOL_VERSION = 2

    fun isTrustedBootstrapNavigation(
        url: String,
        extensionBaseUrl: String,
        token: String,
        isDirectNavigation: Boolean,
        hasUserGesture: Boolean,
        isRedirect: Boolean,
    ): Boolean = isDirectNavigation &&
        !hasUserGesture &&
        !isRedirect &&
        url == "${extensionBaseUrl}bootstrap.html?token=$token"

    fun isTrustedSessionBindingMessage(
        nativeApp: String,
        extensionId: String,
        environmentType: Int,
        expectedEnvironmentType: Int,
        hasExpectedSession: Boolean,
        messageType: String?,
        messageToken: String?,
        expectedToken: String,
        messageRevision: Long,
        currentRevision: Long,
        bootstrapStarted: Boolean,
    ): Boolean = nativeApp == NATIVE_APP &&
        extensionId == EXTENSION_ID &&
        environmentType == expectedEnvironmentType &&
        hasExpectedSession &&
        bootstrapStarted &&
        messageType == "bound" &&
        messageToken == expectedToken &&
        messageRevision in 1..currentRevision
}

private val DEFAULT_COMPATIBILITY_REQUEST_HOSTS =
    FederatedLoginRules.compatibilityRequestHosts +
        CaptchaCompatibilityRules.compatibilityRequestHosts

internal data class GeckoPrivacyPolicy(
    val pageHost: String?,
    val blockAdsAndTrackers: Boolean,
    val hideCookieConsent: Boolean,
    val cookieBannerRemovalDisabled: Boolean,
    val pausedHosts: Set<String>,
    val candyRules: List<CandyRule>,
    val doNotTrackEnabled: Boolean = true,
    val globalPrivacyControlEnabled: Boolean = true,
    val privacySignalRevision: Long = 0L,
    val animationsEnabled: Boolean = true,
    val animationPolicyRevision: Long = 0L,
    val blockThirdPartyCookies: Boolean = true,
    val allowThirdPartyCookiesForSite: Boolean = false,
    /** Strict site protection: requests to other sites go without a Referer (`privacy_signals.js`). */
    val trimCrossSiteReferrer: Boolean = false,
    /** Strict site protection: Vola's Gecko blocker drops the lists' compatibility exceptions. */
    val strictBlocking: Boolean = false,
    val compatibilityRequestHosts: Set<String> = DEFAULT_COMPATIBILITY_REQUEST_HOSTS,
    val topInsetPx: Int = 0,
    val navigationGeneration: Int = 0,
    val scrollMetricsEnabled: Boolean = false,
    val inlineMediaPlayerEnabled: Boolean = false,
    val inlineMediaPlayerMode: String = InlineMediaPlayerMode.Default.stableId,
    val inlineMediaPlayerActionLabel: String = "Open in Vola Player",
    val inlineMediaPlayerPlayLabel: String = "Play",
    val inlineMediaPlayerPauseLabel: String = "Pause",
    val inlineMediaPlayerSeekLabel: String = "Seek",
    val inlineMediaPlayerEnterFullscreenLabel: String = "Enter fullscreen",
    val inlineMediaPlayerExitFullscreenLabel: String = "Exit fullscreen",
    val inlineMediaPlayerCloseLabel: String = "Close Vola Player",
    val inlineMediaPlayerShowControlsLabel: String = "Show controls",
    val inlineMediaPlayerHideControlsLabel: String = "Hide controls",
    val safeAreaLayoutQuietPeriodMillis: Int =
        DeveloperSettings.DEFAULT_SAFE_AREA_LAYOUT_QUIET_PERIOD_MILLIS,
    val safeAreaRequiredFailureCount: Int =
        DeveloperSettings.DEFAULT_SAFE_AREA_REQUIRED_FAILURE_COUNT,
    /** The page site's hidden distracting items (v5 V20), as selectors `hide_distractions.js` hides. */
    val hiddenDistractions: List<String> = emptyList(),
) {
    companion object {
        val Disabled = GeckoPrivacyPolicy(
            pageHost = null,
            blockAdsAndTrackers = false,
            hideCookieConsent = false,
            cookieBannerRemovalDisabled = true,
            pausedHosts = emptySet(),
            candyRules = emptyList(),
            blockThirdPartyCookies = true,
            allowThirdPartyCookiesForSite = false,
            compatibilityRequestHosts = DEFAULT_COMPATIBILITY_REQUEST_HOSTS,
        )
    }
}

internal object GeckoPrivacyPolicyRules {
    fun extensionOwnedAdFilteringWithCandyCookieDefaults(
        pageHost: String?,
        pausedHosts: Set<String>,
        hideCookieConsent: Boolean,
        cookieBannerRemovalDisabled: Boolean,
        blockThirdPartyCookies: Boolean,
        allowThirdPartyCookiesForSite: Boolean,
        trimCrossSiteReferrer: Boolean = false,
        doNotTrackEnabled: Boolean = true,
        globalPrivacyControlEnabled: Boolean = true,
        privacySignalRevision: Long = 0L,
        animationsEnabled: Boolean = true,
        animationPolicyRevision: Long = 0L,
        topInsetPx: Int = 0,
        navigationGeneration: Int = 0,
        scrollMetricsEnabled: Boolean = false,
        inlineMediaPlayerEnabled: Boolean = false,
        inlineMediaPlayerMode: String = InlineMediaPlayerMode.Default.stableId,
        inlineMediaPlayerActionLabel: String = "Open in Vola Player",
        inlineMediaPlayerPlayLabel: String = "Play",
        inlineMediaPlayerPauseLabel: String = "Pause",
        inlineMediaPlayerSeekLabel: String = "Seek",
        inlineMediaPlayerEnterFullscreenLabel: String = "Enter fullscreen",
        inlineMediaPlayerExitFullscreenLabel: String = "Exit fullscreen",
        inlineMediaPlayerCloseLabel: String = "Close Vola Player",
        inlineMediaPlayerShowControlsLabel: String = "Show controls",
        inlineMediaPlayerHideControlsLabel: String = "Hide controls",
        safeAreaLayoutQuietPeriodMillis: Int =
            DeveloperSettings.DEFAULT_SAFE_AREA_LAYOUT_QUIET_PERIOD_MILLIS,
        safeAreaRequiredFailureCount: Int =
            DeveloperSettings.DEFAULT_SAFE_AREA_REQUIRED_FAILURE_COUNT,
        hiddenDistractions: List<String> = emptyList(),
    ): GeckoPrivacyPolicy {
        val developerSettings = DeveloperSettings(
            safeAreaLayoutQuietPeriodMillis = safeAreaLayoutQuietPeriodMillis,
            safeAreaRequiredFailureCount = safeAreaRequiredFailureCount,
        ).normalized()
        return GeckoPrivacyPolicy.Disabled.copy(
            pageHost = pageHost,
            pausedHosts = pausedHosts,
            hideCookieConsent = hideCookieConsent,
            cookieBannerRemovalDisabled = cookieBannerRemovalDisabled,
            doNotTrackEnabled = doNotTrackEnabled,
            globalPrivacyControlEnabled = globalPrivacyControlEnabled,
            privacySignalRevision = privacySignalRevision.coerceAtLeast(0L),
            animationsEnabled = animationsEnabled,
            animationPolicyRevision = animationPolicyRevision.coerceAtLeast(0L),
            blockThirdPartyCookies = blockThirdPartyCookies,
            allowThirdPartyCookiesForSite = allowThirdPartyCookiesForSite,
            trimCrossSiteReferrer = trimCrossSiteReferrer,
            topInsetPx = topInsetPx.coerceAtLeast(0),
            navigationGeneration = navigationGeneration.coerceAtLeast(0),
            scrollMetricsEnabled = scrollMetricsEnabled,
            inlineMediaPlayerEnabled = inlineMediaPlayerEnabled,
            inlineMediaPlayerMode = InlineMediaPlayerMode.fromStableId(
                inlineMediaPlayerMode,
            ).stableId,
            inlineMediaPlayerActionLabel = inlineMediaPlayerActionLabel.take(80),
            inlineMediaPlayerPlayLabel = inlineMediaPlayerPlayLabel.take(80),
            inlineMediaPlayerPauseLabel = inlineMediaPlayerPauseLabel.take(80),
            inlineMediaPlayerSeekLabel = inlineMediaPlayerSeekLabel.take(80),
            inlineMediaPlayerEnterFullscreenLabel =
                inlineMediaPlayerEnterFullscreenLabel.take(80),
            inlineMediaPlayerExitFullscreenLabel =
                inlineMediaPlayerExitFullscreenLabel.take(80),
            inlineMediaPlayerCloseLabel = inlineMediaPlayerCloseLabel.take(80),
            inlineMediaPlayerShowControlsLabel = inlineMediaPlayerShowControlsLabel.take(80),
            inlineMediaPlayerHideControlsLabel = inlineMediaPlayerHideControlsLabel.take(80),
            safeAreaLayoutQuietPeriodMillis =
                developerSettings.safeAreaLayoutQuietPeriodMillis,
            safeAreaRequiredFailureCount = developerSettings.safeAreaRequiredFailureCount,
            hiddenDistractions = hiddenDistractions.filter(HiddenDistractionsRules::isValidSelector)
                .takeLast(HiddenDistractionsRules.MAX_SELECTORS_PER_SITE),
        )
    }
}

internal data class GeckoPrivacyEvent(
    val requestUrl: String,
    val pageUrl: String?,
    val ruleId: String?,
    val wasBlocked: Boolean,
    val isBuiltIn: Boolean,
    val isCompatibilityObservation: Boolean,
    val safeAreaFallbackNavigationGeneration: Int? = null,
    val safeAreaFallbackThemeColor: String? = null,
    val safeAreaFallbackIsTopHeader: Boolean = false,
    val statusBarBackdropNavigationGeneration: Int? = null,
    val statusBarBackdropThemeColor: String? = null,
    val siteThemeColorNavigationGeneration: Int? = null,
    val siteThemeColor: String? = null,
    val readerAvailableNavigationGeneration: Int? = null,
    val readerAvailable: Boolean = false,
    val isCloudflareChallengeResponse: Boolean = false,
)

internal fun interface GeckoPrivacyEventSink {
    fun onEvent(event: GeckoPrivacyEvent)
}

internal fun GeckoPrivacyPolicy.toMessage(token: String, revision: Long): JSONObject = JSONObject()
    .put("type", "policy")
    .put("protocolVersion", CandyPrivacyHostContract.PROTOCOL_VERSION)
    .put("token", token)
    .put("revision", revision)
    .put("pageHost", pageHost)
    .put("blockAds", blockAdsAndTrackers)
    .put("hideConsent", hideCookieConsent)
    .put("cookieBannerRemovalDisabled", cookieBannerRemovalDisabled)
    .put("doNotTrackEnabled", doNotTrackEnabled)
    .put("globalPrivacyControlEnabled", globalPrivacyControlEnabled)
    .put("trimCrossSiteReferrer", trimCrossSiteReferrer)
    .put("strictBlocking", strictBlocking)
    .put("privacySignalRevision", privacySignalRevision)
    .put("animationsEnabled", animationsEnabled)
    .put("animationPolicyRevision", animationPolicyRevision)
    .put("topInsetPx", topInsetPx)
    .put("navigationGeneration", navigationGeneration)
    .put("scrollMetricsEnabled", scrollMetricsEnabled)
    .put("inlineMediaPlayerEnabled", inlineMediaPlayerEnabled)
    .put("inlineMediaPlayerMode", inlineMediaPlayerMode)
    .put("inlineMediaPlayerActionLabel", inlineMediaPlayerActionLabel)
    .put("inlineMediaPlayerPlayLabel", inlineMediaPlayerPlayLabel)
    .put("inlineMediaPlayerPauseLabel", inlineMediaPlayerPauseLabel)
    .put("inlineMediaPlayerSeekLabel", inlineMediaPlayerSeekLabel)
    .put("inlineMediaPlayerEnterFullscreenLabel", inlineMediaPlayerEnterFullscreenLabel)
    .put("inlineMediaPlayerExitFullscreenLabel", inlineMediaPlayerExitFullscreenLabel)
    .put("inlineMediaPlayerCloseLabel", inlineMediaPlayerCloseLabel)
    .put("inlineMediaPlayerShowControlsLabel", inlineMediaPlayerShowControlsLabel)
    .put("inlineMediaPlayerHideControlsLabel", inlineMediaPlayerHideControlsLabel)
    .put("safeAreaLayoutQuietPeriodMillis", safeAreaLayoutQuietPeriodMillis)
    .put("safeAreaRequiredFailureCount", safeAreaRequiredFailureCount)
    .put(
        "compatibilityRequestHosts",
        JSONArray(compatibilityRequestHosts.sorted()),
    )
    .put("pausedHosts", JSONArray(pausedHosts.sorted()))
    .put("hiddenElements", JSONArray(hiddenDistractions))
    .put(
        "rules",
        JSONArray().also { values ->
            candyRules.asSequence()
                .filter { rule ->
                    rule.active && rule.kind != CandyRuleKind.CosmeticCss &&
                        rule.requestHost != null
                }
                .sortedBy(CandyRule::id)
                .forEach { rule ->
                    values.put(
                        JSONObject()
                            .put("id", rule.id)
                            .put("a", if (rule.action == CandyRuleAction.Allow) "A" else "B")
                            .put("k", if (rule.kind == CandyRuleKind.HostPair) "P" else "H")
                            .put("r", rule.requestHost)
                            .put("f", rule.firstPartyHost),
                    )
                }
        },
    )
    .put(
        "cosmetics",
        JSONArray().also { values ->
            candyRules.asSequence()
                .filter { rule ->
                    rule.active && rule.kind == CandyRuleKind.CosmeticCss &&
                        rule.action == CandyRuleAction.Cosmetic &&
                        rule.firstPartyHost != null && rule.cosmeticSelector != null
                }
                .sortedBy(CandyRule::id)
                .take(64)
                .forEach { rule ->
                    values.put(
                        JSONObject()
                            .put("id", rule.id)
                            .put("h", rule.firstPartyHost)
                            .put("s", rule.cosmeticSelector),
                    )
                }
        },
    )

internal fun pictureInPicturePlaybackMessage(
    token: String,
    revision: Long,
    expected: Boolean,
): JSONObject = JSONObject()
    .put("type", "picture-in-picture-playback")
    .put("protocolVersion", CandyPrivacyHostContract.PROTOCOL_VERSION)
    .put("token", token)
    .put("revision", revision)
    .put("expected", expected)

internal fun pictureInPicturePreparationMessage(
    token: String,
    revision: Long,
    navigationGeneration: Int,
    requestId: Long,
    identity: GeckoInlineVideoIdentity,
): JSONObject = pictureInPicturePlaybackMessage(
    token = token,
    revision = revision,
    expected = true,
)
    .put("navigationGeneration", navigationGeneration)
    .put("requestId", requestId)
    .put("documentNonce", identity.documentNonce)
    .put("elementNonce", identity.elementNonce)

internal fun pictureInPictureRestorationMessage(
    token: String,
    revision: Long,
    navigationGeneration: Int,
    requestId: Long,
): JSONObject = pictureInPicturePlaybackMessage(
    token = token,
    revision = revision,
    expected = false,
)
    .put("navigationGeneration", navigationGeneration)
    .put("requestId", requestId)

internal fun inlineVideoPresentationMessage(
    token: String,
    revision: Long,
    navigationGeneration: Int,
    requestId: Long,
    identity: GeckoInlineVideoIdentity?,
    expected: Boolean,
): JSONObject = JSONObject()
    .put("type", "inline-video-presentation")
    .put("protocolVersion", CandyPrivacyHostContract.PROTOCOL_VERSION)
    .put("token", token)
    .put("revision", revision)
    .put("navigationGeneration", navigationGeneration)
    .put("requestId", requestId)
    .put("expected", expected)
    .put("documentNonce", identity?.documentNonce.orEmpty())
    .put("elementNonce", identity?.elementNonce.orEmpty())

internal data class GeckoInlineVideoState(
    val isActive: Boolean,
    val isPlaying: Boolean,
    val isPresented: Boolean,
    val width: Int,
    val height: Int,
    val documentNonce: String?,
    val elementNonce: String?,
)

internal data class GeckoPictureInPicturePreparation(
    val identity: GeckoInlineVideoIdentity,
    val videoRect: BrowserViewportRect,
)

internal fun geckoPictureInPicturePreparationFromMessage(
    message: JSONObject,
    currentRevision: Long,
    currentNavigationGeneration: Int,
    expectedRequestId: Long,
    expectedIdentity: GeckoInlineVideoIdentity,
): GeckoPictureInPicturePreparation? {
    if (
        message.optLong("revision", -1) != currentRevision ||
        message.optInt("navigationGeneration", -1) != currentNavigationGeneration ||
        message.optLong("requestId", -1) != expectedRequestId ||
        message.optBoolean("prepared", false) != true ||
        message.optString("documentNonce") != expectedIdentity.documentNonce ||
        message.optString("elementNonce") != expectedIdentity.elementNonce
    ) {
        return null
    }
    val viewportWidth = message.optDouble("viewportWidth", Double.NaN)
    val viewportHeight = message.optDouble("viewportHeight", Double.NaN)
    val videoLeft = message.optDouble("videoLeft", Double.NaN)
    val videoTop = message.optDouble("videoTop", Double.NaN)
    val videoRight = message.optDouble("videoRight", Double.NaN)
    val videoBottom = message.optDouble("videoBottom", Double.NaN)
    if (
        !listOf(
            viewportWidth,
            viewportHeight,
            videoLeft,
            videoTop,
            videoRight,
            videoBottom,
        ).all(Double::isFinite) ||
        viewportWidth <= 0.0 ||
        viewportHeight <= 0.0 ||
        videoRight <= videoLeft ||
        videoBottom <= videoTop ||
        videoLeft < 0.0 ||
        videoTop < 0.0 ||
        videoRight > viewportWidth ||
        videoBottom > viewportHeight
    ) {
        return null
    }
    return GeckoPictureInPicturePreparation(
        identity = expectedIdentity,
        videoRect = BrowserViewportRect(
            leftFraction = (videoLeft / viewportWidth).toFloat(),
            topFraction = (videoTop / viewportHeight).toFloat(),
            rightFraction = (videoRight / viewportWidth).toFloat(),
            bottomFraction = (videoBottom / viewportHeight).toFloat(),
        ),
    )
}

internal fun isGeckoPictureInPictureRestorationResult(
    message: JSONObject,
    currentRevision: Long,
    currentNavigationGeneration: Int,
    expectedRequestId: Long,
): Boolean =
    message.optLong("revision", -1) == currentRevision &&
        message.optInt("navigationGeneration", -1) == currentNavigationGeneration &&
        message.optLong("requestId", -1) == expectedRequestId &&
        message.optBoolean("prepared", false)

internal enum class GeckoPictureInPicturePlaybackResultRoute {
    Preparation,
    Restoration,
    Stale,
}

internal fun geckoPictureInPicturePlaybackResultRoute(
    message: JSONObject,
    preparationRequestId: Long?,
    restorationRequestId: Long?,
): GeckoPictureInPicturePlaybackResultRoute = when (message.optLong("requestId", -1)) {
    preparationRequestId -> GeckoPictureInPicturePlaybackResultRoute.Preparation
    restorationRequestId -> GeckoPictureInPicturePlaybackResultRoute.Restoration
    else -> GeckoPictureInPicturePlaybackResultRoute.Stale
}

internal fun shouldClearInlineVideoPresentationDesire(
    wasPresented: Boolean,
    isPresented: Boolean,
    presentationDesired: Boolean,
): Boolean = presentationDesired && wasPresented && !isPresented

internal fun geckoInlineVideoStateFromMessage(
    message: JSONObject,
    currentRevision: Long,
    currentNavigationGeneration: Int,
): GeckoInlineVideoState? {
    if (
        message.optLong("revision", -1) != currentRevision ||
        message.optInt("navigationGeneration", -1) != currentNavigationGeneration
    ) {
        return null
    }
    val width = message.optInt("videoWidth", -1)
    val height = message.optInt("videoHeight", -1)
    if (width !in 0..MAX_INLINE_VIDEO_DIMENSION || height !in 0..MAX_INLINE_VIDEO_DIMENSION) {
        return null
    }
    val active = message.optBoolean("active", false)
    val documentNonce = message.optString("documentNonce")
    val elementNonce = message.optString("elementNonce")
    if (active && (!INLINE_VIDEO_NONCE.matches(documentNonce) ||
            !INLINE_VIDEO_NONCE.matches(elementNonce))
    ) {
        return null
    }
    return GeckoInlineVideoState(
        isActive = active,
        isPlaying = active && message.optBoolean("playing", false),
        isPresented = active && message.optBoolean("presented", false),
        width = width.takeIf { active } ?: 0,
        height = height.takeIf { active } ?: 0,
        documentNonce = documentNonce.takeIf { active },
        elementNonce = elementNonce.takeIf { active },
    )
}

internal fun geckoInlineVideoOpenRequestFromMessage(
    message: JSONObject,
    currentRevision: Long,
    currentNavigationGeneration: Int,
): GeckoInlineVideoOpenRequest? {
    if (
        message.optLong("revision", -1) != currentRevision ||
        message.optInt("navigationGeneration", -1) != currentNavigationGeneration
    ) {
        return null
    }
    val documentNonce = message.optString("documentNonce")
    val elementNonce = message.optString("elementNonce")
    if (!INLINE_VIDEO_NONCE.matches(documentNonce) || !INLINE_VIDEO_NONCE.matches(elementNonce)) {
        return null
    }
    return GeckoInlineVideoOpenRequest(
        identity = GeckoInlineVideoIdentity(
            documentNonce = documentNonce,
            elementNonce = elementNonce,
        ),
        navigationGeneration = currentNavigationGeneration,
        expected = message.optBoolean("expected", true),
    )
}

internal fun geckoInlineVideoGestureHapticFromMessage(
    message: JSONObject,
    currentRevision: Long,
    currentNavigationGeneration: Int,
): GeckoInlineVideoGestureHaptic? {
    if (
        message.optLong("revision", -1) != currentRevision ||
        message.optInt("navigationGeneration", -1) != currentNavigationGeneration
    ) {
        return null
    }
    val documentNonce = message.optString("documentNonce")
    val elementNonce = message.optString("elementNonce")
    if (!INLINE_VIDEO_NONCE.matches(documentNonce) || !INLINE_VIDEO_NONCE.matches(elementNonce)) {
        return null
    }
    val phase = GeckoInlineVideoGestureHapticPhase.entries.firstOrNull { candidate ->
        candidate.stableId == message.optString("phase")
    } ?: return null
    return GeckoInlineVideoGestureHaptic(
        identity = GeckoInlineVideoIdentity(
            documentNonce = documentNonce,
            elementNonce = elementNonce,
        ),
        navigationGeneration = currentNavigationGeneration,
        phase = phase,
    )
}

internal fun geckoScrollMetricsFromMessage(
    message: JSONObject,
    currentRevision: Long,
): BrowserEngineScrollMetrics? {
    if (message.optLong("revision", -1) != currentRevision) return null
    val offsetPx = message.optLong("offsetPx", -1)
    val extentPx = message.optLong("extentPx", -1)
    val rangePx = message.optLong("rangePx", -1)
    if (
        offsetPx !in 0..Int.MAX_VALUE.toLong() ||
        extentPx !in 1..Int.MAX_VALUE.toLong() ||
        rangePx !in extentPx..Int.MAX_VALUE.toLong()
    ) {
        return null
    }
    return BrowserEngineScrollMetrics(
        offsetPx = offsetPx.coerceAtMost(rangePx - extentPx).toInt(),
        extentPx = extentPx.toInt(),
        rangePx = rangePx.toInt(),
    )
}

private const val MAX_INLINE_VIDEO_DIMENSION = 16_384
private val INLINE_VIDEO_NONCE = Regex("[a-f0-9]{32}")
