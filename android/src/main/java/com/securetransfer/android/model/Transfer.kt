package com.securetransfer.android.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transfers")
data class Transfer(
    @PrimaryKey
    val id: String,
    val fileName: String,
    val filePath: String,
    val fileSize: Long,
    val targetDeviceId: String,
    val targetDeviceName: String,
    val status: Status,
    val progress: Float,
    val timestamp: Long,
    val speed: Long = 0,
    val errorMessage: String? = null
) {
    enum class Status {
        PENDING,
        IN_PROGRESS,
        COMPLETED,
        FAILED
    }
}
