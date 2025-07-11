package com.securetransfer.android.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.securetransfer.android.model.Transfer
import com.securetransfer.android.ui.theme.Error
import com.securetransfer.android.ui.theme.Success
import com.securetransfer.android.ui.theme.Warning
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun RecentTransferItem(
    transfer: Transfer,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = getStatusIcon(transfer.status),
                contentDescription = transfer.status.name,
                tint = getStatusColor(transfer.status),
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = transfer.fileName,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Text(
                    text = "to ${transfer.targetDeviceName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Text(
                    text = formatTimestamp(transfer.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = formatFileSize(transfer.fileSize),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                if (transfer.status == Transfer.Status.IN_PROGRESS) {
                    LinearProgressIndicator(
                        progress = transfer.progress,
                        modifier = Modifier
                            .width(60.dp)
                            .padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun getStatusIcon(status: Transfer.Status): ImageVector {
    return when (status) {
        Transfer.Status.COMPLETED -> Icons.Default.CheckCircle
        Transfer.Status.FAILED -> Icons.Default.Error
        Transfer.Status.IN_PROGRESS -> Icons.Default.Schedule
        Transfer.Status.PENDING -> Icons.Default.Schedule
    }
}

@Composable
private fun getStatusColor(status: Transfer.Status): androidx.compose.ui.graphics.Color {
    return when (status) {
        Transfer.Status.COMPLETED -> Success
        Transfer.Status.FAILED -> Error
        Transfer.Status.IN_PROGRESS -> Warning
        Transfer.Status.PENDING -> MaterialTheme.colorScheme.onSurfaceVariant
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val formatter = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
    return formatter.format(Date(timestamp))
}

private fun formatFileSize(bytes: Long): String {
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    var size = bytes.toDouble()
    var unitIndex = 0
    
    while (size >= 1024 && unitIndex < units.size - 1) {
        size /= 1024
        unitIndex++
    }
    
    return String.format("%.1f %s", size, units[unitIndex])
}
