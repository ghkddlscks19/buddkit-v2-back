package com.buddkitv2.domain.feed.repository;

import com.buddkitv2.domain.feed.entity.FeedLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface FeedLikeRepository extends JpaRepository<FeedLike, Long> {

    Optional<FeedLike> findByFeed_IdAndUser_Id(Long feedId, Long userId);

    boolean existsByFeed_IdAndUser_Id(Long feedId, Long userId);

    @Query("SELECT fl.feed.id FROM FeedLike fl WHERE fl.user.id = :userId AND fl.feed.id IN :feedIds")
    Set<Long> findLikedFeedIds(@Param("userId") Long userId, @Param("feedIds") List<Long> feedIds);
}
