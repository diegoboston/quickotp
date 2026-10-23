package com.quickotp.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.quickotp.app.R
import com.quickotp.app.update.AppUpdateUiState

@Composable
fun AppUpdateInProgressBanner(
    state: AppUpdateUiState?,
    modifier: Modifier = Modifier,
) {
    when (state) {
        AppUpdateUiState.Checking -> AppUpdateDownloadProgress(progress = null, modifier = modifier)
        is AppUpdateUiState.Downloading -> AppUpdateDownloadProgress(
            progress = state.progress,
            modifier = modifier,
        )
        else -> Unit
    }
}

@Composable
private fun AppUpdateDownloadProgress(
    progress: Float?,
    modifier: Modifier = Modifier,
) {
    val percent = progress?.times(100f)?.toInt()?.coerceIn(0, 100)
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = if (percent != null) {
                stringResource(R.string.update_app_downloading_percent, percent)
            } else {
                stringResource(R.string.update_app_downloading)
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        if (percent != null && progress != null) {
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
    }
}
