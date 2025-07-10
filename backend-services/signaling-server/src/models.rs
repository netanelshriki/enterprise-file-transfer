use serde::{Deserialize, Serialize};
use uuid::Uuid;

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(tag = "type")]
pub enum ClientMessage {
    Register {
        name: String,
        device_type: String,
        public_key: String,
    },
    Discover,
    ConnectRequest {
        target_device_id: Uuid,
    },
    ConnectResponse {
        to_device_id: Uuid,
        accepted: bool,
    },
    SignalingData {
        target_device_id: Uuid,
        data: serde_json::Value,
    },
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(tag = "type")]
pub enum ServerMessage {
    Registered {
        device_id: Uuid,
    },
    DeviceList {
        devices: Vec<DeviceInfo>,
    },
    ConnectionRequest {
        from_device_id: Uuid,
        from_device_name: String,
    },
    ConnectionResponse {
        from_device_id: Uuid,
        accepted: bool,
    },
    SignalingData {
        from_device_id: Uuid,
        data: serde_json::Value,
    },
    Error {
        message: String,
    },
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct DeviceInfo {
    pub id: Uuid,
    pub name: String,
    pub device_type: String,
    pub public_key: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct RoomInfo {
    pub id: String,
    pub device_count: usize,
}

#[derive(Debug, Clone)]
pub struct Device {
    pub id: Uuid,
    pub name: String,
    pub device_type: String,
    pub public_key: String,
    pub last_seen: chrono::DateTime<chrono::Utc>,
}
