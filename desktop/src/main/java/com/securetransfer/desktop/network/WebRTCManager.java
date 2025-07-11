package com.securetransfer.desktop.network;

import com.securetransfer.desktop.model.Device;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.io.File;

public class WebRTCManager {
    private static final Logger logger = LoggerFactory.getLogger(WebRTCManager.class);
    
    private final ConcurrentHashMap<String, WebRTCConnection> activeConnections = new ConcurrentHashMap<>();
    private final STUNTURNConfig stunTurnConfig;
    private boolean initialized = false;
    
    public WebRTCManager(STUNTURNConfig stunTurnConfig) {
        this.stunTurnConfig = stunTurnConfig;
    }
    
    public void initialize() {
        if (initialized) {
            return;
        }
        
        try {
            logger.info("Initializing WebRTC with STUN/TURN servers");
            logger.info("STUN servers: {}", stunTurnConfig.getStunServers());
            logger.info("TURN servers: {}", stunTurnConfig.getTurnServers().size());
            
            initialized = true;
            logger.info("WebRTC initialized successfully");
        } catch (Exception e) {
            logger.error("Failed to initialize WebRTC", e);
            throw new RuntimeException("WebRTC initialization failed", e);
        }
    }
    
    public CompletableFuture<WebRTCConnection> createConnection(Device targetDevice) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                logger.info("Creating WebRTC connection to device: {}", targetDevice.getName());
                
                String connectionId = generateConnectionId(targetDevice);
                WebRTCConnection connection = new WebRTCConnection(connectionId, targetDevice, stunTurnConfig);
                
                connection.initialize();
                activeConnections.put(connectionId, connection);
                
                logger.info("WebRTC connection created: {}", connectionId);
                return connection;
                
            } catch (Exception e) {
                logger.error("Failed to create WebRTC connection to {}", targetDevice.getName(), e);
                throw new RuntimeException("WebRTC connection creation failed", e);
            }
        });
    }
    
    public CompletableFuture<String> sendFileViaWebRTC(Device targetDevice, File file, 
                                                      BiConsumer<Long, Long> progressCallback) {
        return createConnection(targetDevice)
            .thenCompose(connection -> connection.sendFile(file, progressCallback))
            .whenComplete((result, throwable) -> {
                if (throwable != null) {
                    logger.error("WebRTC file transfer failed", throwable);
                }
            });
    }
    
    public CompletableFuture<String> sendTextViaWebRTC(Device targetDevice, String text) {
        return createConnection(targetDevice)
            .thenCompose(connection -> connection.sendText(text))
            .whenComplete((result, throwable) -> {
                if (throwable != null) {
                    logger.error("WebRTC text transfer failed", throwable);
                }
            });
    }
    
    public void closeConnection(String connectionId) {
        WebRTCConnection connection = activeConnections.remove(connectionId);
        if (connection != null) {
            connection.close();
            logger.info("WebRTC connection closed: {}", connectionId);
        }
    }
    
    public void shutdown() {
        logger.info("Shutting down WebRTC manager");
        
        activeConnections.values().forEach(WebRTCConnection::close);
        activeConnections.clear();
        
        initialized = false;
        logger.info("WebRTC manager shutdown complete");
    }
    
    private String generateConnectionId(Device device) {
        return "webrtc-" + device.getId() + "-" + System.currentTimeMillis();
    }
    
    public boolean isInitialized() {
        return initialized;
    }
    
    public int getActiveConnectionCount() {
        return activeConnections.size();
    }
}
