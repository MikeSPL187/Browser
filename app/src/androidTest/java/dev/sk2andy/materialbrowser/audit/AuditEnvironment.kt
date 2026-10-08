package dev.sk2andy.materialbrowser.audit

import android.app.LocaleManager
import android.content.Context
import android.os.Build
import android.os.LocaleList
import android.os.ParcelFileDescriptor
import androidx.test.platform.app.InstrumentationRegistry
import dev.sk2andy.materialbrowser.browser.StartupAddressFocusMode
import dev.sk2andy.materialbrowser.data.AppearanceSettings
import dev.sk2andy.materialbrowser.data.BrowserAppearanceMode
import dev.sk2andy.materialbrowser.data.BrowserSessionStore
import dev.sk2andy.materialbrowser.data.GestureOnboardingStore

/** The settings a real phone may have that change how every screen is laid out. */
data class AuditConfig(
    val languageTag: String,
    val dark: Boolean,
    val fontScale: Float,
) {
    val name: String =
        "$languageTag, ${if (dark) "dark" else "light"}, ${(fontScale * 100).toInt()}% font"

    companion object {
        val EnglishLight = AuditConfig("en-US", dark = false, fontScale = 1f)
        val RussianDark = AuditConfig("ru-RU", dark = true, fontScale = 1f)
        val EnglishDarkLargeFont = AuditConfig("en-US", dark = true, fontScale = 2f)
        val RussianLightLargeFont = AuditConfig("ru-RU", dark = false, fontScale = 2f)
    }
}

/** Puts the device and the app into an [AuditConfig] before launch, and back afterwards. */
internal object AuditEnvironment {
    /** [firstRun]: a fresh install that still shows the welcome and setup screens. */
    fun enter(context: Context, config: AuditConfig, firstRun: Boolean = false) {
        context.getSharedPreferences("browser_session", Context.MODE_PRIVATE).edit().clear().commit()
        if (!firstRun) GestureOnboardingStore(context).markCompleted()
        BrowserSessionStore(context).apply {
            saveAppearanceSettings(
                AppearanceSettings(
                    appearanceMode = if (config.dark) BrowserAppearanceMode.Dark else BrowserAppearanceMode.Light,
                ),
            )
            // The walk opens the address editor itself, as a tap would.
            saveStartupAddressFocusMode(StartupAddressFocusMode.Never)
        }
        setFontScale(config.fontScale)
        setAppLanguage(context, config.languageTag)
    }

    fun reset(context: Context) {
        setFontScale(1f)
        setAppLanguage(context, null)
        context.getSharedPreferences("browser_session", Context.MODE_PRIVATE).edit().clear().commit()
    }

    private fun setFontScale(scale: Float) {
        shell("settings put system font_scale $scale")
    }

    private fun setAppLanguage(context: Context, languageTag: String?) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val locales = languageTag?.let(LocaleList::forLanguageTags) ?: LocaleList.getEmptyLocaleList()
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            context.getSystemService(LocaleManager::class.java).applicationLocales = locales
        }
    }

    private fun shell(command: String) {
        val descriptor = InstrumentationRegistry.getInstrumentation().uiAutomation
            .executeShellCommand(command)
        ParcelFileDescriptor.AutoCloseInputStream(descriptor).use { it.readBytes() }
    }
}
