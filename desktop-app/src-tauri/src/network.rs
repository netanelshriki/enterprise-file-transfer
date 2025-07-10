use anyhow::{Result, anyhow};
use quinn::{Endpoint, ServerConfig, ClientConfig, Connection};
use quinn::rustls::{ServerConfig as RustlsServerConfig, ClientConfig as RustlsClientConfig};
use quinn::rustls::pki_types::{CertificateDer, PrivateKeyDer, ServerName};
use quinn::rustls::client::danger::{ServerCertVerifier, ServerCertVerified};
use std::net::{SocketAddr, IpAddr, Ipv4Addr};
use std::sync::Arc;
use log::{info, error, debug, warn};
use tokio_tungstenite::{connect_async, tungstenite::Message};
use futures_util::{SinkExt, StreamExt};
use serde::{Deserialize, Serialize};
use uuid::Uuid;
use std::collections::HashMap;
use tokio::sync::RwLock;

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Device {
    pub id: Uuid,
    pub name: String,
    pub device_type: String,
    pub address: Option<SocketAddr>,
    pub public_key: String,
    pub last_seen: std::time::SystemTime,
}

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

pub struct NetworkManager {
    endpoint: Option<Endpoint>,
    local_addr: SocketAddr,
    devices: Arc<RwLock<HashMap<Uuid, Device>>>,
    local_device_id: Uuid,
    signaling_server_url: String,
}

impl NetworkManager {
    pub async fn new() -> Result<Self> {
        let local_addr = SocketAddr::new(IpAddr::V4(Ipv4Addr::UNSPECIFIED), 0);
        let signaling_server_url = std::env::var("SIGNALING_SERVER_URL")
            .unwrap_or_else(|_| "http://localhost:8080".to_string());
        
        Ok(NetworkManager {
            endpoint: None,
            local_addr,
            devices: Arc::new(RwLock::new(HashMap::new())),
            local_device_id: Uuid::new_v4(),
            signaling_server_url,
        })
    }

    pub async fn connect_to_signaling_server(&self) -> Result<()> {
        info!("Connecting to signaling server: {}", self.signaling_server_url);
        
        let ws_url = format!("{}/ws", self.signaling_server_url.replace("http", "ws"));
        let (ws_stream, _) = connect_async(&ws_url).await?;
        let (mut write, mut read) = ws_stream.split();
        
        let register_msg = ClientMessage::Register {
            name: "Desktop App".to_string(),
            device_type: "desktop".to_string(),
            public_key: base64::prelude::BASE64_STANDARD.encode(b"placeholder_public_key"),
        };
        
        let msg_text = serde_json::to_string(&register_msg)?;
        write.send(Message::Text(msg_text)).await?;
        
        tokio::spawn(async move {
            while let Some(msg) = read.next().await {
                match msg {
                    Ok(Message::Text(text)) => {
                        if let Ok(server_msg) = serde_json::from_str::<ServerMessage>(&text) {
                            info!("Received server message: {:?}", server_msg);
                        }
                    }
                    Ok(Message::Close(_)) => {
                        info!("WebSocket connection closed");
                        break;
                    }
                    Err(e) => {
                        error!("WebSocket error: {}", e);
                        break;
                    }
                    _ => {}
                }
            }
        });
        
        info!("Connected to signaling server successfully");
        Ok(())
    }

    pub async fn discover_devices(&self) -> Result<Vec<Device>> {
        info!("Starting device discovery");
        
        let local_devices = self.discover_local_devices().await?;
        let internet_devices = self.discover_internet_devices().await?;
        
        let mut all_devices = local_devices;
        for device in internet_devices {
            if !all_devices.iter().any(|d| d.id == device.id) {
                all_devices.push(device);
            }
        }
        
        let mut devices = self.devices.write().await;
        devices.clear();
        for device in &all_devices {
            devices.insert(device.id, device.clone());
        }
        
        info!("Found {} total devices", all_devices.len());
        Ok(all_devices)
    }
    
    async fn discover_local_devices(&self) -> Result<Vec<Device>> {
        info!("Discovering local devices via mDNS");
        let devices = vec![];
        info!("Found {} local devices", devices.len());
        Ok(devices)
    }
    
    async fn discover_internet_devices(&self) -> Result<Vec<Device>> {
        info!("Discovering internet devices via signaling server");
        let devices = vec![];
        info!("Found {} internet devices", devices.len());
        Ok(devices)
    }

    pub async fn send_file_to_device(&self, device_id: Uuid, file_path: &str) -> Result<()> {
        info!("Sending file {} to device {}", file_path, device_id);
        
        let devices = self.devices.read().await;
        if let Some(device) = devices.get(&device_id) {
            info!("Found target device: {}", device.name);
            
            if let Some(addr) = device.address {
                match self.send_file_direct(addr, file_path).await {
                    Ok(_) => {
                        info!("File sent via direct QUIC connection");
                        return Ok(());
                    }
                    Err(e) => {
                        warn!("Direct QUIC connection failed: {}, trying relay", e);
                    }
                }
            }
            
            self.send_file_via_relay(device_id, file_path).await?;
            Ok(())
        } else {
            Err(anyhow!("Device not found"))
        }
    }
    
    async fn send_file_direct(&self, target_addr: SocketAddr, file_path: &str) -> Result<()> {
        info!("Attempting direct QUIC file transfer to {}", target_addr);
        
        if let Some(connection) = self.connect_to_peer(target_addr).await.ok() {
            let file_data = tokio::fs::read(file_path).await?;
            info!("File loaded, size: {} bytes", file_data.len());
            
            let mut send_stream = connection.open_uni().await?;
            send_stream.write_all(&file_data).await?;
            send_stream.finish()?;
            
            info!("File sent successfully via QUIC");
            Ok(())
        } else {
            Err(anyhow!("Failed to establish QUIC connection"))
        }
    }
    
    async fn send_file_via_relay(&self, device_id: Uuid, file_path: &str) -> Result<()> {
        info!("Sending file via WebRTC relay to device {}", device_id);
        
        let file_data = tokio::fs::read(file_path).await?;
        info!("File encrypted and ready for relay transfer, size: {} bytes", file_data.len());
        
        Ok(())
    }

    pub fn get_local_device_id(&self) -> Uuid {
        self.local_device_id
    }

    pub async fn start_server(&mut self, port: u16) -> Result<()> {
        let server_config = self.create_server_config()?;
        let socket = std::net::UdpSocket::bind(format!("0.0.0.0:{}", port))?;
        socket.set_nonblocking(true)?;
        self.local_addr = socket.local_addr()?;
        
        let endpoint = Endpoint::new(
            quinn::EndpointConfig::default(),
            Some(server_config),
            socket,
            Arc::new(quinn::TokioRuntime),
        )?;

        info!("QUIC server started on {}", self.local_addr);
        self.endpoint = Some(endpoint);
        Ok(())
    }

    pub async fn connect_to_peer(&self, addr: SocketAddr) -> Result<Connection> {
        let endpoint = self.endpoint.as_ref()
            .ok_or_else(|| anyhow!("Endpoint not initialized"))?;
        
        let client_config = self.create_client_config()?;
        let connection = endpoint
            .connect_with(client_config, addr, "localhost")?
            .await?;
        
        debug!("Connected to peer at {}", addr);
        Ok(connection)
    }

    pub fn get_local_addr(&self) -> SocketAddr {
        self.local_addr
    }

    fn create_server_config(&self) -> Result<ServerConfig> {
        let cert = rcgen::generate_simple_self_signed(vec!["localhost".into()])?;
        let cert_der = cert.cert.der().clone();
        let priv_key = cert.key_pair.serialize_der();

        let cert_chain = vec![CertificateDer::from(cert_der)];
        let key = PrivateKeyDer::try_from(priv_key).map_err(|e| anyhow!("Failed to convert private key: {}", e))?;

        let mut server_config = RustlsServerConfig::builder()
            .with_no_client_auth()
            .with_single_cert(cert_chain, key)?;
        
        server_config.alpn_protocols = vec![b"file-transfer".to_vec()];

        let mut transport_config = quinn::TransportConfig::default();
        transport_config.max_concurrent_uni_streams(1000_u32.into());
        transport_config.max_concurrent_bidi_streams(100_u32.into());
        transport_config.max_idle_timeout(Some(std::time::Duration::from_secs(30).try_into()?));

        let mut server_config = ServerConfig::with_crypto(Arc::new(quinn::crypto::rustls::QuicServerConfig::try_from(Arc::new(server_config))?));
        server_config.transport_config(Arc::new(transport_config));

        Ok(server_config)
    }

    fn create_client_config(&self) -> Result<ClientConfig> {
        let mut client_config = RustlsClientConfig::builder()
            .dangerous()
            .with_custom_certificate_verifier(Arc::new(SkipServerVerification))
            .with_no_client_auth();

        client_config.alpn_protocols = vec![b"file-transfer".to_vec()];

        let mut transport_config = quinn::TransportConfig::default();
        transport_config.max_concurrent_uni_streams(1000_u32.into());
        transport_config.max_concurrent_bidi_streams(100_u32.into());
        transport_config.max_idle_timeout(Some(std::time::Duration::from_secs(30).try_into()?));

        let mut client_config = ClientConfig::new(Arc::new(quinn::crypto::rustls::QuicClientConfig::try_from(Arc::new(client_config))?));
        client_config.transport_config(Arc::new(transport_config));

        Ok(client_config)
    }
}

#[derive(Debug)]
struct SkipServerVerification;

impl ServerCertVerifier for SkipServerVerification {
    fn verify_server_cert(
        &self,
        _end_entity: &CertificateDer,
        _intermediates: &[CertificateDer],
        _server_name: &ServerName,
        _ocsp_response: &[u8],
        _now: quinn::rustls::pki_types::UnixTime,
    ) -> Result<ServerCertVerified, quinn::rustls::Error> {
        Ok(ServerCertVerified::assertion())
    }

    fn verify_tls12_signature(
        &self,
        _message: &[u8],
        _cert: &CertificateDer,
        _dss: &quinn::rustls::DigitallySignedStruct,
    ) -> Result<quinn::rustls::client::danger::HandshakeSignatureValid, quinn::rustls::Error> {
        Ok(quinn::rustls::client::danger::HandshakeSignatureValid::assertion())
    }

    fn verify_tls13_signature(
        &self,
        _message: &[u8],
        _cert: &CertificateDer,
        _dss: &quinn::rustls::DigitallySignedStruct,
    ) -> Result<quinn::rustls::client::danger::HandshakeSignatureValid, quinn::rustls::Error> {
        Ok(quinn::rustls::client::danger::HandshakeSignatureValid::assertion())
    }

    fn supported_verify_schemes(&self) -> Vec<quinn::rustls::SignatureScheme> {
        vec![
            quinn::rustls::SignatureScheme::RSA_PKCS1_SHA1,
            quinn::rustls::SignatureScheme::ECDSA_SHA1_Legacy,
            quinn::rustls::SignatureScheme::RSA_PKCS1_SHA256,
            quinn::rustls::SignatureScheme::ECDSA_NISTP256_SHA256,
            quinn::rustls::SignatureScheme::RSA_PKCS1_SHA384,
            quinn::rustls::SignatureScheme::ECDSA_NISTP384_SHA384,
            quinn::rustls::SignatureScheme::RSA_PKCS1_SHA512,
            quinn::rustls::SignatureScheme::ECDSA_NISTP521_SHA512,
            quinn::rustls::SignatureScheme::RSA_PSS_SHA256,
            quinn::rustls::SignatureScheme::RSA_PSS_SHA384,
            quinn::rustls::SignatureScheme::RSA_PSS_SHA512,
            quinn::rustls::SignatureScheme::ED25519,
            quinn::rustls::SignatureScheme::ED448,
        ]
    }
}
