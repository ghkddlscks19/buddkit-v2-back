package com.buddkitv2.domain.settlement.repository;

import com.buddkitv2.domain.settlement.entity.Settlement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SettlementRepository extends JpaRepository<Settlement, Long> {

    Optional<Settlement> findBySchedule_Id(Long scheduleId);

    boolean existsBySchedule_Id(Long scheduleId);
}
