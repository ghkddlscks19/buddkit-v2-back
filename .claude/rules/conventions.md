# Coding Conventions

## DTO 작성 규칙

`record` 대신 일반 클래스로 DTO를 작성한다. 서비스 내부 결과 객체도 동일하게 적용한다.

- `dto/request/` — `@Getter @Setter @NoArgsConstructor`
- `dto/response/` — `@Getter @AllArgsConstructor`

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

## 공통 응답 포맷

모든 API 응답은 `ApiResponse<T>`로 감싼다.

```json
{ "success": true,  "data": { ... }, "message": null }
{ "success": false, "data": null,    "message": "에러 메시지" }
```

컨트롤러에서는 `ApiResponse.ok(data)` / `ApiResponse.fail(message)` 를 사용한다.

## 예외 처리 규칙

`IllegalArgumentException`, `IllegalStateException` 등 Java 기본 예외를 서비스 로직에서 직접 던지지 않는다. 반드시 `global/exception/`에 커스텀 예외를 만들어 사용한다.

```java
// 잘못된 예
throw new IllegalArgumentException("유효하지 않은 지역입니다.");

// 올바른 예
throw new InvalidAddressException();
```

커스텀 예외는 `RuntimeException`을 상속하며, `GlobalExceptionHandler`에 해당 예외 핸들러와 HTTP 상태 코드를 명시한다.

## 인증 패턴

JWT 필터가 `SecurityContext`에 `Long userId`를 principal로 저장한다. 인증이 필요한 엔드포인트는 `@AuthenticationPrincipal Long userId`로 추출한다.

```java
@GetMapping("/me")
public ApiResponse<MyPageResponse> getMyPage(@AuthenticationPrincipal Long userId) { ... }
```

인증 불필요 경로 (`SecurityConfig.permitAll`):
`/actuator/health`, `/oauth2/**`, `/login/**`, `/auth/refresh`, `/swagger-ui/**`, `/v3/api-docs/**`, `/users/register`
