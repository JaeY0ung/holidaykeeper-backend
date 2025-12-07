package org.planitsquare.holidaykeeper.holidaykeeperbackend.exception.dto;

import lombok.Builder;

@Builder
public record ErrorResponse(
    int status,
    String error,
    String message
) {

    public static ErrorResponse of(int status, String error, String message) {

        return ErrorResponse.builder()
            .status(status)
            .error(error)
            .message(message)
            .build();
    }
}