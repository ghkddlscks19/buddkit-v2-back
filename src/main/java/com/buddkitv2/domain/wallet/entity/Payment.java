package com.buddkitv2.domain.wallet.entity;

import com.buddkitv2.domain.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "\"Payment\"")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment extends BaseEntity {

    @Id
    @Column(name = "payment_id")
    private UUID paymentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_transaction_id", nullable = false)
    private WalletTransaction walletTransaction;

    @Column(unique = true)
    private String tossPaymentKey;

    private String tossOrderId;

    private String method;

    private Long totalAmount;

    private LocalDateTime approvedAt;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    public static Payment create(WalletTransaction walletTransaction,
                                  String tossPaymentKey, String tossOrderId,
                                  String method, Long totalAmount, LocalDateTime approvedAt) {
        Payment p = new Payment();
        p.paymentId = UUID.randomUUID();
        p.walletTransaction = walletTransaction;
        p.tossPaymentKey = tossPaymentKey;
        p.tossOrderId = tossOrderId;
        p.method = method;
        p.totalAmount = totalAmount;
        p.approvedAt = approvedAt;
        p.status = PaymentStatus.DONE;
        return p;
    }
}
