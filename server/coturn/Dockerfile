FROM ubuntu:22.04

LABEL maintainer="SecureTransfer <support@securetransfer.com>"
LABEL description="Coturn STUN/TURN Server for SecureTransfer"
LABEL version="1.0.0"

# Install coturn and dependencies
RUN apt-get update && apt-get install -y \
    coturn \
    openssl \
    curl \
    && rm -rf /var/lib/apt/lists/*

# Create directories
RUN mkdir -p /etc/coturn/certs \
    && mkdir -p /var/log/coturn \
    && mkdir -p /var/run/coturn \
    && mkdir -p /var/lib/coturn

# Copy configuration
COPY turnserver.conf /etc/coturn/turnserver.conf

# Generate self-signed certificates (replace with real certificates in production)
RUN openssl req -x509 -newkey rsa:4096 -keyout /etc/coturn/certs/private.pem \
    -out /etc/coturn/certs/cert.pem -days 365 -nodes \
    -subj "/C=US/ST=State/L=City/O=SecureTransfer/CN=turn.securetransfer.com"

# Set permissions
RUN chown -R turnserver:turnserver /etc/coturn \
    && chown -R turnserver:turnserver /var/log/coturn \
    && chown -R turnserver:turnserver /var/run/coturn \
    && chown -R turnserver:turnserver /var/lib/coturn \
    && chmod 600 /etc/coturn/certs/private.pem \
    && chmod 644 /etc/coturn/certs/cert.pem

# Expose ports
EXPOSE 3478/udp 3478/tcp 5349/udp 5349/tcp
EXPOSE 49152-65535/udp

# Health check
HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
    CMD curl -f http://localhost:3478 || exit 1

# Switch to turnserver user
USER turnserver

# Start coturn
CMD ["turnserver", "-c", "/etc/coturn/turnserver.conf", "--pidfile=", "-f"]
