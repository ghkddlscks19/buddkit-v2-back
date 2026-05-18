package com.buddkitv2.domain.schedule.service;

import com.buddkitv2.domain.club.dto.request.ClubCreateRequest;
import com.buddkitv2.domain.club.repository.ClubRepository;
import com.buddkitv2.domain.club.repository.UserClubRepository;
import com.buddkitv2.domain.club.service.ClubService;
import com.buddkitv2.domain.common.Address;
import com.buddkitv2.domain.common.AddressRepository;
import com.buddkitv2.domain.schedule.dto.request.ScheduleCreateRequest;
import com.buddkitv2.domain.schedule.dto.request.ScheduleUpdateRequest;
import com.buddkitv2.domain.schedule.dto.response.ScheduleMemberResponse;
import com.buddkitv2.domain.schedule.dto.response.ScheduleResponse;
import com.buddkitv2.domain.schedule.entity.ScheduleStatus;
import com.buddkitv2.domain.schedule.entity.UserScheduleRole;
import com.buddkitv2.domain.schedule.repository.ScheduleRepository;
import com.buddkitv2.domain.schedule.repository.UserScheduleRepository;
import com.buddkitv2.domain.user.entity.Gender;
import com.buddkitv2.domain.user.entity.Interest;
import com.buddkitv2.domain.user.entity.InterestCategory;
import com.buddkitv2.domain.user.entity.User;
import com.buddkitv2.domain.user.repository.InterestRepository;
import com.buddkitv2.domain.user.repository.UserRepository;
import com.buddkitv2.global.config.S3Service;
import com.buddkitv2.global.config.TossPaymentClient;
import com.buddkitv2.global.exception.AlreadyJoinedScheduleException;
import com.buddkitv2.global.exception.ScheduleAccessDeniedException;
import com.buddkitv2.global.exception.ScheduleFullException;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class ScheduleServiceTest {

    @Autowired ScheduleService scheduleService;
    @Autowired ClubService clubService;
    @Autowired UserRepository userRepository;
    @Autowired AddressRepository addressRepository;
    @Autowired InterestRepository interestRepository;
    @Autowired ClubRepository clubRepository;
    @Autowired UserClubRepository userClubRepository;
    @Autowired ScheduleRepository scheduleRepository;
    @Autowired UserScheduleRepository userScheduleRepository;
    @Autowired EntityManager entityManager;

    @MockitoBean TossPaymentClient tossPaymentClient;
    @MockitoBean S3Service s3Service;
    @MockitoBean private com.buddkitv2.domain.chat.service.ChatService chatService;

    private User leader;
    private User member;
    private User outsider;
    private Long clubId;

    @BeforeEach
    void setUp() {
        Address address = addressRepository.save(Address.of("서울특별시", "테스트구", 99000));
        interestRepository.save(Interest.of(InterestCategory.CULTURE, "문화"));

        leader = userRepository.save(User.register(20001L, "모임장", LocalDate.of(1990, 1, 1), Gender.MALE, address, null));
        member = userRepository.save(User.register(20002L, "멤버", LocalDate.of(1991, 2, 2), Gender.FEMALE, address, null));
        outsider = userRepository.save(User.register(20003L, "외부인", LocalDate.of(1992, 3, 3), Gender.MALE, address, null));

        ClubCreateRequest clubReq = new ClubCreateRequest();
        clubReq.setName("테스트모임");
        clubReq.setUserLimit(10);
        clubReq.setDescription("설명");
        clubReq.setClubImage(null);
        clubReq.setCity("서울특별시");
        clubReq.setDistrict("테스트구");
        clubReq.setInterestCategory(InterestCategory.CULTURE);
        clubId = clubService.createClub(leader.getId(), clubReq).getClubId();
        clubService.joinClub(member.getId(), clubId);
    }

    private ScheduleCreateRequest createReq(int limit) {
        ScheduleCreateRequest req = new ScheduleCreateRequest();
        req.setName("정모");
        req.setScheduleTime(LocalDateTime.now().plusDays(7));
        req.setLocation("강남역");
        req.setCost(5000L);
        req.setLimit(limit);
        return req;
    }

    @Test
    void 모임장은_스케줄을_생성할_수_있다() {
        ScheduleResponse res = scheduleService.createSchedule(leader.getId(), clubId, createReq(10));

        assertThat(res.getName()).isEqualTo("정모");
        assertThat(res.getStatus()).isEqualTo(ScheduleStatus.RECRUITING);
        assertThat(res.getParticipantCount()).isEqualTo(1);
        assertThat(res.isJoined()).isTrue();
        assertThat(userScheduleRepository.findBySchedule_IdAndUser_Id(res.getScheduleId(), leader.getId()))
                .isPresent()
                .get()
                .satisfies(us -> assertThat(us.getRole()).isEqualTo(UserScheduleRole.LEADER));
    }

    @Test
    void 모임장이_아닌_멤버가_스케줄을_생성하면_예외를_던진다() {
        assertThatThrownBy(() -> scheduleService.createSchedule(member.getId(), clubId, createReq(10)))
                .isInstanceOf(ScheduleAccessDeniedException.class);
    }

    @Test
    void 모임원이_아닌_사람이_스케줄을_생성하면_예외를_던진다() {
        assertThatThrownBy(() -> scheduleService.createSchedule(outsider.getId(), clubId, createReq(10)))
                .isInstanceOf(ScheduleAccessDeniedException.class);
    }

    @Test
    void 모임장은_스케줄을_수정할_수_있다() {
        ScheduleResponse created = scheduleService.createSchedule(leader.getId(), clubId, createReq(10));
        ScheduleUpdateRequest upd = new ScheduleUpdateRequest();
        upd.setName("수정된 정모");
        upd.setScheduleTime(LocalDateTime.now().plusDays(14));
        upd.setLocation("홍대");
        upd.setCost(10000L);
        upd.setLimit(5);

        ScheduleResponse updated = scheduleService.updateSchedule(leader.getId(), clubId, created.getScheduleId(), upd);

        assertThat(updated.getName()).isEqualTo("수정된 정모");
        assertThat(updated.getCost()).isEqualTo(10000L);
    }

    @Test
    void 모임장은_스케줄을_삭제할_수_있다() {
        ScheduleResponse created = scheduleService.createSchedule(leader.getId(), clubId, createReq(10));
        Long scheduleId = created.getScheduleId();

        scheduleService.deleteSchedule(leader.getId(), clubId, scheduleId);

        assertThat(scheduleRepository.findActiveById(scheduleId)).isEmpty();
    }

    @Test
    void 멤버는_RECRUITING_상태_스케줄에_참여할_수_있다() {
        ScheduleResponse created = scheduleService.createSchedule(leader.getId(), clubId, createReq(10));
        Long scheduleId = created.getScheduleId();

        scheduleService.joinSchedule(member.getId(), clubId, scheduleId);

        assertThat(userScheduleRepository.existsBySchedule_IdAndUser_Id(scheduleId, member.getId())).isTrue();
    }

    @Test
    void 이미_참여한_스케줄에_재참여하면_예외를_던진다() {
        ScheduleResponse created = scheduleService.createSchedule(leader.getId(), clubId, createReq(10));
        Long scheduleId = created.getScheduleId();
        scheduleService.joinSchedule(member.getId(), clubId, scheduleId);

        assertThatThrownBy(() -> scheduleService.joinSchedule(member.getId(), clubId, scheduleId))
                .isInstanceOf(AlreadyJoinedScheduleException.class);
    }

    @Test
    void 정원이_가득_찬_스케줄에_참여하면_예외를_던진다() {
        ScheduleResponse created = scheduleService.createSchedule(leader.getId(), clubId, createReq(1));
        Long scheduleId = created.getScheduleId();

        assertThatThrownBy(() -> scheduleService.joinSchedule(member.getId(), clubId, scheduleId))
                .isInstanceOf(ScheduleFullException.class);
    }

    @Test
    void 멤버는_스케줄_참여를_취소할_수_있다() {
        ScheduleResponse created = scheduleService.createSchedule(leader.getId(), clubId, createReq(10));
        Long scheduleId = created.getScheduleId();
        scheduleService.joinSchedule(member.getId(), clubId, scheduleId);

        scheduleService.leaveSchedule(member.getId(), clubId, scheduleId);

        assertThat(userScheduleRepository.existsBySchedule_IdAndUser_Id(scheduleId, member.getId())).isFalse();
    }

    @Test
    void 모임장은_스케줄에서_탈퇴할_수_없다() {
        ScheduleResponse created = scheduleService.createSchedule(leader.getId(), clubId, createReq(10));

        assertThatThrownBy(() -> scheduleService.leaveSchedule(leader.getId(), clubId, created.getScheduleId()))
                .isInstanceOf(ScheduleAccessDeniedException.class);
    }

    @Test
    void 스케줄_목록을_cursor_기반으로_조회한다() {
        scheduleService.createSchedule(leader.getId(), clubId, createReq(10));
        scheduleService.createSchedule(leader.getId(), clubId, createReq(10));

        List<ScheduleResponse> page1 = scheduleService.getSchedules(leader.getId(), clubId, null, 1);
        assertThat(page1).hasSize(1);

        List<ScheduleResponse> page2 = scheduleService.getSchedules(leader.getId(), clubId, page1.get(0).getScheduleId(), 10);
        assertThat(page2).hasSize(1);
    }

    @Test
    void 모임원이_아닌_사람이_스케줄_목록을_조회하면_예외를_던진다() {
        assertThatThrownBy(() -> scheduleService.getSchedules(outsider.getId(), clubId, null, 10))
                .isInstanceOf(ScheduleAccessDeniedException.class);
    }

    @Test
    void 참여자_목록을_cursor_기반으로_조회한다() {
        ScheduleResponse created = scheduleService.createSchedule(leader.getId(), clubId, createReq(10));
        Long scheduleId = created.getScheduleId();
        scheduleService.joinSchedule(member.getId(), clubId, scheduleId);

        List<ScheduleMemberResponse> members = scheduleService.getScheduleMembers(leader.getId(), clubId, scheduleId, null, 10);

        assertThat(members).hasSize(2);
        assertThat(members.stream().map(ScheduleMemberResponse::getRole))
                .containsExactlyInAnyOrder(UserScheduleRole.LEADER, UserScheduleRole.MEMBER);
    }
}
