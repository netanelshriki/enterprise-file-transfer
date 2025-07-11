package com.securetransfer.desktop.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DecimalFormat;
import java.util.Objects;

public class TransferItem extends HBox {
    
    private static final Logger logger = LoggerFactory.getLogger(TransferItem.class);
    private static final DecimalFormat SIZE_FORMAT = new DecimalFormat("#,##0.0");
    
    private final String fileName;
    private final long fileSize;
    private String status;
    
    private Label fileNameLabel;
    private Label fileSizeLabel;
    private Label statusLabel;
    private ProgressBar progressBar;
    private Button actionButton;
    private ImageView fileIcon;
    
    public TransferItem(String fileName, long fileSize, String status) {
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.status = status;
        
        initializeItem();
    }
    
    private void initializeItem() {
        getStyleClass().add("transfer-item");
        setPadding(new Insets(8, 12, 8, 12));
        setSpacing(12);
        setAlignment(Pos.CENTER_LEFT);
        setPrefHeight(60);
        
        createFileIcon();
        createLabels();
        createProgressBar();
        createActionButton();
        
        getChildren().addAll(
            fileIcon,
            createFileInfoSection(),
            createProgressSection(),
            actionButton
        );
    }
    
    private void createFileIcon() {
        fileIcon = new ImageView();
        fileIcon.setFitWidth(32);
        fileIcon.setFitHeight(32);
        fileIcon.setPreserveRatio(true);
        
        try {
            String iconPath = getIconPathForFile(fileName);
            Image icon = new Image(Objects.requireNonNull(
                getClass().getResourceAsStream(iconPath)));
            fileIcon.setImage(icon);
        } catch (Exception e) {
            logger.warn("Could not load file icon for: {}", fileName);
            fileIcon.setImage(createDefaultFileIcon());
        }
    }
    
    private String getIconPathForFile(String fileName) {
        String extension = getFileExtension(fileName).toLowerCase();
        
        switch (extension) {
            case "pdf":
                return "/images/pdf-icon.png";
            case "doc":
            case "docx":
                return "/images/doc-icon.png";
            case "jpg":
            case "jpeg":
            case "png":
            case "gif":
                return "/images/image-icon.png";
            case "mp4":
            case "avi":
            case "mkv":
            case "mov":
                return "/images/video-icon.png";
            case "mp3":
            case "wav":
            case "flac":
                return "/images/audio-icon.png";
            case "zip":
            case "rar":
            case "7z":
                return "/images/archive-icon.png";
            default:
                return "/images/file-icon.png";
        }
    }
    
    private String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        return lastDot > 0 ? fileName.substring(lastDot + 1) : "";
    }
    
    private Image createDefaultFileIcon() {
        return new Image(Objects.requireNonNull(
            getClass().getResourceAsStream("/images/file-icon.png")));
    }
    
    private void createLabels() {
        fileNameLabel = new Label(fileName);
        fileNameLabel.getStyleClass().add("file-name");
        
        fileSizeLabel = new Label(formatFileSize(fileSize));
        fileSizeLabel.getStyleClass().add("file-size");
        
        statusLabel = new Label(status);
        statusLabel.getStyleClass().add("transfer-status");
    }
    
    private void createProgressBar() {
        progressBar = new ProgressBar(0.0);
        progressBar.setPrefWidth(150);
        progressBar.setVisible(false);
        progressBar.getStyleClass().add("transfer-progress");
    }
    
    private void createActionButton() {
        actionButton = new Button("Cancel");
        actionButton.getStyleClass().add("transfer-action-button");
        actionButton.setPrefWidth(80);
        
        actionButton.setOnAction(event -> {
            logger.info("Action button clicked for file: {}", fileName);
            handleActionButton();
        });
    }
    
    private VBox createFileInfoSection() {
        VBox fileInfo = new VBox(2);
        fileInfo.setAlignment(Pos.CENTER_LEFT);
        fileInfo.getChildren().addAll(fileNameLabel, fileSizeLabel);
        HBox.setHgrow(fileInfo, Priority.ALWAYS);
        return fileInfo;
    }
    
    private VBox createProgressSection() {
        VBox progressSection = new VBox(4);
        progressSection.setAlignment(Pos.CENTER);
        progressSection.setPrefWidth(180);
        progressSection.getChildren().addAll(statusLabel, progressBar);
        return progressSection;
    }
    
    private void handleActionButton() {
        switch (status) {
            case "Queued":
            case "Ready to send":
                startTransfer();
                break;
            case "Transferring":
                pauseTransfer();
                break;
            case "Paused":
                resumeTransfer();
                break;
            case "Completed":
            case "Failed":
                removeItem();
                break;
            default:
                logger.warn("Unknown status for action: {}", status);
        }
    }
    
    private void startTransfer() {
        updateStatus("Transferring");
        progressBar.setVisible(true);
        actionButton.setText("Pause");
        
        new Thread(() -> {
            try {
                for (int i = 0; i <= 100; i += 5) {
                    final double progress = i / 100.0;
                    javafx.application.Platform.runLater(() -> {
                        progressBar.setProgress(progress);
                        if (progress >= 1.0) {
                            updateStatus("Completed");
                            actionButton.setText("Remove");
                        }
                    });
                    Thread.sleep(200);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                javafx.application.Platform.runLater(() -> {
                    updateStatus("Failed");
                    actionButton.setText("Retry");
                });
            }
        }).start();
    }
    
    private void pauseTransfer() {
        updateStatus("Paused");
        actionButton.setText("Resume");
    }
    
    private void resumeTransfer() {
        updateStatus("Transferring");
        actionButton.setText("Pause");
    }
    
    private void removeItem() {
        if (getParent() instanceof VBox) {
            ((VBox) getParent()).getChildren().remove(this);
        }
    }
    
    private void updateStatus(String newStatus) {
        this.status = newStatus;
        statusLabel.setText(newStatus);
        
        statusLabel.getStyleClass().clear();
        statusLabel.getStyleClass().add("transfer-status");
        
        switch (newStatus) {
            case "Completed":
                statusLabel.getStyleClass().add("status-completed");
                break;
            case "Failed":
                statusLabel.getStyleClass().add("status-failed");
                break;
            case "Transferring":
                statusLabel.getStyleClass().add("status-transferring");
                break;
            case "Paused":
                statusLabel.getStyleClass().add("status-paused");
                break;
        }
    }
    
    private String formatFileSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return SIZE_FORMAT.format(bytes / 1024.0) + " KB";
        } else if (bytes < 1024 * 1024 * 1024) {
            return SIZE_FORMAT.format(bytes / (1024.0 * 1024.0)) + " MB";
        } else {
            return SIZE_FORMAT.format(bytes / (1024.0 * 1024.0 * 1024.0)) + " GB";
        }
    }
    
    public String getFileName() {
        return fileName;
    }
    
    public long getFileSize() {
        return fileSize;
    }
    
    public String getStatus() {
        return status;
    }
}
