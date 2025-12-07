package org.planitsquare.holidaykeeper.holidaykeeperbackend.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    // 공통
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "잘못된 입력 값입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다."),

    // 날짜·연도 검증
    INVALID_DATE_RANGE(HttpStatus.BAD_REQUEST, "시작 날짜는 종료 날짜보다 이전이어야 합니다."),
    YEAR_OUT_OF_RANGE(HttpStatus.BAD_REQUEST, "연도는 1975년 이상이어야 합니다."),
    FUTURE_YEAR_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "미래 연도는 허용되지 않습니다."),

    // 국가 코드 오류
    INVALID_COUNTRY_CODE(HttpStatus.BAD_REQUEST, "유효하지 않은 국가 코드입니다."),

    // 공휴일 동기화·조회 관련 오류
    COUNTRY_API_CALL_FAILED(HttpStatus.BAD_REQUEST, "국가 목록 API 호출에 실패했습니다."),
    HOLIDAY_API_CALL_FAILED(HttpStatus.BAD_REQUEST, "공휴일 API 호출에 실패했습니다."),
    HOLIDAY_API_EMPTY_RESPONSE(HttpStatus.BAD_REQUEST, "공휴일 API 응답이 비어있습니다."),
    HOLIDAY_DATA_NOT_FOUND(HttpStatus.NOT_FOUND, "공휴일 데이터를 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {

        this.status = status;
        this.message = message;
    }

    public HttpStatus getStatus() {

        return status;
    }

    public String getMessage() {

        return message;
    }
}