package com.buddkitv2.domain.chat;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Embeddable
@NoArgsConstructor
@EqualsAndHashCode
public class UserChatRoomId implements Serializable {

    @Column(name = "chat_room_id")
    private Long chatRoomId;

    @Column(name = "user_id")
    private Long userId;

    public UserChatRoomId(Long chatRoomId, Long userId) {
        this.chatRoomId = chatRoomId;
        this.userId = userId;
    }
}
