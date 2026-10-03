package dev.sk2andy.materialbrowser.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.browser.SiteCertificate
import dev.sk2andy.materialbrowser.browser.SiteCertificateRules
import dev.sk2andy.materialbrowser.data.AppearanceSettings
import dev.sk2andy.materialbrowser.data.BrowserAppearanceMode
import dev.sk2andy.materialbrowser.ui.theme.MaterialBrowserTheme
import dev.sk2andy.materialbrowser.ui.theme.VolaPreviews
import dev.sk2andy.materialbrowser.ui.theme.VolaSiteInfo
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

internal object SiteCertificateTestTags {
    const val Page = "site_certificate_page"
}

/** «Let’s Encrypt · until 12 Jan 2027» under «Certificate» in Site info (board W-SiteInfo). */
@Composable
internal fun siteCertificateSummary(certificate: SiteCertificate): String {
    val issuer = certificate.issuerName ?: stringResource(R.string.site_certificate_unknown)
    val until = certificate.validUntilMillis ?: return issuer
    val expired = SiteCertificateRules.isExpired(certificate, System.currentTimeMillis())
    return stringResource(
        if (expired) R.string.site_info_certificate_expired else R.string.site_info_certificate_summary,
        issuer,
        certificateDate(until),
    )
}

/**
 * The certificate page of Site info: who it was issued to and by, when it is valid and its SHA-256
 * fingerprint, which can be selected and copied to compare.
 */
@Composable
internal fun SiteCertificateContent(certificate: SiteCertificate, modifier: Modifier = Modifier) {
    val unknown = stringResource(R.string.site_certificate_unknown)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag(SiteCertificateTestTags.Page),
        verticalArrangement = Arrangement.spacedBy(VolaSiteInfo.sectionGap),
    ) {
        SiteInfoCard {
            CertificateField(
                label = stringResource(R.string.site_certificate_subject),
                value = certificate.subjectName ?: certificate.host,
            )
            SiteInfoDivider()
            CertificateField(
                label = stringResource(R.string.site_certificate_issuer),
                value = certificate.issuerName ?: unknown,
            )
            SiteInfoDivider()
            CertificateField(
                label = stringResource(R.string.site_certificate_valid_from),
                value = certificate.validFromMillis?.let { certificateDate(it) } ?: unknown,
            )
            SiteInfoDivider()
            CertificateField(
                label = stringResource(R.string.site_certificate_valid_until),
                value = certificate.validUntilMillis?.let { certificateDate(it) } ?: unknown,
            )
        }
        SiteInfoCard {
            CertificateField(
                label = stringResource(R.string.site_certificate_fingerprint),
                value = certificate.sha256Fingerprint
                    ?.let { SiteCertificateRules.fingerprintLines(it) }
                    ?: unknown,
                monospace = true,
            )
        }
    }
}

@Composable
private fun CertificateField(label: String, value: String, monospace: Boolean = false) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {}
            .padding(
                horizontal = VolaSiteInfo.rowHorizontalPadding,
                vertical = VolaSiteInfo.rowVerticalPadding,
            ),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SelectionContainer {
            Text(
                text = value,
                style = if (monospace) {
                    MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace)
                } else {
                    MaterialTheme.typography.bodyLarge
                },
            )
        }
    }
}

@Composable
private fun certificateDate(millis: Long): String {
    val locale = LocalConfiguration.current.locales[0]
    return remember(millis, locale) {
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
            .withLocale(locale)
            .format(Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()))
    }
}

@VolaPreviews
@Composable
private fun SiteCertificateContentPreview() {
    MaterialBrowserTheme(
        settings = AppearanceSettings(appearanceMode = BrowserAppearanceMode.System),
    ) {
        Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
            SiteCertificateContent(
                certificate = SiteCertificate(
                    host = "north-guide.ru",
                    subjectName = "north-guide.ru",
                    issuerName = "Let's Encrypt",
                    validFromMillis = 1_760_000_000_000L,
                    validUntilMillis = 1_767_800_000_000L,
                    sha256Fingerprint = List(32) { index -> "%02X".format(index * 7 % 256) }
                        .joinToString(":"),
                ),
                modifier = Modifier.padding(VolaSiteInfo.sidePadding),
            )
        }
    }
}
