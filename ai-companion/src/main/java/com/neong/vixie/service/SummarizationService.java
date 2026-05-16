package com.neong.vixie.service;

import com.neong.vixie.model.ChatMessageDto;
import com.neong.vixie.repository.ConversationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for summarizing conversation history when it exceeds max capacity.
 *
 * When conversation history reaches 20 messages, extracts the oldest 10,
 * sends them to OpenAI for summarization, and replaces them with a single
 * system summary message in Redis.
 *
 * Phase 8: Also extracts conversation topics from the summary to feed into
 * UserInteractionProfileService for preference learning.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SummarizationService {

    private static final int SUMMARIZATION_THRESHOLD = 18;
    private static final int MESSAGES_TO_SUMMARIZE = 10;

    private static final String SUMMARIZATION_PROMPT =
            "Summarize the following conversation between a user and an AI companion " +
            "named Hana. Capture the key topics discussed, any personal details the " +
            "user shared, emotional tone, and important context that should be " +
            "remembered for future conversations. " +
            "Write the summary as a brief paragraph in third person. " +
            "Do NOT include greetings or meta-commentary. " +
            "After the summary paragraph, on a new line output ONLY: " +
            "TOPICS: topic1, topic2, topic3 " +
            "(list the 2-5 main conversation topics as comma-separated keywords)";

    /** Pattern to extract topics line from LLM response. */
    private static final Pattern TOPICS_PATTERN =
            Pattern.compile("TOPICS:\\s*(.+)", Pattern.CASE_INSENSITIVE);

    private final ConversationRepository conversationRepository;
    private final GeminiService geminiService;
    private final UserInteractionProfileService profileService;

    /**
     * Check if summarization is needed and perform it if so.
     * Should be called after adding a new message to conversation history.
     *
     * @param userId      the user ID
     * @param characterId the character ID
     */
    public void summarizeIfNeeded(String userId, String characterId) {
        long historySize = conversationRepository.getHistorySize(userId, characterId);

        if (historySize < SUMMARIZATION_THRESHOLD) {
            return;
        }

        log.info("History size {} for user={} character={} exceeds threshold {}, summarizing...",
                historySize, userId, characterId, SUMMARIZATION_THRESHOLD);

        try {
            List<ChatMessageDto> history = conversationRepository.getHistory(userId, characterId);
            if (history.size() < MESSAGES_TO_SUMMARIZE) {
                return;
            }

            // Extract the oldest N messages to summarize
            List<ChatMessageDto> toSummarize = history.subList(0, MESSAGES_TO_SUMMARIZE);

            // Calculate average user message length for profile update
            double avgLength = toSummarize.stream()
                    .filter(m -> "user".equals(m.role()))
                    .mapToInt(m -> m.content().split("\\s+").length)
                    .average()
                    .orElse(0.0);

            // Call Gemini to generate a summary (non-streaming)
            String fullResponse = geminiService.callChat(SUMMARIZATION_PROMPT, toSummarize);

            if (fullResponse == null || fullResponse.isBlank()) {
                log.warn("Summarization returned empty result for user={}", userId);
                return;
            }

            // Extract topics from response (Phase 8)
            List<String> topics = extractTopics(fullResponse);

            // Strip the TOPICS line from the summary text
            String summary = fullResponse.replaceAll("(?i)\\nTOPICS:.*", "").trim();

            // Replace old messages with the summary
            ChatMessageDto summaryDto = ChatMessageDto.of("system",
                    "[Conversation Summary] " + summary);

            conversationRepository.replaceOldestWithSummary(
                    userId, characterId, MESSAGES_TO_SUMMARIZE, summaryDto);

            log.info("Summarized {} messages for user={}, new history size: {}",
                    MESSAGES_TO_SUMMARIZE, userId,
                    conversationRepository.getHistorySize(userId, characterId));

            // Update user interaction profile with extracted metrics (Phase 8)
            try {
                profileService.updateConversationMetrics(userId, avgLength, topics);
            } catch (Exception e) {
                log.warn("Failed to update interaction profile metrics for user={}: {}",
                        userId, e.getMessage());
            }

        } catch (Exception e) {
            log.error("Summarization failed for user={}: {}", userId, e.getMessage(), e);
            // Non-critical failure — conversation continues without summarization
        }
    }

    /**
     * Extract topic keywords from the LLM response.
     * Looks for "TOPICS: topic1, topic2, ..." line.
     */
    private List<String> extractTopics(String response) {
        Matcher matcher = TOPICS_PATTERN.matcher(response);
        if (matcher.find()) {
            String topicsStr = matcher.group(1).trim();
            List<String> topics = new ArrayList<>();
            for (String topic : topicsStr.split(",")) {
                String trimmed = topic.trim().toLowerCase();
                if (!trimmed.isEmpty()) {
                    topics.add(trimmed);
                }
            }
            return topics;
        }
        return List.of();
    }
}

