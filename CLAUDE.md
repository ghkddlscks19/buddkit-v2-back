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

도메인 중심(domain-centric) 패키지 레이아웃을 따른다. 상세 구조와 이전 매핑은 @.claude/rules/structure.md 참조.

- `domain/{name}/` — 각 도메인의 controller, dto, entity, repository, service를 모두 포함
- `global/` — 도메인에 귀속되지 않는 공통 인프라 (config, exception, security, filter)

External dependencies (PostgreSQL, Redis, Kafka, OAuth2 provider) must be configured in `application.yml` before the application can start. Add environment-specific values via `application-{profile}.yml` or environment variables; never commit credentials.

## 패키지 이전 시 주의 사항

`git mv`로 파일을 이동할 때 반드시 아래를 확인한다.

1. **package 선언 업데이트** — 파일 맨 위 `package` 선언이 새 디렉터리 경로와 일치해야 한다.
2. **같은 패키지 참조에 import 추가** — 기존에 같은 패키지에 있어 import 없이 쓰던 클래스들이, 이동 후 다른 패키지로 분리되면 반드시 명시적 import가 필요하다.
   - 예: `api/auth/` 에 함께 있던 `RefreshRequest`, `TokenResponse`가 `dto/request/`, `dto/response/`로 분리 → `AuthController`에 import 추가 필수
3. **와일드카드 import 교체** — `import com.buddkitv2.domain.user.*` 같은 와일드카드는 패키지 분리 후 동작하지 않는다. 필요한 클래스를 개별 import로 교체한다.
4. **이전 후 즉시 컴파일 확인** — `./gradlew compileJava compileTestJava`로 검증한다. 빌드가 통과해야 다음 단계로 넘어간다.

## References

@.claude/rules/domain.md — 도메인 용어, 테이블 매핑, 우선순위
@.claude/rules/git.md — 커밋 타입, 메시지 형식, 커밋 전 체크리스트
@.claude/rules/roadmap.md — 개발 단계, 현재 진행 단계, 각 단계 완료 기준
@.claude/rules/springboot4.md — Spring Boot 4.x 주의 사항 (Jackson 3.x 패키지, @MockitoBean, @WebMvcTest, @EnableJpaAuditing 분리)
@.claude/rules/structure.md — 패키지 레이아웃 (도메인 중심 구조, 하위 패키지 규칙, 현재→목표 이전 매핑)
