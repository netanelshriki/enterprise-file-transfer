package com.securetransfer.server.config;

import com.securetransfer.server.handler.SignalingWebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    
    @Autowired
    private SignalingWebSocketHandler signalingHandler;
    
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(signalingHandler, "/signaling")
                .setAllowedOrigins("*")
                .withSockJS();
        
        registry.addHandler(signalingHandler, "/ws/signaling")
                .setAllowedOrigins("*");
    }
}
