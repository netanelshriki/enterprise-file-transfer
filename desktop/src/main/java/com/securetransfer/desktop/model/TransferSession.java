package com.securetransfer.desktop.model;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

public class TransferSession {
    private final String sessionId;
    private final File file;
    private final String targetDeviceId;
    private final AtomicLong bytesTransferred = new AtomicLong(0);
    private final AtomicLong totalBytes = new AtomicLong(0);
    private final AtomicBoolean cancelled = new AtomicBoolean(false);
    private final AtomicBoolean completed = new AtomicBoolean(false);
    
    private Consumer<TransferSession> progressCallback;
    private Consumer<TransferSession> completionCallback;
    
    public TransferSession(String sessionId, File file, String targetDeviceId) {
        this.sessionId = sessionId;
        this.file = file;
        this.targetDeviceId = targetDeviceId;
        this.totalBytes.set(file.length());
    }
    
    public String getSessionId() {
        return sessionId;
    }
    
    public File getFile() {
        return file;
    }
    
    public String getTargetDeviceId() {
        return targetDeviceId;
    }
    
    public long getBytesTransferred() {
        return bytesTransferred.get();
    }
    
    public long getTotalBytes() {
        return totalBytes.get();
    }
    
    public double getProgress() {
        long total = totalBytes.get();
        return total > 0 ? (double) bytesTransferred.get() / total : 0.0;
    }
    
    public boolean isCancelled() {
        return cancelled.get();
    }
    
    public boolean isCompleted() {
        return completed.get();
    }
    
    public void updateProgress(long transferred, long total) {
        bytesTransferred.set(transferred);
        if (total > 0) {
            totalBytes.set(total);
        }
        
        if (progressCallback != null) {
            progressCallback.accept(this);
        }
    }
    
    public void complete() {
        if (completed.compareAndSet(false, true)) {
            if (completionCallback != null) {
                completionCallback.accept(this);
            }
        }
    }
    
    public void cancel() {
        cancelled.set(true);
    }
    
    public void setProgressCallback(Consumer<TransferSession> callback) {
        this.progressCallback = callback;
    }
    
    public void setCompletionCallback(Consumer<TransferSession> callback) {
        this.completionCallback = callback;
    }
}
