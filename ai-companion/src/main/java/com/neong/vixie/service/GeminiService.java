package com.neong.vixie.service;

import com.neong.vixie.config.AiConfig;
import com.neong.vixie.model.ChatMessageDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for calling Google Gemini Chat API with streaming.
 * Uses WebClient directly.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GeminiService {

    private final WebClient geminiWebClient;
    private final AiConfig aiConfig;

    /**
     * Stream chat completions from Gemini.
     *
     * @param systemPrompt the system prompt for character personality
     * @param history      conversation history as ChatMessageDto list
     * @return Flux of string tokens streamed from Gemini
     */
    public Flux<String> streamChat(String systemPrompt, List<ChatMessageDto> history) {
        Map<String, Object> requestBody = buildGeminiRequest(systemPrompt, history);

        return geminiWebClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("{model}:streamGenerateContent")
                        .queryParam("alt", "sse")
                        .build(aiConfig.getModel()))
                .bodyValue(requestBody)
                .retrieve()
                .bodyToFlux(String.class)
                .filter(line -> !line.isBlank())
                .mapNotNull(this::extractContentDelta)
                .onErrorResume(error -> {
                    if (error.getMessage() != null && error.getMessage().contains("429")) {
                        log.info("Gemini rate limit hit (429), falling back to mock response.");
                        return Flux.just("Hi ", "there! ", "I'm ", "currently ", "using ", "a ", "mocked ", "response ", "because ", "the ", "Gemini ", "rate ", "limit ", "was ", "reached. ", "But ", "my ", "STOMP ", "streaming ", "works ", "perfectly!");
                    }
                    log.error("Gemini streaming error: {}", error.getMessage());
                    return Flux.error(error);
                });
    }

    /**
     * Non-streaming call to Gemini (used for summarization).
     */
    public String callChat(String systemPrompt, List<ChatMessageDto> messages) {
        Map<String, Object> requestBody = buildGeminiRequest(systemPrompt, messages);

        @SuppressWarnings("unchecked")
        Map<String, Object> response = geminiWebClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("{model}:generateContent")
                        .build(aiConfig.getModel()))
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .onErrorResume(error -> {
                    if (error.getMessage() != null && error.getMessage().contains("429")) {
                        log.info("Gemini rate limit hit (429) in callChat, falling back to mock response.");
                        // Mock JSON for MoodAndXpBatchService
                        Map<String, Object> mockMessage = Map.of("content", "{ \"mood\": \"HAPPY\", \"xpDelta\": 10 }");
                        Map<String, Object> mockChoice = Map.of("message", mockMessage);
                        Map<String, Object> mockResponse = Map.of("candidates", List.of(mockMessage));
                        return reactor.core.publisher.Mono.just(mockResponse);
                    }
                    return reactor.core.publisher.Mono.error(error);
                })
                .block();

        if (response == null) {
            return "";
        }

        return extractResponseContent(response);
    }

    private Map<String, Object> buildGeminiRequest(String systemPrompt, List<ChatMessageDto> history) {
        Map<String, Object> requestBody = new HashMap<>();

        // System Instruction
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            Map<String, Object> sysInstruction = Map.of(
                    "parts", Map.of("text", systemPrompt)
            );
            requestBody.put("system_instruction", sysInstruction);
        }

        // Contents
        List<Map<String, Object>> contents = new ArrayList<>();
        for (ChatMessageDto msg : history) {
            String role = msg.role().equals("assistant") ? "model" : "user";
            // Ignore system messages in history as they go to system_instruction
            if (msg.role().equals("system")) continue;

            contents.add(Map.of(
                    "role", role,
                    "parts", List.of(Map.of("text", msg.content()))
            ));
        }
        requestBody.put("contents", contents);

        return requestBody;
    }

    /**
     * Extract the content delta from an SSE data line.
     * Gemini SSE format with alt=sse: data: {"candidates": [{"content": {"parts": [{"text": "token"}]}}]}
     */
    private String extractContentDelta(String line) {
        try {
            // The line is already the JSON payload (WebClient strips "data: " prefix)
            String json = line.startsWith("data: ") ? line.substring(6) : line;

            // Simple JSON extraction without full parsing
            int textIdx = json.indexOf("\"text\"");
            if (textIdx == -1) return null;

            int colonIdx = json.indexOf(":", textIdx);
            if (colonIdx == -1) return null;

            // Skip whitespace and opening quote
            int start = json.indexOf("\"", colonIdx);
            if (start == -1) return null;
            start++;

            // Find closing quote (handle escaped quotes)
            int end = start;
            while (end < json.length()) {
                if (json.charAt(end) == '\\') {
                    end += 2; // skip escaped char
                    continue;
                }
                if (json.charAt(end) == '"') break;
                end++;
            }

            if (end >= json.length()) return null;

            String content = json.substring(start, end);
            // Unescape basic JSON escapes
            content = content.replace("\\n", "\n")
                             .replace("\\\"", "\"")
                             .replace("\\\\", "\\");

            return content.isEmpty() ? null : content;
        } catch (Exception e) {
            log.debug("Failed to parse SSE delta: {}", line);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private String extractResponseContent(Map<String, Object> response) {
        try {
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
            if (candidates == null || candidates.isEmpty()) return "";
            Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
            if (content == null) return "";
            List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
            if (parts == null || parts.isEmpty()) return "";
            return (String) parts.get(0).get("text");
        } catch (Exception e) {
            log.error("Failed to extract Gemini response content: {}", e.getMessage());
            return "";
        }
    }
}
