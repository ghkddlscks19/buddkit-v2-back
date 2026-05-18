package com.buddkitv2.domain.notification.repository;

import com.buddkitv2.domain.notification.entity.Notification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @Query("SELECT n FROM Notification n JOIN FETCH n.notificationType WHERE n.user.id = :userId ORDER BY n.id DESC")
    List<Notification> findByUserId(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT n FROM Notification n JOIN FETCH n.notificationType WHERE n.user.id = :userId AND n.id < :lastId ORDER BY n.id DESC")
    List<Notification> findByUserIdAndLastId(@Param("userId") Long userId, @Param("lastId") Long lastId, Pageable pageable);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.user.id = :userId AND n.isRead = false")
    void markAllAsRead(@Param("userId") Long userId);
}
