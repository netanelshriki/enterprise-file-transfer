package com.securetransfer.android.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "devices")
data class Device(
    @PrimaryKey
    val id: String,
    val name: String,
    val type: DeviceType,
    val ipAddress: String,
    val port: Int,
    val status: Status,
    val lastSeen: Long,
    val publicKey: String? = null
) {
    enum class Status {
        ONLINE,
        OFFLINE,
        BUSY
    }
}

enum class DeviceType {
    DESKTOP,
    MOBILE,
    UNKNOWN
}
