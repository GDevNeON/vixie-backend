package com.neong.vixie.service;

import com.neong.vixie.model.OccasionType;
import com.neong.vixie.model.UserOccasion;
import com.neong.vixie.repository.UserOccasionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OccasionExtractorServiceTest {

    @Mock private GeminiService geminiService;
    @Mock private UserOccasionRepository userOccasionRepository;

    private OccasionExtractorService service;

    @BeforeEach
    void setUp() {
        service = new OccasionExtractorService(geminiService, userOccasionRepository);
    }

    @Test
    void extractAsync_savesDetectedOccasionAsUnconfirmed() {
        when(geminiService.callChat(anyString(), anyList()))
                .thenReturn("{\"type\":\"BIRTHDAY\",\"label\":\"my birthday\",\"date\":\"03-15\"}");
        when(userOccasionRepository.findByUserIdAndTypeAndOccasionDate(
                "user_123", OccasionType.BIRTHDAY, "03-15"))
                .thenReturn(Optional.empty());

        service.extractAsync("user_123", "My birthday is March 15th");

        ArgumentCaptor<UserOccasion> captor = ArgumentCaptor.forClass(UserOccasion.class);
        verify(userOccasionRepository).save(captor.capture());
        assertEquals("user_123", captor.getValue().getUserId());
        assertEquals(OccasionType.BIRTHDAY, captor.getValue().getType());
        assertEquals("03-15", captor.getValue().getOccasionDate());
        assertTrue(captor.getValue().isDetectedFromChat());
        assertFalse(captor.getValue().isConfirmedByUser());
    }

    @Test
    void extractAsync_ignoresEmptyResponses() {
        when(geminiService.callChat(anyString(), anyList())).thenReturn("");

        service.extractAsync("user_123", "hello");

        verify(userOccasionRepository, never()).save(any());
    }
}
