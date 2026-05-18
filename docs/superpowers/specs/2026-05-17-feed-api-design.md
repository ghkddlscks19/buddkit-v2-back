# Feed API 설계

## 개요

모임(Club) 내 피드(Feed) 생성·조회·좋아요·댓글을 처리하는 API.

## 결정 사항

| 항목 | 결정 |
|---|---|
| URL 구조 | 전체 중첩 `/clubs/{clubId}/feeds/{feedId}/...` |
| 이미지 처리 | URL만 수신 (프론트엔드가 S3 presigned URL로 업로드 후 URL 전달) |
| 이미지 수정 | 전체 교체 (기존 FeedImage 삭제 → 새 목록 insert) |
| 좋아요 수 | `Feed.likeCount` 비정규화 컬럼 (좋아요/취소 시 +1/-1) |
| 정렬 | `?sort=LATEST` (기본, feed_id DESC) / `?sort=POPULAR` (like_count DESC, feed_id DESC) |
| 페이지네이션 | cursor 기반 무한스크롤 (피드 목록, 댓글 목록) |
| 접근 권한 | 모든 엔드포인트 모임 멤버 전용 |
| 피드 삭제 권한 | 작성자만 |
| 댓글 삭제/수정 권한 | 댓글 작성자만 |
| 서비스 구조 | FeedService 단일 서비스 (CRUD + 좋아요 + 댓글) |
| 소프트 딜리트 | Feed, FeedComment → `deletedAt`. FeedLike → 하드 딜리트 |

---

## API 엔드포인트

```
# 피드 CRUD
POST   /clubs/{clubId}/feeds                                      피드 생성       [모임멤버]
PATCH  /clubs/{clubId}/feeds/{feedId}                            피드 수정       [작성자]
DELETE /clubs/{clubId}/feeds/{feedId}                            피드 삭제       [작성자]
GET    /clubs/{clubId}/feeds                                      피드 목록       [모임멤버] cursor + sort
GET    /clubs/{clubId}/feeds/{feedId}                            피드 상세       [모임멤버]

# 좋아요
POST   /clubs/{clubId}/feeds/{feedId}/likes                      좋아요          [모임멤버]
DELETE /clubs/{clubId}/feeds/{feedId}/likes/me                   좋아요 취소     [모임멤버]

# 댓글
POST   /clubs/{clubId}/feeds/{feedId}/comments                   댓글 생성       [모임멤버]
PATCH  /clubs/{clubId}/feeds/{feedId}/comments/{commentId}       댓글 수정       [댓글 작성자]
DELETE /clubs/{clubId}/feeds/{feedId}/comments/{commentId}       댓글 삭제       [댓글 작성자]
GET    /clubs/{clubId}/feeds/{feedId}/comments                   댓글 목록       [모임멤버] cursor
```

---

## 엔티티 변경

### Feed.java — 수정
```java
// 추가할 필드
@Column(name = "like_count", nullable = false)
private Long likeCount = 0L;

// 추가할 메서드
public void update(String content) {
    this.content = content;
}

public void softDelete() {
    // BaseEntity.softDelete() 상속 — 이미 추가됨
}

public void incrementLike() {
    this.likeCount++;
}

public void decrementLike() {
    if (this.likeCount > 0) this.likeCount--;
}
```

### FeedComment.java — 수정
```java
// 추가할 메서드
public void update(String content) {
    this.content = content;
}

public void softDelete() {
    // BaseEntity.softDelete() 상속 — 이미 추가됨
}
```

---

## 정렬 방식

피드 목록 조회 시 `?sort=` 파라미터:

| 값 | 정렬 기준 | 페이지네이션 |
|---|---|---|
| `LATEST` (기본값) | `ORDER BY f.id DESC` | cursor — `?lastId=&size=` |
| `POPULAR` | `ORDER BY f.likeCount DESC, f.id DESC` | offset — `?page=&size=` |

- **LATEST**: `WHERE f.club.id = :clubId AND f.id < :lastId AND f.deletedAt IS NULL ORDER BY f.id DESC`
- **POPULAR**: `WHERE f.club.id = :clubId AND f.deletedAt IS NULL ORDER BY f.likeCount DESC, f.id DESC` + `PageRequest.of(page, size)`
- 두 정렬 모드는 파라미터가 다르므로 컨트롤러에서 `sort` 값에 따라 분기 처리. 프론트엔드는 sort 변경 시 페이지 상태를 초기화한다.

---

## 서비스 책임

### FeedService

| 메서드 | 핵심 로직 |
|---|---|
| `createFeed` | 멤버 확인 → Feed.create() → FeedImage 1~5개 저장 |
| `updateFeed` | 멤버 확인 → 작성자 확인 → feed.update() → FeedImage 전체 교체 |
| `deleteFeed` | 멤버 확인 → 작성자 확인 → feed.softDelete() |
| `getFeeds` | 멤버 확인 → sort에 따라 cursor(LATEST) 또는 offset(POPULAR) 조회 |
| `getFeed` | 멤버 확인 → 피드 + 이미지 목록 + 좋아요 수 + 내 좋아요 여부 반환 |
| `likeFeed` | 멤버 확인 → 중복 확인(AlreadyLikedException) → FeedLike 저장 → feed.incrementLike() |
| `unlikeFeed` | 멤버 확인 → FeedLike 조회(NotLikedException) → 삭제 → feed.decrementLike() |
| `createComment` | 멤버 확인 → FeedComment.create() 저장 |
| `updateComment` | 멤버 확인 → 댓글 작성자 확인 → comment.update() |
| `deleteComment` | 멤버 확인 → 댓글 작성자 확인 → comment.softDelete() |
| `getComments` | 멤버 확인 → cursor 기반 댓글 목록 (comment_id ASC) |

---

## 파일 목록

### 신규 생성

```
domain/feed/
├── controller/FeedController.java
├── dto/request/
│   ├── FeedCreateRequest.java       (@Getter @Setter @NoArgsConstructor)
│   ├── FeedUpdateRequest.java       (@Getter @Setter @NoArgsConstructor)
│   └── FeedCommentRequest.java      (@Getter @Setter @NoArgsConstructor)
├── dto/response/
│   ├── FeedResponse.java            (@Getter @AllArgsConstructor)
│   └── FeedCommentResponse.java     (@Getter @AllArgsConstructor)
├── repository/
│   ├── FeedRepository.java
│   ├── FeedImageRepository.java
│   ├── FeedLikeRepository.java
│   └── FeedCommentRepository.java
└── service/FeedService.java

global/exception/
├── FeedNotFoundException.java
├── FeedAccessDeniedException.java
├── FeedCommentNotFoundException.java
├── AlreadyLikedException.java
└── NotLikedException.java
```

### 기존 파일 수정

```
domain/feed/entity/Feed.java
  → likeCount 필드 추가, update() / incrementLike() / decrementLike() 메서드 추가

domain/feed/entity/FeedComment.java
  → update() 메서드 추가

global/exception/GlobalExceptionHandler.java
  → 신규 예외 핸들러 5개 추가
```

---

## DTO 구조

### Request

```java
// FeedCreateRequest
String content;           // nullable (이미지만 있는 피드 허용)
List<String> imageUrls;  // @NotEmpty @Size(min=1, max=5)

// FeedUpdateRequest
String content;
List<String> imageUrls;  // @NotEmpty @Size(min=1, max=5)

// FeedCommentRequest
@NotBlank String content;
```

### Response

```java
// FeedResponse (목록·상세 공용)
Long feedId;
Long userId;
String nickname;
String profileImageUrl;
String content;
List<String> imageUrls;
Long likeCount;
boolean isLiked;           // 내 좋아요 여부
LocalDateTime createdAt;

// FeedCommentResponse
Long commentId;
Long userId;
String nickname;
String profileImageUrl;
String content;
LocalDateTime createdAt;
```

---

## 권한 체크 방식

모든 엔드포인트에서 `@AuthenticationPrincipal Long userId`로 인증.

- **모임 멤버 확인**: `UserClubRepository.findByClub_IdAndUser_Id(clubId, userId)` 존재 여부
- **피드 작성자 확인**: `feed.getUser().getId().equals(userId)`
- **댓글 작성자 확인**: `comment.getUser().getId().equals(userId)`
- **clubId·feedId 정합성**: `feed.getClub().getId().equals(clubId)` + `deletedAt IS NULL`

---

## 예외 처리

| 상황 | 예외 | HTTP |
|---|---|---|
| 피드 없음 / clubId 불일치 | FeedNotFoundException | 404 |
| 모임 미소속 / 작성자 아님 | FeedAccessDeniedException | 403 |
| 댓글 없음 / feedId 불일치 | FeedCommentNotFoundException | 404 |
| 이미 좋아요 | AlreadyLikedException | 409 |
| 좋아요 안 함 | NotLikedException | 404 |
