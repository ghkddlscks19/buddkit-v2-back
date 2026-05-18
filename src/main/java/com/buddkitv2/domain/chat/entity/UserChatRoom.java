package com.buddkitv2.domain.chat.entity;

import com.buddkitv2.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "\"USER_CHAT_ROOM\"")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserChatRoom {

    @Id
    @Column(name = "user_chat_room_id")
    private String id;

    // DB 스키마에 Key NOT NULL 컬럼이 존재 — 레거시 설계의 Redis 키 필드
    @Column(name = "\"Key\"", nullable = false)
    private String key;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id", nullable = false)
    private ChatRoom chatRoom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    private ChatRoomRole role;

    private Long lastReadMessageId;

    public static UserChatRoom create(ChatRoom chatRoom, User user, ChatRoomRole role) {
        UserChatRoom ucr = new UserChatRoom();
        ucr.id = UUID.randomUUID().toString();
        ucr.key = chatRoom.getId() + ":" + user.getId();
        ucr.chatRoom = chatRoom;
        ucr.user = user;
        ucr.role = role;
        return ucr;
    }

    public void updateLastRead(Long lastReadMessageId) {
        this.lastReadMessageId = lastReadMessageId;
    }
}
