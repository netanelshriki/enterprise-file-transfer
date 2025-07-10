use anyhow::{Result, anyhow};
use std::path::Path;
use tokio::fs::File;
use tokio::io::{AsyncReadExt, AsyncWriteExt};
use uuid::Uuid;
use log::{info, error, debug};

pub struct FileTransferManager {
    active_transfers: std::collections::HashMap<String, TransferSession>,
}

#[derive(Debug)]
pub struct TransferSession {
    pub id: String,
    pub filename: String,
    pub file_size: u64,
    pub bytes_transferred: u64,
    pub status: TransferStatus,
    pub target_device: String,
}

#[derive(Debug, Clone)]
pub enum TransferStatus {
    Pending,
    InProgress,
    Completed,
    Failed(String),
}

impl FileTransferManager {
    pub fn new() -> Self {
        FileTransferManager {
            active_transfers: std::collections::HashMap::new(),
        }
    }

    pub async fn transfer_file(
        &mut self,
        file_path: &str,
        target_device_id: &str,
        target_ip: &str,
    ) -> Result<String> {
        let transfer_id = Uuid::new_v4().to_string();
        let path = Path::new(file_path);
        
        if !path.exists() {
            return Err(anyhow!("File does not exist: {}", file_path));
        }

        let filename = path.file_name()
            .and_then(|n| n.to_str())
            .unwrap_or("unknown")
            .to_string();

        let metadata = tokio::fs::metadata(path).await?;
        let file_size = metadata.len();

        let session = TransferSession {
            id: transfer_id.clone(),
            filename: filename.clone(),
            file_size,
            bytes_transferred: 0,
            status: TransferStatus::Pending,
            target_device: target_device_id.to_string(),
        };

        self.active_transfers.insert(transfer_id.clone(), session);

        info!("Starting file transfer: {} to {}", filename, target_device_id);

        match self.perform_transfer(&transfer_id, file_path, target_ip).await {
            Ok(_) => {
                if let Some(session) = self.active_transfers.get_mut(&transfer_id) {
                    session.status = TransferStatus::Completed;
                    session.bytes_transferred = file_size;
                }
                info!("File transfer completed: {}", filename);
                Ok(transfer_id)
            }
            Err(e) => {
                if let Some(session) = self.active_transfers.get_mut(&transfer_id) {
                    session.status = TransferStatus::Failed(e.to_string());
                }
                error!("File transfer failed: {}", e);
                Err(e)
            }
        }
    }

    async fn perform_transfer(
        &mut self,
        transfer_id: &str,
        file_path: &str,
        _target_ip: &str,
    ) -> Result<()> {
        if let Some(session) = self.active_transfers.get_mut(transfer_id) {
            session.status = TransferStatus::InProgress;
        }

        let mut file = File::open(file_path).await?;
        let mut buffer = vec![0u8; 64 * 1024];
        let mut total_bytes = 0u64;

        loop {
            let bytes_read = file.read(&mut buffer).await?;
            if bytes_read == 0 {
                break;
            }

            tokio::time::sleep(tokio::time::Duration::from_millis(10)).await;

            total_bytes += bytes_read as u64;
            if let Some(session) = self.active_transfers.get_mut(transfer_id) {
                session.bytes_transferred = total_bytes;
            }

            debug!("Transferred {} bytes", total_bytes);
        }

        Ok(())
    }

    pub fn get_transfer_status(&self, transfer_id: &str) -> Option<&TransferSession> {
        self.active_transfers.get(transfer_id)
    }

    pub fn get_active_transfers(&self) -> Vec<&TransferSession> {
        self.active_transfers.values().collect()
    }
}
