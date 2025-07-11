package com.securetransfer.desktop.network;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class STUNTURNConfig {
    private static final Logger logger = LoggerFactory.getLogger(STUNTURNConfig.class);
    
    private final List<String> stunServers;
    private final List<TurnServer> turnServers;
    
    public STUNTURNConfig() {
        this.stunServers = new ArrayList<>();
        this.turnServers = new ArrayList<>();
        initializeDefaultServers();
    }
    
    private void initializeDefaultServers() {
        stunServers.add("stun:stun.l.google.com:19302");
        stunServers.add("stun:stun1.l.google.com:19302");
        stunServers.add("stun:stun2.l.google.com:19302");
        stunServers.add("stun:stun3.l.google.com:19302");
        stunServers.add("stun:stun4.l.google.com:19302");
        
        stunServers.add("stun:stun.cloudflare.com:3478");
        stunServers.add("stun:stun.nextcloud.com:443");
        
        logger.info("Initialized {} STUN servers", stunServers.size());
    }
    
    public void addTurnServer(String url, String username, String credential) {
        TurnServer turnServer = new TurnServer(url, username, credential);
        turnServers.add(turnServer);
        logger.info("Added TURN server: {}", url);
    }
    
    public void addCustomStunServer(String url) {
        stunServers.add(url);
        logger.info("Added custom STUN server: {}", url);
    }
    
    public List<String> getStunServers() {
        return new ArrayList<>(stunServers);
    }
    
    public List<TurnServer> getTurnServers() {
        return new ArrayList<>(turnServers);
    }
    
    public static class TurnServer {
        private final String url;
        private final String username;
        private final String credential;
        
        public TurnServer(String url, String username, String credential) {
            this.url = url;
            this.username = username;
            this.credential = credential;
        }
        
        public String getUrl() {
            return url;
        }
        
        public String getUsername() {
            return username;
        }
        
        public String getCredential() {
            return credential;
        }
        
        @Override
        public String toString() {
            return "TurnServer{url='" + url + "', username='" + username + "'}";
        }
    }
}
