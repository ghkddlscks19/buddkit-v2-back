package com.buddkitv2.domain.wallet.repository;

import com.buddkitv2.domain.wallet.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {
}
