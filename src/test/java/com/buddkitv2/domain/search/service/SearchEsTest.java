package com.buddkitv2.domain.search.service;

import com.buddkitv2.domain.search.document.ClubDocument;
import com.buddkitv2.domain.search.event.ClubEventPayload;
import com.buddkitv2.domain.search.repository.ClubSearchRepository;
import com.buddkitv2.global.config.S3Service;
import com.buddkitv2.global.config.TossPaymentClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class SearchEsTest {

    @Autowired SearchService searchService;
    @Autowired ClubSearchRepository clubSearchRepository;

    @MockitoBean KafkaTemplate<String, String> kafkaTemplate;
    @MockitoBean TossPaymentClient tossPaymentClient;
    @MockitoBean S3Service s3Service;

    @BeforeEach
    void setUp() {
        ClubDocument doc1 = ClubDocument.from(new ClubEventPayload(
                "CREATED", 1L, "서울 러닝크루", "매주 한강에서 달리는 모임",
                "서울특별시", "마포구", "SPORTS", "운동/스포츠", 10, 30, null, null));
        ClubDocument doc2 = ClubDocument.from(new ClubEventPayload(
                "CREATED", 2L, "부산 독서모임", "한 달에 한 번 책 이야기",
                "부산광역시", "해운대구", "CULTURE", "문화", 5, 20, null, null));
        ClubDocument doc3 = ClubDocument.from(new ClubEventPayload(
                "CREATED", 3L, "서울 축구 동호회", "주말 풋살 함께해요",
                "서울특별시", "강남구", "SPORTS", "운동/스포츠", 20, 30, null, null));
        clubSearchRepository.saveAll(List.of(doc1, doc2, doc3));
        try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
    }

    @AfterEach
    void tearDown() {
        clubSearchRepository.deleteAll();
    }

    @Test
    void search_키워드_모임명매칭() {
        List<ClubDocument> result = clubSearchRepository.search("러닝", null, null, null, 0, 10);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("서울 러닝크루");
    }

    @Test
    void search_지역필터_서울만반환() {
        List<ClubDocument> result = clubSearchRepository.search(null, null, "서울특별시", null, 0, 10);
        assertThat(result).hasSize(2);
        assertThat(result).extracting(ClubDocument::getCity).containsOnly("서울특별시");
    }

    @Test
    void search_관심사필터_스포츠만반환() {
        List<ClubDocument> result = clubSearchRepository.search(null, "SPORTS", null, null, 0, 10);
        assertThat(result).hasSize(2);
        assertThat(result).extracting(ClubDocument::getInterestCategory).containsOnly("SPORTS");
    }

    @Test
    void recommend_스포츠관심사_서울거주_doc1이_최고점수() {
        List<ClubDocument> result = clubSearchRepository.recommend(
                List.of("SPORTS"), "서울특별시", "마포구", List.of(), 0, 10);
        assertThat(result).isNotEmpty();
        assertThat(result.get(0).getName()).isEqualTo("서울 러닝크루");
    }
}
