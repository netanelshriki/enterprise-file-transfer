package com.enterprise.filetransfer.model;

public class TransferProgress {
    private int progress;
    private String status;
    private String speed;
    private String timeRemaining;
    private long bytesTransferred;
    private long totalBytes;

    public TransferProgress(int progress, String status, String speed, String timeRemaining) {
        this.progress = progress;
        this.status = status;
        this.speed = speed;
        this.timeRemaining = timeRemaining;
    }

    public TransferProgress(long bytesTransferred, long totalBytes) {
        this.bytesTransferred = bytesTransferred;
        this.totalBytes = totalBytes;
        this.progress = totalBytes > 0 ? (int) ((bytesTransferred * 100) / totalBytes) : 0;
        this.status = "Transferring...";
        calculateSpeedAndTime();
    }

    private void calculateSpeedAndTime() {
        if (bytesTransferred > 0 && totalBytes > 0) {
            double mbTransferred = bytesTransferred / (1024.0 * 1024.0);
            double mbTotal = totalBytes / (1024.0 * 1024.0);
            
            this.speed = String.format("%.1f MB/s", mbTransferred);
            
            long remainingBytes = totalBytes - bytesTransferred;
            if (remainingBytes > 0 && mbTransferred > 0) {
                double remainingMB = remainingBytes / (1024.0 * 1024.0);
                int remainingSeconds = (int) (remainingMB / mbTransferred);
                int minutes = remainingSeconds / 60;
                int seconds = remainingSeconds % 60;
                this.timeRemaining = String.format("%02d:%02d", minutes, seconds);
            } else {
                this.timeRemaining = "00:00";
            }
        } else {
            this.speed = "0 MB/s";
            this.timeRemaining = "--:--";
        }
    }

    public int getProgress() { return progress; }
    public void setProgress(int progress) { this.progress = progress; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getSpeed() { return speed; }
    public void setSpeed(String speed) { this.speed = speed; }

    public String getTimeRemaining() { return timeRemaining; }
    public void setTimeRemaining(String timeRemaining) { this.timeRemaining = timeRemaining; }

    public long getBytesTransferred() { return bytesTransferred; }
    public void setBytesTransferred(long bytesTransferred) { this.bytesTransferred = bytesTransferred; }

    public long getTotalBytes() { return totalBytes; }
    public void setTotalBytes(long totalBytes) { this.totalBytes = totalBytes; }
}
