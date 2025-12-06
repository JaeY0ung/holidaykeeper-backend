package org.planitsquare.holidaykeeper.holidaykeeperbackend.model.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;
import lombok.NonNull;

@Schema(description = "공휴일 재동기화 요청")
public record HolidayRefreshRequest(

    @Schema(
        description = "국가 코드 (ISO 3166-1 alpha-2)",
        example = "KR",
        pattern = "^[A-Z]{2}$",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "국가 코드는 필수입니다")
    @NonNull
    @Pattern(regexp = "^[A-Z]{2}$", message = "국가 코드는 2자리 대문자 알파벳이어야 합니다")

    String countryCode,

    @Schema(
        description = "연도",
        example = "2025",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "연도는 필수입니다")
    @NonNull
    @Min(value = 1975, message = "연도는 1975년 이상이어야 합니다")
    Integer year
) {

    public HolidayRefreshRequest {

        if (year > LocalDate.now().getYear()) {
            throw new IllegalArgumentException("연도는 현재 연도보다 클 수 없습니다.");
        }
    }
}
