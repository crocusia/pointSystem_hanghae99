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
   - ✅ 신규 유저 조회 (0 포인트 반환)
   - ✅ 기존 유저 조회 (현재 잔액 반환)

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

    @InjectMocks
    private PointService pointService;

    @Test
    @DisplayName("신규 유저의 포인트 조회 시 0 포인트를 반환해야 한다")
    void getUserPoint_WhenUserIsNew_ShouldReturnZeroBalance() {
       // Given
       Long userId = 1L;
       UserPoint emptyUserPoint = new UserPoint(userId, 0, System.currentTimeMillis());
       // Stub
       when(userPointRepository.selectById(userId)).thenReturn(emptyUserPoint);

       // When
       UserPoint result = pointService.getUserPoint(userId);

       // Then
       assertThat(result.id()).isEqualTo(userId);
       assertThat(result.point()).isEqualTo(0L);
    }

    @Test
    @DisplayName("기존 유저의 포인트 조회 시 현재 잔액을 반환해야 한다")
    void getUserPoint_WhenUserExists_ShouldReturnCurrentBalance() {
        // Given
        long userId = 2L;
        long expectedPoints = 5000L;
        long currentTime = System.currentTimeMillis();
        UserPoint existingUserPoint = new UserPoint(userId, expectedPoints, currentTime);
        when(userPointRepository.selectById(userId)).thenReturn(existingUserPoint);

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
- **Phase 1**: 2개 테스트 ✅
- **Phase 2**: 3개 테스트 ✅
- **Total**: 5/5 passing

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

*Last Updated: 2025-10-23*
