package com.neong.vixie.controller;

import com.neong.vixie.model.ChatMessageDto;
import com.neong.vixie.model.ChatRequestEnvelope;
import com.neong.vixie.model.ChatResponseEnvelope;
import com.neong.vixie.repository.ConversationRepository;
import com.neong.vixie.service.CharacterPromptService;
import com.neong.vixie.service.OpenAiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.List;

/**
 * STOMP WebSocket controller for AI companion chat.
 *
 * Incoming: client sends to /app/chat
 * Outgoing: server sends to /user/queue/reply
 *
 * Flow:
 * 1. Receive message from user
 * 2. Save to Redis conversation history
 * 3. Build system prompt from CharacterPromptService
 * 4. Stream OpenAI response to client via STOMP
 * 5. Save complete assistant response to Redis
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class ChatController {

    private final OpenAiService openAiService;
    private final SimpMessagingTemplate messagingTemplate;
    private final ConversationRepository conversationRepository;
    private final CharacterPromptService characterPromptService;

    @MessageMapping("/chat")
    public void handleChat(ChatRequestEnvelope request, Principal principal) {
        String userId = principal.getName();
        String characterId = request.characterId();
        String userMessage = request.message();

        log.info("Chat message from user={} character={}: {}",
                userId, characterId, userMessage.substring(0, Math.min(50, userMessage.length())));

        // 1. Save user message to conversation history
        conversationRepository.addMessage(userId, characterId,
                ChatMessageDto.of("user", userMessage));

        // 2. Retrieve history for context
        List<ChatMessageDto> history = conversationRepository.getHistory(userId, characterId);

        // 3. Build character system prompt
        String systemPrompt = characterPromptService.buildSystemPrompt(characterId);

        // 4. Stream OpenAI response to client
        StringBuilder fullResponse = new StringBuilder();

        openAiService.streamChat(systemPrompt, history)
                .subscribe(
                        // onNext: send each token chunk to the user
                        chunk -> {
                            fullResponse.append(chunk);
                            messagingTemplate.convertAndSendToUser(
                                    userId,
                                    "/queue/reply",
                                    ChatResponseEnvelope.chunk(chunk)
                            );
                        },
                        // onError: send error message
                        error -> {
                            log.error("OpenAI stream error for user={}: {}",
                                    userId, error.getMessage());
                            messagingTemplate.convertAndSendToUser(
                                    userId,
                                    "/queue/reply",
                                    ChatResponseEnvelope.error(error.getMessage())
                            );
                        },
                        // onComplete: send done signal and save assistant response
                        () -> {
                            messagingTemplate.convertAndSendToUser(
                                    userId,
                                    "/queue/reply",
                                    ChatResponseEnvelope.done()
                            );
                            // 5. Save the complete assistant response to history
                            if (!fullResponse.isEmpty()) {
                                conversationRepository.addMessage(userId, characterId,
                                        ChatMessageDto.of("assistant", fullResponse.toString()));
                            }
                            log.info("Chat response complete for user={}, length={}",
                                    userId, fullResponse.length());
                        }
                );
    }
}
