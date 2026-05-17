package com.buddkitv2.domain.settlement.service;

import com.buddkitv2.domain.schedule.entity.Schedule;
import com.buddkitv2.domain.settlement.entity.Settlement;
import com.buddkitv2.domain.settlement.entity.UserSettlement;
import com.buddkitv2.domain.settlement.entity.UserSettlementStatus;
import com.buddkitv2.domain.settlement.repository.UserSettlementRepository;
import com.buddkitv2.global.exception.InsufficientBalanceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SettlementBatchService {

    private final UserSettlementRepository userSettlementRepository;
    private final SettlementService settlementService;

    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void processReservedSettlements() {
        List<UserSettlement> reserved = userSettlementRepository
                .findByStatus(UserSettlementStatus.PENDING_CONFIRMATION);

        for (UserSettlement us : reserved) {
            Settlement settlement = us.getSettlement();
            Schedule schedule = settlement.getSchedule();
            try {
                settlementService.executeTransfer(us, schedule);
                settlementService.refreshSettlementStatus(settlement);
                log.info("배치 정산 완료: userSettlementId={}", us.getId());
            } catch (InsufficientBalanceException e) {
                us.rollback();
                log.warn("배치 정산 잔액 부족 롤백: userSettlementId={}, userId={}", us.getId(), us.getUser().getId());
            }
        }
    }
}
