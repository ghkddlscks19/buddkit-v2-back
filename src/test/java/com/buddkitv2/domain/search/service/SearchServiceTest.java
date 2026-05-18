package com.buddkitv2.domain.search.service;

import com.buddkitv2.domain.club.entity.Club;
import com.buddkitv2.domain.club.entity.UserClub;
import com.buddkitv2.domain.club.entity.UserClubRole;
import com.buddkitv2.domain.club.repository.ClubRepository;
import com.buddkitv2.domain.club.repository.UserClubRepository;
import com.buddkitv2.domain.common.Address;
import com.buddkitv2.domain.common.AddressRepository;
import com.buddkitv2.domain.search.dto.response.ClubSearchResponse;
import com.buddkitv2.domain.search.repository.ClubSearchRepository;
import com.buddkitv2.domain.user.entity.Gender;
import com.buddkitv2.domain.user.entity.Interest;
import com.buddkitv2.domain.user.entity.InterestCategory;
import com.buddkitv2.domain.user.entity.User;
import com.buddkitv2.domain.user.repository.InterestRepository;
import com.buddkitv2.domain.user.repository.UserRepository;
import com.buddkitv2.global.config.S3Service;
import com.buddkitv2.global.config.TossPaymentClient;
import com.buddkitv2.global.exception.InvalidSearchConditionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class SearchServiceTest {

    @Autowired SearchService searchService;
    @Autowired UserRepository userRepository;
    @Autowired AddressRepository addressRepository;
    @Autowired InterestRepository interestRepository;
    @Autowired ClubRepository clubRepository;
    @Autowired UserClubRepository userClubRepository;

    @MockitoBean KafkaTemplate<String, String> kafkaTemplate;
    @MockitoBean ClubSearchRepository clubSearchRepository;
    @MockitoBean TossPaymentClient tossPaymentClient;
    @MockitoBean S3Service s3Service;

    private User userA;
    private Address address;
    private Interest interest;

    @BeforeEach
    void setUp() {
        address = addressRepository.save(Address.of("서울특별시", "마포구", 11440));
        interest = interestRepository.save(Interest.of(InterestCategory.SPORTS, "운동/스포츠"));
        userA = userRepository.save(
                User.register(20001L, "유저A", LocalDate.of(1990, 1, 1), Gender.MALE, address, null));
    }

    @Test
    void coMemberRecommend_내모임없으면_빈목록반환() {
        List<ClubSearchResponse> result = searchService.coMemberRecommend(userA.getId(), 0, 20);
        assertThat(result).isEmpty();
    }

    @Test
    void coMemberRecommend_겹치는멤버수많은_모임이_상위노출() {
        // given
        User userB = userRepository.save(
                User.register(20002L, "유저B", LocalDate.of(1991, 2, 2), Gender.FEMALE, address, null));
        User userC = userRepository.save(
                User.register(20003L, "유저C", LocalDate.of(1992, 3, 3), Gender.MALE, address, null));

        Club club1 = clubRepository.save(Club.create("모임1", 30, "설명1", null, address, interest));
        Club club2 = clubRepository.save(Club.create("모임2", 30, "설명2", null, address, interest));
        Club club3 = clubRepository.save(Club.create("모임3", 30, "설명3", null, address, interest));

        // userA → club1, club2
        userClubRepository.save(UserClub.create(club1, userA, UserClubRole.LEADER));
        userClubRepository.save(UserClub.create(club2, userA, UserClubRole.LEADER));

        // userB → club1, club3
        userClubRepository.save(UserClub.create(club1, userB, UserClubRole.MEMBER));
        userClubRepository.save(UserClub.create(club3, userB, UserClubRole.LEADER));

        // userC → club2, club3
        userClubRepository.save(UserClub.create(club2, userC, UserClubRole.MEMBER));
        userClubRepository.save(UserClub.create(club3, userC, UserClubRole.MEMBER));

        // when: club3은 userA의 공통 멤버(userB, userC) 2명 → 1위
        List<ClubSearchResponse> result = searchService.coMemberRecommend(userA.getId(), 0, 20);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("모임3");
    }

    @Test
    void searchClubs_district단독사용_예외발생() {
        assertThatThrownBy(() -> searchService.searchClubs(null, null, null, "마포구", 0, 20))
                .isInstanceOf(InvalidSearchConditionException.class);
    }
}
