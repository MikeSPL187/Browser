package dev.sk2andy.materialbrowser.data

import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.text.Normalizer
import java.util.Locale

data class BrowserDownloadRequest(
    val url: String,
    val fileName: String,
    val mimeType: String,
    val userAgent: String? = null,
    val cookies: String? = null,
    val referrer: String? = null,
)

object BrowserDownloadRequestFactory {
    fun create(
        url: String,
        contentDisposition: String? = null,
        mimeType: String? = null,
        userAgent: String? = null,
        cookies: String? = null,
        referrer: String? = null,
    ): BrowserDownloadRequest? {
        if (!SafeDownloadValues.isHttpUrl(url)) return null
        val candidateMimeType = SafeDownloadValues.mimeType(mimeType, url)
        val fileName = SafeDownloadValues.fileName(url, contentDisposition, candidateMimeType)
        val safeMimeType = SafeDownloadValues.finalMimeType(fileName, candidateMimeType)
        return BrowserDownloadRequest(
            url = url,
            fileName = fileName,
            mimeType = safeMimeType,
            userAgent = SafeDownloadValues.header(userAgent),
            cookies = SafeDownloadValues.header(cookies, MAX_COOKIE_LENGTH),
            referrer = SafeDownloadValues.referrer(referrer, url),
        )
    }

    fun isAndroidPackage(request: BrowserDownloadRequest): Boolean =
        request.mimeType.equals(ANDROID_PACKAGE_MIME_TYPE, ignoreCase = true) ||
            request.fileName.endsWith(".apk", ignoreCase = true)

    private const val MAX_COOKIE_LENGTH = 16_384
    internal const val ANDROID_PACKAGE_MIME_TYPE = "application/vnd.android.package-archive"
}

internal object SafeDownloadValues {
    // `\p{Cc}` is every control character, C1 included; `\p{Cntrl}` is only the ASCII ones.
    private val invalidFileNameCharacters = Regex("[\\\\/:*?\"<>|\\p{Cc}\\p{Cf}\\p{Zl}\\p{Zp}]")
    private val mimeTypePattern = Regex("^[a-zA-Z0-9!#$&^_.+-]+/[a-zA-Z0-9!#$&^_.+*-]+$")
    private const val MAX_FILE_NAME_LENGTH = 120

    /** Under the 255 bytes a file system allows a name, with room for Android's ` (1)`. */
    private const val MAX_FILE_NAME_BYTES = 240

    fun isHttpUrl(value: String): Boolean = runCatching {
        val uri = URI(value.trim())
        (uri.scheme.equals("http", ignoreCase = true) ||
            uri.scheme.equals("https", ignoreCase = true)) &&
            !uri.host.isNullOrBlank() &&
            uri.userInfo == null
    }.getOrDefault(false)

    fun mimeType(value: String?, url: String? = null): String {
        val candidate = value
            ?.substringBefore(';')
            ?.trim()
            ?.lowercase(Locale.ROOT)
            .orEmpty()
        return candidate.takeIf(mimeTypePattern::matches)
            ?: mimeTypeFromUrl(url)
            ?: "application/octet-stream"
    }

    /**
     * The type a file is saved and opened with once its final [fileName] is known: an `.apk` is
     * always an Android package, whatever generic type the server sent.
     */
    fun finalMimeType(fileName: String, mimeType: String): String =
        if (fileName.endsWith(".apk", ignoreCase = true)) {
            BrowserDownloadRequestFactory.ANDROID_PACKAGE_MIME_TYPE
        } else {
            mimeType
        }

    fun header(value: String?, maxLength: Int = 4_096): String? = value
        ?.takeIf { it.isNotBlank() && it.length <= maxLength && '\r' !in it && '\n' !in it }

    fun referrer(value: String?, targetUrl: String): String? = runCatching {
        val source = URI(value?.trim() ?: return null)
        val target = URI(targetUrl.trim())
        if (!isHttpUrl(source.toString()) || !isHttpUrl(target.toString())) return null
        if (source.scheme.equals("https", ignoreCase = true) &&
            target.scheme.equals("http", ignoreCase = true)
        ) {
            return null
        }
        val sameOrigin = source.scheme.equals(target.scheme, ignoreCase = true) &&
            source.host.equals(target.host, ignoreCase = true) &&
            effectivePort(source) == effectivePort(target)
        val safe = URI(
            source.scheme,
            null,
            source.host,
            source.port.takeUnless { it == defaultPort(source.scheme) } ?: -1,
            source.path.takeIf { sameOrigin },
            source.query.takeIf { sameOrigin },
            null,
        )
        safe.toString()
    }.getOrNull()?.let { header(it) }

    private fun effectivePort(uri: URI): Int =
        uri.port.takeUnless { it == -1 } ?: defaultPort(uri.scheme)

    private fun defaultPort(scheme: String?): Int = when {
        scheme.equals("http", ignoreCase = true) -> 80
        scheme.equals("https", ignoreCase = true) -> 443
        else -> -1
    }

    fun fileName(url: String, contentDisposition: String?, mimeType: String): String {
        val candidate = contentDispositionFileName(contentDisposition)
            ?: urlFileName(url)
            ?: "download"
        // NFKC first: a lookalike dot (U+2024, U+FF0E) or slash becomes the real one, so the
        // checks below and the download safety check see `invoice.pdf.apk` for what it is.
        val sanitized = Normalizer.normalize(candidate, Normalizer.Form.NFKC)
            .replace(invalidFileNameCharacters, "_")
            .trim()
            .trim('.')
            .ifEmpty { "download" }
        val extension = extensionForMimeType(mimeType)
        val withExtension = if (extension != null && !hasExtension(sanitized)) {
            "$sanitized.$extension"
        } else {
            sanitized
        }
        if (withExtension.length <= MAX_FILE_NAME_LENGTH && utf8Size(withExtension) <= MAX_FILE_NAME_BYTES) {
            return withExtension
        }
        val suffix = withExtension.substringAfterLast('.', missingDelimiterValue = "")
            .takeIf { it.isNotEmpty() && it.length <= 10 }
            ?.let { ".$it" }
            .orEmpty()
        return prefixWithin(
            withExtension,
            maxChars = MAX_FILE_NAME_LENGTH - suffix.length,
            maxBytes = MAX_FILE_NAME_BYTES - utf8Size(suffix),
        ).trimEnd() + suffix
    }

    /** The longest start of [value] within both limits that never splits a character in two. */
    private fun prefixWithin(value: String, maxChars: Int, maxBytes: Int): String {
        var end = 0
        var bytes = 0
        while (end < value.length) {
            val codePoint = value.codePointAt(end)
            val next = end + Character.charCount(codePoint)
            val size = utf8Size(codePoint)
            if (next > maxChars || bytes + size > maxBytes) break
            bytes += size
            end = next
        }
        return value.substring(0, end)
    }

    private fun utf8Size(value: String): Int {
        var bytes = 0
        var index = 0
        while (index < value.length) {
            val codePoint = value.codePointAt(index)
            bytes += utf8Size(codePoint)
            index += Character.charCount(codePoint)
        }
        return bytes
    }

    private fun utf8Size(codePoint: Int): Int = when {
        codePoint < 0x80 -> 1
        codePoint < 0x800 -> 2
        codePoint < 0x10000 -> 3
        else -> 4
    }

    private fun contentDispositionFileName(value: String?): String? {
        if (value.isNullOrBlank()) return null
        val encoded = Regex("filename\\*\\s*=\\s*UTF-8'[^']*'([^;]+)", RegexOption.IGNORE_CASE)
            .find(value)
            ?.groupValues
            ?.getOrNull(1)
            ?.trim()
            ?.trim('"')
        if (!encoded.isNullOrBlank()) {
            return runCatching {
                URLDecoder.decode(encoded.replace("+", "%2B"), StandardCharsets.UTF_8.name())
            }.getOrNull()
        }
        return Regex("filename\\s*=\\s*(?:\"([^\"]+)\"|([^;]+))", RegexOption.IGNORE_CASE)
            .find(value)
            ?.let { match -> match.groupValues[1].ifBlank { match.groupValues[2] } }
            ?.trim()
    }

    private fun urlFileName(value: String): String? = runCatching {
        URI(value).rawPath
            ?.substringAfterLast('/')
            ?.takeIf(String::isNotBlank)
            ?.let { URLDecoder.decode(it.replace("+", "%2B"), StandardCharsets.UTF_8.name()) }
    }.getOrNull()

    private fun hasExtension(value: String): Boolean {
        val extension = value.substringAfterLast('.', missingDelimiterValue = "")
        return extension.matches(Regex("^[a-zA-Z0-9][a-zA-Z0-9+_-]{0,9}$"))
    }

    private fun mimeTypeFromUrl(value: String?): String? = runCatching {
        URI(value ?: return null).path.substringAfterLast('.').lowercase(Locale.ROOT)
    }.getOrNull()?.let { extension ->
        when (extension) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "gif" -> "image/gif"
            "webp" -> "image/webp"
            "avif" -> "image/avif"
            "svg" -> "image/svg+xml"
            "pdf" -> "application/pdf"
            "txt" -> "text/plain"
            "html", "htm" -> "text/html"
            "json" -> "application/json"
            "apk" -> BrowserDownloadRequestFactory.ANDROID_PACKAGE_MIME_TYPE
            else -> null
        }
    }

    private fun extensionForMimeType(mimeType: String): String? = when (mimeType) {
        "image/jpeg" -> "jpg"
        "image/png" -> "png"
        "image/gif" -> "gif"
        "image/webp" -> "webp"
        "image/avif" -> "avif"
        "image/svg+xml" -> "svg"
        "application/pdf" -> "pdf"
        "text/plain" -> "txt"
        "text/html" -> "html"
        "application/json" -> "json"
        BrowserDownloadRequestFactory.ANDROID_PACKAGE_MIME_TYPE -> "apk"
        else -> null
    }
}
