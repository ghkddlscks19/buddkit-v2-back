package com.buddkitv2.domain.user.repository;

import com.buddkitv2.domain.user.entity.Interest;
import com.buddkitv2.domain.user.entity.InterestCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InterestRepository extends JpaRepository<Interest, Long> {
    List<Interest> findByCategoryIn(List<InterestCategory> categories);
    Optional<Interest> findByCategory(InterestCategory category);
}
