use anyhow::Result;
use axum::{
    extract::{ws::WebSocket, State, WebSocketUpgrade},
    http::StatusCode,
    response::Response,
    routing::{get, post},
    Json, Router,
};
use clap::Parser;
use dashmap::DashMap;
use futures_util::{SinkExt, StreamExt};
use std::{net::SocketAddr, sync::Arc, time::SystemTime};
use tokio::sync::broadcast;
use tower_http::{cors::CorsLayer, trace::TraceLayer};
use tracing::{info, error};
use uuid::Uuid;

mod database;
mod models;
mod transfer;
mod crypto;

use database::Database;
use models::*;

#[derive(Parser)]
#[command(name = "signaling-server")]
#[command(about = "WebRTC signaling server for enterprise file transfer")]
struct Args {
    #[arg(short, long, default_value = "0.0.0.0:8080")]
    bind: SocketAddr,
    
    #[arg(short, long, default_value = "signaling.db")]
    database: String,
}

#[derive(Clone)]
struct AppState {
    devices: Arc<DashMap<Uuid, ConnectedDevice>>,
    rooms: Arc<DashMap<String, Room>>,
    db: Database,
    broadcast_tx: broadcast::Sender<ServerMessage>,
}

#[derive(Debug, Clone)]
struct ConnectedDevice {
    id: Uuid,
    name: String,
    device_type: String,
    public_key: String,
    websocket_tx: Option<tokio::sync::mpsc::UnboundedSender<ServerMessage>>,
    last_seen: SystemTime,
}

#[derive(Debug, Clone)]
struct Room {
    id: String,
    devices: Vec<Uuid>,
    created_at: SystemTime,
}

#[tokio::main]
async fn main() -> Result<()> {
    tracing_subscriber::fmt::init();
    
    let args = Args::parse();
    
    let db = Database::new(&args.database).await?;
    db.migrate().await?;
    
    let (broadcast_tx, _) = broadcast::channel(1000);
    
    let state = AppState {
        devices: Arc::new(DashMap::new()),
        rooms: Arc::new(DashMap::new()),
        db,
        broadcast_tx,
    };
    
    let app = Router::new()
        .route("/ws", get(websocket_handler))
        .route("/health", get(health_check))
        .route("/devices", get(list_devices))
        .route("/rooms", post(create_room))
        .route("/rooms/:room_id", get(get_room))
        .layer(CorsLayer::permissive())
        .layer(TraceLayer::new_for_http())
        .with_state(state);
    
    info!("Starting signaling server on {}", args.bind);
    
    let listener = tokio::net::TcpListener::bind(args.bind).await?;
    axum::serve(listener, app).await?;
    
    Ok(())
}

async fn websocket_handler(
    ws: WebSocketUpgrade,
    State(state): State<AppState>,
) -> Response {
    ws.on_upgrade(|socket| handle_websocket(socket, state))
}

async fn handle_websocket(socket: WebSocket, state: AppState) {
    let (mut sender, mut receiver) = socket.split();
    let (tx, mut rx) = tokio::sync::mpsc::unbounded_channel();
    
    let mut device_id: Option<Uuid> = None;
    
    let send_task = tokio::spawn(async move {
        while let Some(msg) = rx.recv().await {
            if let Ok(text) = serde_json::to_string(&msg) {
                if sender.send(axum::extract::ws::Message::Text(text)).await.is_err() {
                    break;
                }
            }
        }
    });
    
    while let Some(msg) = receiver.next().await {
        if let Ok(msg) = msg {
            if let axum::extract::ws::Message::Text(text) = msg {
                if let Ok(client_msg) = serde_json::from_str::<ClientMessage>(&text) {
                    match handle_client_message(client_msg, &state, &tx, &mut device_id).await {
                        Ok(_) => {},
                        Err(e) => {
                            error!("Error handling client message: {}", e);
                            break;
                        }
                    }
                }
            } else if let axum::extract::ws::Message::Close(_) = msg {
                break;
            }
        } else {
            break;
        }
    }
    
    if let Some(id) = device_id {
        state.devices.remove(&id);
        info!("Device {} disconnected", id);
    }
    
    send_task.abort();
}

async fn handle_client_message(
    msg: ClientMessage,
    state: &AppState,
    tx: &tokio::sync::mpsc::UnboundedSender<ServerMessage>,
    device_id: &mut Option<Uuid>,
) -> Result<()> {
    match msg {
        ClientMessage::Register { name, device_type, public_key } => {
            let id = Uuid::new_v4();
            *device_id = Some(id);
            
            let device = ConnectedDevice {
                id,
                name: name.clone(),
                device_type: device_type.clone(),
                public_key: public_key.clone(),
                websocket_tx: Some(tx.clone()),
                last_seen: SystemTime::now(),
            };
            
            state.devices.insert(id, device);
            
            state.db.store_device(&Device {
                id,
                name,
                device_type,
                public_key,
                last_seen: chrono::Utc::now(),
            }).await?;
            
            let response = ServerMessage::Registered { device_id: id };
            tx.send(response)?;
            
            info!("Device {} registered", id);
        },
        
        ClientMessage::Discover => {
            let devices: Vec<DeviceInfo> = state.devices
                .iter()
                .filter(|entry| Some(*entry.key()) != *device_id)
                .map(|entry| DeviceInfo {
                    id: entry.value().id,
                    name: entry.value().name.clone(),
                    device_type: entry.value().device_type.clone(),
                    public_key: entry.value().public_key.clone(),
                })
                .collect();
            
            let response = ServerMessage::DeviceList { devices };
            tx.send(response)?;
        },
        
        ClientMessage::ConnectRequest { target_device_id } => {
            if let Some(target_device) = state.devices.get(&target_device_id) {
                if let Some(target_tx) = &target_device.websocket_tx {
                    let request = ServerMessage::ConnectionRequest {
                        from_device_id: device_id.unwrap_or_default(),
                        from_device_name: state.devices.get(&device_id.unwrap_or_default())
                            .map(|d| d.name.clone())
                            .unwrap_or_default(),
                    };
                    target_tx.send(request)?;
                }
            }
        },
        
        ClientMessage::ConnectResponse { to_device_id, accepted } => {
            if let Some(target_device) = state.devices.get(&to_device_id) {
                if let Some(target_tx) = &target_device.websocket_tx {
                    let response = ServerMessage::ConnectionResponse {
                        from_device_id: device_id.unwrap_or_default(),
                        accepted,
                    };
                    target_tx.send(response)?;
                }
            }
        },
        
        ClientMessage::SignalingData { target_device_id, data } => {
            if let Some(target_device) = state.devices.get(&target_device_id) {
                if let Some(target_tx) = &target_device.websocket_tx {
                    let signal = ServerMessage::SignalingData {
                        from_device_id: device_id.unwrap_or_default(),
                        data,
                    };
                    target_tx.send(signal)?;
                }
            }
        },
    }
    
    Ok(())
}

async fn health_check() -> &'static str {
    "OK"
}

async fn list_devices(State(state): State<AppState>) -> Json<Vec<DeviceInfo>> {
    let devices: Vec<DeviceInfo> = state.devices
        .iter()
        .map(|entry| DeviceInfo {
            id: entry.value().id,
            name: entry.value().name.clone(),
            device_type: entry.value().device_type.clone(),
            public_key: entry.value().public_key.clone(),
        })
        .collect();
    
    Json(devices)
}

async fn create_room(State(state): State<AppState>) -> Result<Json<RoomInfo>, StatusCode> {
    let room_id = Uuid::new_v4().to_string();
    let room = Room {
        id: room_id.clone(),
        devices: Vec::new(),
        created_at: SystemTime::now(),
    };
    
    state.rooms.insert(room_id.clone(), room);
    
    Ok(Json(RoomInfo {
        id: room_id,
        device_count: 0,
    }))
}

async fn get_room(
    axum::extract::Path(room_id): axum::extract::Path<String>,
    State(state): State<AppState>,
) -> Result<Json<RoomInfo>, StatusCode> {
    if let Some(room) = state.rooms.get(&room_id) {
        Ok(Json(RoomInfo {
            id: room.id.clone(),
            device_count: room.devices.len(),
        }))
    } else {
        Err(StatusCode::NOT_FOUND)
    }
}
