package com.buddkitv2.domain.user.repository;

import com.buddkitv2.domain.user.entity.UserInterest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserInterestRepository extends JpaRepository<UserInterest, Long> {
}
