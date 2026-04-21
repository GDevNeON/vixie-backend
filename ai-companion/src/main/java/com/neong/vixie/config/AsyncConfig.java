package com.neong.vixie.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Enables async method execution for @Async annotation support.
 * Used by MoodAndXpBatchService to run batch analysis without blocking the chat stream.
 */
@Configuration
@EnableAsync
public class AsyncConfig {
}
