use anyhow::Result;
use serde_json::{json, Value};
use sqlx::{SqlitePool, Row};
use std::path::PathBuf;

pub struct Database {
    pool: SqlitePool,
}

impl Database {
    pub async fn new() -> Result<Self> {
        let db_path = Self::get_db_path()?;
        let database_url = format!("sqlite:{}", db_path.display());
        
        let pool = SqlitePool::connect(&database_url).await?;
        
        sqlx::migrate!("./migrations").run(&pool).await?;
        
        Ok(Database { pool })
    }

    pub async fn get_transfer_history(&self) -> Result<Vec<Value>> {
        let rows = sqlx::query("SELECT * FROM transfers ORDER BY created_at DESC LIMIT 50")
            .fetch_all(&self.pool)
            .await?;

        let mut transfers = Vec::new();
        for row in rows {
            transfers.push(json!({
                "id": row.get::<String, _>("id"),
                "filename": row.get::<String, _>("filename"),
                "file_size": row.get::<i64, _>("file_size"),
                "target_device": row.get::<String, _>("target_device"),
                "status": row.get::<String, _>("status"),
                "created_at": row.get::<String, _>("created_at"),
                "completed_at": row.get::<Option<String>, _>("completed_at"),
            }));
        }

        Ok(transfers)
    }

    pub async fn save_transfer_record(
        &self,
        id: &str,
        filename: &str,
        file_size: i64,
        target_device: &str,
        status: &str,
    ) -> Result<()> {
        sqlx::query(
            "INSERT INTO transfers (id, filename, file_size, target_device, status, created_at) 
             VALUES (?, ?, ?, ?, ?, datetime('now'))"
        )
        .bind(id)
        .bind(filename)
        .bind(file_size)
        .bind(target_device)
        .bind(status)
        .execute(&self.pool)
        .await?;

        Ok(())
    }

    pub async fn update_transfer_status(&self, id: &str, status: &str) -> Result<()> {
        let completed_at = if status == "completed" {
            Some("datetime('now')")
        } else {
            None
        };

        let mut query = "UPDATE transfers SET status = ?".to_string();
        if completed_at.is_some() {
            query.push_str(", completed_at = datetime('now')");
        }
        query.push_str(" WHERE id = ?");

        sqlx::query(&query)
            .bind(status)
            .bind(id)
            .execute(&self.pool)
            .await?;

        Ok(())
    }

    fn get_db_path() -> Result<PathBuf> {
        let mut path = dirs::data_dir()
            .unwrap_or_else(|| PathBuf::from("."));
        path.push("enterprise-file-transfer");
        std::fs::create_dir_all(&path)?;
        path.push("database.sqlite");
        Ok(path)
    }
}
