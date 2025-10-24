package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PointService 동시성 제어 통합 테스트
 *
 * ReentrantLock 기반 동시성 제어가 올바르게 동작하는지 검증합니다.
 * - 동일 사용자의 동시 요청은 직렬화되어 처리
 * - 서로 다른 사용자의 요청은 병렬 처리
 */
@DisplayName("PointService 동시성 제어 통합 테스트")
class PointServiceConcurrencyTest {

    private PointService pointService;
    private UserPointRepository userPointRepository;
    private PointHistoryRepository pointHistoryRepository;

    // 테스트 상수
    private static final long TEST_MAX_POINT = 10000L;
    private static final long TIMEOUT_SECONDS = 30L;

    // 테스트 데이터 상수
    private static final long BALANCE_SUFFICIENT = 5000L;
    private static final long BALANCE_NEAR_MAX = 9000L;
    private static final long AMOUNT_CHARGE = 1000L;
    private static final long AMOUNT_USE = 1000L;
    private static final long AMOUNT_OVERFLOW_TEST = 500L;

    // 테스트 격리를 위한 고유 사용자 ID 생성기
    private static final AtomicLong USER_ID_GENERATOR = new AtomicLong(1);

    @BeforeEach
    void setUp() {
        userPointRepository = new UserPointTable();
        pointHistoryRepository = new PointHistoryTable();
        pointService = new PointService(TEST_MAX_POINT, userPointRepository, pointHistoryRepository);
    }

    /**
     * 고유한 사용자 ID 생성
     */
    private long getUniqueUserId() {
        return USER_ID_GENERATOR.getAndIncrement();
    }

    /**
     * 동시성 환경에서 모든 스레드가 동시에 시작하도록 작업을 실행하는 헬퍼 메서드
     *
     * startLatch를 사용하여 실제 운영 환경의 "동시 요청" 상황을 재현합니다.
     * 이를 통해 Lock이 제대로 동시성을 제어하는지 검증할 수 있습니다.
     */
    private void executeConcurrently(int threadCount, Runnable task)
        throws InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    startLatch.await(); // 모든 스레드가 동시에 시작
                    task.run();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown(); // 모든 작업 동시 시작!
        awaitAndShutdown(executorService, endLatch);
    }

    /**
     * 동시성 환경에서 예외를 수집하며 모든 스레드가 동시에 시작하도록 실행하는 헬퍼 메서드
     */
    private List<Throwable> executeConcurrentlyWithExceptions(
        int threadCount,
        Runnable task
    ) throws InterruptedException {
        List<Throwable> exceptions = new CopyOnWriteArrayList<>();
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    startLatch.await(); // 모든 스레드가 동시에 시작
                    task.run();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    exceptions.add(e);
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown(); // 모든 작업 동시 시작!
        awaitAndShutdown(executorService, endLatch);

        return exceptions;
    }

    /**
     * ExecutorService 종료 및 타임아웃 검증
     *
     * 중복 코드를 제거하기 위한 공통 메서드
     */
    private void awaitAndShutdown(
        ExecutorService executorService,
        CountDownLatch endLatch
    ) throws InterruptedException {
        boolean completed = endLatch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        executorService.shutdown();

        if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
            executorService.shutdownNow();
        }

        assertThat(completed)
            .as("모든 작업이 %d초 내에 완료되어야 함", TIMEOUT_SECONDS)
            .isTrue();
    }

    /**
     * 테스트 설계)
     * 실제 운영 환경에서 여러 요청이 "동시에" 들어오는 상황을 재현하여
     * 동일 사용자의 요청이 Lock에 의해 순차적으로 처리되는지 검증
     *
     * case 1) 동일 사용자의 동시 충전 요청은 순차적으로 처리, 정확한 결과 반환
     */
    @Test
    @DisplayName("동일 사용자의 동시 충전 요청은 순차적으로 처리되어 정확한 최종 잔액을 반환해야 한다")
    void concurrentCharge_SameUser_ShouldProcessSequentially()
        throws InterruptedException {
        // Given
        long userId = getUniqueUserId();
        int chargeCount = 10;
        long expectedFinalBalance = AMOUNT_CHARGE * chargeCount;

        // When - 충전 요청을 동시에 시작
        executeConcurrently(
            chargeCount,
            () -> pointService.chargePoint(userId, AMOUNT_CHARGE)
        );

        // Then - 최종 잔액 정합성 검증
        UserPoint finalUserPoint = pointService.getUserPoint(userId);
        assertThat(finalUserPoint.point())
            .as("최종 잔액이 예상값과 일치해야 함")
            .isEqualTo(expectedFinalBalance);

        // 거래 내역 개수 검증 (동시성 제어 검증)
        List<PointHistory> histories =
            pointHistoryRepository.selectAllByUserId(userId);
        assertThat(histories)
            .as("모든 거래가 정상 처리되어야 함")
            .hasSize(chargeCount);
    }

    /**
     * 테스트 설계)
     * 서로 다른 사용자는 독립적인 Lock을 가지므로 병렬 처리되어야 함
     * 각 사용자의 작업이 서로 블로킹되지 않는지 검증
     *
     * case 2) 서로 다른 사용자 요청은 독립적으로 병렬 처리
     */
    @Test
    @DisplayName("서로 다른 사용자의 동시 요청은 독립적으로 병렬 처리되어야 한다")
    void concurrentOperations_DifferentUsers_ShouldProcessIndependently()
        throws InterruptedException {
        // Given
        int userCount = 5;
        long[] userIds = new long[userCount];
        for (int i = 0; i < userCount; i++) {
            userIds[i] = getUniqueUserId();
        }

        // When - 각 사용자가 동시에 충전
        ExecutorService executorService = Executors.newFixedThreadPool(userCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(userCount);

        for (long userId : userIds) {
            executorService.submit(() -> {
                try {
                    startLatch.await(); // 동시 시작
                    pointService.chargePoint(userId, AMOUNT_CHARGE);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown(); // 모든 사용자 동시 시작!
        awaitAndShutdown(executorService, endLatch);

        // Then - 각 사용자의 잔액 및 독립성 검증
        for (long userId : userIds) {
            UserPoint userPoint = pointService.getUserPoint(userId);
            assertThat(userPoint.point())
                .as("사용자 %d의 잔액", userId)
                .isEqualTo(AMOUNT_CHARGE);

            // 각 사용자의 거래 내역이 독립적으로 기록되었는지 확인
            List<PointHistory> histories =
                pointHistoryRepository.selectAllByUserId(userId);
            assertThat(histories)
                .as("사용자 %d의 거래 내역", userId)
                .hasSize(1);
        }
    }

    /**
     * 테스트 설계)
     * 실제 운영 환경에서 잔액 부족 상황이 동시에 발생했을 때
     * 일부는 성공하고 일부는 실패하지만 데이터 정합성은 유지되어야 함
     *
     * case 3) 포인트 사용 - 잔액 부족 시, 데이터 정합성 유지
     */
    @Test
    @DisplayName("동시 사용 요청 시 잔액 부족으로 일부 요청이 실패해도 데이터 정합성이 유지되어야 한다")
    void concurrentUse_InsufficientBalance_ShouldMaintainConsistency()
        throws InterruptedException {
        // Given - 잔액 부족 시나리오
        long userId = getUniqueUserId();
        int attemptCount = 10;
        int expectedSuccessCount = 5;  // 5000 / 1000 = 5
        int expectedFailureCount = 5;  // 10 - 5 = 5

        userPointRepository.insertOrUpdate(userId, BALANCE_SUFFICIENT);

        // When - 예외를 수집하며 동시 실행
        List<Throwable> exceptions = executeConcurrentlyWithExceptions(
            attemptCount,
            () -> pointService.usePoint(userId, AMOUNT_USE)
        );

        // Then - 데이터 정합성 검증
        UserPoint finalUserPoint = pointService.getUserPoint(userId);

        assertThat(finalUserPoint.point())
            .as("최종 잔액은 0이어야 함")
            .isEqualTo(0L);

        List<PointHistory> histories =
            pointHistoryRepository.selectAllByUserId(userId);

        assertThat(histories)
            .as("성공한 거래 내역")
            .hasSize(expectedSuccessCount);

        assertThat(exceptions)
            .as("실패한 요청")
            .hasSize(expectedFailureCount)
            .allMatch(e -> e instanceof PointException &&
                ((PointException) e).getErrorCode() ==
                    ErrorCode.INSUFFICIENT_POINTS);
    }

    /**
     * 테스트 설계)
     * 실제 운영 환경에서 최대값 근처에서 동시 충전이 발생했을 때
     * 일부는 성공하고 일부는 실패하지만 데이터 정합성은 유지되어야 함
     *
     * case 4) 포인트 충전 - 상한선 초과 시, 데이터 정합성 유지
     */
    @Test
    @DisplayName("동시 충전 요청 시 최대값 초과로 일부 요청이 실패해도 데이터 정합성이 유지되어야 한다")
    void concurrentCharge_MaxPointExceeded_ShouldMaintainConsistency()
        throws InterruptedException {
        // Given - 최대값 근처 시나리오
        long userId = getUniqueUserId();
        int attemptCount = 10;
        int expectedSuccessCount = 2;  // 9000 + 500 + 500 = 10000
        int expectedFailureCount = 8;  // 나머지 8번 실패

        userPointRepository.insertOrUpdate(userId, BALANCE_NEAR_MAX);

        // When - 예외를 수집하며 동시 실행
        List<Throwable> exceptions = executeConcurrentlyWithExceptions(
            attemptCount,
            () -> pointService.chargePoint(userId, AMOUNT_OVERFLOW_TEST)
        );

        // Then - 데이터 정합성 검증
        UserPoint finalUserPoint = pointService.getUserPoint(userId);

        assertThat(finalUserPoint.point())
            .as("최종 잔액은 최대값이어야 함")
            .isEqualTo(TEST_MAX_POINT);

        List<PointHistory> histories =
            pointHistoryRepository.selectAllByUserId(userId);

        assertThat(histories)
            .as("성공한 충전 거래 내역")
            .hasSize(expectedSuccessCount);

        assertThat(exceptions)
            .as("실패한 충전 요청")
            .hasSize(expectedFailureCount)
            .allMatch(e -> e instanceof PointException &&
                ((PointException) e).getErrorCode() ==
                    ErrorCode.POINT_OVERFLOW);
    }
}
