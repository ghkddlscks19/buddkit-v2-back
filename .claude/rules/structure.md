# Package Structure

## 목표 레이아웃

도메인 중심(domain-centric) 구조를 따른다. 각 도메인은 controller, dto, entity, repository, service를 모두 자신의 패키지 안에 포함한다.

```
com.buddkitv2
├── BuddkitV2Application.java
├── domain/
│   ├── common/                    ← BaseEntity, Address, AddressRepository
│   ├── user/
│   │   ├── controller/            ← UserController, AuthController
│   │   ├── dto/
│   │   │   ├── request/           ← RegisterRequest, ProfileUpdateRequest
│   │   │   └── response/          ← RegisterResponse, ProfileResponse
│   │   ├── entity/                ← User, UserInterest, Gender, UserStatus, Interest, InterestCategory
│   │   ├── repository/            ← UserRepository, UserInterestRepository, InterestRepository
│   │   └── service/               ← UserService
│   ├── club/
│   │   ├── controller/
│   │   ├── dto/
│   │   │   ├── request/
│   │   │   └── response/
│   │   ├── entity/                ← Club, UserClub, ClubLike, UserClubRole
│   │   ├── repository/
│   │   └── service/
│   ├── feed/
│   │   ├── controller/
│   │   ├── dto/
│   │   │   ├── request/
│   │   │   └── response/
│   │   ├── entity/                ← Feed, FeedImage, FeedLike, FeedComment
│   │   ├── repository/
│   │   └── service/
│   ├── schedule/
│   │   ├── controller/
│   │   ├── dto/
│   │   │   ├── request/
│   │   │   └── response/
│   │   ├── entity/                ← Schedule, UserSchedule, ScheduleStatus, UserScheduleRole
│   │   ├── repository/
│   │   └── service/
│   ├── settlement/
│   │   ├── controller/
│   │   ├── dto/
│   │   │   ├── event/             ← Kafka/Spring 이벤트 payload
│   │   │   └── response/
│   │   ├── entity/                ← Settlement, UserSettlement, Transfer, SettlementStatus
│   │   ├── repository/
│   │   └── service/
│   ├── notification/
│   │   ├── controller/
│   │   ├── dto/
│   │   │   ├── event/
│   │   │   ├── request/
│   │   │   └── response/
│   │   ├── entity/                ← Notification, NotificationType, NotificationTypeEnum
│   │   ├── repository/
│   │   └── service/
│   ├── chat/
│   │   ├── controller/
│   │   ├── dto/
│   │   │   ├── request/
│   │   │   └── response/
│   │   ├── entity/                ← ChatRoom, Message, UserChatRoom, ChatRoomType, ChatRoomRole
│   │   ├── repository/
│   │   └── service/
│   ├── wallet/
│   │   ├── controller/
│   │   ├── dto/
│   │   │   └── response/
│   │   ├── entity/                ← Wallet, WalletTransaction, Payment, Transfer, WalletTransactionType
│   │   ├── repository/
│   │   └── service/
│   └── search/
│       ├── controller/
│       ├── dto/
│       │   ├── request/
│       │   └── response/
│       ├── document/              ← Elasticsearch document 클래스
│       ├── repository/
│       └── service/
└── global/
    ├── common/                    ← ApiResponse
    ├── config/                    ← Redis, Kafka, S3, JPA, Security, Swagger 등 설정 클래스
    │   └── kafka/                 ← Kafka 관련 설정만 별도 하위 패키지
    ├── exception/                 ← CustomException, ErrorCode, GlobalExceptionHandler
    ├── filter/                    ← JwtAuthenticationFilter
    └── security/                  ← JwtTokenProvider, OAuth2SuccessHandler, RefreshTokenService,
                                       TempTokenService, SecurityConfig, EntryPoint/Handler 클래스
```

---

## 규칙

### domain/

- 각 도메인 패키지(`user`, `club` 등)는 위 하위 구조를 완전히 갖춘다.
- `entity/` — JPA 엔티티 + 관련 enum. 한 도메인의 엔티티는 모두 여기에.
- `repository/` — `JpaRepository`, QueryDSL `RepositoryCustom`/`RepositoryImpl`도 여기에.
- `service/` — 도메인 로직 전체를 하나의 서비스 클래스에 담는다. 도메인당 서비스 클래스 하나.
- `controller/` — 도메인당 컨트롤러 클래스 하나. 메서드를 추가하는 방식으로 확장.
- `dto/request/` — 외부에서 들어오는 요청 DTO. `@Getter @Setter @NoArgsConstructor`.
- `dto/response/` — 외부로 내보내는 응답 DTO. `@Getter @AllArgsConstructor`.
- `dto/event/` — Spring Event 또는 Kafka 메시지 payload. settlement, notification 도메인에서 사용.

### global/

- 도메인에 귀속되지 않는 공통 인프라 코드만 둔다.
- `config/` — Bean 설정 클래스 전체 (`@Configuration`). Kafka 설정은 `config/kafka/` 하위.
- `exception/` — 예외 클래스 계층, `GlobalExceptionHandler`, `ErrorCode`.
- `security/` — JWT, OAuth2, Security 필터 체인 설정.
- `filter/` — Servlet 필터 (`JwtAuthenticationFilter` 등).

### 테스트 패키지

테스트 패키지는 main과 동일한 레이아웃을 따른다.

```
test/
└── com.buddkitv2
    ├── domain/
    │   ├── user/
    │   │   ├── controller/        ← UserControllerTest
    │   │   └── service/           ← UserServiceTest
    │   └── ...
    └── global/
        ├── config/                ← 테스트 전용 설정 (@TestConfiguration)
        └── security/              ← 테스트용 Security 설정
```

---

## 현재 → 목표 이전 매핑

기능 구현 시 파일 위치를 아래 매핑에 따라 이전한다. 이전은 도메인 단위로 일괄 처리한다.

| 현재 위치 | 목표 위치 |
|---|---|
| `api/user/` | `domain/user/controller/` + `domain/user/dto/` |
| `api/auth/` | `domain/user/controller/` + `domain/user/dto/` |
| `api/common/` | `global/common/` |
| `application/user/` | `domain/user/service/` |
| `domain/user/` (엔티티·레포) | `domain/user/entity/` + `domain/user/repository/` |
| `infra/security/` | `global/security/` |
| `infra/s3/` | `global/config/` |
| `infra/jpa/` | `global/config/` |
