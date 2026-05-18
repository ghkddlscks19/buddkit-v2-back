# Search 도메인 설계

**날짜:** 2026-05-18  
**범위:** `domain/search/` 신규 구현 + `ClubService` Kafka emit 추가

---

## 1. 개요

모임(Club) 검색 및 추천 API. 5가지 기능을 하나의 도메인으로 구현한다.

| 기능 | 설명 |
|------|------|
| 맞춤 모임 추천 | 내 관심사 + 지역 기반 스코어링 |
| 필터 검색 | 관심사 / 지역 단독 필터 |
| 통합 검색 | 키워드(모임명+description) + 관심사/지역 필터 조합 |
| co-member 추천 | 내 모임 멤버들이 가입한 다른 모임 추천 |
| 행동 이벤트 emit | 클릭/조회 이벤트를 Kafka로 emit (Phase 2 추천 고도화용) |

---

## 2. 전체 아키텍처

```
[ClubService]  ──club-events──▶  [SearchEventConsumer]  ──▶  ES club 인덱스
                                                                    │
[SearchController]  ──▶  [SearchService]  ──────────────────────────┘
                                │
                    UserClubRepository (co-member 추천, JPA)
                    UserInterestRepository (내 관심사 조회, JPA)

[SearchController]  ──user-behavior-events──▶  ES user-behavior 인덱스
                                                (Phase 2: 추천 점수 보정)
```

**핵심 원칙:**
- ClubService는 ES를 직접 호출하지 않는다. Kafka 이벤트만 emit.
- ES 장애가 Club CRUD에 영향을 주지 않는다 (eventual consistency).
- co-member 추천은 JPA 쿼리로 처리 (관계형 집계가 ES보다 적합).

---

## 3. API 엔드포인트

| Method | URL | 인증 | 설명 |
|--------|-----|------|------|
| `GET` | `/search/clubs/recommend` | 필요 | 맞춤 모임 추천 |
| `GET` | `/search/clubs` | 필요 | 키워드+필터 통합 검색 |
| `GET` | `/search/clubs/co-members` | 필요 | co-member 기반 추천 |
| `POST` | `/search/clubs/{clubId}/view` | 필요 | 조회 이벤트 emit |

### Query Parameters — `GET /search/clubs`

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| `keyword` | String | 선택 | 모임명 또는 description 부분 매칭 |
| `interestCategory` | InterestCategory | 선택 | 관심사 필터 |
| `city` | String | 선택 | 시/도 필터 |
| `district` | String | 선택 | 시/군/구 필터 (city 없이 단독 사용 불가) |
| `lastId` | Long | 선택 | cursor 기반 페이지네이션 (keyword/filter 검색에만 사용) |
| `size` | int | 선택 | 기본값 20 |

### 공통 응답 (ClubSearchResponse)

```json
{
  "success": true,
  "data": [
    {
      "clubId": 1,
      "name": "서울 러닝크루",
      "description": "함께 달리는 모임",
      "clubImage": "https://...",
      "memberCount": 12,
      "userLimit": 30,
      "city": "서울특별시",
      "district": "마포구",
      "interestCategory": "SPORTS",
      "interestName": "운동/스포츠"
    }
  ],
  "message": null
}
```

---

## 4. Elasticsearch 인덱스 설계

### `club` 인덱스

```json
{
  "mappings": {
    "properties": {
      "clubId":           { "type": "long" },
      "name":             { "type": "text", "analyzer": "nori" },
      "description":      { "type": "text", "analyzer": "nori" },
      "city":             { "type": "keyword" },
      "district":         { "type": "keyword" },
      "interestCategory": { "type": "keyword" },
      "interestName":     { "type": "keyword" },
      "memberCount":      { "type": "integer" },
      "userLimit":        { "type": "integer" },
      "clubImage":        { "type": "keyword", "index": false },
      "deletedAt":        { "type": "date" }
    }
  }
}
```

- `name` / `description`: nori 형태소 분석 → 한국어 부분 매칭 가능
- 통합검색 쿼리: `multi_match` with `name^3, description^1` (name 가중치 3배)

### `user-behavior` 인덱스 (Phase 2용, Phase 1에서 emit만)

```json
{
  "mappings": {
    "properties": {
      "userId":       { "type": "long" },
      "clubId":       { "type": "long" },
      "eventType":    { "type": "keyword" },
      "keyword":      { "type": "keyword" },
      "dwellSeconds": { "type": "integer" },
      "timestamp":    { "type": "date" }
    }
  }
}
```

---

## 5. Kafka 토픽

| 토픽 | 발행자 | 소비자 | 이벤트 타입 |
|------|--------|--------|-------------|
| `club-events` | ClubService | SearchEventConsumer | CREATED, UPDATED, DELETED |
| `user-behavior-events` | SearchController | BehaviorEventConsumer (Phase 2) | VIEW |

### ClubEventPayload

```java
{ eventType, clubId, name, description, city, district,
  interestCategory, interestName, memberCount, userLimit,
  clubImage, deletedAt }
```

### BehaviorEventPayload

```java
{ userId, clubId, eventType, dwellSeconds, timestamp }
```

---

## 6. 추천 알고리즘

### 맞춤 모임 추천 (ES function_score)

**조건:** `deletedAt = null` + 이미 가입한 모임 제외

**스코어 가중치:**

| 조건 | 점수 |
|------|------|
| 관심사 category 일치 (내 관심사 중 하나) | +3 |
| city 일치 | +2 |
| district까지 일치 | +1 |

동점 시 `memberCount` 내림차순 정렬. `page` + `size` 기반 페이지네이션 (`recommend`, `co-members`는 ES/JPA 스코어 순이라 cursor lastId 불가).

### co-member 추천 (JPA)

내가 속한 모임의 다른 멤버들이 가입한 모임을 겹치는 멤버 수 기준으로 정렬. 이미 내가 가입한 모임은 제외.

```sql
SELECT uc2.club, COUNT(uc2.user) as overlap
FROM UserClub uc1
JOIN UserClub uc2 ON uc1.user = uc2.user
WHERE uc1.user.id = :userId
  AND uc2.club NOT IN (내가 가입한 clubs)
  AND uc2.club.deletedAt IS NULL
GROUP BY uc2.club
ORDER BY overlap DESC
LIMIT :size
```

---

## 7. 패키지 구조

```
domain/search/
├── controller/
│   └── SearchController.java
├── dto/
│   ├── request/
│   │   └── ClubViewRequest.java
│   └── response/
│       └── ClubSearchResponse.java
├── document/
│   └── ClubDocument.java
├── event/
│   ├── ClubEventPayload.java
│   └── BehaviorEventPayload.java
├── repository/
│   ├── ClubSearchRepository.java
│   ├── ClubSearchRepositoryCustom.java
│   └── ClubSearchRepositoryImpl.java
└── service/
    ├── SearchService.java
    └── SearchEventConsumer.java
```

**ClubService 변경 (최소):**
- `createClub`, `updateClub`, `deleteClub` 완료 후 `club-events` 토픽 emit 추가
- KafkaTemplate 의존성 추가만. ES 직접 의존 없음.

---

## 8. Phase 2 — 로그 기반 추천 고도화 (설계 범위 외)

Phase 1에서 쌓은 `user-behavior` 데이터를 활용해 추천 점수를 보정한다.

- **클릭/체류 시간 신호**: 자주 클릭하거나 오래 머문 관심사/지역 카테고리의 모임에 가중치 추가
- **검색 키워드 패턴**: 자주 검색하는 키워드로 관심사 추론
- **Collaborative filtering**: 비슷한 행동 패턴의 유저들이 가입한 모임 추천
- 구현 시점: user-behavior 데이터 충분히 쌓인 이후

---

## 9. 예외 처리

| 상황 | 예외 |
|------|------|
| `district` 단독 사용 (city 없음) | `InvalidSearchConditionException` |
| ES 장애 | Kafka 재시도로 eventual consistency 보장, 검색 요청은 ES 가용 시에만 응답 |
