---
description: Phase 완료 후 JavaDoc, IMPLEMENTATION_PLAN.md, DEV_LOG.md 일괄 업데이트
---

# Update All Documentation

Phase를 완료한 후, 변경된 코드와 진행 상황에 맞춰 모든 문서를 일괄 업데이트합니다.

## 작업 순서

### 1. 변경 사항 파악
먼저 현재 Phase에서 변경된 내용을 전체적으로 파악합니다.

```bash
git status
git diff
```

**확인 항목:**
- 새로 생성된 파일
- 수정된 파일
- 삭제된 파일
- 주요 변경 내용

### 2. JavaDoc 업데이트

#### 2.1 업데이트 대상 파일 확인
변경되거나 새로 생성된 Java 파일의 JavaDoc을 확인합니다:
- Service 클래스
- Controller 메서드
- Repository 인터페이스
- Exception 클래스
- Enum 클래스

#### 2.2 JavaDoc 검증 및 업데이트
각 파일의 JavaDoc이 현재 구현과 일치하는지 확인하고 업데이트합니다.

**확인 사항:**
- [ ] 클래스 설명이 정확한가?
- [ ] 메서드 설명이 현재 동작과 일치하는가?
- [ ] `@param` 설명이 정확한가?
- [ ] `@return` 설명이 정확한가?
- [ ] `@throws` 정보가 실제와 일치하는가?
- [ ] 검증 책임 위치가 명시되어 있는가?
- [ ] 사용 예시가 적절한가?
- [ ] `@see` 링크가 올바른가?

**업데이트 예시:**
```java
/**
 * 유저의 포인트를 충전합니다.
 * <p>
 * 충전 금액은 Controller 레이어에서 {@code @Positive} 어노테이션으로 검증되며,
 * 이 메서드는 항상 유효한(양수) amount를 받습니다.
 * 충전 후 포인트 내역(CHARGE)이 자동으로 기록됩니다.
 *
 * @param userId 충전할 유저 ID (양수, Controller에서 검증됨)
 * @param amount 충전할 포인트 금액 (양수, Controller에서 검증됨)
 * @return 충전 후 업데이트된 유저 포인트 정보
 */
public UserPoint chargePoint(long userId, long amount) {
    // ...
}
```

### 3. IMPLEMENTATION_PLAN.md 업데이트

#### 3.1 현재 Phase 상태 업데이트
완료된 Phase의 체크박스와 상태를 업데이트합니다.

**업데이트 형식:**
```markdown
### Phase [N]: [기능명] ✅ COMPLETED
**Goal:** [목표]

**Test Cases:**
- [x] [테스트 케이스 1]
- [x] [테스트 케이스 2]
- [x] ~~[불필요한 테스트]~~ (이유)

**Implementation Steps:**
1. [x] [단계 1]
2. [x] [단계 2]
3. [x] [단계 3]

**Implementation Details:**

*Architecture:*
```
[아키텍처 다이어그램]
```

*Files Created/Modified:*
- ✅ [파일명] - [설명]
- ✅ [파일명] - [설명]

*Key Features:*
- ✅ [기능 1]
- ✅ [기능 2]

*Test Results:*
- ✅ [N]개 테스트 통과
- ✅ [커버리지] coverage
```

#### 3.2 Exception Handling Strategy 업데이트
새로 추가된 ErrorCode나 예외 처리를 반영합니다.

```markdown
**ErrorCode Enum** (중앙 관리):
- [x] `INVALID_USER_ID`
- [x] `VALIDATION_ERROR`
- [x] `TYPE_MISMATCH`
- [x] `INVALID_AMOUNT` (NEW)
- [x] `INSUFFICIENT_POINTS` (NEW)
```

#### 3.3 Development Checklist 업데이트
진행 상황을 체크합니다.

```markdown
### Implementation Order:
1. [x] **Phase 1: Point Inquiry** ✅ COMPLETED (2025-10-22)
2. [x] **Phase 2: Point Charging** ✅ COMPLETED (2025-10-23)
3. [ ] Phase 3: Point Usage
...

### Phase [N] Completion Summary:
- **Files Created**: [개수]개
  - [파일명]
- **Files Modified**: [개수]개
  - [파일명]
- **Tests**: [N]/[N] passing
- **New Features**: [기능 목록]
```

#### 3.4 Notes & Considerations 업데이트
새로운 교훈이나 개선 사항을 추가합니다.

```markdown
### Lessons Learned (Phase [N]):

**1. [교훈 제목]**
- [내용]

**2. [교훈 제목]**
- [내용]
```

### 4. DEV_LOG.md 업데이트

#### 4.1 새 Phase 섹션 추가
날짜와 함께 새로운 Phase 섹션을 추가합니다.

**기본 구조:**
```markdown
## 📅 2025-[MM-DD]

### 🎯 Step [N]-[M]: [기능명] 구현

---

## 1️⃣ 요구사항 분석

### 비즈니스 규칙
- [규칙 1]
- [규칙 2]

### 테스트 시나리오
- [시나리오 1]
- [시나리오 2]

---

## 2️⃣ TDD Red Phase

### 실패하는 테스트 작성
[테스트 코드 예시]

**테스트 결과:**
- ❌ [N]개 실패 (예상된 동작)

---

## 3️⃣ TDD Green Phase

### 최소 구현
[구현 코드 예시]

**구현 내용:**
- [내용 1]
- [내용 2]

**테스트 결과:**
- ✅ [N]개 통과

---

## 4️⃣ TDD Refactor Phase

### 리팩토링 [번호]: [제목]

**문제**: [문제 설명]

**해결**:
[해결 방안]

**효과**:
- [효과 1]
- [효과 2]

---

## 5️⃣ 최종 구현 결과

### 주요 성과

✅ **[성과 1]**
- [내용]

✅ **[성과 2]**
- [내용]

### 기술 스택
- [새로 추가된 기술]

---

## 🔄 리팩토링 히스토리

| 순서 | 리팩토링 내용 | 목적 |
|------|--------------|------|
| [N] | [내용] | [목적] |

---

## 📝 향후 개선 사항

### Priority 1: [항목]
[내용]

### Priority 2: [항목]
[내용]

---

## 💡 배운 점 (Lessons Learned)

[N]. **[제목]**
   - [내용]

---
```

#### 4.2 기존 섹션 연계
이전 Phase와의 연관성을 명시합니다.

```markdown
**이전 Phase와의 차이점:**
- [차이점 1]
- [차이점 2]

**재사용한 패턴:**
- [패턴 1]
- [패턴 2]
```

### 5. 일관성 검증

모든 문서 간 일관성을 확인합니다.

**체크리스트:**
- [ ] JavaDoc의 검증 책임이 실제 구현과 일치
- [ ] IMPLEMENTATION_PLAN.md의 체크박스가 정확
- [ ] DEV_LOG.md의 날짜가 올바름
- [ ] 파일명이 모든 문서에서 동일
- [ ] 용어가 일관되게 사용됨 (예: Repository vs Table)
- [ ] 에러 코드 이름이 모든 곳에서 동일

### 6. 업데이트 완료 보고

```markdown
## ✅ 문서 업데이트 완료!

### 📝 업데이트된 문서

#### 1. JavaDoc
- [파일명]: [업데이트 내용]
- [파일명]: [업데이트 내용]
- 총 [N]개 파일 업데이트

#### 2. IMPLEMENTATION_PLAN.md
- Phase [N] 완료 표시
- Exception Handling 섹션 업데이트
- Development Checklist 업데이트
- Lessons Learned 추가

#### 3. DEV_LOG.md
- Step [N]-[M] 섹션 추가
- TDD 각 단계 상세 기록
- 리팩토링 히스토리 업데이트
- 배운 점 추가

### 📊 현재 진행 상황
- 완료된 Phase: [N]개
- 전체 테스트: [N]개 통과
- 다음 Phase: [Phase 이름]

### 🎯 다음 단계
[다음에 할 작업]
```

## 업데이트 가이드

### JavaDoc 작성 원칙
```java
/**
 * [1줄 요약]
 * <p>
 * [상세 설명]
 * [검증 책임 명시]
 * [특이사항]
 *
 * @param [파라미터명] [설명 + 제약사항]
 * @return [반환값 설명]
 * @throws [예외명] [발생 조건] (실제로 던지는 경우만)
 * @see [관련 클래스]
 */
```

### IMPLEMENTATION_PLAN.md 원칙
- 완료된 항목은 `[x]` 체크
- 불필요한 항목은 `~~취소선~~` + 이유
- 새로운 내용은 **NEW** 표시
- 날짜 기록 필수

### DEV_LOG.md 원칙
- 날짜별로 섹션 구분
- TDD 3단계 모두 기록
- 코드 예시는 핵심만 간략히
- 배운 점은 구체적으로

## 주의사항

1. **순서 준수**: JavaDoc → IMPLEMENTATION_PLAN.md → DEV_LOG.md
2. **일관성**: 용어, 파일명, 에러 코드 이름 통일
3. **정확성**: 실제 구현과 문서 내용 일치
4. **완전성**: 모든 변경 사항 반영
5. **간결성**: 핵심만 기록, 불필요한 세부사항 제외

## 참고 파일
- 모든 Java 파일 (`src/**/*.java`)
- `IMPLEMENTATION_PLAN.md`
- `DEV_LOG.md`
- `git diff` 결과

## 예시 진행

**Step 1**: JavaDoc 업데이트
```
✅ PointService.java - chargePoint 메서드 JavaDoc 추가
✅ PointController.java - charge 엔드포인트 JavaDoc 추가
✅ ErrorCode.java - INVALID_AMOUNT 상수 주석 추가
```

**Step 2**: IMPLEMENTATION_PLAN.md 업데이트
```
✅ Phase 2 완료 표시
✅ ErrorCode 2개 추가 반영
✅ Phase 2 Completion Summary 작성
```

**Step 3**: DEV_LOG.md 업데이트
```
✅ Step 2-1 섹션 추가 (날짜: 2025-10-23)
✅ TDD 3단계 상세 기록
✅ 리팩토링 히스토리 추가
✅ 배운 점 2개 추가
```

**Step 4**: 일관성 검증
```
✅ 모든 문서에서 "INVALID_AMOUNT" 동일하게 사용
✅ 파일명 표기 일관성 확인
✅ 날짜 형식 통일
```

완료! 🎉
