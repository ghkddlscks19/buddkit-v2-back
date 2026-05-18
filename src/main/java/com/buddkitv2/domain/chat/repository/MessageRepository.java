package com.buddkitv2.domain.chat.repository;

import com.buddkitv2.domain.chat.entity.Message;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MessageRepository extends JpaRepository<Message, Long> {

    @Query("SELECT m FROM Message m WHERE m.chatRoom.id = :chatRoomId AND m.deletedAt IS NULL ORDER BY m.id DESC")
    List<Message> findByChatRoomId(@Param("chatRoomId") Long chatRoomId, Pageable pageable);

    @Query("SELECT m FROM Message m WHERE m.chatRoom.id = :chatRoomId AND m.id < :lastId AND m.deletedAt IS NULL ORDER BY m.id DESC")
    List<Message> findByChatRoomIdAndLastId(@Param("chatRoomId") Long chatRoomId, @Param("lastId") Long lastId, Pageable pageable);

    @Query("SELECT COUNT(m) FROM Message m WHERE m.chatRoom.id = :chatRoomId AND m.id > :lastReadMessageId AND m.deletedAt IS NULL")
    long countUnread(@Param("chatRoomId") Long chatRoomId, @Param("lastReadMessageId") Long lastReadMessageId);

    @Query("SELECT MAX(m.id) FROM Message m WHERE m.chatRoom.id = :chatRoomId AND m.deletedAt IS NULL")
    Optional<Long> findMaxIdByChatRoomId(@Param("chatRoomId") Long chatRoomId);

    @Modifying
    @Query("DELETE FROM Message m WHERE m.chatRoom.id = :chatRoomId")
    void deleteAllByChatRoomId(@Param("chatRoomId") Long chatRoomId);
}
