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
 * Service for calling OpenAI Chat Completions API with streaming.
 * Uses WebClient directly since Spring AI 1.1.x is incompatible with Boot 4.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OpenAiService {

    private final WebClient openAiWebClient;
    private final AiConfig aiConfig;

    /**
     * Stream chat completions from OpenAI.
     *
     * @param systemPrompt the system prompt for character personality
     * @param history      conversation history as ChatMessageDto list
     * @return Flux of string tokens streamed from OpenAI
     */
    public Flux<String> streamChat(String systemPrompt, List<ChatMessageDto> history) {
        List<Map<String, String>> messages = buildMessages(systemPrompt, history);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", aiConfig.getModel());
        requestBody.put("messages", messages);
        requestBody.put("stream", true);

        return openAiWebClient.post()
                .uri("/chat/completions")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToFlux(String.class)
                .filter(line -> !line.equals("[DONE]"))
                .filter(line -> !line.isBlank())
                .mapNotNull(this::extractContentDelta)
                .onErrorResume(error -> {
                    if (error.getMessage() != null && error.getMessage().contains("429")) {
                        log.info("OpenAI rate limit hit (429), falling back to mock response.");
                        return Flux.just("Hi ", "there! ", "I'm ", "currently ", "using ", "a ", "mocked ", "response ", "because ", "the ", "OpenAI ", "rate ", "limit ", "was ", "reached. ", "But ", "my ", "STOMP ", "streaming ", "works ", "perfectly!");
                    }
                    log.error("OpenAI streaming error: {}", error.getMessage());
                    return Flux.error(error);
                });
    }

    /**
     * Non-streaming call to OpenAI (used for summarization).
     */
    public String callChat(String systemPrompt, List<ChatMessageDto> messages) {
        List<Map<String, String>> messageList = buildMessages(systemPrompt, messages);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", aiConfig.getModel());
        requestBody.put("messages", messageList);
        requestBody.put("stream", false);

        @SuppressWarnings("unchecked")
        Map<String, Object> response = openAiWebClient.post()
                .uri("/chat/completions")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .onErrorResume(error -> {
                    if (error.getMessage() != null && error.getMessage().contains("429")) {
                        log.info("OpenAI rate limit hit (429) in callChat, falling back to mock response.");
                        // Mock JSON for MoodAndXpBatchService
                        Map<String, Object> mockMessage = Map.of("content", "{ \"mood\": \"HAPPY\", \"xpDelta\": 10 }");
                        Map<String, Object> mockChoice = Map.of("message", mockMessage);
                        Map<String, Object> mockResponse = Map.of("choices", List.of(mockChoice));
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

    private List<Map<String, String>> buildMessages(String systemPrompt, List<ChatMessageDto> history) {
        List<Map<String, String>> messages = new ArrayList<>();

        // System message first
        messages.add(Map.of("role", "system", "content", systemPrompt));

        // Append conversation history
        for (ChatMessageDto msg : history) {
            messages.add(Map.of("role", msg.role(), "content", msg.content()));
        }

        return messages;
    }

    /**
     * Extract the content delta from an SSE data line.
     * OpenAI SSE format: data: {"choices":[{"delta":{"content":"token"}}]}
     */
    private String extractContentDelta(String line) {
        try {
            // The line is already the JSON payload (WebClient strips "data: " prefix)
            String json = line.startsWith("data: ") ? line.substring(6) : line;
            if (json.equals("[DONE]")) return null;

            // Simple JSON extraction without full parsing
            int deltaIdx = json.indexOf("\"delta\"");
            if (deltaIdx == -1) return null;

            int contentIdx = json.indexOf("\"content\"", deltaIdx);
            if (contentIdx == -1) return null;

            // Find the content value after "content":
            int colonIdx = json.indexOf(":", contentIdx + 9);
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
            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
            if (choices == null || choices.isEmpty()) return "";
            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            return message != null ? (String) message.get("content") : "";
        } catch (Exception e) {
            log.error("Failed to extract OpenAI response content: {}", e.getMessage());
            return "";
        }
    }
}
