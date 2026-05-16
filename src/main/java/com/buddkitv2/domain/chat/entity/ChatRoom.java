package com.buddkitv2.domain.chat.entity;

import com.buddkitv2.domain.club.entity.Club;
import com.buddkitv2.domain.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "\"CHAT_ROOM\"")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatRoom extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chat_room_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "club_id", nullable = false)
    private Club club;

    // 논리적 연결만, FK 제약조건 없음
    @Column(name = "schedule_id")
    private Long scheduleId;

    @Enumerated(EnumType.STRING)
    private ChatRoomType type;

    public static ChatRoom createClubRoom(Club club) {
        ChatRoom cr = new ChatRoom();
        cr.club = club;
        cr.type = ChatRoomType.CLUB;
        return cr;
    }

    public static ChatRoom createScheduleRoom(Club club, Long scheduleId) {
        ChatRoom cr = new ChatRoom();
        cr.club = club;
        cr.scheduleId = scheduleId;
        cr.type = ChatRoomType.SCHEDULE;
        return cr;
    }
}
