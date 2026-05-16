# CLAUDE.md

## Commands

```bash
# Build
./gradlew build

# Run application
./gradlew bootRun

# Run all tests
./gradlew test

# Run a single test class
./gradlew test --tests "com.buddkitv2.SomeTest"

# Run a single test method
./gradlew test --tests "com.buddkitv2.SomeTest.methodName"

# Clean build
./gradlew clean build
```

`src/main/resources/application*.yml`은 gitignore 대상 — 절대 커밋하지 않는다.

## DTO 작성 규칙

`record` 대신 일반 클래스로 DTO를 작성한다. Lombok `@Getter`와 `@AllArgsConstructor`(또는 `@NoArgsConstructor`)를 사용한다. 서비스 내부 결과 객체도 동일하게 일반 클래스로 작성한다.

```java
// 잘못된 예
public record RegisterResponse(String accessToken, Long point) {}

// 올바른 예
@Getter
@AllArgsConstructor
public class RegisterResponse {
    private String accessToken;
    private Long point;
}
```

## 서비스/컨트롤러 구조

도메인별 서비스와 컨트롤러는 하나로 유지한다. 기능이 늘어날 때 새 클래스를 만들지 말고 기존 클래스에 메서드를 추가한다.

- `UserService` — 유저 도메인 로직 전체
- `UserController` — 유저 관련 엔드포인트 전체
- 다른 도메인도 동일 원칙

## 테스트 전제 조건

모든 테스트는 실제 DB(PostgreSQL), Redis가 실행 중인 환경에서만 실행한다. 인메모리 DB나 Mock으로 대체하지 않는다. `@SpringBootTest` 기반 통합 테스트는 DB 연결 없이 실행하면 Hibernate 스키마 검증 실패로 반드시 터진다.

## Stack

- **Java 21**, Spring Boot 4.0.6
- **Database**: PostgreSQL via Spring Data JPA
- **Cache**: Redis
- **Messaging**: Kafka
- **Auth**: Spring Security + OAuth2 Client
- **Transport**: Spring MVC (REST) + WebSocket
- **Utilities**: Lombok, Bean Validation, Actuator

## Architecture

Entry point: `com.buddkitv2.BuddkitV2Application`

초기 scaffolding 단계 — `@SpringBootApplication` 진입점만 존재. 패키지 레이아웃:

- `domain/` — JPA entities and repository interfaces
- `application/` — service classes and use-case logic
- `api/` — REST controllers and WebSocket handlers
- `infra/` — Kafka producers/consumers, Redis config, OAuth2 customizations, security config

External dependencies (PostgreSQL, Redis, Kafka, OAuth2 provider) must be configured in `application.yml` before the application can start. Add environment-specific values via `application-{profile}.yml` or environment variables; never commit credentials.

## References

@.claude/rules/domain.md — 도메인 용어, 테이블 매핑, 우선순위
@.claude/rules/git.md — 커밋 타입, 메시지 형식, 커밋 전 체크리스트
@.claude/rules/roadmap.md — 개발 단계, 현재 진행 단계, 각 단계 완료 기준
@.claude/rules/springboot4.md — Spring Boot 4.x 주의 사항 (Jackson 3.x 패키지, @MockitoBean, @WebMvcTest, @EnableJpaAuditing 분리)
