package com.buddkitv2.global.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatSubscriber implements MessageListener {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String channel = new String(message.getChannel(), StandardCharsets.UTF_8);
        String chatRoomId = channel.replace("chat:room:", "");
        try {
            JsonNode payload = objectMapper.readTree(message.getBody());
            messagingTemplate.convertAndSend("/topic/chat-rooms/" + chatRoomId, payload);
        } catch (Exception e) {
            log.warn("채팅 메시지 브로드캐스트 실패 — channel: {}", channel, e);
        }
    }
}
