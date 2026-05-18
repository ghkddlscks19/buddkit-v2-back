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

    List<UserClub> findByClub_Id(Long clubId);

    Optional<UserClub> findByClub_IdAndUser_Id(Long clubId, Long userId);

    boolean existsByClub_IdAndUser_Id(Long clubId, Long userId);

    @Query("SELECT uc.club.id FROM UserClub uc WHERE uc.user.id = :userId")
    List<Long> findClubIdsByUserId(@Param("userId") Long userId);

    @Query("SELECT uc2.club as club, COUNT(DISTINCT shared.user) as overlap " +
           "FROM UserClub uc1 " +
           "JOIN UserClub shared ON uc1.club.id = shared.club.id " +
           "JOIN UserClub uc2 ON shared.user.id = uc2.user.id " +
           "WHERE uc1.user.id = :userId " +
           "  AND shared.user.id != :userId " +
           "  AND uc2.club.id NOT IN :myClubIds " +
           "  AND uc2.club.deletedAt IS NULL " +
           "GROUP BY uc2.club " +
           "ORDER BY COUNT(DISTINCT shared.user) DESC")
    List<ClubOverlapProjection> findCoMemberClubs(
            @Param("userId") Long userId,
            @Param("myClubIds") List<Long> myClubIds,
            Pageable pageable);
}
