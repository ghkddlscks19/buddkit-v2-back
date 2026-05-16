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

도메인 중심(domain-centric) 패키지 레이아웃을 따른다. 상세 구조와 이전 매핑은 @.claude/rules/structure.md 참조.

- `domain/{name}/` — 각 도메인의 controller, dto, entity, repository, service를 모두 포함
- `global/` — 도메인에 귀속되지 않는 공통 인프라 (config, exception, security, filter)

External dependencies (PostgreSQL, Redis, Kafka, OAuth2 provider) must be configured in `application.yml` before the application can start. Add environment-specific values via `application-{profile}.yml` or environment variables; never commit credentials.

## 예외 처리 규칙

`IllegalArgumentException`, `IllegalStateException` 등 Java 기본 예외를 서비스 로직에서 직접 던지지 않는다. 반드시 `global/exception/`에 커스텀 예외를 만들어 사용한다.

```java
// 잘못된 예
throw new IllegalArgumentException("유효하지 않은 지역입니다.");
throw new IllegalStateException("이미 가입된 회원입니다.");

// 올바른 예
throw new InvalidAddressException();
throw new AlreadyRegisteredException();
```

커스텀 예외는 `RuntimeException`을 상속하며, `GlobalExceptionHandler`에 해당 예외 핸들러를 추가해 HTTP 상태 코드와 메시지를 명시적으로 관리한다. 기본 예외를 남기면 전역 핸들러에서 상태 코드를 제어할 수 없고, 어떤 오류인지 코드 검색도 불가능하다.

## 공통 응답 포맷

모든 API 응답은 `ApiResponse<T>`로 감싼다.

```json
{ "success": true,  "data": { ... }, "message": null }
{ "success": false, "data": null,    "message": "에러 메시지" }
```

컨트롤러에서는 `ApiResponse.ok(data)` / `ApiResponse.fail(message)` 를 사용한다.

## 인증 필요 엔드포인트

JWT 필터가 `SecurityContext`에 `Long userId`를 principal로 저장한다. 인증이 필요한 엔드포인트는 `@AuthenticationPrincipal Long userId`로 추출한다.

```java
@GetMapping("/me")
public ApiResponse<MyPageResponse> getMyPage(@AuthenticationPrincipal Long userId) { ... }
```

인증 불필요 경로 (`SecurityConfig.permitAll`):
`/actuator/health`, `/oauth2/**`, `/login/**`, `/auth/refresh`, `/swagger-ui/**`, `/v3/api-docs/**`, `/users/register`

## References

@.claude/rules/domain.md — 도메인 용어, 테이블 매핑, 우선순위
@.claude/rules/git.md — 커밋 타입, 메시지 형식, 커밋 전 체크리스트
@.claude/rules/roadmap.md — 개발 단계, 현재 진행 단계, 각 단계 완료 기준
@.claude/rules/springboot4.md — Spring Boot 4.x 주의 사항 (Jackson 3.x 패키지, @MockitoBean, @WebMvcTest, @EnableJpaAuditing 분리)
@.claude/rules/structure.md — 패키지 레이아웃 (도메인 중심 구조, 하위 패키지 규칙, 현재→목표 이전 매핑)
