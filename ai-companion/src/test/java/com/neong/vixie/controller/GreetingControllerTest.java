package com.neong.vixie.controller;

import com.neong.vixie.service.GreetingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.security.Principal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GreetingControllerTest {

    @Mock
    private GreetingService greetingService;

    @Mock
    private Principal principal;

    @InjectMocks
    private GreetingController greetingController;

    @Test
    void getDailyGreeting_WithValidPrincipal_ReturnsOk() {
        when(principal.getName()).thenReturn("user_1");
        when(greetingService.getDailyGreeting("user_1", "char_1", null))
                .thenReturn(Map.of("greeted", false, "message", "Hello!"));

        ResponseEntity<Map<String, Object>> response = greetingController.getDailyGreeting("char_1", null, principal);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(false, response.getBody().get("greeted"));
        assertEquals("Hello!", response.getBody().get("message"));
        verify(greetingService).getDailyGreeting("user_1", "char_1", null);
    }

    @Test
    void getDailyGreeting_WithMissingPrincipal_ReturnsUnauthorized() {
        ResponseEntity<Map<String, Object>> response = greetingController.getDailyGreeting("char_1", null, null);

        assertEquals(401, response.getStatusCode().value());
        verifyNoInteractions(greetingService);
    }
}
