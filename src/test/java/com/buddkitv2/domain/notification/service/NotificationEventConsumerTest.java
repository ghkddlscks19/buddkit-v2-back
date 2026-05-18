package com.buddkitv2.domain.notification.service;

import com.buddkitv2.domain.common.Address;
import com.buddkitv2.domain.common.AddressRepository;
import com.buddkitv2.domain.notification.dto.event.NotificationEventPayload;
import com.buddkitv2.domain.notification.entity.Notification;
import com.buddkitv2.domain.notification.entity.NotificationTypeEnum;
import com.buddkitv2.domain.notification.repository.NotificationRepository;
import com.buddkitv2.domain.user.entity.Gender;
import com.buddkitv2.domain.user.entity.Interest;
import com.buddkitv2.domain.user.entity.InterestCategory;
import com.buddkitv2.domain.user.entity.User;
import com.buddkitv2.domain.user.repository.InterestRepository;
import com.buddkitv2.domain.user.repository.UserRepository;
import com.buddkitv2.global.config.S3Service;
import com.buddkitv2.global.config.TossPaymentClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@SpringBootTest
@Transactional
class NotificationEventConsumerTest {

    @Autowired NotificationEventConsumer consumer;
    @Autowired NotificationRepository notificationRepository;
    @Autowired UserRepository userRepository;
    @Autowired AddressRepository addressRepository;
    @Autowired InterestRepository interestRepository;
    @Autowired ObjectMapper objectMapper;

    @MockitoBean KafkaTemplate<String, String> kafkaTemplate;
    @MockitoBean TossPaymentClient tossPaymentClient;
    @MockitoBean S3Service s3Service;
    @MockitoBean FcmService fcmService;

    private User user;

    @BeforeEach
    void setUp() {
        Address address = addressRepository.save(Address.of("서울특별시", "마포구", 11440));
        interestRepository.save(Interest.of(InterestCategory.SPORTS, "운동/스포츠"));
        user = userRepository.save(
                User.register(77777L, "소비자", LocalDate.of(1995, 3, 15), Gender.FEMALE, address, null));
    }

    @Test
    void LIKE_이벤트_consume시_Notification_DB_저장() throws Exception {
        String payload = objectMapper.writeValueAsString(
                new NotificationEventPayload(NotificationTypeEnum.LIKE, user.getId(),
                        "홍길동님이 게시물을 좋아합니다."));

        consumer.handleNotificationEvent(payload);

        List<Notification> saved = notificationRepository.findAll().stream()
                .filter(n -> n.getUser().getId().equals(user.getId()))
                .toList();
        assertThat(saved).hasSize(1);
        assertThat(saved.get(0).getContent()).isEqualTo("홍길동님이 게시물을 좋아합니다.");
        assertThat(saved.get(0).getIsRead()).isFalse();
    }

    @Test
    void CHAT_이벤트_consume시_DB_저장_없음_fcmToken_없으면_FCM_전송_안함() throws Exception {
        String payload = objectMapper.writeValueAsString(
                new NotificationEventPayload(NotificationTypeEnum.CHAT, user.getId(), "안녕하세요"));

        consumer.handleNotificationEvent(payload);

        List<Notification> saved = notificationRepository.findAll().stream()
                .filter(n -> n.getUser().getId().equals(user.getId()))
                .toList();
        assertThat(saved).isEmpty();
        verify(fcmService, never()).send(any(), any());
    }
}
