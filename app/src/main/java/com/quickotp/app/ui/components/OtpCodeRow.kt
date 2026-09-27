package com.quickotp.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.quickotp.app.R
import com.quickotp.app.data.OtpAccount
import com.quickotp.app.otp.OtpCodes
import com.quickotp.app.otp.OtpPeriod
import com.quickotp.app.otp.OtpType

private val UrgentRed = Color(0xFFC62828)

@Composable
fun OtpCodeRow(
    account: OtpAccount,
    nowMillis: Long,
    onCopyCode: (String) -> Unit,
    onToggleValue: () -> Unit,
    onAdvanceHotp: () -> Unit,
    onEdit: () -> Unit,
) {
    val code = OtpCodes.currentCode(account, nowMillis)
    val formatted = OtpCodes.formatCode(code)
    val totp = account.otpType == OtpType.TOTP
    val urgent = totp && OtpPeriod.isUrgent(nowMillis, account.periodSeconds)
    val remainingSeconds = if (totp) {
        (OtpPeriod.remainingMillis(nowMillis, account.periodSeconds) / 1000L).toInt()
            .coerceAtLeast(1)
    } else {
        0
    }
    val progress = if (totp) OtpPeriod.progress(nowMillis, account.periodSeconds) else 0f
    val codeColor = if (urgent) UrgentRed else MaterialTheme.colorScheme.onSurface

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = account.label,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    IconButton(onClick = onToggleValue) {
                        Icon(
                            imageVector = if (account.valueVisible) {
                                Icons.Filled.KeyboardArrowDown
                            } else {
                                Icons.Filled.KeyboardArrowUp
                            },
                            contentDescription = stringResource(
                                if (account.valueVisible) R.string.hide_code else R.string.show_code,
                            ),
                        )
                    }
                }
                if (account.valueVisible) {
                    Text(
                        text = formatted,
                        style = MaterialTheme.typography.headlineMedium,
                        fontFamily = FontFamily.Monospace,
                        color = codeColor,
                        modifier = Modifier.clickable { onCopyCode(code) },
                    )
                }
            }
            if (account.valueVisible) {
                if (totp) {
                    val remainingDesc = stringResource(R.string.seconds_remaining, remainingSeconds)
                    CircularProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .size(40.dp)
                            .semantics { contentDescription = remainingDesc },
                        color = if (urgent) UrgentRed else MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        strokeWidth = 3.dp,
                    )
                } else {
                    IconButton(onClick = onAdvanceHotp) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = stringResource(R.string.next_hotp),
                        )
                    }
                }
                Spacer(Modifier.width(4.dp))
            }
            IconButton(onClick = onEdit) {
                Icon(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = stringResource(R.string.edit_otp),
                )
            }
        }
    }
}
