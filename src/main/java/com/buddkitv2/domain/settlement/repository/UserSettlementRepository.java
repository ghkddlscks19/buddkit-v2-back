package com.buddkitv2.domain.settlement.repository;

import com.buddkitv2.domain.settlement.entity.UserSettlement;
import com.buddkitv2.domain.settlement.entity.UserSettlementStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserSettlementRepository extends JpaRepository<UserSettlement, Long> {

    @Query("SELECT us FROM UserSettlement us JOIN FETCH us.settlement WHERE us.user.id = :userId ORDER BY us.id DESC")
    List<UserSettlement> findByUserIdWithSettlement(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT us FROM UserSettlement us JOIN FETCH us.settlement WHERE us.user.id = :userId AND us.id < :lastId ORDER BY us.id DESC")
    List<UserSettlement> findByUserIdAndLastIdWithSettlement(@Param("userId") Long userId, @Param("lastId") Long lastId, Pageable pageable);

    Optional<UserSettlement> findBySettlement_IdAndUser_Id(Long settlementId, Long userId);

    long countBySettlement_Id(Long settlementId);

    long countBySettlement_IdAndStatus(Long settlementId, UserSettlementStatus status);

    @Query("SELECT us FROM UserSettlement us JOIN FETCH us.user WHERE us.settlement.id = :settlementId ORDER BY us.id ASC")
    List<UserSettlement> findBySettlementId(@Param("settlementId") Long settlementId, Pageable pageable);

    @Query("SELECT us FROM UserSettlement us JOIN FETCH us.user WHERE us.settlement.id = :settlementId AND us.id > :lastId ORDER BY us.id ASC")
    List<UserSettlement> findBySettlementIdAndLastId(@Param("settlementId") Long settlementId, @Param("lastId") Long lastId, Pageable pageable);

    List<UserSettlement> findByStatus(UserSettlementStatus status);
}
