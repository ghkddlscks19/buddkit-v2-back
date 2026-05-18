# User Domain 나머지 기능 구현 계획

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 유저 도메인의 나머지 기능 구현 — 프로필 수정, 로그아웃, 회원탈퇴, 포인트 충전(카드), 거래/정산 내역, 내 모임/관심 모임 목록, FCM 토큰 관리.

**Architecture:** 기존 UserController/UserService에 기능을 추가한다. TossPaymentClient를 global/config에 분리해 결제 HTTP 호출을 캡슐화한다. 모임 관련 데이터는 Club 도메인 리포지토리에서 직접 조회한다 (Club 서비스 없음). 페이지네이션은 cursor(lastId) 기반 무한 스크롤로 통일한다.

**Tech Stack:** Spring Boot 4.x, Spring Data JPA, Spring Security (JWT), RestClient (Toss Payments), PostgreSQL, Redis, AWS S3, JUnit5 + AssertJ

---

## 파일 구조 개요

### 신규 생성

```
global/
  config/
    TossPaymentClient.java        ← Toss API /v1/payments/confirm 호출
  exception/
    TossPaymentException.java
    WithdrawnUserException.java

domain/
  user/
    dto/
      request/
        ProfileUpdateRequest.java
        FcmTokenRequest.java
        ChargeRequest.java
      response/
        ChargeResponse.java
        TransactionResponse.java
        SettlementHistoryResponse.java
        MyClubResponse.java
        LikedClubResponse.java
  wallet/
    repository/
      WalletTransactionRepository.java
      PaymentRepository.java
  settlement/
    repository/
      UserSettlementRepository.java
  club/
    repository/
      UserClubRepository.java
      ClubLikeRepository.java
```

### 수정

```
domain/common/BaseEntity.java                     ← @Getter 추가 (createdAt 노출)
domain/user/entity/User.java                      ← updateProfile() 추가
domain/user/dto/response/MyPageResponse.java      ← gender 필드 추가
domain/user/service/UserService.java              ← 신규 메서드 및 의존성 추가
domain/user/controller/UserController.java        ← 신규 엔드포인트 추가
domain/user/controller/AuthController.java        ← logout() 추가
domain/wallet/entity/Wallet.java                  ← charge() 추가
domain/wallet/entity/Payment.java                 ← create() 시그니처 업데이트
global/exception/GlobalExceptionHandler.java      ← 신규 예외 핸들러 추가
global/security/OAuth2SuccessHandler.java         ← 탈퇴 회원 재로그인 차단
```

---

### Task 1: 신규 예외 클래스 + GlobalExceptionHandler 등록

**Files:**
- Create: `src/main/java/com/buddkitv2/global/exception/TossPaymentException.java`
- Create: `src/main/java/com/buddkitv2/global/exception/WithdrawnUserException.java`
- Modify: `src/main/java/com/buddkitv2/global/exception/GlobalExceptionHandler.java`

- [ ] **Step 1: TossPaymentException 생성**

```java
package com.buddkitv2.global.exception;

public class TossPaymentException extends RuntimeException {
    public TossPaymentException() {
        super("결제 처리에 실패했습니다.");
    }
}
```

- [ ] **Step 2: WithdrawnUserException 생성**

```java
package com.buddkitv2.global.exception;

public class WithdrawnUserException extends RuntimeException {
    public WithdrawnUserException() {
        super("탈퇴한 회원입니다.");
    }
}
```

- [ ] **Step 3: GlobalExceptionHandler에 두 핸들러 추가**

`handleInternalError` 메서드 바로 위에 아래 두 메서드를 추가한다.

```java
@ExceptionHandler(TossPaymentException.class)
public ResponseEntity<ApiResponse<Void>> handleTossPayment(TossPaymentException e) {
    return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
}

@ExceptionHandler(WithdrawnUserException.class)
public ResponseEntity<ApiResponse<Void>> handleWithdrawnUser(WithdrawnUserException e) {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.fail(e.getMessage()));
}
```

- [ ] **Step 4: 컴파일 확인**

```bash
./gradlew compileJava
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 5: 커밋**

```bash
git add src/main/java/com/buddkitv2/global/exception/TossPaymentException.java \
        src/main/java/com/buddkitv2/global/exception/WithdrawnUserException.java \
        src/main/java/com/buddkitv2/global/exception/GlobalExceptionHandler.java
git commit -m "feat(user): 커스텀 예외 TossPaymentException, WithdrawnUserException 추가"
```

---

### Task 2: BaseEntity @Getter + MyPageResponse gender + User.updateProfile()

**Files:**
- Modify: `src/main/java/com/buddkitv2/domain/common/BaseEntity.java`
- Modify: `src/main/java/com/buddkitv2/domain/user/dto/response/MyPageResponse.java`
- Modify: `src/main/java/com/buddkitv2/domain/user/entity/User.java`
- Modify: `src/main/java/com/buddkitv2/domain/user/service/UserService.java`

- [ ] **Step 1: BaseEntity에 @Getter 추가**

`BaseEntity` 클래스 선언 바로 위에 `@Getter` 임포트와 어노테이션을 추가한다.

```java
import lombok.Getter;

@Getter  // ← 추가
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {
    // 기존 필드 유지
}
```

- [ ] **Step 2: MyPageResponse에 gender 필드 추가**

```java
package com.buddkitv2.domain.user.dto.response;

import com.buddkitv2.domain.user.entity.Gender;
import com.buddkitv2.domain.user.entity.InterestCategory;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@AllArgsConstructor
public class MyPageResponse {
    private Long id;
    private String nickname;
    private String profileImageUrl;
    private String city;
    private String district;
    private LocalDate birth;
    private Gender gender;
    private List<InterestCategory> interestList;
    private Long balance;
}
```

- [ ] **Step 3: User에 updateProfile() 메서드 추가**

`User.java`의 `withdraw()` 메서드 아래에 추가한다.

```java
public void updateProfile(String nickname, Address address, String profileImageUrl) {
    this.nickname = nickname;
    this.address = address;
    this.profileImageUrl = profileImageUrl;
}
```

- [ ] **Step 4: UserService.getMyPage()에 gender 필드 추가**

`getMyPage()` 메서드의 `return new MyPageResponse(...)` 를 아래로 교체한다.

```java
return new MyPageResponse(
        user.getId(),
        user.getNickname(),
        user.getProfileImageUrl(),
        address != null ? address.getCity() : null,
        address != null ? address.getDistrict() : null,
        user.getBirth(),
        user.getGender(),
        interests,
        wallet.getBalance()
);
```

- [ ] **Step 5: 컴파일 확인**

```bash
./gradlew compileJava compileTestJava
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 6: 기존 테스트 통과 확인**

```bash
./gradlew test --tests "com.buddkitv2.domain.user.service.UserServiceTest"
```

Expected: 기존 2개 테스트 PASS

- [ ] **Step 7: 커밋**

```bash
git add src/main/java/com/buddkitv2/domain/common/BaseEntity.java \
        src/main/java/com/buddkitv2/domain/user/dto/response/MyPageResponse.java \
        src/main/java/com/buddkitv2/domain/user/entity/User.java \
        src/main/java/com/buddkitv2/domain/user/service/UserService.java
git commit -m "feat(user): 프로필 응답에 gender 추가 및 updateProfile 메서드 도입"
```

---

### Task 3: Wallet.charge() + Payment.create() 업데이트

**Files:**
- Modify: `src/main/java/com/buddkitv2/domain/wallet/entity/Wallet.java`
- Modify: `src/main/java/com/buddkitv2/domain/wallet/entity/Payment.java`

- [ ] **Step 1: Wallet에 charge() 메서드 추가**

`Wallet.java`의 `createWithBonus()` 아래에 추가한다.

```java
public void charge(Long amount) {
    this.balance += amount;
}
```

- [ ] **Step 2: Payment.create() 시그니처 업데이트**

기존 `create()` 메서드를 아래로 교체한다. (tossPaymentKey, approvedAt 추가, status를 DONE으로 변경)

```java
public static Payment create(WalletTransaction walletTransaction,
                              String tossPaymentKey, String tossOrderId,
                              String method, Long totalAmount, LocalDateTime approvedAt) {
    Payment p = new Payment();
    p.paymentId = UUID.randomUUID();
    p.walletTransaction = walletTransaction;
    p.tossPaymentKey = tossPaymentKey;
    p.tossOrderId = tossOrderId;
    p.method = method;
    p.totalAmount = totalAmount;
    p.approvedAt = approvedAt;
    p.status = PaymentStatus.DONE;
    return p;
}
```

- [ ] **Step 3: 컴파일 확인**

```bash
./gradlew compileJava
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 4: 커밋**

```bash
git add src/main/java/com/buddkitv2/domain/wallet/entity/Wallet.java \
        src/main/java/com/buddkitv2/domain/wallet/entity/Payment.java
git commit -m "feat(wallet): charge() 메서드 추가 및 Payment.create() 시그니처 확장"
```

---

### Task 4: TossPaymentClient

**Files:**
- Create: `src/main/java/com/buddkitv2/global/config/TossPaymentClient.java`

> **사전 조건:** `application.yml` (또는 `application-local.yml`)에 `toss.secret-key: {your-test-secret-key}` 추가 필요. 테스트 키는 토스페이먼츠 대시보드에서 발급.

- [ ] **Step 1: TossPaymentClient 생성**

`RestClient.Builder`를 주입받아야 Spring Boot가 구성한 Jackson 3.x 직렬화 설정이 적용된다. `RestClient.create()`는 컨텍스트 외부에서 생성되어 메시지 컨버터가 누락될 수 있다.

```java
package com.buddkitv2.global.config;

import com.buddkitv2.global.exception.TossPaymentException;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.Map;

@Component
public class TossPaymentClient {

    private final RestClient restClient;

    @Value("${toss.secret-key}")
    private String secretKey;

    public TossPaymentClient(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
    }

    public TossConfirmResult confirm(String paymentKey, String orderId, Long amount) {
        String encoded = Base64.getEncoder().encodeToString((secretKey + ":").getBytes());
        try {
            TossConfirmResponse response = restClient.post()
                    .uri("https://api.tosspayments.com/v1/payments/confirm")
                    .header("Authorization", "Basic " + encoded)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("paymentKey", paymentKey, "orderId", orderId, "amount", amount))
                    .retrieve()
                    .body(TossConfirmResponse.class);

            if (response == null || !"DONE".equals(response.getStatus())) {
                throw new TossPaymentException();
            }

            LocalDateTime approvedAt = OffsetDateTime.parse(response.getApprovedAt())
                    .toLocalDateTime();

            return new TossConfirmResult(
                    response.getPaymentKey(),
                    response.getOrderId(),
                    response.getMethod(),
                    response.getTotalAmount(),
                    approvedAt
            );
        } catch (TossPaymentException e) {
            throw e;
        } catch (Exception e) {
            throw new TossPaymentException();
        }
    }

    @Getter @Setter @NoArgsConstructor
    public static class TossConfirmResponse {
        private String paymentKey;
        private String orderId;
        private String method;
        private Long totalAmount;
        private String approvedAt;
        private String status;
    }

    public record TossConfirmResult(
            String paymentKey,
            String orderId,
            String method,
            Long totalAmount,
            LocalDateTime approvedAt
    ) {}
}
```

- [ ] **Step 2: 컴파일 확인**

```bash
./gradlew compileJava
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 3: 커밋**

```bash
git add src/main/java/com/buddkitv2/global/config/TossPaymentClient.java
git commit -m "feat(wallet): TossPaymentClient 추가 (카드 결제 confirm)"
```

---

### Task 5: 신규 리포지토리 5개

**Files:**
- Create: `src/main/java/com/buddkitv2/domain/wallet/repository/WalletTransactionRepository.java`
- Create: `src/main/java/com/buddkitv2/domain/wallet/repository/PaymentRepository.java`
- Create: `src/main/java/com/buddkitv2/domain/settlement/repository/UserSettlementRepository.java`
- Create: `src/main/java/com/buddkitv2/domain/club/repository/UserClubRepository.java`
- Create: `src/main/java/com/buddkitv2/domain/club/repository/ClubLikeRepository.java`

- [ ] **Step 1: WalletTransactionRepository 생성**

```java
package com.buddkitv2.domain.wallet.repository;

import com.buddkitv2.domain.wallet.entity.WalletTransaction;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {

    List<WalletTransaction> findByWallet_IdOrderByIdDesc(Long walletId, Pageable pageable);

    List<WalletTransaction> findByWallet_IdAndIdLessThanOrderByIdDesc(Long walletId, Long lastId, Pageable pageable);
}
```

- [ ] **Step 2: PaymentRepository 생성**

```java
package com.buddkitv2.domain.wallet.repository;

import com.buddkitv2.domain.wallet.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {
}
```

- [ ] **Step 3: UserSettlementRepository 생성**

```java
package com.buddkitv2.domain.settlement.repository;

import com.buddkitv2.domain.settlement.entity.UserSettlement;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserSettlementRepository extends JpaRepository<UserSettlement, Long> {

    @Query("SELECT us FROM UserSettlement us JOIN FETCH us.settlement WHERE us.user.id = :userId ORDER BY us.id DESC")
    List<UserSettlement> findByUserIdWithSettlement(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT us FROM UserSettlement us JOIN FETCH us.settlement WHERE us.user.id = :userId AND us.id < :lastId ORDER BY us.id DESC")
    List<UserSettlement> findByUserIdAndLastIdWithSettlement(@Param("userId") Long userId, @Param("lastId") Long lastId, Pageable pageable);
}
```

- [ ] **Step 4: UserClubRepository 생성**

```java
package com.buddkitv2.domain.club.repository;

import com.buddkitv2.domain.club.entity.UserClub;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserClubRepository extends JpaRepository<UserClub, Long> {

    @Query("SELECT uc FROM UserClub uc JOIN FETCH uc.club c JOIN FETCH c.address JOIN FETCH c.interest WHERE uc.user.id = :userId ORDER BY uc.id DESC")
    List<UserClub> findByUserIdWithClub(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT uc FROM UserClub uc JOIN FETCH uc.club c JOIN FETCH c.address JOIN FETCH c.interest WHERE uc.user.id = :userId AND uc.id < :lastId ORDER BY uc.id DESC")
    List<UserClub> findByUserIdAndLastIdWithClub(@Param("userId") Long userId, @Param("lastId") Long lastId, Pageable pageable);
}
```

- [ ] **Step 5: ClubLikeRepository 생성**

```java
package com.buddkitv2.domain.club.repository;

import com.buddkitv2.domain.club.entity.ClubLike;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ClubLikeRepository extends JpaRepository<ClubLike, Long> {

    @Query("SELECT cl FROM ClubLike cl JOIN FETCH cl.club c JOIN FETCH c.address JOIN FETCH c.interest WHERE cl.user.id = :userId ORDER BY cl.id DESC")
    List<ClubLike> findByUserIdWithClub(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT cl FROM ClubLike cl JOIN FETCH cl.club c JOIN FETCH c.address JOIN FETCH c.interest WHERE cl.user.id = :userId AND cl.id < :lastId ORDER BY cl.id DESC")
    List<ClubLike> findByUserIdAndLastIdWithClub(@Param("userId") Long userId, @Param("lastId") Long lastId, Pageable pageable);
}
```

- [ ] **Step 6: 컴파일 확인**

```bash
./gradlew compileJava
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 7: 커밋**

```bash
git add src/main/java/com/buddkitv2/domain/wallet/repository/ \
        src/main/java/com/buddkitv2/domain/settlement/repository/ \
        src/main/java/com/buddkitv2/domain/club/repository/
git commit -m "feat: 지갑/정산/모임 도메인 리포지토리 추가"
```

---

### Task 6: 신규 DTO 8개

**Files:**
- Create: `src/main/java/com/buddkitv2/domain/user/dto/request/ProfileUpdateRequest.java`
- Create: `src/main/java/com/buddkitv2/domain/user/dto/request/FcmTokenRequest.java`
- Create: `src/main/java/com/buddkitv2/domain/user/dto/request/ChargeRequest.java`
- Create: `src/main/java/com/buddkitv2/domain/user/dto/response/ChargeResponse.java`
- Create: `src/main/java/com/buddkitv2/domain/user/dto/response/TransactionResponse.java`
- Create: `src/main/java/com/buddkitv2/domain/user/dto/response/SettlementHistoryResponse.java`
- Create: `src/main/java/com/buddkitv2/domain/user/dto/response/MyClubResponse.java`
- Create: `src/main/java/com/buddkitv2/domain/user/dto/response/LikedClubResponse.java`

- [ ] **Step 1: ProfileUpdateRequest 생성**

```java
package com.buddkitv2.domain.user.dto.request;

import com.buddkitv2.domain.user.entity.InterestCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter @Setter @NoArgsConstructor
public class ProfileUpdateRequest {

    @NotBlank
    @Pattern(regexp = "^[a-zA-Z0-9가-힣]{2,10}$", message = "닉네임은 2~10자의 영문, 한글, 숫자만 가능합니다.")
    private String nickname;

    @NotBlank
    private String city;

    @NotBlank
    private String district;

    @NotNull
    @Size(min = 1, max = 5, message = "관심사는 1개 이상 5개 이하로 선택해주세요.")
    private List<InterestCategory> interests;
}
```

- [ ] **Step 2: FcmTokenRequest 생성**

```java
package com.buddkitv2.domain.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor
public class FcmTokenRequest {

    @NotBlank
    private String token;
}
```

- [ ] **Step 3: ChargeRequest 생성**

```java
package com.buddkitv2.domain.user.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor
public class ChargeRequest {

    @NotBlank
    private String paymentKey;

    @NotBlank
    private String orderId;

    @NotNull
    @Min(1)
    private Long amount;
}
```

- [ ] **Step 4: ChargeResponse 생성**

```java
package com.buddkitv2.domain.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter @AllArgsConstructor
public class ChargeResponse {
    private Long balance;
}
```

- [ ] **Step 5: TransactionResponse 생성**

```java
package com.buddkitv2.domain.user.dto.response;

import com.buddkitv2.domain.wallet.entity.WalletTransactionType;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter @AllArgsConstructor
public class TransactionResponse {
    private Long id;
    private WalletTransactionType type;
    private Long balance;
    private LocalDateTime createdAt;
}
```

- [ ] **Step 6: SettlementHistoryResponse 생성**

```java
package com.buddkitv2.domain.user.dto.response;

import com.buddkitv2.domain.settlement.entity.UserSettlementStatus;
import com.buddkitv2.domain.settlement.entity.UserSettlementType;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter @AllArgsConstructor
public class SettlementHistoryResponse {
    private Long id;
    private UserSettlementStatus status;
    private UserSettlementType type;
    private Long amount;
    private LocalDateTime completedTime;
    private LocalDateTime createdAt;
}
```

- [ ] **Step 7: MyClubResponse 생성**

```java
package com.buddkitv2.domain.user.dto.response;

import com.buddkitv2.domain.user.entity.InterestCategory;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter @AllArgsConstructor
public class MyClubResponse {
    private Long clubId;
    private String name;
    private String clubImage;
    private InterestCategory interestCategory;
    private String interestName;
    private String city;
    private String district;
    private Integer memberCount;
    private String role;
}
```

- [ ] **Step 8: LikedClubResponse 생성**

```java
package com.buddkitv2.domain.user.dto.response;

import com.buddkitv2.domain.user.entity.InterestCategory;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter @AllArgsConstructor
public class LikedClubResponse {
    private Long clubId;
    private String name;
    private String clubImage;
    private InterestCategory interestCategory;
    private String interestName;
    private String city;
    private String district;
    private Integer memberCount;
}
```

- [ ] **Step 9: 컴파일 확인**

```bash
./gradlew compileJava
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 10: 커밋**

```bash
git add src/main/java/com/buddkitv2/domain/user/dto/
git commit -m "feat(user): 프로필 수정/충전/내역/모임 관련 요청·응답 DTO 추가"
```

---

### Task 7: 프로필 수정 (PATCH /users/me)

**Files:**
- Modify: `src/main/java/com/buddkitv2/domain/user/service/UserService.java`
- Modify: `src/main/java/com/buddkitv2/domain/user/controller/UserController.java`
- Modify: `src/test/java/com/buddkitv2/domain/user/service/UserServiceTest.java`

- [ ] **Step 1: 테스트 먼저 작성**

`UserServiceTest.java`에 아래 테스트를 추가한다.

```java
@Test
void 프로필_수정_시_닉네임과_관심사가_변경된다() {
    UserService.RegisterResult result = userService.register(KAKAO_ID, request(), null);
    Long userId = result.getUserId();

    Address newAddress = addressRepository.save(Address.of("부산광역시", "해운대구", 99001));
    interestRepository.save(Interest.of(InterestCategory.SPORTS, "운동/스포츠"));

    ProfileUpdateRequest updateRequest = new ProfileUpdateRequest();
    updateRequest.setNickname("변경닉네임");
    updateRequest.setCity("부산광역시");
    updateRequest.setDistrict("해운대구");
    updateRequest.setInterests(List.of(InterestCategory.SPORTS));

    userService.updateProfile(userId, updateRequest, null);

    MyPageResponse profile = userService.getMyPage(userId);
    assertThat(profile.getNickname()).isEqualTo("변경닉네임");
    assertThat(profile.getCity()).isEqualTo("부산광역시");
    assertThat(profile.getInterestList()).containsExactly(InterestCategory.SPORTS);
}
```

- [ ] **Step 2: 테스트 실행 — 실패 확인**

```bash
./gradlew test --tests "com.buddkitv2.domain.user.service.UserServiceTest.프로필_수정_시_닉네임과_관심사가_변경된다"
```

Expected: FAIL — `userService.updateProfile()` 메서드 없음

- [ ] **Step 3: UserService에 updateProfile() 구현**

`UserService.java`에 새 필드를 추가하고 메서드를 구현한다.

클래스 상단 필드 선언에 아래를 추가한다 (Lombok `@RequiredArgsConstructor`가 자동 주입):
```java
private final WalletTransactionRepository walletTransactionRepository;
private final PaymentRepository paymentRepository;
private final UserSettlementRepository userSettlementRepository;
private final UserClubRepository userClubRepository;
private final ClubLikeRepository clubLikeRepository;
private final TossPaymentClient tossPaymentClient;
```

그리고 아래 메서드를 추가한다:

```java
@Transactional
public MyPageResponse updateProfile(Long userId, ProfileUpdateRequest request, MultipartFile profileImage) {
    User user = userRepository.findById(userId)
            .orElseThrow(UserNotFoundException::new);

    Address address = addressRepository.findByCityAndDistrict(request.getCity(), request.getDistrict())
            .orElseThrow(InvalidAddressException::new);

    List<Interest> interests = interestRepository.findByCategoryIn(request.getInterests());
    if (interests.size() != request.getInterests().size()) {
        throw new InvalidInterestException();
    }

    String profileImageUrl = user.getProfileImageUrl();
    if (profileImage != null && !profileImage.isEmpty()) {
        profileImageUrl = s3Service.upload(profileImage, "profiles");
    }

    user.updateProfile(request.getNickname(), address, profileImageUrl);

    userInterestRepository.deleteAllByUserId(userId);
    interests.forEach(interest -> userInterestRepository.save(UserInterest.create(user, interest)));

    return getMyPage(userId);
}
```

- [ ] **Step 4: UserInterestRepository에 deleteAllByUserId() 추가**

`UserInterestRepository.java`에 아래 메서드를 추가한다.

```java
void deleteAllByUserId(Long userId);
```

- [ ] **Step 5: UserInterest.create() 확인**

`UserInterest.java`에 `create()` 정적 팩토리 메서드가 있는지 확인한다. 없으면 추가한다.

```java
public static UserInterest create(User user, Interest interest) {
    UserInterest ui = new UserInterest();
    ui.user = user;
    ui.interest = interest;
    return ui;
}
```

- [ ] **Step 6: 테스트 재실행 — 통과 확인**

```bash
./gradlew test --tests "com.buddkitv2.domain.user.service.UserServiceTest.프로필_수정_시_닉네임과_관심사가_변경된다"
```

Expected: PASS

- [ ] **Step 7: UserController에 PATCH /users/me 추가**

```java
@PatchMapping(value = "/me", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
public ApiResponse<MyPageResponse> updateProfile(
        @AuthenticationPrincipal Long userId,
        @RequestPart("data") @Valid ProfileUpdateRequest request,
        @RequestPart(value = "profileImage", required = false) MultipartFile profileImage
) {
    return ApiResponse.ok(userService.updateProfile(userId, request, profileImage));
}
```

- [ ] **Step 8: 컴파일 확인**

```bash
./gradlew compileJava compileTestJava
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 9: 커밋**

```bash
git add src/main/java/com/buddkitv2/domain/user/service/UserService.java \
        src/main/java/com/buddkitv2/domain/user/controller/UserController.java \
        src/main/java/com/buddkitv2/domain/user/repository/UserInterestRepository.java \
        src/test/java/com/buddkitv2/domain/user/service/UserServiceTest.java
git commit -m "feat(user): 프로필 수정 API 구현 (PATCH /users/me)"
```

---

### Task 8: 로그아웃 (POST /auth/logout) + 탈퇴 회원 재로그인 차단

**Files:**
- Modify: `src/main/java/com/buddkitv2/domain/user/controller/AuthController.java`
- Modify: `src/main/java/com/buddkitv2/global/security/OAuth2SuccessHandler.java`
- Modify: `src/test/java/com/buddkitv2/domain/user/controller/AuthControllerTest.java`

- [ ] **Step 1: AuthController에 logout() 추가**

`AuthController.java`에 import 추가 및 메서드 추가:

```java
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
// 기존 import 유지
```

`refresh()` 메서드 아래에 추가:

```java
@PostMapping("/logout")
public ResponseEntity<Void> logout(@AuthenticationPrincipal Long userId) {
    refreshTokenService.delete(userId);
    return ResponseEntity.noContent().build();
}
```

- [ ] **Step 2: OAuth2SuccessHandler에 탈퇴 회원 차단 로직 추가**

`OAuth2SuccessHandler.java`에 import 추가:

```java
import com.buddkitv2.domain.user.entity.UserStatus;
import com.buddkitv2.global.exception.WithdrawnUserException;
```

`onAuthenticationSuccess()` 내부의 `if (existing.isPresent())` 블록을 아래로 교체:

```java
if (existing.isPresent()) {
    User user = existing.get();

    if (user.getStatus() == UserStatus.WITHDRAWN) {
        throw new WithdrawnUserException();
    }

    String accessToken = jwtTokenProvider.generateAccessToken(user.getId());
    String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId());
    refreshTokenService.save(user.getId(), refreshToken);

    String redirectUrl = loginSuccessUrl + "?at=" + accessToken + "&rt=" + refreshToken;
    getRedirectStrategy().sendRedirect(request, response, redirectUrl);
} else {
    String tempToken = tempTokenService.issue(kakaoId);
    getRedirectStrategy().sendRedirect(request, response, registerUrl + "?tempToken=" + tempToken);
}
```

- [ ] **Step 3: 컴파일 확인**

```bash
./gradlew compileJava
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 4: 커밋**

```bash
git add src/main/java/com/buddkitv2/domain/user/controller/AuthController.java \
        src/main/java/com/buddkitv2/global/security/OAuth2SuccessHandler.java
git commit -m "feat(user): 로그아웃 API 추가 및 탈퇴 회원 재로그인 차단"
```

---

### Task 9: 회원탈퇴 (DELETE /users/me)

**Files:**
- Modify: `src/main/java/com/buddkitv2/domain/user/service/UserService.java`
- Modify: `src/main/java/com/buddkitv2/domain/user/controller/UserController.java`
- Modify: `src/test/java/com/buddkitv2/domain/user/service/UserServiceTest.java`

- [ ] **Step 1: 테스트 먼저 작성**

`UserServiceTest.java`에 아래 테스트를 추가한다.

```java
@Autowired
private RefreshTokenService refreshTokenService;

@Test
void 회원탈퇴_시_상태가_WITHDRAWN으로_변경된다() {
    UserService.RegisterResult result = userService.register(KAKAO_ID, request(), null);
    Long userId = result.getUserId();

    userService.withdraw(userId);

    User user = userRepository.findById(userId).orElseThrow();
    assertThat(user.getStatus()).isEqualTo(UserStatus.WITHDRAWN);
    assertThat(user.getFcmToken()).isNull();
}
```

- [ ] **Step 2: 테스트 실행 — 실패 확인**

```bash
./gradlew test --tests "com.buddkitv2.domain.user.service.UserServiceTest.회원탈퇴_시_상태가_WITHDRAWN으로_변경된다"
```

Expected: FAIL — `userService.withdraw()` 메서드 없음

- [ ] **Step 3: UserService에 withdraw() 구현**

`UserService.java`에 의존성을 추가한다. 클래스 상단 필드에:

```java
private final RefreshTokenService refreshTokenService;
```

그리고 아래 메서드를 추가한다:

```java
@Transactional
public void withdraw(Long userId) {
    User user = userRepository.findById(userId)
            .orElseThrow(UserNotFoundException::new);
    user.withdraw();
    user.updateFcmToken(null);
    refreshTokenService.delete(userId);
}
```

- [ ] **Step 4: 테스트 재실행 — 통과 확인**

```bash
./gradlew test --tests "com.buddkitv2.domain.user.service.UserServiceTest.회원탈퇴_시_상태가_WITHDRAWN으로_변경된다"
```

Expected: PASS

- [ ] **Step 5: UserController에 DELETE /users/me 추가**

```java
import org.springframework.http.ResponseEntity;

@DeleteMapping("/me")
public ResponseEntity<Void> withdraw(@AuthenticationPrincipal Long userId) {
    userService.withdraw(userId);
    return ResponseEntity.noContent().build();
}
```

- [ ] **Step 6: 전체 UserService 테스트 통과 확인**

```bash
./gradlew test --tests "com.buddkitv2.domain.user.service.UserServiceTest"
```

Expected: 모든 테스트 PASS

- [ ] **Step 7: 커밋**

```bash
git add src/main/java/com/buddkitv2/domain/user/service/UserService.java \
        src/main/java/com/buddkitv2/domain/user/controller/UserController.java \
        src/test/java/com/buddkitv2/domain/user/service/UserServiceTest.java
git commit -m "feat(user): 회원탈퇴 API 구현 (DELETE /users/me)"
```

---

### Task 10: 포인트 충전 (POST /users/me/wallet/charge)

**Files:**
- Modify: `src/main/java/com/buddkitv2/domain/user/service/UserService.java`
- Modify: `src/main/java/com/buddkitv2/domain/user/controller/UserController.java`
- Modify: `src/test/java/com/buddkitv2/domain/user/service/UserServiceTest.java`

- [ ] **Step 1: 테스트 먼저 작성**

`UserServiceTest.java`에 아래를 추가한다.

```java
@Autowired
private WalletTransactionRepository walletTransactionRepository;

@MockitoBean
private TossPaymentClient tossPaymentClient;

@Test
void 충전_성공_시_잔액이_증가한다() {
    UserService.RegisterResult result = userService.register(KAKAO_ID, request(), null);
    Long userId = result.getUserId();
    Long initialBalance = result.getPoint(); // 100_000L

    String paymentKey = "test_pk_123";
    String orderId = "order_001";
    Long amount = 50_000L;

    given(tossPaymentClient.confirm(paymentKey, orderId, amount))
            .willReturn(new TossPaymentClient.TossConfirmResult(
                    paymentKey, orderId, "카드", amount,
                    LocalDateTime.of(2026, 1, 1, 10, 0)
            ));

    ChargeRequest chargeRequest = new ChargeRequest();
    chargeRequest.setPaymentKey(paymentKey);
    chargeRequest.setOrderId(orderId);
    chargeRequest.setAmount(amount);

    ChargeResponse response = userService.chargeWallet(userId, chargeRequest);

    assertThat(response.getBalance()).isEqualTo(initialBalance + amount);
}
```

import 추가 (클래스 상단):
```java
import static org.mockito.BDDMockito.given;
import com.buddkitv2.global.config.TossPaymentClient;
import com.buddkitv2.domain.wallet.repository.WalletTransactionRepository;
import com.buddkitv2.domain.user.dto.request.ChargeRequest;
import com.buddkitv2.domain.user.dto.response.ChargeResponse;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import java.time.LocalDateTime;
```

- [ ] **Step 2: 테스트 실행 — 실패 확인**

```bash
./gradlew test --tests "com.buddkitv2.domain.user.service.UserServiceTest.충전_성공_시_잔액이_증가한다"
```

Expected: FAIL — `userService.chargeWallet()` 메서드 없음

- [ ] **Step 3: UserService에 chargeWallet() 구현**

```java
@Transactional
public ChargeResponse chargeWallet(Long userId, ChargeRequest request) {
    Wallet wallet = walletRepository.findByUserId(userId)
            .orElseThrow(WalletNotFoundException::new);

    TossPaymentClient.TossConfirmResult confirmed =
            tossPaymentClient.confirm(request.getPaymentKey(), request.getOrderId(), request.getAmount());

    WalletTransaction transaction = WalletTransaction.create(
            wallet, wallet, WalletTransactionType.CHARGE, confirmed.totalAmount()
    );
    walletTransactionRepository.save(transaction);

    Payment payment = Payment.create(
            transaction,
            confirmed.paymentKey(),
            confirmed.orderId(),
            confirmed.method(),
            confirmed.totalAmount(),
            confirmed.approvedAt()
    );
    paymentRepository.save(payment);

    wallet.charge(confirmed.totalAmount());

    return new ChargeResponse(wallet.getBalance());
}
```

- [ ] **Step 4: 테스트 재실행 — 통과 확인**

```bash
./gradlew test --tests "com.buddkitv2.domain.user.service.UserServiceTest.충전_성공_시_잔액이_증가한다"
```

Expected: PASS

- [ ] **Step 5: UserController에 POST /users/me/wallet/charge 추가**

```java
@PostMapping("/me/wallet/charge")
public ApiResponse<ChargeResponse> chargeWallet(
        @AuthenticationPrincipal Long userId,
        @RequestBody @Valid ChargeRequest request
) {
    return ApiResponse.ok(userService.chargeWallet(userId, request));
}
```

- [ ] **Step 6: 컴파일 확인**

```bash
./gradlew compileJava compileTestJava
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 7: 커밋**

```bash
git add src/main/java/com/buddkitv2/domain/user/service/UserService.java \
        src/main/java/com/buddkitv2/domain/user/controller/UserController.java \
        src/test/java/com/buddkitv2/domain/user/service/UserServiceTest.java
git commit -m "feat(user): 포인트 충전 API 구현 (POST /users/me/wallet/charge)"
```

---

### Task 11: 거래/정산 내역 (GET /users/me/transactions, /settlements)

**Files:**
- Modify: `src/main/java/com/buddkitv2/domain/settlement/entity/UserSettlement.java`
- Modify: `src/main/java/com/buddkitv2/domain/user/service/UserService.java`
- Modify: `src/main/java/com/buddkitv2/domain/user/controller/UserController.java`
- Modify: `src/test/java/com/buddkitv2/domain/user/service/UserServiceTest.java`

- [ ] **Step 0: UserSettlement에 BaseEntity 상속 추가**

`SettlementHistoryResponse`에 `createdAt`이 필요하다. 현재 `UserSettlement`은 `BaseEntity`를 상속하지 않아 `getCreatedAt()`이 없다.

`UserSettlement.java` 클래스 선언을 아래로 수정한다.

```java
import com.buddkitv2.domain.common.BaseEntity;

public class UserSettlement extends BaseEntity {
    // 기존 필드 그대로 유지
```

컴파일 확인:
```bash
./gradlew compileJava
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 1: 테스트 먼저 작성**

`UserServiceTest.java`에 아래를 추가한다.

```java
@Test
void 거래내역_목록_조회_시_최신순으로_반환된다() {
    UserService.RegisterResult result = userService.register(KAKAO_ID, request(), null);
    Long userId = result.getUserId();

    String paymentKey = "test_pk_456";
    String orderId = "order_002";
    Long amount = 30_000L;

    given(tossPaymentClient.confirm(paymentKey, orderId, amount))
            .willReturn(new TossPaymentClient.TossConfirmResult(
                    paymentKey, orderId, "카드", amount,
                    LocalDateTime.of(2026, 1, 2, 10, 0)
            ));

    ChargeRequest chargeRequest = new ChargeRequest();
    chargeRequest.setPaymentKey(paymentKey);
    chargeRequest.setOrderId(orderId);
    chargeRequest.setAmount(amount);
    userService.chargeWallet(userId, chargeRequest);

    List<TransactionResponse> transactions = userService.getTransactions(userId, null, 20);

    assertThat(transactions).hasSize(1);
    assertThat(transactions.get(0).getType()).isEqualTo(WalletTransactionType.CHARGE);
    assertThat(transactions.get(0).getBalance()).isEqualTo(amount);
}
```

import 추가:
```java
import com.buddkitv2.domain.user.dto.response.TransactionResponse;
import com.buddkitv2.domain.wallet.entity.WalletTransactionType;
import java.util.List;
```

- [ ] **Step 2: 테스트 실행 — 실패 확인**

```bash
./gradlew test --tests "com.buddkitv2.domain.user.service.UserServiceTest.거래내역_목록_조회_시_최신순으로_반환된다"
```

Expected: FAIL — `getTransactions()` 메서드 없음

- [ ] **Step 3: UserService에 getTransactions(), getSettlements() 구현**

```java
@Transactional(readOnly = true)
public List<TransactionResponse> getTransactions(Long userId, Long lastId, int size) {
    Wallet wallet = walletRepository.findByUserId(userId)
            .orElseThrow(WalletNotFoundException::new);
    Pageable pageable = PageRequest.of(0, size);

    List<WalletTransaction> transactions = lastId == null
            ? walletTransactionRepository.findByWallet_IdOrderByIdDesc(wallet.getId(), pageable)
            : walletTransactionRepository.findByWallet_IdAndIdLessThanOrderByIdDesc(wallet.getId(), lastId, pageable);

    return transactions.stream()
            .map(t -> new TransactionResponse(t.getId(), t.getType(), t.getBalance(), t.getCreatedAt()))
            .toList();
}

@Transactional(readOnly = true)
public List<SettlementHistoryResponse> getSettlements(Long userId, Long lastId, int size) {
    Pageable pageable = PageRequest.of(0, size);

    List<UserSettlement> settlements = lastId == null
            ? userSettlementRepository.findByUserIdWithSettlement(userId, pageable)
            : userSettlementRepository.findByUserIdAndLastIdWithSettlement(userId, lastId, pageable);

    return settlements.stream()
            .map(us -> new SettlementHistoryResponse(
                    us.getId(),
                    us.getStatus(),
                    us.getType(),
                    us.getSettlement().getSum(),
                    us.getCompletedTime(),
                    us.getCreatedAt()
            ))
            .toList();
}
```

import 추가:
```java
import com.buddkitv2.domain.user.dto.response.SettlementHistoryResponse;
import com.buddkitv2.domain.user.dto.response.TransactionResponse;
import com.buddkitv2.domain.settlement.entity.UserSettlement;
import com.buddkitv2.domain.wallet.entity.WalletTransaction;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
```

- [ ] **Step 4: 테스트 재실행 — 통과 확인**

```bash
./gradlew test --tests "com.buddkitv2.domain.user.service.UserServiceTest.거래내역_목록_조회_시_최신순으로_반환된다"
```

Expected: PASS

- [ ] **Step 5: UserController에 엔드포인트 추가**

```java
@GetMapping("/me/transactions")
public ApiResponse<List<TransactionResponse>> getTransactions(
        @AuthenticationPrincipal Long userId,
        @RequestParam(required = false) Long lastId,
        @RequestParam(defaultValue = "20") int size
) {
    return ApiResponse.ok(userService.getTransactions(userId, lastId, size));
}

@GetMapping("/me/settlements")
public ApiResponse<List<SettlementHistoryResponse>> getSettlements(
        @AuthenticationPrincipal Long userId,
        @RequestParam(required = false) Long lastId,
        @RequestParam(defaultValue = "20") int size
) {
    return ApiResponse.ok(userService.getSettlements(userId, lastId, size));
}
```

import 추가:
```java
import java.util.List;
import com.buddkitv2.domain.user.dto.response.SettlementHistoryResponse;
import com.buddkitv2.domain.user.dto.response.TransactionResponse;
```

- [ ] **Step 6: 컴파일 확인**

```bash
./gradlew compileJava compileTestJava
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 7: 커밋**

```bash
git add src/main/java/com/buddkitv2/domain/user/service/UserService.java \
        src/main/java/com/buddkitv2/domain/user/controller/UserController.java \
        src/test/java/com/buddkitv2/domain/user/service/UserServiceTest.java
git commit -m "feat(user): 거래/정산 내역 목록 API 구현"
```

---

### Task 12: 내 모임/관심 모임 목록 (GET /users/me/clubs, /liked-clubs)

**Files:**
- Modify: `src/main/java/com/buddkitv2/domain/user/service/UserService.java`
- Modify: `src/main/java/com/buddkitv2/domain/user/controller/UserController.java`

> **Note:** Club/ClubLike 데이터가 없는 상태에서 빈 목록을 반환하는지 확인하는 smoke 테스트로 진행한다. 실제 데이터 검증은 Club 도메인 구현 후 보완한다.

- [ ] **Step 1: 테스트 먼저 작성**

`UserServiceTest.java`에 아래를 추가한다.

```java
@Test
void 가입한_모임이_없으면_빈_목록을_반환한다() {
    UserService.RegisterResult result = userService.register(KAKAO_ID, request(), null);
    Long userId = result.getUserId();

    List<MyClubResponse> clubs = userService.getMyClubs(userId, null, 20);

    assertThat(clubs).isEmpty();
}

@Test
void 관심_모임이_없으면_빈_목록을_반환한다() {
    UserService.RegisterResult result = userService.register(KAKAO_ID, request(), null);
    Long userId = result.getUserId();

    List<LikedClubResponse> liked = userService.getLikedClubs(userId, null, 20);

    assertThat(liked).isEmpty();
}
```

import 추가:
```java
import com.buddkitv2.domain.user.dto.response.MyClubResponse;
import com.buddkitv2.domain.user.dto.response.LikedClubResponse;
```

- [ ] **Step 2: 테스트 실행 — 실패 확인**

```bash
./gradlew test --tests "com.buddkitv2.domain.user.service.UserServiceTest.가입한_모임이_없으면_빈_목록을_반환한다"
```

Expected: FAIL — `getMyClubs()` 메서드 없음

- [ ] **Step 3: UserService에 getMyClubs(), getLikedClubs() 구현**

```java
@Transactional(readOnly = true)
public List<MyClubResponse> getMyClubs(Long userId, Long lastId, int size) {
    Pageable pageable = PageRequest.of(0, size);

    List<UserClub> userClubs = lastId == null
            ? userClubRepository.findByUserIdWithClub(userId, pageable)
            : userClubRepository.findByUserIdAndLastIdWithClub(userId, lastId, pageable);

    return userClubs.stream().map(uc -> {
        var club = uc.getClub();
        var addr = club.getAddress();
        var interest = club.getInterest();
        return new MyClubResponse(
                club.getId(), club.getName(), club.getClubImage(),
                interest.getCategory(), interest.getName(),
                addr.getCity(), addr.getDistrict(),
                club.getMemberCount(), uc.getRole().name()
        );
    }).toList();
}

@Transactional(readOnly = true)
public List<LikedClubResponse> getLikedClubs(Long userId, Long lastId, int size) {
    Pageable pageable = PageRequest.of(0, size);

    List<ClubLike> clubLikes = lastId == null
            ? clubLikeRepository.findByUserIdWithClub(userId, pageable)
            : clubLikeRepository.findByUserIdAndLastIdWithClub(userId, lastId, pageable);

    return clubLikes.stream().map(cl -> {
        var club = cl.getClub();
        var addr = club.getAddress();
        var interest = club.getInterest();
        return new LikedClubResponse(
                club.getId(), club.getName(), club.getClubImage(),
                interest.getCategory(), interest.getName(),
                addr.getCity(), addr.getDistrict(),
                club.getMemberCount()
        );
    }).toList();
}
```

import 추가:
```java
import com.buddkitv2.domain.club.entity.ClubLike;
import com.buddkitv2.domain.club.entity.UserClub;
import com.buddkitv2.domain.user.dto.response.LikedClubResponse;
import com.buddkitv2.domain.user.dto.response.MyClubResponse;
```

- [ ] **Step 4: 테스트 재실행 — 통과 확인**

```bash
./gradlew test --tests "com.buddkitv2.domain.user.service.UserServiceTest.가입한_모임이_없으면_빈_목록을_반환한다"
./gradlew test --tests "com.buddkitv2.domain.user.service.UserServiceTest.관심_모임이_없으면_빈_목록을_반환한다"
```

Expected: 둘 다 PASS

- [ ] **Step 5: UserController에 엔드포인트 추가**

```java
@GetMapping("/me/clubs")
public ApiResponse<List<MyClubResponse>> getMyClubs(
        @AuthenticationPrincipal Long userId,
        @RequestParam(required = false) Long lastId,
        @RequestParam(defaultValue = "20") int size
) {
    return ApiResponse.ok(userService.getMyClubs(userId, lastId, size));
}

@GetMapping("/me/liked-clubs")
public ApiResponse<List<LikedClubResponse>> getLikedClubs(
        @AuthenticationPrincipal Long userId,
        @RequestParam(required = false) Long lastId,
        @RequestParam(defaultValue = "20") int size
) {
    return ApiResponse.ok(userService.getLikedClubs(userId, lastId, size));
}
```

import 추가:
```java
import com.buddkitv2.domain.user.dto.response.LikedClubResponse;
import com.buddkitv2.domain.user.dto.response.MyClubResponse;
```

- [ ] **Step 6: 커밋**

```bash
git add src/main/java/com/buddkitv2/domain/user/service/UserService.java \
        src/main/java/com/buddkitv2/domain/user/controller/UserController.java \
        src/test/java/com/buddkitv2/domain/user/service/UserServiceTest.java
git commit -m "feat(user): 내 모임/관심 모임 목록 API 구현"
```

---

### Task 13: FCM 토큰 (PUT/DELETE /users/me/fcm-token)

**Files:**
- Modify: `src/main/java/com/buddkitv2/domain/user/service/UserService.java`
- Modify: `src/main/java/com/buddkitv2/domain/user/controller/UserController.java`
- Modify: `src/test/java/com/buddkitv2/domain/user/service/UserServiceTest.java`

- [ ] **Step 1: 테스트 먼저 작성**

`UserServiceTest.java`에 아래를 추가한다.

```java
@Test
void FCM_토큰_등록_후_삭제_시_null이_된다() {
    UserService.RegisterResult result = userService.register(KAKAO_ID, request(), null);
    Long userId = result.getUserId();

    userService.saveFcmToken(userId, "fcm-device-token-abc");
    User afterSave = userRepository.findById(userId).orElseThrow();
    assertThat(afterSave.getFcmToken()).isEqualTo("fcm-device-token-abc");

    userService.deleteFcmToken(userId);
    User afterDelete = userRepository.findById(userId).orElseThrow();
    assertThat(afterDelete.getFcmToken()).isNull();
}
```

- [ ] **Step 2: 테스트 실행 — 실패 확인**

```bash
./gradlew test --tests "com.buddkitv2.domain.user.service.UserServiceTest.FCM_토큰_등록_후_삭제_시_null이_된다"
```

Expected: FAIL — `saveFcmToken()` 메서드 없음

- [ ] **Step 3: UserService에 saveFcmToken(), deleteFcmToken() 구현**

```java
@Transactional
public void saveFcmToken(Long userId, String token) {
    User user = userRepository.findById(userId)
            .orElseThrow(UserNotFoundException::new);
    user.updateFcmToken(token);
}

@Transactional
public void deleteFcmToken(Long userId) {
    User user = userRepository.findById(userId)
            .orElseThrow(UserNotFoundException::new);
    user.updateFcmToken(null);
}
```

- [ ] **Step 4: 테스트 재실행 — 통과 확인**

```bash
./gradlew test --tests "com.buddkitv2.domain.user.service.UserServiceTest.FCM_토큰_등록_후_삭제_시_null이_된다"
```

Expected: PASS

- [ ] **Step 5: UserController에 PUT/DELETE /users/me/fcm-token 추가**

```java
@PutMapping("/me/fcm-token")
public ApiResponse<Void> saveFcmToken(
        @AuthenticationPrincipal Long userId,
        @RequestBody @Valid FcmTokenRequest request
) {
    userService.saveFcmToken(userId, request.getToken());
    return ApiResponse.ok(null);
}

@DeleteMapping("/me/fcm-token")
public ResponseEntity<Void> deleteFcmToken(@AuthenticationPrincipal Long userId) {
    userService.deleteFcmToken(userId);
    return ResponseEntity.noContent().build();
}
```

import 추가:
```java
import com.buddkitv2.domain.user.dto.request.FcmTokenRequest;
```

- [ ] **Step 6: 전체 테스트 통과 확인**

```bash
./gradlew test --tests "com.buddkitv2.domain.user.service.UserServiceTest"
```

Expected: 모든 테스트 PASS (DB, Redis 실행 환경 필요)

- [ ] **Step 7: 전체 빌드 확인**

```bash
./gradlew build
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 8: 커밋**

```bash
git add src/main/java/com/buddkitv2/domain/user/service/UserService.java \
        src/main/java/com/buddkitv2/domain/user/controller/UserController.java \
        src/test/java/com/buddkitv2/domain/user/service/UserServiceTest.java
git commit -m "feat(user): FCM 토큰 등록/갱신/삭제 API 구현"
```
