# CLUB 도메인 설계

**날짜:** 2026-05-17  
**브랜치:** feat/user → feat/club 예정

---

## 개요

관심사 기반 소셜 모임(Club) 도메인의 핵심 CRUD API 설계. 모임 생성/수정/조회, 가입/탈퇴, 관심 모임 등록/취소 7개 엔드포인트를 구현한다.

---

## API 엔드포인트

| Method | URL | 설명 | 권한 |
|--------|-----|------|------|
| POST | `/clubs` | 모임 생성 | 인증 필요 |
| PATCH | `/clubs/{clubId}` | 모임 수정 | 인증 + 모임장만 |
| GET | `/clubs/{clubId}` | 모임 상세 조회 | 인증 필요 |
| POST | `/clubs/{clubId}/members` | 모임 가입 | 인증 필요 |
| DELETE | `/clubs/{clubId}/members/me` | 모임 탈퇴 | 인증 필요 |
| POST | `/clubs/{clubId}/like` | 관심 모임 등록 | 인증 필요 |
| DELETE | `/clubs/{clubId}/like` | 관심 모임 취소 | 인증 필요 |

모든 엔드포인트는 JWT 인증 필요. `@AuthenticationPrincipal Long userId`로 요청자 식별.

---

## DTO

### Request

**ClubCreateRequest** / **ClubUpdateRequest** (동일 필드)
```
name            String           모임명 (필수)
userLimit       Integer          최대 인원 (필수)
description     String           소개 (필수)
clubImage       String           S3 presigned URL로 업로드 후 전달받은 URL (nullable)
city            String           시/도 (필수)
district        String           시/군/구 (필수)
interestCategory InterestCategory 관심사 카테고리 (필수)
```

- 클라이언트가 presigned URL로 S3에 직접 업로드한 뒤, 결과 URL을 문자열로 전달
- `clubId`는 수정 시 URL 경로(`/clubs/{clubId}`)에서 받으므로 Request body에 포함하지 않음

### Response

**ClubDetailResponse** (생성/수정/상세 조회 공통 사용)
```
clubId          Long
name            String
description     String
clubImage       String (nullable)
userLimit       Integer
memberCount     Integer
city            String
district        String
interestCategory InterestCategory
interestName    String
isLiked         boolean          요청자의 관심 모임 등록 여부
isMember        boolean          요청자의 가입 여부
myRole          String (nullable) 가입된 경우 LEADER / MEMBER
```

---

## 서비스 로직

### 모임 생성 (`createClub`)
1. userId로 User 조회
2. city + district로 Address 조회 → 없으면 `InvalidAddressException`
3. interestCategory로 Interest 조회 → 없으면 `InvalidInterestException`
4. `Club.create()` 저장 (memberCount = 1로 초기화)
5. `UserClub.create(club, user, LEADER)` 저장
6. `ClubDetailResponse` 반환 (isLiked=false, isMember=true, myRole=LEADER)

### 모임 수정 (`updateClub`)
1. clubId로 Club 조회 → 없으면 `ClubNotFoundException`
2. UserClub에서 userId + clubId로 역할 확인 → LEADER 아니면 `ClubAccessDeniedException`
3. Address, Interest 조회
4. `club.update(name, userLimit, description, clubImage, address, interest)` 호출
5. `ClubDetailResponse` 반환

### 모임 상세 조회 (`getClub`)
1. clubId로 Club 조회 → 없으면 `ClubNotFoundException`
2. `ClubLikeRepository.existsByClub_IdAndUser_Id`로 isLiked 조회
3. `UserClubRepository.findByClub_IdAndUser_Id`로 isMember + myRole 조회
4. `ClubDetailResponse` 반환

### 모임 가입 (`joinClub`)
1. clubId로 Club 조회 → 없으면 `ClubNotFoundException`
2. `UserClubRepository.existsByClub_IdAndUser_Id`로 이미 가입 여부 확인 → `AlreadyJoinedClubException`
3. `club.getMemberCount() >= club.getUserLimit()` → `ClubFullException`
4. userId로 User 조회
5. `UserClub.create(club, user, MEMBER)` 저장
6. `club.incrementMemberCount()`

### 모임 탈퇴 (`leaveClub`)
1. clubId로 Club 조회 → 없으면 `ClubNotFoundException`
2. `UserClubRepository.findByClub_IdAndUser_Id`로 UserClub 조회 → 없으면 `NotJoinedClubException`
3. role이 LEADER면 `ClubLeaderCannotLeaveException`
4. UserClub 삭제
5. `club.decrementMemberCount()`

### 관심 모임 등록 (`likeClub`)
1. clubId로 Club 조회 → 없으면 `ClubNotFoundException`
2. `ClubLikeRepository.existsByClub_IdAndUser_Id`로 이미 등록 여부 확인 → `AlreadyLikedClubException`
3. userId로 User 조회
4. `ClubLike.create(user, club)` 저장

### 관심 모임 취소 (`unlikeClub`)
1. `ClubLikeRepository.findByClub_IdAndUser_Id`로 ClubLike 조회 → 없으면 `ClubLikeNotFoundException`
2. ClubLike 삭제

---

## 예외 처리

| 예외 클래스 | HTTP 상태 | 상황 |
|------------|-----------|------|
| `ClubNotFoundException` | 404 | 존재하지 않는 clubId |
| `ClubAccessDeniedException` | 403 | 모임장이 아닌 사용자가 수정 시도 |
| `ClubFullException` | 409 | 최대 인원 초과 |
| `AlreadyJoinedClubException` | 409 | 이미 가입된 모임에 재가입 시도 |
| `NotJoinedClubException` | 400 | 가입하지 않은 모임 탈퇴 시도 |
| `ClubLeaderCannotLeaveException` | 400 | 모임장의 탈퇴 시도 |
| `AlreadyLikedClubException` | 409 | 이미 등록된 관심 모임 재등록 시도 |
| `ClubLikeNotFoundException` | 404 | 등록하지 않은 관심 모임 취소 시도 |

모든 예외는 `GlobalExceptionHandler`에 핸들러 추가.

---

## 신규 파일

```
domain/club/
├── controller/ClubController.java
├── service/ClubService.java
├── repository/ClubRepository.java
├── dto/request/
│   ├── ClubCreateRequest.java
│   └── ClubUpdateRequest.java
└── dto/response/
    └── ClubDetailResponse.java

global/exception/
├── ClubNotFoundException.java
├── ClubAccessDeniedException.java
├── ClubFullException.java
├── AlreadyJoinedClubException.java
├── NotJoinedClubException.java
├── ClubLeaderCannotLeaveException.java
├── AlreadyLikedClubException.java
└── ClubLikeNotFoundException.java
```

---

## 기존 파일 변경

### Club.java
```java
// 추가 메서드 3개
update(String name, Integer userLimit, String description, String clubImage, Address address, Interest interest)
incrementMemberCount()
decrementMemberCount()
```

### UserClubRepository.java
```java
Optional<UserClub> findByClub_IdAndUser_Id(Long clubId, Long userId)
boolean existsByClub_IdAndUser_Id(Long clubId, Long userId)
```

### ClubLikeRepository.java
```java
Optional<ClubLike> findByClub_IdAndUser_Id(Long clubId, Long userId)
boolean existsByClub_IdAndUser_Id(Long clubId, Long userId)
```

### GlobalExceptionHandler.java
신규 예외 8개 핸들러 추가.
