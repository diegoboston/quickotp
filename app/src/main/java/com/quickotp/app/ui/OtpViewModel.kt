package com.quickotp.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.quickotp.app.QuickOtpApp
import com.quickotp.app.R
import com.quickotp.app.barcode.ClassifiedScan
import com.quickotp.app.barcode.ScanClassifier
import com.quickotp.app.data.EntryKind
import com.quickotp.app.data.EntryOrder
import com.quickotp.app.data.OtpAccount
import com.quickotp.app.data.toAccount
import com.quickotp.app.otp.OtpDraft
import com.quickotp.app.otp.OtpInputParser
import com.quickotp.app.otp.OtpType
import com.quickotp.app.update.AppUpdate
import com.quickotp.app.update.AppUpdateUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class OtpViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = QuickOtpApp.from(app).otpRepository
    private val context = app.applicationContext

    init {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { AppUpdate.clearStaleUpdateCache(getApplication()) }
        }
    }

    val accounts: StateFlow<List<OtpAccount>> = repo.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _pendingDraft = MutableStateFlow<OtpDraft?>(null)
    val pendingDraft: StateFlow<OtpDraft?> = _pendingDraft.asStateFlow()

    private val _pendingBarcode = MutableStateFlow<ClassifiedScan.Stored?>(null)
    val pendingBarcode: StateFlow<ClassifiedScan.Stored?> = _pendingBarcode.asStateFlow()

    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages = _messages.receiveAsFlow()

    fun tryAcceptInput(raw: String, invalidMessage: String = context.getString(R.string.invalid_secret)): Boolean {
        return try {
            _pendingBarcode.value = null
            _pendingDraft.value = OtpInputParser.parse(raw)
            true
        } catch (_: Exception) {
            viewModelScope.launch { _messages.send(invalidMessage) }
            false
        }
    }

    fun tryAcceptScan(
        raw: String,
        format: String,
        invalidMessage: String = context.getString(R.string.invalid_barcode),
    ): Boolean {
        val classified = try {
            ScanClassifier.classify(raw, format)
        } catch (_: Exception) {
            viewModelScope.launch { _messages.send(invalidMessage) }
            return false
        }
        when (classified) {
            is ClassifiedScan.Otp -> {
                _pendingBarcode.value = null
                _pendingDraft.value = classified.draft
            }
            is ClassifiedScan.Stored -> {
                _pendingDraft.value = null
                _pendingBarcode.value = classified
            }
        }
        return true
    }

    fun clearPendingDraft() {
        _pendingDraft.value = null
    }

    fun clearPendingBarcode() {
        _pendingBarcode.value = null
    }

    fun savePending(label: String) {
        val draft = _pendingDraft.value ?: return
        val trimmed = label.trim()
        if (trimmed.isEmpty()) {
            viewModelScope.launch { _messages.send(context.getString(R.string.label_required)) }
            return
        }
        _pendingDraft.value = null
        viewModelScope.launch {
            repo.insert(draft.toAccount(trimmed))
        }
    }

    fun savePendingBarcode(label: String) {
        val stored = _pendingBarcode.value ?: return
        val trimmed = label.trim()
        if (trimmed.isEmpty()) {
            viewModelScope.launch { _messages.send(context.getString(R.string.label_required)) }
            return
        }
        _pendingBarcode.value = null
        viewModelScope.launch {
            repo.insert(
                OtpAccount(
                    label = trimmed,
                    secret = stored.value,
                    kind = EntryKind.BARCODE.name,
                    barcodeFormat = stored.format,
                ),
            )
        }
    }

    fun rename(id: Long, label: String) {
        val trimmed = label.trim()
        if (trimmed.isEmpty()) {
            viewModelScope.launch { _messages.send(context.getString(R.string.label_required)) }
            return
        }
        viewModelScope.launch {
            val account = repo.getById(id) ?: return@launch
            repo.update(account.copy(label = trimmed))
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch { repo.delete(id) }
    }

    fun advanceHotp(id: Long) {
        viewModelScope.launch {
            val account = repo.getById(id) ?: return@launch
            if (account.isBarcode || account.otpType != OtpType.HOTP) return@launch
            repo.update(account.copy(counter = account.counter + 1))
        }
    }

    /** Moves an entry [offset] places in the home list (negative is up). */
    fun move(id: Long, offset: Int) {
        val reordered = EntryOrder.move(accounts.value.map { it.id }, id, offset) ?: return
        viewModelScope.launch { repo.reorder(reordered) }
    }

    fun setValueVisible(id: Long, visible: Boolean) {
        viewModelScope.launch {
            val account = repo.getById(id) ?: return@launch
            if (account.valueVisible == visible) return@launch
            repo.update(account.copy(valueVisible = visible))
        }
    }

    fun notifyCopied() {
        viewModelScope.launch { _messages.send(context.getString(R.string.copied)) }
    }

    private val _appUpdateState = MutableStateFlow<AppUpdateUiState?>(null)
    val appUpdateState: StateFlow<AppUpdateUiState?> = _appUpdateState.asStateFlow()

    private val _launchUpdatePromptHandled = MutableStateFlow(false)

    /**
     * One-shot GitHub Releases check on cold start. Returns the remote version
     * name when a newer build exists, or null when up to date or the check fails.
     */
    fun checkLaunchUpdateAvailable(onResult: (String?) -> Unit) {
        if (_launchUpdatePromptHandled.value) return
        viewModelScope.launch(Dispatchers.IO) {
            val result = try {
                val local = AppUpdate.installedVersionCode(getApplication())
                val release = AppUpdate.fetchLatestRelease()
                if (release.versionCode > local) release.versionName else null
            } catch (_: Exception) {
                null
            }
            _launchUpdatePromptHandled.value = true
            withContext(Dispatchers.Main.immediate) { onResult(result) }
        }
    }

    fun clearAppUpdateState() {
        _appUpdateState.value = null
    }

    /** Checks GitHub Releases and downloads a newer APK when one exists. */
    fun startAppUpdateDownload() {
        val current = _appUpdateState.value
        if (current is AppUpdateUiState.Checking || current is AppUpdateUiState.Downloading) {
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            _appUpdateState.value = AppUpdateUiState.Checking
            try {
                val local = AppUpdate.installedVersionCode(getApplication())
                val release = AppUpdate.fetchLatestRelease()
                if (release.versionCode <= local) {
                    AppUpdate.clearUpdateCache(getApplication())
                    _appUpdateState.value = AppUpdateUiState.UpToDate(release.versionName)
                    return@launch
                }
                _appUpdateState.value = AppUpdateUiState.Downloading(null)
                val apk = AppUpdate.resolveApkForUpdate(getApplication(), release) { frac ->
                    _appUpdateState.value = AppUpdateUiState.Downloading(frac)
                }
                _appUpdateState.value = AppUpdateUiState.ReadyToInstall(apk, release.versionName)
            } catch (e: Exception) {
                AppUpdate.clearUpdateCache(getApplication())
                _appUpdateState.value = AppUpdateUiState.Failed(e.message ?: e.toString())
            }
        }
    }
}
