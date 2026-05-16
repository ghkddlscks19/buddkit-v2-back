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

# 컴파일만 빠르게 확인
./gradlew compileJava compileTestJava
```

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

도메인 중심(domain-centric) 패키지 레이아웃을 따른다. 상세 구조는 @.claude/rules/structure.md 참조.

- `domain/{name}/` — 각 도메인의 controller, dto, entity, repository, service를 모두 포함
- `global/` — 도메인에 귀속되지 않는 공통 인프라 (config, exception, security, filter)

## References

@.claude/rules/conventions.md — DTO 규칙, 응답 포맷, 예외 처리, 인증 패턴
@.claude/rules/domain.md — 도메인 용어, 테이블 매핑, 우선순위
@.claude/rules/git.md — 커밋 타입, 메시지 형식, 커밋 전 체크리스트, 브랜치 전략
@.claude/rules/roadmap.md — 개발 단계, 현재 진행 단계, 각 단계 완료 기준
@.claude/rules/springboot4.md — Spring Boot 4.x 주의 사항 (Jackson 3.x 패키지, @MockitoBean, @WebMvcTest, @EnableJpaAuditing 분리)
@.claude/rules/structure.md — 패키지 레이아웃 (도메인 중심 구조, 하위 패키지 규칙)
