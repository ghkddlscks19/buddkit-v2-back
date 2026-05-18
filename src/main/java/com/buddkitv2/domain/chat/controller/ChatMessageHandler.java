package com.buddkitv2.domain.chat.controller;

import com.buddkitv2.domain.chat.dto.request.ReadRequest;
import com.buddkitv2.domain.chat.dto.request.SendMessageRequest;
import com.buddkitv2.domain.chat.dto.response.MessageResponse;
import com.buddkitv2.domain.chat.service.ChatService;
import com.buddkitv2.global.exception.ChatAccessDeniedException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Controller
@RequiredArgsConstructor
public class ChatMessageHandler {

    private final ChatService chatService;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @MessageMapping("/chat-rooms/{chatRoomId}/messages")
    public void sendMessage(
            @DestinationVariable Long chatRoomId,
            @Payload SendMessageRequest request,
            SimpMessageHeaderAccessor headerAccessor) throws JacksonException {

        Long userId = (Long) headerAccessor.getSessionAttributes().get("userId");
        if (userId == null) throw new ChatAccessDeniedException();
        MessageResponse response = chatService.sendMessage(userId, chatRoomId, request.getText());
        String json = objectMapper.writeValueAsString(response);
        redisTemplate.convertAndSend("chat:room:" + chatRoomId, json);
    }

    @MessageMapping("/chat-rooms/{chatRoomId}/read")
    public void markAsRead(
            @DestinationVariable Long chatRoomId,
            @Payload ReadRequest request,
            SimpMessageHeaderAccessor headerAccessor) {

        Long userId = (Long) headerAccessor.getSessionAttributes().get("userId");
        if (userId == null) throw new ChatAccessDeniedException();
        chatService.markAsRead(userId, chatRoomId, request.getLastReadMessageId());
    }
}
