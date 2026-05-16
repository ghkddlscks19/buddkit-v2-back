package com.buddkitv2.domain.wallet.entity;

public enum PaymentStatus {
    READY,            // 결제 준비
    IN_PROGRESS,      // 결제 진행 중
    DONE,             // 결제 완료
    CANCELED,         // 취소
    PARTIAL_CANCELED, // 부분 취소
    ABORTED,          // 결제 승인 실패
    EXPIRED           // 결제 만료
}
