package com.securetransfer.desktop.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public class DeviceCard extends VBox {
    
    private static final Logger logger = LoggerFactory.getLogger(DeviceCard.class);
    
    private final String deviceName;
    private final String deviceType;
    private final String ipAddress;
    private final boolean isOnline;
    
    private Label nameLabel;
    private Label typeLabel;
    private Label statusLabel;
    private Button sendButton;
    private ImageView deviceIcon;
    
    public DeviceCard(String deviceName, String deviceType, String ipAddress, boolean isOnline) {
        this.deviceName = deviceName;
        this.deviceType = deviceType;
        this.ipAddress = ipAddress;
        this.isOnline = isOnline;
        
        initializeCard();
        setupEventHandlers();
    }
    
    private void initializeCard() {
        getStyleClass().add("device-card");
        setPrefWidth(280);
        setPrefHeight(160);
        setPadding(new Insets(16));
        setSpacing(12);
        setAlignment(Pos.TOP_LEFT);
        
        createDeviceIcon();
        createLabels();
        createActionButton();
        
        getChildren().addAll(
            createHeaderSection(),
            createInfoSection(),
            createActionSection()
        );
    }
    
    private void createDeviceIcon() {
        deviceIcon = new ImageView();
        deviceIcon.setFitWidth(48);
        deviceIcon.setFitHeight(48);
        deviceIcon.setPreserveRatio(true);
        
        try {
            String iconPath = getIconPathForDeviceType(deviceType);
            Image icon = new Image(Objects.requireNonNull(
                getClass().getResourceAsStream(iconPath)));
            deviceIcon.setImage(icon);
        } catch (Exception e) {
            logger.warn("Could not load device icon for type: {}", deviceType);
            deviceIcon.setImage(createDefaultIcon());
        }
    }
    
    private String getIconPathForDeviceType(String type) {
        if (type.toLowerCase().contains("windows") || type.toLowerCase().contains("laptop")) {
            return "/images/laptop-icon.png";
        } else if (type.toLowerCase().contains("android") || type.toLowerCase().contains("phone")) {
            return "/images/phone-icon.png";
        } else if (type.toLowerCase().contains("linux") || type.toLowerCase().contains("desktop")) {
            return "/images/desktop-icon.png";
        }
        return "/images/device-icon.png";
    }
    
    private Image createDefaultIcon() {
        return new Image(Objects.requireNonNull(
            getClass().getResourceAsStream("/images/device-icon.png")));
    }
    
    private void createLabels() {
        nameLabel = new Label(deviceName);
        nameLabel.getStyleClass().add("device-name");
        
        typeLabel = new Label(deviceType);
        typeLabel.getStyleClass().add("device-type");
        
        String statusText = isOnline ? "Online • " + ipAddress : "Offline";
        statusLabel = new Label(statusText);
        statusLabel.getStyleClass().add(isOnline ? "device-status-online" : "device-status-offline");
    }
    
    private void createActionButton() {
        sendButton = new Button(isOnline ? "Send Files" : "Unavailable");
        sendButton.getStyleClass().add("send-button");
        sendButton.setDisable(!isOnline);
        sendButton.setPrefWidth(120);
        
        sendButton.setOnAction(event -> {
            logger.info("Send files to device: {}", deviceName);
            handleSendFiles();
        });
    }
    
    private HBox createHeaderSection() {
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getChildren().addAll(deviceIcon, nameLabel);
        return header;
    }
    
    private VBox createInfoSection() {
        VBox info = new VBox(4);
        info.getChildren().addAll(typeLabel, statusLabel);
        VBox.setVgrow(info, Priority.ALWAYS);
        return info;
    }
    
    private HBox createActionSection() {
        HBox actions = new HBox();
        actions.setAlignment(Pos.CENTER_RIGHT);
        actions.getChildren().add(sendButton);
        return actions;
    }
    
    private void setupEventHandlers() {
        setOnMouseEntered(this::handleMouseEntered);
        setOnMouseExited(this::handleMouseExited);
        setOnMouseClicked(this::handleMouseClicked);
    }
    
    private void handleMouseEntered(MouseEvent event) {
        if (isOnline) {
            getStyleClass().add("device-card-hover");
        }
    }
    
    private void handleMouseExited(MouseEvent event) {
        getStyleClass().remove("device-card-hover");
    }
    
    private void handleMouseClicked(MouseEvent event) {
        if (isOnline && event.getClickCount() == 2) {
            handleSendFiles();
        }
    }
    
    private void handleSendFiles() {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
            javafx.scene.control.Alert.AlertType.INFORMATION);
        alert.setTitle("Send Files");
        alert.setHeaderText("Send to " + deviceName);
        alert.setContentText("File transfer functionality will be implemented in the next phase.\n\n" +
            "Target Device: " + deviceName + "\n" +
            "Type: " + deviceType + "\n" +
            "Address: " + ipAddress);
        alert.showAndWait();
    }
    
    public String getDeviceName() {
        return deviceName;
    }
    
    public String getDeviceType() {
        return deviceType;
    }
    
    public String getIpAddress() {
        return ipAddress;
    }
    
    public boolean isOnline() {
        return isOnline;
    }
    
    public void updateOnlineStatus(boolean online) {
        sendButton.setText(online ? "Send Files" : "Unavailable");
        sendButton.setDisable(!online);
        
        String statusText = online ? "Online • " + ipAddress : "Offline";
        statusLabel.setText(statusText);
        statusLabel.getStyleClass().clear();
        statusLabel.getStyleClass().add(online ? "device-status-online" : "device-status-offline");
    }
}
