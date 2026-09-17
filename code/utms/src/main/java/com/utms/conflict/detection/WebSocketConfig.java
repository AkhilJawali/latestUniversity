package com.utms.conflict.detection;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket/STOMP configuration for real-time conflict detection (A4-16, design KD-65).
 *
 * <p>Enables a STOMP message broker for the drag-and-drop editor (A4-15) to receive
 * real-time conflict feedback during interactive session placement.
 *
 * <p>Configuration:
 * <ul>
 *   <li>Enables simple in-memory message broker for {@code /topic}</li>
 *   <li>Sets application destination prefix to {@code /app}</li>
 *   <li>Registers STOMP endpoint at {@code /ws} for WebSocket connections</li>
 *   <li>Enables SockJS fallback for browsers without WebSocket support</li>
 * </ul>
 *
 * <p>Design note (PD-100): For Phase 1 (50 concurrent coordinators), an in-memory
 * broker is sufficient. No external broker (RabbitMQ, ActiveMQ) is required.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * Configure the message broker.
     *
     * <p>Enables a simple in-memory message broker for destinations prefixed with
     * {@code /topic} (for server-to-client broadcasts) and sets the application
     * destination prefix to {@code /app} (for client-to-server messages).
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Enable a simple in-memory message broker for /topic destinations
        // This is sufficient for Phase 1 (50 concurrent coordinators, PD-100)
        config.enableSimpleBroker("/topic");
        
        // Prefix for messages bound for @MessageMapping methods
        config.setApplicationDestinationPrefixes("/app");
    }

    /**
     * Register STOMP endpoints.
     *
     * <p>Registers the WebSocket endpoint at {@code /ws} that clients connect to.
     * SockJS is enabled as a fallback for browsers that don't support WebSocket.
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
            .setAllowedOriginPatterns("*")  // TODO: Restrict to frontend origin in production
            .withSockJS();  // Fallback for browsers without WebSocket support
    }
}
