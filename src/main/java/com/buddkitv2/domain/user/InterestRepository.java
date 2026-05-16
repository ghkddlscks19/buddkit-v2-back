package com.buddkitv2.domain.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InterestRepository extends JpaRepository<Interest, Long> {
    List<Interest> findByCategoryIn(List<InterestCategory> categories);
}
