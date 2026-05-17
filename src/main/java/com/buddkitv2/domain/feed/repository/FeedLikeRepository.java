package com.buddkitv2.domain.feed.repository;

import com.buddkitv2.domain.feed.entity.FeedLike;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FeedLikeRepository extends JpaRepository<FeedLike, Long> {

    Optional<FeedLike> findByFeed_IdAndUser_Id(Long feedId, Long userId);

    boolean existsByFeed_IdAndUser_Id(Long feedId, Long userId);
}
