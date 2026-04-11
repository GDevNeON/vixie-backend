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
 * Verifies that the OpenAI WebClient bean is correctly configured
 * and available in the application context.
 */
@SpringJUnitConfig(classes = AiConfig.class)
@TestPropertySource(properties = {
        "ai.openai.api-key=test-key-not-real",
        "ai.openai.model=gpt-4o"
})
class AiConfigTest {

    @Autowired
    private ApplicationContext context;

    @Autowired
    private AiConfig aiConfig;

    @Test
    void openAiWebClientBeanExists() {
        WebClient webClient = context.getBean("openAiWebClient", WebClient.class);
        assertNotNull(webClient, "OpenAI WebClient bean should be configured");
    }

    @Test
    void modelConfigurationIsCorrect() {
        assertEquals("gpt-4o", aiConfig.getModel(), "Default model should be gpt-4o");
    }
}
