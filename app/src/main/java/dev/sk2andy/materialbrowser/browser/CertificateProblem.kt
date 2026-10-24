package dev.sk2andy.materialbrowser.browser

import java.net.IDN
import java.security.cert.X509Certificate
import java.util.Locale

/**
 * Why a site's certificate failed (canvas screen «CertError»), as far as the certificate itself
 * tells: the engine only says it is bad. Engine neutral: both engines hand over an
 * [X509Certificate].
 */
enum class CertificateProblem {
    /** The certificate's term ended. */
    Expired,

    /** The certificate's term has not begun yet: often the phone's clock is wrong. */
    NotYetValid,

    /** The certificate names other sites. */
    WrongSite,

    /** The site signed its certificate itself; nobody vouches for it. */
    SelfSigned,

    /** Anything else: an unknown or revoked issuer, a broken chain. */
    Untrusted,
}

internal object CertificateProblemRules {
    fun problem(certificate: X509Certificate?, host: String, nowMillis: Long): CertificateProblem {
        val cert = certificate ?: return CertificateProblem.Untrusted
        val notAfter = cert.notAfter?.time
        val notBefore = cert.notBefore?.time
        val names = runCatching {
            cert.subjectAlternativeNames.orEmpty()
                .filter { entry -> entry.getOrNull(0) == DNS_NAME }
                .mapNotNull { entry -> entry.getOrNull(1) as? String }
        }.getOrDefault(emptyList()).ifEmpty {
            listOfNotNull(SiteCertificateRules.x500Attribute(cert.subjectX500Principal.name, "CN"))
        }
        return problem(
            host = host,
            names = names,
            notBeforeMillis = notBefore,
            notAfterMillis = notAfter,
            selfSigned = cert.subjectX500Principal == cert.issuerX500Principal,
            nowMillis = nowMillis,
        )
    }

    /** The term comes first: it is what the phone's date can explain, then the site's name. */
    fun problem(
        host: String,
        names: List<String>,
        notBeforeMillis: Long?,
        notAfterMillis: Long?,
        selfSigned: Boolean,
        nowMillis: Long,
    ): CertificateProblem = when {
        notAfterMillis != null && nowMillis > notAfterMillis -> CertificateProblem.Expired
        notBeforeMillis != null && nowMillis < notBeforeMillis -> CertificateProblem.NotYetValid
        names.isNotEmpty() && names.none { matches(it, host) } -> CertificateProblem.WrongSite
        selfSigned -> CertificateProblem.SelfSigned
        else -> CertificateProblem.Untrusted
    }

    /** RFC 6125: exact, or one leftmost `*` label standing for one label of the host. */
    fun matches(name: String, host: String): Boolean {
        val pattern = ascii(name.trim().trimEnd('.'))
        val target = ascii(host.trim().trimEnd('.'))
        if (pattern.isEmpty() || target.isEmpty()) return false
        if (!pattern.startsWith("*.")) return pattern == target
        val suffix = pattern.removePrefix("*")
        val label = target.removeSuffix(suffix)
        return target.endsWith(suffix) && label.isNotEmpty() && '.' !in label && suffix.count { it == '.' } >= 2
    }

    private fun ascii(host: String): String =
        runCatching { IDN.toASCII(host, IDN.ALLOW_UNASSIGNED) }.getOrDefault(host).lowercase(Locale.ROOT)

    /** `GeneralName` type of a DNS name in [X509Certificate.getSubjectAlternativeNames]. */
    private const val DNS_NAME = 2
}
