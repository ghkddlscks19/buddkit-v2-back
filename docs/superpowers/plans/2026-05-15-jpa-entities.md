# JPA Entities Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** DDL 기반으로 전체 도메인 JPA 엔티티 및 Enum 타입을 생성한다.

**Architecture:** 기존 `User` 엔티티 컨벤션을 따른다 — `@Entity`, `@Getter`, `@NoArgsConstructor(access = PROTECTED)`, static factory `create(...)`. FK는 `@ManyToOne`으로 매핑하되 `@OneToMany`는 사용하지 않는다(N+1 방지). 주요 도메인 엔티티는 `BaseEntity` 상속, 단순 조인 테이블은 상속 없음.

**Tech Stack:** Java 21, Spring Boot 4.x, Spring Data JPA (Hibernate), Lombok, Jakarta Persistence

---

## 파일 구조

```
domain/
├── common/
│   ├── BaseEntity.java          (기존 — 변경 없음)
│   └── Address.java             (신규)
├── user/
│   ├── User.java                (기존 — birth/gender/city/district 추가)
│   ├── UserStatus.java          (기존)
│   ├── Gender.java              (신규)
│   ├── Interest.java            (신규)
│   ├── InterestCategory.java    (신규)
│   └── UserInterest.java        (신규)
├── club/
│   ├── Club.java                (신규)
│   ├── UserClub.java            (신규)
│   ├── UserClubRole.java        (신규)
│   └── ClubLike.java            (신규)
├── feed/
│   ├── Feed.java                (신규)
│   ├── FeedImage.java           (신규)
│   ├── FeedLike.java            (신규)
│   └── FeedComment.java         (신규)
├── schedule/
│   ├── Schedule.java            (신규)
│   ├── ScheduleStatus.java      (신규)
│   ├── UserSchedule.java        (신규)
│   └── UserScheduleRole.java    (신규)
├── settlement/
│   ├── Settlement.java          (신규)
│   ├── SettlementStatus.java    (신규)
│   ├── UserSettlement.java      (신규)
│   ├── UserSettlementStatus.java (신규)
│   ├── UserSettlementType.java  (신규)
│   └── Transfer.java            (신규)
├── wallet/
│   ├── Wallet.java              (신규)
│   ├── WalletTransaction.java   (신규)
│   ├── WalletTransactionType.java (신규)
│   ├── Payment.java             (신규)
│   └── PaymentStatus.java       (신규)
├── notification/
│   ├── NotificationType.java    (신규 — 엔티티)
│   ├── NotificationTypeEnum.java (신규 — enum)
│   └── Notification.java        (신규)
└── chat/
    ├── ChatRoom.java            (신규)
    ├── ChatRoomType.java        (신규)
    ├── UserChatRoom.java        (신규)
    ├── ChatRoomRole.java        (신규)
    └── Message.java             (신규)
```

---

## Task 1: User 엔티티 보완 + 공통 Enum

**Files:**
- Modify: `src/main/java/com/buddkitv2/domain/user/User.java`
- Create: `src/main/java/com/buddkitv2/domain/user/Gender.java`
- Create: `src/main/java/com/buddkitv2/domain/user/Interest.java`
- Create: `src/main/java/com/buddkitv2/domain/user/InterestCategory.java`
- Create: `src/main/java/com/buddkitv2/domain/user/UserInterest.java`
- Create: `src/main/java/com/buddkitv2/domain/common/Address.java`

- [ ] **Step 1: Gender enum 생성**

```java
// src/main/java/com/buddkitv2/domain/user/Gender.java
package com.buddkitv2.domain.user;

public enum Gender {
    MALE, FEMALE
}
```

- [ ] **Step 2: User 엔티티에 누락 필드 추가**

기존 User.java에 다음 필드를 추가한다 (`fcmToken` 위에):

```java
import java.time.LocalDate;

// 필드 추가 (기존 필드 사이에 삽입)
private LocalDate birth;

@Enumerated(EnumType.STRING)
private Gender gender;

@Column(length = 20)
private String city;

@Column(length = 20)
private String district;
```

- [ ] **Step 3: InterestCategory enum 생성**

```java
// src/main/java/com/buddkitv2/domain/user/InterestCategory.java
package com.buddkitv2.domain.user;

public enum InterestCategory {
    CULTURE,    // 문화(공연/전시)
    SPORTS,     // 운동/스포츠
    TRAVEL,     // 여행
    MUSIC,      // 음악
    CRAFT,      // 공예
    SOCIAL,     // 사교
    LANGUAGE,   // 외국어
    FINANCE     // 재테크
}
```

- [ ] **Step 4: Interest 엔티티 생성**

```java
// src/main/java/com/buddkitv2/domain/user/Interest.java
package com.buddkitv2.domain.user;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "\"INTEREST\"")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Interest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "interest_id")
    private Long id;

    @Enumerated(EnumType.STRING)
    private InterestCategory category;
}
```

- [ ] **Step 5: UserInterest 엔티티 생성**

```java
// src/main/java/com/buddkitv2/domain/user/UserInterest.java
package com.buddkitv2.domain.user;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "\"USER_INTEREST\"")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserInterest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_interest_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interest_id", nullable = false)
    private Interest interest;

    public static UserInterest create(User user, Interest interest) {
        UserInterest ui = new UserInterest();
        ui.user = user;
        ui.interest = interest;
        return ui;
    }
}
```

- [ ] **Step 6: Address 엔티티 생성**

```java
// src/main/java/com/buddkitv2/domain/common/Address.java
package com.buddkitv2.domain.common;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "\"Address\"")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "address_id")
    private Long id;

    @Column(length = 50)
    private String city;

    @Column(length = 50)
    private String district;

    private Integer code;
}
```

- [ ] **Step 7: 빌드 확인**

```bash
./gradlew compileJava
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 8: 커밋**

```bash
git add src/main/java/com/buddkitv2/domain/user/ src/main/java/com/buddkitv2/domain/common/Address.java
git commit -m "feat(user): User 엔티티 필드 보완 및 Interest/Address 엔티티 추가"
```

---

## Task 2: Club 도메인

**Files:**
- Create: `src/main/java/com/buddkitv2/domain/club/Club.java`
- Create: `src/main/java/com/buddkitv2/domain/club/UserClub.java`
- Create: `src/main/java/com/buddkitv2/domain/club/UserClubRole.java`
- Create: `src/main/java/com/buddkitv2/domain/club/ClubLike.java`

- [ ] **Step 1: UserClubRole enum 생성**

```java
// src/main/java/com/buddkitv2/domain/club/UserClubRole.java
package com.buddkitv2.domain.club;

public enum UserClubRole {
    OWNER,   // 모임장
    MANAGER, // 운영진
    MEMBER   // 일반 멤버
}
```

- [ ] **Step 2: Club 엔티티 생성**

```java
// src/main/java/com/buddkitv2/domain/club/Club.java
package com.buddkitv2.domain.club;

import com.buddkitv2.domain.common.BaseEntity;
import com.buddkitv2.domain.user.Interest;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "\"CLUB\"")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Club extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "club_id")
    private Long id;

    @Column(length = 20)
    private String name;

    private Integer userLimit;

    @Column(length = 50)
    private String description;

    private String clubImage;

    @Column(length = 20)
    private String city;

    @Column(length = 20)
    private String district;

    private Integer memberCount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interest_id", nullable = false)
    private Interest interest;

    public static Club create(String name, Integer userLimit, String description,
                               String clubImage, String city, String district, Interest interest) {
        Club club = new Club();
        club.name = name;
        club.userLimit = userLimit;
        club.description = description;
        club.clubImage = clubImage;
        club.city = city;
        club.district = district;
        club.memberCount = 0;
        club.interest = interest;
        return club;
    }
}
```

- [ ] **Step 3: UserClub 엔티티 생성**

```java
// src/main/java/com/buddkitv2/domain/club/UserClub.java
package com.buddkitv2.domain.club;

import com.buddkitv2.domain.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "\"USER_CLUB\"")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserClub {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_club_id")
    private Long id;

    @Enumerated(EnumType.STRING)
    private UserClubRole role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "club_id", nullable = false)
    private Club club;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    public static UserClub create(Club club, User user, UserClubRole role) {
        UserClub uc = new UserClub();
        uc.club = club;
        uc.user = user;
        uc.role = role;
        return uc;
    }
}
```

- [ ] **Step 4: ClubLike 엔티티 생성**

```java
// src/main/java/com/buddkitv2/domain/club/ClubLike.java
package com.buddkitv2.domain.club;

import com.buddkitv2.domain.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "\"CLUB_LIKE\"")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClubLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "club_like_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "club_id", nullable = false)
    private Club club;

    public static ClubLike create(User user, Club club) {
        ClubLike cl = new ClubLike();
        cl.user = user;
        cl.club = club;
        return cl;
    }
}
```

- [ ] **Step 5: 빌드 확인**

```bash
./gradlew compileJava
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: 커밋**

```bash
git add src/main/java/com/buddkitv2/domain/club/
git commit -m "feat(club): Club/UserClub/ClubLike 엔티티 추가"
```

---

## Task 3: Feed 도메인

**Files:**
- Create: `src/main/java/com/buddkitv2/domain/feed/Feed.java`
- Create: `src/main/java/com/buddkitv2/domain/feed/FeedImage.java`
- Create: `src/main/java/com/buddkitv2/domain/feed/FeedLike.java`
- Create: `src/main/java/com/buddkitv2/domain/feed/FeedComment.java`

- [ ] **Step 1: Feed 엔티티 생성**

```java
// src/main/java/com/buddkitv2/domain/feed/Feed.java
package com.buddkitv2.domain.feed;

import com.buddkitv2.domain.club.Club;
import com.buddkitv2.domain.common.BaseEntity;
import com.buddkitv2.domain.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "\"FEED\"")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Feed extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "feed_id")
    private Long id;

    @Column(length = 255)
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "club_id", nullable = false)
    private Club club;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    public static Feed create(String content, Club club, User user) {
        Feed feed = new Feed();
        feed.content = content;
        feed.club = club;
        feed.user = user;
        return feed;
    }
}
```

- [ ] **Step 2: FeedImage 엔티티 생성**

```java
// src/main/java/com/buddkitv2/domain/feed/FeedImage.java
package com.buddkitv2.domain.feed;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "\"FEED_IMAGE\"")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FeedImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "feed_image_id")
    private Long id;

    // 첫 번째 이미지를 썸네일로 사용 (압축)
    @Column(name = "feed_image", length = 255)
    private String imageUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "feed_id", nullable = false)
    private Feed feed;

    public static FeedImage create(String imageUrl, Feed feed) {
        FeedImage fi = new FeedImage();
        fi.imageUrl = imageUrl;
        fi.feed = feed;
        return fi;
    }
}
```

- [ ] **Step 3: FeedLike 엔티티 생성**

```java
// src/main/java/com/buddkitv2/domain/feed/FeedLike.java
package com.buddkitv2.domain.feed;

import com.buddkitv2.domain.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "\"FEED_LIKE\"")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FeedLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "feed_like_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "feed_id", nullable = false)
    private Feed feed;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    public static FeedLike create(Feed feed, User user) {
        FeedLike fl = new FeedLike();
        fl.feed = feed;
        fl.user = user;
        return fl;
    }
}
```

- [ ] **Step 4: FeedComment 엔티티 생성**

```java
// src/main/java/com/buddkitv2/domain/feed/FeedComment.java
package com.buddkitv2.domain.feed;

import com.buddkitv2.domain.common.BaseEntity;
import com.buddkitv2.domain.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "\"FEED_COMMENT\"")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FeedComment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "feed_comment_id")
    private Long id;

    @Column(length = 255)
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "feed_id", nullable = false)
    private Feed feed;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    public static FeedComment create(String content, Feed feed, User user) {
        FeedComment fc = new FeedComment();
        fc.content = content;
        fc.feed = feed;
        fc.user = user;
        return fc;
    }
}
```

- [ ] **Step 5: 빌드 확인**

```bash
./gradlew compileJava
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: 커밋**

```bash
git add src/main/java/com/buddkitv2/domain/feed/
git commit -m "feat(feed): Feed/FeedImage/FeedLike/FeedComment 엔티티 추가"
```

---

## Task 4: Schedule 도메인

**Files:**
- Create: `src/main/java/com/buddkitv2/domain/schedule/Schedule.java`
- Create: `src/main/java/com/buddkitv2/domain/schedule/ScheduleStatus.java`
- Create: `src/main/java/com/buddkitv2/domain/schedule/UserSchedule.java`
- Create: `src/main/java/com/buddkitv2/domain/schedule/UserScheduleRole.java`

- [ ] **Step 1: ScheduleStatus enum 생성**

```java
// src/main/java/com/buddkitv2/domain/schedule/ScheduleStatus.java
package com.buddkitv2.domain.schedule;

public enum ScheduleStatus {
    RECRUITING,   // 모집 중
    IN_PROGRESS,  // 진행 중
    SETTLING,     // 정산 중
    CLOSED        // 종료
}
```

- [ ] **Step 2: UserScheduleRole enum 생성**

```java
// src/main/java/com/buddkitv2/domain/schedule/UserScheduleRole.java
package com.buddkitv2.domain.schedule;

public enum UserScheduleRole {
    MANAGER,    // 운영진 (스케줄 생성자)
    PARTICIPANT // 참여자
}
```

- [ ] **Step 3: Schedule 엔티티 생성**

```java
// src/main/java/com/buddkitv2/domain/schedule/Schedule.java
package com.buddkitv2.domain.schedule;

import com.buddkitv2.domain.club.Club;
import com.buddkitv2.domain.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "\"SCHEDULE\"")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Schedule extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "schedule_id")
    private Long id;

    private LocalDateTime scheduleTime;

    @Column(length = 20)
    private String name;

    @Column(length = 255)
    private String location;

    private Integer cost;

    @Enumerated(EnumType.STRING)
    private ScheduleStatus status;

    @Column(name = "\"limit\"")
    private Integer limit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "club_id", nullable = false)
    private Club club;

    public static Schedule create(String name, LocalDateTime scheduleTime, String location,
                                   Integer cost, Integer limit, Club club) {
        Schedule s = new Schedule();
        s.name = name;
        s.scheduleTime = scheduleTime;
        s.location = location;
        s.cost = cost;
        s.limit = limit;
        s.club = club;
        s.status = ScheduleStatus.RECRUITING;
        return s;
    }
}
```

- [ ] **Step 4: UserSchedule 엔티티 생성**

```java
// src/main/java/com/buddkitv2/domain/schedule/UserSchedule.java
package com.buddkitv2.domain.schedule;

import com.buddkitv2.domain.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "\"USER_SCHEDULE\"")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_schedule_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id", nullable = false)
    private Schedule schedule;

    @Enumerated(EnumType.STRING)
    private UserScheduleRole role;

    public static UserSchedule create(User user, Schedule schedule, UserScheduleRole role) {
        UserSchedule us = new UserSchedule();
        us.user = user;
        us.schedule = schedule;
        us.role = role;
        return us;
    }
}
```

- [ ] **Step 5: 빌드 확인**

```bash
./gradlew compileJava
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: 커밋**

```bash
git add src/main/java/com/buddkitv2/domain/schedule/
git commit -m "feat(schedule): Schedule/UserSchedule 엔티티 추가"
```

---

## Task 5: Settlement 도메인

**Files:**
- Create: `src/main/java/com/buddkitv2/domain/settlement/Settlement.java`
- Create: `src/main/java/com/buddkitv2/domain/settlement/SettlementStatus.java`
- Create: `src/main/java/com/buddkitv2/domain/settlement/UserSettlement.java`
- Create: `src/main/java/com/buddkitv2/domain/settlement/UserSettlementStatus.java`
- Create: `src/main/java/com/buddkitv2/domain/settlement/UserSettlementType.java`
- Create: `src/main/java/com/buddkitv2/domain/settlement/Transfer.java`

- [ ] **Step 1: SettlementStatus enum 생성**

```java
// src/main/java/com/buddkitv2/domain/settlement/SettlementStatus.java
package com.buddkitv2.domain.settlement;

public enum SettlementStatus {
    REQUESTED,    // 정산 요청
    IN_PROGRESS,  // 진행 중
    COMPLETED     // 완료
}
```

- [ ] **Step 2: UserSettlementStatus enum 생성**

```java
// src/main/java/com/buddkitv2/domain/settlement/UserSettlementStatus.java
package com.buddkitv2.domain.settlement;

public enum UserSettlementStatus {
    REQUESTED,            // 정산 요청
    PENDING_CONFIRMATION, // 정산 확인 대기
    COMPLETED             // 정산 완료
}
```

- [ ] **Step 3: UserSettlementType enum 생성**

```java
// src/main/java/com/buddkitv2/domain/settlement/UserSettlementType.java
package com.buddkitv2.domain.settlement;

public enum UserSettlementType {
    KAKAO_PAY,    // 카카오페이
    BANK_TRANSFER, // 계좌 입금
    CASH          // 현금
}
```

- [ ] **Step 4: Settlement 엔티티 생성**

```java
// src/main/java/com/buddkitv2/domain/settlement/Settlement.java
package com.buddkitv2.domain.settlement;

import com.buddkitv2.domain.common.BaseEntity;
import com.buddkitv2.domain.schedule.Schedule;
import com.buddkitv2.domain.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "\"SETTLEMENT\"")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Settlement extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "settlement_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id", nullable = false)
    private Schedule schedule;

    @Column(precision = 18, scale = 2)
    private BigDecimal sum;

    @Enumerated(EnumType.STRING)
    private SettlementStatus status;

    private LocalDateTime completedTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    public static Settlement create(Schedule schedule, BigDecimal sum, User user) {
        Settlement s = new Settlement();
        s.schedule = schedule;
        s.sum = sum;
        s.user = user;
        s.status = SettlementStatus.REQUESTED;
        return s;
    }
}
```

- [ ] **Step 5: UserSettlement 엔티티 생성**

```java
// src/main/java/com/buddkitv2/domain/settlement/UserSettlement.java
package com.buddkitv2.domain.settlement;

import com.buddkitv2.domain.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "\"USER_SETTLEMENT\"")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserSettlement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_settlement_id")
    private Long id;

    @Enumerated(EnumType.STRING)
    private UserSettlementStatus status;

    @Enumerated(EnumType.STRING)
    private UserSettlementType type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "settlement_id", nullable = false)
    private Settlement settlement;

    private LocalDateTime completedTime;

    public static UserSettlement create(User user, Settlement settlement, UserSettlementType type) {
        UserSettlement us = new UserSettlement();
        us.user = user;
        us.settlement = settlement;
        us.type = type;
        us.status = UserSettlementStatus.REQUESTED;
        return us;
    }
}
```

- [ ] **Step 6: 빌드 확인**

```bash
./gradlew compileJava
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 7: 커밋**

```bash
git add src/main/java/com/buddkitv2/domain/settlement/
git commit -m "feat(settlement): Settlement/UserSettlement 엔티티 추가"
```

---

## Task 6: Wallet 도메인

**Files:**
- Create: `src/main/java/com/buddkitv2/domain/wallet/Wallet.java`
- Create: `src/main/java/com/buddkitv2/domain/wallet/WalletTransaction.java`
- Create: `src/main/java/com/buddkitv2/domain/wallet/WalletTransactionType.java`
- Create: `src/main/java/com/buddkitv2/domain/wallet/Payment.java`
- Create: `src/main/java/com/buddkitv2/domain/wallet/PaymentStatus.java`

- [ ] **Step 1: WalletTransactionType enum 생성**

```java
// src/main/java/com/buddkitv2/domain/wallet/WalletTransactionType.java
package com.buddkitv2.domain.wallet;

public enum WalletTransactionType {
    CHARGE,  // 충전
    TRANSFER // 거래
}
```

- [ ] **Step 2: PaymentStatus enum 생성**

토스페이먼츠 결제 상태값을 따른다.

```java
// src/main/java/com/buddkitv2/domain/wallet/PaymentStatus.java
package com.buddkitv2.domain.wallet;

public enum PaymentStatus {
    READY,            // 결제 준비
    IN_PROGRESS,      // 결제 진행 중
    DONE,             // 결제 완료
    CANCELED,         // 취소
    PARTIAL_CANCELED, // 부분 취소
    ABORTED,          // 결제 승인 실패
    EXPIRED           // 결제 만료
}
```

- [ ] **Step 3: Wallet 엔티티 생성**

```java
// src/main/java/com/buddkitv2/domain/wallet/Wallet.java
package com.buddkitv2.domain.wallet;

import com.buddkitv2.domain.common.BaseEntity;
import com.buddkitv2.domain.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "\"WALLET\"")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Wallet extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "wallet_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private Integer balance;

    public static Wallet create(User user) {
        Wallet w = new Wallet();
        w.user = user;
        w.balance = 0;
        return w;
    }
}
```

- [ ] **Step 4: WalletTransaction 엔티티 생성**

```java
// src/main/java/com/buddkitv2/domain/wallet/WalletTransaction.java
package com.buddkitv2.domain.wallet;

import com.buddkitv2.domain.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "\"WALLET_TRANSACTION\"")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WalletTransaction extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "wallet_transaction_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = false)
    private Wallet wallet;

    // 송금 대상 지갑 (자기 자신일 수도 있음, nullable 아님)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_wallet_id", nullable = false)
    private Wallet targetWallet;

    @Enumerated(EnumType.STRING)
    private WalletTransactionType type;

    private Integer balance;

    public static WalletTransaction create(Wallet wallet, Wallet targetWallet,
                                            WalletTransactionType type, Integer balance) {
        WalletTransaction wt = new WalletTransaction();
        wt.wallet = wallet;
        wt.targetWallet = targetWallet;
        wt.type = type;
        wt.balance = balance;
        return wt;
    }
}
```

- [ ] **Step 5: Payment 엔티티 생성**

`payment_id`는 UUID를 binary(16)으로 저장. 애플리케이션 레이어에서 UUID 생성 후 byte[] 변환.

```java
// src/main/java/com/buddkitv2/domain/wallet/Payment.java
package com.buddkitv2.domain.wallet;

import com.buddkitv2.domain.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "\"Payment\"")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment extends BaseEntity {

    @Id
    @Column(name = "payment_id", columnDefinition = "binary(16)")
    private byte[] paymentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_transaction_id", nullable = false)
    private WalletTransaction walletTransaction;

    @Column(unique = true)
    private String tossPaymentKey;

    private String tossOrderId;

    private String method;

    private Long totalAmount;

    private LocalDateTime approvedAt;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    public static Payment create(byte[] paymentId, WalletTransaction walletTransaction,
                                  String tossOrderId, String method, Long totalAmount) {
        Payment p = new Payment();
        p.paymentId = paymentId;
        p.walletTransaction = walletTransaction;
        p.tossOrderId = tossOrderId;
        p.method = method;
        p.totalAmount = totalAmount;
        p.status = PaymentStatus.READY;
        return p;
    }
}
```

- [ ] **Step 6: 빌드 확인**

```bash
./gradlew compileJava
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 7: 커밋**

```bash
git add src/main/java/com/buddkitv2/domain/wallet/
git commit -m "feat(wallet): Wallet/WalletTransaction/Payment 엔티티 추가"
```

---

## Task 7: Transfer 생성 (Wallet 이후)

Task 5에서 보류한 Transfer를 이제 생성한다.

- [ ] **Step 1: Transfer 엔티티 생성**

```java
// src/main/java/com/buddkitv2/domain/settlement/Transfer.java
package com.buddkitv2.domain.settlement;

import com.buddkitv2.domain.wallet.WalletTransaction;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "\"TRANSFER\"")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Transfer {

    @Id
    @Column(name = "transfer_id")
    private String transferId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_settlement_id", nullable = false)
    private UserSettlement userSettlement;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_transaction_id", nullable = false)
    private WalletTransaction walletTransaction;

    public static Transfer create(String transferId, UserSettlement userSettlement,
                                   WalletTransaction walletTransaction) {
        Transfer t = new Transfer();
        t.transferId = transferId;
        t.userSettlement = userSettlement;
        t.walletTransaction = walletTransaction;
        return t;
    }
}
```

- [ ] **Step 2: 빌드 확인**

```bash
./gradlew compileJava
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: 커밋**

```bash
git add src/main/java/com/buddkitv2/domain/settlement/Transfer.java
git commit -m "feat(settlement): Transfer 엔티티 추가"
```

---

## Task 8: Notification 도메인

**Files:**
- Create: `src/main/java/com/buddkitv2/domain/notification/NotificationTypeEnum.java`
- Create: `src/main/java/com/buddkitv2/domain/notification/NotificationType.java`
- Create: `src/main/java/com/buddkitv2/domain/notification/Notification.java`

- [ ] **Step 1: NotificationTypeEnum 생성**

```java
// src/main/java/com/buddkitv2/domain/notification/NotificationTypeEnum.java
package com.buddkitv2.domain.notification;

public enum NotificationTypeEnum {
    SETTLEMENT_REQUESTED,   // 정산 요청 알림
    SCHEDULE_CREATED,       // 정모 생성 알림
    CHAT                    // 채팅 알림 (push 전송만, 목록 조회 불가)
}
```

- [ ] **Step 2: NotificationType 엔티티 생성**

```java
// src/main/java/com/buddkitv2/domain/notification/NotificationType.java
package com.buddkitv2.domain.notification;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "\"NOTIFICATION_TYPE\"")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "type_id")
    private Long id;

    @Enumerated(EnumType.STRING)
    private NotificationTypeEnum type;

    @Column(columnDefinition = "text")
    private String template;
}
```

- [ ] **Step 3: Notification 엔티티 생성**

```java
// src/main/java/com/buddkitv2/domain/notification/Notification.java
package com.buddkitv2.domain.notification;

import com.buddkitv2.domain.common.BaseEntity;
import com.buddkitv2.domain.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "\"NOTIFICATION\"")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    private Long id;

    // 렌더링된 메시지
    @Column(length = 255)
    private String content;

    private Boolean isRead;

    private Boolean fcmSent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "type_id", nullable = false)
    private NotificationType notificationType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    public static Notification create(String content, NotificationType type, User user) {
        Notification n = new Notification();
        n.content = content;
        n.notificationType = type;
        n.user = user;
        n.isRead = false;
        n.fcmSent = false;
        return n;
    }
}
```

- [ ] **Step 4: 빌드 확인**

```bash
./gradlew compileJava
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: 커밋**

```bash
git add src/main/java/com/buddkitv2/domain/notification/
git commit -m "feat(notification): Notification/NotificationType 엔티티 추가"
```

---

## Task 9: Chat 도메인

**Files:**
- Create: `src/main/java/com/buddkitv2/domain/chat/ChatRoom.java`
- Create: `src/main/java/com/buddkitv2/domain/chat/ChatRoomType.java`
- Create: `src/main/java/com/buddkitv2/domain/chat/UserChatRoom.java`
- Create: `src/main/java/com/buddkitv2/domain/chat/ChatRoomRole.java`
- Create: `src/main/java/com/buddkitv2/domain/chat/Message.java`

- [ ] **Step 1: ChatRoomType enum 생성**

```java
// src/main/java/com/buddkitv2/domain/chat/ChatRoomType.java
package com.buddkitv2.domain.chat;

public enum ChatRoomType {
    CLUB,     // 전체 (모임 전체 채팅)
    SCHEDULE  // 정모 (스케줄별 채팅)
}
```

- [ ] **Step 2: ChatRoomRole enum 생성**

```java
// src/main/java/com/buddkitv2/domain/chat/ChatRoomRole.java
package com.buddkitv2.domain.chat;

public enum ChatRoomRole {
    HOST,   // 방장
    MEMBER  // 일반 참여자
}
```

- [ ] **Step 3: ChatRoom 엔티티 생성**

`schedule_id`는 논리적 연결만 — FK 제약조건 없음 (DDL 주석 참고). 따라서 `Long` 타입으로 저장.

```java
// src/main/java/com/buddkitv2/domain/chat/ChatRoom.java
package com.buddkitv2.domain.chat;

import com.buddkitv2.domain.club.Club;
import com.buddkitv2.domain.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "\"CHAT_ROOM\"")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatRoom extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chat_room_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "club_id", nullable = false)
    private Club club;

    // 논리적 연결만, FK 제약조건 없음
    @Column(name = "schedule_id")
    private Long scheduleId;

    @Enumerated(EnumType.STRING)
    private ChatRoomType type;

    public static ChatRoom createClubRoom(Club club) {
        ChatRoom cr = new ChatRoom();
        cr.club = club;
        cr.type = ChatRoomType.CLUB;
        return cr;
    }

    public static ChatRoom createScheduleRoom(Club club, Long scheduleId) {
        ChatRoom cr = new ChatRoom();
        cr.club = club;
        cr.scheduleId = scheduleId;
        cr.type = ChatRoomType.SCHEDULE;
        return cr;
    }
}
```

- [ ] **Step 4: UserChatRoom 엔티티 생성**

`user_chat_room_id`와 `Key` 컬럼 모두 VARCHAR(255). `Key`는 예약어이므로 반드시 escape.

```java
// src/main/java/com/buddkitv2/domain/chat/UserChatRoom.java
package com.buddkitv2.domain.chat;

import com.buddkitv2.domain.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "\"USER_CHAT_ROOM\"")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserChatRoom {

    @Id
    @Column(name = "user_chat_room_id")
    private String userChatRoomId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id", nullable = false)
    private ChatRoom chatRoom;

    @Enumerated(EnumType.STRING)
    private ChatRoomRole role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 메시지 읽음 위치 추적용 키
    @Column(name = "\"Key\"", nullable = false)
    private String readKey;

    public static UserChatRoom create(String id, ChatRoom chatRoom, User user,
                                       ChatRoomRole role, String readKey) {
        UserChatRoom ucr = new UserChatRoom();
        ucr.userChatRoomId = id;
        ucr.chatRoom = chatRoom;
        ucr.user = user;
        ucr.role = role;
        ucr.readKey = readKey;
        return ucr;
    }
}
```

- [ ] **Step 5: Message 엔티티 생성**

`Key` 컬럼은 예약어이므로 escape. `sentAt`은 DDL 그대로 camelCase 유지.

```java
// src/main/java/com/buddkitv2/domain/chat/Message.java
package com.buddkitv2.domain.chat;

import com.buddkitv2.domain.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "\"MESSAGE\"")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "message_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id", nullable = false)
    private ChatRoom chatRoom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(length = 255)
    private String text;

    private LocalDateTime sentAt;

    private Boolean deleted;

    // 메시지 순서/저장 키
    @Column(name = "\"Key\"", nullable = false)
    private String messageKey;

    public static Message create(ChatRoom chatRoom, User user, String text, String messageKey) {
        Message m = new Message();
        m.chatRoom = chatRoom;
        m.user = user;
        m.text = text;
        m.sentAt = LocalDateTime.now();
        m.deleted = false;
        m.messageKey = messageKey;
        return m;
    }
}
```

- [ ] **Step 6: 빌드 확인**

```bash
./gradlew compileJava
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 7: 커밋**

```bash
git add src/main/java/com/buddkitv2/domain/chat/
git commit -m "feat(chat): ChatRoom/UserChatRoom/Message 엔티티 추가"
```

---

## Task 10: 최종 빌드 검증

- [ ] **Step 1: 전체 빌드**

```bash
./gradlew build
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 2: 단위 테스트 실행 (기존 테스트)**

```bash
./gradlew test
```
Expected: BUILD SUCCESSFUL (기존 테스트 통과 확인)

---

## 작업 순서 요약

의존성에 따른 권장 실행 순서:

1. Task 1 (User/Interest/Address) — 다른 모든 도메인이 User 참조
2. Task 2 (Club) — Schedule/Feed가 Club 참조
3. Task 3 (Feed) — 독립적 (Club, User 이후)
4. Task 4 (Schedule) — 독립적 (Club, User 이후)
5. Task 5 (Settlement, Transfer 제외) — Schedule, User 이후
6. Task 6 (Wallet) — User 이후
7. Task 7 (Transfer) — Settlement + Wallet 이후
8. Task 8 (Notification) — User 이후
9. Task 9 (Chat) — Club, User 이후
10. Task 10 (최종 빌드 검증)
