# Spring Boot 4.x 주의 사항

Spring Boot 4.x는 Spring Framework 7 + Jackson 3.x 기반으로, 3.x와 패키지 경로 및 어노테이션이 다르다.
코드 작성 전 아래 항목을 반드시 확인한다.

---

## Jackson 3.x 패키지

Spring Boot 4.x는 Jackson 3.x를 사용한다. 패키지 루트가 바뀌었다.

| 3.x (구) | 4.x (신) |
|---|---|
| `com.fasterxml.jackson.databind.ObjectMapper` | `tools.jackson.databind.ObjectMapper` |
| `com.fasterxml.jackson.databind.*` | `tools.jackson.databind.*` |

`ObjectMapper`를 직접 생성하지 말고 Spring 빈으로 주입받는다. Spring Boot 자동 구성 빈을 사용해야 전역 Jackson 설정(JavaTimeModule 등)이 적용된다.

```java
// 잘못된 예
private final ObjectMapper objectMapper = new ObjectMapper();

// 올바른 예
@RequiredArgsConstructor
public class MyComponent {
    private final ObjectMapper objectMapper; // 빈 주입
}
```

---

## 테스트 어노테이션 변경

| 3.x (구) | 4.x (신) |
|---|---|
| `@MockBean` | `@MockitoBean` |
| `org.springframework.boot.test.mock.mockito.MockBean` | `org.springframework.test.context.bean.override.mockito.MockitoBean` |

```java
// 잘못된 예 (Spring Boot 3.x)
@MockBean
private UserService userService;

// 올바른 예 (Spring Boot 4.x)
@MockitoBean
private UserService userService;
```

---

## @WebMvcTest 패키지 변경

```java
// 잘못된 예 (Spring Boot 3.x)
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;

// 올바른 예 (Spring Boot 4.x)
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
```

---

## @WebMvcTest + Spring Security 제외 설정

Spring Security + OAuth2 Client를 함께 사용할 때 `@WebMvcTest`에서 Security 자동 구성을 제외하지 않으면 OAuth2 설정 누락 에러가 난다. 아래 4개를 모두 제외한다.

```java
@WebMvcTest(value = MyController.class, excludeAutoConfiguration = {
        org.springframework.boot.security.autoconfigure.servlet.SecurityAutoConfiguration.class,
        org.springframework.boot.security.autoconfigure.servlet.ServletWebSecurityAutoConfiguration.class,
        org.springframework.boot.security.autoconfigure.servlet.SecurityFilterAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration.class
})
```

---

## @EnableJpaAuditing 분리

`@SpringBootApplication` 클래스에 `@EnableJpaAuditing`을 두면 `@WebMvcTest` 실행 시 "JPA metamodel must not be empty" 오류가 발생한다. 별도 설정 클래스로 분리한다.

```java
// 잘못된 예 — BuddkitV2Application.java
@EnableJpaAuditing
@SpringBootApplication
public class BuddkitV2Application { ... }

// 올바른 예 — infra/jpa/JpaAuditingConfig.java
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {}
```
