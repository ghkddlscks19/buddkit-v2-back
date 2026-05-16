# Bounded Context: Buddkit V2

## Glossary

### 모임장 (Leader)
모임을 생성한 대표 관리자. `UserClubRole.LEADER`로 표현. 스케줄 생성/수정/삭제 및 정산 생성 권한을 독점 보유. `MEMBER`에게는 어떤 관리 권한도 없음.

> **Note:** 과거 설계에 존재하던 "운영진(MANAGER)" 개념은 제거됨. `UserClubRole`은 `LEADER / MEMBER` 두 값만 존재.

### 지역 (Address)
한국 행정구역 (city + district) 조합을 저장하는 lookup 테이블. 약 250행 고정. `User`와 `Club`은 `address_id` FK로 참조 — city/district 문자열을 직접 저장하지 않음. 유효하지 않은 조합 저장을 DB 레벨에서 방지.

### 관심사 (Interest)
취미 카테고리. `INTEREST` 테이블에 lookup 데이터로 고정 8개 행 존재. 단순 enum이 아닌 엔티티인 이유는 프론트엔드에 표시할 한국어 레이블 등 부가 속성이 필요하기 때문. `Club`은 단일 `Interest` FK 참조 (복수 관심사 불가), 회원은 `UserInterest`를 통해 1~5개 보유.

### 정산 (Settlement)
스케줄 참가비 전체 정산 건. `Settlement.user`는 정산을 생성하고 참가비를 수령하는 모임장(LEADER)을 가리킴. 개별 참여자 단위 처리는 `UserSettlement`에서 관리.

`UserScheduleRole.LEADER`는 스케줄을 생성한 모임장이자 정산 수령자. 항상 `UserClubRole.LEADER`와 동일 인물.

### 채팅 읽음 추적
`UserChatRoom.lastReadMessageId` (Long) — 해당 사용자가 마지막으로 읽은 `Message.id`. 안 읽은 메시지 수는 `message_id > lastReadMessageId`로 계산. Redis pub/sub은 실시간 전달만 담당하므로 별도 messageKey 불필요.

### 정산 완료 시각
- `UserSettlement.completedTime`: 개별 참여자의 정산이 완료된 시각. 참여자별로 각각 찍힘.
- `Settlement.completedTime`: 전체 정산 건이 완료된 시각. 모든 참여자의 정산이 끝났을 때 찍힘.

### Payment
토스페이먼츠 결제 건. `WalletTransaction`을 FK로 참조하며, 항상 `CHARGE` 타입 거래에만 연결됨. `TRANSFER` 거래에 Payment가 연결되는 경우는 없으며, 이 제약은 서비스 레이어에서 보장. 모든 정산은 앱 내 포인트(Wallet)로만 처리 — `UserSettlementType`은 `POINT` 단일.

### 알림 (Notification)
- `SETTLEMENT`, `SCHEDULE`, `LIKE`, `COMMENT`: DB에 `Notification` 행 저장 + FCM 전송. 목록 조회 가능.
- `CHAT`: FCM 전송만. `Notification` 행을 저장하지 않음. 서비스 레이어에서 insert를 생략하는 방식으로 보장.

### 금액 타입
모든 금액 필드는 `Long`으로 통일. 원화는 소수점 없는 정수이며 `Integer` 최대값(약 21억)을 초과하는 정산 합계를 안전하게 표현하기 위해 `Long` 사용. `BigDecimal`은 사용하지 않음.

### Club.memberCount
`UserClub` 행 수와 항상 일치해야 하는 비정규화 컬럼. 모임 목록 조회 시 COUNT 쿼리를 피하기 위한 성능 목적. 가입 시 +1, 탈퇴(소프트 딜리트) 시 -1을 서비스 레이어에서 동일 트랜잭션으로 처리.

### 삭제 정책
- **소프트 딜리트** (`BaseEntity.deletedAt`): Club, Schedule, Feed, FeedComment, Message
- **하드 딜리트**: Schedule 삭제 시 연결된 ChatRoom + Message (애플리케이션 레이어에서 한 트랜잭션으로 처리)
- **상태 기반**: User 탈퇴는 `UserStatus`로 표현 (소프트 딜리트와 별개)
- `ChatRoom.schedule_id`는 FK 없이 논리적 참조만 유지 (cross-domain 경계 + 하드 딜리트 cascade 불필요)

### 지갑 거래 (WalletTransaction)
`wallet` = 거래 주체 지갑, `targetWallet` = 수신 지갑. 항상 non-null.
- `TRANSFER`: `wallet` → `targetWallet` (타인 지갑으로 송금)
- `CHARGE`: `wallet` == `targetWallet` (자기 자신 지갑 — 외부 결제로 충전)
