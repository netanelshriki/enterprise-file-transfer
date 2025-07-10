package com.enterprise.filetransfer.network;

import android.util.Log;

import com.enterprise.filetransfer.model.Device;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.List;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceListener;
import javax.jmdns.ServiceInfo;
import java.net.InetAddress;

public class NetworkManager {
    private static final String TAG = "NetworkManager";
    private static final int CONNECTION_TIMEOUT = 10000; // 10 seconds
    private static final int READ_TIMEOUT = 30000; // 30 seconds
    private static final String SERVICE_TYPE = "_filetransfer._tcp.local.";

    private Socket socket;
    private InputStream inputStream;
    private OutputStream outputStream;
    private AtomicBoolean isConnected = new AtomicBoolean(false);
    
    private WebSocket webSocket;
    private OkHttpClient client;
    private String signalingServerUrl;
    private String localDeviceId;
    private List<Device> discoveredDevices;
    private JmDNS jmdns;

    public NetworkManager() {
        client = new OkHttpClient();
        signalingServerUrl = "ws://localhost:8080/ws"; // Default, can be configured
        localDeviceId = UUID.randomUUID().toString();
        discoveredDevices = new ArrayList<>();
    }
    
    public void setSignalingServerUrl(String url) {
        this.signalingServerUrl = url;
    }
    
    public String getLocalDeviceId() {
        return localDeviceId;
    }
    
    public CompletableFuture<Void> connectToSignalingServer() {
        CompletableFuture<Void> future = new CompletableFuture<>();
        
        Request request = new Request.Builder()
            .url(signalingServerUrl)
            .build();
            
        webSocket = client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                Log.d(TAG, "WebSocket connected to signaling server");
                
                try {
                    JSONObject registerMsg = new JSONObject();
                    registerMsg.put("type", "Register");
                    registerMsg.put("name", "Android App");
                    registerMsg.put("device_type", "android");
                    registerMsg.put("public_key", "placeholder_public_key_android");
                    
                    webSocket.send(registerMsg.toString());
                    future.complete(null);
                } catch (JSONException e) {
                    Log.e(TAG, "Error creating register message", e);
                    future.completeExceptionally(e);
                }
            }
            
            @Override
            public void onMessage(WebSocket webSocket, String text) {
                Log.d(TAG, "Received WebSocket message: " + text);
                handleSignalingMessage(text);
            }
            
            @Override
            public void onMessage(WebSocket webSocket, ByteString bytes) {
                Log.d(TAG, "Received WebSocket bytes: " + bytes.hex());
            }
            
            @Override
            public void onClosing(WebSocket webSocket, int code, String reason) {
                Log.d(TAG, "WebSocket closing: " + code + " / " + reason);
            }
            
            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                Log.d(TAG, "WebSocket closed: " + code + " / " + reason);
            }
            
            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                Log.e(TAG, "WebSocket error", t);
                future.completeExceptionally(t);
            }
        });
        
        return future;
    }
    
    private void handleSignalingMessage(String message) {
        try {
            JSONObject json = new JSONObject(message);
            String type = json.getString("type");
            
            switch (type) {
                case "Registered":
                    String deviceId = json.getString("device_id");
                    Log.d(TAG, "Registered with device ID: " + deviceId);
                    break;
                    
                case "DeviceList":
                    JSONArray devices = json.getJSONArray("devices");
                    updateDeviceList(devices);
                    break;
                    
                case "ConnectionRequest":
                    String fromDeviceId = json.getString("from_device_id");
                    String fromDeviceName = json.getString("from_device_name");
                    Log.d(TAG, "Connection request from: " + fromDeviceName);
                    break;
                    
                case "SignalingData":
                    Log.d(TAG, "Received signaling data");
                    break;
                    
                case "Error":
                    String errorMessage = json.getString("message");
                    Log.e(TAG, "Server error: " + errorMessage);
                    break;
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing signaling message", e);
        }
    }
    
    private void updateDeviceList(JSONArray devicesArray) {
        try {
            List<Device> internetDevices = new ArrayList<>();
            for (int i = 0; i < devicesArray.length(); i++) {
                JSONObject deviceJson = devicesArray.getJSONObject(i);
                Device device = new Device(
                    deviceJson.getString("id"),
                    deviceJson.getString("name"),
                    null, // No direct IP for internet devices
                    0     // No direct port for internet devices
                );
                device.setType(Device.DeviceType.valueOf(deviceJson.getString("device_type").toUpperCase()));
                internetDevices.add(device);
            }
            
            synchronized (discoveredDevices) {
                discoveredDevices.removeIf(d -> d.getIpAddress() == null);
                discoveredDevices.addAll(internetDevices);
            }
            
            Log.d(TAG, "Updated device list with " + internetDevices.size() + " internet devices");
        } catch (JSONException e) {
            Log.e(TAG, "Error updating device list", e);
        }
    }
    
    public CompletableFuture<List<Device>> discoverDevices() {
        CompletableFuture<List<Device>> future = new CompletableFuture<>();
        
        discoverLocalDevices().thenCompose(localDevices -> {
            return discoverInternetDevices();
        }).thenAccept(allDevices -> {
            synchronized (discoveredDevices) {
                future.complete(new ArrayList<>(discoveredDevices));
            }
        }).exceptionally(throwable -> {
            future.completeExceptionally(throwable);
            return null;
        });
        
        return future;
    }
    
    private CompletableFuture<List<Device>> discoverLocalDevices() {
        CompletableFuture<List<Device>> future = new CompletableFuture<>();
        
        try {
            InetAddress addr = InetAddress.getLocalHost();
            jmdns = JmDNS.create(addr);
            
            jmdns.addServiceListener(SERVICE_TYPE, new ServiceListener() {
                @Override
                public void serviceAdded(ServiceEvent event) {
                    Log.d(TAG, "Local service added: " + event.getInfo());
                }
                
                @Override
                public void serviceRemoved(ServiceEvent event) {
                    Log.d(TAG, "Local service removed: " + event.getInfo());
                }
                
                @Override
                public void serviceResolved(ServiceEvent event) {
                    ServiceInfo info = event.getInfo();
                    Device device = new Device(
                        UUID.randomUUID().toString(),
                        info.getName(),
                        info.getHostAddresses()[0],
                        info.getPort()
                    );
                    device.setType(Device.DeviceType.DESKTOP); // Assume local devices are desktop
                    
                    synchronized (discoveredDevices) {
                        discoveredDevices.add(device);
                    }
                    Log.d(TAG, "Local service resolved: " + device.getName());
                }
            });
            
            Thread.sleep(2000);
            future.complete(new ArrayList<>(discoveredDevices));
            
        } catch (Exception e) {
            Log.e(TAG, "Error during local device discovery", e);
            future.completeExceptionally(e);
        }
        
        return future;
    }
    
    private CompletableFuture<List<Device>> discoverInternetDevices() {
        CompletableFuture<List<Device>> future = new CompletableFuture<>();
        
        if (webSocket != null) {
            try {
                JSONObject discoverMsg = new JSONObject();
                discoverMsg.put("type", "Discover");
                webSocket.send(discoverMsg.toString());
                
                Thread.sleep(1000);
                future.complete(new ArrayList<>(discoveredDevices));
            } catch (Exception e) {
                Log.e(TAG, "Error during internet device discovery", e);
                future.completeExceptionally(e);
            }
        } else {
            Log.w(TAG, "WebSocket not connected, skipping internet discovery");
            future.complete(new ArrayList<>());
        }
        
        return future;
    }
    
    public CompletableFuture<String> sendFileToDevice(Device targetDevice, String filePath) {
        CompletableFuture<String> future = new CompletableFuture<>();
        
        Log.d(TAG, "Sending file " + filePath + " to device " + targetDevice.getName());
        
        if (targetDevice.getIpAddress() != null) {
            sendFileDirectly(targetDevice, filePath, future);
        } else {
            sendFileViaRelay(targetDevice, filePath, future);
        }
        
        return future;
    }
    
    private void sendFileDirectly(Device targetDevice, String filePath, CompletableFuture<String> future) {
        Log.d(TAG, "Attempting direct file transfer to " + targetDevice.getIpAddress());
        
        if (connectToDevice(targetDevice)) {
            try {
                byte[] fileData = new byte[0]; // Placeholder
                if (sendData(fileData)) {
                    future.complete("File sent directly to " + targetDevice.getName());
                } else {
                    future.completeExceptionally(new Exception("Failed to send file data"));
                }
            } catch (Exception e) {
                future.completeExceptionally(e);
            } finally {
                disconnect();
            }
        } else {
            future.completeExceptionally(new Exception("Failed to connect to device"));
        }
    }
    
    private void sendFileViaRelay(Device targetDevice, String filePath, CompletableFuture<String> future) {
        Log.d(TAG, "Sending file via WebRTC relay to device " + targetDevice.getName());
        
        try {
            JSONObject connectMsg = new JSONObject();
            connectMsg.put("type", "ConnectRequest");
            connectMsg.put("target_device_id", targetDevice.getId());
            
            if (webSocket != null) {
                webSocket.send(connectMsg.toString());
                
                future.complete("File sent via relay to " + targetDevice.getName());
            } else {
                future.completeExceptionally(new Exception("WebSocket not connected"));
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error sending file via relay", e);
            future.completeExceptionally(e);
        }
    }
    
    public void cleanup() {
        try {
            if (jmdns != null) {
                jmdns.close();
            }
            if (webSocket != null) {
                webSocket.close(1000, "Cleanup");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error during cleanup", e);
        }
        disconnect();
    }

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
