package com.enterprise.filetransfer.viewmodel;

import android.app.Application;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.enterprise.filetransfer.model.Device;
import com.enterprise.filetransfer.model.TransferProgress;
import com.enterprise.filetransfer.service.DeviceDiscoveryService;
import com.enterprise.filetransfer.service.FileTransferService;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainViewModel extends AndroidViewModel {
    private MutableLiveData<List<Device>> devices = new MutableLiveData<>(new ArrayList<>());
    private MutableLiveData<TransferProgress> transferProgress = new MutableLiveData<>();
    private MutableLiveData<Boolean> transferComplete = new MutableLiveData<>(false);
    private MutableLiveData<String> errorMessage = new MutableLiveData<>();

    private DeviceDiscoveryService discoveryService;
    private FileTransferService transferService;
    private ExecutorService executorService;

    public MainViewModel(@NonNull Application application) {
        super(application);
        executorService = Executors.newCachedThreadPool();
        discoveryService = new DeviceDiscoveryService();
        transferService = new FileTransferService();
        
        discoveryService.setDeviceDiscoveryListener(discoveredDevices -> {
            devices.postValue(discoveredDevices);
        });

        transferService.setTransferProgressListener(progress -> {
            transferProgress.postValue(progress);
        });

        transferService.setTransferCompleteListener(success -> {
            if (success) {
                transferComplete.postValue(true);
                transferProgress.postValue(null);
            } else {
                errorMessage.postValue("Transfer failed");
                transferProgress.postValue(null);
            }
        });
    }

    public LiveData<List<Device>> getDevices() {
        return devices;
    }

    public LiveData<TransferProgress> getTransferProgress() {
        return transferProgress;
    }

    public LiveData<Boolean> getTransferComplete() {
        return transferComplete;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public void startDeviceDiscovery() {
        executorService.execute(() -> {
            try {
                discoveryService.startDiscovery();
            } catch (Exception e) {
                errorMessage.postValue("Failed to start device discovery: " + e.getMessage());
            }
        });
    }

    public void stopDeviceDiscovery() {
        executorService.execute(() -> {
            discoveryService.stopDiscovery();
        });
    }

    public void refreshDevices() {
        executorService.execute(() -> {
            try {
                discoveryService.refreshDiscovery();
            } catch (Exception e) {
                errorMessage.postValue("Failed to refresh devices: " + e.getMessage());
            }
        });
    }

    public void startFileTransfer(Device targetDevice, List<Uri> fileUris) {
        executorService.execute(() -> {
            try {
                transferComplete.postValue(false);
                transferService.startTransfer(getApplication(), targetDevice, fileUris);
            } catch (Exception e) {
                errorMessage.postValue("Failed to start transfer: " + e.getMessage());
                transferProgress.postValue(null);
            }
        });
    }

    public void cancelTransfer() {
        executorService.execute(() -> {
            transferService.cancelTransfer();
            transferProgress.postValue(null);
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        stopDeviceDiscovery();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}
