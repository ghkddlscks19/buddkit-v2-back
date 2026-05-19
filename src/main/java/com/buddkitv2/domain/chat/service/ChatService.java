package com.buddkitv2.domain.chat.service;

import com.buddkitv2.domain.chat.dto.response.ChatRoomResponse;
import com.buddkitv2.domain.chat.dto.response.MessageResponse;
import com.buddkitv2.domain.chat.entity.ChatRoom;
import com.buddkitv2.domain.chat.entity.ChatRoomRole;
import com.buddkitv2.domain.chat.entity.ChatRoomType;
import com.buddkitv2.domain.chat.entity.Message;
import com.buddkitv2.domain.chat.entity.UserChatRoom;
import com.buddkitv2.domain.chat.repository.ChatRoomRepository;
import com.buddkitv2.domain.chat.repository.MessageRepository;
import com.buddkitv2.domain.chat.repository.UserChatRoomRepository;
import com.buddkitv2.domain.club.entity.Club;
import com.buddkitv2.domain.user.entity.User;
import com.buddkitv2.domain.notification.dto.event.NotificationEventPayload;
import com.buddkitv2.domain.notification.entity.NotificationTypeEnum;
import com.buddkitv2.global.exception.ChatAccessDeniedException;
import com.buddkitv2.global.exception.ChatRoomNotFoundException;
import com.buddkitv2.global.exception.MessageAccessDeniedException;
import com.buddkitv2.global.exception.MessageNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final MessageRepository messageRepository;
    private final UserChatRoomRepository userChatRoomRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    // ── 자동 생성/삭제 ──────────────────────────────────────────

    @Transactional
    public void createChatRoomForClub(Club club, User user) {
        ChatRoom chatRoom = ChatRoom.createClubRoom(club);
        chatRoomRepository.save(chatRoom);
        userChatRoomRepository.save(UserChatRoom.create(chatRoom, user, ChatRoomRole.LEADER));
    }

    @Transactional
    public void createChatRoomForSchedule(Club club, Long scheduleId, User user) {
        ChatRoom chatRoom = ChatRoom.createScheduleRoom(club, scheduleId);
        chatRoomRepository.save(chatRoom);
        userChatRoomRepository.save(UserChatRoom.create(chatRoom, user, ChatRoomRole.LEADER));
    }

    @Transactional
    public void addClubMember(Long clubId, User user) {
        ChatRoom chatRoom = chatRoomRepository.findByClub_IdAndType(clubId, ChatRoomType.CLUB)
                .orElseThrow(ChatRoomNotFoundException::new);
        if (!userChatRoomRepository.existsByChatRoom_IdAndUser_Id(chatRoom.getId(), user.getId())) {
            userChatRoomRepository.save(UserChatRoom.create(chatRoom, user, ChatRoomRole.MEMBER));
        }
    }

    @Transactional
    public void addScheduleMember(Long scheduleId, User user) {
        // 스케줄 채팅방이 없는 경우(동시성 등) 무시
        chatRoomRepository.findByScheduleId(scheduleId).ifPresent(chatRoom -> {
            if (!userChatRoomRepository.existsByChatRoom_IdAndUser_Id(chatRoom.getId(), user.getId())) {
                userChatRoomRepository.save(UserChatRoom.create(chatRoom, user, ChatRoomRole.MEMBER));
            }
        });
    }

    @Transactional
    public void removeClubMember(Long clubId, Long userId) {
        chatRoomRepository.findByClub_IdAndType(clubId, ChatRoomType.CLUB).ifPresent(chatRoom ->
                userChatRoomRepository.deleteByChatRoom_IdAndUser_Id(chatRoom.getId(), userId));
    }

    @Transactional
    public void removeScheduleMember(Long scheduleId, Long userId) {
        chatRoomRepository.findByScheduleId(scheduleId).ifPresent(chatRoom ->
                userChatRoomRepository.deleteByChatRoom_IdAndUser_Id(chatRoom.getId(), userId));
    }

    @Transactional
    public void deleteChatRoomsByScheduleId(Long scheduleId) {
        chatRoomRepository.findByScheduleId(scheduleId).ifPresent(chatRoom -> {
            messageRepository.deleteAllByChatRoomId(chatRoom.getId());
            userChatRoomRepository.deleteAllByChatRoomId(chatRoom.getId());
            chatRoomRepository.deleteById(chatRoom.getId());
        });
    }

    // ── REST API ───────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ChatRoomResponse> getChatRooms(Long userId, Long clubId) {
        List<ChatRoom> chatRooms = chatRoomRepository.findByClub_Id(clubId);
        if (chatRooms.isEmpty()) throw new ChatRoomNotFoundException();

        List<Long> chatRoomIds = chatRooms.stream().map(ChatRoom::getId).toList();
        List<UserChatRoom> ucrs = userChatRoomRepository.findByUser_IdAndChatRoom_IdIn(userId, chatRoomIds);
        if (ucrs.isEmpty()) throw new ChatAccessDeniedException();

        Map<Long, UserChatRoom> ucrMap = ucrs.stream()
                .collect(Collectors.toMap(ucr -> ucr.getChatRoom().getId(), ucr -> ucr));

        return chatRooms.stream()
                .filter(cr -> ucrMap.containsKey(cr.getId()))
                .map(cr -> toChatRoomResponse(cr, ucrMap.get(cr.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MessageResponse> getMessages(Long userId, Long clubId, Long chatRoomId, Long lastId, int size) {
        requireChatRoomAccess(userId, clubId, chatRoomId);
        PageRequest pageable = PageRequest.of(0, size);
        List<Message> messages = lastId == null
                ? messageRepository.findByChatRoomId(chatRoomId, pageable)
                : messageRepository.findByChatRoomIdAndLastId(chatRoomId, lastId, pageable);
        return messages.stream().map(this::toMessageResponse).toList();
    }

    @Transactional
    public void deleteMessage(Long userId, Long clubId, Long chatRoomId, Long messageId) {
        requireChatRoomAccess(userId, clubId, chatRoomId);
        Message message = messageRepository.findById(messageId)
                .orElseThrow(MessageNotFoundException::new);
        if (!message.getUser().getId().equals(userId)) throw new MessageAccessDeniedException();
        message.softDelete();
    }

    @Transactional
    public void markAsReadToLatest(Long userId, Long clubId, Long chatRoomId) {
        requireChatRoomAccess(userId, clubId, chatRoomId);
        UserChatRoom ucr = userChatRoomRepository.findByChatRoom_IdAndUser_Id(chatRoomId, userId)
                .orElseThrow(ChatAccessDeniedException::new);
        Long latestId = messageRepository.findMaxIdByChatRoomId(chatRoomId).orElse(0L);
        ucr.updateLastRead(latestId);
    }

    // ── STOMP 핸들러에서 호출 ────────────────────────────────────

    @Transactional
    public MessageResponse sendMessage(Long userId, Long chatRoomId, String text) {
        UserChatRoom ucr = userChatRoomRepository.findByChatRoom_IdAndUser_Id(chatRoomId, userId)
                .orElseThrow(ChatAccessDeniedException::new);
        ChatRoom chatRoom = ucr.getChatRoom();
        User user = ucr.getUser();
        Message message = Message.create(chatRoom, user, text);
        messageRepository.save(message);
        String chatContent = user.getNickname() + ": " + text;
        userChatRoomRepository.findByChatRoom_Id(chatRoomId).stream()
                .filter(m -> !m.getUser().getId().equals(userId))
                .forEach(m -> emitNotification(NotificationTypeEnum.CHAT, m.getUser().getId(), chatContent));
        return toMessageResponse(message);
    }

    @Transactional
    public void markAsRead(Long userId, Long chatRoomId, Long lastReadMessageId) {
        UserChatRoom ucr = userChatRoomRepository.findByChatRoom_IdAndUser_Id(chatRoomId, userId)
                .orElseThrow(ChatAccessDeniedException::new);
        ucr.updateLastRead(lastReadMessageId);
    }

    // ── 헬퍼 ────────────────────────────────────────────────────

    private void requireChatRoomAccess(Long userId, Long clubId, Long chatRoomId) {
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(ChatRoomNotFoundException::new);
        if (!chatRoom.getClub().getId().equals(clubId)) throw new ChatRoomNotFoundException();
        if (!userChatRoomRepository.existsByChatRoom_IdAndUser_Id(chatRoomId, userId)) {
            throw new ChatAccessDeniedException();
        }
    }

    private ChatRoomResponse toChatRoomResponse(ChatRoom chatRoom, UserChatRoom ucr) {
        List<Message> lastMsgs = messageRepository.findByChatRoomId(chatRoom.getId(), PageRequest.of(0, 1));
        Message lastMsg = lastMsgs.isEmpty() ? null : lastMsgs.get(0);

        Long lastRead = ucr.getLastReadMessageId();
        long unreadCount = messageRepository.countUnread(chatRoom.getId(), lastRead != null ? lastRead : 0L);

        return new ChatRoomResponse(
                chatRoom.getId(),
                chatRoom.getType(),
                chatRoom.getScheduleId(),
                lastMsg != null ? lastMsg.getText() : null,
                lastMsg != null ? lastMsg.getSentAt() : null,
                unreadCount
        );
    }

    private MessageResponse toMessageResponse(Message message) {
        boolean deleted = message.getDeletedAt() != null;
        return new MessageResponse(
                message.getId(),
                message.getUser().getId(),
                message.getUser().getNickname(),
                deleted ? null : message.getText(),
                message.getSentAt(),
                deleted
        );
    }

    private void emitNotification(NotificationTypeEnum type, Long targetUserId, String content) {
        try {
            kafkaTemplate.send("notification-events", String.valueOf(targetUserId),
                    objectMapper.writeValueAsString(new NotificationEventPayload(type, targetUserId, content)));
        } catch (Exception e) {
            // 알림 emit 실패는 채팅 동작에 영향 없음
        }
    }
}
