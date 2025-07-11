# SecureTransfer Architecture

This document describes the technical architecture and design decisions for SecureTransfer.

## 🏗️ System Overview

SecureTransfer is a cross-platform file transfer application consisting of:
- **Desktop Applications** (Windows/Linux) using JavaFX
- **Android Application** using native Android SDK
- **Signaling Server** for internet transfers using Spring Boot
- **STUN/TURN Infrastructure** for NAT traversal

## 📱 Application Architecture

### Desktop Application (JavaFX)

```
┌─────────────────────────────────────────┐
│              Presentation Layer          │
│  ┌─────────────┐  ┌─────────────────────┐│
│  │   JavaFX    │  │     Controllers     ││
│  │     UI      │  │   (FXML + Java)     ││
│  └─────────────┘  └─────────────────────┘│
├─────────────────────────────────────────┤
│               Business Layer            │
│  ┌─────────────┐  ┌─────────────────────┐│
│  │  Transfer   │  │    Device Discovery ││
│  │  Manager    │  │      Service        ││
│  └─────────────┘  └─────────────────────┘│
├─────────────────────────────────────────┤
│              Network Layer              │
│  ┌─────────────┐  ┌─────────────────────┐│
│  │   Local     │  │     Internet        ││
│  │ Network     │  │   Transfer (WebRTC) ││
│  │(mDNS/TCP)   │  │                     ││
│  └─────────────┘  └─────────────────────┘│
├─────────────────────────────────────────┤
│               Data Layer                │
│  ┌─────────────┐  ┌─────────────────────┐│
│  │ H2 Database │  │   File System       ││
│  │ (Embedded)  │  │     Access          ││
│  └─────────────┘  └─────────────────────┘│
└─────────────────────────────────────────┘
```

### Android Application

```
┌─────────────────────────────────────────┐
│              UI Layer                   │
│  ┌─────────────┐  ┌─────────────────────┐│
│  │  Activities │  │     Fragments       ││
│  │     &       │  │   (Material 3)      ││
│  │  Services   │  │                     ││
│  └─────────────┘  └─────────────────────┘│
├─────────────────────────────────────────┤
│            ViewModel Layer              │
│  ┌─────────────┐  ┌─────────────────────┐│
│  │  Transfer   │  │    Device List      ││
│  │ ViewModel   │  │    ViewModel        ││
│  └─────────────┘  └─────────────────────┘│
├─────────────────────────────────────────┤
│            Repository Layer             │
│  ┌─────────────┐  ┌─────────────────────┐│
│  │  Transfer   │  │    Network          ││
│  │ Repository  │  │   Repository        ││
│  └─────────────┘  └─────────────────────┘│
├─────────────────────────────────────────┤
│               Data Layer                │
│  ┌─────────────┐  ┌─────────────────────┐│
│  │    Room     │  │   Network APIs      ││
│  │  Database   │  │  (OkHttp/Retrofit)  ││
│  └─────────────┘  └─────────────────────┘│
└─────────────────────────────────────────┘
```

## 🌐 Network Architecture

### Connection Hierarchy

1. **Local Network (Fastest)**
   - mDNS device discovery
   - Direct TCP connections
   - 200+ MB/s on gigabit networks

2. **Internet Direct (Fast)**
   - HTTP/2 with OkHttp
   - Direct device-to-device
   - 15+ MB/s typical speeds

3. **WebRTC P2P (Reliable)**
   - STUN for NAT traversal
   - Direct peer connections
   - Works through most firewalls

4. **TURN Relay (Fallback)**
   - Server-mediated transfers
   - Works in restrictive networks
   - Slower but guaranteed connectivity

## 🔒 Security Architecture

### Encryption Pipeline

```
┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│    File     │───▶│  AES-256    │───▶│  Network    │
│   Source    │    │ Encryption  │    │ Transport   │
└─────────────┘    └─────────────┘    └─────────────┘
```

### Key Exchange Protocol

1. **Device Discovery**
   - Each device generates RSA-2048 keypair
   - Public key broadcast with device info
   - Digital signatures for authenticity

2. **Session Establishment**
   - ECDH key exchange for perfect forward secrecy
   - AES-256 session key derivation
   - HMAC-SHA256 for message authentication

3. **File Transfer**
   - AES-256-GCM for authenticated encryption
   - 64KB chunks with individual IVs
   - Integrity verification per chunk

## 🏗️ Component Design

### Core Components

#### TransferManager
```java
public class TransferManager {
    private final NetworkManager networkManager;
    private final SecurityManager securityManager;
    private final StorageManager storageManager;
    
    public CompletableFuture<TransferResult> sendFile(
        Device targetDevice, 
        File file
    );
    
    public void receiveFile(
        TransferRequest request,
        TransferCallback callback
    );
}
```

#### DeviceDiscovery
```java
public class DeviceDiscovery {
    private final mDNSService mdnsService;
    private final SignalingClient signalingClient;
    
    public Observable<Device> discoverDevices();
    public void announceDevice(DeviceInfo info);
    public void stopDiscovery();
}
```

#### NetworkManager
```java
public class NetworkManager {
    private final LocalNetworkTransport localTransport;
    private final InternetTransport internetTransport;
    private final WebRTCTransport webrtcTransport;
    
    public TransportChannel getBestChannel(Device target);
    public CompletableFuture<Void> sendData(
        TransportChannel channel,
        byte[] data
    );
}
```

This architecture provides a robust, scalable, and secure foundation for cross-platform file transfers with excellent user experience and enterprise-grade reliability.
