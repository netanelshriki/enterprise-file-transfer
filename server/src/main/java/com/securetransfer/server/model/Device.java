package com.securetransfer.server.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class Device {
    public enum Type {
        DESKTOP, MOBILE
    }
    
    public enum Status {
        ONLINE, OFFLINE
    }
    
    private String id;
    private String name;
    private Type type;
    private String ipAddress;
    private int port;
    private Status status;
    private long lastSeen;
    
    @JsonIgnore
    private String sessionId;
    
    public Device() {}
    
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
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public Type getType() {
        return type;
    }
    
    public void setType(Type type) {
        this.type = type;
    }
    
    public String getIpAddress() {
        return ipAddress;
    }
    
    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }
    
    public int getPort() {
        return port;
    }
    
    public void setPort(int port) {
        this.port = port;
    }
    
    public Status getStatus() {
        return status;
    }
    
    public void setStatus(Status status) {
        this.status = status;
    }
    
    public long getLastSeen() {
        return lastSeen;
    }
    
    public void setLastSeen(long lastSeen) {
        this.lastSeen = lastSeen;
    }
    
    public String getSessionId() {
        return sessionId;
    }
    
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
    
    @Override
    public String toString() {
        return name + " (" + type + ")";
    }
}
