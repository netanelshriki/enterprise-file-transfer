use tauri::Manager;
use std::sync::Arc;
use tokio::sync::Mutex;

mod network;
mod crypto;
mod discovery;
mod database;
mod file_transfer;

use network::NetworkManager;
use crypto::CryptoManager;
use discovery::DeviceDiscovery;
use database::Database;
use file_transfer::FileTransferManager;

pub struct AppState {
    pub network_manager: Arc<Mutex<NetworkManager>>,
    pub crypto_manager: Arc<CryptoManager>,
    pub device_discovery: Arc<Mutex<DeviceDiscovery>>,
    pub database: Arc<Mutex<Database>>,
    pub file_transfer_manager: Arc<Mutex<FileTransferManager>>,
}

#[cfg_attr(mobile, tauri::mobile_entry_point)]
pub fn run() {
    tauri::Builder::default()
        .setup(|app| {
            if cfg!(debug_assertions) {
                app.handle().plugin(
                    tauri_plugin_log::Builder::default()
                        .level(log::LevelFilter::Info)
                        .build(),
                )?;
            }

            let rt = tokio::runtime::Runtime::new().unwrap();
            let app_state = rt.block_on(async {
                let database = Arc::new(Mutex::new(Database::new().await.unwrap()));
                let crypto_manager = Arc::new(CryptoManager::new().unwrap());
                let network_manager = Arc::new(Mutex::new(NetworkManager::new().await.unwrap()));
                let device_discovery = Arc::new(Mutex::new(DeviceDiscovery::new().await.unwrap()));
                let file_transfer_manager = Arc::new(Mutex::new(FileTransferManager::new()));

                AppState {
                    network_manager,
                    crypto_manager,
                    device_discovery,
                    database,
                    file_transfer_manager,
                }
            });

            app.manage(app_state);
            Ok(())
        })
        .invoke_handler(tauri::generate_handler![
            discover_devices,
            transfer_file,
            get_device_info,
            get_transfer_history
        ])
        .run(tauri::generate_context!())
        .expect("error while running tauri application");
}

#[tauri::command]
async fn discover_devices(state: tauri::State<'_, AppState>) -> Result<Vec<serde_json::Value>, String> {
    let discovery = state.device_discovery.lock().await;
    discovery.discover_devices().await.map_err(|e| e.to_string())
}

#[tauri::command]
async fn transfer_file(
    file_path: String,
    target_device_id: String,
    target_ip: String,
    state: tauri::State<'_, AppState>,
) -> Result<String, String> {
    let mut transfer_manager = state.file_transfer_manager.lock().await;
    transfer_manager
        .transfer_file(&file_path, &target_device_id, &target_ip)
        .await
        .map_err(|e| e.to_string())
}

#[tauri::command]
async fn get_device_info(state: tauri::State<'_, AppState>) -> Result<serde_json::Value, String> {
    let discovery = state.device_discovery.lock().await;
    discovery.get_local_device_info().await.map_err(|e| e.to_string())
}

#[tauri::command]
async fn get_transfer_history(state: tauri::State<'_, AppState>) -> Result<Vec<serde_json::Value>, String> {
    let db = state.database.lock().await;
    db.get_transfer_history().await.map_err(|e| e.to_string())
}
