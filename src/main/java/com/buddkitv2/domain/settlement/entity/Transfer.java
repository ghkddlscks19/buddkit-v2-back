package com.buddkitv2.domain.settlement.entity;

import com.buddkitv2.domain.wallet.entity.WalletTransaction;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "\"TRANSFER\"")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Transfer {

    @Id
    @Column(name = "transfer_id")
    private String transferId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_settlement_id", nullable = false)
    private UserSettlement userSettlement;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_transaction_id", nullable = false)
    private WalletTransaction walletTransaction;

    public static Transfer create(String transferId, UserSettlement userSettlement,
                                   WalletTransaction walletTransaction) {
        Transfer t = new Transfer();
        t.transferId = transferId;
        t.userSettlement = userSettlement;
        t.walletTransaction = walletTransaction;
        return t;
    }
}
