package com.buddkitv2.domain.settlement.service;

import com.buddkitv2.domain.club.entity.UserClub;
import com.buddkitv2.domain.club.entity.UserClubRole;
import com.buddkitv2.domain.club.repository.UserClubRepository;
import com.buddkitv2.domain.schedule.entity.Schedule;
import com.buddkitv2.domain.schedule.entity.ScheduleStatus;
import com.buddkitv2.domain.schedule.entity.UserSchedule;
import com.buddkitv2.domain.schedule.entity.UserScheduleRole;
import com.buddkitv2.domain.schedule.repository.ScheduleRepository;
import com.buddkitv2.domain.schedule.repository.UserScheduleRepository;
import com.buddkitv2.domain.settlement.dto.response.SettlementStatusResponse;
import com.buddkitv2.domain.settlement.entity.Settlement;
import com.buddkitv2.domain.settlement.entity.SettlementStatus;
import com.buddkitv2.domain.settlement.entity.UserSettlement;
import com.buddkitv2.domain.settlement.entity.UserSettlementStatus;
import com.buddkitv2.domain.settlement.repository.SettlementRepository;
import com.buddkitv2.domain.settlement.repository.UserSettlementRepository;
import com.buddkitv2.domain.user.entity.User;
import com.buddkitv2.domain.wallet.entity.Wallet;
import com.buddkitv2.domain.wallet.entity.WalletTransaction;
import com.buddkitv2.domain.wallet.entity.WalletTransactionType;
import com.buddkitv2.domain.wallet.repository.WalletRepository;
import com.buddkitv2.domain.wallet.repository.WalletTransactionRepository;
import com.buddkitv2.global.exception.AlreadySettledException;
import com.buddkitv2.global.exception.InsufficientBalanceException;
import com.buddkitv2.global.exception.ScheduleAccessDeniedException;
import com.buddkitv2.global.exception.ScheduleAlreadySettlingException;
import com.buddkitv2.global.exception.ScheduleNotFoundException;
import com.buddkitv2.global.exception.SettlementNotFoundException;
import com.buddkitv2.global.exception.WalletNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SettlementService {

    private final SettlementRepository settlementRepository;
    private final UserSettlementRepository userSettlementRepository;
    private final ScheduleRepository scheduleRepository;
    private final UserScheduleRepository userScheduleRepository;
    private final UserClubRepository userClubRepository;
    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;

    // ── 헬퍼 ──────────────────────────────────────────────

    private Schedule findActiveSchedule(Long clubId, Long scheduleId) {
        Schedule schedule = scheduleRepository.findActiveById(scheduleId)
                .orElseThrow(ScheduleNotFoundException::new);
        if (!schedule.getClub().getId().equals(clubId)) throw new ScheduleNotFoundException();
        return schedule;
    }

    private UserClub requireClubMember(Long clubId, Long userId) {
        return userClubRepository.findByClub_IdAndUser_Id(clubId, userId)
                .orElseThrow(ScheduleAccessDeniedException::new);
    }

    private void requireLeader(UserClub uc) {
        if (uc.getRole() != UserClubRole.LEADER) throw new ScheduleAccessDeniedException();
    }

    private Wallet findWallet(Long userId) {
        return walletRepository.findByUserId(userId).orElseThrow(WalletNotFoundException::new);
    }

    // Settlement·Schedule 상태 자동 갱신
    public void refreshSettlementStatus(Settlement settlement) {
        long total = userSettlementRepository.countBySettlement_Id(settlement.getId());
        long completed = userSettlementRepository.countBySettlement_IdAndStatus(
                settlement.getId(), UserSettlementStatus.COMPLETED);

        if (settlement.getStatus() == SettlementStatus.REQUESTED && completed > 0) {
            settlement.changeStatus(SettlementStatus.IN_PROGRESS);
        }
        if (total > 0 && total == completed) {
            settlement.complete();
            settlement.getSchedule().changeStatus(ScheduleStatus.CLOSED);
        }
    }

    // 포인트 이체 처리 (즉시 정산·배치 공용)
    public void executeTransfer(UserSettlement userSettlement, Schedule schedule) {
        User member = userSettlement.getUser();
        Wallet memberWallet = findWallet(member.getId());

        if (memberWallet.getBalance() < schedule.getCost()) {
            throw new InsufficientBalanceException();
        }

        UserSchedule leaderSchedule = userScheduleRepository
                .findBySchedule_IdAndRole(schedule.getId(), UserScheduleRole.LEADER)
                .stream().findFirst().orElseThrow(ScheduleNotFoundException::new);
        Wallet leaderWallet = findWallet(leaderSchedule.getUser().getId());

        memberWallet.debit(schedule.getCost());
        leaderWallet.charge(schedule.getCost());
        walletTransactionRepository.save(
                WalletTransaction.create(memberWallet, leaderWallet, WalletTransactionType.TRANSFER, schedule.getCost()));

        userSettlement.complete();
    }

    // ── API ───────────────────────────────────────────────

    @Transactional
    public void requestSettlement(Long userId, Long clubId, Long scheduleId) {
        UserClub uc = requireClubMember(clubId, userId);
        requireLeader(uc);
        Schedule schedule = findActiveSchedule(clubId, scheduleId);

        if (settlementRepository.existsBySchedule_Id(scheduleId)) {
            throw new ScheduleAlreadySettlingException();
        }

        List<UserSchedule> members = userScheduleRepository
                .findBySchedule_IdAndRole(scheduleId, UserScheduleRole.MEMBER);
        long sum = schedule.getCost() * members.size();

        schedule.changeStatus(ScheduleStatus.SETTLING);

        User leaderUser = uc.getUser();
        Settlement settlement = Settlement.create(schedule, sum, leaderUser);
        settlementRepository.save(settlement);

        for (UserSchedule us : members) {
            userSettlementRepository.save(UserSettlement.create(us.getUser(), settlement));
        }
    }

    @Transactional(readOnly = true)
    public List<SettlementStatusResponse> getSettlements(Long userId, Long clubId, Long scheduleId,
                                                          Long lastId, int size) {
        requireClubMember(clubId, userId);
        Settlement settlement = settlementRepository.findBySchedule_Id(scheduleId)
                .orElseThrow(SettlementNotFoundException::new);
        List<UserSettlement> list = lastId == null
                ? userSettlementRepository.findBySettlementId(settlement.getId(), PageRequest.of(0, size))
                : userSettlementRepository.findBySettlementIdAndLastId(settlement.getId(), lastId, PageRequest.of(0, size));
        Schedule schedule = findActiveSchedule(clubId, scheduleId);
        return list.stream().map(us -> new SettlementStatusResponse(
                us.getId(), us.getUser().getId(), us.getUser().getNickname(),
                us.getStatus(), us.getCompletedTime(), schedule.getCost()
        )).toList();
    }

    @Transactional
    public void settleMyShare(Long userId, Long clubId, Long scheduleId) {
        requireClubMember(clubId, userId);
        Schedule schedule = findActiveSchedule(clubId, scheduleId);
        Settlement settlement = settlementRepository.findBySchedule_Id(scheduleId)
                .orElseThrow(SettlementNotFoundException::new);
        UserSettlement userSettlement = userSettlementRepository
                .findBySettlement_IdAndUser_Id(settlement.getId(), userId)
                .orElseThrow(SettlementNotFoundException::new);
        if (userSettlement.getStatus() != UserSettlementStatus.REQUESTED) {
            throw new AlreadySettledException();
        }
        executeTransfer(userSettlement, schedule);
        refreshSettlementStatus(settlement);
    }

    @Transactional
    public void reserveMyShare(Long userId, Long clubId, Long scheduleId) {
        requireClubMember(clubId, userId);
        Settlement settlement = settlementRepository.findBySchedule_Id(scheduleId)
                .orElseThrow(SettlementNotFoundException::new);
        UserSettlement userSettlement = userSettlementRepository
                .findBySettlement_IdAndUser_Id(settlement.getId(), userId)
                .orElseThrow(SettlementNotFoundException::new);
        if (userSettlement.getStatus() != UserSettlementStatus.REQUESTED) {
            throw new AlreadySettledException();
        }
        userSettlement.reserve();
    }

    @Transactional
    public void completeManually(Long userId, Long clubId, Long scheduleId, Long userSettlementId) {
        UserClub uc = requireClubMember(clubId, userId);
        requireLeader(uc);
        Settlement settlement = settlementRepository.findBySchedule_Id(scheduleId)
                .orElseThrow(SettlementNotFoundException::new);
        UserSettlement userSettlement = userSettlementRepository.findById(userSettlementId)
                .filter(us -> us.getSettlement().getId().equals(settlement.getId()))
                .orElseThrow(SettlementNotFoundException::new);
        if (userSettlement.getStatus() == UserSettlementStatus.COMPLETED) {
            throw new AlreadySettledException();
        }
        userSettlement.complete();
        refreshSettlementStatus(settlement);
    }
}
