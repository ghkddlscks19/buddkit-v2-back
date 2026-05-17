package com.buddkitv2.domain.settlement.service;

import com.buddkitv2.domain.club.dto.request.ClubCreateRequest;
import com.buddkitv2.domain.club.service.ClubService;
import com.buddkitv2.domain.common.Address;
import com.buddkitv2.domain.common.AddressRepository;
import com.buddkitv2.domain.schedule.dto.request.ScheduleCreateRequest;
import com.buddkitv2.domain.schedule.entity.ScheduleStatus;
import com.buddkitv2.domain.schedule.repository.ScheduleRepository;
import com.buddkitv2.domain.schedule.service.ScheduleService;
import com.buddkitv2.domain.settlement.dto.response.SettlementStatusResponse;
import com.buddkitv2.domain.settlement.entity.Settlement;
import com.buddkitv2.domain.settlement.entity.SettlementStatus;
import com.buddkitv2.domain.settlement.entity.UserSettlementStatus;
import com.buddkitv2.domain.settlement.repository.SettlementRepository;
import com.buddkitv2.domain.settlement.repository.UserSettlementRepository;
import com.buddkitv2.domain.user.entity.Gender;
import com.buddkitv2.domain.user.entity.Interest;
import com.buddkitv2.domain.user.entity.InterestCategory;
import com.buddkitv2.domain.user.entity.User;
import com.buddkitv2.domain.user.repository.InterestRepository;
import com.buddkitv2.domain.user.repository.UserRepository;
import com.buddkitv2.domain.wallet.entity.Wallet;
import com.buddkitv2.domain.wallet.repository.WalletRepository;
import com.buddkitv2.global.config.S3Service;
import com.buddkitv2.global.config.TossPaymentClient;
import com.buddkitv2.global.exception.AlreadySettledException;
import com.buddkitv2.global.exception.InsufficientBalanceException;
import com.buddkitv2.global.exception.ScheduleAlreadySettlingException;
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
class SettlementServiceTest {

    @Autowired SettlementService settlementService;
    @Autowired ScheduleService scheduleService;
    @Autowired ClubService clubService;
    @Autowired UserRepository userRepository;
    @Autowired AddressRepository addressRepository;
    @Autowired InterestRepository interestRepository;
    @Autowired WalletRepository walletRepository;
    @Autowired SettlementRepository settlementRepository;
    @Autowired UserSettlementRepository userSettlementRepository;
    @Autowired ScheduleRepository scheduleRepository;

    @MockitoBean TossPaymentClient tossPaymentClient;
    @MockitoBean S3Service s3Service;

    private User leader;
    private User member1;
    private User member2;
    private Long clubId;
    private Long scheduleId;
    private static final Long COST = 5000L;

    @BeforeEach
    void setUp() {
        Address address = addressRepository.save(Address.of("서울특별시", "테스트구", 99000));
        interestRepository.save(Interest.of(InterestCategory.CULTURE, "문화"));

        leader  = userRepository.save(User.register(30001L, "모임장", LocalDate.of(1990, 1, 1), Gender.MALE, address, null));
        member1 = userRepository.save(User.register(30002L, "멤버1", LocalDate.of(1991, 2, 2), Gender.FEMALE, address, null));
        member2 = userRepository.save(User.register(30003L, "멤버2", LocalDate.of(1992, 3, 3), Gender.MALE, address, null));

        walletRepository.save(Wallet.create(leader));
        walletRepository.save(Wallet.createWithBonus(member1, 20000L));
        walletRepository.save(Wallet.createWithBonus(member2, 0L));

        ClubCreateRequest clubReq = new ClubCreateRequest();
        clubReq.setName("정산테스트모임");
        clubReq.setUserLimit(10);
        clubReq.setDescription("설명");
        clubReq.setClubImage(null);
        clubReq.setCity("서울특별시");
        clubReq.setDistrict("테스트구");
        clubReq.setInterestCategory(InterestCategory.CULTURE);
        clubId = clubService.createClub(leader.getId(), clubReq).getClubId();
        clubService.joinClub(member1.getId(), clubId);
        clubService.joinClub(member2.getId(), clubId);

        ScheduleCreateRequest schedReq = new ScheduleCreateRequest();
        schedReq.setName("정모");
        schedReq.setScheduleTime(LocalDateTime.now().plusDays(7));
        schedReq.setLocation("강남");
        schedReq.setCost(COST);
        schedReq.setLimit(10);
        scheduleId = scheduleService.createSchedule(leader.getId(), clubId, schedReq).getScheduleId();
        scheduleService.joinSchedule(member1.getId(), clubId, scheduleId);
        scheduleService.joinSchedule(member2.getId(), clubId, scheduleId);
    }

    @Test
    void 모임장이_정산을_요청하면_Settlement와_UserSettlement가_생성된다() {
        settlementService.requestSettlement(leader.getId(), clubId, scheduleId);

        Settlement settlement = settlementRepository.findBySchedule_Id(scheduleId).orElseThrow();
        assertThat(settlement.getSum()).isEqualTo(COST * 2); // member1 + member2
        assertThat(userSettlementRepository.countBySettlement_Id(settlement.getId())).isEqualTo(2);
        assertThat(scheduleRepository.findActiveById(scheduleId).orElseThrow().getStatus())
                .isEqualTo(ScheduleStatus.SETTLING);
    }

    @Test
    void 이미_정산이_요청된_스케줄에_재요청하면_예외를_던진다() {
        settlementService.requestSettlement(leader.getId(), clubId, scheduleId);

        assertThatThrownBy(() -> settlementService.requestSettlement(leader.getId(), clubId, scheduleId))
                .isInstanceOf(ScheduleAlreadySettlingException.class);
    }

    @Test
    void 멤버가_즉시_정산하면_포인트가_이체되고_COMPLETED가_된다() {
        settlementService.requestSettlement(leader.getId(), clubId, scheduleId);

        settlementService.settleMyShare(member1.getId(), clubId, scheduleId);

        Wallet memberWallet = walletRepository.findByUserId(member1.getId()).orElseThrow();
        Wallet leaderWallet = walletRepository.findByUserId(leader.getId()).orElseThrow();
        assertThat(memberWallet.getBalance()).isEqualTo(20000L - COST);
        assertThat(leaderWallet.getBalance()).isEqualTo(COST);

        Settlement settlement = settlementRepository.findBySchedule_Id(scheduleId).orElseThrow();
        long completed = userSettlementRepository.countBySettlement_IdAndStatus(
                settlement.getId(), UserSettlementStatus.COMPLETED);
        assertThat(completed).isEqualTo(1);
    }

    @Test
    void 잔액_부족_멤버가_즉시_정산하면_예외를_던진다() {
        settlementService.requestSettlement(leader.getId(), clubId, scheduleId);

        assertThatThrownBy(() -> settlementService.settleMyShare(member2.getId(), clubId, scheduleId))
                .isInstanceOf(InsufficientBalanceException.class);
    }

    @Test
    void 전원_정산_완료_시_Settlement와_Schedule이_CLOSED된다() {
        settlementService.requestSettlement(leader.getId(), clubId, scheduleId);
        settlementService.settleMyShare(member1.getId(), clubId, scheduleId);

        // member2는 모임장이 수동 처리
        Settlement settlement = settlementRepository.findBySchedule_Id(scheduleId).orElseThrow();
        Long us2Id = userSettlementRepository
                .findBySettlement_IdAndUser_Id(settlement.getId(), member2.getId())
                .orElseThrow().getId();
        settlementService.completeManually(leader.getId(), clubId, scheduleId, us2Id);

        assertThat(settlementRepository.findBySchedule_Id(scheduleId).orElseThrow().getStatus())
                .isEqualTo(SettlementStatus.COMPLETED);
        assertThat(scheduleRepository.findActiveById(scheduleId).orElseThrow().getStatus())
                .isEqualTo(ScheduleStatus.CLOSED);
    }

    @Test
    void 멤버가_예약_정산하면_PENDING_CONFIRMATION이_된다() {
        settlementService.requestSettlement(leader.getId(), clubId, scheduleId);

        settlementService.reserveMyShare(member1.getId(), clubId, scheduleId);

        Settlement settlement = settlementRepository.findBySchedule_Id(scheduleId).orElseThrow();
        assertThat(userSettlementRepository
                .findBySettlement_IdAndUser_Id(settlement.getId(), member1.getId())
                .orElseThrow().getStatus())
                .isEqualTo(UserSettlementStatus.PENDING_CONFIRMATION);
    }

    @Test
    void 이미_정산된_항목을_재정산하면_예외를_던진다() {
        settlementService.requestSettlement(leader.getId(), clubId, scheduleId);
        settlementService.settleMyShare(member1.getId(), clubId, scheduleId);

        assertThatThrownBy(() -> settlementService.settleMyShare(member1.getId(), clubId, scheduleId))
                .isInstanceOf(AlreadySettledException.class);
    }

    @Test
    void 정산_현황_목록을_조회할_수_있다() {
        settlementService.requestSettlement(leader.getId(), clubId, scheduleId);

        List<SettlementStatusResponse> list = settlementService.getSettlements(
                leader.getId(), clubId, scheduleId, null, 10);

        assertThat(list).hasSize(2);
        assertThat(list.get(0).getAmount()).isEqualTo(COST);
        assertThat(list.stream().map(SettlementStatusResponse::getStatus))
                .containsOnly(UserSettlementStatus.REQUESTED);
    }
}
