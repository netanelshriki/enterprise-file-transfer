package com.securetransfer.server.model;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class SignalingMessage {
    private String type;
    private String action;
    private String sourceId;
    private String targetId;
    private Object data;
    private long timestamp;
    
    public SignalingMessage() {
        this.timestamp = System.currentTimeMillis();
    }
    
    public SignalingMessage(String type, String action, String targetId, Object data) {
        this();
        this.type = type;
        this.action = action;
        this.targetId = targetId;
        this.data = data;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public String getAction() {
        return action;
    }
    
    public void setAction(String action) {
        this.action = action;
    }
    
    public String getSourceId() {
        return sourceId;
    }
    
    public void setSourceId(String sourceId) {
        this.sourceId = sourceId;
    }
    
    public String getTargetId() {
        return targetId;
    }
    
    public void setTargetId(String targetId) {
        this.targetId = targetId;
    }
    
    public Object getData() {
        return data;
    }
    
    public void setData(Object data) {
        this.data = data;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
