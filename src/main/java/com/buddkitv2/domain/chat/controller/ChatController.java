package com.buddkitv2.domain.chat.controller;

import com.buddkitv2.domain.chat.dto.response.ChatRoomResponse;
import com.buddkitv2.domain.chat.dto.response.MessageResponse;
import com.buddkitv2.domain.chat.service.ChatService;
import com.buddkitv2.global.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/clubs/{clubId}/chat-rooms")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @GetMapping
    public ApiResponse<List<ChatRoomResponse>> getChatRooms(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long clubId
    ) {
        return ApiResponse.ok(chatService.getChatRooms(userId, clubId));
    }

    @GetMapping("/{chatRoomId}/messages")
    public ApiResponse<List<MessageResponse>> getMessages(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long clubId,
            @PathVariable Long chatRoomId,
            @RequestParam(required = false) Long lastId,
            @RequestParam(defaultValue = "30") int size
    ) {
        return ApiResponse.ok(chatService.getMessages(userId, clubId, chatRoomId, lastId, size));
    }

    @DeleteMapping("/{chatRoomId}/messages/{messageId}")
    public ResponseEntity<Void> deleteMessage(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long clubId,
            @PathVariable Long chatRoomId,
            @PathVariable Long messageId
    ) {
        chatService.deleteMessage(userId, clubId, chatRoomId, messageId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{chatRoomId}/read")
    public ResponseEntity<Void> markAsRead(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long clubId,
            @PathVariable Long chatRoomId
    ) {
        chatService.markAsReadToLatest(userId, clubId, chatRoomId);
        return ResponseEntity.noContent().build();
    }
}
