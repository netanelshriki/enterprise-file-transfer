package com.securetransfer.desktop.network;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;

public class SecurityUtils {
    private static final Logger logger = LoggerFactory.getLogger(SecurityUtils.class);
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 16;
    
    private static String localDeviceId;
    private static SecretKey deviceKey;
    
    static {
        initializeDeviceIdentity();
    }
    
    private static void initializeDeviceIdentity() {
        try {
            Path configDir = Paths.get(System.getProperty("user.home"), ".securetransfer");
            Files.createDirectories(configDir);
            
            Path deviceIdFile = configDir.resolve("device.id");
            Path deviceKeyFile = configDir.resolve("device.key");
            
            if (Files.exists(deviceIdFile)) {
                localDeviceId = Files.readString(deviceIdFile).trim();
            } else {
                localDeviceId = UUID.randomUUID().toString();
                Files.writeString(deviceIdFile, localDeviceId);
                logger.info("Generated new device ID: {}", localDeviceId);
            }
            
            if (Files.exists(deviceKeyFile)) {
                byte[] keyBytes = Base64.getDecoder().decode(Files.readString(deviceKeyFile).trim());
                deviceKey = new SecretKeySpec(keyBytes, ALGORITHM);
            } else {
                KeyGenerator keyGen = KeyGenerator.getInstance(ALGORITHM);
                keyGen.init(256);
                deviceKey = keyGen.generateKey();
                String encodedKey = Base64.getEncoder().encodeToString(deviceKey.getEncoded());
                Files.writeString(deviceKeyFile, encodedKey);
                logger.info("Generated new device key");
            }
            
        } catch (Exception e) {
            logger.error("Failed to initialize device identity", e);
            throw new RuntimeException("Failed to initialize device identity", e);
        }
    }
    
    public static String getLocalDeviceId() {
        return localDeviceId;
    }
    
    public static String generateDeviceId() {
        return UUID.randomUUID().toString();
    }
    
    public static SecretKey generateSessionKey() {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance(ALGORITHM);
            keyGen.init(256);
            return keyGen.generateKey();
        } catch (Exception e) {
            logger.error("Failed to generate session key", e);
            throw new RuntimeException("Failed to generate session key", e);
        }
    }
    
    public static byte[] encrypt(byte[] data, SecretKey key) {
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);
            
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            cipher.init(Cipher.ENCRYPT_MODE, key, parameterSpec);
            
            byte[] encryptedData = cipher.doFinal(data);
            
            byte[] result = new byte[GCM_IV_LENGTH + encryptedData.length];
            System.arraycopy(iv, 0, result, 0, GCM_IV_LENGTH);
            System.arraycopy(encryptedData, 0, result, GCM_IV_LENGTH, encryptedData.length);
            
            return result;
        } catch (Exception e) {
            logger.error("Encryption failed", e);
            throw new RuntimeException("Encryption failed", e);
        }
    }
    
    public static byte[] decrypt(byte[] encryptedData, SecretKey key) {
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            
            byte[] iv = new byte[GCM_IV_LENGTH];
            System.arraycopy(encryptedData, 0, iv, 0, GCM_IV_LENGTH);
            
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            cipher.init(Cipher.DECRYPT_MODE, key, parameterSpec);
            
            return cipher.doFinal(encryptedData, GCM_IV_LENGTH, encryptedData.length - GCM_IV_LENGTH);
        } catch (Exception e) {
            logger.error("Decryption failed", e);
            throw new RuntimeException("Decryption failed", e);
        }
    }
}
