package com.securetransfer.server.service;

import com.securetransfer.server.model.Device;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class DeviceService {
    
    private static final Logger logger = LoggerFactory.getLogger(DeviceService.class);
    private static final long DEVICE_TIMEOUT_MS = 60000; // 1 minute
    
    private final ConcurrentHashMap<String, Device> devices = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> sessionToDeviceMap = new ConcurrentHashMap<>();
    
    public void registerDevice(Device device) {
        devices.put(device.getId(), device);
        sessionToDeviceMap.put(device.getSessionId(), device.getId());
        
        logger.info("Device registered: {} ({})", device.getName(), device.getId());
    }
    
    public void updateDevice(Device device) {
        if (devices.containsKey(device.getId())) {
            devices.put(device.getId(), device);
            logger.debug("Device updated: {} ({})", device.getName(), device.getId());
        }
    }
    
    public Device getDevice(String deviceId) {
        return devices.get(deviceId);
    }
    
    public Device getDeviceBySessionId(String sessionId) {
        String deviceId = sessionToDeviceMap.get(sessionId);
        return deviceId != null ? devices.get(deviceId) : null;
    }
    
    public Collection<Device> getOnlineDevices() {
        return devices.values().stream()
                .filter(device -> device.getStatus() == Device.Status.ONLINE)
                .toList();
    }
    
    public Collection<Device> getAllDevices() {
        return devices.values();
    }
    
    public void setDeviceOffline(String deviceId) {
        Device device = devices.get(deviceId);
        if (device != null) {
            device.setStatus(Device.Status.OFFLINE);
            device.setLastSeen(System.currentTimeMillis());
            
            sessionToDeviceMap.remove(device.getSessionId());
            
            logger.info("Device set offline: {} ({})", device.getName(), device.getId());
        }
    }
    
    public void removeDevice(String deviceId) {
        Device device = devices.remove(deviceId);
        if (device != null) {
            sessionToDeviceMap.remove(device.getSessionId());
            logger.info("Device removed: {} ({})", device.getName(), device.getId());
        }
    }
    
    @Scheduled(fixedRate = 30000) // Run every 30 seconds
    public void cleanupStaleDevices() {
        long currentTime = System.currentTimeMillis();
        
        devices.values().removeIf(device -> {
            if (device.getStatus() == Device.Status.ONLINE && 
                (currentTime - device.getLastSeen()) > DEVICE_TIMEOUT_MS) {
                
                sessionToDeviceMap.remove(device.getSessionId());
                logger.info("Removed stale device: {} ({})", device.getName(), device.getId());
                return true;
            }
            return false;
        });
    }
    
    public int getOnlineDeviceCount() {
        return (int) devices.values().stream()
                .filter(device -> device.getStatus() == Device.Status.ONLINE)
                .count();
    }
    
    public int getTotalDeviceCount() {
        return devices.size();
    }
}
