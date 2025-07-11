package com.securetransfer.server.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.securetransfer.server.model.Device;
import com.securetransfer.server.model.SignalingMessage;
import com.securetransfer.server.service.DeviceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SignalingWebSocketHandler implements WebSocketHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(SignalingWebSocketHandler.class);
    
    @Autowired
    private DeviceService deviceService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    private final ConcurrentHashMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String sessionId = session.getId();
        sessions.put(sessionId, session);
        logger.info("WebSocket connection established: {}", sessionId);
        
        sendMessage(session, new SignalingMessage("connection", "established", sessionId, null));
    }
    
    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        String sessionId = session.getId();
        String payload = message.getPayload().toString();
        
        logger.debug("Received message from {}: {}", sessionId, payload);
        
        try {
            SignalingMessage signalingMessage = objectMapper.readValue(payload, SignalingMessage.class);
            handleSignalingMessage(session, signalingMessage);
        } catch (Exception e) {
            logger.error("Error processing message from {}: {}", sessionId, payload, e);
            sendError(session, "Invalid message format");
        }
    }
    
    private void handleSignalingMessage(WebSocketSession session, SignalingMessage message) throws IOException {
        String sessionId = session.getId();
        
        switch (message.getType()) {
            case "register":
                handleDeviceRegistration(session, message);
                break;
                
            case "discover":
                handleDeviceDiscovery(session, message);
                break;
                
            case "offer":
            case "answer":
            case "ice-candidate":
                handleWebRTCSignaling(session, message);
                break;
                
            case "ping":
                handlePing(session, message);
                break;
                
            default:
                logger.warn("Unknown message type from {}: {}", sessionId, message.getType());
                sendError(session, "Unknown message type: " + message.getType());
        }
    }
    
    private void handleDeviceRegistration(WebSocketSession session, SignalingMessage message) throws IOException {
        String sessionId = session.getId();
        
        try {
            Device device = objectMapper.convertValue(message.getData(), Device.class);
            device.setSessionId(sessionId);
            device.setLastSeen(System.currentTimeMillis());
            device.setStatus(Device.Status.ONLINE);
            
            deviceService.registerDevice(device);
            
            logger.info("Device registered: {} ({})", device.getName(), device.getId());
            sendMessage(session, new SignalingMessage("registered", "success", device.getId(), device));
            
            broadcastDeviceUpdate("device-online", device);
            
        } catch (Exception e) {
            logger.error("Error registering device from session {}", sessionId, e);
            sendError(session, "Device registration failed");
        }
    }
    
    private void handleDeviceDiscovery(WebSocketSession session, SignalingMessage message) throws IOException {
        String sessionId = session.getId();
        
        try {
            var onlineDevices = deviceService.getOnlineDevices();
            sendMessage(session, new SignalingMessage("devices", "list", sessionId, onlineDevices));
            
            logger.debug("Sent device list to {}: {} devices", sessionId, onlineDevices.size());
            
        } catch (Exception e) {
            logger.error("Error handling device discovery from session {}", sessionId, e);
            sendError(session, "Device discovery failed");
        }
    }
    
    private void handleWebRTCSignaling(WebSocketSession session, SignalingMessage message) throws IOException {
        String targetDeviceId = message.getTargetId();
        
        if (targetDeviceId == null) {
            sendError(session, "Target device ID required for WebRTC signaling");
            return;
        }
        
        Device targetDevice = deviceService.getDevice(targetDeviceId);
        if (targetDevice == null || targetDevice.getStatus() != Device.Status.ONLINE) {
            sendError(session, "Target device not available");
            return;
        }
        
        WebSocketSession targetSession = sessions.get(targetDevice.getSessionId());
        if (targetSession == null || !targetSession.isOpen()) {
            sendError(session, "Target device session not available");
            deviceService.setDeviceOffline(targetDeviceId);
            return;
        }
        
        try {
            String sourceDeviceId = deviceService.getDeviceBySessionId(session.getId()).getId();
            message.setSourceId(sourceDeviceId);
            
            sendMessage(targetSession, message);
            logger.debug("Forwarded {} from {} to {}", message.getType(), sourceDeviceId, targetDeviceId);
            
        } catch (Exception e) {
            logger.error("Error forwarding WebRTC signaling", e);
            sendError(session, "Failed to forward signaling message");
        }
    }
    
    private void handlePing(WebSocketSession session, SignalingMessage message) throws IOException {
        String sessionId = session.getId();
        Device device = deviceService.getDeviceBySessionId(sessionId);
        
        if (device != null) {
            device.setLastSeen(System.currentTimeMillis());
            deviceService.updateDevice(device);
        }
        
        sendMessage(session, new SignalingMessage("pong", "success", sessionId, null));
    }
    
    private void sendMessage(WebSocketSession session, SignalingMessage message) throws IOException {
        if (session.isOpen()) {
            String json = objectMapper.writeValueAsString(message);
            session.sendMessage(new TextMessage(json));
        }
    }
    
    private void sendError(WebSocketSession session, String error) throws IOException {
        SignalingMessage errorMessage = new SignalingMessage("error", error, session.getId(), null);
        sendMessage(session, errorMessage);
    }
    
    private void broadcastDeviceUpdate(String type, Device device) {
        SignalingMessage message = new SignalingMessage(type, "broadcast", null, device);
        
        sessions.values().parallelStream()
                .filter(WebSocketSession::isOpen)
                .forEach(session -> {
                    try {
                        sendMessage(session, message);
                    } catch (IOException e) {
                        logger.warn("Failed to broadcast to session {}", session.getId(), e);
                    }
                });
    }
    
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        String sessionId = session.getId();
        logger.error("WebSocket transport error for session {}", sessionId, exception);
        
        handleConnectionClosed(session);
    }
    
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        String sessionId = session.getId();
        logger.info("WebSocket connection closed: {} ({})", sessionId, closeStatus);
        
        handleConnectionClosed(session);
    }
    
    private void handleConnectionClosed(WebSocketSession session) {
        String sessionId = session.getId();
        sessions.remove(sessionId);
        
        Device device = deviceService.getDeviceBySessionId(sessionId);
        if (device != null) {
            deviceService.setDeviceOffline(device.getId());
            broadcastDeviceUpdate("device-offline", device);
            logger.info("Device went offline: {} ({})", device.getName(), device.getId());
        }
    }
    
    @Override
    public boolean supportsPartialMessages() {
        return false;
    }
}
