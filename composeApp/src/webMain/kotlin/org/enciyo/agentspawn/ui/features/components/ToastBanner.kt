package org.enciyo.agentspawn.ui.features.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

enum class ToastType {
    Success,
    Error,
    Warning,
    Info,
}

@Composable
fun ToastBanner(
    message: String,
    type: ToastType,
    modifier: Modifier = Modifier,
) {
    val (icon, backgroundColor) = when (type) {
        ToastType.Success -> Icons.Default.CheckCircle to Color(0xFF2E7D32)
        ToastType.Error -> Icons.Default.Error to MaterialTheme.colorScheme.error
        ToastType.Warning -> Icons.Default.Warning to Color(0xFFED6C02)
        ToastType.Info -> Icons.Default.Info to MaterialTheme.colorScheme.primary
    }

    Surface(
        modifier = modifier,
        color = backgroundColor,
        contentColor = Color.White,
        shape = MaterialTheme.shapes.medium,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
