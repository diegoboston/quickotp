package com.quickotp.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.quickotp.app.R

/** Small stacked arrows that move an entry one place up or down the home list. */
@Composable
fun ReorderArrows(
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
) {
    Column {
        ReorderArrow(
            imageVector = Icons.Filled.ArrowUpward,
            contentDescription = stringResource(R.string.move_up),
            enabled = canMoveUp,
            onClick = onMoveUp,
        )
        ReorderArrow(
            imageVector = Icons.Filled.ArrowDownward,
            contentDescription = stringResource(R.string.move_down),
            enabled = canMoveDown,
            onClick = onMoveDown,
        )
    }
}

@Composable
private fun ReorderArrow(
    imageVector: ImageVector,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val tint = MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            modifier = Modifier.size(18.dp),
            tint = if (enabled) tint else tint.copy(alpha = 0.38f),
        )
    }
}
