package com.securetransfer.android.repository

import androidx.lifecycle.LiveData
import com.securetransfer.android.data.TransferDao
import com.securetransfer.android.model.Transfer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory

class TransferRepository(private val transferDao: TransferDao) {
    
    private val logger = LoggerFactory.getLogger(TransferRepository::class.java)
    
    fun getRecentTransfers(): LiveData<List<Transfer>> = transferDao.getRecentTransfers()
    
    fun getActiveTransfers(): LiveData<List<Transfer>> = transferDao.getActiveTransfers()
    
    suspend fun getTransferById(transferId: String): Transfer? {
        return withContext(Dispatchers.IO) {
            transferDao.getTransferById(transferId)
        }
    }
    
    suspend fun insertTransfer(transfer: Transfer) {
        withContext(Dispatchers.IO) {
            logger.debug("Inserting transfer: ${transfer.fileName}")
            transferDao.insertTransfer(transfer)
        }
    }
    
    suspend fun updateTransfer(transfer: Transfer) {
        withContext(Dispatchers.IO) {
            logger.debug("Updating transfer: ${transfer.fileName}")
            transferDao.updateTransfer(transfer)
        }
    }
    
    suspend fun deleteTransfer(transfer: Transfer) {
        withContext(Dispatchers.IO) {
            logger.debug("Deleting transfer: ${transfer.fileName}")
            transferDao.deleteTransfer(transfer)
        }
    }
    
    suspend fun getTransferStats(): TransferStats {
        return withContext(Dispatchers.IO) {
            val completedCount = transferDao.getCompletedTransferCount()
            val totalBytes = transferDao.getTotalTransferredBytes() ?: 0L
            TransferStats(completedCount, totalBytes)
        }
    }
    
    suspend fun cleanupOldTransfers() {
        withContext(Dispatchers.IO) {
            val cutoffTime = System.currentTimeMillis() - (30 * 24 * 60 * 60 * 1000L) // 30 days
            transferDao.deleteOldTransfers(cutoffTime)
        }
    }
    
    data class TransferStats(
        val completedTransfers: Int,
        val totalBytesTransferred: Long
    )
}
