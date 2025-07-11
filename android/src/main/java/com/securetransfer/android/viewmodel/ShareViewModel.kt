package com.securetransfer.android.viewmodel

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.securetransfer.android.model.Device
import com.securetransfer.android.repository.DeviceRepository
import com.securetransfer.android.repository.TransferRepository
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory

class ShareViewModel(
    private val deviceRepository: DeviceRepository,
    private val transferRepository: TransferRepository
) : ViewModel() {
    
    private val logger = LoggerFactory.getLogger(ShareViewModel::class.java)
    
    private val _uiState = MutableLiveData<ShareUiState>()
    val uiState: LiveData<ShareUiState> = _uiState
    
    val availableDevices: LiveData<List<Device>> = deviceRepository.getOnlineDevices()
    
    init {
        _uiState.value = ShareUiState()
    }
    
    fun setSharedFile(uri: Uri) {
        logger.info("Setting shared file: $uri")
        _uiState.value = _uiState.value?.copy(
            sharedUris = listOf(uri),
            fileCount = 1,
            hasText = false
        )
    }
    
    fun setSharedFiles(uris: List<Uri>) {
        logger.info("Setting shared files: ${uris.size} files")
        _uiState.value = _uiState.value?.copy(
            sharedUris = uris,
            fileCount = uris.size,
            hasText = false
        )
    }
    
    fun setSharedText(text: String) {
        logger.info("Setting shared text")
        _uiState.value = _uiState.value?.copy(
            sharedText = text,
            hasText = true,
            fileCount = 0
        )
    }
    
    fun sendToDevice(device: Device) {
        logger.info("Sending to device: ${device.name}")
        viewModelScope.launch {
            try {
                val currentState = _uiState.value ?: return@launch
                
                when {
                    currentState.hasText && currentState.sharedText != null -> {
                        logger.info("Sharing text to ${device.name}")
                        // TODO: Implement text sharing via FileTransferClient
                    }
                    currentState.sharedUris.isNotEmpty() -> {
                        logger.info("Sharing ${currentState.sharedUris.size} files to ${device.name}")
                        // TODO: Implement file sharing via FileTransferClient
                        for (uri in currentState.sharedUris) {
                            // transferClient.sendFile(device.ipAddress, device.port, uri, fileName)
                        }
                    }
                }
            } catch (e: Exception) {
                logger.error("Failed to send to device", e)
                _uiState.value = currentState.copy(
                    errorMessage = "Failed to send: ${e.message}"
                )
            }
        }
    }
    
    data class ShareUiState(
        val sharedUris: List<Uri> = emptyList(),
        val sharedText: String? = null,
        val fileCount: Int = 0,
        val totalSize: Long = 0,
        val hasText: Boolean = false,
        val isLoading: Boolean = false,
        val errorMessage: String? = null
    )
}
