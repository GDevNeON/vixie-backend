package com.neong.vixie.controller;

import com.neong.vixie.model.ChatMessageDto;
import com.neong.vixie.repository.ConversationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/character")
@RequiredArgsConstructor
@Slf4j
public class HistoryController {

    private final ConversationRepository conversationRepository;

    @GetMapping("/{characterId}/history")
    public ResponseEntity<List<ChatMessageDto>> getHistory(
            @PathVariable String characterId,
            Principal principal) {
            
        String userId = principal.getName();
        log.info("Fetching history for user={} character={}", userId, characterId);
        
        List<ChatMessageDto> history = conversationRepository.getHistory(userId, characterId);
        
        return ResponseEntity.ok(history);
    }
}
