package com.quickotp.app.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DriveFileRenameOutline
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quickotp.app.R
import com.quickotp.app.barcode.ClassifiedScan
import com.quickotp.app.barcode.MlKitBarcodes
import com.quickotp.app.barcode.QrBitmaps
import com.quickotp.app.barcode.WebUrls
import com.quickotp.app.data.OtpAccount
import com.quickotp.app.otp.OtpDraft
import com.quickotp.app.ui.OtpViewModel
import com.quickotp.app.ui.components.AppUpdateInProgressBanner
import com.quickotp.app.ui.components.OtpCodeRow
import com.quickotp.app.ui.components.StoredBarcodeRow
import com.quickotp.app.update.AppUpdate
import com.quickotp.app.update.AppUpdateUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume

private sealed interface HomeDialog {
    data object None : HomeDialog
    data object AddSecret : HomeDialog
    data class AskLabel(val draft: OtpDraft) : HomeDialog
    data class AskBarcodeLabel(val stored: ClassifiedScan.Stored) : HomeDialog
    data class Menu(val account: OtpAccount) : HomeDialog
    data class Rename(val account: OtpAccount) : HomeDialog
    data class ShowBarcode(val account: OtpAccount) : HomeDialog
    data class ShowSecret(val account: OtpAccount) : HomeDialog
    data class ConfirmDelete(val account: OtpAccount) : HomeDialog
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: OtpViewModel) {
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val pendingDraft by viewModel.pendingDraft.collectAsStateWithLifecycle()
    val pendingBarcode by viewModel.pendingBarcode.collectAsStateWithLifecycle()
    val appUpdateState by viewModel.appUpdateState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    var scanning by remember { mutableStateOf(false) }
    var dialog by remember { mutableStateOf<HomeDialog>(HomeDialog.None) }
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            nowMillis = System.currentTimeMillis()
            delay(200)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    LaunchedEffect(pendingDraft, pendingBarcode) {
        val draft = pendingDraft
        val barcode = pendingBarcode
        when {
            draft != null && dialog !is HomeDialog.AskLabel -> {
                scanning = false
                dialog = HomeDialog.AskLabel(draft)
            }
            barcode != null && dialog !is HomeDialog.AskBarcodeLabel -> {
                scanning = false
                dialog = HomeDialog.AskBarcodeLabel(barcode)
            }
        }
    }

    var pendingUpdateApk by remember { mutableStateOf<File?>(null) }
    val installPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        val apk = pendingUpdateApk
        pendingUpdateApk = null
        if (apk == null) return@rememberLauncherForActivityResult
        if (context.packageManager.canRequestPackageInstalls()) {
            AppUpdate.launchInstaller(context, apk)
        } else {
            coroutineScope.launch {
                snackbarHostState.showSnackbar(context.getString(R.string.update_app_needs_permission))
            }
        }
    }
    fun installAppUpdate(apk: File) {
        when (AppUpdate.launchInstaller(context, apk)) {
            AppUpdate.InstallResult.Launched -> Unit
            AppUpdate.InstallResult.NeedsPermission -> {
                pendingUpdateApk = apk
                installPermissionLauncher.launch(
                    Intent(
                        Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        Uri.parse("package:${context.packageName}"),
                    ),
                )
            }
        }
    }

    LaunchedEffect(Unit) {
        val versionName = suspendCancellableCoroutine<String?> { cont ->
            viewModel.checkLaunchUpdateAvailable { cont.resume(it) }
        } ?: return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(
            message = context.getString(R.string.update_app_snackbar_prompt, versionName),
            actionLabel = context.getString(R.string.update_app_snackbar_action),
            withDismissAction = true,
            duration = SnackbarDuration.Long,
        )
        if (result == SnackbarResult.ActionPerformed) {
            viewModel.startAppUpdateDownload()
        }
    }

    val terminalAppUpdate = when (val state = appUpdateState) {
        is AppUpdateUiState.UpToDate,
        is AppUpdateUiState.ReadyToInstall,
        is AppUpdateUiState.Failed,
        -> state
        else -> null
    }
    LaunchedEffect(terminalAppUpdate) {
        when (val state = terminalAppUpdate ?: return@LaunchedEffect) {
            is AppUpdateUiState.UpToDate -> snackbarHostState.showSnackbar(
                context.getString(R.string.update_app_up_to_date, state.versionName),
            )
            is AppUpdateUiState.ReadyToInstall -> {
                snackbarHostState.showSnackbar(
                    context.getString(R.string.update_app_ready, state.versionName),
                )
                installAppUpdate(state.apk)
            }
            is AppUpdateUiState.Failed -> snackbarHostState.showSnackbar(
                context.getString(R.string.update_app_failed, state.message),
            )
            else -> Unit
        }
        viewModel.clearAppUpdateState()
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            scanning = true
        } else {
            coroutineScope.launch {
                snackbarHostState.showSnackbar(context.getString(R.string.camera_permission_required))
            }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        MlKitBarcodes.scanImage(
            context = context,
            uri = uri,
            onFound = { raw, format ->
                viewModel.tryAcceptScan(
                    raw,
                    format,
                    context.getString(R.string.invalid_barcode),
                )
            },
            onNotFound = {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(context.getString(R.string.no_barcode_in_image))
                }
            },
        )
    }

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.app_name)) },
                    actions = {
                        IconButton(
                            onClick = {
                                val granted = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.CAMERA,
                                ) == PackageManager.PERMISSION_GRANTED
                                if (granted) {
                                    scanning = true
                                } else {
                                    cameraLauncher.launch(Manifest.permission.CAMERA)
                                }
                            },
                        ) {
                            Icon(
                                imageVector = Icons.Filled.QrCodeScanner,
                                contentDescription = stringResource(R.string.add_from_barcode),
                            )
                        }
                        IconButton(
                            onClick = {
                                galleryLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                                )
                            },
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PhotoLibrary,
                                contentDescription = stringResource(R.string.add_from_image),
                            )
                        }
                        IconButton(onClick = { dialog = HomeDialog.AddSecret }) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = stringResource(R.string.add_from_secret),
                            )
                        }
                    },
                )
            },
            snackbarHost = {},
        ) { padding ->
            val showingQr = selectedTab == 1
            val visibleAccounts = accounts.filter { it.isBarcode == showingQr }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                AppUpdateInProgressBanner(
                    state = appUpdateState,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
                PrimaryTabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = !showingQr,
                        onClick = { selectedTab = 0 },
                        text = { Text(stringResource(R.string.tab_otp)) },
                    )
                    Tab(
                        selected = showingQr,
                        onClick = { selectedTab = 1 },
                        text = { Text(stringResource(R.string.tab_qr)) },
                    )
                }
                if (visibleAccounts.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(
                                if (showingQr) R.string.empty_qr_codes else R.string.empty_accounts,
                            ),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(0.dp),
                    ) {
                        itemsIndexed(visibleAccounts, key = { _, account -> account.id }) { index, account ->
                            val canMoveUp = index > 0
                            val canMoveDown = index < visibleAccounts.lastIndex
                            fun move(direction: Int) {
                                val neighbor = visibleAccounts.getOrNull(index + direction) ?: return
                                val from = accounts.indexOfFirst { it.id == account.id }
                                val to = accounts.indexOfFirst { it.id == neighbor.id }
                                if (from >= 0 && to >= 0 && to != from) {
                                    viewModel.move(account.id, to - from)
                                }
                            }
                            if (account.isBarcode) {
                                StoredBarcodeRow(
                                    account = account,
                                    onCopyValue = { value ->
                                        clipboard.setText(AnnotatedString(value))
                                        viewModel.notifyCopied()
                                    },
                                    onOpenUrl = { url ->
                                        val opened = runCatching {
                                            context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
                                        }.isSuccess
                                        if (!opened) {
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar(
                                                    context.getString(R.string.open_link_failed),
                                                )
                                            }
                                        }
                                    },
                                    onToggleValue = {
                                        viewModel.setValueVisible(account.id, !account.valueVisible)
                                    },
                                    onEdit = { dialog = HomeDialog.Menu(account) },
                                    canMoveUp = canMoveUp,
                                    canMoveDown = canMoveDown,
                                    onMoveUp = { move(-1) },
                                    onMoveDown = { move(1) },
                                )
                            } else {
                                OtpCodeRow(
                                    account = account,
                                    nowMillis = nowMillis,
                                    onCopyCode = { code ->
                                        clipboard.setText(AnnotatedString(code))
                                        viewModel.notifyCopied()
                                    },
                                    onToggleValue = {
                                        viewModel.setValueVisible(account.id, !account.valueVisible)
                                    },
                                    onAdvanceHotp = { viewModel.advanceHotp(account.id) },
                                    onEdit = { dialog = HomeDialog.Menu(account) },
                                    canMoveUp = canMoveUp,
                                    canMoveDown = canMoveDown,
                                    onMoveUp = { move(-1) },
                                    onMoveDown = { move(1) },
                                )
                            }
                        }
                    }
                }
            }
        }

        if (scanning) {
            ScanScreen(
                onScanned = { raw, format ->
                    viewModel.tryAcceptScan(
                        raw,
                        format,
                        context.getString(R.string.invalid_barcode),
                    )
                },
                onClose = { scanning = false },
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }

    when (val current = dialog) {
        HomeDialog.None -> Unit
        HomeDialog.AddSecret -> AddSecretDialog(
            onDismiss = { dialog = HomeDialog.None },
            onContinue = { raw ->
                if (viewModel.tryAcceptInput(raw)) {
                    dialog = HomeDialog.None
                }
            },
        )
        is HomeDialog.AskLabel -> LabelDialog(
            title = stringResource(R.string.label_title),
            initialLabel = current.draft.suggestedLabel(),
            onDismiss = {
                viewModel.clearPendingDraft()
                dialog = HomeDialog.None
            },
            onSave = { label ->
                viewModel.savePending(label)
                selectedTab = 0
                dialog = HomeDialog.None
            },
        )
        is HomeDialog.AskBarcodeLabel -> {
            val link = WebUrls.httpUrlOrNull(current.stored.value)
            LabelDialog(
                title = stringResource(R.string.label_barcode_title),
                initialLabel = "",
                link = link,
                onDismiss = {
                    viewModel.clearPendingBarcode()
                    dialog = HomeDialog.None
                },
                onSave = { label ->
                    viewModel.savePendingBarcode(label)
                    selectedTab = 1
                    dialog = HomeDialog.None
                },
                onNavigate = if (link == null) {
                    null
                } else {
                    {
                        val opened = runCatching {
                            context.startActivity(Intent(Intent.ACTION_VIEW, link.toUri()))
                        }.isSuccess
                        if (opened) {
                            viewModel.clearPendingBarcode()
                            dialog = HomeDialog.None
                        } else {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(
                                    context.getString(R.string.open_link_failed),
                                )
                            }
                        }
                    }
                },
            )
        }
        is HomeDialog.Menu -> EditSheet(
            account = current.account,
            onDismiss = { dialog = HomeDialog.None },
            onRename = { dialog = HomeDialog.Rename(current.account) },
            onShowBarcode = { dialog = HomeDialog.ShowBarcode(current.account) },
            onShowSecret = { dialog = HomeDialog.ShowSecret(current.account) },
            onDelete = { dialog = HomeDialog.ConfirmDelete(current.account) },
        )
        is HomeDialog.Rename -> RenameDialog(
            account = current.account,
            onDismiss = { dialog = HomeDialog.None },
            onSave = { label ->
                viewModel.rename(current.account.id, label)
                dialog = HomeDialog.None
            },
        )
        is HomeDialog.ShowBarcode -> BarcodeDialog(
            account = current.account,
            onDismiss = { dialog = HomeDialog.None },
        )
        is HomeDialog.ShowSecret -> SecretDialog(
            account = current.account,
            onDismiss = { dialog = HomeDialog.None },
            onCopy = { secret ->
                clipboard.setText(AnnotatedString(secret))
                viewModel.notifyCopied()
            },
        )
        is HomeDialog.ConfirmDelete -> ConfirmDeleteDialog(
            account = current.account,
            onDismiss = { dialog = HomeDialog.None },
            onConfirm = {
                viewModel.delete(current.account.id)
                dialog = HomeDialog.None
            },
        )
    }
}

@Composable
private fun AddSecretDialog(
    onDismiss: () -> Unit,
    onContinue: (String) -> Unit,
) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_from_secret)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.secret_or_uri)) },
                placeholder = { Text(stringResource(R.string.secret_hint)) },
                singleLine = false,
                minLines = 3,
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onContinue(text) },
                enabled = text.isNotBlank(),
            ) {
                Text(stringResource(R.string.continue_action))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

@Composable
private fun LabelDialog(
    title: String,
    initialLabel: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    link: String? = null,
    onNavigate: (() -> Unit)? = null,
) {
    var text by remember { mutableStateOf(initialLabel) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (link != null) {
                    Text(
                        text = link,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        textDecoration = TextDecoration.Underline,
                    )
                    Spacer(Modifier.height(12.dp))
                }
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.label_hint)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                    ),
                )
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.cancel))
                }
                if (onNavigate != null) {
                    TextButton(onClick = onNavigate) {
                        Text(stringResource(R.string.navigate))
                    }
                }
                TextButton(
                    onClick = { onSave(text) },
                    enabled = text.isNotBlank(),
                ) {
                    Text(stringResource(R.string.save))
                }
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditSheet(
    account: OtpAccount,
    onDismiss: () -> Unit,
    onRename: () -> Unit,
    onShowBarcode: () -> Unit,
    onShowSecret: () -> Unit,
    onDelete: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Text(
            text = account.label,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
        )
        ListItem(
            headlineContent = { Text(stringResource(R.string.rename)) },
            leadingContent = {
                Icon(Icons.Outlined.DriveFileRenameOutline, contentDescription = null)
            },
            modifier = Modifier.clickable(onClick = onRename),
        )
        if (!account.isBarcode) {
            ListItem(
                headlineContent = { Text(stringResource(R.string.show_barcode)) },
                leadingContent = { Icon(Icons.Outlined.QrCode2, contentDescription = null) },
                modifier = Modifier.clickable(onClick = onShowBarcode),
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.show_secret)) },
                leadingContent = { Icon(Icons.Outlined.Visibility, contentDescription = null) },
                modifier = Modifier.clickable(onClick = onShowSecret),
            )
        }
        ListItem(
            headlineContent = { Text(stringResource(R.string.delete)) },
            leadingContent = { Icon(Icons.Outlined.Delete, contentDescription = null) },
            modifier = Modifier.clickable(onClick = onDelete),
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun RenameDialog(
    account: OtpAccount,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var text by remember { mutableStateOf(account.label) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(
                    if (account.isBarcode) R.string.rename_barcode else R.string.rename_otp,
                ),
            )
        },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.label_hint)) },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(text) },
                enabled = text.isNotBlank(),
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

@Composable
private fun BarcodeDialog(
    account: OtpAccount,
    onDismiss: () -> Unit,
) {
    val uri = remember(account) { account.toOtpAuthUri() }
    val bitmap: Bitmap = remember(uri) { QrBitmaps.encode(uri) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.otp_barcode_title)) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = account.label,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = stringResource(R.string.show_barcode),
                    modifier = Modifier.size(240.dp),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) }
        },
    )
}

@Composable
private fun SecretDialog(
    account: OtpAccount,
    onDismiss: () -> Unit,
    onCopy: (String) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.otp_secret_title)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(
                    text = account.label,
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = account.secret,
                    style = MaterialTheme.typography.bodyLarge,
                    fontFamily = FontFamily.Monospace,
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.otp_uri_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = account.toOtpAuthUri(),
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onCopy(account.secret) }) {
                Text(stringResource(R.string.copy_secret))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) }
        },
    )
}

@Composable
private fun ConfirmDeleteDialog(
    account: OtpAccount,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(
                    if (account.isBarcode) R.string.delete_barcode_title else R.string.delete_otp_title,
                ),
            )
        },
        text = { Text(stringResource(R.string.delete_otp_message, account.label)) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(stringResource(R.string.delete)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}
