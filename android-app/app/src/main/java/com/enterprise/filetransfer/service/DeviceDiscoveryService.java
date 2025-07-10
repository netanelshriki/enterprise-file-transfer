package com.enterprise.filetransfer.service;

import android.util.Log;

import com.enterprise.filetransfer.model.Device;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;

public class DeviceDiscoveryService {
    private static final String TAG = "DeviceDiscoveryService";
    private static final String SERVICE_TYPE = "_enterprise-transfer._tcp.local.";
    
    private JmDNS jmdns;
    private ServiceListener serviceListener;
    private ScheduledExecutorService scheduler;
    private DeviceDiscoveryListener discoveryListener;
    private List<Device> discoveredDevices = new ArrayList<>();
    private boolean isDiscovering = false;

    public interface DeviceDiscoveryListener {
        void onDevicesDiscovered(List<Device> devices);
    }

    public void setDeviceDiscoveryListener(DeviceDiscoveryListener listener) {
        this.discoveryListener = listener;
    }

    public void startDiscovery() {
        if (isDiscovering) {
            return;
        }

        try {
            InetAddress addr = InetAddress.getLocalHost();
            jmdns = JmDNS.create(addr);
            
            serviceListener = new ServiceListener() {
                @Override
                public void serviceAdded(ServiceEvent event) {
                    Log.d(TAG, "Service added: " + event.getInfo());
                    jmdns.requestServiceInfo(event.getType(), event.getName(), 1);
                }

                @Override
                public void serviceRemoved(ServiceEvent event) {
                    Log.d(TAG, "Service removed: " + event.getInfo());
                    removeDevice(event.getName());
                }

                @Override
                public void serviceResolved(ServiceEvent event) {
                    Log.d(TAG, "Service resolved: " + event.getInfo());
                    ServiceInfo info = event.getInfo();
                    if (info != null) {
                        addDevice(info);
                    }
                }
            };

            jmdns.addServiceListener(SERVICE_TYPE, serviceListener);
            
            scheduler = Executors.newSingleThreadScheduledExecutor();
            scheduler.scheduleAtFixedRate(this::refreshDiscovery, 0, 10, TimeUnit.SECONDS);
            
            isDiscovering = true;
            Log.d(TAG, "Device discovery started");
            
            addDemoDevices();
            
        } catch (IOException e) {
            Log.e(TAG, "Failed to start device discovery", e);
        }
    }

    public void stopDiscovery() {
        if (!isDiscovering) {
            return;
        }

        try {
            if (jmdns != null) {
                if (serviceListener != null) {
                    jmdns.removeServiceListener(SERVICE_TYPE, serviceListener);
                }
                jmdns.close();
                jmdns = null;
            }
            
            if (scheduler != null && !scheduler.isShutdown()) {
                scheduler.shutdown();
            }
            
            isDiscovering = false;
            discoveredDevices.clear();
            Log.d(TAG, "Device discovery stopped");
            
        } catch (IOException e) {
            Log.e(TAG, "Error stopping device discovery", e);
        }
    }

    public void refreshDiscovery() {
        if (!isDiscovering) {
            return;
        }

        if (discoveryListener != null) {
            discoveryListener.onDevicesDiscovered(new ArrayList<>(discoveredDevices));
        }
    }

    private void addDevice(ServiceInfo serviceInfo) {
        try {
            String deviceId = serviceInfo.getPropertyString("device_id");
            String deviceName = serviceInfo.getPropertyString("device_name");
            
            if (deviceId == null || deviceName == null) {
                return;
            }

            InetAddress[] addresses = serviceInfo.getInetAddresses();
            if (addresses.length == 0) {
                return;
            }

            String ipAddress = addresses[0].getHostAddress();
            int port = serviceInfo.getPort();

            Device device = new Device(deviceId, deviceName, ipAddress, port);
            device.setStatus(Device.DeviceStatus.ONLINE);
            
            if (deviceName.toLowerCase().contains("desktop") || 
                deviceName.toLowerCase().contains("pc") ||
                deviceName.toLowerCase().contains("laptop")) {
                device.setType(Device.DeviceType.DESKTOP);
            } else if (deviceName.toLowerCase().contains("phone") ||
                       deviceName.toLowerCase().contains("mobile") ||
                       deviceName.toLowerCase().contains("android")) {
                device.setType(Device.DeviceType.MOBILE);
            }

            synchronized (discoveredDevices) {
                int existingIndex = discoveredDevices.indexOf(device);
                if (existingIndex != -1) {
                    discoveredDevices.set(existingIndex, device);
                } else {
                    discoveredDevices.add(device);
                }
            }

            if (discoveryListener != null) {
                discoveryListener.onDevicesDiscovered(new ArrayList<>(discoveredDevices));
            }

        } catch (Exception e) {
            Log.e(TAG, "Error adding device", e);
        }
    }

    private void removeDevice(String serviceName) {
        synchronized (discoveredDevices) {
            discoveredDevices.removeIf(device -> device.getName().equals(serviceName));
        }

        if (discoveryListener != null) {
            discoveryListener.onDevicesDiscovered(new ArrayList<>(discoveredDevices));
        }
    }

    private void addDemoDevices() {
        Device demoDesktop = new Device("demo-desktop-1", "Demo Desktop", "192.168.1.100", 8080);
        demoDesktop.setType(Device.DeviceType.DESKTOP);
        demoDesktop.setStatus(Device.DeviceStatus.ONLINE);

        Device demoPhone = new Device("demo-phone-1", "Demo Phone", "192.168.1.101", 8080);
        demoPhone.setType(Device.DeviceType.MOBILE);
        demoPhone.setStatus(Device.DeviceStatus.ONLINE);

        synchronized (discoveredDevices) {
            discoveredDevices.add(demoDesktop);
            discoveredDevices.add(demoPhone);
        }

        if (discoveryListener != null) {
            discoveryListener.onDevicesDiscovered(new ArrayList<>(discoveredDevices));
        }
    }
}
