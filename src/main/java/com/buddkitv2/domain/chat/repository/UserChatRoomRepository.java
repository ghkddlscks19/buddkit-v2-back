package com.buddkitv2.domain.chat.repository;

import com.buddkitv2.domain.chat.entity.UserChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserChatRoomRepository extends JpaRepository<UserChatRoom, String> {

    Optional<UserChatRoom> findByChatRoom_IdAndUser_Id(Long chatRoomId, Long userId);

    List<UserChatRoom> findByChatRoom_Id(Long chatRoomId);

    List<UserChatRoom> findByUser_IdAndChatRoom_IdIn(Long userId, List<Long> chatRoomIds);

    boolean existsByChatRoom_IdAndUser_Id(Long chatRoomId, Long userId);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM UserChatRoom ucr WHERE ucr.chatRoom.id = :chatRoomId AND ucr.user.id = :userId")
    void deleteByChatRoom_IdAndUser_Id(@Param("chatRoomId") Long chatRoomId, @Param("userId") Long userId);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM UserChatRoom ucr WHERE ucr.chatRoom.id = :chatRoomId")
    void deleteAllByChatRoomId(@Param("chatRoomId") Long chatRoomId);
}
