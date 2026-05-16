package com.buddkitv2.domain.wallet.repository;

import com.buddkitv2.domain.wallet.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WalletRepository extends JpaRepository<Wallet, Long> {
}
