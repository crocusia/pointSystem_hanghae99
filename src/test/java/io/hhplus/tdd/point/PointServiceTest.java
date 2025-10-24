package io.hhplus.tdd.point;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;

/**
 * Phase 1: Point Inquiry Feature - Unit Tests
 * TDD Red Stage - Tests that will fail until PointService is implemented
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PointService Unit Tests")
class PointServiceTest {

    /** 리팩토링 1) userPointTable은 외부 의존성이기 때문에 Mocking이 필요함
     *  문제) Mocking을 하려면 PointService는 userPointTable을 직접 의존하는 부분을 제거해야 함.
     *  해결) DI를 위해 userPointTable의 인터페이스를 생성, userPointTable에 implement (구현부는 변경하지 않음)
     */
    @Mock
    private UserPointRepository userPointRepository;

    @Mock
    private PointHistoryRepository pointHistoryRepository;

    private PointService pointService;

    private static final long TEST_MAX = 100_000L;
    private static final long DEFAULT_USER_ID = 1L;
    private long currentTime;

    @BeforeEach
    void setUp() {
        pointService = new PointService(TEST_MAX, userPointRepository, pointHistoryRepository);
        currentTime = System.currentTimeMillis();
    }

    // Helper methods for creating test data
    private UserPoint createUserPoint(long userId, long point) {
        return new UserPoint(userId, point, currentTime);
    }

    private PointHistory createPointHistory(long id, long userId, long amount, TransactionType type) {
        return new PointHistory(id, userId, amount, type, currentTime);
    }

    // Helper methods for mock setup
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

    /**
     * 테스트 설계)
     * 서비스 관점에서 포인트 조회는 단순히 레포지토리에서 조회한 값을 반환하는 동작임
     * 신규/기존 유저 구분은 레포지토리의 책임이므로 서비스 테스트에서는 검증 불필요
     *
     * case 1) 유저 포인트 조회 시 올바른 userId로 레포지토리를 호출하는가 = 행위 검증 verify
     */

    @Test
    @DisplayName("유저 포인트 조회 시 올바른 userId로 레포지토리를 호출해야 한다")
    void getUserPoint_ShouldCallRepositoryWithCorrectUserId() {
        // Given
        mockUserPointSelect(DEFAULT_USER_ID, 1000L);

        // When
        pointService.getUserPoint(DEFAULT_USER_ID);

        // Then
        verify(userPointRepository).selectById(DEFAULT_USER_ID);
    }

    /**
     * 리팩토링 2) AI가 작성한 "포인트 조회"관련 일부 테스트 코드 삭제
     *
     * @DisplayName("포인트 조회 시 올바른 업데이트 타임스탬프를 반환해야 한다")
     * 사유) case1을 통해 이미 검증이 된, 중복 테스트이기 때문
     *
     * @DisplayName("여러 유저의 포인트를 독립적으로 조회할 수 있어야 한다")
     * 사유) case1을 통해 이미 조회 기능이 검증되었기 때문에 필요 없다고 생각함
     */

    /**
     * 리팩토링 3) 유저 id의 유효 범위(양수) 조건 설정 (공통 적용), Controller Validation 추가
     * 사유) 보편적으로 index는 양수를 사용하기 때문 (PointHistoryTable에서 내역을 저장할 때의 Index도 1부터 시작함)
     *
     * id가 null, 음수, 0, 오버플로우인 경우에 대한 검증이 필요해짐
     * 1. Controller에 validation 추가
     * 2. 예외 메시지가 하드 코딩된 부분을 개선하기 위해 enum으로 에러 코드 관리
     * 3. ApiControllerAdvice에 관련 예외 핸들러 추가
     */

    /**
     * ========================================
     * Phase 2: Point Charge Feature - Unit Tests
     * TDD Red Stage - Tests for chargePoint method
     * ========================================
     */

    /**
     * 테스트 설계)
     * 서비스 코드의 포인트 충전에서 사용되는 중요 기능 중심 검증
     * case 1) 금액 업데이트하고 거래 내역을 생성 메서드가 호출되는가
     */
    @Test
    @DisplayName("충전 성공 시, 올바른 금액으로 업데이트하고 거래 내역을 생성해야 한다")
    void chargeSuccess_IncreasesBalanceAndCreatesHistory() {
        // Given
        long currentBalance = 0L;
        long chargeAmount = 1000L;
        long expectedBalance = 1000L;

        mockUserPointSelect(DEFAULT_USER_ID, currentBalance);
        mockUserPointUpdate(DEFAULT_USER_ID, expectedBalance);
        mockPointHistoryInsert(DEFAULT_USER_ID, chargeAmount, TransactionType.CHARGE);

        // When
        pointService.chargePoint(DEFAULT_USER_ID, chargeAmount);

        // Then
        verify(userPointRepository).selectById(DEFAULT_USER_ID);
        verify(userPointRepository).insertOrUpdate(DEFAULT_USER_ID, expectedBalance);
        verify(pointHistoryRepository).insert(
            eq(DEFAULT_USER_ID),
            eq(chargeAmount),
            eq(TransactionType.CHARGE),
            anyLong()
        );
    }

    /**
     * 테스트 설계)
     * 충전하고자 하는 금액이 오버플로우가 아니어도, 기존 잔액 + 충전 금액의 합이 오버플로우가 될 수 있음
     * 최대값을 설정하고 이에 대해 경계값 테스트 수행
     * Long.MAX_VALUE에 대해서 테스트하려고 했지만 값이 불필요하게 큰 것 같아 적절한 최대값 설정으로 대체
     *
     * case 2) 두 값의 합이 최대값 초과 시, 실패
     * case 3) 두 값의 합이 최대값과 동일한 경우, 성공
     */

    /**
     * 리팩토링 1)
     * 사용자 편의성을 위해 에러코드 메시지에 현재 잔액 기준, 최대 충전 가능 금액 명시
     */
    @Test
    @DisplayName("잔액과 충전 금액의 합이 최대값을 초과하면 예외가 발생하고, 충전 가능 금액이 메시지에 포함되어야 한다")
    void chargePoint_WhenOverflow_ShouldThrowExceptionWithMaxChargeableAmount() {
        // Given
        long currentBalance = TEST_MAX - 1L;
        long chargeAmount = 2L;
        long expectedMaxChargeable = 1L;

        mockUserPointSelect(DEFAULT_USER_ID, currentBalance);

        // When & Then
        assertThatThrownBy(() -> pointService.chargePoint(DEFAULT_USER_ID, chargeAmount))
                .isInstanceOf(PointException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.POINT_OVERFLOW)
                .hasMessageContaining(String.valueOf(expectedMaxChargeable));
    }

    /**
     * 리팩토링 2)
     * 전역 변수였던 MAX_POINT_BALANCE를 Teatable Code를 위해 @value 기반 매개변수화
     */
    @Test
    @DisplayName("잔액과 충전 금액의 합이 최대값과 같으면 충전에 성공한다")
    void chargePoint_WhenSumEqualsMaxBalance_ShouldSucceed() {
        // Given
        long currentBalance = TEST_MAX - 1L;
        long chargeAmount = 1L;
        long expectedBalance = TEST_MAX;

        mockUserPointSelect(DEFAULT_USER_ID, currentBalance);
        mockUserPointUpdate(DEFAULT_USER_ID, expectedBalance);
        mockPointHistoryInsert(DEFAULT_USER_ID, chargeAmount, TransactionType.CHARGE);

        // When
        pointService.chargePoint(DEFAULT_USER_ID, chargeAmount);

        // Then
        verify(userPointRepository).selectById(DEFAULT_USER_ID);
        verify(userPointRepository).insertOrUpdate(DEFAULT_USER_ID, expectedBalance);
        verify(pointHistoryRepository).insert(
                eq(DEFAULT_USER_ID),
                eq(chargeAmount),
                eq(TransactionType.CHARGE),
                anyLong()
        );
    }

    //추후 실제 DB 사용 시, DB 기반 원자성 검증 필요

    /**
     * ========================================
     * Phase 3: Point Usage Feature - Unit Tests
     * TDD Red Stage - Tests for usePoint method
     * ========================================
     */

    /**
     * 테스트 설계)
     * 포인트 사용 기능의 핵심 동작 검증
     * case 1) 금액 업데이트, 거래 내역 생성 행위 검증
     */
    @Test
    @DisplayName("포인트 사용 성공 시 올바른 금액으로 업데이트하고 거래 내역을 생성해야 한다")
    void usePoint_success_decreasesBalanceAndCreatesHistory() {
        // Given
        long currentBalance = 1000L;
        long useAmount = 999L;
        long expectedBalance = 1L;

        mockUserPointSelect(DEFAULT_USER_ID, currentBalance);
        mockUserPointUpdate(DEFAULT_USER_ID, expectedBalance);
        mockPointHistoryInsert(DEFAULT_USER_ID, useAmount, TransactionType.USE);

        // When
        pointService.usePoint(DEFAULT_USER_ID, useAmount);

        // Then
        verify(userPointRepository).selectById(DEFAULT_USER_ID);
        verify(userPointRepository).insertOrUpdate(DEFAULT_USER_ID, expectedBalance);
        verify(pointHistoryRepository).insert(
            eq(DEFAULT_USER_ID),
            eq(useAmount),
            eq(TransactionType.USE),
            anyLong()
        );
    }

    /**
     * 테스트 설계)
     * case 2) 잔액이 부족할 때 예외 발생
     * 사용자 편의성을 위해 에러 메시지에 현재 잔액 포함
     */
    @Test
    @DisplayName("잔액이 부족하면 예외가 발생하고, 현재 잔액이 메시지에 포함되어야 한다")
    void usePoint_WithInsufficientBalance_ShouldThrowExceptionWithCurrentBalance() {
        // Given
        long currentBalance = 1000L;
        long useAmount = 1001L;

        mockUserPointSelect(DEFAULT_USER_ID, currentBalance);

        // When & Then
        assertThatThrownBy(() -> pointService.usePoint(DEFAULT_USER_ID, useAmount))
                .isInstanceOf(PointException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INSUFFICIENT_POINTS)
                .hasMessageContaining(String.valueOf(currentBalance));
    }
}