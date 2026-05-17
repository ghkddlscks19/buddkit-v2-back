package com.buddkitv2.domain.feed.repository;

import com.buddkitv2.domain.feed.entity.Feed;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FeedRepository extends JpaRepository<Feed, Long> {

    @Query("SELECT f FROM Feed f WHERE f.id = :feedId AND f.deletedAt IS NULL")
    Optional<Feed> findActiveById(@Param("feedId") Long feedId);

    @Query("SELECT f FROM Feed f WHERE f.club.id = :clubId AND f.deletedAt IS NULL ORDER BY f.id DESC")
    List<Feed> findByClubId(@Param("clubId") Long clubId, Pageable pageable);

    @Query("SELECT f FROM Feed f WHERE f.club.id = :clubId AND f.id < :lastId AND f.deletedAt IS NULL ORDER BY f.id DESC")
    List<Feed> findByClubIdAndLastId(@Param("clubId") Long clubId, @Param("lastId") Long lastId, Pageable pageable);

    @Query("SELECT f FROM Feed f WHERE f.club.id = :clubId AND f.deletedAt IS NULL ORDER BY f.likeCount DESC, f.id DESC")
    List<Feed> findByClubIdOrderByPopular(@Param("clubId") Long clubId, Pageable pageable);
}
