package com.neong.vixie.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Gemini API configuration for AI companion chat.
 *
 * Uses WebClient directly.
 *
 * Reads existing ai.gemini.* property keys from application.properties.
 */
@Configuration
public class AiConfig {

    @Value("${ai.gemini.api-key}")
    private String apiKey;

    @Value("${ai.gemini.model:gemini-3.1-flash-lite}")
    private String model;

    /**
     * WebClient pre-configured with Gemini API base URL and API key header.
     */
    @Bean
    public WebClient geminiWebClient() {
        return WebClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com/v1beta/models/")
                .defaultHeader("x-goog-api-key", apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    /**
     * Returns the configured Gemini model name for use by services.
     */
    public String getModel() {
        return model;
    }
}
