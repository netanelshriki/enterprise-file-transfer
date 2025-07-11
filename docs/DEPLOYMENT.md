# SecureTransfer Server Deployment Guide

This guide covers deploying the SecureTransfer signaling server and STUN/TURN infrastructure for internet file transfers.

## 🌐 Overview

For internet transfers, SecureTransfer requires:
1. **Signaling Server** - WebSocket server for device discovery and connection coordination
2. **STUN Server** - For NAT traversal and public IP discovery
3. **TURN Server** - Relay server for restrictive network environments

## 🚀 Quick Deployment (Docker)

### Prerequisites
- Docker and Docker Compose
- Domain name with SSL certificate
- Open ports: 80, 443, 3478, 5349

### 1. Clone and Setup
```bash
git clone https://github.com/netanelshriki/file-transfer-app.git
cd file-transfer-app/deployment
```

### 2. Configure Environment
```bash
cp .env.example .env
# Edit .env with your domain and credentials
```

### 3. Deploy All Services
```bash
docker-compose up -d
```

This deploys:
- Signaling server on port 8080
- STUN/TURN server on ports 3478/5349
- Nginx reverse proxy with SSL

## 🔧 Manual Deployment

### Signaling Server

#### Build from Source
```bash
cd server
mvn clean package
```

#### Run with Java
```bash
java -jar target/server-1.0.0.jar \
  --server.port=8080 \
  --spring.profiles.active=production
```

#### Environment Variables
```bash
export SERVER_PORT=8080
export CORS_ALLOWED_ORIGINS=*
export WEBSOCKET_MAX_CONNECTIONS=1000
export LOG_LEVEL=INFO
```

#### Systemd Service (Linux)
```ini
# /etc/systemd/system/securetransfer-server.service
[Unit]
Description=SecureTransfer Signaling Server
After=network.target

[Service]
Type=simple
User=securetransfer
WorkingDirectory=/opt/securetransfer
ExecStart=/usr/bin/java -jar server-1.0.0.jar
Restart=always
RestartSec=10

Environment=SERVER_PORT=8080
Environment=SPRING_PROFILES_ACTIVE=production

[Install]
WantedBy=multi-user.target
```

Enable and start:
```bash
sudo systemctl enable securetransfer-server
sudo systemctl start securetransfer-server
```

### STUN/TURN Server (Coturn)

#### Install Coturn
```bash
# Ubuntu/Debian
sudo apt-get update
sudo apt-get install coturn

# CentOS/RHEL
sudo yum install coturn
```

#### Configure Coturn
```bash
# /etc/turnserver.conf
listening-port=3478
tls-listening-port=5349

# Your external IP
external-ip=YOUR_PUBLIC_IP

# Authentication
use-auth-secret
static-auth-secret=YOUR_SECRET_KEY

# SSL certificates
cert=/etc/ssl/certs/your-domain.crt
pkey=/etc/ssl/private/your-domain.key

# Logging
log-file=/var/log/turnserver.log
verbose

# Security
no-multicast-peers
no-cli
no-tlsv1
no-tlsv1_1
```

#### Start Coturn
```bash
sudo systemctl enable coturn
sudo systemctl start coturn
```

## ☁️ Cloud Platform Deployment

### AWS Deployment

#### Using AWS ECS
```yaml
# docker-compose.aws.yml
version: '3.8'
services:
  signaling-server:
    image: securetransfer/server:latest
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=production
      - SERVER_PORT=8080
    deploy:
      replicas: 2
      
  turn-server:
    image: coturn/coturn:latest
    ports:
      - "3478:3478/udp"
      - "5349:5349/tcp"
    volumes:
      - ./coturn.conf:/etc/coturn/turnserver.conf
```

#### Application Load Balancer
```json
{
  "Type": "AWS::ElasticLoadBalancingV2::LoadBalancer",
  "Properties": {
    "Name": "securetransfer-alb",
    "Scheme": "internet-facing",
    "Type": "application",
    "Subnets": ["subnet-12345", "subnet-67890"]
  }
}
```

### Google Cloud Platform

#### Using Cloud Run
```yaml
# cloudbuild.yaml
steps:
- name: 'gcr.io/cloud-builders/docker'
  args: ['build', '-t', 'gcr.io/$PROJECT_ID/securetransfer-server', './server']
- name: 'gcr.io/cloud-builders/docker'
  args: ['push', 'gcr.io/$PROJECT_ID/securetransfer-server']
- name: 'gcr.io/cloud-builders/gcloud'
  args: ['run', 'deploy', 'securetransfer-server', 
         '--image', 'gcr.io/$PROJECT_ID/securetransfer-server',
         '--platform', 'managed',
         '--region', 'us-central1',
         '--allow-unauthenticated']
```

#### Deploy TURN on Compute Engine
```bash
# Create VM instance
gcloud compute instances create turn-server \
  --zone=us-central1-a \
  --machine-type=e2-medium \
  --image-family=ubuntu-2004-lts \
  --image-project=ubuntu-os-cloud \
  --tags=turn-server

# Configure firewall
gcloud compute firewall-rules create allow-turn \
  --allow udp:3478,tcp:5349 \
  --source-ranges 0.0.0.0/0 \
  --target-tags turn-server
```

### Microsoft Azure

#### Using Container Instances
```bash
# Create resource group
az group create --name securetransfer-rg --location eastus

# Deploy signaling server
az container create \
  --resource-group securetransfer-rg \
  --name signaling-server \
  --image securetransfer/server:latest \
  --ports 8080 \
  --environment-variables SPRING_PROFILES_ACTIVE=production

# Deploy TURN server
az container create \
  --resource-group securetransfer-rg \
  --name turn-server \
  --image coturn/coturn:latest \
  --ports 3478 5349 \
  --protocol UDP TCP
```

## 🔒 SSL/TLS Configuration

### Let's Encrypt (Certbot)
```bash
# Install certbot
sudo apt-get install certbot

# Get certificate
sudo certbot certonly --standalone -d your-domain.com

# Auto-renewal
sudo crontab -e
# Add: 0 12 * * * /usr/bin/certbot renew --quiet
```

### Nginx Reverse Proxy
```nginx
# /etc/nginx/sites-available/securetransfer
server {
    listen 80;
    server_name your-domain.com;
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl http2;
    server_name your-domain.com;

    ssl_certificate /etc/letsencrypt/live/your-domain.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/your-domain.com/privkey.pem;

    # WebSocket proxy for signaling
    location /ws {
        proxy_pass http://localhost:8080;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # API endpoints
    location /api {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

## 📊 Monitoring and Logging

### Application Metrics
```yaml
# prometheus.yml
global:
  scrape_interval: 15s

scrape_configs:
  - job_name: 'securetransfer-server'
    static_configs:
      - targets: ['localhost:8080']
    metrics_path: '/actuator/prometheus'
```

### Log Aggregation
```yaml
# docker-compose.logging.yml
version: '3.8'
services:
  signaling-server:
    logging:
      driver: "json-file"
      options:
        max-size: "10m"
        max-file: "3"
        
  elasticsearch:
    image: docker.elastic.co/elasticsearch/elasticsearch:7.15.0
    environment:
      - discovery.type=single-node
      
  kibana:
    image: docker.elastic.co/kibana/kibana:7.15.0
    ports:
      - "5601:5601"
```

## 🔧 Configuration Reference

### Signaling Server Configuration
```yaml
# application-production.yml
server:
  port: 8080
  
spring:
  websocket:
    max-connections: 1000
    heartbeat-interval: 30000
    
logging:
  level:
    com.securetransfer: INFO
    org.springframework: WARN
  file:
    name: /var/log/securetransfer/server.log
    
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus
```

### TURN Server Configuration
```ini
# Advanced turnserver.conf
listening-port=3478
tls-listening-port=5349
alt-listening-port=3479
alt-tls-listening-port=5350

# Multiple IPs
listening-ip=0.0.0.0
relay-ip=YOUR_INTERNAL_IP
external-ip=YOUR_PUBLIC_IP

# Authentication
use-auth-secret
static-auth-secret=YOUR_VERY_SECURE_SECRET

# Rate limiting
max-bps=1000000
bps-capacity=2000000
stale-nonce=600

# Security
no-multicast-peers
no-cli
no-tlsv1
no-tlsv1_1
cipher-list="ECDH+AESGCM:ECDH+CHACHA20:DH+AESGCM:ECDH+AES256:DH+AES256:ECDH+AES128:DH+AES:RSA+AESGCM:RSA+AES:!aNULL:!MD5:!DSS"

# Logging
log-file=/var/log/turnserver.log
verbose
```

## 🚨 Security Best Practices

### Firewall Configuration
```bash
# UFW (Ubuntu)
sudo ufw allow 22/tcp      # SSH
sudo ufw allow 80/tcp      # HTTP
sudo ufw allow 443/tcp     # HTTPS
sudo ufw allow 8080/tcp    # Signaling server
sudo ufw allow 3478/udp    # STUN
sudo ufw allow 5349/tcp    # TURN over TLS
sudo ufw enable

# iptables
iptables -A INPUT -p tcp --dport 22 -j ACCEPT
iptables -A INPUT -p tcp --dport 80 -j ACCEPT
iptables -A INPUT -p tcp --dport 443 -j ACCEPT
iptables -A INPUT -p tcp --dport 8080 -j ACCEPT
iptables -A INPUT -p udp --dport 3478 -j ACCEPT
iptables -A INPUT -p tcp --dport 5349 -j ACCEPT
```

### Rate Limiting
```nginx
# Nginx rate limiting
http {
    limit_req_zone $binary_remote_addr zone=api:10m rate=10r/s;
    limit_req_zone $binary_remote_addr zone=ws:10m rate=5r/s;
    
    server {
        location /api {
            limit_req zone=api burst=20 nodelay;
            proxy_pass http://localhost:8080;
        }
        
        location /ws {
            limit_req zone=ws burst=10 nodelay;
            proxy_pass http://localhost:8080;
        }
    }
}
```

## 📈 Scaling and High Availability

### Load Balancing
```yaml
# docker-compose.ha.yml
version: '3.8'
services:
  nginx:
    image: nginx:alpine
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./nginx.conf:/etc/nginx/nginx.conf
      
  signaling-server-1:
    image: securetransfer/server:latest
    environment:
      - SPRING_PROFILES_ACTIVE=production
      
  signaling-server-2:
    image: securetransfer/server:latest
    environment:
      - SPRING_PROFILES_ACTIVE=production
      
  redis:
    image: redis:alpine
    command: redis-server --appendonly yes
```

### Database Clustering
```yaml
# For production with persistent storage
services:
  postgres-primary:
    image: postgres:15
    environment:
      POSTGRES_DB: securetransfer
      POSTGRES_USER: st_user
      POSTGRES_PASSWORD: secure_password
      
  postgres-replica:
    image: postgres:15
    environment:
      PGUSER: postgres
      POSTGRES_PASSWORD: secure_password
    command: |
      bash -c "
      until pg_basebackup --pgdata=/var/lib/postgresql/data -R --slot=replication_slot --host=postgres-primary --port=5432
      do
      echo 'Waiting for primary to connect...'
      sleep 1s
      done
      echo 'Backup done, starting replica...'
      chmod 0700 /var/lib/postgresql/data
      postgres
      "
```

## 🔍 Troubleshooting

### Common Issues

#### Connection Refused
```bash
# Check if service is running
sudo systemctl status securetransfer-server

# Check logs
sudo journalctl -u securetransfer-server -f

# Check port binding
sudo netstat -tlnp | grep 8080
```

#### TURN Server Not Working
```bash
# Test TURN server
turnutils_stunclient -p 3478 your-domain.com

# Check TURN logs
sudo tail -f /var/log/turnserver.log

# Verify firewall
sudo ufw status
```

#### SSL Certificate Issues
```bash
# Check certificate validity
openssl x509 -in /etc/letsencrypt/live/your-domain.com/fullchain.pem -text -noout

# Test SSL connection
openssl s_client -connect your-domain.com:443
```

### Performance Tuning

#### JVM Options
```bash
java -Xms512m -Xmx2g \
     -XX:+UseG1GC \
     -XX:MaxGCPauseMillis=200 \
     -XX:+UseStringDeduplication \
     -jar server-1.0.0.jar
```

#### System Limits
```bash
# /etc/security/limits.conf
securetransfer soft nofile 65536
securetransfer hard nofile 65536

# /etc/sysctl.conf
net.core.somaxconn = 65535
net.ipv4.tcp_max_syn_backlog = 65535
```

This deployment guide provides comprehensive instructions for setting up SecureTransfer's server infrastructure in various environments, from simple Docker deployments to enterprise-grade cloud solutions.
