package com.neong.vixie.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.web.reactive.function.client.WebClient;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifies that the Gemini WebClient bean is correctly configured
 * and available in the application context.
 */
@SpringJUnitConfig(classes = AiConfig.class)
@TestPropertySource(properties = {
        "ai.gemini.api-key=test-key-not-real",
        "ai.gemini.model=gemini-3.1-flash-lite"
})
class AiConfigTest {

    @Autowired
    private ApplicationContext context;

    @Autowired
    private AiConfig aiConfig;

    @Test
    void geminiWebClientBeanExists() {
        WebClient webClient = context.getBean("geminiWebClient", WebClient.class);
        assertNotNull(webClient, "Gemini WebClient bean should be configured");
    }

    @Test
    void modelConfigurationIsCorrect() {
        assertEquals("gemini-3.1-flash-lite", aiConfig.getModel(), "Default model should be gemini-3.1-flash-lite");
    }
}
