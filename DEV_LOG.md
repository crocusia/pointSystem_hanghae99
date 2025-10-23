# 개발 로그 (Development Log)

## 📅 2025-10-22

### 🎯 Step 1 - 1: 포인트 조회 기능 구현

---

## 1️⃣ 초기 구현

### 구현 내용
- **PointService.getUserPoint()** 메서드 구현
- **UserPointRepository** 인터페이스 생성
- **UserPointTable**에 인터페이스 구현

### 설계 결정
```java
@Service
@RequiredArgsConstructor
public class PointService {
    private final UserPointRepository userPointRepository;

    public UserPoint getUserPoint(long userId) {
        validateUserId(userId);
        return userPointRepository.selectById(userId);
    }
}
```

### 리팩토링 1: 의존성 역전 원칙 적용
**문제**: PointService가 UserPointTable을 직접 의존 <br>

**해결**:
- UserPointRepository 인터페이스 생성
- UserPointTable이 인터페이스 구현
- Mocking 가능하게 개선

---

## 2️⃣ 예외 처리 개선

### 리팩토링 2: 하드코딩된 예외 메시지 개선을 위한  ErrorCode Enum 도입

**목적**: 에러 코드, HTTP 상태, 메시지 중앙 관리

**구현**:
```java
@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "Input validation failed"),
    TYPE_MISMATCH(HttpStatus.BAD_REQUEST, "Invalid Data Type");

    private final HttpStatus status;
    private final String message;
}

```

**효과**:
- 에러 정보 중앙 집중화
- `errorCode.name()`으로 명확한 에러 코드 제공
- HTTP 상태 코드와 메시지 함께 관리

---

## 3️⃣ Controller 레이어 검증 강화

### 리팩토링 3: Bean Validation 추가

**의존성 추가**:
```kotlin
// build.gradle.kts
implementation("org.springframework.boot:spring-boot-starter-validation")
```

**Controller 검증**:
```java
@RestController
@RequestMapping("/point")
@Validated
public class PointController {

    @GetMapping("{id}")
    public UserPoint point(@PathVariable @Positive long id) {
        return pointService.getUserPoint(id);
    }
}
```

**검증 항목**:
- ✅ null 체크 (PathVariable은 기본적으로 필수)
- ✅ 타입 체크 (Spring이 자동 처리)
- ✅ 양수 검증 (`@Positive`)

---

### 리팩토링 4: ApiControllerAdvice 개선

**구현 내용**:

1. **Bean Validation 예외 핸들러**
```java
@ExceptionHandler(ConstraintViolationException.class)
public ResponseEntity<ErrorResponse> handleConstraintViolationException(
    ConstraintViolationException e
) {
    final ErrorCode errorCode = ErrorCode.VALIDATION_ERROR;
    String detailMessage = e.getConstraintViolations().stream()
        .findFirst()
        .map(ConstraintViolation::getMessage)
        .orElse(errorCode.getMessage());

    return ResponseEntity
        .status(errorCode.getStatus())
        .body(new ErrorResponse(errorCode.name(), detailMessage));
}
```

2. **타입 불일치 예외 핸들러**
```java
@ExceptionHandler(MethodArgumentTypeMismatchException.class)
public ResponseEntity<ErrorResponse> handleTypeMismatch(
    MethodArgumentTypeMismatchException e
) {
    final ErrorCode errorCode = ErrorCode.TYPE_MISMATCH;
    String detailMessage = String.format(
        "%s. Field: '%s', Required Type: '%s', Received Value: '%s'.",
        errorCode.getMessage(),
        e.getName(),
        e.getRequiredType() != null ? e.getRequiredType().getSimpleName() : "unknown",
        e.getValue() != null ? e.getValue().toString() : "null"
    );

    return ResponseEntity
        .status(errorCode.getStatus())
        .body(new ErrorResponse(errorCode.name(), detailMessage));
}
```

**처리 가능한 예외**:
- Long.MAX_VALUE 초과 (오버플로우)
- 소수점 입력 (1.5 등)
- 문자열 입력 ("abc" 등)

**TO DO**: 
- 통합 테스트 기반, 실제 예외 케이스에 대한 처리 여부 확인 필요

---

## 4️⃣ 테스트 전략

### 테스트 설계 원칙

1. **Service 단위 테스트**
   - Mock을 사용한 격리된 테스트
   - 비즈니스 로직 검증

2. **테스트 케이스**:
   - ✅ 유저 포인트 조회 (레포지토리 반환값 검증)
     - 신규/기존 유저 구분은 레포지토리 책임이므로 서비스 테스트에서 분리 불필요

3. **제거된 테스트** (Controller에서 검증):
   - ~~음수 ID 테스트~~
   - ~~0 ID 테스트~~
   - ~~경계값 테스트~~

### 테스트 코드
```java
@ExtendWith(MockitoExtension.class)
@DisplayName("PointService Unit Tests")
class PointServiceTest {

    @Mock
    private UserPointRepository userPointRepository;

    @Mock
    private PointHistoryRepository pointHistoryRepository;

    private PointService pointService;

    private static final long TEST_MAX = 100_000L;

    @BeforeEach
    void setUp() {
        pointService = new PointService(TEST_MAX, userPointRepository, pointHistoryRepository);
    }

    @Test
    @DisplayName("유저 포인트 조회 시 레포지토리에서 조회한 값을 반환해야 한다")
    void getUserPoint_ShouldReturnUserPointFromRepository() {
        // Given
        long userId = 1L;
        long expectedPoints = 5000L;
        long currentTime = System.currentTimeMillis();
        UserPoint userPoint = new UserPoint(userId, expectedPoints, currentTime);
        // Stub
        when(userPointRepository.selectById(userId)).thenReturn(userPoint);

        // When
        UserPoint result = pointService.getUserPoint(userId);

        // Then
        assertThat(result.id()).isEqualTo(userId);
        assertThat(result.point()).isEqualTo(expectedPoints);
        assertThat(result.updateMillis()).isEqualTo(currentTime);
    }
}
```

---

## 5️⃣ 아키텍처 구조

### 레이어 분리

```
┌─────────────────────────────────────────┐
│         PointController                 │
│  - @Validated                           │
│  - @PathVariable @Positive              │
│  - HTTP 요청/응답 처리                   │
└──────────────┬──────────────────────────┘
               │
               ▼
┌─────────────────────────────────────────┐
│         PointService                    │
│  - 비즈니스 로직                         │
│  - 검증된 데이터만 처리                   │
└──────────────┬──────────────────────────┘
               │
               ▼
┌─────────────────────────────────────────┐
│    UserPointRepository (Interface)      │
│  - 데이터 접근 추상화                     │
└──────────────┬──────────────────────────┘
               │
               ▼
┌─────────────────────────────────────────┐
│         UserPointTable                  │
│  - 실제 데이터 저장소                     │
└─────────────────────────────────────────┘

         예외 처리 (횡단 관심사)
┌─────────────────────────────────────────┐
│      ApiControllerAdvice                │                 
│  - ConstraintViolationException         │
│  - MethodArgumentTypeMismatchException  │
│  - Exception (fallback)                 │
└─────────────────────────────────────────┘
```

### 검증 전략

```
HTTP Request
    │
    ▼
┌──────────────────────────┐
│  Controller 검증          │
│  - @Positive             │
│  - null 체크 (자동)       │
│  - 타입 체크 (자동)       │
└──────────┬───────────────┘
           │
           ▼ (유효한 데이터만 통과)
┌──────────────────────────┐
│  Service 로직            │
│  - 비즈니스 로직 수행     │
└──────────┬───────────────┘
           │
           ▼
┌──────────────────────────┐
│  Repository 접근         │
└──────────────────────────┘
```

---

## 📊 최종 구현 결과

### 주요 성과

✅ **견고한 아키텍처**
- 레이어별 책임 명확
- 의존성 역전 원칙 적용
- 단일 책임 원칙 준수

✅ **효과적인 예외 처리**
- ErrorCode enum 중앙 관리
- 일관된 에러 응답 형식


✅ **높은 테스트 품질**
- Mock을 활용한 단위 테스트
- Given-When-Then 패턴
- 강결합 회피 (구현 세부사항이 아닌 결과 검증)

✅ **TDD 원칙 준수**
- 테스트 우선 작성
- Red-Green-Refactor 사이클

### 기술 스택
- Java 17
- Spring Boot 3.2.0
- Spring Validation (Bean Validation)
- Lombok
- JUnit 5
- Mockito
- AssertJ

---

## 🔄 리팩토링 히스토리

| 순서 | 리팩토링 내용 | 목적 |
|------|--------------|------|
| 1 | UserPointRepository 인터페이스 도입 | 의존성 역전, 테스트 가능성 향상 |
| 2 | ErrorCode enum 도입 | 에러 정보 중앙 관리 |
| 3 | Bean Validation 추가 | Controller 레이어 검증 강화 |
| 4 | ApiControllerAdvice 확장 | 다양한 예외 통합 처리 |
| 5 | JavaDoc 업데이트 | 현재 구현 상태를 정확히 반영 |
| 6 | 신규/기존 유저 테스트 통합 | TDD 관점: 서비스는 레포지토리 반환값만 전달, 신규/기존 구분은 레포지토리 책임 |


---

## 📝 향후 개선 사항

### Priority 1: Controller 통합 테스트
```java
@WebMvcTest(PointController.class)
class PointControllerTest {
    @Test
    void point_WithInvalidId_ShouldReturnValidationError() {
        // GET /point/-1 → 400 Bad Request
        // Bean Validation 동작 확인
        // 에러 응답 형식 검증
    }

    @Test
    void point_WithTypeMismatch_ShouldReturnTypeMismatchError() {
        // GET /point/abc → 400 Bad Request
        // 타입 불일치 처리 확인
    }

    @Test
    void point_WithValidId_ShouldReturnUserPoint() {
        // GET /point/1 → 200 OK
        // 정상 응답 검증
    }
}
```

### Priority 2: 다음 기능 구현
- [ ] 포인트 충전 기능
- [ ] 포인트 사용 기능
- [ ] 포인트 내역 조회 기능

---

## 💡 배운 점 (Lessons Learned)

1. **검증 레이어 분리의 중요성**
   - Controller에서 입력 검증 → Service에서 비즈니스 로직
   - 중복 검증 제거로 코드 단순화
   - Bean Validation 활용으로 선언적 검증

2. **ErrorCode Enum 패턴의 효과**
   - 에러 코드, HTTP 상태, 메시지 중앙 관리
   - `errorCode.name()` 활용으로 명확한 에러 식별
   - 확장 가능한 구조 (새 에러 타입 추가 용이)

3. **인터페이스 기반 설계**
   - Repository 인터페이스로 테스트 용이성 향상
   - Mock 활용 가능
   - 향후 구현체 교체 가능

4. **테스트 설계 철학**
   - 구현 세부사항이 아닌 결과 검증
   - 강결합 회피 (예: empty() 호출 검증 X, 반환값 검증 O)
   - Given-When-Then 패턴으로 명확한 의도 전달

---

## 📚 참고 자료

- [Spring Validation 공식 문서](https://docs.spring.io/spring-framework/reference/core/validation/beanvalidation.html)
- [Spring Exception Handling](https://spring.io/blog/2013/11/01/exception-handling-in-spring-mvc)
- Clean Architecture 원칙
- SOLID 원칙

---

## 📈 다음 단계

1. ✅ 포인트 조회 기능 완료
2. ⏭️ 포인트 충전 기능 구현
3. ⏭️ 포인트 사용 기능 구현
4. ⏭️ 포인트 내역 조회 기능 구현
5. ⏭️ 동시성 처리 구현

---

## 📅 2025-10-23

### 🎯 Step 1 - 2: 포인트 충전 기능 구현

---

## 1️⃣ 초기 구현

### 구현 내용
- **PointService.chargePoint()** 메서드 구현
- **PointHistoryRepository** 인터페이스 생성
- **PointHistoryTable**에 인터페이스 구현
- **application.yml** 설정 추가

### 설계 결정
```java
@Service
public class PointService {
    private final long maxPointBalance;
    private final UserPointRepository userPointRepository;
    private final PointHistoryRepository pointHistoryRepository;

    public PointService(
            @Value("${point.max-balance}") long maxPointBalance,
            UserPointRepository userPointRepository,
            PointHistoryRepository pointHistoryRepository
    ) {
        this.maxPointBalance = maxPointBalance;
        this.userPointRepository = userPointRepository;
        this.pointHistoryRepository = pointHistoryRepository;
    }

    public UserPoint chargePoint(long userId, long amount) {
        UserPoint currentPoint = userPointRepository.selectById(userId);
        long currentBalance = currentPoint.point();

        validateMaxPoint(currentBalance, amount);

        long newBalance = currentBalance + amount;
        UserPoint updatedPoint = userPointRepository.insertOrUpdate(userId, newBalance);

        pointHistoryRepository.insert(userId, amount, TransactionType.CHARGE, System.currentTimeMillis());

        return updatedPoint;
    }

    private void validateMaxPoint(long currentBalance, long amount){
        if (currentBalance > maxPointBalance - amount) {
            long maxChargeableAmount = maxPointBalance - currentBalance;
            throw new UserException(ErrorCode.POINT_OVERFLOW, String.valueOf(maxChargeableAmount));
        }
    }
}
```

### 리팩토링 1: 의존성 역전 원칙 적용
**문제**: PointService가 PointHistoryTable을 직접 의존 <br>

**해결**:
- PointHistoryRepository 인터페이스 생성
- PointHistoryTable이 인터페이스 구현
- Phase 1의 UserPointRepository 패턴 일관성 유지

---

## 2️⃣ 예외 처리 개선

### 리팩토링 2: ErrorCode Enum 확장

**목적**: 포인트 오버플로우 에러 추가

**구현**:
```java
@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    // 기존 코드...

    /**
     * 포인트 오버플로우 에러 (잔액 + 충전 금액이 최대 잔액 초과)
     */
    POINT_OVERFLOW(HttpStatus.BAD_REQUEST, "Maximum point limit exceeded. You can charge up to {0} more.");
}
```

**효과**:
- 동적 메시지 지원 (`{0}` 플레이스홀더)
- 사용자 친화적 에러 메시지 (충전 가능 금액 표시)

---

## 3️⃣ 외부 설정화

### 리팩토링 3: @Value 어노테이션 도입

**목적**: 최대 포인트 잔액을 외부 설정으로 관리

**application.yml 추가**:
```yaml
# 포인트 시스템 설정
point:
  max-balance: 100000
```

**PointService 수정**:
```java
public PointService(
        @Value("${point.max-balance}") long maxPointBalance,
        UserPointRepository userPointRepository,
        PointHistoryRepository pointHistoryRepository
) {
    this.maxPointBalance = maxPointBalance;
    // ...
}
```

**효과**:
- 환경별 다른 최대값 설정 가능
- 재배포 없이 설정 변경 가능
- 테스트 시 다른 값 주입 가능

---

## 4️⃣ 테스트 전략

### 테스트 설계 원칙

1. **Service 단위 테스트**
   - Mock을 사용한 격리된 테스트
   - 비즈니스 로직 검증

2. **테스트 케이스**:
   - ✅ 충전 성공 시 잔액 증가 및 거래 내역 생성
   - ✅ 오버플로우 시 예외 발생 (충전 가능 금액 메시지 포함)
   - ✅ 경계값 테스트 (합이 최대값과 동일)

3. **Mock 주입 방식 변경**:
   - ~~@InjectMocks~~ → @BeforeEach에서 수동 주입
   - 이유: @Value 파라미터 테스트 지원

### 테스트 코드
```java
@ExtendWith(MockitoExtension.class)
@DisplayName("PointService Unit Tests")
class PointServiceTest {

    @Mock
    private UserPointRepository userPointRepository;

    @Mock
    private PointHistoryRepository pointHistoryRepository;

    private PointService pointService;

    private static final long TEST_MAX = 100_000L;

    @BeforeEach
    void setUp() {
        pointService = new PointService(TEST_MAX, userPointRepository, pointHistoryRepository);
    }

    @Test
    @DisplayName("충전 성공 시, 잔액이 증가하고 CHARGE 타입 거래 내역이 생성되어야 한다")
    void chargeSuccess_IncreasesBalanceAndCreatesHistory() {
        // Given
        long userId = 1L;
        long currentBalance = 5000L;
        long chargeAmount = 1000L;
        long expectedBalance = 6000L;

        UserPoint currentPoint = new UserPoint(userId, currentBalance, System.currentTimeMillis());
        UserPoint updatedPoint = new UserPoint(userId, expectedBalance, System.currentTimeMillis());

        when(userPointRepository.selectById(userId)).thenReturn(currentPoint);
        when(userPointRepository.insertOrUpdate(userId, expectedBalance)).thenReturn(updatedPoint);

        PointHistory expectedHistory = new PointHistory(1L, userId, chargeAmount, TransactionType.CHARGE, System.currentTimeMillis());
        when(pointHistoryRepository.insert(eq(userId), eq(chargeAmount), eq(TransactionType.CHARGE), anyLong()))
            .thenReturn(expectedHistory);

        // When
        UserPoint result = pointService.chargePoint(userId, chargeAmount);

        // Then
        assertThat(result.id()).isEqualTo(userId);
        assertThat(result.point()).isEqualTo(expectedBalance);
        verify(pointHistoryRepository, times(1)).insert(
            eq(userId),
            eq(chargeAmount),
            eq(TransactionType.CHARGE),
            anyLong()
        );
    }

    @Test
    @DisplayName("잔액과 충전 금액의 합이 최대값을 초과하면 예외가 발생하고, 충전 가능 금액이 메시지에 포함되어야 한다")
    void chargePoint_WhenOverflow_ShouldThrowExceptionWithMaxChargeableAmount() {
        // Given
        long userId = 1L;
        long currentBalance = TEST_MAX - 1L;
        long chargeAmount = 2L;
        long expectedMaxChargeable = 1L;

        UserPoint currentPoint = new UserPoint(userId, currentBalance, System.currentTimeMillis());
        when(userPointRepository.selectById(userId)).thenReturn(currentPoint);

        // When & Then
        assertThatThrownBy(() -> pointService.chargePoint(userId, chargeAmount))
                .isInstanceOf(UserException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.POINT_OVERFLOW)
                .hasMessageContaining(String.valueOf(expectedMaxChargeable));
    }

    @Test
    @DisplayName("잔액과 충전 금액의 합이 최대값과 같으면 충전에 성공한다")
    void chargePoint_WhenSumEqualsMaxBalance_ShouldSucceed() {
        // Given
        long userId = 1L;
        long currentBalance = TEST_MAX - 1L;
        long chargeAmount = 1L;
        long expectedBalance = TEST_MAX;

        UserPoint currentPoint = new UserPoint(userId, currentBalance, System.currentTimeMillis());
        UserPoint updatedPoint = new UserPoint(userId, expectedBalance, System.currentTimeMillis());

        when(userPointRepository.selectById(userId)).thenReturn(currentPoint);
        when(userPointRepository.insertOrUpdate(userId, expectedBalance)).thenReturn(updatedPoint);

        PointHistory expectedHistory = new PointHistory(1L, userId, chargeAmount, TransactionType.CHARGE, System.currentTimeMillis());
        when(pointHistoryRepository.insert(eq(userId), eq(chargeAmount), eq(TransactionType.CHARGE), anyLong()))
                .thenReturn(expectedHistory);

        // When
        UserPoint result = pointService.chargePoint(userId, chargeAmount);

        // Then
        assertThat(result.id()).isEqualTo(userId);
        assertThat(result.point()).isEqualTo(TEST_MAX);
        verify(pointHistoryRepository, times(1)).insert(
                eq(userId),
                eq(chargeAmount),
                eq(TransactionType.CHARGE),
                anyLong()
        );
    }
}
```

---

## 5️⃣ 아키텍처 구조

### 레이어 분리

```
┌─────────────────────────────────────────┐
│         PointController                 │
│  - @Validated                           │
│  - charge(@Positive amount)             │
│  - HTTP 요청/응답 처리                   │
└──────────────┬──────────────────────────┘
               │
               ▼
┌─────────────────────────────────────────┐
│         PointService                    │
│  - chargePoint() 비즈니스 로직           │
│  - validateMaxPoint() 검증               │
│  - @Value 외부 설정 주입                 │
└──────────────┬──────────────────────────┘
               │
               ├─────────────────────┐
               ▼                     ▼
┌──────────────────────┐  ┌──────────────────────┐
│ UserPointRepository  │  │ PointHistoryRepository│
│  - 포인트 데이터 접근  │  │  - 내역 데이터 접근    │
└──────────┬───────────┘  └──────────┬────────────┘
           │                         │
           ▼                         ▼
┌──────────────────────┐  ┌──────────────────────┐
│  UserPointTable      │  │  PointHistoryTable   │
│  - 실제 데이터 저장소  │  │  - 실제 데이터 저장소  │
└──────────────────────┘  └──────────────────────┘
```

### 오버플로우 검증 전략

```
충전 요청 (userId, amount)
    │
    ▼
┌──────────────────────────┐
│  Controller 검증          │
│  - @Positive (amount > 0)│
└──────────┬───────────────┘
           │
           ▼ (양수만 통과)
┌──────────────────────────┐
│  Service 비즈니스 검증     │
│  - 현재 잔액 조회          │
│  - validateMaxPoint()     │
│    if (current > max - amt)│
│      throw OVERFLOW       │
└──────────┬───────────────┘
           │
           ▼ (검증 통과)
┌──────────────────────────┐
│  포인트 업데이트          │
│  - insertOrUpdate()      │
│  - insert() 거래 내역     │
└──────────────────────────┘
```

---

## 📊 최종 구현 결과

### 주요 성과

✅ **일관된 아키텍처**
- Phase 1 패턴 재사용 (Repository 인터페이스)
- 레이어별 책임 명확
- DIP 일관성 유지

✅ **외부 설정 기반 유연성**
- application.yml로 최대값 관리
- 환경별 설정 가능
- Testable Code 구현

✅ **사용자 친화적 예외 처리**
- 충전 가능 금액 메시지 포함
- 명확한 에러 코드 (POINT_OVERFLOW)

✅ **높은 테스트 품질**
- 경계값 테스트 포함
- Mock을 활용한 단위 테스트
- Given-When-Then 패턴

✅ **TDD 원칙 준수**
- Red-Green-Refactor 사이클
- 테스트 우선 작성

### 테스트 결과
- **Phase 1**: 1개 테스트 ✅
- **Phase 2**: 3개 테스트 ✅
- **Total**: 4/4 passing

---

## 🔄 리팩토링 히스토리

| 순서 | 리팩토링 내용 | 목적 |
|------|--------------|------|
| 1 | PointHistoryRepository 인터페이스 도입 | 의존성 역전, 테스트 가능성 향상 |
| 2 | ErrorCode enum 확장 (POINT_OVERFLOW) | 동적 에러 메시지 지원 |
| 3 | @Value 어노테이션 도입 | 외부 설정화, 유연성 확보 |
| 4 | validateMaxPoint 메서드 추출 | SRP, 가독성 향상 |
| 5 | @InjectMocks → 수동 주입 | Testable Code 구현 |
| 6 | 경계값 테스트 추가 | 엣지 케이스 검증 |
| 7 | 에러 메시지 검증 강화 | UX 품질 보장 |

---

## 📝 향후 개선 사항

### Priority 1: 트랜잭션 원자성 보장
- 현재: In-memory 구조로 원자성 보장 불가
- 향후: 실제 DB 사용 시 `@Transactional` 적용
- Phase 5 (동시성)에서 락 기반 원자성 구현 예정

### Priority 2: 다음 기능 구현
- [ ] 포인트 사용 기능 (Phase 3)
- [ ] 포인트 내역 조회 기능 (Phase 4)
- [ ] 동시성 처리 (Phase 5)

---

## 💡 배운 점 (Lessons Learned)

1. **Testable Code의 중요성**
   - @Value 파라미터는 @InjectMocks와 호환되지 않음
   - 명시적 생성자 주입으로 테스트 가능성 확보
   - Spring 어노테이션에 과도하게 의존하지 말 것

2. **사용자 친화적 에러 메시지**
   - 단순한 에러 메시지보다 액션 가능한 정보 제공
   - "최대 {0}원까지 충전 가능" 메시지로 UX 개선

3. **경계값 테스트의 가치**
   - `>=` vs `>` 연산자 실수 방지
   - 엣지 케이스에서 버그 발견 가능

4. **DIP 일관성**
   - Phase 1의 UserPointRepository 패턴 재사용
   - 코드베이스 일관성으로 유지보수성 향상

5. **오버플로우 방지 로직**
   - `currentBalance + amount > max` → long 오버플로우 위험
   - `currentBalance > max - amount` → 안전한 검증

---

## 📅 2025-10-23

### 🎯 Step 1 - 3: 포인트 사용 기능 구현

---

## 1️⃣ 요구사항 분석

### 비즈니스 규칙
- 사용 금액은 양수여야 함 (Controller 레이어 검증)
- 현재 잔액보다 많은 금액 사용 불가
- 잔액은 음수가 될 수 없음
- 잔액 0까지 사용 가능 (경계값)
- USE 타입 거래 내역 자동 기록
- 잔액 부족 시 현재 잔액 정보 포함한 에러 메시지 반환

### 테스트 시나리오
**정상 케이스:**
1. 충분한 잔액이 있을 때 포인트 사용 성공 + USE 타입 거래 내역 생성 (통합 검증)
2. 잔액을 전부 사용 (경계값 - 잔액 0)

**예외 케이스:**
3. 잔액 부족 시 예외 발생 (현재 잔액 메시지 포함)

---

## 2️⃣ TDD Red Phase

### 실패하는 테스트 작성

**작성된 테스트:**
```java
@Test
@DisplayName("포인트 사용 성공 시, 잔액이 감소하고 USE 타입 거래 내역이 생성되어야 한다")
void usePoint_success_decreasesBalanceAndCreatesHistory()

@Test
@DisplayName("예상 잔액이 0인 경우 포인트 사용에 성공한다")
void usePoint_success_whenBalanceBecomesZero()

@Test
@DisplayName("잔액이 부족하면 예외가 발생하고, 현재 잔액이 메시지에 포함되어야 한다")
void usePoint_WithInsufficientBalance_ShouldThrowExceptionWithCurrentBalance()
```

**테스트 결과:**
- ❌ 3개 실패 (컴파일 에러)
  - `usePoint()` 메서드 미구현
  - `ErrorCode.INSUFFICIENT_POINTS` 미정의

---

## 3️⃣ TDD Green Phase

### 최소 구현

**1. ErrorCode 확장**
```java
INSUFFICIENT_POINTS(HttpStatus.BAD_REQUEST, "Insufficient points. Your current balance is {0}.");
```

**2. PointService.usePoint() 구현**
```java
public UserPoint usePoint(long userId, long amount) {
    // 1. 현재 포인트 조회
    UserPoint currentPoint = userPointRepository.selectById(userId);
    long currentBalance = currentPoint.point();

    // 2. 잔액 부족 검증
    validateSufficientBalance(currentBalance, amount);

    // 3. 새로운 잔액 계산
    long newBalance = currentBalance - amount;

    // 4. 포인트 업데이트
    UserPoint updatedPoint = userPointRepository.insertOrUpdate(userId, newBalance);

    // 5. 거래 내역 기록
    pointHistoryRepository.insert(userId, amount, TransactionType.USE, System.currentTimeMillis());

    // 6. 업데이트된 포인트 반환
    return updatedPoint;
}

private void validateSufficientBalance(long currentBalance, long amount) {
    if (currentBalance < amount) {
        throw new PointException(ErrorCode.INSUFFICIENT_POINTS, String.valueOf(currentBalance));
    }
}
```

**3. PointController.use() 엔드포인트 구현**
```java
@PatchMapping("{id}/use")
public UserPoint use(
        @PathVariable @Positive long id,
        @RequestBody @Positive long amount
) {
    return pointService.usePoint(id, amount);
}
```

**테스트 결과:**
- ✅ 3개 통과 (Phase 3)
- ✅ 전체 7개 통과 (Phase 1: 1개, Phase 2: 3개, Phase 3: 3개)

---

## 4️⃣ TDD Refactor Phase

### 리팩토링 1: UserException → PointException 리네이밍

**문제**: `UserException`이라는 이름이 User 도메인과 혼동 가능

**해결**:
- `PointException` 클래스 생성
- 모든 참조 위치 업데이트 (Service, Test)
- `ApiControllerAdvice`에 `PointException` 전용 핸들러 추가
- `UserException.java` 파일 삭제

**효과**:
- 도메인 명확성 향상 (Point 시스템 예외임을 명시)
- 일관된 네이밍 (PointService, PointController, PointException)
- 예외 처리 구조 개선

### 리팩토링 2: Controller JavaDoc 업데이트

**문제**: `charge()` 메서드에 TODO 주석 남아있음

**해결**: 실제 구현과 일치하는 상세한 JavaDoc 작성

**효과**: 문서 일관성 향상

### 리팩토링 3: 테스트 케이스 통합

**문제**: 잔액 감소와 거래 내역 생성을 별도 테스트로 검증

**해결**:
- 두 테스트를 하나로 통합
- `usePoint_success_decreasesBalanceAndCreatesHistory` 테스트에서 잔액 감소 + 거래 내역 생성 모두 검증
- Then 블록에서 `assertThat`과 `verify`를 함께 사용

**효과**:
- 테스트 개수 감소 (4개 → 3개)
- 테스트 의도 명확화 (포인트 사용은 잔액 감소 + 내역 생성이 원자적으로 일어남)
- DRY 원칙 적용 (중복 Given 블록 제거)

---

## 5️⃣ 최종 구현 결과

### 주요 성과

✅ **Phase 2 패턴 재사용**
- chargePoint()와 동일한 구조
- 검증 메서드 추출 (validateSufficientBalance)
- 단계별 주석으로 명확한 로직 흐름

✅ **사용자 친화적 에러 메시지**
- 잔액 부족 시 현재 잔액 표시
- Phase 2의 충전 가능 금액 표시 패턴과 일관성

✅ **경계값 테스트**
- 잔액 0까지 사용 가능 검증
- `balance < amount` 조건으로 정확한 검증

✅ **예외 클래스 리네이밍**
- PointException으로 도메인 명확성 향상
- ApiControllerAdvice 핸들러 추가

### 기술 스택
- Java 17
- Spring Boot 3.2.0
- PointException (UserException 리네이밍)
- JUnit 5 + Mockito + AssertJ

---

## 🔄 리팩토링 히스토리

| 순서 | 리팩토링 내용 | 목적 |
|------|--------------|------|
| 1 | UserException → PointException 리네이밍 | 도메인 명확성 향상, User와 혼동 방지 |
| 2 | ApiControllerAdvice 핸들러 추가 | PointException 전용 핸들러로 예외 처리 개선 |
| 3 | Controller JavaDoc 업데이트 | TODO 제거, 문서 일관성 확보 |
| 4 | 테스트 케이스 통합 (4개 → 3개) | 잔액 감소 + 거래 내역 생성을 하나의 원자적 동작으로 검증, DRY 원칙 |

---

## 📝 향후 개선 사항

### Priority 1: 트랜잭션 원자성 보장
- 현재: In-memory 구조로 원자성 보장 불가
- 향후: Phase 5 (동시성)에서 락 기반 원자성 구현 예정

### Priority 2: 다음 기능 구현
- [ ] 포인트 내역 조회 기능 (Phase 4)
- [ ] 동시성 처리 (Phase 5)

---

## 💡 배운 점 (Lessons Learned)

**1. 일관된 패턴의 힘**
- Phase 2 chargePoint 구조를 재사용하여 빠른 구현
- 검증 메서드 추출 패턴 일관성 (validateMaxPoint, validateSufficientBalance)
- 새로운 기능도 기존 패턴을 따르면 안정적

**2. 도메인 기반 네이밍의 중요성**
- UserException → PointException
- Point 시스템의 예외임을 명확히 표현
- 네이밍만으로 의도 파악 가능

**3. 사용자 친화적 에러 메시지**
- 단순한 실패 메시지가 아닌 액션 가능한 정보 제공
- "현재 잔액: X" 메시지로 사용자가 얼마나 사용 가능한지 즉시 알 수 있음

**4. 경계값 테스트의 가치**
- 잔액 0 케이스 검증으로 `<` vs `<=` 연산자 실수 방지
- 극단적인 케이스에서 버그 발견 가능

**5. TDD의 안정감**
- Red → Green → Refactor 사이클 준수
- 리팩토링 후에도 모든 테스트 통과 확인으로 회귀 방지

**6. 테스트 리팩토링의 가치**
- 잔액 감소 + 거래 내역 생성을 별도 테스트로 분리할 필요 없음
- 포인트 사용은 원자적 동작이므로 하나의 테스트로 통합 검증
- 테스트 개수보다 테스트 의도 명확성이 중요
- 불필요한 Given 블록 중복 제거로 유지보수성 향상

---

## 📅 2025-10-23

### 🎯 Step 1 - 4: 포인트 내역 조회 기능 구현

---

## 1️⃣ 요구사항 분석

### 비즈니스 규칙
- 특정 유저의 모든 포인트 거래 내역 조회
- 유저 ID는 양수여야 함 (Controller 레이어 검증)
- 거래 내역이 없는 경우 빈 리스트 반환
- CHARGE, USE 타입 모두 포함

### 테스트 시나리오
**정상 케이스:**
1. 포인트 내역 조회 시 레포지토리에서 조회한 값을 반환

---

## 2️⃣ 설계 결정: SRP 적용

### 문제 인식
- PointService가 포인트 잔액 관리(조회/충전/사용)와 거래 내역 조회를 모두 담당
- 단일 책임 원칙(SRP) 위반

### 해결 방안
**PointHistoryService 분리**
- **PointService**: 포인트 잔액 관리 (조회/충전/사용)
- **PointHistoryService**: 거래 내역 조회
- Phase 1의 getUserPoint() 패턴 재사용 (Repository 반환값 그대로 전달)

---

## 3️⃣ TDD Red Phase

### 실패하는 테스트 작성

**PointHistoryServiceTest.java 생성:**
```java
@Test
@DisplayName("포인트 내역 조회 시 레포지토리에서 조회한 값을 반환해야 한다")
void getPointHistory_ShouldReturnHistoriesFromRepository() {
    // Given
    long userId = 1L;
    PointHistory history1 = new PointHistory(1L, userId, 1000L, TransactionType.CHARGE, timestamp);
    PointHistory history2 = new PointHistory(2L, userId, 500L, TransactionType.USE, timestamp + 1000L);
    List<PointHistory> expectedHistories = List.of(history1, history2);

    when(pointHistoryRepository.selectAllByUserId(userId)).thenReturn(expectedHistories);

    // When
    List<PointHistory> result = pointHistoryService.getPointHistory(userId);

    // Then
    assertThat(result).isEqualTo(expectedHistories);
    assertThat(result).hasSize(2);
}
```

**테스트 결과:**
- ❌ 1개 실패 (컴파일 에러)
  - `PointHistoryService` 클래스 미구현
  - `getPointHistory()` 메서드 미정의

---

## 4️⃣ TDD Green Phase

### 최소 구현

**1. PointHistoryService.java 생성**
```java
@Service
public class PointHistoryService {

    private final PointHistoryRepository pointHistoryRepository;

    public PointHistoryService(PointHistoryRepository pointHistoryRepository) {
        this.pointHistoryRepository = pointHistoryRepository;
    }

    /**
     * 특정 유저의 포인트 거래 내역을 조회합니다.
     * <p>
     * 유저 ID 검증은 Controller 레이어에서 {@code @Positive} 어노테이션을 통해 수행되므로,
     * 이 메서드는 항상 유효한(양수) userId를 받습니다.
     * 거래 내역이 없는 경우 빈 리스트가 반환됩니다.
     *
     * @param userId 조회할 유저 ID (양수, Controller 레이어에서 검증됨)
     * @return 유저의 거래 내역 리스트 (거래 내역이 없으면 빈 리스트)
     */
    public List<PointHistory> getPointHistory(long userId) {
        return pointHistoryRepository.selectAllByUserId(userId);
    }
}
```

**2. PointController 수정**
```java
private final PointHistoryService pointHistoryService;

public PointController(PointService pointService, PointHistoryService pointHistoryService) {
    this.pointService = pointService;
    this.pointHistoryService = pointHistoryService;
}

/**
 * 특정 유저의 포인트 충전/이용 내역을 조회합니다.
 * <p>
 * 유저 ID는 {@code @Positive} 어노테이션으로 검증되며, 양수가 아닌 경우 400 Bad Request를 반환합니다.
 * 거래 내역이 없는 경우 빈 리스트를 반환합니다.
 *
 * @param id 조회할 유저 ID (양수만 허용)
 * @return 유저의 포인트 거래 내역 리스트
 */
@GetMapping("{id}/histories")
public List<PointHistory> history(
        @PathVariable @Positive long id
) {
    return pointHistoryService.getPointHistory(id);
}
```

**테스트 결과:**
- ✅ 1개 통과 (Phase 4)
- ✅ 전체 8개 통과 (Phase 1: 1개, Phase 2: 3개, Phase 3: 3개, Phase 4: 1개)

---

## 5️⃣ TDD Refactor Phase - 테스트 코드 리팩토링

### 별도 작업: PointServiceTest 리팩토링

**문제**: Given 블록에 중복 코드 많음
- `System.currentTimeMillis()` 반복 호출
- UserPoint, PointHistory 객체 생성 반복
- Mock 설정 패턴 반복

**해결**:

**1. 공통 상수 및 필드 추가**
```java
private static final long DEFAULT_USER_ID = 1L;
private long currentTime;

@BeforeEach
void setUp() {
    pointService = new PointService(TEST_MAX, userPointRepository, pointHistoryRepository);
    currentTime = System.currentTimeMillis();
}
```

**2. 헬퍼 메서드 - 테스트 데이터 생성**
```java
private UserPoint createUserPoint(long userId, long point) {
    return new UserPoint(userId, point, currentTime);
}

private PointHistory createPointHistory(long id, long userId, long amount, TransactionType type) {
    return new PointHistory(id, userId, amount, type, currentTime);
}
```

**3. 헬퍼 메서드 - Mock 설정**
```java
private void mockUserPointSelect(long userId, long currentBalance) {
    when(userPointRepository.selectById(userId))
        .thenReturn(createUserPoint(userId, currentBalance));
}

private void mockUserPointUpdate(long userId, long newBalance) {
    when(userPointRepository.insertOrUpdate(userId, newBalance))
        .thenReturn(createUserPoint(userId, newBalance));
}

private void mockPointHistoryInsert(long userId, long amount, TransactionType type) {
    when(pointHistoryRepository.insert(eq(userId), eq(amount), eq(type), anyLong()))
        .thenReturn(createPointHistory(1L, userId, amount, type));
}
```

**4. 테스트 코드 리팩토링 적용 예시**

**Before:**
```java
@Test
void chargeSuccess_IncreasesBalanceAndCreatesHistory() {
    // Given
    long userId = 1L;
    long currentBalance = 5000L;
    long chargeAmount = 1000L;
    long expectedBalance = 6000L;

    UserPoint currentPoint = new UserPoint(userId, currentBalance, System.currentTimeMillis());
    UserPoint updatedPoint = new UserPoint(userId, expectedBalance, System.currentTimeMillis());

    when(userPointRepository.selectById(userId)).thenReturn(currentPoint);
    when(userPointRepository.insertOrUpdate(userId, expectedBalance)).thenReturn(updatedPoint);

    PointHistory expectedHistory = new PointHistory(1L, userId, chargeAmount, TransactionType.CHARGE, System.currentTimeMillis());
    when(pointHistoryRepository.insert(eq(userId), eq(chargeAmount), eq(TransactionType.CHARGE), anyLong()))
        .thenReturn(expectedHistory);

    // When
    UserPoint result = pointService.chargePoint(userId, chargeAmount);

    // Then
    assertThat(result.id()).isEqualTo(userId);
    assertThat(result.point()).isEqualTo(expectedBalance);
    assertThat(result.updateMillis()).isEqualTo(updatedPoint.updateMillis());
}
```

**After:**
```java
@Test
void chargeSuccess_IncreasesBalanceAndCreatesHistory() {
    // Given
    long currentBalance = 5000L;
    long chargeAmount = 1000L;
    long expectedBalance = 6000L;

    mockUserPointSelect(DEFAULT_USER_ID, currentBalance);
    mockUserPointUpdate(DEFAULT_USER_ID, expectedBalance);
    mockPointHistoryInsert(DEFAULT_USER_ID, chargeAmount, TransactionType.CHARGE);

    // When
    UserPoint result = pointService.chargePoint(DEFAULT_USER_ID, chargeAmount);

    // Then
    assertThat(result.id()).isEqualTo(DEFAULT_USER_ID);
    assertThat(result.point()).isEqualTo(expectedBalance);
    assertThat(result.updateMillis()).isEqualTo(currentTime);
}
```

**효과**:
- Given 블록 간소화 (15줄 → 5줄)
- 테스트 의도 명확화
- 중복 코드 제거 (DRY 원칙)
- 유지보수성 향상 (Mock 패턴 변경 시 헬퍼 메서드만 수정)
- 일관성 확보 (currentTime 통일)

**테스트 결과:**
- ✅ 7/7 passing (리팩토링 후에도 모든 테스트 통과)

---

## 6️⃣ 최종 구현 결과

### 주요 성과

✅ **SRP (Single Responsibility Principle) 적용**
- PointService: 포인트 잔액 관리
- PointHistoryService: 거래 내역 조회
- 서비스 클래스 책임 명확화

✅ **Phase 1 패턴 재사용**
- 단순 조회 기능은 Repository 반환값 그대로 전달
- 아키텍처 일관성 유지

✅ **향후 확장 가능한 구조**
- 기간별 조회, 타입별 필터링, 페이징 등 추가 용이
- PointHistoryService만 수정하면 됨

✅ **테스트 코드 품질 향상**
- 헬퍼 메서드 도입으로 가독성 대폭 향상
- DRY 원칙 적용
- 유지보수성 향상

### 기술 스택
- Java 17
- Spring Boot 3.2.0
- JUnit 5 + Mockito + AssertJ

---

## 🔄 리팩토링 히스토리

| 순서 | 리팩토링 내용 | 목적 |
|------|--------------|------|
| 1 | PointHistoryService 분리 | SRP 적용, 책임 명확화 |
| 2 | Phase 1 패턴 재사용 | 아키텍처 일관성, 단순성 유지 |
| 3 | PointServiceTest 헬퍼 메서드 도입 | 테스트 가독성, DRY 원칙 |
| 4 | 공통 상수 추출 (DEFAULT_USER_ID, currentTime) | 테스트 일관성, 유지보수성 |
| 5 | Given 블록 간소화 | 테스트 의도 명확화 |

---

## 📝 향후 개선 사항

### Priority 1: PointHistoryService 확장
- 기간별 조회 (startDate, endDate)
- 타입별 필터링 (CHARGE/USE)
- 페이징 처리
- 정렬 옵션 (최신순, 오래된순, 금액순)

### Priority 2: 다음 기능 구현
- [ ] 동시성 처리 (Phase 5)
- [ ] 통합 테스트 작성
- [ ] 코드 커버리지 검증

---

## 💡 배운 점 (Lessons Learned)

**1. SRP의 실용적 적용**
- 처음부터 완벽한 분리보다 필요시점에 분리
- Phase 4에서 PointHistoryService를 분리한 이유:
  - 포인트 잔액 관리와 내역 조회는 서로 다른 책임
  - 향후 내역 조회 기능 확장 시 PointService 영향 없음
  - 테스트 격리 및 유지보수성 향상

**2. 테스트 코드도 리팩토링 대상**
- Given 블록 중복은 프로덕션 코드 중복만큼 해로움
- 헬퍼 메서드 도입으로 테스트 가독성 대폭 향상
- 테스트 코드 품질 = 프로덕션 코드 품질

**3. 일관된 패턴의 힘**
- Phase 1의 단순 조회 패턴을 Phase 4에 재사용
- 새로운 기능도 기존 패턴을 따르면 안정적
- 학습 곡선 감소

**4. 헬퍼 메서드 명명 규칙**
- `create*()`: 테스트 데이터 생성
- `mock*()`: Mock 설정
- 명확한 네이밍으로 의도 파악 용이

**5. 점진적 개선의 가치**
- Phase 1-3 구현 → Phase 4 기능 추가 → 테스트 코드 리팩토링
- 한 번에 완벽하게 하려 하지 말고 단계적 개선
- 각 단계마다 테스트 통과 확인으로 안정성 보장

---

*Last Updated: 2025-10-23*
