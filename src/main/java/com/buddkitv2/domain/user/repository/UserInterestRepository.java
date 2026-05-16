package com.buddkitv2.domain.user.repository;

import com.buddkitv2.domain.user.entity.UserInterest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserInterestRepository extends JpaRepository<UserInterest, Long> {

    @Query("SELECT ui FROM UserInterest ui JOIN FETCH ui.interest WHERE ui.user.id = :userId")
    List<UserInterest> findByUserIdWithInterest(@Param("userId") Long userId);
}
