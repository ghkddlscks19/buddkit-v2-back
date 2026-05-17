package com.buddkitv2.domain.settlement.repository;

import com.buddkitv2.domain.settlement.entity.UserSettlement;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserSettlementRepository extends JpaRepository<UserSettlement, Long> {

    @Query("SELECT us FROM UserSettlement us JOIN FETCH us.settlement WHERE us.user.id = :userId ORDER BY us.id DESC")
    List<UserSettlement> findByUserIdWithSettlement(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT us FROM UserSettlement us JOIN FETCH us.settlement WHERE us.user.id = :userId AND us.id < :lastId ORDER BY us.id DESC")
    List<UserSettlement> findByUserIdAndLastIdWithSettlement(@Param("userId") Long userId, @Param("lastId") Long lastId, Pageable pageable);
}
