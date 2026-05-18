package com.buddkitv2.domain.notification.service;

import com.buddkitv2.domain.common.Address;
import com.buddkitv2.domain.common.AddressRepository;
import com.buddkitv2.domain.notification.dto.response.NotificationResponse;
import com.buddkitv2.domain.notification.entity.Notification;
import com.buddkitv2.domain.notification.entity.NotificationType;
import com.buddkitv2.domain.notification.entity.NotificationTypeEnum;
import com.buddkitv2.domain.notification.repository.NotificationRepository;
import com.buddkitv2.domain.notification.repository.NotificationTypeRepository;
import com.buddkitv2.domain.user.entity.Gender;
import com.buddkitv2.domain.user.entity.Interest;
import com.buddkitv2.domain.user.entity.InterestCategory;
import com.buddkitv2.domain.user.entity.User;
import com.buddkitv2.domain.user.repository.InterestRepository;
import com.buddkitv2.domain.user.repository.UserRepository;
import com.buddkitv2.global.config.S3Service;
import com.buddkitv2.global.config.TossPaymentClient;
import com.buddkitv2.global.exception.NotificationNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class NotificationServiceTest {

    @Autowired NotificationService notificationService;
    @Autowired NotificationRepository notificationRepository;
    @Autowired NotificationTypeRepository notificationTypeRepository;
    @Autowired UserRepository userRepository;
    @Autowired AddressRepository addressRepository;
    @Autowired InterestRepository interestRepository;

    @MockitoBean KafkaTemplate<String, String> kafkaTemplate;
    @MockitoBean TossPaymentClient tossPaymentClient;
    @MockitoBean S3Service s3Service;

    private User userA;
    private User userB;
    private NotificationType likeType;

    @BeforeEach
    void setUp() {
        Address address = addressRepository.save(Address.of("서울특별시", "강남구", 11680));
        interestRepository.save(Interest.of(InterestCategory.CULTURE, "문화"));
        userA = userRepository.save(
                User.register(88881L, "유저A", LocalDate.of(1990, 5, 1), Gender.MALE, address, null));
        userB = userRepository.save(
                User.register(88882L, "유저B", LocalDate.of(1992, 8, 20), Gender.FEMALE, address, null));
        // NotificationTypeSeeder가 앱 시작 시 커밋한 행 조회
        likeType = notificationTypeRepository.findByType(NotificationTypeEnum.LIKE).orElseThrow();
    }

    private Notification save(User user, String content) {
        return notificationRepository.save(Notification.create(content, likeType, user));
    }

    @Test
    void 알림_목록_조회_첫페이지() {
        save(userA, "첫 번째 알림");
        save(userA, "두 번째 알림");
        save(userA, "세 번째 알림");

        List<NotificationResponse> result = notificationService.getNotifications(userA.getId(), null, 10);

        assertThat(result).hasSize(3);
        assertThat(result.get(0).getContent()).isEqualTo("세 번째 알림"); // 최신순
    }

    @Test
    void 알림_목록_조회_cursor_두번째페이지() {
        Notification n1 = save(userA, "오래된 알림");
        save(userA, "최신 알림");

        List<NotificationResponse> result = notificationService.getNotifications(userA.getId(), n1.getId() + 1, 10);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getContent()).isEqualTo("오래된 알림");
    }

    @Test
    void 단건_읽음_처리() {
        Notification n = save(userA, "읽음 테스트");
        assertThat(n.getIsRead()).isFalse();

        notificationService.markAsRead(userA.getId(), n.getId());

        Notification updated = notificationRepository.findById(n.getId()).orElseThrow();
        assertThat(updated.getIsRead()).isTrue();
    }

    @Test
    void 전체_읽음_처리() {
        save(userA, "알림1");
        save(userA, "알림2");
        save(userA, "알림3");

        notificationService.markAllAsRead(userA.getId());

        List<Notification> all = notificationRepository.findAll().stream()
                .filter(n -> n.getUser().getId().equals(userA.getId()))
                .toList();
        assertThat(all).allMatch(Notification::getIsRead);
    }

    @Test
    void 단건_삭제() {
        Notification n = save(userA, "삭제 테스트");

        notificationService.delete(userA.getId(), n.getId());

        assertThat(notificationRepository.findById(n.getId())).isEmpty();
    }

    @Test
    void 타인_알림_읽음_처리_시_예외() {
        Notification n = save(userA, "타인 알림");

        assertThatThrownBy(() -> notificationService.markAsRead(userB.getId(), n.getId()))
                .isInstanceOf(NotificationNotFoundException.class);
    }
}
