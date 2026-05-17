package com.buddkitv2.domain.settlement.dto.response;

import com.buddkitv2.domain.settlement.entity.UserSettlementStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter @AllArgsConstructor
public class SettlementStatusResponse {
    private Long userSettlementId;
    private Long userId;
    private String nickname;
    private UserSettlementStatus status;
    private LocalDateTime completedTime;
    private Long amount;
}
