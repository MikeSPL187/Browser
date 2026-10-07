package dev.sk2andy.materialbrowser.browser.systemwebview

import org.json.JSONObject

internal data class SystemWebViewMediaState(
    val isActive: Boolean,
    val isPlaying: Boolean,
    val isFullscreen: Boolean,
    val title: String?,
    val currentPositionMillis: Long,
    val durationMillis: Long?,
    val playbackRate: Float,
    val sourceUrl: String?,
    val videoWidth: Int,
    val videoHeight: Int,
    val isVideo: Boolean,
)

internal object SystemWebViewMediaBridge {
    const val NAME = "CandySystemMediaBridge"
    const val MAX_MESSAGE_LENGTH = 4_096

    fun parse(raw: String?, expectedToken: String): SystemWebViewMediaState? {
        if (raw == null || raw.length !in 1..MAX_MESSAGE_LENGTH) return null
        return runCatching {
            val json = JSONObject(raw)
            if (json.optInt("v") != 1 || json.optString("token") != expectedToken) return null
            val duration = json.optDouble("duration", Double.NaN)
                .takeIf(Double::isFinite)
                ?.coerceIn(0.0, MAX_MEDIA_SECONDS)
                ?.times(1_000)
                ?.toLong()
            SystemWebViewMediaState(
                isActive = json.optBoolean("active"),
                isPlaying = json.optBoolean("playing"),
                isFullscreen = json.optBoolean("fullscreen"),
                title = json.optString("title").trim().take(MAX_TITLE_LENGTH).ifBlank { null },
                currentPositionMillis = json.optDouble("position", 0.0)
                    .takeIf(Double::isFinite)
                    ?.coerceIn(0.0, MAX_MEDIA_SECONDS)
                    ?.times(1_000)
                    ?.toLong()
                    ?: 0,
                durationMillis = duration,
                playbackRate = json.optDouble("rate", 1.0)
                    .takeIf(Double::isFinite)
                    ?.coerceIn(0.1, 16.0)
                    ?.toFloat()
                    ?: 1f,
                sourceUrl = json.optString("source").trim().take(MAX_URL_LENGTH)
                    .ifBlank { null },
                videoWidth = json.optInt("videoWidth").coerceIn(0, MAX_DIMENSION),
                videoHeight = json.optInt("videoHeight").coerceIn(0, MAX_DIMENSION),
                isVideo = json.optBoolean("video"),
            )
        }.getOrNull()
    }

    fun script(token: String): String {
        require(token.matches(Regex("[A-Za-z0-9_-]{32,80}")))
        return """
            (() => {
              if (globalThis.__candySystemMediaInstalled) return;
              const bridge = globalThis.$NAME;
              if (!bridge || typeof bridge.postMessage !== 'function') return;
              globalThis.__candySystemMediaInstalled = true;
              const token = '$token';
              let active = null;
              let lastTimeReport = 0;
              const inFullscreen = media => {
                const element = document.fullscreenElement;
                return !!element && (element === media || element.contains(media));
              };
              const isLive = media => media.isConnected && !media.ended &&
                (!media.paused || inFullscreen(media));
              // One media element owns the published state. Passive events of other media
              // (metadata, pause, volume) must not replace a playing or fullscreen owner; only a
              // fullscreen element, or a new play outside an owner's fullscreen, takes over.
              const owns = (media, takeover) => {
                if (media === active || !active || !isLive(active) || inFullscreen(media)) {
                  return true;
                }
                return takeover && !inFullscreen(active);
              };
              const report = (media, force = false, takeover = false) => {
                if (!(media instanceof HTMLMediaElement)) return;
                if (!owns(media, takeover)) return;
                const now = performance.now();
                if (!force && now - lastTimeReport < 500) return;
                lastTimeReport = now;
                active = media;
                const isVideo = media instanceof HTMLVideoElement;
                bridge.postMessage(JSON.stringify({
                  v: 1,
                  token,
                  active: !media.ended,
                  playing: !media.paused && !media.ended,
                  fullscreen: inFullscreen(media),
                  title: document.title || '',
                  position: Number.isFinite(media.currentTime) ? media.currentTime : 0,
                  duration: Number.isFinite(media.duration) ? media.duration : null,
                  rate: Number.isFinite(media.playbackRate) ? media.playbackRate : 1,
                  source: media.currentSrc || media.src || '',
                  videoWidth: isVideo ? media.videoWidth : 0,
                  videoHeight: isVideo ? media.videoHeight : 0,
                  video: isVideo
                }));
              };
              const fullscreenMedia = () => {
                const element = document.fullscreenElement;
                if (!element) return null;
                if (element instanceof HTMLMediaElement) return element;
                if (active && element.contains(active)) return active;
                const candidates = Array.from(element.querySelectorAll('video, audio'));
                return candidates.find(media => !media.paused && !media.ended) ||
                  candidates[0] || null;
              };
              document.addEventListener('play', e => report(e.target, true, true), true);
              for (const event of ['pause', 'ended', 'loadedmetadata', 'durationchange',
                                   'ratechange', 'volumechange']) {
                document.addEventListener(event, e => report(e.target, true), true);
              }
              document.addEventListener('timeupdate', e => report(e.target), true);
              document.addEventListener('fullscreenchange', () => {
                const media = fullscreenMedia() || active;
                if (media) report(media, true);
              }, true);
            })();
        """.trimIndent()
    }

    private const val MAX_MEDIA_SECONDS = 604_800.0
    private const val MAX_TITLE_LENGTH = 512
    private const val MAX_URL_LENGTH = 2_048
    private const val MAX_DIMENSION = 16_384
}
