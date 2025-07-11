package com.securetransfer.android.network

import android.content.Context
import android.content.SharedPreferences
import org.slf4j.LoggerFactory
import java.security.SecureRandom
import java.util.*
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object SecurityUtils {
    private val logger = LoggerFactory.getLogger(SecurityUtils::class.java)
    
    private const val ALGORITHM = "AES"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_IV_LENGTH = 12
    private const val GCM_TAG_LENGTH = 16
    private const val PREFS_NAME = "secure_transfer_prefs"
    private const val KEY_DEVICE_ID = "device_id"
    private const val KEY_DEVICE_KEY = "device_key"
    
    private var deviceId: String? = null
    private var deviceKey: SecretKey? = null
    
    fun initialize(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        
        // Load or generate device ID
        deviceId = prefs.getString(KEY_DEVICE_ID, null)
        if (deviceId == null) {
            deviceId = UUID.randomUUID().toString()
            prefs.edit().putString(KEY_DEVICE_ID, deviceId).apply()
            logger.info("Generated new device ID")
        }
        
        // Load or generate device key
        val keyString = prefs.getString(KEY_DEVICE_KEY, null)
        if (keyString != null) {
            try {
                val keyBytes = Base64.getDecoder().decode(keyString)
                deviceKey = SecretKeySpec(keyBytes, ALGORITHM)
            } catch (e: Exception) {
                logger.error("Failed to load device key", e)
                deviceKey = null
            }
        }
        
        if (deviceKey == null) {
            deviceKey = generateSessionKey()
            val encodedKey = Base64.getEncoder().encodeToString(deviceKey!!.encoded)
            prefs.edit().putString(KEY_DEVICE_KEY, encodedKey).apply()
            logger.info("Generated new device key")
        }
    }
    
    fun getLocalDeviceId(): String {
        return deviceId ?: throw IllegalStateException("SecurityUtils not initialized")
    }
    
    fun generateDeviceId(): String {
        return UUID.randomUUID().toString()
    }
    
    fun generateSessionKey(): SecretKey {
        return try {
            val keyGen = KeyGenerator.getInstance(ALGORITHM)
            keyGen.init(256)
            keyGen.generateKey()
        } catch (e: Exception) {
            logger.error("Failed to generate session key", e)
            throw RuntimeException("Failed to generate session key", e)
        }
    }
    
    fun encrypt(data: ByteArray, key: SecretKey): ByteArray {
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            
            val iv = ByteArray(GCM_IV_LENGTH)
            SecureRandom().nextBytes(iv)
            
            val parameterSpec = GCMParameterSpec(GCM_TAG_LENGTH * 8, iv)
            cipher.init(Cipher.ENCRYPT_MODE, key, parameterSpec)
            
            val encryptedData = cipher.doFinal(data)
            
            // Combine IV and encrypted data
            val result = ByteArray(GCM_IV_LENGTH + encryptedData.size)
            System.arraycopy(iv, 0, result, 0, GCM_IV_LENGTH)
            System.arraycopy(encryptedData, 0, result, GCM_IV_LENGTH, encryptedData.size)
            
            result
        } catch (e: Exception) {
            logger.error("Encryption failed", e)
            throw RuntimeException("Encryption failed", e)
        }
    }
    
    fun decrypt(encryptedData: ByteArray, key: SecretKey): ByteArray {
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            
            // Extract IV
            val iv = ByteArray(GCM_IV_LENGTH)
            System.arraycopy(encryptedData, 0, iv, 0, GCM_IV_LENGTH)
            
            val parameterSpec = GCMParameterSpec(GCM_TAG_LENGTH * 8, iv)
            cipher.init(Cipher.DECRYPT_MODE, key, parameterSpec)
            
            // Decrypt data (excluding IV)
            cipher.doFinal(encryptedData, GCM_IV_LENGTH, encryptedData.size - GCM_IV_LENGTH)
        } catch (e: Exception) {
            logger.error("Decryption failed", e)
            throw RuntimeException("Decryption failed", e)
        }
    }
}
