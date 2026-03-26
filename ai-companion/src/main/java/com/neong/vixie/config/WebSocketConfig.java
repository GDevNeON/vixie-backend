package com.neong.vixie.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket configuration for the AI Companion service.
 * Uses STOMP over SockJS with an in-memory SimpleBroker.
 * Endpoint: /ws/ai/chat
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Enable in-memory broker for /queue (point-to-point) and /topic (broadcast)
        config.enableSimpleBroker("/queue", "/topic");
        // Application destination prefix for @MessageMapping methods
        config.setApplicationDestinationPrefixes("/app");
        // User destination prefix for user-specific subscriptions
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws/ai/chat")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }
}
