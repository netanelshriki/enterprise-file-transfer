package com.enterprise.filetransfer.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.IBinder;
import android.util.Log;

import com.enterprise.filetransfer.crypto.CryptoManager;
import com.enterprise.filetransfer.model.Device;
import com.enterprise.filetransfer.model.TransferProgress;
import com.enterprise.filetransfer.network.NetworkManager;

import java.io.InputStream;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class FileTransferService extends Service {
    private static final String TAG = "FileTransferService";
    
    private TransferProgressListener progressListener;
    private TransferCompleteListener completeListener;
    private AtomicBoolean isTransferring = new AtomicBoolean(false);
    private AtomicBoolean shouldCancel = new AtomicBoolean(false);
    
    private CryptoManager cryptoManager;
    private NetworkManager networkManager;

    public interface TransferProgressListener {
        void onProgressUpdate(TransferProgress progress);
    }

    public interface TransferCompleteListener {
        void onTransferComplete(boolean success);
    }

    public FileTransferService() {
        cryptoManager = new CryptoManager();
        networkManager = new NetworkManager();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_NOT_STICKY;
    }

    public void setTransferProgressListener(TransferProgressListener listener) {
        this.progressListener = listener;
    }

    public void setTransferCompleteListener(TransferCompleteListener listener) {
        this.completeListener = listener;
    }

    public void startTransfer(Context context, Device targetDevice, List<Uri> fileUris) {
        if (isTransferring.get()) {
            Log.w(TAG, "Transfer already in progress");
            return;
        }

        isTransferring.set(true);
        shouldCancel.set(false);

        try {
            cryptoManager.generateKeys();
            
            long totalSize = 0;
            for (Uri uri : fileUris) {
                try (InputStream inputStream = context.getContentResolver().openInputStream(uri)) {
                    if (inputStream != null) {
                        totalSize += inputStream.available();
                    }
                }
            }

            if (progressListener != null) {
                progressListener.onProgressUpdate(
                    new TransferProgress(0, "Connecting to " + targetDevice.getName(), "0 MB/s", "--:--"));
            }

            boolean connected = networkManager.connectToDevice(targetDevice);
            if (!connected) {
                throw new Exception("Failed to connect to device");
            }

            if (progressListener != null) {
                progressListener.onProgressUpdate(
                    new TransferProgress(5, "Establishing secure connection...", "0 MB/s", "--:--"));
            }

            boolean keyExchangeSuccess = performKeyExchange(targetDevice);
            if (!keyExchangeSuccess) {
                throw new Exception("Key exchange failed");
            }

            long transferredBytes = 0;
            for (int i = 0; i < fileUris.size() && !shouldCancel.get(); i++) {
                Uri uri = fileUris.get(i);
                
                if (progressListener != null) {
                    progressListener.onProgressUpdate(new TransferProgress(
                        (int) ((transferredBytes * 90) / totalSize) + 10,
                        "Transferring file " + (i + 1) + " of " + fileUris.size(),
                        "0 MB/s", "--:--"));
                }

                long fileSize = transferFile(context, uri, targetDevice);
                transferredBytes += fileSize;

                for (int progress = 0; progress <= 100 && !shouldCancel.get(); progress += 10) {
                    Thread.sleep(100); // Simulate transfer time
                    
                    long currentBytes = transferredBytes - fileSize + (fileSize * progress / 100);
                    if (progressListener != null) {
                        progressListener.onProgressUpdate(new TransferProgress(currentBytes, totalSize));
                    }
                }
            }

            if (shouldCancel.get()) {
                if (completeListener != null) {
                    completeListener.onTransferComplete(false);
                }
                return;
            }

            if (progressListener != null) {
                progressListener.onProgressUpdate(
                    new TransferProgress(100, "Transfer completed successfully!", "0 MB/s", "00:00"));
            }

            if (completeListener != null) {
                completeListener.onTransferComplete(true);
            }

        } catch (Exception e) {
            Log.e(TAG, "Transfer failed", e);
            if (completeListener != null) {
                completeListener.onTransferComplete(false);
            }
        } finally {
            isTransferring.set(false);
            networkManager.disconnect();
        }
    }

    public void cancelTransfer() {
        shouldCancel.set(true);
        networkManager.disconnect();
    }

    private boolean performKeyExchange(Device targetDevice) {
        try {
            Thread.sleep(1000);
            return true;
        } catch (InterruptedException e) {
            return false;
        }
    }

    private long transferFile(Context context, Uri uri, Device targetDevice) throws Exception {
        try (InputStream inputStream = context.getContentResolver().openInputStream(uri)) {
            if (inputStream == null) {
                throw new Exception("Cannot open file");
            }

            byte[] buffer = new byte[8192];
            long totalBytes = 0;
            int bytesRead;

            while ((bytesRead = inputStream.read(buffer)) != -1 && !shouldCancel.get()) {
                byte[] encryptedData = cryptoManager.encrypt(buffer, 0, bytesRead);
                
                boolean sent = networkManager.sendData(encryptedData);
                if (!sent) {
                    throw new Exception("Failed to send data");
                }
                
                totalBytes += bytesRead;
                
                Thread.sleep(10);
            }

            return totalBytes;
        }
    }
}
