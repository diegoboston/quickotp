package com.quickotp.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.quickotp.app.R
import com.quickotp.app.barcode.BarcodeBitmaps
import com.quickotp.app.data.OtpAccount

@Composable
fun StoredBarcodeRow(
    account: OtpAccount,
    onCopyValue: (String) -> Unit,
    onToggleValue: () -> Unit,
    onEdit: () -> Unit,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
) {
    val valueVisible = account.valueVisible
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
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
                            imageVector = if (valueVisible) {
                                Icons.Filled.KeyboardArrowDown
                            } else {
                                Icons.Filled.KeyboardArrowUp
                            },
                            contentDescription = stringResource(
                                if (valueVisible) R.string.show_barcode else R.string.hide_barcode,
                            ),
                        )
                    }
                }
                ReorderArrows(
                    canMoveUp = canMoveUp,
                    canMoveDown = canMoveDown,
                    onMoveUp = onMoveUp,
                    onMoveDown = onMoveDown,
                )
                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = stringResource(R.string.edit_barcode),
                    )
                }
            }
            if (!valueVisible) {
                val rendered = remember(account.secret, account.barcodeFormat) {
                    runCatching {
                        BarcodeBitmaps.render(account.secret, account.barcodeFormat)
                    }.getOrNull()
                }
                if (rendered != null) {
                    val image = remember(rendered) { rendered.bitmap.asImageBitmap() }
                    Image(
                        bitmap = image,
                        contentDescription = stringResource(R.string.barcode_for, account.label),
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .padding(bottom = 4.dp)
                            .fillMaxWidth()
                            .height(if (rendered.wide) 120.dp else 220.dp)
                            .background(Color.White)
                            .padding(8.dp),
                    )
                } else {
                    Text(
                        text = stringResource(R.string.barcode_render_failed),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                    StoredValue(account.secret, onCopyValue)
                }
            } else {
                StoredValue(account.secret, onCopyValue)
            }
        }
    }
}

@Composable
private fun StoredValue(
    value: String,
    onCopyValue: (String) -> Unit,
) {
    Text(
        text = value,
        style = MaterialTheme.typography.bodyLarge,
        fontFamily = FontFamily.Monospace,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCopyValue(value) },
    )
}
