package com.buddkitv2.domain.chat.repository;

import com.buddkitv2.domain.chat.entity.ChatRoom;
import com.buddkitv2.domain.chat.entity.ChatRoomType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    Optional<ChatRoom> findByClub_IdAndType(Long clubId, ChatRoomType type);

    @Query("SELECT cr FROM ChatRoom cr WHERE cr.scheduleId = :scheduleId")
    Optional<ChatRoom> findByScheduleId(@Param("scheduleId") Long scheduleId);

    @Query("SELECT cr FROM ChatRoom cr WHERE cr.club.id = :clubId ORDER BY cr.type ASC")
    List<ChatRoom> findByClub_Id(@Param("clubId") Long clubId);
}
