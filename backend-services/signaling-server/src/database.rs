use anyhow::Result;
use sqlx::{sqlite::SqlitePool, Row};
use uuid::Uuid;

use crate::models::Device;

#[derive(Clone)]
pub struct Database {
    pool: SqlitePool,
}

impl Database {
    pub async fn new(database_url: &str) -> Result<Self> {
        let connection_string = if database_url.starts_with("sqlite:") {
            database_url.to_string()
        } else {
            format!("sqlite:{}", database_url)
        };
        let pool = SqlitePool::connect(&connection_string).await?;
        Ok(Self { pool })
    }
    
    pub async fn migrate(&self) -> Result<()> {
        sqlx::query(
            r#"
            CREATE TABLE IF NOT EXISTS devices (
                id TEXT PRIMARY KEY,
                name TEXT NOT NULL,
                device_type TEXT NOT NULL,
                public_key TEXT NOT NULL,
                last_seen DATETIME NOT NULL
            )
            "#,
        )
        .execute(&self.pool)
        .await?;
        
        sqlx::query(
            r#"
            CREATE TABLE IF NOT EXISTS transfer_sessions (
                id TEXT PRIMARY KEY,
                from_device_id TEXT NOT NULL,
                to_device_id TEXT NOT NULL,
                file_name TEXT NOT NULL,
                file_size INTEGER NOT NULL,
                status TEXT NOT NULL,
                created_at DATETIME NOT NULL,
                completed_at DATETIME,
                FOREIGN KEY (from_device_id) REFERENCES devices (id),
                FOREIGN KEY (to_device_id) REFERENCES devices (id)
            )
            "#,
        )
        .execute(&self.pool)
        .await?;
        
        Ok(())
    }
    
    pub async fn store_device(&self, device: &Device) -> Result<()> {
        sqlx::query(
            r#"
            INSERT OR REPLACE INTO devices (id, name, device_type, public_key, last_seen)
            VALUES (?, ?, ?, ?, ?)
            "#,
        )
        .bind(device.id.to_string())
        .bind(&device.name)
        .bind(&device.device_type)
        .bind(&device.public_key)
        .bind(device.last_seen)
        .execute(&self.pool)
        .await?;
        
        Ok(())
    }
    
    pub async fn get_device(&self, device_id: Uuid) -> Result<Option<Device>> {
        let row = sqlx::query(
            "SELECT id, name, device_type, public_key, last_seen FROM devices WHERE id = ?"
        )
        .bind(device_id.to_string())
        .fetch_optional(&self.pool)
        .await?;
        
        if let Some(row) = row {
            Ok(Some(Device {
                id: Uuid::parse_str(&row.get::<String, _>("id"))?,
                name: row.get("name"),
                device_type: row.get("device_type"),
                public_key: row.get("public_key"),
                last_seen: row.get("last_seen"),
            }))
        } else {
            Ok(None)
        }
    }
    
    pub async fn list_recent_devices(&self, limit: i64) -> Result<Vec<Device>> {
        let rows = sqlx::query(
            "SELECT id, name, device_type, public_key, last_seen FROM devices ORDER BY last_seen DESC LIMIT ?"
        )
        .bind(limit)
        .fetch_all(&self.pool)
        .await?;
        
        let mut devices = Vec::new();
        for row in rows {
            devices.push(Device {
                id: Uuid::parse_str(&row.get::<String, _>("id"))?,
                name: row.get("name"),
                device_type: row.get("device_type"),
                public_key: row.get("public_key"),
                last_seen: row.get("last_seen"),
            });
        }
        
        Ok(devices)
    }
}
