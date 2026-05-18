package com.buddkitv2.domain.chat.entity;

import com.buddkitv2.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "\"MESSAGE\"")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "message_id")
    private Long id;

    // DB 스키마에 Key NOT NULL 컬럼이 존재 — 레거시 설계의 Redis 메시지 키 필드
    @Column(name = "\"Key\"", nullable = false)
    private String key;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id", nullable = false)
    private ChatRoom chatRoom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(length = 255)
    private String text;

    private LocalDateTime sentAt;

    // DB 스키마에 deleted boolean 컬럼이 존재 — 소프트 딜리트 플래그 (deletedAt과 병행 사용)
    private Boolean deleted;

    private LocalDateTime deletedAt;

    public static Message create(ChatRoom chatRoom, User user, String text) {
        Message m = new Message();
        m.key = UUID.randomUUID().toString();
        m.chatRoom = chatRoom;
        m.user = user;
        m.text = text;
        m.sentAt = LocalDateTime.now();
        m.deleted = false;
        return m;
    }

    public void softDelete() {
        this.deleted = true;
        this.deletedAt = LocalDateTime.now();
    }
}
