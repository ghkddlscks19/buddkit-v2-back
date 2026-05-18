package com.buddkitv2.domain.chat.service;

import com.buddkitv2.domain.chat.dto.response.ChatRoomResponse;
import com.buddkitv2.domain.chat.dto.response.MessageResponse;
import com.buddkitv2.domain.chat.entity.ChatRoomType;
import com.buddkitv2.domain.chat.entity.Message;
import com.buddkitv2.domain.chat.repository.ChatRoomRepository;
import com.buddkitv2.domain.chat.repository.MessageRepository;
import com.buddkitv2.domain.chat.repository.UserChatRoomRepository;
import com.buddkitv2.domain.club.dto.request.ClubCreateRequest;
import com.buddkitv2.domain.club.entity.Club;
import com.buddkitv2.domain.club.repository.ClubRepository;
import com.buddkitv2.domain.club.service.ClubService;
import com.buddkitv2.domain.common.Address;
import com.buddkitv2.domain.common.AddressRepository;
import com.buddkitv2.domain.user.entity.Gender;
import com.buddkitv2.domain.user.entity.Interest;
import com.buddkitv2.domain.user.entity.InterestCategory;
import com.buddkitv2.domain.user.entity.User;
import com.buddkitv2.domain.user.repository.InterestRepository;
import com.buddkitv2.domain.user.repository.UserRepository;
import com.buddkitv2.global.config.S3Service;
import com.buddkitv2.global.config.TossPaymentClient;
import com.buddkitv2.global.exception.ChatAccessDeniedException;
import com.buddkitv2.global.exception.MessageAccessDeniedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class ChatServiceTest {

    @Autowired ChatService chatService;
    @Autowired ClubService clubService;
    @Autowired UserRepository userRepository;
    @Autowired AddressRepository addressRepository;
    @Autowired InterestRepository interestRepository;
    @Autowired ClubRepository clubRepository;
    @Autowired ChatRoomRepository chatRoomRepository;
    @Autowired MessageRepository messageRepository;
    @Autowired UserChatRoomRepository userChatRoomRepository;

    @MockitoBean TossPaymentClient tossPaymentClient;
    @MockitoBean S3Service s3Service;

    private User leader;
    private User member;
    private User outsider;
    private Long clubId;
    private Club club;

    @BeforeEach
    void setUp() {
        Address address = addressRepository.save(Address.of("서울특별시", "채팅테스트구", 99010));
        interestRepository.save(Interest.of(InterestCategory.CULTURE, "문화"));

        leader = userRepository.save(User.register(40001L, "모임장", LocalDate.of(1990, 1, 1), Gender.MALE, address, null));
        member = userRepository.save(User.register(40002L, "멤버", LocalDate.of(1991, 2, 2), Gender.FEMALE, address, null));
        outsider = userRepository.save(User.register(40003L, "외부인", LocalDate.of(1992, 3, 3), Gender.MALE, address, null));

        ClubCreateRequest req = new ClubCreateRequest();
        req.setName("채팅테스트모임");
        req.setUserLimit(10);
        req.setDescription("설명");
        req.setClubImage(null);
        req.setCity("서울특별시");
        req.setDistrict("채팅테스트구");
        req.setInterestCategory(InterestCategory.CULTURE);

        clubService.createClub(leader.getId(), req);
        club = clubRepository.findAll().stream()
                .filter(c -> c.getName().equals("채팅테스트모임"))
                .findFirst().orElseThrow();
        clubId = club.getId();

        clubService.joinClub(member.getId(), clubId);
    }

    @Test
    void getChatRooms_모임장은_CLUB_채팅방을_조회할_수_있다() {
        List<ChatRoomResponse> rooms = chatService.getChatRooms(leader.getId(), clubId);
        assertThat(rooms).hasSize(1);
        assertThat(rooms.get(0).getType()).isEqualTo(ChatRoomType.CLUB);
    }

    @Test
    void getChatRooms_비멤버는_채팅방을_조회할_수_없다() {
        assertThatThrownBy(() -> chatService.getChatRooms(outsider.getId(), clubId))
                .isInstanceOf(ChatAccessDeniedException.class);
    }

    @Test
    void sendMessage_멤버는_메시지를_전송할_수_있다() {
        Long chatRoomId = chatRoomRepository.findByClub_IdAndType(clubId, ChatRoomType.CLUB)
                .orElseThrow().getId();

        MessageResponse response = chatService.sendMessage(leader.getId(), chatRoomId, "안녕하세요");

        assertThat(response.getText()).isEqualTo("안녕하세요");
        assertThat(response.getUserId()).isEqualTo(leader.getId());
        assertThat(response.isDeleted()).isFalse();
    }

    @Test
    void sendMessage_비멤버는_메시지를_전송할_수_없다() {
        Long chatRoomId = chatRoomRepository.findByClub_IdAndType(clubId, ChatRoomType.CLUB)
                .orElseThrow().getId();

        assertThatThrownBy(() -> chatService.sendMessage(outsider.getId(), chatRoomId, "침입"))
                .isInstanceOf(ChatAccessDeniedException.class);
    }

    @Test
    void getMessages_메시지_목록을_최신순으로_조회한다() {
        Long chatRoomId = chatRoomRepository.findByClub_IdAndType(clubId, ChatRoomType.CLUB)
                .orElseThrow().getId();
        chatService.sendMessage(leader.getId(), chatRoomId, "첫 번째");
        chatService.sendMessage(leader.getId(), chatRoomId, "두 번째");

        List<MessageResponse> messages = chatService.getMessages(leader.getId(), clubId, chatRoomId, null, 10);

        assertThat(messages).hasSize(2);
        assertThat(messages.get(0).getText()).isEqualTo("두 번째");
    }

    @Test
    void getMessages_cursor_기반_다음_페이지를_조회한다() {
        Long chatRoomId = chatRoomRepository.findByClub_IdAndType(clubId, ChatRoomType.CLUB)
                .orElseThrow().getId();
        chatService.sendMessage(leader.getId(), chatRoomId, "첫 번째");
        MessageResponse second = chatService.sendMessage(leader.getId(), chatRoomId, "두 번째");

        List<MessageResponse> messages = chatService.getMessages(leader.getId(), clubId, chatRoomId, second.getMessageId(), 10);

        assertThat(messages).hasSize(1);
        assertThat(messages.get(0).getText()).isEqualTo("첫 번째");
    }

    @Test
    void deleteMessage_본인_메시지를_삭제할_수_있다() {
        Long chatRoomId = chatRoomRepository.findByClub_IdAndType(clubId, ChatRoomType.CLUB)
                .orElseThrow().getId();
        MessageResponse sent = chatService.sendMessage(leader.getId(), chatRoomId, "삭제 대상");

        chatService.deleteMessage(leader.getId(), clubId, chatRoomId, sent.getMessageId());

        // 소프트 딜리트 확인: deletedAt이 설정됨
        Message deletedMsg = messageRepository.findById(sent.getMessageId()).orElseThrow();
        assertThat(deletedMsg.getDeletedAt()).isNotNull();

        // getMessages는 deletedAt IS NULL 조건으로 조회하므로 삭제된 메시지는 반환되지 않음
        List<MessageResponse> messages = chatService.getMessages(leader.getId(), clubId, chatRoomId, null, 10);
        assertThat(messages).isEmpty();
    }

    @Test
    void deleteMessage_타인_메시지는_삭제할_수_없다() {
        Long chatRoomId = chatRoomRepository.findByClub_IdAndType(clubId, ChatRoomType.CLUB)
                .orElseThrow().getId();
        MessageResponse sent = chatService.sendMessage(leader.getId(), chatRoomId, "남의 메시지");

        assertThatThrownBy(() -> chatService.deleteMessage(member.getId(), clubId, chatRoomId, sent.getMessageId()))
                .isInstanceOf(MessageAccessDeniedException.class);
    }

    @Test
    void markAsReadToLatest_입장_시_최신_메시지까지_읽음_처리된다() {
        Long chatRoomId = chatRoomRepository.findByClub_IdAndType(clubId, ChatRoomType.CLUB)
                .orElseThrow().getId();
        chatService.sendMessage(leader.getId(), chatRoomId, "읽음 대상");

        chatService.markAsReadToLatest(member.getId(), clubId, chatRoomId);

        List<ChatRoomResponse> rooms = chatService.getChatRooms(member.getId(), clubId);
        assertThat(rooms.get(0).getUnreadCount()).isZero();
    }

    @Test
    void markAsRead_특정_메시지까지_읽음_처리된다() {
        Long chatRoomId = chatRoomRepository.findByClub_IdAndType(clubId, ChatRoomType.CLUB)
                .orElseThrow().getId();
        MessageResponse first = chatService.sendMessage(leader.getId(), chatRoomId, "첫 번째");
        chatService.sendMessage(leader.getId(), chatRoomId, "두 번째");

        chatService.markAsRead(member.getId(), chatRoomId, first.getMessageId());

        List<ChatRoomResponse> rooms = chatService.getChatRooms(member.getId(), clubId);
        assertThat(rooms.get(0).getUnreadCount()).isEqualTo(1);
    }

    @Test
    void deleteChatRoomsByScheduleId_채팅방과_메시지가_하드딜리트된다() {
        Long fakeScheduleId = 9999L;
        chatService.createChatRoomForSchedule(club, fakeScheduleId, leader);

        Long chatRoomId = chatRoomRepository.findByScheduleId(fakeScheduleId).orElseThrow().getId();
        chatService.sendMessage(leader.getId(), chatRoomId, "삭제될 메시지");

        chatService.deleteChatRoomsByScheduleId(fakeScheduleId);

        assertThat(chatRoomRepository.findByScheduleId(fakeScheduleId)).isEmpty();
        assertThat(messageRepository.findByChatRoomId(chatRoomId, PageRequest.of(0, 10))).isEmpty();
    }
}
