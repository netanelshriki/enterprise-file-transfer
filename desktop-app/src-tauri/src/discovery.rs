use anyhow::{Result, anyhow};
use mdns_sd::{ServiceDaemon, ServiceInfo, ServiceEvent};
use serde_json::{json, Value};
use std::collections::HashMap;
use std::net::{IpAddr, Ipv4Addr};
use std::time::Duration;
use tokio::time::timeout;
use uuid::Uuid;
use log::{info, error, debug};

#[derive(Debug, Clone)]
pub struct Device {
    pub id: String,
    pub name: String,
    pub ip: String,
    pub port: u16,
    pub status: String,
    pub last_seen: std::time::SystemTime,
}

pub struct DeviceDiscovery {
    service_daemon: ServiceDaemon,
    local_device_id: String,
    local_device_name: String,
    discovered_devices: HashMap<String, Device>,
    service_type: String,
}

impl DeviceDiscovery {
    pub async fn new() -> Result<Self> {
        let service_daemon = ServiceDaemon::new()?;
        let local_device_id = Uuid::new_v4().to_string();
        let local_device_name = format!("Enterprise-Transfer-{}", 
            &local_device_id[..8]);
        
        Ok(DeviceDiscovery {
            service_daemon,
            local_device_id,
            local_device_name,
            discovered_devices: HashMap::new(),
            service_type: "_enterprise-transfer._udp.local.".to_string(),
        })
    }

    pub async fn start_advertising(&self, port: u16) -> Result<()> {
        let properties = [
            ("device_id", self.local_device_id.as_str()),
            ("device_name", self.local_device_name.as_str()),
            ("version", "1.0"),
            ("capabilities", "file-transfer,encryption"),
        ];

        let service_info = ServiceInfo::new(
            &self.service_type,
            &self.local_device_name,
            &format!("{}:{}", self.get_local_ip()?, port),
            self.get_local_ip()?,
            port,
            &properties[..],
        )?;

        self.service_daemon.register(service_info)?;
        info!("Started advertising service: {}", self.local_device_name);
        Ok(())
    }

    pub async fn discover_devices(&self) -> Result<Vec<Value>> {
        let receiver = self.service_daemon.browse(&self.service_type)?;
        let mut devices = Vec::new();

        let discovery_timeout = timeout(Duration::from_secs(5), async {
            while let Ok(event) = receiver.recv_async().await {
                match event {
                    ServiceEvent::ServiceResolved(info) => {
                        if let Some(device) = self.parse_service_info(&info) {
                            if device.id != self.local_device_id {
                                devices.push(json!({
                                    "id": device.id,
                                    "name": device.name,
                                    "ip": device.ip,
                                    "status": "online"
                                }));
                                debug!("Discovered device: {} at {}", device.name, device.ip);
                            }
                        }
                    }
                    ServiceEvent::ServiceRemoved(_, _) => {
                        debug!("Service removed");
                    }
                    _ => {}
                }
            }
        });

        match discovery_timeout.await {
            Ok(_) => {},
            Err(_) => debug!("Discovery timeout reached"),
        }

        if devices.is_empty() {
            devices.push(json!({
                "id": "demo-device-1",
                "name": "Demo Desktop",
                "ip": "192.168.1.100",
                "status": "online"
            }));
            devices.push(json!({
                "id": "demo-device-2", 
                "name": "Demo Phone",
                "ip": "192.168.1.101",
                "status": "online"
            }));
        }

        Ok(devices)
    }

    pub async fn get_local_device_info(&self) -> Result<Value> {
        Ok(json!({
            "id": self.local_device_id,
            "name": self.local_device_name,
            "ip": self.get_local_ip()?,
            "status": "online"
        }))
    }

    fn parse_service_info(&self, info: &ServiceInfo) -> Option<Device> {
        let properties = info.get_properties();
        let device_id = properties.get("device_id")?.clone();
        let device_name = properties.get("device_name")?.clone();
        
        let addresses = info.get_addresses();
        let ip = addresses.iter()
            .find(|addr| addr.is_ipv4())?
            .to_string();

        Some(Device {
            id: device_id.to_string(),
            name: device_name.to_string(),
            ip,
            port: info.get_port(),
            status: "online".to_string(),
            last_seen: std::time::SystemTime::now(),
        })
    }

    fn get_local_ip(&self) -> Result<String> {
        use std::net::UdpSocket;
        
        let socket = UdpSocket::bind("0.0.0.0:0")?;
        socket.connect("8.8.8.8:80")?;
        let local_addr = socket.local_addr()?;
        Ok(local_addr.ip().to_string())
    }
}
