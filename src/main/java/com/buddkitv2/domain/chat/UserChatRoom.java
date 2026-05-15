package com.buddkitv2.domain.chat;

import com.buddkitv2.domain.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "\"USER_CHAT_ROOM\"")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserChatRoom {

    @Id
    @Column(name = "user_chat_room_id")
    private String userChatRoomId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id", nullable = false)
    private ChatRoom chatRoom;

    @Enumerated(EnumType.STRING)
    private ChatRoomRole role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 메시지 읽음 위치 추적용 키
    @Column(name = "\"Key\"", nullable = false)
    private String readKey;

    public static UserChatRoom create(String id, ChatRoom chatRoom, User user,
                                       ChatRoomRole role, String readKey) {
        UserChatRoom ucr = new UserChatRoom();
        ucr.userChatRoomId = id;
        ucr.chatRoom = chatRoom;
        ucr.user = user;
        ucr.role = role;
        ucr.readKey = readKey;
        return ucr;
    }
}
