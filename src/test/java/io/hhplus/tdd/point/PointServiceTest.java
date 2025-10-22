package io.hhplus.tdd.point;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

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

    @InjectMocks
    private PointService pointService;

    /**
     * 테스트 설계)
     * selectById는 새로운 id인 경우 empty를 통해 포인트가 0인 userPoint 객체를 반환함
     * empty 호출 검증 = 강결합
     * 따라서 반환값만 검증하도록 테스트 설정
     *
     * case 1) 신규 유저 조회 시, 0을 반환하는가
     * case 2) 기존 유저 조회 시, 현재 잔액을 반환하는가
     */

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
        // Stub
        when(userPointRepository.selectById(userId)).thenReturn(existingUserPoint);

        // When
        UserPoint result = pointService.getUserPoint(userId);

        // Then
        assertThat(result.id()).isEqualTo(userId);
        assertThat(result.point()).isEqualTo(expectedPoints);
        assertThat(result.updateMillis()).isEqualTo(currentTime);
    }

    /**
     * 리팩토링 2) AI가 작성한 "포인트 조회"관련 일부 테스트 코드 삭제
     *
     * @DisplayName("포인트 조회 시 올바른 업데이트 타임스탬프를 반환해야 한다")
     * 사유) case2를 통해 이미 검증이 된, 중복 테스트이기 때문
     *
     * @DisplayName("여러 유저의 포인트를 독립적으로 조회할 수 있어야 한다")
     * 사유) case2를 통해 이미 조회 기능이 검증되었기 때문에 필요 없다고 생각함
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

}