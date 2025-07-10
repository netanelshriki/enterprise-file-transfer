use anyhow::{Result, anyhow};
use aes_gcm::{Aes256Gcm, Key, Nonce, aead::{Aead, KeyInit, AeadCore}};
use rsa::{RsaPrivateKey, RsaPublicKey, Oaep, RsaPublicKey as PublicKeyTrait};
use rand::rngs::OsRng;
use sha2::{Sha256, Digest};
use serde::{Serialize, Deserialize};

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct EncryptedData {
    pub data: Vec<u8>,
    pub nonce: Vec<u8>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct KeyExchangePacket {
    pub public_key: Vec<u8>,
    pub encrypted_aes_key: Vec<u8>,
}

pub struct CryptoManager {
    rsa_private_key: RsaPrivateKey,
    rsa_public_key: RsaPublicKey,
    aes_key: Option<Key<Aes256Gcm>>,
}

impl CryptoManager {
    pub fn new() -> Result<Self> {
        let mut rng = OsRng;
        let bits = 4096;
        let rsa_private_key = RsaPrivateKey::new(&mut rng, bits)?;
        let rsa_public_key = RsaPublicKey::from(&rsa_private_key);

        Ok(CryptoManager {
            rsa_private_key,
            rsa_public_key,
            aes_key: None,
        })
    }

    pub fn generate_aes_key(&mut self) -> Result<()> {
        let key = Aes256Gcm::generate_key(&mut OsRng);
        self.aes_key = Some(key);
        Ok(())
    }

    pub fn get_public_key_der(&self) -> Result<Vec<u8>> {
        use rsa::pkcs8::EncodePublicKey;
        let der = self.rsa_public_key.to_public_key_der()?;
        Ok(der.as_bytes().to_vec())
    }

    pub fn create_key_exchange_packet(&self, peer_public_key_der: &[u8]) -> Result<KeyExchangePacket> {
        use rsa::pkcs8::DecodePublicKey;
        
        let aes_key = self.aes_key.as_ref()
            .ok_or_else(|| anyhow!("AES key not generated"))?;

        let peer_public_key = RsaPublicKey::from_public_key_der(peer_public_key_der)?;
        let mut rng = OsRng;
        
        let padding = Oaep::new::<Sha256>();
        let encrypted_aes_key = peer_public_key.encrypt(&mut rng, padding, aes_key.as_slice())?;

        Ok(KeyExchangePacket {
            public_key: self.get_public_key_der()?,
            encrypted_aes_key,
        })
    }

    pub fn process_key_exchange_packet(&mut self, packet: &KeyExchangePacket) -> Result<()> {
        let padding = Oaep::new::<Sha256>();
        let decrypted_aes_key = self.rsa_private_key.decrypt(padding, &packet.encrypted_aes_key)?;

        if decrypted_aes_key.len() != 32 {
            return Err(anyhow!("Invalid AES key length"));
        }

        let key = Key::<Aes256Gcm>::from_slice(&decrypted_aes_key);
        self.aes_key = Some(*key);
        Ok(())
    }

    pub fn encrypt_data(&self, data: &[u8]) -> Result<EncryptedData> {
        let aes_key = self.aes_key.as_ref()
            .ok_or_else(|| anyhow!("AES key not set"))?;

        let cipher = Aes256Gcm::new(aes_key);
        let nonce = Aes256Gcm::generate_nonce(&mut OsRng);
        
        let encrypted_data = cipher.encrypt(&nonce, data)
            .map_err(|e| anyhow!("Encryption failed: {}", e))?;

        Ok(EncryptedData {
            data: encrypted_data,
            nonce: nonce.to_vec(),
        })
    }

    pub fn decrypt_data(&self, encrypted_data: &EncryptedData) -> Result<Vec<u8>> {
        let aes_key = self.aes_key.as_ref()
            .ok_or_else(|| anyhow!("AES key not set"))?;

        let cipher = Aes256Gcm::new(aes_key);
        let nonce = Nonce::from_slice(&encrypted_data.nonce);
        
        let decrypted_data = cipher.decrypt(nonce, encrypted_data.data.as_ref())
            .map_err(|e| anyhow!("Decryption failed: {}", e))?;

        Ok(decrypted_data)
    }

    pub fn hash_file(&self, data: &[u8]) -> Vec<u8> {
        let mut hasher = Sha256::new();
        hasher.update(data);
        hasher.finalize().to_vec()
    }

    pub fn verify_file_integrity(&self, data: &[u8], expected_hash: &[u8]) -> bool {
        let actual_hash = self.hash_file(data);
        actual_hash == expected_hash
    }
}
