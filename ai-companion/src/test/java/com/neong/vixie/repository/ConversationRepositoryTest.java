package com.neong.vixie.repository;

import com.neong.vixie.model.ChatMessageDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit test for ConversationRepository using mocked RedisTemplate.
 * Verifies list operations, trim cap, and TTL behavior.
 */
@ExtendWith(MockitoExtension.class)
class ConversationRepositoryTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ListOperations<String, Object> listOps;

    private ConversationRepository repository;

    private static final String USER_ID = "user_123";
    private static final String CHAR_ID = "char_default";
    private static final String EXPECTED_KEY = "vixie:chat:user_123:char_default:history";

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForList()).thenReturn(listOps);
        repository = new ConversationRepository(redisTemplate);
    }

    @Test
    void addMessage_pushesToListAndTrimsAndSetsExpiry() {
        ChatMessageDto msg = ChatMessageDto.of("user", "Hello!");

        repository.addMessage(USER_ID, CHAR_ID, msg);

        verify(listOps).rightPush(EXPECTED_KEY, msg);
        verify(listOps).trim(EXPECTED_KEY, -20, -1);
        verify(redisTemplate).expire(EXPECTED_KEY, Duration.ofHours(48));
    }

    @Test
    void getHistory_returnsEmptyListWhenNoHistory() {
        when(listOps.range(EXPECTED_KEY, 0, -1)).thenReturn(null);

        List<ChatMessageDto> result = repository.getHistory(USER_ID, CHAR_ID);

        assertTrue(result.isEmpty());
    }

    @Test
    void getHistory_returnsCastMessages() {
        ChatMessageDto msg1 = ChatMessageDto.of("user", "Hi");
        ChatMessageDto msg2 = ChatMessageDto.of("assistant", "Hello!");
        when(listOps.range(EXPECTED_KEY, 0, -1)).thenReturn(List.of(msg1, msg2));

        List<ChatMessageDto> result = repository.getHistory(USER_ID, CHAR_ID);

        assertEquals(2, result.size());
        assertEquals("user", result.get(0).role());
        assertEquals("assistant", result.get(1).role());
    }

    @Test
    void getHistorySize_returnsCorrectSize() {
        when(listOps.size(EXPECTED_KEY)).thenReturn(15L);

        long size = repository.getHistorySize(USER_ID, CHAR_ID);

        assertEquals(15L, size);
    }

    @Test
    void replaceOldestWithSummary_trimsAndLeftPushes() {
        ChatMessageDto summary = ChatMessageDto.of("system", "Summary of previous events...");

        repository.replaceOldestWithSummary(USER_ID, CHAR_ID, 10, summary);

        verify(listOps).trim(EXPECTED_KEY, 10, -1);
        verify(listOps).leftPush(EXPECTED_KEY, summary);
        verify(redisTemplate).expire(EXPECTED_KEY, Duration.ofHours(48));
    }
}
