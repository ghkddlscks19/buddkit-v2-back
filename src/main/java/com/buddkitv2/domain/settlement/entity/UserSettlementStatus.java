package com.buddkitv2.domain.settlement.entity;

public enum UserSettlementStatus {
    REQUESTED,            // 정산 요청
    PENDING_CONFIRMATION, // 정산 확인 대기
    COMPLETED             // 정산 완료
}
