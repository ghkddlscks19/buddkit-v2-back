# CLUB 도메인 구현 계획

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 모임 생성/수정/상세 조회/가입/탈퇴/관심 모임 등록·취소 7개 API를 구현한다.

**Architecture:** 기존 USER 도메인과 동일한 도메인 중심 패키지 구조를 따른다. `ClubService` 하나에 모든 비즈니스 로직을 담고, `ClubController`가 7개 엔드포인트를 처리한다. 모든 응답은 `ClubDetailResponse`로 통일한다.

**Tech Stack:** Java 21, Spring Boot 4.0.6, Spring Data JPA, PostgreSQL, Lombok, Bean Validation

---

## 파일 맵

| 파일 | 역할 |
|------|------|
| `global/exception/ClubNotFoundException.java` (신규) | 존재하지 않는 클럽 조회 시 |
| `global/exception/ClubAccessDeniedException.java` (신규) | 모임장 아닌 사용자가 수정 시도 시 |
| `global/exception/ClubFullException.java` (신규) | 최대 인원 초과 가입 시도 시 |
| `global/exception/AlreadyJoinedClubException.java` (신규) | 이미 가입된 모임 재가입 시도 시 |
| `global/exception/NotJoinedClubException.java` (신규) | 가입하지 않은 모임 탈퇴 시도 시 |
| `global/exception/ClubLeaderCannotLeaveException.java` (신규) | 모임장 탈퇴 시도 시 |
| `global/exception/AlreadyLikedClubException.java` (신규) | 이미 등록된 관심 모임 재등록 시도 시 |
| `global/exception/ClubLikeNotFoundException.java` (신규) | 등록하지 않은 관심 모임 취소 시도 시 |
| `global/exception/GlobalExceptionHandler.java` (수정) | 위 8개 예외 핸들러 추가 |
| `domain/club/entity/Club.java` (수정) | `update()`, `incrementMemberCount()`, `decrementMemberCount()` 추가 |
| `domain/club/repository/ClubRepository.java` (신규) | `JpaRepository<Club, Long>` |
| `domain/club/repository/UserClubRepository.java` (수정) | `findByClub_IdAndUser_Id`, `existsByClub_IdAndUser_Id` 추가 |
| `domain/club/repository/ClubLikeRepository.java` (수정) | `findByClub_IdAndUser_Id`, `existsByClub_IdAndUser_Id` 추가 |
| `domain/club/dto/request/ClubCreateRequest.java` (신규) | 모임 생성 요청 DTO |
| `domain/club/dto/request/ClubUpdateRequest.java` (신규) | 모임 수정 요청 DTO |
| `domain/club/dto/response/ClubDetailResponse.java` (신규) | 생성/수정/조회 공통 응답 DTO |
| `domain/club/service/ClubService.java` (신규) | 모든 비즈니스 로직 |
| `domain/club/controller/ClubController.java` (신규) | 7개 HTTP 엔드포인트 |
| `test/.../club/service/ClubServiceTest.java` (신규) | 서비스 통합 테스트 |

---

## Task 1: 예외 클래스 8개 + GlobalExceptionHandler 핸들러

**Files:**
- Create: `src/main/java/com/buddkitv2/global/exception/ClubNotFoundException.java`
- Create: `src/main/java/com/buddkitv2/global/exception/ClubAccessDeniedException.java`
- Create: `src/main/java/com/buddkitv2/global/exception/ClubFullException.java`
- Create: `src/main/java/com/buddkitv2/global/exception/AlreadyJoinedClubException.java`
- Create: `src/main/java/com/buddkitv2/global/exception/NotJoinedClubException.java`
- Create: `src/main/java/com/buddkitv2/global/exception/ClubLeaderCannotLeaveException.java`
- Create: `src/main/java/com/buddkitv2/global/exception/AlreadyLikedClubException.java`
- Create: `src/main/java/com/buddkitv2/global/exception/ClubLikeNotFoundException.java`
- Modify: `src/main/java/com/buddkitv2/global/exception/GlobalExceptionHandler.java`

- [ ] **Step 1: 예외 클래스 8개 생성**

```java
// ClubNotFoundException.java
package com.buddkitv2.global.exception;
public class ClubNotFoundException extends RuntimeException {
    public ClubNotFoundException() { super("존재하지 않는 모임입니다."); }
}

// ClubAccessDeniedException.java
package com.buddkitv2.global.exception;
public class ClubAccessDeniedException extends RuntimeException {
    public ClubAccessDeniedException() { super("모임장만 수행할 수 있는 작업입니다."); }
}

// ClubFullException.java
package com.buddkitv2.global.exception;
public class ClubFullException extends RuntimeException {
    public ClubFullException() { super("모임 정원이 가득 찼습니다."); }
}

// AlreadyJoinedClubException.java
package com.buddkitv2.global.exception;
public class AlreadyJoinedClubException extends RuntimeException {
    public AlreadyJoinedClubException() { super("이미 가입한 모임입니다."); }
}

// NotJoinedClubException.java
package com.buddkitv2.global.exception;
public class NotJoinedClubException extends RuntimeException {
    public NotJoinedClubException() { super("가입하지 않은 모임입니다."); }
}

// ClubLeaderCannotLeaveException.java
package com.buddkitv2.global.exception;
public class ClubLeaderCannotLeaveException extends RuntimeException {
    public ClubLeaderCannotLeaveException() { super("모임장은 탈퇴할 수 없습니다."); }
}

// AlreadyLikedClubException.java
package com.buddkitv2.global.exception;
public class AlreadyLikedClubException extends RuntimeException {
    public AlreadyLikedClubException() { super("이미 관심 모임으로 등록된 모임입니다."); }
}

// ClubLikeNotFoundException.java
package com.buddkitv2.global.exception;
public class ClubLikeNotFoundException extends RuntimeException {
    public ClubLikeNotFoundException() { super("관심 모임으로 등록되지 않은 모임입니다."); }
}
```

- [ ] **Step 2: GlobalExceptionHandler에 핸들러 8개 추가**

기존 핸들러 목록 아래에 추가:

```java
@ExceptionHandler(ClubNotFoundException.class)
public ResponseEntity<ApiResponse<Void>> handleClubNotFound(ClubNotFoundException e) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.fail(e.getMessage()));
}

@ExceptionHandler(ClubAccessDeniedException.class)
public ResponseEntity<ApiResponse<Void>> handleClubAccessDenied(ClubAccessDeniedException e) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.fail(e.getMessage()));
}

@ExceptionHandler({ClubFullException.class, AlreadyJoinedClubException.class, AlreadyLikedClubException.class})
public ResponseEntity<ApiResponse<Void>> handleClubConflict(RuntimeException e) {
    return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.fail(e.getMessage()));
}

@ExceptionHandler({NotJoinedClubException.class, ClubLeaderCannotLeaveException.class})
public ResponseEntity<ApiResponse<Void>> handleClubBadRequest(RuntimeException e) {
    return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
}

@ExceptionHandler(ClubLikeNotFoundException.class)
public ResponseEntity<ApiResponse<Void>> handleClubLikeNotFound(ClubLikeNotFoundException e) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.fail(e.getMessage()));
}
```

- [ ] **Step 3: 컴파일 확인**

```bash
./gradlew compileJava compileTestJava
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: 커밋**

```bash
git add src/main/java/com/buddkitv2/global/exception/
git commit -m "feat(club): 클럽 도메인 예외 클래스 및 핸들러 추가"
```

---

## Task 2: Club 엔티티 메서드 추가 + Repository 정의

**Files:**
- Modify: `src/main/java/com/buddkitv2/domain/club/entity/Club.java`
- Create: `src/main/java/com/buddkitv2/domain/club/repository/ClubRepository.java`
- Modify: `src/main/java/com/buddkitv2/domain/club/repository/UserClubRepository.java`
- Modify: `src/main/java/com/buddkitv2/domain/club/repository/ClubLikeRepository.java`

- [ ] **Step 1: Club.java에 메서드 3개 추가**

`Club` 클래스 내 `create()` 메서드 아래에 추가:

```java
public void update(String name, Integer userLimit, String description,
                   String clubImage, Address address, Interest interest) {
    this.name = name;
    this.userLimit = userLimit;
    this.description = description;
    this.clubImage = clubImage;
    this.address = address;
    this.interest = interest;
}

public void incrementMemberCount() {
    this.memberCount++;
}

public void decrementMemberCount() {
    this.memberCount--;
}
```

- [ ] **Step 2: ClubRepository 생성**

```java
package com.buddkitv2.domain.club.repository;

import com.buddkitv2.domain.club.entity.Club;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClubRepository extends JpaRepository<Club, Long> {
}
```

- [ ] **Step 3: UserClubRepository에 메서드 2개 추가**

기존 메서드 아래에 추가:

```java
Optional<UserClub> findByClub_IdAndUser_Id(Long clubId, Long userId);

boolean existsByClub_IdAndUser_Id(Long clubId, Long userId);
```

파일 상단에 `import java.util.Optional;` 추가.

- [ ] **Step 4: ClubLikeRepository에 메서드 2개 추가**

기존 메서드 아래에 추가:

```java
Optional<ClubLike> findByClub_IdAndUser_Id(Long clubId, Long userId);

boolean existsByClub_IdAndUser_Id(Long clubId, Long userId);
```

파일 상단에 `import java.util.Optional;` 추가.

- [ ] **Step 5: 컴파일 확인**

```bash
./gradlew compileJava compileTestJava
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 6: 커밋**

```bash
git add src/main/java/com/buddkitv2/domain/club/
git commit -m "feat(club): Club 엔티티 메서드 및 Repository 정의"
```

---

## Task 3: DTO 클래스 생성

**Files:**
- Create: `src/main/java/com/buddkitv2/domain/club/dto/request/ClubCreateRequest.java`
- Create: `src/main/java/com/buddkitv2/domain/club/dto/request/ClubUpdateRequest.java`
- Create: `src/main/java/com/buddkitv2/domain/club/dto/response/ClubDetailResponse.java`

- [ ] **Step 1: ClubCreateRequest 생성**

```java
package com.buddkitv2.domain.club.dto.request;

import com.buddkitv2.domain.user.entity.InterestCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ClubCreateRequest {

    @NotBlank
    private String name;

    @NotNull
    private Integer userLimit;

    @NotBlank
    private String description;

    private String clubImage;

    @NotBlank
    private String city;

    @NotBlank
    private String district;

    @NotNull
    private InterestCategory interestCategory;
}
```

- [ ] **Step 2: ClubUpdateRequest 생성**

```java
package com.buddkitv2.domain.club.dto.request;

import com.buddkitv2.domain.user.entity.InterestCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ClubUpdateRequest {

    @NotBlank
    private String name;

    @NotNull
    private Integer userLimit;

    @NotBlank
    private String description;

    private String clubImage;

    @NotBlank
    private String city;

    @NotBlank
    private String district;

    @NotNull
    private InterestCategory interestCategory;
}
```

- [ ] **Step 3: ClubDetailResponse 생성**

```java
package com.buddkitv2.domain.club.dto.response;

import com.buddkitv2.domain.user.entity.InterestCategory;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ClubDetailResponse {

    private Long clubId;
    private String name;
    private String description;
    private String clubImage;
    private Integer userLimit;
    private Integer memberCount;
    private String city;
    private String district;
    private InterestCategory interestCategory;
    private String interestName;
    private boolean isLiked;
    private boolean isMember;
    private String myRole;
}
```

- [ ] **Step 4: 컴파일 확인**

```bash
./gradlew compileJava compileTestJava
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: 커밋**

```bash
git add src/main/java/com/buddkitv2/domain/club/dto/
git commit -m "feat(club): 클럽 도메인 DTO 클래스 생성"
```

---

## Task 4: ClubService — 모임 생성/수정/상세 조회

**Files:**
- Create: `src/main/java/com/buddkitv2/domain/club/service/ClubService.java`
- Create: `src/test/java/com/buddkitv2/domain/club/service/ClubServiceTest.java`

- [ ] **Step 1: 실패하는 테스트 작성**

```java
package com.buddkitv2.domain.club.service;

import com.buddkitv2.domain.club.dto.request.ClubCreateRequest;
import com.buddkitv2.domain.club.dto.request.ClubUpdateRequest;
import com.buddkitv2.domain.club.dto.response.ClubDetailResponse;
import com.buddkitv2.domain.club.entity.Club;
import com.buddkitv2.domain.club.entity.UserClubRole;
import com.buddkitv2.domain.club.repository.ClubRepository;
import com.buddkitv2.domain.club.repository.UserClubRepository;
import com.buddkitv2.domain.club.repository.ClubLikeRepository;
import com.buddkitv2.domain.common.Address;
import com.buddkitv2.domain.common.AddressRepository;
import com.buddkitv2.domain.user.entity.Gender;
import com.buddkitv2.domain.user.entity.Interest;
import com.buddkitv2.domain.user.entity.InterestCategory;
import com.buddkitv2.domain.user.entity.User;
import com.buddkitv2.domain.user.repository.InterestRepository;
import com.buddkitv2.domain.user.repository.UserRepository;
import com.buddkitv2.global.exception.ClubAccessDeniedException;
import com.buddkitv2.global.exception.ClubNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class ClubServiceTest {

    @Autowired ClubService clubService;
    @Autowired UserRepository userRepository;
    @Autowired AddressRepository addressRepository;
    @Autowired InterestRepository interestRepository;
    @Autowired ClubRepository clubRepository;
    @Autowired UserClubRepository userClubRepository;
    @Autowired ClubLikeRepository clubLikeRepository;

    private User leader;
    private User other;
    private Address address;
    private Interest interest;

    @BeforeEach
    void setUp() {
        address = addressRepository.save(Address.of("서울특별시", "테스트구", 99000));
        interest = interestRepository.save(Interest.of(InterestCategory.CULTURE, "문화"));
        leader = User.register(10001L, "모임장", LocalDate.of(1990, 1, 1), Gender.MALE, address, null);
        other = User.register(10002L, "다른유저", LocalDate.of(1992, 3, 3), Gender.FEMALE, address, null);
        userRepository.save(leader);
        userRepository.save(other);
    }

    private ClubCreateRequest createRequest() {
        ClubCreateRequest req = new ClubCreateRequest();
        req.setName("테스트 모임");
        req.setUserLimit(10);
        req.setDescription("테스트 설명");
        req.setClubImage("https://s3.example.com/club.jpg");
        req.setCity("서울특별시");
        req.setDistrict("테스트구");
        req.setInterestCategory(InterestCategory.CULTURE);
        return req;
    }

    @Test
    void 모임_생성_시_Club과_UserClub_LEADER가_생성된다() {
        ClubDetailResponse response = clubService.createClub(leader.getId(), createRequest());

        assertThat(response.getName()).isEqualTo("테스트 모임");
        assertThat(response.getMemberCount()).isEqualTo(1);
        assertThat(response.isMember()).isTrue();
        assertThat(response.getMyRole()).isEqualTo("LEADER");
        assertThat(response.isLiked()).isFalse();
        assertThat(userClubRepository.findByClub_IdAndUser_Id(response.getClubId(), leader.getId()))
                .isPresent()
                .get()
                .satisfies(uc -> assertThat(uc.getRole()).isEqualTo(UserClubRole.LEADER));
    }

    @Test
    void 모임장은_모임을_수정할_수_있다() {
        ClubDetailResponse created = clubService.createClub(leader.getId(), createRequest());

        ClubUpdateRequest updateReq = new ClubUpdateRequest();
        updateReq.setName("수정된 모임명");
        updateReq.setUserLimit(5);
        updateReq.setDescription("수정된 설명");
        updateReq.setClubImage(null);
        updateReq.setCity("서울특별시");
        updateReq.setDistrict("테스트구");
        updateReq.setInterestCategory(InterestCategory.CULTURE);

        ClubDetailResponse updated = clubService.updateClub(leader.getId(), created.getClubId(), updateReq);

        assertThat(updated.getName()).isEqualTo("수정된 모임명");
        assertThat(updated.getUserLimit()).isEqualTo(5);
    }

    @Test
    void 모임장이_아닌_사용자가_수정하면_예외를_던진다() {
        ClubDetailResponse created = clubService.createClub(leader.getId(), createRequest());

        ClubUpdateRequest updateReq = new ClubUpdateRequest();
        updateReq.setName("수정");
        updateReq.setUserLimit(5);
        updateReq.setDescription("설명");
        updateReq.setClubImage(null);
        updateReq.setCity("서울특별시");
        updateReq.setDistrict("테스트구");
        updateReq.setInterestCategory(InterestCategory.CULTURE);

        assertThatThrownBy(() -> clubService.updateClub(other.getId(), created.getClubId(), updateReq))
                .isInstanceOf(ClubAccessDeniedException.class);
    }

    @Test
    void 모임_상세_조회_시_isLiked와_isMember가_정확히_반환된다() {
        ClubDetailResponse created = clubService.createClub(leader.getId(), createRequest());
        Long clubId = created.getClubId();

        ClubDetailResponse fromLeader = clubService.getClub(leader.getId(), clubId);
        assertThat(fromLeader.isMember()).isTrue();
        assertThat(fromLeader.getMyRole()).isEqualTo("LEADER");
        assertThat(fromLeader.isLiked()).isFalse();

        ClubDetailResponse fromOther = clubService.getClub(other.getId(), clubId);
        assertThat(fromOther.isMember()).isFalse();
        assertThat(fromOther.getMyRole()).isNull();
    }

    @Test
    void 존재하지_않는_모임_조회_시_예외를_던진다() {
        assertThatThrownBy(() -> clubService.getClub(leader.getId(), 99999L))
                .isInstanceOf(ClubNotFoundException.class);
    }
}
```

- [ ] **Step 2: 테스트 실패 확인 (ClubService 미존재)**

```bash
./gradlew compileTestJava 2>&1 | head -20
```

Expected: `error: cannot find symbol` — `ClubService` 클래스 없음

- [ ] **Step 3: ClubService 스켈레톤 생성 (컴파일만 통과)**

```java
package com.buddkitv2.domain.club.service;

import com.buddkitv2.domain.club.dto.request.ClubCreateRequest;
import com.buddkitv2.domain.club.dto.request.ClubUpdateRequest;
import com.buddkitv2.domain.club.dto.response.ClubDetailResponse;
import com.buddkitv2.domain.club.entity.Club;
import com.buddkitv2.domain.club.entity.ClubLike;
import com.buddkitv2.domain.club.entity.UserClub;
import com.buddkitv2.domain.club.entity.UserClubRole;
import com.buddkitv2.domain.club.repository.ClubLikeRepository;
import com.buddkitv2.domain.club.repository.ClubRepository;
import com.buddkitv2.domain.club.repository.UserClubRepository;
import com.buddkitv2.domain.common.Address;
import com.buddkitv2.domain.common.AddressRepository;
import com.buddkitv2.domain.user.entity.Interest;
import com.buddkitv2.domain.user.entity.InterestCategory;
import com.buddkitv2.domain.user.entity.User;
import com.buddkitv2.domain.user.repository.InterestRepository;
import com.buddkitv2.domain.user.repository.UserRepository;
import com.buddkitv2.global.exception.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ClubService {

    private final ClubRepository clubRepository;
    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final InterestRepository interestRepository;
    private final UserClubRepository userClubRepository;
    private final ClubLikeRepository clubLikeRepository;

    @Transactional
    public ClubDetailResponse createClub(Long userId, ClubCreateRequest request) {
        throw new UnsupportedOperationException();
    }

    @Transactional
    public ClubDetailResponse updateClub(Long userId, Long clubId, ClubUpdateRequest request) {
        throw new UnsupportedOperationException();
    }

    @Transactional(readOnly = true)
    public ClubDetailResponse getClub(Long userId, Long clubId) {
        throw new UnsupportedOperationException();
    }

    @Transactional
    public void joinClub(Long userId, Long clubId) {
        throw new UnsupportedOperationException();
    }

    @Transactional
    public void leaveClub(Long userId, Long clubId) {
        throw new UnsupportedOperationException();
    }

    @Transactional
    public void likeClub(Long userId, Long clubId) {
        throw new UnsupportedOperationException();
    }

    @Transactional
    public void unlikeClub(Long userId, Long clubId) {
        throw new UnsupportedOperationException();
    }

    private ClubDetailResponse buildDetailResponse(Club club, Long userId) {
        throw new UnsupportedOperationException();
    }
}
```

- [ ] **Step 4: 테스트 실행 — 실패 확인**

```bash
./gradlew test --tests "com.buddkitv2.domain.club.service.ClubServiceTest" 2>&1 | tail -20
```

Expected: `UnsupportedOperationException` 으로 4개 테스트 FAIL

- [ ] **Step 5: createClub, updateClub, getClub, buildDetailResponse 구현**

`ClubService.java`의 스켈레톤 메서드를 아래 내용으로 교체:

```java
private Interest findInterest(InterestCategory category) {
    return interestRepository.findByCategoryIn(List.of(category))
            .stream().findFirst()
            .orElseThrow(InvalidInterestException::new);
}

private Address findAddress(String city, String district) {
    return addressRepository.findByCityAndDistrict(city, district)
            .orElseThrow(InvalidAddressException::new);
}

private ClubDetailResponse buildDetailResponse(Club club, Long userId) {
    boolean isLiked = clubLikeRepository.existsByClub_IdAndUser_Id(club.getId(), userId);
    Optional<UserClub> userClub = userClubRepository.findByClub_IdAndUser_Id(club.getId(), userId);
    boolean isMember = userClub.isPresent();
    String myRole = userClub.map(uc -> uc.getRole().name()).orElse(null);
    Address address = club.getAddress();
    Interest interest = club.getInterest();
    return new ClubDetailResponse(
            club.getId(), club.getName(), club.getDescription(), club.getClubImage(),
            club.getUserLimit(), club.getMemberCount(),
            address.getCity(), address.getDistrict(),
            interest.getCategory(), interest.getName(),
            isLiked, isMember, myRole
    );
}

@Transactional
public ClubDetailResponse createClub(Long userId, ClubCreateRequest request) {
    User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
    Address address = findAddress(request.getCity(), request.getDistrict());
    Interest interest = findInterest(request.getInterestCategory());
    Club club = Club.create(request.getName(), request.getUserLimit(), request.getDescription(),
            request.getClubImage(), address, interest);
    clubRepository.save(club);
    userClubRepository.save(UserClub.create(club, user, UserClubRole.LEADER));
    return buildDetailResponse(club, userId);
}

@Transactional
public ClubDetailResponse updateClub(Long userId, Long clubId, ClubUpdateRequest request) {
    Club club = clubRepository.findById(clubId).orElseThrow(ClubNotFoundException::new);
    userClubRepository.findByClub_IdAndUser_Id(clubId, userId)
            .filter(uc -> uc.getRole() == UserClubRole.LEADER)
            .orElseThrow(ClubAccessDeniedException::new);
    Address address = findAddress(request.getCity(), request.getDistrict());
    Interest interest = findInterest(request.getInterestCategory());
    club.update(request.getName(), request.getUserLimit(), request.getDescription(),
            request.getClubImage(), address, interest);
    return buildDetailResponse(club, userId);
}

@Transactional(readOnly = true)
public ClubDetailResponse getClub(Long userId, Long clubId) {
    Club club = clubRepository.findById(clubId).orElseThrow(ClubNotFoundException::new);
    return buildDetailResponse(club, userId);
}
```

- [ ] **Step 6: 테스트 실행 — 통과 확인**

```bash
./gradlew test --tests "com.buddkitv2.domain.club.service.ClubServiceTest" 2>&1 | tail -10
```

Expected: `4 tests completed, 0 failed`

- [ ] **Step 7: 커밋**

```bash
git add src/main/java/com/buddkitv2/domain/club/service/ClubService.java \
        src/test/java/com/buddkitv2/domain/club/service/ClubServiceTest.java
git commit -m "feat(club): 모임 생성/수정/상세 조회 서비스 구현"
```

---

## Task 5: ClubService — 모임 가입/탈퇴

**Files:**
- Modify: `src/main/java/com/buddkitv2/domain/club/service/ClubService.java`
- Modify: `src/test/java/com/buddkitv2/domain/club/service/ClubServiceTest.java`

- [ ] **Step 1: ClubServiceTest에 가입/탈퇴 테스트 추가**

기존 테스트 클래스 내에 아래 테스트 메서드 추가:

```java
@Test
void 모임_가입_시_memberCount가_증가하고_MEMBER로_등록된다() {
    ClubDetailResponse created = clubService.createClub(leader.getId(), createRequest());
    Long clubId = created.getClubId();

    clubService.joinClub(other.getId(), clubId);

    Club club = clubRepository.findById(clubId).orElseThrow();
    assertThat(club.getMemberCount()).isEqualTo(2);
    assertThat(userClubRepository.findByClub_IdAndUser_Id(clubId, other.getId()))
            .isPresent()
            .get()
            .satisfies(uc -> assertThat(uc.getRole()).isEqualTo(UserClubRole.MEMBER));
}

@Test
void 이미_가입한_모임에_재가입_시_예외를_던진다() {
    ClubDetailResponse created = clubService.createClub(leader.getId(), createRequest());
    Long clubId = created.getClubId();

    assertThatThrownBy(() -> clubService.joinClub(leader.getId(), clubId))
            .isInstanceOf(AlreadyJoinedClubException.class);
}

@Test
void 정원이_가득_찬_모임_가입_시_예외를_던진다() {
    ClubCreateRequest req = createRequest();
    req.setUserLimit(1);
    ClubDetailResponse created = clubService.createClub(leader.getId(), req);
    Long clubId = created.getClubId();

    assertThatThrownBy(() -> clubService.joinClub(other.getId(), clubId))
            .isInstanceOf(ClubFullException.class);
}

@Test
void 모임_탈퇴_시_memberCount가_감소하고_UserClub이_삭제된다() {
    ClubDetailResponse created = clubService.createClub(leader.getId(), createRequest());
    Long clubId = created.getClubId();
    clubService.joinClub(other.getId(), clubId);

    clubService.leaveClub(other.getId(), clubId);

    Club club = clubRepository.findById(clubId).orElseThrow();
    assertThat(club.getMemberCount()).isEqualTo(1);
    assertThat(userClubRepository.existsByClub_IdAndUser_Id(clubId, other.getId())).isFalse();
}

@Test
void 모임장은_탈퇴할_수_없다() {
    ClubDetailResponse created = clubService.createClub(leader.getId(), createRequest());
    Long clubId = created.getClubId();

    assertThatThrownBy(() -> clubService.leaveClub(leader.getId(), clubId))
            .isInstanceOf(ClubLeaderCannotLeaveException.class);
}

@Test
void 가입하지_않은_모임_탈퇴_시_예외를_던진다() {
    ClubDetailResponse created = clubService.createClub(leader.getId(), createRequest());
    Long clubId = created.getClubId();

    assertThatThrownBy(() -> clubService.leaveClub(other.getId(), clubId))
            .isInstanceOf(NotJoinedClubException.class);
}
```

추가로 이미 import 되지 않은 예외 import 추가:
```java
import com.buddkitv2.global.exception.AlreadyJoinedClubException;
import com.buddkitv2.global.exception.ClubFullException;
import com.buddkitv2.global.exception.ClubLeaderCannotLeaveException;
import com.buddkitv2.global.exception.NotJoinedClubException;
```

- [ ] **Step 2: 테스트 실행 — 실패 확인**

```bash
./gradlew test --tests "com.buddkitv2.domain.club.service.ClubServiceTest" 2>&1 | tail -15
```

Expected: 가입/탈퇴 6개 테스트 `UnsupportedOperationException` FAIL

- [ ] **Step 3: joinClub, leaveClub 구현**

`ClubService.java`의 `joinClub`, `leaveClub` 스켈레톤을 교체:

```java
@Transactional
public void joinClub(Long userId, Long clubId) {
    Club club = clubRepository.findById(clubId).orElseThrow(ClubNotFoundException::new);
    if (userClubRepository.existsByClub_IdAndUser_Id(clubId, userId)) {
        throw new AlreadyJoinedClubException();
    }
    if (club.getMemberCount() >= club.getUserLimit()) {
        throw new ClubFullException();
    }
    User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
    userClubRepository.save(UserClub.create(club, user, UserClubRole.MEMBER));
    club.incrementMemberCount();
}

@Transactional
public void leaveClub(Long userId, Long clubId) {
    Club club = clubRepository.findById(clubId).orElseThrow(ClubNotFoundException::new);
    UserClub userClub = userClubRepository.findByClub_IdAndUser_Id(clubId, userId)
            .orElseThrow(NotJoinedClubException::new);
    if (userClub.getRole() == UserClubRole.LEADER) {
        throw new ClubLeaderCannotLeaveException();
    }
    userClubRepository.delete(userClub);
    club.decrementMemberCount();
}
```

- [ ] **Step 4: 테스트 실행 — 통과 확인**

```bash
./gradlew test --tests "com.buddkitv2.domain.club.service.ClubServiceTest" 2>&1 | tail -10
```

Expected: `10 tests completed, 0 failed`

- [ ] **Step 5: 커밋**

```bash
git add src/main/java/com/buddkitv2/domain/club/service/ClubService.java \
        src/test/java/com/buddkitv2/domain/club/service/ClubServiceTest.java
git commit -m "feat(club): 모임 가입/탈퇴 서비스 구현"
```

---

## Task 6: ClubService — 관심 모임 등록/취소

**Files:**
- Modify: `src/main/java/com/buddkitv2/domain/club/service/ClubService.java`
- Modify: `src/test/java/com/buddkitv2/domain/club/service/ClubServiceTest.java`

- [ ] **Step 1: ClubServiceTest에 관심 모임 테스트 추가**

기존 테스트 클래스 내에 추가:

```java
@Test
void 관심_모임_등록_시_ClubLike가_생성된다() {
    ClubDetailResponse created = clubService.createClub(leader.getId(), createRequest());
    Long clubId = created.getClubId();

    clubService.likeClub(other.getId(), clubId);

    assertThat(clubLikeRepository.existsByClub_IdAndUser_Id(clubId, other.getId())).isTrue();
}

@Test
void 이미_등록된_관심_모임_재등록_시_예외를_던진다() {
    ClubDetailResponse created = clubService.createClub(leader.getId(), createRequest());
    Long clubId = created.getClubId();
    clubService.likeClub(other.getId(), clubId);

    assertThatThrownBy(() -> clubService.likeClub(other.getId(), clubId))
            .isInstanceOf(AlreadyLikedClubException.class);
}

@Test
void 관심_모임_취소_시_ClubLike가_삭제된다() {
    ClubDetailResponse created = clubService.createClub(leader.getId(), createRequest());
    Long clubId = created.getClubId();
    clubService.likeClub(other.getId(), clubId);

    clubService.unlikeClub(other.getId(), clubId);

    assertThat(clubLikeRepository.existsByClub_IdAndUser_Id(clubId, other.getId())).isFalse();
}

@Test
void 등록하지_않은_관심_모임_취소_시_예외를_던진다() {
    ClubDetailResponse created = clubService.createClub(leader.getId(), createRequest());
    Long clubId = created.getClubId();

    assertThatThrownBy(() -> clubService.unlikeClub(other.getId(), clubId))
            .isInstanceOf(ClubLikeNotFoundException.class);
}
```

추가 import:
```java
import com.buddkitv2.global.exception.AlreadyLikedClubException;
import com.buddkitv2.global.exception.ClubLikeNotFoundException;
```

- [ ] **Step 2: 테스트 실행 — 실패 확인**

```bash
./gradlew test --tests "com.buddkitv2.domain.club.service.ClubServiceTest" 2>&1 | tail -15
```

Expected: 관심 모임 4개 테스트 `UnsupportedOperationException` FAIL

- [ ] **Step 3: likeClub, unlikeClub 구현**

`ClubService.java`의 `likeClub`, `unlikeClub` 스켈레톤을 교체:

```java
@Transactional
public void likeClub(Long userId, Long clubId) {
    Club club = clubRepository.findById(clubId).orElseThrow(ClubNotFoundException::new);
    if (clubLikeRepository.existsByClub_IdAndUser_Id(clubId, userId)) {
        throw new AlreadyLikedClubException();
    }
    User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
    clubLikeRepository.save(ClubLike.create(user, club));
}

@Transactional
public void unlikeClub(Long userId, Long clubId) {
    ClubLike clubLike = clubLikeRepository.findByClub_IdAndUser_Id(clubId, userId)
            .orElseThrow(ClubLikeNotFoundException::new);
    clubLikeRepository.delete(clubLike);
}
```

- [ ] **Step 4: 전체 테스트 실행 — 통과 확인**

```bash
./gradlew test --tests "com.buddkitv2.domain.club.service.ClubServiceTest" 2>&1 | tail -10
```

Expected: `14 tests completed, 0 failed`

- [ ] **Step 5: 커밋**

```bash
git add src/main/java/com/buddkitv2/domain/club/service/ClubService.java \
        src/test/java/com/buddkitv2/domain/club/service/ClubServiceTest.java
git commit -m "feat(club): 관심 모임 등록/취소 서비스 구현"
```

---

## Task 7: ClubController

**Files:**
- Create: `src/main/java/com/buddkitv2/domain/club/controller/ClubController.java`

- [ ] **Step 1: ClubController 생성**

```java
package com.buddkitv2.domain.club.controller;

import com.buddkitv2.domain.club.dto.request.ClubCreateRequest;
import com.buddkitv2.domain.club.dto.request.ClubUpdateRequest;
import com.buddkitv2.domain.club.dto.response.ClubDetailResponse;
import com.buddkitv2.domain.club.service.ClubService;
import com.buddkitv2.global.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/clubs")
@RequiredArgsConstructor
public class ClubController {

    private final ClubService clubService;

    @PostMapping
    public ApiResponse<ClubDetailResponse> createClub(
            @AuthenticationPrincipal Long userId,
            @RequestBody @Valid ClubCreateRequest request
    ) {
        return ApiResponse.ok(clubService.createClub(userId, request));
    }

    @PatchMapping("/{clubId}")
    public ApiResponse<ClubDetailResponse> updateClub(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long clubId,
            @RequestBody @Valid ClubUpdateRequest request
    ) {
        return ApiResponse.ok(clubService.updateClub(userId, clubId, request));
    }

    @GetMapping("/{clubId}")
    public ApiResponse<ClubDetailResponse> getClub(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long clubId
    ) {
        return ApiResponse.ok(clubService.getClub(userId, clubId));
    }

    @PostMapping("/{clubId}/members")
    public ResponseEntity<Void> joinClub(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long clubId
    ) {
        clubService.joinClub(userId, clubId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{clubId}/members/me")
    public ResponseEntity<Void> leaveClub(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long clubId
    ) {
        clubService.leaveClub(userId, clubId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{clubId}/like")
    public ResponseEntity<Void> likeClub(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long clubId
    ) {
        clubService.likeClub(userId, clubId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{clubId}/like")
    public ResponseEntity<Void> unlikeClub(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long clubId
    ) {
        clubService.unlikeClub(userId, clubId);
        return ResponseEntity.noContent().build();
    }
}
```

- [ ] **Step 2: 빌드 확인**

```bash
./gradlew build 2>&1 | tail -10
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: 커밋**

```bash
git add src/main/java/com/buddkitv2/domain/club/controller/ClubController.java
git commit -m "feat(club): 모임 컨트롤러 구현 (7개 엔드포인트)"
```
