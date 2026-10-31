package dev.sk2andy.materialbrowser

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
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
import dev.sk2andy.materialbrowser.ui.passwords.PasswordEditError
import dev.sk2andy.materialbrowser.ui.passwords.PasswordFindingV5
import dev.sk2andy.materialbrowser.ui.passwords.PasswordHealthV5
import dev.sk2andy.materialbrowser.ui.passwords.PasswordsRules
import dev.sk2andy.materialbrowser.ui.passwords.SensitiveClipboard
import dev.sk2andy.materialbrowser.ui.theme.CandyTheme
import dev.sk2andy.materialbrowser.ui.theme.setCandyContent
import java.security.SecureRandom
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts
import dev.sk2andy.materialbrowser.browser.credentials.PasskeySupport
import dev.sk2andy.materialbrowser.browser.credentials.PasswordImportFile
import dev.sk2andy.materialbrowser.browser.credentials.PasswordImportFiles
import dev.sk2andy.materialbrowser.browser.integration.PasswordsActivityContract
import dev.sk2andy.materialbrowser.data.FavoriteBookmarkImportRules
import dev.sk2andy.materialbrowser.data.FavoriteBookmarkParseResult
import dev.sk2andy.materialbrowser.data.FavoriteEntry
import dev.sk2andy.materialbrowser.data.FavoriteLibrarySignal
import dev.sk2andy.materialbrowser.shared.credentials.ImportedLogin
import dev.sk2andy.materialbrowser.shared.credentials.PasswordImportParse
import dev.sk2andy.materialbrowser.shared.credentials.PasswordImportRules
import dev.sk2andy.materialbrowser.shared.credentials.VaultImportResult
import dev.sk2andy.materialbrowser.ui.passwords.PasswordImportConflictState
import dev.sk2andy.materialbrowser.ui.passwords.PasswordImportFileState
import dev.sk2andy.materialbrowser.ui.passwords.PasswordImportProblem
import dev.sk2andy.materialbrowser.ui.passwords.PasswordImportReport
import dev.sk2andy.materialbrowser.ui.passwords.PasswordImportSource
import dev.sk2andy.materialbrowser.browser.credentials.PasswordCheck
import dev.sk2andy.materialbrowser.browser.credentials.ChangePasswordPage
import dev.sk2andy.materialbrowser.browser.credentials.LeakAnswers
import dev.sk2andy.materialbrowser.browser.credentials.PwnedPasswordCheck
import dev.sk2andy.materialbrowser.shared.credentials.CredentialVaultRules
import dev.sk2andy.materialbrowser.shared.credentials.PasswordExportRules
import dev.sk2andy.materialbrowser.shared.credentials.PasswordHealthRules
import dev.sk2andy.materialbrowser.shared.credentials.TotpRules
import dev.sk2andy.materialbrowser.shared.credentials.VaultLogin
import dev.sk2andy.materialbrowser.ui.passwords.LeakCheckStatus
import dev.sk2andy.materialbrowser.ui.passwords.PasswordsLockedV5
import dev.sk2andy.materialbrowser.ui.passwords.PasswordsHealthCounts
import dev.sk2andy.materialbrowser.ui.passwords.PasswordsListV5
import dev.sk2andy.materialbrowser.ui.passwords.PasswordDetailV5
import dev.sk2andy.materialbrowser.ui.passwords.PasswordEditV5
import dev.sk2andy.materialbrowser.ui.passwords.PasswordsIntroV5
import dev.sk2andy.materialbrowser.ui.passwords.PasswordImportGuideV5
import dev.sk2andy.materialbrowser.ui.passwords.PasswordImportResultV5
import dev.sk2andy.materialbrowser.ui.passwords.PasswordImportV5
import dev.sk2andy.materialbrowser.ui.passwords.RecoveryConfirmV5
import dev.sk2andy.materialbrowser.ui.passwords.RecoveryEntryV5
import dev.sk2andy.materialbrowser.ui.passwords.RecoveryPhraseV5
import dev.sk2andy.materialbrowser.ui.passwords.PasswordsTransition
import dev.sk2andy.materialbrowser.ui.passwords.passwordsTransform
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Haptic
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Motion
import dev.sk2andy.materialbrowser.ui.theme.v5.performV5Haptic
import dev.sk2andy.materialbrowser.ui.v5.ProvideV5SettingsSkin
import dev.sk2andy.materialbrowser.ui.v5.V5PasswordFindingKind

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

    /** Under v5: one finding of the check (canvas «у2»). */
    data class HealthFinding(val id: String, val kind: V5PasswordFindingKind) : PasswordsRoute

    /** «Move to Vola» (board W-Import): where the passwords come from. */
    data object Import : PasswordsRoute

    /** How to get the export from [source], and why the last file brought nothing in. */
    data class ImportGuide(val source: PasswordImportSource, val problem: PasswordImportProblem? = null) : PasswordsRoute

    /** What the import did, and the picked password files to delete. */
    data class ImportDone(
        val report: PasswordImportReport,
        val files: List<Uri>,
        val fileState: PasswordImportFileState = PasswordImportFileState.Present,
        val conflictState: PasswordImportConflictState = PasswordImportConflictState.Offered,
    ) : PasswordsRoute

    /** Screens that show logins: they need the vault open. Moving bookmarks in does not. */
    val needsOpenVault: Boolean get() = this is Logins || this is Detail || this is Edit || this is Health ||
        this is HealthFinding
}

/** Files picked for «Move to Vola» that hold passwords, waiting for the vault to be set up or opened. */
internal data class PendingImport(val source: PasswordImportSource, val files: List<Uri>)

/** Survives rotation, never the process: nothing here is written to a bundle or to disk. */
internal class PasswordsViewModel : ViewModel() {
    var route by mutableStateOf<PasswordsRoute?>(null)
    var busy by mutableStateOf(false)

    /** The prompt said yes and the device key is opening the vault (StrongBox can take a second). */
    var unlocking by mutableStateOf(false)
    var message by mutableStateOf<Int?>(null)
    var revision by mutableIntStateOf(0)

    /** The device key did not open the vault; after the phrase opens it, the device gets a new key. */
    var deviceKeyLost = false

    /** Picked files with passwords, kept while the vault is set up or opened; then imported. */
    var pendingImport: PendingImport? = null
    var resumeImport by mutableStateOf<PendingImport?>(null)

    /** Opened from Settings to move passwords in: back from the sources leaves the window. */
    var launchedForImport = false

    /** The source whose steps the file picker was opened from. */
    var importSource = PasswordImportSource.File

    /** The leak check's answers by login id, kept only while the screen lives. */
    var leakStatus by mutableStateOf(LeakCheckStatus.Off)

    /** Share of hash prefixes the running leak check has answered, 0 to 1. */
    var leakProgress by mutableFloatStateOf(0f)

    /** Times seen in leaks by password hash, 0 for clean ([LeakAnswers]); never written to disk. */
    var leakAnswers by mutableStateOf<Map<String, Int>>(emptyMap())

    /** When the last leak check finished, for «Checked at» under v5. */
    var leakCheckedAt by mutableStateOf<Long?>(null)

    /**
     * The running leak check, in this model's scope so a rotation neither drops it nor leaves the
     * page saying «Checking…» for good; «Stop» cancels it and puts back [statusBeforeCheck].
     */
    var leakJob: Job? = null
    var statusBeforeCheck = LeakCheckStatus.Off
}

/**
 * Vola's own passwords (boards W-Passwords, W-PasswordDetail, W-Locked). The window is secure: no
 * screenshots, no recents preview. The vault opens with strong biometrics or the screen lock and
 * locks itself five minutes after the last use.
 */
class PasswordsActivity : FragmentActivity() {
    private val model: PasswordsViewModel by viewModels()
    private val vault: LocalCredentialVault by lazy { AndroidCredentialVault.get(this) }
    private val passwordCheck: PasswordCheck = PwnedPasswordCheck()
    private val random = SecureRandom()
    private val exportPicker = registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        importFrom(uris)
    }

    /** Where «Export passwords» writes its CSV; the system asks, Vola never picks a folder itself. */
    private val exportTarget = registerForActivityResult(
        ActivityResultContracts.CreateDocument(PasswordExportRules.MIME_TYPE),
    ) { uri -> uri?.let(::exportTo) }

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
            model.launchedForImport = intent.getBooleanExtra(PasswordsActivityContract.EXTRA_IMPORT, false)
            model.route = if (model.launchedForImport) PasswordsRoute.Import else startRoute()
        }
        setCandyContent(animationsEnabled = appearanceSettings.animationsEnabled) {
            val appearanceDark = appearanceSettings.usesDarkColors(isSystemInDarkTheme())
            SideEffect { applyAppearanceSystemBars(appearanceDark) }
            CandyTheme(settings = appearanceSettings, workspaceAccent = workspaceAccent) {
                // V28 B3: the screens take the v5 palette, type and shapes through Material.
                ProvideV5SettingsSkin() {
                    Content()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        CredentialVaultSession.lockIfExpired()
        SensitiveClipboard.clearIfExpired(this)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) applyFullImmersiveMode(isFullImmersiveModeEnabled)
    }

    private fun startRoute(): PasswordsRoute = when {
        !vault.exists -> PasswordsRoute.Intro
        vault.isUnlocked -> PasswordsRoute.Logins
        else -> PasswordsRoute.Locked
    }

    @Composable
    private fun Content() {
        val route = model.route ?: return
        val lockGeneration = CredentialVaultSession.lockGeneration
        LaunchedEffect(lockGeneration) {
            if (!vault.isUnlocked && model.route?.needsOpenVault == true) model.route = PasswordsRoute.Locked
        }
        // Files that waited for the vault come in as soon as it is open and nothing else runs.
        LaunchedEffect(model.resumeImport, model.busy) {
            val pending = model.resumeImport ?: return@LaunchedEffect
            if (model.busy) return@LaunchedEffect
            model.resumeImport = null
            model.importSource = pending.source
            importFrom(pending.files)
        }
        val logins = remember(model.revision, lockGeneration, route) { vault.allLogins() }
        val breaches = remember(logins, model.leakAnswers) { LeakAnswers.breaches(logins, model.leakAnswers) }
        val message = model.message?.let { stringResource(it) }
        // Canvas «ц4»–«ц7»: the vault's pages are v5.
        BackHandler { back(route) }
        val animated = V5Motion.animationsEnabled
        // One page at a time; a new page of the same kind (an error on the form) does not animate.
        AnimatedContent(
            targetState = route,
            transitionSpec = { passwordsTransform(transitionBetween(initialState, targetState), animated) },
            contentKey = { it::class },
            label = "passwords page",
        ) { route ->
            when (route) {
                PasswordsRoute.Intro -> {
                    PasswordsIntroV5(model.busy, message, ::startSetup, ::finish)
                }
                is PasswordsRoute.Phrase -> {
                    val onDone = { model.route = PasswordsRoute.Confirm(route.phrase, PasswordsRules.confirmationPositions(random)) }
                    RecoveryPhraseV5(route.phrase.split(' '), onDone) { back(route) }
                }
                is PasswordsRoute.Confirm -> {
                    RecoveryConfirmV5(route.positions, route.wrong, model.busy, { answers -> confirm(route, answers) }) { back(route) }
                }
                PasswordsRoute.Locked -> {
                    PasswordsLockedV5(
                        busy = model.busy,
                        message = message,
                        onUnlock = ::unlock,
                        onRecover = { model.message = null; model.route = PasswordsRoute.Recover() },
                        onBack = ::finish,
                        onErase = ::eraseVault,
                        unlocking = model.unlocking,
                    )
                }
                is PasswordsRoute.Recover -> {
                    RecoveryEntryV5(route.wrong, model.busy, ::recover) { back(route) }
                }
                PasswordsRoute.Logins -> {
                    PasswordsListV5(
                        logins = logins,
                        onOpen = { login -> use { model.route = PasswordsRoute.Detail(login.id) } },
                        onAdd = { use { model.route = PasswordsRoute.Edit(null) } },
                        onLock = ::lockNow,
                        onBack = ::finish,
                        systemFillNote = systemFillOnly,
                        passkeyNote = if (systemFillOnly) null else remember { PasskeySupport.current(this@PasswordsActivity) },
                        health = remember(logins, breaches, model.leakCheckedAt) {
                            val report = PasswordHealthRules.report(logins, breaches)
                            PasswordsHealthCounts(
                                weak = report.weak.size,
                                reused = report.reused.size,
                                // Unknown until the leak check ran: a dash, not a reassuring zero.
                                breached = report.breached.size.takeIf { model.leakCheckedAt != null },
                                issues = report.attentionWithInsecure,
                            )
                        },
                        onHealth = { use { model.route = PasswordsRoute.Health } },
                        onImport = {
                            use {
                                model.launchedForImport = false
                                model.route = PasswordsRoute.Import
                            }
                        },
                        busy = model.busy,
                        onTurnOff = ::eraseVault,
                        onExport = ::startExport,
                    )
                }
                PasswordsRoute.Import -> {
                    val onSource = { source: PasswordImportSource -> model.route = PasswordsRoute.ImportGuide(source) }
                    val onPickFile = { pickExport(PasswordImportSource.File) }
                    PasswordImportV5(onSource, onPickFile) { back(route) }
                }
                is PasswordsRoute.ImportGuide -> {
                    PasswordImportGuideV5(route.source, route.problem, model.busy, { pickExport(route.source) }) { back(route) }
                }
                is PasswordsRoute.ImportDone -> {
                    val healthIssues = remember(logins, breaches, route.report) {
                        val added = route.report.addedIds.toSet()
                        val report = PasswordHealthRules.report(logins, breaches)
                        // The v5 check counts plain-HTTP logins too; the count here matches the page it opens.
                        report.attentionIds(withInsecure = true).count { it in added }
                    }
                    val onCheck = { use { model.route = PasswordsRoute.Health } }
                    PasswordImportResultV5(
                        report = route.report,
                        fileState = route.fileState,
                        healthIssues = healthIssues,
                        onDeleteFile = { deleteExport(route) },
                        onCheck = onCheck,
                        onDone = { back(route) },
                        conflictState = route.conflictState,
                        onUseFilePasswords = { useFilePasswords(route) },
                    )
                }
                // V24: the v5 check runs the leak check only when asked, never on opening.
                PasswordsRoute.Health -> {
                    PasswordHealthV5(
                        logins = logins,
                        breaches = breaches,
                        unchecked = remember(logins, model.leakAnswers) { LeakAnswers.unchecked(logins, model.leakAnswers) },
                        leakCheck = model.leakStatus,
                        leakProgress = model.leakProgress,
                        checkedAt = model.leakCheckedAt,
                        onCheckLeaks = { checkLeaks(logins) },
                        onStopLeakCheck = ::stopLeakCheck,
                        onFinding = { finding -> use { model.route = PasswordsRoute.HealthFinding(finding.loginId, finding.kind) } },
                        onBack = { back(route) },
                    )
                }
                is PasswordsRoute.HealthFinding -> PasswordFindingV5(
                    logins = logins,
                    breaches = breaches,
                    loginId = route.id,
                    kind = route.kind,
                    onChange = { login -> use { openChangePassword(login.origin) } },
                    onOpenLogin = { login -> use { model.route = PasswordsRoute.Detail(login.id) } },
                    onBack = { back(route) },
                    // A page on its way out (the vault just locked) has no logins: it must not steer.
                    onGone = { if (model.route == route) model.route = PasswordsRoute.Health },
                )
                is PasswordsRoute.Detail -> {
                    val login = logins.firstOrNull { it.id == route.id }
                    if (login == null) {
                        LaunchedEffect(route) { if (model.route == route) model.route = PasswordsRoute.Logins }
                    } else {
                        PasswordDetailV5(
                            login = login,
                            onCopyUsername = { use { copy(login.username) } },
                            onCopyPassword = { use { copy(login.password) } },
                            onEdit = { use { model.route = PasswordsRoute.Edit(login.id) } },
                            onDelete = { delete(login.id) },
                            onBack = { back(route) },
                            onCopyCode = { code -> use { copy(code) } },
                            onSetTotp = { input -> setTotp(login.id, input) },
                            onRemoveTotp = { writeTotp(login.id, null) },
                            onOpenSite = { use { openSite(login.origin) } },
                        )
                    }
                }
                is PasswordsRoute.Edit -> {
                    PasswordEditV5(
                        initial = route.id?.let { id -> logins.firstOrNull { it.id == id } },
                        error = route.error,
                        busy = model.busy,
                        onSave = { site, username, password, note -> save(route, site, username, password, note) },
                        onBack = { back(route) },
                    )
                }
            }
        }
    }

    /** The vault opening or closing gets its own moment; every other step cross-fades. */
    private fun transitionBetween(from: PasswordsRoute, to: PasswordsRoute): PasswordsTransition = when {
        !from.needsOpenVault && to.needsOpenVault -> PasswordsTransition.Unlock
        from.needsOpenVault && to == PasswordsRoute.Locked -> PasswordsTransition.Lock
        else -> PasswordsTransition.Step
    }

    private fun back(route: PasswordsRoute) {
        if (model.busy) return
        model.message = null
        model.route = when (route) {
            is PasswordsRoute.Phrase, is PasswordsRoute.Confirm -> PasswordsRoute.Intro
            is PasswordsRoute.Recover -> PasswordsRoute.Locked
            is PasswordsRoute.Detail -> PasswordsRoute.Logins
            PasswordsRoute.Health -> PasswordsRoute.Logins
            is PasswordsRoute.HealthFinding -> PasswordsRoute.Health
            PasswordsRoute.Import -> if (model.launchedForImport) {
                finish()
                return
            } else {
                PasswordsRoute.Logins
            }
            is PasswordsRoute.ImportGuide -> PasswordsRoute.Import
            // Bookmarks alone, or no open vault: nothing to show here, back to where the user came from.
            is PasswordsRoute.ImportDone -> if (vault.isUnlocked && (route.report.hadPasswords || !model.launchedForImport)) {
                PasswordsRoute.Logins
            } else {
                finish()
                return
            }
            is PasswordsRoute.Edit -> route.id?.let(PasswordsRoute::Detail) ?: PasswordsRoute.Logins
            PasswordsRoute.Intro, PasswordsRoute.Locked -> model.pendingImport?.let { pending ->
                // Passwords waited for the vault; going back drops them and returns to their steps.
                model.pendingImport = null
                PasswordsRoute.ImportGuide(pending.source)
            } ?: run {
                finish()
                return
            }
            PasswordsRoute.Logins -> {
                finish()
                return
            }
        }
    }

    /** Asks Pwned Passwords which passwords leaked, sending only a hash prefix for each (V24 port). */
    private fun checkLeaks(logins: List<VaultLogin>) {
        if (model.leakStatus == LeakCheckStatus.Checking) return
        // Only the model and the check go into the coroutine: it may outlive this activity.
        val state = model
        val check = passwordCheck
        state.statusBeforeCheck = state.leakStatus
        state.leakStatus = LeakCheckStatus.Checking
        state.leakProgress = 0f
        state.leakJob = state.viewModelScope.launch {
            val result = check.checkLeaks(logins) { progress -> state.leakProgress = progress.fraction }
            state.leakJob = null
            if (state.leakStatus != LeakCheckStatus.Checking) return@launch
            // Merged, not replaced: a prefix without an answer this time keeps the answer it had.
            state.leakAnswers = LeakAnswers.record(state.leakAnswers, logins, result)
            state.leakStatus = if (result.complete) LeakCheckStatus.Done else LeakCheckStatus.Failed
            if (result.complete) state.leakCheckedAt = System.currentTimeMillis()
        }
    }

    /** «Stop»: nothing of the stopped check is kept; the page is as it was before it. */
    private fun stopLeakCheck() {
        model.leakJob?.cancel()
        model.leakJob = null
        if (model.leakStatus == LeakCheckStatus.Checking) model.leakStatus = model.statusBeforeCheck
    }

    /** The site's own change-password page in Vola, or the site's start page when it has none. */
    private fun openChangePassword(origin: String) {
        if (model.busy) return
        model.busy = true
        lifecycleScope.launch {
            val target = try {
                withContext(Dispatchers.IO) { ChangePasswordPage.resolve(origin) }
            } finally {
                model.busy = false
            }
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(target)).setPackage(packageName)
            runCatching { startActivity(intent) }
        }
    }

    /** The login's site in a Vola tab; the passwords stay where they are behind it. */
    private fun openSite(origin: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(origin)).setPackage(packageName)
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
                model.unlocking = true
                val result = try {
                    withContext(Dispatchers.IO) { vault.unlock(KeystoreVaultKeyWrapper()) }
                } finally {
                    model.unlocking = false
                }
                when (result) {
                    VaultOpenResult.Opened -> opened()
                    VaultOpenResult.Refused, VaultOpenResult.NoSlot -> {
                        model.deviceKeyLost = true
                        model.message = R.string.passwords_device_key_lost
                    }
                    VaultOpenResult.Missing -> model.route = PasswordsRoute.Intro
                    // Locked or erased while it opened: the screen already shows that.
                    VaultOpenResult.Superseded -> Unit
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
                VaultOpenResult.Superseded -> Unit
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
        window.decorView.performV5Haptic(V5Haptic.Confirm)
        model.message = null
        model.revision++
        val pending = model.pendingImport
        model.pendingImport = null
        model.route = pending?.let { PasswordsRoute.ImportGuide(it.source) } ?: PasswordsRoute.Logins
        model.resumeImport = pending
    }

    /** Opens the system file picker for exports; what it may show is kept to text, HTML and JSON. */
    private fun pickExport(source: PasswordImportSource) {
        if (model.busy) return
        model.importSource = source
        runCatching { exportPicker.launch(EXPORT_TYPES) }
    }

    private fun importFrom(files: List<Uri>) {
        if (files.isEmpty() || model.busy) return
        val source = model.importSource
        if (vault.isUnlocked) CredentialVaultSession.touch(vault)
        runBusy {
            when (val outcome = withContext(Dispatchers.IO) { importFiles(files, source) }) {
                is ImportOutcome.Done -> model.route = outcome.route
                is ImportOutcome.NeedsVault -> {
                    model.pendingImport = PendingImport(source, outcome.passwordFiles)
                    // Bookmarks came in already; the line says only the passwords wait (FR-06).
                    if (outcome.bookmarksImported) model.message = R.string.passwords_import_bookmarks_moved
                    model.route = if (vault.exists) PasswordsRoute.Locked else PasswordsRoute.Intro
                }
            }
            model.revision++
        }
    }

    private sealed interface ImportOutcome {
        data class Done(val route: PasswordsRoute) : ImportOutcome

        /** Passwords are among the files and the vault is not open yet. */
        /** Passwords wait for the vault; bookmarks need none and were moved already. */
        data class NeedsVault(val passwordFiles: List<Uri>, val bookmarksImported: Boolean) : ImportOutcome
    }

    /**
     * Reads [files], tells bookmark exports (HTML) from password exports, and brings both in:
     * bookmarks into favorites, passwords into the vault in one write.
     */
    private fun importFiles(files: List<Uri>, source: PasswordImportSource): ImportOutcome {
        val now = System.currentTimeMillis()
        var problem: PasswordImportProblem? = null
        val loaded = files.mapNotNull { uri ->
            when (val file = PasswordImportFiles.read(contentResolver, uri)) {
                is PasswordImportFile.Loaded -> uri to file
                PasswordImportFile.TooLarge -> null.also { problem = problem ?: PasswordImportProblem.TooLarge }
                PasswordImportFile.NotText -> null.also { problem = problem ?: PasswordImportProblem.NotText }
                PasswordImportFile.Unreadable -> null.also { problem = problem ?: PasswordImportProblem.Unreadable }
            }
        }
        val favorites = ArrayList<FavoriteEntry>()
        var bookmarkFiles = 0
        val passwordFiles = ArrayList<Uri>()
        val logins = ArrayList<ImportedLogin>()
        var passwordsSkipped = 0
        for ((uri, file) in loaded) {
            val bookmarks = FavoriteBookmarkImportRules.parse(file.text, now)
            if (bookmarks is FavoriteBookmarkParseResult.Parsed) {
                bookmarkFiles++
                favorites += bookmarks.favorites
                continue
            }
            when (val parsed = PasswordImportRules.parse(file.text, PasswordsRules::manualOrigin)) {
                is PasswordImportParse.Read -> {
                    passwordFiles += uri
                    logins += parsed.logins
                    passwordsSkipped += parsed.skipped
                }
                PasswordImportParse.Encrypted -> problem = problem ?: PasswordImportProblem.Encrypted
                PasswordImportParse.NotAnExport -> problem = problem ?: PasswordImportProblem.NotAnExport
                PasswordImportParse.TooLarge -> problem = problem ?: PasswordImportProblem.TooLarge
            }
        }
        if (logins.isNotEmpty() && !vault.isUnlocked) {
            // Without a screen lock there is no vault at all: bookmarks must not wait for one too.
            val moved = favorites.isNotEmpty() &&
                (BrowserSessionStore(this).mergeImportedFavoritesCommitted(favorites)?.importedCount ?: 0) > 0
            if (moved) FavoriteLibrarySignal.markChanged()
            return ImportOutcome.NeedsVault(passwordFiles, bookmarksImported = moved)
        }
        fun problem(fallback: PasswordImportProblem) =
            ImportOutcome.Done(PasswordsRoute.ImportGuide(source, problem ?: fallback))
        if (logins.isEmpty() && favorites.isEmpty()) return problem(PasswordImportProblem.NoLogins)

        val merged = if (favorites.isEmpty()) null else BrowserSessionStore(this).mergeImportedFavoritesCommitted(favorites)
        if (merged != null && merged.importedCount > 0) FavoriteLibrarySignal.markChanged()
        val summary = if (logins.isEmpty()) {
            null
        } else {
            (vault.importLogins(logins, now) as? VaultImportResult.Imported)?.summary
        }
        val passwordsFailed = logins.isNotEmpty() && summary == null
        val bookmarksFailed = favorites.isNotEmpty() && merged == null
        if ((passwordsFailed || logins.isEmpty()) && (bookmarksFailed || favorites.isEmpty())) {
            return problem(PasswordImportProblem.Failed)
        }
        val shown = loaded.filter { (uri, _) -> uri in passwordFiles }.ifEmpty { loaded }.map { it.second }
        val report = PasswordImportReport(
            fileName = shown.mapNotNull(PasswordImportFile.Loaded::name).joinToString(", ").ifEmpty { null },
            fileSizeBytes = shown.sumOf(PasswordImportFile.Loaded::sizeBytes),
            added = summary?.added ?: 0,
            duplicates = summary?.duplicates ?: 0,
            skipped = passwordsSkipped + (summary?.rejected ?: 0),
            withTotp = summary?.withTotp ?: 0,
            addedIds = summary?.addedIds.orEmpty(),
            hadPasswords = summary != null,
            bookmarks = merged?.importedCount ?: 0,
            bookmarksSkipped = merged?.skippedCount ?: 0,
            bookmarksLimitReached = merged?.limitReached == true,
            conflicts = summary?.conflicts.orEmpty(),
            full = summary?.full ?: 0,
        )
        return ImportOutcome.Done(PasswordsRoute.ImportDone(report, passwordFiles))
    }

    /** The user takes the file's passwords for the logins Vola kept its own for (E18-04). */
    private fun useFilePasswords(route: PasswordsRoute.ImportDone) = use {
        if (route.conflictState == PasswordImportConflictState.Replacing) return@use
        model.route = route.copy(conflictState = PasswordImportConflictState.Replacing)
        lifecycleScope.launch {
            val now = System.currentTimeMillis()
            val replaced = withContext(Dispatchers.IO) { vault.replacePasswords(route.report.conflicts, now) }
            if (replaced) model.revision++
            val current = model.route as? PasswordsRoute.ImportDone ?: return@launch
            model.route = current.copy(
                conflictState = if (replaced) PasswordImportConflictState.Replaced else PasswordImportConflictState.Failed,
            )
        }
    }

    private fun deleteExport(route: PasswordsRoute.ImportDone) {
        if (route.fileState != PasswordImportFileState.Present) return
        model.route = route.copy(fileState = PasswordImportFileState.Deleting)
        lifecycleScope.launch {
            val deleted = withContext(Dispatchers.IO) {
                route.files.map { file -> PasswordImportFiles.delete(contentResolver, file) }.all { it }
            }
            val current = model.route as? PasswordsRoute.ImportDone ?: return@launch
            model.route = current.copy(
                fileState = if (deleted) PasswordImportFileState.Deleted else PasswordImportFileState.DeleteFailed,
            )
        }
    }

    /**
     * Deletes the vault, its login index and the device key: the way out when the key and the phrase
     * are both lost, or when the user goes back to the system password manager. The owner of the
     * screen lock confirms first; a phone without one has nothing stronger to ask for.
     */
    private fun eraseVault() {
        if (model.busy) return
        if (canAuthenticate()) authenticate(::erase) else erase()
    }

    private fun erase() = runBusy {
        val erased = withContext(Dispatchers.IO) {
            vault.destroy().also { done -> if (done) KeystoreVaultKeyWrapper().deleteKey() }
        }
        if (erased) {
            CredentialVaultSession.lockNow()
            model.deviceKeyLost = false
            model.pendingImport = null
            model.leakStatus = LeakCheckStatus.Off
            model.leakAnswers = emptyMap()
            model.message = null
            model.revision++
            model.route = PasswordsRoute.Intro
        } else {
            model.message = R.string.passwords_failed
        }
    }

    /**
     * «Export passwords»: the owner of the screen lock confirms first, as Chrome and Safari ask,
     * then the system file picker names the file.
     */
    private fun startExport() = use {
        if (model.busy) return@use
        val pick = { runCatching { exportTarget.launch(PasswordExportRules.FILE_NAME) } }
        if (canAuthenticate()) authenticate { pick() } else pick()
    }

    private fun exportTo(target: Uri) {
        // The picker may outlast the five minutes: a vault that locked meanwhile writes nothing.
        if (!vault.isUnlocked) {
            model.route = PasswordsRoute.Locked
            return
        }
        CredentialVaultSession.touch(vault)
        val logins = vault.allLogins()
        runBusy {
            val written = withContext(Dispatchers.IO) {
                runCatching {
                    val csv = PasswordExportRules.csv(logins) { login -> PasswordsRules.displaySite(login.origin) }
                    contentResolver.openOutputStream(target, "wt")?.use { output ->
                        output.write(csv.toByteArray(Charsets.UTF_8))
                        true
                    } ?: false
                }.getOrDefault(false)
            }
            val text = if (written) {
                resources.getQuantityString(R.plurals.passwords_exported, logins.size, logins.size)
            } else {
                getString(R.string.passwords_export_failed)
            }
            Toast.makeText(this@PasswordsActivity, text, Toast.LENGTH_LONG).show()
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

    private fun save(route: PasswordsRoute.Edit, site: String, username: String, password: String, note: String) = use {
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
                    val cleanNote = CredentialVaultRules.cleanNote(note)
                    val result = withContext(Dispatchers.IO) {
                        val saved = if (route.id == null) vault.save(draft, now) else vault.update(route.id, draft, now)
                        // The note is the user's, not the page's: it is kept apart from what a site may save.
                        val id = when (saved) {
                            is VaultSaveResult.Added -> saved.login.id
                            is VaultSaveResult.Updated -> saved.login.id
                            is VaultSaveResult.Unchanged -> saved.login.id
                            else -> null
                        }
                        if (id != null && !vault.setNote(id, cleanNote, now)) VaultSaveResult.Failed else saved
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

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    val text = PasswordsRules.authenticationError(errorCode) ?: return
                    // The lock and intro pages show the line; on the others (export, say) a toast does.
                    when (model.route) {
                        PasswordsRoute.Locked, PasswordsRoute.Intro -> model.message = text
                        else -> Toast.makeText(this@PasswordsActivity, text, Toast.LENGTH_LONG).show()
                    }
                }
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
        /** CSV and HTML come as text of one kind or another, Bitwarden's export as JSON; some providers know neither. */
        val EXPORT_TYPES = arrayOf(
            "text/*",
            "application/json",
            "application/xhtml+xml",
            "application/octet-stream",
            "application/vnd.ms-excel",
        )

        /** The same pair the device key accepts: strong biometrics or the screen lock. */
        const val AUTHENTICATORS =
            BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
    }
}
