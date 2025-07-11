package com.securetransfer.desktop.network;

import com.securetransfer.desktop.model.Device;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class DeviceDiscovery {
    private static final Logger logger = LoggerFactory.getLogger(DeviceDiscovery.class);
    private static final String SERVICE_TYPE = "_securetransfer._tcp.local.";
    
    private JmDNS jmdns;
    private ServiceInfo serviceInfo;
    private final Map<String, Device> discoveredDevices = new ConcurrentHashMap<>();
    private final CopyOnWriteArrayList<Consumer<Device>> deviceAddedListeners = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<Consumer<Device>> deviceRemovedListeners = new CopyOnWriteArrayList<>();
    
    public void start() throws IOException {
        logger.info("Starting mDNS device discovery");
        
        InetAddress addr = InetAddress.getLocalHost();
        jmdns = JmDNS.create(addr);
        
        String deviceName = System.getProperty("user.name") + "'s " + 
                           System.getProperty("os.name").split(" ")[0];
        String deviceId = SecurityUtils.generateDeviceId();
        
        serviceInfo = ServiceInfo.create(SERVICE_TYPE, deviceName, 8080, 
            "deviceId=" + deviceId + ",platform=desktop");
        jmdns.registerService(serviceInfo);
        
        jmdns.addServiceListener(SERVICE_TYPE, new ServiceListener() {
            @Override
            public void serviceAdded(ServiceEvent event) {
                logger.debug("Service added: {}", event.getInfo().getName());
                jmdns.requestServiceInfo(event.getType(), event.getName(), 1000);
            }
            
            @Override
            public void serviceRemoved(ServiceEvent event) {
                logger.debug("Service removed: {}", event.getInfo().getName());
                String deviceId = event.getInfo().getPropertyString("deviceId");
                if (deviceId != null) {
                    Device device = discoveredDevices.remove(deviceId);
                    if (device != null) {
                        notifyDeviceRemoved(device);
                    }
                }
            }
            
            @Override
            public void serviceResolved(ServiceEvent event) {
                ServiceInfo info = event.getInfo();
                logger.debug("Service resolved: {}", info.getName());
                
                String deviceId = info.getPropertyString("deviceId");
                String platform = info.getPropertyString("platform");
                
                if (deviceId != null && !deviceId.equals(SecurityUtils.getLocalDeviceId())) {
                    Device device = new Device(
                        deviceId,
                        info.getName(),
                        "desktop".equals(platform) ? Device.Type.DESKTOP : Device.Type.MOBILE,
                        info.getInetAddresses()[0].getHostAddress(),
                        info.getPort(),
                        Device.Status.ONLINE,
                        System.currentTimeMillis()
                    );
                    
                    discoveredDevices.put(deviceId, device);
                    notifyDeviceAdded(device);
                }
            }
        });
        
        logger.info("mDNS service started on {}", addr.getHostAddress());
    }
    
    public void stop() {
        logger.info("Stopping mDNS device discovery");
        
        if (jmdns != null) {
            try {
                if (serviceInfo != null) {
                    jmdns.unregisterService(serviceInfo);
                }
                jmdns.close();
            } catch (IOException e) {
                logger.error("Error stopping mDNS service", e);
            }
        }
    }
    
    public void addDeviceAddedListener(Consumer<Device> listener) {
        deviceAddedListeners.add(listener);
    }
    
    public void addDeviceRemovedListener(Consumer<Device> listener) {
        deviceRemovedListeners.add(listener);
    }
    
    private void notifyDeviceAdded(Device device) {
        logger.info("Device discovered: {} at {}:{}", device.getName(), 
                   device.getIpAddress(), device.getPort());
        deviceAddedListeners.forEach(listener -> {
            try {
                listener.accept(device);
            } catch (Exception e) {
                logger.error("Error notifying device added listener", e);
            }
        });
    }
    
    private void notifyDeviceRemoved(Device device) {
        logger.info("Device removed: {}", device.getName());
        deviceRemovedListeners.forEach(listener -> {
            try {
                listener.accept(device);
            } catch (Exception e) {
                logger.error("Error notifying device removed listener", e);
            }
        });
    }
    
    public Map<String, Device> getDiscoveredDevices() {
        return new ConcurrentHashMap<>(discoveredDevices);
    }
}
