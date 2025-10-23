package io.hhplus.tdd.point;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 에러 코드 및 HTTP 상태 코드를 중앙에서 관리하는 Enum입니다.
 * <p>
 * 각 ErrorCode는 HTTP 상태 코드와 에러 메시지를 포함하며,
 * {@code name()} 메서드를 통해 에러 코드 문자열을 제공합니다.
 * <p>
 * 사용 예시:
 * <pre>
 * ErrorCode.VALIDATION_ERROR.name()    // "VALIDATION_ERROR"
 * ErrorCode.VALIDATION_ERROR.getStatus()  // HttpStatus.BAD_REQUEST
 * ErrorCode.VALIDATION_ERROR.getMessage() // "Input validation failed"
 * </pre>
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    /**
     * Controller 레이어 입력 검증 에러
     */
    TYPE_MISMATCH(HttpStatus.BAD_REQUEST, "Invalid Data Type"),

    /**
     * Bean Validation 제약 조건 위반 (예: @Positive, @NotNull 등)
     */
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "Input validation failed"),

    /**
     * 포인트 오버플로우 에러 (잔액 + 충전 금액이 최대 잔액 제한 초과)
     * 최대 잔액은 application.yml의 point.max-balance 설정값
     */
    POINT_OVERFLOW(HttpStatus.BAD_REQUEST, "Maximum point limit exceeded. You can charge up to {0} more."),

    /**
     * 포인트 잔액 부족 에러 (사용 금액이 현재 잔액보다 큼)
     * 메시지에 현재 잔액 정보를 포함하여 사용자 편의성 향상
     */
    INSUFFICIENT_POINTS(HttpStatus.BAD_REQUEST, "Insufficient points. Your current balance is {0}.");

    /**
     * HTTP 응답 상태 코드
     */
    private final HttpStatus status;

    /**
     * 에러 메시지
     */
    private final String message;
}
