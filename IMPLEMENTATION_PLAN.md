# IMPLEMENTATION_PLAN.md

This document outlines the implementation plan for the point management system, following TDD principles.

## Project Goal

Implement a user point management system with the following features:
1. Point inquiry
2. Point transaction history inquiry
3. Point charging
4. Point usage

## Implementation Strategy

### Phase 1: Point Inquiry Feature ✅ COMPLETED
**Goal:** Implement `GET /point/{id}` endpoint to retrieve user points

**Test Cases:**

*Unit Tests (PointService):*
- [x] Should return user point from repository (신규/기존 유저 통합 - 레포지토리 책임이므로 서비스에서 구분 불필요)
- [x] ~~Should return user point with zero balance for new user~~ (통합됨)
- [x] ~~Should return existing user point with current balance~~ (통합됨)
- [x] ~~Should return correct updateMillis timestamp~~ (검증됨 - 통합 테스트에 포함)
- [x] ~~Should handle multiple different users independently~~ (불필요 - 단일 조회로 검증됨)
- [x] ~~Should reject invalid user IDs~~ (Controller 레이어에서 검증)

**Implementation Steps:**
1. [x] Create `PointService` class
2. [x] Implement `getUserPoint(long userId)` method
3. [x] Wire service to `PointController.point()` method
4. [x] Verify all tests pass

**Business Rules:**
- New users start with 0 points
- Must return current point balance and last update time
- User ID must be positive (> 0)
- Invalid user IDs validated at Controller layer with `@Positive`

**Implementation Details:**

*Architecture:*
```java
Controller (검증) → Service (로직) → Repository (데이터) → Table (저장소)
```

*Files Created/Modified:*
- ✅ `PointService.java` - 비즈니스 로직 구현
- ✅ `UserPointRepository.java` - 인터페이스 생성 (DIP 적용)
- ✅ `UserPointTable.java` - 인터페이스 구현
- ✅ `PointController.java` - `@Positive` validation 추가
- ✅ `PointServiceTest.java` - 단위 테스트 작성

*Exception Handling:*
- ✅ `ErrorCode.java` - enum으로 에러 코드 중앙 관리
- ✅ `UserException.java` - ErrorCode 기반 예외 클래스
- ✅ `ApiControllerAdvice.java` - 통합 예외 처리
  - `UserException` 핸들러
  - `ConstraintViolationException` 핸들러 (@Positive 위반)
  - `MethodArgumentTypeMismatchException` 핸들러 (타입 불일치)

*Validation Strategy:*
- **Controller Layer**: `@Validated` + `@Positive`
  - null, 타입, 양수 검증
  - Long 오버플로우, 소수점 입력 처리
- **Service Layer**: 비즈니스 로직에만 집중 (검증 제거)

*Test Results:*
- ✅ 1 unit test passing (신규/기존 유저 통합)
- ✅ Mock을 활용한 격리된 테스트
- ✅ Given-When-Then 패턴 적용

---

### Phase 2: Point Charging Feature ✅ COMPLETED
**Goal:** Implement `PATCH /point/{id}/charge` endpoint to add points to user account

**Test Cases:**

*Unit Tests (PointService):*
- [x] Should successfully charge and create CHARGE history (통합 테스트)
- [x] Should throw exception when sum exceeds max balance (with chargeable amount in message)
- [x] Should succeed when sum equals max balance (경계값 테스트)

**Implementation Steps:**
1. [x] Create `PointHistoryRepository` interface (Phase 1 패턴 적용)
2. [x] Implement `chargePoint(long userId, long amount)` in `PointService`
3. [x] Add overflow validation (maxPointBalance 기반)
4. [x] Update user point balance using `UserPointRepository`
5. [x] Record transaction in `PointHistoryRepository` with type CHARGE
6. [x] Wire service to `PointController.charge()` method
7. [x] Verify all tests pass

**Business Rules:**
- Amount must be positive (> 0) - Controller 레이어에서 `@Positive` 검증
- Maximum point balance: configurable via `application.yml` (point.max-balance)
- Must atomically update balance and create history record
- Transaction type must be CHARGE
- Overflow validation: currentBalance + amount ≤ maxPointBalance
- On overflow: throw POINT_OVERFLOW exception with max chargeable amount

**Implementation Details:**

*Architecture:*
```java
Controller (@Positive) → Service (비즈니스 로직) → Repository (인터페이스) → Table (저장소)
```

*Files Created/Modified:*
- ✅ `PointHistoryRepository.java` - 인터페이스 생성 (DIP 적용)
- ✅ `PointHistoryTable.java` - 인터페이스 구현
- ✅ `PointService.java` - chargePoint() 메서드 구현
  - validateMaxPoint() private 메서드 추출
  - @Value로 maxPointBalance 주입
- ✅ `PointController.java` - charge() 엔드포인트 구현 (사용자 수정)
- ✅ `ErrorCode.java` - POINT_OVERFLOW 추가
- ✅ `application.yml` - point.max-balance 설정 추가 (100,000)
- ✅ `PointServiceTest.java` - 3개 테스트 작성

*Key Features:*
- ✅ 외부 설정을 통한 최대 포인트 제한 (application.yml)
- ✅ 경계값 테스트 (TEST_MAX 활용)
- ✅ 오버플로우 방지 및 사용자 친화적 에러 메시지
- ✅ 거래 내역 자동 기록

*Test Results:*
- ✅ 5개 테스트 통과 (Phase 1: 2개, Phase 2: 3개)
- ✅ Mock 기반 격리된 단위 테스트
- ✅ Given-When-Then 패턴 적용

---

### Phase 3: Point Usage Feature ✅ COMPLETED
**Goal:** Implement `PATCH /point/{id}/use` endpoint to deduct points from user account

**Test Cases:**

*Unit Tests (PointService):*
- [x] Should successfully use points and create USE history (잔액 감소 + 거래 내역 생성 통합 검증)
- [x] Should succeed when using all balance (경계값 테스트 - 잔액 0)
- [x] Should throw exception when balance is insufficient (with current balance in message)

**Implementation Steps:**
1. [x] Add `INSUFFICIENT_POINTS` to `ErrorCode` enum
2. [x] Implement `usePoint(long userId, long amount)` in `PointService`
3. [x] Add balance validation (`validateSufficientBalance`)
4. [x] Update user point balance using `UserPointRepository`
5. [x] Record transaction in `PointHistoryRepository` with type USE
6. [x] Wire service to `PointController.use()` method
7. [x] Verify all tests pass

**Business Rules:**
- Amount must be positive (> 0) - Controller 레이어에서 `@Positive` 검증
- User must have sufficient balance (current points >= amount)
- Balance cannot go negative
- Must atomically update balance and create history record
- Transaction type must be USE
- On insufficient balance: throw INSUFFICIENT_POINTS exception with current balance

**Implementation Details:**

*Architecture:*
```java
Controller (@Positive) → Service (비즈니스 로직) → Repository (인터페이스) → Table (저장소)
```

*Files Created/Modified:*
- ✅ `ErrorCode.java` - INSUFFICIENT_POINTS 추가
- ✅ `PointException.java` - UserException에서 리네이밍
- ✅ `PointService.java` - usePoint() 메서드 구현
  - validateSufficientBalance() private 메서드 추출
- ✅ `PointController.java` - use() 엔드포인트 구현
- ✅ `ApiControllerAdvice.java` - PointException 핸들러 추가
- ✅ `PointServiceTest.java` - 4개 테스트 작성

*Key Features:*
- ✅ 잔액 부족 검증 (balance < amount)
- ✅ 사용자 친화적 에러 메시지 (현재 잔액 포함)
- ✅ 경계값 테스트 (잔액 0까지 사용 가능)
- ✅ 거래 내역 자동 기록 (TransactionType.USE)

*Test Results:*
- ✅ 3개 테스트 통과 (Phase 3)
- ✅ 전체 7개 테스트 통과 (Phase 1: 1개, Phase 2: 3개, Phase 3: 3개)
- ✅ Mock 기반 격리된 단위 테스트
- ✅ Given-When-Then 패턴 적용
- ✅ 테스트 리팩토링: 잔액 감소 + 거래 내역 생성을 하나의 테스트로 통합

---

### Phase 4: Point History Inquiry Feature
**Goal:** Implement `GET /point/{id}/histories` endpoint to retrieve point transaction history

**Test Cases:**
- [ ] Should return empty list for user with no transactions
- [ ] Should return all transactions for user in chronological order
- [ ] Should include both CHARGE and USE transactions
- [ ] Should return correct transaction details (amount, type, timestamp)
- [ ] Should only return transactions for specified user

**Implementation Steps:**
1. Implement `getPointHistory(long userId)` in `PointService`
2. Fetch history from `PointHistoryTable`
3. Wire service to `PointController.history()` method
4. Verify all tests pass

**Business Rules:**
- Return all transactions for the specified user
- Transactions should be ordered (consider ordering by timestamp or id)
- Each history entry must include: id, userId, amount, type, updateMillis

---

### Phase 5: Concurrency & Thread Safety
**Goal:** Ensure thread-safe operations for concurrent point transactions

**Test Cases:**
- [ ] Should handle concurrent charges to same user correctly
- [ ] Should handle concurrent uses from same user correctly
- [ ] Should handle mixed concurrent charge/use operations correctly
- [ ] Should prevent race conditions in balance updates
- [ ] Final balance should be consistent after concurrent operations

**Implementation Steps:**
1. Identify critical sections in `PointService`
2. Implement synchronization mechanism (consider `synchronized` or `ReentrantLock`)
3. Add integration tests for concurrent scenarios
4. Verify thread safety through stress testing

**Concurrency Strategy:**
- Use user-level locking to prevent race conditions
- Consider using `ConcurrentHashMap` with user ID as key for locks
- Ensure atomic read-check-update operations

---

## Exception Handling Strategy

### Implemented Exception Structure:

**ErrorCode Enum** (중앙 관리):
- [x] `TYPE_MISMATCH` - 타입 불일치 (오버플로우, 소수점 등)
- [x] `VALIDATION_ERROR` - Bean Validation 제약 위반
- [x] `POINT_OVERFLOW` - 포인트 최대 잔액 초과 (Phase 2)
- [x] `INSUFFICIENT_POINTS` - 잔액 부족 (Phase 3)

**Exception Classes**:
- [x] `PointException` - ErrorCode 기반 비즈니스 예외 (UserException에서 리네이밍)
- [x] ~~`UserException`~~ (PointException으로 리네이밍)

**ApiControllerAdvice Handlers**:
- [x] `PointException` → ErrorCode의 HTTP 상태 코드 + 메시지 (동적 메시지 지원)
- [x] `ConstraintViolationException` → 400 Bad Request
- [x] `MethodArgumentTypeMismatchException` → 400 Bad Request
- [x] `Exception` (fallback) → 500 Internal Server Error

### Error Response Format:
```json
{
  "code": "INVALID_USER_ID",
  "message": "User ID must be positive"
}
```

### Implementation Status:
1. [x] ErrorCode enum 생성 및 확장 가능한 구조
2. [x] UserException 기반 통합 예외 클래스
3. [x] ApiControllerAdvice에 핸들러 추가
4. [x] Controller 레이어 Bean Validation 적용
5. [ ] 포인트 충전/사용 관련 ErrorCode 추가 예정

---

## Testing Strategy

### Unit Tests (PointService):
- [x] Test methods in isolation using Mockito
- [x] Mock `UserPointRepository` dependency
- [x] Focus on business logic verification
- [x] Given-When-Then 패턴 적용
- [x] 강결합 회피 (결과 검증, 구현 세부사항 회피)

**Implemented:**
- `PointServiceTest.java` - 1개 테스트 작성
  - 유저 포인트 조회 (레포지토리 반환값 검증 - 신규/기존 구분은 레포지토리 책임)

### Integration Tests:
- [ ] Test full flow from controller to database tables
- [ ] Use `@WebMvcTest` for controller layer testing
- [ ] Verify Bean Validation works correctly
- [ ] Test error response format

**Recommended:**
```java
@WebMvcTest(PointController.class)
class PointControllerTest {
    @Test
    void point_WithInvalidId_ShouldReturn400() {
        // Validate @Positive constraint
    }
}
```

### Concurrency Tests:
- [ ] Use `ExecutorService` to simulate concurrent requests
- [ ] Verify final state consistency
- [ ] Test with multiple threads accessing same user

---

## Development Checklist

### Setup:
- [x] Repository structure analyzed
- [x] CLAUDE.md created
- [x] IMPLEMENTATION_PLAN.md created
- [x] Initial test structure created
- [x] DEV_LOG.md created

### Implementation Order:
1. [x] **Phase 1: Point Inquiry** ✅ COMPLETED (2025-10-22)
   - [x] PointService 구현
   - [x] UserPointRepository 인터페이스 생성
   - [x] Controller validation 추가
   - [x] 단위 테스트 작성 (2개)
   - [x] ErrorCode enum 구조 구축
   - [x] ApiControllerAdvice 통합 예외 처리

2. [x] **Phase 2: Point Charging** ✅ COMPLETED (2025-10-23)
   - [x] PointHistoryRepository 인터페이스 생성
   - [x] chargePoint() 메서드 구현
   - [x] Overflow 검증 로직 추가
   - [x] 외부 설정 (application.yml) 연동
   - [x] 단위 테스트 작성 (3개)
   - [x] ErrorCode.POINT_OVERFLOW 추가

3. [x] **Phase 3: Point Usage** ✅ COMPLETED (2025-10-23)
   - [x] usePoint() 메서드 구현
   - [x] 잔액 부족 검증 로직 추가
   - [x] PointException 리네이밍 (UserException → PointException)
   - [x] 단위 테스트 작성 (4개)
   - [x] ErrorCode.INSUFFICIENT_POINTS 추가
   - [x] Controller.use() 엔드포인트 구현

4. [ ] Phase 4: Point History Inquiry
5. [ ] Phase 5: Concurrency & Thread Safety
6. [x] Exception Handling (Phase 1-3에서 구축 완료)
7. [ ] Final integration testing
8. [ ] Code coverage verification

### Phase 1 Completion Summary:
- **Files Created**: 5개
  - `PointService.java`
  - `UserPointRepository.java`
  - `ErrorCode.java`
  - `UserException.java`
  - `PointServiceTest.java`
- **Files Modified**: 3개
  - `PointController.java`
  - `UserPointTable.java`
  - `ApiControllerAdvice.java`
- **Tests**: 1/1 passing (포인트 조회)
- **Architecture**: Controller → Service → Repository → Table
- **Validation**: Controller 레이어 Bean Validation 적용

### Phase 2 Completion Summary:
- **Files Created**: 1개
  - `PointHistoryRepository.java`
- **Files Modified**: 5개
  - `PointService.java` (chargePoint 추가, @Value 주입)
  - `PointController.java` (charge 엔드포인트 구현)
  - `PointHistoryTable.java` (인터페이스 구현)
  - `ErrorCode.java` (POINT_OVERFLOW 추가)
  - `application.yml` (point.max-balance 추가)
  - `PointServiceTest.java` (3개 테스트 추가)
- **Tests**: 4/4 passing (Phase 1: 1개, Phase 2: 3개)
- **New Features**:
  - 포인트 충전 기능
  - 최대 포인트 제한 (외부 설정)
  - 오버플로우 검증
  - 경계값 테스트

### Phase 3 Completion Summary:
- **Files Created**: 1개
  - `PointException.java` (UserException 리네이밍)
- **Files Modified**: 5개
  - `PointService.java` (usePoint 추가, validateSufficientBalance 추가)
  - `PointController.java` (use 엔드포인트 구현, charge JavaDoc 업데이트)
  - `ErrorCode.java` (INSUFFICIENT_POINTS 추가)
  - `ApiControllerAdvice.java` (PointException 핸들러 추가)
  - `PointServiceTest.java` (3개 테스트 추가 - 잔액 감소/거래 내역 생성 통합)
- **Files Deleted**: 1개
  - `UserException.java` (PointException으로 대체)
- **Tests**: 7/7 passing (Phase 1: 1개, Phase 2: 3개, Phase 3: 3개)
- **New Features**:
  - 포인트 사용 기능
  - 잔액 부족 검증
  - 경계값 테스트 (잔액 0까지 사용)
  - 사용자 친화적 에러 메시지 (현재 잔액 포함)
  - PointException 리네이밍으로 도메인 명확성 향상

---

## Notes & Considerations

### Design Decisions:
- [x] **Service Layer:** `PointService` 구현으로 비즈니스 로직과 컨트롤러 분리
- [x] **Validation Strategy:** Controller 레이어에서 입력 검증, Service는 비즈니스 로직에 집중
  - Controller: `@Validated` + `@Positive` (null, 타입, 양수 검증)
  - Service: 검증된 데이터만 처리
- [x] **Dependency Inversion:** UserPointRepository 인터페이스로 추상화
- [ ] **Transaction Management:** 향후 in-memory 테이블 일관성 관리 필요
- [ ] **Thread Safety:** 사용자별 락킹으로 동시성 처리 예정

### Lessons Learned (Phase 1):

**1. 검증 레이어 분리의 중요성**
- Controller에서 입력 검증 → Service에서 비즈니스 로직
- 중복 검증 제거로 코드 단순화
- Bean Validation 활용으로 선언적 검증

**2. ErrorCode Enum 패턴의 효과**
- 에러 코드, HTTP 상태, 메시지 중앙 관리
- `errorCode.name()` 활용으로 명확한 에러 식별
- 확장 가능한 구조 (새 에러 타입 추가 용이)

**3. 인터페이스 기반 설계**
- Repository 인터페이스로 테스트 용이성 향상
- Mock 활용 가능
- 향후 구현체 교체 가능

**4. 테스트 설계 철학**
- 구현 세부사항이 아닌 결과 검증
- 강결합 회피 (예: empty() 호출 검증 X, 반환값 검증 O)
- Given-When-Then 패턴으로 명확한 의도 전달

### Potential Issues:
- In-memory tables have simulated latency (200-300ms) - tests will be slower
- No real database transactions - need manual consistency management
- HashMap/ArrayList are not thread-safe - need synchronization
- No persistence - data lost on restart (acceptable for practice project)

### Improvements for Next Phases:

**1. Controller 통합 테스트 추가**
```java
@WebMvcTest(PointController.class)
class PointControllerTest {
    // Bean Validation 동작 확인
    // 에러 응답 형식 검증
}
```

**2. 포인트 충전/사용 시 적용할 패턴**
- Controller validation: `@Positive` for amount
- ErrorCode 확장: `INVALID_AMOUNT`, `INSUFFICIENT_POINTS`
- Service layer: 잔액 검증, 트랜잭션 관리

**3. 동시성 처리 전략**
- 사용자별 락 (ConcurrentHashMap<Long, Lock>)
- 원자적 읽기-검증-업데이트 보장

### Future Enhancements (Out of Scope):
- Maximum point limits
- Transaction reversal/refund functionality
- Batch operations
- Point expiration
- Real database integration
- Caching layer

### References:
- 상세 구현 과정: `DEV_LOG.md` 참조
- 프로젝트 가이드: `CLAUDE.md` 참조
