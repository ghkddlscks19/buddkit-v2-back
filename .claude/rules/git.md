# Git Commit Rules

## Commit Types

| type | 의미 |
|---|---|
| `feat` | 사용자에게 보이는 기능 추가 |
| `fix` | 버그 수정 |
| `docs` | 문서 변경 |
| `test` | 테스트 추가 또는 수정 |
| `refactor` | 동작 변경 없는 구조 개선 |
| `style` | 포맷, 세미콜론 등 의미 없는 스타일 변경 |
| `chore` | 빌드, 설정, 관리 작업 |

## Format

```
<type>(<scope>): <subject>

[optional body]
```

- `scope`: 변경된 도메인 또는 모듈 (예: `user`, `club`, `schedule`, `settlement`)
- `subject`: 변경 의도를 담은 한 줄 요약. 행위("수정", "업데이트")가 아닌 의미를 적는다.

**나쁜 예:**
```
수정
업데이트
코드 정리
버그
```

**좋은 예:**
```
feat(user): 카카오 소셜 로그인 및 회원가입 플로우 구현
fix(schedule): 시작 시간 경과 후 상태 전이 누락 수정
refactor(club): 모임장 권한 위임 로직을 서비스 계층으로 분리
chore: application.yml 템플릿 추가 및 gitignore 설정
```

## Body가 필요한 경우

다음 상황에서는 본문을 작성한다:
- 버그 원인이 복잡하다
- 동작 변경이 있다
- 마이그레이션이나 설정 변경이 있다
- 테스트 범위를 설명해야 한다
- 의도적으로 하지 않은 일이 있다

본문 예시:
```
fix(settlement): 정산 완료 상태에서 중복 TRANSFER 생성 방지

SETTLEMENT가 완료 상태일 때 재호출 시 TRANSFER가 중복 생성되는 문제.
멱등성 보장을 위해 기존 TRANSFER 존재 여부를 먼저 확인하도록 수정.
인증 흐름 자체는 변경하지 않음.
```

## 커밋 전 체크리스트

커밋을 제안하기 전에 반드시 확인:

1. `git status`와 `git diff`로 변경사항 파악
2. 변경사항이 **하나의 주제**로 묶이는지 확인 — 아니라면 커밋을 분리 제안
3. `src/main/resources/application*.yml`이 포함되지 않았는지 확인
4. 빌드가 통과하는 상태인지 확인 (`./gradlew build`)
5. diff에 없는 내용을 메시지에 과장해서 쓰지 않는다

## 커밋 메시지 제안 방식

커밋을 제안할 때는 다음 순서로 진행한다:

1. 변경사항이 하나의 커밋으로 적절한지 판단
2. 분리가 필요하면 커밋 단위와 순서를 먼저 제안
3. Conventional Commit 형식 후보 3개 제시
4. 각 후보가 적합한 이유를 한 줄로 설명
5. 아직 커밋하지 않고 사용자 승인을 받는다
