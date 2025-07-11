package com.securetransfer.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SecureTransferServerApplication {
    
    private static final Logger logger = LoggerFactory.getLogger(SecureTransferServerApplication.class);
    
    public static void main(String[] args) {
        logger.info("Starting SecureTransfer Signaling Server...");
        SpringApplication.run(SecureTransferServerApplication.class, args);
        logger.info("SecureTransfer Signaling Server started successfully");
    }
}
