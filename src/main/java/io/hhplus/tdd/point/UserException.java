package io.hhplus.tdd.point;

import lombok.Getter;

/**
 * ErrorCode 기반의 비즈니스 예외 클래스입니다.
 * <p>
 * 포인트 시스템의 모든 비즈니스 로직 예외는 이 클래스를 통해 발생시킵니다.
 * ErrorCode를 포함하여 일관된 에러 응답을 제공합니다.
 * <p>
 * 사용 예시:
 * <pre>
 * throw new UserException(ErrorCode.INVALID_USER_ID);
 * </pre>
 * <p>
 * ApiControllerAdvice에서 이 예외를 처리하여 적절한 HTTP 상태 코드와 에러 메시지를 반환합니다.
 *
 * @see ErrorCode
 * @see io.hhplus.tdd.ApiControllerAdvice
 */
@Getter
public class UserException extends RuntimeException {
    /**
     * 예외와 연관된 에러 코드
     */
    private final ErrorCode errorCode;

    /**
     * ErrorCode를 기반으로 UserException을 생성합니다.
     *
     * @param errorCode 예외 발생 원인을 나타내는 에러 코드
     */
    public UserException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}