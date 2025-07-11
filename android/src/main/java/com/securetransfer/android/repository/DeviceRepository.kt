package com.securetransfer.android.repository

import androidx.lifecycle.LiveData
import com.securetransfer.android.data.DeviceDao
import com.securetransfer.android.model.Device
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory

class DeviceRepository(private val deviceDao: DeviceDao) {
    
    private val logger = LoggerFactory.getLogger(DeviceRepository::class.java)
    
    fun getAllDevices(): LiveData<List<Device>> = deviceDao.getAllDevices()
    
    fun getOnlineDevices(): LiveData<List<Device>> = deviceDao.getOnlineDevices()
    
    suspend fun getDeviceById(deviceId: String): Device? {
        return withContext(Dispatchers.IO) {
            deviceDao.getDeviceById(deviceId)
        }
    }
    
    suspend fun insertDevice(device: Device) {
        withContext(Dispatchers.IO) {
            logger.debug("Inserting device: ${device.name}")
            deviceDao.insertDevice(device)
        }
    }
    
    suspend fun updateDevice(device: Device) {
        withContext(Dispatchers.IO) {
            logger.debug("Updating device: ${device.name}")
            deviceDao.updateDevice(device)
        }
    }
    
    suspend fun deleteDevice(device: Device) {
        withContext(Dispatchers.IO) {
            logger.debug("Deleting device: ${device.name}")
            deviceDao.deleteDevice(device)
        }
    }
    
    suspend fun startDiscovery() {
        withContext(Dispatchers.IO) {
            logger.info("Starting device discovery")
            // TODO: Implement mDNS discovery
        }
    }
    
    suspend fun cleanupOldDevices() {
        withContext(Dispatchers.IO) {
            val cutoffTime = System.currentTimeMillis() - (5 * 60 * 1000) // 5 minutes
            deviceDao.markOldDevicesOffline(cutoffTime)
            
            val oldCutoffTime = System.currentTimeMillis() - (24 * 60 * 60 * 1000) // 24 hours
            deviceDao.deleteOldDevices(oldCutoffTime)
        }
    }
}
