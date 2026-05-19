# Notification API 구현 플랜

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Kafka 이벤트 기반 알림 생성(DB 저장 + FCM stub) + 알림 목록 조회 / 읽음 처리 / 삭제 API 구현

**Architecture:** Feed/Schedule/Settlement/Chat 서비스가 `notification-events` Kafka 토픽으로 `NotificationEventPayload`를 emit하면, `NotificationEventConsumer`가 소비해 DB 저장 + FCM 전송을 처리한다. `FcmService` 인터페이스 + `StubFcmService` stub 구현체(로그 출력)로 추상화하고, Firebase 키 준비 시 구현체만 교체한다.

**Tech Stack:** Spring Boot 4.0.6, Spring Data JPA, PostgreSQL, Kafka (`KafkaTemplate<String,String>`, `@KafkaListener`), `tools.jackson.databind.ObjectMapper`

---

### Task 1: Notification 엔티티 메서드 + 예외 + GlobalExceptionHandler

**Files:**
- Modify: `src/main/java/com/buddkitv2/domain/notification/entity/Notification.java`
- Modify: `src/main/java/com/buddkitv2/domain/notification/entity/NotificationType.java`
- Create: `src/main/java/com/buddkitv2/global/exception/NotificationNotFoundException.java`
- Modify: `src/main/java/com/buddkitv2/global/exception/GlobalExceptionHandler.java`

- [ ] **Step 1: Notification.java에 markAsRead(), markFcmSent() 추가**

기존 `create()` 팩토리 메서드 아래에 추가:

```java
public void markAsRead() { this.isRead = true; }
public void markFcmSent() { this.fcmSent = true; }
```

- [ ] **Step 2: NotificationType.java에 of() 팩토리 메서드 추가**

기존 필드 아래에 추가:

```java
public static NotificationType of(NotificationTypeEnum type, String template) {
    NotificationType nt = new NotificationType();
    nt.type = type;
    nt.template = template;
    return nt;
}
```

- [ ] **Step 3: NotificationNotFoundException 생성**

```java
package com.buddkitv2.global.exception;

public class NotificationNotFoundException extends RuntimeException {
    public NotificationNotFoundException() {
        super("존재하지 않는 알림입니다.");
    }
}
```

- [ ] **Step 4: GlobalExceptionHandler에 핸들러 추가**

`src/main/java/com/buddkitv2/global/exception/GlobalExceptionHandler.java` 기존 핸들러 아래에 추가:

```java
@ExceptionHandler(NotificationNotFoundException.class)
public ResponseEntity<ApiResponse<Void>> handleNotificationNotFound(NotificationNotFoundException e) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.fail(e.getMessage()));
}
```

파일 상단 import 추가:
```java
import org.springframework.http.HttpStatus;
```

- [ ] **Step 5: 컴파일 확인**

```bash
./gradlew compileJava
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: 커밋**

```bash
git add src/main/java/com/buddkitv2/domain/notification/entity/Notification.java \
  src/main/java/com/buddkitv2/domain/notification/entity/NotificationType.java \
  src/main/java/com/buddkitv2/global/exception/NotificationNotFoundException.java \
  src/main/java/com/buddkitv2/global/exception/GlobalExceptionHandler.java
git commit -m "feat(notification): 알림 엔티티 메서드 + 예외 클래스 추가"
```

---

### Task 2: FcmService 인터페이스 + StubFcmService

**Files:**
- Create: `src/main/java/com/buddkitv2/domain/notification/service/FcmService.java`
- Create: `src/main/java/com/buddkitv2/domain/notification/service/StubFcmService.java`

- [ ] **Step 1: FcmService 인터페이스 생성**

```java
package com.buddkitv2.domain.notification.service;

public interface FcmService {
    void send(String token, String body);
}
```

- [ ] **Step 2: StubFcmService 구현체 생성**

```java
package com.buddkitv2.domain.notification.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class StubFcmService implements FcmService {

    @Override
    public void send(String token, String body) {
        log.info("[FCM stub] token={}, body={}", token, body);
    }
}
```

- [ ] **Step 3: 컴파일 확인**

```bash
./gradlew compileJava
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: 커밋**

```bash
git add src/main/java/com/buddkitv2/domain/notification/service/FcmService.java \
  src/main/java/com/buddkitv2/domain/notification/service/StubFcmService.java
git commit -m "feat(notification): FcmService stub 구현"
```

---

### Task 3: NotificationEventPayload + Repository + NotificationTypeSeeder

**Files:**
- Create: `src/main/java/com/buddkitv2/domain/notification/dto/event/NotificationEventPayload.java`
- Create: `src/main/java/com/buddkitv2/domain/notification/repository/NotificationTypeRepository.java`
- Create: `src/main/java/com/buddkitv2/domain/notification/repository/NotificationRepository.java`
- Create: `src/main/java/com/buddkitv2/domain/notification/service/NotificationTypeSeeder.java`

- [ ] **Step 1: NotificationEventPayload 생성**

```java
package com.buddkitv2.domain.notification.dto.event;

import com.buddkitv2.domain.notification.entity.NotificationTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NotificationEventPayload {
    private NotificationTypeEnum type;
    private Long targetUserId;
    private String content;
}
```

- [ ] **Step 2: NotificationTypeRepository 생성**

```java
package com.buddkitv2.domain.notification.repository;

import com.buddkitv2.domain.notification.entity.NotificationType;
import com.buddkitv2.domain.notification.entity.NotificationTypeEnum;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NotificationTypeRepository extends JpaRepository<NotificationType, Long> {
    Optional<NotificationType> findByType(NotificationTypeEnum type);
}
```

- [ ] **Step 3: NotificationRepository 생성**

JOIN FETCH로 `notificationType` N+1 방지. `lastId`가 없으면 첫 페이지, 있으면 cursor 이후 페이지를 반환한다.

```java
package com.buddkitv2.domain.notification.repository;

import com.buddkitv2.domain.notification.entity.Notification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @Query("SELECT n FROM Notification n JOIN FETCH n.notificationType WHERE n.user.id = :userId ORDER BY n.id DESC")
    List<Notification> findByUserId(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT n FROM Notification n JOIN FETCH n.notificationType WHERE n.user.id = :userId AND n.id < :lastId ORDER BY n.id DESC")
    List<Notification> findByUserIdAndLastId(@Param("userId") Long userId, @Param("lastId") Long lastId, Pageable pageable);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.user.id = :userId AND n.isRead = false")
    void markAllAsRead(@Param("userId") Long userId);
}
```

- [ ] **Step 4: NotificationTypeSeeder 생성**

앱 시작 시 `NOTIFICATION_TYPE` 테이블에 5개 행을 idempotent insert한다(이미 존재하면 skip).

```java
package com.buddkitv2.domain.notification.service;

import com.buddkitv2.domain.notification.entity.NotificationType;
import com.buddkitv2.domain.notification.entity.NotificationTypeEnum;
import com.buddkitv2.domain.notification.repository.NotificationTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class NotificationTypeSeeder implements CommandLineRunner {

    private final NotificationTypeRepository notificationTypeRepository;

    @Override
    @Transactional
    public void run(String... args) {
        seed(NotificationTypeEnum.SETTLEMENT, "{actor}님이 정산을 요청했습니다.");
        seed(NotificationTypeEnum.SCHEDULE, "{clubName} 모임에 새 정모가 생겼습니다.");
        seed(NotificationTypeEnum.LIKE, "{actor}님이 회원님의 게시물을 좋아합니다.");
        seed(NotificationTypeEnum.COMMENT, "{actor}님이 댓글을 남겼습니다.");
        seed(NotificationTypeEnum.CHAT, "");
    }

    private void seed(NotificationTypeEnum type, String template) {
        if (notificationTypeRepository.findByType(type).isEmpty()) {
            notificationTypeRepository.save(NotificationType.of(type, template));
        }
    }
}
```

- [ ] **Step 5: 컴파일 확인**

```bash
./gradlew compileJava
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: 커밋**

```bash
git add src/main/java/com/buddkitv2/domain/notification/dto/event/NotificationEventPayload.java \
  src/main/java/com/buddkitv2/domain/notification/repository/NotificationTypeRepository.java \
  src/main/java/com/buddkitv2/domain/notification/repository/NotificationRepository.java \
  src/main/java/com/buddkitv2/domain/notification/service/NotificationTypeSeeder.java
git commit -m "feat(notification): payload, 리포지토리, NOTIFICATION_TYPE 시더 추가"
```

---

### Task 4: NotificationEventConsumer (TDD)

**Files:**
- Create: `src/test/java/com/buddkitv2/domain/notification/service/NotificationEventConsumerTest.java`
- Create: `src/main/java/com/buddkitv2/domain/notification/service/NotificationEventConsumer.java`

- [ ] **Step 1: 실패 테스트 작성**

`NotificationTypeSeeder`가 `@SpringBootTest` 컨텍스트 시작 시 자동으로 실행돼 `NOTIFICATION_TYPE` 5개 행을 커밋한다. 테스트 메서드의 `@Transactional` 롤백과 무관하게 조회 가능하다.

```java
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
```

- [ ] **Step 2: 테스트 실행 — FAIL 확인**

```bash
./gradlew test --tests "com.buddkitv2.domain.notification.service.NotificationEventConsumerTest" 2>&1 | grep -E "FAIL|error|NotificationEventConsumer"
```
Expected: FAIL (NotificationEventConsumer 클래스 없음)

- [ ] **Step 3: NotificationEventConsumer 구현**

```java
package com.buddkitv2.domain.notification.service;

import com.buddkitv2.domain.notification.dto.event.NotificationEventPayload;
import com.buddkitv2.domain.notification.entity.Notification;
import com.buddkitv2.domain.notification.entity.NotificationType;
import com.buddkitv2.domain.notification.entity.NotificationTypeEnum;
import com.buddkitv2.domain.notification.repository.NotificationRepository;
import com.buddkitv2.domain.notification.repository.NotificationTypeRepository;
import com.buddkitv2.domain.user.entity.User;
import com.buddkitv2.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class NotificationEventConsumer {

    private final NotificationRepository notificationRepository;
    private final NotificationTypeRepository notificationTypeRepository;
    private final UserRepository userRepository;
    private final FcmService fcmService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "notification-events", groupId = "buddkit-notification")
    @Transactional
    public void handleNotificationEvent(String message) {
        try {
            NotificationEventPayload payload = objectMapper.readValue(message, NotificationEventPayload.class);
            User user = userRepository.findById(payload.getTargetUserId()).orElse(null);
            if (user == null) return;

            if (payload.getType() != NotificationTypeEnum.CHAT) {
                NotificationType type = notificationTypeRepository.findByType(payload.getType())
                        .orElseThrow();
                notificationRepository.save(Notification.create(payload.getContent(), type, user));
            }

            if (user.getFcmToken() != null) {
                fcmService.send(user.getFcmToken(), payload.getContent());
            }
        } catch (Exception e) {
            // 역직렬화 실패 또는 DB 오류 시 해당 메시지 스킵
        }
    }
}
```

- [ ] **Step 4: 테스트 실행 — PASS 확인**

```bash
./gradlew test --tests "com.buddkitv2.domain.notification.service.NotificationEventConsumerTest" 2>&1 | grep -E "tests completed|FAILED"
```
Expected: 2 tests completed, 0 failed

- [ ] **Step 5: 커밋**

```bash
git add src/test/java/com/buddkitv2/domain/notification/service/NotificationEventConsumerTest.java \
  src/main/java/com/buddkitv2/domain/notification/service/NotificationEventConsumer.java
git commit -m "feat(notification): NotificationEventConsumer Kafka consumer 구현"
```

---

### Task 5: NotificationService + NotificationResponse (TDD)

**Files:**
- Create: `src/test/java/com/buddkitv2/domain/notification/service/NotificationServiceTest.java`
- Create: `src/main/java/com/buddkitv2/domain/notification/dto/response/NotificationResponse.java`
- Create: `src/main/java/com/buddkitv2/domain/notification/service/NotificationService.java`

- [ ] **Step 1: 실패 테스트 작성**

```java
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

        // lastId = n1.id + 1 이면 n1만 반환
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
```

- [ ] **Step 2: 테스트 실행 — FAIL 확인**

```bash
./gradlew test --tests "com.buddkitv2.domain.notification.service.NotificationServiceTest" 2>&1 | grep -E "FAIL|error|NotificationService"
```
Expected: FAIL (NotificationService 클래스 없음)

- [ ] **Step 3: NotificationResponse 생성**

```java
package com.buddkitv2.domain.notification.dto.response;

import com.buddkitv2.domain.notification.entity.Notification;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class NotificationResponse {
    private Long notificationId;
    private String type;
    private String content;
    private Boolean isRead;
    private LocalDateTime createdAt;

    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(
                n.getId(),
                n.getNotificationType().getType().name(),
                n.getContent(),
                n.getIsRead(),
                n.getCreatedAt()
        );
    }
}
```

- [ ] **Step 4: NotificationService 구현**

```java
package com.buddkitv2.domain.notification.service;

import com.buddkitv2.domain.notification.dto.response.NotificationResponse;
import com.buddkitv2.domain.notification.entity.Notification;
import com.buddkitv2.domain.notification.repository.NotificationRepository;
import com.buddkitv2.global.exception.NotificationNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public List<NotificationResponse> getNotifications(Long userId, Long lastId, int size) {
        List<Notification> notifications = lastId == null
                ? notificationRepository.findByUserId(userId, PageRequest.of(0, size))
                : notificationRepository.findByUserIdAndLastId(userId, lastId, PageRequest.of(0, size));
        return notifications.stream().map(NotificationResponse::from).toList();
    }

    @Transactional
    public void markAsRead(Long userId, Long notificationId) {
        Notification n = notificationRepository.findById(notificationId)
                .filter(it -> it.getUser().getId().equals(userId))
                .orElseThrow(NotificationNotFoundException::new);
        n.markAsRead();
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        notificationRepository.markAllAsRead(userId);
    }

    @Transactional
    public void delete(Long userId, Long notificationId) {
        Notification n = notificationRepository.findById(notificationId)
                .filter(it -> it.getUser().getId().equals(userId))
                .orElseThrow(NotificationNotFoundException::new);
        notificationRepository.delete(n);
    }
}
```

- [ ] **Step 5: 테스트 실행 — PASS 확인**

```bash
./gradlew test --tests "com.buddkitv2.domain.notification.service.NotificationServiceTest" 2>&1 | grep -E "tests completed|FAILED"
```
Expected: 6 tests completed, 0 failed

- [ ] **Step 6: 커밋**

```bash
git add src/test/java/com/buddkitv2/domain/notification/service/NotificationServiceTest.java \
  src/main/java/com/buddkitv2/domain/notification/dto/response/NotificationResponse.java \
  src/main/java/com/buddkitv2/domain/notification/service/NotificationService.java
git commit -m "feat(notification): NotificationService + 응답 DTO 구현"
```

---

### Task 6: NotificationController

**Files:**
- Create: `src/main/java/com/buddkitv2/domain/notification/controller/NotificationController.java`

- [ ] **Step 1: NotificationController 구현**

```java
package com.buddkitv2.domain.notification.controller;

import com.buddkitv2.domain.notification.dto.response.NotificationResponse;
import com.buddkitv2.domain.notification.service.NotificationService;
import com.buddkitv2.global.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ApiResponse<List<NotificationResponse>> getNotifications(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) Long lastId,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(notificationService.getNotifications(userId, lastId, size));
    }

    @PatchMapping("/{id}/read")
    public ApiResponse<Void> markAsRead(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long id) {
        notificationService.markAsRead(userId, id);
        return ApiResponse.ok(null);
    }

    @PatchMapping("/read-all")
    public ApiResponse<Void> markAllAsRead(@AuthenticationPrincipal Long userId) {
        notificationService.markAllAsRead(userId);
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long id) {
        notificationService.delete(userId, id);
        return ResponseEntity.noContent().build();
    }
}
```

- [ ] **Step 2: 컴파일 확인**

```bash
./gradlew compileJava
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: 커밋**

```bash
git add src/main/java/com/buddkitv2/domain/notification/controller/NotificationController.java
git commit -m "feat(notification): NotificationController 구현 (4 endpoints)"
```

---

### Task 7: Repository 메서드 추가 + 이벤트 emit (Feed/Schedule/Settlement/Chat)

**Files:**
- Modify: `src/main/java/com/buddkitv2/domain/club/repository/UserClubRepository.java`
- Modify: `src/main/java/com/buddkitv2/domain/chat/repository/UserChatRoomRepository.java`
- Modify: `src/main/java/com/buddkitv2/domain/feed/service/FeedService.java`
- Modify: `src/main/java/com/buddkitv2/domain/schedule/service/ScheduleService.java`
- Modify: `src/main/java/com/buddkitv2/domain/settlement/service/SettlementService.java`
- Modify: `src/main/java/com/buddkitv2/domain/chat/service/ChatService.java`

- [ ] **Step 1: UserClubRepository에 findByClub_Id 추가**

기존 `UserClubRepository.java`에 다음 메서드 추가:

```java
List<UserClub> findByClub_Id(Long clubId);
```

- [ ] **Step 2: UserChatRoomRepository에 findByChatRoom_Id 추가**

기존 `UserChatRoomRepository.java`에 다음 메서드 추가:

```java
List<UserChatRoom> findByChatRoom_Id(Long chatRoomId);
```

- [ ] **Step 3: FeedService에 emit 추가**

`FeedService.java` 수정:

1. 클래스 필드에 추가 (기존 `private final FeedCommentRepository feedCommentRepository;` 아래):
```java
private final KafkaTemplate<String, String> kafkaTemplate;
private final ObjectMapper objectMapper;
```

2. import 추가:
```java
import com.buddkitv2.domain.notification.dto.event.NotificationEventPayload;
import com.buddkitv2.domain.notification.entity.NotificationTypeEnum;
import org.springframework.kafka.core.KafkaTemplate;
import tools.jackson.databind.ObjectMapper;
```

3. 클래스 맨 아래에 helper 메서드 추가:
```java
private void emitNotification(NotificationTypeEnum type, Long targetUserId, String content) {
    try {
        kafkaTemplate.send("notification-events", String.valueOf(targetUserId),
                objectMapper.writeValueAsString(new NotificationEventPayload(type, targetUserId, content)));
    } catch (Exception e) {
        // 알림 emit 실패는 피드 동작에 영향 없음
    }
}
```

4. `likeFeed()` 메서드에서 `feed.incrementLike();` 다음에 추가:
```java
if (!feed.getUser().getId().equals(userId)) {
    emitNotification(NotificationTypeEnum.LIKE, feed.getUser().getId(),
            uc.getUser().getNickname() + "님이 회원님의 게시물을 좋아합니다.");
}
```

5. `createComment()` 메서드에서 `return toCommentResponse(comment);` 직전에 추가:
```java
if (!feed.getUser().getId().equals(userId)) {
    emitNotification(NotificationTypeEnum.COMMENT, feed.getUser().getId(),
            uc.getUser().getNickname() + "님이 댓글을 남겼습니다.");
}
```

- [ ] **Step 4: ScheduleService에 emit 추가**

`ScheduleService.java` 수정:

1. 필드 추가:
```java
private final KafkaTemplate<String, String> kafkaTemplate;
private final ObjectMapper objectMapper;
```

2. import 추가:
```java
import com.buddkitv2.domain.notification.dto.event.NotificationEventPayload;
import com.buddkitv2.domain.notification.entity.NotificationTypeEnum;
import org.springframework.kafka.core.KafkaTemplate;
import tools.jackson.databind.ObjectMapper;
```

3. helper 메서드 추가:
```java
private void emitNotification(NotificationTypeEnum type, Long targetUserId, String content) {
    try {
        kafkaTemplate.send("notification-events", String.valueOf(targetUserId),
                objectMapper.writeValueAsString(new NotificationEventPayload(type, targetUserId, content)));
    } catch (Exception e) {
        // 알림 emit 실패는 스케줄 동작에 영향 없음
    }
}
```

4. `createSchedule()` 메서드에서 `return toResponse(schedule, userId);` 직전에 추가:
```java
String content = userClub.getClub().getName() + " 모임에 새 정모가 생겼습니다.";
userClubRepository.findByClub_Id(userClub.getClub().getId())
        .forEach(uc -> emitNotification(NotificationTypeEnum.SCHEDULE, uc.getUser().getId(), content));
```

- [ ] **Step 5: SettlementService에 emit 추가**

`SettlementService.java` 수정:

1. 필드 추가:
```java
private final KafkaTemplate<String, String> kafkaTemplate;
private final ObjectMapper objectMapper;
```

2. import 추가:
```java
import com.buddkitv2.domain.notification.dto.event.NotificationEventPayload;
import com.buddkitv2.domain.notification.entity.NotificationTypeEnum;
import org.springframework.kafka.core.KafkaTemplate;
import tools.jackson.databind.ObjectMapper;
```

3. helper 메서드 추가:
```java
private void emitNotification(NotificationTypeEnum type, Long targetUserId, String content) {
    try {
        kafkaTemplate.send("notification-events", String.valueOf(targetUserId),
                objectMapper.writeValueAsString(new NotificationEventPayload(type, targetUserId, content)));
    } catch (Exception e) {
        // 알림 emit 실패는 정산 동작에 영향 없음
    }
}
```

4. `requestSettlement()` 메서드의 기존 `for (UserSchedule us : members)` 루프에서 `userSettlementRepository.save(...)` 다음에 추가:
```java
emitNotification(NotificationTypeEnum.SETTLEMENT, us.getUser().getId(),
        leaderUser.getNickname() + "님이 정산을 요청했습니다.");
```

- [ ] **Step 6: ChatService에 emit 추가**

`ChatService.java` 수정:

1. 필드 추가:
```java
private final KafkaTemplate<String, String> kafkaTemplate;
private final ObjectMapper objectMapper;
```

2. import 추가:
```java
import com.buddkitv2.domain.notification.dto.event.NotificationEventPayload;
import com.buddkitv2.domain.notification.entity.NotificationTypeEnum;
import org.springframework.kafka.core.KafkaTemplate;
import tools.jackson.databind.ObjectMapper;
```

3. helper 메서드 추가:
```java
private void emitNotification(NotificationTypeEnum type, Long targetUserId, String content) {
    try {
        kafkaTemplate.send("notification-events", String.valueOf(targetUserId),
                objectMapper.writeValueAsString(new NotificationEventPayload(type, targetUserId, content)));
    } catch (Exception e) {
        // 알림 emit 실패는 채팅 동작에 영향 없음
    }
}
```

4. `sendMessage()` 메서드에서 `return toMessageResponse(message);` 직전에 추가:
```java
String chatContent = user.getNickname() + ": " + text;
userChatRoomRepository.findByChatRoom_Id(chatRoomId).stream()
        .filter(m -> !m.getUser().getId().equals(userId))
        .forEach(m -> emitNotification(NotificationTypeEnum.CHAT, m.getUser().getId(), chatContent));
```

- [ ] **Step 7: 전체 테스트 통과 확인**

```bash
./gradlew test 2>&1 | grep -E "tests completed|FAILED"
```
Expected: N tests completed, 0 failed

- [ ] **Step 8: 커밋**

```bash
git add src/main/java/com/buddkitv2/domain/club/repository/UserClubRepository.java \
  src/main/java/com/buddkitv2/domain/chat/repository/UserChatRoomRepository.java \
  src/main/java/com/buddkitv2/domain/feed/service/FeedService.java \
  src/main/java/com/buddkitv2/domain/schedule/service/ScheduleService.java \
  src/main/java/com/buddkitv2/domain/settlement/service/SettlementService.java \
  src/main/java/com/buddkitv2/domain/chat/service/ChatService.java
git commit -m "feat(notification): Feed/Schedule/Settlement/Chat 서비스에 notification-events emit 추가"
```
