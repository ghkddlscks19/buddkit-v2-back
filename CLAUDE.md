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
