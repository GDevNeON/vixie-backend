package com.neong.vixie.service;

import com.neong.vixie.model.ChatMessageDto;
import com.neong.vixie.model.RelationshipState;
import com.neong.vixie.repository.RelationshipStateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MoodAndXpBatchServiceTest {

    @Mock
    private GeminiService geminiService;

    @Mock
    private MoodService moodService;

    @Mock
    private RelationshipStateRepository relationshipStateRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private TransactionTemplate transactionTemplate;

    @InjectMocks
    private MoodAndXpBatchService moodAndXpBatchService;

    @Captor
    private ArgumentCaptor<RelationshipState> relationshipStateCaptor;

    @BeforeEach
    void setUp() {
        // Mock transaction template to execute directly
        lenient().doAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(null);
        }).when(transactionTemplate).executeWithoutResult(any());
        
        lenient().when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(null);
        });
    }

    @Test
    void analyzeAndApplyBatch_UpdatesMoodAndXp() {
        String geminiResponse = "{\"mood\": \"HAPPY\", \"xpDelta\": 10}";
        when(geminiService.callChat(anyString(), anyList())).thenReturn(geminiResponse);
        when(moodService.getCurrentMood("user_1")).thenReturn("NEUTRAL");

        RelationshipState state = new RelationshipState();
        state.setId("rel_1");
        state.setUserId("user_1");
        state.setCharacterId("char_1");
        state.setLevel(1);
        state.setCurrentXp(50);
        state.setXpToNextLevel(100);

        when(relationshipStateRepository.findByUserIdAndCharacterId("user_1", "char_1"))
                .thenReturn(Optional.of(state));

        moodAndXpBatchService.analyzeAndApplyBatch("user_1", "char_1", List.of(
                ChatMessageDto.of("user", "I love this!")
        ));

        verify(moodService).setCurrentMood("user_1", "HAPPY");
        verify(messagingTemplate).convertAndSendToUser(eq("user_1"), eq("/queue/emotion"), any(Map.class));

        verify(relationshipStateRepository).save(relationshipStateCaptor.capture());
        RelationshipState savedState = relationshipStateCaptor.getValue();
        assertEquals(60, savedState.getCurrentXp());
        assertEquals(1, savedState.getLevel());
    }

    @Test
    void analyzeAndApplyBatch_LevelsUp() {
        String geminiResponse = "{\"mood\": \"HAPPY\", \"xpDelta\": 15}";
        when(geminiService.callChat(anyString(), anyList())).thenReturn(geminiResponse);
        when(moodService.getCurrentMood("user_1")).thenReturn("HAPPY");

        RelationshipState state = new RelationshipState();
        state.setId("rel_1");
        state.setUserId("user_1");
        state.setCharacterId("char_1");
        state.setLevel(1);
        state.setCurrentXp(90);
        state.setXpToNextLevel(100);

        when(relationshipStateRepository.findByUserIdAndCharacterId("user_1", "char_1"))
                .thenReturn(Optional.of(state));

        moodAndXpBatchService.analyzeAndApplyBatch("user_1", "char_1", List.of());

        // Mood is unchanged, so no emit
        verify(moodService, never()).setCurrentMood(anyString(), anyString());
        verify(messagingTemplate, never()).convertAndSendToUser(anyString(), anyString(), any());

        verify(relationshipStateRepository).save(relationshipStateCaptor.capture());
        RelationshipState savedState = relationshipStateCaptor.getValue();
        assertEquals(2, savedState.getLevel());
        assertEquals(5, savedState.getCurrentXp());
        assertEquals(200, savedState.getXpToNextLevel());
    }
}
