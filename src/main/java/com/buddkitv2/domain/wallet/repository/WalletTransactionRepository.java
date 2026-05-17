package com.buddkitv2.domain.wallet.repository;

import com.buddkitv2.domain.wallet.entity.WalletTransaction;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {

    List<WalletTransaction> findByWallet_IdOrderByIdDesc(Long walletId, Pageable pageable);

    List<WalletTransaction> findByWallet_IdAndIdLessThanOrderByIdDesc(Long walletId, Long lastId, Pageable pageable);
}
