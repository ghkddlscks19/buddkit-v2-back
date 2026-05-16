package com.buddkitv2.domain.chat.entity;

import com.buddkitv2.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "\"MESSAGE\"")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "message_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id", nullable = false)
    private ChatRoom chatRoom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(length = 255)
    private String text;

    private LocalDateTime sentAt;

    private LocalDateTime deletedAt;

    public static Message create(ChatRoom chatRoom, User user, String text) {
        Message m = new Message();
        m.chatRoom = chatRoom;
        m.user = user;
        m.text = text;
        m.sentAt = LocalDateTime.now();
        return m;
    }
}
