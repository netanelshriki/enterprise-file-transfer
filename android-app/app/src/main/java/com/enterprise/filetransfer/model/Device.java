package com.enterprise.filetransfer.model;

public class Device {
    private String id;
    private String name;
    private String ipAddress;
    private int port;
    private DeviceStatus status;
    private DeviceType type;
    private long lastSeen;

    public enum DeviceStatus {
        ONLINE, OFFLINE, CONNECTING, CONNECTED
    }

    public enum DeviceType {
        DESKTOP, MOBILE, UNKNOWN
    }

    public Device(String id, String name, String ipAddress, int port) {
        this.id = id;
        this.name = name;
        this.ipAddress = ipAddress;
        this.port = port;
        this.status = DeviceStatus.ONLINE;
        this.type = DeviceType.UNKNOWN;
        this.lastSeen = System.currentTimeMillis();
    }
    
    public boolean isLocalDevice() {
        return ipAddress != null && !ipAddress.isEmpty();
    }
    
    public boolean isInternetDevice() {
        return !isLocalDevice();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }

    public DeviceStatus getStatus() { return status; }
    public void setStatus(DeviceStatus status) { this.status = status; }

    public DeviceType getType() { return type; }
    public void setType(DeviceType type) { this.type = type; }

    public long getLastSeen() { return lastSeen; }
    public void setLastSeen(long lastSeen) { this.lastSeen = lastSeen; }

    public String getStatusText() {
        switch (status) {
            case ONLINE: return "Online";
            case OFFLINE: return "Offline";
            case CONNECTING: return "Connecting";
            case CONNECTED: return "Connected";
            default: return "Unknown";
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Device device = (Device) obj;
        return id != null ? id.equals(device.id) : device.id == null;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}
