# Schedule (정모) API 설계

## 개요

모임(Club) 내 정기모임(Schedule) 생성·관리·참여·정산을 처리하는 API.

## 결정 사항

| 항목 | 결정 |
|---|---|
| URL 구조 | 전체 중첩 `/clubs/{clubId}/schedules/{scheduleId}/...` |
| 서비스 분리 | ScheduleService (CRUD·참여) + SettlementService (정산) |
| 페이지네이션 | cursor 기반 무한스크롤 (목록·참여자·정산 모두) |
| 즉시 정산 | 포인트 차감 → UserSettlement COMPLETED |
| 예약 정산 | PENDING_CONFIRMATION 저장 → 자정 배치 처리 |
| 잔액 부족 | 해당 UserSettlement만 REQUESTED 롤백, 나머지 계속 진행 |
| ChatRoom 삭제 | Chat 도메인 구현 시 처리 (이번 범위 제외) |

---

## API 엔드포인트

```
# 스케줄 CRUD
POST   /clubs/{clubId}/schedules                                              스케줄 생성       [LEADER]
PATCH  /clubs/{clubId}/schedules/{scheduleId}                                 스케줄 수정       [LEADER]
DELETE /clubs/{clubId}/schedules/{scheduleId}                                 스케줄 삭제       [LEADER] soft-delete
GET    /clubs/{clubId}/schedules                                               스케줄 목록       [모임 멤버] cursor
GET    /clubs/{clubId}/schedules/{scheduleId}                                  스케줄 상세       [모임 멤버]

# 참여
POST   /clubs/{clubId}/schedules/{scheduleId}/members                         참여              [모임 멤버, RECRUITING만]
DELETE /clubs/{clubId}/schedules/{scheduleId}/members/me                      참여 취소         [참여자, LEADER 제외]
GET    /clubs/{clubId}/schedules/{scheduleId}/members                         참여자 목록       [모임 멤버] cursor

# 정산
POST   /clubs/{clubId}/schedules/{scheduleId}/settlements                     정산 요청         [LEADER]
GET    /clubs/{clubId}/schedules/{scheduleId}/settlements                     정산 현황 조회    [모임 멤버] cursor
POST   /clubs/{clubId}/schedules/{scheduleId}/settlements/me                  즉시 정산         [참여자]
POST   /clubs/{clubId}/schedules/{scheduleId}/settlements/me/reserve          자정 예약 정산    [참여자]
PATCH  /clubs/{clubId}/schedules/{scheduleId}/settlements/{userSettlementId}  수동 완료         [LEADER]
```

---

## 상태 전이

### Schedule
```
RECRUITING → IN_PROGRESS → SETTLING → CLOSED
                            ↑              ↑
                     정산 요청 시    Settlement COMPLETED 시 자동
```

### Settlement
```
REQUESTED → IN_PROGRESS → COMPLETED
              ↑                ↑
     첫 멤버 정산 완료    전원 UserSettlement COMPLETED 시
```

### UserSettlement
```
REQUESTED → PENDING_CONFIRMATION → COMPLETED   (예약 경로)
REQUESTED ──────────────────────→ COMPLETED   (즉시 경로)

배치 실행 시:
  잔액 충분: PENDING_CONFIRMATION → COMPLETED
  잔액 부족: PENDING_CONFIRMATION → REQUESTED (롤백)
```

**불변 조건**: 하나라도 UserSettlement이 COMPLETED가 아니면 Settlement·Schedule은 완료되지 않는다.

---

## 서비스 책임

### ScheduleService

| 메서드 | 핵심 로직 |
|---|---|
| `createSchedule` | LEADER 확인 → Schedule.create() → UserSchedule(LEADER) 생성 |
| `updateSchedule` | LEADER 확인 → schedule.update() |
| `deleteSchedule` | LEADER 확인 → schedule.softDelete() |
| `getSchedules` | 모임 멤버 확인 → cursor 기반 목록 |
| `getSchedule` | 모임 멤버 확인 → 상세 + 나의 참여 여부 |
| `joinSchedule` | 모임 멤버 확인 → RECRUITING 상태 확인 → limit 확인 → UserSchedule(MEMBER) 생성 |
| `leaveSchedule` | 참여자 확인 → LEADER 탈퇴 불가 → UserSchedule 삭제 |
| `getScheduleMembers` | 모임 멤버 확인 → cursor 기반 참여자 목록 |

### SettlementService

| 메서드 | 핵심 로직 |
|---|---|
| `requestSettlement` | LEADER 확인 → 중복 요청 방지 → Schedule SETTLING 전환 → Settlement 생성 → 참여 MEMBER 수만큼 UserSettlement(REQUESTED) 생성 |
| `getSettlements` | 모임 멤버 확인 → UserSettlement 목록 반환 |
| `settleMyShare` | UserSettlement 조회 → REQUESTED 확인 → 잔액 확인 → WalletTransaction(TRANSFER) 생성 → 잔액 차감/증가 → COMPLETED 전환 → Settlement·Schedule 상태 갱신 |
| `reserveMyShare` | UserSettlement 조회 → REQUESTED 확인 → PENDING_CONFIRMATION 전환 |
| `completeManually` | LEADER 확인 → UserSettlement COMPLETED 전환 → Settlement·Schedule 상태 갱신 |

### SettlementBatchService

```
@Scheduled(cron = "0 0 0 * * *")
processReservedSettlements():
  1. PENDING_CONFIRMATION 인 UserSettlement 전체 조회
  2. 각각:
     - 잔액 충분 → settleMyShare 로직 재사용 → COMPLETED
     - 잔액 부족 → REQUESTED 롤백
  3. 각 처리 후 Settlement·Schedule 상태 갱신
```

---

## 파일 목록

### 신규 생성

```
domain/schedule/
├── controller/ScheduleController.java
├── dto/request/
│   ├── ScheduleCreateRequest.java
│   └── ScheduleUpdateRequest.java
├── dto/response/
│   ├── ScheduleResponse.java          ← 목록·상세 공용
│   └── ScheduleMemberResponse.java
├── repository/
│   ├── ScheduleRepository.java
│   └── UserScheduleRepository.java
└── service/ScheduleService.java

domain/settlement/
├── dto/response/
│   └── SettlementStatusResponse.java
├── repository/
│   └── SettlementRepository.java
└── service/
    ├── SettlementService.java
    └── SettlementBatchService.java

global/exception/
├── ScheduleNotFoundException.java
├── ScheduleAccessDeniedException.java
├── AlreadyJoinedScheduleException.java
├── NotJoinedScheduleException.java
├── ScheduleNotRecruitingException.java
├── ScheduleFullException.java
├── ScheduleAlreadySettlingException.java
├── SettlementNotFoundException.java
├── AlreadySettledException.java
└── InsufficientBalanceException.java
```

### 기존 파일 수정

```
domain/schedule/entity/Schedule.java
  → update(), softDelete(), changeStatus() 메서드 추가

domain/settlement/entity/Settlement.java
  → updateStatus(), complete() 메서드 추가

domain/settlement/entity/UserSettlement.java
  → complete(), reserve(), rollback() 메서드 추가

global/exception/GlobalExceptionHandler.java
  → 신규 예외 핸들러 추가
```

---

## 권한 체크 방식

모든 엔드포인트에서 `@AuthenticationPrincipal Long userId`로 인증.

- **모임 멤버 확인**: `UserClubRepository.findByClub_IdAndUser_Id(clubId, userId)` 존재 여부
- **LEADER 확인**: 위 결과의 `role == UserClubRole.LEADER`
- **clubId·scheduleId 정합성**: `schedule.getClub().getId().equals(clubId)`

---

## 예외 처리

| 상황 | 예외 | HTTP |
|---|---|---|
| 스케줄 없음 | ScheduleNotFoundException | 404 |
| 모임 미소속 / LEADER 아님 | ScheduleAccessDeniedException | 403 |
| 이미 참여 | AlreadyJoinedScheduleException | 409 |
| 미참여 | NotJoinedScheduleException | 404 |
| RECRUITING 아님 | ScheduleNotRecruitingException | 409 |
| 정원 초과 | ScheduleFullException | 409 |
| 이미 정산 진행 중 | ScheduleAlreadySettlingException | 409 |
| 정산 없음 | SettlementNotFoundException | 404 |
| 이미 정산 완료 | AlreadySettledException | 409 |
| 잔액 부족 | InsufficientBalanceException | 422 |
