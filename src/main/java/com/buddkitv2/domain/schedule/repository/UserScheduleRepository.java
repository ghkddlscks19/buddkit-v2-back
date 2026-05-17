package com.buddkitv2.domain.schedule.repository;

import com.buddkitv2.domain.schedule.entity.UserSchedule;
import com.buddkitv2.domain.schedule.entity.UserScheduleRole;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserScheduleRepository extends JpaRepository<UserSchedule, Long> {

    Optional<UserSchedule> findBySchedule_IdAndUser_Id(Long scheduleId, Long userId);

    boolean existsBySchedule_IdAndUser_Id(Long scheduleId, Long userId);

    long countBySchedule_Id(Long scheduleId);

    List<UserSchedule> findBySchedule_IdAndRole(Long scheduleId, UserScheduleRole role);

    @Query("SELECT us FROM UserSchedule us JOIN FETCH us.user WHERE us.schedule.id = :scheduleId ORDER BY us.id ASC")
    List<UserSchedule> findByScheduleId(@Param("scheduleId") Long scheduleId, Pageable pageable);

    @Query("SELECT us FROM UserSchedule us JOIN FETCH us.user WHERE us.schedule.id = :scheduleId AND us.id > :lastId ORDER BY us.id ASC")
    List<UserSchedule> findByScheduleIdAndLastId(@Param("scheduleId") Long scheduleId, @Param("lastId") Long lastId, Pageable pageable);
}
