package com.securetransfer.android.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.securetransfer.android.R
import com.securetransfer.android.ui.component.DeviceCard
import com.securetransfer.android.ui.component.PermissionRequestCard
import com.securetransfer.android.ui.component.RecentTransferItem
import com.securetransfer.android.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onRequestPermissions: () -> Unit
) {
    val uiState by viewModel.uiState.observeAsState()
    val devices by viewModel.devices.observeAsState(emptyList())
    val recentTransfers by viewModel.recentTransfers.observeAsState(emptyList())
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        TopAppBar(
            title = { Text(stringResource(R.string.app_name)) },
            actions = {
                IconButton(onClick = { /* TODO: Open settings */ }) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings")
                }
            }
        )
        
        when (uiState?.permissionState) {
            MainViewModel.PermissionState.DENIED -> {
                PermissionRequestCard(
                    onRequestPermissions = onRequestPermissions,
                    modifier = Modifier.padding(16.dp)
                )
            }
            MainViewModel.PermissionState.GRANTED -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        DevicesSection(
                            devices = devices,
                            onDeviceClick = { device ->
                                viewModel.selectDevice(device)
                            }
                        )
                    }
                    
                    item {
                        RecentTransfersSection(
                            transfers = recentTransfers,
                            onTransferClick = { transfer ->
                                viewModel.showTransferDetails(transfer)
                            }
                        )
                    }
                }
            }
            else -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
        
        uiState?.selectedDevice?.let { device ->
            FloatingActionButton(
                onClick = { viewModel.startFileSelection() },
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Send Files")
            }
        }
    }
}

@Composable
private fun DevicesSection(
    devices: List<com.securetransfer.android.model.Device>,
    onDeviceClick: (com.securetransfer.android.model.Device) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.nearby_devices),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            if (devices.isEmpty()) {
                Text(
                    text = stringResource(R.string.no_devices_found),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                devices.forEach { device ->
                    DeviceCard(
                        device = device,
                        onClick = { onDeviceClick(device) },
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun RecentTransfersSection(
    transfers: List<com.securetransfer.android.model.Transfer>,
    onTransferClick: (com.securetransfer.android.model.Transfer) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.recent_transfers),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            if (transfers.isEmpty()) {
                Text(
                    text = stringResource(R.string.no_recent_transfers),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                transfers.forEach { transfer ->
                    RecentTransferItem(
                        transfer = transfer,
                        onClick = { onTransferClick(transfer) },
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        }
    }
}
