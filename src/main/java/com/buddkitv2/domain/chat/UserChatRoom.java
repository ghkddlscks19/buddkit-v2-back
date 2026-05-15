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

    @EmbeddedId
    private UserChatRoomId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("chatRoomId")
    @JoinColumn(name = "chat_room_id", nullable = false)
    private ChatRoom chatRoom;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    private ChatRoomRole role;

    private Long lastReadMessageId;

    public static UserChatRoom create(ChatRoom chatRoom, User user, ChatRoomRole role) {
        UserChatRoom ucr = new UserChatRoom();
        ucr.id = new UserChatRoomId(chatRoom.getId(), user.getId());
        ucr.chatRoom = chatRoom;
        ucr.user = user;
        ucr.role = role;
        return ucr;
    }
}
