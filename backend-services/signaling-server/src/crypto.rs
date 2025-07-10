use anyhow::Result;
use aes_gcm::{
    aead::{Aead, KeyInit, OsRng},
    Aes256Gcm, Nonce,
};
use rand::RngCore;

pub struct CryptoManager {
    cipher: Aes256Gcm,
}

impl CryptoManager {
    pub fn new() -> Result<Self> {
        let cipher = Aes256Gcm::generate_key(OsRng);
        let cipher = Aes256Gcm::new(&cipher);
        
        Ok(Self { cipher })
    }
    
    pub fn encrypt(&self, data: &[u8]) -> Result<Vec<u8>> {
        let mut nonce_bytes = [0u8; 12];
        OsRng.fill_bytes(&mut nonce_bytes);
        let nonce = Nonce::from_slice(&nonce_bytes);
        
        let ciphertext = self.cipher.encrypt(nonce, data)
            .map_err(|_| anyhow::anyhow!("Encryption failed"))?;
        
        let mut result = Vec::with_capacity(12 + ciphertext.len());
        result.extend_from_slice(&nonce_bytes);
        result.extend_from_slice(&ciphertext);
        
        Ok(result)
    }
    
    pub fn decrypt(&self, encrypted_data: &[u8]) -> Result<Vec<u8>> {
        if encrypted_data.len() < 12 {
            return Err(anyhow::anyhow!("Encrypted data too short"));
        }
        
        let nonce_bytes = &encrypted_data[0..12];
        let ciphertext = &encrypted_data[12..];
        
        let nonce = Nonce::from_slice(nonce_bytes);
        
        let plaintext = self.cipher.decrypt(nonce, ciphertext)
            .map_err(|_| anyhow::anyhow!("Decryption failed"))?;
        
        Ok(plaintext)
    }
    
    pub fn get_public_key(&self) -> Result<Vec<u8>> {
        Ok(b"placeholder_public_key_for_demo".to_vec())
    }
    
    pub fn encrypt_aes_key_for_peer(&self, _peer_public_key: &[u8]) -> Result<Vec<u8>> {
        Ok(b"encrypted_aes_key_placeholder".to_vec())
    }
    
    pub fn decrypt_aes_key_from_peer(&self, _encrypted_key: &[u8]) -> Result<()> {
        Ok(())
    }
    
    pub fn sign_data(&self, _data: &[u8]) -> Result<Vec<u8>> {
        Ok(b"placeholder_signature".to_vec())
    }
    
    pub fn verify_signature(&self, _data: &[u8], _signature: &[u8], _public_key: &[u8]) -> Result<bool> {
        Ok(true)
    }
}

impl Default for CryptoManager {
    fn default() -> Self {
        Self::new().expect("Failed to create CryptoManager")
    }
}
