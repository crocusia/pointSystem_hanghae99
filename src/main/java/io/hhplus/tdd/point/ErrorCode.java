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
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "Input validation failed");

    /**
     * HTTP 응답 상태 코드
     */
    private final HttpStatus status;

    /**
     * 에러 메시지
     */
    private final String message;
}
