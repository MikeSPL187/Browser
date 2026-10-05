package dev.sk2andy.materialbrowser.browser.credentials

import dev.sk2andy.materialbrowser.shared.credentials.TotpAlgorithm
import dev.sk2andy.materialbrowser.shared.credentials.TotpConfig
import dev.sk2andy.materialbrowser.shared.credentials.TotpRules
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/** Two-factor codes on the phone: [TotpRules] with the platform's HMAC. */
internal object TotpCodes {
    fun code(config: TotpConfig, timeMillis: Long): String = TotpRules.code(config, timeMillis, ::hmac)

    private fun hmac(algorithm: TotpAlgorithm, key: ByteArray, message: ByteArray): ByteArray {
        val name = when (algorithm) {
            TotpAlgorithm.Sha1 -> "HmacSHA1"
            TotpAlgorithm.Sha256 -> "HmacSHA256"
            TotpAlgorithm.Sha512 -> "HmacSHA512"
        }
        return Mac.getInstance(name).run {
            init(SecretKeySpec(key, name))
            doFinal(message)
        }
    }
}
