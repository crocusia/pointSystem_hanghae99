package io.hhplus.tdd.point;

import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 포인트 거래 내역 조회를 담당하는 Service 클래스입니다.
 * <p>
 * 이 서비스는 포인트 이력 조회에만 집중하며, 포인트 잔액 관리는 {@link PointService}에서 담당합니다.
 * SRP (Single Responsibility Principle)를 준수하기 위해 PointService로부터 분리되었습니다.
 * <p>
 * 주요 책임:
 * <ul>
 *   <li>특정 유저의 포인트 거래 내역 조회</li>
 *   <li>향후 확장: 기간별, 타입별, 페이징 조회 등</li>
 * </ul>
 *
 * @see PointService
 * @see PointHistoryRepository
 */
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