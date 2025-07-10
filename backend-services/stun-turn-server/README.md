# STUN/TURN Server Setup

This directory contains the configuration for STUN/TURN servers that enable internet file transfers by handling NAT traversal and firewall bypass.

## Architecture

The setup includes:

1. **TURN Server (Coturn)**: Handles NAT traversal and relay connections
2. **Signaling Server**: WebSocket-based device discovery and connection setup
3. **STUN Functionality**: Built into the TURN server for NAT detection

## Quick Start

### Development (Local)

```bash
# Start the servers
docker-compose up -d

# View logs
docker-compose logs -f

# Stop servers
docker-compose down
```

### Production Deployment

1. **Configure External IP**:
   ```bash
   # Edit turnserver.conf
   external-ip=YOUR_PUBLIC_IP
   ```

2. **Setup TLS Certificates**:
   ```bash
   # Add your certificates to turnserver.conf
   cert=/etc/ssl/certs/turn_server_cert.pem
   pkey=/etc/ssl/private/turn_server_pkey.pem
   ```

3. **Deploy**:
   ```bash
   docker-compose -f docker-compose.prod.yml up -d
   ```

## Server Configuration

### TURN Server (Port 3478/5349)
- **STUN/TURN**: Handles NAT traversal
- **Relay Ports**: 49152-65535 for media relay
- **Authentication**: Long-term credentials
- **TLS Support**: Port 5349 for secure connections

### Signaling Server (Port 8080)
- **WebSocket**: Device discovery and signaling
- **SQLite Database**: Device registration and history
- **REST API**: Device management endpoints

## Connection Fallback Chain

1. **Local Network**: Direct mDNS discovery (fastest)
2. **Direct Internet**: Peer-to-peer via public IPs
3. **STUN-Assisted**: NAT traversal with STUN server
4. **TURN Relay**: Full relay through TURN server (slowest but most reliable)

## Testing

### Test STUN Server
```bash
# Using stun client
stun YOUR_SERVER_IP 3478
```

### Test TURN Server
```bash
# Using turnutils
turnutils_uclient -T -u fileuser -w filepass123 YOUR_SERVER_IP
```

### Test Signaling Server
```bash
# WebSocket connection test
wscat -c ws://YOUR_SERVER_IP:8080/ws

# REST API test
curl http://YOUR_SERVER_IP:8080/health
```

## Security Notes

- Change default TURN credentials in production
- Use TLS certificates for secure connections
- Restrict TURN server access with firewall rules
- Monitor relay usage to prevent abuse

## Monitoring

View server status:
```bash
# Container status
docker-compose ps

# Resource usage
docker stats

# Server logs
docker-compose logs coturn
docker-compose logs signaling-server
```

## Troubleshooting

### Common Issues

1. **NAT Traversal Fails**:
   - Check external-ip configuration
   - Verify firewall rules for relay ports
   - Test STUN server connectivity

2. **High Relay Usage**:
   - Check if direct connections are failing
   - Verify STUN server is working
   - Monitor network conditions

3. **Connection Timeouts**:
   - Increase TURN server timeouts
   - Check relay port availability
   - Verify client configuration

### Port Requirements

- **3478/udp,tcp**: STUN/TURN
- **5349/udp,tcp**: STUN/TURN over TLS
- **8080/tcp**: Signaling server
- **49152-65535/udp**: TURN relay ports
