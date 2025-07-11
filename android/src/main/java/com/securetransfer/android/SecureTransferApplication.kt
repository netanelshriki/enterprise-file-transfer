package com.securetransfer.android

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.work.Configuration
import androidx.work.WorkManager
import com.securetransfer.android.data.AppDatabase
import com.securetransfer.android.repository.DeviceRepository
import com.securetransfer.android.repository.TransferRepository
import com.securetransfer.android.service.DeviceDiscoveryService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.slf4j.LoggerFactory

class SecureTransferApplication : Application(), Configuration.Provider {
    
    private val logger = LoggerFactory.getLogger(SecureTransferApplication::class.java)
    
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    val database by lazy { AppDatabase.getDatabase(this, applicationScope) }
    val deviceRepository by lazy { DeviceRepository(database.deviceDao()) }
    val transferRepository by lazy { TransferRepository(database.transferDao()) }
    
    companion object {
        const val TRANSFER_NOTIFICATION_CHANNEL_ID = "transfer_notifications"
        const val DISCOVERY_NOTIFICATION_CHANNEL_ID = "discovery_notifications"
    }
    
    override fun onCreate() {
        super.onCreate()
        logger.info("SecureTransfer Application starting")
        
        createNotificationChannels()
        initializeServices()
        
        logger.info("SecureTransfer Application initialized successfully")
    }
    
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            val transferChannel = NotificationChannel(
                TRANSFER_NOTIFICATION_CHANNEL_ID,
                "File Transfers",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notifications for ongoing file transfers"
                setShowBadge(false)
            }
            
            val discoveryChannel = NotificationChannel(
                DISCOVERY_NOTIFICATION_CHANNEL_ID,
                "Device Discovery",
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = "Background device discovery service"
                setShowBadge(false)
            }
            
            notificationManager.createNotificationChannel(transferChannel)
            notificationManager.createNotificationChannel(discoveryChannel)
        }
    }
    
    private fun initializeServices() {
        DeviceDiscoveryService.startService(this)
    }
    
    override fun getWorkManagerConfiguration(): Configuration {
        return Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()
    }
}
