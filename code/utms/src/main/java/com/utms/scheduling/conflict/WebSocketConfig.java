package com.utms.scheduling.conflict;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * STOMP-over-WebSocket configuration for real-time conflict feedback (A4-16, KD-65).
 *
 * <p>Clients connect to {@code /ws}, send placement checks to
 * {@code /app/drafts/{draftId}/conflict-check} (see {@link ConflictWsHandler}), and
 * receive results on the {@code /topic} broker destinations. The REST endpoints
 * ({@link ConflictController}) provide the same checks as a fallback.
 *
 * <p>Uses the in-memory SimpleBroker — sufficient for the Phase-1 scale (50+
 * coordinators, PD-100); no external broker/Redis. Allowed origins are permissive
 * for dev (aligns with the current dev-only permitAll security posture); this must be
 * restricted to the frontend origin in production alongside the JWT/CORS hardening.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // TODO (prod): restrict allowed origins to the frontend origin when the
        // auth/CORS hardening lands (mirrors the SecurityConfig permitAll TODO).
        registry.addEndpoint("/ws").setAllowedOriginPatterns("*").withSockJS();
    }
}
