package org.planitsquare.holidaykeeper.holidaykeeperbackend.exception;

import org.planitsquare.holidaykeeper.holidaykeeperbackend.exception.dto.ErrorResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 비즈니스 로직 관련 예외 처리
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e) {

        ErrorCode errorCode = e.getErrorCode();

        ErrorResponse response = ErrorResponse.of(
            errorCode.getStatus().value(),
            errorCode.name(),
            errorCode.getMessage()
        );

        return ResponseEntity
            .status(errorCode.getStatus())
            .body(response);
    }

    /**
     * 예상하지 못한 모든 예외 처리
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {

        ErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR;

        ErrorResponse response = ErrorResponse.of(
            errorCode.getStatus().value(),
            errorCode.name(),
            e.getMessage() != null ? e.getMessage() : errorCode.getMessage()
        );

        return ResponseEntity
            .status(errorCode.getStatus())
            .body(response);
    }
}