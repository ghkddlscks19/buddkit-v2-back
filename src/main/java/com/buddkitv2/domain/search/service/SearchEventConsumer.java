package com.buddkitv2.domain.search.service;

import com.buddkitv2.domain.search.document.ClubDocument;
import com.buddkitv2.domain.search.event.ClubEventPayload;
import com.buddkitv2.domain.search.repository.ClubSearchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class SearchEventConsumer {

    private final ClubSearchRepository clubSearchRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "club-events", groupId = "buddkit-search")
    public void handleClubEvent(String message) {
        try {
            ClubEventPayload payload = objectMapper.readValue(message, ClubEventPayload.class);
            if ("DELETED".equals(payload.getEventType())) {
                clubSearchRepository.deleteById(String.valueOf(payload.getClubId()));
            } else {
                clubSearchRepository.save(ClubDocument.from(payload));
            }
        } catch (Exception e) {
            // 역직렬화 실패 시 해당 메시지 스킵
        }
    }
}
