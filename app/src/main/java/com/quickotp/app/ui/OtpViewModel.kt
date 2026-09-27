package com.quickotp.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.quickotp.app.QuickOtpApp
import com.quickotp.app.R
import com.quickotp.app.barcode.ClassifiedScan
import com.quickotp.app.barcode.ScanClassifier
import com.quickotp.app.data.EntryKind
import com.quickotp.app.data.OtpAccount
import com.quickotp.app.data.toAccount
import com.quickotp.app.otp.OtpDraft
import com.quickotp.app.otp.OtpInputParser
import com.quickotp.app.otp.OtpType
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class OtpViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = QuickOtpApp.from(app).otpRepository
    private val context = app.applicationContext

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

    fun notifyCopied() {
        viewModelScope.launch { _messages.send(context.getString(R.string.copied)) }
    }
}
