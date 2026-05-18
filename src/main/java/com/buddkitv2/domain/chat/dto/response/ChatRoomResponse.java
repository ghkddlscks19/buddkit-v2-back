package com.buddkitv2.domain.chat.dto.response;

import com.buddkitv2.domain.chat.entity.ChatRoomType;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class ChatRoomResponse {
    private Long chatRoomId;
    private ChatRoomType type;
    private Long scheduleId;        // CLUB 타입이면 null
    private String lastMessage;     // 메시지 없으면 null
    private LocalDateTime lastMessageAt;
    private long unreadCount;
}
