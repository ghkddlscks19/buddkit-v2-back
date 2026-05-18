# Chat 도메인 API 설계

## 개요

모임(Club) 전체 채팅 + 스케줄별 채팅을 WebSocket(STOMP) + Redis pub/sub로 구현한다.
메시지 히스토리 조회, 삭제, 읽음 처리는 HTTP REST API로 제공한다.

---

## 아키텍처

```
클라이언트
  │
  ├─ HTTP REST ──▶ ChatController ──▶ ChatService ──▶ DB (목록/삭제/읽음)
  │
  └─ WebSocket
       ├─ STOMP /app/** ──▶ ChatMessageHandler ──▶ Redis pub
       │                                              │
       └─ STOMP /topic/** ◀── Redis sub ────────────┘
```

- **HTTP**: 목록 조회, 메시지 삭제, 읽음 처리 (채팅방 입장 시)
- **WebSocket**: 메시지 전송/수신, 읽음 처리 (채팅방 내 실시간)
- **Redis pub/sub**: 서버 간 브로드캐스트 릴레이 (수평 확장 지원)

---

## REST API

| 메서드 | 경로 | 설명 | 인증 |
|---|---|---|---|
| `GET` | `/clubs/{clubId}/chat-rooms` | 채팅방 목록 (unread count 포함) | 필요 |
| `GET` | `/clubs/{clubId}/chat-rooms/{chatRoomId}/messages` | 메시지 목록 (cursor 기반 무한 스크롤) | 필요 |
| `DELETE` | `/clubs/{clubId}/chat-rooms/{chatRoomId}/messages/{messageId}` | 메시지 삭제 (본인 메시지만) | 필요 |
| `PUT` | `/clubs/{clubId}/chat-rooms/{chatRoomId}/read` | 채팅방 입장 시 읽음 처리 | 필요 |

> 채팅방 생성 API 없음 — ClubService/ScheduleService에서 자동 생성

### GET /clubs/{clubId}/chat-rooms 응답 예시

```json
{
  "success": true,
  "data": [
    {
      "chatRoomId": 1,
      "type": "CLUB",
      "name": "버드킷 클럽 전체 채팅",
      "lastMessage": "안녕하세요",
      "lastMessageAt": "2026-05-18T10:00:00",
      "unreadCount": 3
    },
    {
      "chatRoomId": 2,
      "type": "SCHEDULE",
      "name": "5월 정모",
      "lastMessage": "장소 공유합니다",
      "lastMessageAt": "2026-05-17T20:00:00",
      "unreadCount": 0
    }
  ],
  "message": null
}
```

### GET /clubs/{clubId}/chat-rooms/{chatRoomId}/messages 쿼리 파라미터

| 파라미터 | 타입 | 기본값 | 설명 |
|---|---|---|---|
| `lastId` | Long | null | cursor (없으면 최신부터) |
| `size` | int | 30 | 페이지 크기 |

### 메시지 목록 응답 예시

```json
{
  "success": true,
  "data": [
    {
      "messageId": 123,
      "userId": 42,
      "nickname": "홍길동",
      "text": "안녕하세요",
      "sentAt": "2026-05-18T10:00:00",
      "deleted": false
    }
  ],
  "message": null
}
```

---

## WebSocket (STOMP)

### 연결

- Handshake endpoint: `ws://host/ws`
- SockJS fallback 지원

### Destinations

| 방향 | Destination | 설명 |
|---|---|---|
| SUBSCRIBE | `/topic/chat-rooms/{chatRoomId}` | 채팅방 메시지 수신 |
| SEND | `/app/chat-rooms/{chatRoomId}/messages` | 메시지 전송 |
| SEND | `/app/chat-rooms/{chatRoomId}/read` | 읽음 처리 (채팅방 내) |

### 메시지 전송 payload (클라이언트 → 서버)

```json
{ "text": "안녕하세요" }
```

### 브로드캐스트 payload (서버 → 구독자)

```json
{
  "messageId": 123,
  "userId": 42,
  "nickname": "홍길동",
  "text": "안녕하세요",
  "sentAt": "2026-05-18T10:00:00"
}
```

### 읽음 처리 payload (클라이언트 → 서버)

```json
{ "lastReadMessageId": 123 }
```

---

## 읽음 처리 전략

- **채팅방 입장 시**: `PUT /clubs/{clubId}/chat-rooms/{chatRoomId}/read` 호출 → `lastReadMessageId` = 현재 최신 메시지 ID
- **채팅방 내 새 메시지 수신 시**: STOMP `/app/chat-rooms/{chatRoomId}/read` 전송 → `lastReadMessageId` 업데이트
- **unread count 계산**: `SELECT COUNT(*) FROM MESSAGE WHERE chat_room_id = ? AND message_id > lastReadMessageId AND deleted_at IS NULL`

---

## 자동 생성/삭제 연동

ChatService를 다른 서비스에서 직접 호출하여 처리한다. 이벤트 기반이 아닌 동기 호출로 한 트랜잭션에서 처리.

| 이벤트 | 처리 |
|---|---|
| 클럽 생성 (`ClubService`) | `ChatRoom(CLUB)` + 모임장 `UserChatRoom(LEADER)` 생성 |
| 클럽 가입 (`ClubService`) | CLUB 타입 채팅방에 `UserChatRoom(MEMBER)` 추가 |
| 클럽 탈퇴 (`ClubService`) | `UserChatRoom` 제거 |
| 스케줄 생성 (`ScheduleService`) | `ChatRoom(SCHEDULE)` + 모임장 `UserChatRoom(LEADER)` 생성 |
| 스케줄 참여 (`ScheduleService`) | SCHEDULE 타입 채팅방에 `UserChatRoom(MEMBER)` 추가 |
| 스케줄 삭제 (`ScheduleService`) | `ChatRoom` + `Message` 하드 딜리트 (한 트랜잭션) |

---

## 패키지 구조

```
domain/chat/
  controller/
    ChatController.java          ← REST API
    ChatMessageHandler.java      ← STOMP @MessageMapping
  dto/
    request/
      SendMessageRequest.java
      ReadRequest.java
    response/
      ChatRoomResponse.java
      MessageResponse.java
  entity/                        ← 기존 엔티티 그대로 사용
    ChatRoom.java
    Message.java
    UserChatRoom.java
    UserChatRoomId.java
    ChatRoomType.java
    ChatRoomRole.java
  repository/
    ChatRoomRepository.java
    MessageRepository.java
    UserChatRoomRepository.java
  service/
    ChatService.java

global/config/
  WebSocketConfig.java           ← STOMP + Redis 브로커 설정
```

---

## 권한 규칙

- 채팅방 접근: 해당 클럽의 `UserChatRoom`이 존재하는 경우만 허용
- 메시지 삭제: 본인 메시지만 삭제 가능 (소프트 딜리트 — `deletedAt` 설정)
- WebSocket 인증: Handshake 시 JWT 검증

---

## 제외 범위 (v1)

- 채팅방 참여자 목록 API (클럽 멤버 목록으로 대체)
- 채팅방 상세 조회 API (목록 응답으로 충분)
- 메시지 수정
- 파일/이미지 전송
