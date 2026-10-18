package dev.sk2andy.materialbrowser.browser.credentials

import androidx.annotation.WorkerThread
import dev.sk2andy.materialbrowser.shared.credentials.PasswordHealthRules
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Where «Change password on the site» opens. Few sites serve `/.well-known/change-password`, so it
 * is asked first, as Chrome and Safari do, and a site without it opens at its start page instead
 * of a 404. Two requests to the site itself, without cookies; nothing about the login is sent.
 */
internal object ChangePasswordPage {
    private const val TIMEOUT_MILLIS = 5_000

    @WorkerThread
    fun resolve(origin: String): String = PasswordHealthRules.changePasswordTarget(
        origin,
        changePasswordStatus = status(PasswordHealthRules.changePasswordUrl(origin)),
        missingPageStatus = status(PasswordHealthRules.missingPageUrl(origin)),
    )

    /** The status after redirects; null when the site did not answer. */
    private fun status(url: String): Int? {
        var connection: HttpURLConnection? = null
        return try {
            connection = URL(url).openConnection() as HttpURLConnection
            connection.connectTimeout = TIMEOUT_MILLIS
            connection.readTimeout = TIMEOUT_MILLIS
            connection.useCaches = false
            connection.instanceFollowRedirects = true
            connection.setRequestProperty("User-Agent", "Vola-Browser")
            connection.responseCode
        } catch (_: IOException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        } finally {
            connection?.disconnect()
        }
    }
}
