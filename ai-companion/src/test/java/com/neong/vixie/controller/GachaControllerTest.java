package com.neong.vixie.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.neong.vixie.helpers.api.GlobalExceptionHandler;
import com.neong.vixie.service.GachaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class GachaControllerTest {

    private MockMvc mockMvc;

    @Mock
    private GachaService gachaService;

    @InjectMocks
    private GachaController gachaController;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        converter.setObjectMapper(objectMapper);

        mockMvc = MockMvcBuilders.standaloneSetup(gachaController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(converter)
                .build();
    }

    @Test
    void pull_success_returnsPullResponse() throws Exception {
        GachaController.PullRequest req = new GachaController.PullRequest("banner_1", 1);
        
        List<GachaService.PullResultItem> items = List.of(
                new GachaService.PullResultItem("item_1", "Test Item", "COMMON", "url", true)
        );
        GachaService.PullResponse resp = new GachaService.PullResponse(items, 95, 5);

        when(gachaService.pull(eq("user_1"), eq("banner_1"), eq(1))).thenReturn(resp);

        mockMvc.perform(post("/api/gacha/pull")
                        .principal(() -> "user_1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.new_balance").value(95))
                .andExpect(jsonPath("$.items[0].item_id").value("item_1"));
    }
}
