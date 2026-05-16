package com.neong.vixie.service;

import com.neong.vixie.model.ChatMessageDto;
import com.neong.vixie.repository.ConversationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit test for SummarizationService.
 */
@ExtendWith(MockitoExtension.class)
class SummarizationServiceTest {

    @Mock private ConversationRepository conversationRepository;
    @Mock private GeminiService geminiService;
    @Mock private UserInteractionProfileService profileService;

    private SummarizationService service;

    private static final String USER_ID = "user_123";
    private static final String CHAR_ID = "char_default";

    @BeforeEach
    void setUp() {
        service = new SummarizationService(conversationRepository, geminiService, profileService);
    }

    @Test
    void summarizeIfNeeded_skipsWhenBelowThreshold() {
        when(conversationRepository.getHistorySize(USER_ID, CHAR_ID)).thenReturn(10L);

        service.summarizeIfNeeded(USER_ID, CHAR_ID);

        verify(conversationRepository, never()).getHistory(anyString(), anyString());
        verify(geminiService, never()).callChat(anyString(), anyList());
    }

    @Test
    void summarizeIfNeeded_summarizesWhenAboveThreshold() {
        when(conversationRepository.getHistorySize(USER_ID, CHAR_ID)).thenReturn(18L);

        // Build a history of 18 messages
        List<ChatMessageDto> history = new ArrayList<>();
        for (int i = 0; i < 18; i++) {
            history.add(ChatMessageDto.of(i % 2 == 0 ? "user" : "assistant", "Message " + i));
        }
        when(conversationRepository.getHistory(USER_ID, CHAR_ID)).thenReturn(history);
        when(geminiService.callChat(anyString(), anyList()))
                .thenReturn("The user discussed their favorite food and music preferences.");

        service.summarizeIfNeeded(USER_ID, CHAR_ID);

        // Verify OpenAI was called with the oldest 10 messages
        ArgumentCaptor<List<ChatMessageDto>> messagesCaptor =
                ArgumentCaptor.forClass(List.class);
        verify(geminiService).callChat(anyString(), messagesCaptor.capture());
        assertEquals(10, messagesCaptor.getValue().size());

        // Verify Redis was updated with summary
        ArgumentCaptor<ChatMessageDto> summaryCaptor =
                ArgumentCaptor.forClass(ChatMessageDto.class);
        verify(conversationRepository).replaceOldestWithSummary(
                eq(USER_ID), eq(CHAR_ID), eq(10), summaryCaptor.capture());

        ChatMessageDto summary = summaryCaptor.getValue();
        assertEquals("system", summary.role());
        assertTrue(summary.content().startsWith("[Conversation Summary]"));
    }

    @Test
    void summarizeIfNeeded_handlesEmptySummaryGracefully() {
        when(conversationRepository.getHistorySize(USER_ID, CHAR_ID)).thenReturn(20L);

        List<ChatMessageDto> history = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            history.add(ChatMessageDto.of("user", "msg " + i));
        }
        when(conversationRepository.getHistory(USER_ID, CHAR_ID)).thenReturn(history);
        when(geminiService.callChat(anyString(), anyList())).thenReturn("");

        service.summarizeIfNeeded(USER_ID, CHAR_ID);

        // Should NOT replace if summary is blank
        verify(conversationRepository, never())
                .replaceOldestWithSummary(anyString(), anyString(), anyInt(), any());
    }

    @Test
    void summarizeIfNeeded_handlesOpenAiErrorGracefully() {
        when(conversationRepository.getHistorySize(USER_ID, CHAR_ID)).thenReturn(20L);

        List<ChatMessageDto> history = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            history.add(ChatMessageDto.of("user", "msg " + i));
        }
        when(conversationRepository.getHistory(USER_ID, CHAR_ID)).thenReturn(history);
        when(geminiService.callChat(anyString(), anyList()))
                .thenThrow(new RuntimeException("OpenAI rate limited"));

        // Should NOT throw — summarization failure is non-critical
        assertDoesNotThrow(() -> service.summarizeIfNeeded(USER_ID, CHAR_ID));

        verify(conversationRepository, never())
                .replaceOldestWithSummary(anyString(), anyString(), anyInt(), any());
    }
}
