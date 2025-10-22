package io.hhplus.tdd.point;

import java.util.List;

/**
 * 포인트 거래 내역 데이터 접근을 위한 Repository 인터페이스입니다.
 * <p>
 * 구현체: {@link io.hhplus.tdd.database.PointHistoryTable}
 * <p>
 * Phase 2에서 UserPointRepository 패턴을 따라 인터페이스로 추상화되었습니다.
 * 이를 통해 테스트 시 Mock 객체 사용이 가능하며, 향후 다른 저장소 구현체로 교체가 용이합니다.
 *
 * @see io.hhplus.tdd.database.PointHistoryTable
 */
public interface PointHistoryRepository {

    /**
     * 새로운 포인트 거래 내역을 생성합니다.
     *
     * @param userId 유저 ID
     * @param amount 거래 금액 (충전 또는 사용 금액)
     * @param type 거래 유형 (CHARGE 또는 USE)
     * @param updateMillis 거래 시각 (Unix timestamp in milliseconds)
     * @return 생성된 거래 내역 (자동 생성된 ID 포함)
     */
    PointHistory insert(long userId, long amount, TransactionType type, long updateMillis);

    /**
     * 특정 유저의 모든 거래 내역을 조회합니다.
     *
     * @param userId 조회할 유저 ID
     * @return 유저의 거래 내역 리스트 (거래 내역이 없으면 빈 리스트 반환)
     */
    List<PointHistory> selectAllByUserId(long userId);
}
