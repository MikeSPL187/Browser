package dev.sk2andy.materialbrowser

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import dev.sk2andy.materialbrowser.browser.AndroidBrowserEngineKind
import dev.sk2andy.materialbrowser.browser.credentials.vault.AndroidCredentialVault
import dev.sk2andy.materialbrowser.browser.credentials.vault.CredentialVaultSession
import dev.sk2andy.materialbrowser.browser.credentials.vault.KeystoreVaultKeyWrapper
import dev.sk2andy.materialbrowser.browser.credentials.vault.LocalCredentialVault
import dev.sk2andy.materialbrowser.browser.credentials.vault.RecoveryPhrase
import dev.sk2andy.materialbrowser.browser.credentials.vault.RecoveryPhraseKeyWrapper
import dev.sk2andy.materialbrowser.browser.credentials.vault.VaultOpenResult
import dev.sk2andy.materialbrowser.data.AppDataTransferLock
import dev.sk2andy.materialbrowser.data.BrowserSessionStore
import dev.sk2andy.materialbrowser.shared.credentials.VaultLoginDraft
import dev.sk2andy.materialbrowser.shared.credentials.VaultSaveResult
import dev.sk2andy.materialbrowser.ui.passwords.PasswordDetailScreen
import dev.sk2andy.materialbrowser.ui.passwords.PasswordEditError
import dev.sk2andy.materialbrowser.ui.passwords.PasswordEditScreen
import dev.sk2andy.materialbrowser.ui.passwords.PasswordsIntroScreen
import dev.sk2andy.materialbrowser.ui.passwords.PasswordsListScreen
import dev.sk2andy.materialbrowser.ui.passwords.PasswordsLockedScreen
import dev.sk2andy.materialbrowser.ui.passwords.PasswordsRules
import dev.sk2andy.materialbrowser.ui.passwords.RecoveryConfirmScreen
import dev.sk2andy.materialbrowser.ui.passwords.RecoveryEntryScreen
import dev.sk2andy.materialbrowser.ui.passwords.RecoveryPhraseScreen
import dev.sk2andy.materialbrowser.ui.passwords.SensitiveClipboard
import dev.sk2andy.materialbrowser.ui.theme.CandyTheme
import dev.sk2andy.materialbrowser.ui.theme.setCandyContent
import java.security.SecureRandom
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts
import dev.sk2andy.materialbrowser.browser.credentials.PasswordImportFile
import dev.sk2andy.materialbrowser.browser.credentials.PasswordImportFiles
import dev.sk2andy.materialbrowser.browser.integration.PasswordsActivityContract
import dev.sk2andy.materialbrowser.shared.credentials.PasswordImportParse
import dev.sk2andy.materialbrowser.shared.credentials.PasswordImportRules
import dev.sk2andy.materialbrowser.shared.credentials.VaultImportResult
import dev.sk2andy.materialbrowser.ui.passwords.PasswordImportFileState
import dev.sk2andy.materialbrowser.ui.passwords.PasswordImportGuideScreen
import dev.sk2andy.materialbrowser.ui.passwords.PasswordImportProblem
import dev.sk2andy.materialbrowser.ui.passwords.PasswordImportReport
import dev.sk2andy.materialbrowser.ui.passwords.PasswordImportResultScreen
import dev.sk2andy.materialbrowser.ui.passwords.PasswordImportScreen
import dev.sk2andy.materialbrowser.ui.passwords.PasswordImportSource
import dev.sk2andy.materialbrowser.browser.credentials.PwnedPasswordsClient
import dev.sk2andy.materialbrowser.shared.credentials.PasswordHealthRules
import dev.sk2andy.materialbrowser.shared.credentials.TotpRules
import dev.sk2andy.materialbrowser.shared.credentials.VaultLogin
import dev.sk2andy.materialbrowser.ui.passwords.LeakCheckStatus
import dev.sk2andy.materialbrowser.ui.passwords.PasswordHealthScreen

/** Where the Passwords screen is. A recovery phrase lives only here, in memory, while it is shown. */
internal sealed interface PasswordsRoute {
    data object Intro : PasswordsRoute

    data class Phrase(val phrase: String) : PasswordsRoute

    data class Confirm(val phrase: String, val positions: List<Int>, val wrong: Boolean = false) : PasswordsRoute

    data object Locked : PasswordsRoute

    data class Recover(val wrong: Boolean = false) : PasswordsRoute

    data object Logins : PasswordsRoute

    data class Detail(val id: String) : PasswordsRoute

    data class Edit(val id: String?, val error: PasswordEditError? = null) : PasswordsRoute

    /** The password check (board W-PasswordHealth). */
    data object Health : PasswordsRoute

    /** «Move to Vola» (board W-Import): where the passwords come from. */
    data object Import : PasswordsRoute

    /** How to get the export from [source], and why the last file brought nothing in. */
    data class ImportGuide(val source: PasswordImportSource, val problem: PasswordImportProblem? = null) : PasswordsRoute

    /** What the import did, and the picked file to delete. */
    data class ImportDone(
        val report: PasswordImportReport,
        val file: Uri,
        val fileState: PasswordImportFileState = PasswordImportFileState.Present,
    ) : PasswordsRoute

    /** Screens that show logins or write them: they need the vault open. */
    val needsOpenVault: Boolean
        get() = this is Logins || this is Detail || this is Edit || this is Health || this is Import || this is ImportGuide
}

/** Survives rotation, never the process: nothing here is written to a bundle or to disk. */
internal class PasswordsViewModel : ViewModel() {
    var route by mutableStateOf<PasswordsRoute?>(null)
    var busy by mutableStateOf(false)
    var message by mutableStateOf<Int?>(null)
    var revision by mutableIntStateOf(0)

    /** The device key did not open the vault; after the phrase opens it, the device gets a new key. */
    var deviceKeyLost = false

    /** Where to go once the vault opens, when that is not the list: an import that was under way. */
    var afterOpen: PasswordsRoute? = null

    /** Opened from Settings to move passwords in: back from the sources leaves the window. */
    var launchedForImport = false

    /** The source whose steps the file picker was opened from. */
    var importSource = PasswordImportSource.File

    /** The leak check's answers by login id, kept only while the screen lives. */
    var leakStatus by mutableStateOf(LeakCheckStatus.Off)
    var breaches by mutableStateOf<Map<String, Int>>(emptyMap())
}

/**
 * Vola's own passwords (boards W-Passwords, W-PasswordDetail, W-Locked). The window is secure: no
 * screenshots, no recents preview. The vault opens with strong biometrics or the screen lock and
 * locks itself five minutes after the last use.
 */
class PasswordsActivity : FragmentActivity() {
    private val model: PasswordsViewModel by viewModels()
    private val vault: LocalCredentialVault by lazy { AndroidCredentialVault.get(this) }
    private val random = SecureRandom()
    private val exportPicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(::importFrom)
    }

    /** System WebView fills sites through Android's autofill service; the screen says so. */
    private val systemFillOnly: Boolean by lazy {
        BrowserSessionStore(this).loadAndroidBrowserEngineKind() == AndroidBrowserEngineKind.SystemWebView
    }
    private var isFullImmersiveModeEnabled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (AppDataTransferLock.isActive(this)) {
            finish()
            return
        }
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        enableEdgeToEdge()
        val store = BrowserSessionStore(this)
        isFullImmersiveModeEnabled = store.loadFullImmersiveModeEnabled()
        applyFullImmersiveMode(isFullImmersiveModeEnabled)
        val appearanceSettings = store.loadAppearanceSettings()
        val workspaceAccent = store.loadActiveWorkspaceAccent()
        if (model.route == null) {
            if (intent.getBooleanExtra(PasswordsActivityContract.EXTRA_IMPORT, false)) {
                model.launchedForImport = true
                model.afterOpen = PasswordsRoute.Import
            }
            model.route = startRoute()
        }
        setCandyContent(animationsEnabled = appearanceSettings.animationsEnabled) {
            val appearanceDark = appearanceSettings.usesDarkColors(isSystemInDarkTheme())
            SideEffect { applyAppearanceSystemBars(appearanceDark) }
            CandyTheme(settings = appearanceSettings, workspaceAccent = workspaceAccent) {
                Content()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        CredentialVaultSession.lockIfExpired()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) applyFullImmersiveMode(isFullImmersiveModeEnabled)
    }

    private fun startRoute(): PasswordsRoute = when {
        !vault.exists -> PasswordsRoute.Intro
        vault.isUnlocked -> model.afterOpen?.also { model.afterOpen = null } ?: PasswordsRoute.Logins
        else -> PasswordsRoute.Locked
    }

    @Composable
    private fun Content() {
        val route = model.route ?: return
        val lockGeneration = CredentialVaultSession.lockGeneration
        LaunchedEffect(lockGeneration) {
            val current = model.route
            if (!vault.isUnlocked && current?.needsOpenVault == true) {
                // An import picks up where it was once the vault is open again.
                if (current is PasswordsRoute.Import || current is PasswordsRoute.ImportGuide) model.afterOpen = current
                model.route = PasswordsRoute.Locked
            }
        }
        val logins = remember(model.revision, lockGeneration, route) { vault.allLogins() }
        val message = model.message?.let { stringResource(it) }
        BackHandler { back(route) }
        when (route) {
            PasswordsRoute.Intro -> PasswordsIntroScreen(model.busy, message, ::startSetup, ::finish)
            is PasswordsRoute.Phrase -> RecoveryPhraseScreen(
                words = route.phrase.split(' '),
                onDone = { model.route = PasswordsRoute.Confirm(route.phrase, PasswordsRules.confirmationPositions(random)) },
                onBack = { back(route) },
            )
            is PasswordsRoute.Confirm -> RecoveryConfirmScreen(
                positions = route.positions,
                wrong = route.wrong,
                busy = model.busy,
                onCheck = { answers -> confirm(route, answers) },
                onBack = { back(route) },
            )
            PasswordsRoute.Locked -> PasswordsLockedScreen(
                busy = model.busy,
                message = message,
                onUnlock = ::unlock,
                onRecover = { model.message = null; model.route = PasswordsRoute.Recover() },
                onBack = ::finish,
            )
            is PasswordsRoute.Recover -> RecoveryEntryScreen(route.wrong, model.busy, ::recover) { back(route) }
            PasswordsRoute.Logins -> PasswordsListScreen(
                logins = logins,
                onOpen = { login -> use { model.route = PasswordsRoute.Detail(login.id) } },
                onAdd = { use { model.route = PasswordsRoute.Edit(null) } },
                onLock = ::lockNow,
                onBack = ::finish,
                systemFillNote = systemFillOnly,
                healthIssues = remember(logins, model.breaches) {
                    PasswordHealthRules.report(logins, model.breaches).needsAttention
                },
                onHealth = { use { model.route = PasswordsRoute.Health } },
                onImport = {
                    use {
                        model.launchedForImport = false
                        model.route = PasswordsRoute.Import
                    }
                },
            )
            PasswordsRoute.Import -> PasswordImportScreen(
                onSource = { source -> use { model.route = PasswordsRoute.ImportGuide(source) } },
                onPickFile = { pickExport(PasswordImportSource.File) },
                onBack = { back(route) },
            )
            is PasswordsRoute.ImportGuide -> PasswordImportGuideScreen(
                source = route.source,
                problem = route.problem,
                busy = model.busy,
                onPick = { pickExport(route.source) },
                onBack = { back(route) },
            )
            is PasswordsRoute.ImportDone -> PasswordImportResultScreen(
                report = route.report,
                fileState = route.fileState,
                healthIssues = remember(logins, model.breaches, route.report) {
                    val added = route.report.addedIds.toSet()
                    val report = PasswordHealthRules.report(logins, model.breaches)
                    (report.breached.map { it.login.id } + report.reused.map { it.login.id } + report.weak.map { it.login.id })
                        .distinct()
                        .count { it in added }
                },
                onDeleteFile = { deleteExport(route) },
                onCheck = { use { model.route = PasswordsRoute.Health } },
                onDone = { back(route) },
            )
            PasswordsRoute.Health -> {
                LaunchedEffect(Unit) { if (leakCheckOn && model.leakStatus == LeakCheckStatus.Off) checkLeaks(logins) }
                PasswordHealthScreen(
                    report = remember(logins, model.breaches) { PasswordHealthRules.report(logins, model.breaches) },
                    leakCheck = model.leakStatus,
                    onLeakCheckChange = { on -> setLeakCheck(on, logins) },
                    onOpen = { login -> use { model.route = PasswordsRoute.Detail(login.id) } },
                    onChange = { login -> use { openChangePassword(login.origin) } },
                    onBack = { back(route) },
                )
            }
            is PasswordsRoute.Detail -> {
                val login = logins.firstOrNull { it.id == route.id }
                if (login == null) {
                    LaunchedEffect(route) { model.route = PasswordsRoute.Logins }
                } else {
                    PasswordDetailScreen(
                        login = login,
                        onCopyUsername = { use { copy(login.username) } },
                        onCopyPassword = { use { copy(login.password) } },
                        onEdit = { use { model.route = PasswordsRoute.Edit(login.id) } },
                        onDelete = { delete(login.id) },
                        onBack = { back(route) },
                        onCopyCode = { code -> use { copy(code) } },
                        onSetTotp = { input -> setTotp(login.id, input) },
                        onRemoveTotp = { writeTotp(login.id, null) },
                    )
                }
            }
            is PasswordsRoute.Edit -> PasswordEditScreen(
                initial = route.id?.let { id -> logins.firstOrNull { it.id == id } },
                error = route.error,
                busy = model.busy,
                onSave = { site, username, password -> save(route, site, username, password) },
                onBack = { back(route) },
            )
        }
    }

    private fun back(route: PasswordsRoute) {
        if (model.busy) return
        model.message = null
        model.route = when (route) {
            is PasswordsRoute.Phrase, is PasswordsRoute.Confirm -> PasswordsRoute.Intro
            is PasswordsRoute.Recover -> PasswordsRoute.Locked
            is PasswordsRoute.Detail -> PasswordsRoute.Logins
            PasswordsRoute.Health -> PasswordsRoute.Logins
            PasswordsRoute.Import -> if (model.launchedForImport) {
                finish()
                return
            } else {
                PasswordsRoute.Logins
            }
            is PasswordsRoute.ImportGuide -> PasswordsRoute.Import
            is PasswordsRoute.ImportDone -> PasswordsRoute.Logins
            is PasswordsRoute.Edit -> route.id?.let(PasswordsRoute::Detail) ?: PasswordsRoute.Logins
            PasswordsRoute.Intro, PasswordsRoute.Locked, PasswordsRoute.Logins -> {
                finish()
                return
            }
        }
    }

    /** Whether the leak check is on; off until the user turns it on (Q21b). */
    private val leakCheckOn: Boolean
        get() = getSharedPreferences(PREFERENCES, MODE_PRIVATE).getBoolean(KEY_LEAK_CHECK, false)

    private fun setLeakCheck(on: Boolean, logins: List<VaultLogin>) {
        getSharedPreferences(PREFERENCES, MODE_PRIVATE).edit().putBoolean(KEY_LEAK_CHECK, on).apply()
        if (on) {
            checkLeaks(logins)
        } else {
            model.leakStatus = LeakCheckStatus.Off
            model.breaches = emptyMap()
        }
    }

    /** Asks Pwned Passwords about each distinct password, sending only a hash prefix for each. */
    private fun checkLeaks(logins: List<VaultLogin>) {
        if (model.leakStatus == LeakCheckStatus.Checking) return
        model.leakStatus = LeakCheckStatus.Checking
        lifecycleScope.launch {
            val answers = withContext(Dispatchers.IO) {
                val client = PwnedPasswordsClient()
                val byPassword = logins.map(VaultLogin::password).distinct().associateWith(client::timesSeen)
                logins.associate { login -> login.id to byPassword[login.password] }
            }
            if (model.leakStatus != LeakCheckStatus.Checking) return@launch
            model.breaches = answers.mapNotNull { (id, seen) -> seen?.let { id to it } }.toMap()
            model.leakStatus = if (answers.values.any { it == null }) LeakCheckStatus.Failed else LeakCheckStatus.Done
        }
    }

    /** The site's own change-password page, in Vola. */
    private fun openChangePassword(origin: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(PasswordHealthRules.changePasswordUrl(origin)))
            .setPackage(packageName)
        runCatching { startActivity(intent) }
    }

    /** Runs a screen action on the open vault and keeps it open for another five minutes. */
    private inline fun use(action: () -> Unit) {
        if (!vault.isUnlocked) {
            model.route = PasswordsRoute.Locked
            return
        }
        CredentialVaultSession.touch(vault)
        action()
    }

    private fun startSetup() {
        if (!canAuthenticate()) {
            model.message = R.string.passwords_no_screen_lock
            return
        }
        val wordlist = AndroidCredentialVault.wordlist(this) ?: run {
            model.message = R.string.passwords_failed
            return
        }
        model.message = null
        model.route = PasswordsRoute.Phrase(RecoveryPhrase.generate(wordlist, random))
    }

    private fun confirm(route: PasswordsRoute.Confirm, answers: List<String>) {
        if (!PasswordsRules.confirms(route.phrase, route.positions, answers)) {
            model.route = route.copy(wrong = true)
            return
        }
        authenticate {
            runBusy {
                val recovery = RecoveryPhraseKeyWrapper(route.phrase, random)
                // The device slot comes first: it must be wrapped inside the short window after the prompt.
                val result = withContext(Dispatchers.Default) {
                    vault.create(listOf(KeystoreVaultKeyWrapper(), recovery)).also { recovery.forget() }
                }
                when (result) {
                    VaultOpenResult.Opened -> opened()
                    VaultOpenResult.AlreadyExists -> model.route = PasswordsRoute.Locked
                    else -> {
                        model.message = R.string.passwords_failed
                        model.route = PasswordsRoute.Intro
                    }
                }
            }
        }
    }

    private fun unlock() {
        if (!canAuthenticate()) {
            model.message = R.string.passwords_no_screen_lock
            return
        }
        authenticate {
            runBusy {
                when (withContext(Dispatchers.IO) { vault.unlock(KeystoreVaultKeyWrapper()) }) {
                    VaultOpenResult.Opened -> opened()
                    VaultOpenResult.Refused, VaultOpenResult.NoSlot -> {
                        model.deviceKeyLost = true
                        model.message = R.string.passwords_device_key_lost
                    }
                    VaultOpenResult.Missing -> model.route = PasswordsRoute.Intro
                    else -> model.message = R.string.passwords_failed
                }
            }
        }
    }

    private fun recover(input: String) {
        val wordlist = AndroidCredentialVault.wordlist(this)
        val phrase = wordlist?.let { RecoveryPhrase.normalize(input, it) } ?: run {
            model.route = PasswordsRoute.Recover(wrong = true)
            return
        }
        runBusy {
            val recovery = RecoveryPhraseKeyWrapper(phrase, random)
            val result = withContext(Dispatchers.Default) { vault.unlock(recovery).also { recovery.forget() } }
            when (result) {
                VaultOpenResult.Opened -> {
                    opened()
                    if (model.deviceKeyLost) rebindDevice()
                }
                VaultOpenResult.Refused -> model.route = PasswordsRoute.Recover(wrong = true)
                else -> model.message = R.string.passwords_failed
            }
        }
    }

    /** After the phrase opened the vault, a fresh device key replaces the lost one. */
    private fun rebindDevice() {
        if (!canAuthenticate()) return
        authenticate {
            lifecycleScope.launch {
                val device = KeystoreVaultKeyWrapper()
                val result = withContext(Dispatchers.IO) {
                    device.deleteKey()
                    vault.replaceSlot(device)
                }
                if (result == VaultOpenResult.Opened) model.deviceKeyLost = false
            }
        }
    }

    private fun opened() {
        CredentialVaultSession.touch(vault)
        model.message = null
        model.revision++
        model.route = model.afterOpen ?: PasswordsRoute.Logins
        model.afterOpen = null
    }

    /** Opens the system file picker for an export; what it may show is kept to text and JSON. */
    private fun pickExport(source: PasswordImportSource) = use {
        if (model.busy) return@use
        model.importSource = source
        runCatching { exportPicker.launch(EXPORT_TYPES) }
    }

    private fun importFrom(uri: Uri) = use {
        val source = model.importSource
        runBusy {
            model.route = withContext(Dispatchers.IO) { importRoute(uri, source) }
            model.revision++
        }
    }

    /** Reads, parses and imports [uri]; the route says what came of it. */
    private fun importRoute(uri: Uri, source: PasswordImportSource): PasswordsRoute {
        fun problem(problem: PasswordImportProblem) = PasswordsRoute.ImportGuide(source, problem)
        val file = when (val read = PasswordImportFiles.read(contentResolver, uri)) {
            is PasswordImportFile.Loaded -> read
            PasswordImportFile.TooLarge -> return problem(PasswordImportProblem.TooLarge)
            PasswordImportFile.NotText -> return problem(PasswordImportProblem.NotText)
            PasswordImportFile.Unreadable -> return problem(PasswordImportProblem.Unreadable)
        }
        val parsed = when (val parse = PasswordImportRules.parse(file.text, PasswordsRules::manualOrigin)) {
            is PasswordImportParse.Read -> parse
            PasswordImportParse.Encrypted -> return problem(PasswordImportProblem.Encrypted)
            PasswordImportParse.NotAnExport -> return problem(PasswordImportProblem.NotAnExport)
        }
        if (parsed.logins.isEmpty()) return problem(PasswordImportProblem.NoLogins)
        val summary = when (val result = vault.importLogins(parsed.logins, System.currentTimeMillis())) {
            is VaultImportResult.Imported -> result.summary
            VaultImportResult.Locked -> return PasswordsRoute.Locked
            VaultImportResult.Failed -> return problem(PasswordImportProblem.Failed)
        }
        val report = PasswordImportReport(
            fileName = file.name,
            fileSizeBytes = file.sizeBytes,
            added = summary.added,
            duplicates = summary.duplicates,
            skipped = parsed.skipped + summary.rejected,
            withTotp = summary.withTotp,
            addedIds = summary.addedIds,
        )
        return PasswordsRoute.ImportDone(report, uri)
    }

    private fun deleteExport(route: PasswordsRoute.ImportDone) {
        if (route.fileState != PasswordImportFileState.Present) return
        model.route = route.copy(fileState = PasswordImportFileState.Deleting)
        lifecycleScope.launch {
            val deleted = withContext(Dispatchers.IO) { PasswordImportFiles.delete(contentResolver, route.file) }
            val current = model.route as? PasswordsRoute.ImportDone ?: return@launch
            model.route = current.copy(
                fileState = if (deleted) PasswordImportFileState.Deleted else PasswordImportFileState.DeleteFailed,
            )
        }
    }

    private fun lockNow() {
        CredentialVaultSession.lockNow()
        model.route = PasswordsRoute.Locked
    }

    private fun copy(text: String) {
        SensitiveClipboard.copy(this, text)
        // Android 13 and later show their own «Copied» confirmation.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            Toast.makeText(this, R.string.passwords_copied, Toast.LENGTH_SHORT).show()
        }
    }

    private fun delete(id: String) = use {
        runBusy {
            if (withContext(Dispatchers.IO) { vault.delete(id) }) {
                model.revision++
                model.route = PasswordsRoute.Logins
            } else {
                Toast.makeText(this@PasswordsActivity, R.string.passwords_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }

    /** Keeps the 2FA key in [input] for the login; false when it is not one, so the dialog stays. */
    private fun setTotp(id: String, input: String): Boolean {
        val config = TotpRules.parse(input) ?: return false
        writeTotp(id, TotpRules.canonical(config))
        return true
    }

    private fun writeTotp(id: String, totp: String?) = use {
        runBusy {
            val now = System.currentTimeMillis()
            if (!withContext(Dispatchers.IO) { vault.setTotp(id, totp, now) }) {
                Toast.makeText(this@PasswordsActivity, R.string.passwords_failed, Toast.LENGTH_SHORT).show()
            }
            model.revision++
        }
    }

    private fun save(route: PasswordsRoute.Edit, site: String, username: String, password: String) = use {
        val origin = PasswordsRules.manualOrigin(site)
        when {
            origin == null -> model.route = route.copy(error = PasswordEditError.SiteInvalid)
            password.isEmpty() -> model.route = route.copy(error = PasswordEditError.PasswordRequired)
            else -> {
                val existing = route.id?.let { id -> vault.allLogins().firstOrNull { it.id == id } }
                val draft = VaultLoginDraft(origin, null, existing?.httpRealm, username.trim(), password)
                // Adding by hand never overwrites a saved login: the user edits that one instead.
                val clash = route.id == null && vault.loginsFor(origin).any { it.username == draft.username && it.httpRealm == null }
                if (clash) {
                    model.route = route.copy(error = PasswordEditError.Duplicate)
                    return@use
                }
                runBusy {
                    val now = System.currentTimeMillis()
                    val result = withContext(Dispatchers.IO) {
                        if (route.id == null) vault.save(draft, now) else vault.update(route.id, draft, now)
                    }
                    model.route = when (result) {
                        is VaultSaveResult.Added -> PasswordsRoute.Detail(result.login.id)
                        is VaultSaveResult.Updated -> PasswordsRoute.Detail(result.login.id)
                        is VaultSaveResult.Unchanged -> PasswordsRoute.Detail(result.login.id)
                        VaultSaveResult.Locked -> PasswordsRoute.Locked
                        VaultSaveResult.Rejected -> route.copy(
                            error = if (route.id != null) PasswordEditError.Duplicate else PasswordEditError.Failed,
                        )
                        VaultSaveResult.Failed -> route.copy(error = PasswordEditError.Failed)
                    }
                    model.revision++
                }
            }
        }
    }

    private fun runBusy(block: suspend () -> Unit) {
        if (model.busy) return
        model.busy = true
        lifecycleScope.launch {
            try {
                block()
            } finally {
                model.busy = false
            }
        }
    }

    private fun canAuthenticate(): Boolean =
        BiometricManager.from(this).canAuthenticate(AUTHENTICATORS) == BiometricManager.BIOMETRIC_SUCCESS

    private fun authenticate(onSuccess: () -> Unit) {
        val prompt = BiometricPrompt(
            this,
            ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = onSuccess()
            },
        )
        prompt.authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle(getString(R.string.passwords_prompt_title))
                .setSubtitle(getString(R.string.passwords_prompt_subtitle))
                .setAllowedAuthenticators(AUTHENTICATORS)
                .build(),
        )
    }

    private companion object {
        const val PREFERENCES = "vola_passwords"
        const val KEY_LEAK_CHECK = "leak_check"

        /** CSV comes as text of one kind or another, Bitwarden's export as JSON; some providers know neither. */
        val EXPORT_TYPES = arrayOf("text/*", "application/json", "application/octet-stream", "application/vnd.ms-excel")

        /** The same pair the device key accepts: strong biometrics or the screen lock. */
        const val AUTHENTICATORS =
            BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
    }
}
