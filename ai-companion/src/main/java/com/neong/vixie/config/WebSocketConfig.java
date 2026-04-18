package com.neong.vixie.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import com.neong.vixie.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.Collections;
import java.util.List;

/**
 * WebSocket configuration for the AI Companion service.
 * Uses STOMP over SockJS with an in-memory SimpleBroker.
 * Endpoint: /ws/ai/chat
 */
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
@Slf4j
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtService jwtService;

    @Value("${cors.allowed-origins:http://localhost:3000}")
    private String[] allowedOrigins;

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
        // Raw WebSocket endpoint (for pure STOMP clients like stomp_dart_client without SockJS)
        registry.addEndpoint("/ws/ai/chat")
                .setAllowedOrigins(allowedOrigins);
                
        // SockJS fallback endpoint
        registry.addEndpoint("/ws/ai/chat")
                .setAllowedOrigins(allowedOrigins)
                .withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor =
                        MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
                    List<String> authorization = accessor.getNativeHeader("Authorization");
                    log.debug("STOMP CONNECT Headers: Authorization={}", authorization);

                    if (authorization != null && !authorization.isEmpty()) {
                        String bearerToken = authorization.get(0);
                        if (bearerToken.startsWith("Bearer ")) {
                            String jwt = bearerToken.substring(7);
                            try {
                                if (jwtService.isTokenValid(jwt)) {
                                    String subject = jwtService.extractSubject(jwt);
                                    UsernamePasswordAuthenticationToken auth =
                                            new UsernamePasswordAuthenticationToken(subject, null, Collections.emptyList());
                                    accessor.setUser(auth);
                                    log.debug("STOMP connection authenticated for user: {}", subject);
                                }
                            } catch (Exception e) {
                                log.warn("STOMP JWT authentication failed: {}", e.getMessage());
                            }
                        }
                    }
                }
                return message;
            }
        });
    }
}
