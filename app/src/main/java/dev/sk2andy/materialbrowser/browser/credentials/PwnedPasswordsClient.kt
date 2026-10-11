package dev.sk2andy.materialbrowser.browser.credentials

import androidx.annotation.WorkerThread
import dev.sk2andy.materialbrowser.shared.credentials.PasswordHealthRules
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/**
 * Pwned Passwords over k-anonymity, for the leak check of the password check ([PwnedPasswordCheck]).
 * Only the first five characters of the password's SHA-1 leave the phone; the answer lists every
 * leaked hash with that start (padded, so its size says nothing), and the match happens here.
 * Nothing about the login, the site or the user name is sent.
 */
internal object PwnedPasswordsClient {
    private const val RANGE_URL = "https://api.pwnedpasswords.com/range/"
    private const val TIMEOUT_MILLIS = 10_000
    private const val MAX_BODY_CHARS = 2_000_000
    private const val BUFFER_CHARS = 8_192
    private const val HEX = "0123456789ABCDEF"

    /** The password's SHA-1 in upper-case hex, as Pwned Passwords lists it. */
    fun sha1Hex(text: String): String =
        MessageDigest.getInstance("SHA-1").digest(text.toByteArray(Charsets.UTF_8))
            .joinToString("") { byte -> "%02X".format(byte) }

    /**
     * Every leaked hash that starts with [prefix] (five upper-case hex characters), as `SUFFIX:COUNT`
     * lines; null when [prefix] is not one or the request failed. Nothing is sent for a bad prefix.
     */
    @WorkerThread
    fun range(prefix: String): String? {
        if (prefix.length != PasswordHealthRules.HASH_PREFIX_LENGTH || prefix.any { it !in HEX }) return null
        var connection: HttpURLConnection? = null
        return try {
            connection = URL(RANGE_URL + prefix).openConnection() as HttpURLConnection
            connection.connectTimeout = TIMEOUT_MILLIS
            connection.readTimeout = TIMEOUT_MILLIS
            connection.useCaches = false
            // Padding hides how many hashes share the prefix from anyone watching the traffic.
            connection.setRequestProperty("Add-Padding", "true")
            connection.setRequestProperty("User-Agent", "Vola-Browser")
            if (connection.responseCode != HttpURLConnection.HTTP_OK) return null
            connection.inputStream.bufferedReader().use { reader ->
                // A range answer is a few hundred kilobytes; anything far larger is not one.
                val text = StringBuilder()
                val buffer = CharArray(BUFFER_CHARS)
                while (true) {
                    val read = reader.read(buffer)
                    if (read < 0) break
                    if (text.length + read > MAX_BODY_CHARS) return null
                    text.append(buffer, 0, read)
                }
                text.toString()
            }
        } catch (_: IOException) {
            null
        } finally {
            connection?.disconnect()
        }
    }
}
