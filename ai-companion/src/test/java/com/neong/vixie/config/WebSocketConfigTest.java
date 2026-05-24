package com.neong.vixie.config;

import com.neong.vixie.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.StompWebSocketEndpointRegistration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class WebSocketConfigTest {

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private WebSocketConfig webSocketConfig;

    @Test
    void registerStompEndpoints_ShouldSetAllowedOrigins() {
        String[] allowedOrigins = new String[]{"http://localhost:3000"};
        ReflectionTestUtils.setField(webSocketConfig, "allowedOrigins", allowedOrigins);

        StompEndpointRegistry registry = mock(StompEndpointRegistry.class);
        StompWebSocketEndpointRegistration registration = mock(StompWebSocketEndpointRegistration.class);

        when(registry.addEndpoint("/ws/ai/chat")).thenReturn(registration);
        when(registration.setAllowedOrigins(any())).thenReturn(registration);

        webSocketConfig.registerStompEndpoints(registry);

        verify(registration, org.mockito.Mockito.times(2)).setAllowedOrigins(allowedOrigins);
        verify(registration).withSockJS();
    }
}
