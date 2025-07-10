package com.enterprise.filetransfer.crypto;

import android.util.Log;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class CryptoManager {
    private static final String TAG = "CryptoManager";
    private static final String AES_TRANSFORMATION = "AES/GCM/NoPadding";
    private static final String RSA_TRANSFORMATION = "RSA/ECB/OAEPWITHSHA-256ANDMGF1PADDING";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 16;

    private KeyPair rsaKeyPair;
    private SecretKey aesKey;
    private SecureRandom secureRandom;

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    public CryptoManager() {
        secureRandom = new SecureRandom();
    }

    public void generateKeys() throws Exception {
        KeyPairGenerator rsaGenerator = KeyPairGenerator.getInstance("RSA");
        rsaGenerator.initialize(4096, secureRandom);
        rsaKeyPair = rsaGenerator.generateKeyPair();

        KeyGenerator aesGenerator = KeyGenerator.getInstance("AES");
        aesGenerator.init(256, secureRandom);
        aesKey = aesGenerator.generateKey();

        Log.d(TAG, "Crypto keys generated successfully");
    }

    public byte[] encrypt(byte[] data, int offset, int length) throws Exception {
        if (aesKey == null) {
            throw new IllegalStateException("AES key not generated");
        }

        Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
        
        byte[] iv = new byte[GCM_IV_LENGTH];
        secureRandom.nextBytes(iv);
        
        GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
        cipher.init(Cipher.ENCRYPT_MODE, aesKey, gcmSpec);

        byte[] dataToEncrypt = new byte[length];
        System.arraycopy(data, offset, dataToEncrypt, 0, length);
        
        byte[] encryptedData = cipher.doFinal(dataToEncrypt);
        
        byte[] result = new byte[iv.length + encryptedData.length];
        System.arraycopy(iv, 0, result, 0, iv.length);
        System.arraycopy(encryptedData, 0, result, iv.length, encryptedData.length);
        
        return result;
    }

    public byte[] decrypt(byte[] encryptedData) throws Exception {
        if (aesKey == null) {
            throw new IllegalStateException("AES key not generated");
        }

        if (encryptedData.length < GCM_IV_LENGTH) {
            throw new IllegalArgumentException("Invalid encrypted data length");
        }

        byte[] iv = new byte[GCM_IV_LENGTH];
        System.arraycopy(encryptedData, 0, iv, 0, GCM_IV_LENGTH);
        
        byte[] cipherText = new byte[encryptedData.length - GCM_IV_LENGTH];
        System.arraycopy(encryptedData, GCM_IV_LENGTH, cipherText, 0, cipherText.length);

        Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
        GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
        cipher.init(Cipher.DECRYPT_MODE, aesKey, gcmSpec);

        return cipher.doFinal(cipherText);
    }

    public byte[] encryptAESKey(PublicKey publicKey) throws Exception {
        if (aesKey == null) {
            throw new IllegalStateException("AES key not generated");
        }

        Cipher cipher = Cipher.getInstance(RSA_TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        return cipher.doFinal(aesKey.getEncoded());
    }

    public void setAESKeyFromEncrypted(byte[] encryptedAESKey) throws Exception {
        if (rsaKeyPair == null) {
            throw new IllegalStateException("RSA key pair not generated");
        }

        Cipher cipher = Cipher.getInstance(RSA_TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, rsaKeyPair.getPrivate());
        byte[] aesKeyBytes = cipher.doFinal(encryptedAESKey);
        
        aesKey = new SecretKeySpec(aesKeyBytes, "AES");
    }

    public PublicKey getPublicKey() {
        return rsaKeyPair != null ? rsaKeyPair.getPublic() : null;
    }

    public PrivateKey getPrivateKey() {
        return rsaKeyPair != null ? rsaKeyPair.getPrivate() : null;
    }

    public byte[] getPublicKeyEncoded() {
        return rsaKeyPair != null ? rsaKeyPair.getPublic().getEncoded() : null;
    }
}
