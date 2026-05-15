package com.buddkitv2.domain.wallet;

import com.buddkitv2.domain.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "\"WALLET_TRANSACTION\"")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WalletTransaction extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "wallet_transaction_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = false)
    private Wallet wallet;

    // 송금 대상 지갑
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_wallet_id", nullable = false)
    private Wallet targetWallet;

    @Enumerated(EnumType.STRING)
    private WalletTransactionType type;

    private Integer balance;

    public static WalletTransaction create(Wallet wallet, Wallet targetWallet,
                                            WalletTransactionType type, Integer balance) {
        WalletTransaction wt = new WalletTransaction();
        wt.wallet = wallet;
        wt.targetWallet = targetWallet;
        wt.type = type;
        wt.balance = balance;
        return wt;
    }
}
