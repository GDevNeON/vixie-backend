package com.neong.vixie.controller;

import com.neong.vixie.model.ChatMessageDto;
import com.neong.vixie.model.ChatRequestEnvelope;
import com.neong.vixie.model.ChatResponseEnvelope;
import com.neong.vixie.repository.ConversationRepository;
import com.neong.vixie.service.CharacterPromptService;
import com.neong.vixie.service.GeminiService;
import com.neong.vixie.service.MoodAndXpBatchService;
import com.neong.vixie.service.OccasionExtractorService;
import com.neong.vixie.service.SummarizationService;
import com.neong.vixie.service.TtsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import reactor.core.publisher.Flux;

import java.security.Principal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit test for ChatController verifying STOMP message handling.
 */
@ExtendWith(MockitoExtension.class)
class ChatControllerTest {

    @Mock private GeminiService geminiService;
    @Mock private SimpMessagingTemplate messagingTemplate;
    @Mock private ConversationRepository conversationRepository;
    @Mock private CharacterPromptService characterPromptService;
    @Mock private SummarizationService summarizationService;
    @Mock private MoodAndXpBatchService moodAndXpBatchService;
    @Mock private TtsService ttsService;
    @Mock private OccasionExtractorService occasionExtractorService;
    @Mock private Principal principal;

    private ChatController controller;

    @BeforeEach
    void setUp() {
        controller = new ChatController(geminiService, messagingTemplate,
                conversationRepository, characterPromptService, summarizationService,
                moodAndXpBatchService, ttsService, occasionExtractorService);
    }

    @Test
    void handleChat_savesUserMessage() {
        when(principal.getName()).thenReturn("user_123");
        when(conversationRepository.getHistory("user_123", "char_default")).thenReturn(List.of());
        when(characterPromptService.buildSystemPrompt("user_123", "char_default")).thenReturn("system prompt");
        when(geminiService.streamChat(anyString(), anyList())).thenReturn(Flux.empty());

        ChatRequestEnvelope request = new ChatRequestEnvelope("char_default", "Hello!");
        controller.handleChat(request, principal);

        verify(conversationRepository).addMessage(eq("user_123"), eq("char_default"),
                argThat(msg -> msg.role().equals("user") && msg.content().equals("Hello!")));
        verify(occasionExtractorService).extractAsync("user_123", "Hello!");
    }

    @Test
    void handleChat_sendsChunksToUser() throws InterruptedException {
        when(principal.getName()).thenReturn("user_123");
        when(conversationRepository.getHistory("user_123", "char_default")).thenReturn(List.of());
        when(characterPromptService.buildSystemPrompt("user_123", "char_default")).thenReturn("prompt");
        when(geminiService.streamChat(anyString(), anyList()))
                .thenReturn(Flux.just("Hello", " there", "!"));

        controller.handleChat(new ChatRequestEnvelope("char_default", "Hi"), principal);

        // Allow async Flux to complete
        Thread.sleep(200);

        // Verify chunks sent
        ArgumentCaptor<ChatResponseEnvelope> captor =
                ArgumentCaptor.forClass(ChatResponseEnvelope.class);
        verify(messagingTemplate, atLeast(3))
                .convertAndSendToUser(eq("user_123"), eq("/queue/reply"), captor.capture());

        List<ChatResponseEnvelope> sent = captor.getAllValues();
        // Should have 3 chunks + 1 done
        assertTrue(sent.size() >= 4);
        assertEquals("chunk", sent.get(0).type());
        assertEquals("Hello", sent.get(0).text());
        assertEquals("done", sent.get(sent.size() - 1).type());
    }

    @Test
    void handleChat_savesAssistantResponseOnComplete() throws InterruptedException {
        when(principal.getName()).thenReturn("user_123");
        when(conversationRepository.getHistory("user_123", "char_default")).thenReturn(List.of());
        when(characterPromptService.buildSystemPrompt("user_123", "char_default")).thenReturn("prompt");
        when(geminiService.streamChat(anyString(), anyList()))
                .thenReturn(Flux.just("AI ", "response"));

        controller.handleChat(new ChatRequestEnvelope("char_default", "Hi"), principal);

        Thread.sleep(200);

        // Verify assistant response saved (2 calls: user + assistant)
        verify(conversationRepository, times(2))
                .addMessage(eq("user_123"), eq("char_default"), any());

        // Second call should be the assistant message
        ArgumentCaptor<ChatMessageDto> captor = ArgumentCaptor.forClass(ChatMessageDto.class);
        verify(conversationRepository, times(2))
                .addMessage(eq("user_123"), eq("char_default"), captor.capture());

        ChatMessageDto assistantMsg = captor.getAllValues().get(1);
        assertEquals("assistant", assistantMsg.role());
        assertEquals("AI response", assistantMsg.content());
    }

    @Test
    void handleChat_sendsErrorOnFailure() throws InterruptedException {
        when(principal.getName()).thenReturn("user_123");
        when(conversationRepository.getHistory("user_123", "char_default")).thenReturn(List.of());
        when(characterPromptService.buildSystemPrompt("user_123", "char_default")).thenReturn("prompt");
        when(geminiService.streamChat(anyString(), anyList()))
                .thenReturn(Flux.error(new RuntimeException("API error")));

        controller.handleChat(new ChatRequestEnvelope("char_default", "Hi"), principal);

        Thread.sleep(200);

        verify(messagingTemplate).convertAndSendToUser(
                eq("user_123"), eq("/queue/reply"),
                argThat(env -> env instanceof ChatResponseEnvelope cre
                        && cre.type().equals("error")));
    }

    @Test
    void handleChat_emitsTtsTriggerOnComplete() throws InterruptedException {
        when(principal.getName()).thenReturn("user_123");
        when(conversationRepository.getHistory("user_123", "char_default")).thenReturn(List.of());
        when(characterPromptService.buildSystemPrompt("user_123", "char_default")).thenReturn("prompt");
        when(geminiService.streamChat(anyString(), anyList()))
                .thenReturn(Flux.just("AI ", "response"));

        com.neong.vixie.dto.TtsTriggerPayload mockPayload = 
                new com.neong.vixie.dto.TtsTriggerPayload("AI response");
        when(ttsService.generateTtsPayload("AI response", "char_default"))
                .thenReturn(mockPayload);

        controller.handleChat(new ChatRequestEnvelope("char_default", "Hi"), principal);

        Thread.sleep(200);

        verify(messagingTemplate).convertAndSendToUser(
                eq("user_123"), eq("/queue/tts"),
                eq(mockPayload));
    }
}
