package io.hhplus.tdd.point;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class PointService {

    /**
     * 포인트 최대 잔액 제한
     * application.yml의 point.max-balance 설정값으로부터 주입됩니다.
     */
    private final long maxPointBalance;

    private final UserPointRepository userPointRepository;
    private final PointHistoryRepository pointHistoryRepository;

    /**
     * 동시성 제어 구현)
     * 사용자별 동시성 제어를 위한 Lock 맵에 ConcurrentHashMap 사용
     * 각 사용자마다 독립적인 ReentrantLock을 사용헤 동일 사용자의 동시 요청만 직렬화함
     *
     * ReentrantLock은 fair, non-fair 선택 가능
     * 충전 또는 사용은 순서가 중요하기 때문에 fair : true로 설정
     *
     * 반드시 unlock() 해주어야 하기 때문에 finally에서 unlock
     */
    private final ConcurrentHashMap<Long, ReentrantLock> userLocks = new ConcurrentHashMap<>();

    public PointService(
            @Value("${point.max-balance}") long maxPointBalance,
            UserPointRepository userPointRepository,
        PointHistoryRepository pointHistoryRepository
    ) {
        this.maxPointBalance = maxPointBalance;
        this.userPointRepository = userPointRepository;
        this.pointHistoryRepository = pointHistoryRepository;
    }

    /**
     * 특정 유저의 포인트를 조회합니다.
     * <p>
     * 유저 ID 검증은 Controller 레이어에서 {@code @Positive} 어노테이션을 통해 수행되므로,
     * 이 메서드는 항상 유효한(양수) userId를 받습니다.
     * 신규 유저의 경우 0 포인트를 가진 UserPoint 객체가 반환됩니다.
     *
     * @param userId 조회할 유저 ID (양수, Controller 레이어에서 검증됨)
     * @return 유저의 포인트 정보 (id, point, updateMillis)
     */
    public UserPoint getUserPoint(long userId) {
        return userPointRepository.selectById(userId);
    }

    /**
     * 특정 유저의 포인트를 충전합니다.
     * <p>
     * 충전 금액은 Controller 레이어에서 {@code @Positive} 어노테이션을 통해 검증되므로,
     * 이 메서드는 항상 양수 amount를 받습니다.
     * 충전 후 잔액과 충전 금액의 합이 maxPointBalance를 초과하면 예외가 발생합니다.
     * 충전 성공 시 CHARGE 타입 거래 내역이 기록됩니다.
     * <p>
     * <strong>동시성 제어:</strong> 동일 사용자의 동시 요청은 ReentrantLock을 통해 직렬화되어 처리됩니다.
     * 서로 다른 사용자의 요청은 독립적으로 병렬 처리됩니다.
     *
     * @param userId 충전할 유저 ID (양수, Controller 레이어에서 검증됨)
     * @param amount 충전할 금액 (양수, Controller 레이어에서 검증됨)
     * @return 업데이트된 유저의 포인트 정보
     * @throws PointException 오버플로우 발생 시 (ErrorCode.POINT_OVERFLOW), 메시지에 충전 가능 금액 포함
     */

    //@Transactional
    public UserPoint chargePoint(long userId, long amount) {
        ReentrantLock lock = getLockForUser(userId);
        lock.lock();
        try {
            // 1. 현재 포인트 조회
            UserPoint currentPoint = userPointRepository.selectById(userId);
            long currentBalance = currentPoint.point();

            // 2. 최대값 검증
            validateMaxPoint(currentBalance, amount);

            // 3. 새로운 잔액 계산
            long newBalance = currentBalance + amount;

            // 4. 포인트 업데이트
            UserPoint updatedPoint = userPointRepository.insertOrUpdate(userId, newBalance);

            // 5. 거래 내역 기록
            pointHistoryRepository.insert(userId, amount, TransactionType.CHARGE, System.currentTimeMillis());

            // 6. 업데이트된 포인트 반환
            return updatedPoint;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 특정 유저의 포인트를 사용합니다.
     * <p>
     * 사용 금액은 Controller 레이어에서 {@code @Positive} 어노테이션을 통해 검증되므로,
     * 이 메서드는 항상 양수 amount를 받습니다.
     * 현재 잔액보다 많은 금액을 사용하려고 하면 예외가 발생합니다.
     * 사용 성공 시 USE 타입 거래 내역이 기록됩니다.
     * <p>
     * <strong>동시성 제어:</strong> 동일 사용자의 동시 요청은 ReentrantLock을 통해 직렬화되어 처리됩니다.
     * 서로 다른 사용자의 요청은 독립적으로 병렬 처리됩니다.
     *
     * @param userId 포인트를 사용할 유저 ID (양수, Controller 레이어에서 검증됨)
     * @param amount 사용할 금액 (양수, Controller 레이어에서 검증됨)
     * @return 업데이트된 유저의 포인트 정보
     * @throws PointException 잔액 부족 시 (ErrorCode.INSUFFICIENT_POINTS), 메시지에 현재 잔액 포함
     */

    //@Transactional
    public UserPoint usePoint(long userId, long amount) {
        ReentrantLock lock = getLockForUser(userId);
        lock.lock();
        try {
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
        } finally {
            lock.unlock();
        }
    }

    /**
     * 특정 사용자의 Lock 객체를 가져옵니다.
     * Lock이 존재하지 않으면 새로운 Fair ReentrantLock을 생성합니다.
     *
     * @param userId 사용자 ID
     * @return 사용자별 ReentrantLock 객체
     */
    private ReentrantLock getLockForUser(long userId) {
        return userLocks.computeIfAbsent(userId, key -> new ReentrantLock(true));
    }

    private void validateMaxPoint(long currentBalance, long amount){
        if (currentBalance > maxPointBalance - amount) {
            long maxChargeableAmount = maxPointBalance - currentBalance;
            throw new PointException(ErrorCode.POINT_OVERFLOW, String.valueOf(maxChargeableAmount));
        }
    }

    private void validateSufficientBalance(long currentBalance, long amount) {
        if (currentBalance < amount) {
            throw new PointException(ErrorCode.INSUFFICIENT_POINTS, String.valueOf(currentBalance));
        }
    }
}