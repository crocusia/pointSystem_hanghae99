---
description: TDD Green 단계 - 실패하는 테스트를 통과시키는 최소한의 구현 작성
---

# TDD Green Phase

당신은 TDD의 Green 단계를 수행합니다. 실패하는 테스트 코드를 확인하고, 각 테스트를 통과시키는 **최소한의 구현 코드**를 작성합니다.

## 작업 순서

### 1. 테스트 현황 파악
- 실패하는 테스트 파일 확인
- 각 테스트 케이스의 요구사항 분석
- 테스트 실행하여 실패 메시지 확인

```bash
./gradlew test --tests "[테스트클래스명]"
```

**분석 항목:**
- 테스트 개수
- 각 테스트의 Given-When-Then
- 필요한 메서드 목록
- 예상되는 비즈니스 로직

### 2. 구현 계획 수립
사용자에게 구현 계획을 제시합니다:

```markdown
## 🟢 Green Phase 구현 계획

### 📊 테스트 현황
- 총 테스트: [개수]개
- 실패: [개수]개
- 성공: [개수]개

### 📝 구현할 메서드
1. **[메서드명]** (테스트 [개수]개)
   - 책임: [설명]
   - 파라미터: [타입과 이름]
   - 반환값: [타입]
   - 비즈니스 로직:
     - [로직 1]
     - [로직 2]

### 🔄 구현 순서
1. [메서드1] 구현 → 테스트 실행
2. [메서드2] 구현 → 테스트 실행
3. ...

**이 계획으로 진행하시겠습니까?**
```

### 3. 최소 구현 작성 (Green Phase)
사용자 승인 후, **테스트를 통과시키는 최소한의 코드**를 작성합니다.

**작성 원칙:**
- **최소한의 코드**: 테스트 통과에 필요한 것만 구현
- **단순한 구조**: 복잡한 로직은 나중에 리팩토링
- **명확한 의도**: 코드가 무엇을 하는지 명확하게
- **하드코딩 허용**: Green 단계에서는 하드코딩도 괜찮음 (리팩토링 단계에서 개선)

**예시 1: 단순한 구현**
```java
public UserPoint chargePoint(long userId, long amount) {
    // 최소 구현: 테스트만 통과시킴
    UserPoint current = userPointRepository.selectById(userId);
    long newBalance = current.point() + amount;
    return userPointRepository.insertOrUpdate(userId, newBalance);
}
```

**예시 2: 검증 추가**
```java
public UserPoint chargePoint(long userId, long amount) {
    if (amount <= 0) {
        throw new UserException(ErrorCode.INVALID_AMOUNT);
    }

    UserPoint current = userPointRepository.selectById(userId);
    long newBalance = current.point() + amount;
    return userPointRepository.insertOrUpdate(userId, newBalance);
}
```

### 4. 점진적 테스트 실행
각 메서드 구현 후 즉시 테스트를 실행합니다:

```bash
./gradlew test --tests "[테스트클래스명].[테스트메서드명]"
```

**진행 상황 보고:**
```
🧪 테스트 실행 결과

✅ chargePoint_WithValidAmount_ShouldIncreaseBalance: PASSED
❌ chargePoint_WithNegativeAmount_ShouldThrowException: FAILED
⏭️ chargePoint_WithZeroAmount_ShouldThrowException: NOT RUN

다음: 음수 금액 검증 추가
```

### 5. 전체 테스트 통과 확인
모든 구현이 완료되면 전체 테스트를 실행합니다:

```bash
./gradlew test --tests "[테스트클래스명]"
```

### 6. Green 단계 완료 보고
```
✅ Green 단계 완료!

📊 테스트 결과:
- 총 테스트: [개수]개
- 성공: [개수]개 ✅
- 실패: 0개

📝 구현된 기능:
- [메서드명1]: [설명]
- [메서드명2]: [설명]

💡 개선 가능 영역:
- [개선점 1]
- [개선점 2]

➡️ 다음: Refactor Phase (코드 개선 및 정리)
```

## 작성 가이드

### ✅ Good Practices
```java
// 1. 명확한 검증
if (amount <= 0) {
    throw new UserException(ErrorCode.INVALID_AMOUNT);
}

// 2. 단순한 로직
long newBalance = current.point() + amount;

// 3. 명확한 반환
return userPointRepository.insertOrUpdate(userId, newBalance);
```

### ❌ 피해야 할 것
```java
// 1. 과도한 추상화 (Green 단계에서는 불필요)
// interface Strategy, Factory 패턴 등

// 2. 사용되지 않는 코드
// 테스트에 없는 기능 미리 구현

// 3. 복잡한 로직
// 여러 단계의 중첩된 조건문
```

## 주의사항

1. **최소 구현 우선**: 테스트만 통과시키는 것이 목표
2. **한 번에 하나씩**: 한 테스트씩 통과시키며 진행
3. **즉시 검증**: 구현 후 바로 테스트 실행
4. **리팩토링은 나중에**: Green 단계에서는 동작만 보장
5. **기존 패턴 유지**: DEV_LOG.md의 설계 원칙 준수

## 참고 파일
- `src/test/java/**/*Test.java` - 실패하는 테스트 코드
- `IMPLEMENTATION_PLAN.md` - 비즈니스 규칙
- `DEV_LOG.md` - 기존 구현 패턴
- `src/main/java/**/PointService.java` - Service 구현 위치

## 예시 진행 흐름

**Step 1**: 테스트 확인
```
❌ 3개의 테스트 실패 확인
- 정상 충전 케이스
- 음수 금액 예외
- 0 금액 예외
```

**Step 2**: 첫 번째 테스트 통과
```java
public UserPoint chargePoint(long userId, long amount) {
    UserPoint current = userPointRepository.selectById(userId);
    return userPointRepository.insertOrUpdate(userId, current.point() + amount);
}
```
→ ✅ 정상 충전 테스트 통과

**Step 3**: 예외 케이스 추가
```java
public UserPoint chargePoint(long userId, long amount) {
    if (amount <= 0) {
        throw new UserException(ErrorCode.INVALID_AMOUNT);
    }
    UserPoint current = userPointRepository.selectById(userId);
    return userPointRepository.insertOrUpdate(userId, current.point() + amount);
}
```
→ ✅ 모든 테스트 통과

**Step 4**: Green 단계 완료!
