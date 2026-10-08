package dev.sk2andy.materialbrowser.ui.passwords

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import dev.sk2andy.materialbrowser.audit.AuditConfig
import dev.sk2andy.materialbrowser.audit.AuditEnvironment
import dev.sk2andy.materialbrowser.audit.AuditFinding
import dev.sk2andy.materialbrowser.audit.ScreenAuditor
import dev.sk2andy.materialbrowser.data.AppearanceSettings
import dev.sk2andy.materialbrowser.data.BrowserAppearanceMode
import dev.sk2andy.materialbrowser.shared.credentials.BreachedLogin
import dev.sk2andy.materialbrowser.shared.credentials.PasswordHealthReport
import dev.sk2andy.materialbrowser.shared.credentials.ReusedLogin
import dev.sk2andy.materialbrowser.shared.credentials.VaultLogin
import dev.sk2andy.materialbrowser.shared.credentials.WeakLogin
import dev.sk2andy.materialbrowser.shared.credentials.WeakPasswordKind
import dev.sk2andy.materialbrowser.ui.theme.MaterialBrowserTheme
import org.junit.After
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Every Passwords screen, checked by [ScreenAuditor] in each [AuditConfig] like the screen walk
 * checks the browser. The walk cannot get there: the vault opens only with the screen lock, which
 * the CI emulator does not have, and the window is secure. So each screen is shown on its own,
 * edge to edge in the app's theme as the Passwords window shows it, with a full set of logins.
 */
@RunWith(Parameterized::class)
class PasswordsScreenAuditInstrumentedTest(private val config: AuditConfig) {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var scenario: ActivityScenario<ComponentActivity>
    private val auditor = ScreenAuditor(composeRule) {
        lateinit var current: ComponentActivity
        scenario.onActivity { activity -> current = activity }
        current
    }

    @After
    fun tearDown() {
        if (::scenario.isInitialized) runCatching { scenario.close() }
        AuditEnvironment.reset(context)
    }

    @Test
    fun everyPasswordsScreen() {
        AuditEnvironment.enter(context, config)
        scenario = ActivityScenario.launch(ComponentActivity::class.java)
        val findings = screens().flatMap { (name, content) ->
            show(content)
            auditor.auditScrolling("passwords: $name", config.name)
        }
        if (findings.isNotEmpty()) fail(report(findings))
    }

    private fun show(content: @Composable () -> Unit) {
        val settings = AppearanceSettings(
            appearanceMode = if (config.dark) BrowserAppearanceMode.Dark else BrowserAppearanceMode.Light,
        )
        scenario.onActivity { activity ->
            activity.enableEdgeToEdge()
            activity.setContent { MaterialBrowserTheme(settings = settings) { content() } }
        }
        composeRule.waitForIdle()
    }

    private fun screens(): List<Pair<String, @Composable () -> Unit>> = listOf(
        "intro" to { PasswordsIntroScreen(busy = false, message = null, onStart = {}, onBack = {}) },
        "recovery phrase" to { RecoveryPhraseScreen(words = PHRASE, onDone = {}, onBack = {}) },
        "recovery check" to {
            RecoveryConfirmScreen(positions = listOf(1, 6, 10), wrong = true, busy = false, onCheck = {}, onBack = {})
        },
        "locked" to { PasswordsLockedScreen(busy = false, message = null, onUnlock = {}, onRecover = {}, onBack = {}) },
        "recover with the phrase" to { RecoveryEntryScreen(wrong = true, busy = false, onOpen = {}, onBack = {}) },
        "list" to {
            PasswordsListScreen(
                logins = logins,
                onOpen = {},
                onAdd = {},
                onLock = {},
                onBack = {},
                healthIssues = 3,
                onImport = {},
                onTurnOff = {},
            )
        },
        "list, empty" to {
            PasswordsListScreen(logins = emptyList(), onOpen = {}, onAdd = {}, onLock = {}, onBack = {}, systemFillNote = true)
        },
        "login" to {
            PasswordDetailScreen(
                login = logins[1].copy(totp = TOTP),
                onCopyUsername = {},
                onCopyPassword = {},
                onEdit = {},
                onDelete = {},
                onBack = {},
            )
        },
        "new login" to {
            PasswordEditScreen(initial = null, error = PasswordEditError.SiteInvalid, busy = false, onSave = { _, _, _ -> }, onBack = {})
        },
        "edit login" to {
            PasswordEditScreen(initial = logins.first(), error = null, busy = false, onSave = { _, _, _ -> }, onBack = {})
        },
        "password check" to {
            PasswordHealthScreen(
                report = PasswordHealthReport(
                    total = logins.size,
                    breached = listOf(BreachedLogin(logins[0], timesSeen = 12_345)),
                    reused = listOf(ReusedLogin(logins[1], otherSites = 2)),
                    weak = listOf(WeakLogin(logins[3], length = 6, kind = WeakPasswordKind.DigitsOnly)),
                ),
                leakCheck = LeakCheckStatus.Done,
                onLeakCheckChange = {},
                onOpen = {},
                onChange = {},
                onBack = {},
            )
        },
        "import" to { PasswordImportScreen(onSource = {}, onPickFile = {}, onBack = {}) },
        "import from Chrome" to {
            PasswordImportGuideScreen(
                source = PasswordImportSource.Chrome,
                problem = PasswordImportProblem.NotAnExport,
                busy = false,
                onPick = {},
                onBack = {},
            )
        },
        "import done" to {
            PasswordImportResultScreen(
                report = PasswordImportReport(
                    fileName = "Chrome Passwords.csv",
                    fileSizeBytes = 48_213,
                    added = 214,
                    duplicates = 12,
                    skipped = 3,
                    withTotp = 4,
                    addedIds = emptyList(),
                    bookmarks = 57,
                ),
                fileState = PasswordImportFileState.Present,
                healthIssues = 9,
                onDeleteFile = {},
                onCheck = {},
                onDone = {},
            )
        },
    )

    private fun report(findings: List<AuditFinding>): String {
        val lines = findings
            .groupBy { finding -> finding.kind to finding.detail }
            .entries
            .sortedWith(compareBy({ it.key.first.ordinal }, { it.key.second }))
            .map { (key, seen) ->
                "[${key.first}] ${key.second} — on ${seen.map(AuditFinding::screen).distinct().joinToString()}"
            }
        return "${lines.size} distinct findings in ${config.name}\n" + lines.joinToString("\n")
    }

    private companion object {
        val PHRASE = "acid acorn acre acts afar affix aged agent agile aging agony ahead".split(' ')
        const val TOTP = "otpauth://totp/?secret=JBSWY3DPEHPK3PXP&algorithm=SHA1&digits=6&period=30"

        val logins = listOf(
            login("1", "https://mail.example.com", "anna@example.com"),
            login("2", "https://bank.example.ru", "anna.k"),
            login("3", "https://tasks.example.com", "anna.karenina.long.address@work.example.com"),
            login("4", "https://forum.example.org", "snowfox"),
            login("5", "https://a-very-long-subdomain.cloud-storage.example.net", ""),
        )

        fun login(id: String, origin: String, username: String) = VaultLogin(
            id = id,
            origin = origin,
            formActionOrigin = null,
            httpRealm = null,
            username = username,
            password = "correct horse battery staple",
            createdAtMillis = 0,
            updatedAtMillis = 0,
            lastUsedAtMillis = null,
            timesUsed = 0,
        )

        @JvmStatic
        @Parameterized.Parameters(name = "{index}")
        fun parameters(): List<Array<Any>> = AuditConfig.All.map { config -> arrayOf<Any>(config) }
    }
}
