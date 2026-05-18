package com.buddkitv2.domain.search.service;

import com.buddkitv2.domain.club.repository.ClubOverlapProjection;
import com.buddkitv2.domain.club.repository.UserClubRepository;
import com.buddkitv2.domain.search.document.ClubDocument;
import com.buddkitv2.domain.search.dto.response.ClubSearchResponse;
import com.buddkitv2.domain.search.event.BehaviorEventPayload;
import com.buddkitv2.domain.search.repository.ClubSearchRepository;
import com.buddkitv2.domain.user.entity.User;
import com.buddkitv2.domain.user.repository.UserInterestRepository;
import com.buddkitv2.domain.user.repository.UserRepository;
import com.buddkitv2.global.exception.InvalidSearchConditionException;
import com.buddkitv2.global.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SearchService {

    private final ClubSearchRepository clubSearchRepository;
    private final UserClubRepository userClubRepository;
    private final UserInterestRepository userInterestRepository;
    private final UserRepository userRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public List<ClubSearchResponse> searchClubs(String keyword, String interestCategory,
                                                 String city, String district, int page, int size) {
        if (district != null && city == null) {
            throw new InvalidSearchConditionException();
        }
        List<ClubDocument> docs = clubSearchRepository.search(keyword, interestCategory, city, district, page, size);
        return docs.stream().map(ClubSearchResponse::from).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ClubSearchResponse> recommend(Long userId, int page, int size) {
        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
        List<String> interestCategories = userInterestRepository.findByUserIdWithInterest(userId)
                .stream()
                .map(ui -> ui.getInterest().getCategory().name())
                .collect(Collectors.toList());
        List<Long> myClubIds = userClubRepository.findClubIdsByUserId(userId);
        String city = user.getAddress() != null ? user.getAddress().getCity() : null;
        String district = user.getAddress() != null ? user.getAddress().getDistrict() : null;
        List<ClubDocument> docs = clubSearchRepository.recommend(interestCategories, city, district, myClubIds, page, size);
        return docs.stream().map(ClubSearchResponse::from).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ClubSearchResponse> coMemberRecommend(Long userId, int page, int size) {
        List<Long> myClubIds = userClubRepository.findClubIdsByUserId(userId);
        if (myClubIds.isEmpty()) {
            return List.of();
        }
        List<ClubOverlapProjection> projections = userClubRepository.findCoMemberClubs(
                userId, myClubIds, PageRequest.of(page, size));
        return projections.stream()
                .map(p -> ClubSearchResponse.fromClub(p.getClub()))
                .collect(Collectors.toList());
    }

    public void emitViewEvent(Long userId, Long clubId, Integer dwellSeconds) {
        try {
            BehaviorEventPayload payload = new BehaviorEventPayload(
                    userId, clubId, "VIEW", dwellSeconds, LocalDateTime.now());
            kafkaTemplate.send("user-behavior-events", String.valueOf(userId),
                    objectMapper.writeValueAsString(payload));
        } catch (Exception e) {
            // emit 실패는 무시 — Phase 2 데이터 파이프라인
        }
    }
}
