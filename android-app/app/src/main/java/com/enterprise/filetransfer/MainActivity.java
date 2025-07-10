package com.enterprise.filetransfer;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.enterprise.filetransfer.adapter.DeviceAdapter;
import com.enterprise.filetransfer.databinding.ActivityMainBinding;
import com.enterprise.filetransfer.model.Device;
import com.enterprise.filetransfer.viewmodel.MainViewModel;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final int PERMISSION_REQUEST_CODE = 1001;
    private ActivityMainBinding binding;
    private MainViewModel viewModel;
    private DeviceAdapter deviceAdapter;
    private List<Uri> selectedFiles = new ArrayList<>();

    private final ActivityResultLauncher<Intent> filePickerLauncher = 
        registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                Intent data = result.getData();
                selectedFiles.clear();
                
                if (data.getClipData() != null) {
                    for (int i = 0; i < data.getClipData().getItemCount(); i++) {
                        Uri uri = data.getClipData().getItemAt(i).getUri();
                        selectedFiles.add(uri);
                    }
                } else if (data.getData() != null) {
                    selectedFiles.add(data.getData());
                }
                
                updateSelectedFilesUI();
            }
        });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Enterprise File Transfer");
        }

        initViewModel();
        initRecyclerView();
        initClickListeners();
        checkPermissions();
    }

    private void initViewModel() {
        viewModel = new ViewModelProvider(this).get(MainViewModel.class);
        
        viewModel.getDevices().observe(this, devices -> {
            deviceAdapter.updateDevices(devices);
            if (devices.isEmpty()) {
                binding.tvDeviceStatus.setText("No devices found");
            } else {
                binding.tvDeviceStatus.setText(devices.size() + " devices found");
            }
        });

        viewModel.getTransferProgress().observe(this, progress -> {
            if (progress != null) {
                binding.cardTransferProgress.setVisibility(View.VISIBLE);
                binding.progressTransfer.setProgress(progress.getProgress());
                binding.tvTransferStatus.setText(progress.getStatus());
                binding.tvTransferSpeed.setText(progress.getSpeed());
                binding.tvTimeRemaining.setText(progress.getTimeRemaining());
            } else {
                binding.cardTransferProgress.setVisibility(View.GONE);
            }
        });

        viewModel.getTransferComplete().observe(this, isComplete -> {
            if (isComplete) {
                binding.cardTransferProgress.setVisibility(View.GONE);
                Toast.makeText(this, "Transfer completed successfully!", Toast.LENGTH_SHORT).show();
                selectedFiles.clear();
                updateSelectedFilesUI();
            }
        });
    }

    private void initRecyclerView() {
        deviceAdapter = new DeviceAdapter(new ArrayList<>(), this::onDeviceSelected);
        binding.rvDevices.setLayoutManager(new LinearLayoutManager(this));
        binding.rvDevices.setAdapter(deviceAdapter);
    }

    private void initClickListeners() {
        binding.btnRefreshDevices.setOnClickListener(v -> {
            binding.tvDeviceStatus.setText("Searching for devices...");
            viewModel.refreshDevices();
        });

        binding.btnSelectFiles.setOnClickListener(v -> openFilePicker());

        binding.btnSendFiles.setOnClickListener(v -> {
            Device selectedDevice = deviceAdapter.getSelectedDevice();
            if (selectedDevice != null && !selectedFiles.isEmpty()) {
                viewModel.startFileTransfer(selectedDevice, selectedFiles);
            } else {
                Toast.makeText(this, "Please select a device and files", Toast.LENGTH_SHORT).show();
            }
        });

        binding.btnCancelTransfer.setOnClickListener(v -> {
            viewModel.cancelTransfer();
            binding.cardTransferProgress.setVisibility(View.GONE);
        });
    }

    private void onDeviceSelected(Device device) {
        deviceAdapter.setSelectedDevice(device);
        updateSendButtonState();
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        filePickerLauncher.launch(Intent.createChooser(intent, "Select Files"));
    }

    private void updateSelectedFilesUI() {
        if (selectedFiles.isEmpty()) {
            binding.tvSelectedFiles.setText("No files selected");
        } else {
            binding.tvSelectedFiles.setText(getString(R.string.files_selected, selectedFiles.size()));
        }
        updateSendButtonState();
    }

    private void updateSendButtonState() {
        boolean hasFiles = !selectedFiles.isEmpty();
        boolean hasSelectedDevice = deviceAdapter.getSelectedDevice() != null;
        binding.btnSendFiles.setEnabled(hasFiles && hasSelectedDevice);
    }

    private void checkPermissions() {
        List<String> permissionsNeeded = new ArrayList<>();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) 
                != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) 
                    != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.POST_NOTIFICATIONS);
            }
        }

        if (!permissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, 
                permissionsNeeded.toArray(new String[0]), 
                PERMISSION_REQUEST_CODE);
        } else {
            startDeviceDiscovery();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            
            if (allGranted) {
                startDeviceDiscovery();
            } else {
                Toast.makeText(this, "Permissions are required for the app to work properly", 
                    Toast.LENGTH_LONG).show();
            }
        }
    }

    private void startDeviceDiscovery() {
        viewModel.startDeviceDiscovery();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (viewModel != null) {
            viewModel.stopDeviceDiscovery();
        }
    }
}
