package com.securetransfer.desktop.network;

import com.securetransfer.desktop.model.Device;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

public class EnhancedFileTransferManager {
    private static final Logger logger = LoggerFactory.getLogger(EnhancedFileTransferManager.class);
    
    private final FileTransferClient httpClient;
    private final WebRTCManager webRTCManager;
    private final DeviceDiscovery deviceDiscovery;
    
    private static final int HTTP_DIRECT_TIMEOUT_MS = 5000;
    private static final int WEBRTC_TIMEOUT_MS = 10000;
    
    public EnhancedFileTransferManager(FileTransferClient httpClient, 
                                     WebRTCManager webRTCManager,
                                     DeviceDiscovery deviceDiscovery) {
        this.httpClient = httpClient;
        this.webRTCManager = webRTCManager;
        this.deviceDiscovery = deviceDiscovery;
    }
    
    public CompletableFuture<String> sendFile(Device targetDevice, File file, 
                                            BiConsumer<Long, Long> progressCallback) {
        logger.info("Starting enhanced file transfer to {}: {}", targetDevice.getName(), file.getName());
        
        return attemptLocalTransfer(targetDevice, file, progressCallback)
            .exceptionally(throwable -> {
                logger.debug("Local transfer failed, trying HTTP/2 direct", throwable);
                return null;
            })
            .thenCompose(result -> {
                if (result != null) {
                    return CompletableFuture.completedFuture(result);
                }
                return attemptHttpDirectTransfer(targetDevice, file, progressCallback);
            })
            .exceptionally(throwable -> {
                logger.debug("HTTP/2 direct transfer failed, trying WebRTC P2P", throwable);
                return null;
            })
            .thenCompose(result -> {
                if (result != null) {
                    return CompletableFuture.completedFuture(result);
                }
                return attemptWebRTCTransfer(targetDevice, file, progressCallback);
            })
            .exceptionally(throwable -> {
                logger.debug("WebRTC P2P transfer failed, trying HTTP/2 relay", throwable);
                return null;
            })
            .thenCompose(result -> {
                if (result != null) {
                    return CompletableFuture.completedFuture(result);
                }
                return attemptHttpRelayTransfer(targetDevice, file, progressCallback);
            })
            .whenComplete((result, throwable) -> {
                if (result != null) {
                    logger.info("File transfer completed successfully: {}", result);
                } else {
                    logger.error("All transfer methods failed for file: {}", file.getName(), throwable);
                }
            });
    }
    
    public CompletableFuture<String> sendText(Device targetDevice, String text) {
        logger.info("Starting enhanced text transfer to {}: {} characters", targetDevice.getName(), text.length());
        
        return attemptLocalTextTransfer(targetDevice, text)
            .exceptionally(throwable -> {
                logger.debug("Local text transfer failed, trying HTTP/2 direct", throwable);
                return null;
            })
            .thenCompose(result -> {
                if (result != null) {
                    return CompletableFuture.completedFuture(result);
                }
                return attemptHttpDirectTextTransfer(targetDevice, text);
            })
            .exceptionally(throwable -> {
                logger.debug("HTTP/2 direct text transfer failed, trying WebRTC P2P", throwable);
                return null;
            })
            .thenCompose(result -> {
                if (result != null) {
                    return CompletableFuture.completedFuture(result);
                }
                return attemptWebRTCTextTransfer(targetDevice, text);
            })
            .exceptionally(throwable -> {
                logger.debug("WebRTC P2P text transfer failed, trying HTTP/2 relay", throwable);
                return null;
            })
            .thenCompose(result -> {
                if (result != null) {
                    return CompletableFuture.completedFuture(result);
                }
                return attemptHttpRelayTextTransfer(targetDevice, text);
            });
    }
    
    private CompletableFuture<String> attemptLocalTransfer(Device targetDevice, File file, 
                                                         BiConsumer<Long, Long> progressCallback) {
        if (!isLocalDevice(targetDevice)) {
            return CompletableFuture.failedFuture(new RuntimeException("Device not on local network"));
        }
        
        logger.debug("Attempting local network transfer to {}", targetDevice.getName());
        return httpClient.sendFile(targetDevice.getIpAddress(), targetDevice.getPort(), file, progressCallback)
            .orTimeout(HTTP_DIRECT_TIMEOUT_MS, TimeUnit.MILLISECONDS);
    }
    
    private CompletableFuture<String> attemptHttpDirectTransfer(Device targetDevice, File file, 
                                                              BiConsumer<Long, Long> progressCallback) {
        logger.debug("Attempting HTTP/2 direct transfer to {}", targetDevice.getName());
        return httpClient.sendFile(targetDevice.getIpAddress(), targetDevice.getPort(), file, progressCallback)
            .orTimeout(HTTP_DIRECT_TIMEOUT_MS, TimeUnit.MILLISECONDS);
    }
    
    private CompletableFuture<String> attemptWebRTCTransfer(Device targetDevice, File file, 
                                                          BiConsumer<Long, Long> progressCallback) {
        if (!webRTCManager.isInitialized()) {
            return CompletableFuture.failedFuture(new RuntimeException("WebRTC not initialized"));
        }
        
        logger.debug("Attempting WebRTC P2P transfer to {}", targetDevice.getName());
        return webRTCManager.sendFileViaWebRTC(targetDevice, file, progressCallback)
            .orTimeout(WEBRTC_TIMEOUT_MS, TimeUnit.MILLISECONDS);
    }
    
    private CompletableFuture<String> attemptHttpRelayTransfer(Device targetDevice, File file, 
                                                             BiConsumer<Long, Long> progressCallback) {
        logger.debug("Attempting HTTP/2 relay transfer to {}", targetDevice.getName());
        
        String relayServerUrl = "https://relay.securetransfer.com";
        return httpClient.sendFile(relayServerUrl, 443, file, progressCallback)
            .orTimeout(30000, TimeUnit.MILLISECONDS);
    }
    
    private CompletableFuture<String> attemptLocalTextTransfer(Device targetDevice, String text) {
        if (!isLocalDevice(targetDevice)) {
            return CompletableFuture.failedFuture(new RuntimeException("Device not on local network"));
        }
        
        logger.debug("Attempting local network text transfer to {}", targetDevice.getName());
        return httpClient.sendText(targetDevice.getIpAddress(), targetDevice.getPort(), text);
    }
    
    private CompletableFuture<String> attemptHttpDirectTextTransfer(Device targetDevice, String text) {
        logger.debug("Attempting HTTP/2 direct text transfer to {}", targetDevice.getName());
        return httpClient.sendText(targetDevice.getIpAddress(), targetDevice.getPort(), text);
    }
    
    private CompletableFuture<String> attemptWebRTCTextTransfer(Device targetDevice, String text) {
        if (!webRTCManager.isInitialized()) {
            return CompletableFuture.failedFuture(new RuntimeException("WebRTC not initialized"));
        }
        
        logger.debug("Attempting WebRTC P2P text transfer to {}", targetDevice.getName());
        return webRTCManager.sendTextViaWebRTC(targetDevice, text);
    }
    
    private CompletableFuture<String> attemptHttpRelayTextTransfer(Device targetDevice, String text) {
        logger.debug("Attempting HTTP/2 relay text transfer to {}", targetDevice.getName());
        
        String relayServerUrl = "https://relay.securetransfer.com";
        return httpClient.sendText(relayServerUrl, 443, text);
    }
    
    private boolean isLocalDevice(Device targetDevice) {
        return deviceDiscovery.getDiscoveredDevices().containsValue(targetDevice);
    }
    
    public void shutdown() {
        logger.info("Shutting down enhanced file transfer manager");
        webRTCManager.shutdown();
    }
}
