package com.neong.vixie.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * OpenAI API configuration for AI companion chat.
 *
 * Uses WebClient directly instead of Spring AI framework because
 * Spring AI 1.1.x is incompatible with Spring Boot 4 / Spring Framework 7
 * (NoSuchMethodError on HttpHeaders). When Spring AI 2.x GA is released
 * for Boot 4, this can be migrated to use ChatClient.
 *
 * Reads existing ai.openai.* property keys from application.properties.
 */
@Configuration
public class AiConfig {

    @Value("${ai.openai.api-key}")
    private String apiKey;

    @Value("${ai.openai.model:gpt-4o}")
    private String model;

    /**
     * WebClient pre-configured with OpenAI API base URL and Authorization header.
     */
    @Bean
    public WebClient openAiWebClient() {
        return WebClient.builder()
                .baseUrl("https://api.openai.com/v1")
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    /**
     * Returns the configured OpenAI model name for use by services.
     */
    public String getModel() {
        return model;
    }
}
