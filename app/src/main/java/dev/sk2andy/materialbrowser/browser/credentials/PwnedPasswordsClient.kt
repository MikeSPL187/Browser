package dev.sk2andy.materialbrowser.browser.credentials

import androidx.annotation.WorkerThread
import dev.sk2andy.materialbrowser.shared.credentials.PasswordHealthRules
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/**
 * The leak check of the password check (Q21b), off until the user turns it on. Pwned Passwords is
 * asked with k-anonymity: only the first five characters of the password's SHA-1 leave the phone,
 * the answer lists every leaked hash with that start (padded, so its size says nothing), and the
 * match happens here. Nothing about the login, the site or the user name is sent.
 */
internal class PwnedPasswordsClient(
    private val fetchRange: (prefix: String) -> String? = ::download,
) {
    private val ranges = mutableMapOf<String, String>()

    /** How often [password] was seen in leaks; null when the check could not be made. */
    @WorkerThread
    fun timesSeen(password: String): Int? {
        val hash = sha1Hex(password)
        val prefix = hash.take(PasswordHealthRules.HASH_PREFIX_LENGTH)
        val body = synchronized(ranges) { ranges[prefix] } ?: fetchRange(prefix)?.also { fetched ->
            synchronized(ranges) { ranges[prefix] = fetched }
        } ?: return null
        return PasswordHealthRules.timesSeen(body, hash.drop(PasswordHealthRules.HASH_PREFIX_LENGTH))
    }

    internal companion object {
        private const val RANGE_URL = "https://api.pwnedpasswords.com/range/"
        private const val TIMEOUT_MILLIS = 10_000
        private const val MAX_BODY_CHARS = 2_000_000
        private const val BUFFER_CHARS = 8_192

        fun sha1Hex(text: String): String =
            MessageDigest.getInstance("SHA-1").digest(text.toByteArray(Charsets.UTF_8))
                .joinToString("") { byte -> "%02X".format(byte) }

        private fun download(prefix: String): String? {
            if (prefix.length != PasswordHealthRules.HASH_PREFIX_LENGTH || prefix.any { it !in HEX }) return null
            val connection = URL(RANGE_URL + prefix).openConnection() as HttpURLConnection
            return try {
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
                connection.disconnect()
            }
        }

        private const val HEX = "0123456789ABCDEF"
    }
}
