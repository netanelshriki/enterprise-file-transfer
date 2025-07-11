package com.securetransfer.desktop;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Objects;

public class SecureTransferApp extends Application {
    
    private static final Logger logger = LoggerFactory.getLogger(SecureTransferApp.class);
    private static final String APP_TITLE = "SecureTransfer";
    private static final String APP_VERSION = "1.0.0";
    
    @Override
    public void start(Stage primaryStage) {
        try {
            logger.info("Starting SecureTransfer Desktop Application v{}", APP_VERSION);
            
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MainWindow.fxml"));
            Parent root = loader.load();
            
            Scene scene = new Scene(root, 1200, 800);
            scene.getStylesheets().add(Objects.requireNonNull(
                getClass().getResource("/css/styles.css")).toExternalForm());
            
            primaryStage.setTitle(APP_TITLE + " v" + APP_VERSION);
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(800);
            primaryStage.setMinHeight(600);
            
            try {
                primaryStage.getIcons().add(new Image(Objects.requireNonNull(
                    getClass().getResourceAsStream("/images/app-icon.png"))));
            } catch (Exception e) {
                logger.warn("Could not load application icon: {}", e.getMessage());
            }
            
            primaryStage.setOnCloseRequest(event -> {
                logger.info("Application shutdown requested");
                Platform.exit();
                System.exit(0);
            });
            
            primaryStage.show();
            logger.info("SecureTransfer Desktop Application started successfully");
            
        } catch (IOException e) {
            logger.error("Failed to load main window FXML", e);
            showErrorAndExit("Failed to start application: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error during application startup", e);
            showErrorAndExit("Unexpected error: " + e.getMessage());
        }
    }
    
    private void showErrorAndExit(String message) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
            javafx.scene.control.Alert.AlertType.ERROR);
        alert.setTitle("SecureTransfer - Error");
        alert.setHeaderText("Application Startup Failed");
        alert.setContentText(message);
        alert.showAndWait();
        Platform.exit();
        System.exit(1);
    }
    
    @Override
    public void stop() throws Exception {
        logger.info("SecureTransfer Desktop Application stopping");
        super.stop();
    }
    
    public static void main(String[] args) {
        logger.info("SecureTransfer Desktop Application starting with args: {}", 
            java.util.Arrays.toString(args));
        launch(args);
    }
}
