---
description: TDD Red 단계 - 테스트 시나리오 작성 및 실패하는 테스트 코드 생성
---

# TDD Red Phase

당신은 TDD의 Red 단계를 수행합니다. 사용자가 구현하려는 기능에 대해 테스트 시나리오를 설계하고, 실패하는 테스트 코드를 작성합니다.

## 작업 순서

### 1. 요구사항 분석
- `IMPLEMENTATION_PLAN.md`에서 해당 Phase의 비즈니스 규칙 확인
- `DEV_LOG.md`에서 기존 패턴 파악
- 사용자가 요청한 기능의 세부 사항 확인

### 2. 테스트 시나리오 제시
다음 형식으로 테스트 시나리오를 작성하여 사용자에게 제시합니다:

```markdown
## 📋 테스트 시나리오: [기능명]

### ✅ 정상 케이스
1. **[시나리오명]**
   - Given: [전제조건]
   - When: [실행동작]
   - Then: [기대결과]

### ❌ 예외 케이스
1. **[시나리오명]**
   - Given: [전제조건]
   - When: [실행동작]
   - Then: [기대예외]

### 🔢 경계값 테스트
1. **[시나리오명]**
   - Given: [전제조건]
   - When: [실행동작]
   - Then: [기대결과]
```

**사용자에게 확인을 요청합니다**: "이 테스트 시나리오로 진행하시겠습니까?"

### 3. 테스트 코드 작성 (Red Phase)
사용자 승인 후, **실패하는 테스트 코드**를 작성합니다.

**작성 규칙:**
- Given-When-Then 패턴
- `@DisplayName`으로 한글 설명
- Mock 활용 (Service 단위 테스트)
- AssertJ assertions
- **아직 구현되지 않은 메서드 호출 → 컴파일 에러 또는 테스트 실패**

**예시:**
```java
@Test
@DisplayName("유효한 금액으로 충전 시 잔액이 증가해야 한다")
void chargePoint_WithValidAmount_ShouldIncreaseBalance() {
    // Given
    long userId = 1L;
    long amount = 1000L;
    UserPoint current = new UserPoint(userId, 5000L, System.currentTimeMillis());
    when(userPointRepository.selectById(userId)).thenReturn(current);

    // When
    UserPoint result = pointService.chargePoint(userId, amount);

    // Then
    assertThat(result.point()).isEqualTo(6000L);
}
```

### 4. 테스트 실행 및 실패 확인
```bash
./gradlew test --tests "[테스트클래스명]"
```
- 테스트가 실패하는지 확인
- 실패 메시지를 사용자에게 보고

### 5. 완료 보고
```
✅ Red 단계 완료

📊 작성된 테스트:
- 정상 케이스: [개수]개
- 예외 케이스: [개수]개
- 경계값 테스트: [개수]개
- 총 [개수]개 테스트 (모두 실패 예상)

🔴 실패한 테스트: [개수]개

📝 구현 필요:
- [메서드명1]
- [메서드명2]

➡️ 다음: Green Phase (테스트를 통과시키는 최소 구현)
```

## 주의사항
- Red 단계에서는 **구현 코드를 작성하지 않습니다**
- 테스트는 **반드시 실패**해야 합니다
- 기존 패턴과 일관성 유지
- Controller 검증은 Service 단위 테스트와 분리
