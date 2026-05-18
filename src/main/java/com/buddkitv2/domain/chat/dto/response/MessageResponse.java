package com.buddkitv2.domain.chat.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class MessageResponse {
    private Long messageId;
    private Long userId;
    private String nickname;
    private String text;            // 삭제된 메시지이면 null
    private LocalDateTime sentAt;
    private boolean deleted;
}
