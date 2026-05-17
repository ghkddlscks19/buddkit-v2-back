package com.buddkitv2.domain.user.dto.response;

import com.buddkitv2.domain.settlement.entity.UserSettlementStatus;
import com.buddkitv2.domain.settlement.entity.UserSettlementType;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter @AllArgsConstructor
public class SettlementHistoryResponse {
    private Long id;
    private UserSettlementStatus status;
    private UserSettlementType type;
    private Long amount;
    private LocalDateTime completedTime;
    private LocalDateTime createdAt;
}
