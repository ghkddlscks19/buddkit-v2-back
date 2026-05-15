# Roadmap

## 현재 단계
**1단계 — 인프라 기반** (진행 중)

---

## 1단계 — 인프라 기반

**목표:** 모든 도메인이 공통으로 의존하는 기반 설정 완료

**완료 기준:**
- [ ] application.yml 설정 (DB, Redis, Kafka, OAuth2)
- [ ] Spring Security 기본 설정 (SecurityFilterChain)
- [ ] 공통 응답 포맷 (`ApiResponse<T>`)
- [ ] 공통 예외 처리 (`GlobalExceptionHandler`)
- [ ] JPA BaseEntity (createdAt, updatedAt)

**완료된 항목:**
- [x] application.yml 설정 (DB 연결 확인, Kakao OAuth2 등록 확인)
- [x] 프로젝트 패키지 구조 정의

---

## 2단계 — USER 도메인

**목표:** 회원가입/로그인/프로필 관리

**선행 조건:** 1단계 완료

**완료 기준:**
- [ ] USER, INTEREST, USER_INTEREST, Address 엔티티
- [ ] 카카오 OAuth2 로그인 후 회원 저장 (CustomOAuth2UserService)
- [ ] JWT 또는 세션 기반 인증 처리
- [ ] 회원 정보 조회/수정 API
- [ ] 관심사 설정/수정 API
- [ ] 지역 설정/수정 API

---

## 3단계 — CLUB 도메인

**목표:** 모임 생성/가입/관리

**선행 조건:** 2단계 완료 (모임은 회원을 참조)

**완료 기준:**
- [ ] CLUB, USER_CLUB, CLUB_LIKE 엔티티
- [ ] 모임 생성/수정/삭제 API (모임장만 가능)
- [ ] 모임 가입/탈퇴 API
- [ ] 내 모임 목록 조회 API
- [ ] 모임 상세 조회 API
- [ ] 모임장/운영진 역할 구분 처리

---

## 4단계 — FEED 도메인

**목표:** 모임 내 사진+글 게시물 관리

**선행 조건:** 3단계 완료 (피드는 모임에 속함)

**완료 기준:**
- [ ] FEED, FEED_IMAGE, FEED_LIKE, FEED_COMMENT 엔티티
- [ ] 피드 생성/수정/삭제 API
- [ ] 피드 목록/상세 조회 API (무한 스크롤)
- [ ] 피드 좋아요/취소 API
- [ ] S3 이미지 업로드 연동 (썸네일 압축 포함)

---

## 5단계 — SCHEDULE + 정산/결제

**목표:** 정기모임 생성 및 참가비 정산

**선행 조건:** 3단계 완료 (스케줄은 모임에 속함)

**완료 기준:**
- [ ] SCHEDULE, USER_SCHEDULE 엔티티
- [ ] 스케줄 생성/수정/삭제 API (운영진만)
- [ ] 스케줄 참여/취소 API
- [ ] 스케줄 상태 전이 (모집 중 → 진행 중 → 정산 중 → 종료)
- [ ] WALLET, WALLET_TRANSACTION 엔티티
- [ ] SETTLEMENT, USER_SETTLEMENT, TRANSFER 엔티티
- [ ] 토스페이먼츠 결제 연동 (가상계좌, 카드)
- [ ] 정산 상태 관리 및 수동 변경 API

---

## 6단계 — NOTIFICATION

**목표:** 이벤트 기반 인앱 알림 + FCM 푸시

**선행 조건:** 5단계 완료 (정산 이벤트 필요)

**완료 기준:**
- [ ] NOTIFICATION, NOTIFICATION_TYPE 엔티티
- [ ] Kafka 이벤트 기반 알림 생성 (정산, 정모 생성)
- [ ] FCM 푸시 전송 연동
- [ ] 알림 목록/상세 조회 API
- [ ] 알림 읽음 처리 API

---

## 7단계 — CHAT

**목표:** 모임/정모별 실시간 채팅

**선행 조건:** 3단계, 5단계 완료

**완료 기준:**
- [ ] CHAT_ROOM, USER_CHAT_ROOM, MESSAGE 엔티티
- [ ] WebSocket(STOMP) + Redis pub/sub 구성
- [ ] 채팅방 자동 생성 (모임/정모 생성 시)
- [ ] 메시지 전송/삭제 API
- [ ] 채팅 메시지 목록 조회 API (무한 스크롤)
- [ ] 읽음 위치 추적 (USER_CHAT_ROOM.Key)

---

## 8단계 — SEARCH

**목표:** 모임 검색 및 맞춤 추천

**선행 조건:** 3단계 완료

**완료 기준:**
- [ ] 키워드 검색 API (모임명, 해시태그 — 부분 문자열 매칭)
- [ ] 관심사 필터 검색 API
- [ ] 지역 필터 검색 API
- [ ] 맞춤 모임 추천 API (관심사 + 지역 기반)
- [ ] 무한 스크롤 구현
