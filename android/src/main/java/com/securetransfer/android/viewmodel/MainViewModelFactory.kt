package com.securetransfer.android.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.securetransfer.android.repository.DeviceRepository
import com.securetransfer.android.repository.TransferRepository

class MainViewModelFactory(
    private val deviceRepository: DeviceRepository,
    private val transferRepository: TransferRepository
) : ViewModelProvider.Factory {
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            return MainViewModel(deviceRepository, transferRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
