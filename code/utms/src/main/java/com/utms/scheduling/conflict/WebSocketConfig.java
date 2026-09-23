package com.utms.scheduling.conflict;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket configuration for real-time conflict broadcasting.
 * 
 * KD-65: Spring WebSocket + STOMP for interactive channel.
 * 
 * Design: A4-16  6.2 WebSocket Configuration
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Enable a simple memory-based message broker
        // Clients subscribe to /topic/* for broadcast messages
        config.enableSimpleBroker("/topic");
        // Messages from clients with /app prefix are routed to @MessageMapping methods
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Register STOMP endpoint for WebSocket connections
        // SockJS provides fallback options for browsers that don't support WebSocket
        // TODO: In production, replace setAllowedOriginPatterns("*") with specific allowed origins
        // for CORS security (e.g., setAllowedOrigins("https://app.example.com"))
        registry.addEndpoint("/ws-conflicts")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }
}