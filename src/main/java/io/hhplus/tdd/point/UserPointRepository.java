package io.hhplus.tdd.point;

/**
 * 구현체: {@link io.hhplus.tdd.database.UserPointTable}
 */
public interface UserPointRepository {
    /**
     * 유저 ID로 포인트 정보를 조회합니다.
     * <p>
     * 존재하지 않는 유저의 경우 0 포인트를 가진 UserPoint 객체를 반환합니다.
     *
     * @param id 유저 ID
     * @return 유저의 포인트 정보
     */
    UserPoint selectById(Long id);

    /**
     * 유저의 포인트를 업데이트하거나 신규 생성합니다.
     *
     * @param id 유저 ID
     * @param amount 설정할 포인트 금액
     * @return 업데이트된 유저의 포인트 정보
     */
    UserPoint insertOrUpdate(long id, long amount);
}
