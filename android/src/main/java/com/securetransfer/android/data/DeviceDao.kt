package com.securetransfer.android.data

import androidx.lifecycle.LiveData
import androidx.room.*
import com.securetransfer.android.model.Device

@Dao
interface DeviceDao {
    
    @Query("SELECT * FROM devices ORDER BY lastSeen DESC")
    fun getAllDevices(): LiveData<List<Device>>
    
    @Query("SELECT * FROM devices WHERE status = 'ONLINE' ORDER BY lastSeen DESC")
    fun getOnlineDevices(): LiveData<List<Device>>
    
    @Query("SELECT * FROM devices WHERE id = :deviceId")
    suspend fun getDeviceById(deviceId: String): Device?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDevice(device: Device)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDevices(devices: List<Device>)
    
    @Update
    suspend fun updateDevice(device: Device)
    
    @Delete
    suspend fun deleteDevice(device: Device)
    
    @Query("DELETE FROM devices WHERE lastSeen < :cutoffTime")
    suspend fun deleteOldDevices(cutoffTime: Long)
    
    @Query("UPDATE devices SET status = 'OFFLINE' WHERE lastSeen < :cutoffTime")
    suspend fun markOldDevicesOffline(cutoffTime: Long)
}
