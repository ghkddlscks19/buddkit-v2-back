# User Domain — 나머지 기능 설계

**날짜:** 2026-05-16  
**범위:** 유저 도메인 나머지 기능 (회원가입/로그인 이후)  
**브랜치:** feat/user

---

## 결정 사항 요약

| 항목 | 결정 |
|---|---|
| 아키텍처 | UserController/UserService 확장 (WalletController 분리 없음) |
| 관심사 수정 방식 | 전체 교체 (기존 삭제 후 재삽입) |
| 결제 수단 | 카드 결제만 (Webhook 없음, confirm 방식) |
| 페이지네이션 | cursor 기반 무한 스크롤 (`lastId` + `size`) |
| 거래/정산 내역 | 별도 엔드포인트 2개 |

---

## API 엔드포인트

| Method | Path | 설명 | Controller |
|--------|------|------|------------|
| `GET` | `/users/me` | 상세 프로필 조회 | UserController (기존 확장) |
| `PATCH` | `/users/me` | 프로필 수정 | UserController |
| `DELETE` | `/users/me` | 회원탈퇴 | UserController |
| `POST` | `/auth/logout` | 로그아웃 | AuthController |
| `GET` | `/users/me/transactions` | 거래내역 목록 | UserController |
| `GET` | `/users/me/settlements` | 정산내역 목록 | UserController |
| `POST` | `/users/me/wallet/charge` | 포인트 충전 (카드) | UserController |
| `GET` | `/users/me/clubs` | 내 모임 목록 | UserController |
| `GET` | `/users/me/liked-clubs` | 관심 모임 목록 | UserController |
| `PUT` | `/users/me/fcm-token` | FCM 토큰 등록/갱신 | UserController |
| `DELETE` | `/users/me/fcm-token` | FCM 토큰 삭제 | UserController |

---

## 섹션별 설계

### 1. 프로필 조회 — GET /users/me

기존 `getMyPage` 유지. `MyPageResponse`에 `gender` 필드 추가.

```
MyPageResponse: id, nickname, profileImageUrl, city, district, birth, gender,
                interests (List<InterestCategory>), balance
```

### 2. 프로필 수정 — PATCH /users/me

`multipart/form-data` 수신 (기존 register와 동일한 패턴).

```
Request:
  - data (JSON part): nickname, city, district, interests (List<InterestCategory>)
  - profileImage (file, optional)

처리 순서:
1. city + district → Address 조회 및 유효성 검증
2. profileImage 있으면 S3 업로드 후 URL 교체, 없으면 기존 URL 유지
3. User.updateProfile(nickname, address, profileImageUrl) 호출
4. UserInterest 전체 삭제 후 새 목록으로 재삽입
5. 업데이트된 MyPageResponse 반환
```

`User` 엔티티에 `updateProfile(String nickname, Address address, String profileImageUrl)` 메서드 추가.

### 3. 로그아웃 — POST /auth/logout

```
처리:
1. @AuthenticationPrincipal Long userId
2. RefreshTokenService.delete(userId) → Redis 삭제
3. 204 No Content

비고: AccessToken 블랙리스트 없음 (짧은 만료 시간 가정)
```

### 4. 회원탈퇴 — DELETE /users/me

```
처리 순서:
1. User.withdraw() → UserStatus.WITHDRAWN
2. User.updateFcmToken(null) → FCM 토큰 초기화
3. RefreshTokenService.delete(userId) → Redis 삭제
4. 204 No Content

데이터 보존: UserInterest, Wallet, WalletTransaction 유지 (감사 목적)
탈퇴 후 재가입: 현재 스펙 범위 밖
탈퇴 후 재로그인 시도: WithdrawnUserException (401)
```

### 5. 포인트 충전 — POST /users/me/wallet/charge

```
Request: { paymentKey, orderId, amount (Long) }

처리 순서:
1. 토스 API POST /v1/payments/confirm 호출
   - Authorization: Basic {Base64(secretKey:)}
   - Body: { paymentKey, orderId, amount }
2. 실패 시 TossPaymentException → 400
3. 성공 시:
   a. WalletTransaction.create(wallet, wallet, CHARGE, amount) 저장
   b. Payment.create(paymentKey, orderId, amount, walletTransaction) 저장
   c. wallet.charge(amount) → balance 증가
4. Response: { balance }

Wallet 엔티티에 charge(Long amount) 메서드 추가.
```

### 6. 거래내역 목록 — GET /users/me/transactions

```
Query params: lastId (optional), size (default 20)

조회 대상: 내 wallet_id 기준 WalletTransaction
정렬: id DESC (최신순)
커서: lastId 없으면 최신부터, 있으면 해당 id 미만

Response: [
  { id, type (CHARGE/TRANSFER), balance, createdAt }
]
```

### 7. 정산내역 목록 — GET /users/me/settlements

```
Query params: lastId (optional), size (default 20)

조회 대상: 내 user_id 기준 UserSettlement
정렬: id DESC (최신순)

Response: [
  { id, status, type, amount (Settlement.cost), completedTime, createdAt }
]
```

### 8. 내 모임 목록 — GET /users/me/clubs

```
Query params: lastId (optional), size (default 20)

조회 대상: UserClub (내 userId 기준)
커서: lastId = UserClub.id (가입순 역순)

Response: [
  { clubId, name, clubImage,
    interest: { category, name },
    city, district, memberCount, myRole (LEADER/MEMBER) }
]

비고: UserService에서 UserClubRepository, ClubLikeRepository 직접 참조
      Club 서비스 불필요 (단순 조회)
```

### 9. 관심 모임 목록 — GET /users/me/liked-clubs

```
Query params: lastId (optional), size (default 20)

조회 대상: ClubLike (내 userId 기준)
커서: lastId = ClubLike.id (찜한 순 역순)

Response: [
  { clubId, name, clubImage,
    interest: { category, name },
    city, district, memberCount }
]
```

### 10. FCM 토큰 등록/갱신 — PUT /users/me/fcm-token

```
Request: { token }
처리: User.updateFcmToken(token)
Response: 200 OK
```

### 11. FCM 토큰 삭제 — DELETE /users/me/fcm-token

```
처리: User.updateFcmToken(null)
Response: 204 No Content
```

---

## 신규 파일 목록

### 엔티티 메서드 추가

| 파일 | 추가 메서드 |
|---|---|
| `domain/user/entity/User.java` | `updateProfile(nickname, address, profileImageUrl)` |
| `domain/wallet/entity/Wallet.java` | `charge(Long amount)` |

### 신규 리포지토리

| 파일 | 위치 |
|---|---|
| `WalletTransactionRepository` | `domain/wallet/repository/` |
| `PaymentRepository` | `domain/wallet/repository/` |
| `UserSettlementRepository` | `domain/settlement/repository/` |
| `UserClubRepository` | `domain/club/repository/` |
| `ClubLikeRepository` | `domain/club/repository/` |

### 신규 DTO

| 파일 | 위치 |
|---|---|
| `ProfileUpdateRequest` | `domain/user/dto/request/` |
| `FcmTokenRequest` | `domain/user/dto/request/` |
| `ChargeRequest` | `domain/user/dto/request/` |
| `ChargeResponse` | `domain/user/dto/response/` |
| `TransactionResponse` | `domain/user/dto/response/` |
| `SettlementHistoryResponse` | `domain/user/dto/response/` |
| `MyClubResponse` | `domain/user/dto/response/` |
| `LikedClubResponse` | `domain/user/dto/response/` |

### 신규 예외

| 클래스 | HTTP | 발생 조건 |
|---|---|---|
| `TossPaymentException` | 400 | 토스 API confirm 실패 |
| `WithdrawnUserException` | 401 | 탈퇴 회원 재로그인 시도 |

---

## 테스트 전략

- `UserServiceTest` — 프로필 수정, 충전, 회원탈퇴 로직 (실제 DB/Redis 환경)
- `UserControllerTest` — 전체 엔드포인트 요청/응답 포맷
- `AuthControllerTest` — 로그아웃 흐름

모든 테스트는 실제 PostgreSQL + Redis 환경에서만 실행 (인메모리/Mock 대체 없음).
