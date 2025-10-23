package io.hhplus.tdd;

import io.hhplus.tdd.point.ErrorCode;
import io.hhplus.tdd.point.PointException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
class ApiControllerAdvice extends ResponseEntityExceptionHandler {

    /**
     * 포인트 시스템 비즈니스 로직 예외 처리
     * <p>
     * PointException은 ErrorCode를 포함하고 있으며,
     * 이를 통해 HTTP 상태 코드와 에러 메시지를 결정합니다.
     * <p>
     * 처리하는 예외:
     * - POINT_OVERFLOW: 최대 포인트 제한 초과
     * - INSUFFICIENT_POINTS: 잔액 부족
     */
    @ExceptionHandler(PointException.class)
    public ResponseEntity<ErrorResponse> handlePointException(PointException e) {
        return ResponseEntity
            .status(e.getErrorCode().getStatus())
            .body(new ErrorResponse(e.getErrorCode().name(), e.getMessage()));
    }

    /**
     * Bean Validation 예외 처리 (@Positive 등의 제약 조건 위반)
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolationException(ConstraintViolationException e) {
        final ErrorCode errorCode = ErrorCode.VALIDATION_ERROR;

        String detailMessage = e.getConstraintViolations().stream()
            .findFirst()
            .map(ConstraintViolation::getMessage)
            .orElse(errorCode.getMessage());

        return ResponseEntity
            .status(errorCode.getStatus())
            .body(new ErrorResponse(errorCode.name(), detailMessage));
    }

    /**
     * 경로/쿼리 변수의 타입 불일치 예외 처리 (long 타입에 소수 입력, 오버플로우)
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        final ErrorCode errorCode = ErrorCode.TYPE_MISMATCH;

        String fieldName = e.getName();
        String requiredType = e.getRequiredType() != null ? e.getRequiredType().getSimpleName() : "unknown type";
        String receivedValue = e.getValue() != null ? e.getValue().toString() : "null";

        String detailMessage = String.format(
            "%s. Field: '%s', Required Type: '%s', Received Value: '%s'.",
            errorCode.getMessage(), // 예: "Invalid Data Type"
            fieldName,
            requiredType,
            receivedValue
        );

        return ResponseEntity
            .status(errorCode.getStatus())
            .body(new ErrorResponse(errorCode.name(), detailMessage));
    }

    /**
     * 예상하지 못한 예외 처리 - 500 Internal Server Error 반환 (기존에 있던 코드)
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        return ResponseEntity.status(500).body(new ErrorResponse("500", "에러가 발생했습니다."));
    }
}
