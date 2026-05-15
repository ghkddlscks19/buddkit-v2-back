package com.buddkitv2.domain.chat;

import com.buddkitv2.domain.user.User;
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

    private Boolean deleted;

    // 메시지 순서/저장 키
    @Column(name = "\"Key\"", nullable = false)
    private String messageKey;

    public static Message create(ChatRoom chatRoom, User user, String text, String messageKey) {
        Message m = new Message();
        m.chatRoom = chatRoom;
        m.user = user;
        m.text = text;
        m.sentAt = LocalDateTime.now();
        m.deleted = false;
        m.messageKey = messageKey;
        return m;
    }
}
