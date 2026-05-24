package com.neong.vixie.service;

import com.neong.vixie.model.ChatMessageDto;
import com.neong.vixie.repository.ConversationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SummarizationBehavioralTest {

    @Mock private GeminiService geminiService;
    @Mock private ConversationRepository conversationRepository;
    @Mock private UserInteractionProfileService profileService;

    private SummarizationService service;

    @BeforeEach
    void setUp() {
        service = new SummarizationService(conversationRepository, geminiService, profileService);
    }

    @Test
    void summarizeIfNeeded_doesNotTrigger_whenHistoryIsLessThan15() {
        when(conversationRepository.getHistorySize("user1", "char1")).thenReturn(14L);
        
        service.summarizeIfNeeded("user1", "char1");
        
        verify(geminiService, never()).callChat(anyString(), any());
        verify(conversationRepository, never()).replaceOldestWithSummary(anyString(), anyString(), anyInt(), any());
    }

    @Test
    void summarizeIfNeeded_triggers_whenHistoryExceeds15() {
        when(conversationRepository.getHistorySize("user1", "char1")).thenReturn(16L);
        
        List<ChatMessageDto> oldestMessages = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            oldestMessages.add(ChatMessageDto.of("user", "message " + i));
        }
        when(conversationRepository.getHistory("user1", "char1")).thenReturn(oldestMessages);
        when(geminiService.callChat(anyString(), any())).thenReturn("Summary text");

        service.summarizeIfNeeded("user1", "char1");
        
        verify(geminiService).callChat(anyString(), any());
        verify(conversationRepository).replaceOldestWithSummary(
                eq("user1"), eq("char1"), eq(10),
                any(ChatMessageDto.class)
        );
    }
}
