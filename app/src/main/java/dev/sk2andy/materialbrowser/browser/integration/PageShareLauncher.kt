package dev.sk2andy.materialbrowser.browser.integration

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent

data class PageShareRequest(
    val url: String?,
    val title: String,
    val text: String = "",
) {
    /** EXTRA_TEXT for the receiving app: the shared text, then the link; a bare title otherwise. */
    val sharedText: String
        get() = listOfNotNull(text.takeIf(String::isNotEmpty), url)
            .joinToString(separator = "\n")
            .ifEmpty { title }

    companion object {
        fun create(url: String, title: String): PageShareRequest? {
            val normalizedUrl = BrowserUriPolicy.normalizeHttpUrl(url) ?: return null
            return PageShareRequest(
                url = normalizedUrl,
                title = title.trim(),
            )
        }

        /**
         * A site's Web Share call: title, text and url are separate optional members, so text-only
         * and title-only shares are valid. A url that is present must still be a safe web link.
         */
        fun createWebShare(url: String?, title: String?, text: String?): PageShareRequest? {
            val normalizedUrl = url?.let { value ->
                BrowserUriPolicy.normalizeHttpUrl(value) ?: return null
            }
            val request = PageShareRequest(
                url = normalizedUrl,
                title = title?.trim().orEmpty(),
                text = text?.trim().orEmpty(),
            )
            return request.takeIf { candidate -> candidate.sharedText.isNotEmpty() }
        }
    }
}

sealed interface PageShareResult {
    data object Launched : PageShareResult
    data object Unsupported : PageShareResult
}

class PageShareLauncher(private val context: Context) {
    fun launch(request: PageShareRequest): PageShareResult {
        val target = Intent(Intent.ACTION_SEND)
            .setType("text/plain")
            .putExtra(Intent.EXTRA_TEXT, request.sharedText)
            .apply {
                request.title.takeIf(String::isNotEmpty)?.let { title ->
                    putExtra(Intent.EXTRA_TITLE, title)
                    putExtra(Intent.EXTRA_SUBJECT, title)
                }
            }
        val chooser = Intent.createChooser(target, null).apply {
            if (context !is Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(chooser)
            PageShareResult.Launched
        } catch (_: ActivityNotFoundException) {
            PageShareResult.Unsupported
        } catch (_: SecurityException) {
            PageShareResult.Unsupported
        }
    }
}
