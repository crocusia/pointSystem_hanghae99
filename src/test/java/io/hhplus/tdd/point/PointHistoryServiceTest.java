package io.hhplus.tdd.point;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

/**
 * Phase 4: Point History Inquiry Feature - Unit Tests
 * PointHistoryService의 단위 테스트
 * <p>
 * SRP를 위해 PointServiceTest에서 분리되었습니다.
 * PointHistoryService는 Repository 호출만 담당하므로, 단순히 반환값 검증만 수행합니다.
 */

@ExtendWith(MockitoExtension.class)
@DisplayName("PointHistoryService Unit Tests")
class PointHistoryServiceTest {

    @Mock
    private PointHistoryRepository pointHistoryRepository;

    private PointHistoryService pointHistoryService;

    @BeforeEach
    void setUp() {
        pointHistoryService = new PointHistoryService(pointHistoryRepository);
    }

    /**
     * 테스트 설계)
     * PointHistoryService는 Repository에서 조회한 값을 그대로 반환하는 역할만 수행
     * 상세한 비즈니스 로직 검증은 불필요 (Repository의 책임)
     *
     * Phase 1의 getUserPoint()와 동일한 패턴 적용
     */

    @Test
    @DisplayName("포인트 내역 조회 시 레포지토리를 올바른 userId로 호출해야 한다")
    void getPointHistory_ShouldCallRepositoryWithCorrectUserId() {
        // Given
        long userId = 1L;

        // When
        pointHistoryService.getPointHistory(userId);

        // Then
        verify(pointHistoryRepository).selectAllByUserId(userId);
    }
}