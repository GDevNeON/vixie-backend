package com.neong.vixie.controller;

import com.neong.vixie.dto.UserOccasionRequest;
import com.neong.vixie.dto.UserOccasionResponse;
import com.neong.vixie.model.OccasionType;
import com.neong.vixie.model.UserOccasion;
import com.neong.vixie.repository.UserOccasionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.security.Principal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserOccasionControllerTest {

    @Mock private UserOccasionRepository userOccasionRepository;
    @Mock private Principal principal;

    private UserOccasionController controller;

    @BeforeEach
    void setUp() {
        controller = new UserOccasionController(userOccasionRepository);
        when(principal.getName()).thenReturn("user_123");
    }

    @Test
    void getOccasions_returnsUserOccasions() {
        UserOccasion occasion = UserOccasion.builder()
                .id("occasion_1")
                .userId("user_123")
                .type(OccasionType.BIRTHDAY)
                .label("birthday")
                .occasionDate("03-15")
                .build();
        when(userOccasionRepository.findByUserId("user_123")).thenReturn(List.of(occasion));

        ResponseEntity<List<UserOccasionResponse>> response = controller.getOccasions(principal);

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals("03-15", response.getBody().get(0).occasionDate());
    }

    @Test
    void saveOccasion_confirmsChatDetectedOccasionFromApi() {
        when(userOccasionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        UserOccasionRequest request = new UserOccasionRequest(
                null, OccasionType.BIRTHDAY, "birthday", "03-15",
                true, true, false
        );

        controller.saveOccasion(request, principal);

        ArgumentCaptor<UserOccasion> captor = ArgumentCaptor.forClass(UserOccasion.class);
        verify(userOccasionRepository).save(captor.capture());
        assertTrue(captor.getValue().isDetectedFromChat());
        assertTrue(captor.getValue().isConfirmedByUser());
    }
}
