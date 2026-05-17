package com.buddkitv2.domain.feed.repository;

import com.buddkitv2.domain.feed.entity.FeedComment;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FeedCommentRepository extends JpaRepository<FeedComment, Long> {

    @Query("SELECT c FROM FeedComment c WHERE c.id = :id AND c.deletedAt IS NULL")
    Optional<FeedComment> findActiveById(@Param("id") Long id);

    @Query("SELECT c FROM FeedComment c WHERE c.feed.id = :feedId AND c.deletedAt IS NULL ORDER BY c.id ASC")
    List<FeedComment> findByFeedId(@Param("feedId") Long feedId, Pageable pageable);

    @Query("SELECT c FROM FeedComment c WHERE c.feed.id = :feedId AND c.id > :lastId AND c.deletedAt IS NULL ORDER BY c.id ASC")
    List<FeedComment> findByFeedIdAndLastId(@Param("feedId") Long feedId, @Param("lastId") Long lastId, Pageable pageable);
}
