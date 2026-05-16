package com.buddkitv2.domain.club.repository;

import com.buddkitv2.domain.club.entity.ClubLike;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ClubLikeRepository extends JpaRepository<ClubLike, Long> {

    @Query("SELECT cl FROM ClubLike cl JOIN FETCH cl.club c JOIN FETCH c.address JOIN FETCH c.interest WHERE cl.user.id = :userId ORDER BY cl.id DESC")
    List<ClubLike> findByUserIdWithClub(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT cl FROM ClubLike cl JOIN FETCH cl.club c JOIN FETCH c.address JOIN FETCH c.interest WHERE cl.user.id = :userId AND cl.id < :lastId ORDER BY cl.id DESC")
    List<ClubLike> findByUserIdAndLastIdWithClub(@Param("userId") Long userId, @Param("lastId") Long lastId, Pageable pageable);
}
