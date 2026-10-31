package dev.sk2andy.materialbrowser.audit

import android.content.res.Configuration
import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.sk2andy.materialbrowser.data.AppearanceSettings
import dev.sk2andy.materialbrowser.data.BrowserAppearanceMode
import dev.sk2andy.materialbrowser.shared.credentials.VaultLogin
import dev.sk2andy.materialbrowser.ui.passwords.LeakCheckStatus
import dev.sk2andy.materialbrowser.ui.passwords.PasswordDetailV5
import dev.sk2andy.materialbrowser.ui.passwords.PasswordEditV5
import dev.sk2andy.materialbrowser.ui.passwords.PasswordHealthV5
import dev.sk2andy.materialbrowser.ui.passwords.PasswordImportV5
import dev.sk2andy.materialbrowser.ui.passwords.PasswordsHealthCounts
import dev.sk2andy.materialbrowser.ui.passwords.PasswordsIntroV5
import dev.sk2andy.materialbrowser.ui.passwords.PasswordsListV5
import dev.sk2andy.materialbrowser.ui.passwords.PasswordsLockedV5
import dev.sk2andy.materialbrowser.ui.passwords.RecoveryPhraseV5
import dev.sk2andy.materialbrowser.ui.theme.MaterialBrowserTheme
import dev.sk2andy.materialbrowser.ui.v5.ProvideV5SettingsSkin
import java.util.Locale
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Pictures of screens the screen walk cannot reach on the emulator, drawn on sample data for the
 * Screenshots workflow ([ScreenShots]). The passwords need a screen lock and a fingerprint to
 * open, which the emulator has neither of, so the walk sees only their intro.
 *
 * Runs only with `walkScreenshots=true`; elsewhere it is skipped. Each screen is taken in Russian
 * dark (the owner's phone), Russian light and English light.
 */
@RunWith(AndroidJUnit4::class)
class V5ComponentShotsInstrumentedTest {
    @get:Rule
    val composeRule = createComposeRule()

    private class Look(val name: String, val language: String, val dark: Boolean)

    private val looks = listOf(Look("ru-dark", "ru", true), Look("ru-light", "ru", false), Look("en-light", "en", false))

    @Test
    fun passwords() {
        assumeTrue("Screenshots are off for this run", ScreenShots.enabled)
        val screens: List<Pair<String, @Composable () -> Unit>> = listOf(
            "01 locked" to { PasswordsLockedV5(false, null, {}, {}, {}, {}) },
            "02 locked opening" to { PasswordsLockedV5(true, null, {}, {}, {}, {}, unlocking = true) },
            "03 list" to {
                PasswordsListV5(
                    logins, {}, {}, {}, {}, false, null,
                    PasswordsHealthCounts(weak = 2, reused = 2, breached = null, issues = 3), {}, {}, false, {}, {},
                )
            },
            "04 list empty" to {
                PasswordsListV5(emptyList(), {}, {}, {}, {}, false, null, PasswordsHealthCounts(0, 0, null, 0), {}, {}, false, {}, {})
            },
            "05 login with note and 2fa" to { PasswordDetailV5(logins.first(), {}, {}, {}, {}, {}, {}, { true }, {}, {}) },
            "06 login plain" to { PasswordDetailV5(logins[1], {}, {}, {}, {}, {}, {}, { true }, {}, {}) },
            "07 edit" to { PasswordEditV5(logins.first(), null, false, { _, _, _, _ -> }, {}) },
            "08 add" to { PasswordEditV5(null, null, false, { _, _, _, _ -> }, {}) },
            "09 check" to { PasswordHealthV5(logins, emptyMap(), logins.size, LeakCheckStatus.Off, 0f, null, {}, {}, {}, {}) },
            "10 import" to { PasswordImportV5({}, {}, {}) },
            "11 intro" to { PasswordsIntroV5(false, null, {}, {}) },
            "12 recovery phrase" to {
                RecoveryPhraseV5("acid acorn acre acts afar affix aged agent agile aging agony ahead".split(' '), {}, {})
            },
        )
        var look by mutableStateOf(looks.first())
        var screen by mutableStateOf(screens.first())
        composeRule.setContent {
            val base = LocalConfiguration.current
            val configuration = Configuration(base).apply { setLocale(Locale.forLanguageTag(look.language)) }
            val context = LocalContext.current.createConfigurationContext(configuration)
            CompositionLocalProvider(LocalContext provides context, LocalConfiguration provides configuration) {
                val mode = if (look.dark) BrowserAppearanceMode.Dark else BrowserAppearanceMode.Light
                MaterialBrowserTheme(settings = AppearanceSettings(appearanceMode = mode)) {
                    // The passwords take the v5 palette, type and shapes as PasswordsActivity gives them.
                    ProvideV5SettingsSkin { screen.second() }
                }
            }
        }
        for (currentLook in looks) {
            for (currentScreen in screens) {
                look = currentLook
                screen = currentScreen
                composeRule.waitForIdle()
                // Idle means composed, not yet on the display: screencap took the previous screen.
                SystemClock.sleep(FRAME_SETTLE_MILLIS)
                composeRule.waitForIdle()
                ScreenShots.capture("passwords-${currentLook.name}", currentScreen.first)
            }
        }
    }

    private fun login(id: String, site: String, user: String, password: String, totp: String? = null, note: String? = null) =
        VaultLogin(
            id = id,
            origin = "https://$site",
            formActionOrigin = null,
            httpRealm = null,
            username = user,
            password = password,
            createdAtMillis = EDITED,
            updatedAtMillis = EDITED,
            lastUsedAtMillis = null,
            timesUsed = 2,
            totp = totp,
            note = note,
        )

    private val logins = listOf(
        login(
            "1", "accounts.example.com", "anna@example.com", "Kq9!vR2#mPx7",
            totp = "otpauth://totp/accounts?secret=JBSWY3DPEHPK3PXP",
            note = "Контрольный вопрос: первая школа — лицей 2",
        ),
        login("2", "bank.example", "anna", "123456"),
        login("3", "mail.example.org", "anna.k", "123456"),
        login("4", "shop.example", "anna", "Wt4#nL8!qZ"),
        login("5", "tickets.example", "", "summer2024"),
    )

    private companion object {
        /** 12 October 2026, for the «Edited» line. */
        const val EDITED = 1_791_800_000_000L

        /** Long enough for the composed frame to reach the display on the CI emulator. */
        const val FRAME_SETTLE_MILLIS = 600L
    }
}
