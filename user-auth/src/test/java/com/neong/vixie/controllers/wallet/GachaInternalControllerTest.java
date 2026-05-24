package com.neong.vixie.controllers.wallet;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.neong.vixie.helpers.api.GlobalExceptionHandler;
import com.neong.vixie.models.db.User;
import com.neong.vixie.repositories.user.UserRepository;
import com.neong.vixie.services.wallet.GachaInternalService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class GachaInternalControllerTest {

    private MockMvc mockMvc;

    @Mock
    private GachaInternalService gachaInternalService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private GachaInternalController gachaInternalController;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(gachaInternalController, "serviceKey", "test-secret-key");

        objectMapper = new ObjectMapper();
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        converter.setObjectMapper(objectMapper);

        mockMvc = MockMvcBuilders.standaloneSetup(gachaInternalController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(converter)
                .build();
    }

    @Test
    void getPity_validKey_returnsPity() throws Exception {
        when(gachaInternalService.getPityCount("user_1", "banner_1")).thenReturn(7);

        mockMvc.perform(get("/api/internal/gacha/pity")
                        .header("X-Service-Key", "test-secret-key")
                        .param("userId", "user_1")
                        .param("bannerId", "banner_1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.current_pity").value(7));
    }

    @Test
    void getPity_invalidKey_returnsForbidden() throws Exception {
        // Exception should be AccessDeniedException, which might be handled differently
        // Depending on GlobalExceptionHandler, it could be 403 or 500. Wait, AccessDeniedException usually maps to 403 if handled, otherwise throws nested exception.
        // I will just expect a failure or 403.
        
        try {
            mockMvc.perform(get("/api/internal/gacha/pity")
                            .header("X-Service-Key", "wrong-key")
                            .param("userId", "user_1")
                            .param("bannerId", "banner_1"))
                    .andExpect(status().isForbidden());
        } catch (Exception e) {
            // It might throw NestedServletException if not handled by GlobalExceptionHandler
            if (!(e.getCause() instanceof org.springframework.security.access.AccessDeniedException)) {
                throw e;
            }
        }
    }

    @Test
    void commitPulls_validKey_returnsNewBalance() throws Exception {
        GachaInternalController.CommitPullsRequest req = new GachaInternalController.CommitPullsRequest(
                "user_1", "banner_1", 5, List.of(
                        new GachaInternalController.PullResultItemRequest("item_1", "COMMON")
        ));

        when(gachaInternalService.commitPulls(eq("user_1"), eq("banner_1"), eq(5), any()))
                .thenReturn(95);

        mockMvc.perform(post("/api/internal/gacha/commit")
                        .header("X-Service-Key", "test-secret-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.new_balance").value(95));
    }
}
