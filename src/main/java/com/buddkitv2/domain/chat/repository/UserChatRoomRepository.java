package com.buddkitv2.domain.chat.repository;

import com.buddkitv2.domain.chat.entity.UserChatRoom;
import com.buddkitv2.domain.chat.entity.UserChatRoomId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserChatRoomRepository extends JpaRepository<UserChatRoom, UserChatRoomId> {

    Optional<UserChatRoom> findByChatRoom_IdAndUser_Id(Long chatRoomId, Long userId);

    boolean existsByChatRoom_IdAndUser_Id(Long chatRoomId, Long userId);

    @Modifying
    @Query("DELETE FROM UserChatRoom ucr WHERE ucr.chatRoom.id = :chatRoomId AND ucr.user.id = :userId")
    void deleteByChatRoom_IdAndUser_Id(@Param("chatRoomId") Long chatRoomId, @Param("userId") Long userId);

    @Modifying
    @Query("DELETE FROM UserChatRoom ucr WHERE ucr.chatRoom.id = :chatRoomId")
    void deleteAllByChatRoomId(@Param("chatRoomId") Long chatRoomId);
}
