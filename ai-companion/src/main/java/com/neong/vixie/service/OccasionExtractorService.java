package com.neong.vixie.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neong.vixie.model.ChatMessageDto;
import com.neong.vixie.model.OccasionType;
import com.neong.vixie.model.UserOccasion;
import com.neong.vixie.repository.UserOccasionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class OccasionExtractorService {

    private static final Pattern MM_DD = Pattern.compile("^(0[1-9]|1[0-2])-[0-3][0-9]$");

    private final GeminiService geminiService;
    private final UserOccasionRepository userOccasionRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Async
    public void extractAsync(String userId, String userMessage) {
        try {
            extract(userMessage).ifPresent(extracted -> save(userId, extracted));
        } catch (Exception e) {
            log.debug("Occasion extraction skipped for user={}: {}", userId, e.getMessage());
        }
    }

    Optional<ExtractedOccasion> extract(String userMessage) throws Exception {
        String prompt = """
                Extract any mention of the user's birthday, anniversary, or recurring special occasion.
                If none, return empty string.
                Output only JSON: {"type":"BIRTHDAY|ANNIVERSARY|CUSTOM","label":"...","date":"MM-DD"}.
                """;
        String response = geminiService.callChat(prompt, List.of(ChatMessageDto.of("user", userMessage)));
        String json = firstJsonObject(response);
        if (json == null) {
            return Optional.empty();
        }

        ExtractedOccasion extracted = objectMapper.readValue(json, ExtractedOccasion.class);
        if (extracted.type() == null || extracted.date() == null || !MM_DD.matcher(extracted.date()).matches()) {
            return Optional.empty();
        }
        return Optional.of(extracted);
    }

    private void save(String userId, ExtractedOccasion extracted) {
        OccasionType type = OccasionType.valueOf(extracted.type());
        UserOccasion occasion = userOccasionRepository
                .findByUserIdAndTypeAndOccasionDate(userId, type, extracted.date())
                .orElseGet(UserOccasion::new);

        occasion.setUserId(userId);
        occasion.setType(type);
        occasion.setLabel(extracted.label() == null || extracted.label().isBlank()
                ? type.name().toLowerCase()
                : extracted.label());
        occasion.setOccasionDate(extracted.date());
        occasion.setNotificationEnabled(true);
        occasion.setDetectedFromChat(true);
        occasion.setConfirmedByUser(false);
        userOccasionRepository.save(occasion);
    }

    private String firstJsonObject(String response) {
        if (response == null || response.isBlank()) {
            return null;
        }
        int start = response.indexOf('{');
        int end = response.lastIndexOf('}');
        if (start < 0 || end <= start) {
            return null;
        }
        return response.substring(start, end + 1).replace('\'', '"');
    }

    record ExtractedOccasion(String type, String label, String date) {}
}
