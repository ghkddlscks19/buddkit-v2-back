# Schedule API Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 모임(Club) 내 정모(Schedule) CRUD·참여·정산 API 전체 구현

**Architecture:** ScheduleService(CRUD·참여) + SettlementService(정산·배치)를 분리하고, ScheduleController 하나에서 두 서비스를 주입한다. URL은 `/clubs/{clubId}/schedules/{scheduleId}/...` 전체 중첩 구조. 정산은 포인트 즉시 차감(즉시 정산)과 자정 배치(예약 정산) 두 경로를 지원한다.

**Tech Stack:** Java 21, Spring Boot 4.0.6, Spring Data JPA, PostgreSQL, `@SpringBootTest` 통합 테스트(실제 DB 필수)

---

## 파일 맵

| 구분 | 경로 |
|---|---|
| **Modify** | `domain/common/BaseEntity.java` |
| **Modify** | `domain/schedule/entity/Schedule.java` |
| **Modify** | `domain/settlement/entity/Settlement.java` |
| **Modify** | `domain/settlement/entity/UserSettlement.java` |
| **Modify** | `domain/wallet/entity/Wallet.java` |
| **Modify** | `domain/settlement/repository/UserSettlementRepository.java` |
| **Modify** | `global/exception/GlobalExceptionHandler.java` |
| **Create** | `global/exception/` — 예외 클래스 10개 |
| **Create** | `domain/schedule/repository/ScheduleRepository.java` |
| **Create** | `domain/schedule/repository/UserScheduleRepository.java` |
| **Create** | `domain/settlement/repository/SettlementRepository.java` |
| **Create** | `domain/schedule/dto/request/ScheduleCreateRequest.java` |
| **Create** | `domain/schedule/dto/request/ScheduleUpdateRequest.java` |
| **Create** | `domain/schedule/dto/response/ScheduleResponse.java` |
| **Create** | `domain/schedule/dto/response/ScheduleMemberResponse.java` |
| **Create** | `domain/settlement/dto/response/SettlementStatusResponse.java` |
| **Create** | `domain/schedule/service/ScheduleService.java` |
| **Create** | `domain/settlement/service/SettlementService.java` |
| **Create** | `domain/settlement/service/SettlementBatchService.java` |
| **Create** | `global/config/SchedulingConfig.java` |
| **Create** | `domain/schedule/controller/ScheduleController.java` |
| **Create** | `domain/schedule/service/ScheduleServiceTest.java` (test) |
| **Create** | `domain/settlement/service/SettlementServiceTest.java` (test) |

---

## Task 1: 엔티티 메서드 추가

**Files:**
- Modify: `src/main/java/com/buddkitv2/domain/common/BaseEntity.java`
- Modify: `src/main/java/com/buddkitv2/domain/schedule/entity/Schedule.java`
- Modify: `src/main/java/com/buddkitv2/domain/settlement/entity/Settlement.java`
- Modify: `src/main/java/com/buddkitv2/domain/settlement/entity/UserSettlement.java`
- Modify: `src/main/java/com/buddkitv2/domain/wallet/entity/Wallet.java`

- [ ] **Step 1: BaseEntity에 softDelete() 추가**

`BaseEntity` 마지막에 추가:
```java
public void softDelete() {
    this.deletedAt = java.time.LocalDateTime.now();
}
```

- [ ] **Step 2: Schedule에 update(), changeStatus() 추가**

`Schedule` 클래스에 추가:
```java
public void update(String name, LocalDateTime scheduleTime, String location, Long cost, Integer limit) {
    this.name = name;
    this.scheduleTime = scheduleTime;
    this.location = location;
    this.cost = cost;
    this.limit = limit;
}

public void changeStatus(ScheduleStatus status) {
    this.status = status;
}
```

- [ ] **Step 3: Settlement에 changeStatus() 추가**

`Settlement` 클래스에 추가:
```java
public void changeStatus(SettlementStatus status) {
    this.status = status;
}

public void complete() {
    this.status = SettlementStatus.COMPLETED;
    this.completedTime = java.time.LocalDateTime.now();
}
```

- [ ] **Step 4: UserSettlement에 complete(), reserve(), rollback() 추가**

`UserSettlement` 클래스에 추가:
```java
public void complete() {
    this.status = UserSettlementStatus.COMPLETED;
    this.completedTime = java.time.LocalDateTime.now();
}

public void reserve() {
    this.status = UserSettlementStatus.PENDING_CONFIRMATION;
}

public void rollback() {
    this.status = UserSettlementStatus.REQUESTED;
}
```

- [ ] **Step 5: Wallet에 debit() 추가**

`Wallet` 클래스에 추가:
```java
public void debit(Long amount) {
    this.balance -= amount;
}
```

- [ ] **Step 6: 컴파일 확인**

```bash
./gradlew compileJava
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 7: 커밋**

```bash
git add src/main/java/com/buddkitv2/domain/common/BaseEntity.java \
        src/main/java/com/buddkitv2/domain/schedule/entity/Schedule.java \
        src/main/java/com/buddkitv2/domain/settlement/entity/Settlement.java \
        src/main/java/com/buddkitv2/domain/settlement/entity/UserSettlement.java \
        src/main/java/com/buddkitv2/domain/wallet/entity/Wallet.java
git commit -m "feat(schedule): 스케줄·정산·지갑 엔티티 메서드 추가"
```

---

## Task 2: 예외 클래스 + GlobalExceptionHandler

**Files:**
- Create: `src/main/java/com/buddkitv2/global/exception/` — 10개 파일
- Modify: `src/main/java/com/buddkitv2/global/exception/GlobalExceptionHandler.java`

- [ ] **Step 1: 예외 클래스 10개 생성**

아래 10개를 각각 별도 파일로 생성한다. 패키지는 `com.buddkitv2.global.exception`.

```java
// ScheduleNotFoundException.java
public class ScheduleNotFoundException extends RuntimeException {
    public ScheduleNotFoundException() { super("존재하지 않는 스케줄입니다."); }
}

// ScheduleAccessDeniedException.java
public class ScheduleAccessDeniedException extends RuntimeException {
    public ScheduleAccessDeniedException() { super("스케줄에 대한 권한이 없습니다."); }
}

// AlreadyJoinedScheduleException.java
public class AlreadyJoinedScheduleException extends RuntimeException {
    public AlreadyJoinedScheduleException() { super("이미 참여한 스케줄입니다."); }
}

// NotJoinedScheduleException.java
public class NotJoinedScheduleException extends RuntimeException {
    public NotJoinedScheduleException() { super("참여하지 않은 스케줄입니다."); }
}

// ScheduleNotRecruitingException.java
public class ScheduleNotRecruitingException extends RuntimeException {
    public ScheduleNotRecruitingException() { super("모집 중인 스케줄이 아닙니다."); }
}

// ScheduleFullException.java
public class ScheduleFullException extends RuntimeException {
    public ScheduleFullException() { super("스케줄 정원이 가득 찼습니다."); }
}

// ScheduleAlreadySettlingException.java
public class ScheduleAlreadySettlingException extends RuntimeException {
    public ScheduleAlreadySettlingException() { super("이미 정산이 진행 중인 스케줄입니다."); }
}

// SettlementNotFoundException.java
public class SettlementNotFoundException extends RuntimeException {
    public SettlementNotFoundException() { super("존재하지 않는 정산입니다."); }
}

// AlreadySettledException.java
public class AlreadySettledException extends RuntimeException {
    public AlreadySettledException() { super("이미 정산이 완료된 항목입니다."); }
}

// InsufficientBalanceException.java
public class InsufficientBalanceException extends RuntimeException {
    public InsufficientBalanceException() { super("포인트 잔액이 부족합니다."); }
}
```

- [ ] **Step 2: GlobalExceptionHandler에 핸들러 추가**

`GlobalExceptionHandler`의 `handleException(Exception e)` 핸들러 바로 위에 아래를 삽입:

```java
@ExceptionHandler(ScheduleNotFoundException.class)
public ResponseEntity<ApiResponse<Void>> handleScheduleNotFound(ScheduleNotFoundException e) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.fail(e.getMessage()));
}

@ExceptionHandler(ScheduleAccessDeniedException.class)
public ResponseEntity<ApiResponse<Void>> handleScheduleAccessDenied(ScheduleAccessDeniedException e) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.fail(e.getMessage()));
}

@ExceptionHandler({AlreadyJoinedScheduleException.class, ScheduleAlreadySettlingException.class, AlreadySettledException.class})
public ResponseEntity<ApiResponse<Void>> handleScheduleConflict(RuntimeException e) {
    return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.fail(e.getMessage()));
}

@ExceptionHandler({NotJoinedScheduleException.class, ScheduleNotRecruitingException.class, ScheduleFullException.class})
public ResponseEntity<ApiResponse<Void>> handleScheduleBadRequest(RuntimeException e) {
    return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
}

@ExceptionHandler(SettlementNotFoundException.class)
public ResponseEntity<ApiResponse<Void>> handleSettlementNotFound(SettlementNotFoundException e) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.fail(e.getMessage()));
}

@ExceptionHandler(InsufficientBalanceException.class)
public ResponseEntity<ApiResponse<Void>> handleInsufficientBalance(InsufficientBalanceException e) {
    return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(ApiResponse.fail(e.getMessage()));
}
```

- [ ] **Step 3: 컴파일 확인**

```bash
./gradlew compileJava
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: 커밋**

```bash
git add src/main/java/com/buddkitv2/global/exception/
git commit -m "feat(schedule): 스케줄·정산 예외 클래스 및 핸들러 추가"
```

---

## Task 3: Repository 인터페이스

**Files:**
- Create: `src/main/java/com/buddkitv2/domain/schedule/repository/ScheduleRepository.java`
- Create: `src/main/java/com/buddkitv2/domain/schedule/repository/UserScheduleRepository.java`
- Create: `src/main/java/com/buddkitv2/domain/settlement/repository/SettlementRepository.java`
- Modify: `src/main/java/com/buddkitv2/domain/settlement/repository/UserSettlementRepository.java`

- [ ] **Step 1: ScheduleRepository 생성**

```java
package com.buddkitv2.domain.schedule.repository;

import com.buddkitv2.domain.schedule.entity.Schedule;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {

    @Query("SELECT s FROM Schedule s WHERE s.id = :id AND s.deletedAt IS NULL")
    Optional<Schedule> findActiveById(@Param("id") Long id);

    @Query("SELECT s FROM Schedule s WHERE s.club.id = :clubId AND s.deletedAt IS NULL ORDER BY s.id DESC")
    List<Schedule> findByClubId(@Param("clubId") Long clubId, Pageable pageable);

    @Query("SELECT s FROM Schedule s WHERE s.club.id = :clubId AND s.id < :lastId AND s.deletedAt IS NULL ORDER BY s.id DESC")
    List<Schedule> findByClubIdAndLastId(@Param("clubId") Long clubId, @Param("lastId") Long lastId, Pageable pageable);
}
```

- [ ] **Step 2: UserScheduleRepository 생성**

```java
package com.buddkitv2.domain.schedule.repository;

import com.buddkitv2.domain.schedule.entity.UserSchedule;
import com.buddkitv2.domain.schedule.entity.UserScheduleRole;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserScheduleRepository extends JpaRepository<UserSchedule, Long> {

    Optional<UserSchedule> findBySchedule_IdAndUser_Id(Long scheduleId, Long userId);

    boolean existsBySchedule_IdAndUser_Id(Long scheduleId, Long userId);

    long countBySchedule_Id(Long scheduleId);

    List<UserSchedule> findBySchedule_IdAndRole(Long scheduleId, UserScheduleRole role);

    @Query("SELECT us FROM UserSchedule us JOIN FETCH us.user WHERE us.schedule.id = :scheduleId ORDER BY us.id ASC")
    List<UserSchedule> findByScheduleId(@Param("scheduleId") Long scheduleId, Pageable pageable);

    @Query("SELECT us FROM UserSchedule us JOIN FETCH us.user WHERE us.schedule.id = :scheduleId AND us.id > :lastId ORDER BY us.id ASC")
    List<UserSchedule> findByScheduleIdAndLastId(@Param("scheduleId") Long scheduleId, @Param("lastId") Long lastId, Pageable pageable);
}
```

- [ ] **Step 3: SettlementRepository 생성**

```java
package com.buddkitv2.domain.settlement.repository;

import com.buddkitv2.domain.settlement.entity.Settlement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SettlementRepository extends JpaRepository<Settlement, Long> {

    Optional<Settlement> findBySchedule_Id(Long scheduleId);

    boolean existsBySchedule_Id(Long scheduleId);
}
```

- [ ] **Step 4: UserSettlementRepository에 메서드 추가**

기존 파일의 기존 메서드를 유지하면서 아래를 추가:

```java
    Optional<UserSettlement> findBySettlement_IdAndUser_Id(Long settlementId, Long userId);

    long countBySettlement_Id(Long settlementId);

    long countBySettlement_IdAndStatus(Long settlementId, UserSettlementStatus status);

    @Query("SELECT us FROM UserSettlement us JOIN FETCH us.user WHERE us.settlement.id = :settlementId ORDER BY us.id ASC")
    List<UserSettlement> findBySettlementId(@Param("settlementId") Long settlementId, Pageable pageable);

    @Query("SELECT us FROM UserSettlement us JOIN FETCH us.user WHERE us.settlement.id = :settlementId AND us.id > :lastId ORDER BY us.id ASC")
    List<UserSettlement> findBySettlementIdAndLastId(@Param("settlementId") Long settlementId, @Param("lastId") Long lastId, Pageable pageable);

    List<UserSettlement> findByStatus(UserSettlementStatus status);
```

`UserSettlementStatus` import 추가: `import com.buddkitv2.domain.settlement.entity.UserSettlementStatus;`

- [ ] **Step 5: 컴파일 확인**

```bash
./gradlew compileJava
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: 커밋**

```bash
git add src/main/java/com/buddkitv2/domain/schedule/repository/ \
        src/main/java/com/buddkitv2/domain/settlement/repository/
git commit -m "feat(schedule): 스케줄·정산 레포지토리 생성"
```

---

## Task 4: DTO

**Files:**
- Create: `src/main/java/com/buddkitv2/domain/schedule/dto/request/ScheduleCreateRequest.java`
- Create: `src/main/java/com/buddkitv2/domain/schedule/dto/request/ScheduleUpdateRequest.java`
- Create: `src/main/java/com/buddkitv2/domain/schedule/dto/response/ScheduleResponse.java`
- Create: `src/main/java/com/buddkitv2/domain/schedule/dto/response/ScheduleMemberResponse.java`
- Create: `src/main/java/com/buddkitv2/domain/settlement/dto/response/SettlementStatusResponse.java`

- [ ] **Step 1: ScheduleCreateRequest**

```java
package com.buddkitv2.domain.schedule.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor
public class ScheduleCreateRequest {

    @NotBlank
    private String name;

    @NotNull
    private LocalDateTime scheduleTime;

    @NotBlank
    private String location;

    @NotNull @Min(0)
    private Long cost;

    @NotNull @Min(1)
    private Integer limit;
}
```

- [ ] **Step 2: ScheduleUpdateRequest**

```java
package com.buddkitv2.domain.schedule.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor
public class ScheduleUpdateRequest {

    @NotBlank
    private String name;

    @NotNull
    private LocalDateTime scheduleTime;

    @NotBlank
    private String location;

    @NotNull @Min(0)
    private Long cost;

    @NotNull @Min(1)
    private Integer limit;
}
```

- [ ] **Step 3: ScheduleResponse**

```java
package com.buddkitv2.domain.schedule.dto.response;

import com.buddkitv2.domain.schedule.entity.ScheduleStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter @AllArgsConstructor
public class ScheduleResponse {
    private Long scheduleId;
    private String name;
    private LocalDateTime scheduleTime;
    private String location;
    private Long cost;
    private ScheduleStatus status;
    private Integer limit;
    private long participantCount;
    private boolean isJoined;
}
```

- [ ] **Step 4: ScheduleMemberResponse**

```java
package com.buddkitv2.domain.schedule.dto.response;

import com.buddkitv2.domain.schedule.entity.UserScheduleRole;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter @AllArgsConstructor
public class ScheduleMemberResponse {
    private Long userScheduleId;
    private Long userId;
    private String nickname;
    private String profileImageUrl;
    private UserScheduleRole role;
}
```

- [ ] **Step 5: SettlementStatusResponse**

```java
package com.buddkitv2.domain.settlement.dto.response;

import com.buddkitv2.domain.settlement.entity.UserSettlementStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter @AllArgsConstructor
public class SettlementStatusResponse {
    private Long userSettlementId;
    private Long userId;
    private String nickname;
    private UserSettlementStatus status;
    private LocalDateTime completedTime;
    private Long amount;
}
```

- [ ] **Step 6: 컴파일 확인**

```bash
./gradlew compileJava
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 7: 커밋**

```bash
git add src/main/java/com/buddkitv2/domain/schedule/dto/ \
        src/main/java/com/buddkitv2/domain/settlement/dto/
git commit -m "feat(schedule): 스케줄·정산 DTO 생성"
```

---

## Task 5: ScheduleService 구현

**Files:**
- Create: `src/main/java/com/buddkitv2/domain/schedule/service/ScheduleService.java`

- [ ] **Step 1: ScheduleService 골격 작성**

```java
package com.buddkitv2.domain.schedule.service;

import com.buddkitv2.domain.club.entity.UserClub;
import com.buddkitv2.domain.club.entity.UserClubRole;
import com.buddkitv2.domain.club.repository.UserClubRepository;
import com.buddkitv2.domain.schedule.dto.request.ScheduleCreateRequest;
import com.buddkitv2.domain.schedule.dto.request.ScheduleUpdateRequest;
import com.buddkitv2.domain.schedule.dto.response.ScheduleMemberResponse;
import com.buddkitv2.domain.schedule.dto.response.ScheduleResponse;
import com.buddkitv2.domain.schedule.entity.Schedule;
import com.buddkitv2.domain.schedule.entity.ScheduleStatus;
import com.buddkitv2.domain.schedule.entity.UserSchedule;
import com.buddkitv2.domain.schedule.entity.UserScheduleRole;
import com.buddkitv2.domain.schedule.repository.ScheduleRepository;
import com.buddkitv2.domain.schedule.repository.UserScheduleRepository;
import com.buddkitv2.domain.user.entity.User;
import com.buddkitv2.domain.user.repository.UserRepository;
import com.buddkitv2.global.exception.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final UserScheduleRepository userScheduleRepository;
    private final UserClubRepository userClubRepository;
    private final UserRepository userRepository;

    // ── 헬퍼 ──────────────────────────────────────────────

    private UserClub requireClubMember(Long clubId, Long userId) {
        return userClubRepository.findByClub_IdAndUser_Id(clubId, userId)
                .orElseThrow(ScheduleAccessDeniedException::new);
    }

    private void requireLeader(UserClub userClub) {
        if (userClub.getRole() != UserClubRole.LEADER) {
            throw new ScheduleAccessDeniedException();
        }
    }

    private Schedule findActiveSchedule(Long clubId, Long scheduleId) {
        Schedule schedule = scheduleRepository.findActiveById(scheduleId)
                .orElseThrow(ScheduleNotFoundException::new);
        if (!schedule.getClub().getId().equals(clubId)) {
            throw new ScheduleNotFoundException();
        }
        return schedule;
    }

    private ScheduleResponse toResponse(Schedule schedule, Long userId) {
        long count = userScheduleRepository.countBySchedule_Id(schedule.getId());
        boolean joined = userScheduleRepository.existsBySchedule_IdAndUser_Id(schedule.getId(), userId);
        return new ScheduleResponse(
                schedule.getId(), schedule.getName(), schedule.getScheduleTime(),
                schedule.getLocation(), schedule.getCost(), schedule.getStatus(),
                schedule.getLimit(), count, joined
        );
    }

    // ── CRUD ──────────────────────────────────────────────

    @Transactional
    public ScheduleResponse createSchedule(Long userId, Long clubId, ScheduleCreateRequest req) {
        UserClub userClub = requireClubMember(clubId, userId);
        requireLeader(userClub);
        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
        Schedule schedule = Schedule.create(req.getName(), req.getScheduleTime(),
                req.getLocation(), req.getCost(), req.getLimit(), userClub.getClub());
        scheduleRepository.save(schedule);
        userScheduleRepository.save(UserSchedule.create(user, schedule, UserScheduleRole.LEADER));
        return toResponse(schedule, userId);
    }

    @Transactional
    public ScheduleResponse updateSchedule(Long userId, Long clubId, Long scheduleId, ScheduleUpdateRequest req) {
        UserClub userClub = requireClubMember(clubId, userId);
        requireLeader(userClub);
        Schedule schedule = findActiveSchedule(clubId, scheduleId);
        schedule.update(req.getName(), req.getScheduleTime(), req.getLocation(), req.getCost(), req.getLimit());
        return toResponse(schedule, userId);
    }

    @Transactional
    public void deleteSchedule(Long userId, Long clubId, Long scheduleId) {
        UserClub userClub = requireClubMember(clubId, userId);
        requireLeader(userClub);
        Schedule schedule = findActiveSchedule(clubId, scheduleId);
        schedule.softDelete();
    }

    // ── 조회 ──────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ScheduleResponse> getSchedules(Long userId, Long clubId, Long lastId, int size) {
        requireClubMember(clubId, userId);
        List<Schedule> schedules = lastId == null
                ? scheduleRepository.findByClubId(clubId, PageRequest.of(0, size))
                : scheduleRepository.findByClubIdAndLastId(clubId, lastId, PageRequest.of(0, size));
        return schedules.stream().map(s -> toResponse(s, userId)).toList();
    }

    @Transactional(readOnly = true)
    public ScheduleResponse getSchedule(Long userId, Long clubId, Long scheduleId) {
        requireClubMember(clubId, userId);
        Schedule schedule = findActiveSchedule(clubId, scheduleId);
        return toResponse(schedule, userId);
    }

    // ── 참여 ──────────────────────────────────────────────

    @Transactional
    public void joinSchedule(Long userId, Long clubId, Long scheduleId) {
        requireClubMember(clubId, userId);
        Schedule schedule = findActiveSchedule(clubId, scheduleId);
        if (schedule.getStatus() != ScheduleStatus.RECRUITING) {
            throw new ScheduleNotRecruitingException();
        }
        if (userScheduleRepository.existsBySchedule_IdAndUser_Id(scheduleId, userId)) {
            throw new AlreadyJoinedScheduleException();
        }
        if (userScheduleRepository.countBySchedule_Id(scheduleId) >= schedule.getLimit()) {
            throw new ScheduleFullException();
        }
        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
        userScheduleRepository.save(UserSchedule.create(user, schedule, UserScheduleRole.MEMBER));
    }

    @Transactional
    public void leaveSchedule(Long userId, Long clubId, Long scheduleId) {
        requireClubMember(clubId, userId);
        findActiveSchedule(clubId, scheduleId);
        UserSchedule userSchedule = userScheduleRepository.findBySchedule_IdAndUser_Id(scheduleId, userId)
                .orElseThrow(NotJoinedScheduleException::new);
        if (userSchedule.getRole() == UserScheduleRole.LEADER) {
            throw new ScheduleAccessDeniedException();
        }
        userScheduleRepository.delete(userSchedule);
    }

    // ── 참여자 목록 ────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ScheduleMemberResponse> getScheduleMembers(Long userId, Long clubId, Long scheduleId,
                                                            Long lastId, int size) {
        requireClubMember(clubId, userId);
        findActiveSchedule(clubId, scheduleId);
        List<UserSchedule> list = lastId == null
                ? userScheduleRepository.findByScheduleId(scheduleId, PageRequest.of(0, size))
                : userScheduleRepository.findByScheduleIdAndLastId(scheduleId, lastId, PageRequest.of(0, size));
        return list.stream().map(us -> new ScheduleMemberResponse(
                us.getId(), us.getUser().getId(), us.getUser().getNickname(),
                us.getUser().getProfileImageUrl(), us.getRole()
        )).toList();
    }
}
```

- [ ] **Step 2: 컴파일 확인**

```bash
./gradlew compileJava
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: 커밋**

```bash
git add src/main/java/com/buddkitv2/domain/schedule/service/ScheduleService.java
git commit -m "feat(schedule): ScheduleService 구현"
```

---

## Task 6: ScheduleService 통합 테스트

**Files:**
- Create: `src/test/java/com/buddkitv2/domain/schedule/service/ScheduleServiceTest.java`

- [ ] **Step 1: 테스트 파일 작성**

```java
package com.buddkitv2.domain.schedule.service;

import com.buddkitv2.domain.club.entity.UserClubRole;
import com.buddkitv2.domain.club.repository.ClubRepository;
import com.buddkitv2.domain.club.repository.UserClubRepository;
import com.buddkitv2.domain.club.service.ClubService;
import com.buddkitv2.domain.club.dto.request.ClubCreateRequest;
import com.buddkitv2.domain.common.Address;
import com.buddkitv2.domain.common.AddressRepository;
import com.buddkitv2.domain.schedule.dto.request.ScheduleCreateRequest;
import com.buddkitv2.domain.schedule.dto.request.ScheduleUpdateRequest;
import com.buddkitv2.domain.schedule.dto.response.ScheduleMemberResponse;
import com.buddkitv2.domain.schedule.dto.response.ScheduleResponse;
import com.buddkitv2.domain.schedule.entity.ScheduleStatus;
import com.buddkitv2.domain.schedule.entity.UserScheduleRole;
import com.buddkitv2.domain.schedule.repository.ScheduleRepository;
import com.buddkitv2.domain.schedule.repository.UserScheduleRepository;
import com.buddkitv2.domain.user.entity.Gender;
import com.buddkitv2.domain.user.entity.Interest;
import com.buddkitv2.domain.user.entity.InterestCategory;
import com.buddkitv2.domain.user.entity.User;
import com.buddkitv2.domain.user.repository.InterestRepository;
import com.buddkitv2.domain.user.repository.UserRepository;
import com.buddkitv2.global.config.S3Service;
import com.buddkitv2.global.config.TossPaymentClient;
import com.buddkitv2.global.exception.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class ScheduleServiceTest {

    @Autowired ScheduleService scheduleService;
    @Autowired ClubService clubService;
    @Autowired UserRepository userRepository;
    @Autowired AddressRepository addressRepository;
    @Autowired InterestRepository interestRepository;
    @Autowired ClubRepository clubRepository;
    @Autowired UserClubRepository userClubRepository;
    @Autowired ScheduleRepository scheduleRepository;
    @Autowired UserScheduleRepository userScheduleRepository;

    @MockitoBean TossPaymentClient tossPaymentClient;
    @MockitoBean S3Service s3Service;

    private User leader;
    private User member;
    private User outsider;
    private Long clubId;

    @BeforeEach
    void setUp() {
        Address address = addressRepository.save(Address.of("서울특별시", "테스트구", 99000));
        Interest interest = interestRepository.save(Interest.of(InterestCategory.CULTURE, "문화"));

        leader = userRepository.save(User.register(20001L, "모임장", LocalDate.of(1990,1,1), Gender.MALE, address, null));
        member = userRepository.save(User.register(20002L, "멤버", LocalDate.of(1991,2,2), Gender.FEMALE, address, null));
        outsider = userRepository.save(User.register(20003L, "외부인", LocalDate.of(1992,3,3), Gender.MALE, address, null));

        ClubCreateRequest clubReq = new ClubCreateRequest();
        clubReq.setName("테스트모임");
        clubReq.setUserLimit(10);
        clubReq.setDescription("설명");
        clubReq.setClubImage(null);
        clubReq.setCity("서울특별시");
        clubReq.setDistrict("테스트구");
        clubReq.setInterestCategory(InterestCategory.CULTURE);
        clubId = clubService.createClub(leader.getId(), clubReq).getClubId();
        clubService.joinClub(member.getId(), clubId);
    }

    private ScheduleCreateRequest createReq(int limit) {
        ScheduleCreateRequest req = new ScheduleCreateRequest();
        req.setName("정모");
        req.setScheduleTime(LocalDateTime.now().plusDays(7));
        req.setLocation("강남역");
        req.setCost(5000L);
        req.setLimit(limit);
        return req;
    }

    @Test
    void 모임장은_스케줄을_생성할_수_있다() {
        ScheduleResponse res = scheduleService.createSchedule(leader.getId(), clubId, createReq(10));

        assertThat(res.getName()).isEqualTo("정모");
        assertThat(res.getStatus()).isEqualTo(ScheduleStatus.RECRUITING);
        assertThat(res.getParticipantCount()).isEqualTo(1);
        assertThat(res.isJoined()).isTrue();
        assertThat(userScheduleRepository.findBySchedule_IdAndUser_Id(res.getScheduleId(), leader.getId()))
                .isPresent()
                .get()
                .satisfies(us -> assertThat(us.getRole()).isEqualTo(UserScheduleRole.LEADER));
    }

    @Test
    void 모임장이_아닌_멤버가_스케줄을_생성하면_예외를_던진다() {
        assertThatThrownBy(() -> scheduleService.createSchedule(member.getId(), clubId, createReq(10)))
                .isInstanceOf(ScheduleAccessDeniedException.class);
    }

    @Test
    void 모임원이_아닌_사람이_스케줄을_생성하면_예외를_던진다() {
        assertThatThrownBy(() -> scheduleService.createSchedule(outsider.getId(), clubId, createReq(10)))
                .isInstanceOf(ScheduleAccessDeniedException.class);
    }

    @Test
    void 모임장은_스케줄을_수정할_수_있다() {
        ScheduleResponse created = scheduleService.createSchedule(leader.getId(), clubId, createReq(10));
        ScheduleUpdateRequest upd = new ScheduleUpdateRequest();
        upd.setName("수정된 정모");
        upd.setScheduleTime(LocalDateTime.now().plusDays(14));
        upd.setLocation("홍대");
        upd.setCost(10000L);
        upd.setLimit(5);

        ScheduleResponse updated = scheduleService.updateSchedule(leader.getId(), clubId, created.getScheduleId(), upd);

        assertThat(updated.getName()).isEqualTo("수정된 정모");
        assertThat(updated.getCost()).isEqualTo(10000L);
    }

    @Test
    void 모임장은_스케줄을_삭제할_수_있다() {
        ScheduleResponse created = scheduleService.createSchedule(leader.getId(), clubId, createReq(10));
        Long scheduleId = created.getScheduleId();

        scheduleService.deleteSchedule(leader.getId(), clubId, scheduleId);

        assertThat(scheduleRepository.findActiveById(scheduleId)).isEmpty();
    }

    @Test
    void 멤버는_RECRUITING_상태_스케줄에_참여할_수_있다() {
        ScheduleResponse created = scheduleService.createSchedule(leader.getId(), clubId, createReq(10));
        Long scheduleId = created.getScheduleId();

        scheduleService.joinSchedule(member.getId(), clubId, scheduleId);

        assertThat(userScheduleRepository.existsBySchedule_IdAndUser_Id(scheduleId, member.getId())).isTrue();
    }

    @Test
    void 이미_참여한_스케줄에_재참여하면_예외를_던진다() {
        ScheduleResponse created = scheduleService.createSchedule(leader.getId(), clubId, createReq(10));
        Long scheduleId = created.getScheduleId();
        scheduleService.joinSchedule(member.getId(), clubId, scheduleId);

        assertThatThrownBy(() -> scheduleService.joinSchedule(member.getId(), clubId, scheduleId))
                .isInstanceOf(AlreadyJoinedScheduleException.class);
    }

    @Test
    void 정원이_가득_찬_스케줄에_참여하면_예외를_던진다() {
        ScheduleResponse created = scheduleService.createSchedule(leader.getId(), clubId, createReq(1));
        Long scheduleId = created.getScheduleId();

        assertThatThrownBy(() -> scheduleService.joinSchedule(member.getId(), clubId, scheduleId))
                .isInstanceOf(ScheduleFullException.class);
    }

    @Test
    void 멤버는_스케줄_참여를_취소할_수_있다() {
        ScheduleResponse created = scheduleService.createSchedule(leader.getId(), clubId, createReq(10));
        Long scheduleId = created.getScheduleId();
        scheduleService.joinSchedule(member.getId(), clubId, scheduleId);

        scheduleService.leaveSchedule(member.getId(), clubId, scheduleId);

        assertThat(userScheduleRepository.existsBySchedule_IdAndUser_Id(scheduleId, member.getId())).isFalse();
    }

    @Test
    void 모임장은_스케줄에서_탈퇴할_수_없다() {
        ScheduleResponse created = scheduleService.createSchedule(leader.getId(), clubId, createReq(10));

        assertThatThrownBy(() -> scheduleService.leaveSchedule(leader.getId(), clubId, created.getScheduleId()))
                .isInstanceOf(ScheduleAccessDeniedException.class);
    }

    @Test
    void 스케줄_목록을_cursor_기반으로_조회한다() {
        scheduleService.createSchedule(leader.getId(), clubId, createReq(10));
        scheduleService.createSchedule(leader.getId(), clubId, createReq(10));

        List<ScheduleResponse> page1 = scheduleService.getSchedules(leader.getId(), clubId, null, 1);
        assertThat(page1).hasSize(1);

        List<ScheduleResponse> page2 = scheduleService.getSchedules(leader.getId(), clubId, page1.get(0).getScheduleId(), 10);
        assertThat(page2).hasSize(1);
    }

    @Test
    void 모임원이_아닌_사람이_스케줄_목록을_조회하면_예외를_던진다() {
        assertThatThrownBy(() -> scheduleService.getSchedules(outsider.getId(), clubId, null, 10))
                .isInstanceOf(ScheduleAccessDeniedException.class);
    }

    @Test
    void 참여자_목록을_cursor_기반으로_조회한다() {
        ScheduleResponse created = scheduleService.createSchedule(leader.getId(), clubId, createReq(10));
        Long scheduleId = created.getScheduleId();
        scheduleService.joinSchedule(member.getId(), clubId, scheduleId);

        List<ScheduleMemberResponse> members = scheduleService.getScheduleMembers(leader.getId(), clubId, scheduleId, null, 10);

        assertThat(members).hasSize(2);
        assertThat(members.stream().map(ScheduleMemberResponse::getRole))
                .containsExactlyInAnyOrder(UserScheduleRole.LEADER, UserScheduleRole.MEMBER);
    }
}
```

- [ ] **Step 2: 테스트 실행**

```bash
./gradlew test --tests "com.buddkitv2.domain.schedule.service.ScheduleServiceTest"
```
Expected: 모든 테스트 PASSED

- [ ] **Step 3: 커밋**

```bash
git add src/test/java/com/buddkitv2/domain/schedule/service/ScheduleServiceTest.java
git commit -m "test(schedule): ScheduleService 통합 테스트 추가"
```

---

## Task 7: SettlementService 구현

**Files:**
- Create: `src/main/java/com/buddkitv2/domain/settlement/service/SettlementService.java`

- [ ] **Step 1: SettlementService 작성**

```java
package com.buddkitv2.domain.settlement.service;

import com.buddkitv2.domain.club.entity.UserClub;
import com.buddkitv2.domain.club.entity.UserClubRole;
import com.buddkitv2.domain.club.repository.UserClubRepository;
import com.buddkitv2.domain.schedule.entity.Schedule;
import com.buddkitv2.domain.schedule.entity.ScheduleStatus;
import com.buddkitv2.domain.schedule.entity.UserSchedule;
import com.buddkitv2.domain.schedule.entity.UserScheduleRole;
import com.buddkitv2.domain.schedule.repository.ScheduleRepository;
import com.buddkitv2.domain.schedule.repository.UserScheduleRepository;
import com.buddkitv2.domain.settlement.dto.response.SettlementStatusResponse;
import com.buddkitv2.domain.settlement.entity.Settlement;
import com.buddkitv2.domain.settlement.entity.SettlementStatus;
import com.buddkitv2.domain.settlement.entity.UserSettlement;
import com.buddkitv2.domain.settlement.entity.UserSettlementStatus;
import com.buddkitv2.domain.settlement.repository.SettlementRepository;
import com.buddkitv2.domain.settlement.repository.UserSettlementRepository;
import com.buddkitv2.domain.user.entity.User;
import com.buddkitv2.domain.wallet.entity.Wallet;
import com.buddkitv2.domain.wallet.entity.WalletTransaction;
import com.buddkitv2.domain.wallet.entity.WalletTransactionType;
import com.buddkitv2.domain.wallet.repository.WalletRepository;
import com.buddkitv2.domain.wallet.repository.WalletTransactionRepository;
import com.buddkitv2.global.exception.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SettlementService {

    private final SettlementRepository settlementRepository;
    private final UserSettlementRepository userSettlementRepository;
    private final ScheduleRepository scheduleRepository;
    private final UserScheduleRepository userScheduleRepository;
    private final UserClubRepository userClubRepository;
    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;

    // ── 헬퍼 ──────────────────────────────────────────────

    private Schedule findActiveSchedule(Long clubId, Long scheduleId) {
        Schedule schedule = scheduleRepository.findActiveById(scheduleId)
                .orElseThrow(ScheduleNotFoundException::new);
        if (!schedule.getClub().getId().equals(clubId)) throw new ScheduleNotFoundException();
        return schedule;
    }

    private UserClub requireClubMember(Long clubId, Long userId) {
        return userClubRepository.findByClub_IdAndUser_Id(clubId, userId)
                .orElseThrow(ScheduleAccessDeniedException::new);
    }

    private void requireLeader(UserClub uc) {
        if (uc.getRole() != UserClubRole.LEADER) throw new ScheduleAccessDeniedException();
    }

    private Wallet findWallet(Long userId) {
        return walletRepository.findByUserId(userId).orElseThrow(WalletNotFoundException::new);
    }

    // Settlement·Schedule 상태 자동 갱신
    public void refreshSettlementStatus(Settlement settlement) {
        long total = userSettlementRepository.countBySettlement_Id(settlement.getId());
        long completed = userSettlementRepository.countBySettlement_IdAndStatus(
                settlement.getId(), UserSettlementStatus.COMPLETED);

        if (settlement.getStatus() == SettlementStatus.REQUESTED && completed > 0) {
            settlement.changeStatus(SettlementStatus.IN_PROGRESS);
        }
        if (total > 0 && total == completed) {
            settlement.complete();
            settlement.getSchedule().changeStatus(ScheduleStatus.CLOSED);
        }
    }

    // 포인트 이체 처리 (즉시 정산·배치 공용)
    public void executeTransfer(UserSettlement userSettlement, Schedule schedule) {
        User member = userSettlement.getUser();
        Wallet memberWallet = findWallet(member.getId());

        if (memberWallet.getBalance() < schedule.getCost()) {
            throw new InsufficientBalanceException();
        }

        UserSchedule leaderSchedule = userScheduleRepository
                .findBySchedule_IdAndRole(schedule.getId(), UserScheduleRole.LEADER)
                .stream().findFirst().orElseThrow(ScheduleNotFoundException::new);
        Wallet leaderWallet = findWallet(leaderSchedule.getUser().getId());

        memberWallet.debit(schedule.getCost());
        leaderWallet.charge(schedule.getCost());
        walletTransactionRepository.save(
                WalletTransaction.create(memberWallet, leaderWallet, WalletTransactionType.TRANSFER, schedule.getCost()));

        userSettlement.complete();
    }

    // ── API ───────────────────────────────────────────────

    @Transactional
    public void requestSettlement(Long userId, Long clubId, Long scheduleId) {
        UserClub uc = requireClubMember(clubId, userId);
        requireLeader(uc);
        Schedule schedule = findActiveSchedule(clubId, scheduleId);

        if (settlementRepository.existsBySchedule_Id(scheduleId)) {
            throw new ScheduleAlreadySettlingException();
        }

        List<UserSchedule> members = userScheduleRepository
                .findBySchedule_IdAndRole(scheduleId, UserScheduleRole.MEMBER);
        long sum = schedule.getCost() * members.size();

        schedule.changeStatus(ScheduleStatus.SETTLING);

        User leaderUser = uc.getUser();
        Settlement settlement = Settlement.create(schedule, sum, leaderUser);
        settlementRepository.save(settlement);

        for (UserSchedule us : members) {
            userSettlementRepository.save(UserSettlement.create(us.getUser(), settlement));
        }
    }

    @Transactional(readOnly = true)
    public List<SettlementStatusResponse> getSettlements(Long userId, Long clubId, Long scheduleId,
                                                          Long lastId, int size) {
        requireClubMember(clubId, userId);
        Settlement settlement = settlementRepository.findBySchedule_Id(scheduleId)
                .orElseThrow(SettlementNotFoundException::new);
        List<UserSettlement> list = lastId == null
                ? userSettlementRepository.findBySettlementId(settlement.getId(), PageRequest.of(0, size))
                : userSettlementRepository.findBySettlementIdAndLastId(settlement.getId(), lastId, PageRequest.of(0, size));
        Schedule schedule = findActiveSchedule(clubId, scheduleId);
        return list.stream().map(us -> new SettlementStatusResponse(
                us.getId(), us.getUser().getId(), us.getUser().getNickname(),
                us.getStatus(), us.getCompletedTime(), schedule.getCost()
        )).toList();
    }

    @Transactional
    public void settleMyShare(Long userId, Long clubId, Long scheduleId) {
        requireClubMember(clubId, userId);
        Schedule schedule = findActiveSchedule(clubId, scheduleId);
        Settlement settlement = settlementRepository.findBySchedule_Id(scheduleId)
                .orElseThrow(SettlementNotFoundException::new);
        UserSettlement userSettlement = userSettlementRepository
                .findBySettlement_IdAndUser_Id(settlement.getId(), userId)
                .orElseThrow(SettlementNotFoundException::new);
        if (userSettlement.getStatus() != UserSettlementStatus.REQUESTED) {
            throw new AlreadySettledException();
        }
        executeTransfer(userSettlement, schedule);
        refreshSettlementStatus(settlement);
    }

    @Transactional
    public void reserveMyShare(Long userId, Long clubId, Long scheduleId) {
        requireClubMember(clubId, userId);
        Settlement settlement = settlementRepository.findBySchedule_Id(scheduleId)
                .orElseThrow(SettlementNotFoundException::new);
        UserSettlement userSettlement = userSettlementRepository
                .findBySettlement_IdAndUser_Id(settlement.getId(), userId)
                .orElseThrow(SettlementNotFoundException::new);
        if (userSettlement.getStatus() != UserSettlementStatus.REQUESTED) {
            throw new AlreadySettledException();
        }
        userSettlement.reserve();
    }

    @Transactional
    public void completeManually(Long userId, Long clubId, Long scheduleId, Long userSettlementId) {
        UserClub uc = requireClubMember(clubId, userId);
        requireLeader(uc);
        Settlement settlement = settlementRepository.findBySchedule_Id(scheduleId)
                .orElseThrow(SettlementNotFoundException::new);
        UserSettlement userSettlement = userSettlementRepository.findById(userSettlementId)
                .filter(us -> us.getSettlement().getId().equals(settlement.getId()))
                .orElseThrow(SettlementNotFoundException::new);
        if (userSettlement.getStatus() == UserSettlementStatus.COMPLETED) {
            throw new AlreadySettledException();
        }
        userSettlement.complete();
        refreshSettlementStatus(settlement);
    }
}
```

- [ ] **Step 2: 컴파일 확인**

```bash
./gradlew compileJava
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: 커밋**

```bash
git add src/main/java/com/buddkitv2/domain/settlement/service/SettlementService.java
git commit -m "feat(settlement): SettlementService 구현"
```

---

## Task 8: SettlementService 통합 테스트

**Files:**
- Create: `src/test/java/com/buddkitv2/domain/settlement/service/SettlementServiceTest.java`

- [ ] **Step 1: 테스트 파일 작성**

```java
package com.buddkitv2.domain.settlement.service;

import com.buddkitv2.domain.club.dto.request.ClubCreateRequest;
import com.buddkitv2.domain.club.repository.UserClubRepository;
import com.buddkitv2.domain.club.service.ClubService;
import com.buddkitv2.domain.common.Address;
import com.buddkitv2.domain.common.AddressRepository;
import com.buddkitv2.domain.schedule.dto.request.ScheduleCreateRequest;
import com.buddkitv2.domain.schedule.entity.ScheduleStatus;
import com.buddkitv2.domain.schedule.repository.ScheduleRepository;
import com.buddkitv2.domain.schedule.service.ScheduleService;
import com.buddkitv2.domain.settlement.dto.response.SettlementStatusResponse;
import com.buddkitv2.domain.settlement.entity.Settlement;
import com.buddkitv2.domain.settlement.entity.UserSettlementStatus;
import com.buddkitv2.domain.settlement.repository.SettlementRepository;
import com.buddkitv2.domain.settlement.repository.UserSettlementRepository;
import com.buddkitv2.domain.user.entity.Gender;
import com.buddkitv2.domain.user.entity.Interest;
import com.buddkitv2.domain.user.entity.InterestCategory;
import com.buddkitv2.domain.user.entity.User;
import com.buddkitv2.domain.user.repository.InterestRepository;
import com.buddkitv2.domain.user.repository.UserRepository;
import com.buddkitv2.domain.wallet.entity.Wallet;
import com.buddkitv2.domain.wallet.repository.WalletRepository;
import com.buddkitv2.global.config.S3Service;
import com.buddkitv2.global.config.TossPaymentClient;
import com.buddkitv2.global.exception.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class SettlementServiceTest {

    @Autowired SettlementService settlementService;
    @Autowired ScheduleService scheduleService;
    @Autowired ClubService clubService;
    @Autowired UserRepository userRepository;
    @Autowired AddressRepository addressRepository;
    @Autowired InterestRepository interestRepository;
    @Autowired WalletRepository walletRepository;
    @Autowired SettlementRepository settlementRepository;
    @Autowired UserSettlementRepository userSettlementRepository;
    @Autowired ScheduleRepository scheduleRepository;

    @MockitoBean TossPaymentClient tossPaymentClient;
    @MockitoBean S3Service s3Service;

    private User leader;
    private User member1;
    private User member2;
    private Long clubId;
    private Long scheduleId;
    private static final Long COST = 5000L;

    @BeforeEach
    void setUp() {
        Address address = addressRepository.save(Address.of("서울특별시", "테스트구", 99000));
        Interest interest = interestRepository.save(Interest.of(InterestCategory.CULTURE, "문화"));

        leader  = userRepository.save(User.register(30001L, "모임장", LocalDate.of(1990,1,1), Gender.MALE, address, null));
        member1 = userRepository.save(User.register(30002L, "멤버1", LocalDate.of(1991,2,2), Gender.FEMALE, address, null));
        member2 = userRepository.save(User.register(30003L, "멤버2", LocalDate.of(1992,3,3), Gender.MALE, address, null));

        walletRepository.save(Wallet.create(leader));
        walletRepository.save(Wallet.createWithBonus(member1, 20000L));
        walletRepository.save(Wallet.createWithBonus(member2, 0L));

        ClubCreateRequest clubReq = new ClubCreateRequest();
        clubReq.setName("정산테스트모임"); clubReq.setUserLimit(10);
        clubReq.setDescription("설명"); clubReq.setClubImage(null);
        clubReq.setCity("서울특별시"); clubReq.setDistrict("테스트구");
        clubReq.setInterestCategory(InterestCategory.CULTURE);
        clubId = clubService.createClub(leader.getId(), clubReq).getClubId();
        clubService.joinClub(member1.getId(), clubId);
        clubService.joinClub(member2.getId(), clubId);

        ScheduleCreateRequest schedReq = new ScheduleCreateRequest();
        schedReq.setName("정모"); schedReq.setScheduleTime(LocalDateTime.now().plusDays(7));
        schedReq.setLocation("강남"); schedReq.setCost(COST); schedReq.setLimit(10);
        scheduleId = scheduleService.createSchedule(leader.getId(), clubId, schedReq).getScheduleId();
        scheduleService.joinSchedule(member1.getId(), clubId, scheduleId);
        scheduleService.joinSchedule(member2.getId(), clubId, scheduleId);
    }

    @Test
    void 모임장이_정산을_요청하면_Settlement와_UserSettlement가_생성된다() {
        settlementService.requestSettlement(leader.getId(), clubId, scheduleId);

        Settlement settlement = settlementRepository.findBySchedule_Id(scheduleId).orElseThrow();
        assertThat(settlement.getSum()).isEqualTo(COST * 2); // member1 + member2
        assertThat(userSettlementRepository.countBySettlement_Id(settlement.getId())).isEqualTo(2);
        assertThat(scheduleRepository.findActiveById(scheduleId).orElseThrow().getStatus())
                .isEqualTo(ScheduleStatus.SETTLING);
    }

    @Test
    void 이미_정산이_요청된_스케줄에_재요청하면_예외를_던진다() {
        settlementService.requestSettlement(leader.getId(), clubId, scheduleId);

        assertThatThrownBy(() -> settlementService.requestSettlement(leader.getId(), clubId, scheduleId))
                .isInstanceOf(ScheduleAlreadySettlingException.class);
    }

    @Test
    void 멤버가_즉시_정산하면_포인트가_이체되고_COMPLETED가_된다() {
        settlementService.requestSettlement(leader.getId(), clubId, scheduleId);

        settlementService.settleMyShare(member1.getId(), clubId, scheduleId);

        Wallet memberWallet = walletRepository.findByUserId(member1.getId()).orElseThrow();
        Wallet leaderWallet = walletRepository.findByUserId(leader.getId()).orElseThrow();
        assertThat(memberWallet.getBalance()).isEqualTo(20000L - COST);
        assertThat(leaderWallet.getBalance()).isEqualTo(COST);

        Settlement settlement = settlementRepository.findBySchedule_Id(scheduleId).orElseThrow();
        long completed = userSettlementRepository.countBySettlement_IdAndStatus(
                settlement.getId(), UserSettlementStatus.COMPLETED);
        assertThat(completed).isEqualTo(1);
    }

    @Test
    void 잔액_부족_멤버가_즉시_정산하면_예외를_던진다() {
        settlementService.requestSettlement(leader.getId(), clubId, scheduleId);

        assertThatThrownBy(() -> settlementService.settleMyShare(member2.getId(), clubId, scheduleId))
                .isInstanceOf(InsufficientBalanceException.class);
    }

    @Test
    void 전원_정산_완료_시_Settlement와_Schedule이_CLOSED된다() {
        settlementService.requestSettlement(leader.getId(), clubId, scheduleId);
        settlementService.settleMyShare(member1.getId(), clubId, scheduleId);

        // member2는 모임장이 수동 처리
        Settlement settlement = settlementRepository.findBySchedule_Id(scheduleId).orElseThrow();
        Long us2Id = userSettlementRepository
                .findBySettlement_IdAndUser_Id(settlement.getId(), member2.getId())
                .orElseThrow().getId();
        settlementService.completeManually(leader.getId(), clubId, scheduleId, us2Id);

        assertThat(settlementRepository.findBySchedule_Id(scheduleId).orElseThrow().getStatus())
                .isEqualTo(com.buddkitv2.domain.settlement.entity.SettlementStatus.COMPLETED);
        assertThat(scheduleRepository.findActiveById(scheduleId).orElseThrow().getStatus())
                .isEqualTo(ScheduleStatus.CLOSED);
    }

    @Test
    void 멤버가_예약_정산하면_PENDING_CONFIRMATION이_된다() {
        settlementService.requestSettlement(leader.getId(), clubId, scheduleId);

        settlementService.reserveMyShare(member1.getId(), clubId, scheduleId);

        Settlement settlement = settlementRepository.findBySchedule_Id(scheduleId).orElseThrow();
        assertThat(userSettlementRepository
                .findBySettlement_IdAndUser_Id(settlement.getId(), member1.getId())
                .orElseThrow().getStatus())
                .isEqualTo(UserSettlementStatus.PENDING_CONFIRMATION);
    }

    @Test
    void 이미_정산된_항목을_재정산하면_예외를_던진다() {
        settlementService.requestSettlement(leader.getId(), clubId, scheduleId);
        settlementService.settleMyShare(member1.getId(), clubId, scheduleId);

        assertThatThrownBy(() -> settlementService.settleMyShare(member1.getId(), clubId, scheduleId))
                .isInstanceOf(AlreadySettledException.class);
    }

    @Test
    void 정산_현황_목록을_조회할_수_있다() {
        settlementService.requestSettlement(leader.getId(), clubId, scheduleId);

        List<SettlementStatusResponse> list = settlementService.getSettlements(
                leader.getId(), clubId, scheduleId, null, 10);

        assertThat(list).hasSize(2);
        assertThat(list.get(0).getAmount()).isEqualTo(COST);
        assertThat(list.stream().map(SettlementStatusResponse::getStatus))
                .containsOnly(UserSettlementStatus.REQUESTED);
    }
}
```

- [ ] **Step 2: 테스트 실행**

```bash
./gradlew test --tests "com.buddkitv2.domain.settlement.service.SettlementServiceTest"
```
Expected: 모든 테스트 PASSED

- [ ] **Step 3: 커밋**

```bash
git add src/test/java/com/buddkitv2/domain/settlement/service/SettlementServiceTest.java
git commit -m "test(settlement): SettlementService 통합 테스트 추가"
```

---

## Task 9: SettlementBatchService + SchedulingConfig

**Files:**
- Create: `src/main/java/com/buddkitv2/domain/settlement/service/SettlementBatchService.java`
- Create: `src/main/java/com/buddkitv2/global/config/SchedulingConfig.java`

- [ ] **Step 1: SchedulingConfig 생성**

```java
package com.buddkitv2.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class SchedulingConfig {
}
```

- [ ] **Step 2: SettlementBatchService 생성**

```java
package com.buddkitv2.domain.settlement.service;

import com.buddkitv2.domain.schedule.entity.Schedule;
import com.buddkitv2.domain.settlement.entity.Settlement;
import com.buddkitv2.domain.settlement.entity.UserSettlement;
import com.buddkitv2.domain.settlement.entity.UserSettlementStatus;
import com.buddkitv2.domain.settlement.repository.SettlementRepository;
import com.buddkitv2.domain.settlement.repository.UserSettlementRepository;
import com.buddkitv2.global.exception.InsufficientBalanceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SettlementBatchService {

    private final UserSettlementRepository userSettlementRepository;
    private final SettlementRepository settlementRepository;
    private final SettlementService settlementService;

    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void processReservedSettlements() {
        List<UserSettlement> reserved = userSettlementRepository
                .findByStatus(UserSettlementStatus.PENDING_CONFIRMATION);

        for (UserSettlement us : reserved) {
            Settlement settlement = us.getSettlement();
            Schedule schedule = settlement.getSchedule();
            try {
                settlementService.executeTransfer(us, schedule);
                settlementService.refreshSettlementStatus(settlement);
                log.info("배치 정산 완료: userSettlementId={}", us.getId());
            } catch (InsufficientBalanceException e) {
                us.rollback();
                log.warn("배치 정산 잔액 부족 롤백: userSettlementId={}, userId={}", us.getId(), us.getUser().getId());
            }
        }
    }
}
```

- [ ] **Step 3: 컴파일 확인**

```bash
./gradlew compileJava
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: 커밋**

```bash
git add src/main/java/com/buddkitv2/domain/settlement/service/SettlementBatchService.java \
        src/main/java/com/buddkitv2/global/config/SchedulingConfig.java
git commit -m "feat(settlement): 자정 배치 정산 서비스 구현"
```

---

## Task 10: ScheduleController

**Files:**
- Create: `src/main/java/com/buddkitv2/domain/schedule/controller/ScheduleController.java`

- [ ] **Step 1: ScheduleController 작성**

```java
package com.buddkitv2.domain.schedule.controller;

import com.buddkitv2.domain.schedule.dto.request.ScheduleCreateRequest;
import com.buddkitv2.domain.schedule.dto.request.ScheduleUpdateRequest;
import com.buddkitv2.domain.schedule.dto.response.ScheduleMemberResponse;
import com.buddkitv2.domain.schedule.dto.response.ScheduleResponse;
import com.buddkitv2.domain.schedule.service.ScheduleService;
import com.buddkitv2.domain.settlement.dto.response.SettlementStatusResponse;
import com.buddkitv2.domain.settlement.service.SettlementService;
import com.buddkitv2.global.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/clubs/{clubId}/schedules")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;
    private final SettlementService settlementService;

    // ── 스케줄 CRUD ────────────────────────────────────────

    @PostMapping
    public ApiResponse<ScheduleResponse> createSchedule(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long clubId,
            @RequestBody @Valid ScheduleCreateRequest request) {
        return ApiResponse.ok(scheduleService.createSchedule(userId, clubId, request));
    }

    @PatchMapping("/{scheduleId}")
    public ApiResponse<ScheduleResponse> updateSchedule(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long clubId,
            @PathVariable Long scheduleId,
            @RequestBody @Valid ScheduleUpdateRequest request) {
        return ApiResponse.ok(scheduleService.updateSchedule(userId, clubId, scheduleId, request));
    }

    @DeleteMapping("/{scheduleId}")
    public ResponseEntity<Void> deleteSchedule(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long clubId,
            @PathVariable Long scheduleId) {
        scheduleService.deleteSchedule(userId, clubId, scheduleId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ApiResponse<List<ScheduleResponse>> getSchedules(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long clubId,
            @RequestParam(required = false) Long lastId,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(scheduleService.getSchedules(userId, clubId, lastId, size));
    }

    @GetMapping("/{scheduleId}")
    public ApiResponse<ScheduleResponse> getSchedule(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long clubId,
            @PathVariable Long scheduleId) {
        return ApiResponse.ok(scheduleService.getSchedule(userId, clubId, scheduleId));
    }

    // ── 참여 ──────────────────────────────────────────────

    @PostMapping("/{scheduleId}/members")
    public ResponseEntity<Void> joinSchedule(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long clubId,
            @PathVariable Long scheduleId) {
        scheduleService.joinSchedule(userId, clubId, scheduleId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{scheduleId}/members/me")
    public ResponseEntity<Void> leaveSchedule(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long clubId,
            @PathVariable Long scheduleId) {
        scheduleService.leaveSchedule(userId, clubId, scheduleId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{scheduleId}/members")
    public ApiResponse<List<ScheduleMemberResponse>> getScheduleMembers(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long clubId,
            @PathVariable Long scheduleId,
            @RequestParam(required = false) Long lastId,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(scheduleService.getScheduleMembers(userId, clubId, scheduleId, lastId, size));
    }

    // ── 정산 ──────────────────────────────────────────────

    @PostMapping("/{scheduleId}/settlements")
    public ResponseEntity<Void> requestSettlement(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long clubId,
            @PathVariable Long scheduleId) {
        settlementService.requestSettlement(userId, clubId, scheduleId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{scheduleId}/settlements")
    public ApiResponse<List<SettlementStatusResponse>> getSettlements(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long clubId,
            @PathVariable Long scheduleId,
            @RequestParam(required = false) Long lastId,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(settlementService.getSettlements(userId, clubId, scheduleId, lastId, size));
    }

    @PostMapping("/{scheduleId}/settlements/me")
    public ResponseEntity<Void> settleMyShare(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long clubId,
            @PathVariable Long scheduleId) {
        settlementService.settleMyShare(userId, clubId, scheduleId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{scheduleId}/settlements/me/reserve")
    public ResponseEntity<Void> reserveMyShare(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long clubId,
            @PathVariable Long scheduleId) {
        settlementService.reserveMyShare(userId, clubId, scheduleId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{scheduleId}/settlements/{userSettlementId}")
    public ResponseEntity<Void> completeManually(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long clubId,
            @PathVariable Long scheduleId,
            @PathVariable Long userSettlementId) {
        settlementService.completeManually(userId, clubId, scheduleId, userSettlementId);
        return ResponseEntity.noContent().build();
    }
}
```

- [ ] **Step 2: 빌드 확인**

```bash
./gradlew build
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: 전체 테스트 확인**

```bash
./gradlew test
```
Expected: 모든 테스트 PASSED

- [ ] **Step 4: 커밋**

```bash
git add src/main/java/com/buddkitv2/domain/schedule/controller/ScheduleController.java
git commit -m "feat(schedule): 스케줄·정산 컨트롤러 구현 (13개 엔드포인트)"
```
