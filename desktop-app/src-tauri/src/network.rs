use anyhow::{Result, anyhow};
use quinn::{Endpoint, ServerConfig, ClientConfig, Connection};
use quinn::rustls::{ServerConfig as RustlsServerConfig, ClientConfig as RustlsClientConfig};
use quinn::rustls::pki_types::{CertificateDer, PrivateKeyDer, ServerName};
use quinn::rustls::client::danger::{ServerCertVerifier, ServerCertVerified};
use std::net::{SocketAddr, IpAddr, Ipv4Addr};
use std::sync::Arc;
use std::net::UdpSocket;
use log::{info, error, debug};

pub struct NetworkManager {
    endpoint: Option<Endpoint>,
    local_addr: SocketAddr,
}

impl NetworkManager {
    pub async fn new() -> Result<Self> {
        let local_addr = SocketAddr::new(IpAddr::V4(Ipv4Addr::UNSPECIFIED), 0);
        
        Ok(NetworkManager {
            endpoint: None,
            local_addr,
        })
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
