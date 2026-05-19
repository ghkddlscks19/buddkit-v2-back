# Notification API 구현 설계

## Goal

알림 생성(Kafka consumer), 알림 목록 조회, 읽음 처리, 삭제 API 구현.
FCM 전송은 인터페이스로 추상화하고 Stub 구현체 사용 (Firebase 키 준비 시 교체).

## Architecture

이벤트 발생 도메인이 `notification-events` Kafka 토픽으로 payload를 emit하면, `NotificationEventConsumer`가 소비해 DB 저장 + FCM 전송을 처리한다.

```
FeedService.addLike()       → emit → notification-events → DB 저장 + FCM stub
FeedService.addComment()    → emit → notification-events → DB 저장 + FCM stub
ScheduleService.create()    → emit → notification-events → DB 저장 + FCM stub
SettlementService.create()  → emit → notification-events → DB 저장 + FCM stub
ChatService.sendMessage()   → emit → notification-events → FCM stub만 (DB 저장 없음)
```

**FCM 추상화**: `FcmService` 인터페이스 + `StubFcmService` 구현체 (log만 출력). Firebase Admin SDK 준비 시 구현체만 교체.

## Tech Stack

- Spring Boot 4.0.6 / Kafka (KRaft) / Spring Data JPA / PostgreSQL
- `KafkaTemplate<String, String>` + `ObjectMapper` (tools.jackson.databind) — 기존 Search 도메인 패턴과 동일

## Kafka Topic

| 토픽 | 발행자 | 소비자 |
|------|--------|--------|
| `notification-events` | Feed, Schedule, Settlement, Chat 서비스 | NotificationEventConsumer |

**NotificationEventPayload**:
```json
{ "type": "LIKE", "targetUserId": 42, "content": "김철수님이 회원님의 게시물을 좋아합니다." }
```
- `content`: 발행 시점에 렌더링된 최종 메시지 문자열
- `type`: `NotificationTypeEnum` 값 그대로 직렬화

## Consumer 처리 규칙

| type | DB 저장 | FCM 전송 |
|------|---------|---------|
| SETTLEMENT | O | O |
| SCHEDULE | O | O |
| LIKE | O | O |
| COMMENT | O | O |
| CHAT | X | O |

DB 저장 시: `NotificationTypeRepository.findByType(type)`으로 `NotificationType` 엔티티 로드 → `Notification.create(content, notificationType, user)` → save.

## NOTIFICATION_TYPE 시딩

`CommandLineRunner` 빈으로 앱 시작 시 idempotent insert (존재하면 skip).

| type | template |
|------|----------|
| SETTLEMENT | {actor}님이 정산을 요청했습니다. |
| SCHEDULE | {clubName} 모임에 새 정모가 생겼습니다. |
| LIKE | {actor}님이 회원님의 게시물을 좋아합니다. |
| COMMENT | {actor}님이 댓글을 남겼습니다. |
| CHAT | (FCM 전용 — DB 미저장) |

`template`은 참고용. 실제 렌더링은 각 emit 시점에 수행되며 `content`로 payload에 포함.

## API Endpoints

모든 엔드포인트는 `@AuthenticationPrincipal Long userId` 인증 필요.

### GET /notifications
알림 목록 조회 (cursor 기반 무한 스크롤)

**Query params**: `lastId` (optional), `size` (default 20)

**Response** `ApiResponse<List<NotificationResponse>>`:
```json
[
  {
    "notificationId": 1,
    "type": "LIKE",
    "content": "김철수님이 회원님의 게시물을 좋아합니다.",
    "isRead": false,
    "createdAt": "2026-05-18T10:00:00"
  }
]
```

쿼리: `WHERE user_id = ? AND (lastId IS NULL OR notification_id < lastId) ORDER BY notification_id DESC LIMIT size`

### PATCH /notifications/{id}/read
단건 읽음 처리. 본인 알림이 아니면 `NotificationNotFoundException`.

**Response** `ApiResponse<Void>`

### PATCH /notifications/read-all
본인의 읽지 않은 알림 전체 읽음 처리.

**Response** `ApiResponse<Void>`

### DELETE /notifications/{id}
단건 삭제 (hard delete). 본인 알림이 아니면 `NotificationNotFoundException`.

**Response** `ResponseEntity<Void>` (204 No Content)

## File Structure

### 신규 파일

```
domain/notification/
├── controller/
│   └── NotificationController.java
├── dto/
│   ├── event/
│   │   └── NotificationEventPayload.java
│   └── response/
│       └── NotificationResponse.java
├── repository/
│   ├── NotificationRepository.java
│   └── NotificationTypeRepository.java
└── service/
    ├── FcmService.java                  ← 인터페이스
    ├── StubFcmService.java              ← stub 구현체
    ├── NotificationEventConsumer.java   ← Kafka consumer
    ├── NotificationService.java         ← 목록/읽음/삭제
    └── NotificationTypeSeeder.java      ← CommandLineRunner
```

### 수정 파일 (emit 추가)

| 파일 | 추가 위치 |
|------|----------|
| `FeedService.java` | `addLike()` — LIKE emit, `addComment()` — COMMENT emit |
| `ScheduleService.java` | `create()` — SCHEDULE emit (모임 멤버 전원) |
| `SettlementService.java` | `create()` — SETTLEMENT emit (스케줄 참여자 전원) |
| `ChatService.java` | `sendMessage()` — CHAT emit (채팅방 멤버 전원, 발신자 제외) |

### 기존 엔티티 수정

`Notification.java`에 메서드 추가:
```java
public void markAsRead() { this.isRead = true; }
public void markFcmSent() { this.fcmSent = true; }
```

## Exception

| 예외 클래스 | HTTP | 설명 |
|------------|------|------|
| `NotificationNotFoundException` | 404 | 알림 없음 또는 본인 알림 아님 |

`GlobalExceptionHandler`에 핸들러 추가.

## Test Plan

### NotificationServiceTest (`@SpringBootTest @Transactional`)
- `@MockitoBean KafkaTemplate` — emit은 테스트에서 제외
- 알림 목록 조회: cursor 없이 첫 페이지, lastId로 두 번째 페이지
- 단건 읽음 처리: isRead = true 확인
- 전체 읽음: 3개 미읽음 → 전체 읽음 후 0개 미읽음 확인
- 단건 삭제: 삭제 후 조회 시 없음 확인
- 타인 알림 읽음 시 `NotificationNotFoundException`

### NotificationEventConsumerTest (`@SpringBootTest`)
- `@MockitoBean FcmService` — FCM 실제 호출 방지
- LIKE 이벤트 consume → `Notification` DB 저장 확인
- CHAT 이벤트 consume → DB 저장 없음, `FcmService.send()` 1회 호출 확인
