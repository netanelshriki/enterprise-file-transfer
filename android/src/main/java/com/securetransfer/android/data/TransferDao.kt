package com.securetransfer.android.data

import androidx.lifecycle.LiveData
import androidx.room.*
import com.securetransfer.android.model.Transfer

@Dao
interface TransferDao {
    
    @Query("SELECT * FROM transfers ORDER BY timestamp DESC LIMIT 20")
    fun getRecentTransfers(): LiveData<List<Transfer>>
    
    @Query("SELECT * FROM transfers WHERE status = 'IN_PROGRESS'")
    fun getActiveTransfers(): LiveData<List<Transfer>>
    
    @Query("SELECT * FROM transfers WHERE id = :transferId")
    suspend fun getTransferById(transferId: String): Transfer?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransfer(transfer: Transfer)
    
    @Update
    suspend fun updateTransfer(transfer: Transfer)
    
    @Delete
    suspend fun deleteTransfer(transfer: Transfer)
    
    @Query("DELETE FROM transfers WHERE timestamp < :cutoffTime")
    suspend fun deleteOldTransfers(cutoffTime: Long)
    
    @Query("SELECT COUNT(*) FROM transfers WHERE status = 'COMPLETED'")
    suspend fun getCompletedTransferCount(): Int
    
    @Query("SELECT SUM(fileSize) FROM transfers WHERE status = 'COMPLETED'")
    suspend fun getTotalTransferredBytes(): Long?
}
