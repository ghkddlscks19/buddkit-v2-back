package com.buddkitv2.domain.club.repository;

import com.buddkitv2.domain.club.entity.UserClub;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserClubRepository extends JpaRepository<UserClub, Long> {

    @Query("SELECT uc FROM UserClub uc JOIN FETCH uc.club c JOIN FETCH c.address JOIN FETCH c.interest WHERE uc.user.id = :userId ORDER BY uc.id DESC")
    List<UserClub> findByUserIdWithClub(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT uc FROM UserClub uc JOIN FETCH uc.club c JOIN FETCH c.address JOIN FETCH c.interest WHERE uc.user.id = :userId AND uc.id < :lastId ORDER BY uc.id DESC")
    List<UserClub> findByUserIdAndLastIdWithClub(@Param("userId") Long userId, @Param("lastId") Long lastId, Pageable pageable);

    Optional<UserClub> findByClub_IdAndUser_Id(Long clubId, Long userId);

    boolean existsByClub_IdAndUser_Id(Long clubId, Long userId);
}
