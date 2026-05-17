package com.buddkitv2.domain.club.service;

import com.buddkitv2.domain.club.dto.request.ClubCreateRequest;
import com.buddkitv2.domain.club.dto.request.ClubUpdateRequest;
import com.buddkitv2.domain.club.dto.response.ClubDetailResponse;
import com.buddkitv2.domain.club.entity.Club;
import com.buddkitv2.domain.club.entity.UserClubRole;
import com.buddkitv2.domain.club.repository.ClubRepository;
import com.buddkitv2.domain.club.repository.UserClubRepository;
import com.buddkitv2.domain.club.repository.ClubLikeRepository;
import com.buddkitv2.domain.common.Address;
import com.buddkitv2.domain.common.AddressRepository;
import com.buddkitv2.domain.user.entity.Gender;
import com.buddkitv2.domain.user.entity.Interest;
import com.buddkitv2.domain.user.entity.InterestCategory;
import com.buddkitv2.domain.user.entity.User;
import com.buddkitv2.domain.user.repository.InterestRepository;
import com.buddkitv2.domain.user.repository.UserRepository;
import com.buddkitv2.global.config.S3Service;
import com.buddkitv2.global.config.TossPaymentClient;
import com.buddkitv2.global.exception.AlreadyJoinedClubException;
import com.buddkitv2.global.exception.ClubAccessDeniedException;
import com.buddkitv2.global.exception.ClubFullException;
import com.buddkitv2.global.exception.ClubLeaderCannotLeaveException;
import com.buddkitv2.global.exception.ClubNotFoundException;
import com.buddkitv2.global.exception.NotJoinedClubException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class ClubServiceTest {

    @Autowired ClubService clubService;
    @Autowired UserRepository userRepository;
    @Autowired AddressRepository addressRepository;
    @Autowired InterestRepository interestRepository;
    @Autowired ClubRepository clubRepository;
    @Autowired UserClubRepository userClubRepository;
    @Autowired ClubLikeRepository clubLikeRepository;

    @MockitoBean TossPaymentClient tossPaymentClient;
    @MockitoBean S3Service s3Service;

    private User leader;
    private User other;
    private Address address;
    private Interest interest;

    @BeforeEach
    void setUp() {
        address = addressRepository.save(Address.of("서울특별시", "테스트구", 99000));
        interest = interestRepository.save(Interest.of(InterestCategory.CULTURE, "문화"));
        leader = User.register(10001L, "모임장", LocalDate.of(1990, 1, 1), Gender.MALE, address, null);
        other = User.register(10002L, "다른유저", LocalDate.of(1992, 3, 3), Gender.FEMALE, address, null);
        userRepository.save(leader);
        userRepository.save(other);
    }

    private ClubCreateRequest createRequest() {
        ClubCreateRequest req = new ClubCreateRequest();
        req.setName("테스트 모임");
        req.setUserLimit(10);
        req.setDescription("테스트 설명");
        req.setClubImage("https://s3.example.com/club.jpg");
        req.setCity("서울특별시");
        req.setDistrict("테스트구");
        req.setInterestCategory(InterestCategory.CULTURE);
        return req;
    }

    @Test
    void 모임_생성_시_Club과_UserClub_LEADER가_생성된다() {
        ClubDetailResponse response = clubService.createClub(leader.getId(), createRequest());

        assertThat(response.getName()).isEqualTo("테스트 모임");
        assertThat(response.getMemberCount()).isEqualTo(1);
        assertThat(response.isMember()).isTrue();
        assertThat(response.getMyRole()).isEqualTo("LEADER");
        assertThat(response.isLiked()).isFalse();
        assertThat(userClubRepository.findByClub_IdAndUser_Id(response.getClubId(), leader.getId()))
                .isPresent()
                .get()
                .satisfies(uc -> assertThat(uc.getRole()).isEqualTo(UserClubRole.LEADER));
    }

    @Test
    void 모임장은_모임을_수정할_수_있다() {
        ClubDetailResponse created = clubService.createClub(leader.getId(), createRequest());

        ClubUpdateRequest updateReq = new ClubUpdateRequest();
        updateReq.setName("수정된 모임명");
        updateReq.setUserLimit(5);
        updateReq.setDescription("수정된 설명");
        updateReq.setClubImage(null);
        updateReq.setCity("서울특별시");
        updateReq.setDistrict("테스트구");
        updateReq.setInterestCategory(InterestCategory.CULTURE);

        ClubDetailResponse updated = clubService.updateClub(leader.getId(), created.getClubId(), updateReq);

        assertThat(updated.getName()).isEqualTo("수정된 모임명");
        assertThat(updated.getUserLimit()).isEqualTo(5);
    }

    @Test
    void 모임장이_아닌_사용자가_수정하면_예외를_던진다() {
        ClubDetailResponse created = clubService.createClub(leader.getId(), createRequest());

        ClubUpdateRequest updateReq = new ClubUpdateRequest();
        updateReq.setName("수정");
        updateReq.setUserLimit(5);
        updateReq.setDescription("설명");
        updateReq.setClubImage(null);
        updateReq.setCity("서울특별시");
        updateReq.setDistrict("테스트구");
        updateReq.setInterestCategory(InterestCategory.CULTURE);

        assertThatThrownBy(() -> clubService.updateClub(other.getId(), created.getClubId(), updateReq))
                .isInstanceOf(ClubAccessDeniedException.class);
    }

    @Test
    void 모임_상세_조회_시_isLiked와_isMember가_정확히_반환된다() {
        ClubDetailResponse created = clubService.createClub(leader.getId(), createRequest());
        Long clubId = created.getClubId();

        ClubDetailResponse fromLeader = clubService.getClub(leader.getId(), clubId);
        assertThat(fromLeader.isMember()).isTrue();
        assertThat(fromLeader.getMyRole()).isEqualTo("LEADER");
        assertThat(fromLeader.isLiked()).isFalse();

        ClubDetailResponse fromOther = clubService.getClub(other.getId(), clubId);
        assertThat(fromOther.isMember()).isFalse();
        assertThat(fromOther.getMyRole()).isNull();
    }

    @Test
    void 존재하지_않는_모임_조회_시_예외를_던진다() {
        assertThatThrownBy(() -> clubService.getClub(leader.getId(), 99999L))
                .isInstanceOf(ClubNotFoundException.class);
    }

    @Test
    void 모임_가입_시_memberCount가_증가하고_MEMBER로_등록된다() {
        ClubDetailResponse created = clubService.createClub(leader.getId(), createRequest());
        Long clubId = created.getClubId();

        clubService.joinClub(other.getId(), clubId);

        Club club = clubRepository.findById(clubId).orElseThrow();
        assertThat(club.getMemberCount()).isEqualTo(2);
        assertThat(userClubRepository.findByClub_IdAndUser_Id(clubId, other.getId()))
                .isPresent()
                .get()
                .satisfies(uc -> assertThat(uc.getRole()).isEqualTo(UserClubRole.MEMBER));
    }

    @Test
    void 이미_가입한_모임에_재가입_시_예외를_던진다() {
        ClubDetailResponse created = clubService.createClub(leader.getId(), createRequest());
        Long clubId = created.getClubId();

        assertThatThrownBy(() -> clubService.joinClub(leader.getId(), clubId))
                .isInstanceOf(AlreadyJoinedClubException.class);
    }

    @Test
    void 정원이_가득_찬_모임_가입_시_예외를_던진다() {
        ClubCreateRequest req = createRequest();
        req.setUserLimit(1);
        ClubDetailResponse created = clubService.createClub(leader.getId(), req);
        Long clubId = created.getClubId();

        assertThatThrownBy(() -> clubService.joinClub(other.getId(), clubId))
                .isInstanceOf(ClubFullException.class);
    }

    @Test
    void 모임_탈퇴_시_memberCount가_감소하고_UserClub이_삭제된다() {
        ClubDetailResponse created = clubService.createClub(leader.getId(), createRequest());
        Long clubId = created.getClubId();
        clubService.joinClub(other.getId(), clubId);

        clubService.leaveClub(other.getId(), clubId);

        Club club = clubRepository.findById(clubId).orElseThrow();
        assertThat(club.getMemberCount()).isEqualTo(1);
        assertThat(userClubRepository.existsByClub_IdAndUser_Id(clubId, other.getId())).isFalse();
    }

    @Test
    void 모임장은_탈퇴할_수_없다() {
        ClubDetailResponse created = clubService.createClub(leader.getId(), createRequest());
        Long clubId = created.getClubId();

        assertThatThrownBy(() -> clubService.leaveClub(leader.getId(), clubId))
                .isInstanceOf(ClubLeaderCannotLeaveException.class);
    }

    @Test
    void 가입하지_않은_모임_탈퇴_시_예외를_던진다() {
        ClubDetailResponse created = clubService.createClub(leader.getId(), createRequest());
        Long clubId = created.getClubId();

        assertThatThrownBy(() -> clubService.leaveClub(other.getId(), clubId))
                .isInstanceOf(NotJoinedClubException.class);
    }
}
