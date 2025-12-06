package org.planitsquare.holidaykeeper.holidaykeeperbackend.model.enums;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Schema(description = "공휴일 타입")
@Getter
@AllArgsConstructor
public enum HolidayType {

    @Schema(description = "공휴일")
    PUBLIC("Public", "공휴일"),

    @Schema(description = "은행 및 관공서 휴무일")
    BANK("Bank", "은행 및 관공서 휴무일"),

    @Schema(description = "학교 휴일")
    SCHOOL("School", "학교 휴일"),

    @Schema(description = "관공서 휴무일")
    AUTHORITIES("Authorities", "관공서 휴무일"),

    @Schema(description = "선택적 휴일 (대부분 휴무)")
    OPTIONAL("Optional", "대부분이 휴무하는 선택적 휴일"),

    @Schema(description = "기념일 (유급 휴일 아님)")
    OBSERVANCE("Observance", "기념일, 유급 휴일 아님");

    private final String value;

    private final String description;
}