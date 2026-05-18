# Kakao OAuth2 + JWT Authentication Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 카카오 소셜 로그인 후 AccessToken(JWT, stateless, 15분) + RefreshToken(Redis 저장, 7일)을 발급하고, 인증/인가 실패 시 401/403을 반환한다.

**Architecture:** Spring Security OAuth2 로그인 성공 핸들러에서 User를 DB에 findOrCreate하고, AT/RT를 발급한다. AT는 stateless JWT이고, RT는 SHA-256 해시로 Redis에 `RT:{userId}` 키로 저장한다. `/auth/refresh`로 AT를 재발급할 수 있다.

**Tech Stack:** Spring Security 6, Spring OAuth2 Client, JJWT 0.12.6, Spring Data Redis, JPA

---

## File Structure

**새로 생성:**
- `src/main/java/com/buddkitv2/domain/user/UserRepository.java` — JPA repository
- `src/main/java/com/buddkitv2/application/user/UserService.java` — findOrCreate 로직
- `src/main/java/com/buddkitv2/infra/security/RefreshTokenService.java` — Redis RT 저장/검증/삭제
- `src/main/java/com/buddkitv2/infra/security/JwtAuthenticationEntryPoint.java` — 401 응답
- `src/main/java/com/buddkitv2/infra/security/JwtAccessDeniedHandler.java` — 403 응답
- `src/main/java/com/buddkitv2/api/auth/TokenResponse.java` — AT+RT 응답 DTO
- `src/main/java/com/buddkitv2/api/auth/RefreshRequest.java` — RT 요청 DTO
- `src/main/java/com/buddkitv2/api/auth/AuthController.java` — `/auth/refresh` 엔드포인트
- `src/test/java/com/buddkitv2/infra/security/JwtTokenProviderTest.java`
- `src/test/java/com/buddkitv2/application/user/UserServiceTest.java`
- `src/test/java/com/buddkitv2/api/auth/AuthControllerTest.java`

**수정:**
- `src/main/java/com/buddkitv2/infra/security/JwtTokenProvider.java` — AT/RT 분리, type claim 추가, RT 만료 설정
- `src/main/java/com/buddkitv2/infra/security/JwtAuthenticationFilter.java` — access token type 검증 추가
- `src/main/java/com/buddkitv2/infra/security/OAuth2SuccessHandler.java` — UserService 연동, AT+RT 발급
- `src/main/java/com/buddkitv2/infra/security/SecurityConfig.java` — 401/403 핸들러 등록, `/auth/refresh` permitAll

---

## Task 1: 401/403 Security Error Handlers + SecurityConfig

**Files:**
- Create: `src/main/java/com/buddkitv2/infra/security/JwtAuthenticationEntryPoint.java`
- Create: `src/main/java/com/buddkitv2/infra/security/JwtAccessDeniedHandler.java`
- Modify: `src/main/java/com/buddkitv2/infra/security/SecurityConfig.java`

- [ ] **Step 1: JwtAuthenticationEntryPoint 생성** (미인증 → 401)

```java
package com.buddkitv2.infra.security;

import com.buddkitv2.api.common.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8");
        response.getWriter().write(
                objectMapper.writeValueAsString(ApiResponse.fail("인증이 필요합니다."))
        );
    }
}
```

- [ ] **Step 2: JwtAccessDeniedHandler 생성** (인증됐지만 권한 없음 → 403)

```java
package com.buddkitv2.infra.security;

import com.buddkitv2.api.common.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8");
        response.getWriter().write(
                objectMapper.writeValueAsString(ApiResponse.fail("접근 권한이 없습니다."))
        );
    }
}
```

- [ ] **Step 3: SecurityConfig 수정** — 핸들러 등록, `/auth/refresh` permitAll

```java
package com.buddkitv2.infra.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers("/oauth2/**", "/login/**").permitAll()
                        .requestMatchers("/auth/refresh").permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth2 -> oauth2
                        .successHandler(oAuth2SuccessHandler)
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                        .accessDeniedHandler(jwtAccessDeniedHandler)
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
```

- [ ] **Step 4: 빌드 확인**

```bash
./gradlew build -x test
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: 커밋**

```bash
git add src/main/java/com/buddkitv2/infra/security/JwtAuthenticationEntryPoint.java \
        src/main/java/com/buddkitv2/infra/security/JwtAccessDeniedHandler.java \
        src/main/java/com/buddkitv2/infra/security/SecurityConfig.java
git commit -m "feat(security): 401/403 에러 핸들러 등록 및 /auth/refresh permitAll 추가"
```

---

## Task 2: UserRepository + UserService (findOrCreate)

**Files:**
- Create: `src/main/java/com/buddkitv2/domain/user/UserRepository.java`
- Create: `src/main/java/com/buddkitv2/application/user/UserService.java`
- Create: `src/test/java/com/buddkitv2/application/user/UserServiceTest.java`

- [ ] **Step 1: 실패하는 테스트 작성**

```java
package com.buddkitv2.application.user;

import com.buddkitv2.domain.user.User;
import com.buddkitv2.domain.user.UserRepository;
import com.buddkitv2.domain.user.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private final Long kakaoId = 12345L;
    private final String nickname = "테스트유저";
    private final String profileImageUrl = "https://example.com/img.jpg";

    @Test
    void 기존_회원이면_DB_조회_결과를_반환한다() {
        User existing = User.create(kakaoId, nickname, profileImageUrl);
        when(userRepository.findByKakaoId(kakaoId)).thenReturn(Optional.of(existing));

        User result = userService.findOrCreate(kakaoId, nickname, profileImageUrl);

        assertThat(result.getKakaoId()).isEqualTo(kakaoId);
        verify(userRepository, never()).save(any());
    }

    @Test
    void 신규_회원이면_저장_후_반환한다() {
        User saved = User.create(kakaoId, nickname, profileImageUrl);
        when(userRepository.findByKakaoId(kakaoId)).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(saved);

        User result = userService.findOrCreate(kakaoId, nickname, profileImageUrl);

        assertThat(result.getKakaoId()).isEqualTo(kakaoId);
        verify(userRepository).save(any(User.class));
    }
}
```

- [ ] **Step 2: 테스트 실행 — 실패 확인**

```bash
./gradlew test --tests "com.buddkitv2.application.user.UserServiceTest"
```
Expected: FAIL — `UserService`, `UserRepository` 없음 컴파일 에러

- [ ] **Step 3: UserRepository 생성**

```java
package com.buddkitv2.domain.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByKakaoId(Long kakaoId);
}
```

- [ ] **Step 4: UserService 생성**

```java
package com.buddkitv2.application.user;

import com.buddkitv2.domain.user.User;
import com.buddkitv2.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional
    public User findOrCreate(Long kakaoId, String nickname, String profileImageUrl) {
        return userRepository.findByKakaoId(kakaoId)
                .orElseGet(() -> userRepository.save(
                        User.create(kakaoId, nickname, profileImageUrl)
                ));
    }
}
```

- [ ] **Step 5: 테스트 통과 확인**

```bash
./gradlew test --tests "com.buddkitv2.application.user.UserServiceTest"
```
Expected: PASS (2 tests)

- [ ] **Step 6: 커밋**

```bash
git add src/main/java/com/buddkitv2/domain/user/UserRepository.java \
        src/main/java/com/buddkitv2/application/user/UserService.java \
        src/test/java/com/buddkitv2/application/user/UserServiceTest.java
git commit -m "feat(user): UserRepository 및 findOrCreate 서비스 구현"
```

---

## Task 3: JwtTokenProvider — AT/RT 분리 (type claim, 만료 분리)

**Files:**
- Modify: `src/main/java/com/buddkitv2/infra/security/JwtTokenProvider.java`
- Modify: `src/main/java/com/buddkitv2/infra/security/JwtAuthenticationFilter.java`
- Create: `src/test/java/com/buddkitv2/infra/security/JwtTokenProviderTest.java`

**`application.yml`에 추가해야 할 설정** (gitignored이므로 직접 수정):
```yaml
jwt:
  secret: "your-secret-key-here-must-be-at-least-32-characters-long"
  access-expiration-ms: 900000      # 15분
  refresh-expiration-ms: 604800000  # 7일
```
> 기존 `jwt.expiration-ms` → `jwt.access-expiration-ms` 로 변경

- [ ] **Step 1: 실패하는 테스트 작성**

```java
package com.buddkitv2.infra.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    private JwtTokenProvider provider;

    @BeforeEach
    void setUp() {
        // 32자 이상의 시크릿 (256-bit 이상)
        provider = new JwtTokenProvider(
                "test-secret-key-must-be-at-least-32-chars!!",
                900_000L,        // AT: 15분
                604_800_000L     // RT: 7일
        );
    }

    @Test
    void AccessToken_생성_및_검증() {
        String at = provider.generateAccessToken(1L);

        assertThat(provider.validateToken(at)).isTrue();
        assertThat(provider.isAccessToken(at)).isTrue();
        assertThat(provider.getUserId(at)).isEqualTo(1L);
    }

    @Test
    void RefreshToken_생성_및_검증() {
        String rt = provider.generateRefreshToken(1L);

        assertThat(provider.validateToken(rt)).isTrue();
        assertThat(provider.isAccessToken(rt)).isFalse();
        assertThat(provider.getUserId(rt)).isEqualTo(1L);
    }

    @Test
    void AT를_RT로_사용할_수_없다() {
        String at = provider.generateAccessToken(1L);
        assertThat(provider.isAccessToken(at)).isTrue();

        String rt = provider.generateRefreshToken(1L);
        assertThat(provider.isAccessToken(rt)).isFalse();
    }

    @Test
    void 잘못된_토큰은_검증_실패() {
        assertThat(provider.validateToken("invalid.token.value")).isFalse();
    }
}
```

- [ ] **Step 2: 테스트 실행 — 실패 확인**

```bash
./gradlew test --tests "com.buddkitv2.infra.security.JwtTokenProviderTest"
```
Expected: FAIL — `generateAccessToken`, `generateRefreshToken`, `isAccessToken` 없음

- [ ] **Step 3: JwtTokenProvider 수정**

```java
package com.buddkitv2.infra.security;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtTokenProvider {

    private final SecretKey secretKey;
    private final long accessExpirationMs;
    private final long refreshExpirationMs;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-expiration-ms}") long accessExpirationMs,
            @Value("${jwt.refresh-expiration-ms}") long refreshExpirationMs
    ) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessExpirationMs = accessExpirationMs;
        this.refreshExpirationMs = refreshExpirationMs;
    }

    public String generateAccessToken(Long userId) {
        return buildToken(userId, "access", accessExpirationMs);
    }

    public String generateRefreshToken(Long userId) {
        return buildToken(userId, "refresh", refreshExpirationMs);
    }

    public Long getUserId(String token) {
        String subject = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
        return Long.valueOf(subject);
    }

    public boolean isAccessToken(String token) {
        try {
            String type = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .get("type", String.class);
            return "access".equals(type);
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private String buildToken(Long userId, String type, long expirationMs) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("type", type)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMs))
                .signWith(secretKey)
                .compact();
    }
}
```

- [ ] **Step 4: JwtAuthenticationFilter 수정** — access token인지 확인

기존 조건:
```java
if (token != null && jwtTokenProvider.validateToken(token)) {
```
→ 아래로 교체:
```java
if (token != null && jwtTokenProvider.validateToken(token) && jwtTokenProvider.isAccessToken(token)) {
```

파일 전체:
```java
package com.buddkitv2.infra.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = resolveToken(request);

        if (token != null && jwtTokenProvider.validateToken(token) && jwtTokenProvider.isAccessToken(token)) {
            Long userId = jwtTokenProvider.getUserId(token);
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userId, null, List.of());
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (StringUtils.hasText(bearer) && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }
}
```

- [ ] **Step 5: 테스트 통과 확인**

```bash
./gradlew test --tests "com.buddkitv2.infra.security.JwtTokenProviderTest"
```
Expected: PASS (4 tests)

- [ ] **Step 6: 커밋**

```bash
git add src/main/java/com/buddkitv2/infra/security/JwtTokenProvider.java \
        src/main/java/com/buddkitv2/infra/security/JwtAuthenticationFilter.java \
        src/test/java/com/buddkitv2/infra/security/JwtTokenProviderTest.java
git commit -m "refactor(security): JwtTokenProvider AT/RT 분리 및 type claim 추가"
```

---

## Task 4: RefreshTokenService (Redis)

**Files:**
- Create: `src/main/java/com/buddkitv2/infra/security/RefreshTokenService.java`

> 테스트는 실제 Redis가 필요한 통합 테스트 범위. 이 태스크에서는 별도 단위 테스트 없이 구현만 작성하고, Task 7에서 AuthController 통합 흐름으로 간접 검증한다.

- [ ] **Step 1: RefreshTokenService 생성**

```java
package com.buddkitv2.infra.security;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final StringRedisTemplate redisTemplate;

    private static final long REFRESH_TOKEN_TTL_DAYS = 7;

    public void save(Long userId, String refreshToken) {
        redisTemplate.opsForValue().set(
                key(userId),
                hash(refreshToken),
                REFRESH_TOKEN_TTL_DAYS,
                TimeUnit.DAYS
        );
    }

    public boolean validate(Long userId, String refreshToken) {
        String stored = redisTemplate.opsForValue().get(key(userId));
        return stored != null && stored.equals(hash(refreshToken));
    }

    public void delete(Long userId) {
        redisTemplate.delete(key(userId));
    }

    private String key(Long userId) {
        return "RT:" + userId;
    }

    private String hash(String token) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
```

- [ ] **Step 2: 빌드 확인**

```bash
./gradlew build -x test
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: 커밋**

```bash
git add src/main/java/com/buddkitv2/infra/security/RefreshTokenService.java
git commit -m "feat(security): Redis 기반 RefreshToken 저장/검증/삭제 서비스 구현"
```

---

## Task 5: OAuth2SuccessHandler — UserService 연동, AT+RT 발급

**Files:**
- Create: `src/main/java/com/buddkitv2/api/auth/TokenResponse.java`
- Modify: `src/main/java/com/buddkitv2/infra/security/OAuth2SuccessHandler.java`

- [ ] **Step 1: TokenResponse DTO 생성**

```java
package com.buddkitv2.api.auth;

public record TokenResponse(String accessToken, String refreshToken) {
}
```

- [ ] **Step 2: OAuth2SuccessHandler 수정**

카카오 사용자 속성 구조:
```json
{
  "id": 1234567890,
  "kakao_account": {
    "profile": {
      "nickname": "홍길동",
      "profile_image_url": "https://..."
    }
  }
}
```

```java
package com.buddkitv2.infra.security;

import com.buddkitv2.api.auth.TokenResponse;
import com.buddkitv2.application.user.UserService;
import com.buddkitv2.domain.user.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final UserService userService;
    private final ObjectMapper objectMapper;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        Long kakaoId = ((Number) oAuth2User.getAttribute("id")).longValue();
        Map<String, Object> kakaoAccount = oAuth2User.getAttribute("kakao_account");
        Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");
        String nickname = (String) profile.get("nickname");
        String profileImageUrl = (String) profile.get("profile_image_url");

        User user = userService.findOrCreate(kakaoId, nickname, profileImageUrl);

        String accessToken = jwtTokenProvider.generateAccessToken(user.getId());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId());
        refreshTokenService.save(user.getId(), refreshToken);

        response.setContentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8");
        response.getWriter().write(
                objectMapper.writeValueAsString(new TokenResponse(accessToken, refreshToken))
        );
    }
}
```

- [ ] **Step 3: 빌드 확인**

```bash
./gradlew build -x test
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: 커밋**

```bash
git add src/main/java/com/buddkitv2/api/auth/TokenResponse.java \
        src/main/java/com/buddkitv2/infra/security/OAuth2SuccessHandler.java
git commit -m "feat(security): OAuth2 로그인 성공 시 AT/RT 발급 및 사용자 findOrCreate 연동"
```

---

## Task 6: AuthController — /auth/refresh (AT 재발급)

**Files:**
- Create: `src/main/java/com/buddkitv2/api/auth/RefreshRequest.java`
- Create: `src/main/java/com/buddkitv2/api/auth/AuthController.java`
- Create: `src/test/java/com/buddkitv2/api/auth/AuthControllerTest.java`

흐름: RT JWT 서명 검증 → type=refresh 확인 → userId 추출 → Redis 해시 비교 → 새 AT 반환

- [ ] **Step 1: 실패하는 테스트 작성**

```java
package com.buddkitv2.api.auth;

import com.buddkitv2.infra.security.JwtTokenProvider;
import com.buddkitv2.infra.security.RefreshTokenService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import({com.buddkitv2.infra.security.JwtAuthenticationEntryPoint.class,
         com.buddkitv2.infra.security.JwtAccessDeniedHandler.class})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private RefreshTokenService refreshTokenService;

    @MockBean
    private com.buddkitv2.infra.security.JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void 유효한_RT로_새_AT를_발급받는다() throws Exception {
        String rt = "valid.refresh.token";
        String newAt = "new.access.token";
        Long userId = 1L;

        when(jwtTokenProvider.validateToken(rt)).thenReturn(true);
        when(jwtTokenProvider.isAccessToken(rt)).thenReturn(false);
        when(jwtTokenProvider.getUserId(rt)).thenReturn(userId);
        when(refreshTokenService.validate(userId, rt)).thenReturn(true);
        when(jwtTokenProvider.generateAccessToken(userId)).thenReturn(newAt);

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshRequest(rt))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value(newAt));
    }

    @Test
    void 만료된_RT는_401을_반환한다() throws Exception {
        String rt = "expired.refresh.token";

        when(jwtTokenProvider.validateToken(rt)).thenReturn(false);

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshRequest(rt))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void Redis에_없는_RT는_401을_반환한다() throws Exception {
        String rt = "unknown.refresh.token";
        Long userId = 1L;

        when(jwtTokenProvider.validateToken(rt)).thenReturn(true);
        when(jwtTokenProvider.isAccessToken(rt)).thenReturn(false);
        when(jwtTokenProvider.getUserId(rt)).thenReturn(userId);
        when(refreshTokenService.validate(userId, rt)).thenReturn(false);

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshRequest(rt))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }
}
```

- [ ] **Step 2: 테스트 실행 — 실패 확인**

```bash
./gradlew test --tests "com.buddkitv2.api.auth.AuthControllerTest"
```
Expected: FAIL — `AuthController`, `RefreshRequest` 없음

- [ ] **Step 3: RefreshRequest DTO 생성**

```java
package com.buddkitv2.api.auth;

import jakarta.validation.constraints.NotBlank;

public record RefreshRequest(@NotBlank String refreshToken) {
}
```

- [ ] **Step 4: AuthController 생성**

```java
package com.buddkitv2.api.auth;

import com.buddkitv2.api.common.ApiResponse;
import com.buddkitv2.infra.security.JwtTokenProvider;
import com.buddkitv2.infra.security.RefreshTokenService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenResponse>> refresh(@Valid @RequestBody RefreshRequest request) {
        String rt = request.refreshToken();

        if (!jwtTokenProvider.validateToken(rt) || jwtTokenProvider.isAccessToken(rt)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.fail("유효하지 않은 리프레시 토큰입니다."));
        }

        Long userId = jwtTokenProvider.getUserId(rt);

        if (!refreshTokenService.validate(userId, rt)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.fail("만료되었거나 이미 사용된 리프레시 토큰입니다."));
        }

        String newAccessToken = jwtTokenProvider.generateAccessToken(userId);
        return ResponseEntity.ok(ApiResponse.ok(new TokenResponse(newAccessToken, rt)));
    }
}
```

- [ ] **Step 5: 테스트 통과 확인**

```bash
./gradlew test --tests "com.buddkitv2.api.auth.AuthControllerTest"
```
Expected: PASS (3 tests)

- [ ] **Step 6: 전체 테스트 통과 확인**

```bash
./gradlew test
```
Expected: 모든 테스트 PASS

- [ ] **Step 7: 커밋**

```bash
git add src/main/java/com/buddkitv2/api/auth/RefreshRequest.java \
        src/main/java/com/buddkitv2/api/auth/AuthController.java \
        src/test/java/com/buddkitv2/api/auth/AuthControllerTest.java
git commit -m "feat(auth): /auth/refresh 엔드포인트 구현 — AT 재발급"
```

---

## Self-Review

### 1. Spec 커버리지

| 요구사항 | 태스크 |
|---|---|
| 카카오 OAuth2 로그인 | Task 5 (OAuth2SuccessHandler) |
| AT = JWT stateless, 15분 | Task 3 (JwtTokenProvider.generateAccessToken) |
| RT = Redis 저장, key: RT:{userId}, value: tokenHash | Task 4 (RefreshTokenService) |
| RT = 7일 | Task 3 (refreshExpirationMs) |
| 401 미인증 에러 | Task 1 (JwtAuthenticationEntryPoint) |
| 403 미인가 에러 | Task 1 (JwtAccessDeniedHandler) |
| /auth/refresh로 AT 재발급 | Task 6 (AuthController) |

모든 요구사항 커버됨.

### 2. Placeholder 스캔

없음. 모든 스텝에 구체적인 코드 포함.

### 3. Type 일관성

- `JwtTokenProvider.generateAccessToken(Long userId)` → Task 3에서 정의, Task 5/6에서 사용 ✓
- `JwtTokenProvider.generateRefreshToken(Long userId)` → Task 3에서 정의, Task 5에서 사용 ✓
- `RefreshTokenService.save(Long userId, String refreshToken)` → Task 4에서 정의, Task 5에서 사용 ✓
- `RefreshTokenService.validate(Long userId, String refreshToken)` → Task 4에서 정의, Task 6에서 사용 ✓
- `TokenResponse(String accessToken, String refreshToken)` → Task 5에서 정의, Task 6에서 사용 ✓
- `UserService.findOrCreate(Long, String, String)` → Task 2에서 정의, Task 5에서 사용 ✓
