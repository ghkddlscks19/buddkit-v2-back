# Domain

## 서비스 개요

관심사 기반 소셜 모임 플랫폼. 사용자는 모임에 가입하고, 정기모임(스케줄)을 통해 오프라인 활동을 하며, 참가비를 앱 내 정산으로 처리한다.

---

## 핵심 용어

### USER (회원)
- **회원**: 카카오 소셜 로그인으로 가입한 사용자. 탈퇴는 상태 기반(`UserStatus`)으로 표현 — 소프트 딜리트와 별개.
- **관심사 (Interest)**: 회원이 선택하는 취미 카테고리. 문화(공연/전시), 운동/스포츠, 여행, 음악, 공예, 사교, 외국어, 재테크. 최소 1개 ~ 최대 5개 선택 필수. `INTEREST` 테이블에 고정 8개 행 존재 — 단순 enum이 아닌 엔티티인 이유는 프론트엔드에 표시할 한국어 레이블(`name`) 등 부가 속성이 필요하기 때문.
- **지역**: 도/특별시/광역시(`city`) + 시/군/구(`district`) 조합. `Address` 테이블에 약 250행 고정. `User`와 `Club`은 `address_id` FK로 참조 — city/district 문자열을 직접 저장하지 않음. 유효하지 않은 조합 저장을 DB 레벨에서 방지.
- **FCM 토큰**: 푸시 알림 전송을 위한 Firebase Cloud Messaging 토큰. `USER.fcm_token`에 저장.

### CLUB (모임)
- **모임**: 관심사와 활동 지역 기반의 소셜 그룹. `CLUB` 테이블. 단일 `Interest` FK 참조 (복수 관심사 불가).
- **모임장**: 모임을 생성한 대표 관리자. `UserClubRole.LEADER`로 표현. 스케줄 생성/수정/삭제 및 정산 생성 권한을 독점 보유. `MEMBER`에게는 어떤 관리 권한도 없음. 탈퇴 시 권한 위임 필수. ※ 과거 설계의 "운영진(MANAGER)" 개념은 제거됨 — `UserClubRole`은 `LEADER / MEMBER` 두 값만 존재.
- **memberCount**: `UserClub` 행 수와 항상 일치해야 하는 비정규화 컬럼. 모임 목록 조회 시 COUNT 쿼리를 피하기 위한 성능 목적. 가입 시 +1, 탈퇴 시 -1을 서비스 레이어에서 동일 트랜잭션으로 처리.
- **찜 (Club Like)**: 모임 즐겨찾기. `CLUB_LIKE` 테이블.
- **USER_CLUB**: 회원-모임 소속 관계 및 역할(`role`) 관리.

### SCHEDULE (스케줄 / 정모)
- **스케줄**: 모임 내 정기모임 이벤트. `SCHEDULE` 테이블. 모임장(`UserClubRole.LEADER`)만 생성/수정/삭제 가능.
- **정모**: 스케줄과 동의어로 혼용. 채팅방 타입 `CHAT_ROOM.type`에서 '정모' 값으로 사용.
- **스케줄 상태**: 모집 중 → 진행 중 → 정산 중 → 종료. 시작 시간 경과 후 상태 변경.
- **참가비 (cost)**: 스케줄 참여 시 부담할 비용. `SCHEDULE.cost`.
- **USER_SCHEDULE**: 회원의 스케줄 참여 정보 및 역할(`role`) 관리.

### SETTLEMENT (정산)
- **정산 (Settlement)**: 스케줄 참가비 전체 정산 건. `SETTLEMENT` 테이블. 상태: 정산 요청 → 진행 중 → 완료. `Settlement.user`는 정산을 생성하고 참가비를 수령하는 모임장(LEADER)을 가리킴.
- **completedTime 구분**: `UserSettlement.completedTime`은 개별 참여자의 정산 완료 시각(참여자별로 각각 찍힘). `Settlement.completedTime`은 전체 정산 건의 완료 시각(모든 참여자의 정산이 끝났을 때 찍힘).
- **USER_SETTLEMENT**: 개별 회원의 정산 처리 상태. 상태: 정산 요청 → 정산 확인 대기 → 정산 완료. 타입: `POINT` 단일 — 모든 정산은 앱 내 포인트(Wallet)로만 처리. 토스페이먼츠로 포인트를 충전(`CHARGE`)하고, 정산 시 모임장 지갑으로 이체(`TRANSFER`).
- **TRANSFER**: 정산 완료 시 생성되는 이체 이력. `transfer_id`는 외부 이체 식별자.
- **정산 수동 변경**: 현금 등 외부 수단으로 정산 완료 시 모임장이 수동으로 상태 변경 가능.
- `UserScheduleRole.LEADER`는 스케줄을 생성한 모임장이자 정산 수령자. 항상 `UserClubRole.LEADER`와 동일 인물.

### WALLET (지갑)
- **지갑 (Wallet)**: 앱 내 가상 지갑. 단위: 한국 원화 정수(`Long`). `WALLET` 테이블.
- **WALLET_TRANSACTION**: 지갑 거래 내역. `wallet` = 거래 주체 지갑, `targetWallet` = 수신 지갑. 항상 non-null.
  - `TRANSFER`: `wallet` → `targetWallet` (타인 지갑으로 송금)
  - `CHARGE`: `wallet` == `targetWallet` (자기 자신 지갑 — 외부 결제로 충전)
- **Payment**: 토스페이먼츠 결제 건. `payment_id`는 UUID 타입. `toss_payment_key` unique. `WalletTransaction`을 FK로 참조하며, 항상 `CHARGE` 타입 거래에만 연결됨 — 이 제약은 서비스 레이어에서 보장.
- **금액 타입**: 모든 금액 필드는 `Long`으로 통일. `Integer` 최대값(약 21억)을 초과하는 정산 합계를 안전하게 표현하기 위함. `BigDecimal` 사용하지 않음.
- **결제 수단**: 가상 계좌, 카드 결제. 스케줄 생성 시 참여자별 가상 계좌 생성.

### FEED (피드)
- **피드 (Feed)**: 모임 내 사진+글 게시물. `FEED` 테이블. 사진 최소 1개 ~ 최대 5개.
- **피드 이미지 (Feed Image)**: 피드에 첨부된 사진. 첫 번째 이미지를 썸네일로 사용(압축). `FEED_IMAGE` 테이블.
- **피드 좋아요**: `FEED_LIKE` 테이블.
- **피드 댓글**: `FEED_COMMENT` 테이블. 삭제는 피드 작성자 또는 댓글 작성자만 가능.

### NOTIFICATION (알림)
- **알림 (Notification)**: 이벤트 발생 시 생성되는 인앱 알림. `NOTIFICATION` 테이블.
- **NOTIFICATION_TYPE**: 알림 유형과 메시지 템플릿 정의. 렌더링된 최종 메시지는 `NOTIFICATION.content`에 저장.
- **PUSH 알림**: FCM을 통해 디바이스로 전송. `fcm_sent` 플래그로 전송 여부 추적.
- **알림 타입별 동작**:
  - `SETTLEMENT`, `SCHEDULE`, `LIKE`, `COMMENT`: DB에 `Notification` 행 저장 + FCM 전송. 목록 조회 가능.
  - `CHAT`: FCM 전송만. `Notification` 행을 저장하지 않음 — 서비스 레이어에서 insert를 생략하는 방식으로 보장.

### CHAT (채팅)
- **채팅방 (Chat Room)**: `CHAT_ROOM` 테이블. 타입: 전체(모임 전체 채팅), 정모(스케줄별 채팅). 모임/정모 생성 시 자동 생성.
- **USER_CHAT_ROOM**: 채팅방 참여자 관리. 복합 PK (`chat_room_id` + `user_id`). `lastReadMessageId`(Long)로 읽음 위치 추적 — 안 읽은 메시지 수는 `message_id > lastReadMessageId`로 계산. Redis pub/sub은 실시간 전달만 담당하므로 별도 messageKey 불필요.
- **MESSAGE**: 채팅 메시지. `deletedAt`으로 소프트 딜리트.
- **삭제 연동**: Schedule 삭제 시 연결된 ChatRoom + Message를 애플리케이션 레이어에서 한 트랜잭션으로 하드 딜리트. `ChatRoom.schedule_id`는 FK 없이 논리적 참조만 유지 (cross-domain 경계 + 하드 딜리트 cascade 불필요).

### SEARCH (검색)
- **맞춤 모임**: 회원의 관심사 + 지역 기반 랜덤 추천. 무한 스크롤링.
- **키워드 검색**: 모임명, 해시태그 대상. 부분 문자열 매칭 및 동의어 처리.
- **필터 검색**: 관심사 필터, 지역 필터, 스케줄 일자 필터 조합.

### 삭제 정책
- **소프트 딜리트** (`BaseEntity.deletedAt`): Club, Schedule, Feed, FeedComment, Message
- **하드 딜리트**: Schedule 삭제 시 연결된 ChatRoom + Message (애플리케이션 레이어에서 한 트랜잭션으로 처리)
- **상태 기반**: User 탈퇴는 `UserStatus`로 표현 (소프트 딜리트와 별개)

---

## 우선순위 기준
- **A**: 핵심 기능 (피드 조회/좋아요, 스케줄 정산, 키워드 검색, PUSH 알림)
- **B**: 기본 기능 (회원/모임/피드/스케줄 CRUD, 채팅, 알림 목록)
- **C**: 부가 기능 (찜, 링크 공유, 신고, 권한 위임)
