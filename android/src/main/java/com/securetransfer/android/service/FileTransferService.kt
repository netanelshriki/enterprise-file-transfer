package com.securetransfer.android.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.securetransfer.android.R
import com.securetransfer.android.SecureTransferApplication
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory

class FileTransferService : Service() {
    
    private val logger = LoggerFactory.getLogger(FileTransferService::class.java)
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    companion object {
        private const val NOTIFICATION_ID = 1002
    }
    
    override fun onCreate() {
        super.onCreate()
        logger.info("FileTransferService created")
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        logger.info("FileTransferService started")
        startForeground(NOTIFICATION_ID, createNotification())
        return START_NOT_STICKY
    }
    
    override fun onDestroy() {
        super.onDestroy()
        logger.info("FileTransferService destroyed")
        serviceScope.cancel()
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    private fun createNotification() = NotificationCompat.Builder(
        this,
        SecureTransferApplication.TRANSFER_NOTIFICATION_CHANNEL_ID
    )
        .setContentTitle(getString(R.string.app_name))
        .setContentText(getString(R.string.transferring_files))
        .setSmallIcon(R.drawable.ic_notification)
        .setOngoing(true)
        .setProgress(100, 0, false)
        .build()
}
