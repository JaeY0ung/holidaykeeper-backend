package org.planitsquare.holidaykeeper.holidaykeeperbackend.model.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import org.planitsquare.holidaykeeper.holidaykeeperbackend.model.enums.HolidayType;

@Schema(description = "공휴일 검색 요청")
public record HolidaySearchRequest(

    @Schema(
        description = "검색 시작 날짜",
        example = "2025-01-01",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "시작 날짜는 필수입니다")
    LocalDate startDate,

    @Schema(
        description = "검색 종료 날짜",
        example = "2025-12-31",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "종료 날짜는 필수입니다")
    LocalDate endDate,

    @Schema(
        description = "국가 코드 (ISO 3166-1 alpha-2)",
        example = "KR",
        pattern = "^[A-Z]{2}$",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "국가 코드는 필수입니다")
    @Pattern(regexp = "^[A-Z]{2}$", message = "국가 코드는 2자리 대문자 알파벳이어야 합니다")
    String countryCode,

    @Schema(
        description = """
            공휴일 타입 필터 (선택사항, 다중 선택 가능)
            - PUBLIC: 공휴일
            - BANK: 은행 및 관공서 휴무일
            - SCHOOL: 학교 휴일
            - AUTHORITIES: 관공서 휴무일
            - OPTIONAL: 대부분 휴무 (선택적)
            - OBSERVANCE: 기념일 (유급 휴일 아님)
            """,
        example = "[\"PUBLIC\", \"BANK\"]",
        allowableValues = { "PUBLIC", "BANK", "SCHOOL", "AUTHORITIES", "OPTIONAL", "OBSERVANCE" }
    )
    @Size(max = 6, message = "최대 6개의 타입까지 선택 가능합니다")
    List<HolidayType> types
) {

    public HolidaySearchRequest {

        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("시작 날짜는 종료 날짜보다 이전이어야 합니다");
        }
    }
}