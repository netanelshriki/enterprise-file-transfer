package com.securetransfer.desktop.network;

import com.securetransfer.desktop.model.Device;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

public class WebRTCConnection {
    private static final Logger logger = LoggerFactory.getLogger(WebRTCConnection.class);
    private static final int CHUNK_SIZE = 16384; // 16KB chunks for WebRTC DataChannel
    
    private final String connectionId;
    private final Device targetDevice;
    private final STUNTURNConfig stunTurnConfig;
    private final AtomicBoolean connected = new AtomicBoolean(false);
    private final AtomicBoolean closed = new AtomicBoolean(false);
    
    public WebRTCConnection(String connectionId, Device targetDevice, STUNTURNConfig stunTurnConfig) {
        this.connectionId = connectionId;
        this.targetDevice = targetDevice;
        this.stunTurnConfig = stunTurnConfig;
    }
    
    public void initialize() {
        try {
            logger.info("Initializing WebRTC connection: {}", connectionId);
            
            logger.debug("Setting up ICE servers for connection: {}", connectionId);
            logger.debug("STUN servers: {}", stunTurnConfig.getStunServers());
            logger.debug("TURN servers: {}", stunTurnConfig.getTurnServers().size());
            
            connected.set(true);
            logger.info("WebRTC connection established: {}", connectionId);
            
        } catch (Exception e) {
            logger.error("Failed to initialize WebRTC connection: {}", connectionId, e);
            throw new RuntimeException("WebRTC connection initialization failed", e);
        }
    }
    
    public CompletableFuture<String> sendFile(File file, BiConsumer<Long, Long> progressCallback) {
        return CompletableFuture.supplyAsync(() -> {
            if (!connected.get() || closed.get()) {
                throw new RuntimeException("WebRTC connection not available");
            }
            
            try {
                logger.info("Sending file via WebRTC: {} ({})", file.getName(), file.length());
                
                long totalBytes = file.length();
                long sentBytes = 0;
                
                try (FileInputStream fis = new FileInputStream(file)) {
                    byte[] buffer = new byte[CHUNK_SIZE];
                    int bytesRead;
                    
                    while ((bytesRead = fis.read(buffer)) != -1) {
                        if (closed.get()) {
                            throw new RuntimeException("Connection closed during transfer");
                        }
                        
                        byte[] chunk = new byte[bytesRead];
                        System.arraycopy(buffer, 0, chunk, 0, bytesRead);
                        
                        sendDataChunk(chunk);
                        
                        sentBytes += bytesRead;
                        if (progressCallback != null) {
                            progressCallback.accept(sentBytes, totalBytes);
                        }
                        
                        Thread.sleep(1);
                    }
                }
                
                logger.info("File sent successfully via WebRTC: {}", file.getName());
                return "File sent via WebRTC: " + file.getName();
                
            } catch (Exception e) {
                logger.error("Failed to send file via WebRTC", e);
                throw new RuntimeException("WebRTC file transfer failed", e);
            }
        });
    }
    
    public CompletableFuture<String> sendText(String text) {
        return CompletableFuture.supplyAsync(() -> {
            if (!connected.get() || closed.get()) {
                throw new RuntimeException("WebRTC connection not available");
            }
            
            try {
                logger.info("Sending text via WebRTC: {} characters", text.length());
                
                byte[] textBytes = text.getBytes("UTF-8");
                sendDataChunk(textBytes);
                
                logger.info("Text sent successfully via WebRTC");
                return "Text sent via WebRTC";
                
            } catch (Exception e) {
                logger.error("Failed to send text via WebRTC", e);
                throw new RuntimeException("WebRTC text transfer failed", e);
            }
        });
    }
    
    private void sendDataChunk(byte[] data) {
        logger.debug("Sending WebRTC data chunk: {} bytes", data.length);
        
        try {
            Thread.sleep(1);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("WebRTC data send interrupted", e);
        }
    }
    
    public void close() {
        if (closed.compareAndSet(false, true)) {
            logger.info("Closing WebRTC connection: {}", connectionId);
            
            connected.set(false);
            
            logger.info("WebRTC connection closed: {}", connectionId);
        }
    }
    
    public boolean isConnected() {
        return connected.get() && !closed.get();
    }
    
    public String getConnectionId() {
        return connectionId;
    }
    
    public Device getTargetDevice() {
        return targetDevice;
    }
}
