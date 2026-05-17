package com.buddkitv2.domain.schedule.repository;

import com.buddkitv2.domain.schedule.entity.Schedule;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {

    @Query("SELECT s FROM Schedule s WHERE s.id = :id AND s.deletedAt IS NULL")
    Optional<Schedule> findActiveById(@Param("id") Long id);

    @Query("SELECT s FROM Schedule s WHERE s.club.id = :clubId AND s.deletedAt IS NULL ORDER BY s.id DESC")
    List<Schedule> findByClubId(@Param("clubId") Long clubId, Pageable pageable);

    @Query("SELECT s FROM Schedule s WHERE s.club.id = :clubId AND s.id < :lastId AND s.deletedAt IS NULL ORDER BY s.id DESC")
    List<Schedule> findByClubIdAndLastId(@Param("clubId") Long clubId, @Param("lastId") Long lastId, Pageable pageable);
}
