package com.neong.vixie.service;

import com.neong.vixie.model.ChatMessageDto;
import com.neong.vixie.repository.ConversationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service for summarizing conversation history when it exceeds max capacity.
 *
 * When conversation history reaches 20 messages, extracts the oldest 10,
 * sends them to OpenAI for summarization, and replaces them with a single
 * system summary message in Redis.
 *
 * This preserves conversational context while keeping the token window small.
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
            "Do NOT include greetings or meta-commentary.";

    private final ConversationRepository conversationRepository;
    private final OpenAiService openAiService;

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

            // Call OpenAI to generate a summary (non-streaming)
            String summary = openAiService.callChat(SUMMARIZATION_PROMPT, toSummarize);

            if (summary == null || summary.isBlank()) {
                log.warn("Summarization returned empty result for user={}", userId);
                return;
            }

            // Replace old messages with the summary
            ChatMessageDto summaryDto = ChatMessageDto.of("system",
                    "[Conversation Summary] " + summary);

            conversationRepository.replaceOldestWithSummary(
                    userId, characterId, MESSAGES_TO_SUMMARIZE, summaryDto);

            log.info("Summarized {} messages for user={}, new history size: {}",
                    MESSAGES_TO_SUMMARIZE, userId,
                    conversationRepository.getHistorySize(userId, characterId));

        } catch (Exception e) {
            log.error("Summarization failed for user={}: {}", userId, e.getMessage(), e);
            // Non-critical failure — conversation continues without summarization
        }
    }
}
