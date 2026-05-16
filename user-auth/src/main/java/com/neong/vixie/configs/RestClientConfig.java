package com.neong.vixie.configs;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * Configuration for inter-service communication.
 * user-auth calls ai-companion to validate marketplace items during purchase.
 */
@Configuration
public class RestClientConfig {

    @Value("${ai-companion.base-url:http://localhost:8081}")
    private String aiCompanionBaseUrl;

    @Value("${ai-companion.service-key:#{null}}")
    private String serviceKey;

    @Bean
    public RestClient aiCompanionRestClient() {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl(aiCompanionBaseUrl);

        if (serviceKey != null && !serviceKey.isBlank()) {
            builder.defaultHeader("X-Service-Key", serviceKey);
        }

        return builder.build();
    }
}
