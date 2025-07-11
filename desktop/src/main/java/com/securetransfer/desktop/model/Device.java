package com.securetransfer.desktop.model;

import java.util.Objects;

public class Device {
    public enum Type {
        DESKTOP, MOBILE
    }
    
    public enum Status {
        ONLINE, OFFLINE
    }
    
    private final String id;
    private final String name;
    private final Type type;
    private final String ipAddress;
    private final int port;
    private final Status status;
    private final long lastSeen;
    
    public Device(String id, String name, Type type, String ipAddress, int port, Status status, long lastSeen) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.ipAddress = ipAddress;
        this.port = port;
        this.status = status;
        this.lastSeen = lastSeen;
    }
    
    public String getId() {
        return id;
    }
    
    public String getName() {
        return name;
    }
    
    public Type getType() {
        return type;
    }
    
    public String getIpAddress() {
        return ipAddress;
    }
    
    public int getPort() {
        return port;
    }
    
    public Status getStatus() {
        return status;
    }
    
    public long getLastSeen() {
        return lastSeen;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Device device = (Device) o;
        return Objects.equals(id, device.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return name + " (" + type + ")";
    }
}
