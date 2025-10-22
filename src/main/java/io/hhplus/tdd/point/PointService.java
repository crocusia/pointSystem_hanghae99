package io.hhplus.tdd.point;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PointService {

    private final UserPointRepository userPointRepository;

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

}