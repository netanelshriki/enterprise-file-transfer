package com.securetransfer.android.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.securetransfer.android.model.Device
import com.securetransfer.android.model.Transfer
import com.securetransfer.android.repository.DeviceRepository
import com.securetransfer.android.repository.TransferRepository
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory

class MainViewModel(
    private val deviceRepository: DeviceRepository,
    private val transferRepository: TransferRepository
) : ViewModel() {
    
    private val logger = LoggerFactory.getLogger(MainViewModel::class.java)
    
    private val _uiState = MutableLiveData<UiState>()
    val uiState: LiveData<UiState> = _uiState
    
    val devices: LiveData<List<Device>> = deviceRepository.getAllDevices()
    val recentTransfers: LiveData<List<Transfer>> = transferRepository.getRecentTransfers()
    
    init {
        _uiState.value = UiState()
    }
    
    fun onPermissionsGranted() {
        logger.info("Permissions granted, starting device discovery")
        _uiState.value = _uiState.value?.copy(permissionState = PermissionState.GRANTED)
        startDeviceDiscovery()
    }
    
    fun onPermissionsDenied() {
        logger.warn("Permissions denied")
        _uiState.value = _uiState.value?.copy(permissionState = PermissionState.DENIED)
    }
    
    fun selectDevice(device: Device) {
        logger.info("Device selected: ${device.name}")
        _uiState.value = _uiState.value?.copy(selectedDevice = device)
    }
    
    fun startFileSelection() {
        logger.info("Starting file selection")
        _uiState.value = _uiState.value?.copy(showFileSelection = true)
    }
    
    fun showTransferDetails(transfer: Transfer) {
        logger.info("Showing transfer details for: ${transfer.fileName}")
        _uiState.value = _uiState.value?.copy(selectedTransfer = transfer)
    }
    
    private fun startDeviceDiscovery() {
        viewModelScope.launch {
            try {
                deviceRepository.startDiscovery()
            } catch (e: Exception) {
                logger.error("Failed to start device discovery", e)
            }
        }
    }
    
    data class UiState(
        val permissionState: PermissionState = PermissionState.UNKNOWN,
        val selectedDevice: Device? = null,
        val selectedTransfer: Transfer? = null,
        val showFileSelection: Boolean = false,
        val isLoading: Boolean = false,
        val errorMessage: String? = null
    )
    
    enum class PermissionState {
        UNKNOWN,
        GRANTED,
        DENIED
    }
}
