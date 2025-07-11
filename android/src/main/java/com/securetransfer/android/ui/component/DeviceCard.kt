package com.securetransfer.android.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.securetransfer.android.model.Device
import com.securetransfer.android.model.DeviceType
import com.securetransfer.android.ui.theme.DeviceBusy
import com.securetransfer.android.ui.theme.DeviceOffline
import com.securetransfer.android.ui.theme.DeviceOnline

@Composable
fun DeviceCard(
    device: Device,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(48.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getDeviceIcon(device.type),
                    contentDescription = device.type.name,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .align(Alignment.BottomEnd),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        color = getStatusColor(device.status),
                        shape = CircleShape,
                        modifier = Modifier.size(8.dp)
                    ) {}
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = device.name,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = getStatusText(device.status),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (device.status == Device.Status.ONLINE) {
                FilledTonalButton(
                    onClick = onClick
                ) {
                    Text("Send")
                }
            }
        }
    }
}

@Composable
private fun getDeviceIcon(type: DeviceType): ImageVector {
    return when (type) {
        DeviceType.DESKTOP -> Icons.Default.Computer
        DeviceType.MOBILE -> Icons.Default.Phone
        DeviceType.UNKNOWN -> Icons.Default.Computer
    }
}

@Composable
private fun getStatusColor(status: Device.Status): androidx.compose.ui.graphics.Color {
    return when (status) {
        Device.Status.ONLINE -> DeviceOnline
        Device.Status.BUSY -> DeviceBusy
        Device.Status.OFFLINE -> DeviceOffline
    }
}

private fun getStatusText(status: Device.Status): String {
    return when (status) {
        Device.Status.ONLINE -> "Available"
        Device.Status.BUSY -> "Busy"
        Device.Status.OFFLINE -> "Offline"
    }
}
