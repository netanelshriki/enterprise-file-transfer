package com.securetransfer.desktop.ui;

import com.securetransfer.desktop.model.Device;
import com.securetransfer.desktop.network.DeviceDiscovery;
import com.securetransfer.desktop.network.FileTransferClient;
import com.securetransfer.desktop.network.FileTransferServer;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class MainController implements Initializable {
    
    private static final Logger logger = LoggerFactory.getLogger(MainController.class);
    
    @FXML private MenuBar menuBar;
    @FXML private ToolBar toolBar;
    @FXML private Button settingsButton;
    @FXML private Button refreshButton;
    @FXML private Label statusLabel;
    @FXML private FlowPane deviceGrid;
    @FXML private VBox transferQueue;
    @FXML private ProgressBar discoveryProgress;
    @FXML private Label discoveryLabel;
    
    private final ObservableList<DeviceCard> deviceCards = FXCollections.observableArrayList();
    private final ObservableList<TransferItem> transferItems = FXCollections.observableArrayList();
    
    private DeviceDiscovery deviceDiscovery;
    private FileTransferServer transferServer;
    private FileTransferClient transferClient;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        logger.info("Initializing MainController");
        
        setupNetworking();
        setupUI();
        setupDragAndDrop();
        startDeviceDiscovery();
        
        logger.info("MainController initialized successfully");
    }
    
    private void setupUI() {
        statusLabel.setText("Ready - Discovering devices...");
        discoveryLabel.setText("Searching for devices on network...");
        discoveryProgress.setVisible(true);
        
        deviceGrid.setPadding(new Insets(20));
        deviceGrid.setHgap(15);
        deviceGrid.setVgap(15);
        
        transferQueue.setPadding(new Insets(10));
        transferQueue.setSpacing(5);
        
        addSampleDevices();
    }
    
    private void setupNetworking() {
        try {
            deviceDiscovery = new DeviceDiscovery();
            transferServer = new FileTransferServer();
            transferClient = new FileTransferClient();
            
            deviceDiscovery.addDeviceAddedListener(device -> {
                Platform.runLater(() -> {
                    addDiscoveredDevice(device);
                    discoveryLabel.setText("Found " + deviceDiscovery.getDiscoveredDevices().size() + " devices");
                    statusLabel.setText("Device discovered: " + device.getName());
                });
            });
            
            deviceDiscovery.addDeviceRemovedListener(device -> {
                Platform.runLater(() -> {
                    removeDiscoveredDevice(device);
                    discoveryLabel.setText("Found " + deviceDiscovery.getDiscoveredDevices().size() + " devices");
                    statusLabel.setText("Device disconnected: " + device.getName());
                });
            });
            
            transferServer.start();
            deviceDiscovery.start();
            
            logger.info("Networking services started successfully");
        } catch (Exception e) {
            logger.error("Failed to initialize networking", e);
            statusLabel.setText("Error: Failed to start networking services");
        }
    }
    
    private void addDiscoveredDevice(Device device) {
        String platform = device.getType() == Device.Type.DESKTOP ? "Desktop" : "Mobile";
        boolean isOnline = device.getStatus() == Device.Status.ONLINE;
        
        DeviceCard deviceCard = new DeviceCard(device.getName(), platform, device.getIpAddress(), isOnline);
        deviceCard.setUserData(device); // Store device data for later use
        deviceGrid.getChildren().add(deviceCard);
        
        discoveryProgress.setVisible(false);
    }
    
    private void removeDiscoveredDevice(Device device) {
        deviceGrid.getChildren().removeIf(node -> {
            if (node instanceof DeviceCard) {
                Device nodeDevice = (Device) node.getUserData();
                return nodeDevice != null && nodeDevice.getId().equals(device.getId());
            }
            return false;
        });
    }
    
    private void addSampleDevices() {
        Platform.runLater(() -> {
            if (deviceGrid.getChildren().isEmpty()) {
                DeviceCard laptop = new DeviceCard("John's Laptop", "Windows 11", "192.168.1.100", true);
                DeviceCard phone = new DeviceCard("Sarah's Phone", "Android 14", "192.168.1.101", true);
                DeviceCard desktop = new DeviceCard("Office Desktop", "Linux Ubuntu", "192.168.1.102", false);
                
                deviceGrid.getChildren().addAll(laptop, phone, desktop);
                
                discoveryProgress.setVisible(false);
                discoveryLabel.setText("Found 3 devices (sample)");
                statusLabel.setText("Ready - 3 devices available (sample data)");
            }
        });
    }
    
    private void setupDragAndDrop() {
        deviceGrid.setOnDragOver(this::handleDragOver);
        deviceGrid.setOnDragDropped(this::handleDragDropped);
    }
    
    private void handleDragOver(DragEvent event) {
        if (event.getGestureSource() != deviceGrid && event.getDragboard().hasFiles()) {
            event.acceptTransferModes(TransferMode.COPY);
        }
        event.consume();
    }
    
    private void handleDragDropped(DragEvent event) {
        Dragboard dragboard = event.getDragboard();
        boolean success = false;
        
        if (dragboard.hasFiles()) {
            List<File> files = dragboard.getFiles();
            logger.info("Files dropped: {}", files.size());
            
            for (File file : files) {
                addTransferItem(file.getName(), file.length(), "Queued");
            }
            
            statusLabel.setText("Files added to transfer queue");
            success = true;
        }
        
        event.setDropCompleted(success);
        event.consume();
    }
    
    private void addTransferItem(String fileName, long fileSize, String status) {
        TransferItem item = new TransferItem(fileName, fileSize, status);
        transferQueue.getChildren().add(item);
    }
    
    private void startDeviceDiscovery() {
        if (deviceDiscovery != null) {
            try {
                deviceDiscovery.start();
                statusLabel.setText("Discovering devices on network...");
                discoveryProgress.setVisible(true);
                discoveryLabel.setText("Searching for devices...");
                
                new Thread(() -> {
                    try {
                        Thread.sleep(5000); // Wait 5 seconds for initial discovery
                        Platform.runLater(() -> {
                            if (deviceDiscovery.getDiscoveredDevices().isEmpty()) {
                                addSampleDevices(); // Fallback to sample data if no devices found
                            } else {
                                discoveryProgress.setVisible(false);
                                discoveryLabel.setText("Discovery complete");
                            }
                        });
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        logger.warn("Device discovery timeout interrupted", e);
                    }
                }).start();
                
            } catch (Exception e) {
                logger.error("Failed to start device discovery", e);
                Platform.runLater(() -> {
                    statusLabel.setText("Error starting device discovery");
                    addSampleDevices(); // Fallback to sample data
                });
            }
        } else {
            new Thread(() -> {
                try {
                    Thread.sleep(2000);
                    Platform.runLater(() -> {
                        discoveryProgress.setVisible(false);
                        discoveryLabel.setText("Discovery complete");
                        addSampleDevices();
                    });
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.warn("Device discovery interrupted", e);
                }
            }).start();
        }
    }
    
    @FXML
    private void handleSettingsAction() {
        logger.info("Settings button clicked");
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Settings");
        alert.setHeaderText("SecureTransfer Settings");
        alert.setContentText("Settings dialog will be implemented in the next version.");
        alert.showAndWait();
    }
    
    @FXML
    private void handleRefreshAction() {
        logger.info("Refresh button clicked");
        statusLabel.setText("Refreshing device list...");
        discoveryProgress.setVisible(true);
        discoveryLabel.setText("Searching for devices...");
        
        deviceGrid.getChildren().clear();
        
        if (deviceDiscovery != null) {
            try {
                deviceDiscovery.stop();
                Thread.sleep(500); // Brief pause
                deviceDiscovery.start();
                statusLabel.setText("Device discovery restarted");
            } catch (Exception e) {
                logger.error("Failed to refresh device discovery", e);
                statusLabel.setText("Error refreshing devices");
                addSampleDevices(); // Fallback
            }
        } else {
            startDeviceDiscovery();
        }
        
        new Thread(() -> {
            try {
                Thread.sleep(3000);
                Platform.runLater(() -> {
                    if (deviceGrid.getChildren().isEmpty()) {
                        addSampleDevices();
                    }
                });
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }
    
    @FXML
    private void handleAddFilesAction() {
        logger.info("Add files button clicked");
        
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Files to Transfer");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("All Files", "*.*"),
            new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif"),
            new FileChooser.ExtensionFilter("Documents", "*.pdf", "*.doc", "*.docx", "*.txt"),
            new FileChooser.ExtensionFilter("Videos", "*.mp4", "*.avi", "*.mkv", "*.mov")
        );
        
        List<File> selectedFiles = fileChooser.showOpenMultipleDialog(settingsButton.getScene().getWindow());
        
        if (selectedFiles != null && !selectedFiles.isEmpty()) {
            for (File file : selectedFiles) {
                addTransferItem(file.getName(), file.length(), "Ready to send");
            }
            statusLabel.setText(selectedFiles.size() + " file(s) added to transfer queue");
        }
    }
    
    @FXML
    private void handleAboutAction() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("About SecureTransfer");
        alert.setHeaderText("SecureTransfer v1.0.0");
        alert.setContentText("Secure cross-platform file transfer application.\n\n" +
            "Features:\n" +
            "• Cross-platform file transfers\n" +
            "• End-to-end encryption\n" +
            "• Local and internet transfers\n" +
            "• Zero configuration required\n\n" +
            "Built with Java 21 and JavaFX");
        alert.showAndWait();
    }
    
    @FXML
    private void handleExitAction() {
        logger.info("Exit requested from menu");
        shutdown();
        Platform.exit();
    }
    
    public void shutdown() {
        logger.info("Shutting down networking services");
        
        if (deviceDiscovery != null) {
            deviceDiscovery.stop();
        }
        
        if (transferServer != null) {
            transferServer.stop();
        }
    }
}
