package com.securetransfer.android.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.securetransfer.android.R
import com.securetransfer.android.SecureTransferApplication
import com.securetransfer.android.ui.component.DeviceCard
import com.securetransfer.android.ui.theme.SecureTransferTheme
import com.securetransfer.android.viewmodel.ShareViewModel
import com.securetransfer.android.viewmodel.ShareViewModelFactory
import org.slf4j.LoggerFactory

class ShareActivity : ComponentActivity() {
    
    private val logger = LoggerFactory.getLogger(ShareActivity::class.java)
    
    private val shareViewModel: ShareViewModel by viewModels {
        ShareViewModelFactory(
            (application as SecureTransferApplication).deviceRepository,
            (application as SecureTransferApplication).transferRepository
        )
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        logger.info("ShareActivity created")
        
        handleShareIntent(intent)
        
        setContent {
            SecureTransferTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ShareScreen(
                        viewModel = shareViewModel,
                        onClose = { finish() },
                        onDeviceSelected = { device ->
                            shareViewModel.sendToDevice(device)
                            finish()
                        }
                    )
                }
            }
        }
    }
    
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let { handleShareIntent(it) }
    }
    
    private fun handleShareIntent(intent: Intent) {
        when (intent.action) {
            Intent.ACTION_SEND -> {
                val uri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
                val text = intent.getStringExtra(Intent.EXTRA_TEXT)
                
                when {
                    uri != null -> {
                        logger.info("Sharing file: $uri")
                        shareViewModel.setSharedFile(uri)
                    }
                    text != null -> {
                        logger.info("Sharing text: ${text.take(50)}...")
                        shareViewModel.setSharedText(text)
                    }
                }
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                val uris = intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)
                uris?.let {
                    logger.info("Sharing multiple files: ${it.size} files")
                    shareViewModel.setSharedFiles(it)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareScreen(
    viewModel: ShareViewModel,
    onClose: () -> Unit,
    onDeviceSelected: (com.securetransfer.android.model.Device) -> Unit
) {
    val uiState by viewModel.uiState.observeAsState()
    val devices by viewModel.availableDevices.observeAsState(emptyList())
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        TopAppBar(
            title = { Text(stringResource(R.string.share_files)) },
            navigationIcon = {
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
        )
        
        uiState?.let { state ->
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    ShareInfoCard(
                        fileCount = state.fileCount,
                        totalSize = state.totalSize,
                        hasText = state.hasText
                    )
                }
                
                item {
                    Text(
                        text = stringResource(R.string.select_device),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                
                if (devices.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator()
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = stringResource(R.string.searching_devices),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                } else {
                    items(devices) { device ->
                        DeviceCard(
                            device = device,
                            onClick = { onDeviceSelected(device) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ShareInfoCard(
    fileCount: Int,
    totalSize: Long,
    hasText: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Share,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column {
                when {
                    hasText -> {
                        Text(
                            text = stringResource(R.string.sharing_text),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    fileCount == 1 -> {
                        Text(
                            text = stringResource(R.string.sharing_file),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    fileCount > 1 -> {
                        Text(
                            text = stringResource(R.string.sharing_files, fileCount),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
                
                if (totalSize > 0) {
                    Text(
                        text = formatFileSize(totalSize),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
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
