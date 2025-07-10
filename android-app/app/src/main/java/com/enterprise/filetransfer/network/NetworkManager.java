package com.enterprise.filetransfer.network;

import android.util.Log;

import com.enterprise.filetransfer.model.Device;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

public class NetworkManager {
    private static final String TAG = "NetworkManager";
    private static final int CONNECTION_TIMEOUT = 10000; // 10 seconds
    private static final int READ_TIMEOUT = 30000; // 30 seconds

    private Socket socket;
    private InputStream inputStream;
    private OutputStream outputStream;
    private AtomicBoolean isConnected = new AtomicBoolean(false);

    public boolean connectToDevice(Device device) {
        try {
            Log.d(TAG, "Connecting to device: " + device.getName() + " at " + device.getIpAddress());
            
            socket = new Socket();
            socket.connect(new java.net.InetSocketAddress(device.getIpAddress(), device.getPort()), CONNECTION_TIMEOUT);
            socket.setSoTimeout(READ_TIMEOUT);
            
            inputStream = socket.getInputStream();
            outputStream = socket.getOutputStream();
            
            isConnected.set(true);
            Log.d(TAG, "Successfully connected to device: " + device.getName());
            return true;
            
        } catch (SocketTimeoutException e) {
            Log.e(TAG, "Connection timeout to device: " + device.getName(), e);
            return false;
        } catch (IOException e) {
            Log.e(TAG, "Failed to connect to device: " + device.getName(), e);
            return false;
        }
    }

    public boolean sendData(byte[] data) {
        if (!isConnected.get() || outputStream == null) {
            Log.e(TAG, "Not connected to device");
            return false;
        }

        try {
            byte[] lengthBytes = intToBytes(data.length);
            outputStream.write(lengthBytes);
            
            outputStream.write(data);
            outputStream.flush();
            
            return true;
            
        } catch (IOException e) {
            Log.e(TAG, "Failed to send data", e);
            isConnected.set(false);
            return false;
        }
    }

    public byte[] receiveData() {
        if (!isConnected.get() || inputStream == null) {
            Log.e(TAG, "Not connected to device");
            return null;
        }

        try {
            byte[] lengthBytes = new byte[4];
            int bytesRead = 0;
            while (bytesRead < 4) {
                int read = inputStream.read(lengthBytes, bytesRead, 4 - bytesRead);
                if (read == -1) {
                    Log.e(TAG, "Connection closed while reading length");
                    isConnected.set(false);
                    return null;
                }
                bytesRead += read;
            }
            
            int dataLength = bytesFromInt(lengthBytes);
            if (dataLength <= 0 || dataLength > 100 * 1024 * 1024) { // Max 100MB
                Log.e(TAG, "Invalid data length: " + dataLength);
                return null;
            }
            
            byte[] data = new byte[dataLength];
            bytesRead = 0;
            while (bytesRead < dataLength) {
                int read = inputStream.read(data, bytesRead, dataLength - bytesRead);
                if (read == -1) {
                    Log.e(TAG, "Connection closed while reading data");
                    isConnected.set(false);
                    return null;
                }
                bytesRead += read;
            }
            
            return data;
            
        } catch (SocketTimeoutException e) {
            Log.e(TAG, "Timeout while receiving data", e);
            return null;
        } catch (IOException e) {
            Log.e(TAG, "Failed to receive data", e);
            isConnected.set(false);
            return null;
        }
    }

    public void disconnect() {
        isConnected.set(false);
        
        try {
            if (inputStream != null) {
                inputStream.close();
                inputStream = null;
            }
        } catch (IOException e) {
            Log.e(TAG, "Error closing input stream", e);
        }
        
        try {
            if (outputStream != null) {
                outputStream.close();
                outputStream = null;
            }
        } catch (IOException e) {
            Log.e(TAG, "Error closing output stream", e);
        }
        
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
                socket = null;
            }
        } catch (IOException e) {
            Log.e(TAG, "Error closing socket", e);
        }
        
        Log.d(TAG, "Disconnected from device");
    }

    public boolean isConnected() {
        return isConnected.get() && socket != null && socket.isConnected() && !socket.isClosed();
    }

    private byte[] intToBytes(int value) {
        return new byte[] {
            (byte) (value >>> 24),
            (byte) (value >>> 16),
            (byte) (value >>> 8),
            (byte) value
        };
    }

    private int bytesFromInt(byte[] bytes) {
        return ((bytes[0] & 0xFF) << 24) |
               ((bytes[1] & 0xFF) << 16) |
               ((bytes[2] & 0xFF) << 8) |
               (bytes[3] & 0xFF);
    }
}
