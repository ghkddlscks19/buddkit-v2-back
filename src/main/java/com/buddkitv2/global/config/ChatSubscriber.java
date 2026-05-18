package com.buddkitv2.global.config;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;

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
            // 역직렬화 실패 시 브로드캐스트 생략
        }
    }
}
