# STUN/TURN Server Setup Guide

This guide provides complete instructions for setting up STUN and TURN servers for SecureTransfer's WebRTC functionality.

## Overview

SecureTransfer uses STUN and TURN servers to enable peer-to-peer connections between devices behind NATs and firewalls:

- **STUN Server**: Helps devices discover their public IP address and port
- **TURN Server**: Relays traffic when direct peer-to-peer connection is not possible

## Quick Development Setup

For development and testing, you can use public STUN servers and run a local TURN server.

### Using Public STUN Servers

The application is pre-configured with Google's public STUN servers:
- `stun:stun.l.google.com:19302`
- `stun:stun1.l.google.com:19302`
- `stun:stun2.l.google.com:19302`

### Local TURN Server (Development)

```bash
# Navigate to the coturn directory
cd server/coturn

# Build and run the TURN server
docker-compose up -d

# Check logs
docker-compose logs -f coturn
```

## Production Setup

### Prerequisites

- Ubuntu 22.04 LTS server
- Public IP address
- Domain name (recommended)
- SSL certificate (Let's Encrypt recommended)

### Option 1: Docker Deployment (Recommended)

1. **Prepare the server:**
```bash
# Update system
sudo apt update && sudo apt upgrade -y

# Install Docker and Docker Compose
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh
sudo usermod -aG docker $USER

# Install Docker Compose
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose
```

2. **Deploy TURN server:**
```bash
# Clone the repository
git clone <repository-url>
cd file-transfer-app/server/coturn

# Set your external IP
export EXTERNAL_IP=YOUR_PUBLIC_IP

# Deploy
docker-compose up -d
```

3. **Configure SSL certificates:**
```bash
# Install certbot
sudo apt install certbot

# Get SSL certificate
sudo certbot certonly --standalone -d turn.yourdomain.com

# Copy certificates
sudo cp /etc/letsencrypt/live/turn.yourdomain.com/fullchain.pem ./certs/cert.pem
sudo cp /etc/letsencrypt/live/turn.yourdomain.com/privkey.pem ./certs/private.pem

# Restart coturn
docker-compose restart coturn
```

### Option 2: Native Installation

1. **Install Coturn:**
```bash
sudo apt update
sudo apt install coturn
```

2. **Configure Coturn:**
```bash
# Copy configuration
sudo cp turnserver.conf /etc/coturn/turnserver.conf

# Edit configuration
sudo nano /etc/coturn/turnserver.conf
# Update external-ip, realm, and SSL certificate paths
```

3. **Start Coturn:**
```bash
sudo systemctl enable coturn
sudo systemctl start coturn
sudo systemctl status coturn
```

## Cloud Provider Setup

### AWS EC2

1. **Launch EC2 instance:**
   - AMI: Ubuntu 22.04 LTS
   - Instance type: t3.medium (minimum)
   - Security group: Allow ports 3478, 5349, 49152-65535

2. **Configure security group:**
```bash
# STUN/TURN ports
aws ec2 authorize-security-group-ingress --group-id sg-xxxxxxxx --protocol tcp --port 3478 --cidr 0.0.0.0/0
aws ec2 authorize-security-group-ingress --group-id sg-xxxxxxxx --protocol udp --port 3478 --cidr 0.0.0.0/0
aws ec2 authorize-security-group-ingress --group-id sg-xxxxxxxx --protocol tcp --port 5349 --cidr 0.0.0.0/0
aws ec2 authorize-security-group-ingress --group-id sg-xxxxxxxx --protocol udp --port 5349 --cidr 0.0.0.0/0

# RTP relay ports
aws ec2 authorize-security-group-ingress --group-id sg-xxxxxxxx --protocol udp --port 49152-65535 --cidr 0.0.0.0/0
```

### Google Cloud Platform

1. **Create VM instance:**
```bash
gcloud compute instances create coturn-server \
    --image-family=ubuntu-2204-lts \
    --image-project=ubuntu-os-cloud \
    --machine-type=e2-medium \
    --tags=coturn-server
```

2. **Configure firewall:**
```bash
gcloud compute firewall-rules create allow-coturn \
    --allow tcp:3478,udp:3478,tcp:5349,udp:5349,udp:49152-65535 \
    --source-ranges 0.0.0.0/0 \
    --target-tags coturn-server
```

### DigitalOcean

1. **Create droplet:**
   - Image: Ubuntu 22.04 LTS
   - Size: Basic $12/month (2GB RAM)
   - Enable monitoring and backups

2. **Configure firewall:**
```bash
# Create firewall
doctl compute firewall create \
    --name coturn-firewall \
    --inbound-rules protocol:tcp,ports:22,source_addresses:0.0.0.0/0,source_addresses:::/0 \
    --inbound-rules protocol:tcp,ports:3478,source_addresses:0.0.0.0/0,source_addresses:::/0 \
    --inbound-rules protocol:udp,ports:3478,source_addresses:0.0.0.0/0,source_addresses:::/0 \
    --inbound-rules protocol:tcp,ports:5349,source_addresses:0.0.0.0/0,source_addresses:::/0 \
    --inbound-rules protocol:udp,ports:5349,source_addresses:0.0.0.0/0,source_addresses:::/0 \
    --inbound-rules protocol:udp,ports:49152-65535,source_addresses:0.0.0.0/0,source_addresses:::/0
```

## Configuration

### TURN Server Configuration

Edit `/etc/coturn/turnserver.conf`:

```conf
# Essential settings
listening-port=3478
tls-listening-port=5349
external-ip=YOUR_PUBLIC_IP
realm=yourdomain.com

# Authentication
lt-cred-mech
user=username:password

# SSL certificates
cert=/path/to/cert.pem
pkey=/path/to/private.pem

# Security
fingerprint
no-multicast-peers
no-cli
no-loopback-peers
```

### Client Configuration

Update your application configuration:

```java
// Desktop application (STUNTURNConfig.java)
stunTurnConfig.addTurnServer("turn:your-server.com:3478", "username", "password");
stunTurnConfig.addTurnServer("turns:your-server.com:5349", "username", "password");
```

```kotlin
// Android application
val turnServers = listOf(
    "turn:your-server.com:3478",
    "turns:your-server.com:5349"
)
```

## Testing

### Test STUN Server

```bash
# Using stun client
sudo apt install stun-client
stun your-server.com 3478
```

### Test TURN Server

```bash
# Using turnutils
sudo apt install coturn-utils

# Test TURN allocation
turnutils_uclient -t -T -v your-server.com -p 3478 -u username -w password
```

### Web-based Testing

Use online WebRTC testing tools:
- https://webrtc.github.io/samples/src/content/peerconnection/trickle-ice/
- https://test.webrtc.org/

## Monitoring

### Log Files

```bash
# Coturn logs
tail -f /var/log/coturn/turnserver.log

# System logs
journalctl -u coturn -f
```

### Metrics

Monitor these key metrics:
- Active TURN sessions
- Bandwidth usage
- Connection success rate
- Server resource usage

### Alerting

Set up alerts for:
- High CPU/memory usage
- Failed authentication attempts
- Service downtime
- Certificate expiration

## Security Best Practices

1. **Use strong authentication:**
   - Generate random usernames and passwords
   - Rotate credentials regularly
   - Use time-limited credentials when possible

2. **Network security:**
   - Restrict access to management ports
   - Use fail2ban for brute force protection
   - Enable firewall with minimal required ports

3. **SSL/TLS:**
   - Use valid SSL certificates
   - Disable weak cipher suites
   - Enable HSTS headers

4. **Resource limits:**
   - Set bandwidth limits per user
   - Limit concurrent sessions
   - Monitor resource usage

## Troubleshooting

### Common Issues

1. **Connection failures:**
   - Check firewall settings
   - Verify external IP configuration
   - Test with different STUN/TURN servers

2. **Authentication errors:**
   - Verify username/password
   - Check realm configuration
   - Review log files

3. **SSL certificate issues:**
   - Verify certificate validity
   - Check certificate chain
   - Ensure proper file permissions

### Debug Commands

```bash
# Check port availability
netstat -tulpn | grep :3478

# Test connectivity
telnet your-server.com 3478

# Check SSL certificate
openssl s_client -connect your-server.com:5349

# Monitor traffic
tcpdump -i any port 3478
```

## Cost Optimization

### Bandwidth Management

- Set appropriate bandwidth limits
- Use efficient codecs
- Implement connection quality monitoring

### Server Sizing

- **Small deployment**: 1 vCPU, 2GB RAM (up to 100 concurrent users)
- **Medium deployment**: 2 vCPU, 4GB RAM (up to 500 concurrent users)
- **Large deployment**: 4+ vCPU, 8GB+ RAM (1000+ concurrent users)

### Multi-region Deployment

For global applications, deploy TURN servers in multiple regions:
- US East (Virginia)
- US West (California)
- Europe (Frankfurt)
- Asia Pacific (Singapore)

## Support

For additional help:
- Check the [troubleshooting guide](TROUBLESHOOTING.md)
- Review [deployment documentation](DEPLOYMENT.md)
- Contact support at support@securetransfer.com
