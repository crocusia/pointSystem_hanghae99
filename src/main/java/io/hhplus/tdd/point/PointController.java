package io.hhplus.tdd.point;

import jakarta.validation.constraints.Positive;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/point")
@Validated
public class PointController {

    private static final Logger log = LoggerFactory.getLogger(PointController.class);
    private final PointService pointService;

    public PointController(PointService pointService) {
        this.pointService = pointService;
    }

    /**
     * 특정 유저의 포인트를 조회합니다.
     * <p>
     * 유저 ID는 {@code @Positive} 어노테이션으로 검증되며, 양수가 아닌 경우 400 Bad Request를 반환합니다.
     * 타입 불일치(소수점, 문자열 등) 또는 오버플로우 발생 시에도 400 Bad Request를 반환합니다.
     *
     * @param id 조회할 유저 ID (양수만 허용)
     * @return 유저의 포인트 정보
     */
    @GetMapping("{id}")
    public UserPoint point(
            @PathVariable @Positive long id
    ) {
        return pointService.getUserPoint(id);
    }

    /**
     * TODO - 특정 유저의 포인트 충전/이용 내역을 조회하는 기능을 작성해주세요.
     */
    @GetMapping("{id}/histories")
    public List<PointHistory> history(
            @PathVariable long id
    ) {
        return List.of();
    }

    /**
     * 특정 유저의 포인트를 충전합니다.
     * <p>
     * 유저 ID와 충전 금액은 {@code @Positive} 어노테이션으로 검증되며, 양수가 아닌 경우 400 Bad Request를 반환합니다.
     * 최대 포인트 제한을 초과하는 경우 400 Bad Request와 함께 충전 가능 금액 정보가 포함된 에러 메시지를 반환합니다.
     *
     * @param id 충전할 유저 ID (양수만 허용)
     * @param amount 충전할 금액 (양수만 허용)
     * @return 업데이트된 유저의 포인트 정보
     */
    @PatchMapping("{id}/charge")
    public UserPoint charge(
            @PathVariable @Positive long id,
            @RequestBody @Positive long amount
    ) {
        return pointService.chargePoint(id, amount);
    }

    /**
     * 특정 유저의 포인트를 사용합니다.
     * <p>
     * 유저 ID와 사용 금액은 {@code @Positive} 어노테이션으로 검증되며, 양수가 아닌 경우 400 Bad Request를 반환합니다.
     * 잔액이 부족한 경우 400 Bad Request와 함께 현재 잔액 정보가 포함된 에러 메시지를 반환합니다.
     *
     * @param id 포인트를 사용할 유저 ID (양수만 허용)
     * @param amount 사용할 금액 (양수만 허용)
     * @return 업데이트된 유저의 포인트 정보
     */
    @PatchMapping("{id}/use")
    public UserPoint use(
            @PathVariable @Positive long id,
            @RequestBody @Positive long amount
    ) {
        return pointService.usePoint(id, amount);
    }
}
