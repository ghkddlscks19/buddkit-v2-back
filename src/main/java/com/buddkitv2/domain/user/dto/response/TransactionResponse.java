package com.buddkitv2.domain.user.dto.response;

import com.buddkitv2.domain.wallet.entity.WalletTransactionType;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter @AllArgsConstructor
public class TransactionResponse {
    private Long id;
    private WalletTransactionType type;
    private Long balance;
    private LocalDateTime createdAt;
}
